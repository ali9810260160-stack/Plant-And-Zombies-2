package com.pvz2.graphics.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * آیکون‌های ساده‌ی رویه‌ای (قفل، فلش چپ/راست) که با {@link Pixmap} کشیده
 * می‌شن — چون فایل تصویری برای این‌ها نداشتیم. سبک و بدون وابستگی به asset
 * جدید. نتیجه‌ها کش می‌شن؛ {@link #dispose()} در انتهای برنامه صدا زده بشه.
 */
public final class IconFactory {

    private IconFactory() {}

    private static final ObjectMap<String, Texture> CACHE = new ObjectMap<>();

    /** قفل ساده: بدنه‌ی مستطیلی + کمان بالا. رنگ قابل تنظیم. */
    public static TextureRegion lock(int size, Color color) {
        String key = "lock_" + size + "_" + color;
        Texture cached = CACHE.get(key);
        if (cached != null) return new TextureRegion(cached);

        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(0, 0, 0, 0);
        pm.fill();

        // کمان بالای قفل: یک دایره‌ی توپر که نیمه‌ی پایینش رو بعداً بدنه می‌پوشونه
        // + یک سوراخ داخلی برای توخالی‌شدن (با blending=None تا آلفا صفر واقعاً پاک بشه)
        int shackleOuterR = (int) (size * 0.24f);
        int shackleInnerR = (int) (size * 0.15f);
        int shackleCx = size / 2;
        int shackleCy = (int) (size * 0.36f);

        pm.setColor(color);
        pm.fillCircle(shackleCx, shackleCy, shackleOuterR);
        pm.setBlending(Pixmap.Blending.None);
        pm.setColor(0, 0, 0, 0);
        pm.fillCircle(shackleCx, shackleCy, shackleInnerR);
        pm.setBlending(Pixmap.Blending.SourceOver);

        // بدنه‌ی قفل (روی نیمه‌ی پایینی کمان می‌شینه و شکل نهایی رو می‌سازه)
        int bodyW = (int) (size * 0.62f);
        int bodyH = (int) (size * 0.46f);
        int bodyX = (size - bodyW) / 2;
        int bodyY = (int) (size * 0.40f);

        pm.setColor(color);
        pm.fillRectangle(bodyX, bodyY, bodyW, bodyH);

        // سوراخ کوچیک کلید در وسط بدنه (کنتراست تیره‌تر)
        pm.setColor(color.r * 0.45f, color.g * 0.45f, color.b * 0.45f, color.a);
        int holeR = Math.max(1, size / 14);
        pm.fillCircle(size / 2, bodyY + bodyH / 2, holeR);

        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        CACHE.put(key, tex);
        return new TextureRegion(tex);
    }

    public static TextureRegion lock(int size) {
        return lock(size, new Color(0.9f, 0.9f, 0.92f, 1f));
    }

    /** فلش شورون به چپ یا راست، برای دکمه‌های ناوبری کاروسل. */
    public static TextureRegion chevron(int size, boolean pointingRight, Color color) {
        String key = "chevron_" + size + "_" + pointingRight + "_" + color;
        Texture cached = CACHE.get(key);
        if (cached != null) return new TextureRegion(cached);

        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(0, 0, 0, 0);
        pm.fill();
        pm.setColor(color);

        int pad = size / 5;
        int thickness = Math.max(2, size / 6);
        for (int t = 0; t < thickness; t++) {
            if (pointingRight) {
                pm.drawLine(pad + t, pad, size - pad, size / 2);
                pm.drawLine(pad + t, size - pad, size - pad, size / 2);
            } else {
                pm.drawLine(size - pad - t, pad, pad, size / 2);
                pm.drawLine(size - pad - t, size - pad, pad, size / 2);
            }
        }

        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        CACHE.put(key, tex);
        return new TextureRegion(tex);
    }

    /** دایره‌ی توپر ساده — برای نقاط صفحه (page dots) کاروسل. */
    public static TextureRegion dot(int size, Color color) {
        String key = "dot_" + size + "_" + color;
        Texture cached = CACHE.get(key);
        if (cached != null) return new TextureRegion(cached);

        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(0, 0, 0, 0);
        pm.fill();
        pm.setColor(color);
        pm.fillCircle(size / 2, size / 2, size / 2);

        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        CACHE.put(key, tex);
        return new TextureRegion(tex);
    }

    public static void dispose() {
        for (Texture t : CACHE.values()) t.dispose();
        CACHE.clear();
    }
}
