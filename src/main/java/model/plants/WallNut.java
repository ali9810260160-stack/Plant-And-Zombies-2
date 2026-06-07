package model.plants;

import model.enums.PlantFamily;
import model.enums.PlantTag;
import model.enums.PlantType;

import java.util.List;

/**
 * گیاه گردو - دیوار دفاعی با HP بالا.
 * زامبی‌ها را متوقف می‌کند تا خورده شود.
 */
public class WallNut extends Plant {

    public WallNut() {
        this.type = PlantType.WALL_NUT;
        this.family = PlantFamily.WALL_NUT;
        this.tags = List.of(PlantTag.STACKABLE);
        this.maxHealth = 4000;
        this.currentHealth = 4000;
        this.sunCost = 50;
        this.rechargeTime = 30;
    }

    @Override
    public void onTick(int tickCount) { }

    /** plant food: سلامتی کامل بازیابی می‌شود و یک حلقه دفاعی ایجاد می‌کند */
    @Override
    public void activatePlantFood() { }

    @Override
    public String getDescription() {
        return "Wall-Nut: A sturdy nut with a very hard shell. Use it to block zombies.";
    }
}
