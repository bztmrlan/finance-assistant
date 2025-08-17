package com.github.bztmrlan.financeassistant.exception;

import lombok.Getter;

@Getter
public class FinanceAssistantException extends RuntimeException {
    
    private final String errorCode;
    private final int httpStatus;
    
    public FinanceAssistantException(String message) {
        super(message);
        this.errorCode = "GENERAL_ERROR";
        this.httpStatus = 500;
    }
    
    public FinanceAssistantException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = 500;
    }
    
    public FinanceAssistantException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public FinanceAssistantException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GENERAL_ERROR";
        this.httpStatus = 500;
    }
    
    public FinanceAssistantException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
} 