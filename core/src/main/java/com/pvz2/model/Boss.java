package com.pvz2.model;

import com.pvz2.model.enums.ChapterType;

import java.util.ArrayList;
import java.util.List;

/**
 * موجودیتِ رئیس (Zomboss) برای مراحلِ {@link com.pvz2.model.enums.LevelType#BOSS}.
 *
 * <p>یک مدلِ مستقل (نه زیرکلاسِ {@link com.pvz2.model.zombies.Zombie}) که روی
 * {@link GameSession} نگه‌داری می‌شود. منطقِ AI در
 * {@link com.pvz2.service.BossService} و آسیب/بردِ آن در
 * {@link com.pvz2.service.CombatService} است.
 *
 * <p><b>هر فصل رفتارِ اختصاصیِ خودش را دارد</b> (مطابقِ داکیومنت):
 * <ul>
 *   <li>مصر (ربات چهارپا): موشک (+سنگ‌قبر) + شارژِ سریع به جلو + احضار — حرکت دارد.</li>
 *   <li>یخ (ماموت): موشکِ یخ + بادِ یخی + یخ‌زدنِ ستون — <b>ثابت</b>، بدون حرکت/احضار.</li>
 *   <li>ساحل (کوسه): بچه‌کوسه (خوردنِ گیاهِ آبی) + توربین (کشیدنِ ۲ سطر).</li>
 *   <li>تاریک (اژدها): توپِ آتش + احضار.</li>
 * </ul>
 */
public class Boss {

    public enum State {
        ENTERING, IDLE, MOVING, ATTACKING, SUMMONING, STUNNED, DYING, DEAD
    }

    /** توانایی‌های حمله — رفتار + انتخابِ کلیپ/افکت. */
    public enum AbilityType {
        EGYPT_MISSILE,       // موشک: نابودیِ گیاهِ یک خانه + ۲ سنگ‌قبر
        EGYPT_CHARGE,        // شارژِ سریع به جلو: نابودیِ همه‌ی گیاهانِ ۲ سطر
        ICE_MISSILE,         // موشکِ یخ: نابودیِ گیاهِ یک خانه
        ICE_WIND,            // بادِ یخی: یخ‌زدنِ گیاهانِ ۲ سطر
        ICE_FREEZE_COLUMN,   // یخ‌زدنِ ستون: یخ‌زدنِ ستون + زامبیِ یخ‌زده
        BEACH_SHARK,         // بچه‌کوسه: خوردنِ گیاهِ آبی
        BEACH_TURBINE,       // توربین: کشیدن/نابودیِ گیاهانِ ۲ سطر
        DARK_FIREBALL        // توپِ آتش: نابودیِ گیاهِ یک خانه
    }

    /** یک حمله‌ی پرتابه‌ای در حالِ اجرا (موشک/توپِ آتش/بچه‌کوسه): سقوط → برخورد. */
    public static class BossAttack {
        public final AbilityType type;
        public double targetCol;
        public int targetRow;
        /** سطرِ دومِ هدف برای توانایی‌های ۲-سطری (باد/توربین/شارژ)؛ ۱- اگر ندارد. */
        public int targetRow2 = -1;
        /** برای یخ‌زدنِ ستون: true = کلِ ستونِ targetCol هدف است. */
        public boolean wholeColumn;
        public Phase phase = Phase.INCOMING;
        public int timer;
        public float progress;
        public boolean damageApplied;

        public enum Phase { INCOMING, IMPACT, DONE }

        public BossAttack(AbilityType type, double targetCol, int targetRow, int incomingTicks) {
            this.type = type;
            this.targetCol = targetCol;
            this.targetRow = targetRow;
            this.timer = incomingTicks;
        }
    }

    // ─── هویت و موقعیت ────────────────────────────────────────────────────────
    private final ChapterType chapter;
    private boolean canMoveLanes = true;
    private double x;
    /** لِینِ اصلی (بالای بازه‌ی ۲-سطریِ رئیس، ۱-based). */
    private int lane;
    /** تعدادِ سطرهایی که رئیس اشغال می‌کند (معمولاً ۲). */
    private int laneSpan = 2;
    private int targetLane;
    /** جابه‌جاییِ افقیِ شارژ (ستون؛ منفی=به‌جلو/چپ) — فقط برای رندر. */
    private float chargeOffset;

    // ─── سلامتی (۳ بخش) ───────────────────────────────────────────────────────
    private final int maxHealth;
    private int currentHealth;
    public static final int SEGMENTS = 3;
    /** برای تشخیصِ عبور از مرزِ هر بخشِ سلامتی (GZ3: گیج‌شدن بعد از هر ⅓). */
    private int lastSegments = SEGMENTS;

