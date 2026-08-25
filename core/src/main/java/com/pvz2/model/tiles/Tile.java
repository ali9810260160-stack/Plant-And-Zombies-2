package com.pvz2.model.tiles;

import com.pvz2.model.enums.TileType;
import com.pvz2.model.plants.Plant;
import com.pvz2.model.zombies.Zombie;

import java.util.ArrayList;
import java.util.List;

/**
 * یک خانه از نقشه بازی.
 */
public class Tile {

    private int x;
    private int y;
    private TileType type;
    private Plant plant;
    private Plant secondLayerPlant;
    private List<Zombie> zombiesOnTile;
    private int tileHealth;
    private boolean hasSunPending;
    private int freezeLevel;
    /** حفره (Beghouled): وقتی زامبی گیاهی را می‌خورد، آن خانه crater می‌شود و دیگر گیاه نمی‌گیرد. */
    private boolean crater;

    /** جایزه‌ی نهفته در سنگ‌قبر — با اتمامِ جانِ سنگ‌قبر آزاد می‌شود. */
    public enum Reward { NONE, SUN, PLANT_FOOD }
    private Reward reward = Reward.NONE;

    public Tile(int x, int y, TileType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.zombiesOnTile = new ArrayList<>();
        initTileHealth();
    }

    private void initTileHealth() {
        if (type == TileType.TOMBSTONE || type == TileType.DARK_TOMBSTONE) {
            this.tileHealth = 700;
        } else if (type == TileType.ICY_GROUND) {
            this.tileHealth = 600;
        } else {
            this.tileHealth = 0;
        }
    }

    public boolean isPlantable() {
        if (crater) {
            return false;
        }
        if (type == TileType.TOMBSTONE || type == TileType.DARK_TOMBSTONE) {
            return false;
        }
        if (type == TileType.ICY_GROUND) {
            return false;
        }
        if (type == TileType.SLIPPERY_UP || type == TileType.SLIPPERY_DOWN) {
            return false;
        }
        if (type == TileType.WATER) {
            return false;
        }
        if (type == TileType.NECROMANCY && plant == null) {
            return true;
        }
        return plant == null;
    }

    public boolean isWater() {
        return type == TileType.WATER;
    }

    public boolean isTombstone() {
        return type == TileType.TOMBSTONE || type == TileType.DARK_TOMBSTONE;
    }

    public boolean isSlippery() {
        return type == TileType.SLIPPERY_UP || type == TileType.SLIPPERY_DOWN;
    }

    public boolean isSlipperyUp() {
        return type == TileType.SLIPPERY_UP;
    }

    public void addZombie(Zombie zombie) {
        zombiesOnTile.add(zombie);
    }

    public void removeZombie(Zombie zombie) {
        zombiesOnTile.remove(zombie);
    }

    public void takeDamage(int damage) {
        if (tileHealth > 0) {
            tileHealth = Math.max(0, tileHealth - damage);
            if (tileHealth == 0) {
                if (type == TileType.TOMBSTONE || type == TileType.DARK_TOMBSTONE) {
                    type = getParentChapterNormalType();
                } else if (type == TileType.ICY_GROUND) {
                    type = TileType.CAVE_NORMAL;
                }
            }
        }
    }

    private TileType getParentChapterNormalType() {
        if (type == TileType.DARK_TOMBSTONE) {
            return TileType.DARK_NORMAL;
        }
        return TileType.EGYPT_NORMAL;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public TileType getType() { return type; }
    public void setType(TileType type) {
        this.type = type;
        initTileHealth();
    }
    public Plant getPlant() { return plant; }
    public void setPlant(Plant plant) { this.plant = plant; }
    public Plant getSecondLayerPlant() { return secondLayerPlant; }
    public void setSecondLayerPlant(Plant plant) { this.secondLayerPlant = plant; }
    public List<Zombie> getZombiesOnTile() { return zombiesOnTile; }
    public int getTileHealth() { return tileHealth; }
    public void setTileHealth(int tileHealth) { this.tileHealth = tileHealth; }
    public boolean isHasSunPending() { return hasSunPending; }
    public void setHasSunPending(boolean hasSunPending) { this.hasSunPending = hasSunPending; }
    public int getFreezeLevel() { return freezeLevel; }
    public void setFreezeLevel(int freezeLevel) { this.freezeLevel = freezeLevel; }
    public void incrementFreezeLevel() { this.freezeLevel = Math.min(3, this.freezeLevel + 1); }
    public boolean isCrater() { return crater; }
    public void setCrater(boolean crater) { this.crater = crater; }
    public Reward getReward() { return reward; }
    public void setReward(Reward reward) { this.reward = reward; }
}
