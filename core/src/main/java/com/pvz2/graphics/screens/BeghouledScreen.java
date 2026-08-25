package com.pvz2.graphics.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.pvz2.PVZApplication;
import com.pvz2.graphics.BaseScreen;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.ScreenId;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.util.RandomUtil;

/**
 * مینی‌گیم امتیازی «ترکیب سه‌تایی» (Beghouled) — یک بازی match-3 مستقل.
 *
 * <p>ویژگی‌های نمره‌دار (داک فاز ۲):
 * <ul>
 *   <li>GQ3 — جابه‌جایی با موس (مثل Candy Crush): انتخاب یک نگین و کلیک روی نگینِ مجاور.</li>
 *   <li>GR3 — نرم بودن حرکات: جابه‌جایی و سقوطِ نگین‌ها با درون‌یابی (lerp) پیوسته انجام می‌شود.</li>
 *   <li>GS3 — ترکیب‌های باقیمانده: شمارنده‌ی نگین‌هایی که باید پاک شوند تا بازی تمام شود.</li>
 *   <li>GT3 — گزینه‌ی ارتقای گیاهان: چند گزینه‌ی ارتقا با تصویر (رنگ) و هزینه.</li>
 * </ul>
 * این مینی‌گیم از موتور {@code GameSession} استفاده نمی‌کند (مکانیزمِ کاملاً متفاوت).
 */
public class BeghouledScreen extends BaseScreen {

    private static final int ROWS = 8;
    private static final int COLS = 8;
    private static final int GEMS = 6;
    private static final float CELL = 60f;
    private static final float SWAP_SPEED = 6f;   // واحد بر ثانیه (۱ = یک جابه‌جایی کامل)
    private static final float FALL_SPEED = 900f;  // پیکسل بر ثانیه

    /** رنگِ هر نوع نگین (نمایانگرِ یک گیاه). */
    private static final Color[] GEM_COLORS = {
        new Color(0.30f, 0.78f, 0.30f, 1f),  // Peashooter — سبز
        new Color(0.98f, 0.85f, 0.20f, 1f),  // Sunflower — زرد
        new Color(0.55f, 0.40f, 0.24f, 1f),  // Wall-nut — قهوه‌ای
        new Color(0.40f, 0.75f, 0.95f, 1f),  // Snow Pea — آبی
        new Color(0.90f, 0.25f, 0.25f, 1f),  // Cherry Bomb — قرمز
        new Color(0.70f, 0.45f, 0.85f, 1f),  // Cabbage-pult — بنفش
    };
    private static final String[] GEM_NAMES = {
        "Peashooter", "Sunflower", "Wall-nut", "Snow Pea", "Cherry Bomb", "Cabbage"
    };

    private enum State { IDLE, SWAP, FALL }

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private Stage hudStage;

    private final int[][] grid = new int[ROWS][COLS];
    /** آفستِ عمودیِ نگین بالای مقصد (برای انیمیشن سقوط) — پیکسل. */
    private final float[][] fallOffset = new float[ROWS][COLS];

    private float boardX, boardY;

    private State state = State.IDLE;
    private int selR = -1, selC = -1;
    // جابه‌جایی در حال انجام
    private int aR, aC, bR, bC;
    private float swapT;
    private boolean swapReverting;

    private int matchesRemaining = 40;  // GS3 — نگین‌های لازم تا پایان
    private long score;
    private boolean finished;

    private Label statusLabel;
    private Label scoreLabel;

    public BeghouledScreen(PVZApplication game) { super(game); }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void show() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new FitViewport(GameConstants.VIEWPORT_WIDTH,
                GameConstants.VIEWPORT_HEIGHT, camera);
        camera.position.set(GameConstants.VIEWPORT_WIDTH * 0.5f,
                GameConstants.VIEWPORT_HEIGHT * 0.5f, 0);
        camera.update();

