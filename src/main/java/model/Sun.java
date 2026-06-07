package model;

import model.enums.SunType;

/**
 * یک خورشید در حال سقوط یا روی زمین.
 * توسط بازیکن با collect sun برداشت می‌شود.
 */
public class Sun {

    /** نوع خورشید (معمولی، ویژه، رادیواکتیو) */
    private SunType sunType;

    /** موقعیت افقی هدف */
    private int targetX;

    /** موقعیت عمودی هدف */
    private int targetY;

    /** موقعیت عمودی فعلی (برای انیمیشن سقوط) */
    private double currentY;

    /** ارزش خورشید (25, 100, یا 150 برای رادیواکتیو) */
    private int value;

    /** آیا به زمین رسیده */
    private boolean landed;

    /** تیک ظهور (برای محاسبه زمان سقوط 50 تیک = 5 ثانیه) */
    private int spawnTick;

    /** آیا این خورشید از یک گیاه تولید شده (نه از آسمان) */
    private boolean fromPlant;

    /** موقعیت گیاه مبدأ (برای خورشیدهای گیاهی) */
    private int plantX;
    private int plantY;

    public Sun(SunType sunType, int targetX, int targetY, int spawnTick) {
        this.sunType = sunType;
        this.targetX = targetX;
        this.targetY = targetY;
        this.spawnTick = spawnTick;
        this.landed = false;
        this.value = calculateValue(sunType);
    }

    /** ارزش را بر اساس نوع محاسبه می‌کند */
    private int calculateValue(SunType type) { return 0; }

    public SunType getSunType() { return sunType; }
    public int getTargetX() { return targetX; }
    public int getTargetY() { return targetY; }
    public double getCurrentY() { return currentY; }
    public void setCurrentY(double currentY) { this.currentY = currentY; }
    public int getValue() { return value; }
    public boolean isLanded() { return landed; }
    public void setLanded(boolean landed) { this.landed = landed; }
    public int getSpawnTick() { return spawnTick; }
    public boolean isFromPlant() { return fromPlant; }
    public void setFromPlant(boolean fromPlant) { this.fromPlant = fromPlant; }
    public int getPlantX() { return plantX; }
    public void setPlantX(int plantX) { this.plantX = plantX; }
    public int getPlantY() { return plantY; }
    public void setPlantY(int plantY) { this.plantY = plantY; }
}
