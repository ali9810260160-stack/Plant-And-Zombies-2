package com.pvz2.service;

import com.pvz2.model.*;
import com.pvz2.model.AppState;
import com.pvz2.model.User;
import com.pvz2.model.enums.*;
import com.pvz2.model.plants.Plant;
import com.pvz2.model.plants.PlantFactory;
import com.pvz2.model.plants.PeaPodPlant;
import com.pvz2.model.tiles.Tile;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.util.RandomUtil;
import com.pvz2.view.ConsoleView;
import com.pvz2.view.MapView;

import java.util.ArrayList;
import java.util.List;

/**
 * سرویس اصلی بازی.
 *
 * پیاده‌سازی کامل هر ۸ نوع مرحله ویژه:
 *  1. CONVEYOR_BELT  — نوار کناری (12 ثانیه یک گیاه)
 *  2. LOCKED_PLANTS  — گیاهان زندانی
 *  3. SAVE_OUR_SEEDS — محافظ دانه‌ها (گیاهان از پیش کاشته‌شده)
 *  4. TIMED_WAR      — نبرد زماندار
 *  5. NIGHT_OPS      — شب عملیات (SunService block)
 *  6. DEAD_LINE      — ددلاین (CombatService check)
 *  7. LOVE_YOUR_PLANTS — از دست نده
 *  8. PLANT_WHAT_YOU_GET — هر چه رسد بکار
 */
public class GameService {

    private final WaveService waveService;
    private CombatService combatService;
    private final SunService sunService;
    private final BossService bossService;
    private final ConsoleView view;
    private final MapView mapView;

    /**
     * گیاهانی که در نوار کناری ظاهر می‌شوند
     * (زیرمجموعه‌ای از گیاهان معمول که برای Conveyor Belt مناسب‌اند).
     */
    private static final PlantType[] CONVEYOR_PLANT_POOL = {
        PlantType.SUNFLOWER, PlantType.PEASHOOTER, PlantType.SNOW_PEA,
        PlantType.WALL_NUT, PlantType.CHERRY_BOMB, PlantType.POTATO_MINE,
        PlantType.REPEATER, PlantType.MELON_PULT, PlantType.CHOMPER,
        PlantType.TORCHWOOD, PlantType.CABBAGE_PULT
    };

    /** استخر گردوهای «بولینگ گردویی» که از نوار کناری تحویل می‌شوند. */
    private static final PlantType[] BOWLING_NUT_POOL = {
        PlantType.WALLNUT_BOWLING, PlantType.WALLNUT_BOWLING,
        PlantType.WALLNUT_BOWLING, PlantType.EXPLODE_O_NUT_BOWLING,
        PlantType.BIG_WALLNUT
    };

    /** حداکثر ستونی که می‌توان گردوی بولینگ را در آن گذاشت (سمت چپ زمین). */
    private static final int BOWLING_PLACE_LIMIT = 3;
    /** ظرفیت صف نوار کناری (مطابق UI: حداکثر ۵). */
    private static final int CONVEYOR_CAP = 5;

    public GameService(WaveService waveService, CombatService combatService,
                       SunService sunService, ConsoleView view, MapView mapView) {
        this.waveService = waveService;
        this.combatService = combatService;
        this.sunService = sunService;
        this.bossService = new BossService(view);
        this.view = view;
        this.mapView = mapView;
    }

    public void setCombatServiceRef(CombatService cs) {
        this.combatService = cs;
    }

    // ════════════════════════════════════════════════════════
    //  SESSION CREATION
    // ════════════════════════════════════════════════════════

    public GameSession createSession(Level level, List<PlantType> selectedPlants) {
        GameMap map = new GameMap(level.getMapRows(), level.getMapCols(),
                level.getChapter());
        setupMapForChapter(map, level);
        GameSession session = new GameSession(level, map, selectedPlants);
        List<Wave> waves = waveService.generateWaves(level);
        session.setWaves(waves);
        applyBoosts(session);
        applyLevelTypeSetup(session, level);
        return session;
    }

    // ════════════════════════════════════════════════════════
    //  MAP SETUP PER CHAPTER
    // ════════════════════════════════════════════════════════

    private void setupMapForChapter(GameMap map, Level level) {
        switch (level.getChapter()) {
            case ANCIENT_EGYPT:   setupEgyptTombstones(map); break;
            case FROSTBITE_CAVES: setupFrostbiteTiles(map);  break;
            case BIG_WAVE_BEACH:  setupBeachWater(map);      break;
            case DARK_AGES:       setupDarkAges(map);        break;
            default: break;
        }
    }

    private void setupEgyptTombstones(GameMap map) {
        int count = RandomUtil.between(2, 5);
        for (int i = 0; i < count; i++) {
            int col = RandomUtil.between(3, map.getCols() - 1);
            int row = RandomUtil.between(1, map.getRows());
            Tile t = map.getTile(col, row);
            if (t != null && t.isPlantable())
                map.setTileType(col, row, TileType.TOMBSTONE);
        }
    }

    private void setupFrostbiteTiles(GameMap map) {
        int count = RandomUtil.between(1, 3);
        for (int i = 0; i < count; i++) {
            int col = RandomUtil.between(2, map.getCols() - 1);
            int row = RandomUtil.between(1, map.getRows());
            TileType type = RandomUtil.chance(0.5)
                    ? TileType.SLIPPERY_UP : TileType.SLIPPERY_DOWN;
            map.setTileType(col, row, type);
        }
    }

    private void setupBeachWater(GameMap map) {
        int waterCols = RandomUtil.between(2, 4);
        map.setBeachMaxWaterCols(waterCols);   // EK3: مرزِ ثابتِ جزرومد
        for (int col = map.getCols() - waterCols + 1;
             col <= map.getCols(); col++) {
            for (int row = 1; row <= map.getRows(); row++) {
                map.setTileType(col, row, TileType.WATER);
            }
        }
    }

    // ────────────────────────────────────────────────────────────
    //  EK3: تغییرِ سطحِ آبِ ساحل در حینِ مرحله (جزرومدِ گام‌به‌گام)
    // ────────────────────────────────────────────────────────────

    /** هر چند ثانیه یک‌بار، سطحِ آب یک ستون بالا/پایین می‌رود (بین ۱ و max). */
    private static final int TIDE_STEP_TICKS = 150;

    private void tickBeachTide(GameSession session) {
        GameMap map = session.getGameMap();
        int maxCols = map.getBeachMaxWaterCols();
        if (maxCols <= 1) return;                       // ساحل نیست یا جای نوسان ندارد
        if (session.getCurrentTick() % TIDE_STEP_TICKS != 0) return;

        int minCols = 1;
        int span    = maxCols - minCols;                // دامنه‌ی نوسان
        int cycle   = 2 * span;                         // بالا رفتن + پایین آمدن
        int stepIdx = (session.getCurrentTick() / TIDE_STEP_TICKS) % cycle;
        int waterCols = stepIdx <= span ? (minCols + stepIdx) : (minCols + cycle - stepIdx);

        int cols = map.getCols();
        int rows = map.getRows();
        for (int i = 0; i < maxCols; i++) {
            int col = cols - i;                         // از راست‌ترین ستون
            boolean isWater = i < waterCols;            // راست‌ترین waterCols ستون = آب
            for (int row = 1; row <= rows; row++) {
                Tile t = map.getTile(col, row);
                if (t == null || t.getPlant() != null) continue;   // کاشیِ دارای گیاه دست‌نخورده
                map.setTileType(col, row, isWater ? TileType.WATER : TileType.LOW_TIDE);
            }
        }
    }

    private void setupDarkAges(GameMap map) {
        int necroCount = RandomUtil.between(1, 3);
        for (int i = 0; i < necroCount; i++) {
            int col = RandomUtil.between(4, map.getCols() - 1);
            int row = RandomUtil.between(1, map.getRows());
            map.setTileType(col, row, TileType.NECROMANCY);
        }
    }

    // ════════════════════════════════════════════════════════
    //  SPECIAL LEVEL SETUP
    // ════════════════════════════════════════════════════════

