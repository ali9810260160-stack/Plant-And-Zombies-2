package com.pvz2.graphics.assets;

import com.pvz2.model.enums.PlantType;
import com.pvz2.model.enums.ZombieType;

import java.util.HashMap;
import java.util.Map;

/**
 * مسیر فایل‌های PNG خام صفحه‌ی کلکسیون (Almanac) — گیاهان، زامبی‌ها و عناصر UI.
 *
 * <p>این فایل‌ها بخشی از asset pack اصلی PvZ2 (که با {@link GameAssets#region(String)}
 * و {@code TextureBank} خونده می‌شن) نیستن؛ عکس‌های تکی export‌شده هستن که باید
 * داخل پوشه‌ی {@code assets/} پروژه‌ی libGDX (ماژول {@code android/assets} یا
 * {@code lwjgl3/assets} — هرکدوم که به عنوان classpath اصلی resources ست شده)
 * زیر مسیر زیر قرار بگیرن:
 *
 * <pre>
 *   assets/collection/plants/&lt;file&gt;.png
 *   assets/collection/zombies/&lt;file&gt;.png
 *   assets/collection/ui/&lt;file&gt;.png
 * </pre>
 *
 * <p>بارگذاری واقعی با {@link CollectionAssets} انجام می‌شه (نه {@code GameAssets})
 * چون این‌ها Texture مستقل هستن، نه region داخل اطلس اصلی بازی.
 *
 * <p><b>یادداشت روی نگاشت‌ها:</b> اسم فایل‌های گیاه تقریباً همیشه دقیقاً با
 * {@code PlantType.name()} (بدون آندرلاین، lower-case) مطابقت داره چون از
 * asset‌های واقعی بازی گرفته شدن. اسم فایل‌های زامبی بر اساس فصل (chapter) نام‌گذاری
 * شدن (پیشوند {@code tutorial_} = مصر باستان/عمومی، {@code mummy_} = ری‌اسکین
 * اختصاصی مصر، {@code iceage_} = غارهای یخی، {@code beach_} = ساحل مواج،
 * {@code dark_} = قرون وسطی) — این پروژه فقط ۴ چپتر پیاده‌سازی کرده
 * ({@link com.pvz2.model.enums.ChapterType}), بنابراین نگاشت زامبی‌ها بر همون
 * مبنا انجام شده. مواردی که با علامت «TODO» مشخص شدن، فایل قطعی نداشتن و باید
 * دستی با تصاویر واقعی‌شون جایگزین بشن (تا اون‌موقع placeholder خاکستری/بدون آیکون
 * نشون داده می‌شه — کرش نمی‌کنه).
 */
public final class CollectionAssetPaths {

    private CollectionAssetPaths() {}

    // ─── ریشه‌ی پوشه‌ها ───────────────────────────────────────────────────────
    public static final String ROOT          = "collection/";
    public static final String PLANTS_DIR    = ROOT + "plants/";
    public static final String ZOMBIES_DIR   = ROOT + "zombies/";
    public static final String UI_DIR        = ROOT + "ui/";

    // ─── تب‌ها (بالای صفحه) ───────────────────────────────────────────────────
    public static final String TAB_PLANTS_DOWN    = UI_DIR + "plants_down.png";   // غیرفعال
    public static final String TAB_PLANTS_ACTIVE  = UI_DIR + "plants_active.png"; // انتخاب‌شده
    public static final String TAB_ZOMBIES_DOWN   = UI_DIR + "zombies_down.png";
    public static final String TAB_ZOMBIES_ACTIVE = UI_DIR + "zombies_active.png";

    // ─── قاب‌ها و overlay ها ─────────────────────────────────────────────────
    /** قاب تیره‌ی «دیده‌شده» برای کارت‌های زامبی. */
    public static final String ZOMBIE_READY_FRAME = UI_DIR + "ready.png";
    /** پس‌زمینه‌ی طلایی برای گیاهان boost‌شده. */
    public static final String PLANT_BOOST_BG     = UI_DIR + "boost.png";
    /** آیکون قفل کوچک روی کارت‌های آنلاک‌نشده. */
    public static final String ICON_LOCK_SMALL    = UI_DIR + "lock_small.png";

    // ─── ناوبری / دکمه‌ها ────────────────────────────────────────────────────
    public static final String BUTTON_BACK      = UI_DIR + "buttons_hud_back_normal.png"; // برگشت (صفحه detail)
    public static final String BUTTON_CLOSE_TAB = UI_DIR + "close_tab.png";               // بستن (X) صفحه گرید
    public static final String ARROW_NEXT       = UI_DIR + "stats_screen_nav_arrow_next.png";
    public static final String ARROW_PREV       = UI_DIR + "stats_screen_nav_arrow_previous.png";

