package com.pvz2.map;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.pvz2.map.zones.*;
import com.pvz2.model.enums.ChapterType;

import java.util.Collections;
import java.util.List;

/**
 * کانتینر اصلی نقشه بازی — Game Map.
 *
 * بعد از بارگذاری توسط MapLoader، این شیء تمام اطلاعات
 * ایستای نقشه را نگه می‌دارد. Immutable بعد از ساخت.
 *
 * دسترسی از GameScreen:
 *   GameMap map = MapLoader.load("maps/world.tmx", config);
 *   GridZone grid = map.getGridZone();
 *   ZombieSpawnZone spawns = map.getZombieSpawnZone();
 *   CameraRegionManager camera = map.getCameraManager();
 */
public final class GameMap {

    // ─── نقشه خام libGDX ───────────────────────────────────
    private final TiledMap tiledMap;

    // ─── config فعال‌شده ───────────────────────────────────
    private final LevelMapConfig activeConfig;
    private final ChapterType chapter;

    // ─── ابعاد کلی ─────────────────────────────────────────
    /** ابعاد نقشه به پیکسل در world space */
    private final float mapWorldWidth;
    private final float mapWorldHeight;

    // ─── ناحیه‌های اصلی ────────────────────────────────────

    /** گرید ۵×۹ کاشی قابل کاشت */
    private final GridZone gridZone;

    /** ناحیه ورود زامبی از سمت راست */
    private final ZombieSpawnZone zombieSpawnZone;

    /** خانه — مرز game over */
    private final HouseZone houseZone;

    /** ریل / نوار نقاله — null اگر مرحله ویژه نباشد */
    private final RailZone railZone;

    // ─── دوربین ────────────────────────────────────────────
    private final CameraRegionManager cameraManager;

    // ─── اشیاء ویژه پارس‌شده ───────────────────────────────

    /** تمام SPECIAL_ELEMENT object ها */
    private final List<PvzMapObject> specialElements;

    /** تمام TOMBSTONE_SPAWN object ها */
    private final List<PvzMapObject> tombstoneSpawns;

    /** تمام ICE_BLOCK_SPAWN object ها */
    private final List<PvzMapObject> iceBlockSpawns;

    /** تمام WATER_LINE_MARKER object ها */
    private final List<PvzMapObject> waterLineMarkers;

    /** تمام SUN_DROPPER object ها */
    private final List<PvzMapObject> sunDroppers;

    private GameMap(Builder b) {
        this.tiledMap         = b.tiledMap;
        this.activeConfig     = b.activeConfig;
        this.chapter          = b.chapter;
        this.mapWorldWidth    = b.mapWorldWidth;
        this.mapWorldHeight   = b.mapWorldHeight;
        this.gridZone         = b.gridZone;
        this.zombieSpawnZone  = b.zombieSpawnZone;
        this.houseZone        = b.houseZone;
        this.railZone         = b.railZone;
        this.cameraManager    = b.cameraManager;
        this.specialElements  = Collections.unmodifiableList(b.specialElements);
        this.tombstoneSpawns  = Collections.unmodifiableList(b.tombstoneSpawns);
        this.iceBlockSpawns   = Collections.unmodifiableList(b.iceBlockSpawns);
        this.waterLineMarkers = Collections.unmodifiableList(b.waterLineMarkers);
        this.sunDroppers      = Collections.unmodifiableList(b.sunDroppers);
    }

    // ─── Getters ───────────────────────────────────────────

    public TiledMap            getTiledMap()         { return tiledMap; }
    public LevelMapConfig      getActiveConfig()     { return activeConfig; }
    public ChapterType         getChapter()          { return chapter; }
    public float               getMapWorldWidth()    { return mapWorldWidth; }
    public float               getMapWorldHeight()   { return mapWorldHeight; }

    public GridZone            getGridZone()         { return gridZone; }
    public ZombieSpawnZone     getZombieSpawnZone()  { return zombieSpawnZone; }
    public HouseZone           getHouseZone()        { return houseZone; }
    public RailZone            getRailZone()         { return railZone; }
    public CameraRegionManager getCameraManager()    { return cameraManager; }

    public List<PvzMapObject>  getSpecialElements()  { return specialElements; }
    public List<PvzMapObject>  getTombstoneSpawns()  { return tombstoneSpawns; }
    public List<PvzMapObject>  getIceBlockSpawns()   { return iceBlockSpawns; }
    public List<PvzMapObject>  getWaterLineMarkers() { return waterLineMarkers; }
    public List<PvzMapObject>  getSunDroppers()      { return sunDroppers; }

    /** آیا ریل فعال است؟ */
    public boolean hasRail() { return railZone != null; }

    /** آیا آب در این مرحله وجود دارد؟ (ساحل) */
    public boolean hasWater() {
        return chapter == ChapterType.BIG_WAVE_BEACH || !waterLineMarkers.isEmpty();
    }

    /**
     * آزادسازی منابع libGDX.
     * باید از GameScreen.dispose() صدا زده شود.
     */
    public void dispose() {
        if (tiledMap != null) tiledMap.dispose();
    }

    // ─── Builder ───────────────────────────────────────────

    public static final class Builder {
        TiledMap tiledMap;
        LevelMapConfig activeConfig;
        ChapterType chapter;
        float mapWorldWidth, mapWorldHeight;
        GridZone gridZone;
        ZombieSpawnZone zombieSpawnZone;
        HouseZone houseZone;
        RailZone railZone;
        CameraRegionManager cameraManager;
        List<PvzMapObject> specialElements  = new java.util.ArrayList<>();
        List<PvzMapObject> tombstoneSpawns  = new java.util.ArrayList<>();
        List<PvzMapObject> iceBlockSpawns   = new java.util.ArrayList<>();
        List<PvzMapObject> waterLineMarkers = new java.util.ArrayList<>();
        List<PvzMapObject> sunDroppers      = new java.util.ArrayList<>();

        public Builder tiledMap(TiledMap m)             { this.tiledMap = m;         return this; }
        public Builder config(LevelMapConfig c)         { this.activeConfig = c;     return this; }
        public Builder chapter(ChapterType ch)          { this.chapter = ch;         return this; }
        public Builder worldSize(float w, float h)      { mapWorldWidth=w; mapWorldHeight=h; return this; }
        public Builder gridZone(GridZone g)             { this.gridZone = g;         return this; }
        public Builder zombieSpawnZone(ZombieSpawnZone z){ this.zombieSpawnZone = z;  return this; }
        public Builder houseZone(HouseZone h)           { this.houseZone = h;        return this; }
        public Builder railZone(RailZone r)             { this.railZone = r;         return this; }
        public Builder cameraManager(CameraRegionManager c){ this.cameraManager = c; return this; }
        public Builder specialElements(List<PvzMapObject> l){ this.specialElements = l; return this; }
        public Builder tombstoneSpawns(List<PvzMapObject> l){ this.tombstoneSpawns = l; return this; }
        public Builder iceBlockSpawns(List<PvzMapObject> l) { this.iceBlockSpawns = l; return this; }
        public Builder waterLineMarkers(List<PvzMapObject> l){ this.waterLineMarkers = l; return this; }
        public Builder sunDroppers(List<PvzMapObject> l)    { this.sunDroppers = l;   return this; }
        public GameMap build()                          { return new GameMap(this); }
    }
}
