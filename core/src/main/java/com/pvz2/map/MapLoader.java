package com.pvz2.map;

import com.badlogic.gdx.maps.*;
import com.badlogic.gdx.maps.objects.*;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.pvz2.map.zones.*;
import com.pvz2.map.MapLoader;
import com.pvz2.model.enums.PlantType;

import java.util.*;
import java.util.Map;

/**
 * بارگذار و پارسر نقشه Tiled — Map Loader.
 *
 * تنها نقطه ورود برای ایجاد GameMap:
 *   GameMap map = MapLoader.load("maps/world.tmx", levelConfig);
 *
 * مراحل پردازش (طبق TILED_MAP_GUIDE.md بخش ۱۱):
 *   1. بارگذاری TMX با TmxMapLoader
 *   2. فعال‌سازی لایه‌های مناسب با MapLayerManager
 *   3. جایگزینی variant تصادفی با TileVariantResolver
 *   4. پارس ابعاد و مرزها
 *   5. پارس اشیاء shared (خانه، لانمووِر)
 *   6. پارس اشیاء فصل (plant slot، zombie spawn، ...)
 *   7. پارس اشیاء special (ریل، ...)
 *   8. پارس ناحیه‌های دوربین
 *   9. ساخت و بازگرداندن GameMap
 *
 * ─── مختصات ──────────────────────────────────────────────
 * Tiled:  Y-down، ردیف ۰ بالا
 * libGDX: Y-up،  ردیف ۰ پایین
 *
 * تبدیل Y:
 *   worldY = mapHeightPx - tiledY - objectHeight
 *
 * تبدیل ردیف گرید:
 *   libGDX_row = (GRID_ROWS - 1) - tiled_row
 */
public final class MapLoader {

    // ─── ثابت‌های طرح‌بندی (از TILED_MAP_GUIDE.md بخش ۲) ──

    public static final int   GRID_ROWS       = 5;
    public static final int   GRID_COLS       = 9;
    public static final float CELL_W          = 100f;
    public static final float CELL_H          = 100f;

    /** X شروع گرید در world space */
    public static final float GRID_ORIGIN_X   = 160f;
    /** Y شروع گرید (پایین گرید) در world space */
    public static final float GRID_ORIGIN_Y   = 120f;   // 720 - (120 + 500) = 100px margin top

    public static final float HOUSE_ZONE_W    = 160f;
    public static final float ZOMBIE_ZONE_X   = GRID_ORIGIN_X + GRID_COLS * CELL_W;  // 1060
    public static final float ZOMBIE_ZONE_W   = 180f;

    public static final float VIRTUAL_W       = 1280f;
    public static final float VIRTUAL_H       = 720f;

    // ─── نام property های Tiled ────────────────────────────
    private static final String PROP_PVZ_TYPE      = "pvz.type";
    private static final String PROP_ROW           = "pvz.row";
    private static final String PROP_COL           = "pvz.col";
    private static final String PROP_LANE          = "pvz.lane";
    private static final String PROP_SOIL_TYPE     = "pvz.soil_type";
    private static final String PROP_IS_WATER      = "pvz.is_water";
    private static final String PROP_POWER_TILE    = "pvz.power_tile";
    private static final String PROP_BLOCKED       = "pvz.blocked";
    private static final String PROP_ALLOWED_PLANTS= "pvz.allowed_plants";
    private static final String PROP_SPAWN_CAT     = "pvz.spawn_category";
    private static final String PROP_CAMERA_TYPE   = "pvz.camera_type";
    private static final String PROP_RAIL_ID       = "pvz.rail_id";
    private static final String PROP_DIRECTION     = "pvz.direction";
    private static final String PROP_SPEED         = "pvz.speed_px_sec";
    private static final String PROP_SEQ_INDEX     = "pvz.seq_index";
    private static final String PROP_PLANT_POOL    = "pvz.plant_pool";
    private static final String PROP_WAVE_TRIGGER  = "pvz.wave_trigger";
    private static final String PROP_ZOMBIE_POOL   = "pvz.zombie_pool";
    private static final String PROP_ELEMENT_TYPE  = "pvz.element_type";
    private static final String PROP_INTENSITY     = "pvz.intensity";
    private static final String PROP_TRIGGER       = "pvz.trigger";
    private static final String PROP_DEPTH_FACTOR  = "pvz.depth_factor";
    private static final String PROP_WAVE_AMP      = "pvz.wave_amplitude";
    private static final String PROP_WAVE_SPEED    = "pvz.wave_speed";
    private static final String PROP_COL_START     = "pvz.col_start";
    private static final String PROP_COL_END       = "pvz.col_end";

