package com.pvz2.view.game.anim.controller;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.graphics.util.GameCoords;
import com.pvz2.model.Projectile;
import com.pvz2.model.enums.ProjectileType;
import com.pvz2.view.game.anim.config.ProjectileAnimConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * رندر پرتابه‌ها.
 *
 * دو نوع حرکت پشتیبانی می‌شود:
 *
 *   خطی (isArc=false):
 *     پرتابه با سرعت ثابت در محور X حرکت می‌کند.
 *     مختصات Y ثابت است.
 *     مثال: پرتابه Peashooter، Snow Pea، Fire Pea
 *
 *   سهموی (isArc=true):
 *     پرتابه یک قوس سهمی می‌کشد از source به target.
 *     در اوج قوس یک bounce ظاهری ایجاد می‌شود.
 *     مثال: Kernel-pult (butter)، Cabbage-pult، Catapult
 *
 * هر پرتابه یک ID منحصربه‌فرد دارد که برای ردیابی stateTime
 * و rotation استفاده می‌شود. پس از حذف پرتابه، باید removeProjectile()
 * صدا زده شود تا leak جلوگیری شود.
 *
 * ─── مختصات (رفع باگ نسخه ۲) ─────────────────────────────────────
 * قبلاً از فرمول محلی {@code GRID_ORIGIN_Y + (row-1)*CELL_H} استفاده
 * می‌شد که محور row را برعکس نمی‌کرد (برخلاف {@link GameCoords} که در
 * GridRenderer/EntityRenderer استفاده می‌شود). این یعنی پرتابه‌های
 * ردیف ۱ در پایین صفحه و ردیف ۵ در بالا ظاهر می‌شدند — دقیقاً همان
 * باگی که در ZombieAnimController/PlantAnimController/SunAnimController
 * هم بود. اکنون همه‌جا از GameCoords.toScreenX/toScreenY استفاده می‌شود.
 */
public class ProjectileRenderer {

    // ─── ثابت اندازه سلول (برای محاسبه arc offset؛ مختصات از GameCoords) ──
    public static final float CELL_W = 100f;
    public static final float CELL_H = 100f;

    /** ارتفاعِ ثابتِ اوجِ مسیرِ سهموی وقتی config مقداری ندارد (پیکسل). */
    private static final float DEFAULT_ARC_PEAK = 90f;
    /** طولِ افقیِ فرضیِ پرتابِ هوایی وقتی هدفِ مشخصی ثبت نشده (بر حسبِ ستون). */
    private static final float ARC_FALLBACK_SPAN = 4.5f;

    // ─── رنگ tint پیش‌فرض برای ProjectileType های مختلف ─────────
    private static final Map<ProjectileType, Color> DEFAULT_TINTS = new HashMap<>();
    static {
        DEFAULT_TINTS.put(ProjectileType.ICE,    new Color(0.4f, 0.7f, 1.0f, 1f));
        DEFAULT_TINTS.put(ProjectileType.FIRE,   new Color(1.0f, 0.4f, 0.1f, 1f));
        DEFAULT_TINTS.put(ProjectileType.POISON, new Color(0.5f, 1.0f, 0.5f, 1f));
    }

    // ─── config ──────────────────────────────────────────────────
    private final Map<String, ProjectileAnimConfig> configMap;

    // ─── per-projectile state ────────────────────────────────────
    /** stateTime برای انیمیشن clip هر پرتابه */
    private final Map<Integer, Float>   stateTimers    = new HashMap<>();
    /** زاویه rotation برای پرتابه‌های rotates=true */
    private final Map<Integer, Float>   rotations      = new HashMap<>();

    public ProjectileRenderer(Map<String, ProjectileAnimConfig> configMap) {
        this.configMap = configMap;
    }

    // ════════════════════════════════════════════════════════════
    //  Render
    // ════════════════════════════════════════════════════════════

