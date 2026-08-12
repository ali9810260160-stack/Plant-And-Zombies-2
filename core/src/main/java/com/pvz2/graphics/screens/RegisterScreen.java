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

public class RegisterScreen extends BaseScreen {

    private Stage stage;
    private TextField tfUser, tfPass, tfConfirm, tfNick, tfEmail;
    private SelectBox<String> sbGender;
    private Label errLabel;

    // مرحله دوم: سوال امنیتی
    private String pendingUsername;
    private TextField tfAnswer, tfAnswerConfirm;
    private SelectBox<String> sbQuestion;

    private static final String[] QUESTIONS = {
        "Name of your first school?",
        "Name of your first pet?",
        "Mother's maiden name?",
        "Father's birthplace?",
        "Name of your childhood street?"
    };

    public RegisterScreen(PVZApplication game) { super(game); }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        buildForm();
    }

    private void buildForm() {
        stage.clear();
        Skin skin = GameAssets.getInstance().getSkin();
        Table root = fullTable(skin);

        addTitle(root, skin, "Create Account");

        tfUser    = addField(root, skin, "Username");
        tfPass    = passField(root, skin, "Password");
        tfConfirm = passField(root, skin, "Confirm Password");
        tfNick    = addField(root, skin, "Nickname");
        tfEmail   = addField(root, skin, "Email");

        sbGender = new SelectBox<>(skin);
        sbGender.setItems("Male", "Female");
        root.add(new Label("Gender:", skin)).right().padRight(10);
        root.add(sbGender).fillX().padBottom(10).row();

        errLabel = new Label("", skin, "default");
        errLabel.setColor(Color.RED);
        errLabel.setWrap(true);
        root.add(errLabel).colspan(2).width(400).padBottom(8).row();

        TextButton regBtn = new TextButton("Register", skin, "green");
        regBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { doRegister(skin); }
        });
        root.add(regBtn).colspan(2).width(220).height(50).padTop(6).row();

        TextButton loginLink = new TextButton("← Log in to existing account", skin);
        loginLink.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { goTo(ScreenId.LOGIN); }
        });
        root.add(loginLink).colspan(2).padTop(6).row();
        stage.addActor(root);
    }

    private void doRegister(Skin skin) {
        String err = facade().register(
            tfUser.getText().trim(), tfPass.getText(), tfConfirm.getText(),
            tfNick.getText().trim(), tfEmail.getText().trim(),
            "Male".equals(sbGender.getSelected()) ? "male" : "female"
        );
        if (err != null) { errLabel.setText(err); return; }
        pendingUsername = tfUser.getText().trim();
        buildSecurityQuestionForm(skin);
    }

    private void buildSecurityQuestionForm(Skin skin) {
        stage.clear();
        Table root = fullTable(skin);
        addTitle(root, skin, "Choose Security Question");

        sbQuestion = new SelectBox<>(skin);
        sbQuestion.setItems(QUESTIONS);
        root.add(new Label("Question:", skin)).right().padRight(10);
        root.add(sbQuestion).fillX().padBottom(10).row();

        tfAnswer        = addField(root, skin, "Answer");
        tfAnswerConfirm = addField(root, skin, "Confirm Answer");

        TextButton confirm = new TextButton("Confirm & Continue", skin, "green");
        confirm.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { doSecurityQuestion(); }
        });
        root.add(confirm).colspan(2).width(220).height(50).padTop(10).row();
        stage.addActor(root);
    }

    private void doSecurityQuestion() {
        if (!tfAnswer.getText().equals(tfAnswerConfirm.getText())) {
            showToast(ToastActor.error("Answers do not match"));
            return;
        }
        int qIdx = sbQuestion.getSelectedIndex();
        String err = facade().setSecurityQuestion(qIdx,
                tfAnswer.getText(), tfAnswerConfirm.getText());
        if (err != null) { showToast(ToastActor.error(err)); return; }
        showToast(ToastActor.success("Registration successful! Please log in"));
        goTo(ScreenId.LOGIN);
    }

    private Table fullTable(Skin skin) {
        Table t = new Table(skin);
        t.setFillParent(true);
        t.setBackground("image_ui_dialog_asset_inner_bkgd_10");
        t.pad(28);
        return t;
    }

    private void addTitle(Table t, Skin skin, String title) {
        Label lbl = new Label(title, skin, "big");
        lbl.setColor(Color.YELLOW);
        t.add(lbl).colspan(2).padBottom(20).row();
    }

    private TextField addField(Table t, Skin skin, String label) {
        t.add(new Label(label + ":", skin)).right().padRight(10);
        TextField tf = new TextField("", skin);
        t.add(tf).width(320).padBottom(10).row();
        return tf;
    }

    private TextField passField(Table t, Skin skin, String label) {
        TextField tf = addField(t, skin, label);
        tf.setPasswordMode(true); tf.setPasswordCharacter('*');
        return tf;
    }

    private void showToast(ToastActor t) {
        t.setPosition(640 - t.getWidth() * 0.5f, 200);
        stage.addActor(t);
    }

    @Override public void render(float delta) {
        ScreenUtils.clear(0.06f, 0.04f, 0.1f, 1);
        stage.act(delta); stage.draw();
    }
    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void dispose() { if (stage != null) stage.dispose(); }
}
