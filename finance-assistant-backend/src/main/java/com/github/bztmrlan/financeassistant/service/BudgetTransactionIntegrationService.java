package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.model.Budget;
import com.github.bztmrlan.financeassistant.model.BudgetCategory;
import com.github.bztmrlan.financeassistant.model.Transaction;
import com.github.bztmrlan.financeassistant.repository.BudgetRepository;
import com.github.bztmrlan.financeassistant.repository.BudgetCategoryRepository;
import com.github.bztmrlan.financeassistant.enums.BudgetStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetTransactionIntegrationService {

    private final BudgetRepository budgetRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final BudgetManagementService budgetManagementService;

    /**
     * Update budget spending when a new transaction is added
     * This method should be called after a transaction is successfully saved
     */
    @Transactional
    public void updateBudgetSpendingForTransaction(Transaction transaction) {
        log.debug("Updating budget spending for transaction: {}", transaction.getId());
        
        UUID userId = transaction.getUser().getId();
        UUID categoryId = transaction.getCategory().getId();
        LocalDate transactionDate = transaction.getDate();
        

        List<Budget> relevantBudgets = budgetRepository.findByUserId(userId).stream()
                .filter(budget -> budget.getStatus() == BudgetStatus.ACTIVE)
                .filter(budget -> !transactionDate.isBefore(budget.getStartDate()) && 
                                !transactionDate.isAfter(budget.getEndDate()))
                .toList();
        
        for (Budget budget : relevantBudgets) {
            try {
                updateBudgetCategorySpending(budget.getId(), categoryId, transaction.getAmount());
                log.debug("Updated budget {} category {} spending", budget.getId(), categoryId);
            } catch (Exception e) {
                log.error("Error updating budget spending for budget: {}", budget.getId(), e);
            }
        }
    }

    /**
     * Update budget spending when a transaction is modified
     * This method should be called after a transaction is updated
     */
    @Transactional
    public void updateBudgetSpendingForModifiedTransaction(Transaction oldTransaction, Transaction newTransaction) {
        log.debug("Updating budget spending for modified transaction: {}", newTransaction.getId());
        
        UUID userId = newTransaction.getUser().getId();
        UUID oldCategoryId = oldTransaction.getCategory().getId();
        UUID newCategoryId = newTransaction.getCategory().getId();
        LocalDate oldDate = oldTransaction.getDate();
        LocalDate newDate = newTransaction.getDate();
        

        List<Budget> relevantBudgets = budgetRepository.findByUserId(userId).stream()
                .filter(budget -> budget.getStatus() == BudgetStatus.ACTIVE)
                .toList();
        
        for (Budget budget : relevantBudgets) {
            try {

                if (isDateInBudgetPeriod(budget, oldDate)) {
                    updateBudgetCategorySpending(budget.getId(), oldCategoryId, oldTransaction.getAmount().negate());
                }
                

                if (isDateInBudgetPeriod(budget, newDate)) {
                    updateBudgetCategorySpending(budget.getId(), newCategoryId, newTransaction.getAmount());
                }
                
                log.debug("Updated budget {} for modified transaction", budget.getId());
                
            } catch (Exception e) {
                log.error("Error updating budget spending for modified transaction in budget: {}", budget.getId(), e);
            }
        }
    }

    /**
     * Update budget spending when a transaction is deleted
     * This method should be called after a transaction is deleted
     */
    @Transactional
    public void updateBudgetSpendingForDeletedTransaction(Transaction deletedTransaction) {
        log.debug("Updating budget spending for deleted transaction: {}", deletedTransaction.getId());
        
        UUID userId = deletedTransaction.getUser().getId();
        UUID categoryId = deletedTransaction.getCategory().getId();
        LocalDate transactionDate = deletedTransaction.getDate();
        

        List<Budget> relevantBudgets = budgetRepository.findByUserId(userId).stream()
                .filter(budget -> budget.getStatus() == BudgetStatus.ACTIVE)
                .filter(budget -> !transactionDate.isBefore(budget.getStartDate()) && 
                                !transactionDate.isAfter(budget.getEndDate()))
                .toList();
        
        for (Budget budget : relevantBudgets) {
            try {

                updateBudgetCategorySpending(budget.getId(), categoryId, deletedTransaction.getAmount().negate());
                log.debug("Updated budget {} category {} spending for deleted transaction", budget.getId(), categoryId);
            } catch (Exception e) {
                log.error("Error updating budget spending for deleted transaction in budget: {}", budget.getId(), e);
            }
        }
    }

    /**
     * Update spending for a specific budget category
     */
    private void updateBudgetCategorySpending(UUID budgetId, UUID categoryId, java.math.BigDecimal amountChange) {

        List<BudgetCategory> budgetCategories = budgetCategoryRepository.findAll().stream()
                .filter(bc -> bc.getBudget().getId().equals(budgetId) && 
                             bc.getCategory().getId().equals(categoryId))
                .toList();
        
        if (!budgetCategories.isEmpty()) {
            BudgetCategory budgetCategory = budgetCategories.get(0);
            java.math.BigDecimal newSpentAmount = budgetCategory.getSpentAmount().add(amountChange);
            

            if (newSpentAmount.compareTo(java.math.BigDecimal.ZERO) < 0) {
                newSpentAmount = java.math.BigDecimal.ZERO;
                log.warn("Budget category spending would go below zero, setting to zero. Budget: {}, Category: {}", 
                        budgetId, categoryId);
            }
            
            budgetCategory.setSpentAmount(newSpentAmount);
            budgetCategoryRepository.save(budgetCategory);
            
            log.debug("Updated budget category {} spending from {} to {}", 
                    budgetCategory.getId(), 
                    budgetCategory.getSpentAmount().subtract(amountChange), 
                    newSpentAmount);
        }
    }

    /**
     * Check if a date falls within a budget period
     */
    private boolean isDateInBudgetPeriod(Budget budget, LocalDate date) {
        return !date.isBefore(budget.getStartDate()) && !date.isAfter(budget.getEndDate());
    }

    /**
     * Recalculate all budget spending amounts for a specific budget
     * This is useful for data consistency checks or bulk updates
     */
    @Transactional
    public void recalculateBudgetSpending(UUID budgetId) {
        log.info("Recalculating spending for budget: {}", budgetId);
        
        try {
            budgetManagementService.updateBudgetSpending(budgetId);
            log.info("Budget spending recalculation completed for budget: {}", budgetId);
        } catch (Exception e) {
            log.error("Error recalculating budget spending for budget: {}", budgetId, e);
            throw e;
        }
    }

    /**
     * Recalculate all budget spending amounts for a specific user
     * This is useful for data consistency checks or bulk updates
     */
    @Transactional
    public void recalculateAllUserBudgetSpending(UUID userId) {
        log.info("Recalculating spending for all budgets of user: {}", userId);
        
        List<Budget> userBudgets = budgetRepository.findByUserId(userId);
        
        for (Budget budget : userBudgets) {
            try {
                if (budget.getStatus() == BudgetStatus.ACTIVE) {
                    budgetManagementService.updateBudgetSpending(budget.getId());
                    log.debug("Recalculated spending for budget: {}", budget.getId());
                }
            } catch (Exception e) {
                log.error("Error recalculating spending for budget: {}", budget.getId(), e);
            }
        }
        
        log.info("Budget spending recalculation completed for user: {}. {} budgets processed.", 
                userId, userBudgets.size());
    }

    /**
     * Get budget categories that are affected by a transaction
     * This is useful for previewing budget impact before saving a transaction
     */
    public List<BudgetCategory> getAffectedBudgetCategories(Transaction transaction) {
        UUID userId = transaction.getUser().getId();
        UUID categoryId = transaction.getCategory().getId();
        LocalDate transactionDate = transaction.getDate();
        
        return budgetRepository.findByUserId(userId).stream()
                .filter(budget -> budget.getStatus() == BudgetStatus.ACTIVE)
                .filter(budget -> isDateInBudgetPeriod(budget, transactionDate))
                .flatMap(budget -> budget.getCategories().stream())
                .filter(bc -> bc.getCategory().getId().equals(categoryId))
                .toList();
    }

    /**
     * Preview budget impact of a transaction without actually updating the budget
     */
    public BudgetImpactPreview previewTransactionBudgetImpact(Transaction transaction) {
        List<BudgetCategory> affectedCategories = getAffectedBudgetCategories(transaction);
        
        return BudgetImpactPreview.builder()
                .transactionId(transaction.getId())
                .affectedBudgetCategories(affectedCategories)
                .impactAmount(transaction.getAmount())
                .build();
    }


    @Builder
    @Getter
    public static class BudgetImpactPreview {
        private UUID transactionId;
        private List<BudgetCategory> affectedBudgetCategories;
        private java.math.BigDecimal impactAmount;
    }
} 