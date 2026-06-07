package model.zombies;

import model.enums.ZombieType;

/**
 * زامبی معمولی - ساده‌ترین نوع زامبی.
 * با سرعت ثابت حرکت می‌کند و جلوترین گیاه را می‌خورد.
 */
public class NormalZombie extends Zombie {

    public NormalZombie() {
        this.type = ZombieType.NORMAL;
        this.maxHealth = 200;
        this.currentHealth = 200;
        this.moveSpeed = 0.4;  // خانه بر ثانیه
        this.damagePerSecond = 100;
        this.waveCost = 1;
    }

    @Override
    public void onTick(int tickCount) { }

    @Override
    public String getDescription() {
        return "Zombie: A basic zombie. Not very fast, not very tough.";
    }
}
