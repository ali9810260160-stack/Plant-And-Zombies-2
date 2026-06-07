package service;

import model.GameSession;
import model.Sun;
import model.enums.SunType;

/**
 * سرویس مدیریت خورشید.
 * مسئول سقوط خودکار، برداشت و فرمول زمان‌بندی خورشید.
 */
public class SunService {

    /**
     * بررسی می‌کند آیا در این تیک خورشید جدیدی باید از آسمان بیفتد.
     * فرمول: x = max(6 + 0.05t, 12) که t ثانیه‌های گذشته.
     * @param session session جاری
     * @return true اگر باید خورشید بیفتد
     */
    public boolean shouldDropSun(GameSession session) { return false; }

    /**
     * یک خورشید جدید از آسمان تولید می‌کند.
     * نوع را به صورت تصادفی انتخاب می‌کند (80%/15%/5%).
     * @param session session جاری
     * @return خورشید ساخته‌شده
     */
    public Sun dropSun(GameSession session) { return null; }

    /**
     * نوع خورشید را به صورت تصادفی انتخاب می‌کند.
     * @return نوع خورشید
     */
    public SunType selectRandomSunType() { return null; }

    /**
     * خورشید سقوط‌کننده را یک تیک جلو می‌برد.
     * @param session session جاری
     * @param sun خورشید
     */
    public void advanceFallingSun(GameSession session, Sun sun) { }

    /**
     * خورشید رادیواکتیو را در هوا منفجر می‌کند.
     * 150 آسیب در 5×5 به زامبی‌ها، 80 آسیب در 3×3 به گیاهان.
     * @param session session جاری
     * @param sun خورشید رادیواکتیو
     */
    public void explodeRadioactiveSun(GameSession session, Sun sun) { }

    /**
     * خورشید را از گیاه تولیدکننده برداشت می‌کند.
     * @param session session جاری
     * @param plantX ستون گیاه
     * @param plantY ردیف گیاه
     */
    public void collectPlantSun(GameSession session, int plantX, int plantY) { }

    /**
     * خورشید در حال سقوط را برداشت می‌کند.
     * @param session session جاری
     * @param x ستون
     * @param y ردیف
     */
    public void collectFallingSun(GameSession session, int x, int y) { }

    /**
     * n خورشید به موجودی بازیکن اضافه می‌کند.
     * @param session session جاری
     * @param amount مقدار
     */
    public void addSun(GameSession session, int amount) { }

    /**
     * فاصله سقوط خورشید بر حسب ثانیه را محاسبه می‌کند.
     * @param elapsedSeconds ثانیه‌های گذشته از شروع
     * @return فاصله به ثانیه
     */
    public double calculateSunInterval(double elapsedSeconds) { return 0; }
}
