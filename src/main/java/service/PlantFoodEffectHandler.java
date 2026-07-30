package service;

import view.ConsoleView;
import model.GameSession;
import model.Projectile;
import model.enums.PlantType;
import model.enums.ProjectileType;
import model.enums.ArmorType;
import model.enums.ZombieEffect;
import model.plants.GenericPlant;
import model.zombies.Zombie;
import view.PlantFoodView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * هندلر اثرات غذای گیاه.
 *
 * هر گیاه اثر منحصربه‌فردی دارد که از توضیحات JSON ترجمه شده است.
 * ورودی: نوع گیاه + نمونه + session جاری.
 */
public final class PlantFoodEffectHandler {

    private static final ConsoleView View = new ConsoleView();
    private static final Random RNG = new Random();

    /** Null-safe helper — متد view مربوطه را فقط اگر view موجود باشد صدا می‌زند. */
    private static PlantFoodView pv(ConsoleView view) {
        return view != null ? view.getPlantFoodView() : null;
    }


    private PlantFoodEffectHandler() { }

    /**
     * نقطه ورود اصلی — اثر غذای گیاه متناسب با نوع اعمال می‌شود.
     *
     * @param type    نوع گیاه
     * @param plant   نمونه گیاه روی نقشه
     * @param session نشست بازی (می‌تواند null باشد)
     */
    public static void apply(PlantType type, GenericPlant plant,
                             GameSession session, ConsoleView view) {
        switch (type) {
            // ── تولیدکنندگان خورشید ──────────────────────────────────
            case SUNFLOWER:
                produceSun(session, 150);
                break;
            case TWIN_SUNFLOWER:
                produceSun(session, 250);
                break;
            case SUN_SHROOM:
                produceSun(session, 225);
                break;
            case PRIMAL_SUNFLOWER:
                produceSun(session, 225);
                break;
            case GOLD_BLOOM:
                // مصرفی فوری — اثر را قبل از کاشتن اعمال کرده
                break;

            // ── تیراندازهای خط مستقیم ───────────────────────────────
            case PEASHOOTER:
                rapidFire(plant, session, 30, ProjectileType.NORMAL, 1.0);
                break;
            case REPEATER:
                rapidFire(plant, session, 30, ProjectileType.NORMAL, 1.0);
                fireBigProjectile(plant, session, ProjectileType.NORMAL, 20);
                break;
            case THREEPEATER:
                rapidFireAllLanes(plant, session, 10, ProjectileType.NORMAL);
                break;
            case SNOW_PEA:
                freezeLane(plant, session);
                rapidFire(plant, session, 20, ProjectileType.ICE, 1.0);
                break;
            case ROTOBAGA:
                rapidFireFourDirections(plant, session, 10);
                break;
            case PEA_POD:
                for (int i = 0; i < 5; i++) {
                    fireBigProjectile(plant, session, ProjectileType.NORMAL, 20);
                }
                break;
            case SPLIT_PEA:
                rapidFire(plant, session, 15, ProjectileType.NORMAL, 1.0);
                rapidFireBackward(plant, session, 15, ProjectileType.NORMAL);
                break;
            case CITRON:
                clearLaneDamage(plant, session, 3000);
                break;
            case CAULIPOWER:
                hypnotizeRandom(session, 3);
                break;
            case ELECTRIC_BLUEBERRY:
                killRandom(session, 3);
                break;
            case BOWLING_BULB:
                for (int i = 0; i < 3; i++) {
                    fireBigProjectile(plant, session, ProjectileType.LOBBED, 150);
                }
                break;
            case CACTUS:
                rapidFireStrike(plant, session, 20);
                break;
            case FIRE_PEASHOOTER:
                rapidFire(plant, session, 20, ProjectileType.FIRE, 2.0);
                clearLaneDamage(plant, session, 100);
                break;
            case STARFRUIT:
                rapidFireAllDirections(plant, session, 15);
                break;
            case GOO_PEASHOOTER:
                rapidFire(plant, session, 20, ProjectileType.POISON, 1.0);
                break;
            case MEGA_GATLING_PEA:
                rapidFire(plant, session, 60, ProjectileType.NORMAL, 1.0);
                for (int i = 0; i < 4; i++) {
                    fireBigProjectile(plant, session, ProjectileType.NORMAL, 20);
                }
                break;

            // ── پرتاب‌کنندگان قوسی ──────────────────────────────────
            case SEA_SHROOM:
            case PUFF_SHROOM:
                rapidFire(plant, session, 10, ProjectileType.LOBBED, 1.0);
                break;
            case FUME_SHROOM:
                aoeAroundPlant(plant, session, 2, 100);
                pushBackLane(plant, session);
                break;
            case CABBAGE_PULT:
                lobRandom(plant, session, 5, 40, false);
                break;
            case KERNEL_PULT:
                butterAllInLane(plant, session);
                break;
            case MELON_PULT:
                lobRandom(plant, session, 5, 120, false);
                break;
            case WINTER_MELON:
                lobRandom(plant, session, 5, 120, true);
                break;
            case PEPPER_PULT:
                lobRandom(plant, session, 3, 80, false);
                break;

            // ── انفجاری‌ها ────────────────────────────────────────────
            case POTATO_MINE:
            case PRIMAL_POTATO_MINE:
                // فوری آماده می‌شود + 2 مین اضافه (شبیه‌سازی با AoE)
                aoeAroundPlant(plant, session, 2, 180);
                break;
            case SQUASH:
                killRandom(session, 2);
                break;
            case TANGLE_KELP:
            case ICEBERG_LETTUCE:
                freezeAll(session);
                break;
            case CHERRY_BOMB:
            case GRAPESHOT:
            case JALAPENO:
            case DOOM_SHROOM:
                // مصرفی فوری — بدون اثر اضافی
                break;

            // ── مبارزان تن‌به‌تن ────────────────────────────────────
            case BONK_CHOY:
                aoeAroundPlant(plant, session, 1, 120);
                break;
            case PHAT_BEET:
                aoeAroundPlant(plant, session, 2, 200);
                break;
            case CHOMPER:
                swallowRandom(session, 3);
                break;
            case WASABI_WHIP:
                aoeAroundPlant(plant, session, 1, 100);
                break;
            case KIWIBEAST:
                aoeAroundPlant(plant, session, 2, 300);
                break;

            // ── مدافع‌ها ─────────────────────────────────────────────
            case WALL_NUT:
                plant.healToFull();
                plant.addTemporaryArmor(4000);
                break;
            case TALL_NUT:
                plant.healToFull();
                plant.addTemporaryArmor(8000);
                break;
            case ENDURIAN:
            case EXPLODE_O_NUT:
                plant.healToFull();
                plant.addTemporaryArmor(3000);
                break;
            case PUMPKIN:
                plant.healToFull();
                plant.addTemporaryArmor(6000);
                break;
            case GARLIC:
                pushBackLane(plant, session);
                break;
            case SWEET_POTATO:
                plant.healToFull();
                stunNearbyZombies(plant, session, 2, 100);
                break;

            // ── اصلاح‌کننده‌ها ────────────────────────────────────────
            case TORCHWOOD:
                // بوست آتشی: اثر روی پرتابه‌های بعدی در CombatService
                // اینجا یک AoE حرارتی می‌زنیم
                aoeAroundPlant(plant, session, 2, 60);
                break;
            case MAGNET_SHROOM:
                removeAllMetalArmors(session);
                break;
            case HYPNO_SHROOM:
                hypnotizeRandom(session, 1);
                break;
            case CAT_TAIL:
                rapidFireHoming(plant, session, 20);
                break;
            case SUN_BEAN:
                plant.addTemporaryArmor(1000);
                break;
            case LILY_PAD:
                produceSun(session, 50);
                break;

            // ── نعناع‌ها و مصرفی‌های فوری ────────────────────────────
            case HOT_POTATO:
            case GRAVE_BUSTER:
            case ICE_SHROOM:
            case IMITATER:
            default:
                // بدون اثر اضافی
                break;
        }
    }

