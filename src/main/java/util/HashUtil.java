package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * ابزار هش‌کردن رمز عبور با SHA-256.
 * تمام متدها static هستند — نیازی به نمونه‌سازی نیست.
 * رمزها هرگز به صورت خام ذخیره نمی‌شوند؛
 * فقط خروجی هش مقایسه و ذخیره می‌شود.
 */
public class HashUtil {

    private static final String ALGORITHM = "SHA-256";

    /** جلوگیری از نمونه‌سازی */
    private HashUtil() {
    }

    /**
     * یک رشته را با SHA-256 هش می‌کند و خروجی را
     * به فرمت hex lowercase برمی‌گرداند.
     * مثال: sha256("hello") → "2cf24dba5fb0a30e..."
     *
     * @param input رشته خام (رمز عبور یا پاسخ سوال امنیتی)
     * @return رشته 64 کاراکتری hex یا null اگر input خالی باشد
     * @throws RuntimeException اگر SHA-256 در JVM در دسترس نباشد (عملاً رخ نمی‌دهد)
     */
    public static String sha256(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 بخش استاندارد Java SE است و عملاً هرگز رخ نمی‌دهد
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * رمز خام را با هش ذخیره‌شده مقایسه می‌کند.
     * ابتدا input را هش می‌کند، سپس با storedHash مقایسه می‌کند.
     *
     * @param rawInput    رمز یا پاسخ خام وارد شده توسط کاربر
     * @param storedHash  هش ذخیره‌شده در فایل
     * @return true اگر هش‌ها با هم برابر باشند
     */
    public static boolean verify(String rawInput, String storedHash) {
        if (rawInput == null || storedHash == null) {
            return false;
        }
        String inputHash = sha256(rawInput);
        if (inputHash == null) {
            return false;
        }
        // مقایسه constant-time برای جلوگیری از timing attack
        return constantTimeEquals(inputHash, storedHash);
    }

    /**
     * بررسی می‌کند آیا یک رشته قبلاً هش شده است.
     * هش SHA-256 دقیقاً 64 کاراکتر hex دارد.
     *
     * @param input رشته مورد بررسی
     * @return true اگر به فرمت هش SHA-256 باشد
     */
    public static boolean isHashed(String input) {
        if (input == null) {
            return false;
        }
        return input.matches("[0-9a-f]{64}");
    }

    /**
     * آرایه بایت را به رشته hex lowercase تبدیل می‌کند.
     *
     * @param bytes آرایه بایت
     * @return رشته hex
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexBuilder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // هر بایت را به دو کاراکتر hex تبدیل می‌کند
            hexBuilder.append(String.format("%02x", b));
        }
        return hexBuilder.toString();
    }

    /**
     * دو رشته را در زمان ثابت مقایسه می‌کند.
     * از timing attack جلوگیری می‌کند — حتی اگر طول‌ها فرق داشته باشند.
     *
     * @param a رشته اول
     * @param b رشته دوم
     * @return true اگر برابر باشند
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