    private void applyLevelTypeSetup(GameSession session, Level level) {
        switch (level.getLevelType()) {

            // ──────────────────────────────────────────────────
            //  1. CONVEYOR_BELT: اولین گیاه فوری اضافه می‌شود
            // ──────────────────────────────────────────────────
            case CONVEYOR_BELT:
                session.setSunAmount(0); // خورشید اولیه ندارد
                pushConveyorPlant(session);
                view.printRaw("\u001B[33m📦 Conveyor Belt Level: plants will "
                        + "arrive every 12 seconds. Use 'plant plant -t <TYPE>'"
                        + " to plant the available conveyor plant.\u001B[0m");
                view.printRaw("\u001B[33m📦 Current conveyor: "
                        + session.getConveyorQueue() + "\u001B[0m");
                break;

            // ──────────────────────────────────────────────────
            //  2. LOCKED_PLANTS: اطلاع‌رسانی گیاهان قفل‌شده
            // ──────────────────────────────────────────────────
            case LOCKED_PLANTS:
                if (level.getForcedLockedSlots() != null
                        && !level.getForcedLockedSlots().isEmpty()) {
                    view.printRaw("\u001B[33m🔒 Locked Plants Level! "
                            + "The following plants are LOCKED in this level:\u001B[0m");
                    for (PlantType t : level.getForcedLockedSlots()) {
                        view.printRaw("\u001B[31m   ✗ " + t.name() + "\u001B[0m");
                    }
                }
                break;

            // ──────────────────────────────────────────────────
            //  3. SAVE_OUR_SEEDS: کاشت گیاهان محافظت‌شده
            // ──────────────────────────────────────────────────
            case SAVE_OUR_SEEDS:
                setupSaveOurSeeds(session, level);
                break;

            // ──────────────────────────────────────────────────
            //  4. TIMED_WAR: اعلام هدف و مدت
            // ──────────────────────────────────────────────────
            case TIMED_WAR:
                session.setWaveStarted(true);
                printTimedWarObjective(level);
                break;

            // ──────────────────────────────────────────────────
            //  5. NIGHT_OPS: خورشید اولیه (بدون آسمانی)
            // ──────────────────────────────────────────────────
            case NIGHT_OPS:
                int nightSun = level.getNightOpsSun() > 0 ? level.getNightOpsSun() : 150;
                session.setSunAmount(nightSun);
                view.printRaw("\u001B[34m🌙 Night Ops! No sun falls from the sky."
                        + " Starting sun: " + nightSun + "\u001B[0m");
                break;

            // ──────────────────────────────────────────────────
            //  6. DEAD_LINE: نمایش ستون ممنوع
            // ──────────────────────────────────────────────────
            case DEAD_LINE:
                view.printRaw("\u001B[31m🚫 DEAD LINE at column "
                        + level.getDeadLineColumn()
                        + "! If any zombie passes this column, you LOSE!\u001B[0m");
                break;

            // ──────────────────────────────────────────────────
            //  7. LOVE_YOUR_PLANTS: کاشت گیاهان محافظت‌شده + اعلام
            // ──────────────────────────────────────────────────
            case LOVE_YOUR_PLANTS:
                setupLoveYourPlants(session, level);
                view.printRaw("\u001B[35m💚 Love Your Plants! "
                        + "You can lose at most " + level.getMaxPlantsLost()
                        + " plants.\u001B[0m");
                break;

            // ──────────────────────────────────────────────────
            //  8. PLANT_WHAT_YOU_GET: خورشید ثابت، بدون امواج اولیه
            // ──────────────────────────────────────────────────
            case PLANT_WHAT_YOU_GET:
                int sun = level.getInitialSunAmount() > 0
                        ? level.getInitialSunAmount() : 500;
                session.setSunAmount(sun);
                session.setWaveStarted(false);
                view.printRaw("\u001B[33m🌿 Plant What You Get!\n"
                        + "  Starting sun: " + sun + " (no more from sky)\n"
                        + "  Sunflower family plants are NOT available\n"
                        + "  Plant freely with NO cooldown, then type:"
                        + " 'start zombie waves'\u001B[0m");
                break;

            // ──────────────────────────────────────────────────
            //  مرحله‌ی رئیس (Zomboss) — امواجِ عادی ندارد؛ خودِ رئیس زامبی
            //  اسپاون می‌کند و بین لِین‌ها حمله می‌کند. بردن = تخلیه‌ی هر ۳ بخشِ
            //  سلامتیِ رئیس. نگاه کنید {@link BossService} و {@link CombatService}.
            // ──────────────────────────────────────────────────
            case BOSS:
                session.setWaves(new ArrayList<>());
                session.setWaveStarted(false);
                if (session.getSunAmount() < 300) session.setSunAmount(300);
                bossService.setup(session);
                break;

            // ──────────────────────────────────────────────────
            //  مینی‌گیم: کوزه‌شکنی — بدون موج/خورشید؛ کوزه‌ها ساخته می‌شوند
            // ──────────────────────────────────────────────────
            case VASEBREAKER:
                session.setSunAmount(0);
                session.setWaveStarted(false);
                session.setWaves(new ArrayList<>());
                setupVasebreaker(session, level);
                break;

            // ──────────────────────────────────────────────────
            //  مینی‌گیم: بولینگ گردویی — امواج عادی + تحویل گردو با نوار
            // ──────────────────────────────────────────────────
            case WALLNUT_BOWLING:
                session.setSunAmount(0);
                session.setWaveStarted(true);
                session.setMinigameState(new com.pvz2.model.BowlingState());
                // چند گردوی اولیه روی نوار قرار بده
                for (int i = 0; i < 3; i++) pushConveyorPlant(session);
                view.printRaw("🎳 Wallnut Bowling! Place nuts in the left columns; "
                        + "they roll right and crush zombies.");
                break;

            // ──────────────────────────────────────────────────
            //  مینی‌گیم: من زامبی — بازیِ معکوس (بازیکن زامبی می‌کارد)
            // ──────────────────────────────────────────────────
            case I_ZOMBIE:
                session.setWaveStarted(false);
                session.setWaves(new ArrayList<>());
                session.setSunAmount(175);
                setupIZombie(session, level);
                view.printRaw("🧟 I, Zombie! Place zombies on the right; walk them "
                        + "left past the plants to eat the brains.");
                break;

            // ──────────────────────────────────────────────────
            //  VERSUS (فاز ۳): I, Zombie دونفره — گیاه‌کار می‌کارد، حریف زامبی می‌گذارد
            // ──────────────────────────────────────────────────
            case VERSUS:
                setupVersus(session);
                break;

            // ──────────────────────────────────────────────────
            //  مینی‌گیم امتیازی: زامبی‌های گیاهی (Zombotany) — دفاعِ عادی با
            //  زامبی‌های هیبریدی؛ و بازی امتیازی (Scored) — الگوهای میوپوینت
            // ──────────────────────────────────────────────────
            case ZOMBOTANY:
            case SCORED:
                session.setWaveStarted(true);
                break;

            // ──────────────────────────────────────────────────
            //  مینی‌گیم امتیازی: Beghouled — match-3 روی مپِ واقعی
            // ──────────────────────────────────────────────────
            case BEGHOULED:
                // موجِ عادی نداریم؛ زامبی‌ها را خودمان بی‌پایان spawn می‌کنیم
                // (تا بردِ موج‌محور فعال نشود). خورشید فقط از ترکیب‌ها می‌آید.
                session.setWaveStarted(false);
                session.setWaves(new ArrayList<>());
                session.setSunAmount(0);
                setupBeghouled(session);
                view.printRaw("💎 Beghouled! Swap adjacent plants to make matches of 3+.");
                break;

            // DARK_AGES (فصل) — بدون خورشید آسمانی، اعلام
            default:
                if (level.getChapter() == ChapterType.DARK_AGES) {
                    session.setSunAmount(50);
                }
                break;
        }

        // اطمینان از شروعِ امواج در همه‌ی مراحلِ دفاعی (NORMAL/BOSS/LOCKED/
        // SAVE_OUR_SEEDS/NIGHT_OPS/DEAD_LINE/LOVE_YOUR_PLANTS/CONVEYOR…).
        // فقط این سه استثنا هستند: PLANT_WHAT_YOU_GET (فاز چیدنِ اولیه، دستی
        // شروع می‌شود)، VASEBREAKER و I_ZOMBIE (موجی ندارند).
        LevelType lt = level.getLevelType();
        if (lt != LevelType.PLANT_WHAT_YOU_GET
                && lt != LevelType.VASEBREAKER
                && lt != LevelType.I_ZOMBIE
                && lt != LevelType.BEGHOULED   // زامبی‌ها را خودمان بی‌پایان spawn می‌کنیم
                && lt != LevelType.BOSS) {   // رئیس موجِ عادی ندارد؛ خودش اسپاون می‌کند
            session.setWaveStarted(true);
        }
    }

    // ════════════════════════════════════════════════════════
    //  VASEBREAKER — تولید کوزه‌ها
    // ════════════════════════════════════════════════════════

