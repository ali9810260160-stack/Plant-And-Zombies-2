package com.pvz2.map.zones;

import com.badlogic.gdx.math.Rectangle;

/**
 * ناحیه خانه — House Zone.
 *
 * اگر زامبی به خط مرزی هر لاین برسد:
 *   - ابتدا لانمووِر آن لاین فعال می‌شود
 *   - اگر لانمووِر فعال نباشد → game over
 *
 * مختصات `boundaryX` برای هر لاین: X ای که زامبی نباید از آن عبور کند.
 */
public final class HouseZone {

    private final int laneCount;

    /** X مرزی هر لاین — زامبی نباید از این X عبور کند */
    private final float[] boundaryXPerLane;

    /** ناحیه کامل house در world space */
    private final Rectangle totalBounds;

    /** آیا لانمووِر لاین فعال است؟ (runtime — اینجا فقط وضعیت اولیه) */
    private final boolean[] lawnmowerActive;

    /** موقعیت پارک لانمووِر هر لاین */
    private final float[] lawnmowerX;

    public HouseZone(int laneCount, Rectangle totalBounds) {
        this.laneCount        = laneCount;
        this.totalBounds      = totalBounds;
        this.boundaryXPerLane = new float[laneCount];
        this.lawnmowerActive  = new boolean[laneCount];
        this.lawnmowerX       = new float[laneCount];

        // پیش‌فرض: همه لانمووِرها فعال
        for (int i = 0; i < laneCount; i++) {
            boundaryXPerLane[i] = totalBounds.x + totalBounds.width;
            lawnmowerActive[i]  = true;
            lawnmowerX[i]       = totalBounds.x + totalBounds.width - 20f;
        }
    }

    // ─── تنظیم مرز لاین ────────────────────────────────────

    public void setBoundaryX(int lane, float x) {
        if (isValidLane(lane)) boundaryXPerLane[lane] = x;
    }

    /** تنظیم یک مرز برای همه لاین‌ها */
    public void setBoundaryXAll(float x) {
        for (int i = 0; i < laneCount; i++) boundaryXPerLane[i] = x;
    }

    public void setLawnmowerX(int lane, float x) {
        if (isValidLane(lane)) lawnmowerX[lane] = x;
    }

    // ─── Getters ────────────────────────────────────────────

    public float getBoundaryX(int lane) {
        return isValidLane(lane) ? boundaryXPerLane[lane] : totalBounds.x;
    }

    public float getLawnmowerX(int lane) {
        return isValidLane(lane) ? lawnmowerX[lane] : totalBounds.x;
    }

    public boolean isLawnmowerActive(int lane) {
        return isValidLane(lane) && lawnmowerActive[lane];
    }

    public void setLawnmowerActive(int lane, boolean active) {
        if (isValidLane(lane)) lawnmowerActive[lane] = active;
    }

    public int       getLaneCount()   { return laneCount; }
    public Rectangle getTotalBounds() { return totalBounds; }

    private boolean isValidLane(int l) { return l >= 0 && l < laneCount; }
}
