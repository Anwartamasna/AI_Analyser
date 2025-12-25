package com.example.resumeanalyzer.service;

import com.example.resumeanalyzer.model.ResumeAnalysis;
import com.example.resumeanalyzer.model.Role;
import com.example.resumeanalyzer.model.User;
import com.example.resumeanalyzer.repository.AnalysisRepository;
import com.example.resumeanalyzer.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AnalysisRepository analysisRepository;

    public AdminService(UserRepository userRepository, AnalysisRepository analysisRepository) {
        this.userRepository = userRepository;
        this.analysisRepository = analysisRepository;
    }

    public Page<User> getAllUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size, Sort.by("id").descending()));
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User updateUserRole(Long id, Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    public Page<ResumeAnalysis> getAllAnalyses(int page, int size) {
        return analysisRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    public Optional<ResumeAnalysis> getAnalysisById(Long id) {
        return analysisRepository.findById(id);
    }

    @Transactional
    public void deleteAnalysis(Long id) {
        if (!analysisRepository.existsById(id)) {
            throw new RuntimeException("Analysis not found with id: " + id);
        }
        analysisRepository.deleteById(id);
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalAnalyses", analysisRepository.count());
        return stats;
    }
}
