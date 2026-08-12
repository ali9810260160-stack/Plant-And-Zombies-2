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

public class LoginScreen extends BaseScreen {

    private Stage  stage;
    private String forgotUsername; // مرحله بازیابی رمز

    public LoginScreen(PVZApplication game) { super(game); }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        buildLoginForm();
    }

    private void buildLoginForm() {
        stage.clear();
        Skin skin = GameAssets.getInstance().getSkin();
        Table root = buildRoot(skin);
        addTitle(root, skin, "Login");

        TextField tfUser = field(root, skin, "Username");
        TextField tfPass = field(root, skin, "Password");
        tfPass.setPasswordMode(true); tfPass.setPasswordCharacter('*');

        CheckBox cbStay = new CheckBox("  Remember me", skin);
        root.add(cbStay).colspan(2).left().padBottom(10).row();

        Label errLbl = new Label("", skin);
        errLbl.setColor(Color.RED);
        root.add(errLbl).colspan(2).padBottom(6).row();

        TextButton loginBtn = new TextButton("Login", skin, "green");
        loginBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                String err = facade().login(tfUser.getText().trim(),
                        tfPass.getText(), cbStay.isChecked());
                if (err != null) errLbl.setText(err);
                else goTo(ScreenId.MAIN_MENU);
            }
        });
        root.add(loginBtn).colspan(2).width(220).height(50).padBottom(10).row();

        TextButton forgotBtn = new TextButton("Forgot Password", skin);
        forgotBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { buildForgotStep1(skin); }
        });
        root.add(forgotBtn).colspan(2).padBottom(6).row();

        TextButton regBtn = new TextButton("← Create New Account", skin);
        regBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { goTo(ScreenId.REGISTER); }
        });
        root.add(regBtn).colspan(2).row();
        stage.addActor(root);
    }

    private void buildForgotStep1(Skin skin) {
        stage.clear();
        Table root = buildRoot(skin);
        addTitle(root, skin, "Password Recovery");
        TextField tfUser  = field(root, skin, "Username");
        TextField tfEmail = field(root, skin, "Email");
        Label errLbl = new Label("", skin);
        errLbl.setColor(Color.RED);
        root.add(errLbl).colspan(2).padBottom(6).row();

        TextButton nextBtn = new TextButton("Next ←", skin, "green");
        nextBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                String err = facade().forgotPasswordStep1(
                        tfUser.getText().trim(), tfEmail.getText().trim());
                if (err != null) { errLbl.setText(err); return; }
                forgotUsername = tfUser.getText().trim();
                buildForgotStep2(skin);
            }
        });
        root.add(nextBtn).colspan(2).width(200).height(48).padBottom(10).row();
        addBackBtn(root, skin, this::buildLoginForm);
        stage.addActor(root);
    }

    private void buildForgotStep2(Skin skin) {
        stage.clear();
        Table root = buildRoot(skin);
        String q = facade().getSecurityQuestion(forgotUsername);
        addTitle(root, skin, q != null ? q : "Security Question");
        TextField tfAns = field(root, skin, "Answer");
        Label errLbl = new Label("", skin);
        errLbl.setColor(Color.RED);
        root.add(errLbl).colspan(2).padBottom(6).row();

        TextButton verifyBtn = new TextButton("Confirm", skin, "green");
        verifyBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                String err = facade().verifySecurityAnswer(forgotUsername, tfAns.getText());
                if (err != null) { errLbl.setText(err); return; }
                buildForgotStep3(skin);
            }
        });
        root.add(verifyBtn).colspan(2).width(200).height(48).padBottom(10).row();
        addBackBtn(root, skin, this::buildLoginForm);
        stage.addActor(root);
    }

    private void buildForgotStep3(Skin skin) {
        stage.clear();
        Table root = buildRoot(skin);
        addTitle(root, skin, "New Password");
        TextField tfNew  = field(root, skin, "New Password");
        TextField tfConf = field(root, skin, "Confirm Password");
        tfNew.setPasswordMode(true);  tfNew.setPasswordCharacter('*');
        tfConf.setPasswordMode(true); tfConf.setPasswordCharacter('*');
        Label errLbl = new Label("", skin);
        errLbl.setColor(Color.RED);
        root.add(errLbl).colspan(2).padBottom(6).row();

        TextButton saveBtn = new TextButton("Save", skin, "green");
        saveBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                if (!tfNew.getText().equals(tfConf.getText())) {
                    errLbl.setText("Passwords do not match"); return;
                }
                String err = facade().resetPassword(forgotUsername, tfNew.getText());
                if (err != null) { errLbl.setText(err); return; }
                showToast(ToastActor.success("Password changed successfully"));
                buildLoginForm();
            }
        });
        root.add(saveBtn).colspan(2).width(200).height(48).padBottom(10).row();
        stage.addActor(root);
    }

    private Table buildRoot(Skin skin) {
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

    private TextField field(Table t, Skin skin, String label) {
        t.add(new Label(label + ":", skin)).right().padRight(10);
        TextField tf = new TextField("", skin);
        t.add(tf).width(310).padBottom(10).row();
        return tf;
    }

    private void addBackBtn(Table root, Skin skin, Runnable action) {
        TextButton b = new TextButton("← Back", skin);
        b.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { action.run(); }
        });
        root.add(b).colspan(2).row();
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
