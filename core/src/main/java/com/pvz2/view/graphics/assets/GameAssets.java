package com.pvz2.graphics.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.ObjectMap;
import com.pvz2.graphics.util.GameConfig;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

/**
 * مدیریت مرکزی تمام asset های بازی.
 *
 * <p>پس از فراخوانی {@link #init()} در {@code PVZApplication.create()}،
 * با {@link #getInstance()} ()} از هر جایی قابل دسترسی است.
 *
 * <p>اگر {@code pvz.assets} تنظیم نشده باشد، حالت fallback فعال می‌شود:
 * گیاهان و زامبی‌ها با مستطیل‌های رنگی نمایش داده می‌شوند.
 */
public final class GameAssets {

    private static GameAssets instance;

    private Skin         skin;
    private TextureBank textureBank;
    private PamPlayer pamPlayer;
    private Texture      whiteTexture;
    private TextureRegion whiteRegion;
    /** دیسک نرم شعاعی (دایره با محو شدن آلفا) — برای پرتابه‌ها و خورشیدهای گرد و درخشان */
    private Texture       discTexture;
    private TextureRegion discRegion;
    /** فونت fallback مشترک — فقط اگر یک style از skin پیدا نشود ساخته می‌شود */
    private BitmapFont fallbackFont;

    /** کش تکسچرهای لوکال (فایل‌های PNG خودمون داخل پوشه‌ی assets/، نه پک RTON). */
    private final ObjectMap<String, Texture> localTextureCache = new ObjectMap<>();

    private GameAssets() {}

    public static void init() {
        if (instance != null) return;
        instance = new GameAssets();
        instance.load();
    }

    public static GameAssets getInstance() {
        if (instance == null)
            throw new IllegalStateException("Call GameAssets.init() first");
        return instance;
    }

    private void load() {
        loadSkin();
        loadWhiteTexture();
        loadDiscTexture();
        loadPvzAssets();
    }

    private void loadSkin() {
        skin = PvzSkin.get();
        Gdx.app.log("GameAssets", "pvz-skin loaded");
    }

