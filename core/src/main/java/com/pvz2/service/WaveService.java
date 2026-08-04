package com.pvz2.service;

import com.pvz2.model.GameSession;
import com.pvz2.model.Level;
import com.pvz2.model.Wave;
import com.pvz2.model.enums.ChapterType;
import com.pvz2.model.enums.ZombieType;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.model.zombies.ZombieFactory;
import com.pvz2.util.RandomUtil;
import com.pvz2.view.ConsoleView;

import java.util.ArrayList;
import java.util.List;

/**
 * سرویس تولید امواج زامبی با فرمول سختی.
 */
public class WaveService {

    private final ConsoleView view;

    public WaveService(ConsoleView view) {
        this.view = view;
    }


    public List<Wave> generateWaves(Level level) {
        List<Wave> waves = new ArrayList<>();
        int count = level.getWaveCount();
        int base = level.getInitialWaveDifficulty();
        for (int i = 1; i <= count; i++) {
            boolean isFinal = (i == count);
            int difficulty = calculateDifficulty(base, i, isFinal);
            waves.add(new Wave(i, difficulty, isFinal));
        }
        return waves;
    }

    private int calculateDifficulty(int base, int waveNumber, boolean isFinal) {
        double multiplier = Math.pow(1.25, waveNumber - 1);
        int difficulty = (int) (base * multiplier);
        if (isFinal) {
            difficulty *= 2;
        }
        return difficulty;
    }

    public void spawnWave(Wave wave, GameSession session) {
        ChapterType chapter = session.getGameMap().getChapter();
        ZombieType[] allowed = ZombieFactory.getAllowedZombiesForChapter(chapter);
        int remaining = wave.getWaveDifficulty();
        List<Zombie> spawned = new ArrayList<>();

        while (remaining > 0) {
            ZombieType picked = pickAffordableZombie(allowed, remaining);
            if (picked == null) {
                break;
            }
            Zombie zombie = ZombieFactory.create(picked);
            int cost = ZombieFactory.getWaveCost(picked);
            int lane = RandomUtil.between(1, session.getGameMap().getRows());
            zombie.setX(session.getGameMap().getCols() + 1.0);
            zombie.setY(lane);
            zombie.setLane(lane);
            zombie.setSpawnWave(wave.getWaveNumber());

            applyEgyptTornado(zombie, session, wave);
            applyGlowingChance(zombie);

            wave.registerZombieAdded(zombie);
            session.getActiveZombies().add(zombie);
            spawned.add(zombie);
            remaining -= cost;

            printSpawnMessage(zombie, wave, lane, cost);
        }
        wave.setStarted(true);
    }

    private ZombieType pickAffordableZombie(ZombieType[] allowed, int budget) {
        List<ZombieType> affordable = new ArrayList<>();
        for (ZombieType t : allowed) {
            if (ZombieFactory.getWaveCost(t) <= budget) {
                affordable.add(t);
            }
        }
        if (affordable.isEmpty()) {
            return allowed[RandomUtil.nextInt(allowed.length)];
        }
        return affordable.get(RandomUtil.nextInt(affordable.size()));
    }

    private void applyEgyptTornado(Zombie zombie, GameSession session, Wave wave) {
        if (session.getGameMap().getChapter() != ChapterType.ANCIENT_EGYPT) {
            return;
        }
        if (!wave.isFinalWave()) {
            return;
        }
        int advance = RandomUtil.between(1, 4);
        zombie.setX(Math.max(1.0, zombie.getX() - advance));
    }

    private void applyGlowingChance(Zombie zombie) {
        if (RandomUtil.chance(0.05)) {
            zombie.setGlowing(true);
        }
    }

    private void printSpawnMessage(Zombie z, Wave wave, int lane, int cost) {
        view.printRaw("\u001B[31mZombie " + z.getType().name()
            + " spawned at wave " + wave.getWaveNumber()
            + " in lane " + lane
            + " which costed " + cost + ".\u001B[0m");
    }

    public boolean shouldAdvanceWave(Wave currentWave, Wave nextWave) {
        if (currentWave == null || !currentWave.isStarted()) {
            return false;
        }
        return currentWave.shouldTriggerNextWave();
    }

    public void applyFrostbiteWind(GameSession session, int waveNumber) {
        if (session.getGameMap().getChapter() != ChapterType.FROSTBITE_CAVES) {
            return;
        }
        int rows = session.getGameMap().getRows();
        int affectedCount = RandomUtil.between(1, Math.min(3, rows));
        List<Integer> affected = session.getFrostbiteWindAffectedRows();
        affected.clear();
        for (int i = 0; i < affectedCount; i++) {
            int row = RandomUtil.between(1, rows);
            if (!affected.contains(row)) {
                affected.add(row);
            }
        }
        applyWindToPlants(session, affected);
    }

    private void applyWindToPlants(GameSession session, List<Integer> rows) {
        for (int row : rows) {
            for (int col = 1; col <= session.getGameMap().getCols(); col++) {
                com.pvz2.model.tiles.Tile tile = session.getGameMap().getTile(col, row);
                if (tile == null || tile.getPlant() == null) {
                    continue;
                }
                com.pvz2.model.plants.Plant plant = tile.getPlant();
                if (!plant.isFirePlant()) {
                    plant.incrementFreezeLevel();
                    view.printRaw("\u001B[36mIce wind hit plant "
                        + plant.getType().name()
                        + " at (" + col + "," + row + "). Freeze level: "
                        + plant.getFreezeLevel() + "\u001B[0m");
                }
            }
        }
    }

    public void spawnNecromancyZombie(GameSession session) {
        if (session.getGameMap().getChapter() != ChapterType.DARK_AGES) {
            return;
        }
        for (int row = 1; row <= session.getGameMap().getRows(); row++) {
            for (int col = 1; col <= session.getGameMap().getCols(); col++) {
                com.pvz2.model.tiles.Tile tile = session.getGameMap().getTile(col, row);
                if (tile == null) {
                    continue;
                }
                if (tile.getType() == com.pvz2.model.enums.TileType.NECROMANCY
                        && tile.isTombstone()) {
                    spawnFromNecromancy(session, col, row);
                }
            }
        }
    }

    private void spawnFromNecromancy(GameSession session, int col, int row) {
        Zombie zombie = ZombieFactory.create(ZombieType.NORMAL);
        zombie.setX(col);
        zombie.setY(row);
        zombie.setLane(row);
        session.getActiveZombies().add(zombie);
        view.printRaw("\u001B[35mA zombie emerged from necromancy at ("
            + col + "," + row + ")!\u001B[0m");
    }
}
