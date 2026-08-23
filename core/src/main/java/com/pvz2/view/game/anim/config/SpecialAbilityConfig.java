package com.pvz2.view.game.anim.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * تنظیمات یک قابلیت ویژه زامبی.
 *
 * هر زامبی می‌تواند چندین special ability داشته باشد.
 * هر ability یک trigger condition، یک انیمیشن، و یک action دارد.
 *
 * ─── انواع trigger ───────────────────────────────────────────────────
 *
 * HP_BELOW (پارامتر: نسبت ۰-۱):
 *   وقتی HP زامبی زیر این نسبت رفت، ability فعال می‌شود.
 *   مثال: Gargantuar وقتی HP < 50% یک Imp پرت می‌کند.
 *   مثال JSON: { "trigger": "HP_BELOW", "triggerParam": 0.5, "once": true }
 *
 * PERIODIC (پارامتر: ثانیه):
 *   هر X ثانیه یک بار فعال می‌شود.
 *   مثال: Chicken Zombie هر ۵ ثانیه جوجه می‌دهد.
 *   مثال JSON: { "trigger": "PERIODIC", "triggerParam": 5.0, "once": false }
 *
 * ON_EAT (پارامتر: نادیده گرفته می‌شود):
 *   اولین بار که زامبی شروع به خوردن می‌کند.
 *   مثال: زامبی روزنامه با خوردن گیاه، روزنامه‌اش را می‌اندازد و سریع‌تر می‌شود.
 *
 * ON_ENTER (پارامتر: نادیده گرفته می‌شود):
 *   بلافاصله پس از اسپان شدن.
 *   مثال: ظاهر اولیه با انیمیشن ورود.
 *
 * ─── انواع action ────────────────────────────────────────────────────
 *
 * SPAWN_IMP:
 *   Gargantuar یک Imp به سمت چپ پرت می‌کند.
 *   پارامترها: { "x_offset": "-3" }
 *
 * SPAWN_ZOMBIE:
 *   زامبی جدیدی اسپان می‌کند.
 *   پارامترها: { "zombieType": "CHICKEN_WRANGLER", "count": "3", "spreadLanes": "true" }
 *
 * SUMMON_GRAVE:
 *   یک سنگ قبر ایجاد می‌کند.
 *   پارامترها: { "x_offset": "-1" }
 *
 * COLLECT_SUN:
 *   نزدیک‌ترین خورشید روی صفحه را جمع می‌کند.
 *
 * BURN_LANE:
 *   یک ستون از لاین را می‌سوزاند.
 *   پارامترها: { "intensity": "1.0" }
 *
 * TRANSFORM:
 *   زامبی به نوع دیگری تبدیل می‌شود.
 *   پارامترها: { "targetType": "NEWSPAPER_FAST" }
 *   (مثال: Newspaper Zombie بعد از از دست دادن روزنامه سریع‌تر می‌شود)
 *
 * DROP_ARMOR:
 *   زره را ویژوالی می‌اندازد (معمولاً خودکار با ARMOR_BROKEN event انجام می‌شود،
 *   اما اگر زامبی خودش زره را بیندازد استفاده کن).
 */
public class SpecialAbilityConfig {

    /** شناسه منحصربه‌فرد این ability */
    public String id = "";

    /**
     * نوع trigger: "HP_BELOW" | "PERIODIC" | "ON_EAT" | "ON_ENTER"
     */
    public String trigger = "HP_BELOW";

    /**
     * پارامتر مرتبط با trigger:
     *   HP_BELOW → نسبت HP (مثلاً 0.5 = 50%)
     *   PERIODIC → دوره زمانی (ثانیه)
     *   ON_EAT / ON_ENTER → نادیده گرفته می‌شود
     */
    public float triggerParam = 0.5f;

    /**
     * آیا این ability فقط یک بار اجرا می‌شود؟
     * true  → پس از اولین اجرا، به firedAbilities اضافه می‌شود
     * false → می‌تواند بارها اجرا شود (مثلاً PERIODIC)
     */
    public boolean once = true;

    /**
     * State انیمیشنی که هنگام اجرای این ability پخش می‌شود.
     * معمولاً "SPECIAL"، اما می‌تواند "EAT" یا هر State دیگری باشد.
     */
    public String animState = "SPECIAL";

    /**
     * ═══ ویژگی‌های جدید نسخه ۲: انیمیشن چندمرحله‌ای ═══
     * بعضی قابلیت‌ها یک clip واحد ندارند بلکه سه بخش دارند، طبق نمونه
     * واقعی Ra Zombie: power_up → power (loop) → power_down.
     *
     *   clip:      حالت اصلی (loop یا تک‌مرحله‌ای اگر enter/exit تعریف نشوند)
     *   enterClip: clip شروع (مثل "power_up"). null = بدون فاز ورود.
     *   exitClip:  clip پایان (مثل "power_down"). null = بدون فاز خروج.
     *
     * اگر enterClip تعریف شده باشد، ZombieAnimController ابتدا آن را
     * پخش می‌کند، سپس به clip (فاز LOOP) می‌رود، و در پایان (با فراخوانی
     * دستی endActiveAbility() توسط Action مربوطه) exitClip را پخش می‌کند.
     */
    public String clip = null;
    public String enterClip = null;
    public String exitClip = null;

    /**
     * نوع action که پس از شروع انیمیشن execute می‌شود.
     * باید با یک ZombieAction.getActionId() مطابقت داشته باشد.
     */
    public String action = "";

    /**
     * پارامترهای اضافی برای action.
     * هر action این Map را parse می‌کند.
     */
    public Map<String, String> actionParams = new LinkedHashMap<>();
}
