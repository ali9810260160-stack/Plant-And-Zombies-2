package com.pvz2.model.zombies;

import com.pvz2.model.GameSession;
import com.pvz2.model.enums.ZombieType;

/**
 * ایمپ — زامبی کوچک و سریع.
 */
public class ImpZombie extends Zombie {

    public ImpZombie(ZombieType type, double speed, int dps) {
        super(type, 100, dps, speed, 50);
    }

    @Override
    public void onTick(int tickCount, GameSession session) {
        tickEffects();
        if (type == ZombieType.DRAGON_IMP) {
            removeEffect(com.pvz2.model.enums.ZombieEffect.CHILLED);
        }
    }

    @Override
    public String getDescription() {
        if (type == ZombieType.DRAGON_IMP) {
            return "Dragon Imp: Like Imp but immune to fire attacks!";
        }
        return "Imp: Small and fast zombie. Thrown by Gargantuar!";
    }
}
