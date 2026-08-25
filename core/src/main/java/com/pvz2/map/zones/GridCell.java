package com.pvz2.map.zones;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.pvz2.model.enums.PlantType;

import java.util.Set;

/**
 * یک خانه (Cell) در گرید کاشت گیاه.
 *
 * مختصات:
 *   - row=0 پایین‌ترین لاین (در libGDX Y-up)
 *   - col=0 چپ‌ترین ستون
 *
 * این کلاس فقط داده‌های ایستای نقشه را نگه می‌دارد.
 * وضعیت دینامیک (گیاه کاشته‌شده، cooldown و...) در GameService مدیریت می‌شود.
 */
public final class GridCell {

    // ─── موقعیت در گرید ────────────────────────────────────
    private final int row;   // 0 = پایین (در libGDX)
    private final int col;   // 0 = چپ

    // ─── موقعیت در دنیا ────────────────────────────────────
    /** گوشه پایین-چپ خانه در world space */
    private final Rectangle worldBounds;

    // ─── نوع خاک ──────────────────────────────────────────
    private final SoilType soilType;

    // ─── ویژگی‌های خاص ─────────────────────────────────────
    /** آیا این خانه Power Tile است؟ گیاه روی آن boost می‌گیرد */
    private final boolean isPowerTile;

    /** گیاهان مجاز — null یعنی همه */
    private final Set<PlantType> allowedPlants;

    /** آیا کاشت روی این خانه ممنوع است؟ */
    private final boolean isBlocked;

    /** variant تصادفی انتخاب‌شده توسط TileVariantResolver */
    private int tileVariant;

    public enum SoilType {
        /** خاک معمولی */
        NORMAL,
        /** آب — بیشتر گیاهان نیاز به Lily Pad دارند */
        WATER,
        /** یخ — زمین لیز */
        ICE,
        /** ماسه — کمی تأثیر بصری متفاوت */
        SAND,
        /** Power tile — boost دادن به گیاه */
        POWER,
        /** قفل — کاشت ممنوع */
        BLOCKED
    }

    private GridCell(Builder b) {
        this.row           = b.row;
        this.col           = b.col;
        this.worldBounds   = b.worldBounds;
        this.soilType      = b.soilType;
        this.isPowerTile   = b.isPowerTile || b.soilType == SoilType.POWER;
        this.allowedPlants = b.allowedPlants;
        this.isBlocked     = b.isBlocked   || b.soilType == SoilType.BLOCKED;
        this.tileVariant   = 0;
    }

    // ─── Getters ───────────────────────────────────────────

    public int       getRow()         { return row; }
    public int       getCol()         { return col; }
    public Rectangle getWorldBounds() { return worldBounds; }
    public SoilType  getSoilType()    { return soilType; }
    public boolean   isPowerTile()    { return isPowerTile; }
    public boolean   isBlocked()      { return isBlocked; }
    public boolean   isWater()        { return soilType == SoilType.WATER; }
    public int       getTileVariant() { return tileVariant; }

    /** مرکز خانه در world space — نقطه spawn گیاه */
    public Vector2 getCenterWorld() {
        return new Vector2(worldBounds.x + worldBounds.width  / 2f,
                           worldBounds.y + worldBounds.height / 2f);
    }

    /**
     * آیا گیاه مشخص‌شده روی این خانه قابل کاشت است؟
     * (چک اضافه — منطق اصلی در PlantService انجام می‌شود)
     */
    public boolean canAcceptPlant(PlantType type) {
        if (isBlocked) return false;
        if (allowedPlants == null) return true;
        return allowedPlants.contains(type);
    }

    /** فقط توسط TileVariantResolver صدا زده می‌شود */
    public void setTileVariant(int variant) { this.tileVariant = variant; }

    @Override
    public String toString() {
        return "GridCell[" + row + "," + col + "](" + soilType + ")";
    }

    // ─── Builder ───────────────────────────────────────────

    public static class Builder {
        private int row, col;
        private Rectangle worldBounds = new Rectangle();
        private SoilType soilType     = SoilType.NORMAL;
        private boolean isPowerTile   = false;
        private boolean isBlocked     = false;
        private Set<PlantType> allowedPlants = null;

        public Builder position(int row, int col) { this.row = row; this.col = col; return this; }
        public Builder bounds(float x, float y, float w, float h) {
            this.worldBounds = new Rectangle(x, y, w, h); return this;
        }
        public Builder soilType(SoilType t)           { this.soilType = t;      return this; }
        public Builder soilType(String s)             {
            try { this.soilType = SoilType.valueOf(s.toUpperCase()); }
            catch (Exception e) { this.soilType = SoilType.NORMAL; }
            return this;
        }
        public Builder powerTile(boolean p)           { this.isPowerTile = p;   return this; }
        public Builder blocked(boolean b)             { this.isBlocked = b;     return this; }
        public Builder allowedPlants(Set<PlantType> s){ this.allowedPlants = s; return this; }
        public GridCell build()                       { return new GridCell(this); }
    }
}
