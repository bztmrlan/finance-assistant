package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.dto.GoalRequest;
import com.github.bztmrlan.financeassistant.dto.GoalResponse;
import com.github.bztmrlan.financeassistant.enums.SourceType;
import com.github.bztmrlan.financeassistant.model.Alert;
import com.github.bztmrlan.financeassistant.model.Category;
import com.github.bztmrlan.financeassistant.model.Goal;
import com.github.bztmrlan.financeassistant.model.User;
import com.github.bztmrlan.financeassistant.repository.AlertRepository;
import com.github.bztmrlan.financeassistant.repository.CategoryRepository;
import com.github.bztmrlan.financeassistant.repository.GoalRepository;
import com.github.bztmrlan.financeassistant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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

    @Transactional
    public GoalResponse createGoal(UUID userId, GoalRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
        }

        Goal goal = Goal.builder()
                .user(user)
                .category(category)
                .name(request.getName())
                .targetAmount(request.getTargetAmount())
                .targetDate(request.getTargetDate())
                .currentAmount(BigDecimal.ZERO)
                .completed(false)
                .build();

        Goal savedGoal = goalRepository.save(goal);
        log.info("Created goal: {} for user: {}", savedGoal.getName(), userId);

        return mapToGoalResponse(savedGoal);
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> getUserGoals(UUID userId) {
        List<Goal> goals = goalRepository.findByUserId(userId);
        return goals.stream()
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
        log.info("Deleted goal: {} for user: {}", goalId, userId);
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
        log.info("Updated goal: {} for user: {}", goalId, userId);

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
        log.info("Created goal completion alert for goal: {}", goal.getId());
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
        log.info("Created goal risk alert for goal: {}", goal.getId());
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
} 