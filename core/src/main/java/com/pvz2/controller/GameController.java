package com.pvz2.controller;

import com.pvz2.model.AppState;
import com.pvz2.model.GameSession;
import com.pvz2.model.Level;
import com.pvz2.model.User;
import com.pvz2.model.enums.LevelType;
import com.pvz2.model.enums.MenuType;
import com.pvz2.model.enums.PlantType;
import com.pvz2.model.plants.Plant;
import com.pvz2.model.plants.PlantDataRegistry;
import com.pvz2.model.plants.PlantStats;
import com.pvz2.model.tiles.Tile;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.service.GameService;
import com.pvz2.service.SunService;
import com.pvz2.view.ConsoleView;
import com.pvz2.view.MapView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * کنترلر اصلی بازی — انتخاب گیاه، درون بازی، مرحله.
 */
public class GameController {

    private final GameService gameService;
    private final SunService sunService;
    private final ConsoleView view;
    private final MapView mapView;
    private com.pvz2.service.LevelProgressService levelProgressService;

    private Level pendingLevel;
    private int pendingLevelNumber;
    private List<PlantType> selectedPlants;
    private List<PlantType> boostedPlants;

    public GameController(GameService gameService, SunService sunService,
                          ConsoleView view, MapView mapView) {
        this.gameService = gameService;
        this.sunService = sunService;
        this.view = view;
        this.mapView = mapView;
        this.selectedPlants = new ArrayList<>();
        this.boostedPlants = new ArrayList<>();
        this.pendingLevelNumber = 1;
    }

