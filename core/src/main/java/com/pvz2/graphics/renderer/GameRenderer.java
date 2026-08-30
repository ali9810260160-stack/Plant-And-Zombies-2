package com.pvz2.graphics.renderer;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.GameStateSnapshot;
import com.pvz2.graphics.ServiceLocator;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.graphics.util.GameConfig;
import com.pvz2.map.CameraRegionManager;
import com.pvz2.map.MapService;
import com.pvz2.model.GameSession;
import com.pvz2.model.Level;
import com.pvz2.view.game.anim.AnimationSystem;

/**
 * هماهنگ‌کننده مرکزی رندر — در هر فریم توسط {@link com.pvz2.graphics.screens.GameScreen} فراخوانی می‌شود.
 *
 * <p>ترتیب رندر:
 * ۰. نقشه Tiled (world.tmx) — اگر بارگذاری شده باشد؛ جدا از batch اصلی
 * ۱. پس‌زمینه رویه‌ای (fallback) / کاشی‌ها / چمن‌زن‌ها
 * ۲. موجودیت‌ها (بخش سوم — گیاهان/زامبی‌ها/پرتابه‌ها/خورشیدها با انیمیشن واقعی
 *    از طریق {@link AnimationSystem}؛ اگر asset واقعی PAM در دسترس نباشد،
 *    خودِ AnimationSystem مستطیل‌های رنگی جایگزین رسم می‌کند)
 * ۳. جلوه‌های بصری (انفجار، فلش، ...)
 * ۴. HUD
 *
 * <p><b>اتصال به سیستم نقشه (com.pvz2.map):</b> در سازنده، با
 * {@link MapService#tryLoad(Level)} تلاش می‌شود {@code world.tmx} بارگذاری شود.
 * اگر فایل هنوز طراحی نشده یا لایه/آبجکتی طبق TILED_MAP_GUIDE.md نداشته باشد،
 * نتیجه {@code null} است و {@link GridRenderer} به‌طور خودکار به رندر رویه‌ای
 * (مستطیل‌های رنگی) برمی‌گردد — یعنی این کلاس همیشه قابل استفاده است، چه نقشه
 * طراحی‌شده باشد چه نه.
 */
public class GameRenderer implements Disposable {

    private final SpriteBatch      batch;
    private final GridRenderer     grid;
    private final AnimationSystem  animSystem;
    private final BossRenderer     boss;
    private final HudRenderer      hud;
    private final EffectsRenderer  effects;
    private final IceBlockRenderer iceBlocks = new IceBlockRenderer();

    /** null اگر world.tmx بارگذاری نشده باشد */
    private final com.pvz2.map.GameMap pvzMap;

    /** همیشه غیر-null — اگر نقشه بارگذاری نشده باشد، یک ناحیه ثابت معادل کل صفحه است */
    private final CameraRegionManager cameraManager;

    // ─── لرزش دوربین (جایگزین EntityRenderer.triggerShake که با AnimationSystem حذف شد) ──
    private float shakeTimer = 0f;
    private float shakeMagnitude = 0f;

    /**
     * @param chapter نام فصل (برای رنگ‌های fallback در GridRenderer)
     * @param level   مرحله جاری (از {@code GameFacade.get().getCurrentLevel()}) —
     *                برای انتخاب لایه‌های صحیح نقشه (فصل/مینی‌گیم/special) و seed
     *                تصادفی‌سازی کاشی‌ها؛ می‌تواند null باشد (مثلاً حالت تست)
     */
    public GameRenderer(String chapter, Level level) {
        batch    = new SpriteBatch();
        pvzMap   = MapService.tryLoad(level);

        // بخش سوم: مدیریت کاراکترها (گیاه/زامبی/پرتابه/خورشید) با انیمیشن واقعی PAM
        // (یا مستطیل رنگی fallback اگر -Dpvz.assets ست نشده باشد).
        // این باید قبل از GridRenderer ساخته شود چون configLoader آن
        // (شامل environment_animations.json) به GridRenderer پاس داده
        // می‌شود تا سنگ‌قبر/مورر/گردباد/مه هم با انیمیشن واقعی رندر شوند.
        animSystem = new AnimationSystem(
                GameAssets.getInstance().getPamPlayer(),
                ServiceLocator.getInstance().getGameController());

        grid     = new GridRenderer(chapter, pvzMap,
                animSystem.getConfigLoader(), animSystem.getPamPlayer());
        boss     = new BossRenderer(animSystem.getPamPlayer());
        hud      = new HudRenderer();
        effects  = new EffectsRenderer();

        cameraManager = pvzMap != null
                ? pvzMap.getCameraManager()
                : new CameraRegionManager(
                new Rectangle(0, 0, GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT),
                null);
    }

