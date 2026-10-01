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
            file_bytes = file_stream.getvalue() if hasattr(file_stream, 'getvalue') else (file_stream.read() if hasattr(file_stream, 'read') else file_stream)
            if filename.lower().endswith('.pdf'):
                doc = fitz.open(stream=file_bytes, filetype="pdf")
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
                img = Image.open(BytesIO(file_bytes) if isinstance(file_bytes, bytes) else file_stream)
                text = pytesseract.image_to_string(img)
            
            else:
                # Text or other
                if isinstance(file_bytes, bytes):
                    text = file_bytes.decode('utf-8', errors='ignore')
                else:
                    text = str(file_bytes)

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

            # Limit resume text length to ensure fast prompt evaluation on CPU (~3500 chars)
            trimmed_resume = resume_text[:3500] if len(resume_text) > 3500 else resume_text

            # 2. Call Ollama with concise ATS evaluation prompt
            prompt = f"""You are an ATS (Applicant Tracking System) recruiter. Evaluate the candidate's resume for the target job description.

=== JOB DESCRIPTION ===
{job_description}

=== CANDIDATE'S RESUME ===
{trimmed_resume}

Output ONLY a valid JSON object matching this exact schema:
{{
    "compatibility_score": <integer 0-100>,
    "is_suitable": <boolean - true if score >= 60>,
    "summary": "• <Assessment point 1> • <Assessment point 2> • <Assessment point 3>",
    "experience_level": "<Entry Level / Mid Level / Senior / Executive>",
    "matched_skills": ["<skill1>", "<skill2>", "<skill3>"],
    "missing_skills": ["<critical_skill1>", "<critical_skill2>"],
    "strengths": ["<strength1>", "<strength2>"],
    "recommendations": [
        "• <Actionable recommendation 1>",
        "• <Actionable recommendation 2>",
        "• <Actionable recommendation 3>"
    ],
    "ats_keywords": ["<keyword1>", "<keyword2>"],
    "interview_tips": "<One key interview tip>"
}}

IMPORTANT: Output ONLY valid JSON, no markdown formatting or extra text."""

            logger.info(f"Analyzing resume (length: {len(resume_text)}) with job description (length: {len(job_description)})")
            logger.info(f"Ollama Prompt (first 500 chars): {prompt[:500]}...")

            payload = {
                "model": self.model_name,
                "prompt": prompt,
                "format": "json",
                "stream": False,
                "options": {
                    "temperature": 0.2,
                    "num_predict": 400,
                    "num_ctx": 2048
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
