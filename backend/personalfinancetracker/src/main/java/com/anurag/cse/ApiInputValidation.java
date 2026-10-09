package com.anurag.cse;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ApiInputValidation {
    private ApiInputValidation() {}

    public static void requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw badRequest(field + " is required and must be no longer than " + maxLength + " characters.");
        }
    }

    public static void requireOptionalText(String value, String field, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw badRequest(field + " must be no longer than " + maxLength + " characters.");
        }
    }

    public static void requirePositiveAmount(Double amount, String field) {
        if (amount == null || !Double.isFinite(amount) || amount <= 0) {
            throw badRequest(field + " must be a finite amount greater than zero.");
        }
    }

    public static void requireNonNegativeAmount(Double amount, String field) {
        if (amount == null || !Double.isFinite(amount) || amount < 0) {
            throw badRequest(field + " must be a finite amount greater than or equal to zero.");
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