    // ============================================================
    //  EFFECT IMPLEMENTATIONS
    // ============================================================

    /** اضافه کردن خورشید به session */
    private static void produceSun(GameSession session, int amount) {
        if (session != null) {
            session.addSun(amount);
            if (pv(View) != null) pv(View).printSunProduced(amount);
        }
    }

    /**
     * شلیک رگباری N پرتابه از موقعیت گیاه.
     * @param damageMult ضریب آسیب (1.0 = عادی، 2.0 = دوبرابر)
     */
    private static void rapidFire(GenericPlant plant, GameSession session,
                                  int count, ProjectileType pType, double damageMult) {
        if (session == null) return;
        int dmg = (int)(plant.getCurrentEffectiveDamage() * damageMult);
        for (int i = 0; i < count; i++) {
            Projectile p = plant.buildProjectile(pType, dmg);
            p.setMovingRight(true);
            session.getActiveProjectiles().add(p);
        }
        if (pv(View) != null) pv(View).printRapidFire(count, pType.name() + " projectiles");
    }

    /** پرتابه بزرگ با ضریب آسیب بالا */
    private static void fireBigProjectile(GenericPlant plant, GameSession session,
                                          ProjectileType pType, int damageMultiplier) {
        if (session == null) return;
        int dmg = plant.getCurrentEffectiveDamage() * damageMultiplier;
        Projectile p = plant.buildProjectile(pType, dmg);
        p.setMovingRight(true);
        session.getActiveProjectiles().add(p);
    }

