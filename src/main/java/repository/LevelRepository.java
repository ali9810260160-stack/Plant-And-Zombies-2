package repository;

import model.Level;
import model.enums.ChapterType;

import java.util.List;

/**
 * بارگذاری تعریف مراحل از فایل‌های config.
 * مراحل داده ثابت هستند و از JSON/CSV خوانده می‌شوند.
 */
public class LevelRepository {

    /** مسیر پایه فایل‌های تعریف مراحل */
    private static final String BASE_PATH = "data/levels/";

    /**
     * تمام مراحل یک فصل را بارگذاری می‌کند.
     * @param chapter نوع فصل
     * @return لیست مراحل
     */
    public List<Level> loadByChapter(ChapterType chapter) { return null; }

    /**
     * یک مرحله خاص را با شماره بارگذاری می‌کند.
     * @param chapter فصل
     * @param levelNumber شماره مرحله
     * @return مرحله یا null
     */
    public Level loadLevel(ChapterType chapter, int levelNumber) { return null; }

    /**
     * تمام مراحل بازی را بارگذاری می‌کند.
     * @return لیست تمام مراحل
     */
    public List<Level> loadAll() { return null; }
}
