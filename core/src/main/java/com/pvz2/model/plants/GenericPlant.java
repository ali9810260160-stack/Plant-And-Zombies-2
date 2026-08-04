package com.pvz2.model.plants;

import com.pvz2.model.GameSession;
import com.pvz2.model.Projectile;
import com.pvz2.model.enums.*;
import com.pvz2.model.tiles.Tile;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.util.RandomUtil;

import java.util.ArrayList;
import java.util.List;
import com.pvz2.view.ConsoleView;

/**
 * پیاده‌سازی عمومی گیاه بر اساس داده‌های JSON.
 * این کلاس رفتار اکثر گیاهان را پوشش می‌دهد.
 *
 * فیلدهای effective* بر اثر ارتقا تغییر می‌کنند (نه stats مشترک).
 */
public class GenericPlant extends Plant {

    private final PlantStats stats;

    // -------- تایمرها --------
    private int attackTimer;
    private int sunProductionTimer;
    private int chargeTimer;
    private int magnetCooldown;
    private int sweetPotatoTimer;
    private int stageTimer;
    private int lifespanTicks;

    // وضعیت‌ها
    private boolean sunPending;
    private boolean armed;
    private int armTimer;
    private boolean instantActivated;
    private boolean charged;
    private int shroomStage;   // Sun-shroom: 1/2/3
    private int kiwiStage;     // Kiwibeast: 1/2/3
    private int sunProducedCount;

    // پرتابه‌های در انتظار ارسال به CombatService
    private List<Projectile> pendingProjectiles;

    // -------- فیلدهای effective (تغییرپذیر با ارتقا) --------
    private int effectiveDamage;
    private double effectiveAttackSpeed;
    private double effectiveSunProdIntervalSecs;
    private int bonusAoeDamage;
    private int bonusRange;
    private int bonusLifespanTicks;
    private int bonusChillTimeTicks;
    private int bonusPierce;
    private int bonusBounces;
    private int bonusTargets;
    private int bonusMaxSize;
    private double bonusSpecialChancePct;
    private int bonusSunAmount;

    // -------- فلگ‌های ارتقا --------
    private boolean doubleSunChance;
    private boolean canCrushTwice;
    private boolean targetPriorityUp;

    public GenericPlant(PlantStats stats) {
        super(stats.getType(), stats.getFamily(),
                stats.getBaseHp(), stats.getSunCost(), stats.getRechargeTime());
        this.stats = stats;
        this.tags = stats.getTags();
        this.baseDamage = stats.getBaseDamage();

        // مقداردهی اولیه effective fields از stats
        this.effectiveDamage = stats.getBaseDamage();
        this.effectiveAttackSpeed = stats.getAttackSpeed();
        this.effectiveSunProdIntervalSecs = stats.getActionIntervalSeconds();

        this.attackTimer = 0;
        this.sunProductionTimer = 0;
        this.sunPending = false;
        this.pendingProjectiles = new ArrayList<>();
        this.lifespanTicks = -1;
        this.instantActivated = false;
        this.shroomStage = 1;
        this.kiwiStage = 1;
        this.stageTimer = 0;
        this.sunProducedCount = 0;
        initializeForType();

        // تنظیم armed برای گیاهان انفجاری
        String cat = stats.getCategory();
        this.armed = cat.equals("Explosive") && type != PlantType.POTATO_MINE
                && type != PlantType.PRIMAL_POTATO_MINE
                && type != PlantType.CHERRY_BOMB
                && type != PlantType.JALAPENO;

        if (type == PlantType.POTATO_MINE || type == PlantType.PRIMAL_POTATO_MINE) {
            this.armed = false;
            this.armTimer = 140; // 14 ثانیه
        }
    }
    // ─────────────────────────────────────────────────────────────
    //  مقداردهی اولیه بر اساس نوع گیاه
    // ─────────────────────────────────────────────────────────────
    private void initializeForType() {
        switch (type) {
            case POTATO_MINE:
                armed = false; armTimer = 150; break;     // ۱۵ ثانیه
            case PRIMAL_POTATO_MINE:
                armed = false; armTimer = 50;  break;     // ۵ ثانیه
            case SEA_SHROOM:
            case PUFF_SHROOM:
                lifespanTicks = 600; break;               // ۶۰ ثانیه عمر
            case CITRON:
                chargeTimer = 90; charged = false; break; // ۹ ثانیه شارژ
            case ELECTRIC_BLUEBERRY:
            case CAULIPOWER:
                chargeTimer = 120; charged = false; break;// ۱۲ ثانیه شارژ
            case BOWLING_BULB:
                chargeTimer = 20; charged = false; break;
            case MAGNET_SHROOM:
                magnetCooldown = 100; break;
            default:
                armed = true;
        }
    }