    private void loadWhiteTexture() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        whiteTexture = new Texture(pm);
        pm.dispose();
        whiteRegion = new TextureRegion(whiteTexture);
    }

    /**
     * می‌سازد یک دیسک نرمِ سفید (۶۴×۶۴) که آلفای آن از مرکز (۱) تا لبه (۰) کاهش می‌یابد.
     * با tint کردن این region می‌توان پرتابه‌ها و خورشیدهای گرد و درخشان کشید
     * بدون نیاز به ShapeRenderer (که در pass جاریِ SpriteBatch در دسترس نیست).
     */
    private void loadDiscTexture() {
        int size = 64;
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        float c = (size - 1) / 2f;
        for (int yy = 0; yy < size; yy++) {
            for (int xx = 0; xx < size; xx++) {
                float dx = (xx - c) / c;
                float dy = (yy - c) / c;
                float d = (float) Math.sqrt(dx * dx + dy * dy); // ۰ مرکز، ۱ لبه
                float a = d >= 1f ? 0f : 1f - (d * d); // محو نرم به سمت لبه
                pm.setColor(1f, 1f, 1f, a);
                pm.drawPixel(xx, yy);
            }
        }
        discTexture = new Texture(pm);
        discTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        discRegion = new TextureRegion(discTexture);
    }

    private void loadPvzAssets() {
        String path = GameConfig.PVZ_ASSETS_PATH;
        if (path == null || path.isEmpty()) {
            Gdx.app.log("GameAssets",
                "pvz.assets not set — fallback mode (colored rectangles)");
            return;
        }
        try {
            FileHandle dir = Gdx.files.absolute(path.trim());
            if (!dir.exists()) {
                Gdx.app.error("GameAssets", "Asset folder not found: " + path);
                return;
            }
            textureBank = new TextureBank("768", dir);
            pamPlayer   = new PamPlayer(textureBank, dir);
            Gdx.app.log("GameAssets", "libPVZ loaded from: " + path);
        } catch (Exception e) {
            Gdx.app.error("GameAssets", "libPVZ loading failed", e);
            textureBank = null;
            pamPlayer   = null;
        }
    }

    /** در هر فریم فراخوانی شود تا صف async بارگذاری پردازش شود. */
    public void update() {
        if (textureBank != null) textureBank.update();
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public Skin          getSkin()          { return skin; }
    public PamPlayer     getPamPlayer()     { return pamPlayer; }
    public TextureBank   getTextureBank()   { return textureBank; }
    public TextureRegion getWhiteRegion()   { return whiteRegion; }
    public TextureRegion getDiscRegion()    { return discRegion; }
    public boolean       hasPvzAssets()     { return pamPlayer != null; }

    /**
     * یک TextureRegion از texture atlas بازی اصلی.
     * اگر region وجود نداشت یا asset در دسترس نبود، whiteRegion برمی‌گردد.
     *
     * @param regionName نام region — در asset browser قابل جستجو
     */
    public TextureRegion region(String regionName) {
        if (textureBank == null || regionName == null) return whiteRegion;
        try {
            TextureRegion r = textureBank.region(regionName);
            return r != null ? r : whiteRegion;
        } catch (Exception e) {
            return whiteRegion;
        }
    }

    /**
     * یک TextureRegion از فایل‌های PNG لوکال داخل پوشه‌ی {@code assets/} پروژه
     * (مسیر نسبی، مثلاً {@code "adventure/card_egypt.png"}) — برخلاف
     * {@link #region(String)} که از پک RTON اصلی بازی می‌خونه.
     * <p>
     * نتیجه کش می‌شود؛ اگر فایل پیدا نشه، {@code whiteRegion} برمی‌گرده و
     * برنامه کرش نمی‌کنه.
     *
     * @param relativePath مسیر نسبی به ریشه‌ی پوشه‌ی assets
     */
    public TextureRegion local(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return whiteRegion;
        Texture cached = localTextureCache.get(relativePath);
        if (cached != null) return new TextureRegion(cached);
        try {
            FileHandle fh = Gdx.files.internal(relativePath);
            if (!fh.exists()) {
                Gdx.app.error("GameAssets", "Local asset not found: " + relativePath);
                return whiteRegion;
            }
            Texture tex = new Texture(fh);
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            localTextureCache.put(relativePath, tex);
            return new TextureRegion(tex);
        } catch (Exception e) {
            Gdx.app.error("GameAssets", "Failed to load local asset: " + relativePath, e);
            return whiteRegion;
        }
    }

    public void dispose() {
        if (skin != null)         skin.dispose();
        if (whiteTexture != null) whiteTexture.dispose();
        if (discTexture != null)  discTexture.dispose();
        if (textureBank != null)  textureBank.dispose();

        if (fallbackFont != null) fallbackFont.dispose();
    }

    /**
     * فونت مورد استفاده‌ی یک Label style را برمی‌گرداند — <b>نه</b> یک
     * BitmapFont resource با همین نام. (باگ رایجی که در چند فایل پیدا شد:
     * {@code Skin.getFont(name)} برای نام یک Label style نه null برمی‌گرداند
     * نه آن را پیدا می‌کند — مستقیماً {@code GdxRuntimeException} پرتاب می‌کند،
     * چون دنبال یک BitmapFont resource جدا با آن اسم می‌گردد، در حالی که
     * "medium"/"default" و مشابه، در pvz-skin اسم Label style هستند نه فونت.)
     *
     * <p>اگر style یا فونتش پیدا نشود، فونت پیش‌فرض داخلی LibGDX (بدون نیاز
     * به هیچ asset ای) برمی‌گردد — این متد هرگز throw نمی‌کند.
     *
     * @param labelStyleName مثلاً "medium", "default", "big"
     */
    public BitmapFont fontOf(String labelStyleName) {
        try {
            if (skin != null && labelStyleName != null
                    && skin.has(labelStyleName, Label.LabelStyle.class)) {
                BitmapFont f = skin.get(labelStyleName, Label.LabelStyle.class).font;
                if (f != null) return f;
            }
        } catch (Exception ignored) {
            // ادامه به fallback
        }
        if (fallbackFont == null) fallbackFont = new BitmapFont();
        return fallbackFont;
    }
}
