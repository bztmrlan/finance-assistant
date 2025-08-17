package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.dto.UpdateBudgetRequest;
import com.github.bztmrlan.financeassistant.model.*;
import com.github.bztmrlan.financeassistant.repository.*;
import com.github.bztmrlan.financeassistant.enums.BudgetStatus;
import com.github.bztmrlan.financeassistant.enums.SourceType;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetManagementService {

    private final BudgetRepository budgetRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;
    private final CategoryRepository categoryRepository;


    @Transactional
    public Budget createBudget(Budget budget, List<BudgetCategory> categoryLimits) {
        budget.setStatus(BudgetStatus.ACTIVE);
        Budget savedBudget = budgetRepository.save(budget);
        categoryLimits.forEach(bc -> bc.setBudget(savedBudget));
        budgetCategoryRepository.saveAll(categoryLimits);
        return savedBudget;
    }


    @Transactional
    public BudgetCategory updateCategoryLimit(UUID budgetId, UUID categoryId, BigDecimal newLimit) {
        Optional<BudgetCategory> existing = budgetCategoryRepository.findAll().stream()
                .filter(bc -> bc.getBudget().getId().equals(budgetId) && 
                             bc.getCategory().getId().equals(categoryId))
                .findFirst();
        
        if (existing.isPresent()) {
            BudgetCategory budgetCategory = existing.get();
            budgetCategory.setLimitAmount(newLimit);
            return budgetCategoryRepository.save(budgetCategory);
        } else {
            throw new IllegalArgumentException("Budget category not found");
        }
    }


    @Transactional
    public BudgetCategory addCategoryLimit(UUID budgetId, UUID categoryId, BigDecimal limitAmount) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (budgetCategoryRepository.existsByBudgetAndCategory(budget, category)) {
            throw new IllegalArgumentException("Category limit already exists for this budget");
        }
        
        BudgetCategory budgetCategory = BudgetCategory.builder()
                .budget(budget)
                .category(category)
                .limitAmount(limitAmount)
                .spentAmount(BigDecimal.ZERO)
                .build();
        
        return budgetCategoryRepository.save(budgetCategory);
    }

    @Transactional
    public void deleteCategoryLimit(UUID budgetId, UUID categoryId) {
        log.info("Starting deletion of category {} from budget {}", categoryId, budgetId);
        
        try {
            Budget budget = budgetRepository.findById(budgetId)
                    .orElseThrow(() -> new IllegalArgumentException("Budget not found"));

            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));

            Optional<BudgetCategory> budgetCategory = budgetCategoryRepository.findByBudgetAndCategory(budget, category);
            
            budgetCategory.ifPresent(bc -> budgetCategoryRepository.deleteById(bc.getId()));

        } catch (Exception e) {
            log.error("Exception during category deletion: {}", e.getMessage(), e);
            throw e;
        }
    }


    public BigDecimal calculateCategorySpending(UUID budgetId, UUID categoryId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
        
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndCategoryIdAndDateBetween(
                    budget.getUser().getId(),
                    categoryId,
                    budget.getStartDate(),
                    budget.getEndDate()
                );
        
        return transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    @Transactional
    public Budget updateBudgetSpending(UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
        
        List<BudgetCategory> categories = budget.getCategories();
        
        for (BudgetCategory budgetCategory : categories) {
            BigDecimal totalSpending = calculateCategorySpending(budgetId, budgetCategory.getCategory().getId());
            

            BigDecimal displaySpending;
            if (totalSpending.compareTo(BigDecimal.ZERO) < 0) {
                displaySpending = totalSpending.abs();
            } else if (totalSpending.compareTo(BigDecimal.ZERO) > 0) {
                displaySpending = totalSpending.abs();
            } else {
                displaySpending = BigDecimal.ZERO;
            }
            
            log.debug("Budget {} category {}: calculated totalSpending={}, displaySpending={}, previous spentAmount={}", 
                budgetId, 
                budgetCategory.getCategory().getName(),
                totalSpending,
                displaySpending,
                budgetCategory.getSpentAmount());
            
            budgetCategory.setSpentAmount(displaySpending);
            budgetCategoryRepository.save(budgetCategory);
        }

        budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
        
        log.info("Updated spending amounts for budget {} - all amounts converted to absolute values", budgetId);
        return budget;
    }

    @Transactional
    public Budget updateBudget(UUID budgetId, UpdateBudgetRequest request) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
        

        if (request.getName() != null) {
            budget.setName(request.getName());
        }
        if (request.getDescription() != null) {
            budget.setDescription(request.getDescription());
        }
        if (request.getPeriod() != null) {
            budget.setPeriod(request.getPeriod());
        }
        if (request.getStartDate() != null) {
            budget.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            budget.setEndDate(request.getEndDate());
        }
        
        Budget updatedBudget = budgetRepository.save(budget);
        log.info("Updated budget {} with new details: name={}, period={}, startDate={}, endDate={}", 
                budgetId, request.getName(), request.getPeriod(), request.getStartDate(), request.getEndDate());
        
        return updatedBudget;
    }

    @Transactional
    public void checkBudgetLimitsAndCreateAlerts(UUID budgetId) {

        Budget budget = updateBudgetSpending(budgetId);
        
        List<BudgetCategory> exceededCategories = budgetCategoryRepository.findExceededCategories(budgetId);
        
        for (BudgetCategory exceededCategory : exceededCategories) {

            boolean alertExists = alertRepository.findAll().stream()
                    .anyMatch(alert -> alert.getSourceId().equals(exceededCategory.getId()) &&
                                     alert.getSourceType() == SourceType.BUDGET &&
                                     !alert.isRead());
            
            if (!alertExists) {
                createBudgetAlert(budget, exceededCategory);
            }
        }
        
        log.info("Budget limit check completed for budget {}. {} categories exceeded limits.", 
                budgetId, exceededCategories.size());
    }


    private void createBudgetAlert(Budget budget, BudgetCategory exceededCategory) {
        String message = String.format(
            "Budget '%s' - Category '%s' has exceeded its limit. " +
            "Limit: $%.2f, Spent: $%.2f",
            budget.getName(),
            exceededCategory.getCategory().getName(),
            exceededCategory.getLimitAmount(),
            exceededCategory.getSpentAmount()
        );
        
        Alert alert = Alert.builder()
                .user(budget.getUser())
                .sourceType(SourceType.BUDGET)
                .sourceId(exceededCategory.getId())
                .message(message)
                .read(false)
                .createdAt(java.time.Instant.now())
                .build();
        
        alertRepository.save(alert);
        log.info("Created budget alert for user {}: {}", budget.getUser().getId(), message);
    }


    public BudgetSummary getBudgetSummary(UUID budgetId) {

        Budget budget = updateBudgetSpending(budgetId);
        
        List<BudgetCategory> categories = budget.getCategories();
        

        log.info("Budget {} has {} categories", budgetId, categories.size());
        categories.forEach(cat -> {
            log.info("Category {}: limitAmount={}, spentAmount={}", 
                cat.getCategory() != null ? cat.getCategory().getName() : "null",
                cat.getLimitAmount(),
                cat.getSpentAmount());
        });
        
        BigDecimal totalBudgeted = categories.stream()
                .map(BudgetCategory::getLimitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalSpent = categories.stream()
                .map(BudgetCategory::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("Budget {} summary calculation: totalBudgeted={}, totalSpent={}", 
            budgetId, totalBudgeted, totalSpent);

        List<CategorySummary> categorySummaries = categories.stream()
                .map(cat -> CategorySummary.builder()
                        .categoryId(cat.getCategory().getId())
                        .categoryName(cat.getCategory().getName())
                        .limitAmount(cat.getLimitAmount())
                        .spentAmount(cat.getSpentAmount())
                        .progressPercentage(cat.getLimitAmount().compareTo(BigDecimal.ZERO) > 0 ? 
                            cat.getSpentAmount().divide(cat.getLimitAmount(), 2, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) : 
                            BigDecimal.ZERO)
                        .build())
                .toList();
        
        BudgetSummary summary = BudgetSummary.builder()
                .budgetId(budgetId)
                .budgetName(budget.getName())
                .startDate(budget.getStartDate())
                .endDate(budget.getEndDate())
                .status(budget.getStatus())
                .totalBudgeted(totalBudgeted)
                .totalSpent(totalSpent)
                .remainingAmount(totalBudgeted.subtract(totalSpent))
                .categorySummaries(categorySummaries)
                .build();
        
        log.info("Created BudgetSummary for budget {}: {} categories, totalBudgeted={}, totalSpent={}", 
                budgetId, categorySummaries.size(), totalBudgeted, totalSpent);
        log.debug("BudgetSummary object: {}", summary);
        
        return summary;
    }


    public List<Budget> getUserBudgets(UUID userId) {
        return budgetRepository.findByUserIdWithUserCategories(userId);
    }

    public List<Budget> getActiveUserBudgets(UUID userId) {
        return budgetRepository.findByUserIdWithUserCategories(userId).stream()
                .filter(budget -> budget.getStatus() == BudgetStatus.ACTIVE)
                .toList();
    }


    @Transactional
    public void archiveBudget(UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
        
        budget.setStatus(BudgetStatus.COMPLETED);
        budgetRepository.save(budget);
        
        log.info("Archived budget {}", budgetId);
    }


    @Transactional
    public void deleteBudget(UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));

        budgetCategoryRepository.deleteByBudget(budget);
        

        budgetRepository.deleteById(budgetId);
        
        log.info("Permanently deleted budget {} and all associated data", budgetId);
    }


    @Transactional
    public void evaluateBudget(UUID budgetId, UUID userId) {
        updateBudgetSpending(budgetId);
        checkBudgetLimitsAndCreateAlerts(budgetId);
    }



    @Transactional
    public void updateBudgetCategorySpending(UUID budgetId, UUID categoryId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
        
        BudgetCategory budgetCategory = budgetCategoryRepository.findByBudgetAndCategory(budget, 
            categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found")))
                .orElseThrow(() -> new IllegalArgumentException("Budget category not found"));
        
        BigDecimal totalSpending = calculateCategorySpending(budgetId, categoryId);
        

        BigDecimal displaySpending;
        if (totalSpending.compareTo(BigDecimal.ZERO) < 0) {
            displaySpending = totalSpending.abs();
        } else if (totalSpending.compareTo(BigDecimal.ZERO) > 0) {
            displaySpending = totalSpending.abs();
        } else {
            displaySpending = BigDecimal.ZERO;
        }
        
        budgetCategory.setSpentAmount(displaySpending);
        budgetCategoryRepository.save(budgetCategory);
        
        log.debug("Updated spending for budget {} category {}: {}", 
            budgetId, categoryId, displaySpending);
    }


    @Builder
    @Getter
    public static class BudgetSummary {
        private UUID budgetId;
        private String budgetName;
        private LocalDate startDate;
        private LocalDate endDate;
        private BudgetStatus status;
        private BigDecimal totalBudgeted;
        private BigDecimal totalSpent;
        private BigDecimal remainingAmount;
        private List<CategorySummary> categorySummaries;
    }

    @Builder
    @Getter
    public static class CategorySummary {
        private UUID categoryId;
        private String categoryName;
        private BigDecimal limitAmount;
        private BigDecimal spentAmount;
        private BigDecimal progressPercentage;
    }
} 