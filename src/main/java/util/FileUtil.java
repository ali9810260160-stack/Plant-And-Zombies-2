package util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * ابزار کمکی برای خواندن و نوشتن فایل‌های متنی (JSON).
 * تمام فایل‌ها با encoding UTF-8 خوانده و نوشته می‌شوند.
 * تمام متدها static هستند — نیازی به نمونه‌سازی نیست.
 *
 * ساختار پیش‌فرض دایرکتوری داده:
 *   data/
 *     users.json
 *     levels/
 *       egypt.json
 *       ...
 */
public class FileUtil {

    /** دایرکتوری پایه برای تمام فایل‌های داده */
    public static final String DATA_DIR = "data";

    /** مسیر فایل کاربران */
    public static final String USERS_FILE = DATA_DIR + "/users.json";

    /** جلوگیری از نمونه‌سازی */
    private FileUtil() {
    }

    // ---- خواندن ----

    /**
     * محتوای کامل یک فایل متنی را به صورت String برمی‌گرداند.
     * اگر فایل وجود نداشته باشد null برمی‌گرداند (exception نمی‌دهد).
     *
     * @param filePath مسیر فایل (نسبی یا مطلق)
     * @return محتوای فایل یا null اگر فایل وجود نداشته باشد
     * @throws IOException اگر فایل وجود دارد ولی خواندن ممکن نباشد
     */
    public static String readFile(String filePath) throws IOException {
        if (filePath == null) {
            return null;
        }
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return null;
        }
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * محتوای فایل را می‌خواند. اگر فایل وجود نداشته باشد
     * مقدار پیش‌فرض را برمی‌گرداند.
     *
     * @param filePath     مسیر فایل
     * @param defaultValue مقدار پیش‌فرض در صورت نبود فایل
     * @return محتوای فایل یا defaultValue
     */
    public static String readFileOrDefault(String filePath, String defaultValue) {
        try {
            String content = readFile(filePath);
            return content != null ? content : defaultValue;
        } catch (IOException e) {
            return defaultValue;
        }
    }

    // ---- نوشتن ----

    /**
     * یک رشته را در فایل می‌نویسد (overwrite کامل).
     * اگر دایرکتوری مسیر وجود نداشته باشد، ابتدا آن را می‌سازد.
     *
     * @param filePath مسیر فایل
     * @param content  محتوایی که باید نوشته شود
     * @throws IOException اگر نوشتن ممکن نباشد
     */
    public static void writeFile(String filePath, String content) throws IOException {
        if (filePath == null) {
            throw new IOException("File path cannot be null");
        }
        if (content == null) {
            content = "";
        }
        Path path = Paths.get(filePath);
        // دایرکتوری والد را در صورت نبود می‌سازد
        Path parentDir = path.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }
        Files.write(
                path,
                content.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    /**
     * یک رشته را به انتهای فایل اضافه می‌کند (append).
     * اگر فایل وجود نداشته باشد ساخته می‌شود.
     *
     * @param filePath مسیر فایل
     * @param content  محتوایی که باید اضافه شود
     * @throws IOException اگر نوشتن ممکن نباشد
     */
    public static void appendToFile(String filePath, String content) throws IOException {
        if (filePath == null || content == null) {
            return;
        }
        Path path = Paths.get(filePath);
        Path parentDir = path.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }
        Files.write(
                path,
                content.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
                StandardOpenOption.WRITE
        );
    }

    // ---- بررسی وجود ----

    /**
     * بررسی می‌کند آیا فایل یا دایرکتوری در مسیر داده‌شده وجود دارد.
     *
     * @param filePath مسیر
     * @return true اگر وجود داشته باشد
     */
    public static boolean exists(String filePath) {
        if (filePath == null) {
            return false;
        }
        return Files.exists(Paths.get(filePath));
    }

    /**
     * بررسی می‌کند آیا فایل وجود دارد و خالی نیست.
     *
     * @param filePath مسیر فایل
     * @return true اگر فایل وجود دارد و محتوا دارد
     */
    public static boolean existsAndNotEmpty(String filePath) {
        if (!exists(filePath)) {
            return false;
        }
        try {
            String content = readFile(filePath);
            return content != null && !content.trim().isEmpty();
        } catch (IOException e) {
            return false;
        }
    }

    // ---- مدیریت دایرکتوری ----

    /**
     * یک دایرکتوری را در صورت نبودن می‌سازد.
     * اگر مسیر از قبل وجود داشته باشد هیچ کاری نمی‌کند.
     *
     * @param dirPath مسیر دایرکتوری
     * @throws RuntimeException اگر ساخت دایرکتوری ممکن نباشد
     */
    public static void ensureDirectory(String dirPath) {
        if (dirPath == null) {
            return;
        }
        try {
            Files.createDirectories(Paths.get(dirPath));
        } catch (IOException e) {
            throw new RuntimeException("Cannot create directory: " + dirPath, e);
        }
    }

    /**
     * ساختار پوشه‌های پایه داده را می‌سازد.
     * در شروع برنامه یک بار فراخوانی می‌شود.
     */
    public static void initDataDirectories() {
        ensureDirectory(DATA_DIR);
        ensureDirectory(DATA_DIR + "/levels");
    }

    /**
     * یک فایل را حذف می‌کند.
     * اگر فایل وجود نداشته باشد هیچ کاری نمی‌کند.
     *
     * @param filePath مسیر فایل
     * @return true اگر حذف موفق بود یا فایل از قبل وجود نداشت
     */
    public static boolean deleteFile(String filePath) {
        if (filePath == null) {
            return false;
        }
        try {
            return Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * فایل را به مسیر جدید کپی می‌کند.
     *
     * @param sourcePath مسیر مبدأ
     * @param targetPath مسیر مقصد
     * @throws IOException اگر کپی ممکن نباشد
     */
    public static void copyFile(String sourcePath, String targetPath) throws IOException {
        if (sourcePath == null || targetPath == null) {
            throw new IOException("Source and target paths cannot be null");
        }
        Path source = Paths.get(sourcePath);
        Path target = Paths.get(targetPath);
        Path targetParent = target.getParent();
        if (targetParent != null) {
            Files.createDirectories(targetParent);
        }
        Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * اندازه فایل را به بایت برمی‌گرداند.
     *
     * @param filePath مسیر فایل
     * @return اندازه به بایت یا -1 اگر فایل وجود نداشته باشد
     */
    public static long fileSize(String filePath) {
        if (!exists(filePath)) {
            return -1L;
        }
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            return -1L;
        }
    }

    /**
     * مسیر کامل (absolute) یک فایل را برمی‌گرداند.
     *
     * @param filePath مسیر نسبی
     * @return مسیر مطلق
     */
    public static String absolutePath(String filePath) {
        if (filePath == null) {
            return null;
        }
        return Paths.get(filePath).toAbsolutePath().toString();
    }
}
