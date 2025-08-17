package com.github.bztmrlan.financeassistant.exception;

import com.github.bztmrlan.financeassistant.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {


    @ExceptionHandler(FinanceAssistantException.class)
    public ResponseEntity<ErrorResponse> handleFinanceAssistantException(
            FinanceAssistantException ex, HttpServletRequest request) {
        
        log.error("FinanceAssistantException: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .httpStatus(ex.getHttpStatus())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .traceId(generateTraceId())
                .build();
        
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
        log.warn("ResourceNotFoundException: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .httpStatus(ex.getHttpStatus())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .traceId(generateTraceId())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, HttpServletRequest request) {
        
        log.warn("ValidationException: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .httpStatus(ex.getHttpStatus())
                .fieldErrors(ex.getFieldErrors())
                .globalErrors(ex.getGlobalErrors())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .traceId(generateTraceId())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }


    @ExceptionHandler(com.github.bztmrlan.financeassistant.exception.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleCustomAuthenticationException(
            com.github.bztmrlan.financeassistant.exception.AuthenticationException ex, HttpServletRequest request) {
        
        log.warn("Custom AuthenticationException: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .httpStatus(ex.getHttpStatus())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .traceId(generateTraceId())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }


    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<ErrorResponse> handleFileProcessingException(
            FileProcessingException ex, HttpServletRequest request) {
        
        log.error("FileProcessingException: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .httpStatus(ex.getHttpStatus())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .traceId(generateTraceId())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        log.warn("AccessDeniedException: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("ACCESS_DENIED")
                .message("Access denied to this resource")
                .httpStatus(403)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .traceId(generateTraceId())
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        
        log.warn("BadCredentialsException: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("BAD_CREDENTIALS")
                .message("Invalid username or password")
                .httpStatus(401)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .traceId(generateTraceId())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleSpringAuthenticationException(
            org.springframework.security.core.AuthenticationException ex, HttpServletRequest request) {
        
        log.warn("Spring AuthenticationException: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("AUTHENTICATION_ERROR")
                .message("Authentication failed")
                .details(ex.getMessage())
                .httpStatus(401)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .traceId(generateTraceId())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }


    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation failed: {}", fieldErrors);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .message("Validation failed")
                .httpStatus(400)
                .fieldErrors(fieldErrors)
                .timestamp(LocalDateTime.now())
                .path(((HttpServletRequest) request).getRequestURI())
                .method(((HttpServletRequest) request).getMethod())
                .traceId(generateTraceId())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        log.warn("Type mismatch: {}", ex.getMessage());

        assert ex.getRequiredType() != null;
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("TYPE_MISMATCH")
                .message("Invalid parameter type")
                .httpStatus(400)
                .details(String.format("Parameter '%s' should be of type %s", 
                        ex.getName(), ex.getRequiredType().getSimpleName()))
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .traceId(generateTraceId())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        
        log.error("Data integrity violation: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("DATA_INTEGRITY_VIOLATION")
                .message("Data constraint violation")
                .httpStatus(409)
                .details("The operation would violate database constraints")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .traceId(generateTraceId())
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }


    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            @NonNull HttpMessageNotReadableException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        
        log.warn("Message not readable: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("INVALID_JSON")
                .message("Invalid JSON format")
                .httpStatus(400)
                .details("The request body contains invalid JSON")
                .timestamp(LocalDateTime.now())
                .path(((HttpServletRequest) request).getRequestURI())
                .method(((HttpServletRequest) request).getMethod())
                .traceId(generateTraceId())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred")
                .httpStatus(500)
                .details("Please try again later or contact support if the problem persists")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .traceId(generateTraceId())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }


    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
} 