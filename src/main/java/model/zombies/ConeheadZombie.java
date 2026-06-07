package model.zombies;

import model.enums.ArmorType;
import model.enums.ZombieType;

import java.util.LinkedHashMap;

/**
 * زامبی سرمخروطی - مخروط بالای سرش باید اول از بین برود.
 * مخروط 370 HP دارد.
 */
public class ConeheadZombie extends Zombie {

    public ConeheadZombie() {
        this.type = ZombieType.CONEHEAD;
        this.maxHealth = 200;
        this.currentHealth = 200;
        this.moveSpeed = 0.4;
        this.damagePerSecond = 100;
        this.waveCost = 2;
        this.armors = new LinkedHashMap<>();
        this.armors.put(ArmorType.CONE, 370);
    }

    @Override
    public void onTick(int tickCount) { }

    @Override
    public String getDescription() {
        return "Conehead Zombie: His cone adds extra protection. Requires additional firepower.";
    }
}
