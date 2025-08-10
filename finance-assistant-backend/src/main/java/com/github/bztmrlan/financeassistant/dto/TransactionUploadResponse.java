package com.github.bztmrlan.financeassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionUploadResponse {
    private int totalRows;
    private int successfulTransactions;
    private int failedTransactions;
    private int skippedDuplicates;
    private List<String> errors;
    private List<String> warnings;
    private String processingTime;
    
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
} 