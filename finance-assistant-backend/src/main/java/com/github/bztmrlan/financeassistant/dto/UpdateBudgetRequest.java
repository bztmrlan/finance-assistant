package com.github.bztmrlan.financeassistant.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateBudgetRequest {
    private String name;
    private String description;
    private String period;
    private LocalDate startDate;
    private LocalDate endDate;
}
