package com.github.bztmrlan.financeassistant.exception;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ValidationExceptionTest {

    @Test
    void testValidationExceptionWithMessage() {
        String message = "Validation failed";
        ValidationException exception = new ValidationException(message);
        
        assertEquals(message, exception.getMessage());
        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
        assertEquals(400, exception.getHttpStatus());
        assertNull(exception.getFieldErrors());
        assertNull(exception.getGlobalErrors());
    }

    @Test
    void testValidationExceptionWithMessageAndFieldErrors() {
        String message = "Validation failed";
        Map<String, String> fieldErrors = new HashMap<>();
        fieldErrors.put("name", "Name is required");
        fieldErrors.put("email", "Invalid email format");
        
        ValidationException exception = new ValidationException(message, fieldErrors);
        
        assertEquals(message, exception.getMessage());
        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
        assertEquals(400, exception.getHttpStatus());
        assertEquals(fieldErrors, exception.getFieldErrors());
        assertNull(exception.getGlobalErrors());
    }

    @Test
    void testValidationExceptionWithMessageAndGlobalErrors() {
        String message = "Validation failed";
        List<String> globalErrors = List.of("Global error 1", "Global error 2");
        
        ValidationException exception = new ValidationException(message, globalErrors);
        
        assertEquals(message, exception.getMessage());
        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
        assertEquals(400, exception.getHttpStatus());
        assertNull(exception.getFieldErrors());
        assertEquals(globalErrors, exception.getGlobalErrors());
    }

    @Test
    void testValidationExceptionWithMessageFieldErrorsAndGlobalErrors() {
        String message = "Validation failed";
        Map<String, String> fieldErrors = new HashMap<>();
        fieldErrors.put("name", "Name is required");
        List<String> globalErrors = List.of("Global error");
        
        ValidationException exception = new ValidationException(message, fieldErrors, globalErrors);
        
        assertEquals(message, exception.getMessage());
        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
        assertEquals(400, exception.getHttpStatus());
        assertEquals(fieldErrors, exception.getFieldErrors());
        assertEquals(globalErrors, exception.getGlobalErrors());
    }

    @Test
    void testValidationExceptionInheritance() {
        ValidationException exception = new ValidationException("Test message");
        
        assertTrue(exception instanceof FinanceAssistantException);
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testValidationExceptionWithEmptyFieldErrors() {
        String message = "Validation failed";
        Map<String, String> fieldErrors = new HashMap<>();
        
        ValidationException exception = new ValidationException(message, fieldErrors);
        
        assertEquals(message, exception.getMessage());
        assertNotNull(exception.getFieldErrors());
        assertTrue(exception.getFieldErrors().isEmpty());
    }

    @Test
    void testValidationExceptionWithEmptyGlobalErrors() {
        String message = "Validation failed";
        List<String> globalErrors = List.of();
        
        ValidationException exception = new ValidationException(message, globalErrors);
        
        assertEquals(message, exception.getMessage());
        assertNotNull(exception.getGlobalErrors());
        assertTrue(exception.getGlobalErrors().isEmpty());
    }
} 