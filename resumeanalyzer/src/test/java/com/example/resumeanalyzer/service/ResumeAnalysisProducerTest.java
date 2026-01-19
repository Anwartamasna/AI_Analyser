package com.example.resumeanalyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeAnalysisProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ResumeAnalysisProducer resumeAnalysisProducer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resumeAnalysisProducer, "requestTopic", "resume-analysis-request");
    }

    @Test
    void sendAnalysisRequest_Success() {
        // Arrange
        Long candidateId = 1L;
        String resumeText = "Java Developer with 5 years experience";
        String jobDescription = "Looking for a Java Developer";

        // Act
        resumeAnalysisProducer.sendAnalysisRequest(candidateId, resumeText, jobDescription);

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(1)).send(eq("resume-analysis-request"), messageCaptor.capture());

        String capturedMessage = messageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertTrue(capturedMessage.contains("\"candidate_id\":1"));
        assertTrue(capturedMessage.contains("Java Developer with 5 years experience"));
        assertTrue(capturedMessage.contains("Looking for a Java Developer"));
    }

    @Test
    void sendAnalysisRequest_WithNullValues_StillSends() {
        // Arrange
        Long candidateId = 2L;
        String resumeText = null;
        String jobDescription = null;

        // Act
        resumeAnalysisProducer.sendAnalysisRequest(candidateId, resumeText, jobDescription);

        // Assert
        verify(kafkaTemplate, times(1)).send(eq("resume-analysis-request"), any(String.class));
    }

    @Test
    void sendAnalysisRequest_KafkaFailure_HandlesGracefully() {
        // Arrange
        Long candidateId = 3L;
        String resumeText = "Test resume";
        String jobDescription = "Test job";
        when(kafkaTemplate.send(any(String.class), any(String.class)))
                .thenThrow(new RuntimeException("Kafka unavailable"));

        // Act - Should not throw
        assertDoesNotThrow(() -> {
            resumeAnalysisProducer.sendAnalysisRequest(candidateId, resumeText, jobDescription);
        });
    }

    @Test
    void sendAnalysisRequest_CorrectPayloadStructure() throws Exception {
        // Arrange
        Long candidateId = 100L;
        String resumeText = "Skills: Java, Python";
        String jobDescription = "Senior Developer Position";

        // Act
        resumeAnalysisProducer.sendAnalysisRequest(candidateId, resumeText, jobDescription);

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(any(String.class), messageCaptor.capture());

        // Verify JSON structure
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = objectMapper.readValue(messageCaptor.getValue(), Map.class);
        assertEquals(100, ((Number) payload.get("candidate_id")).intValue());
        assertEquals("Skills: Java, Python", payload.get("resume_text"));
        assertEquals("Senior Developer Position", payload.get("job_description"));
    }
}
