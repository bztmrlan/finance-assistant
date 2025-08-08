package com.github.bztmrlan.financeassistant.service;

import com.github.bztmrlan.financeassistant.model.User;
import com.github.bztmrlan.financeassistant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledRuleEvaluationService {

    private final RuleEngineService ruleEngineService;
    private final UserRepository userRepository;

    /**
     * Evaluate rules for all users daily at 6:00 AM
     */
    @Scheduled(cron = "0 0 6 * * ?")
    public void evaluateRulesDaily() {
        log.info("Starting daily rule evaluation for all users");
        
        List<User> users = userRepository.findAll();
        
        for (User user : users) {
            try {
                ruleEngineService.evaluateRulesForUser(user.getId());
                log.debug("Daily rule evaluation completed for user: {}", user.getId());
            } catch (Exception e) {
                log.error("Error during daily rule evaluation for user: {}", user.getId(), e);
            }
        }
        
        log.info("Daily rule evaluation completed for {} users", users.size());
    }

    /**
     * Evaluate rules for all users weekly on Sunday at 8:00 AM
     */
    @Scheduled(cron = "0 0 8 ? * SUN")
    public void evaluateRulesWeekly() {
        log.info("Starting weekly rule evaluation for all users");
        
        List<User> users = userRepository.findAll();
        
        for (User user : users) {
            try {
                ruleEngineService.evaluateRulesForUser(user.getId());
                log.debug("Weekly rule evaluation completed for user: {}", user.getId());
            } catch (Exception e) {
                log.error("Error during weekly rule evaluation for user: {}", user.getId(), e);
            }
        }
        
        log.info("Weekly rule evaluation completed for {} users", users.size());
    }

    /**
     * Evaluate rules for all users monthly on the 1st at 9:00 AM
     */
    @Scheduled(cron = "0 0 9 1 * ?")
    public void evaluateRulesMonthly() {
        log.info("Starting monthly rule evaluation for all users");
        
        List<User> users = userRepository.findAll();
        
        for (User user : users) {
            try {
                ruleEngineService.evaluateRulesForUser(user.getId());
                log.debug("Monthly rule evaluation completed for user: {}", user.getId());
            } catch (Exception e) {
                log.error("Error during monthly rule evaluation for user: {}", user.getId(), e);
            }
        }
        
        log.info("Monthly rule evaluation completed for {} users", users.size());
    }
} 