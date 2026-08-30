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
    /** خانه‌های محافظت‌شده (SAVE_OUR_SEEDS) — هر عنصر {col1based, row1based}؛
     *  اگر گیاه هرکدام کشته شود، باخت آنی. باید روی مپ مشخص شوند (خانه خطر). */
    public List<int[]> protectedTiles = new ArrayList<>();

    // ─── موجودیت‌ها ──────────────────────────────────────────────────────────
    public List<PlantInfo>      plants      = new ArrayList<>();
    public List<ZombieInfo>     zombies     = new ArrayList<>();
    public List<ProjectileInfo> projectiles = new ArrayList<>();
    public List<SunInfo>        sunItems    = new ArrayList<>();
    /** cooldownِ بسته‌ی بذر: نامِ نوعِ گیاه (lowercase) → کسر ۰..۱ (۱=تازه کاشته). */
    public java.util.Map<String, Float> seedCooldowns = new java.util.HashMap<>();

    // ─── مینی‌گیم کوزه‌شکنی ────────────────────────────────────────────────────
    /** کوزه‌های نشکسته‌ی روی زمین (فقط در مرحله VASEBREAKER). */
    public List<VaseInfo> vases = new ArrayList<>();
    /** تعداد کوزه‌های باقیمانده — برای لیبل وضعیت. */
    public int vasesRemaining;

    // ─── مینی‌گیم بولینگ گردویی ────────────────────────────────────────────────
    /** گردوهای در حال غلتیدن (فقط در مرحله WALLNUT_BOWLING). */
    public List<BowlingBallInfo> bowlingBalls = new ArrayList<>();

    // ─── مینی‌گیم من زامبی ─────────────────────────────────────────────────────
    /** مغزهای زنده به‌ازای هر ردیف (۰-based) — فقط در مرحله I_ZOMBIE. */
    public boolean[] izombieBrains = new boolean[GameConstants.TILE_ROWS];
    /** فهرست زامبی‌های قابل‌کاشت بازیکن (کارت‌ها). */
    public List<ZombieCardInfo> izombieRoster = new ArrayList<>();

    // ─── VERSUS (فاز ۳): دونفره ────────────────────────────────────────────────
    /** خورشیدِ بازیکنِ زامبی (جدا از sunCountِ گیاه‌کار). */
    public int versusZombieSun;
    /** ثانیه‌های باقی‌مانده تا پایانِ مسابقه. */
    public int versusSecondsLeft;
    /** نقشِ برنده وقتی تمام شد: "PLANT"/"ZOMBIE"؛ خالی یعنی ادامه دارد. */
    public String versusWinnerRole = "";

    // ─── مینی‌گیم امتیازیِ ترکیب سه‌تایی (Beghouled) ────────────────────────────
    /** آیا مرحله BEGHOULED است (بازی روی مپِ واقعی). */
    public boolean beghouledActive;
    /** تعداد ترکیب‌های ساخته‌شده و هدفِ لازم برای برد. */
    public int beghouledMatches;
    public int beghouledTarget;
    /** حفره‌ها (crater) — [col0based][row0based] true اگر خانه crater شده. */
    public boolean[][] craters = new boolean[GameConstants.TILE_COLS][GameConstants.TILE_ROWS];

    // ─── شبکه ────────────────────────────────────────────────────────────────
    /** [col0based][row0based] */
    public TileType[][] tiles = new TileType[GameConstants.TILE_COLS][GameConstants.TILE_ROWS];
    /** کسرِ آسیبِ سنگ‌قبرِ هر خانه: ۰=سالم … ۱=نزدیکِ نابودی (برای ۵ حالتِ ترک‌خوردگی). فقط برای خانه‌های TOMBSTONE معنی دارد. */
    public float[][] tombstoneDamage = new float[GameConstants.TILE_COLS][GameConstants.TILE_ROWS];
    /** جایزه‌ی سنگ‌قبرِ هر خانه: 0=هیچ، 1=خورشید(DARK_SUN)، 2=غذای گیاه(DARK_PLANTFOOD). */
    public int[][] tombstoneReward = new int[GameConstants.TILE_COLS][GameConstants.TILE_ROWS];
    /** مراتب 0-based — true = هنوز فعال */
    public boolean[] lawnMowerActive = new boolean[GameConstants.TILE_ROWS];
    /** چمن‌زن‌های متحرکِ فعال: هر عنصر {row(۱-based), x(ستونِ اعشاری)}. */
    public List<double[]> movingMowers = new ArrayList<>();

    // ─── اعلان‌های الگوی امتیازی (Scored / MeoPoint patterns) ─────────────────
    /** اعلان‌های الگوی میوپوینت که این فریم رخ داده‌اند (drain شده از session). */
    public List<String> meoEvents = new ArrayList<>();
    /** توست‌های «جمع‌آوری» (سکه/الماس/گلدان/غذای گیاه از زامبی) این فریم. */
    public List<String> collectEvents = new ArrayList<>();
    /** گردبادهایِ تازه (مصر، موجِ نهایی): هر عنصر {col,row}ِ محلِ فرود. */
    public List<int[]> tornadoDrops = new ArrayList<>();
    /** افکت‌های تمام‌صفحه‌ی تازه‌ی این فریم (مثلِ "iceshroom"). */
    public List<String> screenEffects = new ArrayList<>();
    /** خانه‌های تازه‌سوخته‌ی این فریم (آتشِ اژدهای فصلِ تاریک) — {col,row}. یک‌بارمصرف. */
    public List<int[]> scorchedTiles = new ArrayList<>();
    /** انفجارهایِ تازه‌ی این فریم: هر عنصر {col,row} — برای لرزش + جلوه‌ی انفجار. */
    public List<int[]> explosionEvents = new ArrayList<>();
    /** پرتابه‌های اختاپوسِ تازه: هر عنصر {srcCol,srcRow,tgtCol,tgtRow}. */
    public List<double[]> octopusTosses = new ArrayList<>();
    /** لیزرهای تورکوایزِ تازه‌ی این فریم: هر عنصر {row, nearCol, farCol}. یک‌بارمصرف. */
    public List<int[]> laserZaps = new ArrayList<>();

    // ─── مرحله‌ی رئیس (Zomboss) ─────────────────────────────────────────────────
    /** آیا رئیس فعال است (فقط در مراحلِ BOSS). */
    public boolean bossActive;
    /** ستونِ شناورِ رئیس (۱-based، اعشاری). */
    public double bossPhase1X;
    /** بالاترین لِینِ اشغال‌شده‌ی رئیس (۱-based). */
    public int bossLane;
    /** تعدادِ سطرهایی که رئیس اشغال می‌کند (۲ معمولی، همه برای ماموتِ یخ). */
    public int bossLaneSpan = 2;
    /** کسرِ کلِ سلامتی (۰..۱). */
    public float bossHealthFraction;
    /** تعدادِ بخش‌های سلامتیِ باقی‌مانده (۰..۳). */
    public int bossSegmentsRemaining;
    /** کسرِ پُرشدنِ بخشِ در حالِ تخلیه (۰..۱). */
    public float bossSegmentFraction;
    /** فازِ فعلی (۱..۳). */
    public int bossPhase;
    /** فصلِ رئیس (برای انتخابِ PAM/افکتِ اختصاصی): ancient_egypt|frostbite_caves|... */
    public String bossChapter = "";
    /** حالتِ معناییِ رئیس: entering|idle|moving|attacking|summoning|dying|dead. */
    public String bossState = "idle";
    /** توانایی در حالِ اجرا (برای کلیپِ حمله): egypt_missile|egypt_charge|ice_wind|... */
    public String bossAbility = "";
    /** جابه‌جاییِ افقیِ شارژ (ستون؛ منفی=به‌جلو) — لومیدنِ رئیس. */
    public float bossChargeOffset;
    /** زمانِ سپری‌شده در حالتِ فعلی (ثانیه) — برای پخشِ کلیپ. */
    public float bossStateTime;
    /** آیا رئیس شکست خورده (برای overlay برد). */
    public boolean bossDefeated;
    /** حملاتِ در حالِ اجرای رئیس. */
    public List<BossAttackInfo> bossAttacks = new ArrayList<>();

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
        /** مرحله‌ی بلاکِ یخ روی گیاه: ۱-=بدونِ بلاک، ۰=کامل(total)، ۱..۴=ذوب‌شونده(damage1..4). */
        public int iceBlockStage = -1;
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
        /** شناسه‌ی یکتای شبکه — برای تطبیقِ زامبیِ آینه‌ای در مالتی‌پلیر (فاز ۳). */
        public int netId;
        /** موقعیت x اعشاری، ۱-based */
        public double phase1X;
        /** ردیف int، ۱-based */
        public int phase1Y;
        public float hp, maxHp;
        public List<ArmorInfo> armors = new ArrayList<>();
        /** نام‌های ZombieEffect به lowercase: frozen, chilled, … */
        public List<String> effects = new ArrayList<>();
        /** مرحله‌ی بلاکِ یخ روی زامبی: ۱-=بدونِ بلاک، ۰=کامل(total)، ۱..۵=ذوب‌شونده(damage1..5). */
        public int iceBlockStage = -1;
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
        public double startPhase1X; // موقعیت شلیک — برای سهمی پیوسته
        public int    phase1Y;    // ردیف ۱-based
        public boolean movingRight;
        public boolean isArc;
        public int    targetPhase1X, targetPhase1Y; // برای arc
    }

    // ─── کوزه ────────────────────────────────────────────────────────────────
    public static class VaseInfo {
        /** مختصات ۱-based */
        public int col, row;
        /** plant | gargantuar | random — تعیین‌کننده‌ی رنگ ظاهری کوزه */
        public String kind;
    }

    // ─── کارت زامبیِ من‌زامبی ───────────────────────────────────────────────────
    public static class ZombieCardInfo {
        /** نام ZombieType به lowercase */
        public String type;
        public int    cost;
        /** ۰..۱ — چه مقدار از cooldown مانده */
        public float  cooldownFraction;
        /** آیا خورشیدِ کافی برای کاشت وجود دارد */
        public boolean affordable;
    }

    // ─── گردوی بولینگ ──────────────────────────────────────────────────────────
    public static class BowlingBallInfo {
        /** x اعشاری ۱-based، y ردیف ۱-based */
        public double x;
        public int    y;
        /** wallnut_bowling | big_wallnut | explode_o_nut_bowling */
        public String type;
    }

    // ─── حمله‌ی رئیس ─────────────────────────────────────────────────────────
    public static class BossAttackInfo {
        /** نوعِ توانایی: egypt_missile|egypt_charge|ice_missile|ice_wind|
         *  ice_freeze_column|beach_shark|beach_turbine|dark_fireball */
        public String type;
        /** مختصاتِ هدف ۱-based */
        public double targetCol;
        public int    targetRow;
        /** سطرِ دومِ هدف (توانایی‌های ۲-سطری)؛ ۱- اگر ندارد. */
        public int    targetRow2 = -1;
        /** یخ‌زدنِ ستون: کلِ ستونِ targetCol هدف است. */
        public boolean wholeColumn;
        /** incoming | impact */
        public String phase;
        /** ۰..۱ — پیشرفتِ سقوط/تلگراف */
        public float  progress;
    }

    // ─── آفتاب ───────────────────────────────────────────────────────────────
    public static class SunInfo {
        public int   phase1X, phase1Y;
        public int   value;
        public String sunType;     // normal|special|radioactive
        public boolean isLanded;
        public float  fallProgress; // ۰..۱
        // دزدیده‌شدن توسطِ Ra: بنفش‌شدن و کشیده‌شدن به‌سمتِ چوبِ Ra.
        public boolean beingStolen;
        public float   stealProgress; // ۰..۱
        public int     stealerCol, stealerRow;
    }
}
