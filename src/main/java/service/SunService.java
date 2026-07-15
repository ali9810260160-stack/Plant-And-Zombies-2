package service;

import model.GameSession;
import model.Sun;
import model.enums.ChapterType;
import model.enums.SunType;
import model.tiles.Tile;
import util.RandomUtil;

import java.util.Iterator;
import java.util.List;

/**
 * سرویس مدیریت خورشید — تولید، سقوط، برداشت.
 */
public class SunService {

    private static final int SKY_DROP_DURATION_TICKS = 50;

    /** محاسبه فاصله سقوط بعدی: max(6 + 0.05t, 12) ثانیه */
    public int calculateDropIntervalTicks(double elapsedSeconds) {
        double interval = Math.min(6 + 0.05 * elapsedSeconds, 12);
        return (int) (interval * 10);
    }

    public void tickSkyDrops(GameSession session) {
        if (session.getGameMap().getChapter() == ChapterType.DARK_AGES) {
            return;
        }
        int currentTick = session.getCurrentTick();
        int lastDrop = session.getLastSkyDropTick();
        int interval = calculateDropIntervalTicks(session.getElapsedSeconds());
        if (currentTick - lastDrop >= interval) {
            dropSunFromSky(session, currentTick);
            session.setLastSkyDropTick(currentTick);
        }
        tickFallingSuns(session);
    }

    private void dropSunFromSky(GameSession session, int tick) {
        SunType type = pickSunType();
        int x = RandomUtil.between(1, session.getGameMap().getCols());
        int y = RandomUtil.between(1, session.getGameMap().getRows());
        Sun sun = new Sun(type, x, y, tick);
        session.getActiveSuns().add(sun);
        System.out.println("\u001B[33m☀ New " + typeName(type)
                + " sun is dropping at position (" + x + ", " + y + ")\u001B[0m");
    }

    private String typeName(SunType type) {
        switch (type) {
            case NORMAL:      return "normal";
            case SPECIAL:     return "special";
            case RADIOACTIVE: return "radioactive";
            default:          return "normal";
        }
    }

    private SunType pickSunType() {
        double roll = RandomUtil.nextDouble();
        if (roll < 0.80) {
            return SunType.NORMAL;
        }
        if (roll < 0.95) {
            return SunType.SPECIAL;
        }
        return SunType.RADIOACTIVE;
    }

    private void tickFallingSuns(GameSession session) {
        int currentTick = session.getCurrentTick();
        for (Sun sun : session.getActiveSuns()) {
            if (sun.isLanded() || sun.isCollected()) {
                continue;
            }
            int ticksElapsed = currentTick - sun.getSpawnTick();
            double progress = (double) ticksElapsed / SKY_DROP_DURATION_TICKS;
            sun.setFallProgress(Math.min(1.0, progress));
            if (progress >= 1.0 && !sun.isLanded()) {
                sun.setLanded(true);
                onSunLanded(sun, session);
            }
        }
    }

    private void onSunLanded(Sun sun, GameSession session) {
        if (sun.getType() == SunType.RADIOACTIVE) {
            sun.setValue(sun.getValue() == 150 ? 25 : sun.getValue());
        }
        System.out.println("\u001B[33m☀ Sun reached the ground at position ("
                + sun.getX() + ", " + sun.getY() + ")\u001B[0m");
    }

    public int collectSun(GameSession session, int x, int y) {
        Sun target = findCollectableSun(session, x, y);
        if (target == null) {
            return -1;
        }
        if (target.getType() == SunType.RADIOACTIVE && !target.isLanded()) {
            handleRadioactiveExplosion(target, session);
            target.setCollected(true);
            return 0;
        }
        target.setCollected(true);
        int value = getSunValue(target);
        session.addSun(value);
        return value;
    }

    private Sun findCollectableSun(GameSession session, int x, int y) {
        for (Sun sun : session.getActiveSuns()) {
            if (!sun.isCollected() && sun.getX() == x && sun.getY() == y) {
                return sun;
            }
        }
        return null;
    }

    private void handleRadioactiveExplosion(Sun sun, GameSession session) {
        System.out.println("\u001B[31m☢ Radioactive sun exploded mid-air!\u001B[0m");
        int cx = sun.getX();
        int cy = sun.getY();
        for (model.zombies.Zombie z : session.getActiveZombies()) {
            if (Math.abs((int) z.getX() - cx) <= 2
                    && Math.abs(z.getY() - cy) <= 2) {
                z.takeDamage(150);
            }
        }
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                Tile tile = session.getGameMap().getTile(cx + dx, cy + dy);
                if (tile != null && tile.getPlant() != null) {
                    tile.getPlant().takeDamage(80);
                }
            }
        }
    }

    private int getSunValue(Sun sun) {
        switch (sun.getType()) {
            case NORMAL:      return 25;
            case SPECIAL:     return 100;
            case RADIOACTIVE: return 150;
            default:          return 25;
        }
    }

    public void collectPlantProducedSun(GameSession session, int x, int y) {
        Tile tile = session.getGameMap().getTile(x, y);
        if (tile == null || tile.getPlant() == null) {
            throw new exception.GameException(
                    "No plant at (" + x + ", " + y + ")");
        }
        model.plants.Plant plant = tile.getPlant();
        if (!(plant instanceof model.plants.GenericPlant)) {
            throw new exception.GameException("Plant cannot produce sun.");
        }
        model.plants.GenericPlant gp = (model.plants.GenericPlant) plant;
        if (!gp.isSunPending()) {
            throw new exception.GameException(
                    "No sun ready on plant at (" + x + ", " + y + ")");
        }
        int amount = gp.getSunProductionAmount();
        gp.collectSun();
        session.addSun(amount);
        System.out.println("\u001B[33m☀ plant " + plant.getType().name()
                + " produced a sun at (" + x + ", " + y + ")\u001B[0m");
    }

    public void cleanupCollectedSuns(GameSession session) {
        session.getActiveSuns().removeIf(Sun::isCollected);
    }
}
