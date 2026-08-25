package com.pvz2.map;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSets;
import com.badlogic.gdx.maps.MapGroupLayer;

import java.util.*;

/**
 * جایگزین‌کننده تصادفی کاشی — Tile Variant Resolver.
 *
 * الگوریتم:
 *   1. تمام لایه‌های دارای `pvz.allow_variants=true` را پیدا می‌کند
 *   2. برای هر خانه با کاشی دارای `pvz.variant_group`, تمام کاشی‌های هم‌گروه را می‌یابد
 *   3. با توجه به seed و موقعیت خانه، یک variant تصادفی انتخاب می‌کند
 *   4. کاشی خانه را جایگزین می‌کند
 *
 * Seed:
 *   seed_for_cell = (levelSeed * 7919L) ^ (row * LARGE_PRIME + col)
 *   این تضمین می‌کند: یکسان برای یک مرحله، متفاوت برای مراحل مختلف.
 */
public final class TileVariantResolver {

    private static final long LARGE_PRIME = 6700417L;

    /** کش: variant_group → لیست (tile, weight) مرتب‌شده */
    private final Map<String, List<WeightedTile>> variantCache = new HashMap<>();

    private final long levelSeed;

    public TileVariantResolver(long levelSeed) {
        this.levelSeed = levelSeed;
    }

    // ─── ورودی اصلی ────────────────────────────────────────

    /**
     * جایگزینی کاشی‌ها در کل نقشه.
     * @param map نقشه بارگذاری‌شده
     */
    public void resolve(TiledMap map) {
        buildVariantCache(map.getTileSets());
        processLayers(map.getLayers(), map);
    }

    // ─── ساخت کش variant ───────────────────────────────────

    private void buildVariantCache(TiledMapTileSets tileSets) {
        variantCache.clear();
//        for (TiledMapTileLayer ignored : new ArrayList<>()) { } // placeholder

        // ایتریشن روی همه Tileset ها
        for (com.badlogic.gdx.maps.tiled.TiledMapTileSet tileSet : tileSets) {
            for (TiledMapTile tile : tileSet) {
                MapProperties props = tile.getProperties();
                String group  = props.get("pvz.variant_group", null, String.class);
                if (group == null || group.isEmpty()) continue;

                int weight = 1;
                String wStr = props.get("pvz.variant_weight", "1", String.class);
                try { weight = Integer.parseInt(wStr.trim()); } catch (NumberFormatException ignored2) { }

                variantCache
                    .computeIfAbsent(group, k -> new ArrayList<>())
                    .add(new WeightedTile(tile, weight));
            }
        }
    }

    // ─── پردازش لایه‌ها ─────────────────────────────────────

    private void processLayers(MapLayers layers, TiledMap map) {
        for (MapLayer layer : layers) {
            if (layer instanceof MapGroupLayer) {
                // بازگشتی برای گروه‌ها
                processLayers(((MapGroupLayer) layer).getLayers(), map);
            } else if (layer instanceof TiledMapTileLayer) {
                MapProperties lp = layer.getProperties();
                boolean allowVariants = "true".equalsIgnoreCase(
                    lp.get("pvz.allow_variants", "false", String.class));
                if (allowVariants) {
                    resolveLayer((TiledMapTileLayer) layer);
                }
            }
        }
    }

    private void resolveLayer(TiledMapTileLayer layer) {
        int rows = layer.getHeight();
        int cols = layer.getWidth();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                TiledMapTileLayer.Cell cell = layer.getCell(col, row);
                if (cell == null || cell.getTile() == null) continue;

                MapProperties tileProps = cell.getTile().getProperties();
                String group = tileProps.get("pvz.variant_group", null, String.class);
                if (group == null || group.isEmpty()) continue;

                List<WeightedTile> variants = variantCache.get(group);
                if (variants == null || variants.isEmpty()) continue;

                TiledMapTile chosen = pickVariant(variants, row, col);
                cell.setTile(chosen);
            }
        }
    }

    // ─── انتخاب variant ─────────────────────────────────────

    private TiledMapTile pickVariant(List<WeightedTile> variants, int row, int col) {
        // seed خاص این خانه
        long cellSeed = (levelSeed * 7919L) ^ (row * LARGE_PRIME + col);
        Random rng = new Random(cellSeed);

        // وزن کل
        int totalWeight = 0;
        for (WeightedTile wt : variants) totalWeight += wt.weight;

        int roll = rng.nextInt(totalWeight);
        int cumulative = 0;
        for (WeightedTile wt : variants) {
            cumulative += wt.weight;
            if (roll < cumulative) return wt.tile;
        }
        return variants.get(0).tile; // fallback
    }

    // ─── داخلی ──────────────────────────────────────────────

    private static final class WeightedTile {
        final TiledMapTile tile;
        final int weight;
        WeightedTile(TiledMapTile t, int w) { this.tile = t; this.weight = w; }
    }
}