    private void setupVasebreaker(GameSession session, Level level) {
        int rows = session.getGameMap().getRows();
        int cols = session.getGameMap().getCols();
        int lvl  = Math.max(1, Math.min(3, level.getLevelNumber() > 0
                ? level.getLevelNumber() : session.getLevelNumber()));
        // تعداد ستون کوزه با سطح زیاد می‌شود؛ ستون‌های چپ برای دفاع خالی می‌مانند.
        int vaseCols = Math.min(cols, 2 + 2 * lvl);
        int firstVaseCol = Math.max(2, cols - vaseCols + 1);
        VasebreakerState state = new VasebreakerState(lvl, rows, firstVaseCol, cols);
        session.setMinigameState(state);
        view.printRaw("[33m🏺 Vasebreaker! Break the vases (click) — some hide "
                + "zombies, some hide plants you can then place.[0m");
    }

    /**
     * شکستن یک کوزه: زامبی آزاد می‌کند یا بذر گیاه به انبار اضافه می‌کند.
     * توسط لایه گرافیک (کلیک روی کوزه) از طریق GameFacade فراخوانی می‌شود.
     */
    public void breakVase(GameSession session, int col, int row) {
        if (session.getLevel().getLevelType() != LevelType.VASEBREAKER)
            throw new com.pvz2.exception.GameException("Not a Vasebreaker level.");
        Object ms = session.getMinigameState();
        if (!(ms instanceof VasebreakerState))
            throw new com.pvz2.exception.GameException("Vasebreaker not initialized.");
        VasebreakerState state = (VasebreakerState) ms;
        VasebreakerState.Vase vase = state.unbrokenVaseAt(col, row);
        if (vase == null)
            throw new com.pvz2.exception.GameException(
                    "No vase to break at (" + col + ", " + row + ").");
        vase.broken = true;
        if (vase.zombieContent != null) {
            Zombie z = com.pvz2.model.zombies.ZombieFactory.create(vase.zombieContent);
            if (z != null) {
                z.setX(col);
                z.setY(row);
                z.setLane(row);
                session.getActiveZombies().add(z);
            }
            view.printRaw("[31m🧟 A " + vase.zombieContent.name()
                    + " emerged from the vase![0m");
        } else if (vase.plantContent != null) {
            state.addSeed(vase.plantContent);
            view.printSuccess("🌱 Got a " + vase.plantContent.name()
                    + " seed! Select it below and plant it.");
        } else {
            view.printRaw("[37m💨 The vase was empty.[0m");
        }
    }

    // ════════════════════════════════════════════════════════
    //  WALLNUT BOWLING — گذاشتن گردو و حرکت/برخورد آن
    // ════════════════════════════════════════════════════════

    private void placeBowlingNut(GameSession session, PlantType type, int x, int y) {
        if (!com.pvz2.model.BowlingState.isBowlingNut(type))
            throw new com.pvz2.exception.GameException(
                    "Only bowling nuts can be placed here.");
        if (!session.hasConveyorPlant(type))
            throw new com.pvz2.exception.GameException(
                    type.name() + " is not available on the conveyor.");
        if (x < 1 || x > BOWLING_PLACE_LIMIT)
            throw new com.pvz2.exception.GameException(
                    "Place nuts in columns 1-" + BOWLING_PLACE_LIMIT + ".");
        if (y < 1 || y > session.getGameMap().getRows())
            throw new com.pvz2.exception.GameException("Invalid row: " + y);
        Object ms = session.getMinigameState();
        com.pvz2.model.BowlingState st = (ms instanceof com.pvz2.model.BowlingState)
                ? (com.pvz2.model.BowlingState) ms : null;
        if (st == null) { st = new com.pvz2.model.BowlingState(); session.setMinigameState(st); }
        st.add(new com.pvz2.model.BowlingState.Ball(type, x, y));
        session.useConveyorPlant(type);
    }

    /** حرکت و برخورد گردوها با زامبی‌های واقعی — هر تیک. */
    private void tickBowling(GameSession session) {
        Object ms = session.getMinigameState();
        if (!(ms instanceof com.pvz2.model.BowlingState)) return;
        com.pvz2.model.BowlingState st = (com.pvz2.model.BowlingState) ms;
        int rows = session.getGameMap().getRows();
        int cols = session.getGameMap().getCols();
        java.util.Iterator<com.pvz2.model.BowlingState.Ball> it = st.getBalls().iterator();
        while (it.hasNext()) {
            com.pvz2.model.BowlingState.Ball ball = it.next();
            ball.x += ball.dirX * 0.5;
            int newY = ball.y + ball.dirY;
            if (newY < 1 || newY > rows) { ball.dirY = -ball.dirY; newY = ball.y; }
            ball.y = newY;
            if (ball.x > cols + 1 || ball.x < 0) { it.remove(); continue; }

            Zombie hit = null;
            for (Zombie z : session.getActiveZombies()) {
                if (z.isAlive() && z.getY() == ball.y
                        && Math.abs(z.getX() - ball.x) < 0.7) { hit = z; break; }
            }
            if (hit == null) continue;

            if (ball.type == PlantType.EXPLODE_O_NUT_BOWLING) {
                explodeBowlingArea(session, ball);
                it.remove();
            } else {
                hit.takeDamage(ball.damage());
                // گردوی عادی پس از برخورد ۴۵ درجه منحرف می‌شود؛ بزرگ مستقیم می‌راند.
                if (ball.type == PlantType.WALLNUT_BOWLING) rotateBowlingBall45(ball);
            }
        }
    }

    private void rotateBowlingBall45(com.pvz2.model.BowlingState.Ball ball) {
        if (ball.dirY == 0) {
            ball.dirY = RandomUtil.chance(0.5) ? 1 : -1;   // شروع حرکت مورب
        } else {
            ball.dirX = ball.dirY;                          // بازگشت به حرکت افقی
            ball.dirY = 0;
        }
    }

    private void explodeBowlingArea(GameSession session, com.pvz2.model.BowlingState.Ball ball) {
        int cx = (int) Math.round(ball.x);
        int cy = ball.y;
        for (Zombie z : session.getActiveZombies()) {
            if (!z.isAlive()) continue;
            if (Math.abs(z.getX() - cx) <= 1.2 && Math.abs(z.getY() - cy) <= 1) {
                z.takeDamage(180);
            }
        }
    }

    // ════════════════════════════════════════════════════════
    //  I, ZOMBIE — بازیِ معکوس
    // ════════════════════════════════════════════════════════

    // ════════════════════════════════════════════════════════
    //  VERSUS — I, Zombie دونفره‌ی تحت شبکه (فاز ۳)
    // ════════════════════════════════════════════════════════

    /** ثانیه‌های مسابقه (بردِ گیاه با اتمامِ زمان)؛ قابلِ تنظیم طبق داک. */
    public static final int VERSUS_SECONDS = 120;
    /** خورشیدِ آغازینِ گیاه‌کار و زامبی‌گذار. */
    private static final int VERSUS_PLANT_START_SUN = 100;
    private static final int VERSUS_ZOMBIE_START_SUN = 175;
    /** بازآمدِ خورشیدِ زامبی: هر ۵۰ تیک (۵ ثانیه) این مقدار اضافه می‌شود. */
    private static final int VERSUS_ZOMBIE_SUN_PER_5S = 50;

    private void setupVersus(GameSession session) {
        session.setWaveStarted(false);
        session.setWaves(new ArrayList<>());
        session.setSunAmount(VERSUS_PLANT_START_SUN);   // خورشیدِ گیاه‌کار (کاشت)
        session.setZombieSun(VERSUS_ZOMBIE_START_SUN);  // خورشیدِ زامبی‌گذار (جدا)
        session.setVersusTicksLeft(VERSUS_SECONDS * 10);
        session.setVersusWinnerRole(null);
        int rows = session.getGameMap().getRows();
        // برای مغزها و roster زامبی از همان IZombieState استفاده می‌کنیم؛ ولی
        // برخلافِ تک‌نفره، مدافع پیش‌کاشته نداریم (گیاه‌کار خودش می‌کارد). roster
        // «قطعی» است تا میزبان و مهمان (دو جلسه‌ی جدا) دقیقاً یکسان باشند.
        session.setMinigameState(new com.pvz2.model.IZombieState(rows, true));
        boolean[] mowers = session.getGameMap().getLawnMowers();
        if (mowers != null) java.util.Arrays.fill(mowers, false);
    }

    /** هر تیکِ VERSUS: تایمر + بازآمدِ خورشیدِ زامبی. */
    private void tickVersus(GameSession session) {
        int left = session.getVersusTicksLeft();
        if (left > 0) session.setVersusTicksLeft(left - 1);
        if (session.getCurrentTick() % 50 == 0) {
            session.addZombieSun(VERSUS_ZOMBIE_SUN_PER_5S);
        }
    }

