package com.pvz2.service;
import com.pvz2.model.AppState;
import com.pvz2.model.User;


import com.pvz2.model.*;
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
 * سرویس اصلی بازی — هماهنگی میان combat، موج، خورشید.
 */
public class GameService {

    private final WaveService waveService;
    private CombatService combatService;
    private final SunService sunService;
    private final ConsoleView view;
    private final MapView mapView;

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

    private void applyBoosts(GameSession session) {
        List<PlantType> boosted = session.getBoostedPlants();
        if (boosted == null || boosted.isEmpty()) {
            return;
        }
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

    private void applyLevelTypeSetup(GameSession session, Level level) {
        if (level.getChapter() == ChapterType.DARK_AGES) {
            session.setSunAmount(50);
        }
        if (level.getLevelType() == LevelType.PLANT_WHAT_YOU_GET) {
            int sun = level.getInitialSunAmount() > 0
                    ? level.getInitialSunAmount() : 500;
            session.setSunAmount(sun);
            session.setWaveStarted(false);
        }
        if (level.getLevelType() == LevelType.NIGHT_OPS) {
            int sun = level.getNightOpsSun() > 0 ? level.getNightOpsSun() : 150;
            session.setSunAmount(sun);
        }
    }

    private void setupMapForChapter(GameMap map, Level level) {
        if (level.getChapter() == ChapterType.ANCIENT_EGYPT) {
            setupEgyptTombstones(map);
        } else if (level.getChapter() == ChapterType.FROSTBITE_CAVES) {
            setupFrostbiteTiles(map);
        } else if (level.getChapter() == ChapterType.BIG_WAVE_BEACH) {
            setupBeachWater(map);
        } else if (level.getChapter() == ChapterType.DARK_AGES) {
            setupDarkAges(map);
        }
    }

    private void setupEgyptTombstones(GameMap map) {
        int count = RandomUtil.between(2, 5);
        for (int i = 0; i < count; i++) {
            int col = RandomUtil.between(3, map.getCols() - 1);
            int row = RandomUtil.between(1, map.getRows());
            Tile t = map.getTile(col, row);
            if (t != null && t.isPlantable()) {
                map.setTileType(col, row, TileType.TOMBSTONE);
            }
        }
    }

    private void setupFrostbiteTiles(GameMap map) {
        int count = RandomUtil.between(1, 3);
        for (int i = 0; i < count; i++) {
            int col = RandomUtil.between(2, map.getCols() - 1);
            int row = RandomUtil.between(1, map.getRows());
            TileType type = RandomUtil.chance(0.5)
                    ? TileType.SLIPPERY_UP
                    : TileType.SLIPPERY_DOWN;
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

    public void advanceTime(GameSession session, int ticks) {
        for (int i = 0; i < ticks; i++) {
            if (!session.isInProgress()) {
                break;
            }
            session.advanceTick();
            processOneTick(session);
        }
        mapView.printMap(session);
        combatService.checkWinCondition(session);
    }

    private void processOneTick(GameSession session) {
        sunService.tickSkyDrops(session);
        manageWaves(session);
        combatService.processTick(session);
    }

    private void manageWaves(GameSession session) {
        if (session.getWaves() == null || session.getWaves().isEmpty()) {
            return;
        }
        Wave current = session.getCurrentWave();
        if (current == null) {
            return;
        }
        if (!current.isStarted()) {
            startWave(session, current);
            return;
        }
        if (!current.isFinalWave() && session.hasMoreWaves()
                && current.shouldTriggerNextWave()) {
            session.setCurrentWaveIndex(session.getCurrentWaveIndex() + 1);
            Wave next = session.getCurrentWave();
            if (next != null) {
                beforeWaveStart(session, next);
            }
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
        if (session.getGameMap().getChapter() != ChapterType.DARK_AGES) {
            return;
        }
        int count = RandomUtil.between(0, 3);
        for (int i = 0; i < count; i++) {
            int col = RandomUtil.between(1, session.getGameMap().getCols());
            int row = RandomUtil.between(1, session.getGameMap().getRows());
            Tile tile = session.getGameMap().getTile(col, row);
            if (tile != null && tile.getPlant() == null
                    && !tile.isTombstone()) {
                TileType tombType = getTombstoneType();
                session.getGameMap().setTileType(col, row, tombType);
                view.printRaw("\u001B[35m⛰ A tombstone appeared at ("
                        + col + "," + row + ")!\u001B[0m");
            }
        }
    }

    private TileType getTombstoneType() {
        double roll = RandomUtil.nextDouble();
        if (roll < 0.6) {
            return TileType.DARK_TOMBSTONE;
        }
        return TileType.DARK_TOMBSTONE;
    }

    public void plantPlant(GameSession session, PlantType type, int x, int y) {
        Tile tile = session.getGameMap().getTile(x, y);
        if (tile == null) {
            throw new com.pvz2.exception.GameException(
                    "Invalid position (" + x + ", " + y + ").");
        }
        if (PlantFactory.isWaterPlant(type)) {
            if (!tile.isWater()) {
                throw new com.pvz2.exception.GameException(
                        type.name() + " can only be planted on water.");
            }
        } else if (type == PlantType.PUMPKIN) {
            if (tile.getPlant() == null) {
                throw new com.pvz2.exception.GameException(
                        "Pumpkin needs a plant to protect underneath.");
            }
        } else if (type == PlantType.PEA_POD && tile.getPlant() instanceof PeaPodPlant) {
            // Pea Pod روی Pea Pod موجود: افزودن یک سر (بدون کاشت جدید)
            PeaPodPlant existingPod = (PeaPodPlant) tile.getPlant();
            int cost = PlantFactory.getSunCost(type);
            if (session.getSunAmount() < cost)
                throw new com.pvz2.exception.GameException(
                        "Not enough sun. Need " + cost + ", have " + session.getSunAmount() + ".");
            if (!existingPod.addHead())
                throw new com.pvz2.exception.GameException("Pea Pod already at maximum 5 heads.");
            session.spendSun(cost);
            return;
        } else {
            if (!tile.isPlantable()) {
                throw new com.pvz2.exception.GameException(
                        "Cannot plant at (" + x + ", " + y + ").");
            }
        }
        int cost = PlantFactory.getSunCost(type);
        if (session.getSunAmount() < cost) {
            throw new com.pvz2.exception.GameException(
                    "Not enough sun. Need " + cost
                            + ", have " + session.getSunAmount() + ".");
        }
        session.spendSun(cost);
        Plant plant = PlantFactory.createWithUpgrade(type,
                AppState.getInstance().getCurrentUser());
        if (plant == null) {
            throw new com.pvz2.exception.GameException(
                    "Unknown plant type: " + type.name());
        }
        plant.setX(x);
        plant.setY(y);
        if (type == PlantType.PUMPKIN) {
            tile.setSecondLayerPlant(tile.getPlant());
            tile.setPlant(plant);
        } else if (type == PlantType.LILY_PAD && tile.isWater()) {
            tile.setPlant(plant);
        } else {
            tile.setPlant(plant);
        }
        applyBoostIfStored(plant, session);
    }

    private void applyBoostIfStored(Plant plant, GameSession session) {
        User user = AppState.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        if (plant.isHasStoredBoost()) {
            plant.activatePlantFood(session);
            plant.setHasStoredBoost(false);
        }
    }

    public void pluckPlant(GameSession session, int x, int y) {
        Tile tile = session.getGameMap().getTile(x, y);
        if (tile == null || tile.getPlant() == null) {
            throw new com.pvz2.exception.GameException(
                    "No plant at (" + x + ", " + y + ").");
        }
        if (tile.getSecondLayerPlant() != null) {
            tile.setPlant(tile.getSecondLayerPlant());
            tile.setSecondLayerPlant(null);
        } else {
            tile.setPlant(null);
        }
    }

    public void feedPlant(GameSession session, int x, int y) {
        if (session.getPlantFoodCount() <= 0) {
            throw new com.pvz2.exception.GameException(
                    "No plant food available.");
        }
        Tile tile = session.getGameMap().getTile(x, y);
        if (tile == null || tile.getPlant() == null) {
            throw new com.pvz2.exception.GameException(
                    "No plant at (" + x + ", " + y + ").");
        }
        session.usePlantFood();
        tile.getPlant().activatePlantFood(session);
        view.printSuccess("Plant food used on "
                + tile.getPlant().getType().name() + "!");
    }

    public void releaseNuke(GameSession session) {
        List<Zombie> toKill = new ArrayList<>(session.getActiveZombies());
        for (Zombie z : toKill) {
            z.setCurrentHealth(0);
        }
        view.printSuccess("The nuke released! All zombies obliterated!");
    }

    public void removeCooldown(GameSession session) {
        session.setCooldownCheated(true);
        for (int r = 1; r <= session.getGameMap().getRows(); r++) {
            for (int c = 1; c <= session.getGameMap().getCols(); c++) {
                Tile tile = session.getGameMap().getTile(c, r);
                if (tile != null && tile.getPlant() != null) {
                    tile.getPlant().resetCooldown();
                }
            }
        }
        view.printSuccess("All cooldowns removed!");
    }

    public void addPlantFoodCheat(GameSession session) {
        boolean added = session.addPlantFood();
        if (added) {
            view.printSuccess("Plant food added. You have "
                    + session.getPlantFoodCount() + " now.");
        } else {
            view.printError("Plant food is at maximum (3).");
        }
    }

    public void spawnZombieCheat(GameSession session, String zombieType,
                                 int x, int y) {
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
}
