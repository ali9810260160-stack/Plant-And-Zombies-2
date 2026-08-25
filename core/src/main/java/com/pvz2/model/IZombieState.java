package com.pvz2.model;

import com.pvz2.model.enums.ZombieType;
import com.pvz2.util.RandomUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * وضعیت مینی‌گیم «من زامبی» (I, Zombie) برای فاز گرافیک.
 *
 * <p>بازیِ معکوس: گیاهانِ مدافع از پیش روی زمین کاشته شده‌اند (خودکار به
 * زامبی‌های بازیکن شلیک می‌کنند) و بازیکن با «خورشید» زامبی می‌کارد تا مغزهای
 * سمت چپ هر ردیف را بخورد. بردن = خوردن همه‌ی مغزها.
 *
 * <p>فقط state است؛ منطق در {@code GameService}/{@code CombatService} اجرا می‌شود.
 * موجودیت‌های زامبیِ کاشته‌شده همان {@link com.pvz2.model.zombies.Zombie}های واقعیِ
 * {@link GameSession#getActiveZombies()} هستند (از موتور مبارزه‌ی موجود استفاده می‌شود).
 */
public class IZombieState {

    /** یک زامبیِ قابل‌کاشت در فهرست بازیکن. */
    public static final class Placeable {
        public final ZombieType type;
        public final int cost;
        public final int cooldownTicks;
        public Placeable(ZombieType type, int cost, int cooldownTicks) {
            this.type = type;
            this.cost = cost;
            this.cooldownTicks = cooldownTicks;
        }
    }

    /**
     * استخرِ کاملِ زامبی‌های قابل‌کاشت (طبق داک: مجموعاً باید ≥۱۰ زامبیِ مختلف
     * وجود داشته باشد تا ۵ زامبیِ هر مرحله با مرحله‌ی دیگر کاملاً یکسان نباشد).
     * از هر مرحله ۵ تا به‌صورت تصادفی انتخاب می‌شود. همه در فصلِ مصر resolve
     * می‌شوند (بدنه‌ی زامبیِ عادی + armorِ متمایز برای کارت).
     * (نام، هزینه‌ی خورشید، cooldown به تیک — ۱۰ تیک بر ثانیه).
     */
    private static final Placeable[] POOL = {
        new Placeable(ZombieType.NORMAL,            50,  50),
        new Placeable(ZombieType.IMP,               75,  60),
        new Placeable(ZombieType.CONEHEAD,          100, 90),
        new Placeable(ZombieType.NEWSPAPER_ZOMBIE,  100, 90),
        new Placeable(ZombieType.BUCKETHEAD,        150, 120),
        new Placeable(ZombieType.KNIGHT,            150, 120),
        new Placeable(ZombieType.BLOCKHEAD,         175, 140),
        new Placeable(ZombieType.ALL_STAR,          150, 130),
        new Placeable(ZombieType.PROSPECTOR_ZOMBIE, 125, 110),
        new Placeable(ZombieType.GARGANTUAR,        350, 300),
    };

    /** تعداد زامبی‌های در دسترسِ بازیکن در هر مرحله (طبق داک: ۵). */
    private static final int PICK_PER_LEVEL = 5;

    private final int rows;
    /** مغزها — یکی به‌ازای هر ردیف؛ true یعنی هنوز خورده نشده. */
    private final boolean[] brainAlive;
    /** تیکِ آزادشدنِ مجددِ هر نوع زامبی (برای cooldown کارت‌ها). */
    private final Map<ZombieType, Integer> readyAtTick = new HashMap<>();
    /** ۵ زامبیِ انتخابی این مرحله (تصادفی از POOL). */
    private final List<Placeable> roster = new ArrayList<>();

    public IZombieState(int rows) {
        this(rows, false);
    }

    /**
     * @param deterministic اگر true، ۵ زامبیِ اولِ استخر به‌ترتیب انتخاب می‌شوند
     *   (بدونِ تصادف). برای مالتی‌پلیرِ VERSUS لازم است تا roster میزبان و مهمان
     *   دقیقاً یکسان باشد (هر دو جلسه‌ی جدا می‌سازند).
     */
    public IZombieState(int rows, boolean deterministic) {
        this.rows = rows;
        this.brainAlive = new boolean[rows];
        Arrays.fill(brainAlive, true);
        if (deterministic) pickRosterDeterministic(); else pickRoster();
    }

    /** انتخابِ ثابتِ ۵ زامبیِ اولِ استخر (برای VERSUS). */
    private void pickRosterDeterministic() {
        int n = Math.min(PICK_PER_LEVEL, POOL.length);
        for (int i = 0; i < n; i++) roster.add(POOL[i]);
    }

    /** انتخابِ تصادفیِ ۵ زامبی از استخر (بدون تکرار) برای این مرحله. */
    private void pickRoster() {
        List<Placeable> shuffled = new ArrayList<>(Arrays.asList(POOL));
        for (int i = shuffled.size() - 1; i > 0; i--) {
            int j = RandomUtil.nextInt(i + 1);
            Placeable tmp = shuffled.get(i);
            shuffled.set(i, shuffled.get(j));
            shuffled.set(j, tmp);
        }
        int n = Math.min(PICK_PER_LEVEL, shuffled.size());
        for (int i = 0; i < n; i++) roster.add(shuffled.get(i));
    }

    /** فهرستِ ۵ زامبیِ انتخابیِ این مرحله. */
    public List<Placeable> getRoster() { return roster; }

    public boolean[] getBrains() { return brainAlive; }

    /** خوردن مغزِ یک ردیف (۱-based)؛ false اگر ردیف نامعتبر یا از قبل خورده شده. */
    public boolean eatBrain(int row1) {
        int i = row1 - 1;
        if (i < 0 || i >= rows || !brainAlive[i]) return false;
        brainAlive[i] = false;
        return true;
    }

    public boolean allBrainsEaten() {
        for (boolean b : brainAlive) if (b) return false;
        return true;
    }

    public int brainsRemaining() {
        int n = 0;
        for (boolean b : brainAlive) if (b) n++;
        return n;
    }

    // ─── هزینه و cooldown ──────────────────────────────────────────────────────

    public int costOf(ZombieType type) {
        for (Placeable p : roster) if (p.type == type) return p.cost;
        return Integer.MAX_VALUE;
    }

    private int cooldownTicksOf(ZombieType type) {
        for (Placeable p : roster) if (p.type == type) return p.cooldownTicks;
        return 0;
    }

    public boolean isPlaceable(ZombieType type) {
        for (Placeable p : roster) if (p.type == type) return true;
        return false;
    }

    public boolean isReady(ZombieType type, int currentTick) {
        return currentTick >= readyAtTick.getOrDefault(type, 0);
    }

    public void startCooldown(ZombieType type, int currentTick) {
        readyAtTick.put(type, currentTick + cooldownTicksOf(type));
    }

    /** کسری از cooldownِ باقیمانده (۰=آماده، ۱=تازه مصرف‌شده) — برای نمایش کارت. */
    public float cooldownFraction(ZombieType type, int currentTick) {
        int cd = cooldownTicksOf(type);
        if (cd <= 0) return 0f;
        int readyAt = readyAtTick.getOrDefault(type, 0);
        int remaining = readyAt - currentTick;
        if (remaining <= 0) return 0f;
        return Math.min(1f, remaining / (float) cd);
    }

    /** کمترین هزینه‌ی زامبی — برای تشخیص باخت (نبود خورشید کافی). */
    public int minCost() {
        int m = Integer.MAX_VALUE;
        for (Placeable p : roster) m = Math.min(m, p.cost);
        return m;
    }
}
