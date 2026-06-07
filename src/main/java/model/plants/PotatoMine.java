package model.plants;

import model.enums.PlantFamily;
import model.enums.PlantTag;
import model.enums.PlantType;

import java.util.List;

/**
 * مین سیب‌زمینی - گیاه تله‌ای انفجاری.
 * پس از 14 ثانیه آماده می‌شود و با نزدیک‌شدن زامبی منفجر می‌شود.
 */
public class PotatoMine extends Plant {

    /** آیا مین آماده انفجار است (پس از 14 ثانیه) */
    private boolean armed;

    /** تیک‌های گذشته از کاشت */
    private int ticksPlanted;

    /** تیک‌های لازم برای آماده شدن (14 ثانیه = 140 تیک) */
    private static final int ARM_TICKS = 140;

    public PotatoMine() {
        this.type = PlantType.POTATO_MINE;
        this.family = PlantFamily.EXPLOSIVE;
        this.tags = List.of(PlantTag.TRAP, PlantTag.EXPLOSIVE);
        this.maxHealth = 1;
        this.currentHealth = 1;
        this.sunCost = 25;
        this.rechargeTime = 30;
        this.armed = false;
    }

    @Override
    public void onTick(int tickCount) { }

    /** plant food: بلافاصله آماده می‌شود و یک انفجار بزرگ‌تر ایجاد می‌کند */
    @Override
    public void activatePlantFood() { }

    /** بررسی می‌کند آیا زامبی روی خانه قرار گرفته و منفجر می‌کند */
    public void triggerIfZombiePresent() { }

    public boolean isArmed() { return armed; }
    public void setArmed(boolean armed) { this.armed = armed; }

    @Override
    public String getDescription() {
        return "Potato Mine: Arms itself after a while, then explodes when a zombie steps on it.";
    }
}
