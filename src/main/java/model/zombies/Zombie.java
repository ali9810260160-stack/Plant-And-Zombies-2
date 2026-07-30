package model.zombies;

import model.GameMap;
import model.GameSession;
import model.enums.ArmorType;
import model.enums.ZombieEffect;
import model.enums.ZombieType;
import model.tiles.Tile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * کلاس انتزاعی پایه برای تمام زامبی‌ها.
 */
public abstract class Zombie {

    protected ZombieType type;
    protected int currentHealth;
    protected int maxHealth;
    protected double moveSpeed;
    protected int damagePerSecond;
    protected int waveCost;
    protected double x;
    protected int y;
    protected boolean glowing;
    protected boolean boss;
    protected boolean movingBackward;
    protected boolean hypnotized;
    protected Map<ArmorType, Integer> armors;
    protected Map<ZombieEffect, Integer> activeEffects;
    protected int spawnWave;
    protected int lane;
    protected boolean attacking;
    protected int damageTimer;

    protected Zombie(ZombieType type, int hp, int dps,
                     double speed, int waveCost) {
        this.type = type;
        this.maxHealth = hp;
        this.currentHealth = hp;
        this.damagePerSecond = dps;
        this.moveSpeed = speed;
        this.waveCost = waveCost;
        this.armors = new HashMap<>();
        this.activeEffects = new HashMap<>();
        this.glowing = false;
        this.boss = false;
        this.movingBackward = false;
        this.hypnotized = false;
        this.attacking = false;
        this.damageTimer = 0;
    }

    public abstract void onTick(int tickCount, GameSession session);
    public abstract String getDescription();

    public void onDeath(GameSession session) { }

    public void takeDamage(int damage) {
        if (!isAlive()) {
            return;
        }
        if (!armors.isEmpty()) {
            int remain = applyDamageToArmor(damage);
            if (remain > 0) {
                currentHealth = Math.max(0, currentHealth - remain);
            }
        } else {
            currentHealth = Math.max(0, currentHealth - damage);
        }
    }

    public void takePoisonDamage(int damage) {
        currentHealth = Math.max(0, currentHealth - damage);
    }

    private int applyDamageToArmor(int damage) {
        ArmorType[] priority = {
                ArmorType.HELMET, ArmorType.CONE, ArmorType.BUCKET,
                ArmorType.BLOCK, ArmorType.SHOULDER_ARMOR,
                ArmorType.NEWSPAPER, ArmorType.BARREL
        };
        for (ArmorType at : priority) {
            if (!armors.containsKey(at)) {
                continue;
            }
            int armorHp = armors.get(at);
            if (damage >= armorHp) {
                int remaining = damage - armorHp;
                armors.remove(at);
                return remaining;
            } else {
                armors.put(at, armorHp - damage);
                return 0;
            }
        }
        return damage;
    }

    public boolean isAlive() {
        return currentHealth > 0;
    }

    public boolean hasEffect(ZombieEffect effect) {
        return activeEffects.containsKey(effect);
    }

    public void addEffect(ZombieEffect effect, int durationTicks) {
        activeEffects.put(effect, durationTicks);
    }

    public void removeEffect(ZombieEffect effect) {
        activeEffects.remove(effect);
    }

    public void tickEffects() {
        List<ZombieEffect> toRemove = new ArrayList<>();
        for (Map.Entry<ZombieEffect, Integer> e : activeEffects.entrySet()) {
            int t = e.getValue() - 1;
            if (t <= 0) {
                toRemove.add(e.getKey());
            } else {
                activeEffects.put(e.getKey(), t);
            }
        }
        toRemove.forEach(activeEffects::remove);
    }

    public double getEffectiveMoveSpeed() {
        if (hasEffect(ZombieEffect.FROZEN) || hasEffect(ZombieEffect.STUNNED)) {
            return 0;
        }
        if (hasEffect(ZombieEffect.CHILLED) || hasEffect(ZombieEffect.SLOWED)) {
            return moveSpeed / 2.0;
        }
        return moveSpeed;
    }

    public void move() {
        double delta = getEffectiveMoveSpeed() * 0.1;
        if (movingBackward) {
            x += delta;
        } else {
            x -= delta;
        }
    }

    public boolean isAttackingPlant(GameSession session) {
        GameMap map = session.getGameMap();
        int col = (int) Math.round(x);
        if (!map.isValidPosition(col, y)) {
            return false;
        }
        Tile t = map.getTile(col, y);
        return t != null && t.getPlant() != null && !hypnotized;
    }

    public void removeArmor(ArmorType armorType) {
        armors.remove(armorType);
    }

    public boolean hasArmor(ArmorType armorType) {
        return armors.containsKey(armorType);
    }

    public void addArmor(ArmorType type, int hp) {
        armors.put(type, hp);
    }

    public String getDisplayName() {
        return type.name().replace("_", " ").toLowerCase();
    }

    public ZombieType getType() { return type; }
    public int getCurrentHealth() { return currentHealth; }
    public void setCurrentHealth(int hp) { this.currentHealth = hp; }
    public int getMaxHealth() { return maxHealth; }
    public double getMoveSpeed() { return moveSpeed; }
    public void setMoveSpeed(double s) { this.moveSpeed = s; }
    public int getDamagePerSecond() { return damagePerSecond; }
    public int getWaveCost() { return waveCost; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public boolean isGlowing() { return glowing; }
    public void setGlowing(boolean glowing) { this.glowing = glowing; }
    public boolean isBoss() { return boss; }
    public void setBoss(boolean boss) { this.boss = boss; }
    public boolean isMovingBackward() { return movingBackward; }
    public void setMovingBackward(boolean b) { this.movingBackward = b; }
    public boolean isHypnotized() { return hypnotized; }
    public void setHypnotized(boolean h) { this.hypnotized = h; }
    public int getSpawnWave() { return spawnWave; }
    public void setSpawnWave(int w) { this.spawnWave = w; }
    public int getLane() { return lane; }
    public void setLane(int lane) { this.lane = lane; }
    public Map<ArmorType, Integer> getArmors() { return armors; }
    public Map<ZombieEffect, Integer> getActiveEffects() { return activeEffects; }
    public boolean isAttacking() { return attacking; }
    public void setAttacking(boolean a) { this.attacking = a; }
    public int getDamageTimer() { return damageTimer; }
    public void setDamageTimer(int t) { this.damageTimer = t; }
}