    private void setupIZombie(GameSession session, Level level) {
        int rows = session.getGameMap().getRows();
        session.setMinigameState(new com.pvz2.model.IZombieState(rows));
        // در «من زامبی» به‌جای چمن‌زن، مغز انتهای هر ردیف است (بردِ مغزمحور) —
        // پس چمن‌زن‌های پیش‌فرضِ نقشه را پاک می‌کنیم تا نه دیده شوند و نه دخالت کنند.
        boolean[] mowers = session.getGameMap().getLawnMowers();
        if (mowers != null) java.util.Arrays.fill(mowers, false);
        prePlaceDefenders(session, level);
    }

    /** کاشتِ گیاهانِ مدافع (که به زامبی‌های بازیکن شلیک می‌کنند). */
    private void prePlaceDefenders(GameSession session, Level level) {
        int rows = session.getGameMap().getRows();
        int lvl  = Math.max(1, Math.min(3, level.getLevelNumber()));
        // پیش‌فرض: در هر ردیف یک Peashooter؛ سطوح بالاتر دفاع بیشتری دارند.
        for (int r = 1; r <= rows; r++) {
            placeDefender(session, PlantType.PEASHOOTER, 4, r);
            if (lvl >= 2 && (r % 2 == 1)) placeDefender(session, PlantType.WALL_NUT, 5, r);
            if (lvl >= 2) placeDefender(session, PlantType.PEASHOOTER, 3, r);
            if (lvl >= 3) placeDefender(session, PlantType.SNOW_PEA, 2, r);
        }
    }

    private void placeDefender(GameSession session, PlantType type, int x, int y) {
        Tile tile = session.getGameMap().getTile(x, y);
        if (tile == null || !tile.isPlantable() || tile.getPlant() != null) return;
        Plant plant = PlantFactory.create(type);
        if (plant == null) return;
        plant.setX(x);
        plant.setY(y);
        tile.setPlant(plant);
    }

    /**
     * کاشتِ یک زامبی توسط بازیکن (کلیک روی خانه‌ی سمت راست). زامبی از موتور
     * مبارزه‌ی موجود استفاده می‌کند و خودکار به چپ حرکت می‌کند.
     */
    public void placeZombie(GameSession session, com.pvz2.model.enums.ZombieType type,
                            int x, int y) {
        LevelType lt = session.getLevel().getLevelType();
        boolean versus = lt == LevelType.VERSUS;
        if (lt != LevelType.I_ZOMBIE && !versus)
            throw new com.pvz2.exception.GameException("Not an I, Zombie level.");
        Object ms = session.getMinigameState();
        if (!(ms instanceof com.pvz2.model.IZombieState))
            throw new com.pvz2.exception.GameException("I, Zombie not initialized.");
        com.pvz2.model.IZombieState st = (com.pvz2.model.IZombieState) ms;
        if (!st.isPlaceable(type))
            throw new com.pvz2.exception.GameException(type.name() + " cannot be placed.");
        int rows = session.getGameMap().getRows();
        int cols = session.getGameMap().getCols();
        if (y < 1 || y > rows)
            throw new com.pvz2.exception.GameException("Invalid row: " + y);
        int minCol = Math.max(1, cols - 2);
        if (x < minCol || x > cols)
            throw new com.pvz2.exception.GameException(
                    "Place zombies in the right columns (" + minCol + "-" + cols + ").");
        if (!st.isReady(type, session.getCurrentTick()))
            throw new com.pvz2.exception.GameException(type.name() + " is on cooldown.");
        int cost = st.costOf(type);
        // VERSUS: خورشیدِ زامبی جدا از خورشیدِ گیاه‌کار است.
        int have = versus ? session.getZombieSun() : session.getSunAmount();
        if (have < cost)
            throw new com.pvz2.exception.GameException(
                    "Not enough sun. Need " + cost + ", have " + have + ".");
        if (versus) session.spendZombieSun(cost); else session.spendSun(cost);
        Zombie z = com.pvz2.model.zombies.ZombieFactory.create(type);
        if (z == null)
            throw new com.pvz2.exception.GameException("Unknown zombie: " + type.name());
        z.setX(x);
        z.setY(y);
        z.setLane(y);
        session.getActiveZombies().add(z);
        st.startCooldown(type, session.getCurrentTick());
    }

    // ════════════════════════════════════════════════════════
    //  BEGHOULED — match-3 روی مپِ واقعی
    // ════════════════════════════════════════════════════════

    private static final int BEGHOULED_TARGET = 20;   // تعداد ترکیبِ لازم برای برد
    private static final int BEGHOULED_SPAWN_INTERVAL = 260; // تیک بین اسپاونِ زامبی (~۲۶ ثانیه)

    private void setupBeghouled(GameSession session) {
        com.pvz2.model.BeghouledState state =
                new com.pvz2.model.BeghouledState(BEGHOULED_TARGET);
        session.setMinigameState(state);
        fillBeghouledBoard(session);
        // تضمینِ اینکه زمینِ اولیه حداقل یک حرکتِ ممکن دارد.
        if (!hasPossibleMove(session)) resetBeghouledBoard(session);
        // در Beghouled چمن‌زن نداریم (بازی روی خودِ زمین است).
        boolean[] mowers = session.getGameMap().getLawnMowers();
        if (mowers != null) java.util.Arrays.fill(mowers, false);
    }

    /** پرکردنِ کلِ خانه‌های قابل‌کاشت با گیاهانِ palette، بدونِ هیچ ترکیبِ آماده. */
    private void fillBeghouledBoard(GameSession session) {
        int rows = session.getGameMap().getRows();
        int cols = session.getGameMap().getCols();
        com.pvz2.model.enums.PlantType[] pal = com.pvz2.model.BeghouledState.PALETTE;
        for (int r = 1; r <= rows; r++) {
            for (int c = 1; c <= cols; c++) {
                Tile tile = session.getGameMap().getTile(c, r);
                if (tile == null || tile.isCrater()) continue;
                if (tile.isTombstone() || tile.isWater()) continue;
                com.pvz2.model.enums.PlantType pick;
                int guard = 0;
                do {
                    pick = pal[RandomUtil.nextInt(pal.length)];
                } while (guard++ < 30 && wouldMakeInitialMatch(session, c, r, pick));
                setBeghouledPlant(tile, pick, c, r);
            }
        }
    }

    /** آیا گذاشتنِ نوع در (c,r) با دو خانه‌ی چپ یا پایین یک سه‌تاییِ آماده می‌سازد؟ */
    private boolean wouldMakeInitialMatch(GameSession session, int c, int r,
                                          com.pvz2.model.enums.PlantType t) {
        return (sameType(session, c - 1, r, t) && sameType(session, c - 2, r, t))
                || (sameType(session, c, r - 1, t) && sameType(session, c, r - 2, t));
    }

    private boolean sameType(GameSession session, int c, int r,
                             com.pvz2.model.enums.PlantType t) {
        com.pvz2.model.enums.PlantType at = beghouledTypeAt(session, c, r);
        return at != null && at == t;
    }

    /** نوعِ گیاهِ خانه (۱-based) یا null اگر خالی/خارج/crater. */
    private com.pvz2.model.enums.PlantType beghouledTypeAt(GameSession session, int c, int r) {
        int rows = session.getGameMap().getRows();
        int cols = session.getGameMap().getCols();
        if (c < 1 || c > cols || r < 1 || r > rows) return null;
        Tile tile = session.getGameMap().getTile(c, r);
        if (tile == null || tile.getPlant() == null) return null;
        return tile.getPlant().getType();
    }

    private void setBeghouledPlant(Tile tile, com.pvz2.model.enums.PlantType type, int c, int r) {
        Plant p = PlantFactory.create(type);
        if (p == null) return;
        p.setX(c);
        p.setY(r);
        tile.setPlant(p);
    }

    /**
     * جابه‌جاییِ دو گیاهِ مجاور توسط بازیکن. فقط اگر ترکیب بسازد انجام می‌شود
     * (طبق داک)؛ در غیر این صورت به حالتِ قبل برمی‌گردد و خطا برمی‌گرداند.
     */
    public void beghouledSwap(GameSession session, int c1, int r1, int c2, int r2) {
        if (session.getLevel().getLevelType() != LevelType.BEGHOULED)
            throw new com.pvz2.exception.GameException("Not a Beghouled level.");
        Object ms = session.getMinigameState();
        if (!(ms instanceof com.pvz2.model.BeghouledState))
            throw new com.pvz2.exception.GameException("Beghouled not initialized.");
        if (Math.abs(c1 - c2) + Math.abs(r1 - r2) != 1)
            throw new com.pvz2.exception.GameException("Pick two adjacent plants.");
        Tile a = session.getGameMap().getTile(c1, r1);
        Tile b = session.getGameMap().getTile(c2, r2);
        if (a == null || b == null || a.getPlant() == null || b.getPlant() == null)
            throw new com.pvz2.exception.GameException("Both cells must have a plant.");
        if (a.isCrater() || b.isCrater())
            throw new com.pvz2.exception.GameException("Cannot swap into a crater.");

        swapPlants(a, b, c1, r1, c2, r2);
        if (!hasAnyBeghouledMatch(session)) {
            swapPlants(a, b, c1, r1, c2, r2);   // برگردان — بدونِ ترکیب مجاز نیست
            throw new com.pvz2.exception.GameException("Swap must create a match of 3+.");
        }
        resolveBeghouledCascades(session, (com.pvz2.model.BeghouledState) ms);
    }

