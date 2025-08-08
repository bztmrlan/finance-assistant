package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.enums.SourceType;
import com.github.bztmrlan.financeassistant.model.Alert;
import com.github.bztmrlan.financeassistant.model.Rule;
import com.github.bztmrlan.financeassistant.model.Transaction;
import com.github.bztmrlan.financeassistant.repository.AlertRepository;
import com.github.bztmrlan.financeassistant.repository.RuleRepository;
import com.github.bztmrlan.financeassistant.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleEngineService {

    private final RuleRepository ruleRepository;
    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;

    /**
     * Evaluates all active rules for a user and creates alerts for violations
     * @param userId The user ID to evaluate rules for
     */
    public void evaluateRulesForUser(UUID userId) {
        log.info("Evaluating rules for user: {}", userId);
        
        List<Rule> activeRules = ruleRepository.findByUserIdAndActiveTrue(userId);
        
        for (Rule rule : activeRules) {
            evaluateRule(rule);
        }
    }

    /**
     * Evaluates a specific rule and creates an alert if violated
     * @param rule The rule to evaluate
     */
    public void evaluateRule(Rule rule) {
        log.debug("Evaluating rule: {}", rule.getName());
        

        List<Transaction> transactions = getTransactionsForRule(rule);
        

        BigDecimal totalAmount = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        

        if (isRuleViolated(rule, totalAmount)) {
            createAlert(rule, totalAmount);
        }
    }

    /**
     * Evaluates rules when a new transaction is added
     * @param transaction The new transaction
     */
    public void evaluateRulesForTransaction(Transaction transaction) {
        log.debug("Evaluating rules for new transaction: {}", transaction.getId());
        
        List<Rule> relevantRules = ruleRepository.findByUserIdAndActiveTrueAndCategoryIdOrUserIdAndActiveTrueAndCategoryIdIsNull(
                transaction.getUser().getId(), 
                transaction.getCategory() != null ? transaction.getCategory().getId() : null
        );
        
        for (Rule rule : relevantRules) {
            evaluateRule(rule);
        }
    }

    private List<Transaction> getTransactionsForRule(Rule rule) {
        LocalDate startDate = getStartDateForPeriod(rule.getPeriod());
        LocalDate endDate = LocalDate.now();
        
        if (rule.getCategory() != null) {
            return transactionRepository.findByUserIdAndCategoryIdAndDateBetween(
                    rule.getUser().getId(),
                    rule.getCategory().getId(),
                    startDate,
                    endDate
            );
        } else {
            return transactionRepository.findByUserIdAndDateBetween(
                    rule.getUser().getId(),
                    startDate,
                    endDate
            );
        }
    }

    private LocalDate getStartDateForPeriod(com.github.bztmrlan.financeassistant.enums.TimePeriod period) {
        LocalDate now = LocalDate.now();
        
        return switch (period) {
            case DAILY -> now;
            case WEEKLY -> now.minusWeeks(1);
            case MONTHLY -> now.minusMonths(1);
            case QUARTERLY -> now.minusMonths(3);
            case YEARLY -> now.minusYears(1);
        };
    }

    private boolean isRuleViolated(Rule rule, BigDecimal totalAmount) {
        return switch (rule.getConditionType()) {
            case GREATER_THAN -> totalAmount.compareTo(rule.getThreshold()) > 0;
            case LESS_THAN -> totalAmount.compareTo(rule.getThreshold()) < 0;
            case EQUAL_TO -> totalAmount.compareTo(rule.getThreshold()) == 0;
        };
    }

    private void createAlert(Rule rule, BigDecimal totalAmount) {
        String message = generateAlertMessage(rule, totalAmount);
        
        Alert alert = Alert.builder()
                .user(rule.getUser())
                .sourceType(SourceType.RULE)
                .sourceId(rule.getId())
                .message(message)
                .read(false)
                .createdAt(Instant.now())
                .build();
        
        alertRepository.save(alert);
        log.info("Alert created for rule violation: {}", rule.getName());
    }

    private String generateAlertMessage(Rule rule, BigDecimal totalAmount) {
        String categoryName = rule.getCategory() != null ? rule.getCategory().getName() : "all categories";
        String conditionText = switch (rule.getConditionType()) {
            case GREATER_THAN -> "exceeded";
            case LESS_THAN -> "fell below";
            case EQUAL_TO -> "reached exactly";
        };
        
        return String.format(
                "Rule '%s' violated: Spending in %s %s the threshold of %s (Current: %s)",
                rule.getName(),
                categoryName,
                conditionText,
                rule.getThreshold(),
                totalAmount
        );
    }

    /**
     * Uses Easy Rules engine for more complex rule evaluation
     * This method can be extended for more sophisticated rule logic
     */
    public void evaluateRulesWithEasyRules(UUID userId) {
        // For now, just delegate to the simple evaluation
        // TODO: Implement Easy Rules integration
        evaluateRulesForUser(userId);
    }
} 