    /**
     * رندر کامل یک فریم.
     *
     * @param snap       اسنپ‌شات وضعیت بازی (برای HUD/overlay های فاز۱ — از GameFacade)
     * @param session    جلسه خام بازی (برای AnimationSystem — نگاه کنید
     *                   {@code GameFacade.getCurrentSession()}؛ می‌تواند null باشد،
     *                   مثلاً یک لحظه بعد از پایان بازی)
     * @param delta      زمان از آخرین فریم (ثانیه)
     * @param camera     دوربین صحنه — هم برای batch (combined) و هم برای رندر
     *                   نقشه Tiled (setView) استفاده می‌شود
     * @param speedIndex شاخص سرعت (0=1×, 1=1.5×, 2=2×)
     */
    public void render(GameStateSnapshot snap, GameSession session, float delta,
                       OrthographicCamera camera, int speedIndex) {
        if (session != null) animSystem.update(delta, session);
        effects.update(delta);
        if (shakeTimer > 0) shakeTimer = Math.max(0, shakeTimer - delta);

        // نقشه Tiled با batch داخلی خودش رندر می‌شود — باید کاملاً جدا از
        // batch اصلی (قبل از begin آن) انجام شود.
        grid.renderTiledBackground(camera);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        grid.renderOverlays(batch, snap, GameConfig.showGrid, 0);
        grid.drawScorchedTilesBackground(batch); // زمینِ سوخته (زیرِ موجودیت‌ها)
        if (session != null) animSystem.render(batch, session, delta);
        iceBlocks.render(batch, snap);   // بلاکِ یخِ شفاف روی موجودیت‌هایِ یخ‌زده
        grid.drawTornadoesForeground(batch); // گردباد جلویِ زامبی که آن را می‌اندازد
        grid.drawScreenEffectsForeground(batch); // بادِ یخیِ IceShroom و ...
        grid.drawOctopusProjectiles(batch);      // پرتابه‌ی اختاپوس در حالِ پرواز
        grid.drawBarrelsForeground(batch, snap); // دبه‌ی Barrel Roller (rol/die) جلوی زامبی
        grid.drawPianosForeground(batch, snap);  // پیانوِ Pianist (play/damage/play2/die) جلوی زامبی
        grid.drawLasersForeground(batch);        // پرتوی لیزرِ تورکوایز
        grid.drawExplosionsForeground(batch);    // انفجارِ گیاهانِ انفجاری (روی موجودیت‌ها)
        boss.render(batch, snap);
        effects.render(batch);
        hud.render(batch, snap, speedIndex);

        batch.end();
    }

    /**
     * دوربین را طبق ناحیه فعال (FIXED یا EXTENDED — برای مراحل نوار نقاله/ریل)
     * به‌روزرسانی می‌کند. باید هر فریم، قبل از {@code viewport.apply()} صدا زده شود.
     */
    public CameraRegionManager getCameraManager() { return cameraManager; }

    /** برای دسترسی مستقیم به zone های نقشه (GridZone، ZombieSpawnZone، ...) — null اگر بارگذاری نشده */
    public com.pvz2.map.GameMap getPvzMap() { return pvzMap; }

    // ─── جلوه‌های خارجی (توسط GameScreen فراخوانی می‌شوند) ──────────────────

