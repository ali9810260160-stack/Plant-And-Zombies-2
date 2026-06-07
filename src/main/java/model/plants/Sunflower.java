package model.plants;

import model.enums.PlantFamily;
import model.enums.PlantTag;
import model.enums.PlantType;

import java.util.List;

/**
 * گیاه آفتابگردان - تولیدکننده اصلی خورشید.
 * هر 24 ثانیه 25 واحد خورشید تولید می‌کند.
 */
public class Sunflower extends SunProducer {

    public Sunflower() {
        this.type = PlantType.SUNFLOWER;
        this.family = PlantFamily.SUN_PRODUCER;
        this.tags = List.of(PlantTag.DAY);
        this.maxHealth = 300;
        this.currentHealth = 300;
        this.sunCost = 50;
        this.rechargeTime = 7.5;
        this.sunProduced = 25;
        this.productionIntervalTicks = 240; // 24 ثانیه
    }

    @Override
    public void onTick(int tickCount) { }

    /** plant food: یک ردیف از خورشیدهای بزرگ (50 واحد) تولید می‌کند */
    @Override
    public void activatePlantFood() { }

    @Override
    public int collectSun() { return 0; }

    @Override
    public String getDescription() {
        return "Sunflower: Produces sun for you to plant more plants.";
    }
}
