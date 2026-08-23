package com.pvz2.view.game.anim.config;

/**
 * تنظیمات رندر یک نوع پرتابه.
 *
 * هر گیاه می‌تواند چندین نوع پرتابه داشته باشد.
 * مثال: Kernel-pult → دو پرتابه: butter (isArc=true) و kernel (isArc=true)
 *
 * شناسه (id) باید با یکی از موارد زیر مطابقت داشته باشد:
 *   - ProjectileType.name() → برای گیاهانی که پرتابه generic دارند
 *   - نام دلخواه → برای lookup دستی
 *
 * انواع رندر:
 *   ۱. PAM animation (pamPath + clip) → برای پرتابه‌های انیمیشنی
 *   ۲. Static sprite (texturePath + regionName) → برای پرتابه‌های ساده
 *
 * مثال JSON برای گیاه Peashooter:
 * <pre>
 *   "projectiles": [
 *     {
 *       "id": "NORMAL",
 *       "pamPath": "PLANTS/PEASHOOTER/PEASHOOTER.PAM",
 *       "clip": "pea",
 *       "isArc": false,
 *       "scale": 1.0
 *     }
 *   ]
 * </pre>
 *
 * مثال JSON برای گیاه Cabbage-pult:
 * <pre>
 *   "projectiles": [
 *     {
 *       "id": "LOBBED",
 *       "pamPath": "PLANTS/CABBAGE_PULT/CABBAGE_PULT.PAM",
 *       "clip": "cabbage",
 *       "isArc": true,
 *       "arcHeight": 180.0,
 *       "scale": 1.0
 *     }
 *   ]
 * </pre>
 */
public class ProjectileAnimConfig {

    /**
     * شناسه — برای lookup از PlantAnimConfig.projectiles.
     * معمولاً ProjectileType.name() (مثلاً "NORMAL"، "ICE"، "LOBBED").
     * برای گیاهانی با چند نوع پرتابه می‌تواند نام دلخواه باشد.
     */
    public String id = "";

    // ─── روش رندر ۱: PAM animation ──────────────────────────────

    /** مسیر PAM از root asset dir. null اگر از static sprite استفاده شود. */
    public String pamPath = null;

    /** نام clip در PAM. null اگر static sprite باشد. */
    public String clip = null;

    // ─── روش رندر ۲: Static sprite ──────────────────────────────

    /**
     * مسیر Atlas یا texture برای static sprite.
     * null اگر PAM استفاده شود.
     * مثال: "atlas/plants.atlas"
     */
    public String texturePath = null;

    /**
     * نام region داخل Atlas.
     * مثال: "pea_normal"
     */
    public String regionName = null;

    // ─── ویژگی‌های فیزیکی رندر ────────────────────────────────

    /** ضریب مقیاس نسبت به اندازه پیش‌فرض (1.0 = اندازه اصلی) */
    public float scale = 1.0f;

    /**
     * آیا مسیر سهموی (arc) دارد؟
     * false → حرکت خطی افقی (پرتابه‌های عادی)
     * true  → حرکت سهموی (Kernel-pult، Cabbage-pult، Catapult)
     */
    public boolean isArc = false;

    /**
     * ارتفاع اوج سهمی (پیکسل مجازی).
     * فقط وقتی isArc=true معنا دارد.
     * پیش‌فرض: 150 پیکسل
     */
    public float arcHeight = 150f;

    /**
     * رنگ tint hex (null = بدون tint).
     * مثال: "4488FF" برای پرتابه یخ، "FF4422" برای پرتابه آتش
     */
    public String tintColor = null;

    /**
     * آیا هنگام برخورد با هدف rotate می‌شود؟
     * (برای butter، ball projectile ها)
     */
    public boolean rotates = false;

    /**
     * سرعت rotation (درجه در ثانیه).
     * فقط اگر rotates=true.
     */
    public float rotationSpeed = 180f;
}
