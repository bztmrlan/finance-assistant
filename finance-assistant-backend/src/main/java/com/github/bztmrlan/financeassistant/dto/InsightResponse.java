package com.github.bztmrlan.financeassistant.dto;

import com.github.bztmrlan.financeassistant.enums.InsightType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightResponse {
    
    private UUID id;
    private UUID userId;
    private InsightType type;
    private String message;
    private String userQuestion;
    private Instant generatedAt;
    private boolean viewed;
    private Double confidenceScore;
    private String categoryTags;
    private String timePeriod;
    

    private Map<String, Object> insightData;
    

    private String analysisType;
    private String dataSource;
    private Integer dataPointsAnalyzed;
} 