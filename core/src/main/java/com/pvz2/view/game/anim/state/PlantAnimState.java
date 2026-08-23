package com.pvz2.view.game.anim.state;

/**
 * حالت‌های انیمیشنی ممکن برای گیاهان.
 *
 * سیستم اولویت:
 *   State Machine همیشه حالتی را نمایش می‌دهد که priority بالاتری دارد.
 *   اگر چند حالت همزمان درخواست شوند، بالاترین اولویت برنده است.
 *
 * مثال:
 *   گیاه در حال FREEZE_2 است و ناگهان PLANT_FOOD فعال می‌شود:
 *   PLANT_FOOD (priority=10) > FREEZE_2 (priority=2) → حالت PLANT_FOOD اجرا می‌شود.
 *   پس از اتمام PLANT_FOOD، به حالت قبلی (FREEZE_2) برمی‌گردد.
 */
public enum PlantAnimState {

    /** حالت پیش‌فرض: انیمیشن idle گیاه. loop. */
    IDLE(0),

    /** یخ‌زدگی سطح ۱: کمی آبی، سرعت عادی */
    FREEZE_1(1),

    /** یخ‌زدگی سطح ۲: آبی‌تر، سرعت نصف */
    FREEZE_2(2),

    /** حالت حمله / شلیک / تولید خورشید. معمولاً loop=false و returnTo=IDLE */
    ATTACK(5),

    /** Plant Food فعال شده — بالاترین اولویت عادی */
    PLANT_FOOD(10),

    /**
     * یخ‌زدگی سطح ۳: کاملاً یخ‌زده — speedMul=0.
     * اولویت بالاتر از ATTACK چون گیاه یخ‌زده نمی‌تواند شلیک کند.
     */
    FREEZE_3(15),

    /**
     * حالت هشدار قبل از حمله ویژه (مثلاً Squash قبل از له کردن).
     * فقط برای گیاهانی که این state را در JSON تعریف کرده‌اند.
     */
    SQUASH_WARN(18),

    /** در حال مردن — بالاترین اولویت. loop=false. پس از اتمام entity حذف می‌شود. */
    DYING(20);

    /** اولویت این حالت — هر چه بالاتر، بر حالات دیگر غلبه می‌کند */
    public final int priority;

    PlantAnimState(int priority) {
        this.priority = priority;
    }

    /**
     * آیا این state می‌تواند state فعلی را override کند؟
     * برای state های غیر-loop (DYING، PLANT_FOOD) باید تا پایان پخش شوند.
     */
    public boolean canOverride(PlantAnimState current, boolean currentIsDone) {
        if (currentIsDone) return true;
        return this.priority > current.priority;
    }
}
