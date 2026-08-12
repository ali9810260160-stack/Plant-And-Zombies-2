package com.pvz2.graphics;

import com.pvz2.model.enums.LevelType;
import com.pvz2.model.enums.PlantType;

import java.util.ArrayList;
import java.util.List;

/**
 * اسنپ‌شات کامل وضعیت بازی — پل داده‌ای بین فاز ۱ و رندرر فاز ۲.
 *
 * <p><b>مختصات:</b> تمام فیلدهای {@code phase1X/phase1Y} از ۱ شروع می‌شوند
 * (مطابق فاز ۱). {@link com.pvz2.graphics.util.GameCoords} تبدیل به مختصات
 * صفحه را انجام می‌دهد.
 */
public final class GameStateSnapshot {

    // ─── منابع ──────────────────────────────────────────────────────────────
    public int sunCount;
    public int plantFoodCount;
    public int coinCount;
    public int gemCount;
    public long meoPoints;

    // ─── نوار کناری Conveyor ─────────────────────────────────────────────────
    public List<PlantType> conveyorQueue = new ArrayList<>();

    // ─── موج ────────────────────────────────────────────────────────────────
    public int   currentWave;
    public int   totalWaves;
    /** ۰..۱ — میزان پیشروی خطرناک‌ترین زامبی */
    public float zombieProgress;

    // ─── نوع مرحله و وضعیت‌های ویژه ─────────────────────────────────────────
    public LevelType levelType;
    public boolean waitingForPlayerStart; // PLANT_WHAT_YOU_GET
    public int deadlineColumn;            // 1-based, فقط DEAD_LINE
    public int maxPlantsAllowed;          // LOVE_YOUR_PLANTS
    public int plantsLost;
    public int timedWarKills;
    public int timedWarSun;
    public int timedWarTarget;
    public int timedWarSunTarget;
    public int timedWarSeconds;
    public boolean timedWarSunMode;
    public boolean egyptTornadoActive;
    public List<Integer> frostbiteWindRows = new ArrayList<>();

    // ─── موجودیت‌ها ──────────────────────────────────────────────────────────
    public List<PlantInfo>      plants      = new ArrayList<>();
    public List<ZombieInfo>     zombies     = new ArrayList<>();
    public List<ProjectileInfo> projectiles = new ArrayList<>();
    public List<SunInfo>        sunItems    = new ArrayList<>();

    // ─── شبکه ────────────────────────────────────────────────────────────────
    /** [col0based][row0based] */
    public TileType[][] tiles = new TileType[GameConstants.TILE_COLS][GameConstants.TILE_ROWS];
    /** مراتب 0-based — true = هنوز فعال */
    public boolean[] lawnMowerActive = new boolean[GameConstants.TILE_ROWS];

    // ─── وضعیت کلی ───────────────────────────────────────────────────────────
    public GameStatus status = GameStatus.PLAYING;

    // ─────────────────────────────────────────────────────────────────────────
    //  Enums and inner classes
    // ─────────────────────────────────────────────────────────────────────────

    public enum GameStatus { PLAYING, WON, LOST }

    public enum TileType {
        NORMAL, TOMBSTONE, ICY_GROUND, SLIPPERY_UP, SLIPPERY_DOWN,
        WATER, LOW_SHORE, NECROMANCY, DARK_TOMBSTONE, CRATER
    }

    // ─── گیاه ────────────────────────────────────────────────────────────────
    public static class PlantInfo {
        /** نام PlantType به lowercase */
        public String type;
        /** مختصات ۱-based (فاز ۱) */
        public int phase1X, phase1Y;
        public float hp, maxHp;
        /** ۰=طبیعی، ۳=کاملاً یخ‌زده */
        public int freezeLevel;
        public boolean boosted;
        /** ۰..۱ — چه درصدی از cooldown مانده */
        public float cooldownFraction;
        /** آیا گربه شده (جادوگر) */
        public boolean isCat;
        /** آیا این لایه روی گیاه اصلی است (pumpkin) */
        public boolean isOverlay;
    }

    // ─── زامبی ───────────────────────────────────────────────────────────────
    public static class ZombieInfo {
        public String type;
        /** موقعیت x اعشاری، ۱-based */
        public double phase1X;
        /** ردیف int، ۱-based */
        public int phase1Y;
        public float hp, maxHp;
        public List<ArmorInfo> armors = new ArrayList<>();
        /** نام‌های ZombieEffect به lowercase: frozen, chilled, … */
        public List<String> effects = new ArrayList<>();
        /** walk | eating | die | idle | attack */
        public String currentClip;
        public boolean isHypnotized;
        public boolean isGlowing;
        public boolean movingBackward;
        public boolean isAttacking;

        public static class ArmorInfo {
            public String type;
            public float hp, maxHp;
        }
    }

    // ─── پرتابه ──────────────────────────────────────────────────────────────
    public static class ProjectileInfo {
        public String type;       // normal|fire|ice|poison|lobbed|strike
        public double phase1X;    // اعشاری، ۱-based
        public int    phase1Y;    // ردیف ۱-based
        public boolean movingRight;
        public boolean isArc;
        public int    targetPhase1X, targetPhase1Y; // برای arc
    }

    // ─── آفتاب ───────────────────────────────────────────────────────────────
    public static class SunInfo {
        public int   phase1X, phase1Y;
        public int   value;
        public String sunType;     // normal|special|radioactive
        public boolean isLanded;
        public float  fallProgress; // ۰..۱
    }
}
