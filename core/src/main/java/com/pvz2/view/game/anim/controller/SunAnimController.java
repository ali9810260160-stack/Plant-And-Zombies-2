package com.pvz2.view.game.anim.controller;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.pvz2.graphics.util.GameCoords;
import com.pvz2.model.Sun;
import com.pvz2.model.enums.SunType;

import java.util.HashMap;
import java.util.Map;

/**
 * رندر و انیمیشن خورشیدها — نسخه ۲.
 *
 * ─── تغییرات نسخه ۲ ──────────────────────────────────────────────
 *
 *  ۱. رفع کامل باگ مختصات: نسخه قبلی فقط نیمی از باگ را رفع کرده بود
 *     (تشخیص داده بود sun.getX()/getY() ستون/ردیف است نه پیکسل)، اما
 *     همچنان از فرمول محلی {@code GRID_ORIGIN_Y + (row-1)*CELL_H}
 *     استفاده می‌کرد که محور row را برعکس نمی‌کند. اکنون از
 *     {@link GameCoords} استفاده می‌شود — دقیقاً هماهنگ با
 *     GridRenderer/EntityRenderer/سایر anim controller ها.
 *
 *  ۲. یافته واقعی از بررسی assets: خورشید فقط <b>یک</b> PAM دارد
 *     (نه سه PAM جدا برای NORMAL/SPECIAL/RADIOACTIVE):
 *       768/INITIAL/EFFECTS/SUN/SUN.PAM
 *       clips: animation, transition_red, red, transition_blue, blue
 *     یعنی:
 *       NORMAL      → "animation" (بدون گذار رنگی)
 *       SPECIAL     → "animation" + tint طلایی پررنگ‌تر (Sun-100 در
 *                     بازی واقعی صرفاً نسخه بزرگ‌تر همین خورشید است)
 *       RADIOACTIVE → transition_red → red (+ tint سبز رادیواکتیو اضافی)
 */
public class SunAnimController {

    public static final float CELL_W = 100f;
    public static final float CELL_H = 100f;

    /** مسیر واقعی PAM خورشید (طبق بررسی assets — یک فایل با چند clip رنگی). */
    private static final String SUN_PAM_PATH = "768/INITIAL/EFFECTS/SUN/SUN.PAM";

    private static final String CLIP_IDLE             = "animation";
    private static final String CLIP_RADIOACTIVE_IN   = "transition_red";
    private static final String CLIP_RADIOACTIVE_LOOP = "red";
    private static final String CLIP_COLLECT          = "animation"; // بدون clip اختصاصی جمع‌آوری در assets واقعی

    private static final float SPAWN_Y = 780f;

    private static final float BOUNCE_DURATION = 0.25f;
    private static final float BOUNCE_HEIGHT   = 15f;

    private static final float COLLECT_DURATION  = 0.35f;
    private static final float COLLECT_MAX_SCALE = 1.4f;
    private static final float SUN_COUNTER_X = 60f;
    private static final float SUN_COUNTER_Y = 680f;

    private static class SunState {
        float stateTime = 0f;
        float bounceTimer = 0f;
        boolean bounced = false;
        float collectTimer = 0f;
        boolean collecting = false;
        float glowPulse = 0f;
        boolean radioactiveTransitionDone = false;
    }

    private final Map<Sun, SunState> sunStates = new HashMap<>();

    // ════════════════════════════════════════════════════════════
    //  Update
    // ════════════════════════════════════════════════════════════

    public void update(float delta, Sun sun) {
        SunState s = sunStates.computeIfAbsent(sun, k -> new SunState());
        s.stateTime += delta;

        if (sun.isLanded() && !s.bounced) {
            s.bounceTimer += delta;
            if (s.bounceTimer >= BOUNCE_DURATION) s.bounced = true;
        }

        if (sun.getType() == SunType.RADIOACTIVE && !s.radioactiveTransitionDone) {
            if (s.stateTime >= 0.4f) s.radioactiveTransitionDone = true; // طول transition_red تخمینی
        }

        if (sun.isCollected() && !s.collecting) {
            s.collecting = true;
            s.collectTimer = 0f;
        }
        if (s.collecting) s.collectTimer += delta;

        s.glowPulse = (float) Math.sin(s.stateTime * 5.0) * 0.15f + 0.85f;
    }

    // ════════════════════════════════════════════════════════════
    //  Render
    // ════════════════════════════════════════════════════════════

