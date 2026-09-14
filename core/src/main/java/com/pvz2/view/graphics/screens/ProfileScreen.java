package com.pvz2.graphics.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.pvz2.PVZApplication;
import com.pvz2.graphics.*;
import com.pvz2.graphics.actors.ToastActor;
import com.pvz2.graphics.assets.AssetIds;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.model.AppState;
import com.pvz2.model.User;
import pvz.skin.BorderedTable;

/**
 * منوی پروفایل — اطلاعات حساب، ویرایش نام‌کاربری/نام‌مستعار/ایمیل، تغییر رمز.
 * سند فاز دو تصویر جداگانه‌ای برای این منو نداره؛ استایل هم‌راستا با بقیه‌ی
 * منوها نگه داشته شده: هر بخش کارت جدای خودش، فلش بازگشت بالا-چپ (مثل News).
 * منطق واقعی (تغییر نام/رمز و اعتبارسنجی) از {@link GameFacade} فاز یک می‌آید.
 */
/**
 * منوی پروفایل — اطلاعات حساب، ویرایش نام‌کاربری/نام‌مستعار/ایمیل، تغییر رمز.
 * <p>
 * <b>باگ پیدا و فیکس‌شده:</b> {@code GameFacade.getProfileStats()} کلیدهای
 * فارسی برمی‌گردونه ("نام کاربری", "سکه", ...)، ولی فونت bitmap اسکین
 * pvz-skin فقط گلیف لاتین/انگلیسی داره -- برای همین متن فارسی بی‌صدا حذف
 * می‌شد و فقط ":" و مقدار می‌موند (خالی به‌نظر می‌رسید). اینجا دیگه از اون
 * Map استفاده نمی‌کنیم؛ مستقیم از getter های {@link User} با لیبل انگلیسی
 * می‌خونیم.
 */
public class ProfileScreen extends BaseScreen {

    private Stage stage;

