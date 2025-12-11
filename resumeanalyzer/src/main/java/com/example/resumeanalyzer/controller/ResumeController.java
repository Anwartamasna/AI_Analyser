package com.example.resumeanalyzer.controller;

import com.example.resumeanalyzer.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Main Spring Boot Application Entry Point and REST Controller.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", maxAge = 3600) // Allow all origins for dev
public class ResumeController {

    @Autowired
    private AnalysisService analysisService;

    /**
     * Endpoint to upload a resume, provide a job description, and get an AI
     * analysis.
     * 
     * @param resumeFile     The uploaded file.
     * @param jobDescription The text of the job description.
     * @return A JSON string containing the AI's suitability analysis.
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeResume(
            @RequestParam("resume") MultipartFile resumeFile,
            @RequestParam("jobDescription") String jobDescription) {

        if (resumeFile.isEmpty() || jobDescription.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Resume file and job description are required."));
        }

        try {
            // Service handles MinIO upload, DB save, and Gemini API call
            Map<String, Object> result = analysisService.analyzeResume(resumeFile, jobDescription);

            return ResponseEntity.ok(result);

        } catch (IOException e) {
            System.err.println("Error during file processing or API call: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }
}