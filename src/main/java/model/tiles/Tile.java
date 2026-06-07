package model.tiles;

import model.enums.TileType;
import model.plants.Plant;
import model.zombies.Zombie;

import java.util.List;

/**
 * یک خانه از نقشه بازی.
 * می‌تواند گیاه، زامبی و نوع زمین داشته باشد.
 */
public class Tile {

    /** موقعیت ستون (1 تا 9) */
    private int x;

    /** موقعیت ردیف (1 تا 5) */
    private int y;

    /** نوع این خانه */
    private TileType type;

    /** گیاه کاشته‌شده روی این خانه (null اگر خالی) */
    private Plant plant;

    /**
     * گیاه لایه دوم (مثلاً pumpkin روی گیاه دیگر).
     * null اگر وجود نداشته باشد.
     */
    private Plant secondLayerPlant;

    /** زامبی‌های موجود روی این خانه */
    private List<Zombie> zombiesOnTile;

    /** سلامتی خانه (برای tombstone و icy ground) */
    private int tileHealth;

    /** آیا ماشین چمن‌زنی این ردیف هنوز فعال نشده */
    private boolean lawnMowerActive;

    /** خورشیدی که روی این خانه (از گیاه) منتظر برداشت است */
    private boolean hasSunPending;

    public Tile(int x, int y, TileType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.lawnMowerActive = true;
    }

    /** بررسی می‌کند آیا می‌توان گیاه کاشت */
    public boolean isPlantable() { return false; }

    /** بررسی می‌کند آیا خانه آب است */
    public boolean isWater() { return type == TileType.WATER; }

    /** بررسی می‌کند آیا خانه قبر است */
    public boolean isTombstone() {
        return type == TileType.TOMBSTONE || type == TileType.DARK_TOMBSTONE;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public TileType getType() { return type; }
    public void setType(TileType type) { this.type = type; }
    public Plant getPlant() { return plant; }
    public void setPlant(Plant plant) { this.plant = plant; }
    public Plant getSecondLayerPlant() { return secondLayerPlant; }
    public void setSecondLayerPlant(Plant plant) { this.secondLayerPlant = plant; }
    public List<Zombie> getZombiesOnTile() { return zombiesOnTile; }
    public int getTileHealth() { return tileHealth; }
    public void setTileHealth(int tileHealth) { this.tileHealth = tileHealth; }
    public boolean isLawnMowerActive() { return lawnMowerActive; }
    public void setLawnMowerActive(boolean active) { this.lawnMowerActive = active; }
    public boolean isHasSunPending() { return hasSunPending; }
    public void setHasSunPending(boolean hasSunPending) { this.hasSunPending = hasSunPending; }
}
