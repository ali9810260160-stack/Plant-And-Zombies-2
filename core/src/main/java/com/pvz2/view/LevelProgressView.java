package com.pvz2.view;

/**
 * کلاس View مخصوص نمایش پیشرفت مراحل و فصل‌ها.
 */
public class LevelProgressView {

    /**
     * چاپ وضعیت یک مرحله (باز / قفل) در یک فصل.
     *
     * @param levelNumber شماره مرحله
     * @param status      رشته وضعیت (با رنگ‌بندی ANSI)
     * @param levelType   نام نوع مرحله (مثلاً "(Normal)")
     */
    public void printLevelRow(int levelNumber, String status, String levelType) {
        System.out.printf("  Level %-2d  [%s]  %-18s%n",
                levelNumber, status, levelType);
    }

    /**
     * چاپ یک ردیف از نقشه جهانی فصل‌ها.
     *
     * @param chapterName نام فصل
     * @param status      رشته وضعیت (OPEN / LOCKED با رنگ‌بندی)
     * @param unlockedCount تعداد مراحل باز‌شده
     * @param totalCount  کل مراحل
     */
    public void printChapterRow(String chapterName, String status,
                                 long unlockedCount, int totalCount) {
        System.out.printf("  %-22s  [%s]  %d/%d levels unlocked%n",
                chapterName, status, unlockedCount, totalCount);
    }
}
