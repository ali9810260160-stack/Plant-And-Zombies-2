package com.pvz2.controller;

import com.pvz2.exception.GameException;
import com.pvz2.model.*;
import com.pvz2.model.enums.*;
import com.pvz2.model.plants.*;
import com.pvz2.model.tiles.Tile;
import com.pvz2.service.*;
import com.pvz2.view.ConsoleView;
import com.pvz2.view.MapView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

/**
 * کنترلر اصلی بازی.
 * مدیریت کامل هر ۸ نوع مرحله ویژه در buildLevel، startGame، plantPlant.
 */
public class GameController {

    private final GameService gameService;
    private LevelProgressService levelProgressService;
    private final SunService sunService;
    private final ConsoleView view;
    private final MapView mapView;

    private Level pendingLevel;
    private int pendingLevelNumber;
    private final List<PlantType> selectedPlants = new ArrayList<>();
    private final List<PlantType> boostedPlants  = new ArrayList<>();

    /** گیاهان قفل‌شده در مرحله LOCKED_PLANTS فعلی */
    private final List<PlantType> currentLevelLockedPlants = new ArrayList<>();

    public GameController(GameService gameService, SunService sunService,
                          ConsoleView view, MapView mapView) {
        this.gameService = gameService;
        this.sunService  = sunService;
        this.view        = view;
        this.mapView     = mapView;
    }

    /** نمایش فصل‌های موجود */
    public void showAllChapters(AppState appState) {
        if (levelProgressService == null) {
            view.printError("Level com.pvz2.service not initialized.");
            return;
        }
        levelProgressService.showAllChapters(appState.getCurrentUser());
    }

    /** نمایش مراحل یک فصل */
    public void showChapterLevels(String chapterName, AppState appState) {
        try {
            ChapterType chapter =
                    ChapterType.valueOf(chapterName.toUpperCase());
            if (levelProgressService != null) {
                levelProgressService.showChapterLevels(
                        appState.getCurrentUser(), chapter);
            }
        } catch (IllegalArgumentException e) {
            view.printError("Unknown chapter: " + chapterName);
        }
    }

    // ════════════════════════════════════════════════════════
    //  ENTER CHAPTER
    // ════════════════════════════════════════════════════════

    public void enterChapterWithLevel(String chapterName, int levelNum,
                                      AppState appState) {
        com.pvz2.model.User user = appState.getCurrentUser();
        ChapterType chapter;
        try {
            chapter = ChapterType.valueOf(chapterName.toUpperCase());
        } catch (IllegalArgumentException e) {
            view.printError("Unknown chapter: " + chapterName);
            return;
        }
        if (!user.isLevelUnlocked(chapterName, levelNum)) {
            view.printError("Level " + levelNum + " of " + chapterName
                    + " is locked. Complete previous levels first.");
            return;
        }
        pendingLevel = buildLevel(chapter, levelNum);
        pendingLevelNumber = levelNum;
        selectedPlants.clear();
        boostedPlants.clear();
        currentLevelLockedPlants.clear();

        // CONVEYOR_BELT → منوی انتخاب گیاه ندارد؛ مستقیم به IN_GAME
        if (pendingLevel.getLevelType() == LevelType.CONVEYOR_BELT) {
            view.printRaw("\u001B[33m📦 CONVEYOR BELT level — no plant selection needed."
                    + " Plants arrive automatically every 12 seconds!\u001B[0m");
            startGameDirectly(appState);
            return;
        }

        // LOCKED_PLANTS → آماده کردن لیست قفل‌ها
        if (pendingLevel.getLevelType() == LevelType.LOCKED_PLANTS) {
            setupLockedPlantsForLevel(pendingLevel, chapter);
        }

        appState.setCurrentMenu(MenuType.PLANT_SELECT);
        view.printSuccess("Level " + levelNum + " of " + chapterName
                + " selected. Choose your plants.");
        printLevelTypeHint(pendingLevel);
    }

    public void enterChapter(String chapterName, AppState appState) {
        enterChapterWithLevel(chapterName, 1, appState);
    }

    // ════════════════════════════════════════════════════════
    //  BUILD LEVEL — کامل برای هر ۸ نوع
    // ════════════════════════════════════════════════════════

