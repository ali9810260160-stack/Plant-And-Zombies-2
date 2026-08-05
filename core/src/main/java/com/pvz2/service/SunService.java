package com.pvz2.service;

import com.pvz2.model.GameSession;
import com.pvz2.model.Sun;
import com.pvz2.model.enums.ChapterType;
import com.pvz2.model.enums.LevelType;
import com.pvz2.model.enums.SunType;
import com.pvz2.model.tiles.Tile;
import com.pvz2.util.RandomUtil;
import com.pvz2.view.ConsoleView;

/**
 * سرویس مدیریت خورشید — تولید، سقوط، برداشت.
 *
 * مراحلی که خورشید از آسمان نمی‌بارد:
 *  - DARK_AGES  (عصر تاریکی)
 *  - NIGHT_OPS  (شب عملیات)
 *  - PLANT_WHAT_YOU_GET (هر چه رسد بکار)
 */
public class SunService {

    private static final int SKY_DROP_DURATION_TICKS = 50;
    private com.pvz2.service.CombatService combatService;
    private ConsoleView view;

    public SunService(ConsoleView view) {
        this.view = view;
    }

    public void setCombatService(com.pvz2.service.CombatService cs) {
        this.combatService = cs;
    }

    /** فاصله سقوط بعدی: max(6+0.05t, 12) ثانیه — طبق داکیومنت */
    public int calculateDropIntervalTicks(double elapsedSeconds) {
        double interval = Math.min(6 + 0.05 * elapsedSeconds, 12);
        return (int) (interval * 10);
    }

    /**
     * هر تیک — مدیریت سقوط خورشید از آسمان.
     * بلوک‌های خورشید آسمان:
     *  1. DARK_AGES
     *  2. NIGHT_OPS  ← اضافه شد
     *  3. PLANT_WHAT_YOU_GET ← اضافه شد
     */
    public void tickSkyDrops(GameSession session) {
        if (isSkyDropBlocked(session)) {
            // در این مراحل خورشید از آسمان نمی‌بارد
            tickFallingSuns(session); // سان‌های در حال سقوط رو ادامه بده
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

    /**
     * آیا سقوط خورشید از آسمان در این session بلوک است؟
     */
    private boolean isSkyDropBlocked(GameSession session) {
        if (session.getLevel() == null) return false;
        // فصل عصر تاریکی
        if (session.getGameMap().getChapter() == ChapterType.DARK_AGES) {
            return true;
        }
        LevelType lt = session.getLevel().getLevelType();
        // شب عملیات — هیچ آفتابی از آسمان نمی‌بارد
        if (lt == LevelType.NIGHT_OPS) {
            return true;
        }
        // هر چه رسد بکار — فقط خورشید اولیه، بعداً هم نمی‌بارد
        if (lt == LevelType.PLANT_WHAT_YOU_GET) {
            return true;
        }
        return false;
    }

    private void dropSunFromSky(GameSession session, int tick) {
        SunType type = pickSunType();
        int x = RandomUtil.between(1, session.getGameMap().getCols());
        int y = RandomUtil.between(1, session.getGameMap().getRows());
        Sun sun = new Sun(type, x, y, tick);
        session.getActiveSuns().add(sun);
        view.printRaw("\u001B[33m☀ New " + typeName(type)
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
        if (roll < 0.80) return SunType.NORMAL;
        if (roll < 0.95) return SunType.SPECIAL;
        return SunType.RADIOACTIVE;
    }

    private void tickFallingSuns(GameSession session) {
        int currentTick = session.getCurrentTick();
        for (Sun sun : session.getActiveSuns()) {
            if (sun.isLanded() || sun.isCollected()) continue;
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
        view.printRaw("\u001B[33m☀ Sun reached the ground at position ("
                + sun.getX() + ", " + sun.getY() + ")\u001B[0m");
    }

    public int collectSun(GameSession session, int x, int y) {
        Sun target = findCollectableSun(session, x, y);
        if (target == null) return -1;
        if (target.getType() == SunType.RADIOACTIVE && !target.isLanded()) {
            handleRadioactiveExplosion(target, session);
            target.setCollected(true);
            return 0;
        }
        target.setCollected(true);
        int value = getSunValue(target);
        session.addSun(value);
        // ثبت برای Timed War (حالت خورشید)
        if (session.getLevel() != null
                && session.getLevel().getLevelType() == LevelType.TIMED_WAR
                && session.getLevel().isTimedWarSunMode()) {
            session.registerTimedWarSun(value);
        }
        if (combatService != null) {
            combatService.onSunCollectedForQuest(value);
        }
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
        view.printRaw("\u001B[31m☢ Radioactive sun exploded mid-air!\u001B[0m");
        int cx = sun.getX();
        int cy = sun.getY();
        for (com.pvz2.model.zombies.Zombie z : session.getActiveZombies()) {
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
            throw new com.pvz2.exception.GameException("No plant at (" + x + ", " + y + ")");
        }
        com.pvz2.model.plants.Plant plant = tile.getPlant();
        if (!(plant instanceof com.pvz2.model.plants.GenericPlant)) {
            throw new com.pvz2.exception.GameException("Plant cannot produce sun.");
        }
        com.pvz2.model.plants.GenericPlant gp = (com.pvz2.model.plants.GenericPlant) plant;
        if (!gp.isSunPending()) {
            throw new com.pvz2.exception.GameException(
                    "No sun ready on plant at (" + x + ", " + y + ")");
        }
        int amount = gp.getSunProductionAmount();
        gp.collectSun();
        session.addSun(amount);
        // ثبت برای Timed War (حالت خورشید)
        if (session.getLevel() != null
                && session.getLevel().getLevelType() == LevelType.TIMED_WAR
                && session.getLevel().isTimedWarSunMode()) {
            session.registerTimedWarSun(amount);
        }
        view.printRaw("\u001B[33m☀ plant " + plant.getType().name()
                + " produced a sun at (" + x + ", " + y + ")\u001B[0m");
    }

    public void cleanupCollectedSuns(GameSession session) {
        session.getActiveSuns().removeIf(Sun::isCollected);
    }
}
