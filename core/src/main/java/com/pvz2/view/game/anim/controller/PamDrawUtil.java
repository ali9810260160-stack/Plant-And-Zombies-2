package com.pvz2.view.game.anim.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.pvz2.graphics.assets.GameAssets;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * پل امن بین کنترلرهای انیمیشن (بخش سوم) و {@code pvz.libpvz.pam.PamPlayer}.
 *
 * <p>دو دلیل برای وجود این کلاس مشترک به‌جای صدا زدن مستقیم PamPlayer در هر
 * کنترلر:
 * <ol>
 *   <li><b>Fallback همیشگی</b> — تا وقتی asset واقعی بازی (PAM/atlas) در
 *       دسترس نیست (یعنی {@code -Dpvz.assets} ست نشده)، سیستم انیمیشن اصلاً
 *       چیزی رسم نمی‌کرد (کد اصلی این بخش کامنت بود) — یعنی از GridRenderer
 *       رگرسیون می‌داد (که حداقل مستطیل رنگی می‌کشید). اینجا اگر
 *       {@code GameAssets.hasPvzAssets()} false باشد یا draw واقعی fail کند،
 *       یک مستطیل رنگی جایگزین (با رنگ مخصوص هر نوع) کشیده می‌شود.</li>
 *   <li><b>عدم قطعیت overload با visMap</b> — مستندات libPVZ نمونه‌ی
 *       {@code draw(batch, ClipRef, time, x, y, flip, visMap)} را نشان
 *       می‌دهد، ولی نسخه‌ی مسیر-رشته‌ای+visMap تضمین‌شده نیست. برای این یکی
 *       overload از reflection استفاده می‌شود تا اگر وجود نداشت، برنامه
 *       compile-break نشود و فقط بدون قابلیت hide/show زره رندر شود.</li>
 * </ol>
 */
public final class PamDrawUtil {

    private PamDrawUtil() {}

    private static Method visMapDrawMethod;
    private static boolean visMapMethodChecked = false;

    // ماتریس‌های موقتِ scale (تک‌رشته‌ای رندر — استفاده‌ی مجدد بدونِ allocation).
    private static final Matrix4 SAVED_M = new Matrix4();
    private static final Matrix4 TMP_M   = new Matrix4();
    /** بزرگ‌ترین نسبتِ منطقیِ scale (جلوگیری از بزرگ‌نماییِ ناخواسته). */
    private static final float MAX_SCALE = 1.0f;

    /**
     * رندر یک clip از یک PAM با fallback خودکار.
     *
     * @param pamPlayer  نمونه {@code pvz.libpvz.pam.PamPlayer} (به‌صورت Object)
     * @param batch      SpriteBatch در حال begin()
     * @param pamPath    مسیر فایل PAM (مثل {@code "PLANTS/PEASHOOTER/PEASHOOTER.PAM"})
     * @param clip       نام کلیپ (مثل {@code "idle"})
     * @param stateTime  زمان سپری‌شده در این state (ثانیه)
     * @param x          مرکز پایین موجودیت روی محور X (world)
     * @param y          مرکز پایین موجودیت روی محور Y (world)
     * @param loop       آیا کلیپ باید حلقه بزند؟ (WALK/IDLE/EAT=true، DYING/power=false).
     *                   ⚠️ توجه: امضای {@code PamPlayer.draw(...,boolean)} پارامترِ آخرش
     *                   {@code loop} است نه flip؛ libPVZ اصلاً flip افقی ندارد. اگر اینجا
     *                   false پاس داده شود، انیمیشن یک‌بار پخش و روی فریمِ آخر «فریز»
     *                   می‌شود (باگِ راه‌رفتنِ ثابتِ زامبی‌ها).
     * @param visMap     نقشه نمایانی بخش‌ها (زره و ...) — می‌تواند null باشد
     * @param fallbackW  عرض مستطیل fallback
     * @param fallbackH  ارتفاع مستطیل fallback
     * @param fallbackColor رنگ مستطیل fallback
     */
    public static void draw(Object pamPlayer, Batch batch, String pamPath, String clip,
                             float stateTime, float x, float y, boolean loop,
                             Map<String, Boolean> visMap,
                             float fallbackW, float fallbackH, Color fallbackColor) {
        draw(pamPlayer, batch, pamPath, clip, stateTime, x, y, loop, visMap,
                fallbackW, fallbackH, fallbackColor, MAX_SCALE);
    }