    /**
     * رندر یک پرتابه.
     *
     * @param batch       SpriteBatch در حالت begin()
     * @param proj        مدل پرتابه از فاز ۱
     * @param delta       ثانیه گذشته از آخرین فریم
     * @param projectileId شناسه منحصربه‌فرد (برای ردیابی state)
     * @param pamPlayer   نمونه PamPlayer از libPVZ
     */
    public void render(SpriteBatch batch, Projectile proj,
                       float delta, int projectileId, Object pamPlayer) {

        // config معمولاً null است (JSON فعلاً آرایه‌ی projectiles ندارد)؛ در آن
        // صورت پرتابه به‌صورتِ procedural (دیسکِ درخشانِ متمایز به‌ازای هر نوع)
        // رسم می‌شود تا همیشه دیده شود و انواع قابلِ تشخیص باشند (CI2/CJ3).
        ProjectileAnimConfig cfg = resolveConfig(proj);

        // ── به‌روزرسانی state time ─────────────────────────────
        float st = stateTimers.getOrDefault(projectileId, 0f) + delta;
        stateTimers.put(projectileId, st);

        // ── مختصات base (از GameCoords — شامل معکوس‌سازی صحیح محور row) ──
        float worldX = GameCoords.toScreenX(proj.getX());
        float worldY = GameCoords.toScreenY(proj.getY());

        // ── جابجایی Y برای پرتابه سهموی (قوسِ پیوسته با اوجِ ثابت) ──
        if (proj.isArc()) {
            float peak = (cfg != null && cfg.arcHeight > 0) ? cfg.arcHeight : DEFAULT_ARC_PEAK;
            worldY += computeArcYOffset(proj, peak);
        }

        Color saved = batch.getColor().cpy();

        if (cfg != null && cfg.pamPath != null && cfg.clip != null) {
            // ── rotation (فقط برای مسیرِ PAM) ──
            float rotation = 0f;
            if (cfg.rotates) {
                float rot = rotations.getOrDefault(projectileId, 0f) + cfg.rotationSpeed * delta;
                if (rot >= 360f) rot -= 360f;
                rotations.put(projectileId, rot);
                rotation = rot;
            }
            applyTint(batch, proj, cfg);
            renderPam(batch, pamPlayer, cfg, st, worldX, worldY, rotation);
        } else {
            drawProceduralProjectile(batch, proj.getType(), worldX, worldY);
        }

        batch.setColor(saved);
    }

    // ════════════════════════════════════════════════════════════
    //  رندرِ procedural — ظاهرِ متمایزِ هر نوعِ پرتابه (CJ3)
    // ════════════════════════════════════════════════════════════

    private void drawProceduralProjectile(SpriteBatch batch, ProjectileType type, float x, float y) {
        if (type == null) type = ProjectileType.NORMAL;
        switch (type) {
            case FIRE:
                glow(batch, x, y, 20f, 1f, 0.45f, 0.05f, 0.55f);
                disc(batch, x, y, 13f, 1f, 0.62f, 0.10f, 1f);
                disc(batch, x, y, 6f, 1f, 0.95f, 0.55f, 1f);
                break;
            case ICE:
                glow(batch, x, y, 19f, 0.55f, 0.85f, 1f, 0.5f);
                disc(batch, x, y, 12f, 0.65f, 0.90f, 1f, 1f);
                disc(batch, x, y, 5f, 0.95f, 0.99f, 1f, 1f);
                break;
            case POISON:
                glow(batch, x, y, 19f, 0.55f, 0.20f, 0.75f, 0.5f);
                disc(batch, x, y, 12f, 0.55f, 0.18f, 0.78f, 1f);
                disc(batch, x, y, 5f, 0.75f, 1f, 0.35f, 1f);
                break;
            case LOBBED:
                glow(batch, x, y, 26f, 0.9f, 0.35f, 0.30f, 0.4f);
                disc(batch, x, y, 18f, 0.80f, 0.24f, 0.28f, 1f);
                disc(batch, x, y, 8f, 1f, 0.55f, 0.55f, 1f);
                break;
            case STRIKE:
                glow(batch, x, y, 20f, 1f, 0.95f, 0.4f, 0.5f);
                disc(batch, x, y, 8f, 1f, 1f, 0.7f, 1f);
                batch.setColor(1f, 1f, 0.8f, 0.9f);
                batch.draw(GameAssets.getInstance().getWhiteRegion(), x - 16, y - 2.5f, 32, 5);
                batch.setColor(Color.WHITE);
                break;
            default: // NORMAL — نخودِ سبز
                glow(batch, x, y, 16f, 0.4f, 0.9f, 0.25f, 0.4f);
                disc(batch, x, y, 11f, 0.30f, 0.80f, 0.22f, 1f);
                disc(batch, x, y, 4.5f, 0.75f, 1f, 0.55f, 1f);
                break;
        }
        batch.setColor(Color.WHITE);
    }

    /** دیسکِ توپُر با شعاعِ r و رنگِ داده‌شده (از discRegion — دایره‌ی نرم). */
    private void disc(SpriteBatch batch, float cx, float cy, float r,
                      float rr, float gg, float bb, float aa) {
        batch.setColor(rr, gg, bb, aa);
        TextureRegion d = GameAssets.getInstance().getDiscRegion();
        batch.draw(d, cx - r, cy - r, r * 2f, r * 2f);
    }

