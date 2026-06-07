package util;

/**
 * ابزار هش‌کردن رمز عبور با SHA-256.
 * برای ذخیره امن رمز کاربران استفاده می‌شود.
 */
public class HashUtil {

    /**
     * یک رشته را با SHA-256 هش می‌کند.
     * @param input رشته خام
     * @return رشته هش‌شده به فرمت hex
     */
    public static String sha256(String input) { return null; }

    /**
     * رمز خام را با هش ذخیره‌شده مقایسه می‌کند.
     * @param rawPassword رمز خام
     * @param storedHash هش ذخیره‌شده
     * @return true اگر مطابقت داشته باشد
     */
    public static boolean verify(String rawPassword, String storedHash) { return false; }
}
