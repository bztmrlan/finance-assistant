package com.github.bztmrlan.financeassistant.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FileProcessingExceptionTest {

    @Test
    void testFileProcessingExceptionWithMessage() {
        String message = "File processing failed";
        FileProcessingException exception = new FileProcessingException(message);
        
        assertEquals(message, exception.getMessage());
        assertEquals("FILE_PROCESSING_ERROR", exception.getErrorCode());
        assertEquals(400, exception.getHttpStatus());
    }

    @Test
    void testFileProcessingExceptionWithMessageAndCause() {
        String message = "File processing failed";
        Throwable cause = new RuntimeException("Original cause");
        FileProcessingException exception = new FileProcessingException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals("FILE_PROCESSING_ERROR", exception.getErrorCode());
        assertEquals(400, exception.getHttpStatus());
    }

    @Test
    void testFileProcessingExceptionWithMessageAndErrorCode() {
        String message = "File processing failed";
        String errorCode = "CSV_PARSE_ERROR";
        FileProcessingException exception = new FileProcessingException(message, errorCode);
        
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(400, exception.getHttpStatus());
    }

    @Test
    void testFileProcessingExceptionInheritance() {
        FileProcessingException exception = new FileProcessingException("Test message");
        
        assertTrue(exception instanceof FinanceAssistantException);
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testFileProcessingExceptionWithEmptyMessage() {
        String message = "";
        FileProcessingException exception = new FileProcessingException(message);
        
        assertEquals(message, exception.getMessage());
        assertEquals("FILE_PROCESSING_ERROR", exception.getErrorCode());
        assertEquals(400, exception.getHttpStatus());
    }

    @Test
    void testFileProcessingExceptionWithNullMessage() {
        String message = null;
        FileProcessingException exception = new FileProcessingException(message);
        
        assertNull(exception.getMessage());
        assertEquals("FILE_PROCESSING_ERROR", exception.getErrorCode());
        assertEquals(400, exception.getHttpStatus());
    }

    @Test
    void testFileProcessingExceptionWithNullCause() {
        String message = "File processing failed";
        Throwable cause = null;
        FileProcessingException exception = new FileProcessingException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertEquals("FILE_PROCESSING_ERROR", exception.getErrorCode());
        assertEquals(400, exception.getHttpStatus());
    }

    @Test
    void testFileProcessingExceptionWithCustomErrorCode() {
        String message = "Invalid file format";
        String errorCode = "INVALID_FORMAT";
        FileProcessingException exception = new FileProcessingException(message, errorCode);
        
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(400, exception.getHttpStatus());
    }
} 