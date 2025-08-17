package com.github.bztmrlan.financeassistant.exception;

import lombok.Getter;

@Getter
public class AuthenticationException extends FinanceAssistantException {
    
    public AuthenticationException(String message) {
        super(message, "AUTHENTICATION_ERROR", 401);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, "AUTHENTICATION_ERROR", 401, cause);
    }
} 