package com.github.bztmrlan.financeassistant.exception;

import lombok.Getter;

@Getter
public class FileProcessingException extends FinanceAssistantException {
    
    public FileProcessingException(String message) {
        super(message, "FILE_PROCESSING_ERROR", 400);
    }
    
    public FileProcessingException(String message, Throwable cause) {
        super(message, "FILE_PROCESSING_ERROR", 400, cause);
    }
    
    public FileProcessingException(String message, String errorCode) {
        super(message, errorCode, 400);
    }
} 