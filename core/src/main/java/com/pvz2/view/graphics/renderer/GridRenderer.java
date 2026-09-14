package com.pvz2.graphics.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.GameStateSnapshot;
import com.pvz2.graphics.GameStateSnapshot.TileType;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.graphics.util.GameCoords;
import com.pvz2.view.game.anim.config.AnimConfigLoader;
import com.pvz2.view.game.anim.config.EnvironmentAnimConfig;
import com.pvz2.view.game.anim.controller.PamDrawUtil;

/**
 * رندر پس‌زمینه، کاشی‌ها، شبکه و چمن‌زن‌ها.
 *
 * <p>پس‌زمینه به دو صورت ممکن است رندر شود:
 * <ul>
 *   <li><b>نقشه Tiled واقعی</b> — اگر {@code world.tmx} در {@code assets/maps/}
 *       بارگذاری شده باشد (از طریق {@link com.pvz2.map.MapService})، پس‌زمینه/گرید/
 *       جلوی صحنه هر فصل با {@link OrthogonalTiledMapRenderer} رندر می‌شود.</li>
 *   <li><b>Fallback رویه‌ای</b> — تا وقتی world.tmx طراحی نشده، مثل قبل با
 *       مستطیل‌های رنگی مخصوص هر فصل رندر می‌شود.</li>
 * </ul>
 * در هر دو حالت، overlay های پویای فاز ۱ (یخ‌زدگی، سنگ‌قبر، نکرومنسی، …) و
 * خطوط راهنمای گرید/چمن‌زن‌ها/مارکرهای مرحله ویژه روی آن رسم می‌شوند —
 * این‌ها وضعیت runtime هستند و در Tiled طراحی نمی‌شوند.
 *
 * <p><b>ویژگی جدید نسخه ۲:</b> سنگ‌قبر (tombstone/necromancy tile overlay)،
 * چمن‌زن‌ها، و گردباد شنی مصر اکنون در صورت وجود asset واقعی (از
 * {@code environment_animations.json} — با {@link PamDrawUtil}) به‌جای
 * فقط مستطیل رنگی، انیمیشن واقعی PAM نمایش می‌دهند؛ اگر asset در دسترس
 * نباشد، دقیقاً همان رفتار رویه‌ای رنگی قبلی حفظ می‌شود (هیچ‌وقت خالی نمی‌ماند).
 */
public class GridRenderer implements Disposable {

    private final String chapter;

    /** null اگر world.tmx هنوز بارگذاری نشده — در این حالت رندر رویه‌ای فعال است */
    private final com.pvz2.map.GameMap pvzMap;
    private final OrthogonalTiledMapRenderer tiledRenderer;

    /**
     * دوربین مخصوص رندر نقشه — کل نقشه (mapW×mapH) را می‌بیند و روی همان
     * ناحیه‌ی صفحه‌ای که FitViewport تعیین کرده کشیده می‌شود، پس نقشه دقیقاً
     * فضای مجازی ۱۲۸۰×۷۲۰ را پر می‌کند (به‌جای اینکه ۱:۱ برش بخورد). موقعیت
     * آبجکت‌های نقشه در {@link com.pvz2.map.MapLoader} با همین ضریب مقیاس می‌شوند،
     * پس گرید منطقی و موجودیت‌ها با پس‌زمینه هم‌تراز می‌مانند.
     */
    private final OrthographicCamera mapCamera;

    /** ═══ ویژگی جدید نسخه ۲ ═══ — null-safe: اگر null باشد، فقط fallback رنگی رندر می‌شود. */
    private final AnimConfigLoader configLoader;
    private final Object pamPlayer;

    /** state time جداگانه برای انیمیشن‌های محیطی (مورر/گردباد) — هر فریم افزایش می‌یابد. */
    private float envStateTime = 0f;
    /** state time جداگانه به‌ازای هر مورر فعال (کلید: شماره ردیف). */
    private final java.util.Map<Integer, Float> mowerStateTime = new java.util.HashMap<>();

    // ─── رنگ‌های tile overlay ─────────────────────────────────────────────────
    private static final Color COL_WATER      = new Color(0.1f, 0.4f, 0.8f, 0.65f);
    private static final Color COL_ICY        = new Color(0.65f, 0.88f, 1f,  0.55f);
    private static final Color COL_SLIPPERY   = new Color(0.55f, 0.82f, 1f,  0.45f);
    private static final Color COL_TOMBSTONE  = new Color(0.45f, 0.42f, 0.4f, 0.6f);
    private static final Color COL_NECROMANCY = new Color(0.4f, 0.15f, 0.45f, 0.5f);
    private static final Color COL_LOW_SHORE  = new Color(0.3f, 0.55f, 0.35f, 0.5f);
    private static final Color COL_GRID_LINE  = new Color(1f, 0f, 0f, 0.22f);
    /** قابِ قرمزِ پررنگِ حالتِ دیباگ دورِ هر کاشیِ نقشه. */
    private static final Color COL_DEBUG_TILE = new Color(1f, 0.12f, 0.12f, 0.95f);
    private static final Color COL_MOWER_ON   = new Color(0.45f, 0.9f, 0.2f, 1f);

    public GridRenderer(String chapter, com.pvz2.map.GameMap pvzMap) {
        this(chapter, pvzMap, null, null);
    }

    /**
     * ═══ سازنده جدید نسخه ۲ ═══
     * @param configLoader اختیاری (می‌تواند null باشد) — از
     *                      {@link com.pvz2.view.game.anim.AnimationSystem#getConfigLoader()}
     *                      گرفته می‌شود تا داده environment_animations.json
     *                      (سنگ‌قبر/مورر/گردباد/مه) در دسترس این کلاس باشد.
     *                      اگر null باشد، دقیقاً رفتار رویه‌ای رنگی قبلی حفظ می‌شود.
     * @param pamPlayer     همان نمونه‌ای که AnimationSystem استفاده می‌کند
     *                      (از {@link com.pvz2.graphics.assets.GameAssets#getPamPlayer()}).
     */
    public GridRenderer(String chapter, com.pvz2.map.GameMap pvzMap,
                        AnimConfigLoader configLoader, Object pamPlayer) {
        this.chapter = chapter;
        this.pvzMap  = pvzMap;
        this.configLoader = configLoader;
        this.pamPlayer    = pamPlayer;
        // OrthogonalTiledMapRenderer با batch داخلی خودش — batch اصلی GameRenderer را
        // دست نمی‌زند، بین begin/end آن هم صدا زده نمی‌شود (رندر مپ کاملاً جدا انجام می‌شود)
        this.tiledRenderer = pvzMap != null
                ? new OrthogonalTiledMapRenderer(pvzMap.getTiledMap())
                : null;

        if (pvzMap != null) {
            float mw = pvzMap.getMapWorldWidth();
            float mh = pvzMap.getMapWorldHeight();
            mapCamera = new OrthographicCamera();
            // viewport = کل نقشه؛ مبدأ پایین-چپ در (0,0)، مرکز در وسط نقشه
            mapCamera.setToOrtho(false, mw, mh);
            mapCamera.position.set(mw / 2f, mh / 2f, 0);
            mapCamera.update();
        } else {
            mapCamera = null;
        }
    }

    /** آیا نقشه Tiled واقعی بارگذاری شده؟ */
    public boolean hasTiledMap() { return tiledRenderer != null; }

    /**
     * رندر پس‌زمینه Tiled — باید جدا از batch اصلی و <b>قبل</b> از
     * {@code batch.begin()} صدا زده شود (OrthogonalTiledMapRenderer خودش
     * begin/end را روی batch داخلی مدیریت می‌کند).
     */
    public void renderTiledBackground(OrthographicCamera camera) {
        if (tiledRenderer == null) return;
        // از دوربین مخصوص نقشه استفاده کن (کل نقشه را می‌بیند) تا نقشه کل صفحه را
        // پر کند و ۱:۱ برش نخورد. اگر به هر دلیل mapCamera نبود، به دوربین بازی
        // برگرد (رفتار قبلی).
        tiledRenderer.setView(mapCamera != null ? mapCamera : camera);
        tiledRenderer.render();
    }

    /**
     * رندر تمام لایه‌های پویا (overlay کاشی، خطوط گرید، چمن‌زن‌ها، مارکرهای
     * مرحله ویژه) — باید داخل batch اصلی (بین begin/end) صدا زده شود.
     * اگر نقشه Tiled بارگذاری نشده باشد، پس‌زمینه رویه‌ای هم همین‌جا رسم می‌شود.
     */
    public void renderOverlays(Batch batch, GameStateSnapshot snap,
                               boolean showGrid, float stateTime) {
        // کالر (GameRenderer) فعلاً stateTime=0 پاس می‌دهد؛ پس ساعتِ داخلی را با
        // delta واقعی جلو می‌بریم تا انیمیشن‌ها (کوزه‌ها/گردباد/idle محیطی) زنده باشند.
        this.envStateTime += com.badlogic.gdx.Gdx.graphics.getRawDeltaTime();
        if (!hasTiledMap()) drawBackground(batch);
        // المان‌های فصل (سنگ‌قبر/یخ/آب/گردباد) در مینی‌گیم‌ها (به‌جز بازیِ امتیازی)
        // رسم نمی‌شوند — چون این مراحل نقشه‌ی اختصاصیِ خودشان را دارند.
        if (!seasonSuppressed(snap)) drawTileOverlays(batch, snap);
        if (showGrid) drawGridLines(batch);
        // حالتِ دیباگ: دورِ آبجکت‌های کاشیِ نقشه قابِ قرمز رسم می‌شود.
        if (com.pvz2.graphics.util.GameConfig.debugMode) drawDebugTileOutlines(batch);
        drawIZombieRowHighlight(batch, snap);
        drawCursorCellHighlight(batch);
        drawLawnMowers(batch, snap);
        drawVases(batch, snap);
        drawBowlingBalls(batch, snap);
        drawBowlingBoundary(batch, snap);
        drawIZombie(batch, snap);
        drawBeghouled(batch, snap);
        drawSpecialLevelMarkers(batch, snap);
    }

