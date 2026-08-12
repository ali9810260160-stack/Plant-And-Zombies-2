package com.pvz2.graphics.util;

import java.util.HashMap;
import java.util.Map;

/**
 * مسیرهای فایل PAM برای تمام گیاهان و زامبی‌ها.
 *
 * <p>فرمت مسیر: {@code 768/INITIAL/{TYPE}/{NAME}/{NAME}.PAM}
 *
 * <p>کلیپ‌های رایج گیاهان:  {@code idle}, {@code attack}, {@code death}, {@code intro}
 * <p>کلیپ‌های رایج زامبی‌ها: {@code walk}, {@code eating}, {@code die}, {@code attack}
 *
 * <p><b>برای یافتن مسیر دقیق از Asset Browser استفاده کنید:</b>
 * <pre>java -Dpvz.assets="/path/to/assets" -jar pvz-asset-browser.jar</pre>
 *
 * <p>مسیرهای خالی را بعد از جستجو در asset browser پُر کنید.
 */
public final class PamPaths {

    private PamPaths() {}

    private static final String P = "768/INITIAL/PLANT/";   // پیشوند گیاه
    private static final String Z = "768/INITIAL/ZOMBIE/";  // پیشوند زامبی

    // =========================================================================
    //  PLANTS
    // =========================================================================

    private static final Map<String, String> PLANTS = new HashMap<>();

