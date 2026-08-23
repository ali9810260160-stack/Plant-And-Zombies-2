package com.pvz2.model.zombies;

import com.pvz2.model.GameSession;
import com.pvz2.model.enums.AnimEvent;
import com.pvz2.model.enums.ZombieType;
import com.pvz2.model.tiles.Tile;

/**
 * زامبیِ گیاهیِ «کدو» (Zombotany squash) — طبق داک فاز ۱:
 * خیلی سریع حرکت می‌کند و اگر به یک گیاه برسد، هم خودش و هم آن گیاه را از بین
 * می‌برد (برخوردِ کشنده‌ی دوطرفه).
 */
public class ZombotanySquash extends Zombie {

    public ZombotanySquash() {
        // سرعتِ بالا (۰.۵ ~ نزدیکِ سریع‌ترین زامبی)، جانِ معمولی.
        super(ZombieType.ZOMBOTANY_SQUASH, 190, 100, 0.5, 150);
    }

    @Override
    public void onTick(int tickCount, GameSession session) {
        tickEffects();
        if (!isAlive()) return;
        int col = (int) Math.ceil(x);
        Tile t = tileAt(session, col);
        if (t == null || t.getPlant() == null) {
            t = tileAt(session, (int) Math.round(x));   // خانه‌ی در حالِ ورود
        }
        if (t != null && t.getPlant() != null) {
            t.setPlant(null);                 // گیاه نابود
            session.setPlantsLost(session.getPlantsLost() + 1);
            setCurrentHealth(0);              // خودِ کدو هم نابود
            fireAnimEvent(AnimEvent.DYING_STARTED);
        }
    }

    private Tile tileAt(GameSession session, int col) {
        if (!session.getGameMap().isValidPosition(col, y)) return null;
        return session.getGameMap().getTile(col, y);
    }

    @Override
    public String getDescription() {
        return "Zombotany Squash: very fast; destroys itself and the plant it reaches.";
    }
}