    // ─── ماشینِ حالت ───────────────────────────────────────────────────────────
    private State state = State.ENTERING;
    private double stateTime;
    /** توانایی‌ای که همین حالا در حالِ اجراست (برای کلیپِ رئیس در حالتِ ATTACKING). */
    private AbilityType currentAbility;

    // ─── تایمرها (تیک) ─────────────────────────────────────────────────────────
    private int attackTimer;
    private int spawnTimer;
    private int moveTimer;

    private final List<BossAttack> attacks = new ArrayList<>();

    public Boss(ChapterType chapter, int maxHealth, double x, int startLane) {
        this.chapter = chapter;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.x = x;
        this.lane = startLane;
        this.targetLane = startLane;
    }

    // ─── سلامتی ─────────────────────────────────────────────────────────────────

    public void takeDamage(int dmg) {
        if (dmg <= 0 || !isVulnerable()) return;
        currentHealth = Math.max(0, currentHealth - dmg);
    }

    public boolean isVulnerable() {
        return state == State.IDLE || state == State.MOVING
                || state == State.ATTACKING || state == State.SUMMONING
                || state == State.STUNNED;   // GZ3: گیج‌بودن هم آسیب‌پذیر است
    }

    public int getLastSegments() { return lastSegments; }
    public void setLastSegments(int s) { this.lastSegments = s; }

    public boolean isDefeated() { return state == State.DEAD; }
    public boolean isHealthDepleted() { return currentHealth <= 0; }

    public int phase() {
        double f = healthFraction();
        if (f > 2.0 / 3.0) return 1;
        if (f > 1.0 / 3.0) return 2;
        return 3;
    }

    public float healthFraction() {
        return maxHealth <= 0 ? 0f : Math.max(0f, Math.min(1f, (float) currentHealth / maxHealth));
    }

    public int segmentsRemaining() {
        if (currentHealth <= 0) return 0;
        return (int) Math.ceil(healthFraction() * SEGMENTS);
    }

    public float currentSegmentFraction() {
        float f = healthFraction();
        float perSeg = 1f / SEGMENTS;
        int fullBelow = segmentsRemaining() - 1;
        float within = (f - fullBelow * perSeg) / perSeg;
        return Math.max(0f, Math.min(1f, within));
    }

    // ─── موقعیت/سطرها ─────────────────────────────────────────────────────────

    /** سطرِ دومِ اشغال‌شده (زیرِ سطرِ اصلی؛ اگر لبه بود، بالای آن). */
    public int secondLane(int rows) {
        if (laneSpan <= 1) return lane;
        return lane < rows ? lane + 1 : lane - 1;
    }

    /** بالاترین سطرِ اشغال‌شده (۱-based). */
    public int topRow(int rows) {
        return Math.max(1, Math.min(lane, rows - laneSpan + 1));
    }

    /** آیا رئیس این سطر را اشغال می‌کند (برای برخوردِ پرتابه). */
    public boolean coversRow(int row, int rows) {
        int top = topRow(rows);
        return row >= top && row <= top + laneSpan - 1;
    }

    // ─── getters / setters ───────────────────────────────────────────────────────

    public ChapterType getChapter() { return chapter; }
    public boolean canMoveLanes() { return canMoveLanes; }
    public void setCanMoveLanes(boolean b) { this.canMoveLanes = b; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public int getLane() { return lane; }
    public void setLane(int lane) { this.lane = lane; }
    public int getLaneSpan() { return laneSpan; }
    public void setLaneSpan(int s) { this.laneSpan = s; }
    public int getTargetLane() { return targetLane; }
    public void setTargetLane(int t) { this.targetLane = t; }
    public float getChargeOffset() { return chargeOffset; }
    public void setChargeOffset(float o) { this.chargeOffset = o; }
    public int getMaxHealth() { return maxHealth; }
    public int getCurrentHealth() { return currentHealth; }
    public State getState() { return state; }
    public void setState(State state) {
        if (this.state != state) { this.state = state; this.stateTime = 0; }
    }
    public double getStateTime() { return stateTime; }
    public void addStateTime(double dt) { this.stateTime += dt; }
    public AbilityType getCurrentAbility() { return currentAbility; }
    public void setCurrentAbility(AbilityType a) { this.currentAbility = a; }
    public int getAttackTimer() { return attackTimer; }
    public void setAttackTimer(int t) { this.attackTimer = t; }
    public int getSpawnTimer() { return spawnTimer; }
    public void setSpawnTimer(int t) { this.spawnTimer = t; }
    public int getMoveTimer() { return moveTimer; }
    public void setMoveTimer(int t) { this.moveTimer = t; }
    public List<BossAttack> getAttacks() { return attacks; }
}
