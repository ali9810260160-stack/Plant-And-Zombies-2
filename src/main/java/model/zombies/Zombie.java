package model.zombies;

import model.enums.ArmorType;
import model.enums.ZombieEffect;
import model.enums.ZombieType;

import java.util.Map;

/**
 * کلاس انتزاعی پایه برای تمام زامبی‌ها.
 * شامل مشخصات ذاتی، زره، موقعیت و افکت‌های فعال.
 */
public abstract class Zombie {

    /** نوع زامبی */
    protected ZombieType type;

    /** سلامتی فعلی */
    protected int currentHealth;

    /** حداکثر سلامتی */
    protected int maxHealth;

    /** سرعت حرکت (خانه بر ثانیه) */
    protected double moveSpeed;

    /** آسیب دمیج به گیاه در هر ثانیه */
    protected int damagePerSecond;

    /** هزینه موج (waveCost) برای محاسبه سختی موج */
    protected int waveCost;

    /** موقعیت افقی (می‌تواند اعشاری باشد) */
    protected double x;

    /** موقعیت ردیف (صحیح) */
    protected int y;

    /** آیا این زامبی درخشان است (5% احتمال، plant food می‌دهد) */
    protected boolean glowing;

    /** آیا این زامبی رئیس است (ماشین چمن‌زنی را فعال نمی‌کند) */
    protected boolean boss;

    /** زره‌های فعال با مقدار HP هر کدام */
    protected Map<ArmorType, Integer> armors;

    /** افکت‌های وضعیتی فعال با تیک باقیمانده */
    protected Map<ZombieEffect, Integer> activeEffects;

    /** شماره موج که این زامبی در آن ظاهر شده */
    protected int spawnWave;

    /** شماره ردیف که این زامبی در آن حرکت می‌کند */
    protected int lane;

    // ---- Abstract Methods ----

    /** رفتار خاص این زامبی را در هر تیک اجرا می‌کند */
    public abstract void onTick(int tickCount);

    /** رشته توضیحات این زامبی را برمی‌گرداند (برای collection) */
    public abstract String getDescription();

    // ---- Common Methods ----

    /** آسیب وارد می‌کند - ابتدا به زره، بعد به خود زامبی */
    public void takeDamage(int damage) { }

    /** آسیب سمی وارد می‌کند - زره را نادیده می‌گیرد */
    public void takePoisonDamage(int damage) { }

    /** بررسی می‌کند آیا زامبی زنده است */
    public boolean isAlive() { return currentHealth > 0; }

    /** بررسی می‌کند آیا افکت خاصی فعال است */
    public boolean hasEffect(ZombieEffect effect) { return false; }

    /** یک افکت اضافه می‌کند */
    public void addEffect(ZombieEffect effect, int durationTicks) { }

    /** یک افکت حذف می‌کند */
    public void removeEffect(ZombieEffect effect) { }

    /** سرعت مؤثر را با احتساب افکت‌ها برمی‌گرداند */
    public double getEffectiveMoveSpeed() { return moveSpeed; }

    /** یک نوع زره را حذف می‌کند (وقتی HP آن صفر شد) */
    public void removeArmor(ArmorType armorType) { }

    /** بررسی می‌کند آیا زره خاصی دارد */
    public boolean hasArmor(ArmorType armorType) { return false; }

    // ---- Getters & Setters ----

    public ZombieType getType() { return type; }
    public int getCurrentHealth() { return currentHealth; }
    public void setCurrentHealth(int currentHealth) { this.currentHealth = currentHealth; }
    public int getMaxHealth() { return maxHealth; }
    public double getMoveSpeed() { return moveSpeed; }
    public void setMoveSpeed(double moveSpeed) { this.moveSpeed = moveSpeed; }
    public int getDamagePerSecond() { return damagePerSecond; }
    public int getWaveCost() { return waveCost; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public boolean isGlowing() { return glowing; }
    public void setGlowing(boolean glowing) { this.glowing = glowing; }
    public boolean isBoss() { return boss; }
    public int getSpawnWave() { return spawnWave; }
    public void setSpawnWave(int spawnWave) { this.spawnWave = spawnWave; }
    public int getLane() { return lane; }
    public void setLane(int lane) { this.lane = lane; }
    public Map<ArmorType, Integer> getArmors() { return armors; }
    public Map<ZombieEffect, Integer> getActiveEffects() { return activeEffects; }
}
