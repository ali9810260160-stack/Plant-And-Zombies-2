package model;

import model.enums.ChapterType;
import model.enums.TileType;
import model.tiles.Tile;

import java.util.ArrayList;
import java.util.List;

/**
 * نقشه بازی - ماتریس خانه‌ها.
 * پیش‌فرض 5 ردیف x 9 ستون.
 */
public class GameMap {

    private int rows;
    private int cols;
    private Tile[][] tiles;
    private ChapterType chapter;
    private boolean[] lawnMowers;

    public GameMap(int rows, int cols, ChapterType chapter) {
        this.rows = rows;
        this.cols = cols;
        this.chapter = chapter;
        this.tiles = new Tile[rows][cols];
        this.lawnMowers = new boolean[rows];
        initializeTiles();
        initLawnMowers();
    }

    private void initializeTiles() {
        TileType defaultType = getDefaultTileType();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                tiles[r][c] = new Tile(c + 1, r + 1, defaultType);
            }
        }
    }

    private TileType getDefaultTileType() {
        if (chapter == null) {
            return TileType.EGYPT_NORMAL;
        }
        switch (chapter) {
            case ANCIENT_EGYPT: return TileType.EGYPT_NORMAL;
            case FROSTBITE_CAVES: return TileType.CAVE_NORMAL;
            case BIG_WAVE_BEACH: return TileType.BEACH_NORMAL;
            case DARK_AGES: return TileType.DARK_NORMAL;
            default: return TileType.EGYPT_NORMAL;
        }
    }

    private void initLawnMowers() {
        for (int i = 0; i < rows; i++) {
            lawnMowers[i] = true;
        }
    }

    /** خانه مشخص را برمی‌گرداند (x: ستون 1-based، y: ردیف 1-based) */
    public Tile getTile(int x, int y) {
        if (!isValidPosition(x, y)) {
            return null;
        }
        return tiles[y - 1][x - 1];
    }

    public boolean isValidPosition(int x, int y) {
        return x >= 1 && x <= cols && y >= 1 && y <= rows;
    }

    public Tile[] getRow(int y) {
        if (y < 1 || y > rows) {
            return null;
        }
        return tiles[y - 1];
    }

    public Tile[] getColumn(int x) {
        if (x < 1 || x > cols) {
            return null;
        }
        Tile[] col = new Tile[rows];
        for (int r = 0; r < rows; r++) {
            col[r] = tiles[r][x - 1];
        }
        return col;
    }

    /** خانه‌های در شعاع r از مرکز (cx, cy) را برمی‌گرداند */
    public List<Tile> getTilesInRadius(int cx, int cy, int radius) {
        List<Tile> result = new ArrayList<>();
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int nx = cx + dx;
                int ny = cy + dy;
                if (isValidPosition(nx, ny)) {
                    result.add(getTile(nx, ny));
                }
            }
        }
        return result;
    }

    public void setTileType(int x, int y, TileType type) {
        Tile t = getTile(x, y);
        if (t != null) {
            t.setType(type);
        }
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public Tile[][] getTiles() { return tiles; }
    public ChapterType getChapter() { return chapter; }
    public boolean[] getLawnMowers() { return lawnMowers; }

    public boolean isLawnMowerAvailable(int rowIndex) {
        return rowIndex >= 0 && rowIndex < rows && lawnMowers[rowIndex];
    }

    public void setLawnMowerUsed(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < rows) {
            lawnMowers[rowIndex] = false;
        }
    }
}
