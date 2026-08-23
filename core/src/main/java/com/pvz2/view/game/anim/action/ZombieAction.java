package com.pvz2.view.game.anim.action;

import com.pvz2.model.zombies.Zombie;

import java.util.Map;

/**
 * رابط برای action های قابلیت ویژه زامبی.
 *
 * هر action یک رفتار Game Logic محور را پیاده‌سازی می‌کند که
 * هنگام اجرای special ability فعال می‌شود.
 *
 * ─── اصل طراحی ──────────────────────────────────────────────────
 * Action ها نباید مستقیماً state بازی را تغییر دهند.
 * باید از طریق یک EventBus یا callback به GameController درخواست بدهند.
 * این الگو باعث می‌شود View و Model کاملاً از هم جدا بمانند.
 *
 * ─── جریان اجرا ─────────────────────────────────────────────────
 * <pre>
 *   // در ZombieAnimController.fireAbility():
 *   ZombieAction action = registry.getAction(sa.action);
 *   if (action != null && action.canFire(zombie)) {
 *       action.onStart(zombie, sa.actionParams);
 *   }
 *
 *   // پس از پایان انیمیشن special:
 *   action.onEnd(zombie);
 * </pre>
 *
 * ─── پیاده‌سازی‌های موجود ────────────────────────────────────────
 *   SpawnImpAction    → Gargantuar یک Imp پرت می‌کند
 *   SpawnZombieAction → زامبی جدیدی اسپان می‌کند (Chicken Wrangler)
 *   SummonGraveAction → سنگ قبر ایجاد می‌کند (Necromancer)
 *   CollectSunAction  → خورشید نزدیک را می‌دزدد (Sunflower Zombie)
 *   BurnPlantAction   → لاین را می‌سوزاند (Fire Knight)
 *   TransformAction   → به نوع دیگری تبدیل می‌شود (Newspaper Zombie)
 */
public interface ZombieAction {

    /**
     * شناسه منحصربه‌فرد این action.
     * باید با فیلد "action" در SpecialAbilityConfig مطابقت داشته باشد.
     *
     * مثال‌ها: "SPAWN_IMP"، "SPAWN_ZOMBIE"، "SUMMON_GRAVE"
     */
    String getActionId();

    /**
     * هنگام شروع اجرای ability صدا زده می‌شود.
     * معمولاً همزمان با شروع انیمیشن SPECIAL.
     *
     * @param zombie  زامبی صاحب این ability
     * @param params  پارامترهای از SpecialAbilityConfig.actionParams
     */
    void onStart(Zombie zombie, Map<String, String> params);

    /**
     * هنگام پایان انیمیشن ability صدا زده می‌شود.
     * برای clean‌up یا اثرات نهایی استفاده می‌شود.
     *
     * @param zombie  زامبی صاحب این ability
     */
    void onEnd(Zombie zombie);

    /**
     * آیا این action می‌تواند در حال حاضر اجرا شود؟
     * اگر false برگرداند، ability fire نمی‌شود (و به firedAbilities هم اضافه نمی‌شود).
     *
     * استفاده: جلوگیری از اجرای ability در شرایط غیرمنطقی.
     * مثال: Gargantuar نمی‌تواند Imp بیندازد اگر هیچ Imp در دسترس نباشد.
     *
     * پیاده‌سازی پیش‌فرض: همیشه true
     */
    default boolean canFire(Zombie zombie) {
        return true;
    }

    // ─── utility methods برای parse کردن params ─────────────────

    default int parseIntParam(Map<String, String> params, String key, int defaultVal) {
        try { return Integer.parseInt(params.getOrDefault(key, String.valueOf(defaultVal)).trim()); }
        catch (Exception e) { return defaultVal; }
    }

    default float parseFloatParam(Map<String, String> params, String key, float defaultVal) {
        try { return Float.parseFloat(params.getOrDefault(key, String.valueOf(defaultVal)).trim()); }
        catch (Exception e) { return defaultVal; }
    }

    default boolean parseBoolParam(Map<String, String> params, String key, boolean defaultVal) {
        String v = params.get(key);
        if (v == null) return defaultVal;
        return Boolean.parseBoolean(v.trim());
    }

    default String parseStringParam(Map<String, String> params, String key, String defaultVal) {
        return params.getOrDefault(key, defaultVal);
    }
}
