package com.github.bztmrlan.financeassistant.exception;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ExceptionUtilsTest {

    @Test
    void testRequireNonNullWithValidObject() {
        String validObject = "test";
        assertDoesNotThrow(() -> ExceptionUtils.requireNonNull(validObject, "Should not throw"));
    }

    @Test
    void testRequireNonNullWithNullObject() {
        String nullObject = null;
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ExceptionUtils.requireNonNull(nullObject, "Object is null"));
        
        assertEquals("Object is null", exception.getMessage());
        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
        assertEquals(400, exception.getHttpStatus());
    }

    @Test
    void testRequireNonEmptyWithValidString() {
        String validString = "test";
        assertDoesNotThrow(() -> ExceptionUtils.requireNonEmpty(validString, "Should not throw"));
    }

    @Test
    void testRequireNonEmptyWithEmptyString() {
        String emptyString = "";
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ExceptionUtils.requireNonEmpty(emptyString, "String is empty"));
        
        assertEquals("String is empty", exception.getMessage());
    }

    @Test
    void testRequireNonEmptyWithWhitespaceString() {
        String whitespaceString = "   ";
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ExceptionUtils.requireNonEmpty(whitespaceString, "String is whitespace"));
        
        assertEquals("String is whitespace", exception.getMessage());
    }

    @Test
    void testRequireNonEmptyWithNullString() {
        String nullString = null;
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ExceptionUtils.requireNonEmpty(nullString, "String is null"));
        
        assertEquals("String is null", exception.getMessage());
    }

    @Test
    void testRequireValidUuidWithValidUuid() {
        UUID validUuid = UUID.randomUUID();
        assertDoesNotThrow(() -> ExceptionUtils.requireValidUuid(validUuid, "Should not throw"));
    }

    @Test
    void testRequireValidUuidWithNullUuid() {
        UUID nullUuid = null;
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ExceptionUtils.requireValidUuid(nullUuid, "UUID is null"));
        
        assertEquals("UUID is null", exception.getMessage());
    }

    @Test
    void testRequireTrueWithTrueCondition() {
        assertDoesNotThrow(() -> ExceptionUtils.requireTrue(true, "Should not throw"));
    }

    @Test
    void testRequireTrueWithFalseCondition() {
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ExceptionUtils.requireTrue(false, "Condition is false"));
        
        assertEquals("Condition is false", exception.getMessage());
    }

    @Test
    void testRequireFalseWithFalseCondition() {
        assertDoesNotThrow(() -> ExceptionUtils.requireFalse(false, "Should not throw"));
    }

    @Test
    void testRequireFalseWithTrueCondition() {
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ExceptionUtils.requireFalse(true, "Condition is true"));
        
        assertEquals("Condition is true", exception.getMessage());
    }

    @Test
    void testRequirePositiveWithPositiveNumber() {
        assertDoesNotThrow(() -> ExceptionUtils.requirePositive(5, "Should not throw"));
        assertDoesNotThrow(() -> ExceptionUtils.requirePositive(5.5, "Should not throw"));
        assertDoesNotThrow(() -> ExceptionUtils.requirePositive(0.1, "Should not throw"));
    }

    @Test
    void testRequirePositiveWithZero() {
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ExceptionUtils.requirePositive(0, "Number is zero"));
        
        assertEquals("Number is zero", exception.getMessage());
    }

    @Test
    void testRequirePositiveWithNegativeNumber() {
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ExceptionUtils.requirePositive(-5, "Number is negative"));
        
        assertEquals("Number is negative", exception.getMessage());
    }

    @Test
    void testRequirePositiveWithNullNumber() {
        Number nullNumber = null;
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ExceptionUtils.requirePositive(nullNumber, "Number is null"));
        
        assertEquals("Number is null", exception.getMessage());
    }

    @Test
    void testRequireNonNegativeWithNonNegativeNumber() {
        assertDoesNotThrow(() -> ExceptionUtils.requireNonNegative(0, "Should not throw"));
        assertDoesNotThrow(() -> ExceptionUtils.requireNonNegative(5, "Should not throw"));
        assertDoesNotThrow(() -> ExceptionUtils.requireNonNegative(0.0, "Should not throw"));
    }

    @Test
    void testRequireNonNegativeWithNegativeNumber() {
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ExceptionUtils.requireNonNegative(-5, "Number is negative"));
        
        assertEquals("Number is negative", exception.getMessage());
    }

    @Test
    void testRequireNonNegativeWithNullNumber() {
        Number nullNumber = null;
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> ExceptionUtils.requireNonNegative(nullNumber, "Number is null"));
        
        assertEquals("Number is null", exception.getMessage());
    }
} 