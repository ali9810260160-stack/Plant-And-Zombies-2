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
 * صفحه‌ی ثبت‌نام + انتخابِ سوالِ امنیتی.
 *
 * <p>پس‌زمینه‌ی از پیش‌ساخته‌ی {@code auth/signup_bg.png} یک بردِ چوبیِ خالی است؛
 * همه‌ی فیلدها داخلِ همان برد در یک {@link ScrollPane} قرار می‌گیرند تا اگر جا کم
 * آمد اسکرول شوند ولی از قابِ برد بیرون نزنند. مرحله‌ی دومْ سوالِ امنیتی روی
 * {@code auth/security_question_bg.png}. مختصات‌ها تخمینی‌اند و با اسکرین‌شات دقیق می‌شوند.</p>
 */
public class RegisterScreen extends BaseScreen {

    private Stage stage;
    private TextField tfUser, tfPass, tfConfirm, tfNick, tfEmail;
    private SelectBox<String> sbGender;
    private Label errLabel;

    private String pendingUsername;
    private TextField tfAnswer, tfAnswerConfirm;
    private SelectBox<String> sbQuestion;

    /** سوال‌های امنیتیِ باموضوعِ PvZ — مطابقِ تصویرِ مرجع. */
    private static final String[] QUESTIONS = {
        "What was your first plant's nickname?",
        "Favorite PvZ 2 world?",
        "The weirdest zombie you've seen?",
        "Your Dave-approved lucky number?",
        "Name of your home lawn?"
    };

    public RegisterScreen(PVZApplication game) { super(game); }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        buildForm();
    }

    // ───────────────────────── ثبت‌نام ─────────────────────────
    private void buildForm() {
        stage.clear();
        Skin skin = GameAssets.getInstance().getSkin();
        stage.addActor(AuthUi.background("auth/signup_bg.png"));

        // ناحیه‌ی داخلِ بردِ چوبیِ signup_bg (تقریبی)
        Table form = new Table(skin);
        form.setBounds(295, 165, 687, 358);
        form.top().padTop(12);

        // ---- فیلدها داخلِ ScrollPane (در همان باکس می‌مانند) ----
        Table fields = new Table(skin);
        fields.defaults().padBottom(9);
        tfUser    = row(fields, skin, "Username");
        tfEmail   = row(fields, skin, "Email");
        tfPass    = passRow(fields, skin, "Password");
        tfConfirm = passRow(fields, skin, "Confirm Password");
        tfNick    = row(fields, skin, "Nickname");

        sbGender = new SelectBox<>(skin);
        sbGender.setItems("Male", "Female");
        fields.add(authLabel(skin, "Gender")).right().padRight(12);
        fields.add(sbGender).width(300).height(42).left().row();

        ScrollPane sp = new ScrollPane(fields);   // استایلِ خالی ⇒ پس‌زمینه‌ی شفاف
        sp.setScrollingDisabled(true, false);
        sp.setFadeScrollBars(false);
        sp.setOverscroll(false, false);
        form.add(sp).width(650).height(232).padBottom(8).row();

        errLabel = new Label("", skin, "default");
        errLabel.setColor(Color.SCARLET);
        errLabel.setWrap(true);
        errLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        form.add(errLabel).width(560).padBottom(6).row();

        // ---- دکمه‌ها ----
        Table buttons = new Table();
        TextButton regBtn = new TextButton("Register", skin, "green");
        regBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { doRegister(skin); }
        });
        TextButton cancelBtn = new TextButton("Cancel", skin, "brown");
        cancelBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { goTo(ScreenId.WELCOME); }
        });
        buttons.add(regBtn).width(200).height(50).padRight(24);
        buttons.add(cancelBtn).width(170).height(50);
        form.add(buttons).row();

        stage.addActor(form);

        // گوشه‌ها
        stage.addActor(AuthUi.hotspot(9, 636, 73, 70, () -> goTo(ScreenId.WELCOME)));
        stage.addActor(AuthUi.hotspot(1200, 650, 73, 56, () -> goTo(ScreenId.WELCOME)));
    }

    private void doRegister(Skin skin) {
        String nick = tfNick.getText().trim();
        if (nick.isEmpty()) nick = tfUser.getText().trim();
        String err = facade().register(
            tfUser.getText().trim(), tfPass.getText(), tfConfirm.getText(),
            nick, tfEmail.getText().trim(),
            "Male".equals(sbGender.getSelected()) ? "male" : "female"
        );
        if (err != null) { errLabel.setText(err); return; }
        pendingUsername = tfUser.getText().trim();
        buildSecurityQuestionForm(skin);
    }

    // ─────────────── مرحله‌ی دوم: سوالِ امنیتی ───────────────
    private void buildSecurityQuestionForm(Skin skin) {
        stage.clear();
        stage.addActor(AuthUi.background("auth/security_question_bg.png"));

        Table form = new Table(skin);
        form.setBounds(295, 160, 687, 350);
        form.top().padTop(22);

        form.add(authLabel(skin, "Question")).right().padRight(12).padBottom(12);
        sbQuestion = new SelectBox<>(skin);
        sbQuestion.setItems(QUESTIONS);
        form.add(sbQuestion).width(400).height(44).left().padBottom(12).row();

        tfAnswer        = row(form, skin, "Answer");
        tfAnswerConfirm = row(form, skin, "Confirm Answer");

        Label err = new Label("", skin);
        err.setColor(Color.SCARLET);
        form.add(err).colspan(2).padBottom(6).row();

        Table buttons = new Table();
        TextButton confirm = new TextButton("Save & Continue", skin, "green");
        confirm.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { doSecurityQuestion(err); }
        });
        TextButton cancel = new TextButton("Cancel", skin, "brown");
        cancel.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { buildForm(); }
        });
        buttons.add(confirm).width(230).height(50).padRight(24);
        buttons.add(cancel).width(150).height(50);
        form.add(buttons).colspan(2).padTop(4).row();

        stage.addActor(form);
        stage.addActor(AuthUi.hotspot(9, 636, 73, 70, this::buildForm));
        stage.addActor(AuthUi.hotspot(1200, 650, 73, 56, () -> goTo(ScreenId.WELCOME)));
    }

    private void doSecurityQuestion(Label err) {
        if (!tfAnswer.getText().equals(tfAnswerConfirm.getText())) {
            err.setText("Answers do not match"); return;
        }
        int qIdx = sbQuestion.getSelectedIndex();
        String e = facade().setSecurityQuestion(qIdx, tfAnswer.getText(), tfAnswerConfirm.getText());
        if (e != null) { err.setText(e); return; }
        showToast(ToastActor.success("Registration successful! Please log in"));
        goTo(ScreenId.LOGIN);
    }

    // ───────────────────────── کمک‌کننده‌ها ─────────────────────────
    private TextField row(Table t, Skin skin, String label) {
        t.add(authLabel(skin, label)).right().padRight(12);
        TextField tf = new TextField("", skin);
        t.add(tf).width(300).height(42).left().row();
        return tf;
    }

    private TextField passRow(Table t, Skin skin, String label) {
        TextField tf = row(t, skin, label);
        tf.setPasswordMode(true); tf.setPasswordCharacter('*');
        return tf;
    }

    private Label authLabel(Skin skin, String text) {
        Label l = new Label(text, skin);
        l.setColor(Color.valueOf("ffe9a8"));
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