    private Level buildLevel(ChapterType chapter, int levelNumber) {
        LevelType type = getLevelType(chapter, levelNumber);
        Level level = new Level(levelNumber, chapter, type);

        int baseDiff = 400 + (getChapterIndex(chapter) * 200)
                + (levelNumber - 1) * 100;
        level.setInitialWaveDifficulty(baseDiff);
        level.setWaveCount(levelNumber == 4 ? 5 : 3);
        level.setMapRows(5);
        level.setMapCols(9);
        level.setPlantSlots(8);

        switch (type) {

            // ── 1. CONVEYOR_BELT ──────────────────────────────────
            // گیاهان از پیش انتخاب نمی‌شوند — نوار هر 12 ثانیه گیاه می‌دهد
            case CONVEYOR_BELT:
                level.setInitialSunAmount(0);
                // sun برای CONVEYOR_BELT از طریق گیاهان (Sunflower) تامین می‌شود
                break;

            // ── 2. LOCKED_PLANTS ─────────────────────────────────
            // نوع اول: یک خانواده کلاً قفل (فقط یک نوع از آن‌ها آزاد)
            // نوع دوم: چند گیاه خاص قفل
            case LOCKED_PLANTS:
                configureLockPlants(level, chapter);
                break;

            // ── 3. SAVE_OUR_SEEDS ────────────────────────────────
            // گیاهان از پیش کاشته شده → از بین رفتنشان = باخت فوری
            case SAVE_OUR_SEEDS:
                // موقعیت‌های pre-placed طبق فصل
                level.setPreplacedPlants(getSaveOurSeedsPositions(chapter));
                level.setInitialSunAmount(150);
                break;

            // ── 4. TIMED_WAR ──────────────────────────────────────
            // کشتن X زامبی یا تولید X خورشید در Y ثانیه
            case TIMED_WAR:
                configureTiimedWar(level, chapter);
                break;

            // ── 5. NIGHT_OPS ──────────────────────────────────────
            // بدون خورشید آسمانی؛ خورشید اولیه 150
            case NIGHT_OPS:
                level.setNightOpsSun(150);
                level.setInitialSunAmount(150);
                break;

            // ── 6. DEAD_LINE ──────────────────────────────────────
            // زامبی از ستون 5 رد شود → باخت
            case DEAD_LINE:
                level.setDeadLineColumn(5);
                level.setInitialSunAmount(50);
                break;

            // ── 7. LOVE_YOUR_PLANTS ───────────────────────────────
            // حداکثر 3 گیاه می‌توان از دست داد
            case LOVE_YOUR_PLANTS:
                level.setMaxPlantsLost(3);
                level.setInitialSunAmount(50);
                level.setPreplacedPlants(getLoveYourPlantsPositions(chapter));
                break;

            // ── 8. PLANT_WHAT_YOU_GET ─────────────────────────────
            // خورشید ثابت 750، بدون آسمانی، بدون Sunflower
            case PLANT_WHAT_YOU_GET:
                level.setInitialSunAmount(750);
                // گیاهان Sun Producer قفل می‌شوند
                level.setForcedLockedSlots(getSunProducerPlants());
                break;

            case NORMAL:
            case BOSS:
            default:
                level.setInitialSunAmount(50);
                break;
        }
        return level;
    }

    // ════════════════════════════════════════════════════════
    //  LOCKED_PLANTS CONFIGURATION
    // ════════════════════════════════════════════════════════

    private void configureLockPlants(Level level, ChapterType chapter) {
        // نوع اول: کل خانواده LOBBER قفل (برای فصل BIG_WAVE_BEACH)
        List<PlantType> locked = new ArrayList<>();
        if (chapter == ChapterType.BIG_WAVE_BEACH) {
            // قفل کردن گیاهان LOBBER — مناسب برای فصل ساحل
            locked.addAll(getPlantsByFamily(PlantFamily.LOBBER));
        } else {
            // قفل کردن 3 گیاه تصادفی از unlocked‌های کاربر
            locked.add(PlantType.SNOW_PEA);
            locked.add(PlantType.CHOMPER);
            locked.add(PlantType.TALL_NUT);
        }
        level.setForcedLockedSlots(locked);
    }

    private List<PlantType> getPlantsByFamily(PlantFamily family) {
        List<PlantType> result = new ArrayList<>();
        for (PlantType t : PlantType.values()) {
            PlantStats stats = PlantDataRegistry.getInstance().getStats(t);
            if (stats != null && stats.getFamily() == family) result.add(t);
        }
        return result;
    }

