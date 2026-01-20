package com.example.resumeanalyzer.config;

import com.example.resumeanalyzer.model.Role;
import com.example.resumeanalyzer.model.User;
import com.example.resumeanalyzer.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminSeeder adminSeeder;

    @Test
    void seedAdminUser_WhenAdminNotExists_CreatesAdmin() throws Exception {
        // Arrange
        when(userRepository.existsByUsername("AnwarTamasna")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CommandLineRunner runner = adminSeeder.seedAdminUser(userRepository, passwordEncoder);
        runner.run();

        // Assert
        verify(userRepository).save(any(User.class));
    }

    @Test
    void seedAdminUser_WhenAdminExists_DoesNotCreateAdmin() throws Exception {
        // Arrange
        when(userRepository.existsByUsername("AnwarTamasna")).thenReturn(true);

        // Act
        CommandLineRunner runner = adminSeeder.seedAdminUser(userRepository, passwordEncoder);
        runner.run();

        // Assert
        verify(userRepository, never()).save(any());
    }

    @Test
    void seedAdminUser_UsesEnvironmentPassword() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(adminSeeder, "adminPassword", "customPassword");
        when(userRepository.existsByUsername("AnwarTamasna")).thenReturn(false);
        when(passwordEncoder.encode("customPassword")).thenReturn("encodedCustomPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CommandLineRunner runner = adminSeeder.seedAdminUser(userRepository, passwordEncoder);
        runner.run();

        // Assert
        verify(passwordEncoder).encode("customPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void seedAdminUser_GeneratesRandomPassword_WhenNoEnvVar() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(adminSeeder, "adminPassword", "");
        when(userRepository.existsByUsername("AnwarTamasna")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedRandomPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CommandLineRunner runner = adminSeeder.seedAdminUser(userRepository, passwordEncoder);
        runner.run();

        // Assert
        verify(passwordEncoder).encode(argThat(password -> password != null && password.length() > 10));
        verify(userRepository).save(any(User.class));
    }
}
