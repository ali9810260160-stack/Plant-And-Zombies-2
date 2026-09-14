package com.pvz2.graphics.actors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.model.enums.ChapterType;
import com.pvz2.model.enums.ZombieType;
import com.pvz2.view.game.anim.config.AnimConfigLoader;
import com.pvz2.view.game.anim.config.StateConfig;
import com.pvz2.view.game.anim.config.ZombieAnimConfig;
import com.pvz2.view.game.anim.controller.PamDrawUtil;

/**
 * کارتِ زامبیِ قابل‌کاشت برای مینی‌گیم «من زامبی» — قرینه‌ی {@link PlantCardActor}
 * ولی برای زامبی‌ها. مثلِ کارتِ گیاه: <b>تصویر</b> (انیمیشنِ idleِ واقعیِ زامبی +
 * overlayِ armor برای cone/bucket/block)، <b>قیمتِ خورشید</b>، و <b>overlayِ
 * recharge</b> (زمانِ باقی‌مانده تا آماده‌سازیِ مجدد، از پایین پر می‌شود).
 *
 * <p>تصویرِ زامبی از همان config رندرِ بازی ({@code character_animations.json}) با
 * {@link AnimConfigLoader#resolveZombieConfig} در فصلِ مصر resolve می‌شود؛ زامبی‌های
 * armor-دار (CONEHEAD/BUCKETHEAD/BLOCKHEAD) بدنه‌ی زامبیِ عادی + یک armor overlay
 * دارند (part از خودِ PAM با {@link PamDrawUtil#drawPart}).
 */
public class ZombieCardActor extends Actor {

    private static final Color COL_BG        = new Color(0.30f, 0.10f, 0.12f, 1f); // پس‌زمینه‌ی سرخ‌فامِ زامبی
    private static final Color COL_SELECTED  = new Color(0.95f, 0.85f, 0.2f, 1f);
    private static final Color COL_COOLDOWN  = new Color(0f, 0f, 0f, 0.62f);
    private static final Color COL_UNAFFORD  = new Color(0f, 0f, 0f, 0.42f);
    private static final Color FALLBACK_COL  = new Color(0.45f, 0.5f, 0.35f, 1f);

    private final ZombieType type;
    private final String pamPath;
    private final String clip;
    private final String armorPart;   // null اگر بدونِ armor
    private final BitmapFont font;

    private int     sunCost;
    private float   cooldownFraction; // 0=آماده، 1=تازه مصرف‌شده
    private boolean affordable = true;
    private boolean selected;
    private float   stateTime;

    public ZombieCardActor(ZombieType type, int sunCost, Skin skin) {
        this.type    = type;
        this.sunCost = sunCost;
        this.font    = GameAssets.getInstance().fontOf("default");

        String pam = null, cl = null, armor = null;
        ZombieAnimConfig cfg = loader().resolveZombieConfig(type, ChapterType.ANCIENT_EGYPT);
        // اگر این نوع در فصلِ مصر config نداشت، به بدنه‌ی زامبیِ عادی برگرد تا
        // کارت همیشه یک زامبیِ واقعی نشان دهد (نه مستطیلِ رنگی).
        if (cfg == null || cfg.pamPath == null || cfg.pamPath.isEmpty()) {
            cfg = loader().resolveZombieConfig(ZombieType.NORMAL, ChapterType.ANCIENT_EGYPT);
        }
        if (cfg != null) {
            pam = cfg.pamPath;
            StateConfig idle = cfg.states != null ? cfg.states.get("IDLE") : null;
            if (idle == null && cfg.states != null) idle = cfg.states.get("WALK");
            cl = idle != null ? idle.clip : null;
            armor = armorPartFor(cfg.forcedArmor);
        }
        this.pamPath   = pam;
        this.clip      = cl;
        this.armorPart = armor;
        setSize(GameConstants.CARD_W, GameConstants.CARD_H);
    }