    // ─── ورودی اصلی ────────────────────────────────────────

    /**
     * بارگذاری و پارس کامل نقشه.
     *
     * @param tmxPath    مسیر نسبت به assets/ (مثلاً "maps/world.tmx")
     * @param config     تنظیمات مرحله جاری
     * @return           GameMap آماده برای استفاده
     */
    public static GameMap load(String tmxPath, LevelMapConfig config) {

        // ─── ۱. بارگذاری TMX ───────────────────────────────
        TiledMap tiledMap = new TmxMapLoader().load(tmxPath);
        MapProperties mapProps = tiledMap.getProperties();

        int tileWidth  = mapProps.get("tilewidth",  Integer.class);
        int tileHeight = mapProps.get("tileheight", Integer.class);
        int mapTilesW  = mapProps.get("width",      Integer.class);
        int mapTilesH  = mapProps.get("height",     Integer.class);

        float mapWorldW = mapTilesW * tileWidth;
        float mapWorldH = mapTilesH * tileHeight;

        // ─── ۲. visibility لایه‌ها ─────────────────────────
        MapLayerManager layerManager = new MapLayerManager(tiledMap);
        layerManager.applyConfig(config);

        // ─── ۳. variant تصادفی ─────────────────────────────
        TileVariantResolver variantResolver = new TileVariantResolver(config.getVariantSeed());
        variantResolver.resolve(tiledMap);

        // ─── تبدیل نقشه → صفحه ─────────────────────────────
        // کل نقشه (mapWorldW × mapWorldH) به فضای مجازی ۱۲۸۰×۷۲۰ کشیده می‌شود
        // (همان کاری که رندرِ نقشه در GridRenderer با دوربینِ full-map انجام می‌دهد).
        // پس هر آبجکت نقشه با همین ضریب به مختصات صفحه تبدیل می‌شود تا گرید منطقی،
        // موقعیت زامبی‌ها و … دقیقاً روی همان‌جایی بیفتند که نقشه رسم می‌شود.
        float sx = VIRTUAL_W / mapWorldW;
        float sy = VIRTUAL_H / mapWorldH;

        List<MapLayer> objectLayers = layerManager.collectVisibleObjectLayers();

        // ─── هندسه گرید را از خودِ نقشه استخراج کن (منبع حقیقت) ─────────────
        Geometry geo = deriveGeometry(objectLayers, mapWorldH, sx, sy);
        com.pvz2.graphics.GameConstants.applyGridGeometry(
                geo.gx, geo.gy, geo.tw, geo.th, geo.houseW, geo.zombieX, geo.zombieW);

        // ─── ۴. ساخت ناحیه‌ها (با هندسه استخراج‌شده) ───────
        GridZone       gridZone      = buildGridZone(geo);
        ZombieSpawnZone spawnZone    = buildZombieSpawnZone(geo);
        HouseZone      houseZone     = buildHouseZone(geo);

        // ─── ۵. ۶. ۷. پارس اشیاء ──────────────────────────
        List<PvzMapObject> specialElements  = new ArrayList<>();
        List<PvzMapObject> tombstoneSpawns  = new ArrayList<>();
        List<PvzMapObject> iceBlockSpawns   = new ArrayList<>();
        List<PvzMapObject> waterLineMarkers = new ArrayList<>();
        List<PvzMapObject> sunDroppers      = new ArrayList<>();

        RailZone railZone = null;
        Map<String, RailZoneBuilder> railBuilders = new HashMap<>();

        for (MapLayer layer : objectLayers) {
            MapObjects objects = layer.getObjects();
            for (MapObject obj : objects) {
                Rectangle rawRect = toWorldRect(obj, mapWorldH);
                if (rawRect == null) continue;
                // به فضای صفحه ۱۲۸۰×۷۲۰ مقیاس کن
                Rectangle worldRect = new Rectangle(
                        rawRect.x * sx, rawRect.y * sy,
                        rawRect.width * sx, rawRect.height * sy);

                MapProperties p = obj.getProperties();
                String typeStr  = p.get(PROP_PVZ_TYPE, "", String.class);
                MapObjectType type = MapObjectType.fromString(typeStr);

                PvzMapObject.Builder pb = new PvzMapObject.Builder()
                    .type(type)
                    .name(obj.getName() != null ? obj.getName() : "")
                    .bounds(worldRect)
                    .props(propsToMap(p));

                switch (type) {

                    case PLANT_SLOT:
                        parseAndAddPlantSlot(pb, p, gridZone, worldRect);
                        break;

                    case ZOMBIE_SPAWN_POINT:
                        parseAndAddZombieSpawn(pb, p, spawnZone, worldRect);
                        break;

                    case HOUSE_BOUNDARY:
                        parseHouseBoundary(p, houseZone, worldRect);
                        break;

                    case LAWNMOWER_SLOT:
                        parseLawnmowerSlot(p, houseZone, worldRect);
                        break;

                    case RAIL_SEGMENT:
                        parseRailSegment(p, pb, railBuilders, worldRect);
                        break;

                    case CONVEYOR_SLOT:
                        parseConveyorSlot(p, pb, railBuilders, worldRect);
                        break;

                    case CAMERA_REGION:
                        // پارس بعداً در مرحله ۸
                        break;

                    case SPECIAL_ELEMENT:
                        specialElements.add(pb.build());
                        break;

                    case TOMBSTONE_SPAWN:
                        tombstoneSpawns.add(pb.build());
                        break;

                    case ICE_BLOCK_SPAWN:
                        iceBlockSpawns.add(pb.build());
                        break;

                    case WATER_LINE_MARKER:
                        waterLineMarkers.add(pb.build());
                        break;

                    case SUN_DROPPER:
                        sunDroppers.add(pb.build());
                        break;

                    default:
                        break;
                }
            }
        }

        // ─── نهایی‌سازی Rail Zone ─────────────────────────
        if (!railBuilders.isEmpty()) {
            railZone = railBuilders.values().iterator().next().build();
        }

        // ─── ۸. دوربین ────────────────────────────────────
        CameraRegionManager cameraManager =
            parseCameraRegions(objectLayers, mapWorldH, sx, sy, config.isExtendedCamera());

        // ─── ۹. ساخت GameMap ──────────────────────────────
        return new GameMap.Builder()
            .tiledMap(tiledMap)
            .config(config)
            .chapter(config.getChapter())
            .worldSize(mapWorldW, mapWorldH)
            .gridZone(gridZone)
            .zombieSpawnZone(spawnZone)
            .houseZone(houseZone)
            .railZone(railZone)
            .cameraManager(cameraManager)
            .specialElements(specialElements)
            .tombstoneSpawns(tombstoneSpawns)
            .iceBlockSpawns(iceBlockSpawns)
            .waterLineMarkers(waterLineMarkers)
            .sunDroppers(sunDroppers)
            .build();
    }

