package com.github.bztmrlan.financeassistant.controller;

import com.github.bztmrlan.financeassistant.dto.InsightRequest;
import com.github.bztmrlan.financeassistant.dto.InsightResponse;
import com.github.bztmrlan.financeassistant.security.CustomUserDetailsService;
import com.github.bztmrlan.financeassistant.service.InsightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
@Slf4j
public class InsightController {

    private final InsightService insightService;


    @PostMapping
    public ResponseEntity<InsightResponse> generateInsight(
            @Valid @RequestBody InsightRequest request,
            Authentication authentication) {
        
        try {
            UUID userId = extractUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().build();
            }
            

            InsightResponse insight = insightService.generateInsight(request, userId);
            
            return ResponseEntity.ok(insight);
            
        } catch (Exception e) {
            log.error("Error generating insight", e);
            return ResponseEntity.internalServerError().build();
        }
    }


    @GetMapping
    public ResponseEntity<List<InsightResponse>> getUserInsights(Authentication authentication) {
        try {
            UUID userId = extractUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().build();
            }
            
            List<InsightResponse> insights = insightService.getUserInsights(userId);
            return ResponseEntity.ok(insights);
            
        } catch (Exception e) {
            log.error("Error retrieving user insights", e);
            return ResponseEntity.internalServerError().build();
        }
    }


    @GetMapping("/{insightId}")
    public ResponseEntity<InsightResponse> getInsight(
            @PathVariable UUID insightId,
            Authentication authentication) {
        
        try {
            UUID userId = extractUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().build();
            }

            List<InsightResponse> insights = insightService.getUserInsights(userId);
            InsightResponse insight = insights.stream()
                    .filter(i -> i.getId().equals(insightId))
                    .findFirst()
                    .orElse(null);
            
            if (insight == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(insight);
            
        } catch (Exception e) {
            log.error("Error retrieving insight: {}", insightId, e);
            return ResponseEntity.internalServerError().build();
        }
    }


    @PutMapping("/{insightId}/view")
    public ResponseEntity<Void> markInsightAsViewed(
            @PathVariable UUID insightId,
            Authentication authentication) {
        
        try {
            UUID userId = extractUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().build();
            }
            
            insightService.markInsightAsViewed(insightId, userId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("Error marking insight as viewed: {}", insightId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{insightId}")
    public ResponseEntity<Void> deleteInsight(
            @PathVariable UUID insightId,
            Authentication authentication) {
        
        try {
            UUID userId = extractUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().build();
            }
            
            insightService.deleteInsight(insightId, userId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("Error deleting insight: {}", insightId, e);
            return ResponseEntity.internalServerError().build();
        }
    }


    @GetMapping("/type/{insightType}")
    public ResponseEntity<List<InsightResponse>> getInsightsByType(
            @PathVariable String insightType,
            Authentication authentication) {
        
        try {
            UUID userId = extractUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().build();
            }
            
            List<InsightResponse> allInsights = insightService.getUserInsights(userId);
            List<InsightResponse> filteredInsights = allInsights.stream()
                    .filter(insight -> insight.getType().toString().equalsIgnoreCase(insightType))
                    .toList();
            
            return ResponseEntity.ok(filteredInsights);
            
        } catch (Exception e) {
            log.error("Error retrieving insights by type: {}", insightType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/period/{timePeriod}")
    public ResponseEntity<List<InsightResponse>> getInsightsByTimePeriod(
            @PathVariable String timePeriod,
            Authentication authentication) {
        
        try {
            UUID userId = extractUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().build();
            }
            
            List<InsightResponse> allInsights = insightService.getUserInsights(userId);
            List<InsightResponse> filteredInsights = allInsights.stream()
                    .filter(insight -> timePeriod.equalsIgnoreCase(insight.getTimePeriod()))
                    .toList();
            
            return ResponseEntity.ok(filteredInsights);
            
        } catch (Exception e) {
            log.error("Error retrieving insights by time period: {}", timePeriod, e);
            return ResponseEntity.internalServerError().build();
        }
    }


    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Authentication not available");
        }

        Object principal = authentication.getPrincipal();
        return ((CustomUserDetailsService.CustomUserDetails) principal).getUserId();
    }
} 