    static {
        // ─── تولیدکننده خورشید ──────────────────────────────────────────────
        reg(PLANTS, "sunflower",
                P + "PLANT_SUNFLOWER/PLANT_SUNFLOWER.PAM");
        reg(PLANTS, "twin_sunflower",
                P + "PLANT_TWINSUNFLOWER/PLANT_TWINSUNFLOWER.PAM");
        reg(PLANTS, "sun_shroom",
                P + "PLANT_SUNSHROOM/PLANT_SUNSHROOM.PAM");
        reg(PLANTS, "power_lily",
                // جستجو کنید: "POWER_LILY" یا "POWERLILY" در asset browser
                "");
        reg(PLANTS, "sun_bean",
                // جستجو کنید: "SUNBEAN" در asset browser
                "");

        // ─── تیرانداز مستقیم (Pea family) ───────────────────────────────────
        reg(PLANTS, "peashooter",
                P + "PLANT_PEASHOOTER/PLANT_PEASHOOTER.PAM");
        reg(PLANTS, "repeater",
                P + "PLANT_REPEATER/PLANT_REPEATER.PAM");
        reg(PLANTS, "gatling_pea",
                P + "PLANT_GATLINGPEA/PLANT_GATLINGPEA.PAM");
        reg(PLANTS, "snow_pea",
                P + "PLANT_SNOWPEA/PLANT_SNOWPEA.PAM");
        reg(PLANTS, "fire_peashooter",
                // جستجو کنید: "FIREPEASHOOTER" در asset browser
                "");
        reg(PLANTS, "pea_pod",
                P + "PLANT_PEAPOD/PLANT_PEAPOD.PAM");

        // ─── تیرانداز — سایر ─────────────────────────────────────────────────
        reg(PLANTS, "cactus",
                P + "PLANT_CACTUS/PLANT_CACTUS.PAM");
        reg(PLANTS, "lightning_reed",
                P + "PLANT_LIGHTNINGREED/PLANT_LIGHTNINGREED.PAM");
        reg(PLANTS, "laser_bean",
                P + "PLANT_LASERBEAN/PLANT_LASERBEAN.PAM");
        reg(PLANTS, "snapdragon",
                // جستجو کنید: "SNAPDRAGON" در asset browser
                "");
        reg(PLANTS, "cold_snapdragon",
                // جستجو کنید: "COLDSNAPDRAGON" در asset browser
                "");
        reg(PLANTS, "citron",
                P + "PLANT_CITRON/PLANT_CITRON.PAM");
        reg(PLANTS, "electric_blueberry",
                // جستجو کنید: "ELECTRICBLUEBERRY" در asset browser
                "");
        reg(PLANTS, "red_stinger",
                // جستجو کنید: "REDSTINGER" در asset browser
                "");
        reg(PLANTS, "homing_thistle",
                // جستجو کنید: "HOMINGTHISTLE" در asset browser
                "");
        reg(PLANTS, "magnifying_grass",
                // جستجو کنید: "MAGNIFYINGGRASS" در asset browser
                "");
        reg(PLANTS, "starfruit",
                P + "PLANT_STARFRUIT/PLANT_STARFRUIT.PAM");

        // ─── لابر (پرتابه هوایی) ─────────────────────────────────────────────
        reg(PLANTS, "cabbage_pult",
                P + "PLANT_CABBAGEPULT/PLANT_CABBAGEPULT.PAM");
        reg(PLANTS, "kernel_pult",
                P + "PLANT_KERNELPULT/PLANT_KERNELPULT.PAM");
        reg(PLANTS, "melon_pult",
                P + "PLANT_MELONPULT/PLANT_MELONPULT.PAM");
        reg(PLANTS, "winter_melon",
                P + "PLANT_WINTERMELON/PLANT_WINTERMELON.PAM");
        reg(PLANTS, "pepper_pult",
                // جستجو کنید: "PEPPERPULT" در asset browser
                "");
        reg(PLANTS, "pecanpult",
                // جستجو کنید: "PECANPULT" در asset browser
                "");

        // ─── دیوار (Wall-Nut family) ─────────────────────────────────────────
        reg(PLANTS, "wallnut",
                P + "PLANT_WALLNUT/PLANT_WALLNUT.PAM");
        reg(PLANTS, "tall_nut",
                P + "PLANT_TALLNUT/PLANT_TALLNUT.PAM");
        reg(PLANTS, "pumpkin",
                P + "PLANT_PUMPKIN/PLANT_PUMPKIN.PAM");
        reg(PLANTS, "infi_nut",
                // جستجو کنید: "INFINUT" در asset browser
                "");

        // ─── انفجاری ────────────────────────────────────────────────────────
        reg(PLANTS, "cherry_bomb",
                P + "PLANT_CHERRYBOMB/PLANT_CHERRYBOMB.PAM");
        reg(PLANTS, "jalapeno",
                P + "PLANT_JALAPENO/PLANT_JALAPENO.PAM");
        reg(PLANTS, "potato_mine",
                P + "PLANT_POTATOMINE/PLANT_POTATOMINE.PAM");
        reg(PLANTS, "primal_potato_mine",
                // جستجو کنید: "PRIMALPOTATOMINE" در asset browser
                "");
        reg(PLANTS, "spore_shroom",
                // جستجو کنید: "SPORESHROOM" در asset browser
                "");

        // ─── مبارز تن‌به‌تن ──────────────────────────────────────────────────
        reg(PLANTS, "chomper",
                P + "PLANT_CHOMPER/PLANT_CHOMPER.PAM");
        reg(PLANTS, "squash",
                P + "PLANT_SQUASH/PLANT_SQUASH.PAM");

        // ─── مدافع/تله ───────────────────────────────────────────────────────
        reg(PLANTS, "spikeweed",
                P + "PLANT_SPIKEWEED/PLANT_SPIKEWEED.PAM");
        reg(PLANTS, "spikerock",
                P + "PLANT_SPIKEROCK/PLANT_SPIKEROCK.PAM");

        // ─── پشتیبان (Modifier) ──────────────────────────────────────────────
        reg(PLANTS, "torchwood",
                P + "PLANT_TORCHWOOD/PLANT_TORCHWOOD.PAM");
        reg(PLANTS, "garlic",
                P + "PLANT_GARLIC/PLANT_GARLIC.PAM");
        reg(PLANTS, "sweet_potato",
                P + "PLANT_SWEETPOTATO/PLANT_SWEETPOTATO.PAM");
        reg(PLANTS, "magnet_shroom",
                P + "PLANT_MAGNETSHROOM/PLANT_MAGNETSHROOM.PAM");
        reg(PLANTS, "hypno_shroom",
                P + "PLANT_HYPNOSHROOM/PLANT_HYPNOSHROOM.PAM");
        reg(PLANTS, "blover",
                P + "PLANT_BLOVER/PLANT_BLOVER.PAM");
        reg(PLANTS, "grave_buster",
                P + "PLANT_GRAVEBUSTER/PLANT_GRAVEBUSTER.PAM");
        reg(PLANTS, "iceberg_lettuce",
                // جستجو کنید: "ICEBERGLETTUCE" در asset browser
                "");
        reg(PLANTS, "umbrella_leaf",
                // جستجو کنید: "UMBRELLALEAF" در asset browser
                "");
        reg(PLANTS, "perfume_shroom",
                // جستجو کنید: "PERFUMESHROOM" در asset browser
                "");

        // ─── آبی (Water) ─────────────────────────────────────────────────────
        reg(PLANTS, "lily_pad",
                P + "PLANT_LILYPAD/PLANT_LILYPAD.PAM");
        reg(PLANTS, "tangle_kelp",
                // جستجو کنید: "TANGLEKELP" در asset browser
                "");
        reg(PLANTS, "guacodile",
                // جستجو کنید: "GUACODILE" در asset browser
                "");

        // ─── قارچ (Shroom) ───────────────────────────────────────────────────
        reg(PLANTS, "puff_shroom",
                P + "PLANT_PUFFSHROOM/PLANT_PUFFSHROOM.PAM");
        reg(PLANTS, "fume_shroom",
                P + "PLANT_FUMESHROOM/PLANT_FUMESHROOM.PAM");
        reg(PLANTS, "scaredy_shroom",
                // جستجو کنید: "SCAREDYSHROOM" در asset browser
                "");

        // ─── نعناع (Mint) ─────────────────────────────────────────────────────
        reg(PLANTS, "pepper_mint",
                // جستجو کنید: "PEPPERMINT" در asset browser
                "");
        reg(PLANTS, "cold_snapdragon_mint",
                // جستجو کنید: "COLDSNAPDRAGONYMINT" در asset browser
                "");
        reg(PLANTS, "electric_mint",
                // جستجو کنید: "ELECTRICMINT" در asset browser
                "");
        reg(PLANTS, "shadow_peashooter",
                // جستجو کنید: "SHADOWPEASHOOTER" در asset browser
                "");
        reg(PLANTS, "sun_mint",
                // جستجو کنید: "SUNMINT" در asset browser
                "");

        // ─── سایر ────────────────────────────────────────────────────────────
        reg(PLANTS, "bowling_bulb",
                // جستجو کنید: "BOWLINGBULB" در asset browser
                "");
        reg(PLANTS, "bamboo_shoot",
                // جستجو کنید: "BAMBOOSHOOT" در asset browser
                "");
        reg(PLANTS, "kiwibeast",
                // جستجو کنید: "KIWIBEAST" در asset browser
                "");
        reg(PLANTS, "marigold",
                P + "PLANT_MARIGOLD/PLANT_MARIGOLD.PAM");
        reg(PLANTS, "aloe",
                // جستجو کنید: "ALOE" در asset browser
                "");
    }

