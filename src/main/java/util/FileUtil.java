package util;

import java.io.*;

/**
 * ابزار کمکی برای خواندن و نوشتن فایل‌های JSON.
 * ذخیره‌سازی اطلاعات کاربران و پیشرفت بازی.
 */
public class FileUtil {

    /**
     * محتوای یک فایل را به صورت رشته می‌خواند.
     * @param filePath مسیر فایل
     * @return محتوای فایل
     * @throws IOException در صورت خطای فایل
     */
    public static String readFile(String filePath) throws IOException { return null; }

    /**
     * یک رشته را در فایل می‌نویسد.
     * @param filePath مسیر فایل
     * @param content محتوا
     * @throws IOException در صورت خطای فایل
     */
    public static void writeFile(String filePath, String content) throws IOException { }

    /**
     * بررسی می‌کند فایل وجود دارد.
     * @param filePath مسیر
     * @return true اگر وجود داشته باشد
     */
    public static boolean exists(String filePath) { return false; }

    /**
     * دایرکتوری را در صورت نبودن می‌سازد.
     * @param dirPath مسیر دایرکتوری
     */
    public static void ensureDirectory(String dirPath) { }
}
