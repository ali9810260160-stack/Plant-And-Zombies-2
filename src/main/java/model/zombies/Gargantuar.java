package model.zombies;

import model.GameSession;
import model.enums.ArmorType;
import model.enums.ZombieType;

/**
 * زامبی غول‌پیکر — یک ضربه گیاهان را نابود می‌کند، Imp پرتاب می‌کند.
 */
public class Gargantuar extends Zombie {

    private boolean impThrown;

    public Gargantuar() {
        super(ZombieType.GARGANTUAR, 3000, 500, 0.15, 1500);
        this.impThrown = false;
        this.boss = true;
    }

    @Override
    public void onTick(int tickCount, GameSession session) {
        tickEffects();
        if (!impThrown && currentHealth <= maxHealth / 2) {
            throwImp(session);
            impThrown = true;
        }
    }

    private void throwImp(GameSession session) {
        ImpZombie imp = new ImpZombie(ZombieType.IMP, 0.22, 100);
        int impX = Math.max(1, (int) x - 3);
        imp.setX(impX);
        imp.setY(y);
        imp.setLane(y);
        imp.setSpawnWave(spawnWave);
        if (session.getActiveZombies() != null) {
            session.getActiveZombies().add(imp);
        }
    }

    @Override
    public String getDescription() {
        return "Gargantuar: 3000 HP, destroys plants in one hit. "
               + "Throws an Imp when at half health!";
    }
}