    public void render(SpriteBatch batch, Sun sun, Object pamPlayer, boolean isHovered) {
        SunState s = sunStates.get(sun);
        if (s == null) return;

        // ── مختصات (از GameCoords — شامل معکوس‌سازی صحیح محور row) ──
        float worldX = GameCoords.toScreenX(sun.getX());
        float worldY = computeWorldY(sun, s);

        if (s.collecting) {
            float t = Math.min(1f, s.collectTimer / COLLECT_DURATION);
            t = t * t; // ease-in quadratic
            worldX = lerp(worldX, SUN_COUNTER_X, t);
            worldY = lerp(worldY, SUN_COUNTER_Y, t);
        }

        float scale = 1.0f;
        float alpha = 1.0f;
        if (s.collecting) {
            float t = Math.min(1f, s.collectTimer / COLLECT_DURATION);
            scale = lerp(1.0f, COLLECT_MAX_SCALE, t);
            alpha = 1.0f - t;
        }
        if (alpha <= 0.02f) return;

        String clip = resolveClip(sun, s);

        Color saved = batch.getColor().cpy();
        applyTint(batch, sun, isHovered, s, alpha);

        float size = (sun.getType() == SunType.SPECIAL ? 34f : 26f) * scale;
        Color fallbackColor = sun.getType() == SunType.RADIOACTIVE
                ? new Color(0.6f, 1f, 0.4f, alpha)
                : new Color(1f, 0.85f, 0.15f, alpha);

        PamDrawUtil.draw(pamPlayer, batch, SUN_PAM_PATH, clip, s.stateTime,
                worldX, worldY, true, size, size, fallbackColor);

        batch.setColor(saved);
    }

    // ════════════════════════════════════════════════════════════
    //  منطق داخلی
    // ════════════════════════════════════════════════════════════

    private float computeWorldY(Sun sun, SunState s) {
        float targetY = GameCoords.toScreenY(sun.getY());

        if (!sun.isLanded()) {
            float fp = Math.max(0f, Math.min(1f, (float) sun.getFallProgress()));
            return lerp(SPAWN_Y, targetY, fp);
        }

        if (!s.bounced) {
            float t = s.bounceTimer / BOUNCE_DURATION;
            return targetY + BOUNCE_HEIGHT * (float) Math.sin(t * Math.PI);
        }

        return targetY;
    }

    private String resolveClip(Sun sun, SunState s) {
        if (sun.getType() == SunType.RADIOACTIVE) {
            return s.radioactiveTransitionDone ? CLIP_RADIOACTIVE_LOOP : CLIP_RADIOACTIVE_IN;
        }
        if (s.collecting) return CLIP_COLLECT;
        return CLIP_IDLE;
    }

    private void applyTint(SpriteBatch batch, Sun sun, boolean isHovered, SunState s, float alpha) {
        float r = 1f, g = 1f, b = 1f, a = alpha;

        if (sun.getType() == SunType.SPECIAL) {
            r = 1.0f; g = 0.92f; b = 0.55f;
        }
        if (sun.getType() == SunType.RADIOACTIVE) {
            r = 0.75f; g = 1.0f; b = 0.55f;
        }
        if (isHovered) {
            float p = s.glowPulse;
            r = Math.min(1f, r + 0.15f * p);
            g = Math.min(1f, g + 0.10f * p);
        }
        batch.setColor(r, g, b, a);
    }

    // ════════════════════════════════════════════════════════════
    //  Life Cycle
    // ════════════════════════════════════════════════════════════

    public void removeSun(Sun sun) { sunStates.remove(sun); }
    public void clear() { sunStates.clear(); }

    /** شعاعِ تشخیصِ عبورِ نشانگر روی خورشید (پیکسلِ صفحه). */
    private static final float HOVER_RADIUS = 34f;

    /** خورشیدهایی که controller هنوز state آن‌ها را نگه داشته (برای رندرِ ادامه‌دار). */
    public java.util.Set<Sun> trackedSuns() { return sunStates.keySet(); }

    /**
     * آیا نشانگرِ موس (در مختصاتِ world) روی این خورشید است؟ برای درخششِ hover
     * (CN3). موقعیتِ خورشید دقیقاً مطابقِ فرمولِ رندر محاسبه می‌شود.
     */
    public boolean isUnderCursor(Sun sun, float cursorX, float cursorY) {
        SunState s = sunStates.get(sun);
        if (s == null || sun.isCollected()) return false;
        float wx = GameCoords.toScreenX(sun.getX());
        float wy = computeWorldY(sun, s);
        float dx = cursorX - wx, dy = cursorY - wy;
        return dx * dx + dy * dy <= HOVER_RADIUS * HOVER_RADIUS;
    }

    /**
     * آیا انیمیشنِ جمع‌آوریِ این خورشید هنوز باید ادامه یابد؟ (پس از حذف از
     * activeSuns مدل، تا کاملشدنِ پروازِ خورشید به شمارنده). وقتی false شد،
     * فراخواننده باید {@link #removeSun} را صدا بزند.
     */
    public boolean shouldKeepAnimating(Sun sun) {
        SunState s = sunStates.get(sun);
        if (s == null) return false;
        return sun.isCollected() && s.collectTimer < COLLECT_DURATION;
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
