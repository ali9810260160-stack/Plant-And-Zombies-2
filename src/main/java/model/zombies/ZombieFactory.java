package model.zombies;

import model.enums.ArmorType;
import model.enums.ZombieType;

import static model.enums.ZombieType.*;

/**
 * Factory برای ساخت نمونه‌های زامبی بر اساس نوع.
 */
public class ZombieFactory {

    /**
     * یک نمونه جدید از زامبی مورد نظر می‌سازد.
     * @param type نوع زامبی
     * @return نمونه ساخته‌شده
     */
    public static Zombie create(ZombieType type){
        Zombie zombie;
        switch (type) {
            case NORMAL: return new NormalZombie(NORMAL,190,100,0.185,100);
            case CONEHEAD: return new ConeheadZombie();
            case BUCKETHEAD: zombie = new NormalZombie(BUCKETHEAD,190,400,0.185,100);
                zombie.armors.put(ArmorType.BUCKET,1100);
                return zombie;
            case KNIGHT: zombie = new NormalZombie(KNIGHT,190,550,0.185,100);
                zombie.armors.put(ArmorType.HELMET,1600);
                zombie.armors.put(ArmorType.SHOULDER_ARMOR,1600);
                return zombie;
            case BLOCKHEAD: zombie = new NormalZombie(BLOCKHEAD,190,700,0.185,100);
                zombie.armors.put(ArmorType.BLOCK,2200);
                return zombie;
            case GARGANTUAR: return new Gargantuar();
            case IMP: return new ImpZombie(IMP,0.22,100);
            case ALL_STAR: zombie = new NormalZombie(ALL_STAR,1100,1000,0.16,100);
                return zombie;
            case ARCADE_ZOMBIE: zombie = new NormalZombie(ARCADE_ZOMBIE,490,600,0.19,100);
                // دستگاه آرکید مانند یک دبه، جلوی زامبی هل داده می‌شود و جان مستقل دارد
                zombie.armors.put(ArmorType.BARREL,1100);
                return zombie;
            case PARASOL_ZOMBIE: return new NormalZombie(PARASOL_ZOMBIE,350,200,0.25,100);
            case TURQUOISE_ZOMBIE: return new NormalZombie(TURQUOISE_ZOMBIE,250,500,0.185,100);
            case PROSPECTOR_ZOMBIE: return new NormalZombie(PROSPECTOR_ZOMBIE,190,200,0.16,100);
            case PIANIST_ZOMBIE: return new NormalZombie(PIANIST_ZOMBIE,840,450,0.12,4000);
            case NEWSPAPER_ZOMBIE: zombie = new NormalZombie(NEWSPAPER_ZOMBIE,460,700,0.22,200);
                zombie.armors.put(ArmorType.NEWSPAPER,800);
                return zombie;
            //========================================================================================
            case BARREL_ROLLER: zombie = new NormalZombie(BARREL_ROLLER,190,100,0.185,100);
                // دبه جلوی زامبی هل داده می‌شود؛ با از بین رفتن آن دو ایمپ بیرون می‌آیند
                zombie.armors.put(ArmorType.BARREL,1100);
                return zombie;
            case RA_ZOMBIE: return new NormalZombie(RA_ZOMBIE,190,100,0.2,100);
            case EXPLORER_ZOMBIE: return new NormalZombie(EXPLORER_ZOMBIE,250,250,0.25,100);
            case TOMB_RAISER: return new NormalZombie(TOMB_RAISER,380,300,0.185,100);
            case DODO_RIDER: return new NormalZombie(DODO_RIDER,490,600,0.3,100);
            case HUNTER_ZOMBIE: return new NormalZombie(HUNTER_ZOMBIE,700,500,0.12,100);
            case TROGLOBITE: return new NormalZombie(TROGLOBITE,470,600,0.185,100);
            case FISHERMAN_ZOMBIE: return new NormalZombie(FISHERMAN_ZOMBIE,1000,700,0.185,100);
            case SNORKEL_ZOMBIE: return new NormalZombie(SNORKEL_ZOMBIE,350,200,0.185,100);
            case OCTOPUS_ZOMBIE: return new NormalZombie(OCTOPUS_ZOMBIE,910,900,0.12,100);
            case JESTER_ZOMBIE: return new JesterZombie();
            case WIZARD_ZOMBIE: return new NormalZombie(WIZARD_ZOMBIE,490,800,0.12,100);
            case KING_ZOMBIE: return new NormalZombie(KING_ZOMBIE,1000,750,0,100);
            case DRAGON_IMP: return new ImpZombie(DRAGON_IMP,0.185,150);
            // برای زامبی های مینی گیم اطلاعات زیادی تو داک نبود بعضی از فیلدارو خودم پر کردم
            case ZOMBOTANY_PEASHOOTER: return new NormalZombie(ZOMBOTANY_PEASHOOTER,190,100,0.185,100);
            case ZOMBOTANY_WALLNUT: return new NormalZombie(ZOMBOTANY_WALLNUT,4000,100,0.185,100);
            case ZOMBOTANY_JALAPENO: return new NormalZombie(ZOMBOTANY_JALAPENO,190,100,0.185,100);
            case ZOMBOTANY_SQUASH: return new NormalZombie(ZOMBOTANY_SQUASH,190,100,0.4,100);
            case SUN_PRODUCER_ZOMBIE: return new NormalZombie(SUN_PRODUCER_ZOMBIE,190,100,0.185,100);
            default: return null;
        }
    }

