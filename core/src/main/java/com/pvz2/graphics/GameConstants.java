package com.pvz2.graphics;

/** ثابت‌های اصلی طراحی بصری — ۱۲۸۰×۷۲۰. */
public final class GameConstants {
    private GameConstants() {}

    public static final int   VIEWPORT_WIDTH  = 1280;
    public static final int   VIEWPORT_HEIGHT = 720;

    // ─── ابعاد شبکه بازی (فاز ۱: ستون ۱..۹ | ردیف ۱..۵) ───────────────────
    public static final int   TILE_COLS  = 9;
    public static final int   TILE_ROWS  = 5;
    public static final float TILE_W     = 136f;
    public static final float TILE_H     = 124f;

    // ─── موقعیت شبکه روی صفحه ───────────────────────────────────────────────
    /** فاصله از چپ — فضای چمن‌زن */
    public static final float GRID_X     = 52f;
    /** پایین شبکه — بالای نوار seed bank */
    public static final float GRID_Y     = 120f;
    public static final float GRID_W     = TILE_COLS * TILE_W;   // 1224px
    public static final float GRID_H     = TILE_ROWS * TILE_H;   // 620px

    // ─── نوار seed bank (پایین) ──────────────────────────────────────────────
    public static final float SEED_BANK_Y = 0f;
    public static final float SEED_BANK_H = 120f;
    public static final float CARD_W      = 82f;
    public static final float CARD_H      = 112f;
    public static final float CARD_PAD    = 3f;

    // ─── HUD (بالا) ───────────────────────────────────────────────────────────
    public static final float HUD_Y = GRID_Y + GRID_H;           // 740? → باید باشه 740
    // اصلاح: چون GRID_Y=120 و GRID_H=620 → HUD_Y=740 > 720 → تنظیم
    // بهتره: GRID_Y=100, SEED_BANK_H=100, HUD_H=80 → 100+620+... نه
    // واقعی: top bar 80px | grid 540px | seed bank 100px = 720
    public static final float TOP_BAR_Y  = 640f;
    public static final float TOP_BAR_H  = 80f;

    // ─── ابعاد نهایی با TOP_BAR=80, SEED_BANK=100 ────────────────────────────
    // بازنویسی ثابت‌ها برای چیدمان صحیح:
    // y=0..100 → seed bank
    // y=100..640 → grid (5×108=540px)
    // y=640..720 → HUD/top bar
    public static final float GRID_Y_REAL     = 100f;
    public static final float TILE_H_REAL     = 108f;   // 540/5
    public static final float SEED_BANK_H_REAL= 100f;

    // ─── مقادیر اصلی که در کد استفاده می‌شود ────────────────────────────────
    // (اینها را جایگزین موارد بالا می‌کنیم)
    public static final float G_X  = 52f;     // grid left x
    public static final float G_Y  = 100f;    // grid bottom y
    public static final float TW   = 136f;    // tile width
    public static final float TH   = 108f;    // tile height
    public static final float SB_H = 100f;    // seed bank height

    public static final int   MAX_PLANT_SLOTS = 8;
    public static final float CARD_ANIM_COOLDOWN_SPEED = 0.5f;
    public static final float TOAST_DURATION = 2.5f;

    /** ضریب‌های سرعت بازی: 1× 1.5× 2× */
    public static final float[] SPEED_MULTIPLIERS = {1f, 1.5f, 2f};

    /** ۱۰ تیک = ۱ ثانیه بازی */
    public static final float TICKS_PER_SECOND = 10f;
}
