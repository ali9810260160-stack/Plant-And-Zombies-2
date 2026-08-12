package com.pvz2.graphics.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.GameStateSnapshot;
import com.pvz2.graphics.GameStateSnapshot.TileType;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.graphics.util.GameCoords;

/**
 * رندر پس‌زمینه، کاشی‌ها، شبکه و چمن‌زن‌ها.
 *
 * <p>هر فصل رنگ پس‌زمینه اختصاصی دارد.
 * با وجود asset های بازی، texture های واقعی بارگذاری می‌شوند.
 */
public class GridRenderer {

    private final String chapter;

    // ─── رنگ‌های tile overlay ─────────────────────────────────────────────────
    private static final Color COL_WATER      = new Color(0.1f, 0.4f, 0.8f, 0.65f);
    private static final Color COL_ICY        = new Color(0.65f, 0.88f, 1f,  0.55f);
    private static final Color COL_SLIPPERY   = new Color(0.55f, 0.82f, 1f,  0.45f);
    private static final Color COL_TOMBSTONE  = new Color(0.45f, 0.42f, 0.4f, 0.6f);
    private static final Color COL_NECROMANCY = new Color(0.4f, 0.15f, 0.45f, 0.5f);
    private static final Color COL_LOW_SHORE  = new Color(0.3f, 0.55f, 0.35f, 0.5f);
    private static final Color COL_GRID_LINE  = new Color(1f, 0f, 0f, 0.22f);
    private static final Color COL_MOWER_ON   = new Color(0.45f, 0.9f, 0.2f, 1f);

    public GridRenderer(String chapter) {
        this.chapter = chapter;
    }

