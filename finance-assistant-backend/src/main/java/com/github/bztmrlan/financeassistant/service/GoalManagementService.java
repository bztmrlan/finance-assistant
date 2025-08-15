package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.dto.GoalRequest;
import com.github.bztmrlan.financeassistant.dto.GoalResponse;
import com.github.bztmrlan.financeassistant.enums.SourceType;
import com.github.bztmrlan.financeassistant.model.Alert;
import com.github.bztmrlan.financeassistant.model.Category;
import com.github.bztmrlan.financeassistant.model.Goal;
import com.github.bztmrlan.financeassistant.model.Transaction;
import com.github.bztmrlan.financeassistant.model.User;
import com.github.bztmrlan.financeassistant.repository.AlertRepository;
import com.github.bztmrlan.financeassistant.repository.CategoryRepository;
import com.github.bztmrlan.financeassistant.repository.GoalRepository;
import com.github.bztmrlan.financeassistant.repository.TransactionRepository;
import com.github.bztmrlan.financeassistant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoalManagementService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AlertRepository alertRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public GoalResponse createGoal(UUID userId, GoalRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
                    .orElseThrow(() -> new RuntimeException("Category not found or does not belong to user"));
        }

        Goal goal = Goal.builder()
                .user(user)
                .category(category)
                .name(request.getName())
                .targetAmount(request.getTargetAmount())
                .currentAmount(BigDecimal.ZERO)
                .targetDate(request.getTargetDate())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .completed(false)
                .build();

        Goal savedGoal = goalRepository.save(goal);
        return mapToGoalResponse(savedGoal);
    }

    @Transactional
    public List<GoalResponse> getUserGoals(UUID userId) {
        List<Goal> goals = goalRepository.findByUserId(userId);

        

        List<Goal> updatedGoals = new ArrayList<>();
        for (Goal goal : goals) {
            try {
                Goal updatedGoal = updateGoalProgressFromTransactions(goal.getId());
                updatedGoals.add(updatedGoal);
            } catch (Exception e) {
                log.warn("Failed to update progress for goal {}: {}", goal.getId(), e.getMessage());
                updatedGoals.add(goal);
            }
        }

        return updatedGoals.stream()
                .map(this::mapToGoalResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<GoalResponse> getGoalById(UUID goalId, UUID userId) {
        return goalRepository.findById(goalId)
                .filter(goal -> goal.getUser().getId().equals(userId))
                .map(this::mapToGoalResponse);
    }

    @Transactional
    public GoalResponse updateGoalProgress(UUID goalId, UUID userId, BigDecimal amount) {
        Goal goal = goalRepository.findById(goalId)
                .filter(g -> g.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        goal.updateProgress(amount);
        Goal updatedGoal = goalRepository.save(goal);

        if (updatedGoal.isCompleted()) {
            createGoalCompletedAlert(updatedGoal);
        }

        checkGoalRisk(updatedGoal);

        log.info("Updated goal progress: {} for goal: {}", amount, goalId);
        return mapToGoalResponse(updatedGoal);
    }

    @Transactional
    public void deleteGoal(UUID goalId, UUID userId) {
        Goal goal = goalRepository.findById(goalId)
                .filter(g -> g.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        goalRepository.delete(goal);
    }

    @Transactional
    public GoalResponse updateGoal(UUID goalId, UUID userId, GoalRequest request) {
        Goal goal = goalRepository.findById(goalId)
                .filter(g -> g.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
        }

        goal.setName(request.getName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setTargetDate(request.getTargetDate());
        goal.setCategory(category);

        Goal updatedGoal = goalRepository.save(goal);

        return mapToGoalResponse(updatedGoal);
    }

    private void createGoalCompletedAlert(Goal goal) {
        Alert alert = Alert.builder()
                .user(goal.getUser())
                .sourceType(SourceType.GOAL)
                .sourceId(goal.getId())
                .message("Congratulations! You've achieved your goal: " + goal.getName())
                .read(false)
                .createdAt(java.time.Instant.now())
                .build();

        alertRepository.save(alert);
    }

    private void checkGoalRisk(Goal goal) {
        LocalDate now = LocalDate.now();
        long daysRemaining = ChronoUnit.DAYS.between(now, goal.getTargetDate());
        
        if (daysRemaining <= 30 && daysRemaining > 0) {
            BigDecimal progressPercentage = goal.getCurrentAmount()
                    .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            if (progressPercentage.compareTo(BigDecimal.valueOf(25)) < 0) {
                createGoalRiskAlert(goal, daysRemaining, progressPercentage);
            }
        }
    }

    private void createGoalRiskAlert(Goal goal, long daysRemaining, BigDecimal progressPercentage) {
        String message = String.format(
                "Warning: Your goal '%s' is at risk. You have %d days remaining and only %.1f%% progress. " +
                "You need to save $%.2f more to reach your target.",
                goal.getName(),
                daysRemaining,
                progressPercentage,
                goal.getTargetAmount().subtract(goal.getCurrentAmount())
        );

        Alert alert = Alert.builder()
                .user(goal.getUser())
                .sourceType(SourceType.GOAL)
                .sourceId(goal.getId())
                .message(message)
                .read(false)
                .createdAt(java.time.Instant.now())
                .build();

        alertRepository.save(alert);
    }

    private GoalResponse mapToGoalResponse(Goal goal) {
        LocalDate now = LocalDate.now();
        long daysRemaining = ChronoUnit.DAYS.between(now, goal.getTargetDate());
        
        BigDecimal progressPercentage = BigDecimal.ZERO;
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            progressPercentage = goal.getCurrentAmount()
                    .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return GoalResponse.builder()
                .id(goal.getId())
                .name(goal.getName())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .progressPercentage(progressPercentage)
                .targetDate(goal.getTargetDate())
                .completed(goal.isCompleted())
                .categoryName(goal.getCategory() != null ? goal.getCategory().getName() : null)
                .currency("USD")
                .daysRemaining(daysRemaining)
                .build();
    }

    @Transactional
    public void evaluateGoalsForUser(UUID userId) {
        List<Goal> goals = goalRepository.findByUserId(userId);
        goals.forEach(this::checkGoalRisk);
    }
    

    @Transactional
    public void calculateGoalProgressFromTransactions(UUID userId) {
        List<Goal> goals = goalRepository.findByUserId(userId);
        
        for (Goal goal : goals) {
            try {
                updateGoalProgressFromTransactions(goal.getId());
            } catch (Exception e) {
                log.error("Error calculating progress for goal {}: {}", goal.getId(), e.getMessage());
            }
        }

    }
    

    @Transactional
    public Goal updateGoalProgressFromTransactions(UUID goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        
        BigDecimal calculatedProgress = calculateProgressFromTransactions(goal);
        

        if (calculatedProgress.compareTo(goal.getCurrentAmount()) != 0) {
            goal.setCurrentAmount(calculatedProgress);

            if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
                goal.setCompleted(true);
                createGoalCompletedAlert(goal);
            }

            goal = goalRepository.save(goal);
            checkGoalRisk(goal);
        }

        return goal;
    }
    

    private BigDecimal calculateProgressFromTransactions(Goal goal) {

        LocalDate startDate = LocalDate.now().minusYears(1);
        LocalDate endDate = goal.getTargetDate();
        
        BigDecimal totalIncome = BigDecimal.ZERO;
        
        if (goal.getCategory() != null) {

            List<Transaction> transactions = transactionRepository
                .findByUserIdAndCategoryIdAndDateBetween(
                    goal.getUser().getId(),
                    goal.getCategory().getId(),
                    startDate,
                    endDate
                );
            

            totalIncome = transactions.stream()
                .map(Transaction::getAmount)
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
            log.debug("Goal {} with category {}: Found {} transactions, total income: ${}", 
                goal.getName(), goal.getCategory().getName(), transactions.size(), totalIncome);
        } else {

            List<Transaction> allTransactions = transactionRepository
                .findByUserIdAndDateBetween(
                    goal.getUser().getId(),
                    startDate,
                    endDate
                );

            totalIncome = allTransactions.stream()
                .map(Transaction::getAmount)
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
            log.debug("Goal {} without category: Found {} transactions, total income: ${}", 
                goal.getName(), allTransactions.size(), totalIncome);
        }
        
        return totalIncome;
    }
} 