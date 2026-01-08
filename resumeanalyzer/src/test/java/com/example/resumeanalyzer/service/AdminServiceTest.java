package com.example.resumeanalyzer.service;

import com.example.resumeanalyzer.model.ResumeAnalysis;
import com.example.resumeanalyzer.model.Role;
import com.example.resumeanalyzer.model.User;
import com.example.resumeanalyzer.repository.AnalysisRepository;
import com.example.resumeanalyzer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AnalysisRepository analysisRepository;

    @InjectMocks
    private AdminService adminService;

    private User testUser;
    private ResumeAnalysis testAnalysis;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(Role.USER);

        testAnalysis = new ResumeAnalysis();
        testAnalysis.setId(1L);
        testAnalysis.setJobDescription("Software Engineer");
        testAnalysis.setSuitabilityScore(75);
    }

    @Test
    void getAllUsers_ShouldReturnPageOfUsers() {
        // Arrange
        Page<User> expectedPage = new PageImpl<>(Arrays.asList(testUser));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(expectedPage);

        // Act
        Page<User> result = adminService.getAllUsers(0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("testuser", result.getContent().get(0).getUsername());
        verify(userRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void getUserById_WhenUserExists_ShouldReturnUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = adminService.getUserById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    void getUserById_WhenUserNotExists_ShouldReturnEmpty() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = adminService.getUserById(99L);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void updateUserRole_ShouldUpdateAndReturnUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = adminService.updateUserRole(1L, Role.ADMIN);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUserRole_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            adminService.updateUserRole(99L, Role.ADMIN);
        });
    }

    @Test
    void deleteUser_WhenUserExists_ShouldDeleteSuccessfully() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);

        // Act & Assert
        assertDoesNotThrow(() -> adminService.deleteUser(1L));
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteUser_WhenUserNotExists_ShouldThrowException() {
        // Arrange
        when(userRepository.existsById(99L)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            adminService.deleteUser(99L);
        });
    }

    @Test
    void getAllAnalyses_ShouldReturnPageOfAnalyses() {
        // Arrange
        Page<ResumeAnalysis> expectedPage = new PageImpl<>(Arrays.asList(testAnalysis));
        when(analysisRepository.findAll(any(Pageable.class))).thenReturn(expectedPage);

        // Act
        Page<ResumeAnalysis> result = adminService.getAllAnalyses(0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getStats_ShouldReturnStatistics() {
        // Arrange
        when(userRepository.count()).thenReturn(10L);
        when(analysisRepository.count()).thenReturn(50L);

        // Act
        Map<String, Object> stats = adminService.getStats();

        // Assert
        assertNotNull(stats);
        assertEquals(10L, stats.get("totalUsers"));
        assertEquals(50L, stats.get("totalAnalyses"));
    }

    @Test
    void deleteAnalysis_WhenAnalysisExists_ShouldDeleteSuccessfully() {
        // Arrange
        when(analysisRepository.existsById(1L)).thenReturn(true);
        doNothing().when(analysisRepository).deleteById(1L);

        // Act & Assert
        assertDoesNotThrow(() -> adminService.deleteAnalysis(1L));
        verify(analysisRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteAnalysis_WhenAnalysisNotExists_ShouldThrowException() {
        // Arrange
        when(analysisRepository.existsById(99L)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            adminService.deleteAnalysis(99L);
        });
    }
}
