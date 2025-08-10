package com.github.bztmrlan.financeassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawTransactionData {
    private LocalDate date;
    private BigDecimal amount;
    private String type;
    private String description;
    private String category;
    private String currency;
    private int rowNumber;
} 