    // ============================================================
    //  TICK
    // ============================================================

    @Override
    public void onTick(int tickCount, GameSession session) {
        // گیاهان آنی (HP=0): یک‌بار فعال شده و از بین می‌روند
        if (!instantActivated && maxHealth == 0) {
            instantActivated = true;
            activateInstantAbility(session);
            currentHealth = 0;
            return;
        }
        if (isFrozen()) {
            tickEffects();
            return;
        }

        // شمارش عمر (Sea-shroom / Puff-shroom)
        if (lifespanTicks > 0) {
            if (--lifespanTicks == 0) { currentHealth = 0; return; }
        }
        tickCooldown();
        tickEffects();
        handleArmTimer();
        handleStageGrowth();
        handleSunProduction();
        handleAttack(session);
        handleSpecialAbilities(session);
    }

    // ─────────────────────────────────────────────────────────────
    //  گیاهان آنی (HP=0) — اجرا هنگام کاشت
    // ─────────────────────────────────────────────────────────────
    private void activateInstantAbility(GameSession session) {
        if (session == null) return;
        switch (type) {
            case GOLD_BLOOM:
                session.addSun(375);
                break;
            case CHERRY_BOMB:
                aoeExplosion(session, x, y, 1, baseDamage > 0 ? baseDamage : 1800);
                break;
            case GRAPESHOT:
                aoeExplosion(session, x, y, 1, baseDamage > 0 ? baseDamage : 1800);
                launchGrapeshots(session);
                break;
            case JALAPENO:
                burnEntireRow(session, y, baseDamage > 0 ? baseDamage : 1800);
                break;
            case DOOM_SHROOM:
                doomExplosion(session);
                break;
            case ICE_SHROOM:
                for (Zombie z : session.getActiveZombies()) {
                    z.addEffect(ZombieEffect.FROZEN,  100);
                    z.addEffect(ZombieEffect.CHILLED, 200);
                }
                break;
            case HOT_POTATO:
                Tile hotTile = session.getGameMap().getTile(x, y);
                if (hotTile != null && hotTile.getType() == TileType.ICY_GROUND)
                    hotTile.setType(TileType.CAVE_NORMAL);
                break;
            case GRAVE_BUSTER:
                Tile graveTile = session.getGameMap().getTile(x, y);
                if (graveTile != null && graveTile.isTombstone())
                    graveTile.setType(TileType.EGYPT_NORMAL);
                break;
            // ─── Mint plants: اعمال Plant Food به همه اعضای خانواده ───
            case ENLIGHTEN_MINT:
                applyMintFoodToFamily(session, PlantFamily.SUN_PRODUCER);   break;
            case APPEASE_MINT:
                applyMintFoodToFamily(session, PlantFamily.SHOOTER);        break;
            case ARMA_MINT:
                applyMintFoodToFamily(session, PlantFamily.LOBBER);         break;
            case BOMBARD_MINT:
                applyMintFoodToFamily(session, PlantFamily.EXPLOSIVE);      break;
            case ENFORCE_MINT:
                applyMintFoodToFamily(session, PlantFamily.MELEE);          break;
            case REINFORCE_MINT:
                applyMintFoodToFamily(session, PlantFamily.WALL_NUT);       break;
            case ENCHANT_MINT:
                applyMintFoodToFamily(session, PlantFamily.MODIFIER);       break;
            case PIERCE_MINT:
                applyMintFoodToFamily(session, PlantFamily.STRIKE_THROUGH); break;
            case CATTAIL_MINT:
                applyMintFoodToFamily(session, PlantFamily.HOMING);         break;
            default:
                break;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  تایمر مسلح‌شدن (Potato Mine)
    // ─────────────────────────────────────────────────────────────
    private void handleArmTimer() {
        if ((type == PlantType.POTATO_MINE
                || type == PlantType.PRIMAL_POTATO_MINE) && !armed) {
            armTimer--;
            if (armTimer <= 0) {
                armed = true;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  رشد مرحله‌ای (Sun-shroom / Kiwibeast)
    // ─────────────────────────────────────────────────────────────
    private void handleStageGrowth() {
        if (type == PlantType.SUN_SHROOM && shroomStage < 3) {
            stageTimer++;
            int threshold = shroomStage == 1 ? 240 : 720;
            if (stageTimer >= threshold) { shroomStage++; stageTimer = 0; }
        }
        if (type == PlantType.KIWIBEAST && kiwiStage < 3) {
            stageTimer++;
            if (stageTimer >= 300) { kiwiStage++; stageTimer = 0; }
        }
    }
    // ─────────────────────────────────────────────────────────────
    //  تولید خورشید (همه SUN_PRODUCER ها)
    // ─────────────────────────────────────────────────────────────
    private void handleSunProduction() {
        if (family != PlantFamily.SUN_PRODUCER) return;
        if (type == PlantType.SUN_BEAN)          return; // خورشید فقط هنگام ضربه
        if (sunPending)                           return;
        if (++sunProductionTimer >= 240) {        // هر ۲۴ ثانیه
            sunProductionTimer = 0;
            sunPending = true;
            sunProducedCount++;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  حمله — dispatch به منطق مخصوص هر گیاه
    // ─────────────────────────────────────────────────────────────
    private void handleAttack(GameSession session) {
        // گیاهان تله — فعال‌سازی در handleSpecialAbilities
        if (type == PlantType.POTATO_MINE || type == PlantType.PRIMAL_POTATO_MINE
                || type == PlantType.TANGLE_KELP   || type == PlantType.ICEBERG_LETTUCE
                || type == PlantType.SQUASH)
            return;

        double spd = stats.getAttackSpeed();
        if (spd <= 0 || baseDamage <= 0) return;

        // گیاهان شارژی
        if (type == PlantType.CITRON || type == PlantType.CAULIPOWER
                || type == PlantType.ELECTRIC_BLUEBERRY || type == PlantType.BOWLING_BULB) {
            handleChargeAttack(session);
            return;
        }

        if (++attackTimer >= (int)(10.0 / spd)) {
            attackTimer = 0;
            generateProjectilesForType(session);
        }
    }

    private void handleChargeAttack(GameSession session) {
        if (!charged) {
            if (--chargeTimer <= 0) {
                charged = true;
                chargeTimer = getChargeResetTime();
            }
        } else {
            charged = false;
            fireChargedShot(session);
        }
    }
    private int getChargeResetTime() {
        switch (type) {
            case CITRON:             return 90;
            case CAULIPOWER:
            case ELECTRIC_BLUEBERRY: return 120;
            case BOWLING_BULB:       return 20;
            default:                 return 90;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  تولید پرتابه بر اساس نوع گیاه
    // ─────────────────────────────────────────────────────────────
    private void generateProjectilesForType(GameSession session) {
        int dmg = effectiveDamage();
        switch (type) {
            // ── چندلاینه ──
            case THREEPEATER:
                addProj(ProjectileType.NORMAL,  0, dmg, true);
                addProj(ProjectileType.NORMAL, -1, dmg, true);
                addProj(ProjectileType.NORMAL, +1, dmg, true);
                break;
            case STARFRUIT:
                addProj(ProjectileType.NORMAL,  0, dmg, true);
                addProj(ProjectileType.NORMAL, -1, dmg, true);
                addProj(ProjectileType.NORMAL, +1, dmg, true);
                addProj(ProjectileType.NORMAL, -1, dmg, false);
                addProj(ProjectileType.NORMAL, +1, dmg, false);
                break;
            case ROTOBAGA:
                addProj(ProjectileType.NORMAL, -1, dmg, true);
                addProj(ProjectileType.NORMAL, +1, dmg, true);
                addProj(ProjectileType.NORMAL, -1, dmg, false);
                addProj(ProjectileType.NORMAL, +1, dmg, false);
                break;
            // ── جلو/عقب ──
            case SPLIT_PEA:
                addProj(ProjectileType.NORMAL, 0, dmg, true);
                addProj(ProjectileType.NORMAL, 0, dmg, false);
                addProj(ProjectileType.NORMAL, 0, dmg, false);
                break;
            case BONK_CHOY:
                addProj(ProjectileType.NORMAL, 0, dmg, true);
                addProj(ProjectileType.NORMAL, 0, dmg, false);
                break;
            case WASABI_WHIP:
                addProj(ProjectileType.FIRE, 0, dmg, true);
                addProj(ProjectileType.FIRE, 0, dmg, false);
                break;
            // ── چندتیری ──
            case REPEATER:
                addProj(ProjectileType.NORMAL, 0, dmg, true);
                addProj(ProjectileType.NORMAL, 0, dmg, true);
                break;
            case MEGA_GATLING_PEA:
                for (int i = 0; i < 4; i++)
                    addProj(ProjectileType.NORMAL, 0, dmg, true);
                break;
            // ── ویژه ──
            case SNOW_PEA:
                addProj(ProjectileType.ICE, 0, dmg, true);
                break;
            case FIRE_PEASHOOTER:
                addProj(ProjectileType.FIRE, 0, dmg, true);
                break;
            case GOO_PEASHOOTER:
                addProj(ProjectileType.POISON, 0, dmg, true);
                break;
            case CACTUS:
            case FUME_SHROOM: {
                Projectile p = makeProj(ProjectileType.STRIKE, 0, dmg, true);
                p.setPassesThrough(true);
                pendingProjectiles.add(p);
                break;
            }
            case SEA_SHROOM:
            case PUFF_SHROOM: {
                Projectile p = makeProj(ProjectileType.NORMAL, 0, dmg, true);
                p.setTargetX(x + 3);           // برد کوتاه ۳ خانه
                pendingProjectiles.add(p);
                break;
            }
            case MELON_PULT: {
                Projectile p = makeProj(ProjectileType.LOBBED, 0, dmg, true);
                p.setArc(true); p.setExplodes(true); p.setAoeRadius(1);
                pendingProjectiles.add(p);
                break;
            }
            case WINTER_MELON: {
                Projectile p = makeProj(ProjectileType.ICE, 0, dmg, true);
                p.setArc(true); p.setExplodes(true); p.setAoeRadius(1);
                pendingProjectiles.add(p);
                break;
            }
            case PEPPER_PULT: {
                Projectile p = makeProj(ProjectileType.FIRE, 0, dmg, true);
                p.setArc(true); p.setExplodes(true); p.setAoeRadius(1);
                pendingProjectiles.add(p);
                break;
            }
            case CABBAGE_PULT: {
                Projectile p = makeProj(ProjectileType.LOBBED, 0, dmg, true);
                p.setArc(true);
                pendingProjectiles.add(p);
                break;
            }
            case PHAT_BEET:
                if (session != null)
                    aoeAttack(session, dmg);
                break;
            case KIWIBEAST:
                if (session != null)
                    aoeAttack(session, 15 * kiwiStage);  // ۱۵/۳۰/۴۵
                break;
            case CAT_TAIL:
                if (session != null) generateHomingProjectile(session);
                break;
            default:
                addProj(defaultProjectileType(), 0, dmg, true);
                break;
        }
    }

    /** شات شارژی — Citron / Electric Blueberry / Caulipower / Bowling Bulb */
    private void fireChargedShot(GameSession session) {
        int dmg = effectiveDamage();
        switch (type) {
            case CITRON: {
                Projectile p = makeProj(ProjectileType.NORMAL, 0, dmg, true);
                p.setSpeed(5.0);
                pendingProjectiles.add(p);
                break;
            }
            case ELECTRIC_BLUEBERRY:
                if (session != null) {
                    List<Zombie> zombies = session.getActiveZombies();
                    if (!zombies.isEmpty()) {
                        zombies.get(RandomUtil.between(0, zombies.size()-1))
                                .setCurrentHealth(0);
                    }
                }
                break;
            case CAULIPOWER:
                if (session != null) {
                    List<Zombie> zombies = session.getActiveZombies();
                    if (!zombies.isEmpty()) {
                        Zombie t = zombies.get(RandomUtil.between(0, zombies.size()-1));
                        t.setHypnotized(true);
                        t.setMovingBackward(true);
                    }
                }
                break;
            case BOWLING_BULB:
                // سه نوع پیاز با آسیب‌های متفاوت در لاین‌های مجاور
                addProj(ProjectileType.ICE,    -1, dmg,       true);
                addProj(ProjectileType.NORMAL,  0, dmg * 2,   true);
                addProj(ProjectileType.FIRE,   +1, dmg * 3,   true);
                break;
            default:
                addProj(defaultProjectileType(), 0, dmg, true);
        }
    }

    private void generateHomingProjectile(GameSession session) {
        Zombie target = null;
        double minX = Double.MAX_VALUE;
        for (Zombie z : session.getActiveZombies()) {
            if (z.isAlive() && z.getX() < minX) { minX = z.getX(); target = z; }
        }
        int targetLane = (target != null) ? target.getY() : y;
        Projectile p = makeProj(ProjectileType.NORMAL, targetLane - y, effectiveDamage(), true);
        p.setHoming(true);
        pendingProjectiles.add(p);
    }

    /** حمله مساحتی (Phat Beet، Kiwibeast) */
    private void aoeAttack(GameSession session, int dmg) {
        for (Zombie z : session.getActiveZombies())
            if (Math.abs(z.getX() - x) <= 1 && Math.abs(z.getY() - y) <= 1)
                z.takeDamage(dmg);
    }

    // ─────────────────────────────────────────────────────────────
    //  توانایی‌های دوره‌ای ویژه
    // ─────────────────────────────────────────────────────────────
    private void handleSpecialAbilities(GameSession session) {
        if (session == null) return;
        switch (type) {
            case POTATO_MINE:
            case PRIMAL_POTATO_MINE: handlePotatoMine(session); break;
            case SQUASH:             handleSquash(session);      break;
            case ICEBERG_LETTUCE:    handleIcebergLettuce(session); break;
            case TANGLE_KELP:        handleTangleKelp(session);  break;
            case MAGNET_SHROOM:      handleMagnetShroom(session); break;
            case SWEET_POTATO:       handleSweetPotato(session);  break;
            default: break;
        }
    }

    /** Potato Mine: منفجر شدن هنگام تماس زامبی */
    private void handlePotatoMine(GameSession session) {
        if (!armed) return;
        for (Zombie z : session.getActiveZombies()) {
            if (!z.isAlive()) continue;
            if (z.getY() == y && Math.abs(z.getX() - x) < 0.6) {
                int radius = (type == PlantType.PRIMAL_POTATO_MINE) ? 1 : 0;
                int dmg    = (type == PlantType.PRIMAL_POTATO_MINE) ? 2400 : 1800;
                aoeExplosion(session, x, y, radius, dmg);
                currentHealth = 0;
                return;
            }
        }
    }

    /** Squash: له‌کردن اولین زامبی مجاور */
    private void handleSquash(GameSession session) {
        for (Zombie z : session.getActiveZombies()) {
            if (z.isAlive() && z.getY() == y && Math.abs(z.getX() - x) <= 1) {
                z.setCurrentHealth(0);
                currentHealth = 0;
                return;
            }
        }
    }

    /** Iceberg Lettuce: تله یخ — اولین زامبی روی آن را یخ می‌زند */
    private void handleIcebergLettuce(GameSession session) {
        for (Zombie z : session.getActiveZombies()) {
            if (z.isAlive() && z.getY() == y && Math.abs(z.getX() - x) < 0.6) {
                for (Zombie other : session.getActiveZombies())
                    if (other.getY() == y && Math.abs(other.getX() - x) <= 2) {
                        other.addEffect(ZombieEffect.FROZEN,  150);
                        other.addEffect(ZombieEffect.CHILLED, 300);
                    }
                currentHealth = 0;
                return;
            }
        }
    }

    /** Tangle Kelp: کشیدن زامبی آبی به زیر آب */
    private void handleTangleKelp(GameSession session) {
        Tile myTile = session.getGameMap().getTile(x, y);
        if (myTile == null || !myTile.isWater()) return;
        for (Zombie z : session.getActiveZombies()) {
            if (z.isAlive() && z.getY() == y && Math.abs(z.getX() - x) < 0.6) {
                z.setCurrentHealth(0);
                currentHealth = 0;
                return;
            }
        }
    }

    /** Magnet-shroom: جذب فلز از زامبی‌های مجاور */
    private void handleMagnetShroom(GameSession session) {
        if (--magnetCooldown > 0) return;
        magnetCooldown = 100;
        for (Zombie z : session.getActiveZombies()) {
            if (z.isAlive() && z.getY() == y && Math.abs(z.getX() - x) <= 3) {
                if (z.hasArmor(ArmorType.HELMET)) { z.removeArmor(ArmorType.HELMET); return; }
                if (z.hasArmor(ArmorType.BUCKET)) { z.removeArmor(ArmorType.BUCKET); return; }
            }
        }
    }

    /** Sweet Potato: جذب زامبی‌های لاین مجاور */
    private void handleSweetPotato(GameSession session) {
        if (++sweetPotatoTimer < 20) return;
        sweetPotatoTimer = 0;
        int rows = session.getGameMap().getRows();
        for (Zombie z : session.getActiveZombies()) {
            if (z.isAlive() && Math.abs(z.getY() - y) == 1 && !z.isHypnotized()) {
                if (z.getY() >= 1 && z.getY() <= rows) {
                    z.setY(y);
                    z.setLane(y);
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  هوک‌های تعامل با زامبی
    // ─────────────────────────────────────────────────────────────
    @Override
    public boolean onZombieAttack(Zombie zombie, GameSession session) {
        switch (type) {
            case GARLIC:
                redirectZombieToAdjacentLane(zombie, session);
                return false;   // هنوز آسیب می‌بیند تا تمام شود

            case ENDURIAN:
                zombie.takeDamage(Math.max(1, zombie.getDamagePerSecond() / 10));
                return false;

            case SUN_BEAN:
                session.addSun(5);
                return false;

            default:
                return false;
        }
    }

    private void redirectZombieToAdjacentLane(Zombie zombie, GameSession session) {
        int rows = session.getGameMap().getRows();
        int newLane = (y < rows) ? y + 1 : y - 1;
        if (newLane >= 1 && newLane <= rows) {
            zombie.setY(newLane);
            zombie.setLane(newLane);
            zombie.setAttacking(false);
        }
    }

    @Override
    public void onPlantDestroyed(Zombie killer, GameSession session) {
        switch (type) {
            case EXPLODE_O_NUT:
                aoeExplosion(session, x, y, 1, 1800);
                break;
            case HYPNO_SHROOM:
                if (killer != null) {
                    killer.setHypnotized(true);
                    killer.setMovingBackward(true);
                }
                break;
            default:
                break;
        }
    }


    // ─────────────────────────────────────────────────────────────
    //  سازنده‌های کمکی پرتابه
    // ─────────────────────────────────────────────────────────────
    private Projectile makeProj(ProjectileType pt, int yOff, int dmg, boolean right) {
        Projectile p = new Projectile(pt, x, y, dmg);
        p.setMovingRight(right);
        p.setYOffset(yOff);
        return p;
    }

    private void addProj(ProjectileType pt, int yOff, int dmg, boolean right) {
        pendingProjectiles.add(makeProj(pt, yOff, dmg, right));
    }

    private ProjectileType defaultProjectileType() {
        if (hasTag(PlantTag.ICE))    return ProjectileType.ICE;
        if (hasTag(PlantTag.FIRE))   return ProjectileType.FIRE;
        if (hasTag(PlantTag.POISON)) return ProjectileType.POISON;
        String cat = stats.getCategory();
        if ("Lobber".equals(cat))        return ProjectileType.LOBBED;
        if ("StrikeThrough".equals(cat)) return ProjectileType.STRIKE;
        return ProjectileType.NORMAL;
    }

    private int effectiveDamage() {
        int dmg = baseDamage + (level - 1) * 5;
        return boosted ? (int)(dmg * 1.5) : dmg;
    }

    // ─────────────────────────────────────────────────────────────
    //  انفجارها / اثرات مساحتی
    // ─────────────────────────────────────────────────────────────
    private void aoeExplosion(GameSession session, int cx, int cy, int radius, int dmg) {
        if (session == null) return;
        for (Zombie z : session.getActiveZombies())
            if (Math.abs(z.getX() - cx) <= radius && Math.abs(z.getY() - cy) <= radius)
                z.takeDamage(dmg);
    }

    private void burnEntireRow(GameSession session, int row, int dmg) {
        for (Zombie z : session.getActiveZombies())
            if (z.getY() == row) z.takeDamage(dmg);
    }

    private void doomExplosion(GameSession session) {
        int dmg = baseDamage > 0 ? baseDamage : 5000;
        for (Zombie z : session.getActiveZombies()) z.takeDamage(dmg);
        // ایجاد گودال (تایل غیر قابل کشت)
        Tile t = session.getGameMap().getTile(x, y);
        if (t != null) t.setType(TileType.ICY_GROUND);
    }

    private void launchGrapeshots(GameSession session) {
        List<Zombie> zombies = session.getActiveZombies();
        if (zombies.isEmpty()) return;
        int grapeDmg = 40;
        for (int i = 0; i < 8; i++)
            zombies.get(i % zombies.size()).takeDamage(grapeDmg);
    }

    private void applyMintFoodToFamily(GameSession session, PlantFamily targetFamily) {
        for (int r = 1; r <= session.getGameMap().getRows(); r++)
            for (int c = 1; c <= session.getGameMap().getCols(); c++) {
                Tile t = session.getGameMap().getTile(c, r);
                if (t != null && t.getPlant() != null
                        && t.getPlant().getFamily() == targetFamily)
                    t.getPlant().activatePlantFood(session);
            }
    }

    public void   setArmed(boolean b) { armed = b; }
    public int    getShroomStage()    { return shroomStage; }
    public int    getKiwiStage()      { return kiwiStage; }

    @Override
    public String getDescription() {
        String ab = stats.getBaseAbility();
        return (ab != null && !ab.isEmpty()) ? ab : type.name();
    }

    /** برگرداندن دوره تولید خورشید بر حسب تیک (با در نظر گرفتن ارتقا). */
    private int getSunProductionPeriodTicks() {
        double baseSecs = effectiveSunProdIntervalSecs;
        if (type == PlantType.SUN_SHROOM) {
            // رشد تدریجی SunShroom
            if (sunProducedCount < 5) {
                baseSecs = effectiveSunProdIntervalSecs + 0;
            } else if (sunProducedCount < 10) {
                baseSecs = Math.max(effectiveSunProdIntervalSecs - 4, 12.0);
            } else {
                baseSecs = Math.max(effectiveSunProdIntervalSecs - 8, 10.0);
            }
        }
        return (int)(baseSecs * 10);
    }

    private void handleAttack() {
        if (effectiveAttackSpeed <= 0 || effectiveDamage <= 0) {
            return;
        }
        attackTimer++;
        int attackPeriodTicks = (int)(10.0 / effectiveAttackSpeed);
        if (attackTimer >= attackPeriodTicks) {
            attackTimer = 0;
            generateProjectile();
        }
    }

    private void generateProjectile() {
        ProjectileType pType = getProjectileType();
        int dmg = getCurrentEffectiveDamage();
        Projectile proj = new Projectile(pType, x, y, dmg);
        applyProjectileProperties(proj);
        proj.setMovingRight(true); // گیاهان به سمت راست (ستون بالاتر) شلیک می‌کنند
        pendingProjectiles.add(proj);
    }

    private ProjectileType getProjectileType() {
        if (hasTag(PlantTag.ICE)) return ProjectileType.ICE;
        if (hasTag(PlantTag.FIRE)) return ProjectileType.FIRE;
        if (hasTag(PlantTag.POISON)) return ProjectileType.POISON;
        String cat = stats.getCategory();
        if (cat.equals("Lobber")) return ProjectileType.LOBBED;
        if (cat.equals("StrikeThrough")) return ProjectileType.STRIKE;
        return ProjectileType.NORMAL;
    }

    private void applyProjectileProperties(Projectile proj) {
        String cat = stats.getCategory();
        if (cat.equals("Lobber")) proj.setArc(true);
        if (cat.equals("StrikeThrough")) proj.setPassesThrough(true);
        if (bonusPierce > 0) proj.setPassesThrough(true);
    }

    /** آسیب مؤثر با احتساب ارتقا و بوست */
    public int getCurrentEffectiveDamage() {
        int dmg = effectiveDamage;
        if (boosted) dmg = (int)(dmg * 1.5);
        return dmg;
    }

    // ============================================================
    //  PLANT FOOD — به PlantFoodEffectHandler واگذار می‌شود
    // ============================================================

    @Override
    public void activatePlantFood(GameSession session) {
        switch (type) {
            // تولیدکنندگان خورشید: تولید فوری انبوه
            case SUNFLOWER: case TWIN_SUNFLOWER:
            case PRIMAL_SUNFLOWER: case SUN_SHROOM:
                sunPending = true;
                sunProducedCount += 3;   // نشانه تولید اضافی
                break;

            // دیوارها: ترمیم کامل HP
            case WALL_NUT: case TALL_NUT: case ENDURIAN:
            case SWEET_POTATO: case SUN_BEAN: case PUMPKIN:
                currentHealth = maxHealth;
                break;

            // شلیک سریع ۵ تایی
            default:
                for (int i = 0; i < 5; i++)
                    generateProjectilesForType(null);
                break;
        }
        // اگر session null بود (مثلاً test)، minimal اثر
        com.pvz2.service.PlantFoodEffectHandler.apply(type, this, session);
    }

    /**
     * فعال‌سازی Plant Food با ارجاع به ConsoleView برای خروجی پیام‌ها.
     * از GameService فراخوانده می‌شود که به com.pvz2.view دسترسی دارد.
     */
    public void activatePlantFood(GameSession session, ConsoleView view) {
        switch (type) {
            case SUNFLOWER: case TWIN_SUNFLOWER:
            case PRIMAL_SUNFLOWER: case SUN_SHROOM:
                sunPending = true;
                sunProducedCount += 3;
                break;
            case WALL_NUT: case TALL_NUT: case ENDURIAN:
            case SWEET_POTATO: case SUN_BEAN: case PUMPKIN:
                currentHealth = maxHealth;
                break;
            default:
                for (int i = 0; i < 5; i++)
                    generateProjectilesForType(null);
                break;
        }
        com.pvz2.service.PlantFoodEffectHandler.apply(type, this, session);
    }

    // ============================================================
    //  PUBLIC METHODS برای PlantFoodEffectHandler
    // ============================================================

    /** اضافه کردن پرتابه از خارج (plant food) */
    public void addPendingProjectile(Projectile proj) {
        pendingProjectiles.add(proj);
    }

    /** ساخت پرتابه با نوع و آسیب مشخص از موقعیت این گیاه */
    public Projectile buildProjectile(ProjectileType pType, int damage) {
        Projectile p = new Projectile(pType, x, y, damage);
        applyProjectileProperties(p);
        if (pType == ProjectileType.LOBBED) p.setArc(true);
        p.setMovingRight(true);
        return p;
    }

    /** بازیابی کامل HP (برای plant food روی WallNut‌ها) */
    public void healToFull() {
        currentHealth = maxHealth;
    }

    /** اضافه کردن HP موقت به عنوان زره */
    public void addTemporaryArmor(int amount) {
        maxHealth += amount;
        currentHealth += amount;
    }

    // ============================================================
    //  UPGRADE SETTERS — صدا زده توسط PlantUpgradeHandler
    // ============================================================

    public void addBonusDamage(int amount) {
        effectiveDamage = Math.max(0, effectiveDamage + amount);
        baseDamage = effectiveDamage;
    }

    public void addBonusHp(int amount) {
        maxHealth += amount;
        currentHealth = Math.min(currentHealth + amount, maxHealth);
    }

    public void reduceSunCost(int amount) {
        sunCost = Math.max(0, sunCost - amount);
    }

    public void reduceCooldown(double seconds) {
        rechargeTime = Math.max(0.5, rechargeTime - seconds);
    }

    public void reduceSunProdInterval(double seconds) {
        effectiveSunProdIntervalSecs = Math.max(5.0, effectiveSunProdIntervalSecs - seconds);
    }

    public void increaseAttackSpeedPct(double pct) {
        effectiveAttackSpeed *= (1.0 + pct / 100.0);
    }

    public void reduceArmTime(double seconds) {
        armTimer = Math.max(10, armTimer - (int)(seconds * 10));
    }

    public void reduceDigestTime(double seconds) {
        // Chomper: زمان هضم در GameService کنترل می‌شود
        // این فیلد در آینده می‌تواند استفاده شود
    }

    public void addBonusAoeDamage(int amount) { bonusAoeDamage += amount; }
    public void addBonusRange(int tiles) { bonusRange += tiles; }
    public void addBonusLifespan(int ticks) { bonusLifespanTicks += ticks; }
    public void addBonusChillTime(int ticks) { bonusChillTimeTicks += ticks; }
    public void addBonusPierce(int amount) { bonusPierce += amount; }
    public void addBonusBounces(int amount) { bonusBounces += amount; }
    public void addBonusTargets(int amount) { bonusTargets += amount; }
    public void addBonusMaxSize(int amount) { bonusMaxSize += amount; }
    public void addBonusSpecialChance(double pct) { bonusSpecialChancePct += pct; }
    public void addBonusSunAmount(int amount) { bonusSunAmount += amount; }
    public void setDoubleSunChance(boolean v) { doubleSunChance = v; }
    public void setCanCrushTwice(boolean v) { canCrushTwice = v; }
    public void setTargetPriorityUp(boolean v) { targetPriorityUp = v; }

    // ============================================================
    //  SUN PRODUCTION — interface برای GameService
    // ============================================================

    public boolean isSunPending() { return sunPending; }

    public void collectSun() {
        sunPending = false;
        sunProducedCount++;
    }

    /** مقدار خورشید تولیدشده (با احتساب ارتقاها) */
    public int getSunProductionAmount() {
        int base;
        if (type == PlantType.TWIN_SUNFLOWER) {
            base = 50;
        } else if (type == PlantType.SUN_SHROOM) {
            if (sunProducedCount < 5) base = 15;
            else if (sunProducedCount < 10) base = 20;
            else base = 25;
        } else {
            base = 25;
        }
        base += bonusSunAmount;
        if (doubleSunChance && Math.random() < 0.3) {
            base *= 2;
        }
        return base;
    }

    // ============================================================
    //  MISC GETTERS
    // ============================================================

    public boolean isArmed() { return armed; }
    public int getEffectiveRange() { return stats.getRange() + bonusRange; }
    public int getBonusAoeDamage() { return bonusAoeDamage; }
    public int getBonusChillTimeTicks() { return bonusChillTimeTicks; }
    public PlantStats getStats() { return stats; }

    public List<Projectile> pollPendingProjectiles() {
        List<Projectile> result = new ArrayList<>(pendingProjectiles);
        pendingProjectiles.clear();
        return result;
    }
}
