package model;

import model.enums.GameResult;
import model.enums.PlantType;
import model.zombies.Zombie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * وضعیت جاری یک مرحله در حال اجرا.
 */
public class GameSession {

    private Level level;
    private GameMap gameMap;
    private int currentTick;
    private int sunAmount;
    private int plantFoodCount;
    private int currentWaveIndex;
    private List<Wave> waves;
    private List<Zombie> activeZombies;
    private List<Projectile> activeProjectiles;
    private List<Sun> activeSuns;
    private List<PlantType> selectedPlants;
    private List<PlantType> boostedPlants;
    private GameResult result;
    private boolean cooldownCheated;
    private int plantsLost;
    private int zombiesKilled;
    private long meoPoints;
    private boolean waveStarted;

    /** تیک آخرین سقوط خورشید از آسمان */
    private int lastSkyDropTick;
    /** تیک شروع نبرد زمان‌دار */
    private int timedWarStartTick;
    /** حالت انتظار بین موج‌ها */
    private boolean betweenWaves;
    /** آیا گردباد مصر باید اتفاق بیفتد */
    private boolean egyptTornadoActive;
    /** ردیف‌هایی که باد یخی زده */
    private List<Integer> frostbiteWindAffectedRows;
    /** گیاهانی که تبدیل به گربه شدند (برای Wizard) */
    private Map<String, model.plants.Plant> catPlants;
    /** مینی‌گیم state */
    private Object minigameState;
    /** امتیاز multi-kill برای scoreservice */
    private int consecutiveKills;
    private long lastKillTick;

    public GameSession(Level level, GameMap gameMap, List<PlantType> selectedPlants) {
        this.level = level;
        this.gameMap = gameMap;
        this.selectedPlants = new ArrayList<>(selectedPlants);
        this.currentTick = 0;
        this.result = GameResult.IN_PROGRESS;
        this.cooldownCheated = false;
        this.sunAmount = level.getInitialSunAmount() > 0
                ? level.getInitialSunAmount() : 50;
        this.plantFoodCount = 0;
        this.currentWaveIndex = 0;
        this.activeZombies = new ArrayList<>();
        this.activeProjectiles = new ArrayList<>();
        this.activeSuns = new ArrayList<>();
        this.waves = new ArrayList<>();
        this.boostedPlants = new ArrayList<>();
        this.plantsLost = 0;
        this.zombiesKilled = 0;
        this.meoPoints = 0;
        this.waveStarted = false;
        this.lastSkyDropTick = 0;
        this.betweenWaves = false;
        this.frostbiteWindAffectedRows = new ArrayList<>();
        this.catPlants = new HashMap<>();
        this.consecutiveKills = 0;
        this.lastKillTick = 0;
    }

    public void advanceTick() {
        currentTick++;
    }

    public double getElapsedSeconds() {
        return currentTick / 10.0;
    }

    public boolean isInProgress() {
        return result == GameResult.IN_PROGRESS;
    }

    public void addSun(int amount) {
        sunAmount += amount;
    }

    public boolean spendSun(int amount) {
        if (sunAmount < amount) {
            return false;
        }
        sunAmount -= amount;
        return true;
    }

    public boolean addPlantFood() {
        if (plantFoodCount >= 3) {
            return false;
        }
        plantFoodCount++;
        return true;
    }

    public boolean usePlantFood() {
        if (plantFoodCount <= 0) {
            return false;
        }
        plantFoodCount--;
        return true;
    }

    public Wave getCurrentWave() {
        if (waves == null || currentWaveIndex >= waves.size()) {
            return null;
        }
        return waves.get(currentWaveIndex);
    }

    public boolean hasMoreWaves() {
        return currentWaveIndex < waves.size() - 1;
    }

    public boolean allWavesFinished() {
        if (waves == null || waves.isEmpty()) {
            return false;
        }
        if (currentWaveIndex < waves.size() - 1) {
            return false;
        }
        return activeZombies.isEmpty();
    }

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
    public void setBoostedPlants(List<PlantType> b) { this.boostedPlants = b; }
    public GameResult getResult() { return result; }
    public void setResult(GameResult result) { this.result = result; }
    public boolean isCooldownCheated() { return cooldownCheated; }
    public void setCooldownCheated(boolean b) { this.cooldownCheated = b; }
    public int getPlantsLost() { return plantsLost; }
    public void setPlantsLost(int plantsLost) { this.plantsLost = plantsLost; }
    public int getZombiesKilled() { return zombiesKilled; }
    public void setZombiesKilled(int zombiesKilled) { this.zombiesKilled = zombiesKilled; }
    public long getMeoPoints() { return meoPoints; }
    public void setMeoPoints(long meoPoints) { this.meoPoints = meoPoints; }
    public boolean isWaveStarted() { return waveStarted; }
    public void setWaveStarted(boolean waveStarted) { this.waveStarted = waveStarted; }
    public int getLastSkyDropTick() { return lastSkyDropTick; }
    public void setLastSkyDropTick(int t) { this.lastSkyDropTick = t; }
    public boolean isBetweenWaves() { return betweenWaves; }
    public void setBetweenWaves(boolean b) { this.betweenWaves = b; }
    public boolean isEgyptTornadoActive() { return egyptTornadoActive; }
    public void setEgyptTornadoActive(boolean b) { this.egyptTornadoActive = b; }
    public List<Integer> getFrostbiteWindAffectedRows() { return frostbiteWindAffectedRows; }
    public Map<String, model.plants.Plant> getCatPlants() { return catPlants; }
    public Object getMinigameState() { return minigameState; }
    public void setMinigameState(Object s) { this.minigameState = s; }
    public int getConsecutiveKills() { return consecutiveKills; }
    public void setConsecutiveKills(int n) { this.consecutiveKills = n; }
    public long getLastKillTick() { return lastKillTick; }
    public void setLastKillTick(long t) { this.lastKillTick = t; }
}
