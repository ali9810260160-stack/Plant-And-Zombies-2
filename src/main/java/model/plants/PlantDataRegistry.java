package model.plants;

import model.enums.PlantFamily;
import model.enums.PlantTag;
import model.enums.PlantType;

import java.util.EnumMap;
import java.util.Map;

/**
 * رجیستری مرکزی آمار گیاهان.
 * داده‌ها از JSON بارگذاری می‌شوند.
 * این کلاس یک Singleton است و در شروع برنامه پر می‌شود.
 */
public class PlantDataRegistry {

    private static PlantDataRegistry instance;
    private final Map<PlantType, PlantStats> statsMap;

    private PlantDataRegistry() {
        statsMap = new EnumMap<>(PlantType.class);
        loadDefaults();
    }

    public static PlantDataRegistry getInstance() {
        if (instance == null) {
            instance = new PlantDataRegistry();
        }
        return instance;
    }

    public PlantStats getStats(PlantType type) {
        return statsMap.get(type);
    }

    public void registerStats(PlantStats stats) {
        statsMap.put(stats.getType(), stats);
    }

    /** بارگذاری پیش‌فرض‌ها برای اطمینان از کارکرد بدون فایل JSON */
    private void loadDefaults() {

        // Sun Producers
        reg(PlantType.SUNFLOWER, PlantFamily.SUN_PRODUCER,
                300, 50, 5, 0, 24, 0, "SunProducer",
                "Sunflower produces 50 sun every 24 seconds.",
                PlantTag.DAY, PlantTag.SUN);

        reg(PlantType.TWIN_SUNFLOWER, PlantFamily.SUN_PRODUCER,
                300, 125, 15, 0, 24, 0, "SunProducer",
                "Twin Sunflower produces 100 sun every production cycle.",
                PlantTag.DAY, PlantTag.SUN);

        reg(PlantType.SUN_SHROOM, PlantFamily.SUN_PRODUCER,
                300, 25, 5, 0, 24, 0, "SunProducer",
                "Sun-shroom grows over time and produces 25, then 50, then 75 sun.",
                PlantTag.NIGHT, PlantTag.SHROOM, PlantTag.RAMP_UP, PlantTag.SUN);

        reg(PlantType.PRIMAL_SUNFLOWER, PlantFamily.SUN_PRODUCER,
                300, 75, 5, 0, 24, 0, "SunProducer",
                "Primal Sunflower produces large 75 sun drops from the start.",
                PlantTag.DAY, PlantTag.SUN);

        reg(PlantType.GOLD_BLOOM, PlantFamily.SUN_PRODUCER,
                0, 0, 75, 0, 0, 0, "SunProducer",
                "Gold Bloom instantly produces a large amount of sun and disappears.",
                PlantTag.DAY, PlantTag.SUN, PlantTag.INSTANT);

        // Pea / Shooter Plants
        reg(PlantType.PEASHOOTER, PlantFamily.PEA,
                300, 100, 5, 20, 1.5, 9, "Shooter",
                "Peashooter fires peas straight ahead in its lane.",
                PlantTag.DAY, PlantTag.PEA);

        reg(PlantType.REPEATER, PlantFamily.PEA,
                300, 200, 5, 20, 1.5, 9, "Shooter",
                "Repeater fires two peas at a time.",
                PlantTag.DAY, PlantTag.PEA);

        reg(PlantType.THREEPEATER, PlantFamily.PEA,
                300, 300, 5, 20, 1.5, 9, "Shooter",
                "Threepeater fires peas in three lanes at the same time.",
                PlantTag.DAY, PlantTag.PEA);

        reg(PlantType.SNOW_PEA, PlantFamily.PEA,
                300, 150, 5, 20, 1.5, 9, "Shooter",
                "Snow Pea fires frozen peas that slow zombies.",
                PlantTag.DAY, PlantTag.PEA, PlantTag.ICE);

        reg(PlantType.ROTOBAGA, PlantFamily.SHOOTER,
                300, 150, 5, 10, 1.5, 9, "Shooter",
                "Rotobaga fires projectiles diagonally in multiple directions.",
                PlantTag.DAY);

        reg(PlantType.PEA_POD, PlantFamily.PEA,
                300, 125, 5, 20, 1.5, 9, "Shooter",
                "Pea Pod can be stacked up to five heads, increasing the number of peas fired.",
                PlantTag.DAY, PlantTag.PEA, PlantTag.STACK);

        reg(PlantType.SPLIT_PEA, PlantFamily.PEA,
                300, 125, 5, 20, 1.5, 9, "Shooter",
                "Split Pea fires one pea forward and two peas backward.",
                PlantTag.DAY, PlantTag.PEA);

        reg(PlantType.CITRON, PlantFamily.SHOOTER,
                300, 350, 5, 800, 9, 9, "Shooter",
                "Citron charges and fires a powerful plasma shot straight ahead.",
                PlantTag.DAY, PlantTag.CHARGE);

        reg(PlantType.CAULIPOWER, PlantFamily.HOMING,
                300, 250, 15, 999999, 12, 9, "Homing",
                "Caulipower hypnotizes random zombies from a distance.",
                PlantTag.DAY, PlantTag.MAGIC, PlantTag.CHARGE);

        reg(PlantType.ELECTRIC_BLUEBERRY, PlantFamily.HOMING,
                300, 150, 15, 5000, 12, 9, "Homing",
                "Electric Blueberry strikes random zombies with lightning.",
                PlantTag.DAY, PlantTag.CHARGE);

        reg(PlantType.BOWLING_BULB, PlantFamily.SHOOTER,
                300, 200, 5, 40, 2, 9, "Shooter",
                "Bowling Bulb fires bulbs that bounce between lanes.",
                PlantTag.DAY, PlantTag.CHARGE);

        reg(PlantType.CACTUS, PlantFamily.STRIKE_THROUGH,
                300, 175, 5, 30, 1.5, 9, "StrikeThrough",
                "Cactus fires piercing spikes that can pass through multiple zombies.",
                PlantTag.DAY);

        reg(PlantType.FIRE_PEASHOOTER, PlantFamily.PEA,
                300, 175, 5, 40, 1.5, 9, "Shooter",
                "Fire Peashooter fires flaming peas with increased damage.",
                PlantTag.DAY, PlantTag.PEA, PlantTag.FIRE);

        reg(PlantType.STARFRUIT, PlantFamily.SHOOTER,
                300, 150, 5, 20, 1.5, 9, "Shooter",
                "Starfruit fires stars in five directions.",
                PlantTag.DAY);

        reg(PlantType.GOO_PEASHOOTER, PlantFamily.PEA,
                300, 125, 5, 20, 1.5, 9, "Shooter",
                "Goo Peashooter fires poisonous peas that damage zombies over time.",
                PlantTag.DAY, PlantTag.PEA, PlantTag.POISON);

        reg(PlantType.MEGA_GATLING_PEA, PlantFamily.PEA,
                300, 400, 5, 20, 1.5, 9, "Shooter",
                "Mega Gatling Pea rapidly fires four peas in a row.",
                PlantTag.DAY, PlantTag.PEA);

        reg(PlantType.SEA_SHROOM, PlantFamily.SHROOM,
                300, 0, 15, 20, 1.5, 3, "Shooter",
                "Sea-shroom is a free short-range water shooter with limited lifetime.",
                PlantTag.NIGHT, PlantTag.SHROOM, PlantTag.WATER);

        reg(PlantType.PUFF_SHROOM, PlantFamily.SHROOM,
                300, 0, 5, 20, 1.5, 3, "Shooter",
                "Puff-shroom is a free short-range mushroom shooter with limited lifetime.",
                PlantTag.NIGHT, PlantTag.SHROOM);

        reg(PlantType.FUME_SHROOM, PlantFamily.STRIKE_THROUGH,
                300, 125, 5, 20, 1.5, 4, "StrikeThrough",
                "Fume-shroom emits fumes that pass through zombies.",
                PlantTag.NIGHT, PlantTag.SHROOM);

        // Lobbers
        reg(PlantType.CABBAGE_PULT, PlantFamily.LOBBER,
                300, 100, 5, 40, 2.9, 9, "Lobber",
                "Cabbage-pult lobs cabbages over obstacles.",
                PlantTag.DAY);

        reg(PlantType.KERNEL_PULT, PlantFamily.LOBBER,
                300, 100, 5, 20, 2.9, 9, "Lobber",
                "Kernel-pult lobs corn kernels and can stun zombies with butter.",
                PlantTag.DAY);

        reg(PlantType.MELON_PULT, PlantFamily.LOBBER,
                300, 325, 5, 80, 2.9, 9, "Lobber",
                "Melon-pult lobs melons that deal area damage.",
                PlantTag.DAY, PlantTag.AOE);

        reg(PlantType.WINTER_MELON, PlantFamily.LOBBER,
                300, 500, 5, 80, 2.9, 9, "Lobber",
                "Winter Melon lobs icy melons that deal area damage and slow zombies.",
                PlantTag.DAY, PlantTag.ICE, PlantTag.AOE);

        reg(PlantType.PEPPER_PULT, PlantFamily.LOBBER,
                300, 200, 5, 50, 2.9, 9, "Lobber",
                "Pepper-pult lobs flaming peppers that deal area damage.",
                PlantTag.DAY, PlantTag.FIRE, PlantTag.AOE);

        // Explosives and Traps
        reg(PlantType.POTATO_MINE, PlantFamily.EXPLOSIVE,
                300, 25, 25, 1800, 0, 0, "Explosive",
                "Potato Mine arms after a delay and explodes when stepped on.",
                PlantTag.DAY, PlantTag.TRAP, PlantTag.EXPLOSIVE, PlantTag.CHARGE);

        reg(PlantType.PRIMAL_POTATO_MINE, PlantFamily.EXPLOSIVE,
                300, 50, 5, 2400, 0, 0, "Explosive",
                "Primal Potato Mine arms quickly and explodes in a 3x3 area.",
                PlantTag.DAY, PlantTag.TRAP, PlantTag.EXPLOSIVE, PlantTag.CHARGE);

        reg(PlantType.CHERRY_BOMB, PlantFamily.EXPLOSIVE,
                0, 150, 35, 1800, 0, 0, "Explosive",
                "Cherry Bomb explodes instantly in a 3x3 area.",
                PlantTag.DAY, PlantTag.EXPLOSIVE, PlantTag.AOE);

        reg(PlantType.SQUASH, PlantFamily.EXPLOSIVE,
                300, 50, 20, 1800, 0, 1, "Explosive",
                "Squash jumps on and crushes the nearest zombie.",
                PlantTag.DAY, PlantTag.TRAP, PlantTag.EXPLOSIVE);

        reg(PlantType.GRAPESHOT, PlantFamily.EXPLOSIVE,
                0, 150, 35, 1800, 0, 0, "Explosive",
                "Grapeshot explodes and launches bouncing grapes.",
                PlantTag.DAY, PlantTag.EXPLOSIVE, PlantTag.AOE);

        reg(PlantType.JALAPENO, PlantFamily.EXPLOSIVE,
                0, 125, 35, 1800, 0, 0, "Explosive",
                "Jalapeno burns all zombies in its entire row.",
                PlantTag.DAY, PlantTag.FIRE, PlantTag.EXPLOSIVE, PlantTag.AOE);

        reg(PlantType.DOOM_SHROOM, PlantFamily.EXPLOSIVE,
                0, 125, 15, 1800, 0, 0, "Explosive",
                "Doom-shroom creates a massive explosion and leaves a crater.",
                PlantTag.NIGHT, PlantTag.SHROOM, PlantTag.EXPLOSIVE, PlantTag.AOE);

        reg(PlantType.TANGLE_KELP, PlantFamily.EXPLOSIVE,
                300, 25, 15, 999999, 0, 0, "Explosive",
                "Tangle Kelp pulls the first water zombie down and destroys it.",
                PlantTag.WATER, PlantTag.TRAP);

        reg(PlantType.ICEBERG_LETTUCE, PlantFamily.EXPLOSIVE,
                300, 0, 20, 0, 0, 0, "Explosive",
                "Iceberg Lettuce freezes the first zombie that steps on it.",
                PlantTag.DAY, PlantTag.TRAP, PlantTag.ICE);

        // Melee Attackers
        reg(PlantType.BONK_CHOY, PlantFamily.MELEE,
                300, 150, 5, 15, 0.25, 1, "MeleeAttacker",
                "Bonk Choy punches zombies in front of and behind it rapidly.",
                PlantTag.DAY);

        reg(PlantType.PHAT_BEET, PlantFamily.MELEE,
                300, 150, 5, 15, 2, 1, "MeleeAttacker",
                "Phat Beet damages zombies around itself with sound waves.",
                PlantTag.DAY, PlantTag.AOE);

        reg(PlantType.CHOMPER, PlantFamily.MELEE,
                300, 150, 5, 999999, 40, 1, "MeleeAttacker",
                "Chomper eats one zombie instantly, then spends time digesting.",
                PlantTag.DAY);

        reg(PlantType.WASABI_WHIP, PlantFamily.MELEE,
                300, 150, 5, 40, 2, 1, "MeleeAttacker",
                "Wasabi Whip attacks zombies in front of and behind it with fiery strikes.",
                PlantTag.DAY, PlantTag.FIRE);

        reg(PlantType.KIWIBEAST, PlantFamily.MELEE,
                300, 175, 5, 15, 2, 1, "MeleeAttacker",
                "Kiwibeast grows over time and deals area damage around itself.",
                PlantTag.DAY, PlantTag.AOE, PlantTag.RAMP_UP);

        // Wall-nuts / Defenders
        reg(PlantType.WALL_NUT, PlantFamily.WALL_NUT,
                4000, 50, 20, 0, 0, 0, "WallNut",
                "Wall-nut is a strong defensive blocker.",
                PlantTag.DAY);

        reg(PlantType.TALL_NUT, PlantFamily.WALL_NUT,
                8000, 125, 20, 0, 0, 0, "WallNut",
                "Tall-nut is a taller and stronger wall that blocks jumping zombies.",
                PlantTag.DAY);

        reg(PlantType.ENDURIAN, PlantFamily.WALL_NUT,
                3000, 100, 15, 20, 0, 0, "WallNut",
                "Endurian blocks zombies and damages attackers.",
                PlantTag.DAY);

        reg(PlantType.GARLIC, PlantFamily.WALL_NUT,
                300, 50, 20, 0, 0, 0, "WallNut",
                "Garlic forces zombies that bite it to move to another lane.",
                PlantTag.DAY, PlantTag.MOVE_ZOMBIES);

        reg(PlantType.SWEET_POTATO, PlantFamily.WALL_NUT,
                3000, 150, 20, 0, 0, 0, "WallNut",
                "Sweet Potato attracts zombies from nearby lanes.",
                PlantTag.DAY, PlantTag.MOVE_ZOMBIES);

        reg(PlantType.EXPLODE_O_NUT, PlantFamily.WALL_NUT,
                4000, 50, 20, 1800, 0, 0, "WallNut",
                "Explode-o-nut blocks zombies and explodes when destroyed.",
                PlantTag.DAY, PlantTag.EXPLOSIVE);

        reg(PlantType.PUMPKIN, PlantFamily.WALL_NUT,
                4000, 150, 20, 0, 0, 0, "WallNut",
                "Pumpkin protects another plant by being planted over it.",
                PlantTag.DAY, PlantTag.STACK);

        reg(PlantType.SUN_BEAN, PlantFamily.WALL_NUT,
                1000, 50, 20, 0, 0, 0, "WallNut",
                "Sun Bean produces sun when zombies attack it.",
                PlantTag.DAY, PlantTag.SUN);

        // Modifiers / Support / Homing
        reg(PlantType.TORCHWOOD, PlantFamily.MODIFIER,
                300, 175, 5, 0, 0, 0, "Modifier",
                "Torchwood turns peas passing through it into fire peas.",
                PlantTag.DAY, PlantTag.FIRE, PlantTag.PEA);

        reg(PlantType.MAGNET_SHROOM, PlantFamily.HOMING,
                300, 100, 15, 0, 10, 4, "Homing",
                "Magnet-shroom removes metal objects from zombies.",
                PlantTag.NIGHT, PlantTag.SHROOM, PlantTag.MAGIC);

        reg(PlantType.HYPNO_SHROOM, PlantFamily.MODIFIER,
                300, 125, 20, 0, 0, 0, "Modifier",
                "Hypno-shroom hypnotizes the zombie that eats it.",
                PlantTag.NIGHT, PlantTag.SHROOM, PlantTag.MAGIC);

        reg(PlantType.CAT_TAIL, PlantFamily.HOMING,
                300, 175, 20, 15, 1.5, 9, "Homing",
                "Cat-tail fires homing spikes at the nearest zombie.",
                PlantTag.DAY, PlantTag.HOMING);

        reg(PlantType.IMITATER, PlantFamily.MODIFIER,
                0, 0, 0, 0, 0, 0, "Modifier",
                "Imitater copies another selected plant.",
                PlantTag.DAY);

        reg(PlantType.ICE_SHROOM, PlantFamily.EXPLOSIVE,
                0, 75, 50, 0, 0, 0, "Explosive",
                "Ice-shroom freezes all zombies on the lawn.",
                PlantTag.NIGHT, PlantTag.SHROOM, PlantTag.ICE);

        reg(PlantType.LILY_PAD, PlantFamily.MODIFIER,
                300, 25, 5, 0, 0, 0, "Modifier",
                "Lily Pad allows other plants to be planted on water.",
                PlantTag.WATER, PlantTag.STACK);

        reg(PlantType.HOT_POTATO, PlantFamily.EXPLOSIVE,
                0, 0, 5, 0, 0, 0, "Explosive",
                "Hot Potato melts ice blocks.",
                PlantTag.DAY, PlantTag.FIRE);

        reg(PlantType.GRAVE_BUSTER, PlantFamily.EXPLOSIVE,
                0, 0, 10, 999999, 0, 0, "Explosive",
                "Grave Buster removes graves.",
                PlantTag.DAY);

        // Mints
        reg(PlantType.ENLIGHTEN_MINT, PlantFamily.MINT,
                0, 0, 85, 0, 0, 0, "Mint",
                "Enlighten-mint boosts SunProducer plants for a short time.",
                PlantTag.DAY, PlantTag.MINT, PlantTag.SUN);

        reg(PlantType.APPEASE_MINT, PlantFamily.MINT,
                0, 0, 85, 0, 0, 0, "Mint",
                "Appease-mint boosts Shooter plants for a short time.",
                PlantTag.DAY, PlantTag.MINT, PlantTag.PEA);

        reg(PlantType.ARMA_MINT, PlantFamily.MINT,
                0, 0, 85, 0, 0, 0, "Mint",
                "Arma-mint boosts Lobber plants for a short time.",
                PlantTag.DAY, PlantTag.MINT, PlantTag.LOBBER);

        reg(PlantType.BOMBARD_MINT, PlantFamily.MINT,
                0, 0, 85, 0, 0, 0, "Mint",
                "Bombard-mint boosts Explosive plants for a short time.",
                PlantTag.DAY, PlantTag.MINT, PlantTag.EXPLOSIVE);

        reg(PlantType.ENFORCE_MINT, PlantFamily.MINT,
                0, 0, 85, 0, 0, 0, "Mint",
                "Enforce-mint boosts MeleeAttacker plants for a short time.",
                PlantTag.DAY, PlantTag.MINT, PlantTag.MELEE);

        reg(PlantType.REINFORCE_MINT, PlantFamily.MINT,
                0, 0, 85, 0, 0, 0, "Mint",
                "Reinforce-mint boosts WallNut plants for a short time.",
                PlantTag.DAY, PlantTag.MINT, PlantTag.WALL_NUT);

        reg(PlantType.ENCHANT_MINT, PlantFamily.MINT,
                0, 0, 85, 0, 0, 0, "Mint",
                "Enchant-mint boosts Modifier plants for a short time.",
                PlantTag.DAY, PlantTag.MINT, PlantTag.MAGIC);

        reg(PlantType.PIERCE_MINT, PlantFamily.MINT,
                0, 0, 85, 0, 0, 0, "Mint",
                "Pierce-mint boosts StrikeThrough plants for a short time.",
                PlantTag.DAY, PlantTag.MINT, PlantTag.STRIKE_THROUGH);

        reg(PlantType.CATTAIL_MINT, PlantFamily.MINT,
                0, 0, 85, 0, 0, 0, "Mint",
                "Cattail-mint boosts Homing plants for a short time.",
                PlantTag.DAY, PlantTag.MINT, PlantTag.HOMING);
    }

    private void reg(PlantType type, PlantFamily family,
                     int hp, int cost, double recharge,
                     int dmg, double atkSpd, int range,
                     String category, String desc,
                     PlantTag... tagArr) {

        PlantStats s = new PlantStats(type, family, hp, cost,
                recharge, dmg, atkSpd, range, category, desc);

        java.util.List<PlantTag> tagList = new java.util.ArrayList<>();

        for (PlantTag t : tagArr) {
            tagList.add(t);
        }

        s.setTags(tagList);
        statsMap.put(type, s);
    }
}