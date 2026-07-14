package model.zombies;

import model.enums.ArmorType;
import model.enums.ChapterType;
import model.enums.ZombieType;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory برای ساخت نمونه‌های زامبی.
 * اطلاعات از ZombieDataRegistry خوانده می‌شود.
 */
public class ZombieFactory {

    public static Zombie create(ZombieType type) {
        ZombieStats stats = ZombieDataRegistry.getInstance().getStats(type);
        if (stats == null) {
            return createDefault(type);
        }
        return createFromStats(type, stats);
    }

    private static Zombie createFromStats(ZombieType type, ZombieStats stats) {
        switch (type) {
            case GARGANTUAR: return new Gargantuar();
            case IMP:        return new ImpZombie(ZombieType.IMP, 0.22, 100);
            case DRAGON_IMP: return new ImpZombie(ZombieType.DRAGON_IMP, 0.22, 150);
            case JESTER_ZOMBIE: return new JesterZombie();
            default:         return buildNormal(type, stats);
        }
    }

    private static NormalZombie buildNormal(ZombieType type, ZombieStats stats) {
        NormalZombie z = new NormalZombie(type, stats.getHp(),
                stats.getDps(), stats.getMoveSpeed(), stats.getWaveCost());
        for (ArmorType at : stats.getArmors().keySet()) {
            z.addArmor(at, stats.getArmors().get(at));
        }
        return z;
    }

    private static Zombie createDefault(ZombieType type) {
        return new NormalZombie(type, 190, 100, 0.185, 100);
    }

    public static int getWaveCost(ZombieType type) {
        ZombieStats stats = ZombieDataRegistry.getInstance().getStats(type);
        return stats != null ? stats.getWaveCost() : 100;
    }

    public static ZombieType[] getAllowedZombiesForChapter(ChapterType chapter) {
        List<ZombieType> allowed = new ArrayList<>(getCommonZombies());
        switch (chapter) {
            case ANCIENT_EGYPT:
                allowed.add(ZombieType.RA_ZOMBIE);
                allowed.add(ZombieType.EXPLORER_ZOMBIE);
                allowed.add(ZombieType.TOMB_RAISER);
                break;
            case FROSTBITE_CAVES:
                allowed.add(ZombieType.DODO_RIDER);
                allowed.add(ZombieType.HUNTER_ZOMBIE);
                allowed.add(ZombieType.TROGLOBITE);
                break;
            case BIG_WAVE_BEACH:
                allowed.add(ZombieType.FISHERMAN_ZOMBIE);
                allowed.add(ZombieType.SNORKEL_ZOMBIE);
                allowed.add(ZombieType.OCTOPUS_ZOMBIE);
                break;
            case DARK_AGES:
                allowed.add(ZombieType.JESTER_ZOMBIE);
                allowed.add(ZombieType.WIZARD_ZOMBIE);
                allowed.add(ZombieType.KING_ZOMBIE);
                allowed.add(ZombieType.DRAGON_IMP);
                break;
            default:
                break;
        }
        return allowed.toArray(new ZombieType[0]);
    }

    private static List<ZombieType> getCommonZombies() {
        List<ZombieType> list = new ArrayList<>();
        list.add(ZombieType.NORMAL);
        list.add(ZombieType.CONEHEAD);
        list.add(ZombieType.BUCKETHEAD);
        list.add(ZombieType.KNIGHT);
        list.add(ZombieType.BLOCKHEAD);
        list.add(ZombieType.GARGANTUAR);
        list.add(ZombieType.IMP);
        list.add(ZombieType.ALL_STAR);
        list.add(ZombieType.ARCADE_ZOMBIE);
        list.add(ZombieType.PARASOL_ZOMBIE);
        list.add(ZombieType.TURQUOISE_ZOMBIE);
        list.add(ZombieType.PROSPECTOR_ZOMBIE);
        list.add(ZombieType.PIANIST_ZOMBIE);
        list.add(ZombieType.NEWSPAPER_ZOMBIE);
        list.add(ZombieType.BARREL_ROLLER);
        return list;
    }
}