    /** ردیفی که اشاره‌گر موس روی آن است (۱-based؛ ۰=هیچ) — برای من‌زامبی. */
    private int highlightRow = 0;
    public void setHighlightRow(int row1based) { this.highlightRow = row1based; }

    /** خانه‌ی زیرِ اشاره‌گر برای هایلایتِ سفید (۱-based؛ ۰=هیچ) — کاشت/بیل/غذای گیاه. */
    private int cursorHiCol = 0, cursorHiRow = 0;
    public void setCursorHighlight(int col1based, int row1based) {
        this.cursorHiCol = col1based; this.cursorHiRow = row1based;
    }
    private static final Color COL_CURSOR_HILITE = new Color(1f, 1f, 1f, 0.30f);

    /** روشن‌سازیِ سفیدِ خانه‌ی زیرِ اشاره‌گر (کاشت/بیلچه/غذای گیاه). */
    private void drawCursorCellHighlight(Batch batch) {
        if (cursorHiCol < 1 || cursorHiCol > GameConstants.TILE_COLS
                || cursorHiRow < 1 || cursorHiRow > GameConstants.TILE_ROWS) return;
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        float tx = GameCoords.tileLeft(cursorHiCol);
        float ty = GameCoords.tileBottom(cursorHiRow);
        batch.setColor(COL_CURSOR_HILITE);
        batch.draw(white, tx, ty, GameConstants.TW, GameConstants.TH);
        batch.setColor(Color.WHITE);
    }

    // ─── Beghouled: انتخاب + حفره‌ها ─────────────────────────────────────────────
    private int begSelCol = 0, begSelRow = 0;  // ۱-based؛ ۰=هیچ
    public void setBeghouledSelection(int col1based, int row1based) {
        this.begSelCol = col1based; this.begSelRow = row1based;
    }
    private static final Color COL_CRATER   = new Color(0.28f, 0.20f, 0.12f, 0.9f);
    private static final Color COL_BEG_SEL   = new Color(1f, 0.95f, 0.3f, 1f);

    /** حفره‌ها (crater = خاکِ خالی) + هایلایتِ خانه‌ی انتخاب‌شده — فقط BEGHOULED. */
    private void drawBeghouled(Batch batch, GameStateSnapshot snap) {
        if (!snap.beghouledActive) return;
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        float tw = GameConstants.TW, th = GameConstants.TH;
        // حفره‌ها: لکه‌ی خاکیِ تیره روی خانه
        if (snap.craters != null) {
            for (int c = 0; c < GameConstants.TILE_COLS; c++) {
                for (int r = 0; r < GameConstants.TILE_ROWS; r++) {
                    if (!snap.craters[c][r]) continue;
                    float tx = GameCoords.tileLeft(c + 1);
                    float ty = GameCoords.tileBottom(r + 1);
                    batch.setColor(COL_CRATER);
                    batch.draw(white, tx + tw * 0.12f, ty + th * 0.12f, tw * 0.76f, th * 0.76f);
                    batch.setColor(0f, 0f, 0f, 0.35f);
                    batch.draw(white, tx + tw * 0.28f, ty + th * 0.28f, tw * 0.44f, th * 0.44f);
                }
            }
        }
        // هایلایتِ خانه‌ی انتخاب‌شده (قابِ نبضی)
        if (begSelCol >= 1 && begSelRow >= 1) {
            float tx = GameCoords.tileLeft(begSelCol);
            float ty = GameCoords.tileBottom(begSelRow);
            float a = 0.55f + 0.35f * (float) Math.sin(envStateTime * 6f);
            batch.setColor(COL_BEG_SEL.r, COL_BEG_SEL.g, COL_BEG_SEL.b, a);
            float t = 3f;
            batch.draw(white, tx, ty, tw, t);
            batch.draw(white, tx, ty + th - t, tw, t);
            batch.draw(white, tx, ty, t, th);
            batch.draw(white, tx + tw - t, ty, t, th);
        }
        batch.setColor(Color.WHITE);
    }

    // ─── مینی‌گیم من زامبی ──────────────────────────────────────────────────────

    private static final Color COL_BRAIN     = new Color(0.95f, 0.55f, 0.72f, 1f);
    private static final Color COL_SUN_PROD   = new Color(1f, 0.85f, 0.2f, 1f);
    private static final Color COL_ROW_HILITE = new Color(1f, 1f, 1f, 0.16f);

    /** روشن‌کردنِ ردیفِ زیرِ اشاره‌گر (EC3) — فقط وقتی من‌زامبی و در حال کاشت. */
    private void drawIZombieRowHighlight(Batch batch, GameStateSnapshot snap) {
        if (highlightRow < 1 || highlightRow > GameConstants.TILE_ROWS) return;
        if (snap.levelType == null
                || snap.levelType != com.pvz2.model.enums.LevelType.I_ZOMBIE) return;
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        float ty = GameCoords.tileBottom(highlightRow);
        batch.setColor(COL_ROW_HILITE);
        batch.draw(white, GameConstants.G_X, ty,
                GameConstants.TILE_COLS * GameConstants.TW, GameConstants.TH);
        batch.setColor(Color.WHITE);
    }

    // ─── منابعِ رندرِ من‌زامبی ────────────────────────────────────────────────
    private static final String IZOMBIE_BRAIN_PNG = "minigame/izombie/timer_deco_bigbrainz.png";
    // زامبیِ تولیدکننده‌ی خورشید از انیمیشنِ زامبیِ معمولی استفاده می‌کند (درخشان).
    private static final String SUN_PRODUCER_PAM =
            "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_BASIC/ZOMBIE_EGYPT_BASIC.PAM";
    private static final Color COL_SUN_GLOW = new Color(1f, 0.92f, 0.35f, 1f);
    private TextureRegion brainTex;
    private boolean brainTexTried;

    private TextureRegion brainTexture() {
        if (!brainTexTried) {
            brainTexTried = true;
            try { brainTex = GameAssets.getInstance().local(IZOMBIE_BRAIN_PNG); }
            catch (Exception ignored) { brainTex = null; }
        }
        return brainTex;
    }

    /**
     * مغزِ انتهای هر ردیف (سمت چپ، به‌جای چمن‌زن) + زامبیِ تولیدکننده‌ی خورشیدِ
     * درخشانِ هر ردیف (سمت راست) — فقط I_ZOMBIE.
     */
    private void drawIZombie(Batch batch, GameStateSnapshot snap) {
        if (snap.levelType == null
                || snap.levelType != com.pvz2.model.enums.LevelType.I_ZOMBIE) return;
        float gw = GameConstants.TILE_COLS * GameConstants.TW;
        float th = GameConstants.TH;
        TextureRegion brain = brainTexture();

        for (int r = 0; r < GameConstants.TILE_ROWS; r++) {
            float rowCy = GameCoords.tileBottom(r + 1) + th * 0.5f;

            // ─── مغز (سمت چپ، جای چمن‌زن) — اگر هنوز خورده نشده ──────────────
            if (snap.izombieBrains != null && r < snap.izombieBrains.length
                    && snap.izombieBrains[r]) {
                drawBrain(batch, brain, GameConstants.G_X - th * 0.55f, rowCy, th * 1.15f);
            }

            // ─── زامبیِ تولیدکننده‌ی خورشیدِ درخشان (سمت راست هر ردیف) ────────
            float px = GameConstants.G_X + gw + GameConstants.TW * 0.35f;
            drawGlowingSunProducer(batch, px, GameCoords.tileBottom(r + 1));
        }
        batch.setColor(Color.WHITE);
    }

    private void drawBrain(Batch batch, TextureRegion brain, float cx, float cy, float targetH) {
        if (brain == null) {   // fallback: مغزِ رویه‌ای
            TextureRegion white = GameAssets.getInstance().getWhiteRegion();
            batch.setColor(COL_BRAIN);
            batch.draw(white, cx - targetH * 0.35f, cy - targetH * 0.3f, targetH * 0.7f, targetH * 0.6f);
            batch.setColor(Color.WHITE);
            return;
        }
        float aspect = brain.getRegionWidth() / (float) brain.getRegionHeight();
        float h = targetH, w = h * aspect;
        batch.setColor(Color.WHITE);
        batch.draw(brain, cx - w / 2f, cy - h / 2f, w, h);
    }