    private void swapPlants(Tile a, Tile b, int c1, int r1, int c2, int r2) {
        Plant pa = a.getPlant();
        Plant pb = b.getPlant();
        a.setPlant(pb);
        b.setPlant(pa);
        if (pa != null) { pa.setX(c2); pa.setY(r2); }
        if (pb != null) { pb.setX(c1); pb.setY(r1); }
    }

    private boolean hasAnyBeghouledMatch(GameSession session) {
        return computeBeghouledMatches(session) != null;
    }

    /** ماتریسِ [col0][row0] خانه‌های داخلِ ترکیب (۳+ همنوعِ پیوسته)، یا null. */
    private boolean[][] computeBeghouledMatches(GameSession session) {
        int rows = session.getGameMap().getRows();
        int cols = session.getGameMap().getCols();
        boolean[][] m = new boolean[cols][rows];
        boolean any = false;
        // افقی
        for (int r = 1; r <= rows; r++) {
            int run = 1;
            for (int c = 2; c <= cols + 1; c++) {
                com.pvz2.model.enums.PlantType cur = c <= cols ? beghouledTypeAt(session, c, r) : null;
                com.pvz2.model.enums.PlantType prev = beghouledTypeAt(session, c - 1, r);
                if (cur != null && cur == prev) {
                    run++;
                } else {
                    if (run >= 3) for (int k = c - run; k < c; k++) { m[k - 1][r - 1] = true; any = true; }
                    run = 1;
                }
            }
        }
        // عمودی
        for (int c = 1; c <= cols; c++) {
            int run = 1;
            for (int r = 2; r <= rows + 1; r++) {
                com.pvz2.model.enums.PlantType cur = r <= rows ? beghouledTypeAt(session, c, r) : null;
                com.pvz2.model.enums.PlantType prev = beghouledTypeAt(session, c, r - 1);
                if (cur != null && cur == prev) {
                    run++;
                } else {
                    if (run >= 3) for (int k = r - run; k < r; k++) { m[c - 1][k - 1] = true; any = true; }
                    run = 1;
                }
            }
        }
        return any ? m : null;
    }

    /** حلقه‌ی آبشاری: پاک‌کردنِ ترکیب‌ها، پاداشِ خورشید، سقوط، پرکردن، تکرار. */
    private void resolveBeghouledCascades(GameSession session, com.pvz2.model.BeghouledState state) {
        int rows = session.getGameMap().getRows();
        int cols = session.getGameMap().getCols();
        boolean cascade = false;
        while (true) {
            boolean[][] m = computeBeghouledMatches(session);
            if (m == null) break;
            int cleared = 0;
            for (int c = 1; c <= cols; c++)
                for (int r = 1; r <= rows; r++)
                    if (m[c - 1][r - 1]) {
                        Tile tile = session.getGameMap().getTile(c, r);
                        if (tile != null) tile.setPlant(null);
                        cleared++;
                    }
            // هر خانه‌ی پاک‌شده = بخشی از یک ترکیب؛ پاداش ۵۰ خورشید به ازای هر خانه،
            // به‌علاوه‌ی یک بونوسِ آبشاری (طبق داک، ترکیب‌های پس از حرکت بیشتر می‌دهند).
            int reward = com.pvz2.model.BeghouledState.SUN_PER_MATCH * cleared
                    + (cascade ? com.pvz2.model.BeghouledState.SUN_PER_MATCH : 0);
            session.addSun(reward);
            state.addMatches(1);            // هر مرحله‌ی resolve = یک ترکیب به‌سوی هدف
            applyBeghouledGravity(session);
            cascade = true;
        }
        // طبق داک: اگر دیگر هیچ حرکتِ ممکنی نیست، کلِ زمین reset می‌شود.
        if (!hasPossibleMove(session)) resetBeghouledBoard(session);
    }

    /** سقوطِ گیاهان به سمتِ ردیفِ ۱ (به‌جز crater) و پرکردنِ بالا با گیاهِ تصادفی. */
    private void applyBeghouledGravity(GameSession session) {
        int rows = session.getGameMap().getRows();
        int cols = session.getGameMap().getCols();
        com.pvz2.model.enums.PlantType[] pal = com.pvz2.model.BeghouledState.PALETTE;
        for (int c = 1; c <= cols; c++) {
            // جمع‌آوریِ گیاهانِ موجودِ ستون از پایین به بالا (به‌ترتیب) با ردکردنِ craterها
            java.util.List<com.pvz2.model.enums.PlantType> stack = new ArrayList<>();
            for (int r = 1; r <= rows; r++) {
                Tile t = session.getGameMap().getTile(c, r);
                if (t == null || t.isCrater()) continue;
                if (t.getPlant() != null) { stack.add(t.getPlant().getType()); t.setPlant(null); }
            }
            int idx = 0;
            for (int r = 1; r <= rows; r++) {
                Tile t = session.getGameMap().getTile(c, r);
                if (t == null || t.isCrater()) continue;   // crater خالی می‌ماند
                com.pvz2.model.enums.PlantType type = idx < stack.size()
                        ? stack.get(idx) : pal[RandomUtil.nextInt(pal.length)];
                idx++;
                setBeghouledPlant(t, type, c, r);
            }
        }
    }

    /**
     * ارتقای همه‌ی گیاهانِ نوعِ from به to روی زمین، با خرجِ خورشید (جدولِ داک).
     * پس از ارتقا اگر ترکیبی ساخته شد resolve می‌شود.
     */
    public void beghouledUpgrade(GameSession session,
                                 com.pvz2.model.enums.PlantType from,
                                 com.pvz2.model.enums.PlantType to) {
        if (session.getLevel().getLevelType() != LevelType.BEGHOULED)
            throw new com.pvz2.exception.GameException("Not a Beghouled level.");
        Object ms = session.getMinigameState();
        if (!(ms instanceof com.pvz2.model.BeghouledState))
            throw new com.pvz2.exception.GameException("Beghouled not initialized.");
        // اعتبارسنجی گزینه‌ی ارتقا و هزینه از جدول.
        int cost = -1;
        for (com.pvz2.model.BeghouledState.Upgrade u : com.pvz2.model.BeghouledState.UPGRADES) {
            if (u.from == from && u.to == to) { cost = u.cost; break; }
        }
        if (cost < 0) throw new com.pvz2.exception.GameException("Invalid upgrade.");
        // آیا اصلاً گیاهی از این نوع روی زمین هست؟
        int rows = session.getGameMap().getRows();
        int cols = session.getGameMap().getCols();
        boolean found = false;
        for (int r = 1; r <= rows && !found; r++)
            for (int c = 1; c <= cols && !found; c++)
                if (beghouledTypeAt(session, c, r) == from) found = true;
        if (!found) throw new com.pvz2.exception.GameException("No " + from.name() + " to upgrade.");
        if (session.getSunAmount() < cost)
            throw new com.pvz2.exception.GameException(
                    "Not enough sun. Need " + cost + ", have " + session.getSunAmount() + ".");
        session.spendSun(cost);
        for (int r = 1; r <= rows; r++) {
            for (int c = 1; c <= cols; c++) {
                Tile t = session.getGameMap().getTile(c, r);
                if (t != null && t.getPlant() != null && t.getPlant().getType() == from) {
                    setBeghouledPlant(t, to, c, r);
                }
            }
        }
        resolveBeghouledCascades(session, (com.pvz2.model.BeghouledState) ms);
    }