    /**
     * نسخه با سقفِ scaleِ دلخواه — برای موجودیت‌های بزرگ‌تر از canvasِ نیتیو
     * (مثلِ Zombossِ قرون‌وسطی/مصر که باید چند برابرِ اندازه‌ی نیتیو رسم شوند).
     * {@code maxScale > 1} بزرگ‌نمایی را مجاز می‌کند.
     */
    public static void draw(Object pamPlayer, Batch batch, String pamPath, String clip,
                             float stateTime, float x, float y, boolean loop,
                             Map<String, Boolean> visMap,
                             float fallbackW, float fallbackH, Color fallbackColor,
                             float maxScale) {
        if (pamPlayer != null && GameAssets.getInstance().hasPvzAssets()
                && pamPath != null && !pamPath.isEmpty() && clip != null && !clip.isEmpty()) {
            try {
                pvz.libpvz.pam.PamPlayer p = (pvz.libpvz.pam.PamPlayer) pamPlayer;
                // getClip تا وقتی PAM هنوز bake نشده null برمی‌گرداند (و بارگذاریِ
                // async را کیک می‌زند). در آن حالت به‌جای «هیچ»، مستطیلِ fallback
                // کشیده می‌شود تا موجودیت همیشه دیده شود (نه صفحه‌ی خالی).
                if (p.getClip(pamPath, clip) == null) {
                    drawFallbackRect(batch, x, y, fallbackW, fallbackH, fallbackColor);
                    return;
                }
                // ── مقیاس‌دهی: PamPlayer در اندازه‌ی نیتیوِ canvas (مثلاً ۳۹۰px)
                // رسم می‌کند که برای خانه‌های ~۵۰-۶۷px بسیار بزرگ است. با ماتریسِ
                // transform حولِ نقطه‌ی (x,y) به ارتفاعِ هدف (fallbackH) کوچک می‌کنیم.
                float scale = computeScale(p, pamPath, fallbackH, maxScale);
                boolean scaled = scale > 0f && Math.abs(scale - 1f) > 0.001f;
                if (scaled) {
                    SAVED_M.set(batch.getTransformMatrix());
                    TMP_M.set(SAVED_M).translate(x, y, 0f).scale(scale, scale, 1f)
                            .translate(-x, -y, 0f);
                    batch.setTransformMatrix(TMP_M);
                }
                boolean drewVis = visMap != null && !visMap.isEmpty() && tryDrawWithVisMap(
                        pamPlayer, batch, pamPath, clip, stateTime, x, y, loop, visMap);
                if (!drewVis) {
                    p.draw(batch, pamPath, clip, stateTime, x, y, loop);
                }
                if (scaled) batch.setTransformMatrix(SAVED_M);
                return;
            } catch (Exception ignored) {
                // clip/pam پیدا نشد یا asset ناقص بود — به fallback زیر می‌رویم
            }
        }
        drawFallbackRect(batch, x, y, fallbackW, fallbackH, fallbackColor);
    }

    /** بدون visMap (پرتابه‌ها، خورشید) */
    public static void draw(Object pamPlayer, Batch batch, String pamPath, String clip,
                             float stateTime, float x, float y, boolean loop,
                             float fallbackW, float fallbackH, Color fallbackColor) {
        draw(pamPlayer, batch, pamPath, clip, stateTime, x, y, loop, null,
                fallbackW, fallbackH, fallbackColor);
    }

    /** بدون visMap، با سقفِ scaleِ دلخواه (بزرگ‌نمایی مجاز) — برای Zomboss. */
    public static void draw(Object pamPlayer, Batch batch, String pamPath, String clip,
                             float stateTime, float x, float y, boolean loop,
                             float fallbackW, float fallbackH, Color fallbackColor,
                             float maxScale) {
        draw(pamPlayer, batch, pamPath, clip, stateTime, x, y, loop, null,
                fallbackW, fallbackH, fallbackColor, maxScale);
    }

