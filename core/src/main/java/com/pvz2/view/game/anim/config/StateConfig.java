package com.pvz2.view.game.anim.config;

/**
 * تنظیمات یک حالت (State) در انیمیشن یک موجود.
 *
 * این کلاس مستقیماً از JSON خوانده می‌شود توسط AnimConfigLoader.
 * تمام فیلدها باید public باشند (برای Json libGDX).
 *
 * مثال JSON:
 * <pre>
 *   "ATTACK": {
 *     "clip": "shooting",
 *     "loop": false,
 *     "returnTo": "IDLE",
 *     "speedMul": 1.2
 *   }
 * </pre>
 */
public class StateConfig {

    /**
     * نام clip در فایل PAM.
     * این نام باید دقیقاً با نام clip داخل PAM file مطابقت داشته باشد.
     * برای یافتن نام‌های clip → PVZ Asset Browser استفاده کنید.
     */
    public String clip = "";

    /**
     * آیا انیمیشن loop می‌شود؟
     * true  → انیمیشن بی‌نهایت تکرار می‌شود (IDLE، WALK، EAT)
     * false → یک بار پخش می‌شود، سپس returnTo اجرا می‌شود (ATTACK، PLANT_FOOD، DYING)
     */
    public boolean loop = true;

    /**
     * پس از اتمام انیمیشن غیر-loop، به کدام State برمی‌گردد؟
     * مقدار: نام یک PlantAnimState یا ZombieAnimState (مثلاً "IDLE"، "WALK")
     * null → در آخرین frame می‌ماند (مناسب برای DYING)
     */
    public String returnTo = null;

    /**
     * ضریب سرعت پخش انیمیشن.
     * 1.0 = عادی
     * 2.0 = دو برابر سریع
     * 0.5 = نصف سرعت (برای یخ‌زدگی سطح ۲)
     * 0.0 = متوقف (برای یخ‌زدگی سطح ۳)
     */
    public float speedMul = 1.0f;

    /**
     * رنگ tint به فرمت hex 6 رقمی.
     * null   → بدون تغییر رنگ
     * "88BBFF" → آبی کم‌رنگ (یخ‌زدگی سطح ۱)
     * "66AAFF" → آبی (یخ‌زدگی سطح ۲)
     * "4488FF" → آبی پررنگ (یخ‌زدگی سطح ۳)
     * "FF6622" → نارنجی‌قرمز (آتش)
     *
     * نکته: tint با Color.valueOf(tintColor + "FF") پارس می‌شود.
     */
    public String tintColor = null;

    /**
     * شدت tint (0.0 = بدون tint، 1.0 = رنگ کامل).
     * در blend mode: finalColor = entityColor * (1-tintAlpha) + tint * tintAlpha
     */
    public float tintAlpha = 0.4f;
}
