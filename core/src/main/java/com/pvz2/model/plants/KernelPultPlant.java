package com.pvz2.model.plants;

import com.pvz2.model.GameSession;
import com.pvz2.model.Projectile;
import com.pvz2.model.enums.ProjectileType;

import java.util.ArrayList;
import java.util.List;

/**
 * Kernel-pult — در تناوب بین دو حالت شلیک می‌کند:
 *  - دانه ذرت: ۲۰ آسیب
 *  - کره: ۴۰ آسیب + Stun
 */
public class KernelPultPlant extends GenericPlant {

    private boolean nextShotIsButter;   // نوبت بعدی کره است؟
    private int attackTimer;
    private List<Projectile> pendingProj;

    private static final int STUN_TICKS = 60;   // ۶ ثانیه Stun از کره

    public KernelPultPlant(PlantStats stats) {
        super(stats);
        this.nextShotIsButter = false;  // اولین شات ذرت است
        this.attackTimer = 0;
        this.pendingProj = new ArrayList<>();
    }

    @Override
    public void onTick(int tickCount, GameSession session) {
        if (isDisabled()) { tickEffects(); return; }
        tickCooldown();
        tickEffects();

        double spd = getStats().getAttackSpeed();
        if (spd > 0 && ++attackTimer >= (int)(10.0 / spd)) {
            attackTimer = 0;
            fireKernelOrButter(session);
            fireAttackTrigger(); // برای انیمیشن
        }
    }

    private void fireKernelOrButter(GameSession session) {
        if (nextShotIsButter) {
            // کره: آسیب ۴۰ + Stun
            Projectile p = new Projectile(ProjectileType.LOBBED, x, y,
                    baseDamage * 2);          // آسیب دو برابر
            p.setMovingRight(true);
            p.setArc(true);
            p.setStunOnHit(true);             // علامت Stun برای CombatService
            pendingProj.add(p);
        } else {
            // ذرت: آسیب پایه
            Projectile p = new Projectile(ProjectileType.LOBBED, x, y, baseDamage);
            p.setMovingRight(true);
            p.setArc(true);
            pendingProj.add(p);
        }
        nextShotIsButter = !nextShotIsButter;  // تغییر نوبت
    }

    @Override
    public List<Projectile> pollPendingProjectiles() {
        List<Projectile> combined = new ArrayList<>(super.pollPendingProjectiles());
        combined.addAll(pendingProj);
        pendingProj.clear();
        return combined;
    }

    @Override
    public String getDescription() {
        return "تناوب بین دانه ذرت (آسیب کم) و کره (آسیب دو برابر + توقف موقت).";
    }

    public boolean isNextShotButter() { return nextShotIsButter; }
}
