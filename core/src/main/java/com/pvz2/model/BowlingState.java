package com.pvz2.model;

import com.pvz2.model.enums.PlantType;

import java.util.ArrayList;
import java.util.List;

/**
 * وضعیت مینی‌گیم «بولینگ گردویی» (Wallnut Bowling) برای فاز گرافیک.
 *
 * <p>فقط داده‌ی توپ‌های در حال حرکت را نگه می‌دارد؛ منطق حرکت/برخورد در
 * {@code GameService.tickBowling} روی زامبی‌های واقعیِ {@link GameSession} اجرا
 * می‌شود. گردوها از طریق «نوار کناری» تحویل داده می‌شوند (بازاستفاده از
 * conveyorQueue همان session).
 *
 * <p><b>سه نوع گردو</b> (طبق داک — DW3):
 * <ul>
 *   <li>{@link PlantType#WALLNUT_BOWLING}: گردوی عادی — پس از برخورد ۴۵ درجه منحرف می‌شود.</li>
 *   <li>{@link PlantType#BIG_WALLNUT}: گردوی بزرگ — مستقیم پیش می‌رود و له می‌کند.</li>
 *   <li>{@link PlantType#EXPLODE_O_NUT_BOWLING}: گردوی قرمز — با برخورد در ۳×۳ منفجر می‌شود.</li>
 * </ul>
 */
public class BowlingState {

    /** یک گردوی در حال غلتیدن. مختصات ۱-based (مطابق فاز ۱)؛ x اعشاری. */
    public static final class Ball {
        public final PlantType type;
        public double x;
        public int y;
        public int dirX = 1;
        public int dirY = 0;
        public boolean exploded;

        public Ball(PlantType type, double x, int y) {
            this.type = type;
            this.x = x;
            this.y = y;
        }

        public int damage() {
            switch (type) {
                case BIG_WALLNUT:           return 200;
                case EXPLODE_O_NUT_BOWLING: return 180;
                case WALLNUT_BOWLING:
                default:                    return 190;
            }
        }
    }

    private final List<Ball> balls = new ArrayList<>();

    public List<Ball> getBalls() { return balls; }

    public void add(Ball b) { balls.add(b); }

    public static boolean isBowlingNut(PlantType type) {
        return type == PlantType.WALLNUT_BOWLING
                || type == PlantType.BIG_WALLNUT
                || type == PlantType.EXPLODE_O_NUT_BOWLING;
    }
}
