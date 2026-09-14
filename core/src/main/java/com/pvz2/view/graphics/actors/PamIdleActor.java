package com.pvz2.graphics.actors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.model.enums.ChapterType;
import com.pvz2.model.enums.PlantType;
import com.pvz2.model.enums.ZombieType;
import com.pvz2.view.game.anim.config.AnimConfigLoader;
import com.pvz2.view.game.anim.config.PlantAnimConfig;
import com.pvz2.view.game.anim.config.StateConfig;
import com.pvz2.view.game.anim.config.ZombieAnimConfig;

import pvz.libpvz.pam.PamPlayer;

/**
 * اکتوری که انیمیشنِ حالتِ بیکار (idle) یک گیاه یا زامبی را با PAM واقعیِ بازی
 * رسم می‌کند — برای صفحه‌ی کلکسیون (رفعِ V3/W3 که «انیمیشن idle به‌جای عکس»
 * می‌خواهند). اگر PAM در دسترس/bake نبود، به یک تصویرِ ثابت (fallback) برمی‌گردد
 * تا هیچ‌وقت خالی نماند.
 *
 * <p>تکنیکِ رسمِ PAM دقیقاً از {@code NpcDialogOverlay.PortraitActor} گرفته شده:
 * canvasِ PAM حولِ مرکزِ جعبه با ماتریسِ transform مقیاس می‌شود. baking خودکار
 * با فراخوانیِ {@code GameAssets.getInstance().update()} در حلقه‌ی render صفحه
 * پیش می‌رود (صفحه‌ی کلکسیون این را هر فریم صدا می‌زند).
 */
public class PamIdleActor extends Actor {

    private final String pamPath;
    private final String clip;
    private final TextureRegion fallback;   // تصویرِ ثابتِ جایگزین (ممکن است null باشد)
    private float time;

    public PamIdleActor(String pamPath, String clip, TextureRegion fallback) {
        this.pamPath  = pamPath;
        this.clip     = clip;
        this.fallback = fallback;
    }

    // ─── Factories ──────────────────────────────────────────────────────────

    public static PamIdleActor forPlant(PlantType type, TextureRegion fallback) {
        String pam = null, clip = null;
        PlantAnimConfig cfg = loader().getPlantConfig(type);
        if (cfg != null) {
            pam = cfg.pamPath;
            StateConfig idle = cfg.states.get("IDLE");
            clip = idle != null ? idle.clip : null;
        }
        return new PamIdleActor(pam, clip, fallback);
    }

    public static PamIdleActor forZombie(ZombieType type, TextureRegion fallback) {
        String pam = null, clip = null;
        // زامبی‌های عمومی (NORMAL/CONEHEAD/...) per-chapter هستند؛ برای کلکسیون
        // فصلِ پیش‌فرضِ مصر را resolve می‌کنیم (fallback داخلی اگر نبود).
        ZombieAnimConfig cfg = loader().resolveZombieConfig(type, ChapterType.ANCIENT_EGYPT);
        if (cfg != null) {
            pam = cfg.pamPath;
            StateConfig idle = cfg.states != null ? cfg.states.get("IDLE") : null;
            if (idle == null && cfg.states != null) idle = cfg.states.get("WALK"); // بعضی زامبی‌ها idle ندارند
            clip = idle != null ? idle.clip : null;
        }
        return new PamIdleActor(pam, clip, fallback);
    }

    // ─── AnimConfigLoader مشترک (lazy، یک‌بار پارس) ──────────────────────────

    private static AnimConfigLoader sharedLoader;

    private static AnimConfigLoader loader() {
        if (sharedLoader == null) {
            sharedLoader = new AnimConfigLoader("data/character_animations.json");
        }
        return sharedLoader;
    }

    // ─── Rendering ──────────────────────────────────────────────────────────

    @Override
    public void act(float delta) {
        super.act(delta);
        time += delta;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float boxX = getX(), boxY = getY(), boxW = getWidth(), boxH = getHeight();

        if (pamPath != null && !pamPath.isEmpty() && clip != null && !clip.isEmpty()) {
            GameAssets assets = GameAssets.getInstance();
            if (assets.hasPvzAssets()) {
                PamPlayer p = assets.getPamPlayer();
                if (p != null && drawPam(batch, p, boxX, boxY, boxW, boxH)) return;
            }
        }
        drawFallback(batch, parentAlpha, boxX, boxY, boxW, boxH);
    }

    private boolean drawPam(Batch batch, PamPlayer p, float boxX, float boxY, float boxW, float boxH) {
        // اگر PAM هنوز bake نشده یا clip نامعتبر است → false (تا fallback رسم شود)
        try { if (p.getClip(pamPath, clip) == null) return false; } catch (Exception e) { return false; }

        Rectangle canvas;
        try { canvas = p.bounds(pamPath); } catch (Exception e) { return false; }
        if (canvas == null || canvas.width <= 0 || canvas.height <= 0) return false;

        float s  = Math.min(boxW / canvas.width, boxH / canvas.height);
        float cx = boxX + boxW / 2f;
        float cy = boxY + boxH / 2f;

        Color saved = batch.getColor().cpy();
        batch.setColor(Color.WHITE);
        Matrix4 old = batch.getTransformMatrix().cpy();
        batch.setTransformMatrix(old.cpy()
                .translate(cx, cy, 0f).scale(s, s, 1f).translate(-cx, -cy, 0f));
        try {
            p.draw(batch, pamPath, clip, time, cx, cy, true);
        } catch (Exception e) {
            batch.setTransformMatrix(old);
            batch.setColor(saved);
            return false;
        }
        batch.setTransformMatrix(old);
        batch.setColor(saved);
        return true;
    }

    private void drawFallback(Batch batch, float parentAlpha, float boxX, float boxY, float boxW, float boxH) {
        if (fallback == null) return;
        Color saved = batch.getColor().cpy();
        batch.setColor(1f, 1f, 1f, parentAlpha);
        float fw = fallback.getRegionWidth(), fh = fallback.getRegionHeight();
        float s  = Math.min(boxW / fw, boxH / fh);
        float w = fw * s, h = fh * s;
        batch.draw(fallback, boxX + (boxW - w) / 2f, boxY + (boxH - h) / 2f, w, h);
        batch.setColor(saved);
    }
}
