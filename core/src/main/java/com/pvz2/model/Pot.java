package com.pvz2.model;

import java.time.LocalDateTime;

public class Pot {

    private int x;
    private int y;
    private boolean locked;
    private String plantType;
    private LocalDateTime plantedAt;
    private int growthHours;

    public Pot(int x, int y) {
        this.x = x;
        this.y = y;
        this.locked = false;
    }

    public boolean isReady() {
        if (plantedAt == null || plantType == null) return false;
        long elapsed = java.time.temporal.ChronoUnit.HOURS.between(
                plantedAt, LocalDateTime.now());
        return elapsed >= growthHours;
    }

    public double getRemainingHours() {
        long elapsed = java.time.temporal.ChronoUnit.HOURS.between(
                plantedAt, LocalDateTime.now());
        return growthHours - elapsed;
    }

    /**
     * ثانیه‌های باقی‌مانده تا آماده شدن گیاه — دقت بالاتر از
     * {@link #getRemainingHours()} (که به ساعت گرد می‌کنه)، برای نمایش
     * زنده‌ی «Xh Ym» در UI. هیچ‌وقت منفی برنمی‌گردونه.
     */
    public long getRemainingSeconds() {
        if (plantedAt == null || plantType == null) return 0;
        long totalSeconds = growthHours * 3600L;
        long elapsedSeconds = java.time.temporal.ChronoUnit.SECONDS.between(
                plantedAt, LocalDateTime.now());
        return Math.max(0, totalSeconds - elapsedSeconds);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public String getPlantType() { return plantType; }
    public void setPlantType(String plantType) { this.plantType = plantType; }
    public LocalDateTime getPlantedAt() { return plantedAt; }
    public void setPlantedAt(LocalDateTime t) { this.plantedAt = t; }
    public int getGrowthHours() { return growthHours; }
    public void setGrowthHours(int h) { this.growthHours = h; }
}
