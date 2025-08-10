package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.repository.GoalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledGoalEvaluationService {

    private final GoalRepository goalRepository;
    private final GoalManagementService goalManagementService;


    @Scheduled(cron = "0 0 9 * * ?")
    public void evaluateAllGoalsDaily() {
        log.info("Starting daily goal evaluation...");
        

        List<UUID> userIds = goalRepository.findAll().stream()
                .map(goal -> goal.getUser().getId())
                .distinct()
                .toList();

        for (UUID userId : userIds) {
            try {
                goalManagementService.evaluateGoalsForUser(userId);
                log.debug("Evaluated goals for user: {}", userId);
            } catch (Exception e) {
                log.error("Failed to evaluate goals for user: {}", userId, e);
            }
        }

        log.info("Completed daily goal evaluation for {} users", userIds.size());
    }


    @Scheduled(cron = "0 0 8 ? * SUN")
    public void weeklyGoalProgressReport() {
        log.info("Starting weekly goal progress report...");
        

        evaluateAllGoalsDaily();
        
        log.info("Completed weekly goal progress report");
    }
} 