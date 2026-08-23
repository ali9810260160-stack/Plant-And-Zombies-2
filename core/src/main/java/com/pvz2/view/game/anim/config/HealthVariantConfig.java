package com.pvz2.view.game.anim.config;

/**
 * تعریف یک variant انیمیشن بر اساس درصد HP زامبی.
 *
 * اکثر زامبی‌های PvZ2 سه حالت بصری دارند:
 *   NORMAL   (50-100% HP): ظاهر کامل
 *   DAMAGED  (25-50%  HP): بعضی اعضا افتاده، ظاهر کثیف‌تر
 *   CRITICAL (0-25%   HP): بیشتر اعضا افتاده، تقریباً اسکلت
 *
 * این variant از طریق clipSuffix پیاده‌سازی می‌شود:
 *   اگر state.clip = "walk" و clipSuffix = "_damaged" باشد:
 *   → نام clip نهایی = "walk_damaged"
 *
 * برای پیدا کردن نام‌های دقیق clip → PVZ Asset Browser را اجرا کنید.
 *
 * مثال JSON:
 * <pre>
 *   "healthVariants": [
 *     { "id": "NORMAL",   "minHpRatio": 0.50, "maxHpRatio": 1.00, "clipSuffix": "" },
 *     { "id": "DAMAGED",  "minHpRatio": 0.25, "maxHpRatio": 0.50, "clipSuffix": "_damaged" },
 *     { "id": "CRITICAL", "minHpRatio": 0.00, "maxHpRatio": 0.25, "clipSuffix": "_critical" }
 *   ]
 * </pre>
 */
public class HealthVariantConfig {

    /** شناسه این variant (برای debug و logging) */
    public String id = "NORMAL";

    /**
     * حداقل نسبت HP (inclusive).
     * مثال: 0.25 یعنی این variant از HP 25% به بالا اعمال می‌شود.
     */
    public float minHpRatio = 0.0f;

    /**
     * حداکثر نسبت HP (inclusive).
     * مثال: 0.50 یعنی این variant تا HP 50% اعمال می‌شود.
     */
    public float maxHpRatio = 1.0f;

    /**
     * پسوند clip که به انتهای نام clip در StateConfig اضافه می‌شود.
     * رشته خالی "" یعنی بدون پسوند (برای حالت NORMAL).
     *
     * مثال‌های واقعی PvZ2:
     *   Normal Zombie NORMAL   → "" (clip: "walk")
     *   Normal Zombie DAMAGED  → "_damaged" (clip: "walk_damaged" — بدون دست)
     *   Normal Zombie CRITICAL → "_critical" (clip: "walk_critical" — بدون دست و سر)
     *
     *   Gargantuar NORMAL   → "" (clip: "walk")
     *   Gargantuar DAMAGED  → "_damaged" (clip: "walk_damaged" — با زخم‌های بیشتر)
     *   (Gargantuar معمولاً فقط ۲ variant دارد)
     */
    public String clipSuffix = "";
}
