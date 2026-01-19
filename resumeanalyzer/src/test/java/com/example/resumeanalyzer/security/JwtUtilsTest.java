package com.example.resumeanalyzer.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private JwtUtils jwtUtils;

    // Must be at least 256 bits (32 characters in Base64)
    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtYXQtbGVhc3QtMjU2LWJpdHMtbG9uZy1mb3ItaHM1MTI=";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", TEST_SECRET);
    }

    @Test
    void generateJwtToken_Success() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testuser");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(userDetails.getAuthorities()).thenAnswer(inv -> authorities);

        // Act
        String token = jwtUtils.generateJwtToken(authentication);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    void generateJwtToken_WithAdminRole() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("admin");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(userDetails.getAuthorities()).thenAnswer(inv -> authorities);

        // Act
        String token = jwtUtils.generateJwtToken(authentication);

        // Assert
        assertNotNull(token);
        String role = jwtUtils.getRoleFromJwtToken(token);
        assertEquals("ADMIN", role);
    }

    @Test
    void getUserNameFromJwtToken_Success() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testuser");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(userDetails.getAuthorities()).thenAnswer(inv -> authorities);
        String token = jwtUtils.generateJwtToken(authentication);

        // Act
        String username = jwtUtils.getUserNameFromJwtToken(token);

        // Assert
        assertEquals("testuser", username);
    }

    @Test
    void getRoleFromJwtToken_Success() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testuser");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(userDetails.getAuthorities()).thenAnswer(inv -> authorities);
        String token = jwtUtils.generateJwtToken(authentication);

        // Act
        String role = jwtUtils.getRoleFromJwtToken(token);

        // Assert
        assertEquals("USER", role);
    }

    @Test
    void validateJwtToken_ValidToken_ReturnsTrue() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testuser");
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(userDetails.getAuthorities()).thenAnswer(inv -> authorities);
        String token = jwtUtils.generateJwtToken(authentication);

        // Act
        boolean isValid = jwtUtils.validateJwtToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void validateJwtToken_InvalidToken_ReturnsFalse() {
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act
        boolean isValid = jwtUtils.validateJwtToken(invalidToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateJwtToken_ExpiredToken_ReturnsFalse() {
        // Arrange - Create an expired token manually
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        String expiredToken = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date(System.currentTimeMillis() - 86400000))
                .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // Expired 1 hour ago
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // Act
        boolean isValid = jwtUtils.validateJwtToken(expiredToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateJwtToken_MalformedToken_ReturnsFalse() {
        // Arrange
        String malformedToken = "not.a.valid.jwt.token.format";

        // Act
        boolean isValid = jwtUtils.validateJwtToken(malformedToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateJwtToken_EmptyToken_ReturnsFalse() {
        // Arrange
        String emptyToken = "";

        // Act
        boolean isValid = jwtUtils.validateJwtToken(emptyToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateJwtToken_NullToken_ReturnsFalse() {
        // Arrange
        String nullToken = null;

        // Act
        boolean isValid = jwtUtils.validateJwtToken(nullToken);

        // Assert
        assertFalse(isValid);
    }
}
