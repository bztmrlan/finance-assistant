package com.github.bztmrlan.financeassistant.controller;

import com.github.bztmrlan.financeassistant.model.Budget;
import com.github.bztmrlan.financeassistant.model.BudgetCategory;
import com.github.bztmrlan.financeassistant.service.BudgetManagementService;
import com.github.bztmrlan.financeassistant.service.BudgetEvaluationService;
import com.github.bztmrlan.financeassistant.security.CustomUserDetailsService;
import com.github.bztmrlan.financeassistant.repository.UserRepository;
import com.github.bztmrlan.financeassistant.repository.CategoryRepository;
import com.github.bztmrlan.financeassistant.repository.BudgetCategoryRepository;
import com.github.bztmrlan.financeassistant.repository.BudgetRepository;
import com.github.bztmrlan.financeassistant.model.Category;
import com.github.bztmrlan.financeassistant.enums.BudgetStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@Slf4j
public class BudgetController {

    private final BudgetManagementService budgetManagementService;
    private final BudgetEvaluationService budgetEvaluationService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final BudgetRepository budgetRepository;



    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            @RequestBody CreateBudgetRequest request,
            Authentication authentication) {
        
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);
            request.getBudget().setUser(createUserReference(userId));
            

            List<BudgetCategory> processedCategoryLimits = request.getCategoryLimits().stream()
                .map(categoryLimit -> {
                    Category category = categoryRepository.findById(categoryLimit.getCategoryId())
                        .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryLimit.getCategoryId()));

                    return BudgetCategory.builder()
                        .category(category)
                        .limitAmount(categoryLimit.getLimitAmount())
                        .spentAmount(BigDecimal.ZERO)
                        .build();
                })
                .toList();
            
            Budget createdBudget = budgetManagementService.createBudget(
                request.getBudget(), 
                processedCategoryLimits
            );
            

            BudgetResponse response = convertToBudgetResponse(createdBudget);
            log.info("Created budget response: {}", response);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error creating budget", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getUserBudgets(Authentication authentication) {
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);
            List<Budget> budgets = budgetManagementService.getUserBudgets(userId);

            List<Budget> updatedBudgets = new ArrayList<>();
            for (Budget budget : budgets) {
                try {
                    Budget updatedBudget = budgetManagementService.updateBudgetSpending(budget.getId());
                    updatedBudgets.add(updatedBudget);
                } catch (Exception e) {
                    log.warn("Failed to update spending for budget {}: {}", budget.getId(), e.getMessage());
                    updatedBudgets.add(budget);
                }
            }
            
            List<BudgetResponse> responses = updatedBudgets.stream()
                .map(this::convertToBudgetResponse)
                .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error retrieving user budgets", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/active")
    public ResponseEntity<List<BudgetResponse>> getActiveUserBudgets(Authentication authentication) {
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);
            List<Budget> activeBudgets = budgetManagementService.getActiveUserBudgets(userId);
            

            List<Budget> updatedActiveBudgets = new ArrayList<>();
            for (Budget budget : activeBudgets) {
                try {
                    Budget updatedBudget = budgetManagementService.updateBudgetSpending(budget.getId());
                    updatedActiveBudgets.add(updatedBudget);
                } catch (Exception e) {
                    log.warn("Failed to update spending for budget {}: {}", budget.getId(), e.getMessage());
                    updatedActiveBudgets.add(budget);
                }
            }
            
            List<BudgetResponse> responses = updatedActiveBudgets.stream()
                .map(this::convertToBudgetResponse)
                .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error retrieving active user budgets", e);
            return ResponseEntity.internalServerError().build();
        }
    }


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
            log.info("Returning budget summary for budget {}: {} categories", budgetId, 
                    summary.getCategorySummaries() != null ? summary.getCategorySummaries().size() : 0);
            log.debug("Budget summary object: {}", summary);
            return ResponseEntity.ok(summary);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error retrieving budget summary", e);
            return ResponseEntity.internalServerError().build();
        }
    }

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

    @GetMapping("/test-delete/{budgetId}/categories/{categoryId}")
    public ResponseEntity<String> testDeleteCategoryLimit(
            @PathVariable UUID budgetId,
            @PathVariable UUID categoryId,
            Authentication authentication) {
        
        log.info("TEST DELETE request received for budget {} category {}", budgetId, categoryId);
        
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);


            Budget budget = budgetRepository.findById(budgetId)
                    .orElseThrow(() -> new IllegalArgumentException("Budget not found"));

            
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));

            

            Optional<BudgetCategory> budgetCategory = budgetCategoryRepository.findByBudgetAndCategory(budget, category);
            if (budgetCategory.isPresent()) {
                BudgetCategory bc = budgetCategory.get();



                

                long totalCount = budgetCategoryRepository.count();

                
                return ResponseEntity.ok("Test successful - found budget category with ID: " + bc.getId() + ", Total count: " + totalCount);
            } else {
                long totalCount = budgetCategoryRepository.count();
                return ResponseEntity.ok("Test successful - no budget category found, Total count: " + totalCount);
            }
            
        } catch (Exception e) {
            log.error("Test failed with exception:", e);
            return ResponseEntity.badRequest().body("Test failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/{budgetId}/categories/{categoryId}")
    public ResponseEntity<String> deleteCategoryLimit(
            @PathVariable UUID budgetId,
            @PathVariable UUID categoryId,
            Authentication authentication) {
        

        
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);

            budgetManagementService.getUserBudgets(userId).stream()
                    .filter(b -> b.getId().equals(budgetId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
            

            budgetManagementService.deleteCategoryLimit(budgetId, categoryId);
            

            return ResponseEntity.ok("Category removed from budget successfully");
            
        } catch (IllegalArgumentException e) {
            log.error("IllegalArgumentException in deleteCategoryLimit: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting category limit", e);
            log.error("Exception details:", e);
            log.error("Exception stack trace:", e.getStackTrace());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }


    @GetMapping("/attention-needed")
    public ResponseEntity<List<BudgetResponse>> getBudgetsNeedingAttention(Authentication authentication) {
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);
            List<Budget> activeBudgets = budgetManagementService.getActiveUserBudgets(userId);
            List<BudgetResponse> responses = activeBudgets.stream()
                .map(this::convertToBudgetResponse)
                .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error retrieving budgets needing attention", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{budgetId}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @PathVariable UUID budgetId,
            @RequestBody UpdateBudgetRequest request,
            Authentication authentication) {
        
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);
            

            budgetManagementService.getUserBudgets(userId).stream()
                    .filter(b -> b.getId().equals(budgetId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
            
            Budget updatedBudget = budgetManagementService.updateBudget(budgetId, request);
            BudgetResponse response = convertToBudgetResponse(updatedBudget);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating budget", e);
            return ResponseEntity.badRequest().build();
        }
    }

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

    @DeleteMapping("/{budgetId}")
    public ResponseEntity<String> deleteBudget(
            @PathVariable UUID budgetId,
            Authentication authentication) {
        
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);
            

            budgetManagementService.getUserBudgets(userId).stream()
                    .filter(b -> b.getId().equals(budgetId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
            
            budgetManagementService.deleteBudget(budgetId);
            
            return ResponseEntity.ok("Budget deleted successfully");
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting budget", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private UUID extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Authentication not available");
        }
        
        Object principal = authentication.getPrincipal();
        

        if (principal instanceof CustomUserDetailsService.CustomUserDetails) {
            return ((CustomUserDetailsService.CustomUserDetails) principal).getUserId();
        }
        

        if (principal instanceof String) {
            try {
                return UUID.fromString((String) principal);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid UUID format in authentication principal: " + principal);
            }
        }

        if (principal instanceof UUID) {
            return (UUID) principal;
        }
        
        throw new IllegalArgumentException("Unsupported authentication principal type: " + 
            (principal != null ? principal.getClass().getSimpleName() : "null"));
    }
    
    private com.github.bztmrlan.financeassistant.model.User createUserReference(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private BudgetResponse convertToBudgetResponse(Budget budget) {
        if (budget == null) {
            log.warn("Budget is null, cannot convert to response");
            return null;
        }
        
        log.debug("Converting budget {} to response", budget.getId());
        
        List<BudgetCategoryResponse> categoryResponses = budget.getCategories() != null ? 
            budget.getCategories().stream()
                .map(categoryLimit -> {
                    String categoryName = categoryLimit.getCategory() != null ? categoryLimit.getCategory().getName() : "null";
                    UUID categoryUserId = categoryLimit.getCategory() != null && categoryLimit.getCategory().getUser() != null ? 
                        categoryLimit.getCategory().getUser().getId() : null;
                    
                    log.debug("Category {} (User: {}): limitAmount={}, spentAmount={}", 
                        categoryName,
                        categoryUserId,
                        categoryLimit.getLimitAmount(),
                        categoryLimit.getSpentAmount());
                    
                    return new BudgetCategoryResponse(
                        categoryLimit.getId(),
                        categoryLimit.getCategory() != null ? categoryLimit.getCategory().getId() : null,
                        categoryName,
                        categoryLimit.getLimitAmount(),
                        categoryLimit.getSpentAmount()
                    );
                })
                .toList() : List.of();

        BigDecimal totalBudgetAmount = categoryResponses.stream()
                .map(BudgetCategoryResponse::getLimitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BudgetResponse response = new BudgetResponse(
            budget.getId(),
            budget.getName(),
            budget.getDescription(),
            budget.getStartDate(),
            budget.getEndDate(),
            budget.getStatus(),
            budget.getUser() != null ? budget.getUser().getId() : null,
            budget.getAmount() != null ? budget.getAmount() : totalBudgetAmount,
            budget.getPeriod(),
            categoryResponses
        );
            
        log.debug("Converted budget response: {}", response);
        return response;
    }

    @Data
    public static class CreateBudgetRequest {
        private Budget budget;
        private List<CategoryLimitRequest> categoryLimits;
    }
    
    @Data
    public static class CategoryLimitRequest {
        private UUID categoryId;
        private BigDecimal limitAmount;
    }

    @Data
    public static class UpdateLimitRequest {
        private java.math.BigDecimal newLimit;
    }

    @Data
    public static class UpdateBudgetRequest {
        private String name;
        private String description;
        private java.math.BigDecimal amount;
        private String period;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Data
    public static class AddCategoryLimitRequest {
        private UUID categoryId;
        private java.math.BigDecimal limitAmount;
    }

    @Data
    @AllArgsConstructor
    public static class BudgetResponse {
        private UUID id;
        private String name;
        private String description;
        private LocalDate startDate;
        private LocalDate endDate;
        private BudgetStatus status;
        private UUID userId;
        private BigDecimal amount;
        private String period;
        private List<BudgetCategoryResponse> categoryLimits;
    }

    @Data
    @AllArgsConstructor
    public static class BudgetCategoryResponse {
        private UUID id;
        private UUID categoryId;
        private String categoryName;
        private BigDecimal limitAmount;
        private BigDecimal spentAmount;
    }
} 