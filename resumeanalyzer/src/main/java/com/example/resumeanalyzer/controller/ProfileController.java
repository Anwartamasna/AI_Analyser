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

        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "id", user.getId()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ResumeAnalysis>> getHistory() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));

        List<ResumeAnalysis> history = analysisRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        // Enhance history with presigned URLs if needed, but for listing maybe just
        // return metadata
        // If we want the file download link:
        history.forEach(analysis -> {
            if (analysis.getFileUrl() != null) {
                analysis.setFileUrl(minioService.getFileUrl(analysis.getFileUrl()));
            }
        });

        return ResponseEntity.ok(history);
    }
}
