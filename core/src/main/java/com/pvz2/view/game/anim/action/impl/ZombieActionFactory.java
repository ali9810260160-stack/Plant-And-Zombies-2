package com.pvz2.view.game.anim.action.impl;

import com.pvz2.controller.GameController;
import com.pvz2.view.game.anim.AnimationSystem;
import com.pvz2.view.game.anim.action.ZombieAction;

/**
 * Factory برای ساخت همه ZombieAction های از پیش تعریف‌شده.
 *
 * برای اضافه کردن action جدید:
 *   ۱. یک کلاس جدید در این package بساز که ZombieAction را implement کند
 *   ۲. آن را به آرایه برگشتی createAll() اضافه کن
 *   ۳. در character_animations.json، "action": "YOUR_ACTION_ID" بنویس
 *
 * استفاده در AnimationSystem:
 * <pre>
 *   ZombieActionRegistry registry = new ZombieActionRegistry();
 *   for (ZombieAction action : ZombieActionFactory.createAll(gameController, this)) {
 *       registry.register(action);
 *   }
 * </pre>
 */
public final class ZombieActionFactory {

    private ZombieActionFactory() { }

    /**
     * ساخت همه action های پیش‌فرض.
     *
     * @param gc              نمونه GameController
     * @param animationSystem ═══ پارامتر جدید نسخه ۲ ═══ — برای Action های
     *                        چندمرحله‌ای (SpawnImp برای FLYING، CollectSun/
     *                        HookNearestPlant برای endAbilityFor)
     * @return آرایه‌ای از همه action های آماده‌به‌کار
     */
    public static ZombieAction[] createAll(GameController gc, AnimationSystem animationSystem) {
        return new ZombieAction[] {
            new SpawnImpAction(gc, animationSystem), // "SPAWN_IMP"    → Gargantuar
            new SpawnZombieAction(gc),                // "SPAWN_ZOMBIE" → Chicken Wrangler
            new SummonGraveAction(gc),                 // "SUMMON_GRAVE" → Tomb Raiser
            new CollectSunAction(gc, animationSystem),  // "COLLECT_SUN"  → Ra Zombie
            new BurnPlantAction(gc),                     // "BURN_LANE"    → Dark Knight
            new TransformAction(gc),                      // "TRANSFORM"    → Newspaper Zombie
            // ═══ اکشن‌های جدید نسخه ۲ (کشف‌شده از بررسی مستقیم assets واقعی) ═══
            new TransformPlantToSheepAction(gc),               // Dark Wizard
            new BuffNearbyZombiesAction(gc),                    // King Zombie
            new ThrowIceProjectileAction(gc),                    // Hunter Zombie
            new PushBackPlantAction(gc),                          // Troglobite
            new HookNearestPlantAction(gc, animationSystem),       // Fisherman Zombie
            new TossPlantBehindAction(gc),                          // Octopus Zombie
        };
    }
}
