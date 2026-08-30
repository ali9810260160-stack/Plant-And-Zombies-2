package com.pvz2.model.zombies;

import com.pvz2.model.GameMap;
import com.pvz2.model.GameSession;
import com.pvz2.model.enums.AnimEvent;
import com.pvz2.model.enums.ArmorType;
import com.pvz2.model.enums.ZombieEffect;
import com.pvz2.model.enums.ZombieType;
import com.pvz2.model.tiles.Tile;

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

    /** رویداد pending برای لایه انیمیشن (بخش سوم) — نگاه کنید Plant.pendingAnimEvent. */
    protected AnimEvent pendingAnimEvent;

    /**
     * شناسه‌ی یکتای شبکه (فاز ۳) — برای همگام‌سازیِ مالتی‌پلیر: مهمان زامبی‌های
     * آینه‌ای را با همین id تطبیق می‌دهد تا انیمیشنِ راه‌رفتن بین فریم‌ها پیوسته بماند.
     */
    private static final java.util.concurrent.atomic.AtomicInteger NET_SEQ =
            new java.util.concurrent.atomic.AtomicInteger(1);
    private int netId = NET_SEQ.getAndIncrement();
    public int getNetId() { return netId; }
    public void setNetId(int id) { this.netId = id; }

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
        boolean hadArmor = !armors.isEmpty();
        if (hadArmor) {
            int remain = applyDamageToArmor(damage);
            if (!armors.isEmpty()) {
                // زره هنوز باقی است — رویداد آسیبِ عمومی برای جلوه بصری (رنگ/تکان)
                fireAnimEvent(AnimEvent.TOOK_DAMAGE_ZOMBIE);
            } else {
                // آخرین لایه زره همین حالا شکست — جلوه پرتاب‌شدن زره
                fireAnimEvent(AnimEvent.ARMOR_BROKEN);
            }
            if (remain > 0) {
                currentHealth = Math.max(0, currentHealth - remain);
            }
        } else {
            currentHealth = Math.max(0, currentHealth - damage);
            if (damage > 0) fireAnimEvent(AnimEvent.TOOK_DAMAGE_ZOMBIE);
        }
        if (currentHealth <= 0) {
            fireAnimEvent(AnimEvent.DYING_STARTED);
        }
    }

    public void takePoisonDamage(int damage) {
        currentHealth = Math.max(0, currentHealth - damage);
        if (currentHealth <= 0) fireAnimEvent(AnimEvent.DYING_STARTED);
    }

    // ─── لایه انیمیشن (بخش سوم) ─────────────────────────────────────────────

    public AnimEvent getPendingAnimEvent() {
        return pendingAnimEvent != null ? pendingAnimEvent : AnimEvent.NONE;
    }

    public void clearPendingAnimEvent() { this.pendingAnimEvent = null; }

    /** فقط اگر اولویت بالاتری از رویداد فعلیِ مصرف‌نشده داشته باشد جایگزین می‌شود. */
    public void fireAnimEvent(AnimEvent e) {
        if (e == null) return;
        if (pendingAnimEvent == null || e.overrides(pendingAnimEvent)) {
            pendingAnimEvent = e;
        }
    }

    /** برای سازگاری با کدهای قدیمی که مستقیم ست می‌کردند — از fireAnimEvent عبور می‌کند. */
    public void setPendingAnimEvent(AnimEvent e) { fireAnimEvent(e); }

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

    /** تعدادِ ضرباتِ آتشِ خورده به بلاکِ یخِ زامبی (۰=بلاکِ کامل). */
    private int iceMeltHits = 0;

    /** کشته‌شده با انفجار — هنگامِ مرگ جلوه‌ی خاکستر/پودرشدن پخش می‌شود. */
    private boolean pulverized = false;
    public boolean isPulverized() { return pulverized; }
    public void setPulverized(boolean pulverized) { this.pulverized = pulverized; }

    /** اختاپوس‌پرت‌کن: آیا اختاپوسِ خود را پرتاب کرده است؟ (یک‌بار). */
    private boolean octopusTossed = false;
    public boolean isOctopusTossed() { return octopusTossed; }
    public void setOctopusTossed(boolean v) { this.octopusTossed = v; }

    /** مراحلِ بلاکِ یخِ زامبی: total + damage1..damage5 (assets/ice blocks/zombie). */
    public static final int ICE_MELT_STAGES = 6;

    public boolean hasEffect(ZombieEffect effect) {
        return activeEffects.containsKey(effect);
    }

    public void addEffect(ZombieEffect effect, int durationTicks) {
        boolean wasActive = activeEffects.containsKey(effect);
        // با تشکیلِ یک بلاکِ یخِ تازه (FROZEN جدید)، شمارنده‌ی ذوب صفر می‌شود.
        if (effect == ZombieEffect.FROZEN && !wasActive) iceMeltHits = 0;
        activeEffects.put(effect, durationTicks);
        if (!wasActive) fireAnimEvent(AnimEvent.EFFECT_APPLIED);
    }

    /** تعدادِ ضرباتِ آتشِ خورده به بلاکِ یخِ زامبی (۰=کامل). */
    public int getIceMeltHits() { return iceMeltHits; }

    /**
     * یک ضربه‌ی تیرِ آتشین بلاکِ یخِ زامبی را یک مرحله ذوب می‌کند؛ با رسیدن به
     * آخرین مرحله، یخ کاملاً آب شده و اثرِ FROZEN برداشته می‌شود.
     */
    public void meltIceBlock() {
        if (!hasEffect(ZombieEffect.FROZEN)) return;
        iceMeltHits++;
        if (iceMeltHits >= ICE_MELT_STAGES) {
            removeEffect(ZombieEffect.FROZEN);
            iceMeltHits = 0;
        }
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

    /** مشعلِ Explorer روشن است؟ (تیرِ یخی خاموش، تیرِ آتشین روشن می‌کند). */
    private boolean torchLit = true;
    public boolean isTorchLit() { return torchLit; }
    public void setTorchLit(boolean lit) { this.torchLit = lit; }

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
    public void setHypnotized(boolean h) {
        boolean changed = this.hypnotized != h;
        this.hypnotized = h;
        if (changed && h) fireAnimEvent(AnimEvent.HYPNOTIZED);
    }
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