    // ─── آیکون‌های آمار (صفحه‌ی جزئیات) ──────────────────────────────────────
    public static final String STAT_ICON_ARMING_TIME     = UI_DIR + "almanac_stat_icon_armingtime.png";
    public static final String STAT_ICON_FAMILY          = UI_DIR + "almanac_stat_icon_family.png";
    public static final String STAT_ICON_PLANT_FOOD      = UI_DIR + "almanac_stat_icon_plantfood.png";
    public static final String STAT_ICON_SPECIAL         = UI_DIR + "almanac_stat_icon_special.png";
    public static final String STAT_ICON_SUN_COST        = UI_DIR + "almanac_stat_icon_suncost.png";
    public static final String STAT_ICON_SUN_PRODUCTION  = UI_DIR + "almanac_stat_icon_sunproduction.png";

    // =========================================================================
    //  PLANTS  —  PlantType → نام فایل زیر collection/plants/
    // =========================================================================

    private static final Map<PlantType, String> PLANTS = new HashMap<>();

    static {
        reg(PLANTS, PlantType.SUNFLOWER,            "sunflower.png");
        reg(PLANTS, PlantType.TWIN_SUNFLOWER,        "twinsunflower.png");
        reg(PLANTS, PlantType.SUN_SHROOM,            "sunshroom.png");
        reg(PLANTS, PlantType.PRIMAL_SUNFLOWER,      "primalsunflower.png");
        reg(PLANTS, PlantType.GOLD_BLOOM,            "goldbloom.png");

        reg(PLANTS, PlantType.PEASHOOTER,            "peashooter.png");
        reg(PLANTS, PlantType.REPEATER,              "repeater.png");
        reg(PLANTS, PlantType.THREEPEATER,           "threepeater.png");
        reg(PLANTS, PlantType.SNOW_PEA,              "snowpea.png");
        reg(PLANTS, PlantType.ROTOBAGA,              ""); // TODO: فایل «rotobaga» در پک موجود نبود
        reg(PLANTS, PlantType.PEA_POD,               "peapod.png");
        reg(PLANTS, PlantType.SPLIT_PEA,             "splitpea.png");
        reg(PLANTS, PlantType.CITRON,                "citron.png");
        reg(PLANTS, PlantType.CAULIPOWER,            "caulipower.png");
        reg(PLANTS, PlantType.ELECTRIC_BLUEBERRY,    "electricblueberry.png");
        reg(PLANTS, PlantType.BOWLING_BULB,          "bowlingbulb.png");
        reg(PLANTS, PlantType.CACTUS,                "cactus.png");
        reg(PLANTS, PlantType.FIRE_PEASHOOTER,       "firepeashooter.png");
        reg(PLANTS, PlantType.STARFRUIT,             "starfruit.png");
        reg(PLANTS, PlantType.GOO_PEASHOOTER,        ""); // TODO: فایل معادل موجود نبود
        reg(PLANTS, PlantType.MEGA_GATLING_PEA,      "megagatling.png");

        reg(PLANTS, PlantType.SEA_SHROOM,            "seashroom.png");
        reg(PLANTS, PlantType.PUFF_SHROOM,           "puffshroom.png");
        reg(PLANTS, PlantType.FUME_SHROOM,           "fumeshroom.png");

        reg(PLANTS, PlantType.CABBAGE_PULT,          "cabbagepult.png");
        reg(PLANTS, PlantType.KERNEL_PULT,           "kernelpult.png");
        reg(PLANTS, PlantType.MELON_PULT,            "Melonpult.png");
        reg(PLANTS, PlantType.WINTER_MELON,          "wintermelon.png");
        reg(PLANTS, PlantType.PEPPER_PULT,           "pepperpult.png");

        reg(PLANTS, PlantType.POTATO_MINE,           "potatomine.png");
        reg(PLANTS, PlantType.PRIMAL_POTATO_MINE,    "primalpotatomine.png");
        reg(PLANTS, PlantType.CHERRY_BOMB,           "cherry_bomb.png");
        reg(PLANTS, PlantType.SQUASH,                "squash.png");
        reg(PLANTS, PlantType.GRAPESHOT,             "grapeshot.png");
        reg(PLANTS, PlantType.JALAPENO,              "jalapeno.png");
        reg(PLANTS, PlantType.DOOM_SHROOM,           "doomshroom.png");
        reg(PLANTS, PlantType.TANGLE_KELP,           "tanglekelp.png");
        reg(PLANTS, PlantType.ICEBERG_LETTUCE,       "iceburg.png"); // نام فایل با غلط املایی

        reg(PLANTS, PlantType.BONK_CHOY,             "bonkchoy.png");
        reg(PLANTS, PlantType.PHAT_BEET,             "phatbeet.png");
        reg(PLANTS, PlantType.CHOMPER,                "chomper.png");
        reg(PLANTS, PlantType.WASABI_WHIP,           "wasabiwhip.png");
        reg(PLANTS, PlantType.KIWIBEAST,             "kiwibeast.png");

        reg(PLANTS, PlantType.WALL_NUT,              "wallnut.png");
        reg(PLANTS, PlantType.TALL_NUT,              "tallnut.png");
        reg(PLANTS, PlantType.ENDURIAN,              "endurian.png");
        reg(PLANTS, PlantType.GARLIC,                "garlic.png");
        reg(PLANTS, PlantType.SWEET_POTATO,          "sweetpotato.png");
        reg(PLANTS, PlantType.EXPLODE_O_NUT,         "explodeonut.png");
        reg(PLANTS, PlantType.PUMPKIN,               "pumpkin.png");

        reg(PLANTS, PlantType.SUN_BEAN,              "sunbean.png");
        reg(PLANTS, PlantType.TORCHWOOD,             "torchwood.png");
        reg(PLANTS, PlantType.MAGNET_SHROOM,         "magnetshroom.png");
        reg(PLANTS, PlantType.HYPNO_SHROOM,          "hypnoshroom.png");
        reg(PLANTS, PlantType.CAT_TAIL,              ""); // TODO: فقط نسخه‌ی مینتش (cattail_mint) در enum هست، فایل مستقل نبود
        reg(PLANTS, PlantType.IMITATER,              "imitater.png");
        reg(PLANTS, PlantType.ICE_SHROOM,            "iceshroom.png");
        reg(PLANTS, PlantType.LILY_PAD,              "lilypad.png");
        reg(PLANTS, PlantType.HOT_POTATO,            "hotpotato.png");
        reg(PLANTS, PlantType.GRAVE_BUSTER,          "gravebuster.png");

        reg(PLANTS, PlantType.ENLIGHTEN_MINT,        "enlightenmint.png");
        reg(PLANTS, PlantType.APPEASE_MINT,          "appeasemint.png");
        reg(PLANTS, PlantType.ARMA_MINT,             "armamint.png");
        reg(PLANTS, PlantType.BOMBARD_MINT,          "bombardmint.png");
        reg(PLANTS, PlantType.ENFORCE_MINT,          "enforcemint.png");
        reg(PLANTS, PlantType.REINFORCE_MINT,        "reinforcemint.png");
        reg(PLANTS, PlantType.ENCHANT_MINT,          "enchantmint.png");
        reg(PLANTS, PlantType.PIERCE_MINT,           ""); // TODO: فایل معادل موجود نبود
        reg(PLANTS, PlantType.CATTAIL_MINT,          ""); // TODO: فایل معادل موجود نبود

        // Minigame-only — بدون asset اختصاصی، از ظاهر پایه استفاده می‌شه
        reg(PLANTS, PlantType.WALLNUT_BOWLING,        "wallnut.png");
        reg(PLANTS, PlantType.EXPLODE_O_NUT_BOWLING,  "explodeonut.png");
        reg(PLANTS, PlantType.BIG_WALLNUT,            "wallnut.png");
    }

