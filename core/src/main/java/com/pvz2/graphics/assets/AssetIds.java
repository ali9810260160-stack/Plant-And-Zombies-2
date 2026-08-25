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

    // ─── Adventure — بازطراحی گرافیکی (فایل‌های PNG لوکال) ─────────────────
    // این‌ها برخلاف بقیه‌ی این فایل، شناسه‌ی RTON نیستن — مسیر نسبی فایل داخل
    // پوشه‌ی assets/ پروژه‌ان و با GameAssets.getInstance().local(...) لود
    // می‌شن (نه GameAssets.region(...)). خود فایل‌ها رو با همین مسیرها زیر
    // assets/adventure/ کپی کن.

    public static final String ADV_BG_CHAPTERS   = "adventure/bg_chapters.png";
    public static final String ADV_BG_EGYPT      = "adventure/bg_egypt.png";
    public static final String ADV_BG_ICEAGE     = "adventure/bg_iceage.png";
    public static final String ADV_BG_BEACH      = "adventure/bg_beach.png";
    public static final String ADV_BG_DARK       = "adventure/bg_dark.png";

    public static final String ADV_CARD_EGYPT    = "adventure/card_egypt.png";
    public static final String ADV_CARD_ICEAGE   = "adventure/card_iceage.png";
    public static final String ADV_CARD_BEACH    = "adventure/card_beach.png";
    public static final String ADV_CARD_DARK     = "adventure/card_dark.png";

    public static final String ADV_ICON_DIFFICULTY = "adventure/icon_difficulty.png";

    public static final String ADV_ICON_BACK       = "adventure/icon_back.png";
    public static final String ADV_ICON_COLLECTION = "adventure/icon_collection.png";
    public static final String ADV_ICON_GREENHOUSE = "adventure/icon_greenhouse.png";
    public static final String ADV_ICON_STORE      = "adventure/icon_store.png";
    public static final String ADV_ICON_QUESTS     = "adventure/icon_quests.png";
    public static final String ADV_ICON_MINIGAMES  = "adventure/icon_minigames.png";
    public static final String ADV_WIDGET_COIN     = "adventure/widget_coin.png";
    public static final String ADV_WIDGET_GEM      = "adventure/widget_gem.png";

    /**
     * [FILL ME] تامبنیل هر مرحله (چپتر → آرایه‌ی ۴تایی به ازای مرحله ۱..۴).
     * فعلاً همه خالی‌ان؛ خودت با شناسه‌ی RTON مناسب (از GameAssets.region)
     * یا مسیر لوکال (با یه پیشوند تشخیص، مثلاً "local:...") پرشون کن.
     * تا وقتی خالی‌ان، جای تامبنیل یه باکس خاکستری ساده رندر می‌شه.
     */
    public static final String[] LEVEL_THUMBNAILS_EGYPT   = {"", "", "", ""};
    public static final String[] LEVEL_THUMBNAILS_ICEAGE  = {"", "", "", ""};
    public static final String[] LEVEL_THUMBNAILS_BEACH   = {"", "", "", ""};
    public static final String[] LEVEL_THUMBNAILS_DARK    = {"", "", "", ""};

    // ─── Shop — بازطراحی گرافیکی (فایل‌های PNG لوکال، خودت فرستادی) ────────
    // مثل بخش Adventure بالا، این‌ها مسیر نسبی فایل زیر assets/ هستن و با
    // GameAssets.getInstance().local(...) لود می‌شن، نه region(...).

    public static final String GH_BG              = "greenhouse/bg_greenhouse.png";
    public static final String GH_POT_GOLDEN      = "greenhouse/pot_golden.png";
    public static final String GH_POT_BROWN       = "greenhouse/pot_brown.png";
    public static final String GH_ICON_LOCKED     = "greenhouse/icon_locked.png";
    public static final String GH_TICKET_DIAMOND  = "greenhouse/ticket_diamond.png";
    public static final String GH_TICKET_COIN     = "greenhouse/ticket_coin.png";
    public static final String GH_ICON_SHOVEL     = "greenhouse/icon_shovel.png";
    public static final String GH_BUTTON_SPEEDUP  = "greenhouse/button_gems_speedup.png";

    public static final String GH_ICON_BACK       = "greenhouse/icon_back.png";
    public static final String GH_ICON_QUESTS     = "greenhouse/icon_quests.png";
    public static final String GH_ICON_MINIGAMES  = "greenhouse/icon_minigames.png";
    public static final String GH_ICON_STORE      = "greenhouse/icon_store.png";
    public static final String GH_WIDGET_PLANTFOOD = "greenhouse/widget_plantfood.png";
    public static final String GH_WIDGET_COIN     = "greenhouse/widget_coin.png";
    public static final String GH_WIDGET_GEM      = "greenhouse/widget_gem.png";

    // ─── Quests — بازطراحی گرافیکی (منوی مأموریت‌ها) ────────────────────────
    // مثل بخش Adventure بالا، این‌ها مسیر نسبی فایل داخل assets/ هستن و با
    // GameAssets.getInstance().local(...) لود می‌شن. فایل‌ها زیر assets/quests/.

    /** تب «Daily» فعال — سبز با فلش رو به پایین (طبق مرجع quests-menu-list). */
    public static final String QUEST_TAB_DAILY_ACTIVE    = "quests/daily_active.png";
    /** تب «Daily» غیرفعال. */
    public static final String QUEST_TAB_DAILY_INACTIVE  = "quests/daily_inactive.png";
    /** تب «Events» فعال — آبی (فایل اصلی «epic»، اما در UI برای دسته‌ی Events استفاده می‌شود). */
    public static final String QUEST_TAB_EVENTS_ACTIVE   = "quests/epic_active.png";
    /** تب «Events» غیرفعال. */
    public static final String QUEST_TAB_EVENTS_INACTIVE = "quests/epic_inactive.png";
    /** تب «Main/Story» (اصلی) فعال — از دکمه‌ی قهوه‌ایِ عمومی استفاده می‌شود. */
    public static final String QUEST_TAB_MAIN_ACTIVE     = "quests/brown_active.png";
    /** تب «Main/Story» غیرفعال. */
    public static final String QUEST_TAB_MAIN_INACTIVE   = "quests/brown_down.png";
    /** تب «Minigames» فعال — از دکمه‌ی ساحلِ موجِ بزرگ استفاده می‌شود. */
    public static final String QUEST_TAB_MINI_ACTIVE     = "quests/bigwavebeach_active.png";
    /** تب «Minigames» غیرفعال. */
    public static final String QUEST_TAB_MINI_INACTIVE   = "quests/bigwavebeach_down.png";

    /** آیکون کوچک سکه — نوار بالا و کنار مقدار جایزه. */
    public static final String QUEST_ICON_COIN  = "quests/coin_icon.png";
    /** آیکون کوچک الماس — نوار بالا و کنار مقدار جایزه. */
    public static final String QUEST_ICON_GEM   = "quests/gem_icon.png";

    /** آیکون بزرگ جایزه‌ی سکه (کنار هر ردیف کوئست). */
    public static final String QUEST_REWARD_COINS = "quests/epic_reward_coins.png";
    /** آیکون بزرگ جایزه‌ی الماس (کنار هر ردیف کوئست). */
    public static final String QUEST_REWARD_GEMS  = "quests/epic_reward_gems.png";

    /** دکمه‌ی ضربدر قرمز بستن صفحه — بالای panel_edge_to_edge، سمت راست. */
    public static final String QUEST_CLOSE_TAB = "quests/close_tab.png";

    /** نوار چوبی بالای صفحه (هدر) — کشیده می‌شود تا عرض کامل صفحه. */
    public static final String QUEST_PANEL_EDGE = "quests/panel_edge_to_edge.png";

    /** پس‌زمینه‌ی ردیف کوئست روزانه‌ی «آماده‌ی دریافت» (سرِ سبز + بدنه‌ی کرم). */
    public static final String QUEST_ROW_CLAIMABLE_BG = "quests/quest_panel_daily.png";
    public static final String SHOP_BACKGROUND    = "shop/shop-background.png";
    public static final String SHOP_PRODUCT_CARD  = "shop/product-card.png";
    public static final String SHOP_CLOSE_BUTTON  = "shop/close_tab.png";

    public static final String SHOP_ICON_POT_LOCAL      = "shop/shop-pot.png";
    public static final String SHOP_ICON_PLANT_FOOD_LOCAL = "shop/plant-food.png";
    public static final String SHOP_ICON_SEED_RANDOM_LOCAL = "shop/seed-random.png";
    public static final String SHOP_ICON_SEED_CHOICE_LOCAL = "shop/seed-choice.png";
    public static final String SHOP_ICON_CURRENCY_LOCAL    = "shop/currency-exchange.png";
    public static final String SHOP_ICON_DAILY_LOCAL       = "shop/shop-daily.png";

    // ─── صفحه‌ی برد/باخت (BK2) — فایل‌های PNG لوکال زیر assets/Exports/ ──────
    /** نوشته‌ی «You Won» — صفحه‌ی برد. */
    public static final String END_WON_TEXT   = "Exports/game over or win page/you_won_text_715x216.png";
    /** نوشته‌ی «You Lost» — صفحه‌ی باخت. */
    public static final String END_LOST_TEXT  = "Exports/game over or win page/you_lost_text_699x208.png";
    /** تصویر مغزِ خورده‌شده روی بشقاب — صفحه‌ی باخت. */
    public static final String END_FAIL_BRAIN = "Exports/game over or win page/fail_screen_brain_only.png";
}
