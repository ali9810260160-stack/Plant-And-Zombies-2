package util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * ابزار کمکی برای خواندن و نوشتن فایل‌ها.
 */
public class FileUtil {

    public static void initDataDirectories() {
        ensureDirectory("data");
        ensureDirectory("data/saves");
    }

    public static String readFile(String filePath) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writeFile(String filePath, String content) throws IOException {
        Path path = Paths.get(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean exists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    public static void ensureDirectory(String dirPath) {
        try {
            Files.createDirectories(Paths.get(dirPath));
        } catch (IOException e) {
            System.err.println("Cannot create directory: " + dirPath);
        }
    }
}
