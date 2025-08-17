package com.github.bztmrlan.financeassistant.dto;

import com.github.bztmrlan.financeassistant.controller.BudgetController;
import com.github.bztmrlan.financeassistant.model.Budget;
import lombok.Data;

import java.util.List;

@Data
public class CreateBudgetRequest {
    private Budget budget;
    private List<CategoryLimitRequest> categoryLimits;
}
