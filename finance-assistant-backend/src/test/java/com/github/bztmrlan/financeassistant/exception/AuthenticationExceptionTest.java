package com.github.bztmrlan.financeassistant.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthenticationExceptionTest {

    @Test
    void testAuthenticationExceptionWithMessage() {
        String message = "Authentication failed";
        AuthenticationException exception = new AuthenticationException(message);
        
        assertEquals(message, exception.getMessage());
        assertEquals("AUTHENTICATION_ERROR", exception.getErrorCode());
        assertEquals(401, exception.getHttpStatus());
    }

    @Test
    void testAuthenticationExceptionWithMessageAndCause() {
        String message = "Authentication failed";
        Throwable cause = new RuntimeException("Original cause");
        AuthenticationException exception = new AuthenticationException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals("AUTHENTICATION_ERROR", exception.getErrorCode());
        assertEquals(401, exception.getHttpStatus());
    }

    @Test
    void testAuthenticationExceptionInheritance() {
        AuthenticationException exception = new AuthenticationException("Test message");

        assertInstanceOf(FinanceAssistantException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testAuthenticationExceptionWithEmptyMessage() {
        String message = "";
        AuthenticationException exception = new AuthenticationException(message);
        
        assertEquals(message, exception.getMessage());
        assertEquals("AUTHENTICATION_ERROR", exception.getErrorCode());
        assertEquals(401, exception.getHttpStatus());
    }

    @Test
    void testAuthenticationExceptionWithNullMessage() {
        String message = null;
        AuthenticationException exception = new AuthenticationException(message);
        
        assertNull(exception.getMessage());
        assertEquals("AUTHENTICATION_ERROR", exception.getErrorCode());
        assertEquals(401, exception.getHttpStatus());
    }

    @Test
    void testAuthenticationExceptionWithNullCause() {
        String message = "Authentication failed";
        Throwable cause = null;
        AuthenticationException exception = new AuthenticationException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertEquals("AUTHENTICATION_ERROR", exception.getErrorCode());
        assertEquals(401, exception.getHttpStatus());
    }
} 