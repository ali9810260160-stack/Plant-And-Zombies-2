package service;

import model.GameSession;
import model.User;
import model.enums.PlantType;
import util.RandomUtil;
import view.ConsoleView;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * سرویس بازی امتیازی.
 * ویژگی کلیدی: همه بازیکنان در یک روز با seed یکسان بازی می‌کنند.
 *
 * ۵ الگوی امتیازی:
 * 1. Multi-Kill: کشتن چند زامبی با یک تیر
 * 2. Speed-Kill: کشتن زامبی در کمتر از ۳ ثانیه بعد از ظهورش
 * 3. Simultaneous-AoE: کشتن ۳+ زامبی با یک انفجار همزمان
 * 4. Clean-Wave: موج بدون از دست دادن گیاه
 * 5. Item-Collector: جمع‌آوری ۵ خورشید در ۱۰ تیک
 */
public class ScoredGameService {

    private static final int DAILY_SEED_MULTIPLIER = 100003;
    private static final long MULTI_KILL_BONUS = 500L;
    private static final long SPEED_KILL_BONUS = 300L;
    private static final long AOE_KILL_BONUS = 750L;
    private static final long CLEAN_WAVE_BONUS = 1000L;
    private static final long ITEM_COLLECT_BONUS = 200L;

    private final ConsoleView view;
    private long meoPoints;
    private int multiKillCount;
    private int lastKillTick;
    private int consecutiveKills;
    private int plantsLostThisWave;
    private int itemsCollectedInWindow;
    private int itemWindowStartTick;
    private boolean inItemWindow;

    public ScoredGameService(ConsoleView view) {
        this.view = view;
        reset();
    }

    public void reset() {
        meoPoints = 0;
        multiKillCount = 0;
        lastKillTick = -1;
        consecutiveKills = 0;
        plantsLostThisWave = 0;
        itemsCollectedInWindow = 0;
        itemWindowStartTick = -1;
        inItemWindow = false;
    }

    /** seed روزانه — همه بازیکنان یک seed دارند */
    public long getDailySeed() {
        LocalDate today = LocalDate.now();
        return today.toEpochDay() * DAILY_SEED_MULTIPLIER;
    }

    public Random getDailyRandom() {
        return new Random(getDailySeed());
    }

    /**
     * گیاهان پیش‌فرض برای بازی امتیازی (یکسان برای همه بازیکنان)
     */
    public List<PlantType> getDefaultScoredPlants() {
        return Arrays.asList(
                PlantType.SUNFLOWER,
                PlantType.PEASHOOTER,
                PlantType.REPEATER,
                PlantType.SNOW_PEA,
                PlantType.CHERRY_BOMB,
                PlantType.WALL_NUT,
                PlantType.CABBAGE_PULT,
                PlantType.POTATO_MINE
        );
    }

    // ─────────────────────────────────────
    // الگو ۱: Multi-Kill — یک تیر چند زامبی
    // ─────────────────────────────────────
    public void onProjectileKill(int killCountInOneShot, int currentTick) {
        if (killCountInOneShot >= 2) {
            long bonus = MULTI_KILL_BONUS * killCountInOneShot;
            meoPoints += bonus;
            printMeoEvent("⚡ MULTI-KILL x" + killCountInOneShot + "!",
                    bonus, currentTick);
        }
        onZombieKilled(currentTick);
    }

    // ─────────────────────────────────────
    // الگو ۲: Speed-Kill — مرگ سریع
    // ─────────────────────────────────────
    public void onZombieKilled(int currentTick) {
        if (lastKillTick != -1 && currentTick - lastKillTick <= 30) {
            consecutiveKills++;
            if (consecutiveKills >= 2) {
                long bonus = SPEED_KILL_BONUS * consecutiveKills;
                meoPoints += bonus;
                printMeoEvent("⚡ SPEED-KILL COMBO x" + consecutiveKills + "!",
                        bonus, currentTick);
            }
        } else {
            consecutiveKills = 1;
        }
        lastKillTick = currentTick;
    }

    public void onZombieSpawned(int zombieId, int spawnTick) {
        // ثبت زمان ظهور هر زامبی برای الگوی Speed-Kill
    }

    // ─────────────────────────────────────
    // الگو ۳: AoE Kill — انفجار همزمان
    // ─────────────────────────────────────
    public void onAoeKill(int simultaneousKills, int currentTick) {
        if (simultaneousKills >= 3) {
            long bonus = AOE_KILL_BONUS * simultaneousKills;
            meoPoints += bonus;
            printMeoEvent("💥 AOE x" + simultaneousKills + " simultaneous kills!",
                    bonus, currentTick);
        }
    }

