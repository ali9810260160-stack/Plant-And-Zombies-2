package com.pvz2.view.game.anim.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * تنظیمات کامل انیمیشن یک نوع زامبی.
 *
 * کلیدهای استاندارد در states map:
 *   "IDLE"    — قبل از شروع موج (الزامی)
 *   "WALK"    — حرکت معمولی (الزامی)
 *   "EAT"     — در حال خوردن گیاه (الزامی برای زامبی‌های زمینی)
 *   "SPECIAL" — قابلیت ویژه (اگر دارد)
 *   "DYING"   — انیمیشن مرگ (الزامی)
 *
 * مثال JSON:
 * <pre>
 * "GARGANTUAR": {
 *   "pamPath": "ZOMBIES/GARGANTUAR/GARGANTUAR.PAM",
 *   "scaleX": 1.5, "scaleY": 1.5,
 *   "states": {
 *     "WALK":    { "clip": "walk",  "loop": true },
 *     "EAT":     { "clip": "smash", "loop": false, "returnTo": "WALK" },
 *     "SPECIAL": { "clip": "throw", "loop": false, "returnTo": "WALK" },
 *     "DYING":   { "clip": "death", "loop": false }
 *   },
 *   "healthVariants": [
 *     { "id": "NORMAL",  "minHpRatio": 0.5, "maxHpRatio": 1.0, "clipSuffix": "" },
 *     { "id": "DAMAGED", "minHpRatio": 0.0, "maxHpRatio": 0.5, "clipSuffix": "_damaged" }
 *   ],
 *   "specialAbilities": [
 *     {
 *       "id": "THROW_IMP",
 *       "trigger": "HP_BELOW",
 *       "triggerParam": 0.5,
 *       "once": true,
 *       "animState": "SPECIAL",
 *       "action": "SPAWN_IMP",
 *       "actionParams": { "x_offset": "-3" }
 *     }
 *   ]
 * }
 * </pre>
 */
public class ZombieAnimConfig {

    /** نام نوع زامبی (ZombieType.name()) — توسط loader پر می‌شود */
    public String zombieType = "";

    /**
     * مسیر PAM از root asset directory.
     * خالی برای زامبی «مجازی» (به baseZombieType نگاه کنید) —
     * مثال: "ZOMBIES/CONEHEAD/CONEHEAD.PAM"
     */
    public String pamPath = "";

    /**
     * ═══ ویژگی جدید نسخه ۲ ═══
     * فقط برای زامبی‌های «مجازی»: کلید یک entry دیگر در همین فایل که این
     * زامبی از آن مشتق می‌شود. یافته کلیدی: CONEHEAD/BUCKETHEAD/KNIGHT/
     * BLOCKHEAD در واقعیت بازی هیچ PAM جداگانه‌ای ندارند — همان زامبی
     * NORMAL هر فصل هستند با یک armor روی سرشان. null برای زامبی‌های
     * مستقل با PAM واقعی.
     * مثال: "NORMAL__ANCIENT_EGYPT"
     */
    public String baseZombieType = null;

    /**
     * ═══ ویژگی جدید نسخه ۲ ═══
     * فقط برای زامبی‌های مجازی: نام یک یا چند ArmorType (جدا با "+")
     * که همیشه باید روی این زامبی فعال باشد.
     * مثال: "CONE" یا "HELMET+SHOULDER_ARMOR" (Knight)
     */
    public String forcedArmor = null;

    /**
     * نقشه state → تنظیمات آن state.
     * کلید: نام ZombieAnimState (مثلاً "WALK"، "EAT"، "SPECIAL")
     */
    public Map<String, StateConfig> states = new LinkedHashMap<>();

    /**
     * لیست health variant ها — به ترتیب از بالاترین HP به پایین‌ترین.
     * اگر خالی باشد، هیچ variant ای اعمال نمی‌شود (clip suffix = "").
     */
    public List<HealthVariantConfig> healthVariants = new ArrayList<>();

    /**
     * نقشه armor → تنظیمات بصری آن زره.
     * کلید: ArmorType.name() (مثلاً "CONE"، "BUCKET"، "HELMET")
     * مقدار: ArmorAnimConfig
     */
    public Map<String, ArmorAnimConfig> armors = new LinkedHashMap<>();

    /**
     * لیست قابلیت‌های ویژه این زامبی.
     * ترتیب مهم است: ابتدا بررسی می‌شوند.
     */
    public List<SpecialAbilityConfig> specialAbilities = new ArrayList<>();

    /**
     * مقیاس زامبی.
     * پیش‌فرض 1.0 (اندازه استاندارد).
     * Gargantuar ~ 1.5، Zombie Chicken ~ 0.9
     */
    public float scaleX = 1.0f;
    public float scaleY = 1.0f;

    /**
     * آفست از موقعیت base.
     * معمولاً offsetX برای تنظیم نقطه pivot زامبی استفاده می‌شود.
     */
    public float offsetX = 0f;
    public float offsetY = 0f;

    // ─── Helpers ─────────────────────────────────────────────────

    /** آیا این یک زامبی «مجازی» (بدون PAM مستقل) است؟ */
    public boolean isVirtual() {
        return baseZombieType != null;
    }

    public StateConfig getState(String stateName) {
        return states.get(stateName);
    }

    public boolean hasSpecialAbilities() {
        return !specialAbilities.isEmpty();
    }

    public boolean hasHealthVariants() {
        return !healthVariants.isEmpty();
    }

    public boolean hasArmor(String armorName) {
        return armors.containsKey(armorName);
    }

    /**
     * دریافت suffix مناسب بر اساس نسبت HP.
     * @param hpRatio نسبت HP فعلی (0.0 - 1.0)
     * @return suffix clip (مثلاً "" یا "_damaged" یا "_critical")
     */
    public String getHealthVariantSuffix(float hpRatio) {
        for (HealthVariantConfig hv : healthVariants) {
            if (hpRatio >= hv.minHpRatio && hpRatio <= hv.maxHpRatio) {
                return hv.clipSuffix;
            }
        }
        return "";
    }

    /**
     * دریافت ID variant بر اساس نسبت HP.
     */
    public String getHealthVariantId(float hpRatio) {
        for (HealthVariantConfig hv : healthVariants) {
            if (hpRatio >= hv.minHpRatio && hpRatio <= hv.maxHpRatio) {
                return hv.id;
            }
        }
        return "NORMAL";
    }
}
