package exception;

/**
 * استثنای اعتبارسنجی ورودی کاربر.
 * برای خطاهای register، login، تغییر اطلاعات پروفایل و ... استفاده می‌شود.
 */
public class ValidationException extends RuntimeException {

    /** کد خطای یکتا برای شناسایی نوع خطا */
    private final String errorCode;

    public ValidationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ValidationException(String message) {
        super(message);
        this.errorCode = "VALIDATION_ERROR";
    }

    public String getErrorCode() { return errorCode; }
}
