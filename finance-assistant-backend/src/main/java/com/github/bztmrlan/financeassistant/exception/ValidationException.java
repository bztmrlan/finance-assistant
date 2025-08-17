package com.github.bztmrlan.financeassistant.exception;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class ValidationException extends FinanceAssistantException {
    
    private final Map<String, String> fieldErrors;
    private final List<String> globalErrors;
    
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", 400);
        this.fieldErrors = null;
        this.globalErrors = null;
    }
    
    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message, "VALIDATION_ERROR", 400);
        this.fieldErrors = fieldErrors;
        this.globalErrors = null;
    }
    
    public ValidationException(String message, List<String> globalErrors) {
        super(message, "VALIDATION_ERROR", 400);
        this.fieldErrors = null;
        this.globalErrors = globalErrors;
    }
    
    public ValidationException(String message, Map<String, String> fieldErrors, List<String> globalErrors) {
        super(message, "VALIDATION_ERROR", 400);
        this.fieldErrors = fieldErrors;
        this.globalErrors = globalErrors;
    }

} 