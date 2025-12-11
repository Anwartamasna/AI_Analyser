package com.example.resumeanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate; // Added for HTTP requests
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections; // Added for setting Accept header

/**
 * Service to handle the communication with the Gemini API for resume analysis.
 */
@Service
public class AnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisService.class);

    // IMPORTANT: In a real application, inject this via an environment variable or
    // application.properties.
    // For this demonstration, we use a placeholder.
    @org.springframework.beans.factory.annotation.Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-09-2025:generateContent?key=%s";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converts a file and job description into an AI suitability analysis.
     * 
     * @param resumeFile     The uploaded resume file (e.g., PDF, PNG).
     * @param jobDescription The text of the job description.
     * @return A string containing the AI's JSON-formatted analysis.
     * @throws IOException If file processing fails or API call fails.
     */
    @SuppressWarnings("null")
    public String analyzeSuitability(MultipartFile resumeFile, String jobDescription) throws IOException {

        long startTime = System.currentTimeMillis();

        // 1. Encode the resume file content to Base64
        byte[] fileBytes = resumeFile.getBytes();
        String base64Data = Base64.getEncoder().encodeToString(fileBytes);
        String mimeType = resumeFile.getContentType();

        logger.info("Time taken for file encoding: {}ms", System.currentTimeMillis() - startTime);

        // Fallback for file types, though common types like PDF/PNG are usually fine.
        if (mimeType == null || mimeType.isEmpty()) {
            mimeType = "application/octet-stream";
        }

        // 2. Construct the System Instruction
        String systemInstruction = "You are an expert HR suitability analyst. Your task is to compare the provided candidate resume document against the job description. Provide your complete assessment in a single, strict JSON object. Do NOT include any markdown formatting like ```json.";

        // 3. Construct the User Query and Multi-part Content (Request Body)
        ObjectNode requestBody = objectMapper.createObjectNode();

        // System Instruction
        ArrayNode systemInstructionParts = requestBody.putObject("systemInstruction").putArray("parts");
        systemInstructionParts.addObject().put("text", systemInstruction);

        // Contents (User Prompt + File)
        ArrayNode contents = requestBody.putArray("contents");
        ObjectNode userContent = contents.addObject();
        ArrayNode userParts = userContent.putArray("parts");

        // Part 1: Text Prompt
        String userPrompt = String.format(
                "Analyze the candidate's resume (provided below) for the following job description. Output a suitability analysis JSON: Job Description: %s",
                jobDescription);
        userParts.addObject().put("text", userPrompt);

        // Part 2: Inline File Data (Resume)
        ObjectNode inlineData = userParts.addObject().putObject("inlineData");
        inlineData.put("mimeType", mimeType);
        inlineData.put("data", base64Data);

        // Generation Config for structured JSON output
        ObjectNode generationConfig = requestBody.putObject("generationConfig");
        generationConfig.put("responseMimeType", "application/json");
        ObjectNode responseSchema = generationConfig.putObject("responseSchema");
        responseSchema.put("type", "OBJECT");

        ObjectNode properties = responseSchema.putObject("properties");

        properties.putObject("suitability_score").put("type", "NUMBER").put("description",
                "Suitability score out of 100.");
        properties.putObject("is_suitable").put("type", "BOOLEAN").put("description",
                "True if score > 70, false otherwise.");

        ObjectNode strengthsProp = properties.putObject("key_strengths");
        strengthsProp.put("type", "ARRAY");
        strengthsProp.put("description", "3-4 main strengths that match the job description.");
        strengthsProp.putObject("items").put("type", "STRING");

        ObjectNode gapsProp = properties.putObject("key_gaps");
        gapsProp.put("type", "ARRAY");
        gapsProp.put("description", "3-4 main gaps or missing skills/experience.");
        gapsProp.putObject("items").put("type", "STRING");

        properties.putObject("recommendation").put("type", "STRING").put("description",
                "A concise, actionable recommendation.");

        ArrayNode requiredFields = responseSchema.putArray("required");
        requiredFields.add("suitability_score").add("is_suitable").add("key_strengths").add("key_gaps")
                .add("recommendation");

        // --- REAL API CALL IMPLEMENTATION ---
        String geminiResponse;
        try {
            long apiStartTime = System.currentTimeMillis();
            logger.info("Sending request to Gemini API...");

            // 4. Set up HTTP Request
            RestTemplate restTemplate = new RestTemplate(); // Instantiated locally for simplicity
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            // 5. Send the Request to the Gemini API
            String apiUrl = String.format(API_URL_TEMPLATE, geminiApiKey);
            geminiResponse = restTemplate.postForObject(apiUrl, entity, String.class);

            logger.info("Time taken for Gemini API Call: {}ms", System.currentTimeMillis() - apiStartTime);

        } catch (Exception e) {
            // Catch any RestTemplate or connectivity issues
            throw new IOException(
                    "Error during actual Gemini API communication. Ensure your API key is correct and network is available. Error: "
                            + e.getMessage());
        }

        // 6. Parse the Response to extract the JSON payload
        try {
            JsonNode rootNode = objectMapper.readTree(geminiResponse);
            JsonNode textNode = rootNode.path("candidates").path(0).path("content").path("parts").path(0).path("text");

            if (textNode.isTextual()) {
                // The AI's JSON output is inside this text field.
                return textNode.asText();
            }
            throw new RuntimeException("AI response structure was unexpected or contained no text output.");

        } catch (Exception e) {
            throw new IOException("Failed to parse response from Gemini API: " + e.getMessage());
        }
    }
}