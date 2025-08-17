package com.github.bztmrlan.financeassistant.dto;

import com.github.bztmrlan.financeassistant.controller.BudgetController;
import com.github.bztmrlan.financeassistant.enums.BudgetStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class BudgetResponse {
    private UUID id;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private BudgetStatus status;
    private UUID userId;
    private String period;
    private List<BudgetCategoryResponse> categoryLimits;
}
