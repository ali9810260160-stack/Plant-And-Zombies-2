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

    private static final Color PANEL_BG   = new Color(0.05f, 0.03f, 0.01f, 0.72f);
    private static final Color PANEL_EDGE = new Color(0.55f, 0.40f, 0.15f, 0.9f);
    private static final Color SLOT_EMPTY = new Color(0.12f, 0.14f, 0.10f, 0.75f);

    private final BitmapFont   font;
    private final GlyphLayout  layout = new GlyphLayout();

    // ─── آیکون‌ها/قاب‌های واقعیِ HUD ─────────────────────────────────────────────
    private final TextureRegion sunIcon;    // خورشیدِ درخشانِ بانک
    private final TextureRegion foodBank;   // پنلِ سبزِ غذای گیاه (برگ + ۳ سوکت)
    private final TextureRegion foodDot;    // برگِ روشن برای سوکتِ پُر
    private final TextureRegion addBtn;     // دکمه‌ی + کنارِ بانک
    private final TextureRegion meterFrame; // قابِ خالیِ نوارِ موج
    private final TextureRegion meterFill;  // کاشیِ سبزِ پُرشونده
    private final TextureRegion meterHead;  // سرِ زامبیِ نشانگر
    private final TextureRegion meterFlag;  // پرچمِ قرمزِ موج
    private final TextureRegion coinFrame;  // قابِ نمایشِ سکه
    private final TextureRegion coinIcon;
    private final TextureRegion gemIcon;

    public HudRenderer() {
        // GameAssets.fontOf(...) هرگز throw نمی‌کند — نگاه کنید به توضیح آنجا
        // (باگ قبلی: skin.getFont("medium") چون "medium" اسم Label style بود نه
        // فونت resource، همان لحظه ساخت GameScreen کل بازی را crash می‌کرد)
        font = GameAssets.getInstance().fontOf("medium");
        GameAssets a = GameAssets.getInstance();
        // خورشیدِ HUD ابتدا از اتلسِ اصلی؛ اگر نبود، آیکونِ کارتِ خورشیدِ لوکال.
        TextureRegion sun = a.region("IMAGE_UI_HUD_INGAME_SUN_DOWN");
        sunIcon    = has(sun) ? sun : a.local("Exports/droped/card_sun.png");
        foodBank   = a.local("Exports/in game/plantfood_bank.png");
        foodDot    = a.local("Exports/plantfood/plantfood_button.png");
        addBtn     = a.local("Exports/in game/add.png");
        meterFrame = a.local("Exports/in game/progress_meter.png");
        meterFill  = a.local("Exports/in game/progress_meter_fill.png");
        meterHead  = a.local("Exports/in game/progress_meter_zombiehead.png");
        meterFlag  = a.local("Exports/in game/progress_meter_flag_default.png");
        TextureRegion cf = a.region("IMAGE_UI_GENERIC_BUTTONS_COIN_BUY_NORMAL");
        coinFrame  = has(cf) ? cf : null;
        coinIcon   = a.local("quests/coin_icon.png");
        gemIcon    = a.local("quests/gem_icon.png");
    }

    /** آیا region واقعی است (نه fallbackِ whiteRegion)؟ */
    private boolean has(TextureRegion r) {
        return r != null && r != GameAssets.getInstance().getWhiteRegion();
    }

    public void render(Batch batch, GameStateSnapshot snap, int speedIndex) {
        drawSunCounter(batch, snap.sunCount);
        drawPlantFoodBank(batch, snap.plantFoodCount);
        // در مراحلِ رئیس، نوارِ موج بی‌معنی است (موجِ عادی ندارد) — نوارِ سلامتیِ
        // ۳-بخشیِ رئیس جایگزینِ آن است.
        if (!snap.bossActive) drawWaveProgress(batch, snap);
        drawSpeedIndicator(batch, speedIndex);
        drawCurrencies(batch, snap.coinCount, snap.gemCount);
        drawSpecialLevelInfo(batch, snap);
        if (snap.bossActive) drawBossHealthBar(batch, snap);
    }

    // ─── نوارِ سلامتیِ ۳-بخشیِ رئیس (Zomboss) ─────────────────────────────────────
    private static final Color BOSS_SEG_FULL  = new Color(0.88f, 0.13f, 0.13f, 1f);
    private static final Color BOSS_SEG_DRAIN  = new Color(1f, 0.6f, 0.1f, 1f);
    private static final Color BOSS_SEG_EMPTY = new Color(0.18f, 0.05f, 0.05f, 0.85f);
    private static final Color BOSS_BAR_BG    = new Color(0f, 0f, 0f, 0.6f);

    private void drawBossHealthBar(Batch batch, GameStateSnapshot snap) {
        TextureRegion w = GameAssets.getInstance().getWhiteRegion();
        float barW = 460f, barH = 22f, gap = 4f;
        float bx = (GameConstants.VIEWPORT_WIDTH - barW) / 2f;
        float by = GameConstants.TOP_BAR_Y + 18f;

        // پس‌زمینه
        batch.setColor(BOSS_BAR_BG);
        batch.draw(w, bx - 6, by - 6, barW + 12, barH + 12);

        // ۳ بخش (از راست تخلیه می‌شوند)
        int segs = 3;
        float segW = (barW - gap * (segs - 1)) / segs;
        float fill3 = snap.bossHealthFraction * segs; // ۰..۳
        for (int i = 0; i < segs; i++) {
            float sx = bx + i * (segW + gap);
            batch.setColor(BOSS_SEG_EMPTY);
            batch.draw(w, sx, by, segW, barH);
            float f = Math.max(0f, Math.min(1f, fill3 - i));
            if (f > 0f) {
                boolean draining = f < 1f && (i == (int) Math.ceil(fill3) - 1);
                batch.setColor(draining ? BOSS_SEG_DRAIN : BOSS_SEG_FULL);
                batch.draw(w, sx, by, segW * f, barH);
            }
        }
        batch.setColor(Color.WHITE);

        boolean dazed = "stunned".equals(snap.bossState);
        String label = dazed
                ? "😵 ZOMBOSS DAZED! — Phase " + snap.bossPhase + "/3"
                : "👹 ZOMBOSS — Phase " + snap.bossPhase + "/3";
        layout.setText(font, label);
        drawText(batch, label, bx + (barW - layout.width) / 2f, by + barH + 18f, WARN_COL);
    }

    // ─────────────────────────────────────────────────────────────────────────

    /** بانکِ خورشید: خورشیدِ درخشانِ بزرگ + عدد (بدونِ جعبه، مطابقِ مرجع). */
    private void drawSunCounter(Batch batch, int amount) {
        float cx = 52f, cy = GameConstants.TOP_BAR_Y + 52f;
        drawIconCentered(batch, sunIcon, cx, cy, 68f);
        String s = String.valueOf(amount);
        layout.setText(font, s);
        drawTextShadow(batch, s, cx + 46f, cy + layout.height / 2f, Color.WHITE);
    }

    /** بانکِ غذای گیاه: پنلِ سبزِ واقعی + سوکت‌های روشن به تعدادِ موجود + دکمه‌ی +. */
    private void drawPlantFoodBank(Batch batch, int count) {
        float ph = 58f, pw = has(foodBank) ? ph * (206f / 88f) : 135f;   // نسبتِ اصلی
        float px = 16f, py = 12f;                                        // پایین-چپ (زیرِ ستونِ کارت‌ها)
        float cy = py + ph / 2f;
        batch.setColor(Color.WHITE);
        if (has(foodBank)) batch.draw(foodBank, px, py, pw, ph);

        // سه سوکت در نیمه‌ی راستِ پنل روشن می‌شوند (تخمینِ هندسه‌ی داخلِ پنل)
        float slot0 = px + pw * 0.42f;
        float slotGap = pw * 0.17f;
        float dotH = ph * 0.42f;
        int lit = Math.max(0, Math.min(3, count));
        for (int i = 0; i < lit; i++) {
            drawIconCentered(batch, foodDot, slot0 + i * slotGap, cy, dotH);
        }
        // دکمه‌ی + سمتِ راستِ پنل
        if (has(addBtn)) {
            float aH = ph * 0.9f;
            drawIconCentered(batch, addBtn, px + pw + aH * 0.55f, cy, aH);
        }
        batch.setColor(Color.WHITE);
    }

    /** نوارِ پیشرفتِ موج با قاب/پُرکننده/سرِ زامبی/پرچمِ واقعی (مطابقِ مرجع). */
    private void drawWaveProgress(Batch batch, GameStateSnapshot snap) {
        float bw = 300f, bh = has(meterFrame) ? bw * (33f / 273f) : 26f;
        float bx = (GameConstants.VIEWPORT_WIDTH - bw) / 2f;
        float by = GameConstants.TOP_BAR_Y + 40f;
        float prog = Math.max(0f, Math.min(1f, snap.zombieProgress));

        // کانالِ داخلیِ قاب (تخمین)
        float insetX = bw * 0.045f, insetY = bh * 0.28f;
        float chX = bx + insetX, chY = by + insetY;
        float chW = bw - insetX * 2f, chH = bh - insetY * 2f;

        batch.setColor(Color.WHITE);
        if (has(meterFill) && prog > 0f) {
            batch.draw(meterFill, chX, chY, chW * prog, chH);
        } else if (prog > 0f) {
            TextureRegion w = GameAssets.getInstance().getWhiteRegion();
            batch.setColor(0.4f, 0.85f, 0.2f, 1f);
            batch.draw(w, chX, chY, chW * prog, chH);
            batch.setColor(Color.WHITE);
        }
        if (has(meterFrame)) batch.draw(meterFrame, bx, by, bw, bh);

        // پرچم‌های موج در مرزهای موج
        if (has(meterFlag) && snap.totalWaves > 1) {
            float fH = bh * 1.5f, fW = fH * (27f / 22f);
            for (int i = 1; i < snap.totalWaves; i++) {
                float mx = chX + chW * i / (float) snap.totalWaves;
                batch.draw(meterFlag, mx - fW / 2f, by + bh * 0.35f, fW, fH);
            }
        }
        // سرِ زامبیِ نشانگر روی موقعیتِ پیشرفت
        if (has(meterHead)) {
            float hH = bh * 1.9f, hW = hH * (42f / 45f);
            float hx = chX + chW * prog;
            batch.draw(meterHead, hx - hW / 2f, by + (bh - hH) / 2f, hW, hH);
        }

        String waveLabel = snap.currentWave == snap.totalWaves
                ? "Final Wave!" : "Wave " + snap.currentWave + "/" + snap.totalWaves;
        layout.setText(font, waveLabel);
        drawTextShadow(batch, waveLabel, bx + (bw - layout.width) / 2f, by - 6f, Color.WHITE);
    }

    private void drawSpeedIndicator(Batch batch, int speedIndex) {
        // دکمه‌های واقعیِ سرعت/توقف در GameScreen رندر می‌شوند؛ اینجا چیزی نمی‌کشیم.
    }

    /**
     * سکه در قابِ واقعی (بالا-راست) + الماس زیرِ آن.
     * قابِ سکه از اتلس؛ اگر نبود، فقط آیکون+عدد.
     */
    private void drawCurrencies(Batch batch, int coins, int gems) {
        // چپِ ردیفِ دکمه‌های ابزار (که در GameScreen رندر می‌شوند) قرار می‌گیرد.
        float coinCy = GameConstants.TOP_BAR_Y + 68f;
        float gemCy  = GameConstants.TOP_BAR_Y + 32f;
        batch.setColor(Color.WHITE);

        if (coinFrame != null) {
            float fh = 40f, fw = fh * (coinFrame.getRegionWidth() / (float) coinFrame.getRegionHeight());
            float fx = GameConstants.VIEWPORT_WIDTH - 360f;
            batch.draw(coinFrame, fx, coinCy - fh / 2f, fw, fh);
            String s = String.valueOf(coins);
            drawTextShadow(batch, s, fx + fw * 0.42f, coinCy + 7f, Color.WHITE);
            drawIconCentered(batch, gemIcon, fx + 12f, gemCy, 28f);
            drawCurrencyNum(batch, String.valueOf(gems), fx + 30f, gemCy, GEM_COL);
        } else {
            float iconX = GameConstants.VIEWPORT_WIDTH - 290f;
            drawIconCentered(batch, coinIcon, iconX, coinCy, 32f);
            drawCurrencyNum(batch, String.valueOf(coins), iconX + 24f, coinCy, COIN_COL);
            drawIconCentered(batch, gemIcon, iconX, gemCy, 28f);
            drawCurrencyNum(batch, String.valueOf(gems), iconX + 22f, gemCy, GEM_COL);
        }
    }

    private void drawCurrencyNum(Batch batch, String s, float x, float cy, Color col) {
        layout.setText(font, s);
        drawText(batch, s, x, cy + layout.height / 2f, col);
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

    /** آیکون را با حفظِ نسبتِ ابعاد، در ارتفاعِ هدف، حولِ مرکز (cx,cy) می‌کشد. */
    private void drawIconCentered(Batch batch, TextureRegion region, float cx, float cy,
                                  float targetH) {
        if (region == null) return;
        float aspect = region.getRegionHeight() > 0
                ? region.getRegionWidth() / (float) region.getRegionHeight() : 1f;
        float h = targetH, wdt = h * aspect;
        batch.setColor(Color.WHITE);
        batch.draw(region, cx - wdt / 2f, cy - h / 2f, wdt, h);
    }

    /** پنلِ نیمه‌شفافِ HUD: بدنه‌ی تیره + حاشیه‌ی طلاییِ نازک. */
    private void drawRoundedPanel(Batch batch, float x, float y, float w, float h,
                                  Color body, Color edge) {
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        batch.setColor(body);
        batch.draw(white, x, y, w, h);
        batch.setColor(edge);
        batch.draw(white, x, y + h - 2f, w, 2f);   // بالا
        batch.draw(white, x, y, w, 2f);            // پایین
        batch.draw(white, x, y, 2f, h);            // چپ
        batch.draw(white, x + w - 2f, y, 2f, h);   // راست
        batch.setColor(Color.WHITE);
    }

    private void drawText(Batch batch, String text, float x, float y, Color col) {
        if (font == null) return;
        font.setColor(col);
        font.draw(batch, text, x, y);
        font.setColor(Color.WHITE);
    }

    /** متن با سایه‌ی تیره — روی پس‌زمینه‌ی شلوغِ بازی خوانا می‌ماند (بدونِ نوارِ تیره). */
    private void drawTextShadow(Batch batch, String text, float x, float y, Color col) {
        if (font == null) return;
        font.setColor(0f, 0f, 0f, 0.75f);
        font.draw(batch, text, x + 1.5f, y - 1.5f);
        font.setColor(col);
        font.draw(batch, text, x, y);
        font.setColor(Color.WHITE);
    }
}