    private void setupLockedPlantsForLevel(Level level, ChapterType chapter) {
        currentLevelLockedPlants.clear();
        if (level.getForcedLockedSlots() != null)
            currentLevelLockedPlants.addAll(level.getForcedLockedSlots());
        if (!currentLevelLockedPlants.isEmpty()) {
            view.printRaw("\u001B[31m🔒 The following plants are LOCKED in this level:\u001B[0m");
            for (PlantType t : currentLevelLockedPlants)
                view.printRaw("\u001B[31m   ✗ " + t.name() + "\u001B[0m");
        }
    }

    // ════════════════════════════════════════════════════════
    //  TIMED_WAR CONFIGURATION
    // ════════════════════════════════════════════════════════

    private void configureTiimedWar(Level level, ChapterType chapter) {
        if (chapter == ChapterType.FROSTBITE_CAVES) {
            // نوع زامبی: کشتن 12 زامبی در 60 ثانیه
            level.setTimedWarSunMode(false);
            level.setTimedWarZombieTarget(12);
            level.setTimedWarSeconds(60);
        } else if (chapter == ChapterType.DARK_AGES) {
            // نوع خورشید: جمع‌آوری 300 خورشید در 90 ثانیه
            level.setTimedWarSunMode(true);
            level.setTimedWarSunTarget(300);
            level.setTimedWarSeconds(90);
        } else {
            // پیش‌فرض
            level.setTimedWarSunMode(false);
            level.setTimedWarZombieTarget(10);
            level.setTimedWarSeconds(60);
        }
        level.setInitialSunAmount(50);
    }

    // ════════════════════════════════════════════════════════
    //  PRE-PLACED PLANT POSITIONS
    // ════════════════════════════════════════════════════════

    /** موقعیت‌های Save Our Seeds: {x, y, plantType.ordinal()} */
    private int[][] getSaveOurSeedsPositions(ChapterType chapter) {
        return new int[][]{
            {2, 2, PlantType.WALL_NUT.ordinal()},
            {2, 4, PlantType.WALL_NUT.ordinal()},
            {3, 1, PlantType.SUNFLOWER.ordinal()},
            {3, 3, PlantType.SUNFLOWER.ordinal()},
            {3, 5, PlantType.SUNFLOWER.ordinal()}
        };
    }

    /** موقعیت‌های Love Your Plants: {x, y, plantType.ordinal()} */
    private int[][] getLoveYourPlantsPositions(ChapterType chapter) {
        return new int[][]{
            {1, 1, PlantType.PEASHOOTER.ordinal()},
            {1, 3, PlantType.SUNFLOWER.ordinal()},
            {1, 5, PlantType.PEASHOOTER.ordinal()},
            {2, 2, PlantType.SNOW_PEA.ordinal()},
            {2, 4, PlantType.SNOW_PEA.ordinal()}
        };
    }

    /** لیست گیاهان تولیدکننده خورشید (برای قفل در PLANT_WHAT_YOU_GET) */
    private List<PlantType> getSunProducerPlants() {
        return Arrays.asList(
            PlantType.SUNFLOWER, PlantType.TWIN_SUNFLOWER,
            PlantType.SUN_SHROOM, PlantType.PRIMAL_SUNFLOWER
        );
    }

    // ════════════════════════════════════════════════════════
    //  LEVEL TYPE MAP
    // ════════════════════════════════════════════════════════

    private LevelType getLevelType(ChapterType chapter, int levelNumber) {
        if (levelNumber == 1) return LevelType.NORMAL;
        if (levelNumber == 4) return LevelType.BOSS;
        switch (chapter) {
            case ANCIENT_EGYPT:
                return levelNumber == 2
                        ? LevelType.CONVEYOR_BELT : LevelType.SAVE_OUR_SEEDS;
            case FROSTBITE_CAVES:
                return levelNumber == 2
                        ? LevelType.NIGHT_OPS : LevelType.TIMED_WAR;
            case BIG_WAVE_BEACH:
                return levelNumber == 2
                        ? LevelType.LOCKED_PLANTS : LevelType.DEAD_LINE;
            case DARK_AGES:
                return levelNumber == 2
                        ? LevelType.LOVE_YOUR_PLANTS : LevelType.PLANT_WHAT_YOU_GET;
            default:
                return LevelType.NORMAL;
        }
    }

    private int getChapterIndex(ChapterType chapter) {
        switch (chapter) {
            case ANCIENT_EGYPT:   return 0;
            case FROSTBITE_CAVES: return 1;
            case BIG_WAVE_BEACH:  return 2;
            case DARK_AGES:       return 3;
            default:              return 0;
        }
    }

