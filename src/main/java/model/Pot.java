package model;

import model.enums.PlantType;

/**
 * یک گلدان در گلخانه.
 * می‌تواند قفل، خالی، در حال رشد یا آماده برداشت باشد.
 */
public class Pot {

    /** موقعیت ستون (1-5) */
    private int x;

    /** موقعیت ردیف (1-4) */
    private int y;

    /** آیا این گلدان آزاد شده */
    private boolean unlocked;

    /** نوع گیاه کاشته‌شده (null اگر خالی) */
    private PlantType plantType;

    /** آیا گیاه marigold (گل معمولی) است */
    private boolean marigold;

    /** زمان کاشت (System.currentTimeMillis) */
    private long plantedAt;

    /** زمان رشد کامل (millis) - 2 ساعت برای marigold، 8 ساعت برای گیاه تصادفی */
    private long growthDurationMillis;

    /** آیا آماده برداشت است */
    private boolean ready;

    public Pot(int x, int y) {
        this.x = x;
        this.y = y;
        this.unlocked = (y == 1); // ردیف اول از ابتدا آزاد
        this.ready = false;
    }

    /** زمان باقیمانده تا رشد کامل (میلی‌ثانیه) را برمی‌گرداند */
    public long getRemainingGrowthMillis() { return 0; }

    /** ساعت‌های باقیمانده تا رشد (برای محاسبه هزینه تسریع) را برمی‌گرداند */
    public double getRemainingHours() { return 0; }

    /** هزینه الماس برای تسریع رشد را برمی‌گرداند (سقف ساعت‌های باقیمانده) */
    public int getAccelerateCost() { return 0; }

    /** وضعیت رشد را بررسی و ready را به‌روز می‌کند */
    public void checkGrowthStatus() { }

    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
    public PlantType getPlantType() { return plantType; }
    public void setPlantType(PlantType plantType) { this.plantType = plantType; }
    public boolean isMaigold() { return marigold; }
    public void setMarigold(boolean marigold) { this.marigold = marigold; }
    public long getPlantedAt() { return plantedAt; }
    public void setPlantedAt(long plantedAt) { this.plantedAt = plantedAt; }
    public long getGrowthDurationMillis() { return growthDurationMillis; }
    public void setGrowthDurationMillis(long d) { this.growthDurationMillis = d; }
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
}
