package com.pvz2.map;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * یک شیء پارس‌شده از Object Layer نقشه Tiled.
 *
 * این کلاس صرفاً داده است (Data class) — هیچ منطق بازی‌ای ندارد.
 * نقش اشیاء در نقشه: «pointer» یا «spawn marker» هستند.
 * موجودیت واقعی (گیاه، زامبی، لانمووِر و...) توسط سیستم بازی ایجاد می‌شود.
 */
public final class PvzMapObject {

    private final MapObjectType type;
    private final String        name;

    /** موقعیت و اندازه در مختصات دنیای libGDX (Y-up) */
    private final Rectangle worldBounds;

    /** مختصات مرکز */
    private final Vector2 center;

    /** تمام Custom Property های Tiled این شیء */
    private final Map<String, String> properties;

    // ─── فیلدهای از پیش پارس‌شده — برای دسترسی سریع ─────

    /** لاین (ردیف) این شیء در گرید — از پایین (libGDX) */
    private final int lane;

    /** ستون این شیء در گرید */
    private final int col;

    private PvzMapObject(Builder b) {
        this.type        = b.type;
        this.name        = b.name;
        this.worldBounds = b.worldBounds;
        this.center      = new Vector2(b.worldBounds.x + b.worldBounds.width  / 2f,
                                       b.worldBounds.y + b.worldBounds.height / 2f);
        this.properties  = Collections.unmodifiableMap(b.properties);
        this.lane        = b.lane;
        this.col         = b.col;
    }

    // ─── Getters ───────────────────────────────────────────

    public MapObjectType getType()        { return type; }
    public String        getName()        { return name; }
    public Rectangle     getWorldBounds() { return worldBounds; }
    public Vector2       getCenter()      { return center; }
    public int           getLane()        { return lane; }
    public int           getCol()         { return col; }

    // ─── دسترسی به properties ──────────────────────────────

    public String  getProp(String key, String  def) { return properties.getOrDefault(key, def); }
    public int     getPropInt(String key, int   def) {
        String v = properties.get(key);
        if (v == null) return def;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return def; }
    }
    public float   getPropFloat(String key, float def) {
        String v = properties.get(key);
        if (v == null) return def;
        try { return Float.parseFloat(v.trim()); } catch (NumberFormatException e) { return def; }
    }
    public boolean getPropBool(String key, boolean def) {
        String v = properties.get(key);
        if (v == null) return def;
        return "true".equalsIgnoreCase(v.trim());
    }

    @Override
    public String toString() {
        return "PvzMapObject{type=" + type + ", name='" + name + "', lane=" + lane + ", col=" + col + "}";
    }

    // ─── Builder ───────────────────────────────────────────

    public static class Builder {
        private MapObjectType type = MapObjectType.UNKNOWN;
        private String name        = "";
        private Rectangle worldBounds = new Rectangle();
        private final Map<String, String> properties = new HashMap<>();
        private int lane = -1;
        private int col  = -1;

        public Builder type(MapObjectType t)        { this.type = t;        return this; }
        public Builder name(String n)               { this.name = n;        return this; }
        public Builder bounds(Rectangle r)          { this.worldBounds = r; return this; }
        public Builder bounds(float x, float y, float w, float h) {
            this.worldBounds = new Rectangle(x, y, w, h); return this;
        }
        public Builder prop(String k, String v)     { properties.put(k, v); return this; }
        public Builder props(Map<String, String> m) { properties.putAll(m); return this; }
        public Builder lane(int l)                  { this.lane = l;        return this; }
        public Builder col(int c)                   { this.col  = c;        return this; }
        public PvzMapObject build()                 { return new PvzMapObject(this); }
    }
}
