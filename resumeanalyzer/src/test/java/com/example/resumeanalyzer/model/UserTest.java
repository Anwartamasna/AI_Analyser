package com.example.resumeanalyzer.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void constructor_SetsFieldsCorrectly() {
        // Act
        User user = new User("testuser", "test@example.com", "Test User", "password123");

        // Assert
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test User", user.getFullName());
        assertEquals("password123", user.getPassword());
    }

    @Test
    void defaultConstructor_CreatesEmptyUser() {
        // Act
        User user = new User();

        // Assert
        assertNull(user.getId());
        assertNull(user.getUsername());
        assertNull(user.getEmail());
    }

    @Test
    void defaultRole_IsUser() {
        // Act
        User user = new User("testuser", "test@example.com", "Test User", "password");

        // Assert
        assertEquals(Role.USER, user.getRole());
    }

    @Test
    void setRole_UpdatesRole() {
        // Arrange
        User user = new User("testuser", "test@example.com", "Test User", "password");

        // Act
        user.setRole(Role.ADMIN);

        // Assert
        assertEquals(Role.ADMIN, user.getRole());
    }

    @Test
    void setId_UpdatesId() {
        // Arrange
        User user = new User();

        // Act
        user.setId(100L);

        // Assert
        assertEquals(100L, user.getId());
    }

    @Test
    void setUsername_UpdatesUsername() {
        // Arrange
        User user = new User();

        // Act
        user.setUsername("newusername");

        // Assert
        assertEquals("newusername", user.getUsername());
    }

    @Test
    void setEmail_UpdatesEmail() {
        // Arrange
        User user = new User();

        // Act
        user.setEmail("new@email.com");

        // Assert
        assertEquals("new@email.com", user.getEmail());
    }

    @Test
    void setFullName_UpdatesFullName() {
        // Arrange
        User user = new User();

        // Act
        user.setFullName("New Full Name");

        // Assert
        assertEquals("New Full Name", user.getFullName());
    }

    @Test
    void setPassword_UpdatesPassword() {
        // Arrange
        User user = new User();

        // Act
        user.setPassword("newpassword");

        // Assert
        assertEquals("newpassword", user.getPassword());
    }

    @Test
    void setProfilePicture_UpdatesProfilePicture() {
        // Arrange
        User user = new User();

        // Act
        user.setProfilePicture("profile.jpg");

        // Assert
        assertEquals("profile.jpg", user.getProfilePicture());
    }

    @Test
    void setAnalysisHistory_UpdatesHistory() {
        // Arrange
        User user = new User();
        ResumeAnalysis analysis = new ResumeAnalysis();
        analysis.setId(1L);

        // Act
        user.setAnalysisHistory(List.of(analysis));

        // Assert
        assertEquals(1, user.getAnalysisHistory().size());
        assertEquals(1L, user.getAnalysisHistory().get(0).getId());
    }

    @Test
    void getAnalysisHistory_DefaultsToEmptyList() {
        // Arrange
        User user = new User();

        // Assert
        assertNotNull(user.getAnalysisHistory());
        assertTrue(user.getAnalysisHistory().isEmpty());
    }
}