    /** شلیک به تمام ردیف‌ها (Threepeater) */
    private static void rapidFireAllLanes(GenericPlant plant, GameSession session,
                                          int count, ProjectileType pType) {
        if (session == null) return;
        int rows = session.getGameMap().getRows();
        int dmg = plant.getCurrentEffectiveDamage();
        for (int row = 1; row <= rows; row++) {
            for (int i = 0; i < count; i++) {
                Projectile p = new Projectile(pType, plant.getX(), row, dmg);
                p.setMovingRight(true);
                session.getActiveProjectiles().add(p);
            }
        }
        if (pv(View) != null) pv(View).printFiredAllLanes(rows);
    }

    /** شلیک به ۴ جهت قطری (Rotobaga) */
    private static void rapidFireFourDirections(GenericPlant plant, GameSession session,
                                                int countPerDir) {
        if (session == null) return;
        int dmg = plant.getCurrentEffectiveDamage();
        for (int i = 0; i < countPerDir; i++) {
            // فقط جلو و عقب (افقی) — دستکاری Y در CombatService
            Projectile fwd = new Projectile(ProjectileType.NORMAL, plant.getX(),
                    plant.getY(), dmg);
            fwd.setMovingRight(true);
            Projectile bwd = new Projectile(ProjectileType.NORMAL, plant.getX(),
                    plant.getY(), dmg);
            bwd.setMovingRight(false);
            session.getActiveProjectiles().add(fwd);
            session.getActiveProjectiles().add(bwd);
        }
        if (pv(View) != null) pv(View).printOmnidirectionalFire();
    }

    /** شلیک همه جهت (Starfruit) */
    private static void rapidFireAllDirections(GenericPlant plant, GameSession session,
                                               int count) {
        rapidFireFourDirections(plant, session, count);
    }

