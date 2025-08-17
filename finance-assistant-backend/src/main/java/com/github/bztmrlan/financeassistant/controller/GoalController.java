package com.github.bztmrlan.financeassistant.controller;

import com.github.bztmrlan.financeassistant.dto.GoalRequest;
import com.github.bztmrlan.financeassistant.dto.GoalProgressRequest;
import com.github.bztmrlan.financeassistant.dto.GoalResponse;
import com.github.bztmrlan.financeassistant.service.GoalManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.UUID;
import com.github.bztmrlan.financeassistant.security.CustomUserDetailsService;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
@Slf4j
public class GoalController {

    private final GoalManagementService goalManagementService;


    private UUID extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Authentication is required");
        }
        
        Object principal = authentication.getPrincipal();

        return ((CustomUserDetailsService.CustomUserDetails) principal).getUserId();
    }

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(
            @RequestBody GoalRequest request,
            Authentication authentication) {
        UUID userId = extractUserIdFromAuthentication(authentication);
        GoalResponse goal = goalManagementService.createGoal(userId, request);
        return ResponseEntity.ok(goal);
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> getUserGoals(Authentication authentication) {
        UUID userId = extractUserIdFromAuthentication(authentication);
        List<GoalResponse> goals = goalManagementService.getUserGoals(userId);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/{goalId}")
    public ResponseEntity<GoalResponse> getGoal(
            @PathVariable UUID goalId,
            Authentication authentication) {
        UUID userId = extractUserIdFromAuthentication(authentication);
        return goalManagementService.getGoalById(goalId, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{goalId}")
    public ResponseEntity<GoalResponse> updateGoal(
            @PathVariable UUID goalId,
            @RequestBody GoalRequest request,
            Authentication authentication) {
        UUID userId = extractUserIdFromAuthentication(authentication);
        GoalResponse goal = goalManagementService.updateGoal(goalId, userId, request);
        return ResponseEntity.ok(goal);
    }

    @PutMapping("/{goalId}/progress")
    public ResponseEntity<GoalResponse> updateGoalProgress(
            @PathVariable UUID goalId,
            @RequestBody GoalProgressRequest request,
            Authentication authentication) {
        UUID userId = extractUserIdFromAuthentication(authentication);
        GoalResponse goal = goalManagementService.updateGoalProgress(goalId, userId, request.getAmount());
        return ResponseEntity.ok(goal);
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> deleteGoal(
            @PathVariable UUID goalId,
            Authentication authentication) {
        UUID userId = extractUserIdFromAuthentication(authentication);
        goalManagementService.deleteGoal(goalId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/evaluate")
    public ResponseEntity<Void> evaluateGoals(Authentication authentication) {
        UUID userId = extractUserIdFromAuthentication(authentication);
        goalManagementService.evaluateGoalsForUser(userId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/calculate-progress")
    public ResponseEntity<Void> calculateGoalProgressFromTransactions(Authentication authentication) {
        UUID userId = extractUserIdFromAuthentication(authentication);
        goalManagementService.calculateGoalProgressFromTransactions(userId);
        return ResponseEntity.ok().build();
    }
} 