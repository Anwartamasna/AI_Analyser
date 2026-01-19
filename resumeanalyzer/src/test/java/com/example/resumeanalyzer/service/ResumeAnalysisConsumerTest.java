package com.example.resumeanalyzer.service;

import com.example.resumeanalyzer.model.ResumeAnalysis;
import com.example.resumeanalyzer.repository.AnalysisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeAnalysisConsumerTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AnalysisRepository analysisRepository;

    @InjectMocks
    private ResumeAnalysisConsumer resumeAnalysisConsumer;

    private ResumeAnalysis testAnalysis;

    @BeforeEach
    void setUp() {
        testAnalysis = new ResumeAnalysis();
        testAnalysis.setId(1L);
        testAnalysis.setJobTitle("Pending Analysis");
        testAnalysis.setSuitabilityScore(0);
        // Clear any pending analyses
        AnalysisService.pendingAnalyses.clear();
    }

    @Test
    void consumeAnalysisResult_Success_UpdatesAnalysis() {
        // Arrange
        String message = """
                {
                    "candidate_id": 1,
                    "analysis": {
                        "compatibility_score": 85,
                        "summary": "Strong candidate",
                        "matched_skills": ["Java", "Spring"],
                        "missing_skills": ["Docker"],
                        "recommendations": ["Learn Docker", "Get certified"]
                    }
                }
                """;
        when(analysisRepository.findById(1L)).thenReturn(Optional.of(testAnalysis));
        when(analysisRepository.save(any(ResumeAnalysis.class))).thenReturn(testAnalysis);

        // Act
        resumeAnalysisConsumer.consumeAnalysisResult(message);

        // Assert
        ArgumentCaptor<ResumeAnalysis> captor = ArgumentCaptor.forClass(ResumeAnalysis.class);
        verify(analysisRepository).save(captor.capture());
        
        ResumeAnalysis saved = captor.getValue();
        assertEquals(85, saved.getSuitabilityScore());
        assertEquals("Strong candidate", saved.getSummary());
        assertEquals("Analysis Completed", saved.getJobTitle());
        assertTrue(saved.getRecommendation().contains("Learn Docker"));
    }

    @Test
    void consumeAnalysisResult_CompletesPendingFuture() {
        // Arrange
        String message = """
                {
                    "candidate_id": 1,
                    "analysis": {
                        "compatibility_score": 75
                    }
                }
                """;
        CompletableFuture<ResumeAnalysis> future = new CompletableFuture<>();
        AnalysisService.pendingAnalyses.put(1L, future);
        
        when(analysisRepository.findById(1L)).thenReturn(Optional.of(testAnalysis));
        when(analysisRepository.save(any(ResumeAnalysis.class))).thenReturn(testAnalysis);

        // Act
        resumeAnalysisConsumer.consumeAnalysisResult(message);

        // Assert
        assertTrue(future.isDone());
        assertFalse(AnalysisService.pendingAnalyses.containsKey(1L));
    }

    @Test
    void consumeAnalysisResult_AnalysisNotFound_DoesNotThrow() {
        // Arrange
        String message = """
                {
                    "candidate_id": 999,
                    "analysis": {
                        "compatibility_score": 50
                    }
                }
                """;
        when(analysisRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> resumeAnalysisConsumer.consumeAnalysisResult(message));
        verify(analysisRepository, never()).save(any());
    }

    @Test
    void consumeAnalysisResult_InvalidJson_HandlesGracefully() {
        // Arrange
        String invalidMessage = "not valid json";

        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> resumeAnalysisConsumer.consumeAnalysisResult(invalidMessage));
        verify(analysisRepository, never()).findById(any());
    }

    @Test
    void consumeAnalysisResult_WithSingleRecommendation() {
        // Arrange
        String message = """
                {
                    "candidate_id": 1,
                    "analysis": {
                        "compatibility_score": 60,
                        "recommendations": "Single recommendation text"
                    }
                }
                """;
        when(analysisRepository.findById(1L)).thenReturn(Optional.of(testAnalysis));
        when(analysisRepository.save(any(ResumeAnalysis.class))).thenReturn(testAnalysis);

        // Act
        resumeAnalysisConsumer.consumeAnalysisResult(message);

        // Assert
        ArgumentCaptor<ResumeAnalysis> captor = ArgumentCaptor.forClass(ResumeAnalysis.class);
        verify(analysisRepository).save(captor.capture());
        assertEquals("Single recommendation text", captor.getValue().getRecommendation());
    }

    @Test
    void consumeAnalysisResult_DoesNotChangeCompletedJobTitle() {
        // Arrange
        testAnalysis.setJobTitle("Software Engineer");
        String message = """
                {
                    "candidate_id": 1,
                    "analysis": {
                        "compatibility_score": 90
                    }
                }
                """;
        when(analysisRepository.findById(1L)).thenReturn(Optional.of(testAnalysis));
        when(analysisRepository.save(any(ResumeAnalysis.class))).thenReturn(testAnalysis);

        // Act
        resumeAnalysisConsumer.consumeAnalysisResult(message);

        // Assert
        ArgumentCaptor<ResumeAnalysis> captor = ArgumentCaptor.forClass(ResumeAnalysis.class);
        verify(analysisRepository).save(captor.capture());
        assertEquals("Software Engineer", captor.getValue().getJobTitle());
    }
}
