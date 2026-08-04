package com.pvz2.exception;

/**
 * استثنای احراز هویت.
 * برای ورود ناموفق، رمز اشتباه، نام کاربری تکراری و ... استفاده می‌شود.
 */
public class AuthException extends RuntimeException {

    private final String errorCode;

    public AuthException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public AuthException(String message) {
        super(message);
        this.errorCode = "AUTH_ERROR";
    }

    public String getErrorCode() { return errorCode; }
}