    // =========================================================================
    //  ZOMBIES
    // =========================================================================

    private static final Map<String, String> ZOMBIES = new HashMap<>();

    static {
        // ─── عمومی ───────────────────────────────────────────────────────────
        reg(ZOMBIES, "basic",          Z + "ZOMBIE_BASIC/ZOMBIE_BASIC.PAM");
        reg(ZOMBIES, "normal",         Z + "ZOMBIE_BASIC/ZOMBIE_BASIC.PAM");
        reg(ZOMBIES, "conehead",       Z + "ZOMBIE_CONEHEAD/ZOMBIE_CONEHEAD.PAM");
        reg(ZOMBIES, "buckethead",     Z + "ZOMBIE_BUCKETHEAD/ZOMBIE_BUCKETHEAD.PAM");
        reg(ZOMBIES, "knight",         Z + "ZOMBIE_KNIGHT/ZOMBIE_KNIGHT.PAM");
        reg(ZOMBIES, "blockhead",
                // جستجو کنید: "BLOCKHEAD" یا "ZOMBIE_BLOCK" در asset browser
                "");
        reg(ZOMBIES, "gargantuar",     Z + "ZOMBIE_GARGANTUAR/ZOMBIE_GARGANTUAR.PAM");
        reg(ZOMBIES, "imp",            Z + "ZOMBIE_IMP/ZOMBIE_IMP.PAM");
        reg(ZOMBIES, "all_star",       Z + "ZOMBIE_ALLSTAR/ZOMBIE_ALLSTAR.PAM");
        reg(ZOMBIES, "arcade",
                // جستجو کنید: "ARCADE" در asset browser
                "");
        reg(ZOMBIES, "parasol",        Z + "ZOMBIE_PARASOL/ZOMBIE_PARASOL.PAM");
        reg(ZOMBIES, "turquoise",
                // جستجو کنید: "TURQUOISE" در asset browser
                "");
        reg(ZOMBIES, "prospector",
                // جستجو کنید: "PROSPECTOR" در asset browser
                "");
        reg(ZOMBIES, "pianist",
                // جستجو کنید: "PIANIST" در asset browser
                "");
        reg(ZOMBIES, "newspaper",      Z + "ZOMBIE_NEWSPAPER/ZOMBIE_NEWSPAPER.PAM");
        reg(ZOMBIES, "barrel_roller",
                // جستجو کنید: "BARRELROLLER" در asset browser
                "");

        // ─── مصر باستان ──────────────────────────────────────────────────────
        reg(ZOMBIES, "ra",
                Z + "ZOMBIE_EGYPT_RA/ZOMBIE_EGYPT_RA.PAM");
        reg(ZOMBIES, "ra_zombie",
                Z + "ZOMBIE_EGYPT_RA/ZOMBIE_EGYPT_RA.PAM");
        reg(ZOMBIES, "explorer",
                Z + "ZOMBIE_EGYPT_EXPLORER/ZOMBIE_EGYPT_EXPLORER.PAM");
        reg(ZOMBIES, "tomb_raiser",
                Z + "ZOMBIE_EGYPT_TOMBSTONE/ZOMBIE_EGYPT_TOMBSTONE.PAM");
        reg(ZOMBIES, "egypt_basic",
                Z + "ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM");

        // ─── غار یخی ─────────────────────────────────────────────────────────
        reg(ZOMBIES, "dodo_rider",
                // جستجو کنید: "DODO" در asset browser
                "");
        reg(ZOMBIES, "hunter",
                // جستجو کنید: "HUNTER" در asset browser
                "");
        reg(ZOMBIES, "troglobite",
                // جستجو کنید: "TROGLOBITE" در asset browser
                "");

        // ─── ساحل ────────────────────────────────────────────────────────────
        reg(ZOMBIES, "fisherman",
                Z + "ZOMBIE_BEACH_FISHERMAN/ZOMBIE_BEACH_FISHERMAN.PAM");
        reg(ZOMBIES, "snorkel",
                Z + "ZOMBIE_BEACH_SNORKEL/ZOMBIE_BEACH_SNORKEL.PAM");
        reg(ZOMBIES, "octopus",
                // جستجو کنید: "OCTOPUS" در asset browser
                "");

        // ─── قرون وسطی ───────────────────────────────────────────────────────
        reg(ZOMBIES, "jester",
                Z + "ZOMBIE_DARK_JESTER/ZOMBIE_DARK_JESTER.PAM");
        reg(ZOMBIES, "wizard",
                Z + "ZOMBIE_DARK_WIZARD/ZOMBIE_DARK_WIZARD.PAM");
        reg(ZOMBIES, "king",
                Z + "ZOMBIE_DARK_KING/ZOMBIE_DARK_KING.PAM");
        reg(ZOMBIES, "dragon_imp",
                // جستجو کنید: "DRAGON_IMP" در asset browser
                "");

        // ─── Zombotany (مینی‌گیم) ─────────────────────────────────────────────
        reg(ZOMBIES, "zombotany_peashooter",   "");
        reg(ZOMBIES, "zombotany_wallnut",       "");
        reg(ZOMBIES, "zombotany_jalapeno",      "");
        reg(ZOMBIES, "zombotany_squash",        "");

        // ─── IZombie (sun producer zombie) ───────────────────────────────────
        reg(ZOMBIES, "sun_producer_zombie",
                // از انیمیشن زامبی عادی به عنوان fallback استفاده کنید
                Z + "ZOMBIE_BASIC/ZOMBIE_BASIC.PAM");
    }

    private static void reg(Map<String, String> map, String key, String path) {
        map.put(key, path);
    }

    /** مسیر PAM گیاه — "" اگر باید از asset browser پیدا شود */
    public static String forPlant(String type) {
        if (type == null) return "";
        return PLANTS.getOrDefault(type.toLowerCase(), "");
    }

    /** مسیر PAM زامبی — "" اگر باید از asset browser پیدا شود */
    public static String forZombie(String type) {
        if (type == null) return "";
        return ZOMBIES.getOrDefault(type.toLowerCase(), "");
    }

    public static boolean hasPlantPath(String type) {
        String p = forPlant(type);
        return p != null && !p.isEmpty();
    }

    public static boolean hasZombiePath(String type) {
        String p = forZombie(type);
        return p != null && !p.isEmpty();
    }
}
