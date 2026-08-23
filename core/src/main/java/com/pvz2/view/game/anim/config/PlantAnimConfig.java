package com.pvz2.view.game.anim.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * تنظیمات کامل انیمیشن یک نوع گیاه.
 *
 * کلیدهای استاندارد در states map:
 *   "IDLE"       — حالت پیش‌فرض (الزامی)
 *   "ATTACK"     — شلیک/تولید (اگر گیاه حمله دارد)
 *   "FREEZE_1"   — یخ‌زدگی سطح ۱ (اختیاری)
 *   "FREEZE_2"   — یخ‌زدگی سطح ۲ (اختیاری)
 *   "FREEZE_3"   — یخ‌زدگی کامل (اختیاری)
 *   "PLANT_FOOD" — حالت plant food (اگر گیاه آن را دارد)
 *   "SQUASH_WARN"— هشدار قبل از حمله Squash (فقط Squash)
 *   "DYING"      — انیمیشن مرگ (الزامی)
 *
 * مثال JSON:
 * <pre>
 * "SUNFLOWER": {
 *   "pamPath": "PLANTS/SUNFLOWER/SUNFLOWER.PAM",
 *   "scaleX": 0.85,
 *   "scaleY": 0.85,
 *   "states": {
 *     "IDLE":       { "clip": "idle",       "loop": true },
 *     "ATTACK":     { "clip": "produce_sun","loop": false, "returnTo": "IDLE" },
 *     "PLANT_FOOD": { "clip": "plant_food", "loop": false, "returnTo": "IDLE" },
 *     "DYING":      { "clip": "death",      "loop": false }
 *   },
 *   "projectiles": []
 * }
 * </pre>
 */
public class PlantAnimConfig {

    /** نام نوع گیاه (PlantType.name()) — توسط loader پر می‌شود */
    public String plantType = "";

    /**
     * مسیر PAM از root asset directory.
     * مثال: "PLANTS/PEASHOOTER/PEASHOOTER.PAM"
     *
     * ساختار مورد نیاز در asset dir:
     *   assets/IMAGES/...
     *   assets/ATLASES/...
     *   assets/Resources.json
     */
    public String pamPath = "";

    /**
     * نقشه state → تنظیمات آن state.
     * کلید: نام PlantAnimState (مثلاً "IDLE"، "ATTACK")
     * مقدار: StateConfig مربوطه
     */
    public Map<String, StateConfig> states = new LinkedHashMap<>();

    /**
     * لیست پرتابه‌های این گیاه.
     * یک گیاه می‌تواند چندین نوع پرتابه داشته باشد.
     * مثال: Kernel-pult → [kernel, butter]
     */
    public List<ProjectileAnimConfig> projectiles = new ArrayList<>();

    /**
     * مقیاس X گیاه نسبت به اندازه پیش‌فرض PAM.
     * اکثر گیاهان ~ 0.8 - 1.0 هستند.
     */
    public float scaleX = 1.0f;

    /** مقیاس Y گیاه */
    public float scaleY = 1.0f;

    /**
     * آفست از مرکز سلول (پیکسل مجازی).
     * برای تنظیم دقیق موقعیت گیاه داخل cell استفاده می‌شود.
     * مثال: offsetY=10 گیاه را ۱۰ پیکسل بالاتر از مرکز cell قرار می‌دهد.
     */
    public float offsetX = 0f;
    public float offsetY = 0f;

    /**
     * نام‌های clip آسیب فیزیکی به ترتیب شدت افزایشی — ویژگی جدید
     * بر اساس یافته‌ی واقعی: Wall-nut واقعاً clip های "damage"/"damage2"/
     * "damage3" دارد (جدا از یخ‌زدگی). خالی = این گیاه چنین حالتی ندارد.
     * مثال: ["damage", "damage2", "damage3"]
     */
    public List<String> damageVariants = new ArrayList<>();

    // ─── Helper ─────────────────────────────────────────────────

    /** دریافت StateConfig یک state خاص (null اگر تعریف نشده باشد) */
    public StateConfig getState(String stateName) {
        return states.get(stateName);
    }

    /** آیا این گیاه حالت یخ‌زدگی دارد؟ */
    public boolean hasFreezeStates() {
        return states.containsKey("FREEZE_1")
            || states.containsKey("FREEZE_2")
            || states.containsKey("FREEZE_3");
    }

    /** آیا این گیاه plant food animation دارد؟ */
    public boolean hasPlantFoodAnim() {
        return states.containsKey("PLANT_FOOD");
    }

    /** آیا این گیاه حالت‌های آسیب فیزیکی دارد (مثل Wall-nut)؟ */
    public boolean hasDamageVariants() {
        return !damageVariants.isEmpty();
    }

    /**
     * انتخاب نام clip آسیب مناسب بر اساس نسبت HP فعلی.
     * تقسیم مساوی بین تعداد variant های موجود؛ هرچه hpRatio کمتر،
     * اندیس بالاتر (آسیب بیشتر) انتخاب می‌شود.
     */
    public String getDamageClipForRatio(float hpRatio) {
        if (damageVariants.isEmpty()) return null;
        int n = damageVariants.size();
        int idx = (int) ((1f - hpRatio) * n);
        idx = Math.max(0, Math.min(n - 1, idx));
        return damageVariants.get(idx);
    }

    /** دریافت اولین ProjectileAnimConfig با شناسه داده‌شده */
    public ProjectileAnimConfig getProjectileConfig(String id) {
        for (ProjectileAnimConfig p : projectiles) {
            if (id.equals(p.id)) return p;
        }
        return null;
    }
}
