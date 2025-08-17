package com.github.bztmrlan.financeassistant.exception;

import com.github.bztmrlan.financeassistant.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("GET");
    }

    @Test
    void testHandleCustomFinanceAssistantException() {
        String message = "Test exception";
        String errorCode = "TEST_ERROR";
        int httpStatus = 400;
        
        FinanceAssistantException exception = new FinanceAssistantException(message, errorCode, httpStatus);
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleFinanceAssistantException(exception, mockRequest);
        
        assertNotNull(response);
        assertEquals(HttpStatus.valueOf(httpStatus), response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(errorCode, errorResponse.getErrorCode());
        assertEquals(message, errorResponse.getMessage());
        assertEquals("/api/test", errorResponse.getPath());
        assertEquals("GET", errorResponse.getMethod());
        assertNotNull(errorResponse.getTraceId());
    }

    @Test
    void testHandleResourceNotFoundException() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Category", "123");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFoundException(exception, mockRequest);
        
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("RESOURCE_NOT_FOUND", errorResponse.getErrorCode());
        assertEquals("Category with identifier '123' not found", errorResponse.getMessage());
    }

    @Test
    void testHandleValidationException() {
        ValidationException exception = new ValidationException("Validation failed");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(exception, mockRequest);
        
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("VALIDATION_ERROR", errorResponse.getErrorCode());
        assertEquals("Validation failed", errorResponse.getMessage());
    }

    @Test
    void testHandleCustomAuthenticationException() {
        com.github.bztmrlan.financeassistant.exception.AuthenticationException exception = 
            new com.github.bztmrlan.financeassistant.exception.AuthenticationException("Authentication failed");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleCustomAuthenticationException(exception, mockRequest);
        
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("AUTHENTICATION_ERROR", errorResponse.getErrorCode());
        assertEquals("Authentication failed", errorResponse.getMessage());
    }

    @Test
    void testHandleFileProcessingException() {
        FileProcessingException exception = new FileProcessingException("File processing failed");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleFileProcessingException(exception, mockRequest);
        
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("FILE_PROCESSING_ERROR", errorResponse.getErrorCode());
        assertEquals("File processing failed", errorResponse.getMessage());
    }

    @Test
    void testHandleAccessDeniedException() {
        AccessDeniedException exception = new AccessDeniedException("Access denied");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDeniedException(exception, mockRequest);
        
        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("ACCESS_DENIED", errorResponse.getErrorCode());
        assertEquals("Access denied to this resource", errorResponse.getMessage());
    }

    @Test
    void testHandleBadCredentialsException() {
        BadCredentialsException exception = new BadCredentialsException("Invalid credentials");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadCredentialsException(exception, mockRequest);
        
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("BAD_CREDENTIALS", errorResponse.getErrorCode());
        assertEquals("Invalid username or password", errorResponse.getMessage());
    }

    @Test
    void testHandleSpringAuthenticationException() {
        AuthenticationException exception = new AuthenticationException("Spring auth failed") {};
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleSpringAuthenticationException(exception, mockRequest);
        
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("AUTHENTICATION_ERROR", errorResponse.getErrorCode());
        assertEquals("Authentication failed", errorResponse.getMessage());
        assertEquals("Spring auth failed", errorResponse.getDetails());
    }

    @Test
    void testHandleMethodArgumentTypeMismatchException() throws Exception {

        org.springframework.core.MethodParameter methodParameter = mock(org.springframework.core.MethodParameter.class);
        
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
            "invalid", String.class, "paramName", methodParameter, new Exception("Cause"));
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodArgumentTypeMismatch(exception, mockRequest);
        
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("TYPE_MISMATCH", errorResponse.getErrorCode());
        assertEquals("Invalid parameter type", errorResponse.getMessage());
        assertTrue(errorResponse.getDetails().contains("paramName"));
        assertTrue(errorResponse.getDetails().contains("String"));
    }

    @Test
    void testHandleDataIntegrityViolationException() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException("Constraint violation");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(exception, mockRequest);
        
        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("DATA_INTEGRITY_VIOLATION", errorResponse.getErrorCode());
        assertEquals("Data constraint violation", errorResponse.getMessage());
    }



    @Test
    void testHandleGenericException() {
        Exception exception = new Exception("Unexpected error");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception, mockRequest);
        
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("INTERNAL_SERVER_ERROR", errorResponse.getErrorCode());
        assertEquals("An unexpected error occurred", errorResponse.getMessage());
    }

    @Test
    void testTraceIdGeneration() {
        FinanceAssistantException exception = new FinanceAssistantException("Test");
        
        ResponseEntity<ErrorResponse> response1 = exceptionHandler.handleFinanceAssistantException(exception, mockRequest);
        ResponseEntity<ErrorResponse> response2 = exceptionHandler.handleFinanceAssistantException(exception, mockRequest);
        
        ErrorResponse error1 = response1.getBody();
        ErrorResponse error2 = response2.getBody();
        
        assertNotNull(error1);
        assertNotNull(error2);
        assertNotNull(error1.getTraceId());
        assertNotNull(error2.getTraceId());
        assertNotEquals(error1.getTraceId(), error2.getTraceId());

        assertEquals(8, error1.getTraceId().length());
        assertEquals(8, error2.getTraceId().length());
    }

    @Test
    void testErrorResponseStructure() {
        ValidationException exception = new ValidationException("Test validation");
        
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(exception, mockRequest);
        ErrorResponse errorResponse = response.getBody();
        
        assertNotNull(errorResponse);
        assertNotNull(errorResponse.getErrorCode());
        assertNotNull(errorResponse.getMessage());
        assertNotNull(errorResponse.getTimestamp());
        assertNotNull(errorResponse.getPath());
        assertNotNull(errorResponse.getMethod());
        assertNotNull(errorResponse.getTraceId());

        assertNull(errorResponse.getDetails());
        assertNull(errorResponse.getFieldErrors());
        assertNull(errorResponse.getGlobalErrors());
    }
} 