    /** زامبیِ معمولیِ ساکن + هاله‌ی نورانی که تولیدِ خورشید را نشان می‌دهد. */
    private void drawGlowingSunProducer(Batch batch, float cx, float bottomY) {
        float th = GameConstants.TH;
        float pulse = 0.55f + 0.25f * (float) Math.sin(envStateTime * 3f);
        // ۱) هاله‌ی نورانیِ پشت (disc با ترکیبِ افزایشی)
        TextureRegion disc = GameAssets.getInstance().getDiscRegion();
        if (disc != null) {
            batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                    com.badlogic.gdx.graphics.GL20.GL_ONE);
            batch.setColor(COL_SUN_GLOW.r, COL_SUN_GLOW.g, COL_SUN_GLOW.b, pulse * 0.7f);
            float g = th * 2.0f;
            batch.draw(disc, cx - g / 2f, bottomY + th * 0.2f - g / 2f + th * 0.5f, g, g);
            batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                    com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
        // ۲) بدنه‌ی زامبیِ معمولی
        batch.setColor(Color.WHITE);
        float bodyH = th * 1.7f;
        float by = bottomY - th * 0.15f;
        PamDrawUtil.draw(pamPlayer, batch, SUN_PRODUCER_PAM, "idle", envStateTime,
                cx, by, true, GameConstants.TW * 0.9f, bodyH, COL_SUN_PROD);
        // ۳) درخششِ افزایشیِ روی خودِ بدنه (نورانی)
        batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE);
        batch.setColor(COL_SUN_GLOW.r, COL_SUN_GLOW.g, COL_SUN_GLOW.b, pulse * 0.35f);
        PamDrawUtil.draw(pamPlayer, batch, SUN_PRODUCER_PAM, "idle", envStateTime,
                cx, by, true, GameConstants.TW * 0.9f, bodyH, COL_SUN_GLOW);
        batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);
    }

    // ─── مینی‌گیم بولینگ گردویی ────────────────────────────────────────────────

    private static final Color COL_NUT_NORMAL  = new Color(0.60f, 0.40f, 0.20f, 1f);
    private static final Color COL_NUT_BIG      = new Color(0.45f, 0.30f, 0.14f, 1f);
    private static final Color COL_NUT_EXPLODE  = new Color(0.85f, 0.16f, 0.10f, 1f);

    // گردوهای بولینگ همگی از انیمیشنِ گردوی معمولی (idle) استفاده می‌کنند و مثلِ
    // چرخ می‌غلتند (طبق درخواست: «برای همه‌ی انواع از همان انیمیشنِ گردوی معمولی»).
    private static final String WALLNUT_PAM = "768/INITIAL/PLANT/WALLNUT/WALLNUT.PAM";
    private static final float NUT_TARGET_MUL   = 1.6f;   // ارتفاعِ هدف = TH*این (قابل تنظیم)
    private static final float NUT_BIG_MUL       = 2.1f;   // گردوی بزرگ
    private static final float NUT_MAX_SCALE     = 6f;     // اجازه‌ی بزرگ‌نمایی
    private static final float NUT_ROLL_DEG_COL  = 150f;   // درجه چرخش به ازای هر ستونِ حرکت
    private static final float NUT_PIVOT_FRAC    = 0.5f;   // مرکزِ چرخش نسبت به ارتفاع

    /** رسم گردوهای بولینگ با PAMِ واقعیِ گردو + چرخشِ چرخ‌مانند. فقط WALLNUT_BOWLING. */
    private void drawBowlingBalls(Batch batch, GameStateSnapshot snap) {
        if (snap.bowlingBalls == null || snap.bowlingBalls.isEmpty()) return;
        for (GameStateSnapshot.BowlingBallInfo b : snap.bowlingBalls) {
            boolean big = "big_wallnut".equals(b.type);
            float cx = GameCoords.toScreenX((float) b.x);
            float cy = GameCoords.toScreenY(b.y);
            float targetH = GameConstants.TH * (big ? NUT_BIG_MUL : NUT_TARGET_MUL);
            float half = targetH * NUT_PIVOT_FRAC;
            // anchor (پایینِ گردو) زیرِ مرکزِ خانه؛ مرکزِ چرخش دقیقاً روی مرکزِ خانه.
            float anchorY = cy - half;
            // با حرکت به راست، ساعت‌گرد بغلت (زاویه‌ی منفی)
            float rot = -(float) b.x * NUT_ROLL_DEG_COL;
            PamDrawUtil.drawRotated(pamPlayer, batch, WALLNUT_PAM, "idle",
                    envStateTime, cx, anchorY, true, rot, half,
                    GameConstants.TW * 0.6f, targetH, nutColor(b.type), NUT_MAX_SCALE);
        }
        batch.setColor(Color.WHITE);
    }

    private Color nutColor(String type) {
        if ("explode_o_nut_bowling".equals(type)) return COL_NUT_EXPLODE;
        if ("big_wallnut".equals(type))           return COL_NUT_BIG;
        return COL_NUT_NORMAL;
    }

    // ─── مینی‌گیم کوزه‌شکنی ────────────────────────────────────────────────────

    private static final Color COL_VASE_PLANT      = new Color(0.28f, 0.72f, 0.30f, 1f);
    private static final Color COL_VASE_GARGANTUAR = new Color(0.55f, 0.24f, 0.70f, 1f);
    private static final Color COL_VASE_RANDOM     = new Color(0.93f, 0.55f, 0.14f, 1f);

    // مسیرهای PAM کوزه‌های واقعی (نسبت به IMAGES/). هر PAM سه کلیپ دارد:
    //   drop  = افتادن از آسمان و جاگرفتن (شروع مرحله)
    //   idle  = کوزه‌ی سالمِ ساکن
    //   break = انیمیشن شکستن با کلیک بازیکن
    private static final String VASE_PAM_GREEN      = "768/FULL/VASEBREAKER/VASE_GREEN/VASE_GREEN.PAM";
    private static final String VASE_PAM_GARGANTUAR = "768/FULL/VASEBREAKER/VASE_GARGANTUAR/VASE_GARGANTUAR.PAM";
    private static final String VASE_PAM_BROWN      = "768/FULL/VASEBREAKER/VASE_BROWN/VASE_BROWN.PAM";

    // مدت‌زمانِ پیش‌فرض کلیپ‌ها (ثانیه) اگر PAM هنوز bake نشده باشد.
    private static final float VASE_DROP_FALLBACK  = 0.9f;
    private static final float VASE_BREAK_FALLBACK = 0.7f;

    // بزرگ‌نماییِ کوزه — canvasِ PAM بلند است (فضای افتادن از آسمان) پس کوزه‌ی واقعی
    // کسرِ کوچکی از canvas است؛ برای دیده‌شدنِ درست باید هدفِ ارتفاع را چند برابر کرد.
    private static final float VASE_TARGET_MUL = 9f;    // ضریبِ اندازه (تنظیم‌شده با بازخوردِ کاربر)
    private static final float VASE_MAX_SCALE  = 40f;   // سقفِ بزرگ‌نمایی برداشته شود

    /** حالتِ انیمیشنِ یک کوزه — در لایه‌ی رندر نگه‌داری می‌شود (View-only). */
    private static final class VaseAnim {
        final String kind;
        final float  dropStart;   // زمانِ مطلق (envStateTime) شروعِ افتادن
        boolean seen;             // این فریم در snapshot دیده شد؟
        boolean breaking;         // وارد فازِ شکستن شده؟
        float   breakStart;       // زمانِ مطلقِ شروعِ شکستن
        VaseAnim(String kind, float dropStart) { this.kind = kind; this.dropStart = dropStart; }
    }

    /** حالتِ انیمیشن هر کوزه بر اساس کلید col,row. */
    private final java.util.Map<Long, VaseAnim> vaseAnims = new java.util.HashMap<>();
    /** کشِ مدت‌زمان کلیپ‌ها (pam|clip → ثانیه). */
    private final java.util.Map<String, Float> clipDurCache = new java.util.HashMap<>();

    private static long vaseKey(int col, int row) { return ((long) col << 20) | (row & 0xFFFFF); }

    /**
     * رسم کوزه‌های واقعیِ کوزه‌شکنی با PAM. سه فازِ انیمیشن:
     * شروع مرحله همه با {@code drop} می‌افتند و جا می‌گیرند → {@code idle}؛
     * با کلیکِ بازیکن (حذف از snapshot) → {@code break} پخش و سپس محو می‌شود.
     * فقط در مرحله VASEBREAKER.
     */
    private void drawVases(Batch batch, GameStateSnapshot snap) {
        boolean active = snap.levelType == com.pvz2.model.enums.LevelType.VASEBREAKER
                && snap.vases != null;
        if (!active) {
            if (!vaseAnims.isEmpty()) vaseAnims.clear();
            return;
        }
        float now = envStateTime;

        // ۱) علامتِ «دیده‌نشده» به همه، سپس هماهنگ‌سازی با snapshot.
        for (VaseAnim a : vaseAnims.values()) a.seen = false;
        for (GameStateSnapshot.VaseInfo v : snap.vases) {
            long k = vaseKey(v.col, v.row);
            VaseAnim a = vaseAnims.get(k);
            if (a == null) {
                // استگرِ ملایم بر اساس موقعیت تا کوزه‌ها آبشاری بیفتند (نه یکجا).
                float stagger = (v.col * 0.06f) + (v.row * 0.04f);
                a = new VaseAnim(v.kind, now + stagger);
                vaseAnims.put(k, a);
            }
            a.seen = true;
        }
        // ۲) کوزه‌هایی که این فریم نبودند ولی هنوز نشکسته‌اند = تازه شکسته‌اند.
        for (VaseAnim a : vaseAnims.values()) {
            if (!a.seen && !a.breaking) {
                a.breaking = true;
                a.breakStart = now;
            }
        }
        // ۳) رندر + پاکسازیِ کوزه‌هایی که انیمیشنِ شکستن‌شان تمام شده.
        java.util.Iterator<java.util.Map.Entry<Long, VaseAnim>> it = vaseAnims.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<Long, VaseAnim> e = it.next();
            long k = e.getKey();
            VaseAnim a = e.getValue();
            int col = (int) (k >> 20);
            int row = (int) (k & 0xFFFFF);
            String pam = vasePam(a.kind);

            float cx = GameCoords.tileLeft(col) + GameConstants.TW / 2f;
            float by = GameCoords.tileBottom(row) + GameConstants.TH * 0.65f;
            float targetH = ("gargantuar".equals(a.kind)
                    ? GameConstants.TH * 1.55f : GameConstants.TH * 1.25f) * VASE_TARGET_MUL;
            Color fb = vaseColor(a.kind);

            String clip; float clipTime; boolean loop;
            if (a.breaking) {
                float bt = now - a.breakStart;
                float dur = clipDur(pam, "break", VASE_BREAK_FALLBACK);
                if (bt >= dur) { it.remove(); continue; }   // شکستن تمام شد → حذف
                clip = "break"; clipTime = bt; loop = false;
            } else {
                float dt = now - a.dropStart;
                if (dt < 0f) continue;                       // هنوز نوبتِ افتادنش نشده
                float dropDur = clipDur(pam, "drop", VASE_DROP_FALLBACK);
                if (dt < dropDur) { clip = "drop"; clipTime = dt; loop = false; }
                else { clip = "idle"; clipTime = dt - dropDur; loop = true; }
            }
            PamDrawUtil.draw(pamPlayer, batch, pam, clip, clipTime, cx, by, loop,
                    GameConstants.TW * 0.6f, targetH, fb, VASE_MAX_SCALE);
        }
        batch.setColor(Color.WHITE);
    }

    /** مدت‌زمان یک کلیپ از PAM (کش‌شده)؛ اگر هنوز bake نشده، مقدارِ پیش‌فرض. */
    private float clipDur(String pam, String clip, float fallback) {
        String key = pam + "|" + clip;
        Float cached = clipDurCache.get(key);
        if (cached != null) return cached;
        float dur = fallback;
        try {
            if (pamPlayer instanceof pvz.libpvz.pam.PamPlayer) {
                pvz.libpvz.pam.PamPlayer p = (pvz.libpvz.pam.PamPlayer) pamPlayer;
                if (p.getClip(pam, clip) != null) {          // فقط وقتی bake شده
                    float d = p.clipDurationSeconds(pam, clip);
                    if (d > 0.01f) { dur = d; clipDurCache.put(key, dur); }
                    return dur;                               // کش فقط وقتی معتبر است
                }
            }
        } catch (Exception ignored) { }
        return dur;   // هنوز bake نشده → پیش‌فرض، دفعه‌ی بعد دوباره تلاش
    }

    private String vasePam(String kind) {
        if ("plant".equals(kind))      return VASE_PAM_GREEN;
        if ("gargantuar".equals(kind)) return VASE_PAM_GARGANTUAR;
        return VASE_PAM_BROWN;   // random / پیش‌فرض = کوزه‌ی قهوه‌ایِ رمزآلود
    }

    private Color vaseColor(String kind) {
        if (kind == null) return COL_VASE_RANDOM;
        switch (kind) {
            case "plant":      return COL_VASE_PLANT;
            case "gargantuar": return COL_VASE_GARGANTUAR;
            default:           return COL_VASE_RANDOM;
        }
    }

    @Override
    public void dispose() {
        if (tiledRenderer != null) tiledRenderer.dispose();
        // توجه: pvzMap.dispose() اینجا صدا زده نمی‌شود — TiledMap توسط GameRenderer
        // (که مالک pvzMap است) آزاد می‌شود، تا مالکیت منابع دوپاره نشود.
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void drawBackground(Batch batch) {
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();

        // پس‌زمینه کامل صفحه
        // جستجو کنید: "BACKGROUND_{CHAPTER}" در asset browser
        // مثال: "BACKGROUND_EGYPT", "BACKGROUND_FROSTBITE", "BACKGROUND_BEACH", "BACKGROUND_DARK"
        // TextureRegion bg = GameAssets.getInstance().region("BACKGROUND_" + chapter.toUpperCase());
        // if (bg != whiteRegion) { batch.draw(bg, 0, 0, 1280, 720); return; }

        // Fallback: رنگ فصل
        batch.setColor(chapterBgColor());
        batch.draw(white, 0, 0, GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT);

        // رنگ تیره‌تر برای ناحیه grid
        batch.setColor(chapterGridColor());
        batch.draw(white, GameConstants.G_X, GameConstants.G_Y,
                GameConstants.TILE_COLS * GameConstants.TW,
                GameConstants.TILE_ROWS * GameConstants.TH);
        batch.setColor(Color.WHITE);
    }

    private void drawTileOverlays(Batch batch, GameStateSnapshot snap) {
        if (snap.tiles == null) return;
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();

        for (int c = 0; c < GameConstants.TILE_COLS; c++) {
            for (int r = 0; r < GameConstants.TILE_ROWS; r++) {
                TileType type = snap.tiles[c][r];
                Color col = tileOverlayColor(type);
                if (col == null) continue;

                // phase1: c+1, r+1
                float tx = GameCoords.tileLeft(c + 1);
                float ty = GameCoords.tileBottom(r + 1);

                // ═══ ویژگی جدید نسخه ۲ ═══
                // سنگ قبر (TOMBSTONE/DARK_TOMBSTONE): اگر environment_animations.json
                // بارگذاری شده، انیمیشن واقعی PAM رندر کن (وگرنه مستطیل رنگی fallback).
                if (isGravestoneType(type) && configLoader != null) {
                    // سنگ‌قبرهای جایزه‌دار (Dark Ages) PAMِ خاصِ خودشان را دارند.
                    int reward = (snap.tombstoneReward != null
                            && c < snap.tombstoneReward.length
                            && r < snap.tombstoneReward[c].length)
                            ? snap.tombstoneReward[c][r] : 0;
                    String key = reward == 1 ? "DARK_AGES_SUN"
                               : reward == 2 ? "DARK_AGES_PLANTFOOD"
                               : gravestoneVariantKey(type);
                    EnvironmentAnimConfig envCfg = configLoader.getGravestoneConfig(key);
                    if (envCfg != null && !envCfg.pamPath.isEmpty()) {
                        float centerX = tx + GameConstants.TW / 2f;
                        float centerY = ty + GameConstants.TH * 0.45f; // ~مرکزِ خانه
                        float dmg = (snap.tombstoneDamage != null
                                && c < snap.tombstoneDamage.length
                                && r < snap.tombstoneDamage[c].length)
                                ? snap.tombstoneDamage[c][r] : 0f;
                        String clip = gravestoneClipForDamage(dmg);
                        PamDrawUtil.draw(pamPlayer, batch, envCfg.pamPath, clip,
                                envStateTime, centerX, centerY, false, null,
                                GameConstants.TW * 1.1f, GameConstants.TH * 1.5f, col);
                        continue;
                    }
                }

                batch.setColor(col.r, col.g, col.b, col.a);
                batch.draw(white, tx, ty, GameConstants.TW, GameConstants.TH);
            }
        }
        batch.setColor(Color.WHITE);
    }

    /** خطِ قرمزِ مرزِ کاشتِ گردو در مینی‌گیمِ Wallnut Bowling (کاشت فقط ستون‌های ۱..۳). */
    private void drawBowlingBoundary(Batch batch, GameStateSnapshot snap) {
        if (snap.levelType != com.pvz2.model.enums.LevelType.WALLNUT_BOWLING) return;
        float lx = GameCoords.tileLeft(4); // لبه‌ی راستِ ستونِ ۳
        float y0 = GameConstants.G_Y;
        float y1 = y0 + GameConstants.TILE_ROWS * GameConstants.TH;
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        batch.setColor(1f, 0.15f, 0.15f, 0.9f);
        batch.draw(white, lx - 2f, y0, 4f, y1 - y0);
        batch.setColor(Color.WHITE);
    }

    private boolean isGravestoneType(TileType type) {
        return type == TileType.TOMBSTONE || type == TileType.DARK_TOMBSTONE;
    }

    /**
     * انتخابِ کلیپِ ترک‌خوردگیِ سنگ‌قبر بر اساسِ کسرِ آسیب (۰=سالم … ۱=نزدیکِ نابودی).
     * asset ها پنج مرحله دارند: undamaged + damage1..damage4.
     */
    private String gravestoneClipForDamage(float dmg) {
        if (dmg < 0.20f) return "undamaged";
        if (dmg < 0.40f) return "damage1";
        if (dmg < 0.60f) return "damage2";
        if (dmg < 0.80f) return "damage3";
        return "damage4";
    }

    /** انتخاب کلید سنگ‌قبر مناسب بر اساس فصل فعلی (برای gravestoneConfigs). */
    private String gravestoneVariantKey(TileType type) {
        if (type == TileType.DARK_TOMBSTONE) return "DARK_AGES_NORMAL";
        return "ANCIENT_EGYPT";
    }

    private void drawGridLines(Batch batch) {
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        batch.setColor(COL_GRID_LINE);
        float x0 = GameConstants.G_X;
        float y0 = GameConstants.G_Y;
        float gw = GameConstants.TILE_COLS * GameConstants.TW;
        float gh = GameConstants.TILE_ROWS * GameConstants.TH;

        for (int c = 0; c <= GameConstants.TILE_COLS; c++) {
            float lx = x0 + c * GameConstants.TW;
            batch.draw(white, lx, y0, 1.5f, gh);
        }
        for (int r = 0; r <= GameConstants.TILE_ROWS; r++) {
            float ly = y0 + r * GameConstants.TH;
            batch.draw(white, x0, ly, gw, 1.5f);
        }
        batch.setColor(Color.WHITE);
    }

    /**
     * حالتِ دیباگ: دورِ هر کاشیِ نقشه (سلولِ گرید) یک قابِ قرمزِ پررنگ رسم می‌کند.
     * گرید از روی همان آبجکت‌های PLANT_SLOT نقشه مشتق شده، پس قاب‌ها دقیقاً روی
     * کاشی‌های نقشه می‌نشینند.
     */
    private void drawDebugTileOutlines(Batch batch) {
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        batch.setColor(COL_DEBUG_TILE);
        float t = 2f;
        float tw = GameConstants.TW, th = GameConstants.TH;
        for (int c = 0; c < GameConstants.TILE_COLS; c++) {
            for (int r = 0; r < GameConstants.TILE_ROWS; r++) {
                float tx = GameConstants.G_X + c * tw;
                float ty = GameConstants.G_Y + r * th;
                batch.draw(white, tx, ty, tw, t);              // پایین
                batch.draw(white, tx, ty + th - t, tw, t);     // بالا
                batch.draw(white, tx, ty, t, th);              // چپ
                batch.draw(white, tx + tw - t, ty, t, th);     // راست
            }
        }
        batch.setColor(Color.WHITE);
    }

    private void drawLawnMowers(Batch batch, GameStateSnapshot snap) {
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        for (int r = 0; r < GameConstants.TILE_ROWS; r++) {
            if (!snap.lawnMowerActive[r]) continue;

            float mx = GameConstants.G_X - 46f;
            float my = GameCoords.tileBottom(r + 1) + GameConstants.TH * 0.2f;

            // ═══ ویژگی جدید نسخه ۲ ═══ — انیمیشن واقعی مورر (idle، در صورت
            // وجود PAM؛ حالت transition/attack زمانی که زامبی به مورر برسد
            // نیاز به وضعیت اضافی از فاز ۱ دارد که فعلاً در snapshot موجود
            // نیست — با idle ساده رندر می‌شود، fallback رنگی دقیقاً حفظ شده).
            if (configLoader != null) {
                EnvironmentAnimConfig envCfg = configLoader.getMowerConfig(chapterKeyForMower());
                if (envCfg != null && !envCfg.pamPath.isEmpty()) {
                    float st = mowerStateTime.merge(r, com.badlogic.gdx.Gdx.graphics.getDeltaTime(), Float::sum);
                    PamDrawUtil.draw(pamPlayer, batch, envCfg.pamPath, "idle", st,
                            mx + 19f, my, true, null, 38f, GameConstants.TH * 0.6f, COL_MOWER_ON);
                    continue;
                }
            }

            batch.setColor(COL_MOWER_ON);
            batch.draw(white, mx, my, 38f, GameConstants.TH * 0.6f);
        }

        // ── چمن‌زن‌های متحرک: از سرِ لاین حرکت می‌کنند با کلیپِ attack ──
        if (snap.movingMowers != null && !snap.movingMowers.isEmpty()) {
            EnvironmentAnimConfig envCfg = configLoader != null
                    ? configLoader.getMowerConfig(chapterKeyForMower()) : null;
            for (double[] m : snap.movingMowers) {
                int row = (int) m[0];
                float mx = GameCoords.toScreenX(m[1]);
                float my = GameCoords.tileBottom(row) + GameConstants.TH * 0.2f;
                if (envCfg != null && !envCfg.pamPath.isEmpty()) {
                    float st = mowerStateTime.merge(1000 + row,
                            com.badlogic.gdx.Gdx.graphics.getDeltaTime(), Float::sum);
                    PamDrawUtil.draw(pamPlayer, batch, envCfg.pamPath, "attack", st,
                            mx, my, true, null, 44f, GameConstants.TH * 0.7f, COL_MOWER_ON);
                } else {
                    batch.setColor(COL_MOWER_ON);
                    batch.draw(white, mx - 22f, my, 44f, GameConstants.TH * 0.7f);
                    batch.setColor(Color.WHITE);
                }
            }
        }
        batch.setColor(Color.WHITE);
    }

    /** نگاشت رشته فصل داخلی این کلاس به کلید ChapterType در environment_animations.json. */
    private String chapterKeyForMower() {
        if (chapter == null) return "ANCIENT_EGYPT";
        switch (chapter.toUpperCase()) {
            case "FROSTBITE_CAVES":
            case "FROSTBITE": return "FROSTBITE_CAVES";
            case "BIG_WAVE_BEACH":
            case "BEACH":     return "BIG_WAVE_BEACH";
            case "DARK_AGES":
            case "DARK":      return "DARK_AGES";
            default:          return "ANCIENT_EGYPT";
        }
    }

    /** مارکرهای مربوط به نوع مرحله (deadline، protected tiles، …) */
    private void drawSpecialLevelMarkers(Batch batch, GameStateSnapshot snap) {
        if (snap.levelType == null) return;
        switch (snap.levelType) {
            case DEAD_LINE:
                drawDeadlineLine(batch, snap.deadlineColumn);
                break;
            case SAVE_OUR_SEEDS:
                drawProtectedTileMarkers(batch, snap);
                break;
            case LOVE_YOUR_PLANTS:
                drawLovePlantsBorder(batch);
                break;
            default: break;
        }

        // المان‌های فصل (باد یخی/گردباد/مرزِ دریا) — در مینی‌گیم‌ها حذف می‌شوند.
        if (seasonSuppressed(snap)) return;

        // باد یخی (غار)
        if (!snap.frostbiteWindRows.isEmpty()) {
            drawFrostbiteWind(batch, snap.frostbiteWindRows);
        }

        // گردباد مصر
        if (snap.egyptTornadoActive) {
            drawEgyptTornado(batch);
        }

        // مرز آب دریا (ساحل موج بزرگ) — خطی ثابت که نشان می‌دهد آب تا کجا
        // می‌تواند بالا بیاید (مستقل از سطح فعلی دریا).
        drawSeaBoundary(batch, snap);
    }

    /**
     * آیا المان‌های فصل (سنگ‌قبر/یخ/ساحل/گردباد) باید حذف شوند؟ برای همه‌ی
     * مینی‌گیم‌ها به‌جز بازیِ امتیازی (SCORED)، چون این مراحل نقشه‌ی اختصاصیِ
     * خودشان را دارند و نباید عناصرِ فصل رویشان بیاید.
     */
    private boolean seasonSuppressed(GameStateSnapshot snap) {
        if (snap == null || snap.levelType == null) return false;
        switch (snap.levelType) {
            case VASEBREAKER:
            case WALLNUT_BOWLING:
            case I_ZOMBIE:
            case ZOMBOTANY:
            case BEGHOULED:
                return true;
            default:
                return false;   // SCORED و مراحلِ عادی عناصرِ فصل را نگه می‌دارند
        }
    }

    /**
     * خط مرزی سطح دریا در «ساحل موج بزرگ». چپ‌ترین ستونی که خانه‌ی آب/ساحلِ‌پست
     * دارد را پیدا می‌کند و در لبه‌ی چپِ آن یک خط‌چین عمودی فیروزه‌ای می‌کشد.
     */
    private void drawSeaBoundary(Batch batch, GameStateSnapshot snap) {
        if (snap.tiles == null) return;
        int minWaterCol = Integer.MAX_VALUE;
        for (int c = 0; c < GameConstants.TILE_COLS; c++) {
            for (int r = 0; r < GameConstants.TILE_ROWS; r++) {
                TileType t = snap.tiles[c][r];
                if (t == TileType.WATER || t == TileType.LOW_SHORE) {
                    if (c + 1 < minWaterCol) minWaterCol = c + 1;
                    break;
                }
            }
        }
        if (minWaterCol == Integer.MAX_VALUE) return;
        float lx = GameCoords.tileLeft(minWaterCol);
        float y0 = GameConstants.G_Y;
        float y1 = y0 + GameConstants.TILE_ROWS * GameConstants.TH;
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        batch.setColor(0.2f, 0.9f, 1f, 0.85f);
        for (float y = y0; y < y1; y += 18f) {
            batch.draw(white, lx - 1.5f, y, 3f, 10f);   // خط‌چین
        }
        batch.setColor(Color.WHITE);
    }

    private void drawDeadlineLine(Batch batch, int deadlineCol1Based) {
        if (deadlineCol1Based <= 0) return;
        float lx = GameCoords.tileLeft(deadlineCol1Based);
        batch.setColor(1f, 0.05f, 0.05f, 0.8f);
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        batch.draw(white, lx, GameConstants.G_Y,
                3f, GameConstants.TILE_ROWS * GameConstants.TH);
        batch.setColor(Color.WHITE);
    }

    /** PAMِ خانه‌ی محافظت‌شده (SAVE_OUR_SEEDS) — جایگزینِ کادرِ زردِ رویه‌ای. */
    private static final String PROTECT_TILE_PAM =
            "768/INITIAL/BACKGROUNDS/PROTECT_TILE/PROTECT_TILE.PAM";

    private void drawProtectedTileMarkers(Batch batch, GameStateSnapshot snap) {
        // خانه‌های محافظت‌شده‌ی واقعی («خانه خطر»): با PAMِ PROTECT_TILE مشخص می‌شوند.
        // اگر گیاهشان کشته شود، باخت آنی.
        if (snap.protectedTiles == null || snap.protectedTiles.isEmpty()) return;
        float tw = GameConstants.TW, th = GameConstants.TH;
        Color fallback = new Color(1f, 0.80f, 0.10f, 0.45f); // اگر PAM نبود: هاله‌ی طلایی
        Color saved = batch.getColor().cpy();
        batch.setColor(Color.WHITE);
        for (int[] pos : snap.protectedTiles) {
            float cx = GameCoords.tileLeft(pos[0]) + tw / 2f;
            float by = GameCoords.tileBottom(pos[1]);
            PamDrawUtil.draw(pamPlayer, batch, PROTECT_TILE_PAM, "animation", envStateTime,
                    cx, by, true, tw, th, fallback);
        }
        batch.setColor(saved);
    }

    private void drawLovePlantsBorder(Batch batch) {
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        batch.setColor(0.9f, 0.5f, 0.05f, 0.35f);
        float gx = GameConstants.G_X, gy = GameConstants.G_Y;
        float gw = GameConstants.TILE_COLS * GameConstants.TW;
        float gh = GameConstants.TILE_ROWS * GameConstants.TH;
        batch.draw(white, gx, gy, gw, 4);
        batch.draw(white, gx, gy + gh - 4, gw, 4);
        batch.draw(white, gx, gy, 4, gh);
        batch.draw(white, gx + gw - 4, gy, 4, gh);
        batch.setColor(Color.WHITE);
    }

    private void drawFrostbiteWind(Batch batch, java.util.List<Integer> rows) {
        // جستجو کنید: "IMAGE_FROSTBITE_WIND" در asset browser
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        for (int row : rows) {
            float ty = GameCoords.tileBottom(row);
            batch.setColor(0.7f, 0.92f, 1f, 0.4f);
            batch.draw(white, GameConstants.G_X, ty,
                    GameConstants.TILE_COLS * GameConstants.TW, GameConstants.TH);
        }
        batch.setColor(Color.WHITE);
    }

    // ─── گردبادِ ترتیب‌دار (intro→loop→outro) که زامبی را می‌اندازد ──────────────
    private static final float TOR_INTRO = 0.6f, TOR_LOOP = 1.4f, TOR_OUTRO = 0.7f;
    private static final class Tornado { int col, row; float t; }
    private final java.util.List<Tornado> tornadoes = new java.util.ArrayList<>();

    /** یک گردبادِ جدید در خانه‌ی (col,row) ایجاد می‌کند (۱-based). */
    public void spawnTornado(int col, int row) {
        Tornado tor = new Tornado();
        tor.col = col; tor.row = row; tor.t = 0f;
        tornadoes.add(tor);
    }

    /**
     * گردبادهایِ فعال را با توالیِ intro→loop→outro رندر می‌کند و پس از outro حذف
     * می‌کند. توسطِ {@code GameRenderer} <b>بعد از</b> رندرِ زامبی‌ها صدا زده می‌شود
     * تا گردباد جلویِ زامبی (که آن را می‌اندازد) دیده شود.
     */
    public void drawTornadoesForeground(Batch batch) {
        if (tornadoes.isEmpty() || configLoader == null) return;
        EnvironmentAnimConfig envCfg = configLoader.getMiscConfig("SANDSTORM_TOP");
        if (envCfg == null || envCfg.pamPath.isEmpty()) { tornadoes.clear(); return; }
        float dt = com.badlogic.gdx.Gdx.graphics.getRawDeltaTime();
        Color fallback = new Color(0.9f, 0.8f, 0.5f, 0.5f);
        java.util.Iterator<Tornado> it = tornadoes.iterator();
        while (it.hasNext()) {
            Tornado tor = it.next();
            tor.t += dt;
            String clip; float clipTime; boolean loop;
            if (tor.t < TOR_INTRO) {
                clip = "intro"; clipTime = tor.t; loop = false;
            } else if (tor.t < TOR_INTRO + TOR_LOOP) {
                clip = "loop"; clipTime = tor.t - TOR_INTRO; loop = true;
            } else if (tor.t < TOR_INTRO + TOR_LOOP + TOR_OUTRO) {
                clip = "outro"; clipTime = tor.t - TOR_INTRO - TOR_LOOP; loop = false;
            } else { it.remove(); continue; }
            float cx = GameCoords.tileLeft(tor.col) + GameConstants.TW / 2f;
            float by = GameCoords.tileBottom(tor.row);
            PamDrawUtil.draw(pamPlayer, batch, envCfg.pamPath, clip, clipTime,
                    cx, by, loop, null,
                    GameConstants.TW * 1.5f, GameConstants.TH * 2.3f, fallback);
        }
    }

    // ─── زمینِ سوخته (آتشِ اژدهای فصلِ تاریک) ───────────────────────────────────────
    //   توالی: animation (ورود) → animation2 (ماندگاری، loop) → animation3 (خروج).
    private static final String SCORCHED_PAM =
            "768/FULL/EFFECTS/SCORCHED_EARTH_TILE/SCORCHED_EARTH_TILE.PAM";
    private static final float SCORCH_ENTER = 0.6f, SCORCH_IDLE = 4.0f, SCORCH_EXIT = 0.7f;
    private static final class Scorch { int col, row; float t; }
    private final java.util.List<Scorch> scorches = new java.util.ArrayList<>();

    /** انیمیشنِ زمینِ سوخته را در خانه‌ی (col,row) آغاز می‌کند (۱-based). */
    public void spawnScorchedTile(int col, int row) {
        Scorch s = new Scorch();
        s.col = col; s.row = row; s.t = 0f;
        scorches.add(s);
    }

    /** خانه‌های سوخته‌ی فعال را زیرِ موجودیت‌ها رندر می‌کند و پس از خروج حذف می‌کند. */
    public void drawScorchedTilesBackground(Batch batch) {
        if (scorches.isEmpty()) return;
        float dt = com.badlogic.gdx.Gdx.graphics.getRawDeltaTime();
        Color fallback = new Color(0.15f, 0.10f, 0.08f, 0.55f);
        Color saved = batch.getColor().cpy();
        batch.setColor(Color.WHITE);
        java.util.Iterator<Scorch> it = scorches.iterator();
        while (it.hasNext()) {
            Scorch s = it.next();
            s.t += dt;
            String clip; float clipTime; boolean loop;
            if (s.t < SCORCH_ENTER) {
                clip = "animation"; clipTime = s.t; loop = false;
            } else if (s.t < SCORCH_ENTER + SCORCH_IDLE) {
                clip = "animation2"; clipTime = s.t - SCORCH_ENTER; loop = true;
            } else if (s.t < SCORCH_ENTER + SCORCH_IDLE + SCORCH_EXIT) {
                clip = "animation3"; clipTime = s.t - SCORCH_ENTER - SCORCH_IDLE; loop = false;
            } else { it.remove(); continue; }
            // مرکزِ کاشی (نیم‌کاشی بالاتر از کف تا سرِ جای درست بنشیند).
            float cx = GameCoords.tileLeft(s.col) + GameConstants.TW / 2f;
            float cy = GameCoords.tileBottom(s.row) + GameConstants.TH * 0.5f;
            // هم‌اندازه‌ی کاشی؛ چون scale بر اساسِ کلِ canvasِ افکت است (بزرگ‌تر از
            // خودِ لکه)، هدفِ ارتفاع سخاوتمندانه گرفته می‌شود + maxScale برای بزرگ‌نمایی.
            PamDrawUtil.draw(pamPlayer, batch, SCORCHED_PAM, clip, clipTime,
                    cx, cy, loop,
                    GameConstants.TW * 1.6f, GameConstants.TH * 1.6f, fallback, 3f);
        }
        batch.setColor(saved);
    }

    // ─── افکتِ انفجارِ گیاهانِ انفجاری (Cherry Bomb و ...) ──────────────────────────
    private static final String EXPLOSION_PAM =
            "768/FULL/EFFECTS/CHERRYBOMB_EXPLOSION_REAR/CHERRYBOMB_EXPLOSION_REAR.PAM";
    private static final float EXPLOSION_DUR = 0.9f;
    private static final class ExplosionFx { float x, y, t; }
    private final java.util.List<ExplosionFx> explosionFxs = new java.util.ArrayList<>();

    /** یک افکتِ انفجارِ PAM در نقطه‌ی صفحه‌ای (x,y) آغاز می‌کند. */
    public void spawnExplosionPam(float x, float y) {
        ExplosionFx fx = new ExplosionFx();
        fx.x = x; fx.y = y; fx.t = 0f;
        explosionFxs.add(fx);
    }

    /** انفجارهایِ فعال را (روی موجودیت‌ها) پخش و پس از پایان حذف می‌کند. */
    public void drawExplosionsForeground(Batch batch) {
        if (explosionFxs.isEmpty()) return;
        float dt = com.badlogic.gdx.Gdx.graphics.getRawDeltaTime();
        Color fallback = new Color(1f, 0.6f, 0.1f, 0.85f);
        Color saved = batch.getColor().cpy();
        batch.setColor(Color.WHITE);
        java.util.Iterator<ExplosionFx> it = explosionFxs.iterator();
        while (it.hasNext()) {
            ExplosionFx fx = it.next();
            fx.t += dt;
            if (fx.t >= EXPLOSION_DUR) { it.remove(); continue; }
            PamDrawUtil.draw(pamPlayer, batch, EXPLOSION_PAM, "explosion", fx.t,
                    fx.x, fx.y, false, null,
                    GameConstants.TW * 3f, GameConstants.TH * 3f, fallback);
        }
        batch.setColor(saved);
    }

    // ─── دبه‌ی Barrel Roller (rol=غلتیدن، die=شکستن) ────────────────────────────────
    private static final String BARREL_PAM =
            "768/FULL/ZOMBIE/ZOMBIE_PIRATE_BARREL_PUSHER_BARREL/ZOMBIE_PIRATE_BARREL_PUSHER_BARREL.PAM";
    private static final float BARREL_DIE_DUR = 0.7f;
    /** آخرین موقعیتِ صفحه‌ایِ دبه‌ی هر زامبی (کلید: netId) تا وقتی دبه سالم است. */
    private final java.util.Map<Integer, float[]> barrelPos = new java.util.HashMap<>();
    private float barrelRollTime = 0f;
    private static final class BarrelBreak { float x, y, t; }
    private final java.util.List<BarrelBreak> barrelBreaks = new java.util.ArrayList<>();

    /** دبه‌ی Barrel Roller ها را جلوی زامبی می‌کشد؛ لحظه‌ی شکستن، کلیپِ die را پخش می‌کند. */
    public void drawBarrelsForeground(Batch batch, GameStateSnapshot snap) {
        float dt = com.badlogic.gdx.Gdx.graphics.getRawDeltaTime();
        barrelRollTime += dt;
        Color fallback = new Color(0.55f, 0.36f, 0.18f, 0.9f);
        Color saved = batch.getColor().cpy();
        batch.setColor(Color.WHITE);

        java.util.Set<Integer> stillRolling = new java.util.HashSet<>();
        if (snap != null && snap.zombies != null) {
            for (GameStateSnapshot.ZombieInfo z : snap.zombies) {
                if (!"barrel_roller".equals(z.type)) continue;
                boolean hasBarrel = false;
                for (GameStateSnapshot.ZombieInfo.ArmorInfo a : z.armors)
                    if ("barrel".equals(a.type)) { hasBarrel = true; break; }
                if (!hasBarrel) continue;
                float cx = GameCoords.toScreenX(z.phase1X - 0.55); // جلوی زامبی (به سمتِ خانه)
                float by = GameCoords.tileBottom(z.phase1Y);
                stillRolling.add(z.netId);
                barrelPos.put(z.netId, new float[]{cx, by});
                PamDrawUtil.draw(pamPlayer, batch, BARREL_PAM, "rol", barrelRollTime,
                        cx, by, true, GameConstants.TW * 1.1f, GameConstants.TH, fallback);
            }
        }
        // دبه‌هایی که این فریم دیگر سالم نیستند = شکستند → کلیپِ die را یک‌بار پخش کن.
        java.util.Iterator<java.util.Map.Entry<Integer, float[]>> it = barrelPos.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<Integer, float[]> e = it.next();
            if (!stillRolling.contains(e.getKey())) {
                BarrelBreak b = new BarrelBreak();
                b.x = e.getValue()[0]; b.y = e.getValue()[1]; b.t = 0f;
                barrelBreaks.add(b);
                it.remove();
            }
        }
        // پخشِ انیمیشنِ شکستنِ دبه.
        java.util.Iterator<BarrelBreak> bit = barrelBreaks.iterator();
        while (bit.hasNext()) {
            BarrelBreak b = bit.next();
            b.t += dt;
            if (b.t >= BARREL_DIE_DUR) { bit.remove(); continue; }
            PamDrawUtil.draw(pamPlayer, batch, BARREL_PAM, "die", b.t,
                    b.x, b.y, false, GameConstants.TW * 1.1f, GameConstants.TH, fallback);
        }
        batch.setColor(saved);
    }

    // ─── پیانوِ Pianist (play/damage/play2/die) جلوی زامبی ───────────────────────
    private static final String PIANO_PAM = "768/FULL/ZOMBIE/PIANO/PIANO.PAM";
    private static final float PIANO_DIE_DUR    = 0.8f;
    private static final float PIANO_DAMAGE_DUR = 0.5f;
    private final java.util.Map<Integer, float[]> pianoPos = new java.util.HashMap<>();
    private final java.util.Map<Integer, Boolean> pianoHalf = new java.util.HashMap<>();
    private final java.util.Map<Integer, Float> pianoDamageT = new java.util.HashMap<>();
    private float pianoTime = 0f;
    private static final class PianoBreak { float x, y, t; }
    private final java.util.List<PianoBreak> pianoBreaks = new java.util.ArrayList<>();

    /**
     * پیانوِ زامبیِ پیانیست را دقیقاً روبه‌رویش می‌کشد. کلیپ بر اساسِ جانِ پیانو:
     * {@code play} (جانِ بالای نیمه)، {@code damage} (لحظه‌ی افتِ زیرِ نیمه)،
     * {@code play2} (نیمه‌ی دوم)، و {@code die} (لحظه‌ی نابودیِ کاملِ پیانو). پیانو
     * پیش از خودِ زامبی نابود می‌شود چون به‌صورتِ زره (ArmorType.PIANO) مدل شده.
     */
    public void drawPianosForeground(Batch batch, GameStateSnapshot snap) {
        float dt = com.badlogic.gdx.Gdx.graphics.getRawDeltaTime();
        pianoTime += dt;
        Color fallback = new Color(0.16f, 0.13f, 0.13f, 0.95f);
        Color saved = batch.getColor().cpy();
        batch.setColor(Color.WHITE);

        java.util.Set<Integer> alive = new java.util.HashSet<>();
        if (snap != null && snap.zombies != null) {
            for (GameStateSnapshot.ZombieInfo z : snap.zombies) {
                if (!"pianist_zombie".equals(z.type)) continue;
                float hp = -1f, maxHp = -1f;
                for (GameStateSnapshot.ZombieInfo.ArmorInfo a : z.armors)
                    if ("piano".equals(a.type)) { hp = a.hp; maxHp = a.maxHp; break; }
                if (hp <= 0f) continue;   // پیانو نابود شده → کلیپِ die جدا پخش می‌شود
                float cx = GameCoords.toScreenX(z.phase1X - 0.6); // دقیقاً روبه‌روی زامبی
                float by = GameCoords.tileBottom(z.phase1Y);
                alive.add(z.netId);
                pianoPos.put(z.netId, new float[]{cx, by});

                float frac = maxHp > 0 ? hp / maxHp : 1f;
                String clip;
                if (frac <= 0.5f) {
                    if (!Boolean.TRUE.equals(pianoHalf.get(z.netId))) {
                        pianoHalf.put(z.netId, true);
                        pianoDamageT.put(z.netId, 0f);   // شروعِ کلیپِ damage
                    }
                    Float dtmr = pianoDamageT.get(z.netId);
                    if (dtmr != null && dtmr < PIANO_DAMAGE_DUR) {
                        pianoDamageT.put(z.netId, dtmr + dt);
                        clip = "damage";
                    } else {
                        clip = "play2";
                    }
                } else {
                    clip = "play";
                }
                PamDrawUtil.draw(pamPlayer, batch, PIANO_PAM, clip, pianoTime,
                        cx, by, false, GameConstants.TW * 1.3f, GameConstants.TH * 1.1f, fallback);
            }
        }
        // پیانوهایی که این فریم دیگر سالم نیستند = نابود شدند → کلیپِ die یک‌بار.
        java.util.Iterator<java.util.Map.Entry<Integer, float[]>> it = pianoPos.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<Integer, float[]> e = it.next();
            if (!alive.contains(e.getKey())) {
                PianoBreak b = new PianoBreak();
                b.x = e.getValue()[0]; b.y = e.getValue()[1]; b.t = 0f;
                pianoBreaks.add(b);
                pianoHalf.remove(e.getKey());
                pianoDamageT.remove(e.getKey());
                it.remove();
            }
        }
        java.util.Iterator<PianoBreak> bit = pianoBreaks.iterator();
        while (bit.hasNext()) {
            PianoBreak b = bit.next();
            b.t += dt;
            if (b.t >= PIANO_DIE_DUR) { bit.remove(); continue; }
            PamDrawUtil.draw(pamPlayer, batch, PIANO_PAM, "die", b.t,
                    b.x, b.y, false, GameConstants.TW * 1.3f, GameConstants.TH * 1.1f, fallback);
        }
        batch.setColor(saved);
    }

    // ─── لیزرِ تورکوایز (پرتوی رویه‌ایِ محوشونده) ────────────────────────────────
    private static final float LASER_DUR = 0.45f;
    private static final class LaserFx { float x1, x2, y, t; }
    private final java.util.List<LaserFx> lasers = new java.util.ArrayList<>();

    /** یک پرتوی لیزر در ردیفِ row (۱-based) از nearCol تا farCol آغاز می‌کند. */
    public void spawnLaser(int row, int nearCol, int farCol) {
        int lo = Math.max(1, Math.min(nearCol, farCol));
        int hi = Math.max(nearCol, farCol);
        LaserFx fx = new LaserFx();
        fx.x1 = GameCoords.tileLeft(lo);
        fx.x2 = GameCoords.tileLeft(hi) + GameConstants.TW;
        fx.y  = GameCoords.toScreenY(row);
        fx.t  = 0f;
        lasers.add(fx);
    }

    /** پرتوهای لیزرِ فعال را (روی موجودیت‌ها) می‌کشد و پس از محو حذف می‌کند. */
    public void drawLasersForeground(Batch batch) {
        if (lasers.isEmpty()) return;
        float dt = com.badlogic.gdx.Gdx.graphics.getRawDeltaTime();
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        Color saved = batch.getColor().cpy();
        java.util.Iterator<LaserFx> it = lasers.iterator();
        while (it.hasNext()) {
            LaserFx fx = it.next();
            fx.t += dt;
            float p = fx.t / LASER_DUR;
            if (p >= 1f) { it.remove(); continue; }
            float alpha = 1f - p;                       // محوشدن
            float w = fx.x2 - fx.x1;
            float coreH = GameConstants.TH * 0.18f;
            float glowH = GameConstants.TH * 0.5f;
            // هاله‌ی بیرونیِ فیروزه‌ای
            batch.setColor(0.2f, 0.95f, 0.9f, 0.35f * alpha);
            batch.draw(white, fx.x1, fx.y - glowH / 2f, w, glowH);
            // هسته‌ی روشن
            batch.setColor(0.8f, 1f, 1f, 0.95f * alpha);
            batch.draw(white, fx.x1, fx.y - coreH / 2f, w, coreH);
        }
        batch.setColor(saved);
    }

    // ─── پرتابه‌ی اختاپوس (اختاپوس‌پرت‌کن، ساحل) ────────────────────────────────────
    private static final String OCTOPUS_PROJECTILE_PAM =
            "768/FULL/EFFECTS/ZOMBIE_OCTOPUS_PROJECTILE/ZOMBIE_OCTOPUS_PROJECTILE.PAM";
    private static final float OCTOPUS_FLIGHT = 0.75f; // مدتِ پروازِ اختاپوس (ثانیه)
    private static final float OCTOPUS_ARC    = 90f;    // اوجِ سهمیِ پرواز (پیکسل)
    private static final class OctopusShot { float sx, sy, tx, ty, t; }
    private final java.util.List<OctopusShot> octopusShots = new java.util.ArrayList<>();

    /** یک پرتابه‌ی اختاپوس از (srcCol,srcRow) به (tgtCol,tgtRow) پرتاب می‌کند (۱-based). */
    public void spawnOctopusProjectile(double srcCol, double srcRow,
                                        double tgtCol, double tgtRow) {
        OctopusShot o = new OctopusShot();
        o.sx = GameCoords.toScreenX(srcCol);
        o.sy = GameCoords.toScreenY((int) srcRow);
        o.tx = GameCoords.toScreenX(tgtCol);
        o.ty = GameCoords.toScreenY((int) tgtRow);
        o.t = 0f;
        octopusShots.add(o);
    }

    /** پرتابه‌های اختاپوسِ در حالِ پرواز را روی مسیرِ سهمی رندر می‌کند (foreground). */
    public void drawOctopusProjectiles(Batch batch) {
        if (octopusShots.isEmpty()) return;
        float dt = com.badlogic.gdx.Gdx.graphics.getRawDeltaTime();
        Color fallback = new Color(0.65f, 0.35f, 0.65f, 0.9f);
        java.util.Iterator<OctopusShot> it = octopusShots.iterator();
        while (it.hasNext()) {
            OctopusShot o = it.next();
            o.t += dt;
            float p = o.t / OCTOPUS_FLIGHT;
            if (p >= 1f) { it.remove(); continue; }
            float x = o.sx + (o.tx - o.sx) * p;
            float y = o.sy + (o.ty - o.sy) * p + OCTOPUS_ARC * 4f * p * (1f - p);
            PamDrawUtil.draw(pamPlayer, batch, OCTOPUS_PROJECTILE_PAM, "animation",
                    o.t, x, y, true, null,
                    GameConstants.TW * 0.9f, GameConstants.TH * 0.9f, fallback);
        }
    }

    // ─── افکت‌های تمام‌صفحه (بادِ یخیِ IceShroom و ...) ─────────────────────────────
    private static final class ScreenFx { String pam, clip; float t, dur; }
    private final java.util.List<ScreenFx> screenFxs = new java.util.ArrayList<>();

    /** یک افکتِ تمام‌صفحه با نامِ داده‌شده ایجاد می‌کند. */
    public void spawnScreenEffect(String name) {
        if ("iceshroom".equals(name)) {
            ScreenFx fx = new ScreenFx();
            fx.pam  = "768/FULL/EFFECTS/ICESHROOM_FX/ICESHROOM_FX.PAM";
            fx.clip = "animation";
            fx.t = 0f; fx.dur = 1.6f;
            screenFxs.add(fx);
        }
    }

    /** افکت‌های تمام‌صفحه‌ی فعال را روی کلِ شبکه یک‌بار پخش می‌کند (foreground). */
    public void drawScreenEffectsForeground(Batch batch) {
        if (screenFxs.isEmpty()) return;
        float dt = com.badlogic.gdx.Gdx.graphics.getRawDeltaTime();
        float gridW = GameConstants.TILE_COLS * GameConstants.TW;
        float gridH = GameConstants.TILE_ROWS * GameConstants.TH;
        float cx = GameConstants.G_X + gridW / 2f;
        float by = GameConstants.G_Y;
        Color fallback = new Color(0.7f, 0.9f, 1f, 0.35f);
        java.util.Iterator<ScreenFx> it = screenFxs.iterator();
        while (it.hasNext()) {
            ScreenFx fx = it.next();
            fx.t += dt;
            if (fx.t >= fx.dur) { it.remove(); continue; }
            PamDrawUtil.draw(pamPlayer, batch, fx.pam, fx.clip, fx.t,
                    cx, by, false, null, gridW, gridH, fallback);
        }
    }

    private void drawEgyptTornado(Batch batch) {
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        float rectX = GameConstants.G_X + GameConstants.TILE_COLS * GameConstants.TW - 60;
        float rectY = GameConstants.G_Y;
        float rectW = 60, rectH = GameConstants.TILE_ROWS * GameConstants.TH;
        Color fallback = new Color(0.9f, 0.8f, 0.5f, 0.45f);

        // ═══ ویژگی جدید نسخه ۲ ═══ — گردباد شنی واقعی مصر: دو لایه
        // (SANDSTORM_REAR پشت، SANDSTORM_TOP جلو). این متد فقط لایه TOP
        // (که در ترتیب رندر GridRenderer، جلوی گیاه/زامبی می‌آید) رندر
        // می‌کند. اگر PAM موجود نباشد، دقیقاً همان مستطیل رنگی قبلی حفظ می‌شود.
        if (configLoader != null) {
            EnvironmentAnimConfig envCfg = configLoader.getMiscConfig("SANDSTORM_TOP");
            if (envCfg != null && !envCfg.pamPath.isEmpty()) {
                PamDrawUtil.draw(pamPlayer, batch, envCfg.pamPath, "loop", envStateTime,
                        rectX + rectW / 2f, rectY, true, null, rectW, rectH, fallback);
                return;
            }
        }

        batch.setColor(fallback);
        batch.draw(white, rectX, rectY, rectW, rectH);
        batch.setColor(Color.WHITE);
    }

    // ─── Colors ──────────────────────────────────────────────────────────────

    private Color chapterBgColor() {
        switch (chapter != null ? chapter.toLowerCase() : "") {
            case "ancient_egypt": return new Color(0.78f, 0.68f, 0.44f, 1f);
            case "frostbite_caves": return new Color(0.55f, 0.72f, 0.88f, 1f);
            case "big_wave_beach": return new Color(0.42f, 0.68f, 0.82f, 1f);
            case "dark_ages":     return new Color(0.2f,  0.17f, 0.12f, 1f);
            default:              return new Color(0.3f,  0.48f, 0.22f, 1f);
        }
    }

    private Color chapterGridColor() {
        switch (chapter != null ? chapter.toLowerCase() : "") {
            case "ancient_egypt": return new Color(0.68f, 0.58f, 0.34f, 1f);
            case "frostbite_caves": return new Color(0.45f, 0.62f, 0.78f, 1f);
            case "big_wave_beach": return new Color(0.35f, 0.6f,  0.35f, 1f);
            case "dark_ages":     return new Color(0.15f, 0.12f, 0.08f, 1f);
            default:              return new Color(0.25f, 0.42f, 0.16f, 1f);
        }
    }

    private Color tileOverlayColor(TileType type) {
        if (type == null) return null;
        switch (type) {
            case WATER:        return COL_WATER;
            case ICY_GROUND:   return COL_ICY;
            case SLIPPERY_UP:
            case SLIPPERY_DOWN:return COL_SLIPPERY;
            case TOMBSTONE:
            case DARK_TOMBSTONE: return COL_TOMBSTONE;
            case NECROMANCY:   return COL_NECROMANCY;
            case LOW_SHORE:    return COL_LOW_SHORE;
            default:           return null;
        }
    }
}
