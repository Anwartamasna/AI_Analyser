package com.example.resumeanalyzer.repository;

import com.example.resumeanalyzer.model.ResumeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnalysisRepository extends JpaRepository<ResumeAnalysis, Long> {
    List<ResumeAnalysis> findByUserIdOrderByCreatedAtDesc(Long userId);
}
