package com.github.bztmrlan.financeassistant.exception;

import java.util.UUID;


public class ExceptionUtils {
    

    public static void requireNonNull(Object obj, String message) {
        if (obj == null) {
            throw new ValidationException(message);
        }
    }

    public static void requireNonEmpty(String str, String message) {
        if (str == null || str.trim().isEmpty()) {
            throw new ValidationException(message);
        }
    }

    public static void requireValidUuid(UUID uuid, String message) {
        if (uuid == null) {
            throw new ValidationException(message);
        }
    }

    public static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new ValidationException(message);
        }
    }

    public static void requireFalse(boolean condition, String message) {
        if (condition) {
            throw new ValidationException(message);
        }
    }

    public static void requirePositive(Number number, String message) {
        if (number == null || number.doubleValue() <= 0) {
            throw new ValidationException(message);
        }
    }

    public static void requireNonNegative(Number number, String message) {
        if (number == null || number.doubleValue() < 0) {
            throw new ValidationException(message);
        }
    }
} 