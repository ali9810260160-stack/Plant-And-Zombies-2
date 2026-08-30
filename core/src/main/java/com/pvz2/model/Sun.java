package com.pvz2.model;

import com.pvz2.model.enums.SunType;

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
    private int value;  // mutable: radioactive→normal on landing

    // ─── دزدیده‌شدن توسطِ Ra: خورشید بنفش می‌شود و طیِ ۴ ثانیه به‌سمتِ چوبِ Ra
    //     کشیده و دزدیده می‌شود (لایه‌ی گرافیک این‌ها را برای رنگ/حرکت می‌خواند).
    private boolean beingStolen;
    private double  stealProgress;   // ۰..۱ پیشرفتِ کشیده‌شدن
    private int     stealerCol;      // ستونِ Ra (مقصدِ کشش)
    private int     stealerRow;      // ردیفِ Ra

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
    public void setValue(int v) { this.value = v; }

    public boolean isBeingStolen() { return beingStolen; }
    public void setBeingStolen(boolean b) { this.beingStolen = b; }
    public double getStealProgress() { return stealProgress; }
    public void setStealProgress(double p) { this.stealProgress = p; }
    public int getStealerCol() { return stealerCol; }
    public int getStealerRow() { return stealerRow; }
    public void setStealer(int col, int row) { this.stealerCol = col; this.stealerRow = row; }
}
