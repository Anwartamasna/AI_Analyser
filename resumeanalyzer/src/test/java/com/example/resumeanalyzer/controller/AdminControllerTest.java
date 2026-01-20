package com.example.resumeanalyzer.controller;

import com.example.resumeanalyzer.model.ResumeAnalysis;
import com.example.resumeanalyzer.model.Role;
import com.example.resumeanalyzer.model.User;
import com.example.resumeanalyzer.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private User testUser;
    private ResumeAnalysis testAnalysis;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "Test User", "password");
        testUser.setId(1L);
        testUser.setRole(Role.USER);

        testAnalysis = new ResumeAnalysis();
        testAnalysis.setId(1L);
        testAnalysis.setJobTitle("Software Engineer");
        testAnalysis.setSuitabilityScore(85);
        testAnalysis.setCreatedAt(LocalDateTime.now());
        testAnalysis.setUser(testUser);
    }

    // ==================== USERS ====================

    @Test
    void getAllUsers_Success() {
        // Arrange
        Page<User> userPage = new PageImpl<>(List.of(testUser));
        when(adminService.getAllUsers(anyInt(), anyInt())).thenReturn(userPage);

        // Act
        ResponseEntity<?> response = adminController.getAllUsers(0, 10);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(0, body.get("currentPage"));
        assertEquals(1L, body.get("totalItems"));
    }

    @Test
    void getUserById_Found() {
        // Arrange
        when(adminService.getUserById(1L)).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<?> response = adminController.getUserById(1L);

        // Assert
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getUserById_NotFound() {
        // Arrange
        when(adminService.getUserById(99L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = adminController.getUserById(99L);

        // Assert
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void updateUserRole_Success() {
        // Arrange
        when(adminService.updateUserRole(1L, Role.ADMIN)).thenReturn(testUser);
        Map<String, String> request = Map.of("role", "ADMIN");

        // Act
        ResponseEntity<?> response = adminController.updateUserRole(1L, request);

        // Assert
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void updateUserRole_InvalidRole() {
        // Arrange
        Map<String, String> request = Map.of("role", "INVALID_ROLE");

        // Act
        ResponseEntity<?> response = adminController.updateUserRole(1L, request);

        // Assert
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void updateUserRole_UserNotFound() {
        // Arrange
        when(adminService.updateUserRole(anyLong(), any())).thenThrow(new RuntimeException("User not found"));
        Map<String, String> request = Map.of("role", "ADMIN");

        // Act
        ResponseEntity<?> response = adminController.updateUserRole(99L, request);

        // Assert
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void deleteUser_Success() {
        // Arrange
        doNothing().when(adminService).deleteUser(1L);

        // Act
        ResponseEntity<?> response = adminController.deleteUser(1L);

        // Assert
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void deleteUser_NotFound() {
        // Arrange
        doThrow(new RuntimeException("User not found")).when(adminService).deleteUser(99L);

        // Act
        ResponseEntity<?> response = adminController.deleteUser(99L);

        // Assert
        assertEquals(404, response.getStatusCode().value());
    }

    // ==================== ANALYSES ====================

    @Test
    void getAllAnalyses_Success() {
        // Arrange
        Page<ResumeAnalysis> analysisPage = new PageImpl<>(List.of(testAnalysis));
        when(adminService.getAllAnalyses(anyInt(), anyInt())).thenReturn(analysisPage);

        // Act
        ResponseEntity<?> response = adminController.getAllAnalyses(0, 10);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(1L, body.get("totalItems"));
    }

    @Test
    void getAnalysisById_Found() {
        // Arrange
        when(adminService.getAnalysisById(1L)).thenReturn(Optional.of(testAnalysis));

        // Act
        ResponseEntity<?> response = adminController.getAnalysisById(1L);

        // Assert
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getAnalysisById_NotFound() {
        // Arrange
        when(adminService.getAnalysisById(99L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = adminController.getAnalysisById(99L);

        // Assert
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void deleteAnalysis_Success() {
        // Arrange
        doNothing().when(adminService).deleteAnalysis(1L);

        // Act
        ResponseEntity<?> response = adminController.deleteAnalysis(1L);

        // Assert
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void deleteAnalysis_NotFound() {
        // Arrange
        doThrow(new RuntimeException("Analysis not found")).when(adminService).deleteAnalysis(99L);

        // Act
        ResponseEntity<?> response = adminController.deleteAnalysis(99L);

        // Assert
        assertEquals(404, response.getStatusCode().value());
    }

    // ==================== STATS ====================

    @Test
    void getStats_Success() {
        // Arrange
        Map<String, Object> stats = Map.of("totalUsers", 10L, "totalAnalyses", 50L);
        when(adminService.getStats()).thenReturn(stats);

        // Act
        ResponseEntity<?> response = adminController.getStats();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(10L, body.get("totalUsers"));
        assertEquals(50L, body.get("totalAnalyses"));
    }
}