    // ════════════════════════════════════════════════════════
    //  PLANT SELECT MENU
    // ════════════════════════════════════════════════════════

    public void showAllPlantsForSelect(AppState appState) {
        com.pvz2.model.User user = appState.getCurrentUser();
        view.printHeader("All Available Plants");
        for (PlantType type : PlantType.values()) {
            PlantStats stats = PlantDataRegistry.getInstance().getStats(type);
            if (stats == null) continue;
            boolean owned = user.getUnlockedPlants() != null
                    && user.getUnlockedPlants().contains(type.name());
            boolean locked = currentLevelLockedPlants.contains(type);
            String status = locked
                    ? ConsoleView.RED + "[LOCKED in this level]" + ConsoleView.RESET
                    : owned
                    ? ConsoleView.GREEN + "[owned]" + ConsoleView.RESET
                    : ConsoleView.RED + "[locked]" + ConsoleView.RESET;
            view.printRaw("  " + type.name() + "  cost:" + stats.getSunCost()
                    + "  " + status);
        }
    }

    public void showAvailablePlants(AppState appState) {
        com.pvz2.model.User user = appState.getCurrentUser();
        view.printHeader("Available Plants for This Level");
        List<String> unlocked = user.getUnlockedPlants();
        if (unlocked == null || unlocked.isEmpty()) {
            view.printInfo("No unlocked plants. Buy from collection menu first.");
            return;
        }
        int i = 1;
        for (String name : unlocked) {
            PlantType t = parsePlantType(name);
            boolean locked = t != null && currentLevelLockedPlants.contains(t);
            boolean selected = t != null && selectedPlants.contains(t);
            String tag = locked ? ConsoleView.RED + " [LOCKED]" + ConsoleView.RESET
                    : selected ? ConsoleView.GREEN + " ✔ SELECTED" + ConsoleView.RESET : "";
            view.printRaw("  " + i++ + ". " + name + tag);
        }
        view.printRaw(ConsoleView.CYAN + "  Selected: "
                + selectedPlants.size() + "/"
                + (pendingLevel != null ? pendingLevel.getPlantSlots() : 8)
                + ConsoleView.RESET);
    }

    /**
     * اضافه کردن گیاه به لیست انتخاب‌شده.
     * LOCKED_PLANTS: رد کردن گیاهان قفل‌شده.
     * PLANT_WHAT_YOU_GET: رد کردن Sun Producers.
     */
    public void addPlantToSelect(String typeName, AppState appState) {
        com.pvz2.model.User user = appState.getCurrentUser();
        PlantType type = parsePlantType(typeName);
        if (type == null) {
            view.printError("Unknown plant: " + typeName);
            return;
        }
        if (!userHasPlant(user, type)) {
            view.printError("You don't own " + typeName + ". Purchase first.");
            return;
        }
        // LOCKED_PLANTS بررسی
        if (currentLevelLockedPlants.contains(type)) {
            view.printError(typeName + " is LOCKED in this level type!");
            return;
        }
        // PLANT_WHAT_YOU_GET: بدون گیاهان Sun Producer
        if (pendingLevel != null
                && pendingLevel.getLevelType() == LevelType.PLANT_WHAT_YOU_GET) {
            PlantStats stats = PlantDataRegistry.getInstance().getStats(type);
            if (stats != null && stats.getFamily() == PlantFamily.SUN_PRODUCER) {
                view.printError(typeName + " (Sun Producer) is not allowed "
                        + "in Plant What You Get levels!");
                return;
            }
        }
        if (selectedPlants.contains(type)) {
            view.printError(typeName + " is already selected.");
            return;
        }
        if (pendingLevel != null
                && selectedPlants.size() >= pendingLevel.getPlantSlots()) {
            view.printError("Plant slots full ("
                    + pendingLevel.getPlantSlots() + " max).");
            return;
        }
        selectedPlants.add(type);
        view.printSuccess(typeName + " added. ("
                + selectedPlants.size() + "/"
                + (pendingLevel != null ? pendingLevel.getPlantSlots() : 8) + ")");
    }

    public void removePlantFromSelect(String typeName, AppState appState) {
        PlantType type = parsePlantType(typeName);
        if (type == null) {
            view.printError("Unknown plant: " + typeName);
            return;
        }
        if (!selectedPlants.remove(type)) {
            view.printError(typeName + " is not selected.");
            return;
        }
        view.printSuccess(typeName + " removed.");
    }

