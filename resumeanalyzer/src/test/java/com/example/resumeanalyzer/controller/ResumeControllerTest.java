package com.example.resumeanalyzer.controller;

import com.example.resumeanalyzer.service.AnalysisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeControllerTest {

    @Mock
    private AnalysisService analysisService;

    @InjectMocks
    private ResumeController resumeController;

    @Test
    void analyzeResume_Success() throws IOException {
        // Arrange
        MockMultipartFile resumeFile = new MockMultipartFile(
                "resume",
                "resume.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );
        String jobDescription = "Looking for a Java Developer";
        
        Map<String, Object> expectedResult = Map.of(
                "suitability_score", 85,
                "is_suitable", true,
                "recommendation", "Great candidate"
        );
        when(analysisService.analyzeResume(any(), anyString())).thenReturn(expectedResult);

        // Act
        ResponseEntity<?> response = resumeController.analyzeResume(resumeFile, jobDescription);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(85, body.get("suitability_score"));
        assertTrue((Boolean) body.get("is_suitable"));
    }

    @Test
    void analyzeResume_EmptyFile_ReturnsBadRequest() {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "resume",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );
        String jobDescription = "Looking for a developer";

        // Act
        ResponseEntity<?> response = resumeController.analyzeResume(emptyFile, jobDescription);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        verify(analysisService, never()).analyzeResume(any(), any());
    }

    @Test
    void analyzeResume_EmptyJobDescription_ReturnsBadRequest() {
        // Arrange
        MockMultipartFile resumeFile = new MockMultipartFile(
                "resume",
                "resume.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );
        String emptyJobDescription = "";

        // Act
        ResponseEntity<?> response = resumeController.analyzeResume(resumeFile, emptyJobDescription);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        verify(analysisService, never()).analyzeResume(any(), any());
    }

    @Test
    void analyzeResume_ServiceThrowsIOException_ReturnsInternalServerError() throws IOException {
        // Arrange
        MockMultipartFile resumeFile = new MockMultipartFile(
                "resume",
                "resume.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );
        String jobDescription = "Looking for a developer";
        
        when(analysisService.analyzeResume(any(), anyString()))
                .thenThrow(new IOException("Storage error"));

        // Act
        ResponseEntity<?> response = resumeController.analyzeResume(resumeFile, jobDescription);

        // Assert
        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    void analyzeResume_LargeFile_Success() throws IOException {
        // Arrange
        byte[] largeContent = new byte[1024 * 1024]; // 1MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "resume",
                "large_resume.pdf",
                "application/pdf",
                largeContent
        );
        String jobDescription = "Developer position";
        
        Map<String, Object> result = Map.of("suitability_score", 70);
        when(analysisService.analyzeResume(any(), anyString())).thenReturn(result);

        // Act
        ResponseEntity<?> response = resumeController.analyzeResume(largeFile, jobDescription);

        // Assert
        assertEquals(200, response.getStatusCode().value());
    }
}
