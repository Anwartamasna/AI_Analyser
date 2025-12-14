package com.example.resumeanalyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ResumeAnalysisProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.request}")
    private String requestTopic;

    public ResumeAnalysisProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendAnalysisRequest(Long candidateId, String resumeText, String jobDescription) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("candidate_id", candidateId);
            payload.put("resume_text", resumeText);
            payload.put("job_description", jobDescription);

            String message = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(requestTopic, message);
        } catch (Exception e) {
            e.printStackTrace();
            // Handle error (log it, etc)
        }
    }
}
