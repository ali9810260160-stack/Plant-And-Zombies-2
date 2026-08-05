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

    public GameService(WaveService waveService, CombatService combatService,
                       SunService sunService, ConsoleView view, MapView mapView) {
        this.waveService = waveService;
        this.combatService = combatService;
        this.sunService = sunService;
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
        for (int col = map.getCols() - waterCols + 1;
             col <= map.getCols(); col++) {
            for (int row = 1; row <= map.getRows(); row++) {
                map.setTileType(col, row, TileType.WATER);
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

            // DARK_AGES (فصل) — بدون خورشید آسمانی، اعلام
            default:
                if (level.getChapter() == ChapterType.DARK_AGES) {
                    session.setSunAmount(50);
                }
                break;
        }
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
        if (session.getLevel().getLevelType() != LevelType.CONVEYOR_BELT) return;
        if (session.isConveyorReady(session.getCurrentTick())) {
            pushConveyorPlant(session);
        }
    }

    private void pushConveyorPlant(GameSession session) {
        // گیاه تصادفی از pool
        PlantType newPlant = CONVEYOR_PLANT_POOL[
                RandomUtil.between(0, CONVEYOR_PLANT_POOL.length - 1)];
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
        sunService.tickSkyDrops(session);
        // CONVEYOR_BELT: هر تیک چک کن
        if (session.getLevel().getLevelType() == LevelType.CONVEYOR_BELT) {
            tickConveyor(session);
        }
        // موج‌ها فقط اگر waveStarted باشد
        if (session.isWaveStarted()) {
            manageWaves(session);
        }
        combatService.processTick(session);
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
        Tile tile = session.getGameMap().getTile(x, y);
        if (tile == null)
            throw new com.pvz2.exception.GameException("Invalid position (" + x + ", " + y + ").");

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

        int cost = PlantFactory.getSunCost(type);
        if (session.getSunAmount() < cost)
            throw new com.pvz2.exception.GameException(
                    "Not enough sun. Need " + cost + ", have " + session.getSunAmount() + ".");

        session.spendSun(cost);
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
