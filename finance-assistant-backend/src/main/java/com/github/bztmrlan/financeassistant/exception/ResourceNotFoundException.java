package com.github.bztmrlan.financeassistant.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends FinanceAssistantException {
    
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", 404);
    }
    
    public ResourceNotFoundException(String resourceType, String identifier) {
        super(String.format("%s with identifier '%s' not found", resourceType, identifier), 
              "RESOURCE_NOT_FOUND", 404);
    }
} 