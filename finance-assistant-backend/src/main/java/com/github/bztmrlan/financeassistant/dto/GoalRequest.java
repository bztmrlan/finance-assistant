package com.github.bztmrlan.financeassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalRequest {
    private String name;
    private BigDecimal targetAmount;
    private LocalDate targetDate;
    private UUID categoryId;
    private String currency;
} 