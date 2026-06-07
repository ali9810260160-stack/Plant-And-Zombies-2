package model.zombies;

import model.enums.ZombieType;

/**
 * ایمپ - زامبی کوچک و سریع.
 * توسط Gargantuar پرتاب می‌شود یا مستقل ظاهر می‌شود.
 */
public class ImpZombie extends Zombie {

    public ImpZombie() {
        this.type = ZombieType.IMP;
        this.maxHealth = 280;
        this.currentHealth = 280;
        this.moveSpeed = 0.8;  // سریع‌تر از زامبی معمولی
        this.damagePerSecond = 200;
        this.waveCost = 3;
    }

    @Override
    public void onTick(int tickCount) { }

    @Override
    public String getDescription() {
        return "Imp: Small but speedy. Thrown by Gargantuars.";
    }
}