    /** آیا حرکتِ ممکنی (جابه‌جاییِ مجاور که ترکیب بسازد) روی زمین وجود دارد؟ */
    private boolean hasPossibleMove(GameSession session) {
        int rows = session.getGameMap().getRows();
        int cols = session.getGameMap().getCols();
        for (int r = 1; r <= rows; r++) {
            for (int c = 1; c <= cols; c++) {
                Tile a = session.getGameMap().getTile(c, r);
                if (a == null || a.getPlant() == null || a.isCrater()) continue;
                // امتحانِ جابه‌جایی با راست و بالا (پوششِ همه‌ی زوج‌های مجاور)
                int[][] neigh = { {c + 1, r}, {c, r + 1} };
                for (int[] n : neigh) {
                    if (n[0] > cols || n[1] > rows) continue;
                    Tile b = session.getGameMap().getTile(n[0], n[1]);
                    if (b == null || b.getPlant() == null || b.isCrater()) continue;
                    swapPlants(a, b, c, r, n[0], n[1]);
                    boolean match = hasAnyBeghouledMatch(session);
                    swapPlants(a, b, c, r, n[0], n[1]);   // برگردان
                    if (match) return true;
                }
            }
        }
        return false;
    }

    /** reset کاملِ زمین (طبق داک: اگر هیچ حرکتی نبود) — همه‌ی خانه‌های غیر-crater دوباره. */
    private void resetBeghouledBoard(GameSession session) {
        int rows = session.getGameMap().getRows();
        int cols = session.getGameMap().getCols();
        for (int r = 1; r <= rows; r++)
            for (int c = 1; c <= cols; c++) {
                Tile t = session.getGameMap().getTile(c, r);
                if (t != null && !t.isCrater()) t.setPlant(null);
            }
        // چند تلاش تا زمینی با حداقل یک حرکتِ ممکن و بدونِ ترکیبِ آماده بسازیم.
        for (int attempt = 0; attempt < 12; attempt++) {
            fillBeghouledBoard(session);
            if (hasPossibleMove(session)) return;
            for (int r = 1; r <= rows; r++)
                for (int c = 1; c <= cols; c++) {
                    Tile t = session.getGameMap().getTile(c, r);
                    if (t != null && !t.isCrater()) t.setPlant(null);
                }
        }
        fillBeghouledBoard(session);   // آخرین تلاش (بعید است)
    }

    /**
     * رفتارِ per-tickِ زامبی‌های گیاهیِ Zombotany. چون موتورِ مبارزه {@code onTick}
     * زامبی‌ها را صدا نمی‌زند، اینجا فقط برای مرحله‌ی ZOMBOTANY آن را اجرا می‌کنیم
     * (peashooter شلیک، jalapeno سوزاندنِ ردیف در ۱۰ثانیه، squash برخوردِ کشنده).
     */
    private void tickZombotany(GameSession session) {
        int tick = session.getCurrentTick();
        for (Zombie z : new ArrayList<>(session.getActiveZombies())) {
            if (z.isAlive()) z.onTick(tick, session);
        }
    }

    /** فقط غول‌پیکرها را tick می‌کند تا در نیمه‌جان imp پرتاب کنند (کپی برای throwImp). */
    private void tickGargantuars(GameSession session) {
        int tick = session.getCurrentTick();
        for (Zombie z : new ArrayList<>(session.getActiveZombies())) {
            if (z.isAlive() && z instanceof com.pvz2.model.zombies.Gargantuar) {
                z.onTick(tick, session);
            }
        }
    }

    /** فاصله‌ی نزدیک‌شدنِ اختاپوس برای پرتاب (ستون). */
    private static final double OCTOPUS_TOSS_RANGE = 4.0;

    /**
     * اختاپوس‌پرت‌کن: وقتی به گیاهی در لاینِ خود نزدیک شد، یک‌بار اختاپوسِ خود را
     * به سمتِ نزدیک‌ترین گیاهِ جلو پرتاب می‌کند (رویدادِ گرافیکی + انیمیشنِ پرتابه).
     */
    private void tickOctopus(GameSession session) {
        for (Zombie z : session.getActiveZombies()) {
            if (!z.isAlive() || z.isOctopusTossed()
                    || z.getType() != com.pvz2.model.enums.ZombieType.OCTOPUS_ZOMBIE) continue;
            int row = z.getY();
            com.pvz2.model.plants.Plant target = null;
            // نزدیک‌ترین گیاهِ جلو (ستونِ کوچکتر، به سمتِ خانه) در همان لاین
            for (int c = (int) Math.floor(z.getX()); c >= 1; c--) {
                com.pvz2.model.tiles.Tile t = session.getGameMap().getTile(c, row);
                if (t != null && t.getPlant() != null) { target = t.getPlant(); break; }
            }
            if (target == null || z.getX() - target.getX() > OCTOPUS_TOSS_RANGE) continue;
            z.setOctopusTossed(true);
            session.addOctopusToss(z.getX(), row, target.getX(), row);
        }
    }

    /** اسپاونِ بی‌پایانِ زامبی برای Beghouled — هر SPAWN_INTERVAL تیک یک زامبی. */
    private void tickBeghouled(GameSession session) {
        if (session.getCurrentTick() % BEGHOULED_SPAWN_INTERVAL != 0) return;
        ChapterType chapter = session.getGameMap().getChapter();
        com.pvz2.model.enums.ZombieType[] allowed =
                com.pvz2.model.zombies.ZombieFactory.getAllowedZombiesForChapter(chapter);
        com.pvz2.model.enums.ZombieType pick = allowed[RandomUtil.nextInt(allowed.length)];
        Zombie z = com.pvz2.model.zombies.ZombieFactory.create(pick);
        if (z == null) return;
        int lane = RandomUtil.between(1, session.getGameMap().getRows());
        z.setX(session.getGameMap().getCols() + 1.0);
        z.setY(lane);
        z.setLane(lane);
        session.getActiveZombies().add(z);
    }

    // ════════════════════════════════════════════════════════
    //  SAVE_OUR_SEEDS — pre-place protected plants
    // ════════════════════════════════════════════════════════

    private void setupSaveOurSeeds(GameSession session, Level level) {
        // گیاهان از پیش تعیین‌شده در JSON (در صورت وجود) یا random
        int[][] positions = level.getPreplacedPlants();
        if (positions == null || positions.length == 0) {
            positions = defaultSaveOurSeedsPositions();
        }
        int placed = 0;
        for (int[] pos : positions) {
            int x = pos[0];
            int y = pos[1];
            PlantType type = pos.length > 2
                    ? safeOrdinalToPlantType(pos[2])
                    : PlantType.WALL_NUT;
            Tile tile = session.getGameMap().getTile(x, y);
            if (tile != null && tile.isPlantable() && tile.getPlant() == null) {
                Plant plant = PlantFactory.create(type);
                if (plant != null) {
                    plant.setX(x);
                    plant.setY(y);
                    tile.setPlant(plant);
                    session.addProtectedPosition(x, y);
                    placed++;
                }
            }
        }
        view.printRaw("\u001B[33m\u001B[1m🌱 Save Our Seeds! "
                + placed + " protected plants placed on the map.\u001B[0m");
        view.printRaw("\u001B[31m  ⚠ If ANY protected plant is eaten → INSTANT GAME OVER!\u001B[0m");
        // نمایش موقعیت‌ها
        for (int[] p : session.getProtectedPlantPositions()) {
            view.printRaw("\u001B[33m  ★ Protected at (" + p[0] + "," + p[1] + ")\u001B[0m");
        }
    }

    private int[][] defaultSaveOurSeedsPositions() {
        return new int[][]{
            {2, 2, PlantType.WALL_NUT.ordinal()},
            {2, 3, PlantType.WALL_NUT.ordinal()},
            {3, 1, PlantType.SUNFLOWER.ordinal()},
            {3, 5, PlantType.SUNFLOWER.ordinal()}
        };
    }

    private PlantType safeOrdinalToPlantType(int ordinal) {
        PlantType[] values = PlantType.values();
        if (ordinal >= 0 && ordinal < values.length) return values[ordinal];
        return PlantType.WALL_NUT;
    }

    // ════════════════════════════════════════════════════════
    //  LOVE_YOUR_PLANTS — pre-place plants + warnings
    // ════════════════════════════════════════════════════════

