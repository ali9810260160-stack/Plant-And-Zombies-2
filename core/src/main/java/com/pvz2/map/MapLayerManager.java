package com.pvz2.map;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.MapGroupLayer;

import java.util.ArrayList;
import java.util.List;

/**
 * مدیریت visibility گروه‌های لایه — Map Layer Manager.
 *
 * بر اساس TILED_MAP_GUIDE.md بخش ۴ عمل می‌کند:
 *   - گروه shared: همیشه visible
 *   - گروه فصل: فقط فصل فعال
 *   - گروه‌های special: فقط آن‌هایی که در LevelMapConfig هستند
 *   - گروه مینی‌گیم: فقط اگر تنظیم شده
 *   - گروه camera: همیشه پردازش می‌شود (visibility اهمیتی ندارد)
 *
 * نکته: گروه‌ها با Property `pvz.type` شناخته می‌شوند.
 * لایه‌های داخل گروه visibility را از گروه به ارث می‌برند.
 */
public final class MapLayerManager {

    /** نام Group Layer های شناخته‌شده */
    private static final String TYPE_SHARED   = "SHARED";
    private static final String TYPE_CHAPTER  = "CHAPTER";
    private static final String TYPE_SPECIAL  = "SPECIAL";
    private static final String TYPE_MINIGAME = "MINIGAME";
    private static final String TYPE_CAMERA   = "CAMERA";

    private final TiledMap map;

    public MapLayerManager(TiledMap map) {
        this.map = map;
    }

    // ─── فعال‌سازی اصلی ────────────────────────────────────

    /**
     * همه گروه‌ها را بر اساس config تنظیم می‌کند.
     * این متد ابتدا همه را hide می‌کند، سپس مناسب‌ها را show می‌کند.
     */
    public void applyConfig(LevelMapConfig config) {
        // مرحله ۱: همه group های top-level را hide کن
        hideAllGroups(map.getLayers());

        // مرحله ۲: گروه shared را همیشه نشان بده
        setGroupVisible(map.getLayers(), TYPE_SHARED, null, true);

        // مرحله ۳: گروه camera را همیشه نشان بده (برای parse — render نمی‌شود)
        setGroupVisible(map.getLayers(), TYPE_CAMERA, null, true);

        if (config.isMinigame()) {
            // حالت مینی‌گیم: فقط گروه مینی‌گیم + shared
            String mgGroup = config.getMinigameGroupName();
            if (mgGroup != null)
                setGroupByName(map.getLayers(), mgGroup, true);
        } else {
            // حالت عادی: فصل + special ها
            String chGroup = config.getChapterGroupName();
            setGroupByName(map.getLayers(), chGroup, true);

            for (String specialId : config.getSpecialIds()) {
                String spGroup = config.getSpecialGroupName(specialId);
                if (spGroup != null)
                    setGroupByName(map.getLayers(), spGroup, true);
            }
        }
    }

    // ─── تنظیم بر اساس pvz.type ─────────────────────────────

    /**
     * تمام گروه‌هایی که pvz.type == typeValue دارند را visible/hidden می‌کند.
     * @param specificId اگر null نباشد، فقط گروه با pvz.chapter/pvz.special_id/pvz.minigame_id == specificId
     */
    private void setGroupVisible(MapLayers layers, String typeValue,
                                 String specificId, boolean visible) {
        for (MapLayer layer : layers) {
            if (!(layer instanceof MapGroupLayer)) continue;
            MapProperties props = layer.getProperties();
            String pvzType = props.get("pvz.type", "", String.class);

            if (!typeValue.equalsIgnoreCase(pvzType)) continue;

            if (specificId != null) {
                // چک کن که id هم مطابقت دارد
                String id = getGroupId(props, typeValue);
                if (!specificId.equalsIgnoreCase(id)) continue;
            }

            setLayerVisibilityRecursive(layer, visible);
        }
    }

    /** فعال/غیرفعال کردن گروه بر اساس نام دقیق */
    private void setGroupByName(MapLayers layers, String groupName, boolean visible) {
        for (MapLayer layer : layers) {
            if (!(layer instanceof MapGroupLayer)) continue;
            if (groupName.equalsIgnoreCase(layer.getName())) {
                setLayerVisibilityRecursive(layer, visible);
                return;
            }
        }
        // نادیده بگیر اگر پیدا نشد — گروه ممکن است هنوز طراحی نشده باشد
    }

    // ─── کمکی ───────────────────────────────────────────────

    /** hide کردن تمام Group Layer های سطح اول */
    private void hideAllGroups(MapLayers layers) {
        for (MapLayer layer : layers) {
            if (layer instanceof MapGroupLayer)
                setLayerVisibilityRecursive(layer, false);
        }
    }

    /** تنظیم visibility به صورت بازگشتی برای گروه و تمام فرزندانش */
    public static void setLayerVisibilityRecursive(MapLayer layer, boolean visible) {
        layer.setVisible(visible);
        if (layer instanceof MapGroupLayer) {
            for (MapLayer child : ((MapGroupLayer) layer).getLayers())
                setLayerVisibilityRecursive(child, visible);
        }
    }

    /** استخراج ID گروه بر اساس نوع */
    private String getGroupId(MapProperties props, String type) {
        switch (type.toUpperCase()) {
            case TYPE_CHAPTER:  return props.get("pvz.chapter",    "", String.class);
            case TYPE_SPECIAL:  return props.get("pvz.special_id", "", String.class);
            case TYPE_MINIGAME: return props.get("pvz.minigame_id","", String.class);
            default:            return "";
        }
    }

    // ─── utility های عمومی ──────────────────────────────────

    /** لیست تمام Group Layer های سطح اول با pvz.type مشخص */
    public List<MapGroupLayer> getGroupsByType(String type) {
        List<MapGroupLayer> result = new ArrayList<>();
        for (MapLayer layer : map.getLayers()) {
            if (!(layer instanceof MapGroupLayer)) continue;
            String pvzType = layer.getProperties().get("pvz.type", "", String.class);
            if (type.equalsIgnoreCase(pvzType))
                result.add((MapGroupLayer) layer);
        }
        return result;
    }

    /** یافتن گروه با نام دقیق */
    public MapGroupLayer findGroupByName(String name) {
        for (MapLayer layer : map.getLayers()) {
            if (layer instanceof MapGroupLayer && name.equalsIgnoreCase(layer.getName()))
                return (MapGroupLayer) layer;
        }
        return null;
    }

    /**
     * جمع‌آوری تمام Object Layer های visible زیر یک گروه.
     * برای parse کردن اشیاء بعد از applyConfig.
     */
    public List<com.badlogic.gdx.maps.MapLayer> collectVisibleObjectLayers() {
        List<com.badlogic.gdx.maps.MapLayer> result = new ArrayList<>();
        collectObjectLayersRecursive(map.getLayers(), result);
        return result;
    }

    private void collectObjectLayersRecursive(MapLayers layers,
                                              List<com.badlogic.gdx.maps.MapLayer> out) {
        for (MapLayer layer : layers) {
            if (!layer.isVisible()) continue;
            if (layer instanceof MapGroupLayer) {
                collectObjectLayersRecursive(((MapGroupLayer) layer).getLayers(), out);
            } else if (layer instanceof com.badlogic.gdx.maps.MapLayer
                    && !(layer instanceof com.badlogic.gdx.maps.tiled.TiledMapTileLayer)) {
                out.add(layer);
            }
        }
    }
}
