package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.model.Budget;
import com.github.bztmrlan.financeassistant.repository.BudgetRepository;
import com.github.bztmrlan.financeassistant.enums.BudgetStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetEvaluationService {

    private final BudgetRepository budgetRepository;
    private final BudgetManagementService budgetManagementService;

    /**
     * Evaluate all active budgets for a specific user
     */
    public void evaluateUserBudgets(UUID userId) {
        log.info("Starting budget evaluation for user: {}", userId);
        
        List<Budget> activeBudgets = budgetRepository.findByUserId(userId).stream()
                .filter(budget -> budget.getStatus() == BudgetStatus.ACTIVE)
                .toList();
        
        for (Budget budget : activeBudgets) {
            try {
                evaluateBudget(budget.getId());
                log.debug("Budget evaluation completed for budget: {}", budget.getId());
            } catch (Exception e) {
                log.error("Error during budget evaluation for budget: {}", budget.getId(), e);
            }
        }
        
        log.info("Budget evaluation completed for user: {}. {} budgets evaluated.", userId, activeBudgets.size());
    }

    /**
     * Evaluate all active budgets for all users
     */
    public void evaluateAllActiveBudgets() {
        log.info("Starting evaluation of all active budgets");
        
        List<Budget> activeBudgets = budgetRepository.findAll().stream()
                .filter(budget -> budget.getStatus() == BudgetStatus.ACTIVE)
                .toList();
        
        for (Budget budget : activeBudgets) {
            try {
                evaluateBudget(budget.getId());
                log.debug("Budget evaluation completed for budget: {}", budget.getId());
            } catch (Exception e) {
                log.error("Error during budget evaluation for budget: {}", budget.getId(), e);
            }
        }
        
        log.info("All active budgets evaluation completed. {} budgets evaluated.", activeBudgets.size());
    }

    /**
     * Evaluate a specific budget
     */
    public void evaluateBudget(UUID budgetId) {
        log.debug("Evaluating budget: {}", budgetId);
        
        try {

            budgetManagementService.checkBudgetLimitsAndCreateAlerts(budgetId);
            

            checkBudgetPeriodEnd(budgetId);
            
        } catch (Exception e) {
            log.error("Error evaluating budget: {}", budgetId, e);
            throw e;
        }
    }

    /**
     * Check if budget period has ended and update status accordingly
     */
    private void checkBudgetPeriodEnd(UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
        
        LocalDate today = LocalDate.now();
        
        if (budget.getEndDate().isBefore(today) && budget.getStatus() == BudgetStatus.ACTIVE) {

            budget.setStatus(BudgetStatus.COMPLETED);
            budgetRepository.save(budget);
            
            log.info("Budget {} period ended. Status updated to COMPLETED.", budgetId);
            

            createBudgetCompletionSummary(budgetId);
        }
    }

    /**
     * Create a summary when a budget period ends
     */
    private void createBudgetCompletionSummary(UUID budgetId) {
        try {
            var summary = budgetManagementService.getBudgetSummary(budgetId);
            
            log.info("Budget {} completion summary - Total Budgeted: ${}, Total Spent: ${}, Remaining: ${}",
                    budgetId,
                    summary.getTotalBudgeted(),
                    summary.getTotalSpent(),
                    summary.getRemainingAmount());
            
        } catch (Exception e) {
            log.error("Error creating budget completion summary for budget: {}", budgetId, e);
        }
    }

    /**
     * Evaluate budgets that are approaching their end date (within specified days)
     */
    public void evaluateBudgetsApproachingEnd(int daysThreshold) {
        log.info("Evaluating budgets approaching end date (within {} days)", daysThreshold);
        
        LocalDate thresholdDate = LocalDate.now().plusDays(daysThreshold);
        
        List<Budget> approachingEndBudgets = budgetRepository.findAll().stream()
                .filter(budget -> budget.getStatus() == BudgetStatus.ACTIVE)
                .filter(budget -> budget.getEndDate().isBefore(thresholdDate) && 
                                budget.getEndDate().isAfter(LocalDate.now()))
                .toList();
        
        for (Budget budget : approachingEndBudgets) {
            try {
                log.info("Budget {} is approaching end date: {}", budget.getId(), budget.getEndDate());
                

                evaluateBudget(budget.getId());
                

                checkBudgetWarningThreshold(budget.getId());
                
            } catch (Exception e) {
                log.error("Error evaluating budget approaching end: {}", budget.getId(), e);
            }
        }
        
        log.info("Evaluation of budgets approaching end date completed. {} budgets evaluated.", 
                approachingEndBudgets.size());
    }

    /**
     * Check if budget is approaching warning threshold (e.g., 80% of limit)
     */
    private void checkBudgetWarningThreshold(UUID budgetId) {
        try {
            var summary = budgetManagementService.getBudgetSummary(budgetId);
            

            summary.getCategories().forEach(budgetCategory -> {
                if (budgetCategory.getLimitAmount().compareTo(BigDecimal.ZERO) > 0) {
                    double percentageUsed = budgetCategory.getSpentAmount()
                            .divide(budgetCategory.getLimitAmount(), 4, java.math.RoundingMode.HALF_UP)
                            .doubleValue();
                    
                    if (percentageUsed >= 0.8 && percentageUsed < 1.0) {
                        log.info("Budget category {} is approaching limit: {:.1f}% used", 
                                budgetCategory.getCategory().getName(), percentageUsed * 100);
                    }
                }
            });
            
        } catch (Exception e) {
            log.error("Error checking budget warning threshold for budget: {}", budgetId, e);
        }
    }

    /**
     * Get budgets that need immediate attention (exceeded limits or approaching end)
     */
    public List<Budget> getBudgetsNeedingAttention(UUID userId) {
        List<Budget> activeBudgets = budgetRepository.findByUserId(userId).stream()
                .filter(budget -> budget.getStatus() == BudgetStatus.ACTIVE)
                .toList();
        
        return activeBudgets.stream()
                .filter(budget -> {
                    try {
                        var summary = budgetManagementService.getBudgetSummary(budget.getId());
                        

                        boolean needsAttention = summary.getCategories().stream()
                                .anyMatch(bc -> {
                                    if (bc.getLimitAmount().compareTo(BigDecimal.ZERO) > 0) {
                                        double percentageUsed = bc.getSpentAmount()
                                                .divide(bc.getLimitAmount(), 4, java.math.RoundingMode.HALF_UP)
                                                .doubleValue();
                                        return percentageUsed >= 0.8;
                                    }
                                    return false;
                                });
                        

                        boolean periodEndingSoon = budget.getEndDate().isBefore(LocalDate.now().plusDays(7));
                        
                        return needsAttention || periodEndingSoon;
                        
                    } catch (Exception e) {
                        log.error("Error checking if budget needs attention: {}", budget.getId(), e);
                        return false;
                    }
                })
                .toList();
    }
} 