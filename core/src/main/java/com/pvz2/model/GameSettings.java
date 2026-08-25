package com.pvz2.model;

import com.google.gson.Gson;
import com.pvz2.util.FileUtil;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * تنظیمات گرافیکی و گیم‌پلی اضافه‌شده در فاز دوم.
 * این کلاس کاملاً جدید است و هیچ فایل فاز یکی را تغییر نمی‌دهد.
 *
 * ذخیره‌سازی:
 *   فایل JSON در data/settings.json
 *   با Gson سریال/دی‌سریال می‌شود (مانند الگوی QuestDataRegistry)
 *
 * Singleton — یک نمونه در کل برنامه.
 *
 * تنظیمات:
 *   gameSpeed    (1.0f – 3.0f)  — ضریب سرعت گیم‌پلی
 *   showGrid     (boolean)       — نمایش خطوط شبکه روی board
 *   debugMode    (boolean)       — فعال کردن پنل تقلب در GameScreen
 *   masterVolume (0.0f – 1.0f)  — حجم کلی صدا
 *   musicEnabled (boolean)       — موسیقی پس‌زمینه
 *   sfxEnabled   (boolean)       — افکت‌های صوتی
 */
public class GameSettings {

    // ═══════════════════════════════════════════════
    //  مسیر فایل
    // ═══════════════════════════════════════════════

    private static final String SETTINGS_FILE = "data/settings.json";

    // ═══════════════════════════════════════════════
    //  Singleton
    // ═══════════════════════════════════════════════

    private static GameSettings instance;

    /**
     * دریافت نمونه Singleton.
     * اگر هنوز بارگذاری نشده → از فایل JSON بخوان.
     * اگر فایل وجود ندارد → مقادیر پیش‌فرض.
     *
     * TODO: پیاده‌سازی:
     *   if (instance == null):
     *     File file = new File(SETTINGS_FILE);
     *     if (file.exists()):
     *       try (FileReader r = new FileReader(file)):
     *         instance = new Gson().fromJson(r, GameSettings.class);
     *     if (instance == null): instance = new GameSettings();
     *   return instance;
     */
    public static GameSettings getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    /**
     * ریست singleton (مثلاً بین تست‌ها).
     * TODO: instance = null;
     */
    public static void reset() {
        instance = null;
    }

    // ═══════════════════════════════════════════════
    //  فیلدهای تنظیمات (سریال‌شونده با Gson)
    // ═══════════════════════════════════════════════

    /** ضریب سرعت بازی: 1.0 = عادی، 2.0 = دو برابر، 3.0 = سه برابر. */
    private float gameSpeed = 1.0f;

    /** نمایش خطوط شبکه‌بندی روی board حین بازی. */
    private boolean showGrid = false;

    /** حالت Debug — نمایش پنل تقلب. */
    private boolean debugMode = false;

    /** حجم کلی صدا [0.0 – 1.0]. */
    private float masterVolume = 0.8f;

    /** آیا موسیقی پس‌زمینه فعال است؟ */
    private boolean musicEnabled = true;

    /** آیا افکت‌های صوتی فعال هستند؟ */
    private boolean sfxEnabled = true;

    // ═══════════════════════════════════════════════
    //  Constructor خصوصی
    // ═══════════════════════════════════════════════

    /** Gson نیاز به constructor بدون آرگومان دارد. */
    private GameSettings() {}

    // ═══════════════════════════════════════════════
    //  بارگذاری و ذخیره
    // ═══════════════════════════════════════════════

    /**
     * بارگذاری از فایل JSON.
     * اگر فایل وجود نداشت → شیء با مقادیر پیش‌فرض.
     *
     * TODO: پیاده‌سازی:
     *   File file = new File(SETTINGS_FILE);
     *   if (!file.exists()) return new GameSettings();
     *   try (FileReader reader = new FileReader(file)):
     *     GameSettings s = new Gson().fromJson(reader, GameSettings.class);
     *     return (s != null) ? s : new GameSettings();
     *   catch (IOException e):
     *     return new GameSettings();
     */
    private static GameSettings load() {
        File file = new File(SETTINGS_FILE);
        if (!file.exists()) return new GameSettings();
        try (FileReader reader = new FileReader(file)) {
            GameSettings s = new Gson().fromJson(reader, GameSettings.class);
            return (s != null) ? s : new GameSettings();
        } catch (IOException e) {
            return new GameSettings();
        }
    }

