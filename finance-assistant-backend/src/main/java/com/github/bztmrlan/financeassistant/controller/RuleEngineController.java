package com.github.bztmrlan.financeassistant.controller;

import com.github.bztmrlan.financeassistant.security.CustomUserDetailsService;
import com.github.bztmrlan.financeassistant.service.RuleEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/rule-engine")
@RequiredArgsConstructor
@Slf4j
public class RuleEngineController {

    private final RuleEngineService ruleEngineService;

    /**
     * Manually trigger rule evaluation for the authenticated user
     */
    @PostMapping("/evaluate")
    public ResponseEntity<String> evaluateRules(Authentication authentication) {
        try {

            UUID userId = extractUserIdFromAuthentication(authentication);
            
            ruleEngineService.evaluateRulesForUser(userId);
            
            return ResponseEntity.ok("Rule evaluation completed successfully");
        } catch (Exception e) {
            log.error("Error evaluating rules", e);
            return ResponseEntity.internalServerError()
                    .body("Error evaluating rules: " + e.getMessage());
        }
    }

    /**
     * Evaluate rules using Easy Rules engine
     */
    @PostMapping("/evaluate/easy-rules")
    public ResponseEntity<String> evaluateRulesWithEasyRules(Authentication authentication) {
        try {
            UUID userId = extractUserIdFromAuthentication(authentication);
            
            ruleEngineService.evaluateRulesWithEasyRules(userId);
            
            return ResponseEntity.ok("Easy Rules evaluation completed successfully");
        } catch (Exception e) {
            log.error("Error evaluating rules with Easy Rules", e);
            return ResponseEntity.internalServerError()
                    .body("Error evaluating rules: " + e.getMessage());
        }
    }

    /**
     * Evaluate rules for a specific user (admin endpoint)
     */
    @PostMapping("/evaluate/{userId}")
    public ResponseEntity<String> evaluateRulesForUser(@PathVariable UUID userId) {
        try {
            ruleEngineService.evaluateRulesForUser(userId);
            
            return ResponseEntity.ok("Rule evaluation completed successfully for user: " + userId);
        } catch (Exception e) {
            log.error("Error evaluating rules for user: {}", userId, e);
            return ResponseEntity.internalServerError()
                    .body("Error evaluating rules: " + e.getMessage());
        }
    }

    private UUID extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() != null) {
            if (authentication.getPrincipal() instanceof CustomUserDetailsService.CustomUserDetails) {
                return ((CustomUserDetailsService.CustomUserDetails) authentication.getPrincipal()).getUserId();
            } else {
                throw new UnsupportedOperationException(
                    "Authentication principal is not of expected type CustomUserDetails"
                );
            }
        }
        throw new IllegalArgumentException("Authentication is required");
    }
} 