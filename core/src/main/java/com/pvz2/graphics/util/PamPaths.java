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
                P + "SUNFLOWER/SUNFLOWER.PAM");
        reg(PLANTS, "twin_sunflower",
                P + "TWINSUNFLOWER/TWINSUNFLOWER.PAM");
        reg(PLANTS, "sun_shroom",
                P + "SUNSHROOM/SUNSHROOM.PAM");
        reg(PLANTS, "power_lily",
                // جستجو کنید: "POWER_LILY" یا "POWERLILY" در asset browser
                P + "POWERLILY/POWERLILY.PAM");
        reg(PLANTS, "sun_bean",
                // جستجو کنید: "SUNBEAN" در asset browser
                P + "SUNBEAN/SUNBEAN.PAM");

        // ─── تیرانداز مستقیم (Pea family) ───────────────────────────────────
        reg(PLANTS, "peashooter",
                P + "PEASHOOTER/PEASHOOTER.PAM");
        reg(PLANTS, "repeater",
                P + "REPEATER/REPEATER.PAM");
        reg(PLANTS, "gatling_pea",
                P + "GATLINGPEA/GATLINGPEA.PAM");
        reg(PLANTS, "snow_pea",
                P + "SNOWPEA/SNOWPEA.PAM");
        reg(PLANTS, "fire_peashooter",
                // جستجو کنید: "FIREPEASHOOTER" در asset browser
                P +  "FIREPEATER/FIREPEATER.PAM");
        reg(PLANTS, "pea_pod",
                P + "PEAPOD/PEAPOD.PAM");

        // ─── تیرانداز — سایر ─────────────────────────────────────────────────
        reg(PLANTS, "cactus",
                P + "CACTUS/CACTUS.PAM");
        reg(PLANTS, "lightning_reed",
                P + "LIGHTNINGREED/LIGHTNINGREED.PAM");
        reg(PLANTS, "laser_bean",
                P + "LASERBEAN/LASERBEAN.PAM");
        reg(PLANTS, "snapdragon",
                // جستجو کنید: "SNAPDRAGON" در asset browser
                P + "SNAPDRAGON/SNAPDRAGON.PAM");
        reg(PLANTS, "cold_snapdragon",
                // جستجو کنید: "COLDSNAPDRAGON" در asset browser
                P +  "COLDSNAPDRAGON/COLDSNAPDRAGON.PAM");
        reg(PLANTS, "citron",
                P + "CITRON/CITRON.PAM");
        reg(PLANTS, "electric_blueberry",
                // جستجو کنید: "ELECTRICBLUEBERRY" در asset browser
                P + "ELECTRICBLUEBERRY/ELECTRICBLUEBERRY.PAM");
        reg(PLANTS, "red_stinger",
                // جستجو کنید: "REDSTINGER" در asset browser
                P + "REDSTINGER/REDSTINGER.PAM");
        reg(PLANTS, "homing_thistle",
                // جستجو کنید: "HOMINGTHISTLE" در asset browser
                P + "HOMINGTHISTLE/HOMINGTHISTLE.PAM");
        reg(PLANTS, "magnifying_grass",
                // جستجو کنید: "MAGNIFYINGGRASS" در asset browser
                P + "MAGNIFYING/MAGNIFYING.PAM");
        reg(PLANTS, "starfruit",
                P + "STARFRUIT/STARFRUIT.PAM");

        // ─── لابر (پرتابه هوایی) ─────────────────────────────────────────────
        reg(PLANTS, "cabbage_pult",
                P + "CABBAGEPULT/CABBAGEPULT.PAM");
        reg(PLANTS, "kernel_pult",
                P + "KERNELPULT/KERNELPULT.PAM");
        reg(PLANTS, "melon_pult",
                P + "MELONPULT/MELONPULT.PAM");
        reg(PLANTS, "winter_melon",
                P + "WINTERMELON/WINTERMELON.PAM");
        reg(PLANTS, "pepper_pult",
                // جستجو کنید: "PEPPERPULT" در asset browser
                P +  "PEPPERPULT/PEPPERPULT.PAM");
        reg(PLANTS, "pecanpult",
                // جستجو کنید: "PECANPULT" در asset browser
                P + "PECANPULT/PECANPULT.PAM");

        // ─── دیوار (Wall-Nut family) ─────────────────────────────────────────
        reg(PLANTS, "wallnut",
                P + "WALLNUT/WALLNUT.PAM");
        reg(PLANTS, "tall_nut",
                P + "TALLNUT/TALLNUT.PAM");
        reg(PLANTS, "pumpkin",
                P + "PUMPKIN/PUMPKIN.PAM");
        reg(PLANTS, "infi_nut",
                // جستجو کنید: "INFINUT" در asset browser
                P + "INFINUT/INFINUT.PAM");

        // ─── انفجاری ────────────────────────────────────────────────────────
        reg(PLANTS, "cherry_bomb",
                P + "CHERRYBOMB/CHERRYBOMB.PAM");
        reg(PLANTS, "jalapeno",
                P + "JALAPENO/JALAPENO.PAM");
        reg(PLANTS, "potato_mine",
                P + "POTATOMINE/POTATOMINE.PAM");
        reg(PLANTS, "primal_potato_mine",
                // جستجو کنید: "PRIMALPOTATOMINE" در asset browser
                P + "PRIMAL_POTATOMINE/PRIMAL_POTATOMINE.PAM");
        reg(PLANTS, "spore_shroom",
                // جستجو کنید: "SPORESHROOM" در asset browser
                P + "SPORE/SPORE.PAM");

        // ─── مبارز تن‌به‌تن ──────────────────────────────────────────────────
        reg(PLANTS, "chomper",
                P + "CHOMPER/CHOMPER.PAM");
        reg(PLANTS, "squash",
                P + "SQUASH/SQUASH.PAM");

        // ─── مدافع/تله ───────────────────────────────────────────────────────
        reg(PLANTS, "spikeweed",
                P + "SPIKEWEED/SPIKEWEED.PAM");
        reg(PLANTS, "spikerock",
                P + "SPIKEROCK/SPIKEROCK.PAM");

        // ─── پشتیبان (Modifier) ──────────────────────────────────────────────
        reg(PLANTS, "torchwood",
                P + "TORCHWOOD/TORCHWOOD.PAM");
        reg(PLANTS, "garlic",
                P + "GARLIC/GARLIC.PAM");
        reg(PLANTS, "sweet_potato",
                P + "SWEETPOTATO/SWEETPOTATO.PAM");
        reg(PLANTS, "magnet_shroom",
                P + "MAGNETSHROOM/MAGNETSHROOM.PAM");
        reg(PLANTS, "hypno_shroom",
                P + "HYPNOSHROOM/HYPNOSHROOM.PAM");
        reg(PLANTS, "blover",
                P + "BLOVER/BLOVER.PAM");
        reg(PLANTS, "grave_buster",
                P + "GRAVEBUSTER/GRAVEBUSTER.PAM");
        reg(PLANTS, "iceberg_lettuce",
                // جستجو کنید: "ICEBERGLETTUCE" در asset browser
                "");
        reg(PLANTS, "umbrella_leaf",
                // جستجو کنید: "UMBRELLALEAF" در asset browser
                P + "");
        reg(PLANTS, "perfume_shroom",
                // جستجو کنید: "PERFUMESHROOM" در asset browser
                P + "PERFSHROOM/PERFSHROOM.PAM");

        // ─── آبی (Water) ─────────────────────────────────────────────────────
        reg(PLANTS, "lily_pad",
                P + "LILYPAD/LILYPAD.PAM");
        reg(PLANTS, "tangle_kelp",
                // جستجو کنید: "TANGLEKELP" در asset browser
                P + "TANGLEKELP/TANGLEKELP.PAM");
        reg(PLANTS, "guacodile",
                // جستجو کنید: "GUACODILE" در asset browser
                P + "GUACODILE/GUACODILE.PAM");

        // ─── قارچ (Shroom) ───────────────────────────────────────────────────
        reg(PLANTS, "puff_shroom",
                P + "PUFFSHROOM/PUFFSHROOM.PAM");
        reg(PLANTS, "fume_shroom",
                P + "FUMESHROOM/FUMESHROOM.PAM");
        reg(PLANTS, "scaredy_shroom",
                // جستجو کنید: "SCAREDYSHROOM" در asset browser
                P + "SCAREDYSHROOM/SCAREDYSHROOM.PAM");

        // ─── نعناع (Mint) ─────────────────────────────────────────────────────
        reg(PLANTS, "pepper_mint",
                // جستجو کنید: "PEPPERMINT" در asset browser
                P + "PEPPERMINT/PEPPERMINT.PAM");
        reg(PLANTS, "cold_snapdragon_mint",
                // جستجو کنید: "COLDSNAPDRAGONYMINT" در asset browser
                P + "");
        reg(PLANTS, "electric_mint",
                // جستجو کنید: "ELECTRICMINT" در asset browser
                P + "");
        reg(PLANTS, "shadow_peashooter",
                // جستجو کنید: "SHADOWPEASHOOTER" در asset browser
                P + "SHADOWPEASHOOTER/SHADOWPEASHOOTER.PAM");
        reg(PLANTS, "sun_mint",
                // جستجو کنید: "SUNMINT" در asset browser
                "");

        // ─── سایر ────────────────────────────────────────────────────────────
        reg(PLANTS, "bowling_bulb",
                // جستجو کنید: "BOWLINGBULB" در asset browser
                P + "BOWLINGBULB/BOWLINGBULB.PAM");
        reg(PLANTS, "bamboo_shoot",
                // جستجو کنید: "BAMBOOSHOOT" در asset browser
                P + "BAMBOOSPARTAN/BAMBOOSPARTAN.PAM");
        reg(PLANTS, "kiwibeast",
                // جستجو کنید: "KIWIBEAST" در asset browser
                P + "KIWIBEAST/KIWIBEAST.PAM");
        reg(PLANTS, "marigold",
                P + "MARIGOLD/MARIGOLD.PAM");
        reg(PLANTS, "aloe",
                // جستجو کنید: "ALOE" در asset browser
                P + "ALOE/ALOE.PAM");
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
                Z + "ZOMBIE_80S_ARCADE/ZOMBIE_80S_ARCADE.PAM");
        reg(ZOMBIES, "parasol",        Z + "ZOMBIE_PARASOL/ZOMBIE_PARASOL.PAM");
        reg(ZOMBIES, "turquoise",
                // جستجو کنید: "TURQUOISE" در asset browser
                Z + "");
        reg(ZOMBIES, "prospector",
                // جستجو کنید: "PROSPECTOR" در asset browser
                Z + "ZOMBIE_PROSPECTOR/ZOMBIE_PROSPECTOR.PAM");
        reg(ZOMBIES, "pianist",
                // جستجو کنید: "PIANIST" در asset browser
                "");
        reg(ZOMBIES, "newspaper",      Z + "ZOMBIE_NEWSPAPER/ZOMBIE_NEWSPAPER.PAM");
        reg(ZOMBIES, "barrel_roller",
                // جستجو کنید: "BARRELROLLER" در asset browser
                Z + "ZOMBIE_PIANO/ZOMBIE_PIANO.PAM");

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
                Z + "ZOMBIE_ICEAGE_DODORIDER/ZOMBIE_ICEAGE_DODORIDER.PAM");
        reg(ZOMBIES, "hunter",
                // جستجو کنید: "HUNTER" در asset browser
                Z + "ZOMBIE_ICEAGE_HUNTER/ZOMBIE_ICEAGE_HUNTER.PAM");
        reg(ZOMBIES, "troglobite",
                // جستجو کنید: "TROGLOBITE" در asset browser
                Z + "ZOMBIE_DINO_TROGLOBITE/ZOMBIE_DINO_TROGLOBITE.PAM");

        // ─── ساحل ────────────────────────────────────────────────────────────
        reg(ZOMBIES, "fisherman",
                Z + "ZOMBIE_BEACH_FISHERMAN/ZOMBIE_BEACH_FISHERMAN.PAM");
        reg(ZOMBIES, "snorkel",
                Z + "ZOMBIE_BEACH_SNORKEL/ZOMBIE_BEACH_SNORKEL.PAM");
        reg(ZOMBIES, "octopus",
                // جستجو کنید: "OCTOPUS" در asset browser
                Z + "ZOMBIE_BEACH_OCTOPUS/ZOMBIE_BEACH_OCTOPUS.PAM");

        // ─── قرون وسطی ───────────────────────────────────────────────────────
        reg(ZOMBIES, "jester",
                Z + "ZOMBIE_DARK_JESTER/ZOMBIE_DARK_JESTER.PAM");
        reg(ZOMBIES, "wizard",
                Z + "ZOMBIE_DARK_WIZARD/ZOMBIE_DARK_WIZARD.PAM");
        reg(ZOMBIES, "king",
                Z + "ZOMBIE_DARK_KING/ZOMBIE_DARK_KING.PAM");
        reg(ZOMBIES, "dragon_imp",
                // جستجو کنید: "DRAGON_IMP" در asset browser
                Z + "ZOMBIE_DARK_IMP_DRAGON/ZOMBIE_DARK_IMP_DRAGON.PAM");

        // ─── Zombotany (مینی‌گیم) ─────────────────────────────────────────────
        reg(ZOMBIES, "zombotany_peashooter",   Z + "PEASHOOTER/PEASHOOTER.PAM");
        reg(ZOMBIES, "zombotany_wallnut",       Z + "WALLNUT/WALLNUT.PAM");
        reg(ZOMBIES, "zombotany_jalapeno",      Z + "JALAPENO/JALAPENO.PAM");
        reg(ZOMBIES, "zombotany_squash",        Z + "SQUASH/SQUASH.PAM");

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
