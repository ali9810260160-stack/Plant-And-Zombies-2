package com.pvz2.model.enums;

/**
 * رویدادهای انیمیشنی که از لایه منطق (فاز ۱) به لایه View منتقل می‌شوند.
 *
 * مکانیزم:
 *   - CombatService (و سایر Service ها) پس از هر تغییر state مهم،
 *     یک AnimEvent روی entity ست می‌کنند.
 *   - AnimationSystem هر فریم این event را می‌خواند، به controller می‌دهد،
 *     سپس آن را پاک می‌کند (fire-and-forget).
 *   - فقط آخرین event ذخیره می‌شود؛ اگر در یک تیک چند event رخ دهد،
 *     event با بالاترین اولویت باید ست شود.
 *
 * اولویت (عدد بالاتر = مهم‌تر، override می‌کند):
 *   DIED / DYING_STARTED (20) > PLANT_FOOD_ACTIVATED (15) > ATTACK_TRIGGER (10)
 *   > ARMOR_BROKEN (8) > HEALTH_VARIANT_CHANGED (7) > TOOK_DAMAGE (5)
 *   > FREEZE_LEVEL_CHANGED (4) > STARTED_EATING (3) > STOPPED_EATING (2) > NONE (0)
 */
public enum AnimEvent {

    // ────────── گیاهان ──────────────────────────────────────────────────────
    /** گیاه یک پرتابه شلیک کرد — برای trigger کردن انیمیشن ATTACK */
    ATTACK_TRIGGER(10),

    /** Plant Food روی این گیاه فعال شد */
    PLANT_FOOD_ACTIVATED(15),

    /** گیاه آسیب دید — برای اعمال flash effect قرمز */
    TOOK_DAMAGE(5),

    /** سطح یخ‌زدگی گیاه تغییر کرد (incrementFreezeLevel یا thaw) */
    FREEZE_LEVEL_CHANGED(4),

    /** گیاه مُرد — انیمیشن مرگ پخش شود سپس از صحنه حذف شود */
    DIED(20),

    // ────────── زامبی‌ها ────────────────────────────────────────────────────
    /** زامبی شروع به خوردن گیاه کرد */
    STARTED_EATING(3),

    /** زامبی خوردن را متوقف کرد (گیاه از بین رفت یا زامبی حرکت کرد) */
    STOPPED_EATING(2),

    /**
     * یک زره از زامبی جدا شد.
     * این event باید visibility map را trigger کند تا sprite زره hide شود.
     */
    ARMOR_BROKEN(8),

    /** زامبی آسیب دید — flash effect قرمز */
    TOOK_DAMAGE_ZOMBIE(5),

    /**
     * درصد HP به یکی از threshold ها رسید (۵۰٪ یا ۲۵٪).
     * باعث تغییر clip به variant مناسب می‌شود.
     */
    HEALTH_VARIANT_CHANGED(7),

    /** زامبی شروع به مردن کرد — انیمیشن DYING پخش شود */
    DYING_STARTED(20),

    /** یک special ability زامبی فعال شد */
    SPECIAL_ABILITY_FIRED(12),

    /** پایانِ فازِ LOOP یک ability چندمرحله‌ای → پخشِ کلیپِ EXIT (مثلِ power_down تورکوایز) */
    SPECIAL_ABILITY_ENDED(11),

    /** زامبی هیپنوتیزم شد — flip direction */
    HYPNOTIZED(6),

    /** یک status effect اعمال شد (CHILLED, FROZEN, BURNING, ...) */
    EFFECT_APPLIED(4),

    // ────────── بدون رویداد ─────────────────────────────────────────────────
    NONE(0);

    // ────────── اولویت ──────────────────────────────────────────────────────
    public final int priority;

    AnimEvent(int priority) {
        this.priority = priority;
    }

    /**
     * بررسی می‌کند که آیا این event باید event فعلی را override کند.
     * @param current event فعلی روی entity
     * @return true اگر این event اولویت بالاتری دارد
     */
    public boolean overrides(AnimEvent current) {
        return this.priority > current.priority;
    }
}