    /** شلیک به عقب */
    private static void rapidFireBackward(GenericPlant plant, GameSession session,
                                          int count, ProjectileType pType) {
        if (session == null) return;
        int dmg = plant.getCurrentEffectiveDamage();
        for (int i = 0; i < count; i++) {
            Projectile p = new Projectile(pType, plant.getX(), plant.getY(), dmg);
            p.setMovingRight(false); // شلیک به عقب (به سمت خانه) برای SplitPea
            session.getActiveProjectiles().add(p);
        }
    }

    /** شلیک نفوذکننده (Cactus) */
    private static void rapidFireStrike(GenericPlant plant, GameSession session, int count) {
        if (session == null) return;
        int dmg = plant.getCurrentEffectiveDamage();
        for (int i = 0; i < count; i++) {
            Projectile p = plant.buildProjectile(ProjectileType.STRIKE, dmg);
            p.setPassesThrough(true);
            p.setMovingRight(true);
            session.getActiveProjectiles().add(p);
        }
        if (pv(View) != null) pv(View).printPiercingShots(count);
    }

    /** شلیک به‌سوی زامبی تصادفی (Cat Tail) */
    private static void rapidFireHoming(GenericPlant plant, GameSession session, int count) {
        if (session == null) return;
        List<Zombie> zombies = session.getActiveZombies();
        if (zombies.isEmpty()) return;
        int dmg = plant.getCurrentEffectiveDamage();
        for (int i = 0; i < count; i++) {
            Zombie target = zombies.get(RNG.nextInt(zombies.size()));
            Projectile p = new Projectile(ProjectileType.NORMAL,
                    plant.getX(), plant.getY(), dmg);
            p.setY(target.getY());
            session.getActiveProjectiles().add(p);
        }
        if (pv(View) != null) pv(View).printHomingShots(count);
    }

    /** آسیب به تمام زامبی‌های ردیف */
    private static void clearLaneDamage(GenericPlant plant, GameSession session,
                                        int damage) {
        if (session == null) return;
        int row = plant.getY();
        int killed = 0;
        for (Zombie z : session.getActiveZombies()) {
            if (z.getY() == row) {
                z.takeDamage(damage);
                if (!z.isAlive()) killed++;
            }
        }
        if (pv(View) != null) pv(View).printLaneCleared(row, killed);
    }

    /** یخ‌زدن تمام زامبی‌های ردیف (Snow Pea) */
    private static void freezeLane(GenericPlant plant, GameSession session) {
        if (session == null) return;
        int row = plant.getY();
        for (Zombie z : session.getActiveZombies()) {
            if (z.getY() == row) {
                z.addEffect(ZombieEffect.FROZEN, 150); // 15 ثانیه
            }
        }
        if (pv(View) != null) pv(View).printFrozeLane(row);
    }

    /** یخ‌زدن همه زامبی‌ها (IceShroom، TangleKelp) */
    private static void freezeAll(GameSession session) {
        if (session == null) return;
        for (Zombie z : session.getActiveZombies()) {
            z.addEffect(ZombieEffect.FROZEN, 150);
        }
        if (pv(View) != null) pv(View).printFrozeAll();
    }

    /** هیپنوتیزم N زامبی تصادفی */
    private static void hypnotizeRandom(GameSession session, int count) {
        if (session == null) return;
        List<Zombie> zombies = new ArrayList<>(session.getActiveZombies());
        Collections.shuffle(zombies, RNG);
        int n = Math.min(count, zombies.size());
        for (int i = 0; i < n; i++) {
            zombies.get(i).setHypnotized(true);
        }
        if (pv(View) != null) pv(View).printHypnotized(n);
    }

    /** کشتن N زامبی تصادفی */
    private static void killRandom(GameSession session, int count) {
        if (session == null) return;
        List<Zombie> zombies = new ArrayList<>(session.getActiveZombies());
        Collections.shuffle(zombies, RNG);
        int n = Math.min(count, zombies.size());
        for (int i = 0; i < n; i++) {
            zombies.get(i).takeDamage(Integer.MAX_VALUE / 2);
        }
        if (pv(View) != null) pv(View).printInstantKill(n);
    }