    private void setupLoveYourPlants(GameSession session, Level level) {
        // در صورت وجود pre-placed plants، آن‌ها را کاشت کن
        int[][] positions = level.getPreplacedPlants();
        if (positions == null || positions.length == 0) return;
        for (int[] pos : positions) {
            int x = pos[0];
            int y = pos[1];
            PlantType type = pos.length > 2
                    ? safeOrdinalToPlantType(pos[2])
                    : PlantType.PEASHOOTER;
            Tile tile = session.getGameMap().getTile(x, y);
            if (tile != null && tile.isPlantable() && tile.getPlant() == null) {
                Plant plant = PlantFactory.create(type);
                if (plant != null) {
                    plant.setX(x);
                    plant.setY(y);
                    tile.setPlant(plant);
                    session.addProtectedLovePlantPosition(x, y);
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════
    //  CONVEYOR_BELT TICK
    // ════════════════════════════════════════════════════════

    /**
     * هر تیک در advanceTime فراخوانی می‌شود.
     * هر 120 تیک (12 ثانیه) یک گیاه تصادفی به نوار اضافه می‌کند.
     */
    private void tickConveyor(GameSession session) {
        LevelType lt = session.getLevel().getLevelType();
        if (lt != LevelType.CONVEYOR_BELT && lt != LevelType.WALLNUT_BOWLING) return;
        if (session.getConveyorQueue().size() >= CONVEYOR_CAP) return;
        if (session.isConveyorReady(session.getCurrentTick())) {
            pushConveyorPlant(session);
        }
    }

    private void pushConveyorPlant(GameSession session) {
        if (session.getConveyorQueue().size() >= CONVEYOR_CAP) return;
        // استخر مناسب بر اساس نوع مرحله (گیاه برای نوار کناری، گردو برای بولینگ)
        PlantType[] pool = session.getLevel().getLevelType() == LevelType.WALLNUT_BOWLING
                ? BOWLING_NUT_POOL : CONVEYOR_PLANT_POOL;
        PlantType newPlant = pool[RandomUtil.between(0, pool.length - 1)];
        session.addToConveyor(newPlant, session.getCurrentTick());
        view.printRaw("\u001B[33m📦 Conveyor: "
                + newPlant.name()
                + " is now available! (use: plant plant -t "
                + newPlant.name() + " -l (x, y))\u001B[0m");
    }

    // ════════════════════════════════════════════════════════
    //  ADVANCE TIME
    // ════════════════════════════════════════════════════════

    public void advanceTime(GameSession session, int ticks) {
        for (int i = 0; i < ticks; i++) {
            if (!session.isInProgress()) break;
            session.advanceTick();
            processOneTick(session);
        }
        mapView.printMap(session);
        combatService.checkWinCondition(session);
    }

    private void processOneTick(GameSession session) {
        LevelType lt = session.getLevel().getLevelType();
        // کوزه‌شکنی/بولینگ/من‌زامبی خورشید آسمانی ندارند.
        if (lt != LevelType.VASEBREAKER && lt != LevelType.WALLNUT_BOWLING
                && lt != LevelType.I_ZOMBIE) {
            sunService.tickSkyDrops(session);
        }
        // نوار کناری برای مراحل نوار کناری و بولینگ گردویی
        if (lt == LevelType.CONVEYOR_BELT || lt == LevelType.WALLNUT_BOWLING) {
            tickConveyor(session);
        }
        // من‌زامبی: در هر ردیف یک زامبیِ تولیدکننده‌ی خورشید هست؛ هر ۵ ثانیه
        // خورشید می‌دهند و نرخِ تولید با گذرِ زمان بیشتر می‌شود (طبق داک).
        if (lt == LevelType.I_ZOMBIE && session.getCurrentTick() % 50 == 0) {
            int producers = session.getGameMap().getRows();      // یکی به‌ازای هر ردیف
            // نرخِ هر تولیدکننده از ۵ شروع و با زمان تا ۲۰ بالا می‌رود.
            int perProducer = 5 + Math.min(15, session.getCurrentTick() / 600);
            session.addSun(perProducer * producers);
        }
        // Beghouled: زامبی‌های بی‌پایان را خودمان spawn می‌کنیم.
        if (lt == LevelType.BEGHOULED) {
            tickBeghouled(session);
        }
        // VERSUS: تایمرِ ۲ دقیقه + بازآمدِ خورشیدِ زامبی‌گذار.
        if (lt == LevelType.VERSUS) {
            tickVersus(session);
        }
        // موج‌ها فقط اگر waveStarted باشد
        if (session.isWaveStarted()) {
            manageWaves(session);
        }
        combatService.processTick(session);
        // چمن‌زنِ متحرک: هر تیک جلو می‌رود و زامبی‌های لاین را یکی‌یکی می‌کشد.
        combatService.tickMowers(session);
        // غول‌پیکر: در نیمه‌جان imp پرتاب می‌کند (onTick زامبی‌ها به‌صورتِ عمومی
        // توسطِ موتور صدا زده نمی‌شود؛ پس مثلِ Zombotany صریحاً گیت‌شده صدا می‌زنیم).
        tickGargantuars(session);
        // اختاپوس‌پرت‌کن: نزدیکِ گیاه که رسید، اختاپوسِ خود را پرتاب می‌کند.
        tickOctopus(session);
        // حرکت/برخورد گردوهای بولینگ پس از حرکت زامبی‌ها
        if (lt == LevelType.WALLNUT_BOWLING) {
            tickBowling(session);
        }
        // رئیس (Zomboss): حرکت/حمله/اسپاون پس از پردازشِ مبارزه
        if (lt == LevelType.BOSS) {
            bossService.tick(session);
        }
        // Zombotany: رفتارِ اختصاصیِ زامبی‌های گیاهی (شلیک/آتش/کدو) پس از مبارزه.
        if (lt == LevelType.ZOMBOTANY) {
            tickZombotany(session);
        }
        // EK3: تغییرِ سطحِ آبِ ساحل (فقط برای مراحلِ ساحلی؛ خودش early-return دارد)
        tickBeachTide(session);
        // خورشیدهای جمع‌آوری‌شده را از مدل پاک کن (لایه‌ی انیمیشن، انیمیشنِ
        // پروازِ آن‌ها به سمتِ شمارنده را از state داخلیِ خودش ادامه می‌دهد).
        sunService.cleanupCollectedSuns(session);
        // cooldownِ بسته‌های بذر (recharge کارت‌ها)
        session.tickSeedCooldowns();
    }

    // ════════════════════════════════════════════════════════
    //  WAVE MANAGEMENT
    // ════════════════════════════════════════════════════════

    private void manageWaves(GameSession session) {
        if (session.getWaves() == null || session.getWaves().isEmpty()) return;
        Wave current = session.getCurrentWave();
        if (current == null) return;
        if (!current.isStarted()) {
            startWave(session, current);
            return;
        }
        if (!current.isFinalWave() && session.hasMoreWaves()
                && current.shouldTriggerNextWave()) {
            session.setCurrentWaveIndex(session.getCurrentWaveIndex() + 1);
            Wave next = session.getCurrentWave();
            if (next != null) beforeWaveStart(session, next);
        }
    }

    private void startWave(GameSession session, Wave wave) {
        view.printWaveStarted(wave.getWaveNumber(), wave.isFinalWave());
        beforeWaveStart(session, wave);
        waveService.spawnWave(wave, session);
    }

    private void beforeWaveStart(GameSession session, Wave wave) {
        waveService.applyFrostbiteWind(session, wave.getWaveNumber());
        addDarkAgesTombstones(session);
        waveService.spawnNecromancyZombie(session);
    }

    private void addDarkAgesTombstones(GameSession session) {
        if (session.getGameMap().getChapter() != ChapterType.DARK_AGES) return;
        int count = RandomUtil.between(0, 3);
        for (int i = 0; i < count; i++) {
            int col = RandomUtil.between(1, session.getGameMap().getCols());
            int row = RandomUtil.between(1, session.getGameMap().getRows());
            Tile tile = session.getGameMap().getTile(col, row);
            if (tile != null && tile.getPlant() == null && !tile.isTombstone()) {
                session.getGameMap().setTileType(col, row, TileType.DARK_TOMBSTONE);
                // بعضی سنگ‌قبرها جایزه دارند: خورشید یا غذای گیاه (با نابودی آزاد می‌شود).
                double rewardRoll = RandomUtil.nextDouble();
                if (rewardRoll < 0.25)      tile.setReward(Tile.Reward.SUN);
                else if (rewardRoll < 0.50) tile.setReward(Tile.Reward.PLANT_FOOD);
                view.printRaw("\u001B[35m⛰ A tombstone appeared at ("
                        + col + "," + row + ")!\u001B[0m");
            }
        }
    }

    private void applyBoosts(GameSession session) {
        List<PlantType> boosted = session.getBoostedPlants();
        if (boosted == null || boosted.isEmpty()) return;
        for (int r = 1; r <= session.getGameMap().getRows(); r++) {
            for (int c = 1; c <= session.getGameMap().getCols(); c++) {
                Tile t = session.getGameMap().getTile(c, r);
                if (t != null && t.getPlant() != null
                        && boosted.contains(t.getPlant().getType())) {
                    t.getPlant().activatePlantFood(session);
                    t.getPlant().setBoosted(true);
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════
    //  PLANT / PLUCK / FEED
    // ════════════════════════════════════════════════════════

    public void plantPlant(GameSession session, PlantType type, int x, int y) {
        // بولینگ گردویی: «کاشت» یعنی رها کردن یک گردوی غلتان، نه گیاه روی خانه.
        if (session.getLevel().getLevelType() == LevelType.WALLNUT_BOWLING) {
            placeBowlingNut(session, type, x, y);
            return;
        }

        Tile tile = session.getGameMap().getTile(x, y);
        if (tile == null)
            throw new com.pvz2.exception.GameException("Invalid position (" + x + ", " + y + ").");

        // ─── نوار کناری: گیاه باید روی نوار باشد و رایگان کاشته می‌شود ───────
        boolean conveyor = session.getLevel().getLevelType() == LevelType.CONVEYOR_BELT;
        if (conveyor && !session.hasConveyorPlant(type))
            throw new com.pvz2.exception.GameException(
                    type.name() + " is not on the conveyor belt.");

        // ─── کوزه‌شکنی: فقط بذرهای برداشت‌شده از کوزه‌ها، رایگان کاشته می‌شوند ──
        boolean vasebreaker = session.getLevel().getLevelType() == LevelType.VASEBREAKER;
        VasebreakerState vbState = null;
        if (vasebreaker) {
            Object ms = session.getMinigameState();
            vbState = (ms instanceof VasebreakerState) ? (VasebreakerState) ms : null;
            if (vbState == null || !vbState.hasSeed(type))
                throw new com.pvz2.exception.GameException(
                        type.name() + " is not in your vase inventory.");
            if (vbState.hasUnbrokenVaseAt(x, y))
                throw new com.pvz2.exception.GameException(
                        "Break the vase here before planting.");
        }

        if (PlantFactory.isWaterPlant(type)) {
            if (!tile.isWater())
                throw new com.pvz2.exception.GameException(type.name() + " can only be planted on water.");
        } else if (type == PlantType.PUMPKIN) {
            if (tile.getPlant() == null)
                throw new com.pvz2.exception.GameException("Pumpkin needs a plant underneath.");
        } else if (type == PlantType.PEA_POD
                && tile.getPlant() instanceof PeaPodPlant) {
            PeaPodPlant pod = (PeaPodPlant) tile.getPlant();
            int cost = PlantFactory.getSunCost(type);
            if (session.getSunAmount() < cost)
                throw new com.pvz2.exception.GameException(
                        "Not enough sun. Need " + cost + ", have " + session.getSunAmount() + ".");
            if (!pod.addHead())
                throw new com.pvz2.exception.GameException("Pea Pod already at max 5 heads.");
            session.spendSun(cost);
            return;
        } else {
            if (!tile.isPlantable())
                throw new com.pvz2.exception.GameException("Cannot plant at (" + x + ", " + y + ").");
        }

        // نوار کناری و کوزه‌شکنی گیاه رایگان دارند؛ در بقیه مراحل هزینه خورشید دارد
        boolean freePlant = conveyor || vasebreaker;
        // cooldownِ بسته‌ی بذر: در مراحلِ عادی نمی‌توان گیاهِ در حالِ recharge را کاشت
        if (!freePlant && !session.isSeedReady(type))
            throw new com.pvz2.exception.GameException(type.name() + " is recharging.");
        int cost = freePlant ? 0 : PlantFactory.getSunCost(type);
        if (!freePlant && session.getSunAmount() < cost)
            throw new com.pvz2.exception.GameException(
                    "Not enough sun. Need " + cost + ", have " + session.getSunAmount() + ".");

        if (cost > 0) session.spendSun(cost);
        Plant plant = PlantFactory.createWithUpgrade(type,
                AppState.getInstance().getCurrentUser());
        if (plant == null)
            throw new com.pvz2.exception.GameException("Unknown plant type: " + type.name());

        plant.setX(x);
        plant.setY(y);

        // PLANT_WHAT_YOU_GET: بدون cooldown در فاز آزاد
        if (!session.isWaveStarted()
                && session.getLevel().getLevelType() == LevelType.PLANT_WHAT_YOU_GET) {
            plant.resetCooldown();
        }

        if (type == PlantType.PUMPKIN) {
            tile.setSecondLayerPlant(tile.getPlant());
            tile.setPlant(plant);
        } else {
            tile.setPlant(plant);
        }
        // نوار کناری: بعد از کاشت موفق، گیاه از نوار حذف می‌شود
        if (conveyor) session.useConveyorPlant(type);
        // کوزه‌شکنی: بذر از انبار مصرف می‌شود
        if (vasebreaker && vbState != null) vbState.consumeSeed(type);
        // مراحلِ عادی: cooldownِ بسته‌ی بذر را آغاز کن (recharge)
        if (!freePlant) {
            int ticks = (int) Math.round(PlantFactory.getRechargeTime(type) * 10.0); // ۱۰ تیک/ثانیه
            session.startSeedCooldown(type, ticks);
        }
        applyBoostIfStored(plant, session);
    }

    private void applyBoostIfStored(Plant plant, GameSession session) {
        User user = AppState.getInstance().getCurrentUser();
        if (user == null) return;
        if (plant.isHasStoredBoost()) {
            plant.activatePlantFood(session);
            plant.setHasStoredBoost(false);
        }
    }

    public void pluckPlant(GameSession session, int x, int y) {
        Tile tile = session.getGameMap().getTile(x, y);
        if (tile == null || tile.getPlant() == null)
            throw new com.pvz2.exception.GameException("No plant at (" + x + ", " + y + ").");
        if (tile.getSecondLayerPlant() != null) {
            tile.setPlant(tile.getSecondLayerPlant());
            tile.setSecondLayerPlant(null);
        } else {
            tile.setPlant(null);
        }
    }

    public void feedPlant(GameSession session, int x, int y) {
        if (session.getPlantFoodCount() <= 0)
            throw new com.pvz2.exception.GameException("No plant food available.");
        Tile tile = session.getGameMap().getTile(x, y);
        if (tile == null || tile.getPlant() == null)
            throw new com.pvz2.exception.GameException("No plant at (" + x + ", " + y + ").");
        session.usePlantFood();
        tile.getPlant().activatePlantFood(session);
        view.printSuccess("Plant food used on " + tile.getPlant().getType().name() + "!");
    }

    // ════════════════════════════════════════════════════════
    //  CHEATS
    // ════════════════════════════════════════════════════════

    public void releaseNuke(GameSession session) {
        List<Zombie> toKill = new ArrayList<>(session.getActiveZombies());
        for (Zombie z : toKill) z.setCurrentHealth(0);
        view.printSuccess("The nuke released! All zombies obliterated!");
    }

    public void removeCooldown(GameSession session) {
        session.setCooldownCheated(true);
        for (int r = 1; r <= session.getGameMap().getRows(); r++) {
            for (int c = 1; c <= session.getGameMap().getCols(); c++) {
                Tile tile = session.getGameMap().getTile(c, r);
                if (tile != null && tile.getPlant() != null)
                    tile.getPlant().resetCooldown();
            }
        }
        view.printSuccess("All cooldowns removed!");
    }

    public void addPlantFoodCheat(GameSession session) {
        boolean added = session.addPlantFood();
        if (added) view.printSuccess("Plant food added. You have "
                + session.getPlantFoodCount() + " now.");
        else view.printError("Plant food is at maximum (3).");
    }

    public void spawnZombieCheat(GameSession session, String zombieType, int x, int y) {
        try {
            com.pvz2.model.enums.ZombieType type =
                    com.pvz2.model.enums.ZombieType.valueOf(zombieType.toUpperCase());
            Zombie z = com.pvz2.model.zombies.ZombieFactory.create(type);
            z.setX(x);
            z.setY(y);
            z.setLane(y);
            session.getActiveZombies().add(z);
            view.printSuccess("Spawned " + zombieType + " at (" + x + "," + y + ")");
        } catch (IllegalArgumentException e) {
            view.printError("Unknown zombie type: " + zombieType);
        }
    }

    // ════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════

    private void printTimedWarObjective(Level level) {
        if (level.isTimedWarSunMode()) {
            view.printRaw("\u001B[33m\u001B[1m⏱ TIMED WAR!\u001B[0m"
                    + " Collect " + level.getTimedWarSunTarget()
                    + " sun in " + level.getTimedWarSeconds() + " seconds!");
        } else {
            view.printRaw("\u001B[33m\u001B[1m⏱ TIMED WAR!\u001B[0m"
                    + " Kill " + level.getTimedWarZombieTarget()
                    + " zombies in " + level.getTimedWarSeconds() + " seconds!");
        }
        view.printRaw("  Use 'show special status' to see remaining time and progress.");
    }
}