    public void boostPlant(String typeName, AppState appState) {
        com.pvz2.model.User user = appState.getCurrentUser();
        PlantType type = parsePlantType(typeName);
        if (type == null) { view.printError("Unknown plant: " + typeName); return; }
        if (!selectedPlants.contains(type)) {
            view.printError(typeName + " must be selected first."); return;
        }
        if (user.getGems() < 2) {
            view.printError("Need 2 gems. Have: " + user.getGems()); return;
        }
        user.setGems(user.getGems() - 2);
        boostedPlants.add(type);
        view.printSuccess(typeName + " will be boosted when planted! (-2 gems)");
    }

    // ════════════════════════════════════════════════════════
    //  START GAME
    // ════════════════════════════════════════════════════════

    /**
     * شروع بازی معمولی (بعد از انتخاب گیاه).
     * CONVEYOR_BELT از طریق startGameDirectly() شروع می‌شود.
     */
    public void startGame(AppState appState) {
        if (pendingLevel == null) {
            view.printError("No level selected.");
            return;
        }
        // CONVEYOR_BELT نیازی به گیاه انتخاب‌شده ندارد
        if (pendingLevel.getLevelType() != LevelType.CONVEYOR_BELT
                && selectedPlants.isEmpty()) {
            view.printError("Select at least one plant first (add plant -t <TYPE>).");
            return;
        }
        List<PlantType> plants = new ArrayList<>(selectedPlants);
        GameSession session = gameService.createSession(pendingLevel, plants);
        session.setLevelNumber(pendingLevelNumber);
        session.setBoostedPlants(new ArrayList<>(boostedPlants));
        appState.setCurrentSession(session);
        appState.setCurrentMenu(MenuType.IN_GAME);
        // PLANT_WHAT_YOU_GET: امواج شروع نمی‌شوند تا "start zombie waves" زده شود
        if (pendingLevel.getLevelType() != LevelType.PLANT_WHAT_YOU_GET) {
            session.setWaveStarted(true);
        }
        view.printSuccess("Game started! Use 'advance time -t <n> ticks' to play.");
        mapView.printMap(session);
    }

    /** CONVEYOR_BELT: شروع مستقیم بدون plant select */
    private void startGameDirectly(AppState appState) {
        GameSession session = gameService.createSession(
                pendingLevel, new ArrayList<>());
        session.setLevelNumber(pendingLevelNumber);
        session.setWaveStarted(true);
        appState.setCurrentSession(session);
        appState.setCurrentMenu(MenuType.IN_GAME);
        view.printSuccess("Conveyor Belt level started!");
        mapView.printMap(session);
    }