    /** نگاشتِ armorِ اجباری به نامِ partِ PAM (پارت‌ها در PAMِ پایه‌ی زامبی موجودند). */
    private static String armorPartFor(String forcedArmor) {
        if (forcedArmor == null) return null;
        switch (forcedArmor.toUpperCase()) {
            case "CONE":   return "zombie_armor_cone_norm";
            case "BUCKET": return "zombie_armor_bucket_norm";
            case "BLOCK":  return "zombie_armor_brick_norm";
            default:       return null;
        }
    }

    private static AnimConfigLoader sharedLoader;
    private static AnimConfigLoader loader() {
        if (sharedLoader == null) sharedLoader = new AnimConfigLoader("data/character_animations.json");
        return sharedLoader;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        stateTime += delta;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float x = getX(), y = getY(), w = getWidth(), h = getHeight();
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();

        // ─── پس‌زمینه ─────────────────────────────────────────────────────
        batch.setColor(COL_BG.r, COL_BG.g, COL_BG.b, parentAlpha);
        batch.draw(white, x, y, w, h);

        // ─── تصویرِ زامبی (بدنه + armor overlay) ─────────────────────────
        batch.setColor(Color.WHITE);
        Object pam = GameAssets.getInstance().getPamPlayer();
        float ix = x + w * 0.5f;
        float iy = y + h * 0.12f;
        float ih = h * 0.82f;
        PamDrawUtil.draw(pam, batch, pamPath, clip, stateTime, ix, iy, true,
                w * 0.7f, ih, FALLBACK_COL);
        if (armorPart != null) {
            PamDrawUtil.drawPart(pam, batch, pamPath, clip, stateTime, ix, iy, armorPart, ih);
        }

        // ─── overlayِ recharge (از بالا به پایین تیره می‌شود) ────────────
        if (cooldownFraction > 0f) {
            float overlayH = h * cooldownFraction;
            batch.setColor(COL_COOLDOWN.r, COL_COOLDOWN.g, COL_COOLDOWN.b,
                    COL_COOLDOWN.a * parentAlpha);
            batch.draw(white, x, y + h - overlayH, w, overlayH);
        } else if (!affordable) {
            // خورشیدِ کافی نیست (ولی cooldown تمام شده) — کمی تیره
            batch.setColor(COL_UNAFFORD.r, COL_UNAFFORD.g, COL_UNAFFORD.b,
                    COL_UNAFFORD.a * parentAlpha);
            batch.draw(white, x, y, w, h);
        }

        // ─── حاشیه‌ی انتخاب ────────────────────────────────────────────────
        if (selected) drawBorder(batch, white, x, y, w, h, COL_SELECTED, 3f, parentAlpha);

        // ─── هزینه‌ی خورشید (پایین) ─────────────────────────────────────────
        if (font != null) {
            font.setColor(Color.YELLOW.r, Color.YELLOW.g, Color.YELLOW.b, parentAlpha);
            font.draw(batch, String.valueOf(sunCost), x + 3, y + 14);
            font.setColor(Color.WHITE);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawBorder(Batch batch, TextureRegion white, float x, float y,
                            float w, float h, Color col, float t, float alpha) {
        batch.setColor(col.r, col.g, col.b, alpha);
        batch.draw(white, x,         y,         w, t);
        batch.draw(white, x,         y + h - t, w, t);
        batch.draw(white, x,         y,         t, h);
        batch.draw(white, x + w - t, y,         t, h);
    }

    // ─── Setters / Getters ──────────────────────────────────────────────────
    public void setCooldownFraction(float f) { cooldownFraction = Math.max(0, Math.min(1, f)); }
    public void setAffordable(boolean v)     { affordable = v; }
    public void setSelected(boolean v)       { selected = v; }
    public void setSunCost(int v)            { sunCost = v; }

    public ZombieType getZombieType() { return type; }
    public boolean    isSelected()    { return selected; }
}