    /**
     * waveCost یک نوع زامبی را برمی‌گرداند.
     * @param type نوع زامبی
     * @return waveCost
     */
    public static int getWaveCost(ZombieType type) {
        switch (type) {
            case NORMAL: return 100;
            case CONEHEAD: return 200;
            case BUCKETHEAD: return 400;
            case KNIGHT: return 550;
            case BLOCKHEAD: return 700;
            case GARGANTUAR: return 1500;
            case IMP: return 100;
            case ALL_STAR: return 1000;
            case ARCADE_ZOMBIE: return 600;
            case PARASOL_ZOMBIE: return 200;
            case TURQUOISE_ZOMBIE: return 500;
            case PROSPECTOR_ZOMBIE: return 200;
            case PIANIST_ZOMBIE: return 450;
            case NEWSPAPER_ZOMBIE: return 700;
            case BARREL_ROLLER: return 100;
            case RA_ZOMBIE: return 100;
            case EXPLORER_ZOMBIE: return 250;
            case TOMB_RAISER: return 300;
            case DODO_RIDER: return 600;
            case HUNTER_ZOMBIE: return 500;
            case TROGLOBITE: return 600;
            case FISHERMAN_ZOMBIE: return 700;
            case SNORKEL_ZOMBIE: return 200;
            case OCTOPUS_ZOMBIE: return 900;
            case JESTER_ZOMBIE: return 450;
            case WIZARD_ZOMBIE: return 800;
            case KING_ZOMBIE: return 750;
            case DRAGON_IMP: return 150;
            case ZOMBOTANY_PEASHOOTER: return 100;
            case ZOMBOTANY_WALLNUT: return 100;
            case ZOMBOTANY_JALAPENO: return 100;
            case ZOMBOTANY_SQUASH: return 100;
            case SUN_PRODUCER_ZOMBIE: return 100;
            default: return -1;
        }
    }

    /**
     * لیست زامبی‌های مجاز در یک فصل مشخص را برمی‌گرداند.
     * @param chapterType نوع فصل
     * @return آرایه انواع زامبی مجاز
     */
    public static ZombieType[] getAllowedZombiesForChapter(model.enums.ChapterType chapterType) {
        ZombieType[] zombieTypes = new ZombieType[19];
        getCommonZombies(zombieTypes);
        switch (chapterType){
            case ANCIENT_EGYPT:
                zombieTypes[15] = RA_ZOMBIE;
                zombieTypes[16] = EXPLORER_ZOMBIE;
                zombieTypes[17] = TOMB_RAISER;
                return zombieTypes;
            case FROSTBITE_CAVES:
                zombieTypes[15] = DODO_RIDER;
                zombieTypes[16] = HUNTER_ZOMBIE;
                zombieTypes[17] = TROGLOBITE;
                return zombieTypes;
            case BIG_WAVE_BEACH:
                zombieTypes[15] = FISHERMAN_ZOMBIE;
                zombieTypes[16] = SNORKEL_ZOMBIE;
                zombieTypes[17] = OCTOPUS_ZOMBIE;
                return zombieTypes;
            case DARK_AGES:
                zombieTypes[15] = JESTER_ZOMBIE;
                zombieTypes[16] = WIZARD_ZOMBIE;
                zombieTypes[17] = KING_ZOMBIE;
                zombieTypes[18] = DRAGON_IMP;
                return zombieTypes;
            default:
                return null;

        }

    }


    private static void getCommonZombies(ZombieType[] zombieTypes){
        zombieTypes[0] = NORMAL;
        zombieTypes[1] = CONEHEAD;
        zombieTypes[2] = BUCKETHEAD;
        zombieTypes[3] = KNIGHT;
        zombieTypes[4] = BLOCKHEAD;
        zombieTypes[5] = GARGANTUAR;
        zombieTypes[6] = IMP;
        zombieTypes[7] = ALL_STAR;
        zombieTypes[8] = ARCADE_ZOMBIE;
        zombieTypes[9] = PARASOL_ZOMBIE;
        zombieTypes[10] = TURQUOISE_ZOMBIE;
        zombieTypes[11] = PROSPECTOR_ZOMBIE;
        zombieTypes[12] = PIANIST_ZOMBIE;
        zombieTypes[13] = NEWSPAPER_ZOMBIE;
        zombieTypes[14] = BARREL_ROLLER;
    }

}
