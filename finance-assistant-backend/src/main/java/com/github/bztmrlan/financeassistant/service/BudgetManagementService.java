package com.github.bztmrlan.financeassistant.service;

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

    /**
     * Create a new budget with category limits
     */
    @Transactional
    public Budget createBudget(Budget budget, List<BudgetCategory> categoryLimits) {

        budget.setStatus(BudgetStatus.ACTIVE);

        Budget savedBudget = budgetRepository.save(budget);
        

        categoryLimits.forEach(bc -> bc.setBudget(savedBudget));
        

        budgetCategoryRepository.saveAll(categoryLimits);
        
        log.info("Created budget {} for user {}", savedBudget.getId(), savedBudget.getUser().getId());
        return savedBudget;
    }

    /**
     * Update category spending limits for a budget
     */
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

    /**
     * Add a new category limit to an existing budget
     */
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

    /**
     * Calculate current spending for a budget category within the budget period
     */
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

    /**
     * Update spent amounts for all categories in a budget
     */
    @Transactional
    public void updateBudgetSpending(UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
        
        List<BudgetCategory> categories = budget.getCategories();
        
        for (BudgetCategory budgetCategory : categories) {
            BigDecimal spentAmount = calculateCategorySpending(budgetId, budgetCategory.getCategory().getId());
            budgetCategory.setSpentAmount(spentAmount);
            budgetCategoryRepository.save(budgetCategory);
        }
        
        log.debug("Updated spending amounts for budget {}", budgetId);
    }

    /**
     * Check if any category limits are exceeded and create alerts
     */
    @Transactional
    public void checkBudgetLimitsAndCreateAlerts(UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
        

        updateBudgetSpending(budgetId);
        
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

    /**
     * Create an alert for exceeded budget category
     */
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

    /**
     * Get budget summary with spending details
     */
    public BudgetSummary getBudgetSummary(UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
        

        updateBudgetSpending(budgetId);
        
        List<BudgetCategory> categories = budget.getCategories();
        
        BigDecimal totalBudgeted = categories.stream()
                .map(BudgetCategory::getLimitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalSpent = categories.stream()
                .map(BudgetCategory::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return BudgetSummary.builder()
                .budgetId(budgetId)
                .budgetName(budget.getName())
                .startDate(budget.getStartDate())
                .endDate(budget.getEndDate())
                .status(budget.getStatus())
                .totalBudgeted(totalBudgeted)
                .totalSpent(totalSpent)
                .remainingAmount(totalBudgeted.subtract(totalSpent))
                .categories(categories)
                .build();
    }

    /**
     * Get all budgets for a user
     */
    public List<Budget> getUserBudgets(UUID userId) {
        return budgetRepository.findByUserId(userId);
    }

    /**
     * Get active budgets for a user
     */
    public List<Budget> getActiveUserBudgets(UUID userId) {
        return budgetRepository.findByUserId(userId).stream()
                .filter(budget -> budget.getStatus() == BudgetStatus.ACTIVE)
                .toList();
    }

    /**
     * Archive a budget (set status to ARCHIVED)
     */
    @Transactional
    public void archiveBudget(UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
        
        budget.setStatus(BudgetStatus.COMPLETED);
        budgetRepository.save(budget);
        
        log.info("Archived budget {}", budgetId);
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
        private List<BudgetCategory> categories;
    }
} 