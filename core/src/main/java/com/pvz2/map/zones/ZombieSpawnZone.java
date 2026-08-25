package com.pvz2.map.zones;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.pvz2.map.PvzMapObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ناحیه ورود زامبی — Zombie Spawn Zone.
 *
 * هر لاین یک یا چند SpawnPoint دارد که نقطه spawn زامبی‌ها هستند.
 * اشیاء واقعی زامبی توسط WaveManager در این نقاط spawn می‌شوند.
 */
public final class ZombieSpawnZone {

    /** انواع spawn */
    public enum SpawnCategory {
        LAND,         // زامبی پیاده از سمت راست
        WATER,        // زامبی از آب می‌آید
        AIR,          // زامبی از هوا (بالت)
        UNDERGROUND,  // زامبی از زیر زمین
        RAIL          // زامبی روی ریل
    }

    /** یک نقطه spawn در یک لاین */
    public static final class SpawnPoint {
        public final int           lane;
        public final SpawnCategory category;
        public final Vector2       worldPos;   // نقطه spawn در world space
        public final PvzMapObject  sourceObj;  // شیء اصلی از Tiled

        public SpawnPoint(int lane, SpawnCategory cat, Vector2 pos, PvzMapObject obj) {
            this.lane      = lane;
            this.category  = cat;
            this.worldPos  = pos;
            this.sourceObj = obj;
        }
    }

    private final int laneCount;
    private final Rectangle bounds;

    /** [lane] → لیست SpawnPoint های آن لاین */
    @SuppressWarnings("unchecked")
    private final List<SpawnPoint>[] spawnsByLane;

    public ZombieSpawnZone(int laneCount, Rectangle bounds) {
        this.laneCount    = laneCount;
        this.bounds       = bounds;
        this.spawnsByLane = new List[laneCount];
        for (int i = 0; i < laneCount; i++) spawnsByLane[i] = new ArrayList<>();
    }

    public void addSpawnPoint(SpawnPoint sp) {
        if (sp.lane >= 0 && sp.lane < laneCount)
            spawnsByLane[sp.lane].add(sp);
    }

    /** تمام spawn point های یک لاین */
    public List<SpawnPoint> getSpawnPoints(int lane) {
        if (lane < 0 || lane >= laneCount) return Collections.emptyList();
        return Collections.unmodifiableList(spawnsByLane[lane]);
    }

    /** نقطه spawn پیش‌فرض یک لاین (اولین LAND spawn) */
    public Vector2 getDefaultSpawnPos(int lane) {
        for (SpawnPoint sp : getSpawnPoints(lane))
            if (sp.category == SpawnCategory.LAND) return sp.worldPos;
        // fallback: لبه راست ناحیه
        float y = bounds.y + (lane + 0.5f) * (bounds.height / laneCount);
        return new Vector2(bounds.x + bounds.width, y);
    }

    public int       getLaneCount() { return laneCount; }
    public Rectangle getBounds()    { return bounds; }
}
