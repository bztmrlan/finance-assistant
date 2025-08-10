package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.model.Goal;
import com.github.bztmrlan.financeassistant.model.Transaction;
import com.github.bztmrlan.financeassistant.repository.GoalRepository;
import com.github.bztmrlan.financeassistant.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoalTransactionIntegrationService {

    private final GoalRepository goalRepository;
    private final TransactionRepository transactionRepository;
    private final GoalManagementService goalManagementService;

    @Transactional
    public void processTransactionForGoals(Transaction transaction) {
        if (transaction.getCategory() == null) {
            return;
        }

        List<Goal> userGoals = goalRepository.findByUserId(transaction.getUser().getId());
        
        for (Goal goal : userGoals) {
            if (shouldLinkTransactionToGoal(transaction, goal)) {
                updateGoalProgress(goal, transaction);
            }
        }
    }

    private boolean shouldLinkTransactionToGoal(Transaction transaction, Goal goal) {
        return !goal.isCompleted() &&
               goal.getCategory() != null &&
               goal.getCategory().getId().equals(transaction.getCategory().getId()) &&
               transaction.getAmount().compareTo(BigDecimal.ZERO) > 0 &&
               goal.getCurrency().equals(transaction.getCurrency());
    }

    private void updateGoalProgress(Goal goal, Transaction transaction) {
        try {
            goalManagementService.updateGoalProgress(goal.getId(), goal.getUser().getId(), transaction.getAmount());
            log.info("Updated goal progress for goal: {} with transaction: {}", goal.getId(), transaction.getId());
        } catch (Exception e) {
            log.error("Failed to update goal progress for goal: {} with transaction: {}", goal.getId(), transaction.getId(), e);
        }
    }

    @Transactional
    public void syncAllTransactionsWithGoals(UUID userId) {
        List<Transaction> transactions = transactionRepository.findByUserId(userId);
        List<Goal> goals = goalRepository.findByUserId(userId);

        for (Goal goal : goals) {
            goal.setCurrentAmount(BigDecimal.ZERO);
            goal.setCompleted(false);
        }
        goalRepository.saveAll(goals);

        for (Transaction transaction : transactions) {
            if (transaction.getCategory() != null) {
                for (Goal goal : goals) {
                    if (shouldLinkTransactionToGoal(transaction, goal)) {
                        goal.updateProgress(transaction.getAmount());
                    }
                }
            }
        }

        goalRepository.saveAll(goals);
        log.info("Synced {} transactions with {} goals for user: {}", transactions.size(), goals.size(), userId);
    }

    @Transactional
    public void syncTransactionWithGoals(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        processTransactionForGoals(transaction);
    }
} 