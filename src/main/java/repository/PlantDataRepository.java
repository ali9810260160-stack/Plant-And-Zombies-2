package repository;

import model.enums.PlantType;

import java.util.Map;

/**
 * بارگذاری داده‌های ثابت گیاهان از فایل plants.csv.
 * مسیر: phase1/assets/Data/plants.csv
 */
public class PlantDataRepository {

    private static final String CSV_PATH = "phase1/assets/Data/plants.csv";

    /**
     * تمام داده‌های گیاهان را از CSV می‌خواند.
     * @return Map از PlantType به PlantStats
     */
    public Map<PlantType, PlantStats> loadAll() { return null; }

    /**
     * داده‌های یک گیاه خاص را برمی‌گرداند.
     * @param type نوع گیاه
     * @return PlantStats یا null
     */
    public PlantStats getStats(PlantType type) { return null; }

    /**
     * کلاس داده‌ای برای مشخصات ثابت گیاه از CSV.
     */
    public static class PlantStats {
        public int sunCost;
        public int health;
        public int damage;
        public double rechargeTime;
        public double attackInterval;
        public int seedPacketsToUpgrade;
        public int coinsToUpgrade;
        public String family;
        public String tags;
    }
}
