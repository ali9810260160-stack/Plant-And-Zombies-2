package com.pvz2.graphics;

/**
 * ثابت‌های اصلی طراحی بصری — ۱۲۸۰×۷۲۰.
 *
 * <p><b>هندسه گرید از {@link com.pvz2.map.MapLoader} می‌آید</b> — همان جایی که
 * TILED_MAP_GUIDE.md بخش ۲ آن را تعریف کرده (HOUSE=160px | GRID=900px=9×100 |
 * ZOMBIE_ENTRY=180px | RAIL=40px، جمعاً ۱۲۸۰px). این طراحی عمدی است تا مختصات
 * محاسبه‌شده توسط {@link com.pvz2.graphics.util.GameCoords} دقیقاً روی همان
 * خانه‌هایی بیفتد که نقشه Tiled (world.tmx) رسم می‌کند — یک منبع حقیقت، بدون
 * احتمال drift بین دو لایه.
 */
public final class GameConstants {
    private GameConstants() {}

    public static final int   VIEWPORT_WIDTH  = 1280;
    public static final int   VIEWPORT_HEIGHT = 720;

    // ─── ابعاد شبکه بازی (فاز ۱: ستون ۱..۹ | ردیف ۱..۵) ───────────────────
    public static final int   TILE_COLS  = com.pvz2.map.MapLoader.GRID_COLS;   // 9
    public static final int   TILE_ROWS  = com.pvz2.map.MapLoader.GRID_ROWS;   // 5

    // ─── موقعیت و اندازه شبکه روی صفحه ────────────────────────────────────
    // ⚠️ این مقادیر دیگر ثابت نیستند: هنگام بارگذاری world.tmx توسط
    // {@link com.pvz2.map.MapLoader}، از روی خودِ آبجکت‌های PLANT_SLOT نقشه
    // (که به فضای صفحه ۱۲۸۰×۷۲۰ مقیاس شده‌اند) دوباره محاسبه می‌شوند تا گرید
    // منطقی دقیقاً روی همان کاشی‌هایی بیفتد که نقشه رسم می‌کند — یک منبع حقیقت،
    // بدون drift بین لایه نقشه و لایه موجودیت‌ها. اگر نقشه بارگذاری نشود،
    // این مقادیر پیش‌فرض (طرح‌بندی راهنمای ۱۲۸۰×۷۲۰) باقی می‌مانند و رندر
    // رویه‌ای (مستطیل رنگی) از آن‌ها استفاده می‌کند.
    /** فاصله از چپ — شروع ناحیه گرید */
    public static float G_X  = 160f;
    /** پایین شبکه */
    public static float G_Y  = 120f;
    /** عرض هر کاشی */
    public static float TW   = 100f;
    /** ارتفاع هر کاشی */
    public static float TH   = 100f;

    // ─── ناحیه خانه (چپ) و ورود زامبی (راست) — از نقشه محاسبه می‌شوند ───────
    public static float HOUSE_ZONE_W   = 160f;
    public static float ZOMBIE_ZONE_X  = 1060f;
    public static float ZOMBIE_ZONE_W  = 180f;

    /**
     * به‌روزرسانی هندسه گرید از روی نقشه بارگذاری‌شده — توسط
     * {@link com.pvz2.map.MapLoader#load} صدا زده می‌شود.
     * نوارهای UI (seed bank / HUD) عمداً از این مقادیر مستقل‌اند و ثابت می‌مانند.
     */
    public static void applyGridGeometry(float gx, float gy, float tw, float th,
                                         float houseW, float zombieX, float zombieW) {
        if (tw <= 0 || th <= 0) return;   // داده نامعتبر — پیش‌فرض را نگه دار
        G_X = gx; G_Y = gy; TW = tw; TH = th;
        HOUSE_ZONE_W = houseW; ZOMBIE_ZONE_X = zombieX; ZOMBIE_ZONE_W = zombieW;
    }

    /** بازگرداندن هندسه گرید به پیش‌فرض ۱۲۸۰×۷۲۰ (وقتی نقشه‌ای بارگذاری نشده). */
    public static void resetGridGeometry() {
        G_X = 160f; G_Y = 120f; TW = 100f; TH = 100f;
        HOUSE_ZONE_W = 160f; ZOMBIE_ZONE_X = 1060f; ZOMBIE_ZONE_W = 180f;
    }

    // ─── نوار seed bank (پایین صفحه) — ثابت، مستقل از هندسه گرید نقشه ───────
    public static final float SB_H = 120f;   // فضای UI پایین صفحه

    public static final float CARD_W      = 82f;
    public static final float CARD_H      = 112f;
    public static final float CARD_PAD    = 3f;

    // ─── HUD / نوار بالا — ثابت، مستقل از هندسه گرید نقشه ──────────────────
    public static final float TOP_BAR_Y = VIEWPORT_HEIGHT - 100f;                  // 620
    public static final float TOP_BAR_H = 100f;

    public static final int   MAX_PLANT_SLOTS = 8;
    public static final float CARD_ANIM_COOLDOWN_SPEED = 0.5f;
    public static final float TOAST_DURATION = 2.5f;

    /** هزینه‌ی الماس برای boost کردن یک گیاه — صفحه‌ی انتخاب گیاه. */
    public static final int PLANT_BOOST_COST_GEMS = 15;

    /** ضریب‌های سرعت بازی: 1× 1.5× 2× */
    public static final float[] SPEED_MULTIPLIERS = {1f, 1.5f, 2f};

    /** ۱۰ تیک = ۱ ثانیه بازی */
    public static final float TICKS_PER_SECOND = 10f;
}
