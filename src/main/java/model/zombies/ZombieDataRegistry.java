package model.zombies;

import model.enums.ArmorType;
import model.enums.ZombieType;

import java.util.EnumMap;
import java.util.Map;

/**
 * رجیستری مرکزی آمار زامبی‌ها — Singleton، داده‌ها در شروع بارگذاری می‌شوند.
 */
public class ZombieDataRegistry {

    private static ZombieDataRegistry instance;
    private final Map<ZombieType, ZombieStats> map;

    private ZombieDataRegistry() {
        map = new EnumMap<>(ZombieType.class);
        loadDefaults();
    }

    public static ZombieDataRegistry getInstance() {
        if (instance == null) {
            instance = new ZombieDataRegistry();
        }
        return instance;
    }

    public ZombieStats getStats(ZombieType type) {
        return map.get(type);
    }

    public void register(ZombieStats stats) {
        map.put(stats.getType(), stats);
    }

    private void loadDefaults() {
        reg(ZombieType.NORMAL, 190, 100, 0.185, 100, "Basic zombie.");
        reg(ZombieType.CONEHEAD, 190, 100, 0.185, 200,
            "Cone gives extra protection.")
            .withArmor(ArmorType.CONE, 370);
        reg(ZombieType.BUCKETHEAD, 190, 100, 0.185, 400,
            "Bucket provides massive protection.")
            .withArmor(ArmorType.BUCKET, 1100);
        reg(ZombieType.KNIGHT, 190, 100, 0.185, 550,
            "Medieval armor. Destroy helmet and shoulders first.")
            .withArmor(ArmorType.HELMET, 1600)
            .withArmor(ArmorType.SHOULDER_ARMOR, 1600);
        reg(ZombieType.BLOCKHEAD, 190, 100, 0.185, 700,
            "Ice block protection.")
            .withArmor(ArmorType.BLOCK, 2200);
        reg(ZombieType.IMP, 100, 100, 0.22, 50, "Small fast zombie.");
        reg(ZombieType.ALL_STAR, 1100, 200, 0.16, 1000,
            "Charges at high speed, one-hit kills plants.");
        reg(ZombieType.ARCADE_ZOMBIE, 490, 100, 0.19, 600,
            "Pushes arcade machine. Spawns Imps when machine breaks.")
            .withArmor(ArmorType.BARREL, 1100);
        reg(ZombieType.PARASOL_ZOMBIE, 350, 100, 0.25, 200,
            "Deflects all lobber projectiles.");
        reg(ZombieType.TURQUOISE_ZOMBIE, 250, 100, 0.185, 500,
            "Steals sun then fires laser destroying 4 plants!");
        reg(ZombieType.PROSPECTOR_ZOMBIE, 190, 100, 0.16, 200,
            "Dynamite explodes after 10s, sending him backward.");
        reg(ZombieType.PIANIST_ZOMBIE, 840, 100, 0.12, 450,
            "Plays music randomly shifting zombies between lanes.");
        reg(ZombieType.NEWSPAPER_ZOMBIE, 460, 100, 0.22, 200,
            "Gets enraged when newspaper is destroyed!")
            .withArmor(ArmorType.NEWSPAPER, 800);
        reg(ZombieType.BARREL_ROLLER, 190, 100, 0.185, 100,
            "Pushes barrel; two Imps emerge when barrel breaks.")
            .withArmor(ArmorType.BARREL, 1100);
        reg(ZombieType.RA_ZOMBIE, 190, 100, 0.2, 100,
            "Raises sun from the ground magnetically.");
        reg(ZombieType.EXPLORER_ZOMBIE, 250, 150, 0.25, 250,
            "Torch burns nearby plants. Ice extinguishes it.");
        reg(ZombieType.TOMB_RAISER, 380, 100, 0.185, 300,
            "Creates tombstones every few seconds.");
        reg(ZombieType.DODO_RIDER, 490, 150, 0.3, 600,
            "Flies over obstacles (not Tall-Nuts).");
        reg(ZombieType.HUNTER_ZOMBIE, 700, 100, 0.12, 500,
            "Throws ice at plants incrementing freeze level.");
        reg(ZombieType.TROGLOBITE, 470, 100, 0.185, 600,
            "Pushes ice blocks that destroy plants.");
        reg(ZombieType.FISHERMAN_ZOMBIE, 1000, 100, 0.0, 700,
            "Hooks plants pulling them to the right.");
        reg(ZombieType.SNORKEL_ZOMBIE, 350, 100, 0.185, 200,
            "Swims under water, only lobbers can hit it.");
        reg(ZombieType.OCTOPUS_ZOMBIE, 910, 100, 0.12, 900,
            "Throws octopuses that freeze plants.");
        reg(ZombieType.JESTER_ZOMBIE, 490, 100, 0.12, 450,
            "Deflects projectiles back at plants when spinning.");
        reg(ZombieType.WIZARD_ZOMBIE, 490, 0, 0.12, 800,
            "Turns plants into harmless cats!");
        reg(ZombieType.KING_ZOMBIE, 1000, 0, 0.0, 750,
            "Upgrades nearby zombies to Knights.");
        reg(ZombieType.DRAGON_IMP, 150, 100, 0.22, 150,
            "Immune to fire damage!");
        reg(ZombieType.ZOMBOTANY_PEASHOOTER, 190, 100, 0.185, 100,
            "Fires peas at your plants!");
        reg(ZombieType.ZOMBOTANY_WALLNUT, 4000, 100, 0.185, 100,
            "Extremely tough zombie.");
        reg(ZombieType.ZOMBOTANY_JALAPENO, 190, 100, 0.185, 100,
            "Burns entire row if alive for 10 seconds!");
        reg(ZombieType.ZOMBOTANY_SQUASH, 190, 500, 0.4, 100,
            "Fast; destroys itself and plant on contact.");
        reg(ZombieType.SUN_PRODUCER_ZOMBIE, 490, 100, 0.185, 100,
            "Produces sun for you. Protect it!");
    }

    private ZombieStats reg(ZombieType t, int hp, int dps, double spd,
                             int cost, String desc) {
        ZombieStats s = new ZombieStats(t, hp, dps, spd, cost, desc);
        map.put(t, s);
        return s;
    }
}
