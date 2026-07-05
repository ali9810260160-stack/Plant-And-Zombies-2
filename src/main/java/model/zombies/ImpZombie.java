package model.zombies;

import model.GameSession;
import model.enums.ZombieType;

import java.util.LinkedHashMap;

/**
 * ایمپ - زامبی کوچک و سریع.
 * توسط Gargantuar پرتاب می‌شود یا مستقل ظاهر می‌شود.
 */
public class ImpZombie extends Zombie {

    public ImpZombie(ZombieType zombieType, double speed, int waveCost) {
        this.type = zombieType;
        this.maxHealth = 280;
        this.currentHealth = 280;
        this.moveSpeed = speed;  // سریع‌تر از زامبی معمولی
        this.damagePerSecond = 200;
        this.waveCost = waveCost;
        this.armors = new LinkedHashMap<>();
        this.activeEffects = new LinkedHashMap<>();
    }

    @Override
    public void onTick(int tickCount, GameSession gameSession) {
        return;
    }

    /** ایمپ اژدها در برابر تیرهای آتشین مقاوم است و تاثیری از آن‌ها نمی‌گیرد */
    public boolean isFireImmune() {
        return type == ZombieType.DRAGON_IMP;
    }

    @Override
    public String getDescription() {
        if (type == ZombieType.DRAGON_IMP) {
            return "Dragon Imp: Same as a regular Imp, but immune to fire.";
        }
        return "Imp: Small but speedy. Thrown by Gargantuars.";
    }
}
