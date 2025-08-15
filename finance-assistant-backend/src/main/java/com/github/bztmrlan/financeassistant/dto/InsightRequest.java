package com.github.bztmrlan.financeassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightRequest {
    
    @NotBlank(message = "Question is required")
    @Size(min = 10, max = 500, message = "Question must be between 10 and 500 characters")
    private String question;
    
    private String timePeriod;
    
    private String categoryFilter;
    
    private Boolean includeCharts;
    
    private String analysisDepth;
} 