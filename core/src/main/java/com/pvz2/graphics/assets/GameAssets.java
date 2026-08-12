package com.pvz2.graphics.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.pvz2.graphics.util.GameConfig;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;

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

    public void dispose() {
        if (skin != null)         skin.dispose();
        if (whiteTexture != null) whiteTexture.dispose();
        if (textureBank != null)  textureBank.dispose();
    }
}
