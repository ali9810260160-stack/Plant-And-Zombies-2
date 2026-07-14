package model.plants;

import model.Projectile;
import model.enums.PlantFamily;
import model.enums.PlantTag;
import model.enums.PlantType;
import model.enums.ProjectileType;

import java.util.ArrayList;
import java.util.List;

/**
 * پیاده‌سازی عمومی گیاه بر اساس داده‌های JSON.
 * این کلاس رفتار اکثر گیاهان را پوشش می‌دهد.
 */
public class GenericPlant extends Plant {

    private final PlantStats stats;
    private int attackTimer;
    private int sunProductionTimer;
    private boolean sunPending;
    private boolean armed;
    private int armTimer;
    private int sunProducedCount;
    private List<Projectile> pendingProjectiles;

    public GenericPlant(PlantStats stats) {
        super(stats.getType(), stats.getFamily(),
              stats.getBaseHp(), stats.getSunCost(), stats.getRechargeTime());
        this.stats = stats;
        this.tags = stats.getTags();
        this.baseDamage = stats.getBaseDamage();
        this.attackTimer = 0;
        this.sunProductionTimer = 0;
        this.sunPending = false;
        this.armed = (stats.getCategory().equals("Explosive")
                      && !stats.getCategory().equals("MeleeAttacker"));
        this.armTimer = 0;
        this.pendingProjectiles = new ArrayList<>();
        if (type == PlantType.POTATO_MINE) {
            armed = false;
            armTimer = 140;
        }
    }

    @Override
    public void onTick(int tickCount) {
        if (isFrozen()) {
            tickEffects();
            return;
        }
        tickCooldown();
        tickEffects();
        handleArmTimer();
        handleSunProduction(tickCount);
        handleAttack(tickCount);
    }

    private void handleArmTimer() {
        if (type == PlantType.POTATO_MINE && !armed) {
            armTimer--;
            if (armTimer <= 0) {
                armed = true;
            }
        }
    }

    private void handleSunProduction(int tick) {
        String category = stats.getCategory();
        if (!category.equals("SunProducer")) {
            return;
        }
        sunProductionTimer++;
        int period = getSunProductionPeriod();
        if (sunProductionTimer >= period && !sunPending) {
            sunPending = true;
            sunProductionTimer = 0;
        }
    }

    private int getSunProductionPeriod() {
        if (type == PlantType.SUN_SHROOM) {
            if (sunProducedCount < 5) {
                return 240;
            } else if (sunProducedCount < 10) {
                return 240;
            }
            return 240;
        }
        return 240;
    }

    private void handleAttack(int tick) {
        if (stats.getAttackSpeed() <= 0 || baseDamage <= 0) {
            return;
        }
        attackTimer++;
        int attackPeriod = (int) (10.0 / stats.getAttackSpeed());
        if (attackTimer >= attackPeriod) {
            attackTimer = 0;
            generateProjectile();
        }
    }

    private void generateProjectile() {
        ProjectileType pType = getProjectileType();
        Projectile proj = new Projectile(pType, x, y, getEffectiveDamage());
        applyProjectileProperties(proj);
        pendingProjectiles.add(proj);
    }

    private ProjectileType getProjectileType() {
        if (hasTag(PlantTag.ICE)) {
            return ProjectileType.ICE;
        }
        if (hasTag(PlantTag.FIRE)) {
            return ProjectileType.FIRE;
        }
        if (hasTag(PlantTag.POISON)) {
            return ProjectileType.POISON;
        }
        String cat = stats.getCategory();
        if (cat.equals("Lobber")) {
            return ProjectileType.LOBBED;
        }
        if (cat.equals("StrikeThrough")) {
            return ProjectileType.STRIKE;
        }
        return ProjectileType.NORMAL;
    }

    private void applyProjectileProperties(Projectile proj) {
        String cat = stats.getCategory();
        if (cat.equals("Lobber")) {
            proj.setArc(true);
        }
        if (cat.equals("StrikeThrough") || proj.getType() == ProjectileType.STRIKE) {
            proj.setPassesThrough(proj.getType() == ProjectileType.STRIKE);
        }
    }

    private int getEffectiveDamage() {
        int dmg = baseDamage + (level - 1) * 5;
        if (boosted) {
            dmg = (int) (dmg * 1.5);
        }
        return dmg;
    }

    @Override
    public void activatePlantFood() {
        String cat = stats.getCategory();
        if (cat.equals("SunProducer")) {
            for (int i = 0; i < 4; i++) {
                pendingProjectiles.add(createSunFoodSun());
            }
        } else if (cat.equals("Shooter") || cat.equals("Lobber")) {
            for (int i = 0; i < 6; i++) {
                generateProjectile();
            }
        } else if (cat.equals("WallNut")) {
            currentHealth = maxHealth;
        }
    }

    private Projectile createSunFoodSun() {
        return new Projectile(ProjectileType.NORMAL, x, y, 0);
    }

    @Override
    public String getDescription() {
        return stats.getDescription();
    }

    public boolean isSunPending() {
        return sunPending;
    }

    public void collectSun() {
        sunPending = false;
        sunProducedCount++;
    }

    public boolean isArmed() {
        return armed;
    }

    public List<Projectile> pollPendingProjectiles() {
        List<Projectile> result = new ArrayList<>(pendingProjectiles);
        pendingProjectiles.clear();
        return result;
    }

    public int getSunProductionAmount() {
        if (type == PlantType.TWIN_SUNFLOWER) {
            return 50;
        }
        if (type == PlantType.SUN_SHROOM) {
            if (sunProducedCount < 5) {
                return 15;
            } else if (sunProducedCount < 10) {
                return 20;
            }
            return 25;
        }
        return 25;
    }

    public PlantStats getStats() {
        return stats;
    }

}
