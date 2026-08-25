package com.pvz2.map.zones;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.pvz2.map.PvzMapObject;

import java.util.ArrayList;
import java.util.List;

/**
 * ناحیه گرید کاشت گیاه (Plant Grid Zone).
 *
 * شامل:
 *   - آرایه دوبعدی GridCell (rows × cols)
 *   - لیست تمام PLANT_SLOT object های پارس‌شده
 *   - متدهای کمکی برای تبدیل مختصات
 */
public final class GridZone {

    public static final int DEFAULT_ROWS = 5;
    public static final int DEFAULT_COLS = 9;

    private final int rows;
    private final int cols;
    private final float cellWidth;
    private final float cellHeight;

    /** مختصات گوشه پایین-چپ کل گرید در world space */
    private final float originX;
    private final float originY;

    /** [row][col] — row=0 پایین، col=0 چپ */
    private final GridCell[][] cells;

    /** تمام PLANT_SLOT object های خام از Tiled */
    private final List<PvzMapObject> plantSlotObjects;

    public GridZone(int rows, int cols,
                    float originX, float originY,
                    float cellWidth, float cellHeight) {
        this.rows       = rows;
        this.cols       = cols;
        this.originX    = originX;
        this.originY    = originY;
        this.cellWidth  = cellWidth;
        this.cellHeight = cellHeight;
        this.cells      = new GridCell[rows][cols];
        this.plantSlotObjects = new ArrayList<>();
    }

    // ─── Cell management ────────────────────────────────────

    public void setCell(int row, int col, GridCell cell) {
        if (isValidPos(row, col)) cells[row][col] = cell;
    }

    public GridCell getCell(int row, int col) {
        if (!isValidPos(row, col)) return null;
        return cells[row][col];
    }

    public boolean isValidPos(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    /** مرکز خانه در world space */
    public Vector2 getCellCenter(int row, int col) {
        return new Vector2(
            originX + col * cellWidth  + cellWidth  / 2f,
            originY + row * cellHeight + cellHeight / 2f
        );
    }

    /** ناحیه کل گرید */
    public Rectangle getTotalBounds() {
        return new Rectangle(originX, originY, cols * cellWidth, rows * cellHeight);
    }

    /**
     * یافتن خانه زیر نقطه world — برای input handling.
     * @return خانه یافت‌شده یا null
     */
    public GridCell getCellAtWorld(float worldX, float worldY) {
        if (worldX < originX || worldY < originY) return null;
        int col = (int) ((worldX - originX) / cellWidth);
        int row = (int) ((worldY - originY) / cellHeight);
        return getCell(row, col);
    }

    // ─── Object list ────────────────────────────────────────

    public void addPlantSlotObject(PvzMapObject obj) { plantSlotObjects.add(obj); }
    public List<PvzMapObject> getPlantSlotObjects()  { return plantSlotObjects; }

    // ─── Getters ────────────────────────────────────────────

    public int   getRows()       { return rows; }
    public int   getCols()       { return cols; }
    public float getOriginX()    { return originX; }
    public float getOriginY()    { return originY; }
    public float getCellWidth()  { return cellWidth; }
    public float getCellHeight() { return cellHeight; }
}
