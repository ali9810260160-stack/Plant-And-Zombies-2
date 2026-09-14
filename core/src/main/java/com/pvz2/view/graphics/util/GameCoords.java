package com.pvz2.graphics.util;

import com.badlogic.gdx.math.Vector2;
import com.pvz2.graphics.GameConstants;

/**
 * تبدیل مختصات بین فضای بازی (فاز ۱) و فضای صفحه (LibGDX).
 *
 * <p><b>فاز ۱:</b> col ۱..۹ (چپ=خانه → راست=زامبی)، row ۱..۵ (بالا → پایین)
 * <p><b>LibGDX:</b> y افزایشی به بالا، مبداء پایین-چپ
 *
 * <p>مرکز کاشی (col, row) در فضای صفحه:
 * <pre>
 *   screenX = G_X + (col-1)*TW + TW/2
 *   screenY = G_Y + (TILE_ROWS - row)*TH + TH/2
 * </pre>
 */
public final class GameCoords {

    private GameCoords() {}

    // ─── فاز ۱ → صفحه (مرکز کاشی) ─────────────────────────────────────────

    /** col ۱-based → مرکز x روی صفحه */
    public static float toScreenX(double phase1Col) {
        return GameConstants.G_X + (float)(phase1Col - 1) * GameConstants.TW
                + GameConstants.TW * 0.5f;
    }

    /** row ۱-based → مرکز y روی صفحه */
    public static float toScreenY(int phase1Row) {
        return GameConstants.G_Y
                + (GameConstants.TILE_ROWS - phase1Row) * GameConstants.TH
                + GameConstants.TH * 0.5f;
    }

    /** گوشه چپ-پایین کاشی (col ۱-based) → x صفحه */
    public static float tileLeft(int phase1Col) {
        return GameConstants.G_X + (phase1Col - 1) * GameConstants.TW;
    }

    /** گوشه چپ-پایین کاشی (row ۱-based) → y صفحه */
    public static float tileBottom(int phase1Row) {
        return GameConstants.G_Y + (GameConstants.TILE_ROWS - phase1Row) * GameConstants.TH;
    }

    // ─── صفحه → فاز ۱ (برای mouse input) ───────────────────────────────────

    /** x صفحه → col ۱-based (کلمپ شده ۱..COLS) */
    public static int toPhase1Col(float screenX) {
        int col = (int) ((screenX - GameConstants.G_X) / GameConstants.TW) + 1;
        return Math.max(1, Math.min(GameConstants.TILE_COLS, col));
    }

    /** y صفحه → row ۱-based (کلمپ شده ۱..ROWS) */
    public static int toPhase1Row(float screenY) {
        int row = GameConstants.TILE_ROWS
                - (int) ((screenY - GameConstants.G_Y) / GameConstants.TH);
        return Math.max(1, Math.min(GameConstants.TILE_ROWS, row));
    }

    /** آیا مختصات صفحه داخل شبکه قرار دارد؟ */
    public static boolean inGrid(float sx, float sy) {
        return sx >= GameConstants.G_X
                && sx <= GameConstants.G_X + GameConstants.TILE_COLS * GameConstants.TW
                && sy >= GameConstants.G_Y
                && sy <= GameConstants.G_Y + GameConstants.TILE_ROWS * GameConstants.TH;
    }

    /** آیا مختصات صفحه داخل seed bank قرار دارد؟ */
    public static boolean inSeedBank(float sx, float sy) {
        return sy >= 0 && sy <= GameConstants.SB_H;
    }

    /** تبدیل batch توسط viewport (برای input processor) */
    public static Vector2 unproject(com.badlogic.gdx.utils.viewport.Viewport vp,
                                     int screenX, int screenY) {
        return vp.unproject(new Vector2(screenX, screenY));
    }
}