    // ═══════════════════════════════════════════════════════
    //  استخراج هندسه گرید از خودِ نقشه
    // ═══════════════════════════════════════════════════════

    /** هندسه گرید در فضای صفحه ۱۲۸۰×۷۲۰ — از آبجکت‌های نقشه استخراج می‌شود. */
    static final class Geometry {
        float gx, gy, tw, th;          // origin (چپ-پایین) و اندازه کاشی
        float houseW, zombieX, zombieW;
    }

    /**
     * هندسه گرید را از آبجکت‌های PLANT_SLOT / HOUSE_BOUNDARY نقشه (مقیاس‌شده به
     * صفحه) محاسبه می‌کند. اگر آبجکت PLANT_SLOT ای نبود، به پیش‌فرض راهنما
     * (۱۶۰/۱۲۰/۱۰۰) برمی‌گردد تا نقشه‌های ناقص هم crash نکنند.
     */
    private static Geometry deriveGeometry(List<MapLayer> objectLayers,
                                           float mapWorldH, float sx, float sy) {
        Geometry g = new Geometry();

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        int slotCount = 0;
        float houseRight = -Float.MAX_VALUE;

        for (MapLayer layer : objectLayers) {
            for (MapObject obj : layer.getObjects()) {
                MapProperties p = obj.getProperties();
                MapObjectType t = MapObjectType.fromString(getString(p, PROP_PVZ_TYPE, ""));
                Rectangle raw = toWorldRect(obj, mapWorldH);
                if (raw == null) continue;
                Rectangle s = new Rectangle(raw.x * sx, raw.y * sy,
                                            raw.width * sx, raw.height * sy);
                if (t == MapObjectType.PLANT_SLOT) {
                    slotCount++;
                    minX = Math.min(minX, s.x);
                    minY = Math.min(minY, s.y);
                    maxX = Math.max(maxX, s.x + s.width);
                    maxY = Math.max(maxY, s.y + s.height);
                } else if (t == MapObjectType.HOUSE_BOUNDARY) {
                    houseRight = Math.max(houseRight, s.x + s.width);
                }
            }
        }

        if (slotCount > 0 && maxX > minX && maxY > minY) {
            g.gx = minX;
            g.gy = minY;
            g.tw = (maxX - minX) / GRID_COLS;   // گام کامل ستون (نه صرفاً عرض یک کاشی)
            g.th = (maxY - minY) / GRID_ROWS;
        } else {
            g.gx = GRID_ORIGIN_X; g.gy = GRID_ORIGIN_Y; g.tw = CELL_W; g.th = CELL_H;
        }

        g.houseW  = houseRight > 0 ? houseRight : g.gx;
        g.zombieX = g.gx + GRID_COLS * g.tw;                       // لبه راست گرید
        g.zombieW = Math.max(20f, VIRTUAL_W - g.zombieX);
        return g;
    }

