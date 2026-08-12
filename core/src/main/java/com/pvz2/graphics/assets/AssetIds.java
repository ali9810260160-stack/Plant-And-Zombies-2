package com.pvz2.graphics.assets;

/**
 * شناسه‌ی resource های asset پک اصلی PvZ2 که در منوی اصلی و فروشگاه استفاده می‌شوند.
 * <p>
 * هرکدوم رو با PvZ Asset Browser پیدا کن و مقدارش رو جایگزین کن. تا وقتی
 * خالی («‌«) بمونن، همون بخش گرافیکی مربوطه (پس‌زمینه/لوگو/آیکون) رندر
 * نمی‌شه و به‌جاش fallback متنی/بدون‌آیکون نمایش داده می‌شه — کرش نمی‌کنه.
 */


public final class AssetIds {

    private AssetIds() { }

    // ─── منوی اصلی ───────────────────────────────────────────────

    /** [FILL ME] پس‌زمینه‌ی تمام‌صفحه‌ی منوی اصلی. */
    public static final String MAIN_MENU_BACKGROUND = "IMAGE_MAINMENU_BACKGROUND";

    /** [FILL ME] لوگوی «Plants vs. Zombies 2» — جایگزین متن ساده می‌شود. */
    public static final String MAIN_MENU_LOGO = "IMAGE_UI_MAINMENU_PVZ2_LOGO_HORIZONTAL";

    /** [FILL ME] آیکون سکه برای نوار بالا (HUD). */
    public static final String ICON_COIN = "IMAGE_EFFECTS_COIN_GOLD_COIN_GOLD_98X95";

    /** [FILL ME] آیکون الماس برای نوار بالا (HUD). */
    public static final String ICON_GEM = "IMAGE_EFFECTS_PRIZE_GEMS_LARGE_PRIZE_GEMS_LARGE_302X296";

    /** [FILL ME] آیکون دکمه‌ی اخبار (روزنامه/بلندگو). اختیاری — بدونش فقط دکمه متنی می‌مونه. */
    public static final String ICON_NEWS_BUTTON = "IMAGE_UI_HUD_NEWSBUTTON_BUTTONS_HUD_NEWS_SELECTED_COPY_2";

    /** [FILL ME] آیکون نشان (badge) هشدار اخبار خوانده‌نشده. اختیاری — بدونش فقط عدد قرمز کافیه. */
    public static final String ICON_NEWS_ALERT = "IMAGE_UI_CLAIM_SMALL";

    // ─── فروشگاه (Shop) ──────────────────────────────────────────

    /** [FILL ME] آیکون گلدان گلخانه. */
    public static final String ICON_SHOP_POT = "IMAGE_UI_GENERIC_BUTTON_HUD_MINIGAMES_SELECTED";

    /** [FILL ME] آیکون بطری Plant Food. */
    public static final String ICON_SHOP_PLANT_FOOD = "IMAGE_UI_ALMANAC_PLANT_FOOD_STAT_ICON";

    /** [FILL ME] آیکون بسته‌بذر تصادفی. */
    public static final String ICON_SHOP_SEED_RANDOM = "";

    /** [FILL ME] آیکون بسته‌بذر انتخابی. */
    public static final String ICON_SHOP_SEED_CHOICE = "";

    /** [FILL ME] آیکون تبدیل الماس به سکه. */
    public static final String ICON_SHOP_CURRENCY = "";

    /** [FILL ME] آیکون پیشنهاد روزانه (جعبه هدیه). */
    public static final String ICON_SHOP_DAILY = "IMAGE_UI_HUD_LOD_LOD_BDAY_GIFT";

    // ─── آیکون‌های چپترهای Adventure ───

    /** [FILL ME] Ancient Egypt chapter icon (pyramid). */
    public static final String ICON_CHAPTER_ANCIENT_EGYPT = "IMAGE_UI_EVENT_PANELS_EGYPT";

    /** [FILL ME] Frostbite Caves chapter icon (snowflake/ice cave). */
    public static final String ICON_CHAPTER_FROSTBITE_CAVES = "IMAGE_UI_EVENT_PANELS_ICEAGE";

    /** [FILL ME] Big Wave Beach chapter icon (wave/beach). */
    public static final String ICON_CHAPTER_BIG_WAVE_BEACH = "IMAGE_UI_EVENT_PANELS_BEACH";

    /** [FILL ME] Dark Ages chapter icon (castle). */
    public static final String ICON_CHAPTER_DARK_AGES = "IMAGE_UI_EVENT_PANELS_DARKAGES";

    /** [FILL ME] Lock icon overlay for locked chapters/levels. */
    public static final String ICON_LOCK = "";

    // ─── پروفایل ───

    /** [FILL ME] آواتار/فریم پروفایل پیش‌فرض. اختیاری. */
    public static final String ICON_PROFILE_AVATAR = "IMAGE_UI_MAINMENU_MM_PLAYERICON" +
        "";

    // ─── گلخانه (Greenhouse) ───

    /** [FILL ME] آیکون گلدان خالی. */
    public static final String ICON_POT_EMPTY = "IMAGE_FIREBREAKER_VASE_GREEN_FIREWORKS_VASE_GREEN_FIREWORKS_115X150";

    /** [FILL ME] جلوه‌ی درخشش/آماده‌ی برداشت روی گلدان. */
    public static final String ICON_POT_READY_GLOW = "";
}