    /** PLANT_WHAT_YOU_GET: شروع امواج زامبی */
    public void startZombieWaves(AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) { view.printError("No active game."); return; }
        if (session.isWaveStarted()) {
            view.printError("Waves already started."); return;
        }
        session.setWaveStarted(true);
        view.printSuccess("Zombie waves started! Good luck!");
    }

    // ════════════════════════════════════════════════════════
    //  IN-GAME COMMANDS
    // ════════════════════════════════════════════════════════

    public void advanceTime(int ticks, AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) { view.printError("No active game."); return; }
        gameService.advanceTime(session, ticks);
    }

    /**
     * کاشت گیاه.
     * CONVEYOR_BELT: از صف نوار مصرف می‌کند.
     * سایر مراحل: از selectedPlants بررسی می‌کند.
     */
    public void plantPlant(Matcher m, AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) { view.printError("Not in a game."); return; }
        String typeName = m.group(1).toUpperCase();
        int x = Integer.parseInt(m.group(2));
        int y = Integer.parseInt(m.group(3));
        PlantType type = parsePlantType(typeName);
        if (type == null) { view.printError("Unknown plant: " + typeName); return; }

        // ── CONVEYOR_BELT: بررسی نوار ────────────────────────────
        if (session.getLevel().getLevelType() == LevelType.CONVEYOR_BELT) {
            if (!session.hasConveyorPlant(type)) {
                view.printError(typeName + " is not available on the conveyor right now."
                        + " Available: " + session.getConveyorQueue());
                return;
            }
            session.useConveyorPlant(type);
        } else {
            // سایر مراحل: باید از selectedPlants باشد
            if (!session.getSelectedPlants().contains(type)) {
                view.printError(typeName + " was not selected for this level.");
                return;
            }
        }
        try {
            gameService.plantPlant(session, type, x, y);
            view.printSuccess(typeName + " planted at (" + x + ", " + y + ").");
        } catch (GameException e) {
            view.printError(e.getMessage());
        }
    }

    public void pluckPlant(Matcher m, AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) { view.printError("Not in a game."); return; }
        int x = Integer.parseInt(m.group(1));
        int y = Integer.parseInt(m.group(2));
        try {
            gameService.pluckPlant(session, x, y);
            view.printSuccess("Plant removed from (" + x + ", " + y + ").");
        } catch (GameException e) {
            view.printError(e.getMessage());
        }
    }

    public void collectSun(Matcher m, AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) { view.printError("Not in a game."); return; }
        int x = Integer.parseInt(m.group(1));
        int y = Integer.parseInt(m.group(2));
        Tile tile = session.getGameMap().getTile(x, y);
        if (tile != null && tile.getPlant() instanceof com.pvz2.model.plants.GenericPlant) {
            com.pvz2.model.plants.GenericPlant gp = (com.pvz2.model.plants.GenericPlant) tile.getPlant();
            if (gp.isSunPending()) {
                try {
                    sunService.collectPlantProducedSun(session, x, y);
                } catch (GameException e) { view.printError(e.getMessage()); }
                return;
            }
        }
        int gained = sunService.collectSun(session, x, y);
        if (gained < 0) view.printError("No sun at (" + x + ", " + y + ").");
        else view.printSuccess("Collected " + gained + " sun! Total: " + session.getSunAmount());
    }

    public void feedPlant(Matcher m, AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) { view.printError("Not in a game."); return; }
        int x = Integer.parseInt(m.group(1));
        int y = Integer.parseInt(m.group(2));
        try {
            gameService.feedPlant(session, x, y);
        } catch (GameException e) { view.printError(e.getMessage()); }
    }

    public void showMap(AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) { view.printError("Not in a game."); return; }
        mapView.printMap(session);
    }

    public void showPlantsStatus(AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) { view.printError("Not in a game."); return; }
        view.printHeader("Plant Status");
        for (PlantType type : session.getSelectedPlants()) {
            // حالت CONVEYOR_BELT: نمایش صف
            if (session.getLevel().getLevelType() == LevelType.CONVEYOR_BELT) {
                boolean avail = session.hasConveyorPlant(type);
                view.printRaw("  " + type.name() + (avail ? " [ON CONVEYOR]" : " [waiting]"));
                continue;
            }
            PlantStats stats = PlantDataRegistry.getInstance().getStats(type);
            int cost = stats != null ? stats.getSunCost() : 0;
            view.printRaw("  " + type.name() + "  cost:" + cost);
        }
    }

    public void showTileStatus(Matcher m, AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) { view.printError("Not in a game."); return; }
        int x = Integer.parseInt(m.group(1));
        int y = Integer.parseInt(m.group(2));
        Tile tile = session.getGameMap().getTile(x, y);
        if (tile == null) { view.printError("Invalid position."); return; }
        view.printHeader("Tile (" + x + ", " + y + ")");
        view.printRaw("  Type: " + tile.getType().toString());
        if (tile.getPlant() != null) {
            com.pvz2.model.plants.Plant p = tile.getPlant();
            boolean prot = session.isProtectedPosition(x, y);
            view.printRaw("  Plant: " + p.getType().name()
                    + (prot ? " ★PROTECTED★" : "")
                    + "  HP: " + p.getCurrentHealth() + "/" + p.getMaxHealth());
        } else {
            view.printRaw("  Plant: (none)");
        }
        session.getActiveZombies().stream()
            .filter(z -> (int) Math.round(z.getX()) == x && z.getY() == y)
            .forEach(z -> view.printRaw("  Zombie: " + z.getType().name()
                    + "  HP: " + z.getCurrentHealth()));
    }

    public void showZombiesInfo(AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) { view.printError("Not in a game."); return; }
        if (session.getActiveZombies().isEmpty()) {
            view.printInfo("No zombies on the field."); return;
        }
        view.printHeader("Zombies on the Field");
        for (com.pvz2.model.zombies.Zombie z : session.getActiveZombies()) {
            view.printRaw(z.getType().name() + ":");
            view.printRaw("  position: " + (int)z.getX() + ", " + z.getY());
            view.printRaw("  health: " + z.getCurrentHealth());
            if (!z.getArmors().isEmpty())
                view.printRaw("  armor: " + z.getArmors());
            if (!z.getActiveEffects().isEmpty())
                view.printRaw("  effects: " + z.getActiveEffects());
        }
    }

    // ════════════════════════════════════════════════════════
    //  SPECIAL LEVEL STATUS COMMANDS
    // ════════════════════════════════════════════════════════

    /**
     * نمایش وضعیت مرحله ویژه (برای: Timed War، Dead Line، Love Your Plants، Save Our Seeds).
     * دستور: show special status
     */
    public void showSpecialLevelStatus(AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) { view.printError("Not in a game."); return; }
        Level level = session.getLevel();
        if (level == null) { view.printError("No level data."); return; }
        switch (level.getLevelType()) {
            case TIMED_WAR:
                showTimedWarStatus(session, level);
                break;
            case DEAD_LINE:
                view.printRaw(ConsoleView.YELLOW + "🚫 DEAD LINE Level"
                        + ConsoleView.RESET);
                view.printRaw("  Forbidden column: " + level.getDeadLineColumn());
                view.printRaw("  Status: "
                        + (session.isInProgress() ? "IN PROGRESS" : session.getResult()));
                break;
            case LOVE_YOUR_PLANTS:
                view.printRaw(ConsoleView.YELLOW + "💚 Love Your Plants Level"
                        + ConsoleView.RESET);
                view.printRaw("  Plants lost: " + session.getPlantsLost()
                        + " / " + level.getMaxPlantsLost() + " allowed");
                view.printRaw("  Remaining: "
                        + (level.getMaxPlantsLost() - session.getPlantsLost())
                        + " plant(s) can still be lost.");
                break;
            case SAVE_OUR_SEEDS:
                view.printRaw(ConsoleView.YELLOW + "🌱 Save Our Seeds Level"
                        + ConsoleView.RESET);
                List<int[]> pos = session.getProtectedPlantPositions();
                view.printRaw("  Protected plants still alive: " + pos.size());
                for (int[] p : pos)
                    view.printRaw("    ★ (" + p[0] + ", " + p[1] + ")");
                break;
            case CONVEYOR_BELT:
                showConveyorStatus(appState);
                break;
            case NIGHT_OPS:
                view.printRaw(ConsoleView.BLUE + "🌙 Night Ops Level"
                        + ConsoleView.RESET);
                view.printRaw("  Sun: " + session.getSunAmount()
                        + " (no sky drops!)");
                break;
            case PLANT_WHAT_YOU_GET:
                view.printRaw(ConsoleView.YELLOW + "🌿 Plant What You Get Level"
                        + ConsoleView.RESET);
                view.printRaw("  Sun: " + session.getSunAmount());
                view.printRaw("  Waves started: " + session.isWaveStarted());
                if (!session.isWaveStarted())
                    view.printRaw("  Type 'start zombie waves' when ready.");
                break;
            default:
                view.printInfo("This is a normal level. No special status.");
                break;
        }
    }

    private void showTimedWarStatus(GameSession session, Level level) {
        view.printRaw(ConsoleView.YELLOW + "⏱ TIMED WAR Status" + ConsoleView.RESET);
        int remaining = session.getTimedWarRemainingSeconds();
        view.printRaw("  Time remaining: " + remaining + "s");
        if (level.isTimedWarSunMode()) {
            view.printRaw("  Sun collected: "
                    + session.getTimedWarSunAchieved()
                    + " / " + level.getTimedWarSunTarget() + " target");
        } else {
            view.printRaw("  Zombies killed: "
                    + session.getTimedWarKillsAchieved()
                    + " / " + level.getTimedWarZombieTarget() + " target");
        }
        view.printRaw("  Status: " + (session.isTimedWarGoalReached()
                ? ConsoleView.GREEN + "GOAL REACHED!" + ConsoleView.RESET
                : session.isTimedWarExpired()
                ? ConsoleView.RED + "TIME EXPIRED" + ConsoleView.RESET
                : "In progress..."));
    }

    /**
     * نمایش وضعیت نوار کناری (Conveyor Belt).
     * دستور: show conveyor
     */
    public void showConveyorStatus(AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) { view.printError("Not in a game."); return; }
        view.printRaw(ConsoleView.YELLOW + "📦 CONVEYOR BELT Status" + ConsoleView.RESET);
        List<com.pvz2.model.enums.PlantType> queue = session.getConveyorQueue();
        if (queue.isEmpty()) {
            view.printRaw("  No plants available right now. Wait for next delivery...");
        } else {
            view.printRaw("  Available plants on conveyor:");
            for (com.pvz2.model.enums.PlantType t : queue) {
                PlantStats stats = PlantDataRegistry.getInstance().getStats(t);
                int cost = stats != null ? stats.getSunCost() : 0;
                view.printRaw("    → " + t.name() + "  (cost: " + cost + " sun)");
            }
        }
        int elapsed = session.getCurrentTick() - session.getLastConveyorTick();
        int remaining = Math.max(0, 120 - elapsed);
        view.printRaw("  Next plant in: " + (remaining / 10) + "s");
    }

    // ════════════════════════════════════════════════════════
    //  CHEATS
    // ════════════════════════════════════════════════════════

    public void releaseNuke(AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) { view.printError("Not in a game."); return; }
        gameService.releaseNuke(session);
    }

    public void removeCooldown(AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) { view.printError("Not in a game."); return; }
        gameService.removeCooldown(session);
    }

    public void addPlantFood(AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) { view.printError("Not in a game."); return; }
        gameService.addPlantFoodCheat(session);
    }

    public void spawnZombie(Matcher m, AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) { view.printError("Not in a game."); return; }
        gameService.spawnZombieCheat(session,
                m.group(1), Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3)));
    }

    public void cheatAddSuns(int count, AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) { view.printError("Not in a game."); return; }
        session.addSun(count);
        view.printSuccess("Added " + count + " sun. Total: " + session.getSunAmount());
    }

    public void cheatCurrency(Matcher m, AppState appState) {
        User user = appState.getCurrentUser();
        if (user == null) {
            return;
        }
        int amount = Integer.parseInt(m.group(1));
        String type = m.group(2);
        if (type.equalsIgnoreCase("coin")) {
            user.setCoins(user.getCoins() + amount);
            view.printSuccess("Added " + amount + " coins! Total: "
                    + user.getCoins());
        } else {
            user.setGems(user.getGems() + amount);
            view.printSuccess("Added " + amount + " gems! Total: "
                    + user.getGems());
        }
    }

    public void showSunAmount(AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) {
            view.printError("Not in a game.");
            return;
        }
        view.printInfo("☀ Current sun: "
                + ConsoleView.YELLOW + session.getSunAmount());
    }



    // ════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════

    private PlantType parsePlantType(String name) {
        try { return PlantType.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private boolean userHasPlant(com.pvz2.model.User user, PlantType type) {
        return user.getUnlockedPlants() != null
                && user.getUnlockedPlants().contains(type.name());
    }

    private void printLevelTypeHint(Level level) {
        switch (level.getLevelType()) {
            case LOCKED_PLANTS:
                view.printRaw(ConsoleView.YELLOW
                        + "🔒 LOCKED PLANTS level — some plants cannot be selected!"
                        + ConsoleView.RESET);
                break;
            case SAVE_OUR_SEEDS:
                view.printRaw(ConsoleView.RED
                        + "🌱 SAVE OUR SEEDS — pre-placed plants must be protected!"
                        + ConsoleView.RESET);
                break;
            case TIMED_WAR:
                view.printRaw(ConsoleView.YELLOW
                        + "⏱ TIMED WAR — complete the objective before time runs out!"
                        + ConsoleView.RESET);
                break;
            case NIGHT_OPS:
                view.printRaw(ConsoleView.BLUE
                        + "🌙 NIGHT OPS — no sun falls! Use sun producers."
                        + ConsoleView.RESET);
                break;
            case DEAD_LINE:
                view.printRaw(ConsoleView.RED
                        + "🚫 DEAD LINE — don't let zombies cross column "
                        + level.getDeadLineColumn() + "!"
                        + ConsoleView.RESET);
                break;
            case LOVE_YOUR_PLANTS:
                view.printRaw(ConsoleView.YELLOW
                        + "💚 LOVE YOUR PLANTS — max " + level.getMaxPlantsLost()
                        + " plant losses allowed!"
                        + ConsoleView.RESET);
                break;
            case PLANT_WHAT_YOU_GET:
                view.printRaw(ConsoleView.YELLOW
                        + "🌿 PLANT WHAT YOU GET — no sun producers! "
                        + "Start with " + level.getInitialSunAmount() + " sun."
                        + ConsoleView.RESET);
                break;
            default:
                break;
        }
    }

    public void setLevelProgressService(LevelProgressService levelProgressService) {
        this.levelProgressService = levelProgressService;
    }
}