    public void setLevelProgressService(com.pvz2.service.LevelProgressService lps) {
        this.levelProgressService = lps;
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
            com.pvz2.model.enums.ChapterType chapter =
                    com.pvz2.model.enums.ChapterType.valueOf(chapterName.toUpperCase());
            if (levelProgressService != null) {
                levelProgressService.showChapterLevels(
                        appState.getCurrentUser(), chapter);
            }
        } catch (IllegalArgumentException e) {
            view.printError("Unknown chapter: " + chapterName);
        }
    }

    public void enterChapterWithLevel(String chapterName, int levelNum,
                                      AppState appState) {
        com.pvz2.model.enums.ChapterType chapter;
        try {
            chapter = com.pvz2.model.enums.ChapterType.valueOf(chapterName.toUpperCase());
        } catch (IllegalArgumentException e) {
            view.printError("Unknown chapter: " + chapterName);
            return;
        }
        com.pvz2.model.User user = appState.getCurrentUser();
        if (levelProgressService != null
                && !levelProgressService.isLevelUnlocked(
                user, chapter, levelNum)) {
            view.printError("Level " + levelNum
                    + " of " + chapterName
                    + " is locked! Complete previous levels first.");
            if (levelProgressService != null) {
                levelProgressService.showChapterLevels(user, chapter);
            }
            return;
        }
        pendingLevel = buildLevel(chapter, levelNum);
        pendingLevelNumber = levelNum;
        selectedPlants = new ArrayList<>();
        boostedPlants = new ArrayList<>();
        appState.setCurrentMenu(com.pvz2.model.enums.MenuType.PLANT_SELECT);
        view.printHeader("🌿 Plant Selection — "
                + chapterName + " Level " + levelNum);
        view.printInfo("Select up to " + pendingLevel.getPlantSlots()
                + " plants. Type 'show available plants'.");
    }

    public void enterChapter(String chapterName, AppState appState) {
        com.pvz2.model.enums.ChapterType chapter;
        try {
            chapter = com.pvz2.model.enums.ChapterType.valueOf(
                    chapterName.toUpperCase());
        } catch (IllegalArgumentException e) {
            view.printError("Unknown chapter: " + chapterName
                    + ". Available: ANCIENT_EGYPT, FROSTBITE_CAVES,"
                    + " BIG_WAVE_BEACH, DARK_AGES");
            return;
        }
        if (levelProgressService != null) {
            levelProgressService.showChapterLevels(
                    appState.getCurrentUser(), chapter);
        }
        view.printInfo("Use: menu enter chapter "
                + chapterName + " -l <1-4>");
    }

    private Level buildLevel(com.pvz2.model.enums.ChapterType chapter,
                             int levelNumber) {
        LevelType type = getLevelType(chapter, levelNumber);
        Level level = new Level(levelNumber, chapter, type);
        int baseDiff = 400 + (getChapterIndex(chapter) * 200)
                + (levelNumber - 1) * 100;
        level.setInitialWaveDifficulty(baseDiff);
        level.setWaveCount(levelNumber == 4 ? 5 : 3);
        level.setMapRows(5);
        level.setMapCols(9);
        level.setPlantSlots(8);
        if (type == LevelType.NIGHT_OPS) {
            level.setNightOpsSun(150);
        }
        if (type == LevelType.DEAD_LINE) {
            level.setDeadLineColumn(5);
        }
        if (type == LevelType.LOVE_YOUR_PLANTS) {
            level.setMaxPlantsLost(3);
        }
        if (type == LevelType.PLANT_WHAT_YOU_GET) {
            level.setInitialSunAmount(750);
        }
        return level;
    }

    private LevelType getLevelType(com.pvz2.model.enums.ChapterType chapter,
                                   int levelNumber) {
        if (levelNumber == 1) {
            return LevelType.NORMAL;
        }
        if (levelNumber == 4) {
            return LevelType.BOSS;
        }
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

    private int getChapterIndex(com.pvz2.model.enums.ChapterType chapter) {
        switch (chapter) {
            case ANCIENT_EGYPT:   return 0;
            case FROSTBITE_CAVES: return 1;
            case BIG_WAVE_BEACH:  return 2;
            case DARK_AGES:       return 3;
            default:              return 0;
        }
    }

    public void showAllPlantsForSelect(AppState appState) {
        User user = appState.getCurrentUser();
        view.printHeader("All Available Plants");
        for (PlantType type : PlantType.values()) {
            PlantStats stats = PlantDataRegistry.getInstance().getStats(type);
            if (stats == null) {
                continue;
            }
            boolean owned = user.getUnlockedPlants() != null
                    && user.getUnlockedPlants().contains(type.name());
            String status = owned
                    ? ConsoleView.GREEN + "[owned]" + ConsoleView.RESET
                    : ConsoleView.RED + "[locked]" + ConsoleView.RESET;
            view.getGameView().printAllPlantsForSelectRow(type.name(), stats.getSunCost(), status);
        }
    }

    public void showAvailablePlants(AppState appState) {
        User user = appState.getCurrentUser();
        view.printHeader("Available Plants for This Level");
        List<String> unlocked = user.getUnlockedPlants();
        if (unlocked == null || unlocked.isEmpty()) {
            view.printInfo("You need to unlock plants first "
                    + "(menu collection purchase-plant -p <name>).");
            view.printInfo("Starter plants: SUNFLOWER, PEASHOOTER, WALL_NUT");
            return;
        }
        int i = 1;
        for (String plantName : unlocked) {
            boolean selected = false;
            for (PlantType t : selectedPlants) {
                if (t.name().equals(plantName)) {
                    selected = true;
                    break;
                }
            }
            String mark = selected
                    ? ConsoleView.GREEN + " ✔ SELECTED" + ConsoleView.RESET : "";
            view.printRaw("  " + i++ + ". " + plantName + mark);
        }
        view.printRaw(ConsoleView.CYAN + "  Selected: "
                + selectedPlants.size() + "/"
                + pendingLevel.getPlantSlots() + ConsoleView.RESET);
    }

    public void addPlantToSelect(String typeName, AppState appState) {
        User user = appState.getCurrentUser();
        PlantType type;
        try {
            type = PlantType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            view.printError("Unknown plant: " + typeName);
            return;
        }
        if (!userHasPlant(user, type)) {
            view.printError("You don't own " + typeName
                    + ". Purchase it first from the collection menu.");
            return;
        }
        if (selectedPlants.contains(type)) {
            view.printError(typeName + " is already selected.");
            return;
        }
        if (selectedPlants.size() >= pendingLevel.getPlantSlots()) {
            view.printError("Plant slots full ("
                    + pendingLevel.getPlantSlots() + " max).");
            return;
        }
        selectedPlants.add(type);
        view.printSuccess(typeName + " added. ("
                + selectedPlants.size() + "/"
                + pendingLevel.getPlantSlots() + ")");
    }

    private boolean userHasPlant(User user, PlantType type) {
        if (user.getUnlockedPlants() == null) {
            return false;
        }
        return user.getUnlockedPlants().contains(type.name());
    }

    public void removePlantFromSelect(String typeName, AppState appState) {
        PlantType type;
        try {
            type = PlantType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            view.printError("Unknown plant: " + typeName);
            return;
        }
        if (!selectedPlants.remove(type)) {
            view.printError(typeName + " is not selected.");
            return;
        }
        view.printSuccess(typeName + " removed from selection.");
    }

    public void boostPlant(String typeName, AppState appState) {
        User user = appState.getCurrentUser();
        PlantType type;
        try {
            type = PlantType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            view.printError("Unknown plant: " + typeName);
            return;
        }
        if (!selectedPlants.contains(type)) {
            view.printError(typeName + " must be selected first.");
            return;
        }
        if (user.getGems() < 2) {
            view.printError("Need 2 gems to boost. You have: "
                    + user.getGems());
            return;
        }
        user.setGems(user.getGems() - 2);
        boostedPlants.add(type);
        view.printSuccess(typeName + " will be boosted when planted! (-2 gems)");
    }

    public void startGame(AppState appState) {
        if (pendingLevel == null) {
            view.printError("No level selected.");
            return;
        }
        if (selectedPlants.isEmpty()) {
            view.printError("Select at least one plant first.");
            return;
        }
        GameSession session = gameService.createSession(
                pendingLevel, selectedPlants);
        session.setLevelNumber(pendingLevelNumber);
        session.setBoostedPlants(new ArrayList<>(boostedPlants));
        appState.setCurrentSession(session);
        appState.setCurrentMenu(MenuType.IN_GAME);
        if (pendingLevel.getLevelType() != LevelType.PLANT_WHAT_YOU_GET) {
            session.setWaveStarted(true);
        }
        view.printSuccess("Game started! Type 'advance time -t <n> ticks' to play.");
        mapView.printMap(session);
    }

    public void startZombieWaves(AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) {
            view.printError("No active game session.");
            return;
        }
        session.setWaveStarted(true);
        view.printSuccess("Zombie waves started!");
    }

    public void advanceTime(int ticks, AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) {
            view.printError("No active game session.");
            return;
        }
        gameService.advanceTime(session, ticks);
    }

    public void plantPlant(Matcher m, AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) {
            view.printError("Not in a game.");
            return;
        }
        String typeName = m.group(1).toUpperCase();
        int x = Integer.parseInt(m.group(2));
        int y = Integer.parseInt(m.group(3));
        PlantType type;
        try {
            type = PlantType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            view.printError("Unknown plant: " + typeName);
            return;
        }
        if (!session.getSelectedPlants().contains(type)) {
            view.printError(typeName + " was not selected for this level.");
            return;
        }
        gameService.plantPlant(session, type, x, y);
        view.printSuccess(typeName + " planted at (" + x + ", " + y + ").");
    }

    public void pluckPlant(Matcher m, AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) {
            view.printError("Not in a game.");
            return;
        }
        int x = Integer.parseInt(m.group(1));
        int y = Integer.parseInt(m.group(2));
        gameService.pluckPlant(session, x, y);
        view.printSuccess("Plant removed from (" + x + ", " + y + ").");
    }

    public void collectSun(Matcher m, AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) {
            view.printError("Not in a game.");
            return;
        }
        int x = Integer.parseInt(m.group(1));
        int y = Integer.parseInt(m.group(2));
        Tile tile = session.getGameMap().getTile(x, y);
        if (tile != null && tile.getPlant() != null
                && tile.isHasSunPending()) {
            sunService.collectPlantProducedSun(session, x, y);
        } else {
            int gained = sunService.collectSun(session, x, y);
            if (gained < 0) {
                view.printError("No sun at (" + x + ", " + y + ").");
            } else {
                view.printSuccess("Collected " + gained + " sun! Total: "
                        + session.getSunAmount());
            }
        }
    }

    public void feedPlant(Matcher m, AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) {
            view.printError("Not in a game.");
            return;
        }
        int x = Integer.parseInt(m.group(1));
        int y = Integer.parseInt(m.group(2));
        gameService.feedPlant(session, x, y);
    }

    public void showMap(AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) {
            view.printError("Not in a game.");
            return;
        }
        mapView.printMap(session);
    }

    public void showPlantsStatus(AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) {
            view.printError("Not in a game.");
            return;
        }
        view.printHeader("🌱 Plants Status");
        for (int r = 1; r <= session.getGameMap().getRows(); r++) {
            for (int c = 1; c <= session.getGameMap().getCols(); c++) {
                Tile tile = session.getGameMap().getTile(c, r);
                if (tile == null || tile.getPlant() == null) {
                    continue;
                }
                printPlantStatus(tile.getPlant(), c, r);
            }
        }
    }

    private void printPlantStatus(Plant plant, int x, int y) {
        PlantStats stats = PlantDataRegistry.getInstance()
                .getStats(plant.getType());
        int cost = stats != null ? stats.getSunCost() : 0;
        String cooldown = plant.getRemainingCooldownTicks() > 0
                ? String.format("%.1fs", plant.getRemainingCooldownTicks() / 10.0)
                : "Ready";
        view.getGameView().printPlantStatus(x, y, plant.getType().name(),
                plant.getCurrentHealth(), plant.getMaxHealth(),
                cost, cooldown, plant.isFrozen());
    }

    public void showTileStatus(Matcher m, AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) {
            view.printError("Not in a game.");
            return;
        }
        int x = Integer.parseInt(m.group(1));
        int y = Integer.parseInt(m.group(2));
        Tile tile = session.getGameMap().getTile(x, y);
        if (tile == null) {
            view.printError("Invalid position.");
            return;
        }
        view.printHeader("Tile (" + x + ", " + y + ")");
        view.printRaw(ConsoleView.CYAN + "  Type: "
                + ConsoleView.RESET + tile.getType().name());
        if (tile.getPlant() != null) {
            printPlantStatus(tile.getPlant(), x, y);
        }
        for (Zombie z : session.getActiveZombies()) {
            if (z.getY() == y && Math.abs(z.getX() - x) < 0.5) {
                printZombieStatus(z);
            }
        }
    }

    private void printZombieStatus(Zombie zombie) {
        view.getGameView().printZombieStatus(
                zombie.getType().name(), zombie.getCurrentHealth(),
                zombie.getX(), zombie.getY(), zombie.getArmors().toString());
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

    public void showZombiesInfo(AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) {
            view.printError("Not in a game.");
            return;
        }
        view.printHeader("🧟 Zombies Info");
        if (session.getActiveZombies().isEmpty()) {
            view.printInfo("No zombies on the field.");
            return;
        }
        for (Zombie z : session.getActiveZombies()) {
            printDetailedZombieInfo(z);
        }
    }

    private void printDetailedZombieInfo(Zombie zombie) {
        view.printRaw(ConsoleView.RED + zombie.getType().name()
                + ":" + ConsoleView.RESET);
        view.getGameView().printZombiePosition(zombie.getX(), zombie.getY());
        view.printRaw("  health: " + zombie.getCurrentHealth());
        if (!zombie.getArmors().isEmpty()) {
            view.printRaw("  armor:");
            zombie.getArmors().forEach((at, hp) ->
                    view.printRaw("    " + at.name() + ": " + hp));
        }
        if (!zombie.getActiveEffects().isEmpty()) {
            view.printRaw("  effects:");
            zombie.getActiveEffects().forEach((ef, ticks) ->
                    view.getGameView().printZombieEffect(ef.name(), ticks / 10.0));
        }
    }

    public void cheatAddSuns(int count, AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) {
            view.printError("Not in a game.");
            return;
        }
        session.addSun(count);
        view.printSuccess("Added " + count + " sun! Total: "
                + session.getSunAmount());
    }

    public void releaseNuke(AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) {
            view.printError("Not in a game.");
            return;
        }
        gameService.releaseNuke(session);
    }

    public void removeCooldown(AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) {
            view.printError("Not in a game.");
            return;
        }
        gameService.removeCooldown(session);
    }

    public void addPlantFood(AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) {
            view.printError("Not in a game.");
            return;
        }
        gameService.addPlantFoodCheat(session);
    }

    public void spawnZombie(Matcher m, AppState appState) {
        GameSession session = appState.getCurrentSession();
        if (session == null) {
            view.printError("Not in a game.");
            return;
        }
        String type = m.group(1);
        int x = Integer.parseInt(m.group(2));
        int y = Integer.parseInt(m.group(3));
        gameService.spawnZombieCheat(session, type, x, y);
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
}
