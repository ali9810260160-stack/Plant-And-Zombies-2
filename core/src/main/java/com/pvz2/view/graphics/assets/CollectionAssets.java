package com.pvz2.graphics.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;
import java.util.Map;

/**
 * بارگذاری و کش کردن تصاویر خام صفحه‌ی کلکسیون (پلاگین/زامبی/UI).
 *
 * <p>برخلاف {@link GameAssets} که از {@code TextureBank} با نام region استفاده
 * می‌کنه، این کلاس مستقیماً فایل‌های PNG رو از پوشه‌ی {@code assets/} پروژه با
 * {@link Gdx#files} می‌خونه (lazy load — فقط وقتی درخواست بشه).
 *
 * <p>اگر فایلی موجود نباشه (هنوز اضافه نشده یا مسیر اشتباهه)، بجای کرش،
 * {@code null} برمی‌گردونه؛ کدهای صدازننده باید حالت fallback (مثلاً مستطیل
 * خاکستری با {@link GameAssets#getWhiteRegion()}) رو خودشون رسم کنن.
 */
public final class CollectionAssets {

    private static CollectionAssets instance;

    private final Map<String, Texture> cache = new HashMap<>();

    private CollectionAssets() {}

    public static CollectionAssets getInstance() {
        if (instance == null) instance = new CollectionAssets();
        return instance;
    }

    /**
     * یک TextureRegion از مسیر داده‌شده (نسبی به ریشه‌ی assets، مثلاً
     * {@code CollectionAssetPaths.plantPath(PlantType.PEASHOOTER)}).
     *
     * @return region یا {@code null} اگر فایل پیدا نشد / مسیر خالی بود
     */
    public TextureRegion region(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return null;
        Texture tex = cache.get(relativePath);
        if (tex != null) return new TextureRegion(tex);
        if (cache.containsKey(relativePath)) return null; // قبلاً امتحان شده و پیدا نشده

        try {
            FileHandle fh = Gdx.files.internal(relativePath);
            if (!fh.exists()) {
                Gdx.app.error("CollectionAssets", "Missing file: " + relativePath);
                cache.put(relativePath, null);
                return null;
            }
            tex = new Texture(fh);
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            cache.put(relativePath, tex);
            return new TextureRegion(tex);
        } catch (Exception e) {
            Gdx.app.error("CollectionAssets", "Failed to load: " + relativePath, e);
            cache.put(relativePath, null);
            return null;
        }
    }

    public boolean exists(String relativePath) {
        return region(relativePath) != null;
    }

    public void dispose() {
        for (Texture t : cache.values()) {
            if (t != null) t.dispose();
        }
        cache.clear();
    }
}
