package com.pvz2.exception;

/**
 * استثنای منطق بازی.
 * برای کاشت نامعتبر گیاه، خورشید ناکافی، موجودی ناکافی فروشگاه و ... استفاده می‌شود.
 */
public class GameException extends RuntimeException {

    private final String errorCode;

    public GameException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public GameException(String message) {
        super(message);
        this.errorCode = "GAME_ERROR";
    }

    public String getErrorCode() { return errorCode; }
}
