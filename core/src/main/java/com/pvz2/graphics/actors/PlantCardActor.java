package com.pvz2.graphics.actors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.graphics.util.PamPaths;
import com.pvz2.model.enums.PlantType;

/**
 * کارت گیاه — قابل‌استفاده مجدد در سه بافت (داک فاز ۲):
 * <ol>
 *   <li>Seed Bank داخل بازی</li>
 *   <li>صفحه انتخاب گیاه</li>
 *   <li>صفحه کلکسیون (almanac)</li>
 * </ol>
 */
public class PlantCardActor extends Actor {

    private static final Color COL_READY    = new Color(0.12f, 0.42f, 0.12f, 1f);
    private static final Color COL_LOCKED   = new Color(0.25f, 0.25f, 0.25f, 1f);
    private static final Color COL_SELECTED = new Color(0.2f,  0.8f,  0.2f,  1f);
    private static final Color COL_BOOSTED  = new Color(1f,    0.85f, 0.1f,  1f);
    private static final Color COL_COOLDOWN = new Color(0f,    0f,    0f,    0.62f);

    private final PlantType plantType;
    private final String    pamPath;
    private final BitmapFont font;

    private int     sunCost;
    private float   cooldownFraction; // 0=آماده، 1=تازه کاشته
    private boolean locked;
    private boolean selected;
    private boolean boosted;
    private int     level;            // سطح ارتقا (0..2)
    private float   stateTime;

    public PlantCardActor(PlantType type, int sunCost, Skin skin) {
        this.plantType = type;
        this.sunCost   = sunCost;
        this.pamPath   = PamPaths.forPlant(type.name().toLowerCase());
        this.font      = skin.getFont("default");
        setSize(GameConstants.CARD_W, GameConstants.CARD_H);
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
        Color bg = locked ? COL_LOCKED : COL_READY;
        batch.setColor(bg.r, bg.g, bg.b, parentAlpha);
        batch.draw(white, x, y, w, h);

        // ─── تصویر گیاه ───────────────────────────────────────────────────
        if (!locked) drawPlantImage(batch, x, y, w, h, parentAlpha);

        // ─── Cooldown overlay ─────────────────────────────────────────────
        if (cooldownFraction > 0 && !locked) {
            float overlayH = h * cooldownFraction;
            batch.setColor(COL_COOLDOWN.r, COL_COOLDOWN.g,
                    COL_COOLDOWN.b, COL_COOLDOWN.a * parentAlpha);
            batch.draw(white, x, y + h - overlayH, w, overlayH);
        }

        // ─── حاشیه انتخاب یا بوست ─────────────────────────────────────
        if (selected)      drawBorder(batch, white, x, y, w, h, COL_SELECTED, 3f, parentAlpha);
        else if (boosted)  drawBorder(batch, white, x, y, w, h, COL_BOOSTED,  3f, parentAlpha);

        // ─── هزینه خورشید (پایین) ────────────────────────────────────────
        batch.setColor(Color.WHITE.r, Color.WHITE.g, Color.WHITE.b, parentAlpha);
        if (font != null && !locked) {
            font.setColor(Color.YELLOW.r, Color.YELLOW.g, Color.YELLOW.b, parentAlpha);
            font.draw(batch, String.valueOf(sunCost), x + 3, y + 14);
            font.setColor(Color.WHITE);
        }

        // ─── سطح ارتقا (گوشه بالا-راست) ─────────────────────────────────
        if (level > 0 && font != null) {
            font.setColor(Color.GOLD.r, Color.GOLD.g, Color.GOLD.b, parentAlpha);
            font.draw(batch, "Lv" + (level + 1), x + w - 28, y + h - 2);
            font.setColor(Color.WHITE);
        }

        // ─── قفل ──────────────────────────────────────────────────────────
        if (locked) {
            batch.setColor(1f, 1f, 0.3f, parentAlpha * 0.9f);
            batch.draw(white, x + w * 0.38f, y + h * 0.3f, w * 0.24f, h * 0.35f);
        }

        batch.setColor(Color.WHITE);
    }

    private void drawPlantImage(Batch batch, float x, float y, float w, float h, float alpha) {
        GameAssets assets = GameAssets.getInstance();
        if (assets.hasPvzAssets() && pamPath != null && !pamPath.isEmpty()) {
            batch.setColor(1, 1, 1, alpha);
            assets.getPamPlayer().draw(batch, pamPath, "idle",
                    stateTime, x + w * 0.5f, y + h * 0.58f, false);
        } else {
            batch.setColor(0.3f, 0.75f, 0.3f, alpha * 0.8f);
            TextureRegion white = assets.getWhiteRegion();
            batch.draw(white, x + 8, y + 18, w - 16, h - 28);
        }
    }

    private void drawBorder(Batch batch, TextureRegion white, float x, float y,
                             float w, float h, Color col, float t, float alpha) {
        batch.setColor(col.r, col.g, col.b, alpha);
        batch.draw(white, x,         y,         w,  t);
        batch.draw(white, x,         y + h - t, w,  t);
        batch.draw(white, x,         y,         t,  h);
        batch.draw(white, x + w - t, y,         t,  h);
    }

    // ─── Setters ──────────────────────────────────────────────────────────────
    public void setCooldownFraction(float f) { cooldownFraction = Math.max(0, Math.min(1, f)); }
    public void setLocked(boolean v)         { locked = v; }
    public void setSelected(boolean v)       { selected = v; }
    public void setBoosted(boolean v)        { boosted = v; }
    public void setSunCost(int v)            { sunCost = v; }
    public void setUpgradeLevel(int v)       { level = v; }

    // ─── Getters ──────────────────────────────────────────────────────────────
    public PlantType getPlantType()      { return plantType; }
    public boolean   isLocked()          { return locked; }
    public boolean   isReady()           { return !locked && cooldownFraction == 0f; }
    public boolean   isSelected()        { return selected; }
}
