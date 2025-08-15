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
public class TransactionResponse {
    private UUID id;
    private UUID userId;
    private UUID categoryId;
    private String categoryName;
    private String categoryType;
    private LocalDate date;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String type;
} 