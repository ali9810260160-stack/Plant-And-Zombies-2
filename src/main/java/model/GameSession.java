package model;

import model.enums.GameResult;
import model.enums.PlantType;
import model.zombies.Zombie;

import java.util.List;

/**
 * وضعیت جاری یک مرحله در حال اجرا.
 * تمام state زنده بازی اینجاست: تیک، خورشید، موج، پرتابه‌ها.
 */
public class GameSession {

    /** مرحله‌ای که در حال اجرا است */
    private Level level;

    /** نقشه بازی */
    private GameMap gameMap;

    /** تیک کنونی از شروع بازی */
    private int currentTick;

    /** میزان خورشید فعلی بازیکن */
    private int sunAmount;

    /** تعداد غذای گیاه موجود (حداکثر 3) */
    private int plantFoodCount;

    /** شماره موج جاری */
    private int currentWaveIndex;

    /** لیست تمام امواج این مرحله */
    private List<Wave> waves;

    /** لیست تمام زامبی‌های زنده */
    private List<Zombie> activeZombies;

    /** لیست پرتابه‌های در حال حرکت */
    private List<Projectile> activeProjectiles;

    /** لیست خورشیدهای روی صفحه */
    private List<Sun> activeSuns;

    /** گیاهانی که بازیکن برای این مرحله انتخاب کرده */
    private List<PlantType> selectedPlants;

    /** گیاهانی که boost شده‌اند (برای این مرحله) */
    private List<PlantType> boostedPlants;

    /** نتیجه بازی */
    private GameResult result;

    /** آیا cooldown چیت فعال است */
    private boolean cooldownCheated;

    /** تعداد گیاهان از دست رفته (برای Love Your Plants) */
    private int plantsLost;

    /** تعداد زامبی‌های کشته‌شده (برای Timed War) */
    private int zombiesKilled;

    /** میوپوینت جمع‌شده در این بازی */
    private long meoPoints;

    /** آیا بازیکن "start zombie waves" را فراخوانی کرده (برای Plant What You Get) */
    private boolean waveStarted;

    public GameSession(Level level, GameMap gameMap, List<PlantType> selectedPlants) {
        this.level = level;
        this.gameMap = gameMap;
        this.selectedPlants = selectedPlants;
        this.currentTick = 0;
        this.result = GameResult.IN_PROGRESS;
        this.cooldownCheated = false;
    }

    /** یک تیک بازی را پیش می‌برد */
    public void advanceTick() { }

    /** بررسی می‌کند آیا شرط برد/باخت برقرار است */
    public void checkWinLoseCondition() { }

    /** تعداد ثانیه‌های گذشته را برمی‌گرداند */
    public double getElapsedSeconds() { return currentTick / 10.0; }

    public Level getLevel() { return level; }
    public GameMap getGameMap() { return gameMap; }
    public int getCurrentTick() { return currentTick; }
    public void setCurrentTick(int tick) { this.currentTick = tick; }
    public int getSunAmount() { return sunAmount; }
    public void setSunAmount(int sunAmount) { this.sunAmount = sunAmount; }
    public int getPlantFoodCount() { return plantFoodCount; }
    public void setPlantFoodCount(int count) { this.plantFoodCount = count; }
    public int getCurrentWaveIndex() { return currentWaveIndex; }
    public void setCurrentWaveIndex(int idx) { this.currentWaveIndex = idx; }
    public List<Wave> getWaves() { return waves; }
    public void setWaves(List<Wave> waves) { this.waves = waves; }
    public List<Zombie> getActiveZombies() { return activeZombies; }
    public List<Projectile> getActiveProjectiles() { return activeProjectiles; }
    public List<Sun> getActiveSuns() { return activeSuns; }
    public List<PlantType> getSelectedPlants() { return selectedPlants; }
    public List<PlantType> getBoostedPlants() { return boostedPlants; }
    public void setBoostedPlants(List<PlantType> boostedPlants) { this.boostedPlants = boostedPlants; }
    public GameResult getResult() { return result; }
    public void setResult(GameResult result) { this.result = result; }
    public boolean isCooldownCheated() { return cooldownCheated; }
    public void setCooldownCheated(boolean cooldownCheated) { this.cooldownCheated = cooldownCheated; }
    public int getPlantsLost() { return plantsLost; }
    public void setPlantsLost(int plantsLost) { this.plantsLost = plantsLost; }
    public int getZombiesKilled() { return zombiesKilled; }
    public void setZombiesKilled(int zombiesKilled) { this.zombiesKilled = zombiesKilled; }
    public long getMeoPoints() { return meoPoints; }
    public void setMeoPoints(long meoPoints) { this.meoPoints = meoPoints; }
    public boolean isWaveStarted() { return waveStarted; }
    public void setWaveStarted(boolean waveStarted) { this.waveStarted = waveStarted; }
}
