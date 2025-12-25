package com.example.resumeanalyzer.config;

import com.example.resumeanalyzer.model.Role;
import com.example.resumeanalyzer.model.User;
import com.example.resumeanalyzer.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeeder {

    @Bean
    CommandLineRunner seedAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminUsername = "AnwarTamasna";
            if (!userRepository.existsByUsername(adminUsername)) {
                User admin = new User(
                    adminUsername,
                    "admin@resumeanalyzer.com",
                    "Anwar Tamasna",
                    passwordEncoder.encode("anwartamasna")
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