    // =========================================================================
    //  ZOMBIES  —  ZombieType → نام فایل زیر collection/zombies/
    // =========================================================================

    private static final Map<ZombieType, String> ZOMBIES = new HashMap<>();

    static {
        // ─── مشترک بین همه چپترها (از ری‌اسکین «tutorial» = پایه/عمومی) ────────
        reg(ZOMBIES, ZombieType.NORMAL,            "tutorial.png");
        reg(ZOMBIES, ZombieType.CONEHEAD,          "tutorial_armor1.png");
        reg(ZOMBIES, ZombieType.BUCKETHEAD,        "tutorial_armor2.png");
        // KNIGHT فقط در Dark Ages مدل armor3 داره — از همون به‌عنوان نماینده‌ی مشترک استفاده شد
        reg(ZOMBIES, ZombieType.KNIGHT,            "dark_armor3.png");
        reg(ZOMBIES, ZombieType.BLOCKHEAD,         "tutorial_armor4.png");
        reg(ZOMBIES, ZombieType.GARGANTUAR,        "tutorial_gargantuar.png");
        reg(ZOMBIES, ZombieType.IMP,               "tutorial_imp.png");
        reg(ZOMBIES, ZombieType.ALL_STAR,          "modern_allstar.png");
        reg(ZOMBIES, ZombieType.ARCADE_ZOMBIE,     "eighties_arcade.png");
        // TODO: تصویر دقیق Parasol Zombie در پک نبود — بهترین تطبیق موقت از ست ساحل
        reg(ZOMBIES, ZombieType.PARASOL_ZOMBIE,    "beach_surfer.png");
        reg(ZOMBIES, ZombieType.TURQUOISE_ZOMBIE,  "lostcity_crystalskull.png");
        reg(ZOMBIES, ZombieType.PROSPECTOR_ZOMBIE, "prospector.png");
        reg(ZOMBIES, ZombieType.PIANIST_ZOMBIE,    "piano.png");
        reg(ZOMBIES, ZombieType.NEWSPAPER_ZOMBIE,  "modern_newspaper.png");
        reg(ZOMBIES, ZombieType.BARREL_ROLLER,     "barrelroller.png");

        // ─── مصر باستان ──────────────────────────────────────────────────────
        reg(ZOMBIES, ZombieType.RA_ZOMBIE,         "ra.png");
        reg(ZOMBIES, ZombieType.EXPLORER_ZOMBIE,   "explorer.png");
        reg(ZOMBIES, ZombieType.TOMB_RAISER,       "tomb_raiser.png");

        // ─── غارهای یخی ──────────────────────────────────────────────────────
        reg(ZOMBIES, ZombieType.DODO_RIDER,        "iceage_dodo.png");
        reg(ZOMBIES, ZombieType.HUNTER_ZOMBIE,     "iceage_hunter.png");
        reg(ZOMBIES, ZombieType.TROGLOBITE,        "iceage_troglobite.png");

        // ─── ساحل ────────────────────────────────────────────────────────────
        reg(ZOMBIES, ZombieType.FISHERMAN_ZOMBIE,  "beach_fisherman.png");
        reg(ZOMBIES, ZombieType.SNORKEL_ZOMBIE,    "beach_snorkel.png");
        reg(ZOMBIES, ZombieType.OCTOPUS_ZOMBIE,    "beach_octopus.png");

        // ─── قرون وسطی ───────────────────────────────────────────────────────
        // TODO: «juggler» نزدیک‌ترین معادل بصری به jester بود؛ اگر فایل اختصاصی jester پیدا شد جایگزین شود
        reg(ZOMBIES, ZombieType.JESTER_ZOMBIE,     "dark_juggler.png");
        reg(ZOMBIES, ZombieType.WIZARD_ZOMBIE,     "dark_wizard.png");
        reg(ZOMBIES, ZombieType.KING_ZOMBIE,       "dark_king.png");
        reg(ZOMBIES, ZombieType.DRAGON_IMP,        "dark_imp_dragon.png");

        // ─── مینی‌گیم‌ها — بدون asset اختصاصی در این پک ─────────────────────────
        reg(ZOMBIES, ZombieType.ZOMBOTANY_PEASHOOTER, "tutorial.png");
        reg(ZOMBIES, ZombieType.ZOMBOTANY_WALLNUT,    "tutorial.png");
        reg(ZOMBIES, ZombieType.ZOMBOTANY_JALAPENO,   "tutorial.png");
        reg(ZOMBIES, ZombieType.ZOMBOTANY_SQUASH,     "tutorial.png");
        reg(ZOMBIES, ZombieType.SUN_PRODUCER_ZOMBIE,  "tutorial.png");
    }

    private static void reg(Map<PlantType, String> map, PlantType type, String file) {
        map.put(type, file);
    }
    private static void reg(Map<ZombieType, String> map, ZombieType type, String file) {
        map.put(type, file);
    }

    /** مسیر کامل داخلی (internal) عکس یک گیاه — "" اگر مپ نشده باشه. */
    public static String plantPath(PlantType type) {
        String f = PLANTS.get(type);
        return (f == null || f.isEmpty()) ? "" : PLANTS_DIR + f;
    }

    /** مسیر کامل داخلی (internal) عکس یک زامبی — "" اگر مپ نشده باشه. */
    public static String zombiePath(ZombieType type) {
        String f = ZOMBIES.get(type);
        return (f == null || f.isEmpty()) ? "" : ZOMBIES_DIR + f;
    }

    public static boolean hasPlantImage(PlantType type)   { return !plantPath(type).isEmpty(); }
    public static boolean hasZombieImage(ZombieType type) { return !zombiePath(type).isEmpty(); }
}
