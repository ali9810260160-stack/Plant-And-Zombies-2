package com.pvz2.graphics.util;

/** تنظیمات اجرایی. */
public final class GameConfig {
    private GameConfig() {}

    /**
     * [FILL ME] مسیر پوشه‌ی asset پک استخراج‌شده‌ی PvZ2 (همون پوشه‌ای که
     * IMAGES/, ATLASES/, Resources.json توشه).
     * <p>
     * همین‌جا مستقیم بنویسش -- مطمئن‌ترین راهه، چون به تنظیمات VM options
     * تو IntelliJ/Gradle وابسته نیست (که گاهی به پروسه‌ی اجرای بازی
     * منتقل نمی‌شن). مثال ویندوز (حتماً با \\\\ یا / بنویس، نه یک \\ تنها):
     *   "C:\\\\Users\\\\Asus\\\\Documents\\\\pvz2-assets"
     * یا:
     *   "C:/Users/Asus/Documents/pvz2-assets"
     */
    private static final String HARDCODED_PATH = "";

    /** اگه HARDCODED_PATH خالی بمونه، از system property هم پشتیبانی می‌شه (اختیاری). */
    public static final String PVZ_ASSETS_PATH =
            !HARDCODED_PATH.isEmpty() ? HARDCODED_PATH : System.getProperty("pvz.assets", "C:\\Users\\Asus\\Documents\\ap-project-plant-and-zombies\\assets");

    // ─── تنظیمات قابل‌تغییر در runtime ─────────────────────────────────────
    public static int     gameSpeed        = 0;     // 0=1x 1=1.5x 2=2x
    public static boolean showGrid         = false;
    public static boolean debugMode        = false;
    public static int     difficultyLevel  = 3;     // 1..5
    public static float   musicVolume      = 0.7f;
    public static float   sfxVolume        = 1.0f;
}
