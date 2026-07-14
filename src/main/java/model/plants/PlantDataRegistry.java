package model.plants;

import model.enums.PlantFamily;
import model.enums.PlantTag;
import model.enums.PlantType;

import java.util.EnumMap;
import java.util.Map;

/**
 * رجیستری مرکزی آمار گیاهان. داده‌ها از JSON بارگذاری می‌شوند.
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
        reg(PlantType.SUNFLOWER, PlantFamily.SUN_PRODUCER,
            300, 50, 7.5, 0, 0, 0, "SunProducer",
            "Sunflower produces sun every 24 seconds.",
            PlantTag.DAY, PlantTag.SUN);
        reg(PlantType.TWIN_SUNFLOWER, PlantFamily.SUN_PRODUCER,
            300, 125, 7.5, 0, 0, 0, "SunProducer",
            "TwinSunflower produces double sun.",
            PlantTag.DAY, PlantTag.SUN);
        reg(PlantType.SUN_SHROOM, PlantFamily.SUN_PRODUCER,
            300, 25, 7.5, 0, 0, 0, "SunProducer",
            "SunShroom grows stronger over time.",
            PlantTag.NIGHT, PlantTag.SHROOM, PlantTag.SUN);
        reg(PlantType.PEASHOOTER, PlantFamily.SHOOTER,
            300, 100, 7.5, 20, 1.5, 9, "Shooter",
            "Fires peas at zombies in its lane.",
            PlantTag.DAY, PlantTag.PEA);
        reg(PlantType.REPEATER, PlantFamily.SHOOTER,
            300, 200, 7.5, 20, 1.5, 9, "Shooter",
            "Fires two peas at once.",
            PlantTag.DAY, PlantTag.PEA);
        reg(PlantType.GATLING_PEA, PlantFamily.SHOOTER,
            300, 250, 7.5, 20, 3.0, 9, "Shooter",
            "Fires four peas rapidly.",
            PlantTag.DAY, PlantTag.PEA);
        reg(PlantType.MEGA_GATLING_PEA, PlantFamily.SHOOTER,
            300, 300, 7.5, 25, 4.0, 9, "Shooter",
            "Fires eight peas at once!",
            PlantTag.DAY, PlantTag.PEA);
        reg(PlantType.SNOW_PEA, PlantFamily.SHOOTER,
            300, 175, 7.5, 20, 1.5, 9, "Shooter",
            "Ice peas chill zombies, slowing them.",
            PlantTag.DAY, PlantTag.PEA, PlantTag.ICE);
        reg(PlantType.TORCHWOOD, PlantFamily.SHOOTER,
            500, 175, 5.0, 0, 0, 0, "Modifier",
            "Converts peas passing through it to fireballs.",
            PlantTag.DAY, PlantTag.PEA, PlantTag.FIRE);
        reg(PlantType.CABBAGE_PULT, PlantFamily.LOBBER,
            300, 100, 7.5, 40, 1.0, 9, "Lobber",
            "Lobs cabbages that ignore obstacles.",
            PlantTag.DAY);
        reg(PlantType.MELON_PULT, PlantFamily.LOBBER,
            300, 300, 7.5, 80, 1.0, 9, "Lobber",
            "Lobs melons for heavy AoE damage.",
            PlantTag.DAY, PlantTag.AOE);
        reg(PlantType.WINTER_MELON, PlantFamily.LOBBER,
            300, 200, 7.5, 80, 1.0, 9, "Lobber",
            "Lobs icy melons dealing AoE + chill.",
            PlantTag.DAY, PlantTag.ICE, PlantTag.AOE);
        reg(PlantType.KERNEL_PULT, PlantFamily.LOBBER,
            300, 100, 7.5, 20, 1.0, 9, "Lobber",
            "Lobs corn kernels; butter stuns zombies.",
            PlantTag.DAY);
        reg(PlantType.CHERRY_BOMB, PlantFamily.EXPLOSIVE,
            300, 150, 50.0, 180, 0, 0, "Explosive",
            "Explodes in a 3x3 area immediately!",
            PlantTag.DAY, PlantTag.EXPLOSIVE, PlantTag.AOE);
        reg(PlantType.POTATO_MINE, PlantFamily.EXPLOSIVE,
            300, 25, 30.0, 180, 0, 0, "Explosive",
            "Arms after 14s then destroys first zombie.",
            PlantTag.DAY, PlantTag.TRAP, PlantTag.EXPLOSIVE);
        reg(PlantType.JALAPENO, PlantFamily.EXPLOSIVE,
            300, 125, 50.0, 180, 0, 0, "Explosive",
            "Burns all zombies in its entire row!",
            PlantTag.DAY, PlantTag.FIRE, PlantTag.EXPLOSIVE);
        reg(PlantType.EXPLODE_O_NUT, PlantFamily.EXPLOSIVE,
            2000, 125, 30.0, 150, 0, 0, "Explosive",
            "Blocks zombies then explodes in 3x3.",
            PlantTag.DAY, PlantTag.EXPLOSIVE);
        reg(PlantType.CHOMPER, PlantFamily.MELEE_ATTACKER,
            300, 150, 7.5, 9999, 0.3, 1, "MeleeAttacker",
            "Swallows zombies whole. Takes 42s to digest.",
            PlantTag.DAY);
        reg(PlantType.BONK_CHOY, PlantFamily.MELEE_ATTACKER,
            300, 125, 7.5, 40, 2.0, 1, "MeleeAttacker",
            "Punches zombies front and back rapidly.",
            PlantTag.DAY);
        reg(PlantType.SQUASH, PlantFamily.MELEE_ATTACKER,
            300, 50, 30.0, 180, 0, 1, "MeleeAttacker",
            "Crushes the nearest zombie instantly!",
            PlantTag.DAY, PlantTag.EXPLOSIVE);
        reg(PlantType.WALL_NUT, PlantFamily.WALL_NUT,
            4000, 50, 30.0, 0, 0, 0, "WallNut",
            "Massive HP blocker for zombie traffic.",
            PlantTag.DAY, PlantTag.STACKABLE);
        reg(PlantType.TALL_NUT, PlantFamily.WALL_NUT,
            8000, 125, 30.0, 0, 0, 0, "WallNut",
            "Cannot be vaulted by jumping zombies.",
            PlantTag.DAY, PlantTag.STACKABLE);
        reg(PlantType.PUMPKIN, PlantFamily.WALL_NUT,
            4000, 125, 30.0, 0, 0, 0, "WallNut",
            "Protects the plant inside from damage.",
            PlantTag.DAY, PlantTag.STACKABLE);
        reg(PlantType.GARLIC, PlantFamily.MODIFIER,
            600, 50, 30.0, 0, 0, 0, "Modifier",
            "Forces biting zombies to change lanes.",
            PlantTag.DAY, PlantTag.MOVE_ZOMBIES);
        reg(PlantType.SWEET_POTATO, PlantFamily.MODIFIER,
            800, 125, 30.0, 0, 0, 0, "Modifier",
            "Attracts zombies from adjacent lanes.",
            PlantTag.DAY, PlantTag.MOVE_ZOMBIES);
        reg(PlantType.MAGNET_SHROOM, PlantFamily.MODIFIER,
            300, 100, 7.5, 0, 0.5, 4, "Modifier",
            "Pulls metal armor off zombies.",
            PlantTag.NIGHT, PlantTag.SHROOM);
        reg(PlantType.HYPNO_SHROOM, PlantFamily.MODIFIER,
            300, 75, 30.0, 0, 0, 0, "Modifier",
            "Hypnotizes zombies to fight for you!",
            PlantTag.NIGHT, PlantTag.SHROOM, PlantTag.MAGIC);
        reg(PlantType.LASER_BEAN, PlantFamily.STRIKE_THROUGH,
            300, 200, 7.5, 30, 1.0, 9, "StrikeThrough",
            "Laser passes through all zombies in lane.",
            PlantTag.DAY);
        reg(PlantType.FUME_SHROOM, PlantFamily.STRIKE_THROUGH,
            300, 75, 7.5, 20, 1.0, 5, "StrikeThrough",
            "Fumes pass through all obstacles.",
            PlantTag.NIGHT, PlantTag.SHROOM);
        reg(PlantType.SNAPDRAGON, PlantFamily.MELEE_ATTACKER,
            300, 150, 7.5, 40, 1.5, 1, "MeleeAttacker",
            "Breathes fire in a 2x3 area.",
            PlantTag.DAY, PlantTag.FIRE, PlantTag.AOE);
        reg(PlantType.LILY_PAD, PlantFamily.MODIFIER,
            400, 25, 7.5, 0, 0, 0, "WaterPlant",
            "Floats on water for other plants to stand on.",
            PlantTag.WATER, PlantTag.STACKABLE);
        reg(PlantType.TANGLE_KELP, PlantFamily.MODIFIER,
            100, 25, 30.0, 9999, 0, 0, "WaterPlant",
            "Drags the first zombie stepping on it under.",
            PlantTag.WATER, PlantTag.TRAP);
        reg(PlantType.PUFF_SHROOM, PlantFamily.MODIFIER,
            100, 0, 7.5, 20, 1.0, 9, "Shooter",
            "Free but temporary; disappears after 120s.",
            PlantTag.NIGHT, PlantTag.SHROOM);
        reg(PlantType.SPORE_SHROOM, PlantFamily.MODIFIER,
            300, 25, 7.5, 20, 1.0, 9, "Shooter",
            "Fires spore clouds at zombies.",
            PlantTag.NIGHT, PlantTag.SHROOM);
        reg(PlantType.SPIKEWEED, PlantFamily.EXPLOSIVE,
            300, 100, 7.5, 10, 1.0, 0, "MeleeAttacker",
            "Damages zombies that walk over it.",
            PlantTag.DAY, PlantTag.TRAP);
        reg(PlantType.COB_CANNON, PlantFamily.LOBBER,
            500, 500, 36.0, 180, 0.3, 9, "Lobber",
            "Fires a massive explosive cob cannon!",
            PlantTag.DAY, PlantTag.AOE);
        reg(PlantType.PEANUT, PlantFamily.LOBBER,
            400, 125, 7.5, 30, 1.0, 9, "Lobber",
            "Lobs peanuts at zombies.",
            PlantTag.DAY);
        reg(PlantType.CITRON, PlantFamily.HOMING,
            300, 325, 7.5, 60, 0.5, 9, "Homing",
            "Plasma ball homes in on zombies anywhere.",
            PlantTag.DAY, PlantTag.CHARGE);
        reg(PlantType.HOMING_THISTLE, PlantFamily.HOMING,
            300, 200, 7.5, 25, 1.5, 9, "Homing",
            "Locks on and fires at zombies anywhere.",
            PlantTag.DAY, PlantTag.CHARGE);
        reg(PlantType.SPEARMINT, PlantFamily.MINT,
            100, 100, 60.0, 0, 0, 0, "Mint",
            "Activates plant food on all pea plants.",
            PlantTag.DAY);
        reg(PlantType.FROSTBITE_CAVES_MINT, PlantFamily.MINT,
            100, 100, 60.0, 0, 0, 0, "Mint",
            "Activates plant food on all ice plants.",
            PlantTag.NIGHT, PlantTag.ICE);
        reg(PlantType.TORCHWOOD_MINT, PlantFamily.MINT,
            100, 100, 60.0, 0, 0, 0, "Mint",
            "Activates plant food on all fire plants.",
            PlantTag.DAY, PlantTag.FIRE);
        reg(PlantType.WALLNUT_BOWLING, PlantFamily.WALL_NUT,
            4000, 0, 0, 180, 0, 9, "MinigameOnly",
            "Bowling walnut: deflects at 45deg on zombie hit.",
            PlantTag.DAY);
        reg(PlantType.EXPLODE_O_NUT_BOWLING, PlantFamily.EXPLOSIVE,
            300, 0, 0, 180, 0, 9, "MinigameOnly",
            "Explodes in 3x3 on first zombie hit.",
            PlantTag.DAY, PlantTag.EXPLOSIVE);
        reg(PlantType.BIG_WALLNUT, PlantFamily.WALL_NUT,
            8000, 0, 0, 200, 0, 9, "MinigameOnly",
            "Crushes all zombies in path.",
            PlantTag.DAY);
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
