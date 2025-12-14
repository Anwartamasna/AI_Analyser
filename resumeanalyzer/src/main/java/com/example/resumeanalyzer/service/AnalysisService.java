package com.example.resumeanalyzer.service;

import com.example.resumeanalyzer.model.ResumeAnalysis;
import com.example.resumeanalyzer.model.User;
import com.example.resumeanalyzer.repository.AnalysisRepository;
import com.example.resumeanalyzer.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class AnalysisService {

    private final MinioService minioService;
    private final AnalysisRepository analysisRepository;
    private final UserRepository userRepository;
    private final ResumeAnalysisProducer resumeAnalysisProducer;

    public AnalysisService(MinioService minioService, AnalysisRepository analysisRepository,
            UserRepository userRepository, ResumeAnalysisProducer resumeAnalysisProducer) {
        this.minioService = minioService;
        this.analysisRepository = analysisRepository;
        this.userRepository = userRepository;
        this.resumeAnalysisProducer = resumeAnalysisProducer;
    }

    // Shared map for coordinating async Kafka responses
    public static final Map<Long, CompletableFuture<ResumeAnalysis>> pendingAnalyses = new ConcurrentHashMap<>();

    public Map<String, Object> analyzeResume(MultipartFile resumeFile, String jobDescription) throws IOException {
        // 1. Upload to MinIO
        String fileUrl = "";
        try {
            fileUrl = minioService.uploadFile(resumeFile);
        } catch (Exception e) {
            System.err.println("MinIO upload failed: " + e.getMessage());
            throw new IOException("Failed to upload file to storage", e);
        }

        // 2. Save Initial Record to DB
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ResumeAnalysis analysis = new ResumeAnalysis();
        analysis.setUser(user);
        analysis.setJobDescription(jobDescription);
        analysis.setJobTitle("Pending Analysis");
        analysis.setSuitabilityScore(0);
        analysis.setFileUrl(fileUrl);

        // Save and get ID
        analysis = analysisRepository.save(analysis);

        // Register Future for waiting
        CompletableFuture<ResumeAnalysis> future = new CompletableFuture<>();
        pendingAnalyses.put(analysis.getId(), future);

        // 3. Send to Kafka (Python Service)
        String resumeText = "Resume file: " + fileUrl;
        try {
            if (resumeFile.getOriginalFilename() != null && resumeFile.getOriginalFilename().endsWith(".txt")) {
                resumeText = new String(resumeFile.getBytes());
            }
        } catch (Exception e) {
            // ignore
        }

        resumeAnalysisProducer.sendAnalysisRequest(analysis.getId(), resumeText, jobDescription);

        // 4. Wait for response (Pseudo-Sync)
        try {
            // Wait up to 30 seconds for the Python service to reply
            ResumeAnalysis completedAnalysis = future.get(30, TimeUnit.SECONDS);

            // Map Entity to Frontend JSON structure
            Map<String, Object> result = new HashMap<>();
            result.put("suitability_score", completedAnalysis.getSuitabilityScore());
            result.put("is_suitable", completedAnalysis.getSuitabilityScore() >= 50); // Simple logic
            result.put("recommendation", completedAnalysis.getRecommendation());

            // Convert stored JSON strings back to Lists
            ObjectMapper mapper = new ObjectMapper();

            if (completedAnalysis.getMatchedSkills() != null) {
                try {
                    // Check if it's a JSON array string
                    String ms = completedAnalysis.getMatchedSkills();
                    if (ms.trim().startsWith("[")) {
                        result.put("key_strengths", mapper.readValue(ms, new TypeReference<List<String>>() {
                        }));
                    } else {
                        // Fallback for raw string
                        result.put("key_strengths", List.of(ms));
                    }
                } catch (Exception e) {
                    result.put("key_strengths", new ArrayList<>());
                }
            } else {
                result.put("key_strengths", new ArrayList<>());
            }

            if (completedAnalysis.getMissingSkills() != null) {
                try {
                    String ms = completedAnalysis.getMissingSkills();
                    if (ms.trim().startsWith("[")) {
                        result.put("key_gaps", mapper.readValue(ms, new TypeReference<List<String>>() {
                        }));
                    } else {
                        result.put("key_gaps", List.of(ms));
                    }
                } catch (Exception e) {
                    result.put("key_gaps", new ArrayList<>());
                }
            } else {
                result.put("key_gaps", new ArrayList<>());
            }

            return result;

        } catch (Exception e) {
            System.err.println("Timeout or error waiting for analysis: " + e.getMessage());
            // Fallback to provisional if timeout
            Map<String, Object> provisionalResult = new HashMap<>();
            provisionalResult.put("suitability_score", 0);
            provisionalResult.put("is_suitable", false);
            provisionalResult.put("job_title", "Analysis in Progress...");
            provisionalResult.put("status", "PENDING_TIMEOUT");
            provisionalResult.put("message", "Analysis taking longer than expected. Check history later.");
            return provisionalResult;
        } finally {
            // Cleanup
            pendingAnalyses.remove(analysis.getId());
        }
    }
}