    /** قورت دادن N زامبی (Chomper) */
    private static void swallowRandom(GameSession session, int count) {
        killRandom(session, count);
        if (pv(View) != null) pv(View).printChomperSwallowed(count);
    }

    /** آسیب AoE در شعاع r دور گیاه */
    private static void aoeAroundPlant(GenericPlant plant, GameSession session,
                                       int radius, int damage) {
        if (session == null) return;
        int px = plant.getX();
        int py = plant.getY();
        int killed = 0;
        for (Zombie z : session.getActiveZombies()) {
            int zx = (int) Math.round(z.getX());
            int zy = z.getY();
            if (Math.abs(zx - px) <= radius && Math.abs(zy - py) <= radius) {
                z.takeDamage(damage + plant.getBonusAoeDamage());
                if (!z.isAlive()) killed++;
            }
        }
        if (pv(View) != null) pv(View).printAoeBlast(2 * radius + 1, killed);
    }

    /** هل دادن زامبی‌های ردیف به عقب */
    private static void pushBackLane(GenericPlant plant, GameSession session) {
        if (session == null) return;
        int row = plant.getY();
        for (Zombie z : session.getActiveZombies()) {
            if (z.getY() == row) {
                z.setX(Math.min(z.getX() + 3.0, session.getGameMap().getCols() + 0.5));
            }
        }
        if (pv(View) != null) pv(View).printPushBack(row);
    }

    /** خواباندن (butter) همه زامبی‌های ردیف — مشابه KernelPult */
    private static void butterAllInLane(GenericPlant plant, GameSession session) {
        if (session == null) return;
        int row = plant.getY();
        for (Zombie z : session.getActiveZombies()) {
            if (z.getY() == row) {
                z.addEffect(ZombieEffect.STUNNED, 80); // 8 ثانیه
            }
        }
        if (pv(View) != null) pv(View).printButtered(row);
    }

    /** پرتاب قوسی به مکان‌های تصادفی */
    private static void lobRandom(GenericPlant plant, GameSession session,
                                  int count, int damage, boolean ice) {
        if (session == null) return;
        ProjectileType pType = ice ? ProjectileType.ICE : ProjectileType.LOBBED;
        for (int i = 0; i < count; i++) {
            Projectile p = plant.buildProjectile(pType, damage
                    + plant.getBonusAoeDamage());
            p.setArc(true);
            p.setMovingRight(true);
            session.getActiveProjectiles().add(p);
        }
        if (pv(View) != null) pv(View).printLobbedBalls(count);
    }

    /** حذف تمام زره‌های فلزی از همه زامبی‌ها (MagnetShroom) */
    private static void removeAllMetalArmors(GameSession session) {
        if (session == null) return;
        int removed = 0;
        for (Zombie z : session.getActiveZombies()) {
            if (z.hasArmor(ArmorType.BUCKET)) { z.removeArmor(ArmorType.BUCKET); removed++; }
            if (z.hasArmor(ArmorType.HELMET)) { z.removeArmor(ArmorType.HELMET); removed++; }
        }
        if (pv(View) != null) pv(View).printRemovedArmors(removed);
    }

    /** فلج کردن زامبی‌های نزدیک (SweetPotato plant food) */
    private static void stunNearbyZombies(GenericPlant plant, GameSession session,
                                          int radius, int durationTicks) {
        if (session == null) return;
        int px = plant.getX();
        int py = plant.getY();
        for (Zombie z : session.getActiveZombies()) {
            int zx = (int) Math.round(z.getX());
            int zy = z.getY();
            if (Math.abs(zx - px) <= radius && Math.abs(zy - py) <= radius) {
                z.addEffect(ZombieEffect.STUNNED, durationTicks);
            }
        }
        if (pv(View) != null) pv(View).printStunnedNearby();
    }
}
