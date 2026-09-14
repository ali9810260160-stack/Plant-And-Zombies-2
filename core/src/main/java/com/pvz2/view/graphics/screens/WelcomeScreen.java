package com.pvz2.graphics.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.pvz2.PVZApplication;
import com.pvz2.graphics.*;
import com.pvz2.graphics.actors.ToastActor;
import com.pvz2.graphics.assets.GameAssets;

/**
 * صفحه‌ی ورود (welcome) — نقطه‌ی شروعِ بازیِ کاربرِ ناوارد.
 *
 * <p>پس‌زمینه‌ی از پیش‌ساخته‌ی {@code auth/welcome_bg.png} قابِ چوبی + دو دکمه‌ی بزرگِ
 * «SIGN UP / REGISTER» و «LOGIN» و لینک‌های «FORGOT PASSWORD؟/SETTINGS/CREDITS» را
 * به‌صورتِ هنر دارد؛ اینجا فقط نواحیِ نامرئیِ قابل‌کلیک روی همان‌ها گذاشته می‌شود.
 * مختصات‌ها تخمینی‌اند و پس از اولین اسکرین‌شات دقیق می‌شوند.</p>
 */
public class WelcomeScreen extends BaseScreen {

    private Stage stage;

    public WelcomeScreen(PVZApplication game) { super(game); }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);

        stage.addActor(AuthUi.background("auth/welcome_bg.png"));

        // ---- دکمه‌های اصلی (روی هنرِ پخته‌شده) ----
        stage.addActor(AuthUi.hotspot(336, 214, 282, 178, () -> goTo(ScreenId.REGISTER)));   // SIGN UP / REGISTER
        stage.addActor(AuthUi.hotspot(673, 214, 273, 197, () -> goTo(ScreenId.LOGIN)));       // LOGIN

        // ---- لینک‌های پایینِ برد ----
        stage.addActor(AuthUi.hotspot(491, 157, 305, 43, () -> goTo(ScreenId.LOGIN)));        // FORGOT PASSWORD?
        stage.addActor(AuthUi.hotspot(495, 115, 141, 42, () -> goTo(ScreenId.SETTINGS)));     // SETTINGS
        stage.addActor(AuthUi.hotspot(664, 115, 141, 42, this::showCredits));                 // CREDITS

        // ---- گوشه‌ها (پیکانِ برگشت + X) = خروج از بازی ----
        stage.addActor(AuthUi.hotspot(9, 636, 73, 70, () -> Gdx.app.exit()));                 // back
        stage.addActor(AuthUi.hotspot(1200, 650, 73, 56, () -> Gdx.app.exit()));              // X

        // ---- نشانگرِ وضعیتِ اتصال به سرور (فاز ۳) ----
        com.pvz2.graphics.actors.NetStatusActor status =
                new com.pvz2.graphics.actors.NetStatusActor(GameAssets.getInstance().getSkin());
        status.setPosition(20, 20);
        stage.addActor(status);
    }

    private void showCredits() {
        ToastActor t = ToastActor.success("Plants vs. Zombies 2 — AP Spring 2026 project");
        t.setPosition(640 - t.getWidth() * 0.5f, 120);
        stage.addActor(t);
    }

    @Override public void render(float delta) {
        GameAssets.getInstance().update();
        ScreenUtils.clear(0f, 0f, 0f, 1);
        stage.act(delta);
        stage.draw();
    }
    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void dispose() { if (stage != null) stage.dispose(); }
}
