package model.plants;

import model.GameSession;
import model.enums.PlantEffect;
import model.enums.PlantFamily;
import model.enums.PlantTag;
import model.enums.PlantType;
import model.zombies.Zombie;
import view.ConsoleView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * کلاس انتزاعی پایه برای تمام گیاهان.
 */
public abstract class Plant {

    protected PlantType type;
    protected PlantFamily family;
    protected List<PlantTag> tags;
    protected int level;
    protected int currentHealth;
    protected int maxHealth;
    protected int sunCost;
    protected double rechargeTime;
    protected int remainingCooldownTicks;
    protected int x;
    protected int y;
    protected boolean boosted;
    protected boolean hasStoredBoost;
    protected Map<PlantEffect, Integer> activeEffects;
    protected int seedPackets;
    protected int baseDamage;
    protected double attackSpeed;
    protected int attackCooldown;
    protected int currentAttackCooldown;
    protected boolean frozen;
    protected int freezeLevel;

    protected Plant(PlantType type, PlantFamily family, int hp,
                    int sunCost, double rechargeTime) {
        this.type = type;
        this.family = family;
        this.maxHealth = hp;
        this.currentHealth = hp;
        this.sunCost = sunCost;
        this.rechargeTime = rechargeTime;
        this.remainingCooldownTicks = 0;
        this.level = 1;
        this.tags = new ArrayList<>();
        this.activeEffects = new HashMap<>();
        this.frozen = false;
        this.freezeLevel = 0;
        this.seedPackets = 0;
    }

    /** تیک بازی — مدیریت تایمرها و رفتار دوره‌ای گیاه */
    public abstract void onTick(int tickCount, GameSession session);

    /**
     * فعال‌سازی اثر غذای گیاه.
     *
     * @param session نشست بازی جاری (می‌تواند null باشد در حالت‌های خاص)
     */
    public abstract void activatePlantFood(GameSession session);

    /** توضیح توانایی منحصربه‌فرد گیاه */
    public abstract String getDescription();

    /**
     * هنگامی که زامبی به این گیاه حمله می‌کند فراخوانده می‌شود.
     * @return true اگر حمله «مدیریت شد» و گیاه نباید آسیب بخورد (مثل Chomper)
     */
    public boolean onZombieAttack(Zombie zombie, GameSession session) {
        return false;
    }

    /**
     * هنگامی که این گیاه نابود می‌شود فراخوانده می‌شود.
     * @param killer زامبی‌ای که آن را نابود کرد (ممکن است null باشد)
     */
    public void onPlantDestroyed(Zombie killer, GameSession session) {
        // پیش‌فرض: بدون اثر
    }

    public void takeDamage(int damage) {
        currentHealth = Math.max(0, currentHealth - damage);
    }

    public boolean isAlive() {
        return currentHealth > 0;
    }

    public boolean isOnCooldown() {
        return remainingCooldownTicks > 0;
    }

    public void resetCooldown() {
        remainingCooldownTicks = 0;
    }

    public void startCooldown() {
        this.remainingCooldownTicks = (int) (rechargeTime * 10);
    }

    public void tickCooldown() {
        if (remainingCooldownTicks > 0) {
            remainingCooldownTicks--;
        }
    }

    public boolean hasEffect(PlantEffect effect) {
        return activeEffects.containsKey(effect);
    }

    public void addEffect(PlantEffect effect, int durationTicks) {
        activeEffects.put(effect, durationTicks);
    }

    public void removeEffect(PlantEffect effect) {
        activeEffects.remove(effect);
    }

    public void tickEffects() {
        List<PlantEffect> toRemove = new ArrayList<>();
        for (Map.Entry<PlantEffect, Integer> e : activeEffects.entrySet()) {
            int remaining = e.getValue() - 1;
            if (remaining <= 0) {
                toRemove.add(e.getKey());
            } else {
                activeEffects.put(e.getKey(), remaining);
            }
        }
        toRemove.forEach(activeEffects::remove);
    }

    public boolean isFrozen() {
        return frozen || freezeLevel >= 3;
    }

    public void incrementFreezeLevel() {
        freezeLevel = Math.min(3, freezeLevel + 1);
        if (freezeLevel >= 3) {
            frozen = true;
        }
    }

    public void thaw() {
        frozen = false;
        freezeLevel = 0;
    }

    public boolean hasTag(PlantTag tag) {
        return tags != null && tags.contains(tag);
    }

    public boolean isFirePlant() {
        return hasTag(PlantTag.FIRE);
    }

    public String getDisplayName() {
        return type.name().replace("_", "-").toLowerCase();
    }

    // ---- getters & setters ----

    public PlantType getType() { return type; }
    public PlantFamily getFamily() { return family; }
    public List<PlantTag> getTags() { return tags; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getCurrentHealth() { return currentHealth; }
    public void setCurrentHealth(int hp) { this.currentHealth = hp; }
    public int getMaxHealth() { return maxHealth; }
    public void setMaxHealth(int hp) { this.maxHealth = hp; }
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
    public void setHasStoredBoost(boolean b) { this.hasStoredBoost = b; }
    public int getSeedPackets() { return seedPackets; }
    public void setSeedPackets(int seedPackets) { this.seedPackets = seedPackets; }
    public Map<PlantEffect, Integer> getActiveEffects() { return activeEffects; }
    public int getBaseDamage() { return baseDamage; }
    public int getFreezeLevel() { return freezeLevel; }
    public void setFreezeLevel(int lvl) {
        this.freezeLevel = lvl;
        if (lvl >= 3) this.frozen = true;
    }
}