    /**
     * تلاش برای overload دارای visMap از طریق reflection — اگر در این نسخه
     * از libPVZ وجود نداشته باشد، false برمی‌گرداند (نه throw) و کالر به
     * نسخه ساده سقوط می‌کند.
     */
    private static boolean tryDrawWithVisMap(Object pamPlayer, Batch batch, String pamPath,
                                              String clip, float stateTime, float x, float y,
                                              boolean loop, Map<String, Boolean> visMap) {
        try {
            if (!visMapMethodChecked) {
                visMapMethodChecked = true;
                try {
                    visMapDrawMethod = pamPlayer.getClass().getMethod("draw",
                            Batch.class, String.class, String.class, float.class,
                            float.class, float.class, boolean.class, Map.class);
                } catch (NoSuchMethodException nsme) {
                    visMapDrawMethod = null;
                }
            }
            if (visMapDrawMethod == null) return false;
            visMapDrawMethod.invoke(pamPlayer, batch, pamPath, clip, stateTime, x, y, loop, visMap);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * محاسبه‌ی ضریبِ scale برای اینکه ارتفاعِ canvasِ نیتیوِ PAM به {@code targetH}
     * نگاشته شود. اگر bounds در دسترس نبود یا targetH نامعتبر بود، ۱ برمی‌گردد.
     */
    private static float computeScale(pvz.libpvz.pam.PamPlayer p, String pamPath, float targetH,
                                      float maxScale) {
        if (targetH <= 0f) return 1f;
        try {
            Rectangle b = p.bounds(pamPath);
            if (b != null && b.height > 1f) {
                return Math.min(maxScale, targetH / b.height);
            }
        } catch (Exception ignored) { }
        return 1f;
    }

    /** مستطیل رنگی ساده — وقتی asset واقعی در دسترس نیست. */
    public static void drawFallbackRect(Batch batch, float centerX, float bottomY,
                                         float w, float h, Color color) {
        Color saved = batch.getColor().cpy();
        batch.setColor(color);
        batch.draw(GameAssets.getInstance().getWhiteRegion(),
                centerX - w / 2f, bottomY, w, h);
        batch.setColor(saved);
    }

    /**
     * رندرِ یک clip با <b>چرخش</b> (به‌علاوه‌ی مقیاس) حولِ نقطه‌ی
     * {@code (x, y + pivotYOffset)}. برای گردوهای بولینگ که مثلِ چرخ می‌غلتند.
     * مرکزِ چرخش را با {@code pivotYOffset} روی مرکزِ بصریِ موجودیت تنظیم کنید تا
     * به‌جای «تابِ پاندولی» حولِ پایه، دورِ خودش بغلتد.
     *
     * @param rotationDeg  زاویه‌ی چرخش (درجه، پادساعت‌گرد مثبت)
     * @param pivotYOffset فاصله‌ی عمودیِ مرکزِ چرخش از anchorِ (x,y)
     */
    public static void drawRotated(Object pamPlayer, Batch batch, String pamPath, String clip,
                                   float stateTime, float x, float y, boolean loop,
                                   float rotationDeg, float pivotYOffset,
                                   float fallbackW, float fallbackH, Color fallbackColor,
                                   float maxScale) {
        if (pamPlayer != null && GameAssets.getInstance().hasPvzAssets()
                && pamPath != null && !pamPath.isEmpty() && clip != null && !clip.isEmpty()) {
            try {
                pvz.libpvz.pam.PamPlayer p = (pvz.libpvz.pam.PamPlayer) pamPlayer;
                if (p.getClip(pamPath, clip) == null) {
                    drawFallbackRect(batch, x, y, fallbackW, fallbackH, fallbackColor);
                    return;
                }
                float scale = computeScale(p, pamPath, fallbackH, maxScale);
                if (scale <= 0f) scale = 1f;
                float px = x, py = y + pivotYOffset;   // مرکزِ چرخش
                SAVED_M.set(batch.getTransformMatrix());
                TMP_M.set(SAVED_M).translate(px, py, 0f)
                        .rotate(0f, 0f, 1f, rotationDeg)
                        .scale(scale, scale, 1f)
                        .translate(-px, -py, 0f);
                batch.setTransformMatrix(TMP_M);
                p.draw(batch, pamPath, clip, stateTime, x, y, loop);
                batch.setTransformMatrix(SAVED_M);
                return;
            } catch (Exception ignored) { }
        }
        drawFallbackRect(batch, x, y, fallbackW, fallbackH, fallbackColor);
    }

    /**
     * رندرِ فقط یک پارتِ خاص از یک clip (whitelist) — از {@code PamPlayer.drawPart}.
     * برخلافِ draw عادی، این متد قیدِ «پارت‌های flag-دارِ پیش‌فرض‌مخفی» (armor/custom)
     * را دور می‌زند، پس برای overlayِ آرمور یا رندرِ یک قطعه‌ی جدا (سر/دست/دستِ افتاده)
     * مناسب است. پارت در همان موقعیتِ فریمِ انیمیشن رسم می‌شود (هم‌تراز با بدن).
     *
     * <p>بدونِ fallback؛ اگر PAM/clip/part نبود چیزی رسم نمی‌شود (overlay است، نه بدنه).
     *
     * @param offsetX جابه‌جاییِ افقیِ اضافه روی مرکز (برای پرتابِ قطعه؛ ۰ = هم‌تراز با بدن)
     * @param offsetY جابه‌جاییِ عمودیِ اضافه (برای سقوط؛ ۰ = هم‌تراز با بدن)
     */
    public static void drawPart(Object pamPlayer, Batch batch, String pamPath, String clip,
                                float stateTime, float x, float y, String part,
                                float targetH, float offsetX, float offsetY) {
        if (pamPlayer == null || !GameAssets.getInstance().hasPvzAssets()
                || pamPath == null || pamPath.isEmpty()
                || clip == null || clip.isEmpty()
                || part == null || part.isEmpty()) {
            return;
        }
        try {
            pvz.libpvz.pam.PamPlayer p = (pvz.libpvz.pam.PamPlayer) pamPlayer;
            if (p.getClip(pamPath, clip) == null) return;   // هنوز bake نشده
            float scale = computeScale(p, pamPath, targetH, MAX_SCALE);
            float px = x + offsetX, py = y + offsetY;
            boolean scaled = scale > 0f && Math.abs(scale - 1f) > 0.001f;
            if (scaled) {
                SAVED_M.set(batch.getTransformMatrix());
                TMP_M.set(SAVED_M).translate(px, py, 0f).scale(scale, scale, 1f)
                        .translate(-px, -py, 0f);
                batch.setTransformMatrix(TMP_M);
            }
            p.drawPart(batch, pamPath, clip, stateTime, px, py, part);
            if (scaled) batch.setTransformMatrix(SAVED_M);
        } catch (Exception ignored) { }
    }

    /** overlayِ ساده‌ی یک پارت هم‌تراز با بدن (بدونِ جابه‌جایی). */
    public static void drawPart(Object pamPlayer, Batch batch, String pamPath, String clip,
                                float stateTime, float x, float y, String part, float targetH) {
        drawPart(pamPlayer, batch, pamPath, clip, stateTime, x, y, part, targetH, 0f, 0f);
    }

    /**
     * طولِ (ثانیه‌ایِ) یک clip از یک PAM — از {@code PamPlayer.clipDurationSeconds}.
     * تا وقتی PAM هنوز bake نشده یا asset در دسترس نیست، {@code -1} برمی‌گرداند
     * (کالر باید در آن حالت به مقدارِ fallback تکیه کند).
     */
    public static float clipDuration(Object pamPlayer, String pamPath, String clip) {
        if (pamPlayer == null || !GameAssets.getInstance().hasPvzAssets()
                || pamPath == null || pamPath.isEmpty() || clip == null || clip.isEmpty()) {
            return -1f;
        }
        try {
            pvz.libpvz.pam.PamPlayer p = (pvz.libpvz.pam.PamPlayer) pamPlayer;
            if (p.getClip(pamPath, clip) == null) return -1f; // هنوز bake نشده
            float d = p.clipDurationSeconds(pamPath, clip);
            return d > 0f ? d : -1f;
        } catch (Exception ignored) {
            return -1f;
        }
    }

    /** ساخت سریع یک visibility map تک‌کلیدی (برای موارد ساده). */
    public static Map<String, Boolean> mapOf(String key, boolean value) {
        Map<String, Boolean> m = new HashMap<>();
        m.put(key, value);
        return m;
    }
}
