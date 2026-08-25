package com.pvz2.model;

import com.pvz2.model.enums.PlantType;

/**
 * وضعیت مینی‌گیم امتیازی «ترکیب سه‌تایی» (Beghouled) — نسخه‌ی روی مپِ واقعی.
 *
 * <p>برخلاف نسخه‌ی قدیمیِ مستقل، این بازی روی خودِ زمینِ بازی انجام می‌شود:
 * کلِ گرید با ۵ نوع گیاهِ واقعی پر می‌شود، زامبی‌ها بی‌پایان می‌آیند و مثلِ یک
 * مرحله‌ی عادی رفتار می‌کنند، و بازیکن گیاهانِ مجاور را جابه‌جا می‌کند تا ترکیبِ
 * سه‌تایی (یا بیشتر) بسازد. منطقِ swap/match/gravity در {@code GameService} روی
 * {@code Tile}های واقعی اجرا می‌شود؛ این کلاس فقط <b>state</b> است.
 *
 * <p>قواعد (داک فاز ۱):
 * <ul>
 *   <li>هر ترکیب یک خورشیدِ ۵۰تایی می‌دهد؛ ترکیبِ بزرگ‌تر بیشتر (۴تایی=۲، ۵تایی=۳).</li>
 *   <li>فقط جابه‌جایی‌ای مجاز است که ترکیب بسازد.</li>
 *   <li>گیاهِ خورده‌شده → crater (دیگر گیاه نمی‌گیرد).</li>
 *   <li>اگر هیچ حرکتِ ممکنی نبود، کلِ زمین reset می‌شود.</li>
 *   <li>برد: ساختِ {@link #target} ترکیب.</li>
 * </ul>
 */
public class BeghouledState {

    /** ۵ نوع گیاهِ روی زمین (نقشِ «نگین»ها). */
    public static final PlantType[] PALETTE = {
        PlantType.PEASHOOTER,
        PlantType.SUNFLOWER,
        PlantType.WALL_NUT,
        PlantType.SNOW_PEA,
        PlantType.CABBAGE_PULT,
    };

    /** خورشیدِ پاداشِ هر ترکیبِ سه‌تایی. */
    public static final int SUN_PER_MATCH = 50;

    /** یک گزینه‌ی ارتقا: تبدیلِ همه‌ی گیاهانِ نوعِ from به to با هزینه‌ی خورشید. */
    public static final class Upgrade {
        public final PlantType from;
        public final PlantType to;
        public final int cost;
        public final String label;
        public Upgrade(PlantType from, PlantType to, int cost, String label) {
            this.from = from; this.to = to; this.cost = cost; this.label = label;
        }
    }

    /** جدولِ ارتقا (زیرمجموعه‌ی داک که با palette/enum سازگار است؛ زنجیره‌ای). */
    public static final Upgrade[] UPGRADES = {
        new Upgrade(PlantType.PEASHOOTER,   PlantType.REPEATER,     500,  "Peashooter -> Repeater"),
        new Upgrade(PlantType.WALL_NUT,     PlantType.TALL_NUT,     500,  "Wall-nut -> Tall-nut"),
        new Upgrade(PlantType.CABBAGE_PULT, PlantType.MELON_PULT,   1000, "Cabbage -> Melon-pult"),
        new Upgrade(PlantType.MELON_PULT,   PlantType.WINTER_MELON, 750,  "Melon -> Winter Melon"),
    };

    private final int target;      // تعداد ترکیبِ لازم برای برد
    private int matchesMade;

    public BeghouledState(int target) {
        this.target = Math.max(1, target);
    }

    public int getTarget()      { return target; }
    public int getMatchesMade() { return matchesMade; }
    public void addMatches(int n) { matchesMade += n; }
    public boolean isComplete() { return matchesMade >= target; }
    public int remaining()      { return Math.max(0, target - matchesMade); }
}
