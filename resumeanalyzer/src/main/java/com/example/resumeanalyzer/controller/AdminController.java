package com.example.resumeanalyzer.controller;

import com.example.resumeanalyzer.model.ResumeAnalysis;
import com.example.resumeanalyzer.model.Role;
import com.example.resumeanalyzer.model.User;
import com.example.resumeanalyzer.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ==================== USERS ====================

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<User> users = adminService.getAllUsers(page, size);
        Map<String, Object> response = new HashMap<>();
        response.put("users", users.getContent().stream().map(this::mapUserToResponse).toList());
        response.put("currentPage", users.getNumber());
        response.put("totalItems", users.getTotalElements());
        response.put("totalPages", users.getTotalPages());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return adminService.getUserById(id)
                .map(user -> ResponseEntity.ok(mapUserToResponse(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String roleStr = request.get("role");
            Role role = Role.valueOf(roleStr.toUpperCase());
            User updatedUser = adminService.updateUserRole(id, role);
            return ResponseEntity.ok(mapUserToResponse(updatedUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: " + request.get("role")));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            adminService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== ANALYSES ====================

    @GetMapping("/analyses")
    public ResponseEntity<?> getAllAnalyses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ResumeAnalysis> analyses = adminService.getAllAnalyses(page, size);
        Map<String, Object> response = new HashMap<>();
        response.put("analyses", analyses.getContent().stream().map(this::mapAnalysisToResponse).toList());
        response.put("currentPage", analyses.getNumber());
        response.put("totalItems", analyses.getTotalElements());
        response.put("totalPages", analyses.getTotalPages());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analyses/{id}")
    public ResponseEntity<?> getAnalysisById(@PathVariable Long id) {
        return adminService.getAnalysisById(id)
                .map(analysis -> ResponseEntity.ok(mapAnalysisToResponse(analysis)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/analyses/{id}")
    public ResponseEntity<?> deleteAnalysis(@PathVariable Long id) {
        try {
            adminService.deleteAnalysis(id);
            return ResponseEntity.ok(Map.of("message", "Analysis deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== STATS ====================

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    // ==================== HELPERS ====================

    private Map<String, Object> mapUserToResponse(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("email", user.getEmail());
        map.put("fullName", user.getFullName());
        map.put("role", user.getRole().name());
        map.put("analysisCount", user.getAnalysisHistory().size());
        return map;
    }

    private Map<String, Object> mapAnalysisToResponse(ResumeAnalysis analysis) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", analysis.getId());
        map.put("jobTitle", analysis.getJobTitle());
        map.put("suitabilityScore", analysis.getSuitabilityScore());
        map.put("createdAt", analysis.getCreatedAt().toString());
        map.put("userId", analysis.getUser().getId());
        map.put("username", analysis.getUser().getUsername());
        return map;
    }
}
