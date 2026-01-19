package com.example.resumeanalyzer.controller;

import com.example.resumeanalyzer.model.ResumeAnalysis;
import com.example.resumeanalyzer.model.Role;
import com.example.resumeanalyzer.model.User;
import com.example.resumeanalyzer.repository.AnalysisRepository;
import com.example.resumeanalyzer.repository.UserRepository;
import com.example.resumeanalyzer.service.MinioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private MinioService minioService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProfileController profileController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "Test User", "password");
        testUser.setId(1L);
        testUser.setRole(Role.USER);

        // Setup security context
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
    }

    @Test
    void getUserProfile_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<?> response = profileController.getUserProfile();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("testuser", body.get("username"));
        assertEquals("test@example.com", body.get("email"));
        assertEquals("Test User", body.get("fullName"));
    }

    @Test
    void getUserProfile_WithProfilePicture() {
        // Arrange
        testUser.setProfilePicture("profile-123.jpg");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(minioService.getFileUrl("profile-123.jpg")).thenReturn("http://minio/profile-123.jpg");

        // Act
        ResponseEntity<?> response = profileController.getUserProfile();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("http://minio/profile-123.jpg", body.get("profilePicture"));
    }

    @Test
    void updateUserProfile_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        Map<String, String> updateRequest = Map.of(
                "fullName", "Updated Name",
                "email", "updated@example.com"
        );

        // Act
        ResponseEntity<?> response = profileController.updateUserProfile(updateRequest);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        verify(userRepository).save(testUser);
        assertEquals("Updated Name", testUser.getFullName());
        assertEquals("updated@example.com", testUser.getEmail());
    }

    @Test
    void updateUserProfile_EmailAlreadyTaken() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);
        Map<String, String> updateRequest = Map.of("email", "taken@example.com");

        // Act
        ResponseEntity<?> response = profileController.updateUserProfile(updateRequest);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        verify(userRepository, never()).save(any());
    }

    @Test
    void uploadProfilePicture_Success() {
        // Arrange
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "profile.jpg",
                "image/jpeg",
                "image content".getBytes()
        );
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(minioService.uploadFile(any())).thenReturn("uploaded-profile.jpg");
        when(minioService.getFileUrl("uploaded-profile.jpg")).thenReturn("http://minio/uploaded-profile.jpg");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        ResponseEntity<?> response = profileController.uploadProfilePicture(imageFile);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        verify(userRepository).save(testUser);
        assertEquals("uploaded-profile.jpg", testUser.getProfilePicture());
    }

    @Test
    void uploadProfilePicture_InvalidFileType() {
        // Arrange
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "document.txt",
                "text/plain",
                "text content".getBytes()
        );
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<?> response = profileController.uploadProfilePicture(textFile);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        verify(minioService, never()).uploadFile(any());
    }

    @Test
    void getHistory_Success() {
        // Arrange
        ResumeAnalysis analysis1 = new ResumeAnalysis();
        analysis1.setId(1L);
        analysis1.setSuitabilityScore(85);
        analysis1.setFileUrl("file1.pdf");

        ResumeAnalysis analysis2 = new ResumeAnalysis();
        analysis2.setId(2L);
        analysis2.setSuitabilityScore(70);
        analysis2.setFileUrl("file2.pdf");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(analysisRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(analysis1, analysis2));
        when(minioService.getFileUrl(anyString())).thenReturn("http://minio/file.pdf");

        // Act
        ResponseEntity<List<ResumeAnalysis>> response = profileController.getHistory();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void getHistory_EmptyHistory() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(analysisRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        // Act
        ResponseEntity<List<ResumeAnalysis>> response = profileController.getHistory();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }
}
