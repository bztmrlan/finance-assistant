package com.github.bztmrlan.financeassistant.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResourceNotFoundExceptionTest {

    @Test
    void testResourceNotFoundExceptionWithMessage() {
        String message = "Resource not found";
        ResourceNotFoundException exception = new ResourceNotFoundException(message);
        
        assertEquals(message, exception.getMessage());
        assertEquals("RESOURCE_NOT_FOUND", exception.getErrorCode());
        assertEquals(404, exception.getHttpStatus());
    }

    @Test
    void testResourceNotFoundExceptionWithResourceTypeAndIdentifier() {
        String resourceType = "Category";
        String identifier = "123";
        ResourceNotFoundException exception = new ResourceNotFoundException(resourceType, identifier);
        
        String expectedMessage = "Category with identifier '123' not found";
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals("RESOURCE_NOT_FOUND", exception.getErrorCode());
        assertEquals(404, exception.getHttpStatus());
    }

    @Test
    void testResourceNotFoundExceptionInheritance() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Test message");
        
        assertTrue(exception instanceof FinanceAssistantException);
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testResourceNotFoundExceptionWithSpecialCharacters() {
        String resourceType = "User Profile";
        String identifier = "user@example.com";
        ResourceNotFoundException exception = new ResourceNotFoundException(resourceType, identifier);
        
        String expectedMessage = "User Profile with identifier 'user@example.com' not found";
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void testResourceNotFoundExceptionWithNullIdentifier() {
        String resourceType = "Category";
        String identifier = null;
        ResourceNotFoundException exception = new ResourceNotFoundException(resourceType, identifier);
        
        String expectedMessage = "Category with identifier 'null' not found";
        assertEquals(expectedMessage, exception.getMessage());
    }
} 