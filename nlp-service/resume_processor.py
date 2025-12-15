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
        self.model_name = "qwen2.5:7b" # User requested Qwen3:8b, but using qwen2.5:7b as stable equivalent or we can try "qwen2.5-coder:7b"
        
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

            # 2. Call Ollama
            prompt = f"""
            Act as an Expert HR Resume Analyzer. 
            Analyze the following resume against the Job Description.

            JOB DESCRIPTION:
            {job_description}

            RESUME TEXT:
            {resume_text}

            Provide the output STRICTLY in the following JSON format:
            {{
                "compatibility_score": <int 0-100>,
                "summary": "<concise summary of fit>",
                "matched_skills": ["<skill1>", "<skill2>"],
                "missing_skills": ["<skill1>", "<skill2>"],
                "recommendations": ["<rec1>", "<rec2>"]
            }}
            """

            logger.info(f"Analyzing resume (length: {len(resume_text)}) with job description (length: {len(job_description)})")
            logger.info(f"Ollama Prompt (first 500 chars): {prompt[:500]}...")

            payload = {
                "model": self.model_name,
                "prompt": prompt,
                "format": "json",
                "stream": False,
                "options": {
                    "temperature": 0.2
                }
            }

            logger.info("Sending request to Ollama...")
            response = requests.post(f"{self.ollama_host}/api/generate", json=payload, timeout=120)
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
                    "compatibility_score": 0,
                    "summary": "Empty response from AI model",
                    "matched_skills": [],
                    "missing_skills": [],
                    "recommendations": []
                }

        except Exception as e:
            logger.exception("Analysis failed with exception")
            logger.error(f"Analysis failed: {e}")
            # Return basic fallback
            return {
                "compatibility_score": 0,
                "summary": "Analysis failed due to internal error.",
                "matched_skills": [],
                "missing_skills": [],
                "recommendations": ["Please retry later."]
            }
