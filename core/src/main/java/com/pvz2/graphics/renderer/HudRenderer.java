package com.pvz2.graphics.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.GameStateSnapshot;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.model.enums.LevelType;

/**
 * رندر نوار HUD بالای صفحه:
 * آفتاب | غذای گیاه | پیشرفت موج | سرعت | سکه/الماس
 * و اطلاعات مراحل ویژه (تایمر، تلفات گیاه، ...).
 */
public class HudRenderer {

    private static final Color BG       = new Color(0.08f, 0.05f, 0.02f, 0.9f);
    private static final Color SUN_COL  = new Color(1f,   0.88f, 0.1f,  1f);
    private static final Color FOOD_ON  = new Color(0.3f, 0.9f,  0.3f,  1f);
    private static final Color FOOD_OFF = new Color(0.2f, 0.3f,  0.2f,  0.6f);
    private static final Color WARN_COL = new Color(1f,   0.2f,  0.2f,  1f);
    private static final Color COIN_COL = new Color(1f,   0.82f, 0.1f,  1f);
    private static final Color GEM_COL  = new Color(0.35f, 0.85f, 1f,   1f);

    private final BitmapFont   font;
    private final GlyphLayout  layout = new GlyphLayout();

    public HudRenderer() {
        font = GameAssets.getInstance().getSkin().getFont("medium");
        if (font == null)
            throw new IllegalStateException("Font 'medium' not found in skin");
    }

