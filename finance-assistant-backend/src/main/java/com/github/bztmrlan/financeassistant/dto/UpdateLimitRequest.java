package com.github.bztmrlan.financeassistant.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateLimitRequest {
    private BigDecimal newLimit;
}
