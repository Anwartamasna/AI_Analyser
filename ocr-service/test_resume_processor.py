"""
Unit tests for resume_processor.py

Run with: pytest test_resume_processor.py -v --cov=. --cov-report=html
"""

import pytest
import json
from unittest.mock import Mock, patch, MagicMock
from io import BytesIO


class TestResumeProcessor:
    """Test cases for ResumeProcessor class"""

    @pytest.fixture
    def mock_minio_client(self):
        """Create a mock MinIO client"""
        with patch('resume_processor.Minio') as mock:
            yield mock

    @pytest.fixture
    def mock_requests(self):
        """Create a mock for requests library"""
        with patch('resume_processor.requests') as mock:
            yield mock

    def test_extract_text_from_txt_file(self):
        """Test text extraction from plain text file"""
        from resume_processor import ResumeProcessor
        
        with patch('resume_processor.Minio'), \
             patch('resume_processor.requests'):
            processor = ResumeProcessor()
            
            # Create a mock text file
            text_content = b"John Doe\nSoftware Engineer\n5 years experience"
            file_stream = BytesIO(text_content)
            
            result = processor.extract_text(file_stream, "resume.txt")
            
            assert "John Doe" in result
            assert "Software Engineer" in result

    def test_download_file_parses_url_correctly(self):
        """Test that file URL parsing works correctly"""
        from resume_processor import ResumeProcessor
        
        with patch('resume_processor.Minio') as mock_minio, \
             patch('resume_processor.requests'):
            
            mock_client = MagicMock()
            mock_response = MagicMock()
            mock_response.read.return_value = b"test content"
            mock_client.get_object.return_value = mock_response
            mock_minio.return_value = mock_client
            
            processor = ResumeProcessor()
            
            # Test URL parsing
            file_stream, filename = processor.download_file("http://minio:9000/resumes/test.pdf")
            
            assert filename == "test.pdf"
            mock_client.get_object.assert_called_once_with("resumes", "test.pdf")

    def test_analyze_returns_valid_json_structure(self):
        """Test that analyze returns expected JSON structure"""
        from resume_processor import ResumeProcessor
        
        mock_ollama_response = {
            "response": json.dumps({
                "compatibility_score": 75,
                "is_suitable": True,
                "summary": "• Strong candidate • Good skills",
                "experience_level": "Mid Level",
                "matched_skills": ["Python", "Java"],
                "missing_skills": ["Kubernetes"],
                "strengths": ["Problem solving"],
                "recommendations": ["• Learn Docker"],
                "ats_keywords": ["python", "java"],
                "interview_tips": "Focus on system design"
            })
        }
        
        with patch('resume_processor.Minio'), \
             patch('resume_processor.requests') as mock_requests:
            
            mock_response = MagicMock()
            mock_response.json.return_value = mock_ollama_response
            mock_response.raise_for_status = MagicMock()
            mock_requests.post.return_value = mock_response
            
            processor = ResumeProcessor()
            
            # Test with raw text input
            result = processor.analyze(
                "John Doe, Software Engineer with 5 years experience",
                "Looking for a Python developer"
            )
            
            assert "compatibility_score" in result
            assert "is_suitable" in result
            assert "summary" in result
            assert result["compatibility_score"] == 75

    def test_analyze_handles_empty_response(self):
        """Test that analyze handles empty Ollama response gracefully"""
        from resume_processor import ResumeProcessor
        
        mock_ollama_response = {
            "response": ""
        }
        
        with patch('resume_processor.Minio'), \
             patch('resume_processor.requests') as mock_requests:
            
            mock_response = MagicMock()
            mock_response.json.return_value = mock_ollama_response
            mock_response.raise_for_status = MagicMock()
            mock_requests.post.return_value = mock_response
            
            processor = ResumeProcessor()
            
            result = processor.analyze("test resume", "test job")
            
            assert "summary" in result

    def test_analyze_handles_exception(self):
        """Test that analyze handles exceptions gracefully"""
        from resume_processor import ResumeProcessor
        
        with patch('resume_processor.Minio'), \
             patch('resume_processor.requests') as mock_requests:
            
            mock_requests.post.side_effect = Exception("Connection error")
            
            processor = ResumeProcessor()
            
            result = processor.analyze("test resume", "test job")
            
            assert "summary" in result
            assert "failed" in result["summary"].lower() or "error" in result["summary"].lower()


class TestInputValidation:
    """Test input validation and edge cases"""

    def test_resume_file_prefix_handling(self):
        """Test handling of 'Resume file:' prefix in input"""
        from resume_processor import ResumeProcessor
        
        with patch('resume_processor.Minio') as mock_minio, \
             patch('resume_processor.requests'):
            
            mock_client = MagicMock()
            mock_response = MagicMock()
            mock_response.read.return_value = b"test content"
            mock_client.get_object.return_value = mock_response
            mock_minio.return_value = mock_client
            
            processor = ResumeProcessor()
            
            # Test with 'Resume file:' prefix
            file_stream, filename = processor.download_file("Resume file: http://minio:9000/resumes/test.pdf")
            
            assert filename == "test.pdf"


if __name__ == "__main__":
    pytest.main([__file__, "-v", "--cov=.", "--cov-report=term-missing"])
