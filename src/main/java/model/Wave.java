package model;

import model.zombies.Zombie;

import java.util.ArrayList;
import java.util.List;

/**
 * یک موج حمله زامبی در یک مرحله.
 */
public class Wave {

    private int waveNumber;
    private boolean finalWave;
    private int waveDifficulty;
    private List<Zombie> zombies;
    private boolean started;
    private int totalInitialHealth;
    private int totalHealthLost;

    public Wave(int waveNumber, int waveDifficulty, boolean finalWave) {
        this.waveNumber = waveNumber;
        this.waveDifficulty = waveDifficulty;
        this.finalWave = finalWave;
        this.zombies = new ArrayList<>();
        this.started = false;
        this.totalInitialHealth = 0;
        this.totalHealthLost = 0;
    }

    public boolean shouldTriggerNextWave() {
        if (totalInitialHealth == 0) {
            return false;
        }
        double lostPercent = (double) totalHealthLost / totalInitialHealth;
        return lostPercent >= 0.75;
    }

    public void registerHealthLost(int amount) {
        totalHealthLost += amount;
    }

    public void registerZombieAdded(Zombie z) {
        totalInitialHealth += z.getMaxHealth();
    }

    public double getHealthLostPercent() {
        if (totalInitialHealth == 0) {
            return 0;
        }
        return (double) totalHealthLost / totalInitialHealth;
    }

    public int getWaveNumber() { return waveNumber; }
    public boolean isFinalWave() { return finalWave; }
    public int getWaveDifficulty() { return waveDifficulty; }
    public List<Zombie> getZombies() { return zombies; }
    public void setZombies(List<Zombie> zombies) { this.zombies = zombies; }
    public boolean isStarted() { return started; }
    public void setStarted(boolean started) { this.started = started; }
    public int getTotalInitialHealth() { return totalInitialHealth; }
    public void setTotalInitialHealth(int h) { this.totalInitialHealth = h; }
    public int getTotalHealthLost() { return totalHealthLost; }
    public void setTotalHealthLost(int h) { this.totalHealthLost = h; }
}
