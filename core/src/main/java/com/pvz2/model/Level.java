package com.pvz2.model;

import com.pvz2.model.enums.ChapterType;
import com.pvz2.model.enums.LevelType;
import com.pvz2.model.enums.PlantType;

import java.util.List;

/**
 * مشخصات یک مرحله بازی (داده ثابت).
 * وضعیت جاری بازی در GameSession نگه داشته می‌شود.
 */
public class Level {

    /** شماره مرحله */
    private int levelNumber;

    /** فصل متعلق به این مرحله */
    private ChapterType chapter;

    /** نوع مرحله (عادی، ویژه، مینی‌گیم) */
    private LevelType levelType;

    /** سختی اولین موج */
    private int initialWaveDifficulty;

    /** تعداد امواج */
    private int waveCount;

    /** تعداد ردیف‌های نقشه (پیش‌فرض 5) */
    private int mapRows;

    /** تعداد ستون‌های نقشه (پیش‌فرض 9) */
    private int mapCols;

    /** تعداد اسلات انتخاب گیاه (پیش‌فرض 8) */
    private int plantSlots;

    /** گیاهانی که در این مرحله مجاز نیستند */
    private List<PlantType> lockedPlants;

    /** گیاهانی که از ابتدا قفل هستند (Locked Plants Level) */
    private List<PlantType> forcedLockedSlots;

    /** آیا این مرحله آنلاک شده */
    private boolean unlocked;

    /** آیا بازیکن این مرحله را کامل کرده */
    private boolean completed;

    // ---- پارامترهای مراحل ویژه ----

    /** تعداد زامبی برای Timed War */
    private int timedWarZombieTarget;

    /** زمان Timed War (ثانیه) */
    private int timedWarSeconds;

    /** خورشید اولیه برای Plant What You Get */
    private int initialSunAmount;

    /** خط ددلاین (ستون) برای Dead Line */
    private int deadLineColumn;

    /** تعداد مجاز گیاه از دست رفته برای Love Your Plants */
    private int maxPlantsLost;

    /** خورشید اولیه برای Night Ops */
    private int nightOpsSun;

    public Level(int levelNumber, ChapterType chapter, LevelType levelType) {
        this.levelNumber = levelNumber;
        this.chapter = chapter;
        this.levelType = levelType;
        this.mapRows = 5;
        this.mapCols = 9;
        this.plantSlots = 8;
        this.unlocked = false;
        this.completed = false;
    }

    public int getLevelNumber() { return levelNumber; }
    public ChapterType getChapter() { return chapter; }
    public LevelType getLevelType() { return levelType; }
    public int getInitialWaveDifficulty() { return initialWaveDifficulty; }
    public void setInitialWaveDifficulty(int d) { this.initialWaveDifficulty = d; }
    public int getWaveCount() { return waveCount; }
    public void setWaveCount(int waveCount) { this.waveCount = waveCount; }
    public int getMapRows() { return mapRows; }
    public int getMapCols() { return mapCols; }
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
    public int getTimedWarZombieTarget() { return timedWarZombieTarget; }
    public void setTimedWarZombieTarget(int t) { this.timedWarZombieTarget = t; }
    public int getTimedWarSeconds() { return timedWarSeconds; }
    public void setTimedWarSeconds(int t) { this.timedWarSeconds = t; }
    public int getInitialSunAmount() { return initialSunAmount; }
    public void setInitialSunAmount(int s) { this.initialSunAmount = s; }
    public int getDeadLineColumn() { return deadLineColumn; }
    public void setDeadLineColumn(int d) { this.deadLineColumn = d; }
    public int getNightOpsSun() { return nightOpsSun; }
    public void setNightOpsSun(int n) { this.nightOpsSun = n; }
    public int getMaxPlantsLost() { return maxPlantsLost; }
    public void setMaxPlantsLost(int m) { this.maxPlantsLost = m; }
    public void setMapRows(int mapRows) { this.mapRows = mapRows; }
    public void setMapCols(int mapCols) { this.mapCols = mapCols; }
}
