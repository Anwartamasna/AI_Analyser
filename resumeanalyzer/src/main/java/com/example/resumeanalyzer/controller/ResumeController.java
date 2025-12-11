package com.example.resumeanalyzer.controller;

import com.example.resumeanalyzer.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Main Spring Boot Application Entry Point and REST Controller.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173") // Allow React (default Vite port) to communicate
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
    public ResponseEntity<String> analyzeResume(
            @RequestParam("resume") MultipartFile resumeFile,
            @RequestParam("jobDescription") String jobDescription) {

        if (resumeFile.isEmpty() || jobDescription.isEmpty()) {
            return new ResponseEntity<>("{\"error\": \"Resume file and job description are required.\"}",
                    HttpStatus.BAD_REQUEST);
        }

        try {
            // Service handles the conversion to Base64 and the Gemini API call
            String resultJson = analysisService.analyzeSuitability(resumeFile, jobDescription);

            // The service returns the raw JSON from the AI, which we pass through.
            return new ResponseEntity<>(resultJson, HttpStatus.OK);

        } catch (IOException e) {
            System.err.println("Error during file processing or API call: " + e.getMessage());
            return new ResponseEntity<>("{\"error\": \"Internal server error during analysis. Check service logs.\"}",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}