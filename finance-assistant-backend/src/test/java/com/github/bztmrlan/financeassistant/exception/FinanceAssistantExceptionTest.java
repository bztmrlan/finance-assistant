package com.github.bztmrlan.financeassistant.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FinanceAssistantExceptionTest {

    @Test
    void testFinanceAssistantExceptionWithMessage() {
        String message = "Test error message";
        FinanceAssistantException exception = new FinanceAssistantException(message);
        
        assertEquals(message, exception.getMessage());
        assertEquals("GENERAL_ERROR", exception.getErrorCode());
        assertEquals(500, exception.getHttpStatus());
    }

    @Test
    void testFinanceAssistantExceptionWithMessageAndErrorCode() {
        String message = "Test error message";
        String errorCode = "TEST_ERROR";
        FinanceAssistantException exception = new FinanceAssistantException(message, errorCode);
        
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(500, exception.getHttpStatus());
    }

    @Test
    void testFinanceAssistantExceptionWithMessageErrorCodeAndHttpStatus() {
        String message = "Test error message";
        String errorCode = "TEST_ERROR";
        int httpStatus = 400;
        FinanceAssistantException exception = new FinanceAssistantException(message, errorCode, httpStatus);
        
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(httpStatus, exception.getHttpStatus());
    }

    @Test
    void testFinanceAssistantExceptionWithMessageAndCause() {
        String message = "Test error message";
        Throwable cause = new RuntimeException("Original cause");
        FinanceAssistantException exception = new FinanceAssistantException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals("GENERAL_ERROR", exception.getErrorCode());
        assertEquals(500, exception.getHttpStatus());
    }

    @Test
    void testFinanceAssistantExceptionWithMessageErrorCodeHttpStatusAndCause() {
        String message = "Test error message";
        String errorCode = "TEST_ERROR";
        int httpStatus = 400;
        Throwable cause = new RuntimeException("Original cause");
        FinanceAssistantException exception = new FinanceAssistantException(message, errorCode, httpStatus, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(httpStatus, exception.getHttpStatus());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testExceptionInheritance() {
        String message = "Test error message";
        FinanceAssistantException exception = new FinanceAssistantException(message);
        
        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }
} 