    // ─────────────────────────────────────
    // الگو ۴: Clean Wave — موج بدون باخت
    // ─────────────────────────────────────
    public void onWaveCompleted(int waveNumber, int plantsLost) {
        if (plantsLost == 0) {
            long bonus = CLEAN_WAVE_BONUS * waveNumber;
            meoPoints += bonus;
            printMeoEvent("🛡 CLEAN WAVE " + waveNumber
                    + "! No plants lost!", bonus, 0);
        }
        plantsLostThisWave = 0;
    }

    public void onPlantLost() {
        plantsLostThisWave++;
    }

    // ─────────────────────────────────────
    // الگو ۵: Item Collector — جمع‌آوری آیتم
    // ─────────────────────────────────────
    public void onItemCollected(int currentTick) {
        if (!inItemWindow) {
            inItemWindow = true;
            itemWindowStartTick = currentTick;
            itemsCollectedInWindow = 1;
        } else if (currentTick - itemWindowStartTick <= 100) {
            itemsCollectedInWindow++;
            if (itemsCollectedInWindow >= 5) {
                long bonus = ITEM_COLLECT_BONUS * itemsCollectedInWindow;
                meoPoints += bonus;
                printMeoEvent("✨ ITEM COLLECTOR x"
                                + itemsCollectedInWindow + " in 10 ticks!",
                        bonus, currentTick);
                inItemWindow = false;
                itemsCollectedInWindow = 0;
            }
        } else {
            inItemWindow = true;
            itemWindowStartTick = currentTick;
            itemsCollectedInWindow = 1;
        }
    }

    private void printMeoEvent(String event, long bonus, int tick) {
        System.out.println(ConsoleView.BOLD + ConsoleView.YELLOW
                + "  🏆 MEO-POINT! " + event
                + " +" + bonus + " pts  [Total: " + meoPoints + "]"
                + ConsoleView.RESET);
    }

    public void saveHighScore(User user, long finalScore) {
        if (finalScore > user.getHighestMeoPoint()) {
            user.setHighestMeoPoint(finalScore);
            System.out.println(ConsoleView.GREEN
                    + "  🏆 NEW HIGH SCORE: " + finalScore
                    + " MeoPoints!" + ConsoleView.RESET);
        } else {
            System.out.println(ConsoleView.CYAN
                    + "  Score: " + finalScore
                    + " MeoPoints (Best: " + user.getHighestMeoPoint() + ")"
                    + ConsoleView.RESET);
        }
    }

    public void printFinalScore(User user) {
        view.printSeparator();
        view.printHeader("🏆 Scored Game — Final Results");
        System.out.println(ConsoleView.YELLOW
                + "  Total MeoPoints:    " + meoPoints + ConsoleView.RESET);
        System.out.println(ConsoleView.GREEN
                + "  Your Best Score:   "
                + user.getHighestMeoPoint() + ConsoleView.RESET);
        System.out.println(ConsoleView.CYAN
                + "  Daily Seed:        " + getDailySeed() + ConsoleView.RESET);
        view.printSeparator();
        System.out.println(ConsoleView.WHITE + "  MeoPoint Breakdown:" + ConsoleView.RESET);
        System.out.println("    Pattern 1 — Multi-Kill:      "
                + ConsoleView.YELLOW + "(earned via StrikeThrough projectiles)"
                + ConsoleView.RESET);
        System.out.println("    Pattern 2 — Speed-Kill Combo: "
                + ConsoleView.YELLOW + "(fast consecutive kills)"
                + ConsoleView.RESET);
        System.out.println("    Pattern 3 — AoE Simultaneous: "
                + ConsoleView.YELLOW + "(Cherry Bomb, Melon, explosions)"
                + ConsoleView.RESET);
        System.out.println("    Pattern 4 — Clean Wave:       "
                + ConsoleView.YELLOW + "(no plants lost per wave)"
                + ConsoleView.RESET);
        System.out.println("    Pattern 5 — Item Collector:   "
                + ConsoleView.YELLOW + "(5+ suns in 10 ticks)"
                + ConsoleView.RESET);
        view.printSeparator();
    }

    public long getMeoPoints() { return meoPoints; }
    public void addMeoPoints(long pts) { meoPoints += pts; }
}
