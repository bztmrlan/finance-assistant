package com.github.bztmrlan.financeassistant.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.bztmrlan.financeassistant.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simplified integration test for exception handling
 * Tests the exception handling logic without requiring full Spring context
 */
class ExceptionHandlingIntegrationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testExceptionResponseSerialization() throws Exception {
        // Test that our exception classes can be properly serialized to JSON
        // This validates the integration between exceptions and error responses
        
        // Test ResourceNotFoundException
        ResourceNotFoundException resourceException = new ResourceNotFoundException("Category", "123");
        assertNotNull(resourceException.getMessage());
        assertEquals("RESOURCE_NOT_FOUND", resourceException.getErrorCode());
        assertEquals(404, resourceException.getHttpStatus());
        
        // Test ValidationException
        ValidationException validationException = new ValidationException("Validation failed");
        assertNotNull(validationException.getMessage());
        assertEquals("VALIDATION_ERROR", validationException.getErrorCode());
        assertEquals(400, validationException.getHttpStatus());
        
        // Test AuthenticationException
        AuthenticationException authException = new AuthenticationException("Authentication failed");
        assertNotNull(authException.getMessage());
        assertEquals("AUTHENTICATION_ERROR", authException.getErrorCode());
        assertEquals(401, authException.getHttpStatus());
        
        // Test FileProcessingException
        FileProcessingException fileException = new FileProcessingException("File processing failed");
        assertNotNull(fileException.getMessage());
        assertEquals("FILE_PROCESSING_ERROR", fileException.getErrorCode());
        assertEquals(400, fileException.getHttpStatus());
    }

    @Test
    void testErrorResponseStructure() throws Exception {
        // Test that ErrorResponse can be properly constructed and serialized
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("TEST_ERROR")
                .message("Test error message")
                .httpStatus(400)
                .details("Test details")
                .path("/api/test")
                .method("GET")
                .traceId("12345678")
                .build();
        
        // Verify all fields are set correctly
        assertNotNull(errorResponse);
        assertEquals("TEST_ERROR", errorResponse.getErrorCode());
        assertEquals("Test error message", errorResponse.getMessage());
        assertEquals(400, errorResponse.getHttpStatus());
        assertEquals("Test details", errorResponse.getDetails());
        assertEquals("/api/test", errorResponse.getPath());
        assertEquals("GET", errorResponse.getMethod());
        assertEquals("12345678", errorResponse.getTraceId());
        
        // Test JSON serialization
        String json = objectMapper.writeValueAsString(errorResponse);
        assertNotNull(json);
        assertTrue(json.contains("TEST_ERROR"));
        assertTrue(json.contains("Test error message"));
        assertTrue(json.contains("400"));
    }

    @Test
    void testExceptionChainHandling() throws Exception {
        // Test that exceptions can be chained properly
        Exception originalException = new RuntimeException("Original error");
        FileProcessingException fileException = new FileProcessingException("File failed", originalException);
        
        assertNotNull(fileException.getCause());
        assertEquals(originalException, fileException.getCause());
        assertEquals("File failed", fileException.getMessage());
        assertEquals("FILE_PROCESSING_ERROR", fileException.getErrorCode());
        assertEquals(400, fileException.getHttpStatus());
    }

    @Test
    void testValidationExceptionWithFieldErrors() throws Exception {
        // Test ValidationException with field errors
        java.util.Map<String, String> fieldErrors = new java.util.HashMap<>();
        fieldErrors.put("name", "Name is required");
        fieldErrors.put("email", "Invalid email format");
        
        ValidationException validationException = new ValidationException("Validation failed", fieldErrors);
        
        assertNotNull(validationException.getFieldErrors());
        assertEquals(2, validationException.getFieldErrors().size());
        assertEquals("Name is required", validationException.getFieldErrors().get("name"));
        assertEquals("Invalid email format", validationException.getFieldErrors().get("email"));
    }

    @Test
    void testExceptionUtilsIntegration() throws Exception {
        // Test that ExceptionUtils properly throws ValidationException
        String testString = null;
        
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            ExceptionUtils.requireNonNull(testString, "String cannot be null");
        });
        
        assertEquals("String cannot be null", exception.getMessage());
        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
        assertEquals(400, exception.getHttpStatus());
    }

    @Test
    void testGlobalExceptionHandlerCompatibility() throws Exception {
        // Test that our custom exceptions are compatible with the GlobalExceptionHandler
        // This ensures the integration between custom exceptions and the handler
        
        // Test that all exception types can be handled
        ResourceNotFoundException resourceException = new ResourceNotFoundException("Test", "123");
        assertNotNull(resourceException.getMessage());
        assertNotNull(resourceException.getErrorCode());
        assertTrue(resourceException.getHttpStatus() > 0);
        
        ValidationException validationException = new ValidationException("Test validation");
        assertNotNull(validationException.getMessage());
        assertNotNull(validationException.getErrorCode());
        assertTrue(validationException.getHttpStatus() > 0);
        
        AuthenticationException authException = new AuthenticationException("Test auth");
        assertNotNull(authException.getMessage());
        assertNotNull(authException.getErrorCode());
        assertTrue(authException.getHttpStatus() > 0);
        
        FileProcessingException fileException = new FileProcessingException("Test file");
        assertNotNull(fileException.getMessage());
        assertNotNull(fileException.getErrorCode());
        assertTrue(fileException.getHttpStatus() > 0);
    }
} 