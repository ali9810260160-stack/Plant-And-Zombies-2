package model.plants;

import model.enums.PlantFamily;
import model.enums.PlantTag;
import model.enums.PlantType;
import model.enums.ProjectileType;

import java.util.List;

/**
 * گیاه نخودانداز - ساده‌ترین گیاه تیرانداز.
 * هر 1.5 ثانیه یک نخود به سمت زامبی‌ها شلیک می‌کند.
 * تیر از سنگ‌قبر رد نمی‌شود.
 */
public class Peashooter extends Shooter {

    public Peashooter() {
        this.type = PlantType.PEASHOOTER;
        this.family = PlantFamily.SHOOTER;
        this.tags = List.of(PlantTag.PEA, PlantTag.DAY);
        this.maxHealth = 300;
        this.currentHealth = 300;
        this.sunCost = 100;
        this.rechargeTime = 7.5;
        this.projectileType = ProjectileType.NORMAL;
        this.damage = 20;
        this.projectilesPerShot = 1;
        this.attackIntervalTicks = 15; // 1.5 ثانیه
    }

    @Override
    public boolean hasTargetInLane() { return false; }

    @Override
    public void shoot() { }

    /** plant food: تیرهای سریع به تمام زامبی‌های نقشه شلیک می‌کند */
    @Override
    public void activatePlantFood() { }

    @Override
    public void onTick(int tickCount) { }

    @Override
    public String getDescription() {
        return "Peashooter: Shoots peas at zombies. A classic.";
    }
}
