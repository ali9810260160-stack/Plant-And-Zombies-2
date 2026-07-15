package model;

import model.enums.SunType;

/**
 * مدل خورشید در حال سقوط یا موجود روی صفحه.
 */
public class Sun {

    private SunType type;
    private int x;
    private int y;
    private double fallProgress;
    private boolean landed;
    private boolean collected;

    /** تیک شروع سقوط */
    private int spawnTick;

    /** ارزش خورشید */
    private int value;

    public Sun(SunType type, int x, int y, int spawnTick) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.spawnTick = spawnTick;
        this.fallProgress = 0;
        this.landed = false;
        this.collected = false;
        this.value = getSunValue(type);
    }

    private int getSunValue(SunType sunType) {
        switch (sunType) {
            case NORMAL:       return 25;
            case SPECIAL:      return 100;
            case RADIOACTIVE:  return 150;
            default:           return 25;
        }
    }

    public SunType getType() { return type; }
    public int getX() { return x; }
    public int getY() { return y; }
    public double getFallProgress() { return fallProgress; }
    public void setFallProgress(double fp) { this.fallProgress = fp; }
    public boolean isLanded() { return landed; }
    public void setLanded(boolean landed) { this.landed = landed; }
    public boolean isCollected() { return collected; }
    public void setCollected(boolean collected) { this.collected = collected; }
    public int getSpawnTick() { return spawnTick; }
    public int getValue() { return value; }

    public void setValue(int value) {
        this.value = value;
    }
}
