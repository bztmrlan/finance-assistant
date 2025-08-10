package com.github.bztmrlan.financeassistant.controller;

import com.github.bztmrlan.financeassistant.model.Budget;
import com.github.bztmrlan.financeassistant.model.BudgetCategory;
import com.github.bztmrlan.financeassistant.service.BudgetManagementService;
import com.github.bztmrlan.financeassistant.service.BudgetEvaluationService;
import com.github.bztmrlan.financeassistant.security.CustomUserDetailsService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@Slf4j
public class BudgetController {

    private final BudgetManagementService budgetManagementService;
    private final BudgetEvaluationService budgetEvaluationService;

    /**
     * Create a new budget with category limits
     */
    @PostMapping
    public ResponseEntity<Budget> createBudget(
            @RequestBody CreateBudgetRequest request,
            Authentication authentication) {
        
        try {

            UUID userId = extractUserIdFromAuthentication(authentication);

            request.getBudget().setUser(createUserReference(userId));
            
            Budget createdBudget = budgetManagementService.createBudget(
                request.getBudget(), 
                request.getCategoryLimits()
            );
            
            return ResponseEntity.ok(createdBudget);
            
        } catch (Exception e) {
            log.error("Error creating budget", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all budgets for the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<Budget>> getUserBudgets(Authentication authentication) {
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);
            List<Budget> budgets = budgetManagementService.getUserBudgets(userId);
            return ResponseEntity.ok(budgets);
        } catch (Exception e) {
            log.error("Error retrieving user budgets", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get active budgets for the authenticated user
     */
    @GetMapping("/active")
    public ResponseEntity<List<Budget>> getActiveUserBudgets(Authentication authentication) {
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);
            List<Budget> activeBudgets = budgetManagementService.getActiveUserBudgets(userId);
            return ResponseEntity.ok(activeBudgets);
        } catch (Exception e) {
            log.error("Error retrieving active user budgets", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get budget summary with spending details
     */
    @GetMapping("/{budgetId}/summary")
    public ResponseEntity<BudgetManagementService.BudgetSummary> getBudgetSummary(
            @PathVariable UUID budgetId,
            Authentication authentication) {
        
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);
            

            Budget budget = budgetManagementService.getUserBudgets(userId).stream()
                    .filter(b -> b.getId().equals(budgetId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
            
            var summary = budgetManagementService.getBudgetSummary(budgetId);
            return ResponseEntity.ok(summary);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error retrieving budget summary", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update category spending limit
     */
    @PutMapping("/{budgetId}/categories/{categoryId}/limit")
    public ResponseEntity<BudgetCategory> updateCategoryLimit(
            @PathVariable UUID budgetId,
            @PathVariable UUID categoryId,
            @RequestBody UpdateLimitRequest request,
            Authentication authentication) {
        
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);
            

            budgetManagementService.getUserBudgets(userId).stream()
                    .filter(b -> b.getId().equals(budgetId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
            
            BudgetCategory updatedCategory = budgetManagementService.updateCategoryLimit(
                budgetId, categoryId, request.getNewLimit()
            );
            
            return ResponseEntity.ok(updatedCategory);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating category limit", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Add a new category limit to an existing budget
     */
    @PostMapping("/{budgetId}/categories")
    public ResponseEntity<BudgetCategory> addCategoryLimit(
            @PathVariable UUID budgetId,
            @RequestBody AddCategoryLimitRequest request,
            Authentication authentication) {
        
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);

            budgetManagementService.getUserBudgets(userId).stream()
                    .filter(b -> b.getId().equals(budgetId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
            
            BudgetCategory newCategoryLimit = budgetManagementService.addCategoryLimit(
                budgetId, request.getCategoryId(), request.getLimitAmount()
            );
            
            return ResponseEntity.ok(newCategoryLimit);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error adding category limit", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Manually trigger budget evaluation for a specific budget
     */
    @PostMapping("/{budgetId}/evaluate")
    public ResponseEntity<String> evaluateBudget(
            @PathVariable UUID budgetId,
            Authentication authentication) {
        
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);

            budgetManagementService.getUserBudgets(userId).stream()
                    .filter(b -> b.getId().equals(budgetId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
            
            budgetManagementService.checkBudgetLimitsAndCreateAlerts(budgetId);
            
            return ResponseEntity.ok("Budget evaluation completed successfully");
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error evaluating budget", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get budgets that need attention (exceeded limits or approaching end)
     */
    @GetMapping("/attention-needed")
    public ResponseEntity<List<Budget>> getBudgetsNeedingAttention(Authentication authentication) {
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);
            List<Budget> attentionBudgets = budgetEvaluationService.getBudgetsNeedingAttention(userId);
            return ResponseEntity.ok(attentionBudgets);
        } catch (Exception e) {
            log.error("Error retrieving budgets needing attention", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Archive a budget
     */
    @PutMapping("/{budgetId}/archive")
    public ResponseEntity<String> archiveBudget(
            @PathVariable UUID budgetId,
            Authentication authentication) {
        
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);
            

            budgetManagementService.getUserBudgets(userId).stream()
                    .filter(b -> b.getId().equals(budgetId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
            
            budgetManagementService.archiveBudget(budgetId);
            
            return ResponseEntity.ok("Budget archived successfully");
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error archiving budget", e);
            return ResponseEntity.internalServerError().build();
        }
    }


    
    private UUID extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Authentication not available");
        }
        
        if (authentication.getPrincipal() instanceof CustomUserDetailsService.CustomUserDetails) {
            return ((CustomUserDetailsService.CustomUserDetails) authentication.getPrincipal()).getUserId();
        }
        
        throw new IllegalArgumentException("Unsupported authentication principal type");
    }
    
    private com.github.bztmrlan.financeassistant.model.User createUserReference(UUID userId) {
        return com.github.bztmrlan.financeassistant.model.User.builder()
                .id(userId)
                .build();
    }

    @Data
    public static class CreateBudgetRequest {
        private Budget budget;
        private List<BudgetCategory> categoryLimits;
    }

    @Data
    public static class UpdateLimitRequest {
        private java.math.BigDecimal newLimit;
    }

    @Data
    public static class AddCategoryLimitRequest {
        private UUID categoryId;
        private java.math.BigDecimal limitAmount;
    }
} 