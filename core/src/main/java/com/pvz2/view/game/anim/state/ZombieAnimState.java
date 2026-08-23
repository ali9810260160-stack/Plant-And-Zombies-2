package com.pvz2.view.game.anim.state;

/**
 * حالت‌های انیمیشنی ممکن برای زامبی‌ها.
 *
 * نکات مهم:
 *   ۱. health variant (NORMAL/DAMAGED/CRITICAL) یک بُعد جداگانه است و
 *      به عنوان suffix به نام clip اضافه می‌شود، نه یک state جداگانه.
 *      مثال: state=WALK + variant=DAMAGED → clip="walk_damaged"
 *
 *   ۲. armor ها از طریق visibility map مدیریت می‌شوند و state را عوض نمی‌کنند.
 *
 *   ۳. status effect ها (CHILLED، FROZEN) سرعت پخش را تغییر می‌دهند.
 */
public enum ZombieAnimState {

    /** قبل از شروع موج — زامبی ایستاده است */
    IDLE(0),

    /** در حال حرکت به سمت چپ — حالت پیش‌فرض */
    WALK(1),

    /**
     * در حال خوردن گیاه.
     * همراه با health variant suffix:
     *   WALK (50-100% HP) → "eat_ground"
     *   WALK (25-50% HP)  → "eat_ground_damaged"
     *   WALK (0-25% HP)   → "eat_ground_critical"  (اگر تعریف شده باشد)
     */
    EAT(5),

    /**
     * ═══ ویژگی جدید نسخه ۲ ═══
     * در حال پرواز (فقط Imp، بعد از پرتاب شدن توسط Gargantuar).
     * clip واقعی معمولاً "fly" است. loop=true تا رسیدن به مقصد.
     */
    FLYING(6),

    /**
     * ═══ ویژگی جدید نسخه ۲ ═══
     * لحظه فرود (فقط Imp، clip واقعی "land"). loop=false، سپس WALK.
     */
    LANDING(7),

    /**
     * در حال اجرای قابلیت ویژه.
     * مثال:
     *   Gargantuar: پرت کردن Imp → clip="throw"
     *   Chicken Zombie: احضار جوجه → clip="summon"
     *   Necromancer: ایجاد قبر → clip="curse"
     * معمولاً loop=false، returnTo=WALK یا EAT
     */
    SPECIAL(10),

    /** در حال مردن — loop=false. پس از اتمام entity حذف می‌شود. */
    DYING(20);

    public final int priority;

    ZombieAnimState(int priority) {
        this.priority = priority;
    }

    public boolean canOverride(ZombieAnimState current, boolean currentIsDone) {
        if (currentIsDone) return true;
        return this.priority > current.priority;
    }
}