    public void render(Batch batch, GameStateSnapshot snap, int speedIndex) {
        drawBackground(batch);
        drawSunCounter(batch, snap.sunCount);
        drawPlantFoodDots(batch, snap.plantFoodCount);
        drawWaveProgress(batch, snap);
        drawSpeedIndicator(batch, speedIndex);
        drawCurrencies(batch, snap.coinCount, snap.gemCount);
        drawSpecialLevelInfo(batch, snap);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void drawBackground(Batch batch) {
        TextureRegion w = GameAssets.getInstance().getWhiteRegion();
        // جستجو کنید: "IMAGE_UI_SEEDBANK_BACKGROUND" در asset browser
        batch.setColor(BG);
        batch.draw(w, 0, GameConstants.TOP_BAR_Y,
                GameConstants.VIEWPORT_WIDTH, GameConstants.TOP_BAR_H);
        batch.setColor(Color.WHITE);
    }

    private void drawSunCounter(Batch batch, int amount) {
        float x = 10f, y = GameConstants.TOP_BAR_Y + 54f;
        // جستجو کنید: "IMAGE_SUN_ICON" یا "REANIM_SUN" در asset browser
        drawCircle(batch, x + 20, y, 20, SUN_COL);
        drawText(batch, String.valueOf(amount), x + 46, y + 8, SUN_COL);
    }

    private void drawPlantFoodDots(Batch batch, int count) {
        float startX = 120f;
        float cy     = GameConstants.TOP_BAR_Y + 54f;
        for (int i = 0; i < 3; i++) {
            Color c = i < count ? FOOD_ON : FOOD_OFF;
            // جستجو کنید: "IMAGE_PLANTFOOD_ICON" در asset browser
            drawCircle(batch, startX + i * 28f, cy, 11, c);
        }
    }

    private void drawWaveProgress(Batch batch, GameStateSnapshot snap) {
        float bx = 200f, by = GameConstants.TOP_BAR_Y + 16f;
        float bw = 480f, bh = 18f;
        TextureRegion w = GameAssets.getInstance().getWhiteRegion();

        // پس‌زمینه نوار
        batch.setColor(0.2f, 0.1f, 0.05f, 0.85f);
        batch.draw(w, bx, by, bw, bh);

        // پیشرفت
        if (snap.zombieProgress > 0) {
            batch.setColor(0.85f, 0.12f, 0.12f, 1f);
            batch.draw(w, bx, by, bw * snap.zombieProgress, bh);
        }

        // نشانگر موج‌ها
        drawWaveMarkers(batch, w, bx, by, bw, bh, snap);

        // برچسب
        String waveLabel = snap.currentWave == snap.totalWaves
                ? "Final Wave!" : "Wave " + snap.currentWave + "/" + snap.totalWaves;
        drawText(batch, waveLabel, bx + bw * 0.5f - 30, by + bh + 14f, Color.WHITE);
    }

    private void drawWaveMarkers(Batch batch, TextureRegion w,
                                  float bx, float by, float bw, float bh,
                                  GameStateSnapshot snap) {
        if (snap.totalWaves <= 1) return;
        for (int i = 1; i < snap.totalWaves; i++) {
            float mx  = bx + bw * i / (float) snap.totalWaves;
            Color col = (i == snap.totalWaves - 1)
                    ? new Color(1f, 0.25f, 0.25f, 1f)   // موج پرچم = قرمز
                    : new Color(1f, 0.88f, 0.1f, 1f);   // موج معمولی = زرد
            batch.setColor(col);
            batch.draw(w, mx - 1.5f, by - 5, 3f, bh + 10);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawSpeedIndicator(Batch batch, int speedIndex) {
        String[] labels = {"1×", "1.5×", "2×"};
        float x = GameConstants.VIEWPORT_WIDTH - 210f;
        float y = GameConstants.TOP_BAR_Y + 38f;
        drawText(batch, "⚡" + labels[speedIndex], x, y, Color.YELLOW);
    }

    private void drawCurrencies(Batch batch, int coins, int gems) {
        float x = GameConstants.VIEWPORT_WIDTH - 160f;
        float y = GameConstants.TOP_BAR_Y + 70f;
        drawText(batch, String.valueOf(coins) + " 🪙", x, y, COIN_COL);
        drawText(batch, String.valueOf(gems)  + " 💎", x, y - 22, GEM_COL);
    }

    private void drawSpecialLevelInfo(Batch batch, GameStateSnapshot snap) {
        if (snap.levelType == null) return;
        switch (snap.levelType) {
            case TIMED_WAR:
                drawTimedWarInfo(batch, snap);
                break;
            case LOVE_YOUR_PLANTS:
                drawLovePlantsInfo(batch, snap);
                break;
            case DEAD_LINE:
                drawDeadlineInfo(batch, snap);
                break;
            default: break;
        }
    }

    private void drawTimedWarInfo(Batch batch, GameStateSnapshot snap) {
        float x = 700f, y = GameConstants.TOP_BAR_Y + 72f;
        String timerStr = String.format("⏱ %02d", snap.timedWarSeconds);
        drawText(batch, timerStr, x, y, snap.timedWarSeconds < 10 ? WARN_COL : Color.WHITE);

        if (snap.timedWarSunMode) {
            drawText(batch, "☀ " + snap.timedWarSun + "/" + snap.timedWarSunTarget,
                    x, y - 22, SUN_COL);
        } else {
            drawText(batch, "🧟 " + snap.timedWarKills + "/" + snap.timedWarTarget,
                    x, y - 22, Color.ORANGE);
        }
    }

    private void drawLovePlantsInfo(Batch batch, GameStateSnapshot snap) {
        float x = 700f, y = GameConstants.TOP_BAR_Y + 60f;
        int remaining = snap.maxPlantsAllowed - snap.plantsLost;
        Color col = remaining <= 1 ? WARN_COL : Color.WHITE;
        drawText(batch, "🌱 " + remaining + " plants allowed", x, y, col);
    }

    private void drawDeadlineInfo(Batch batch, GameStateSnapshot snap) {
        float x = 700f, y = GameConstants.TOP_BAR_Y + 58f;
        drawText(batch, "⚠ Forbidden Line!", x, y, WARN_COL);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void drawCircle(Batch batch, float cx, float cy, float r, Color col) {
        TextureRegion w = GameAssets.getInstance().getWhiteRegion();
        batch.setColor(col);
        batch.draw(w, cx - r, cy - r, r * 2, r * 2);
        batch.setColor(Color.WHITE);
    }

    private void drawText(Batch batch, String text, float x, float y, Color col) {
        if (font == null) return;
        font.setColor(col);
        font.draw(batch, text, x, y);
        font.setColor(Color.WHITE);
    }
}
