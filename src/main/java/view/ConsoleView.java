package view;

/**
 * کلاس مرکزی چاپ خروجی به ترمینال.
 * تمام print‌ها از این کلاس عبور می‌کنند تا در فاز 2 راحت‌تر جایگزین شوند.
 */
public class ConsoleView {

    /** یک پیام موفقیت چاپ می‌کند */
    public void printSuccess(String message) { }

    /** یک پیام خطا چاپ می‌کند */
    public void printError(String message) { }

    /** یک پیام اطلاعاتی چاپ می‌کند */
    public void printInfo(String message) { }

    /** لیستی از رشته‌ها را با شماره‌گذاری چاپ می‌کند */
    public void printNumberedList(java.util.List<String> items) { }

    /**
     * اطلاعات زامبی را با فرمت مشخص‌شده در داک چاپ می‌کند.
     * @param zombieInfoLines خطوط اطلاعات
     */
    public void printZombiesInfo(java.util.List<String> zombieInfoLines) { }

    /**
     * وضعیت گیاهان را چاپ می‌کند (هزینه، cooldown، قابل کاشت بودن).
     * @param statusLines خطوط وضعیت
     */
    public void printPlantsStatus(java.util.List<String> statusLines) { }

    /** پیام spawn موج را چاپ می‌کند */
    public void printWaveStarted(int waveNumber, boolean isFinal) { }

    /** پیام spawn زامبی را چاپ می‌کند */
    public void printZombieSpawned(String type, int wave, int lane, int cost) { }

    /** پیام مرگ زامبی را چاپ می‌کند */
    public void printZombieDead(String type, double x, int y) { }

    /** پیام نابودی گیاه را چاپ می‌کند */
    public void printPlantDestroyed(String type, int x, int y) { }

    /** پیام تولید خورشید گیاه را چاپ می‌کند */
    public void printPlantProducedSun(String plantType, int x, int y) { }

    /** پیام ظهور خورشید جدید از آسمان را چاپ می‌کند */
    public void printSunDropping(String type, int x, int y) { }

    /** پیام فرود خورشید به زمین را چاپ می‌کند */
    public void printSunLanded(int x, int y) { }

    /** پیام drop آیتم از زامبی را چاپ می‌کند */
    public void printZombieDropped(String item, int count) { }

    /** پیام ماشین چمن‌زنی را چاپ می‌کند */
    public void printLawnMowerTriggered(int row, java.util.List<String> killedZombies) { }

    /** پیام باخت را چاپ می‌کند */
    public void printGameOver() { }

    /** پیام برد را چاپ می‌کند */
    public void printGameWon() { }

    /** پیام glowing zombie plant food drop را چاپ می‌کند */
    public void printGlowingZombieDroppedFood(int currentCount) { }
}
