package com.example.resumeanalyzer.controller;

import com.example.resumeanalyzer.model.ResumeAnalysis;
import com.example.resumeanalyzer.model.User;
import com.example.resumeanalyzer.repository.AnalysisRepository;
import com.example.resumeanalyzer.repository.UserRepository;
import com.example.resumeanalyzer.service.MinioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final AnalysisRepository analysisRepository;
    private final MinioService minioService;

    public ProfileController(UserRepository userRepository, AnalysisRepository analysisRepository,
            MinioService minioService) {
        this.userRepository = userRepository;
        this.analysisRepository = analysisRepository;
        this.minioService = minioService;
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("fullName", user.getFullName());
        
        // Get presigned URL for profile picture if exists
        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            try {
                profile.put("profilePicture", minioService.getFileUrl(user.getProfilePicture()));
            } catch (Exception e) {
                profile.put("profilePicture", null);
            }
        } else {
            profile.put("profilePicture", null);
        }

        return ResponseEntity.ok(profile);
    }

    @PutMapping("/user")
    public ResponseEntity<?> updateUserProfile(@RequestBody Map<String, String> updateRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));

        // Update allowed fields
        if (updateRequest.containsKey("fullName")) {
            user.setFullName(updateRequest.get("fullName"));
        }
        if (updateRequest.containsKey("email")) {
            String newEmail = updateRequest.get("email");
            // Check if email is taken by another user
            if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is already taken!"));
            }
            user.setEmail(newEmail);
        }

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully!"));
    }

    @PostMapping("/picture")
    public ResponseEntity<?> uploadProfilePicture(@RequestParam("file") MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));

        try {
            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only image files are allowed!"));
            }

            // Upload to MinIO
            String fileName = minioService.uploadFile(file);
            user.setProfilePicture(fileName);
            userRepository.save(user);

            // Return the presigned URL
            String pictureUrl = minioService.getFileUrl(fileName);
            return ResponseEntity.ok(Map.of(
                "message", "Profile picture uploaded successfully!",
                "profilePicture", pictureUrl
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to upload picture: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<ResumeAnalysis>> getHistory() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));

        List<ResumeAnalysis> history = analysisRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        // Enhance history with presigned URLs
        history.forEach(analysis -> {
            if (analysis.getFileUrl() != null) {
                analysis.setFileUrl(minioService.getFileUrl(analysis.getFileUrl()));
            }
        });

        return ResponseEntity.ok(history);
    }
}

