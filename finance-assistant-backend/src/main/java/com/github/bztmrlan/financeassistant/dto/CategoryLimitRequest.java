package com.github.bztmrlan.financeassistant.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CategoryLimitRequest {
    private UUID categoryId;
    private BigDecimal limitAmount;
}