    public void render(Batch batch, GameStateSnapshot snap,
                       boolean showGrid, float stateTime) {
        drawBackground(batch);
        drawTileOverlays(batch, snap);
        if (showGrid) drawGridLines(batch);
        drawLawnMowers(batch, snap);
        drawSpecialLevelMarkers(batch, snap);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void drawBackground(Batch batch) {
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();

        // پس‌زمینه کامل صفحه
        // جستجو کنید: "BACKGROUND_{CHAPTER}" در asset browser
        // مثال: "BACKGROUND_EGYPT", "BACKGROUND_FROSTBITE", "BACKGROUND_BEACH", "BACKGROUND_DARK"
        // TextureRegion bg = GameAssets.getInstance().region("BACKGROUND_" + chapter.toUpperCase());
        // if (bg != whiteRegion) { batch.draw(bg, 0, 0, 1280, 720); return; }

        // Fallback: رنگ فصل
        batch.setColor(chapterBgColor());
        batch.draw(white, 0, 0, GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT);

        // رنگ تیره‌تر برای ناحیه grid
        batch.setColor(chapterGridColor());
        batch.draw(white, GameConstants.G_X, GameConstants.G_Y,
                GameConstants.TILE_COLS * GameConstants.TW,
                GameConstants.TILE_ROWS * GameConstants.TH);
        batch.setColor(Color.WHITE);
    }

    private void drawTileOverlays(Batch batch, GameStateSnapshot snap) {
        if (snap.tiles == null) return;
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();

        for (int c = 0; c < GameConstants.TILE_COLS; c++) {
            for (int r = 0; r < GameConstants.TILE_ROWS; r++) {
                TileType type = snap.tiles[c][r];
                Color col = tileOverlayColor(type);
                if (col == null) continue;
                // phase1: c+1, r+1
                float tx = GameCoords.tileLeft(c + 1);
                float ty = GameCoords.tileBottom(r + 1);
                batch.setColor(col.r, col.g, col.b, col.a);
                batch.draw(white, tx, ty, GameConstants.TW, GameConstants.TH);
            }
        }
        batch.setColor(Color.WHITE);
    }

    private void drawGridLines(Batch batch) {
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        batch.setColor(COL_GRID_LINE);
        float x0 = GameConstants.G_X;
        float y0 = GameConstants.G_Y;
        float gw = GameConstants.TILE_COLS * GameConstants.TW;
        float gh = GameConstants.TILE_ROWS * GameConstants.TH;

        for (int c = 0; c <= GameConstants.TILE_COLS; c++) {
            float lx = x0 + c * GameConstants.TW;
            batch.draw(white, lx, y0, 1.5f, gh);
        }
        for (int r = 0; r <= GameConstants.TILE_ROWS; r++) {
            float ly = y0 + r * GameConstants.TH;
            batch.draw(white, x0, ly, gw, 1.5f);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawLawnMowers(Batch batch, GameStateSnapshot snap) {
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        for (int r = 0; r < GameConstants.TILE_ROWS; r++) {
            if (!snap.lawnMowerActive[r]) continue;
            // جستجو کنید: "IMAGE_LAWNMOWER" در asset browser
            float mx = GameConstants.G_X - 46f;
            float my = GameCoords.tileBottom(r + 1) + GameConstants.TH * 0.2f;
            batch.setColor(COL_MOWER_ON);
            batch.draw(white, mx, my, 38f, GameConstants.TH * 0.6f);
        }
        batch.setColor(Color.WHITE);
    }

    /** مارکرهای مربوط به نوع مرحله (deadline، protected tiles، …) */
    private void drawSpecialLevelMarkers(Batch batch, GameStateSnapshot snap) {
        if (snap.levelType == null) return;
        switch (snap.levelType) {
            case DEAD_LINE:
                drawDeadlineLine(batch, snap.deadlineColumn);
                break;
            case SAVE_OUR_SEEDS:
                drawProtectedTileMarkers(batch, snap);
                break;
            case LOVE_YOUR_PLANTS:
                drawLovePlantsBorder(batch);
                break;
            default: break;
        }

        // باد یخی (غار)
        if (!snap.frostbiteWindRows.isEmpty()) {
            drawFrostbiteWind(batch, snap.frostbiteWindRows);
        }

        // گردباد مصر
        if (snap.egyptTornadoActive) {
            drawEgyptTornado(batch);
        }
    }

    private void drawDeadlineLine(Batch batch, int deadlineCol1Based) {
        if (deadlineCol1Based <= 0) return;
        float lx = GameCoords.tileLeft(deadlineCol1Based);
        batch.setColor(1f, 0.05f, 0.05f, 0.8f);
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        batch.draw(white, lx, GameConstants.G_Y,
                3f, GameConstants.TILE_ROWS * GameConstants.TH);
        batch.setColor(Color.WHITE);
    }

    private void drawProtectedTileMarkers(Batch batch, GameStateSnapshot snap) {
        // گیاهان حفاظت‌شده با کادر قرمز مشخص می‌شوند
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        for (GameStateSnapshot.PlantInfo p : snap.plants) {
            float tx = GameCoords.tileLeft(p.phase1X);
            float ty = GameCoords.tileBottom(p.phase1Y);
            batch.setColor(1f, 0f, 0f, 0.5f);
            batch.draw(white, tx, ty, GameConstants.TW, 3);          // bottom
            batch.draw(white, tx, ty + GameConstants.TH - 3, GameConstants.TW, 3); // top
            batch.draw(white, tx, ty, 3, GameConstants.TH);          // left
            batch.draw(white, tx + GameConstants.TW - 3, ty, 3, GameConstants.TH); // right
        }
        batch.setColor(Color.WHITE);
    }

    private void drawLovePlantsBorder(Batch batch) {
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        batch.setColor(0.9f, 0.5f, 0.05f, 0.35f);
        float gx = GameConstants.G_X, gy = GameConstants.G_Y;
        float gw = GameConstants.TILE_COLS * GameConstants.TW;
        float gh = GameConstants.TILE_ROWS * GameConstants.TH;
        batch.draw(white, gx, gy, gw, 4);
        batch.draw(white, gx, gy + gh - 4, gw, 4);
        batch.draw(white, gx, gy, 4, gh);
        batch.draw(white, gx + gw - 4, gy, 4, gh);
        batch.setColor(Color.WHITE);
    }

    private void drawFrostbiteWind(Batch batch, java.util.List<Integer> rows) {
        // جستجو کنید: "IMAGE_FROSTBITE_WIND" در asset browser
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        for (int row : rows) {
            float ty = GameCoords.tileBottom(row);
            batch.setColor(0.7f, 0.92f, 1f, 0.4f);
            batch.draw(white, GameConstants.G_X, ty,
                    GameConstants.TILE_COLS * GameConstants.TW, GameConstants.TH);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawEgyptTornado(Batch batch) {
        // جستجو کنید: "IMAGE_TORNADO" یا "ANIM_TORNADO" در asset browser
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        batch.setColor(0.9f, 0.8f, 0.5f, 0.45f);
        // محل گردباد (سمت راست صفحه)
        batch.draw(white, GameConstants.G_X + GameConstants.TILE_COLS * GameConstants.TW - 60,
                GameConstants.G_Y, 60, GameConstants.TILE_ROWS * GameConstants.TH);
        batch.setColor(Color.WHITE);
    }

    // ─── Colors ──────────────────────────────────────────────────────────────

    private Color chapterBgColor() {
        switch (chapter != null ? chapter.toLowerCase() : "") {
            case "ancient_egypt": return new Color(0.78f, 0.68f, 0.44f, 1f);
            case "frostbite_caves": return new Color(0.55f, 0.72f, 0.88f, 1f);
            case "big_wave_beach": return new Color(0.42f, 0.68f, 0.82f, 1f);
            case "dark_ages":     return new Color(0.2f,  0.17f, 0.12f, 1f);
            default:              return new Color(0.3f,  0.48f, 0.22f, 1f);
        }
    }

    private Color chapterGridColor() {
        switch (chapter != null ? chapter.toLowerCase() : "") {
            case "ancient_egypt": return new Color(0.68f, 0.58f, 0.34f, 1f);
            case "frostbite_caves": return new Color(0.45f, 0.62f, 0.78f, 1f);
            case "big_wave_beach": return new Color(0.35f, 0.6f,  0.35f, 1f);
            case "dark_ages":     return new Color(0.15f, 0.12f, 0.08f, 1f);
            default:              return new Color(0.25f, 0.42f, 0.16f, 1f);
        }
    }

    private Color tileOverlayColor(TileType type) {
        if (type == null) return null;
        switch (type) {
            case WATER:        return COL_WATER;
            case ICY_GROUND:   return COL_ICY;
            case SLIPPERY_UP:
            case SLIPPERY_DOWN:return COL_SLIPPERY;
            case TOMBSTONE:
            case DARK_TOMBSTONE: return COL_TOMBSTONE;
            case NECROMANCY:   return COL_NECROMANCY;
            case LOW_SHORE:    return COL_LOW_SHORE;
            default:           return null;
        }
    }
}
