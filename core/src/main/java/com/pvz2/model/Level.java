package com.pvz2.model;

import com.pvz2.model.enums.ChapterType;
import com.pvz2.model.enums.LevelType;
import com.pvz2.model.enums.PlantType;

import java.util.List;

/**
 * مشخصات یک مرحله بازی (داده ثابت).
 * شامل پارامترهای کامل هر 8 نوع مرحله ویژه.
 */
public class Level {

    private int levelNumber;
    private ChapterType chapter;
    private LevelType levelType;
    private int initialWaveDifficulty;
    private int waveCount;
    private int mapRows;
    private int mapCols;
    private int plantSlots;
    private List<PlantType> lockedPlants;
    private List<PlantType> forcedLockedSlots;
    private boolean unlocked;
    private boolean completed;

    // ── پارامترهای مراحل ویژه ──────────────────────────────

    /** خورشید اولیه برای Night Ops و Plant What You Get */
    private int initialSunAmount;

    /** آستانه از دست دادن گیاه برای Love Your Plants */
    private int maxPlantsLost;

    /** ستون خط ددلاین برای Dead Line */
    private int deadLineColumn;

    /** خورشید اولیه اختصاصی Night Ops */
    private int nightOpsSun;

    /** تعداد زامبی هدف برای Timed War */
    private int timedWarZombieTarget;

    /** زمان کل Timed War (ثانیه) */
    private int timedWarSeconds;

    /** آیا هدف Timed War خورشید است (در غیر این صورت زامبی) */
    private boolean timedWarSunMode;

    /** مقدار خورشید هدف در حالت خورشید Timed War */
    private int timedWarSunTarget;

    /**
     * موقعیت‌های از پیش تعیین‌شده گیاهان برای Save Our Seeds / Love Your Plants.
     * هر int[3] = {x, y, plantTypeOrdinal}
     */
    private int[][] preplacedPlants;

    public Level(int levelNumber, ChapterType chapter, LevelType levelType) {
        this.levelNumber = levelNumber;
        this.chapter = chapter;
        this.levelType = levelType;
        this.mapRows = 5;
        this.mapCols = 9;
        this.plantSlots = 8;
        this.unlocked = false;
        this.completed = false;
        this.deadLineColumn = 5;
        this.maxPlantsLost = 3;
    }

    // ── getters / setters ──────────────────────────────────

    public int getLevelNumber() { return levelNumber; }
    public ChapterType getChapter() { return chapter; }
    public LevelType getLevelType() { return levelType; }
    public int getInitialWaveDifficulty() { return initialWaveDifficulty; }
    public void setInitialWaveDifficulty(int d) { this.initialWaveDifficulty = d; }
    public int getWaveCount() { return waveCount; }
    public void setWaveCount(int waveCount) { this.waveCount = waveCount; }
    public int getMapRows() { return mapRows; }
    public void setMapRows(int mapRows) { this.mapRows = mapRows; }
    public int getMapCols() { return mapCols; }
    public void setMapCols(int mapCols) { this.mapCols = mapCols; }
    public int getPlantSlots() { return plantSlots; }
    public void setPlantSlots(int plantSlots) { this.plantSlots = plantSlots; }
    public List<PlantType> getLockedPlants() { return lockedPlants; }
    public void setLockedPlants(List<PlantType> lockedPlants) { this.lockedPlants = lockedPlants; }
    public List<PlantType> getForcedLockedSlots() { return forcedLockedSlots; }
    public void setForcedLockedSlots(List<PlantType> f) { this.forcedLockedSlots = f; }
    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public int getInitialSunAmount() { return initialSunAmount; }
    public void setInitialSunAmount(int s) { this.initialSunAmount = s; }
    public int getMaxPlantsLost() { return maxPlantsLost; }
    public void setMaxPlantsLost(int m) { this.maxPlantsLost = m; }
    public int getDeadLineColumn() { return deadLineColumn; }
    public void setDeadLineColumn(int d) { this.deadLineColumn = d; }
    public int getNightOpsSun() { return nightOpsSun; }
    public void setNightOpsSun(int n) { this.nightOpsSun = n; }
    public int getTimedWarZombieTarget() { return timedWarZombieTarget; }
    public void setTimedWarZombieTarget(int t) { this.timedWarZombieTarget = t; }
    public int getTimedWarSeconds() { return timedWarSeconds; }
    public void setTimedWarSeconds(int t) { this.timedWarSeconds = t; }
    public boolean isTimedWarSunMode() { return timedWarSunMode; }
    public void setTimedWarSunMode(boolean b) { this.timedWarSunMode = b; }
    public int getTimedWarSunTarget() { return timedWarSunTarget; }
    public void setTimedWarSunTarget(int t) { this.timedWarSunTarget = t; }
    public int[][] getPreplacedPlants() { return preplacedPlants; }
    public void setPreplacedPlants(int[][] p) { this.preplacedPlants = p; }
}
