package model.plants;

import model.enums.PlantType;

/**
 * Factory برای ساخت نمونه‌های گیاه بر اساس نوع.
 * جلوگیری از تکرار کد new در سراسر پروژه.
 */
public class PlantFactory {

    /**
     * یک نمونه جدید از گیاه مورد نظر می‌سازد.
     * @param type نوع گیاه
     * @return نمونه ساخته‌شده
     */
    public static Plant create(PlantType type) {
        return null; // در فاز 1 پیاده‌سازی می‌شود
    }

    /**
     * هزینه خورشید گیاه را بدون ساخت نمونه برمی‌گرداند.
     * @param type نوع گیاه
     * @return هزینه خورشید
     */
    public static int getSunCost(PlantType type) {
        return 0;
    }

    /**
     * بررسی می‌کند آیا این گیاه قابل کاشت در آب است.
     * @param type نوع گیاه
     * @return true اگر مستقیم روی آب قابل کاشت باشد
     */
    public static boolean isWaterPlant(PlantType type) {
        return false;
    }
}
