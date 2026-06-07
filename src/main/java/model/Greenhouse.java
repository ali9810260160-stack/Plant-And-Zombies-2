package model;

import java.util.List;

/**
 * گلخانه کاربر - 20 گلدان در 4 ردیف × 5 ستون.
 * ردیف‌های 2 تا 4 ابتدا قفل هستند.
 */
public class Greenhouse {

    /** ماتریس گلدان‌ها (4 ردیف × 5 ستون، 1-based) */
    private Pot[][] pots;

    public Greenhouse() {
        this.pots = new Pot[4][5];
    }

    /** گلدان مشخص را برمی‌گرداند (x: 1-5، y: 1-4) */
    public Pot getPot(int x, int y) { return null; }

    /** بررسی می‌کند آیا گلدان آزاد و قابل کاشت است */
    public boolean isPotAvailable(int x, int y) { return false; }

    /** تعداد گلدان‌های آزاد‌شده را برمی‌گرداند */
    public int getUnlockedPotCount() { return 0; }

    /** لیست گلدان‌های آماده برداشت را برمی‌گرداند */
    public List<Pot> getReadyPots() { return null; }

    public Pot[][] getPots() { return pots; }
}
