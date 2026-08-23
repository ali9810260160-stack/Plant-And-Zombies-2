package com.pvz2.model.zombies;

import com.pvz2.model.GameSession;
import com.pvz2.model.enums.ZombieType;
import com.pvz2.model.tiles.Tile;

/**
 * زامبیِ گیاهیِ «فلفل» (Zombotany jalapeno) — طبق داک فاز ۱:
 * اگر بعد از گذشتِ ۱۰ ثانیه از ورودش به باغ نابود نشده باشد، کلِ گیاهانِ ردیفِ
 * خودش را با آتش می‌سوزاند و از بین می‌برد. پس از آن، مثلِ یک زامبیِ عادی ادامه
 * می‌دهد (تهدیدی که باید ظرفِ ۱۰ ثانیه کشته شود).
 */
public class ZombotanyJalapeno extends Zombie {

    private static final int FUSE_TICKS = 100;   // ۱۰ ثانیه (۱۰ تیک بر ثانیه)
    private int age;
    private boolean burned;

    public ZombotanyJalapeno() {
        super(ZombieType.ZOMBOTANY_JALAPENO, 190, 100, 0.185, 150);
    }

    @Override
    public void onTick(int tickCount, GameSession session) {
        tickEffects();
        if (burned) return;
        age++;
        if (age >= FUSE_TICKS) {
            burnRow(session);
            burned = true;
        }
    }

    /** سوزاندنِ همه‌ی گیاهانِ ردیفِ این زامبی. */
    private void burnRow(GameSession session) {
        int cols = session.getGameMap().getCols();
        for (int c = 1; c <= cols; c++) {
            Tile t = session.getGameMap().getTile(c, y);
            if (t != null && t.getPlant() != null) {
                t.getPlant().takeDamage(1_000_000);   // ثبتِ مرگِ گیاه
                t.setPlant(null);
                session.setPlantsLost(session.getPlantsLost() + 1);
            }
        }
    }

    @Override
    public String getDescription() {
        return "Zombotany Jalapeno: burns every plant in its row after 10s.";
    }
}