    // ═══════════════════════════════════════════════════════
    //  ساخت ناحیه‌ها (از هندسه استخراج‌شده)
    // ═══════════════════════════════════════════════════════

    private static GridZone buildGridZone(Geometry g) {
        GridZone zone = new GridZone(
            GRID_ROWS, GRID_COLS,
            g.gx, g.gy, g.tw, g.th
        );
        // پیش‌پر کردن با خانه‌های پیش‌فرض؛ پارس واقعی از PLANT_SLOT ها cell را
        // با soil/blocked/allowed درست جایگزین می‌کند (اما موقعیت یکسان است).
        for (int r = 0; r < GRID_ROWS; r++) {
            for (int c = 0; c < GRID_COLS; c++) {
                float x = g.gx + c * g.tw;
                float y = g.gy + r * g.th;
                GridCell cell = new GridCell.Builder()
                    .position(r, c)
                    .bounds(x, y, g.tw, g.th)
                    .soilType(GridCell.SoilType.NORMAL)
                    .build();
                zone.setCell(r, c, cell);
            }
        }
        return zone;
    }

    private static ZombieSpawnZone buildZombieSpawnZone(Geometry g) {
        Rectangle bounds = new Rectangle(
            g.zombieX, g.gy, g.zombieW, GRID_ROWS * g.th
        );
        ZombieSpawnZone zone = new ZombieSpawnZone(GRID_ROWS, bounds);
        for (int lane = 0; lane < GRID_ROWS; lane++) {
            float worldY = g.gy + lane * g.th + g.th / 2f;
            ZombieSpawnZone.SpawnPoint sp = new ZombieSpawnZone.SpawnPoint(
                lane,
                ZombieSpawnZone.SpawnCategory.LAND,
                new Vector2(g.zombieX + g.zombieW - 10f, worldY),
                null
            );
            zone.addSpawnPoint(sp);
        }
        return zone;
    }

    private static HouseZone buildHouseZone(Geometry g) {
        Rectangle bounds = new Rectangle(0, g.gy, g.houseW, GRID_ROWS * g.th);
        HouseZone zone = new HouseZone(GRID_ROWS, bounds);
        zone.setBoundaryXAll(g.houseW);
        for (int lane = 0; lane < GRID_ROWS; lane++) {
            zone.setLawnmowerX(lane, g.houseW - 20f);
        }
        return zone;
    }

    // ═══════════════════════════════════════════════════════
    //  پارسر هر نوع Object
    // ═══════════════════════════════════════════════════════

