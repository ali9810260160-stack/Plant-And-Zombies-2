package com.pvz2.graphics.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.pvz2.PVZApplication;
import com.pvz2.graphics.BaseScreen;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.ScreenId;
import com.pvz2.graphics.assets.GameAssets;

/**
 * صفحه‌ی لودینگِ ابتدای بازی — مطابقِ
 * {@code documant-and-files/.../loading page.jpg}: پس‌زمینه‌ی تمام‌صفحه که هر
 * ۲ ثانیه با fadeِ نرم به تصویرِ بعدی می‌رود (assets/backgrounds/loading page)،
 * یک نوارِ پیشرفتِ سبز و نوشته‌ی «loading...». کلِ صفحه {@value #TOTAL} ثانیه
 * دوام دارد و سپس به مقصد ({@code target}) می‌رود.
 */
public class LoadingScreen extends BaseScreen {

    private static final String[] BG = {
        "backgrounds/loading page/backdrop_a.png",
        "backgrounds/loading page/backdrop_b.png",
        "backgrounds/loading page/backdrop_c.png",
        "backgrounds/loading page/backdrop_j.png",
    };
    private static final float TOTAL = 10f;   // مدتِ کلِ لودینگ
    private static final float PER   = 2f;    // مدتِ نمایشِ هر تصویر
    private static final float FADE  = 0.6f;  // مدتِ fade بینِ دو تصویر

    private static final float VW = GameConstants.VIEWPORT_WIDTH;
    private static final float VH = GameConstants.VIEWPORT_HEIGHT;

    private final ScreenId target;
    private Stage stage;
    private SpriteBatch batch;
    private TextureRegion[] regions;
    private TextureRegion white;
    private Label loadingLabel;
    private float time;
    private boolean advanced;

    public LoadingScreen(PVZApplication game, ScreenId target) {
        super(game);
        this.target = target;
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(VW, VH));
        batch = new SpriteBatch();
        GameAssets assets = GameAssets.getInstance();
        white = assets.getWhiteRegion();
        regions = new TextureRegion[BG.length];
        for (int i = 0; i < BG.length; i++) regions[i] = assets.local(BG[i]);

        Skin skin = assets.getSkin();
        loadingLabel = new Label("loading...", skin, "medium");
        loadingLabel.setColor(0.85f, 1f, 0.5f, 1f);
        loadingLabel.setAlignment(Align.center);
        loadingLabel.setSize(VW, 30f);
        loadingLabel.setPosition(0f, 58f);
        stage.addActor(loadingLabel);
    }

    @Override
    public void render(float delta) {
        GameAssets.getInstance().update();
        time += delta;
        if (time >= TOTAL && !advanced) {
            advanced = true;
            game.goTo(target);
            return;
        }

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        stage.getViewport().apply();
        batch.setProjectionMatrix(stage.getCamera().combined);
        batch.begin();

        // ── پس‌زمینه‌ی تمام‌صفحه با fadeِ نرم به تصویرِ بعدی ──
        int idx = Math.min(regions.length - 1, (int) (time / PER));
        float within = time - idx * PER;               // ۰..PER در این اسلات
        batch.setColor(Color.WHITE);
        drawFull(regions[idx]);
        if (idx + 1 < regions.length && within > PER - FADE) {
            float a = (within - (PER - FADE)) / FADE;  // ۰..۱
            batch.setColor(1f, 1f, 1f, Math.min(1f, a));
            drawFull(regions[idx + 1]);
            batch.setColor(Color.WHITE);
        }

        // ── نوارِ پیشرفت (سبز) نزدیکِ پایین-وسط ──
        float p = Math.min(1f, time / TOTAL);
        float barW = 520f, barH = 26f;
        float bx = (VW - barW) / 2f, by = 96f;
        batch.setColor(0f, 0f, 0f, 0.55f);
        batch.draw(white, bx - 4f, by - 4f, barW + 8f, barH + 8f);   // قابِ تیره
        batch.setColor(0.10f, 0.12f, 0.10f, 1f);
        batch.draw(white, bx, by, barW, barH);                       // شیارِ خالی
        batch.setColor(0.35f, 0.85f, 0.20f, 1f);
        batch.draw(white, bx, by, barW * p, barH);                   // پرشدنِ سبز
        batch.setColor(0.55f, 1f, 0.35f, 1f);
        batch.draw(white, bx, by + barH * 0.6f, barW * p, barH * 0.22f); // های‌لایتِ بالا
        batch.setColor(Color.WHITE);

        batch.end();

        stage.act(delta);
        stage.draw();
    }

    /** یک TextureRegion را تمام‌صفحه (۱۲۸۰×۷۲۰) می‌کشد. */
    private void drawFull(TextureRegion r) {
        if (r == null) return;
        batch.draw(r, 0f, 0f, VW, VH);
    }

    @Override
    public void resize(int w, int h) { stage.getViewport().update(w, h, true); }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (batch != null) batch.dispose();
    }
}