    // ═══════════════════════════════════════════════
    //  Persist per-user (فاز ۲)
    // ═══════════════════════════════════════════════

    /**
     * callbackِ ذخیره‌سازیِ per-user — در {@code ServiceLocator.wire()} ست می‌شود
     * تا هر بار {@link #save()} صدا زده شد، تنظیماتِ فعلی روی کاربرِ لاگین‌شده هم
     * نوشته و در users.json ذخیره شود. Model به graphics/repository وابسته نمی‌ماند.
     */
    @FunctionalInterface
    public interface Persister { void persist(GameSettings s); }

    private static Persister persister;

    public static void setPersister(Persister p) { persister = p; }

    /** اعمالِ تنظیماتِ ذخیره‌شده‌ی یک کاربر روی این نمونه (هنگام لاگین/auto-login). */
    public void applyFromUser(User u) {
        if (u == null) return;
        this.masterVolume = u.getMasterVolume();
        this.musicEnabled = u.isMusicEnabled();
        this.sfxEnabled   = u.isSfxEnabled();
        this.showGrid     = u.isShowGrid();
        this.gameSpeed    = u.getGameSpeed();
    }

    /** نوشتنِ تنظیماتِ فعلی روی یک کاربر (پیش از persist در repository). */
    public void writeToUser(User u) {
        if (u == null) return;
        u.setMasterVolume(masterVolume);
        u.setMusicEnabled(musicEnabled);
        u.setSfxEnabled(sfxEnabled);
        u.setShowGrid(showGrid);
        u.setGameSpeed(gameSpeed);
    }

    /**
     * ذخیره تنظیمات در فایل JSON.
     * از SettingsScreen.onSave() فراخوانی می‌شود.
     *
     * TODO: پیاده‌سازی:
     *   try:
     *     new File("data").mkdirs();
     *     try (FileWriter w = new FileWriter(SETTINGS_FILE)):
     *       new Gson().toJson(this, w);
     *   catch (IOException e):
     *     e.printStackTrace();
     */
    public void save() {
        try {
            new File("data").mkdirs();
            try (FileWriter w = new FileWriter(SETTINGS_FILE)) {
                new Gson().toJson(this, w);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // علاوه بر فایلِ سراسری، روی کاربرِ لاگین‌شده هم سیو کن (per-user).
        if (persister != null) {
            try { persister.persist(this); } catch (Exception ignored) { }
        }
    }

    // ═══════════════════════════════════════════════
    //  Getters و Setters
    // ═══════════════════════════════════════════════

    /**
     * ضریب سرعت بازی.
     * این مقدار در GameScreen روی delta time ضرب می‌شود.
     * مقدار مجاز: [1.0 – 3.0]
     */
    public float getGameSpeed() {
        return gameSpeed;
    }

    public void setGameSpeed(float gameSpeed) {
        this.gameSpeed = Math.max(1.0f, Math.min(3.0f, gameSpeed));
    }

    /** آیا شبکه‌بندی board نمایش داده شود؟ */
    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    /** آیا حالت Debug فعال است؟ */
    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /** حجم کلی صدا [0.0 – 1.0]. */
    public float getMasterVolume() {
        return masterVolume;
    }

    public void setMasterVolume(float masterVolume) {
        this.masterVolume = Math.max(0f, Math.min(1f, masterVolume));
    }

    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    public void setMusicEnabled(boolean musicEnabled) {
        this.musicEnabled = musicEnabled;
    }

    public boolean isSfxEnabled() {
        return sfxEnabled;
    }

    public void setSfxEnabled(boolean sfxEnabled) {
        this.sfxEnabled = sfxEnabled;
    }
}
