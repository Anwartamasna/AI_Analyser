package com.example.resumeanalyzer.service;

import com.example.resumeanalyzer.model.ResumeAnalysis;
import com.example.resumeanalyzer.repository.AnalysisRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ResumeAnalysisConsumer {

    private final ObjectMapper objectMapper;
    private final AnalysisRepository analysisRepository;

    public ResumeAnalysisConsumer(ObjectMapper objectMapper, AnalysisRepository analysisRepository) {
        this.objectMapper = objectMapper;
        this.analysisRepository = analysisRepository;
    }

    @KafkaListener(topics = "${kafka.topic.response}", groupId = "resume-group")
    public void consumeAnalysisResult(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            long candidateId = rootNode.get("candidate_id").asLong();
            JsonNode analysisData = rootNode.get("analysis");

            System.out.println("Received analysis for candidate " + candidateId);

            Optional<ResumeAnalysis> analysisOpt = analysisRepository.findById(candidateId);
            if (analysisOpt.isPresent()) {
                ResumeAnalysis analysis = analysisOpt.get();

                // Update fields
                if (analysisData.has("compatibility_score")) {
                    analysis.setSuitabilityScore(analysisData.get("compatibility_score").asInt());
                }

                if (analysisData.has("summary")) {
                    analysis.setSummary(analysisData.get("summary").asText());
                }

                if (analysisData.has("matched_skills")) {
                    analysis.setMatchedSkills(analysisData.get("matched_skills").toString());
                }

                if (analysisData.has("missing_skills")) {
                    analysis.setMissingSkills(analysisData.get("missing_skills").toString());
                }

                if (analysisData.has("recommendations")) {
                    // Check if it's an array and convert to text, or if it's a string
                    JsonNode recNode = analysisData.get("recommendations");
                    if (recNode.isArray()) {
                        // Join with newlines or keep as JSON. Frontend expects string or we can
                        // convert.
                        // But for now let's store as JSON string representation to be safe,
                        // or better yet, join them for the 'recommendation' field if Frontend expects a
                        // single string?
                        // Frontend App.jsx uses `result.recommendation` (singular) but renders it as
                        // text.
                        // Python returns `recommendations` (plural list).
                        // Let's join them.
                        StringBuilder sb = new StringBuilder();
                        for (JsonNode n : recNode) {
                            if (sb.length() > 0)
                                sb.append("\n\n");
                            sb.append(n.asText());
                        }
                        analysis.setRecommendation(sb.toString());
                    } else {
                        analysis.setRecommendation(recNode.asText());
                    }
                }

                // We could also store detailed recommendations if the model had a field for it.
                // For now, we update the existing fields matching the model.
                // Ideally, change jobTitle to status or add a status field, but I'll set
                // jobTitle to "Completed" or similar if implied?
                // The Python service returns "summary", "matched_skills", etc.
                // Currently ResumeAnalysis entity is limited. I'll just update score and maybe
                // prepend summary to JobTitle or Description?
                // Better to leave jobTitle as is if provided, or update if it was "Pending
                // Analysis".

                if ("Pending Analysis".equals(analysis.getJobTitle())) {
                    analysis.setJobTitle("Analysis Completed");
                }

                // If we want to store the full JSON result, we should add a field to
                // ResumeAnalysis.
                // For this MVP step, updating the score is the key proof of concept.

                analysisRepository.save(analysis);

                // Complete the Future if AnalysisService is waiting
                if (AnalysisService.pendingAnalyses.containsKey(candidateId)) {
                    AnalysisService.pendingAnalyses.get(candidateId).complete(analysis);
                    AnalysisService.pendingAnalyses.remove(candidateId);
                }

                System.out.println("Updated analysis record for ID: " + candidateId);
            } else {
                System.err.println("Analysis record not found for ID: " + candidateId);
            }

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
