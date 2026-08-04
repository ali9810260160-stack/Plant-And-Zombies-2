package com.pvz2.model.plants;

import com.pvz2.model.GameSession;
import com.pvz2.model.Projectile;
import com.pvz2.model.enums.ProjectileType;

import java.util.ArrayList;
import java.util.List;

/**
 * Pea Pod — تعداد سرهایی که کاشته شده تعیین می‌کند چند نخود شلیک می‌شود (۱ تا ۵).
 * هر بار که Pea Pod را روی خودش بکاری، یک سر اضافه می‌شود.
 */
public class PeaPodPlant extends GenericPlant {

    private int headCount;
    private static final int MAX_HEADS = 5;
    private int attackTimer;
    private List<Projectile> pendingProj;

    public PeaPodPlant(PlantStats stats) {
        super(stats);
        this.headCount = 1;
        this.attackTimer = 0;
        this.pendingProj = new ArrayList<>();
    }

    /** افزودن یک سر (حداکثر ۵) — هنگامی که بازیکن دوباره روی آن Pea Pod می‌کارد */
    public boolean addHead() {
        if (headCount >= MAX_HEADS) return false;
        headCount++;
        return true;
    }

    @Override
    public void onTick(int tickCount, GameSession session) {
        if (isFrozen()) { tickEffects(); return; }
        tickCooldown();
        tickEffects();

        double spd = getStats().getAttackSpeed();
        if (spd > 0 && baseDamage > 0 && ++attackTimer >= (int)(10.0 / spd)) {
            attackTimer = 0;
            // headCount نخود موازی شلیک می‌شود
            for (int i = 0; i < headCount; i++) {
                Projectile p = new Projectile(ProjectileType.NORMAL, x, y,
                        baseDamage + (level - 1) * 5);
                p.setMovingRight(true);
                p.setYOffset(0);
                pendingProj.add(p);
            }
        }
    }

    @Override
    public List<Projectile> pollPendingProjectiles() {
        // ادغام پرتابه‌های داخلی با والد
        List<Projectile> combined = new ArrayList<>(super.pollPendingProjectiles());
        combined.addAll(pendingProj);
        pendingProj.clear();
        return combined;
    }

    @Override
    public String getDescription() {
        return "شلیک " + headCount + " نخود موازی — با هر کاشت مجدد یک سر اضافه می‌شود (حداکثر ۵).";
    }

    public int getHeadCount() { return headCount; }
}
