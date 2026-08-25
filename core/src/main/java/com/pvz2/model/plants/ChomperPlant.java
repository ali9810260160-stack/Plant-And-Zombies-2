package com.pvz2.model.plants;

import com.pvz2.model.AppState;
import com.pvz2.model.GameSession;
import com.pvz2.model.zombies.Zombie;

/**
 * Chomper — زامبی مجاور را فوری می‌بلعد (Instant Kill).
 * بعد از بلعیدن، ۴۰ ثانیه (۴۰۰ تیک) زمان هضم دارد و نمی‌تواند بلعد.
 */
public class ChomperPlant extends GenericPlant {

    private boolean digesting;
    private int digestTimer;
    private static final int DIGEST_TICKS = 400;  // ۴۰ ثانیه

    public ChomperPlant(PlantStats stats) {
        super(stats);
        this.digesting = false;
        this.digestTimer = 0;
    }

    @Override
    public void onTick(int tickCount, GameSession session) {
        // شمارش تایمر هضم
        if (digesting) {
            if (--digestTimer <= 0) {
                digesting = false;
                digestTimer = 0;
            }
        }
        super.onTick(tickCount, session);
    }

    /**
     * وقتی زامبی حمله می‌کند: اگر در حال هضم نیست، زامبی را فوری می‌بلعد.
     * @return true = حمله مدیریت شد (Chomper آسیب نمی‌بیند)
     */
    @Override
    public boolean onZombieAttack(Zombie zombie, GameSession session) {
        if (!digesting) {
            // باگ قبلی: setCurrentHealth(0) مستقیم صدا زده می‌شد که از
            // takeDamage() (جایی که رویداد انیمیشن DYING_STARTED ست می‌شود) رد
            // نمی‌شد — یعنی زامبی بلعیده‌شده هیچ‌وقت انیمیشن مرگ نمی‌گرفت.
            zombie.setCurrentHealth(0);
            zombie.fireAnimEvent(com.pvz2.model.enums.AnimEvent.DYING_STARTED);
            digesting = true;
            digestTimer = DIGEST_TICKS;
            fireAttackTrigger(); // برای انیمیشن: لحظه بلعیدن
            return true;                  // آسیبی به Chomper نمی‌رسد
        }
        return false;   // در حال هضم → زامبی به طور عادی حمله می‌کند
    }

    public void activatePlantFood() {
        // Plant Food: سه زامبی مجاور را یکجا می‌بلعد (تایمر هضم ریست می‌شود)
        digesting = false;
        digestTimer = 0;
        super.activatePlantFood(AppState.getInstance().getCurrentSession());
    }

    @Override
    public String getDescription() {
        return "بلعیدن آنی زامبی مجاور — بعد از بلعیدن ۴۰ ثانیه هضم.";
    }

    public boolean isDigesting() { return digesting; }
    public int getDigestTimer()  { return digestTimer; }
}
