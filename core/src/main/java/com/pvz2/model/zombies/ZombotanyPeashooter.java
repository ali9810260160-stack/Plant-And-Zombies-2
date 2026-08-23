package com.pvz2.model.zombies;

import com.pvz2.model.GameSession;
import com.pvz2.model.Projectile;
import com.pvz2.model.enums.ProjectileType;
import com.pvz2.model.enums.ZombieType;
import com.pvz2.model.tiles.Tile;

/**
 * زامبیِ گیاهیِ «تیرانداز» (Zombotany peashooter) — طبق داک فاز ۱:
 * مانند گیاهِ peashooter به جلو (سمتِ چپ) تیر پرتاب می‌کند؛ تیر به سمت چپ حرکت
 * می‌کند تا به یک گیاه برخورد کند و به آن آسیب بزند.
 *
 * <p>از پرتابه‌ی واقعیِ موتور استفاده می‌کند: {@code movingRight=false} +
 * {@code hitsPlants=true} (موتور آن را به چپ می‌برد و به اولین گیاه می‌زند).
 */
public class ZombotanyPeashooter extends Zombie {

    private static final int FIRE_INTERVAL = 15;   // ~۱.۵ ثانیه
    private static final int PEA_DAMAGE     = 20;
    private int fireTimer;

    public ZombotanyPeashooter() {
        super(ZombieType.ZOMBOTANY_PEASHOOTER, 190, 100, 0.185, 150);
    }

    @Override
    public void onTick(int tickCount, GameSession session) {
        tickEffects();
        fireTimer++;
        if (fireTimer % FIRE_INTERVAL != 0) return;
        if (!plantToLeft(session)) return;
        Projectile pea = new Projectile(ProjectileType.NORMAL, x - 0.5, y, PEA_DAMAGE);
        pea.setMovingRight(false);   // به سمتِ گیاهان (چپ)
        pea.setHitsPlants(true);
        session.getActiveProjectiles().add(pea);
    }

    /** آیا گیاهی در سمتِ چپِ این زامبی در همان ردیف هست؟ */
    private boolean plantToLeft(GameSession session) {
        int startCol = (int) Math.floor(x);
        for (int c = startCol; c >= 1; c--) {
            Tile t = session.getGameMap().getTile(c, y);
            if (t != null && t.getPlant() != null) return true;
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "Zombotany Peashooter: shoots peas that damage your plants.";
    }
}