    /** هاله‌ی نرمِ محیطی (شعاعِ بزرگ‌تر، آلفای کم). */
    private void glow(SpriteBatch batch, float cx, float cy, float r,
                      float rr, float gg, float bb, float aa) {
        disc(batch, cx, cy, r, rr, gg, bb, aa * 0.5f);
    }

    // ════════════════════════════════════════════════════════════
    //  منطق داخلی
    // ════════════════════════════════════════════════════════════

    /**
     * محاسبه آفست Y برای مسیر سهموی.
     *
     * فرمول: offset = arcHeight * 4 * t * (1 - t)   که t ∈ [0, 1]
     *
     * t از موقعیت X فعلی و X مقصد محاسبه می‌شود:
     *   t = 0 → آغاز پرتاب (زامبی سمت راست)
     *   t = 0.5 → اوج قوس
     *   t = 1 → رسیدن به مقصد
     *
     * @param proj      پرتابه (x=موقعیت فعلی، targetX=مقصد)
     * @param arcHeight ارتفاع اوج (از config)
     */
    private float computeArcYOffset(Projectile proj, float arcHeight) {
        // مبنا: موقعیتِ واقعیِ شلیک (startX از مدل) تا هدف (targetX یا یک طولِ
        // فرضی). اوجِ قوس برای همه‌ی پرتابه‌ها ثابت است و t نسبتِ پیشرفتِ افقی
        // است — پس حرکت پیوسته و مطابقِ CI5 است.
        double sx = proj.getStartX();
        double cx = proj.getX();
        double end = (proj.getTargetX() > 0 && proj.getTargetX() != sx)
                ? proj.getTargetX() : sx + ARC_FALLBACK_SPAN;

        float span = (float) Math.abs(end - sx);
        if (span < 0.001f) return 0f;

        float t = (float) (Math.abs(cx - sx) / span);
        t = Math.max(0f, Math.min(1f, t));

        // فرمول سهمی استاندارد: h = peak · 4·t·(1−t)
        return arcHeight * 4f * t * (1f - t);
    }

    private void renderPam(SpriteBatch batch, Object pamPlayer,
                           ProjectileAnimConfig cfg, float stateTime,
                           float worldX, float worldY, float rotation) {
        // ساده‌سازی: rotation واقعی (چرخش تصویر) نیاز به تغییر ماتریس Batch
        // در وسط یک پاس رندر دارد که پرخطر است (چون این batch از GameRenderer
        // می‌آید و از قبل begin شده) — فعلاً پرتابه‌های چرخنده (مثل کره‌ها)
        // بدون چرخش بصری واقعی رندر می‌شوند؛ حرکت و تغییر frame همچنان درست کار می‌کند.
        PamDrawUtil.draw(pamPlayer, batch, cfg.pamPath, cfg.clip, stateTime,
                worldX, worldY, true, 24f, 24f, new Color(0.9f, 0.85f, 0.3f, 0.95f));
    }

    /** پیدا کردن config مناسب برای این پرتابه */
    private ProjectileAnimConfig resolveConfig(Projectile proj) {
        // ابتدا با ProjectileType.name() جستجو کن
        String key = proj.getType().name();
        ProjectileAnimConfig cfg = configMap.get(key);
        return cfg;
    }

    private void applyTint(SpriteBatch batch, Projectile proj, ProjectileAnimConfig cfg) {
        Color tint = null;

        // tint از config
        if (cfg.tintColor != null && !cfg.tintColor.isEmpty()) {
            try { tint = Color.valueOf(cfg.tintColor + "FF"); } catch (Exception ignored) { }
        }

        // tint پیش‌فرض بر اساس ProjectileType (اگر config tint ندارد)
        if (tint == null) {
            tint = DEFAULT_TINTS.get(proj.getType());
        }

        if (tint != null) {
            Color c = batch.getColor();
            batch.setColor(c.r * tint.r, c.g * tint.g, c.b * tint.b, c.a);
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Life Cycle
    // ════════════════════════════════════════════════════════════

    /** پاکسازی state یک پرتابه که از صحنه حذف شده */
    public void removeProjectile(int projectileId) {
        stateTimers.remove(projectileId);
        rotations.remove(projectileId);
    }

    /** پاکسازی همه state ها (مثلاً هنگام بازشروع بازی) */
    public void clear() {
        stateTimers.clear();
        rotations.clear();
    }
}
