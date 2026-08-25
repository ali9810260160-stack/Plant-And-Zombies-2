package com.pvz2.map;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * مدیریت دوربین نقشه — Camera Region Manager.
 *
 * دو ناحیه از TILED_MAP_GUIDE.md بخش ۷:
 *   - FIXED:    ناحیه پایه (خانه + گرید + ناحیه ورود زامبی)
 *   - EXTENDED: وقتی Rail Zone فعال است، دوربین بزرگ‌تر می‌شود
 *
 * دوربین به صورت نرم (smooth) بین دو حالت جابجا می‌شود.
 */
public final class CameraRegionManager {

    private static final float LERP_SPEED = 3.5f;  // سرعت انتقال نرم دوربین

    /** ناحیه پایه — همیشه موجود */
    private final Rectangle fixedRegion;

    /** ناحیه گسترش‌یافته — فقط وقتی Rail Zone فعال است */
    private final Rectangle extendedRegion;

    /** آیا در حالت extended هستیم؟ */
    private boolean extendedActive;

    /** ناحیه فعلی که دوربین دنبال می‌کند (برای lerp) */
    private final Rectangle currentRegion;

    public CameraRegionManager(Rectangle fixedRegion, Rectangle extendedRegion) {
        this.fixedRegion    = new Rectangle(fixedRegion);
        this.extendedRegion = extendedRegion != null
                              ? new Rectangle(extendedRegion)
                              : new Rectangle(fixedRegion);
        this.extendedActive = false;
        this.currentRegion  = new Rectangle(fixedRegion);
    }

    // ─── سوئیچ حالت ────────────────────────────────────────

    /** فعال/غیرفعال کردن ناحیه گسترش‌یافته */
    public void setExtended(boolean active) {
        if (this.extendedActive != active) {
            this.extendedActive = active;
        }
    }

    // ─── به‌روزرسانی دوربین ─────────────────────────────────

    /**
     * هر فریم صدا زده می‌شود — دوربین را به ناحیه هدف نزدیک می‌کند.
     *
     * @param camera دوربین libGDX
     * @param delta  زمان سپری‌شده (ثانیه)
     */
    public void update(OrthographicCamera camera, float delta) {
        Rectangle target = extendedActive ? extendedRegion : fixedRegion;

        // lerp ناحیه فعلی به سمت target
        float t = Math.min(1f, LERP_SPEED * delta);
        currentRegion.x      = MathUtils.lerp(currentRegion.x,      target.x,      t);
        currentRegion.y      = MathUtils.lerp(currentRegion.y,      target.y,      t);
        currentRegion.width  = MathUtils.lerp(currentRegion.width,  target.width,  t);
        currentRegion.height = MathUtils.lerp(currentRegion.height, target.height, t);

        // اعمال به دوربین
        applyToCamera(camera);
    }

    /** اعمال فوری بدون lerp — برای transition صفحه */
    public void applyImmediate(OrthographicCamera camera) {
        Rectangle target = extendedActive ? extendedRegion : fixedRegion;
        currentRegion.set(target);
        applyToCamera(camera);
    }

    private void applyToCamera(OrthographicCamera camera) {
        // مرکز دوربین
        camera.position.set(
            currentRegion.x + currentRegion.width  / 2f,
            currentRegion.y + currentRegion.height / 2f,
            0
        );
        // zoom بر اساس نسبت ناحیه به viewport
        float zoomX = currentRegion.width  / camera.viewportWidth;
        float zoomY = currentRegion.height / camera.viewportHeight;
        camera.zoom = Math.max(zoomX, zoomY);
        camera.update();
    }

    // ─── کمکی ──────────────────────────────────────────────

    /** تبدیل مختصات صفحه به world */
    public Vector2 screenToWorld(OrthographicCamera camera, float screenX, float screenY) {
        com.badlogic.gdx.math.Vector3 v = new com.badlogic.gdx.math.Vector3(screenX, screenY, 0);
        camera.unproject(v);
        return new Vector2(v.x, v.y);
    }

    // ─── Getters ────────────────────────────────────────────

    public Rectangle getFixedRegion()    { return new Rectangle(fixedRegion); }
    public Rectangle getExtendedRegion() { return new Rectangle(extendedRegion); }
    public Rectangle getCurrentRegion()  { return new Rectangle(currentRegion); }
    public boolean   isExtended()        { return extendedActive; }
}