    public ProfileScreen(PVZApplication game) { super(game); }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        buildUi();
    }

    private void buildUi() {
        Skin skin = GameAssets.getInstance().getSkin();

        Table root = new Table();
        root.setFillParent(true);
        root.pad(16);

        root.add(buildTitleBar(skin)).fillX().padBottom(16).row();

        Table content = new Table();
        content.add(buildInfoCard(skin)).padRight(14).top();
        content.add(buildEditAndPasswordCard(skin)).top();
        root.add(content);

        stage.addActor(root);
    }

    /** فلش بازگشت + عنوان، بالا-چپ (هم‌سبک با NewsScreen). */
    private Table buildTitleBar(Skin skin) {
        Table bar = new Table();
        ImageButton backBtn = new ImageButton(skin, "previous");
        backBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { goTo(ScreenId.MAIN_MENU); }
        });
        bar.add(backBtn).size(44).padRight(14);
        bar.add(new Label("Profile", skin, "big_outline")).expandX().left();
        return bar;
    }

    private BorderedTable buildInfoCard(Skin skin) {
        BorderedTable card = new BorderedTable();

        // [FILL ME] آواتار پروفایل -- اختیاری. خالیه، جاش رزرو شده.
        if (!AssetIds.ICON_PROFILE_AVATAR.isEmpty()) {
            TextureRegion avatar = GameAssets.getInstance().region(AssetIds.ICON_PROFILE_AVATAR);
            card.add(new Image(avatar)).size(72).colspan(2).padBottom(10).row();
        }

        card.add(new Label("Account Info", skin, "medium")).colspan(2).padBottom(12).row();

        User user = AppState.getInstance().getCurrentUser();
        if (user == null) {
            card.add(new Label("Not logged in.", skin, "default")).colspan(2);
            return card;
        }

        addInfoRow(card, skin, "Username", user.getUsername());
        addInfoRow(card, skin, "Nickname", user.getNickname());
        addInfoRow(card, skin, "Games Played", String.valueOf(user.getGamesPlayed()));
        addInfoRow(card, skin, "Coins", String.valueOf(user.getCoins()));
        addInfoRow(card, skin, "Gems", String.valueOf(user.getGems()));
        addInfoRow(card, skin, "Levels Completed", String.valueOf(user.getLevelsCompleted()));
        addInfoRow(card, skin, "Best MeoPoint", String.valueOf(user.getHighestMeoPoint()));
        return card;
    }

    private void addInfoRow(Table card, Skin skin, String label, String value) {
        Label keyLbl = new Label(label + ":", skin, "default");
        keyLbl.setColor(Color.DARK_GRAY);
        Label valLbl = new Label(value, skin, "default");
        card.add(keyLbl).right().padRight(10).padBottom(8);
        card.add(valLbl).left().padBottom(8).row();
    }

    private Table buildEditAndPasswordCard(Skin skin) {
        Table col = new Table();

        BorderedTable editCard = new BorderedTable();
        editCard.add(new Label("Edit Info", skin, "medium")).colspan(3).padBottom(10).row();
        addEditRow(editCard, skin, "New Username", val -> {
            String err = facade().changeUsername(val);
            if (err != null) return err;
            stage.clear(); buildUi(); return null;
        });
        addEditRow(editCard, skin, "New Nickname", val -> {
            String err = facade().changeNickname(val);
            if (err != null) return err;
            stage.clear(); buildUi(); return null;
        });
        addEditRow(editCard, skin, "New Email", val -> {
            String err = facade().changeEmail(val);
            if (err != null) return err;
            stage.clear(); buildUi(); return null;
        });
        col.add(editCard).padBottom(14).row();

        BorderedTable pwCard = new BorderedTable();
        buildPasswordSection(pwCard, skin);
        col.add(pwCard).row();

        return col;
    }

    private void addEditRow(Table root, Skin skin, String label, FieldAction action) {
        root.add(new Label(label + ":", skin, "default")).right().padRight(10).padBottom(8);
        TextField tf = new TextField("", skin);
        root.add(tf).width(220).padBottom(8);
        TextButton btn = new TextButton("Change", skin, "green");
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                String v = tf.getText().trim();
                if (v.isEmpty()) { showToast(ToastActor.error("Field is empty")); return; }
                String err = action.apply(v);
                if (err != null) showToast(ToastActor.error(err));
                else showToast(ToastActor.success("Change applied"));
            }
        });
        root.add(btn).width(90).padLeft(8).padBottom(8).row();
    }

    private void buildPasswordSection(Table root, Skin skin) {
        root.add(new Label("Change Password", skin, "medium")).colspan(2).padBottom(10).row();

        root.add(new Label("Old Password:", skin, "default")).right().padRight(10).padBottom(8);
        TextField tfOld = new TextField("", skin);
        tfOld.setPasswordMode(true); tfOld.setPasswordCharacter('*');
        root.add(tfOld).width(220).padBottom(8).row();

        root.add(new Label("New Password:", skin, "default")).right().padRight(10).padBottom(8);
        TextField tfNew = new TextField("", skin);
        tfNew.setPasswordMode(true); tfNew.setPasswordCharacter('*');
        root.add(tfNew).width(220).padBottom(8).row();

        TextButton btn = new TextButton("Change Password", skin, "green");
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                String err = facade().changePassword(tfOld.getText(), tfNew.getText());
                if (err != null) showToast(ToastActor.error(err));
                else { showToast(ToastActor.success("Password changed")); tfOld.setText(""); tfNew.setText(""); }
            }
        });
        root.add(btn).colspan(2).width(200).height(40).padTop(4);
    }

    private void showToast(ToastActor t) {
        t.setPosition(GameConstants.VIEWPORT_WIDTH / 2f - t.getWidth() * 0.5f, 80);
        stage.addActor(t);
    }

    @FunctionalInterface
    private interface FieldAction { String apply(String value); }

    @Override
    public void render(float delta) {
        GameAssets.getInstance().update();
        ScreenUtils.clear(0.06f, 0.05f, 0.1f, 1);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void dispose() { if (stage != null) stage.dispose(); }
}
