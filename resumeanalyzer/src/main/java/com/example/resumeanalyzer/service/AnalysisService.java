package com.example.resumeanalyzer.service;

import com.example.resumeanalyzer.model.ResumeAnalysis;
import com.example.resumeanalyzer.model.User;
import com.example.resumeanalyzer.repository.AnalysisRepository;
import com.example.resumeanalyzer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
public class AnalysisService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    private final MinioService minioService;
    private final AnalysisRepository analysisRepository;
    private final UserRepository userRepository;

    public AnalysisService(MinioService minioService, AnalysisRepository analysisRepository,
            UserRepository userRepository) {
        this.minioService = minioService;
        this.analysisRepository = analysisRepository;
        this.userRepository = userRepository;
    }

    public Map<String, Object> analyzeResume(MultipartFile resumeFile, String jobDescription) throws IOException {

        // 1. Upload to MinIO
        String fileName = minioService.uploadFile(resumeFile);
        // We store the filename in DB so we can generate fresh presigned URLs later.

        // 2. Prepare Gemini Request
        // In a real app, use PDFBox to extract text. Here we imply context via
        // filename/placeholder or text if txt.
        String context = "Filename: " + resumeFile.getOriginalFilename();

        String requestBody = "{"
                + "\"contents\": [{"
                + "\"parts\": [{"
                + "\"text\": \"You are an expert HR AI. Analyze the following resume context against the job description. "
                + "Return ONLY a valid JSON object (no markdown, no code blocks) with the following structure: "
                + "{\\\"suitability_score\\\": <integer 0-100>, \\\"is_suitable\\\": <boolean>, "
                + "\\\"job_title\\\": \\\"<inferred job title from JD>\\\", "
                + "\\\"key_strengths\\\": [<string array>], \\\"key_gaps\\\": [<string array>], \\\"recommendation\\\": \\\"<string>\\\"}. "
                + "\\n\\nJOB DESCRIPTION:\\n" + escapeJson(jobDescription) + "\\n\\n"
                + "RESUME CONTEXT: " + escapeJson(context) + "\""
                + "}]"
                + "}]"
                + "}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_API_URL + "?key=" + geminiApiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            ObjectMapper mapper = new ObjectMapper();

            // 1. Parse outer Gemini response
            Map<String, Object> outerMap = mapper.readValue(responseBody, Map.class);

            // 2. Navigate to candidates[0].content.parts[0].text
            // Safety checks omitted for brevity but recommended in prod
            java.util.List candidates = (java.util.List) outerMap.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new IOException("Gemini returned no candidates: " + responseBody);
            }
            Map<String, Object> candidate = (Map<String, Object>) candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
            java.util.List parts = (java.util.List) content.get("parts");
            Map<String, Object> part = (Map<String, Object>) parts.get(0);
            String rawText = (String) part.get("text");

            // 3. Clean and Parse Inner JSON
            String jsonText = rawText.trim();
            if (jsonText.startsWith("```json")) {
                jsonText = jsonText.substring(7);
            }
            if (jsonText.startsWith("```")) {
                jsonText = jsonText.substring(3);
            }
            if (jsonText.endsWith("```")) {
                jsonText = jsonText.substring(0, jsonText.length() - 3);
            }

            Map<String, Object> analysisResult = mapper.readValue(jsonText, Map.class);

            // 4. Save to DB
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ResumeAnalysis analysis = new ResumeAnalysis();
            analysis.setUser(user);
            analysis.setJobDescription(jobDescription);

            // Robustly get fields from the map
            Object jobTitleObj = analysisResult.get("job_title");
            analysis.setJobTitle(jobTitleObj != null ? jobTitleObj.toString() : "Analysis");

            Object scoreObj = analysisResult.get("suitability_score");
            if (scoreObj instanceof Integer) {
                analysis.setSuitabilityScore((Integer) scoreObj);
            } else if (scoreObj instanceof String) {
                analysis.setSuitabilityScore(Integer.parseInt((String) scoreObj));
            } else {
                analysis.setSuitabilityScore(0);
            }

            analysis.setFileUrl(fileName); // Storing the MinIO object name

            analysisRepository.save(analysis);

            // 5. Return the friendly JSON map
            return analysisResult;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", e);
        } catch (Exception e) {
            if (e.getMessage().contains("429") || e.getMessage().contains("Quota exceeded")
                    || e.getMessage().contains("RESOURCE_EXHAUSTED")) {
                System.err.println("Gemini Quota Exceeded. Using MOCK response for testing purposes.");
                Map<String, Object> mockResult = Map.of(
                        "suitability_score", 85,
                        "is_suitable", true,
                        "job_title", "Mock Job Title (Quota Exceeded)",
                        "key_strengths", java.util.List.of("Persistence", "Problem Solving", "Mocking APIs"),
                        "key_gaps", java.util.List.of("Real AI Analysis"),
                        "recommendation",
                        "Your quota ran out, but this flow proves the app works! Wait a minute and try again for real AI.");

                // Save Mock Result to DB to verify full flow
                try {
                    String username = SecurityContextHolder.getContext().getAuthentication().getName();
                    User user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    ResumeAnalysis analysis = new ResumeAnalysis();
                    analysis.setUser(user);
                    analysis.setJobDescription(jobDescription);
                    analysis.setJobTitle("Mock Job (Quota Exceeded)");
                    analysis.setSuitabilityScore(85);
                    analysis.setFileUrl(fileName);

                    analysisRepository.save(analysis);
                    return mockResult;
                } catch (Exception dbEx) {
                    throw new IOException("Failed to save mock result: " + dbEx.getMessage(), dbEx);
                }
            }
            throw new IOException("Failed to process AI response: " + e.getMessage(), e);
        }
    }

    private String escapeJson(String input) {
        if (input == null)
            return "";
        return input.replace("\"", "\\\"").replace("\n", " ");
    }
}