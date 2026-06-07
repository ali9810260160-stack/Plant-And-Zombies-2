package model.plants;

/**
 * کلاس انتزاعی برای گیاهان تولیدکننده خورشید.
 * مشترکات Sunflower, TwinSunflower, SunShroom و ... اینجاست.
 */
public abstract class SunProducer extends Plant {

    /** مقدار خورشید تولیدشده در هر بار */
    protected int sunProduced;

    /** تیک‌های بین دو تولید خورشید */
    protected int productionIntervalTicks;

    /** تیک‌های گذشته از آخرین تولید */
    protected int ticksSinceLastProduction;

    /** آیا خورشید تولیدشده روی گیاه منتظر برداشت است */
    protected boolean hasPendingSun;

    /** آیا گیاه در حال تولید خورشید بعدی است */
    public boolean isProducing() { return !hasPendingSun; }

    /** مقدار خورشید را برمی‌گرداند و خورشید منتظر را صفر می‌کند */
    public int collectSun() { return 0; }

    public int getSunProduced() { return sunProduced; }
    public int getProductionIntervalTicks() { return productionIntervalTicks; }
    public boolean isHasPendingSun() { return hasPendingSun; }
    public void setHasPendingSun(boolean hasPendingSun) { this.hasPendingSun = hasPendingSun; }
}