    public void triggerExplosion(float cx, float cy, float radius) {
        grid.spawnExplosionPam(cx, cy);   // افکتِ انفجارِ PAM (Cherry Bomb و ...)
        triggerShake(5f, 0.3f);
        com.pvz2.graphics.audio.SoundManager.get()
                .playSfx(com.pvz2.graphics.audio.SoundManager.SFX_EXPLOSION);
    }

    public void triggerHitFlash(float cx, float cy, float w, float h) {
        effects.addHitFlash(cx, cy, w, h);
    }

    public void triggerProjectileHit(float cx, float cy, String projectileType) {
        effects.addProjectileHit(cx, cy, projectileType);
    }

    public void triggerPlantFoodAura(float cx, float cy) {
        effects.addPlantFoodAura(cx, cy, 2.5f);
    }

    /** ردیفِ زیرِ اشاره‌گر برای روشن‌سازی در مرحله‌ی من‌زامبی (۱-based؛ ۰=هیچ). */
    public void setHighlightRow(int row1based) { grid.setHighlightRow(row1based); }
    public void setCursorHighlight(int col1based, int row1based) {
        grid.setCursorHighlight(col1based, row1based);
    }
    /** یک گردبادِ جدید در خانه‌ی (col,row) ایجاد می‌کند (مصر، موجِ نهایی). */
    public void spawnTornado(int col, int row) { grid.spawnTornado(col, row); }
    /** یک افکتِ تمام‌صفحه (مثلِ "iceshroom") را پخش می‌کند. */
    public void spawnScreenEffect(String name) { grid.spawnScreenEffect(name); }
    /** انیمیشنِ زمینِ سوخته را در خانه‌ی (col,row) شروع می‌کند (۱-based). */
    public void spawnScorchedTile(int col, int row) { grid.spawnScorchedTile(col, row); }
    /** یک پرتابه‌ی اختاپوس از src به tgt (۱-based col/row) پرتاب می‌کند. */
    public void spawnOctopusProjectile(double sc, double sr, double tc, double tr) {
        grid.spawnOctopusProjectile(sc, sr, tc, tr);
    }
    /** پرتوی لیزرِ تورکوایز در ردیفِ row از nearCol تا farCol (۱-based). */
    public void spawnLaser(int row, int nearCol, int farCol) {
        grid.spawnLaser(row, nearCol, farCol);
    }
    public void setBeghouledSelection(int col1based, int row1based) {
        grid.setBeghouledSelection(col1based, row1based);
    }

    /** موقعیتِ نشانگرِ موس (مختصاتِ world) — برای درخششِ hover خورشیدها. */
    public void setCursorWorld(float x, float y) { animSystem.setCursorWorld(x, y); }

    public void triggerBigImpact(float cx, float cy) {
        effects.addExplosion(cx, cy, 80f);
        triggerShake(9f, 0.45f);
    }

    public void showTextPopup(float cx, float cy, String text,
                              com.badlogic.gdx.graphics.Color color) {
        effects.addTextPopup(cx, cy, text, color);
    }

    private void triggerShake(float magnitude, float duration) {
        shakeMagnitude = magnitude;
        shakeTimer = duration;
    }

    /** لرزشِ دوربین بدونِ جلوه‌ی انفجار (مثلِ حرکتِ رئیس). */
    public void shake(float magnitude, float duration) {
        triggerShake(magnitude, duration);
    }

    /** آفست فعلی لرزش دوربین (برای اعمال روی موقعیت دوربین در GameScreen) */
    public float getShakeOffsetX() {
        return shakeTimer > 0 ? (float) (Math.random() - 0.5) * 2f * shakeMagnitude : 0f;
    }

    public float getShakeOffsetY() {
        return shakeTimer > 0 ? (float) (Math.random() - 0.5) * 2f * shakeMagnitude : 0f;
    }

    @Override
    public void dispose() {
        batch.dispose();
        grid.dispose();
        animSystem.dispose();
        if (pvzMap != null) pvzMap.dispose();
    }
}
