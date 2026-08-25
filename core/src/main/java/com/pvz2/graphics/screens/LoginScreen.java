package com.pvz2.graphics.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.pvz2.PVZApplication;
import com.pvz2.graphics.*;
import com.pvz2.graphics.actors.ToastActor;
import com.pvz2.graphics.assets.GameAssets;

/**
 * صفحه‌ی ورود + بازیابیِ رمز.
 *
 * <p>هر گام پس‌زمینه‌ی از پیش‌ساخته‌ی مخصوصِ خودش را دارد (پوشه‌ی {@code auth/})؛
 * ویجت‌های تعاملی روی ناحیه‌ی خالیِ بردِ چوبی/سنگیِ همان تصویر چیده می‌شوند.
 * مختصات‌ها تخمینی‌اند و پس از اسکرین‌شات دقیق می‌شوند.</p>
 */
public class LoginScreen extends BaseScreen {

    private Stage  stage;
    private String forgotUsername;

    public LoginScreen(PVZApplication game) { super(game); }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        buildLoginForm();
    }

    // ───────────────────────── ورود ─────────────────────────
    private void buildLoginForm() {
        stage.clear();
        Skin skin = GameAssets.getInstance().getSkin();
        stage.addActor(AuthUi.background("auth/login_bg.png"));

        // نشانگرِ وضعیتِ اتصال به سرور (فاز ۳)
        com.pvz2.graphics.actors.NetStatusActor netStatus =
                new com.pvz2.graphics.actors.NetStatusActor(skin);
        netStatus.setPosition(20, 20);
        stage.addActor(netStatus);

        // ناحیه‌ی داخلِ بردِ چوبیِ login_bg (تقریبی)
        Table form = new Table(skin);
        form.setBounds(360, 175, 545, 320);
        form.top().padTop(18);

        TextField tfUser = new TextField("", skin);
        TextField tfPass = new TextField("", skin);
        tfPass.setPasswordMode(true); tfPass.setPasswordCharacter('*');

        form.add(authLabel(skin, "Username")).right().padRight(12).padBottom(12);
        form.add(tfUser).width(300).height(44).padBottom(12).row();
        form.add(authLabel(skin, "Password")).right().padRight(12).padBottom(6);
        form.add(tfPass).width(300).height(44).padBottom(6).row();

        CheckBox cbStay = new CheckBox("  Remember me", skin);
        cbStay.getLabel().setColor(Color.WHITE);
        form.add(cbStay).colspan(2).left().padLeft(80).padBottom(6).row();

        Label errLbl = new Label("", skin);
        errLbl.setColor(Color.SCARLET);
        form.add(errLbl).colspan(2).padBottom(4).row();

        TextButton loginBtn = new TextButton("Login", skin, "green");
        loginBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                String err = facade().login(tfUser.getText().trim(), tfPass.getText(), cbStay.isChecked());
                if (err != null) errLbl.setText(err);
                else goTo(ScreenId.MAIN_MENU);
            }
        });
        form.add(loginBtn).colspan(2).width(220).height(50).padBottom(4).row();

        TextButton forgotBtn = new TextButton("Forgot Password?", skin);
        forgotBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { buildForgotStep1(); }
        });
        form.add(forgotBtn).colspan(2).row();

        stage.addActor(form);

        // گوشه‌ها: back → welcome، X → خروج
        stage.addActor(AuthUi.hotspot(9, 636, 73, 70, () -> goTo(ScreenId.WELCOME)));
        stage.addActor(AuthUi.hotspot(1200, 650, 73, 56, () -> goTo(ScreenId.WELCOME)));
    }

    // ─────────────── بازیابیِ رمز: گام ۱ (نام کاربری/ایمیل) ───────────────
    private void buildForgotStep1() {
        stage.clear();
        Skin skin = GameAssets.getInstance().getSkin();
        stage.addActor(AuthUi.background("auth/password_recovery_bg.jpg"));

        Table form = new Table(skin);
        form.setBounds(430, 175, 560, 300);
        form.top().padTop(20);

        TextField tfUser  = new TextField("", skin);
        TextField tfEmail = new TextField("", skin);
        form.add(authLabel(skin, "Username")).right().padRight(12).padBottom(12);
        form.add(tfUser).width(300).height(44).padBottom(12).row();
        form.add(authLabel(skin, "Email")).right().padRight(12).padBottom(12);
        form.add(tfEmail).width(300).height(44).padBottom(12).row();

        Label errLbl = new Label("", skin);
        errLbl.setColor(Color.SCARLET);
        form.add(errLbl).colspan(2).padBottom(6).row();

        TextButton nextBtn = new TextButton("Next", skin, "green");
        nextBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                String err = facade().forgotPasswordStep1(tfUser.getText().trim(), tfEmail.getText().trim());
                if (err != null) { errLbl.setText(err); return; }
                forgotUsername = tfUser.getText().trim();
                buildForgotStep2();
            }
        });
        form.add(nextBtn).colspan(2).width(220).height(50).padBottom(4).row();
        stage.addActor(form);

        stage.addActor(AuthUi.hotspot(9, 636, 73, 70, this::buildLoginForm));
        stage.addActor(AuthUi.hotspot(1200, 650, 73, 56, () -> goTo(ScreenId.WELCOME)));
    }

    // ─────────────── گام ۲: سوال امنیتی ───────────────
    private void buildForgotStep2() {
        stage.clear();
        Skin skin = GameAssets.getInstance().getSkin();
        stage.addActor(AuthUi.background("auth/security_question_bg.png"));

        Table form = new Table(skin);
        form.setBounds(430, 175, 560, 300);
        form.top().padTop(20);

        String q = facade().getSecurityQuestion(forgotUsername);
        Label qLbl = new Label(q != null ? q : "Security Question", skin);
        qLbl.setColor(Color.valueOf("ffe9a8"));
        qLbl.setWrap(true);
        form.add(qLbl).colspan(2).width(460).padBottom(14).row();

        TextField tfAns = new TextField("", skin);
        form.add(authLabel(skin, "Answer")).right().padRight(12).padBottom(12);
        form.add(tfAns).width(300).height(44).padBottom(12).row();

        Label errLbl = new Label("", skin);
        errLbl.setColor(Color.SCARLET);
        form.add(errLbl).colspan(2).padBottom(6).row();

        TextButton verifyBtn = new TextButton("Confirm", skin, "green");
        verifyBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                String err = facade().verifySecurityAnswer(forgotUsername, tfAns.getText());
                if (err != null) { errLbl.setText(err); return; }
                buildForgotStep3();
            }
        });
        form.add(verifyBtn).colspan(2).width(220).height(50).padBottom(4).row();
        stage.addActor(form);

        stage.addActor(AuthUi.hotspot(9, 636, 73, 70, this::buildLoginForm));
        stage.addActor(AuthUi.hotspot(1200, 650, 73, 56, () -> goTo(ScreenId.WELCOME)));
    }

    // ─────────────── گام ۳: رمز جدید ───────────────
    private void buildForgotStep3() {
        stage.clear();
        Skin skin = GameAssets.getInstance().getSkin();
        stage.addActor(AuthUi.background("auth/password_recovery_bg.jpg"));

        Table form = new Table(skin);
        form.setBounds(430, 175, 560, 300);
        form.top().padTop(20);

        TextField tfNew  = new TextField("", skin);
        TextField tfConf = new TextField("", skin);
        tfNew.setPasswordMode(true);  tfNew.setPasswordCharacter('*');
        tfConf.setPasswordMode(true); tfConf.setPasswordCharacter('*');
        form.add(authLabel(skin, "New")).right().padRight(12).padBottom(12);
        form.add(tfNew).width(300).height(44).padBottom(12).row();
        form.add(authLabel(skin, "Confirm")).right().padRight(12).padBottom(12);
        form.add(tfConf).width(300).height(44).padBottom(12).row();

        Label errLbl = new Label("", skin);
        errLbl.setColor(Color.SCARLET);
        form.add(errLbl).colspan(2).padBottom(6).row();

        TextButton saveBtn = new TextButton("Save", skin, "green");
        saveBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                if (!tfNew.getText().equals(tfConf.getText())) { errLbl.setText("Passwords do not match"); return; }
                String err = facade().resetPassword(forgotUsername, tfNew.getText());
                if (err != null) { errLbl.setText(err); return; }
                showToast(ToastActor.success("Password changed successfully"));
                buildLoginForm();
            }
        });
        form.add(saveBtn).colspan(2).width(220).height(50).padBottom(4).row();
        stage.addActor(form);

        stage.addActor(AuthUi.hotspot(9, 636, 73, 70, this::buildLoginForm));
        stage.addActor(AuthUi.hotspot(1200, 650, 73, 56, () -> goTo(ScreenId.WELCOME)));
    }

    private Label authLabel(Skin skin, String text) {
        Label l = new Label(text, skin);
        l.setColor(Color.valueOf("ffe9a8"));   // کرمیِ روشن، خوانا روی چوب
        return l;
    }

    private void showToast(ToastActor t) {
        t.setPosition(640 - t.getWidth() * 0.5f, 120);
        stage.addActor(t);
    }

    @Override public void render(float delta) {
        GameAssets.getInstance().update();
        ScreenUtils.clear(0f, 0f, 0f, 1);
        stage.act(delta); stage.draw();
    }
    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void dispose() { if (stage != null) stage.dispose(); }
}
