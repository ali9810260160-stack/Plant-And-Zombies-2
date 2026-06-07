package model.zombies;

import model.enums.ZombieType;

/**
 * Factory برای ساخت نمونه‌های زامبی بر اساس نوع.
 */
public class ZombieFactory {

    /**
     * یک نمونه جدید از زامبی مورد نظر می‌سازد.
     * @param type نوع زامبی
     * @return نمونه ساخته‌شده
     */
    public static Zombie create(ZombieType type) {
        return null; // در فاز 1 پیاده‌سازی می‌شود
    }

    /**
     * waveCost یک نوع زامبی را برمی‌گرداند.
     * @param type نوع زامبی
     * @return waveCost
     */
    public static int getWaveCost(ZombieType type) {
        return 0;
    }

    /**
     * لیست زامبی‌های مجاز در یک فصل مشخص را برمی‌گرداند.
     * @param chapterType نوع فصل
     * @return آرایه انواع زامبی مجاز
     */
    public static ZombieType[] getAllowedZombiesForChapter(
            model.enums.ChapterType chapterType) {
        return new ZombieType[0];
    }
}
