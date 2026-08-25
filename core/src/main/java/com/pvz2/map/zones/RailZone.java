package com.pvz2.map.zones;

import com.badlogic.gdx.math.Rectangle;
import com.pvz2.map.PvzMapObject;
import com.pvz2.model.enums.PlantType;

import java.util.*;

/**
 * ناحیه ریل — Rail Zone (اختیاری).
 *
 * در مراحل ویژه با نوار نقاله (Conveyor Belt) یا ریل دودو فعال می‌شود.
 * وقتی فعال است، دوربین به حالت EXTENDED می‌رود.
 *
 * دو نقش:
 *   1. Rail حرکت: دودو / زامبی ریلی روی آن حرکت می‌کند
 *   2. Conveyor: گیاهان از راست به چپ می‌آیند (مرحله ویژه انتخاب گیاه)
 */
public final class RailZone {

    /** جهت حرکت روی ریل */
    public enum Direction { LEFT_RIGHT, RIGHT_LEFT, BIDIRECTIONAL }

    /** یک slot روی نوار نقاله */
    public static final class ConveyorSlot {
        public final int           sequenceIndex;
        public final float         worldX;
        public final float         worldY;
        public final Set<PlantType> plantPool;   // گیاهان ممکن در این slot
        public final PvzMapObject  sourceObj;

        public ConveyorSlot(int idx, float x, float y,
                            Set<PlantType> pool, PvzMapObject obj) {
            this.sequenceIndex = idx;
            this.worldX        = x;
            this.worldY        = y;
            this.plantPool     = Collections.unmodifiableSet(pool);
            this.sourceObj     = obj;
        }
    }

    private final String    railId;
    private final Direction direction;
    private final float     speedPxPerSec;   // سرعت حرکت روی ریل
    private final Rectangle bounds;          // ناحیه کل ریل در world space

    /** Conveyor slot ها مرتب بر اساس sequenceIndex */
    private final List<ConveyorSlot> conveyorSlots;

    /** اشیاء خام RAIL_SEGMENT از Tiled */
    private final List<PvzMapObject> railSegmentObjects;

    public RailZone(String railId, Direction direction,
                    float speedPxPerSec, Rectangle bounds) {
        this.railId            = railId;
        this.direction         = direction;
        this.speedPxPerSec     = speedPxPerSec;
        this.bounds            = bounds;
        this.conveyorSlots     = new ArrayList<>();
        this.railSegmentObjects = new ArrayList<>();
    }

    // ─── مدیریت slot ها ────────────────────────────────────

    public void addConveyorSlot(ConveyorSlot slot) {
        conveyorSlots.add(slot);
        conveyorSlots.sort(Comparator.comparingInt(s -> s.sequenceIndex));
    }

    public void addRailSegment(PvzMapObject obj) { railSegmentObjects.add(obj); }

    // ─── Getters ────────────────────────────────────────────

    public String            getRailId()            { return railId; }
    public Direction         getDirection()         { return direction; }
    public float             getSpeedPxPerSec()     { return speedPxPerSec; }
    public Rectangle         getBounds()            { return bounds; }
    public List<ConveyorSlot> getConveyorSlots()   { return Collections.unmodifiableList(conveyorSlots); }
    public List<PvzMapObject> getRailSegments()    { return Collections.unmodifiableList(railSegmentObjects); }

    /** آیا این ریل به عنوان Conveyor Belt استفاده می‌شود؟ */
    public boolean isConveyor() { return !conveyorSlots.isEmpty(); }
}
