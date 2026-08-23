package com.pvz2.view.game.anim.action;

import com.badlogic.gdx.Gdx;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * رجیستری مرکزی تمام ZombieAction های موجود.
 *
 * ─── نحوه استفاده ────────────────────────────────────────────────
 * <pre>
 *   // در AnimationSystem.init():
 *   ZombieActionRegistry registry = new ZombieActionRegistry();
 *   registry.register(new SpawnImpAction(gameController));
 *   registry.register(new SpawnZombieAction(gameController));
 *   registry.register(new SummonGraveAction(gameController));
 *   registry.register(new CollectSunAction(gameController));
 *   registry.register(new BurnPlantAction(gameController));
 *   registry.register(new TransformAction(gameController));
 *
 *   // در ZombieAnimController:
 *   ZombieAction action = registry.getAction("SPAWN_IMP");
 *   if (action != null) action.onStart(zombie, params);
 * </pre>
 *
 * ─── قابلیت گسترش ───────────────────────────────────────────────
 * برای زامبی‌های جدید با قابلیت خاص:
 *   ۱. یک کلاس جدید implement ZombieAction بساز
 *   ۲. آن را در اینجا register کن
 *   ۳. در character_animations.json به عنوان "action" ست کن
 *   ← هیچ تغییری در بقیه سیستم نیاز نیست
 */
public class ZombieActionRegistry {

    private static final String TAG = "ZombieActionRegistry";

    private final Map<String, ZombieAction> actions = new LinkedHashMap<>();

    /** ثبت یک action جدید */
    public void register(ZombieAction action) {
        if (action == null || action.getActionId() == null) return;

        String id = action.getActionId();
        if (actions.containsKey(id)) {
            Gdx.app.error(TAG, "Action با ID '" + id + "' قبلاً ثبت شده بود — override می‌شود");
        }
        actions.put(id, action);
    }

    /**
     * دریافت action با شناسه داده‌شده.
     * @return action یا null اگر ثبت نشده باشد
     */
    public ZombieAction getAction(String actionId) {
        if (actionId == null || actionId.isEmpty()) return null;
        ZombieAction action = actions.get(actionId);
        if (action == null) {
            Gdx.app.error(TAG, "Action '" + actionId + "' در registry پیدا نشد");
        }
        return action;
    }

    /** آیا action با این ID ثبت شده؟ */
    public boolean has(String actionId) {
        return actions.containsKey(actionId);
    }

    /** لیست همه action های ثبت‌شده */
    public Collection<ZombieAction> getAllActions() {
        return actions.values();
    }

    /** تعداد action های ثبت‌شده */
    public int size() {
        return actions.size();
    }
}