    private static void parseAndAddPlantSlot(PvzMapObject.Builder pb,
                                              MapProperties p,
                                              GridZone zone,
                                              Rectangle screenRect) {
        int tiledRow = getInt(p, PROP_ROW, -1);
        int col      = getInt(p, PROP_COL, -1);

        if (tiledRow < 0 || col < 0) return;

        // تبدیل ردیف Tiled (Y-down) به libGDX (Y-up)
        int libRow = (GRID_ROWS - 1) - tiledRow;
        if (libRow < 0 || libRow >= GRID_ROWS || col >= GRID_COLS) return;

        String soilStr   = getString(p, PROP_SOIL_TYPE, "NORMAL");
        boolean isWater  = getBool(p, PROP_IS_WATER, false);
        boolean isPower  = getBool(p, PROP_POWER_TILE, false);
        boolean blocked  = getBool(p, PROP_BLOCKED, false);
        String allowedStr= getString(p, PROP_ALLOWED_PLANTS, "");

        GridCell.SoilType soil = isWater ? GridCell.SoilType.WATER :
                                 isPower ? GridCell.SoilType.POWER :
                                 GridCell.SoilType.NORMAL;
        try { soil = GridCell.SoilType.valueOf(soilStr.toUpperCase()); }
        catch (Exception ignored) { }

        Set<PlantType> allowed = parseAllowedPlants(allowedStr);

        // موقعیت خانه را از خانه‌بندی یکنواخت گرید بگیر (نه مستطیل خام آبجکت که
        // ممکن است کمی کوچک‌تر/جابه‌جا باشد) تا گرید کاملاً منظم باشد و با خطوط
        // راهنما و overlay ها هم‌تراز شود. اندازه از GameConstants استخراج‌شده می‌آید.
        float tw = com.pvz2.graphics.GameConstants.TW;
        float th = com.pvz2.graphics.GameConstants.TH;
        float x  = com.pvz2.graphics.GameConstants.G_X + col    * tw;
        float y  = com.pvz2.graphics.GameConstants.G_Y + libRow * th;

        GridCell cell = new GridCell.Builder()
            .position(libRow, col)
            .bounds(x, y, tw, th)
            .soilType(soil)
            .powerTile(isPower)
            .blocked(blocked)
            .allowedPlants(allowed.isEmpty() ? null : allowed)
            .build();

        zone.setCell(libRow, col, cell);
        zone.addPlantSlotObject(pb.lane(libRow).col(col).build());
    }

    private static void parseAndAddZombieSpawn(PvzMapObject.Builder pb,
                                                MapProperties p,
                                                ZombieSpawnZone zone,
                                                Rectangle worldRect) {
        int tiledLane = getInt(p, PROP_LANE, 0);
        int libLane   = (GRID_ROWS - 1) - tiledLane;

        String catStr = getString(p, PROP_SPAWN_CAT, "LAND");
        ZombieSpawnZone.SpawnCategory cat;
        try { cat = ZombieSpawnZone.SpawnCategory.valueOf(catStr.toUpperCase()); }
        catch (Exception e) { cat = ZombieSpawnZone.SpawnCategory.LAND; }

        PvzMapObject obj = pb.lane(libLane).build();
        ZombieSpawnZone.SpawnPoint sp = new ZombieSpawnZone.SpawnPoint(
            libLane, cat,
            new Vector2(worldRect.x + worldRect.width / 2f,
                        worldRect.y + worldRect.height / 2f),
            obj
        );
        zone.addSpawnPoint(sp);
    }

    private static void parseHouseBoundary(MapProperties p,
                                            HouseZone zone,
                                            Rectangle worldRect) {
        int tiledLane = getInt(p, PROP_LANE, -1);
        float boundX  = worldRect.x + worldRect.width;

        if (tiledLane == -1) {
            zone.setBoundaryXAll(boundX);
        } else {
            int libLane = (GRID_ROWS - 1) - tiledLane;
            zone.setBoundaryX(libLane, boundX);
        }
    }

    private static void parseLawnmowerSlot(MapProperties p,
                                            HouseZone zone,
                                            Rectangle worldRect) {
        int tiledLane = getInt(p, PROP_LANE, 0);
        int libLane   = (GRID_ROWS - 1) - tiledLane;
        zone.setLawnmowerX(libLane, worldRect.x + worldRect.width / 2f);
    }

    private static void parseRailSegment(MapProperties p,
                                          PvzMapObject.Builder pb,
                                          Map<String, RailZoneBuilder> builders,
                                          Rectangle worldRect) {
        String railId = getString(p, PROP_RAIL_ID, "main_rail");

        if (!builders.containsKey(railId)) {
            String dirStr  = getString(p, PROP_DIRECTION, "LEFT_RIGHT");
            float speed    = getFloat(p, PROP_SPEED, 80f);

            RailZone.Direction dir;
            try { dir = RailZone.Direction.valueOf(dirStr.toUpperCase()); }
            catch (Exception e) { dir = RailZone.Direction.LEFT_RIGHT; }

            RailZoneBuilder rb = new RailZoneBuilder(railId, dir, speed, worldRect);
            builders.put(railId, rb);
        }
        builders.get(railId).addRailSegment(pb.build());
    }

