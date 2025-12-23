import os
import json
import logging
import requests
import fitz  # PyMuPDF
import pytesseract
from PIL import Image
from io import BytesIO
from minio import Minio

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class ResumeProcessor:
    def __init__(self):
        logger.info("Initializing ResumeProcessor with Ollama and OCR...")
        
        # MinIO Configuration
        self.minio_endpoint = os.environ.get('MINIO_ENDPOINT', 'minio:9000')
        self.minio_access_key = os.environ.get('MINIO_ACCESS_KEY', 'minioadmin')
        self.minio_secret_key = os.environ.get('MINIO_SECRET_KEY', 'minioadmin')
        self.minio_secure = False
        
        self.minio_client = Minio(
            self.minio_endpoint,
            access_key=self.minio_access_key,
            secret_key=self.minio_secret_key,
            secure=self.minio_secure
        )
        
        # Ollama Configuration
        self.ollama_host = os.environ.get('OLLAMA_HOST', 'http://ollama:11434')
        self.model_name = "qwen2.5:1.5b"  # Using smaller model for faster CPU inference
        
        # Ensure model is pulled
        self.ensure_model_exists()

    def ensure_model_exists(self):
        try:
            logger.info(f"Checking/Pulling Ollama model: {self.model_name}...")
            # Trigger a pull. This is async in Ollama API usually, but we can fire and forget or wait.
            # Efficient way: list models, if missing, pull.
            # Simple way: always pull (idempotent but maybe slow on startup).
            # We'll just fire a pull request.
            requests.post(f"{self.ollama_host}/api/pull", json={"name": self.model_name})
            logger.info(f"Pull request sent for {self.model_name}")
        except Exception as e:
            logger.error(f"Failed to trigger model pull: {e}")

    def download_file(self, file_url_or_path):
        # Extract bucket and object name from URL or assume it's passed
        # Format: http://minio:9000/bucket/filename
        # Or just "filename" if we know the bucket.
        # Let's try to parse or search buckets.
        
        try:
            if "Resume file: " in file_url_or_path:
                file_url_or_path = file_url_or_path.replace("Resume file: ", "").strip()

            # Naive parsing
            parts = file_url_or_path.split('/')
            if len(parts) > 3:
                bucket_name = parts[-2]
                object_name = parts[-1]
            else:
                # Fallback to default bucket if URL structure isn't standard
                bucket_name = "resumes" 
                object_name = parts[-1]

            logger.info(f"Downloading {object_name} from bucket {bucket_name}...")
            response = self.minio_client.get_object(bucket_name, object_name)
            return BytesIO(response.read()), object_name
        except Exception as e:
            logger.error(f"Error downloading file: {e}")
            raise

    def extract_text(self, file_stream, filename):
        logger.info(f"Extracting text from {filename}...")
        text = ""
        
        try:
            if filename.lower().endswith('.pdf'):
                doc = fitz.open(stream=file_stream, filetype="pdf")
                for page in doc:
                    text += page.get_text()
                
                # Verify if text extraction was successful (PDF might be image-based)
                if len(text.strip()) < 50:
                    logger.info("Low text content in PDF, attempting OCR on pages...")
                    for i, page in enumerate(doc):
                        pix = page.get_pixmap()
                        img = Image.frombytes("RGB", [pix.width, pix.height], pix.samples)
                        text += pytesseract.image_to_string(img)
            
            elif filename.lower().endswith(('.png', '.jpg', '.jpeg', '.tiff', '.bmp')):
                img = Image.open(file_stream)
                text = pytesseract.image_to_string(img)
            
            else:
                # Text or other
                text = file_stream.read().decode('utf-8', errors='ignore')

        except Exception as e:
            logger.error(f"OCR/Extraction failed: {e}")
            return ""
            
        return text

    def analyze(self, resume_input, job_description):
        try:
            # 1. Get Text
            try:
                if resume_input.startswith("http") or "Resume file:" in resume_input:
                    file_stream, filename = self.download_file(resume_input)
                    resume_text = self.extract_text(file_stream, filename)
                else:
                    resume_text = resume_input # Assume raw text if not URL
            except Exception as e:
                logger.error(f"Failed to get resume text: {e}")
                resume_text = "Error extracting resume text."

            # 2. Call Ollama with enhanced prompt
            prompt = f"""You are an expert HR professional, career coach, and ATS (Applicant Tracking System) specialist with 15+ years of experience in tech recruitment.

Analyze the candidate's resume against the job description with EXTREME attention to detail. Be both encouraging and honest.

=== JOB DESCRIPTION ===
{job_description}

=== CANDIDATE'S RESUME ===
{resume_text}

=== ANALYSIS INSTRUCTIONS ===
Perform a comprehensive analysis following these guidelines:

1. **COMPATIBILITY SCORE (0-100)**: 
   - 90-100: Exceptional match, exceeds requirements
   - 75-89: Strong match, meets most requirements  
   - 60-74: Good potential, some gaps to address
   - 40-59: Partial match, significant development needed
   - 0-39: Major mismatch, consider other roles

2. **MATCHED SKILLS**: List ALL skills from the resume that match or relate to job requirements. Include:
   - Hard technical skills (programming languages, tools, frameworks)
   - Soft skills (leadership, communication, teamwork)
   - Domain knowledge and industry experience
   - Certifications and qualifications

3. **MISSING SKILLS**: Identify CRITICAL skills required by the job that are not mention in the resume. Suggest skills to improve and prioritize by importance.

4. **EXPERIENCE ANALYSIS**: Evaluate years of experience, relevance of past roles, and career progression.

5. **RECOMMENDATIONS**: Provide 4-6 SPECIFIC, ACTIONABLE recommendations:
   - How to tailor the resume for THIS specific role
   - Skills to highlight more prominently
   - Certifications or training to pursue
   - How to address experience gaps
   - Keywords to add for ATS optimization

6. **SUMMARY**: Write a 2-3 sentence professional assessment of the candidate's fit, highlighting their strongest selling points and main improvement areas.

=== OUTPUT FORMAT (STRICT JSON) ===
{{
    "compatibility_score": <integer 0-100>,
    "is_suitable": <boolean - true if score >= 60>,
    "summary": "<2-3 sentence professional assessment>",
    "experience_level": "<Entry Level / Mid Level / Senior / Executive>",
    "matched_skills": ["<skill1>", "<skill2>", "<skill3>", ...],
    "missing_skills": ["<critical_skill1>", "<critical_skill2>", ...],
    "strengths": ["<key_strength1>", "<key_strength2>", "<key_strength3>"],
    "recommendations": [
        "<specific actionable recommendation 1>",
        "<specific actionable recommendation 2>",
        "<specific actionable recommendation 3>",
        "<specific actionable recommendation 4>"
    ],
    "ats_keywords": ["<keyword1>", "<keyword2>", "<keyword3>"],
    "interview_tips": "<One key tip for interviewing for this role>"
}}

IMPORTANT: 
- Be HONEST but CONSTRUCTIVE. Don't inflate scores.
- Provide SPECIFIC advice, not generic statements.
- Focus on what the candidate CAN improve.
- Output ONLY valid JSON, no markdown or extra text."""

            logger.info(f"Analyzing resume (length: {len(resume_text)}) with job description (length: {len(job_description)})")
            logger.info(f"Ollama Prompt (first 500 chars): {prompt[:500]}...")

            payload = {
                "model": self.model_name,
                "prompt": prompt,
                "format": "json",
                "stream": False,
                "options": {
                    "temperature": 0.3,
                    "num_predict": 2048
                }
            }

            logger.info("Sending request to Ollama...")
            response = requests.post(f"{self.ollama_host}/api/generate", json=payload, timeout=300)
            response.raise_for_status()
            
            result_json = response.json()
            analysis_content = result_json.get("response", "{}")
            
            # Parse the JSON string inside "response"
            if analysis_content:
                logger.info(f"Raw Ollama Response: {analysis_content}")
                return json.loads(analysis_content)
            else:
                logger.error("Empty response from Ollama")
                return {
                    "summary": "Empty response from AI model"
                }

        except Exception as e:
            logger.exception("Analysis failed with exception")
            logger.error(f"Analysis failed: {e}")
            # Return basic fallback
            return {
                "summary": "Analysis failed due to internal error."
            }
