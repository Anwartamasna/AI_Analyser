package com.example.resumeanalyzer.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ResumeAnalysisTest {

    @Test
    void defaultConstructor_CreatesEmptyAnalysis() {
        // Act
        ResumeAnalysis analysis = new ResumeAnalysis();

        // Assert
        assertNull(analysis.getId());
        assertNull(analysis.getUser());
        assertNull(analysis.getJobTitle());
        assertNotNull(analysis.getCreatedAt()); // Default is LocalDateTime.now()
    }

    @Test
    void setId_UpdatesId() {
        // Arrange
        ResumeAnalysis analysis = new ResumeAnalysis();

        // Act
        analysis.setId(123L);

        // Assert
        assertEquals(123L, analysis.getId());
    }

    @Test
    void setUser_UpdatesUser() {
        // Arrange
        ResumeAnalysis analysis = new ResumeAnalysis();
        User user = new User("testuser", "test@example.com", "Test User", "password");
        user.setId(1L);

        // Act
        analysis.setUser(user);

        // Assert
        assertNotNull(analysis.getUser());
        assertEquals(1L, analysis.getUser().getId());
    }

    @Test
    void setJobTitle_UpdatesJobTitle() {
        // Arrange
        ResumeAnalysis analysis = new ResumeAnalysis();

        // Act
        analysis.setJobTitle("Software Engineer");

        // Assert
        assertEquals("Software Engineer", analysis.getJobTitle());
    }

    @Test
    void setJobDescription_UpdatesJobDescription() {
        // Arrange
        ResumeAnalysis analysis = new ResumeAnalysis();

        // Act
        analysis.setJobDescription("Looking for a Java developer with 5 years experience");

        // Assert
        assertEquals("Looking for a Java developer with 5 years experience", analysis.getJobDescription());
    }

    @Test
    void setSuitabilityScore_UpdatesScore() {
        // Arrange
        ResumeAnalysis analysis = new ResumeAnalysis();

        // Act
        analysis.setSuitabilityScore(85);

        // Assert
        assertEquals(85, analysis.getSuitabilityScore());
    }

    @Test
    void setFileUrl_UpdatesFileUrl() {
        // Arrange
        ResumeAnalysis analysis = new ResumeAnalysis();

        // Act
        analysis.setFileUrl("resume-uuid.pdf");

        // Assert
        assertEquals("resume-uuid.pdf", analysis.getFileUrl());
    }

    @Test
    void setCreatedAt_UpdatesCreatedAt() {
        // Arrange
        ResumeAnalysis analysis = new ResumeAnalysis();
        LocalDateTime newTime = LocalDateTime.of(2024, 1, 15, 10, 30);

        // Act
        analysis.setCreatedAt(newTime);

        // Assert
        assertEquals(newTime, analysis.getCreatedAt());
    }

    @Test
    void setSummary_UpdatesSummary() {
        // Arrange
        ResumeAnalysis analysis = new ResumeAnalysis();

        // Act
        analysis.setSummary("Strong candidate with excellent technical skills");

        // Assert
        assertEquals("Strong candidate with excellent technical skills", analysis.getSummary());
    }

    @Test
    void setMatchedSkills_UpdatesMatchedSkills() {
        // Arrange
        ResumeAnalysis analysis = new ResumeAnalysis();

        // Act
        analysis.setMatchedSkills("[\"Java\", \"Spring Boot\", \"PostgreSQL\"]");

        // Assert
        assertEquals("[\"Java\", \"Spring Boot\", \"PostgreSQL\"]", analysis.getMatchedSkills());
    }

    @Test
    void setMissingSkills_UpdatesMissingSkills() {
        // Arrange
        ResumeAnalysis analysis = new ResumeAnalysis();

        // Act
        analysis.setMissingSkills("[\"Docker\", \"Kubernetes\"]");

        // Assert
        assertEquals("[\"Docker\", \"Kubernetes\"]", analysis.getMissingSkills());
    }

    @Test
    void setRecommendation_UpdatesRecommendation() {
        // Arrange
        ResumeAnalysis analysis = new ResumeAnalysis();

        // Act
        analysis.setRecommendation("Consider learning containerization technologies");

        // Assert
        assertEquals("Consider learning containerization technologies", analysis.getRecommendation());
    }

    @Test
    void createdAt_DefaultsToNow() {
        // Arrange
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        // Act
        ResumeAnalysis analysis = new ResumeAnalysis();

        // Assert
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertTrue(analysis.getCreatedAt().isAfter(before));
        assertTrue(analysis.getCreatedAt().isBefore(after));
    }

    @Test
    void allFieldsCanBeSet() {
        // Arrange
        ResumeAnalysis analysis = new ResumeAnalysis();
        User user = new User("testuser", "test@example.com", "Test User", "password");

        // Act
        analysis.setId(1L);
        analysis.setUser(user);
        analysis.setJobTitle("Developer");
        analysis.setJobDescription("Java Developer Position");
        analysis.setSuitabilityScore(90);
        analysis.setFileUrl("resume.pdf");
        analysis.setSummary("Excellent candidate");
        analysis.setMatchedSkills("[\"Java\"]");
        analysis.setMissingSkills("[\"Python\"]");
        analysis.setRecommendation("Hire immediately");

        // Assert
        assertEquals(1L, analysis.getId());
        assertNotNull(analysis.getUser());
        assertEquals("Developer", analysis.getJobTitle());
        assertEquals("Java Developer Position", analysis.getJobDescription());
        assertEquals(90, analysis.getSuitabilityScore());
        assertEquals("resume.pdf", analysis.getFileUrl());
        assertEquals("Excellent candidate", analysis.getSummary());
        assertEquals("[\"Java\"]", analysis.getMatchedSkills());
        assertEquals("[\"Python\"]", analysis.getMissingSkills());
        assertEquals("Hire immediately", analysis.getRecommendation());
    }
}