        boardX = (GameConstants.VIEWPORT_WIDTH - COLS * CELL) * 0.5f - 90f;
        boardY = (GameConstants.VIEWPORT_HEIGHT - ROWS * CELL) * 0.5f;

        initBoard();
        buildHud();
        Gdx.input.setInputProcessor(new InputMultiplexer(hudStage, buildInput()));
    }

    /** ساختِ تخته‌ی اولیه بدون هیچ ترکیبِ آماده. */
    private void initBoard() {
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++) {
                int g;
                do { g = RandomUtil.nextInt(GEMS); }
                while (createsInitialMatch(r, c, g));
                grid[r][c] = g;
            }
    }

    private boolean createsInitialMatch(int r, int c, int g) {
        return (c >= 2 && grid[r][c - 1] == g && grid[r][c - 2] == g)
                || (r >= 2 && grid[r - 1][c] == g && grid[r - 2][c] == g);
    }

    private void buildHud() {
        hudStage = new Stage(new FitViewport(
                GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Skin skin = GameAssets.getInstance().getSkin();

        Label title = new Label("Beghouled — Match 3", skin);
        title.setPosition(boardX, boardY + ROWS * CELL + 24f);
        hudStage.addActor(title);

        statusLabel = new Label("", skin);
        statusLabel.setPosition(boardX, boardY + ROWS * CELL + 2f);
        hudStage.addActor(statusLabel);

        // پنل سمت راست: امتیاز + گزینه‌های ارتقا (GT3)
        float px = boardX + COLS * CELL + 26f;
        scoreLabel = new Label("", skin);
        scoreLabel.setPosition(px, boardY + ROWS * CELL - 10f);
        hudStage.addActor(scoreLabel);

        Label upg = new Label("Upgrades:", skin);
        upg.setPosition(px, boardY + ROWS * CELL - 60f);
        hudStage.addActor(upg);

        // سه گزینه‌ی ارتقا: تبدیلِ همه‌ی نگین‌های یک نوع به نوعِ قوی‌تر با هزینه‌ی امتیاز
        int[][] upgrades = { {0, 4}, {2, 5}, {1, 3} }; // from → to
        int[] costs = { 300, 500, 400 };
        for (int i = 0; i < upgrades.length; i++) {
            final int from = upgrades[i][0];
            final int to = upgrades[i][1];
            final int cost = costs[i];
            TextButton b = new TextButton(
                    GEM_NAMES[from] + " → " + GEM_NAMES[to] + "  (" + cost + "⭐)", skin);
            b.setBounds(px, boardY + ROWS * CELL - 100f - i * 46f, 250f, 40f);
            b.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    applyUpgrade(from, to, cost);
                }
            });
            hudStage.addActor(b);
        }

        TextButton back = new TextButton("Back", skin);
        back.setBounds(px, boardY + 6f, 120f, 44f);
        back.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { goTo(ScreenId.QUEST); }
        });
        hudStage.addActor(back);
    }

    private InputAdapter buildInput() {
        return new InputAdapter() {
            @Override public boolean touchDown(int sx, int sy, int ptr, int btn) {
                if (state != State.IDLE || finished) return false;
                Vector2 w = viewport.unproject(new Vector2(sx, sy));
                int c = (int) ((w.x - boardX) / CELL);
                int r = (int) ((w.y - boardY) / CELL);
                if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return false;
                handleCellClick(r, c);
                return true;
            }
        };
    }

    private void handleCellClick(int r, int c) {
        if (selR < 0) { selR = r; selC = c; return; }
        if (r == selR && c == selC) { selR = selC = -1; return; }
        if (isAdjacent(r, c, selR, selC)) {
            beginSwap(selR, selC, r, c, false);
            selR = selC = -1;
        } else {
            selR = r; selC = c;   // انتخابِ نگینِ جدید
        }
    }

    private boolean isAdjacent(int r1, int c1, int r2, int c2) {
        return Math.abs(r1 - r2) + Math.abs(c1 - c2) == 1;
    }

    private void beginSwap(int r1, int c1, int r2, int c2, boolean reverting) {
        aR = r1; aC = c1; bR = r2; bC = c2;
        swapT = 0f;
        swapReverting = reverting;
        state = State.SWAP;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Update loop
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void render(float delta) {
        update(delta);

        ScreenUtils.clear(0.12f, 0.16f, 0.10f, 1f);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawBoard();
        batch.end();

        statusLabel.setText(finished ? "🎉 Cleared! Well done."
                : "Gems remaining: " + Math.max(0, matchesRemaining));
        scoreLabel.setText("Score: " + score);
        hudStage.act(delta);
        hudStage.draw();
    }

    private void update(float delta) {
        switch (state) {
            case SWAP: updateSwap(delta); break;
            case FALL: updateFall(delta); break;
            default: break;
        }
    }

    private void updateSwap(float delta) {
        swapT += delta * SWAP_SPEED;
        if (swapT < 1f) return;
        // جابه‌جایی کامل شد — مقادیر را واقعاً عوض کن
        int tmp = grid[aR][aC]; grid[aR][aC] = grid[bR][bC]; grid[bR][bC] = tmp;
        state = State.IDLE;
        if (swapReverting) return;   // بازگشتِ ناموفق تمام شد
        if (hasAnyMatch()) {
            resolveMatches();
        } else {
            beginSwap(aR, aC, bR, bC, true);  // ترکیبی نشد → برگردان
        }
    }

    private void updateFall(float delta) {
        boolean settled = true;
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++) {
                if (fallOffset[r][c] > 0f) {
                    fallOffset[r][c] = Math.max(0f, fallOffset[r][c] - FALL_SPEED * delta);
                    if (fallOffset[r][c] > 0f) settled = false;
                }
            }
        if (!settled) return;
        state = State.IDLE;
        if (hasAnyMatch()) resolveMatches();   // آبشار (cascade)
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Match / gravity
    // ─────────────────────────────────────────────────────────────────────────

    private boolean hasAnyMatch() {
        return computeMatches() != null;
    }

    /** برمی‌گرداند ماتریسِ نگین‌های ترکیب‌شده، یا null اگر هیچ ترکیبی نباشد. */
    private boolean[][] computeMatches() {
        boolean[][] m = new boolean[ROWS][COLS];
        boolean any = false;
        // افقی
        for (int r = 0; r < ROWS; r++) {
            int run = 1;
            for (int c = 1; c <= COLS; c++) {
                if (c < COLS && grid[r][c] == grid[r][c - 1] && grid[r][c] >= 0) {
                    run++;
                } else {
                    if (run >= 3) { for (int k = c - run; k < c; k++) { m[r][k] = true; any = true; } }
                    run = 1;
                }
            }
        }
        // عمودی
        for (int c = 0; c < COLS; c++) {
            int run = 1;
            for (int r = 1; r <= ROWS; r++) {
                if (r < ROWS && grid[r][c] == grid[r - 1][c] && grid[r][c] >= 0) {
                    run++;
                } else {
                    if (run >= 3) { for (int k = r - run; k < r; k++) { m[k][c] = true; any = true; } }
                    run = 1;
                }
            }
        }
        return any ? m : null;
    }

    private void resolveMatches() {
        boolean[][] m = computeMatches();
        if (m == null) { state = State.IDLE; return; }
        int cleared = 0;
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                if (m[r][c]) { grid[r][c] = -1; cleared++; }
        score += 50L * cleared;
        matchesRemaining -= cleared;
        if (matchesRemaining <= 0) finished = true;
        applyGravity();
        state = State.FALL;
    }

    /** سقوطِ نگین‌ها به پایین و پرکردنِ بالا با نگین‌های جدید (با آفستِ انیمیشن). */
    private void applyGravity() {
        for (int c = 0; c < COLS; c++) {
            int write = 0; // از پایین
            for (int r = 0; r < ROWS; r++) {
                if (grid[r][c] >= 0) {
                    if (write != r) {
                        grid[write][c] = grid[r][c];
                        fallOffset[write][c] = (r - write) * CELL;
                        grid[r][c] = -1;
                    }
                    write++;
                }
            }
            // پرکردنِ خانه‌های خالیِ بالا با نگین جدید
            for (int r = write; r < ROWS; r++) {
                grid[r][c] = RandomUtil.nextInt(GEMS);
                fallOffset[r][c] = (ROWS - r + 1) * CELL;
            }
        }
    }

    private void applyUpgrade(int from, int to, int cost) {
        if (finished || state != State.IDLE) return;
        if (score < cost) {
            statusLabel.setText("Not enough score for upgrade!");
            return;
        }
        score -= cost;
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                if (grid[r][c] == from) grid[r][c] = to;
        // ممکن است ارتقا ترکیب بسازد → resolve
        if (hasAnyMatch()) resolveMatches();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Rendering
    // ─────────────────────────────────────────────────────────────────────────

    private void drawBoard() {
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        // پس‌زمینه‌ی تخته
        batch.setColor(0.08f, 0.10f, 0.07f, 1f);
        batch.draw(white, boardX - 6, boardY - 6, COLS * CELL + 12, ROWS * CELL + 12);

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                float cellX = boardX + c * CELL;
                float cellY = boardY + r * CELL;
                // خانه‌ی شطرنجی
                boolean dark = (r + c) % 2 == 0;
                batch.setColor(dark ? 0.16f : 0.20f, dark ? 0.20f : 0.24f, 0.14f, 1f);
                batch.draw(white, cellX, cellY, CELL, CELL);

                int gem = grid[r][c];
                if (gem < 0) continue;

                // موقعیتِ رسمِ نگین (با آفستِ سقوط یا جابه‌جایی)
                float gx = cellX;
                float gy = cellY + fallOffset[r][c];
                if (state == State.SWAP) {
                    float p = MathUtils.clamp(swapT, 0f, 1f);
                    if (r == aR && c == aC) {
                        gx = MathUtils.lerp(cellX, boardX + bC * CELL, p);
                        gy = MathUtils.lerp(cellY, boardY + bR * CELL, p);
                    } else if (r == bR && c == bC) {
                        gx = MathUtils.lerp(cellX, boardX + aC * CELL, p);
                        gy = MathUtils.lerp(cellY, boardY + aR * CELL, p);
                    }
                }
                drawGem(white, gem, gx, gy, r == selR && c == selC);
            }
        }
        batch.setColor(Color.WHITE);
    }

    private void drawGem(TextureRegion white, int gem, float x, float y, boolean selected) {
        float pad = 6f;
        Color col = GEM_COLORS[gem];
        // انتخاب: قابِ روشن
        if (selected) {
            batch.setColor(1f, 1f, 1f, 0.9f);
            batch.draw(white, x + 2, y + 2, CELL - 4, CELL - 4);
        }
        batch.setColor(col.r * 0.5f, col.g * 0.5f, col.b * 0.5f, 1f);
        batch.draw(white, x + pad - 2, y + pad - 2, CELL - 2 * pad + 4, CELL - 2 * pad + 4);
        batch.setColor(col);
        batch.draw(white, x + pad, y + pad, CELL - 2 * pad, CELL - 2 * pad);
        // شکلِ داخلی متفاوت برای تمایزِ بیشتر (اندازه بر اساس نوع)
        float inner = (CELL - 2 * pad) * (0.30f + 0.06f * gem);
        float cx = x + CELL / 2f - inner / 2f;
        float cy = y + CELL / 2f - inner / 2f;
        batch.setColor(1f, 1f, 1f, 0.35f);
        batch.draw(white, cx, cy, inner, inner);
    }

    @Override
    public void resize(int w, int h) {
        viewport.update(w, h, true);
        hudStage.getViewport().update(w, h, true);
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (hudStage != null) hudStage.dispose();
    }
}