    private static void parseConveyorSlot(MapProperties p,
                                           PvzMapObject.Builder pb,
                                           Map<String, RailZoneBuilder> builders,
                                           Rectangle worldRect) {
        String railId   = getString(p, PROP_RAIL_ID, "main_rail");
        int seqIndex    = getInt(p, PROP_SEQ_INDEX, 0);
        String poolStr  = getString(p, PROP_PLANT_POOL, "");
        Set<PlantType> pool = parseAllowedPlants(poolStr);

        RailZoneBuilder rb = builders.computeIfAbsent(railId,
            k -> new RailZoneBuilder(k, RailZone.Direction.LEFT_RIGHT, 80f, worldRect));

        RailZone.ConveyorSlot slot = new RailZone.ConveyorSlot(
            seqIndex,
            worldRect.x + worldRect.width  / 2f,
            worldRect.y + worldRect.height / 2f,
            pool,
            pb.build()
        );
        rb.addConveyorSlot(slot);
    }

    // ─── دوربین ────────────────────────────────────────────

    private static CameraRegionManager parseCameraRegions(List<MapLayer> objectLayers,
                                                           float mapWorldH,
                                                           float sx, float sy,
                                                           boolean extendedActive) {
        Rectangle fixed    = new Rectangle(0, 0, VIRTUAL_W, VIRTUAL_H);
        Rectangle extended = new Rectangle(0, 0, VIRTUAL_W, VIRTUAL_H);

        for (MapLayer layer : objectLayers) {
            for (MapObject obj : layer.getObjects()) {
                MapProperties p = obj.getProperties();
                if (!MapObjectType.CAMERA_REGION.name().equalsIgnoreCase(
                        getString(p, PROP_PVZ_TYPE, ""))) continue;

                Rectangle raw = toWorldRect(obj, mapWorldH);
                if (raw == null) continue;
                Rectangle worldRect = new Rectangle(raw.x * sx, raw.y * sy,
                                                    raw.width * sx, raw.height * sy);

                String cType = getString(p, PROP_CAMERA_TYPE, "FIXED");
                if ("EXTENDED".equalsIgnoreCase(cType)) extended = worldRect;
                else                                    fixed    = worldRect;
            }
        }

        CameraRegionManager mgr = new CameraRegionManager(fixed, extended);
        mgr.setExtended(extendedActive);
        return mgr;
    }

    // ═══════════════════════════════════════════════════════
    //  کمکی: تبدیل مختصات Tiled → libGDX
    // ═══════════════════════════════════════════════════════

    /**
     * تبدیل MapObject به Rectangle در مختصات libGDX (Y-up، مبدأ پایین-چپ).
     *
     * <p>⚠️ نکته‌ی مهم (رفعِ باگِ دو-بار-flip): {@code TmxMapLoader} ی libGDX
     * مختصاتِ آبجکت‌ها را <b>از قبل</b> به Y-up تبدیل می‌کند (همان فضایی که لایه‌های
     * کاشی در آن رندر می‌شوند). پس نباید دوباره Y را flip کنیم. flipِ اضافه (نسخه‌ی
     * قبلی: {@code mapWorldH - r.y - r.height}) کلِ گرید را عمودی وارونه/جابه‌جا
     * می‌کرد و بالاتر از پس‌زمینه می‌انداخت (تأیید‌شده با لاگِ pvz_grid_debug:
     * {@code screenMinY=308≈453×sy} = همان tiledY خام). پارامتر {@code mapWorldH}
     * برای سازگاریِ امضا نگه داشته شده اما دیگر لازم نیست.
     */
    private static Rectangle toWorldRect(MapObject obj, float mapWorldH) {
        if (obj instanceof RectangleMapObject) {
            Rectangle r = ((RectangleMapObject) obj).getRectangle();
            return new Rectangle(r.x, r.y, r.width, r.height);
        }
        if (obj instanceof EllipseMapObject) {
            com.badlogic.gdx.math.Ellipse e = ((EllipseMapObject) obj).getEllipse();
            return new Rectangle(e.x, e.y, e.width, e.height);
        }
        if (obj instanceof PolylineMapObject || obj instanceof PolygonMapObject) {
            // bounding box از رئوس — libGDX رئوس را هم در فضای Y-up می‌دهد
            float[] verts = obj instanceof PolylineMapObject
                ? ((PolylineMapObject) obj).getPolyline().getVertices()
                : ((PolygonMapObject)  obj).getPolygon().getVertices();
            float objX = obj.getProperties().get("x", 0f, Float.class);
            float objY = obj.getProperties().get("y", 0f, Float.class);
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (int i = 0; i < verts.length; i += 2) {
                float vx = objX + verts[i];
                float vy = objY + verts[i + 1];
                if (vx < minX) minX = vx; if (vx > maxX) maxX = vx;
                if (vy < minY) minY = vy; if (vy > maxY) maxY = vy;
            }
            return new Rectangle(minX, minY, maxX - minX, maxY - minY);
        }
        // Point
        float x = obj.getProperties().get("x", 0f, Float.class);
        float y = obj.getProperties().get("y", 0f, Float.class);
        return new Rectangle(x, y, 1, 1);
    }

