package model;

import model.enums.ChapterType;
import model.tiles.Tile;

/**
 * نقشه بازی - ماتریس خانه‌ها.
 * پیش‌فرض 5 ردیف × 9 ستون.
 */
public class GameMap {

    /** تعداد ردیف‌های نقشه (پیش‌فرض 5) */
    private int rows;

    /** تعداد ستون‌های نقشه (پیش‌فرض 9) */
    private int cols;

    /** ماتریس خانه‌ها */
    private Tile[][] tiles;

    /** فصل متعلق به این نقشه */
    private ChapterType chapter;

    /**
     * آیا ماشین چمن‌زنی ردیف r هنوز فعال است.
     * اندیس 0 برای ردیف 1.
     */
    private boolean[] lawnMowers;

    public GameMap(int rows, int cols, ChapterType chapter) {
        this.rows = rows;
        this.cols = cols;
        this.chapter = chapter;
        this.tiles = new Tile[rows][cols];
        this.lawnMowers = new boolean[rows];
    }

    /** خانه مشخص را برمی‌گرداند (x: ستون، y: ردیف، 1-based) */
    public Tile getTile(int x, int y) { return null; }

    /** بررسی می‌کند مختصات در محدوده نقشه است */
    public boolean isValidPosition(int x, int y) { return false; }

    /** تمام خانه‌های یک ردیف را برمی‌گرداند */
    public Tile[] getRow(int y) { return null; }

    /** تمام خانه‌های یک ستون را برمی‌گرداند */
    public Tile[] getColumn(int x) { return null; }

    /** خانه‌های در شعاع r از مرکز (cx, cy) را برمی‌گرداند */
    public Tile[] getTilesInRadius(int cx, int cy, int radius) { return null; }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public Tile[][] getTiles() { return tiles; }
    public ChapterType getChapter() { return chapter; }
    public boolean[] getLawnMowers() { return lawnMowers; }
    public void setLawnMowerUsed(int row) { this.lawnMowers[row] = false; }
}
