package com.pvz2.graphics.renderer;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.pvz2.graphics.GameStateSnapshot;
import com.pvz2.graphics.util.GameConfig;

/**
 * هماهنگ‌کننده مرکزی رندر — در هر فریم توسط {@link com.pvz2.graphics.screens.GameScreen} فراخوانی می‌شود.
 *
 * <p>ترتیب رندر:
 * ۱. پس‌زمینه / کاشی‌ها / چمن‌زن‌ها
 * ۲. موجودیت‌ها (depth-sorted)
 * ۳. جلوه‌های بصری (انفجار، فلش، ...)
 * ۴. HUD
 */
public class GameRenderer implements Disposable {

    private final SpriteBatch      batch;
    private final GridRenderer     grid;
    private final EntityRenderer   entities;
    private final HudRenderer      hud;
    private final EffectsRenderer  effects;

    public GameRenderer(String chapter) {
        batch    = new SpriteBatch();
        grid     = new GridRenderer(chapter);
        entities = new EntityRenderer();
        hud      = new HudRenderer();
        effects  = new EffectsRenderer();
    }

    /**
     * رندر کامل یک فریم.
     *
     * @param snap       اسنپ‌شات وضعیت بازی (از GameFacade)
     * @param delta      زمان از آخرین فریم (ثانیه)
     * @param projMatrix projection matrix دوربین
     * @param speedIndex شاخص سرعت (0=1×, 1=1.5×, 2=2×)
     */
    public void render(GameStateSnapshot snap, float delta,
                       Matrix4 projMatrix, int speedIndex) {
        entities.update(delta);
        effects.update(delta);

        batch.setProjectionMatrix(projMatrix);
        batch.begin();

        grid.render(batch, snap, GameConfig.showGrid, 0);
        entities.render(batch, snap);
        effects.render(batch);
        hud.render(batch, snap, speedIndex);

        batch.end();
    }

    // ─── جلوه‌های خارجی (توسط GameScreen فراخوانی می‌شوند) ──────────────────

    public void triggerExplosion(float cx, float cy, float radius) {
        effects.addExplosion(cx, cy, radius);
        entities.triggerShake(5f, 0.3f);
    }

    public void triggerHitFlash(float cx, float cy, float w, float h) {
        effects.addHitFlash(cx, cy, w, h);
    }

    public void triggerProjectileHit(float cx, float cy, String projectileType) {
        effects.addProjectileHit(cx, cy, projectileType);
    }

    public void triggerPlantFoodAura(float cx, float cy) {
        effects.addPlantFoodAura(cx, cy, 2.5f);
    }

    public void triggerBigImpact(float cx, float cy) {
        effects.addExplosion(cx, cy, 80f);
        entities.triggerShake(9f, 0.45f);
    }

    public void showTextPopup(float cx, float cy, String text,
                               com.badlogic.gdx.graphics.Color color) {
        effects.addTextPopup(cx, cy, text, color);
    }

    @Override
    public void dispose() {
        batch.dispose();
    }
}