    // ═══════════════════════════════════════════════════════
    //  کمکی: خواندن Properties
    // ═══════════════════════════════════════════════════════

    private static String getString(MapProperties p, String key, String def) {
        Object v = p.get(key);
        if (v == null) return def;
        return v.toString().trim();
    }
    private static int getInt(MapProperties p, String key, int def) {
        try {
            Object v = p.get(key);
            if (v == null) return def;
            if (v instanceof Integer) return (Integer) v;
            return Integer.parseInt(v.toString().trim());
        } catch (Exception e) { return def; }
    }
    private static float getFloat(MapProperties p, String key, float def) {
        try {
            Object v = p.get(key);
            if (v == null) return def;
            if (v instanceof Float) return (Float) v;
            return Float.parseFloat(v.toString().trim());
        } catch (Exception e) { return def; }
    }
    private static boolean getBool(MapProperties p, String key, boolean def) {
        Object v = p.get(key);
        if (v == null) return def;
        if (v instanceof Boolean) return (Boolean) v;
        return "true".equalsIgnoreCase(v.toString().trim());
    }

    private static Set<PlantType> parseAllowedPlants(String csv) {
        Set<PlantType> result = new LinkedHashSet<>();
        if (csv == null || csv.trim().isEmpty()) return result;
        for (String name : csv.split(",")) {
            try { result.add(PlantType.valueOf(name.trim().toUpperCase())); }
            catch (IllegalArgumentException ignored) { }
        }
        return result;
    }

    private static Map<String, String> propsToMap(MapProperties props) {
        Map<String, String> map = new LinkedHashMap<>();
        Iterator<String> it = props.getKeys();
        while (it.hasNext()) {
            String key = it.next();
            Object val = props.get(key);
            if (val != null) map.put(key, val.toString());
        }
        return map;
    }

    // ─── inner Builder helper برای RailZone ──────────────

    /** کمک به ساخت تدریجی RailZone از چند Object جداگانه */
    private static final class RailZoneBuilder {

        private final String id;
        private final com.pvz2.map.zones.RailZone.Direction dir;
        private final float speed;
        private final Rectangle bounds;
        private final List<com.pvz2.map.zones.RailZone.ConveyorSlot> slots = new ArrayList<>();
        private final List<PvzMapObject> segments = new ArrayList<>();

        RailZoneBuilder(String id, com.pvz2.map.zones.RailZone.Direction dir,
                float speed, Rectangle bounds) {
            this.id = id; this.dir = dir; this.speed = speed;
            this.bounds = new Rectangle(bounds);
        }
        void addConveyorSlot(com.pvz2.map.zones.RailZone.ConveyorSlot s){ slots.add(s); }
        void addRailSegment(PvzMapObject o){ segments.add(o); }

        com.pvz2.map.zones.RailZone build() {
            com.pvz2.map.zones.RailZone zone =
                new com.pvz2.map.zones.RailZone(id, dir, speed, bounds);
            for (com.pvz2.map.zones.RailZone.ConveyorSlot s : slots)
                zone.addConveyorSlot(s);
            for (PvzMapObject seg : segments)
                zone.addRailSegment(seg);
            return zone;
        }

    }

    private MapLoader() { }
}
