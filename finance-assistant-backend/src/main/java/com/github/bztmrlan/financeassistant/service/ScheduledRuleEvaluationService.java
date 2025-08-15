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
    private final BudgetEvaluationService budgetEvaluationService;
    private final UserRepository userRepository;


    @Scheduled(cron = "0 0 6 * * ?")
    public void evaluateRulesAndBudgetsDaily() {
        log.info("Starting daily rule and budget evaluation for all users");
        
        List<User> users = userRepository.findAll();
        
        for (User user : users) {
            try {

                ruleEngineService.evaluateRulesForUser(user.getId());
                log.debug("Daily rule evaluation completed for user: {}", user.getId());
                

                budgetEvaluationService.evaluateUserBudgets(user.getId());
                log.debug("Daily budget evaluation completed for user: {}", user.getId());
                
            } catch (Exception e) {
                log.error("Error during daily evaluation for user: {}", user.getId(), e);
            }
        }
        
        log.info("Daily rule and budget evaluation completed for {} users", users.size());
    }


    @Scheduled(cron = "0 0 8 ? * SUN")
    public void evaluateRulesAndBudgetsWeekly() {
        log.info("Starting weekly rule and budget evaluation for all users");
        
        List<User> users = userRepository.findAll();
        
        for (User user : users) {
            try {

                ruleEngineService.evaluateRulesForUser(user.getId());
                log.debug("Weekly rule evaluation completed for user: {}", user.getId());
                

                budgetEvaluationService.evaluateUserBudgets(user.getId());
                log.debug("Weekly budget evaluation completed for user: {}", user.getId());
                
            } catch (Exception e) {
                log.error("Error during weekly evaluation for user: {}", user.getId(), e);
            }
        }
        
        log.info("Weekly rule and budget evaluation completed for {} users", users.size());
    }


    @Scheduled(cron = "0 0 9 1 * ?")
    public void evaluateRulesAndBudgetsMonthly() {
        log.info("Starting monthly rule and budget evaluation for all users");
        
        List<User> users = userRepository.findAll();
        
        for (User user : users) {
            try {

                ruleEngineService.evaluateRulesForUser(user.getId());
                log.debug("Monthly rule evaluation completed for user: {}", user.getId());
                

                budgetEvaluationService.evaluateUserBudgets(user.getId());
                log.debug("Monthly budget evaluation completed for user: {}", user.getId());
                
            } catch (Exception e) {
                log.error("Error during monthly evaluation for user: {}", user.getId(), e);
            }
        }
        
        log.info("Monthly rule and budget evaluation completed for {} users", users.size());
    }


    @Scheduled(cron = "0 0 14 * * ?")
    public void evaluateBudgetsApproachingEnd() {
        log.info("Starting evaluation of budgets approaching end date");
        
        try {
            budgetEvaluationService.evaluateBudgetsApproachingEnd(7);
            log.info("Evaluation of budgets approaching end date completed");
        } catch (Exception e) {
            log.error("Error during evaluation of budgets approaching end date", e);
        }
    }
}