package com.example.resumeanalyzer.controller;

import com.example.resumeanalyzer.model.Role;
import com.example.resumeanalyzer.model.User;
import com.example.resumeanalyzer.repository.UserRepository;
import com.example.resumeanalyzer.security.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthController authController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "Test User", "encodedPassword");
        testUser.setId(1L);
        testUser.setRole(Role.USER);
    }

    @Test
    void authenticateUser_Success() {
        // Arrange
        Map<String, String> loginRequest = Map.of(
                "username", "testuser",
                "password", "password123"
        );
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt-token-123");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<?> response = authController.authenticateUser(loginRequest);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("jwt-token-123", body.get("token"));
        assertEquals("testuser", body.get("username"));
        assertEquals("USER", body.get("role"));
    }

    @Test
    void authenticateUser_InvalidCredentials() {
        // Arrange
        Map<String, String> loginRequest = Map.of(
                "username", "testuser",
                "password", "wrongpassword"
        );
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act
        ResponseEntity<?> response = authController.authenticateUser(loginRequest);

        // Assert
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void authenticateUser_AdminRole() {
        // Arrange
        testUser.setRole(Role.ADMIN);
        Map<String, String> loginRequest = Map.of(
                "username", "admin",
                "password", "adminpass"
        );
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("admin-token");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<?> response = authController.authenticateUser(loginRequest);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ADMIN", body.get("role"));
    }

    @Test
    void registerUser_Success() {
        // Arrange
        Map<String, String> signUpRequest = Map.of(
                "username", "newuser",
                "email", "new@example.com",
                "fullName", "New User",
                "password", "password123"
        );
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(new User());

        // Act
        ResponseEntity<?> response = authController.registerUser(signUpRequest);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void registerUser_UsernameAlreadyExists() {
        // Arrange
        Map<String, String> signUpRequest = Map.of(
                "username", "existinguser",
                "email", "new@example.com",
                "fullName", "New User",
                "password", "password123"
        );
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // Act
        ResponseEntity<?> response = authController.registerUser(signUpRequest);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_EmailAlreadyExists() {
        // Arrange
        Map<String, String> signUpRequest = Map.of(
                "username", "newuser",
                "email", "existing@example.com",
                "fullName", "New User",
                "password", "password123"
        );
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act
        ResponseEntity<?> response = authController.registerUser(signUpRequest);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        verify(userRepository, never()).save(any());
    }
}
