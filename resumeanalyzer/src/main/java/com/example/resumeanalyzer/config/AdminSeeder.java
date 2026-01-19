package com.example.resumeanalyzer.config;

import com.example.resumeanalyzer.model.Role;
import com.example.resumeanalyzer.model.User;
import com.example.resumeanalyzer.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

import java.security.SecureRandom;
import java.util.Base64;

@Configuration
public class AdminSeeder {

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Bean
    CommandLineRunner seedAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminUsername = "AnwarTamasna";
            if (!userRepository.existsByUsername(adminUsername)) {
                // Use environment variable, or generate a secure random password
                String password = adminPassword;
                if (password == null || password.isBlank()) {
                    SecureRandom random = new SecureRandom();
                    byte[] bytes = new byte[16];
                    random.nextBytes(bytes);
                    password = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                    System.out.println("========================================");
                    System.out.println("GENERATED ADMIN PASSWORD: " + password);
                    System.out.println("Please save this password and set ADMIN_PASSWORD env var!");
                    System.out.println("========================================");
                }
                
                User admin = new User(
                    adminUsername,
                    "admin@resumeanalyzer.com",
                    "Anwar Tamasna",
                    passwordEncoder.encode(password)
                );
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);
                System.out.println("Admin user '" + adminUsername + "' created successfully.");
            } else {
                System.out.println("Admin user '" + adminUsername + "' already exists.");
            }
        };
    }
}
