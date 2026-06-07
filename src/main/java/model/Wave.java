package model;

import model.zombies.Zombie;

import java.util.List;

/**
 * یک موج حمله زامبی در یک مرحله.
 * هر مرحله چندین موج دارد و موج آخر (flag wave) دو برابر سختی دارد.
 */
public class Wave {

    /** شماره موج (از 1) */
    private int waveNumber;

    /** آیا این موج آخر (flag wave) است */
    private boolean finalWave;

    /** سختی این موج (waveCost مجموع زامبی‌ها) */
    private int waveDifficulty;

    /** لیست زامبی‌های این موج */
    private List<Zombie> zombies;

    /** آیا این موج شروع شده */
    private boolean started;

    /** درصد HP از دست رفته زامبی‌های این موج (برای trigger موج بعدی - 75%) */
    private double healthLostPercent;

    /** مجموع HP اولیه همه زامبی‌های این موج */
    private int totalInitialHealth;

    /** مجموع HP از دست رفته تا الان */
    private int totalHealthLost;

    public Wave(int waveNumber, int waveDifficulty, boolean finalWave) {
        this.waveNumber = waveNumber;
        this.waveDifficulty = waveDifficulty;
        this.finalWave = finalWave;
    }

    /** بررسی می‌کند آیا شرط شروع موج بعدی (75% HP از دست رفته) برقرار است */
    public boolean shouldTriggerNextWave() { return false; }

    /** HP از دست رفته یک زامبی را ثبت می‌کند */
    public void registerHealthLost(int amount) { }

    public int getWaveNumber() { return waveNumber; }
    public boolean isFinalWave() { return finalWave; }
    public int getWaveDifficulty() { return waveDifficulty; }
    public List<Zombie> getZombies() { return zombies; }
    public void setZombies(List<Zombie> zombies) { this.zombies = zombies; }
    public boolean isStarted() { return started; }
    public void setStarted(boolean started) { this.started = started; }
    public double getHealthLostPercent() { return healthLostPercent; }
}
