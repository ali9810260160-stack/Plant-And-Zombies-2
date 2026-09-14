package com.pvz2.graphics.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.pvz2.graphics.assets.GameAssets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * رندر جلوه‌های بصری گذرا:
 * انفجار | برخورد پرتابه | ذرات | فلش مرگ موجودیت‌ها
 *
 * <p>این کلاس بخش "زیبایی" داک فاز ۲ را پیاده می‌کند:
 * جلوه انفجار، جلوه آسیب‌دیدن، جلوه برخورد پرتابه.
 */
public class EffectsRenderer {

    private final List<Effect> effects = new ArrayList<>();

    // ─── API ──────────────────────────────────────────────────────────────────

    /** انفجار (Cherry Bomb, Jalapeno, etc.) */
    public void addExplosion(float cx, float cy, float radius) {
        effects.add(new ExplosionEffect(cx, cy, radius));
    }

    /** فلش روشن هنگام آسیب دیدن (گیاه/زامبی/قبر) */
    public void addHitFlash(float cx, float cy, float w, float h) {
        effects.add(new HitFlashEffect(cx, cy, w, h));
    }

    /** برخورد پرتابه */
    public void addProjectileHit(float cx, float cy, String projectileType) {
        effects.add(new ProjectileHitEffect(cx, cy, projectileType));
    }

    /** غذای گیاه — هاله نورانی */
    public void addPlantFoodAura(float cx, float cy, float duration) {
        effects.add(new PlantFoodAuraEffect(cx, cy, duration));
    }

    /** ظهور اعلان متنی (wave start, etc.) */
    public void addTextPopup(float cx, float cy, String text, Color color) {
        effects.add(new TextPopupEffect(cx, cy, text, color));
    }

    public void update(float delta) {
        Iterator<Effect> it = effects.iterator();
        while (it.hasNext()) {
            Effect e = it.next();
            e.time += delta;
            if (e.time >= e.duration) it.remove();
        }
    }

    public void render(Batch batch) {
        for (Effect e : effects) e.draw(batch);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Effect subclasses
    // ─────────────────────────────────────────────────────────────────────────

    private abstract static class Effect {
        float cx, cy, time, duration;
        Effect(float cx, float cy, float dur) {
            this.cx = cx; this.cy = cy; this.duration = dur;
        }
        float progress() { return duration > 0 ? Math.min(1, time / duration) : 1; }
        abstract void draw(Batch batch);
    }

    // ─── انفجار ───────────────────────────────────────────────────────────────
    private static class ExplosionEffect extends Effect {
        float radius;
        ExplosionEffect(float cx, float cy, float r) {
            super(cx, cy, 0.55f);
            this.radius = r;
        }
        @Override
        void draw(Batch batch) {
            float p   = progress();
            float r   = radius * p;
            float alp = 1f - p;
            // جستجو کنید: "ANIM_EXPLOSION" یا "IMAGE_EXPLOSION" در asset browser
            TextureRegion white = GameAssets.getInstance().getWhiteRegion();
            batch.setColor(1f, 0.5f + 0.5f * (1 - p), 0f, alp * 0.85f);
            batch.draw(white, cx - r, cy - r, r * 2, r * 2);
            // حلقه داخلی روشن‌تر
            float ri = r * 0.5f;
            batch.setColor(1f, 0.95f, 0.6f, alp);
            batch.draw(white, cx - ri, cy - ri, ri * 2, ri * 2);
            batch.setColor(Color.WHITE);
        }
    }

    // ─── فلش آسیب ─────────────────────────────────────────────────────────────
    private static class HitFlashEffect extends Effect {
        float w, h;
        HitFlashEffect(float cx, float cy, float w, float h) {
            super(cx, cy, 0.18f);
            this.w = w; this.h = h;
        }
        @Override
        void draw(Batch batch) {
            float alp = (1f - progress()) * 0.75f;
            TextureRegion white = GameAssets.getInstance().getWhiteRegion();
            batch.setColor(1f, 1f, 1f, alp);
            batch.draw(white, cx - w * 0.5f, cy - h * 0.5f, w, h);
            batch.setColor(Color.WHITE);
        }
    }

    // ─── برخورد پرتابه ────────────────────────────────────────────────────────
    private static class ProjectileHitEffect extends Effect {
        Color particleColor;
        ProjectileHitEffect(float cx, float cy, String type) {
            super(cx, cy, 0.25f);
            particleColor = hitColor(type);
        }
        private static Color hitColor(String type) {
            if (type == null)           return Color.GREEN;
            if (type.contains("fire"))  return Color.ORANGE;
            if (type.contains("snow") || type.contains("ice")) return Color.CYAN;
            if (type.contains("lightning")) return Color.YELLOW;
            return new Color(0.3f, 0.85f, 0.3f, 1f);
        }
        @Override
        void draw(Batch batch) {
            float p = progress();
            float r = 14f * (1 - p);
            float a = 1f - p;
            TextureRegion white = GameAssets.getInstance().getWhiteRegion();
            batch.setColor(particleColor.r, particleColor.g, particleColor.b, a);
            // ۴ ذره به اطراف
            float d = 18f * p;
            for (int i = 0; i < 4; i++) {
                double angle = Math.PI * 0.5 * i;
                float px = cx + (float) Math.cos(angle) * d;
                float py = cy + (float) Math.sin(angle) * d;
                batch.draw(white, px - r * 0.5f, py - r * 0.5f, r, r);
            }
            batch.setColor(Color.WHITE);
        }
    }

    // ─── هاله غذای گیاه ────────────────────────────────────────────────────────
    private static class PlantFoodAuraEffect extends Effect {
        PlantFoodAuraEffect(float cx, float cy, float dur) {
            super(cx, cy, dur);
        }
        @Override
        void draw(Batch batch) {
            float p   = (float)(Math.sin(time * 8) * 0.5 + 0.5);
            float r   = 60f + p * 20f;
            float alp = 0.4f + p * 0.25f;
            TextureRegion white = GameAssets.getInstance().getWhiteRegion();
            batch.setColor(0.3f, 1f, 0.3f, alp);
            batch.draw(white, cx - r, cy - r * 1.2f, r * 2, r * 2.4f);
            batch.setColor(Color.WHITE);
        }
    }

    // ─── متن بالا رونده ────────────────────────────────────────────────────────
    private static class TextPopupEffect extends Effect {
        String text;
        Color color;
        TextPopupEffect(float cx, float cy, String t, Color c) {
            super(cx, cy, 1.4f);
            this.text = t;
            this.color = c;
        }
        @Override
        void draw(Batch batch) {
            float p   = progress();
            float yOff= p * 60f;
            float alp = p < 0.7f ? 1f : 1f - (p - 0.7f) / 0.3f;
            // GameAssets.fontOf(...) هرگز throw/null نمی‌کند — قبلاً اینجا
            // skin.getFont("medium") بود که چون "medium" اسم Label style است نه
            // فونت resource، هر بار popup متن (مثلاً امتیاز میوپوینت) throw می‌کرد.
            com.badlogic.gdx.graphics.g2d.BitmapFont f =
                    GameAssets.getInstance().fontOf("medium");
            f.setColor(color.r, color.g, color.b, alp);
            f.draw(batch, text, cx - 40, cy + yOff);
            f.setColor(Color.WHITE);
        }
    }
}
