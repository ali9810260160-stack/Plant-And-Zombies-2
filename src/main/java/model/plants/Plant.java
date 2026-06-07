package model.plants;

import model.enums.PlantEffect;
import model.enums.PlantFamily;
import model.enums.PlantTag;
import model.enums.PlantType;

import java.util.List;
import java.util.Map;

/**
 * کلاس انتزاعی پایه برای تمام گیاهان.
 * هر گیاه مشخصات ذاتی (stats) و وضعیت جاری (state) دارد.
 */
public abstract class Plant {

    /** نوع گیاه */
    protected PlantType type;

    /** دسته‌بندی اصلی گیاه */
    protected PlantFamily family;

    /** تگ‌های گیاه */
    protected List<PlantTag> tags;

    /** سطح فعلی ارتقا (از 1 شروع می‌شود) */
    protected int level;

    /** سلامتی فعلی */
    protected int currentHealth;

    /** حداکثر سلامتی (بر اساس سطح) */
    protected int maxHealth;

    /** هزینه کاشت بر حسب خورشید */
    protected int sunCost;

    /** زمان cooldown بین دو کاشت (بر حسب ثانیه بازی) */
    protected double rechargeTime;

    /** تیک‌های باقیمانده تا پایان cooldown */
    protected int remainingCooldownTicks;

    /** موقعیت ستون روی نقشه */
    protected int x;

    /** موقعیت ردیف روی نقشه */
    protected int y;

    /** آیا گیاه boost شده (اثر plant food بلافاصله فعال) */
    protected boolean boosted;

    /** آیا یک بوست ذخیره‌ای از گلخانه دارد */
    protected boolean hasStoredBoost;

    /** افکت‌های فعال روی این گیاه به همراه تیک باقیمانده */
    protected Map<PlantEffect, Integer> activeEffects;

    /** تعداد seed packet جمع‌آوری‌شده برای این نوع گیاه */
    protected int seedPackets;

    // ---- Abstract Methods ----

    /** عمل اصلی گیاه را در هر تیک انجام می‌دهد */
    public abstract void onTick(int tickCount);

    /** اثر plant food را روی این گیاه فعال می‌کند */
    public abstract void activatePlantFood();

    /** رشته توضیحات این گیاه را برمی‌گرداند (برای منوی collection) */
    public abstract String getDescription();

    // ---- Common Methods ----

    /** آسیب وارد می‌کند و سلامتی را کاهش می‌دهد */
    public void takeDamage(int damage) { }

    /** بررسی می‌کند آیا گیاه زنده است */
    public boolean isAlive() { return currentHealth > 0; }

    /** بررسی می‌کند آیا گیاه در حال cooldown است */
    public boolean isOnCooldown() { return remainingCooldownTicks > 0; }

    /** cooldown را ریست می‌کند (برای cheat) */
    public void resetCooldown() { remainingCooldownTicks = 0; }

    /** یک تیک از cooldown کم می‌کند */
    public void tickCooldown() { }

    /** بررسی می‌کند آیا افکت خاصی فعال است */
    public boolean hasEffect(PlantEffect effect) { return false; }

    /** یک افکت را اضافه می‌کند */
    public void addEffect(PlantEffect effect, int durationTicks) { }

    /** یک افکت را حذف می‌کند */
    public void removeEffect(PlantEffect effect) { }

    // ---- Getters & Setters ----

    public PlantType getType() { return type; }
    public PlantFamily getFamily() { return family; }
    public List<PlantTag> getTags() { return tags; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getCurrentHealth() { return currentHealth; }
    public void setCurrentHealth(int currentHealth) { this.currentHealth = currentHealth; }
    public int getMaxHealth() { return maxHealth; }
    public int getSunCost() { return sunCost; }
    public double getRechargeTime() { return rechargeTime; }
    public int getRemainingCooldownTicks() { return remainingCooldownTicks; }
    public void setRemainingCooldownTicks(int ticks) { this.remainingCooldownTicks = ticks; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public boolean isBoosted() { return boosted; }
    public void setBoosted(boolean boosted) { this.boosted = boosted; }
    public boolean isHasStoredBoost() { return hasStoredBoost; }
    public void setHasStoredBoost(boolean hasStoredBoost) { this.hasStoredBoost = hasStoredBoost; }
    public int getSeedPackets() { return seedPackets; }
    public void setSeedPackets(int seedPackets) { this.seedPackets = seedPackets; }
}
