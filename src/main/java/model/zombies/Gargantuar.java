package model.zombies;

import model.enums.ZombieType;

/**
 * زامبی غول‌پیکر - قوی‌ترین زامبی معمولی.
 * با یک ضربه گیاه را از بین می‌برد.
 * وقتی به نصف HP رسید، imp را پرتاب می‌کند.
 */
public class Gargantuar extends Zombie {

    /** آیا imp را قبلاً پرتاب کرده */
    private boolean impThrown;

    /** imp پشت این گارگانتوار */
    private ImpZombie carriedImp;

    public Gargantuar() {
        this.type = ZombieType.GARGANTUAR;
        this.maxHealth = 3000;
        this.currentHealth = 3000;
        this.moveSpeed = 0.2;
        this.damagePerSecond = Integer.MAX_VALUE; // یک ضربه
        this.waveCost = 15;
        this.boss = true;
        this.impThrown = false;
        this.carriedImp = new ImpZombie();
    }

    @Override
    public void onTick(int tickCount) { }

    /**
     * imp را به ستون سوم از چپ همان ردیف پرتاب می‌کند.
     * وقتی HP به نصف رسید فراخوانی می‌شود.
     */
    public void throwImp() { }

    public boolean isImpThrown() { return impThrown; }
    public void setImpThrown(boolean impThrown) { this.impThrown = impThrown; }
    public ImpZombie getCarriedImp() { return carriedImp; }

    @Override
    public String getDescription() {
        return "Gargantuar: Smashes plants with a telephone pole. Very tough.";
    }
}
