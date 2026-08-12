package com.pvz2.graphics.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.pvz2.PVZApplication;
import com.pvz2.graphics.*;
import com.pvz2.graphics.assets.AssetIds;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.model.User;

/**
 * منوی اصلی — مرکز ناوبری بازی.
 * <p>
 * چیدمان و استایل مطابق تصویر منوی اصلی سند فاز ۲: پس‌زمینه‌ی تمام‌صفحه،
 * لوگوی بازی، دکمه‌ی بزرگ Adventure وسط، ردیف دکمه‌های ناوبری پایین با
 * استایل‌های واقعی pvz-skin (green/brown/purple/almanac/settings)، نوار
 * سکه/الماس بالا-راست، و دکمه‌ی اخبار بالا-چپ با نشان تعداد خوانده‌نشده.
 */
public class MainMenuScreen extends BaseScreen {

    private Stage stage;
    private Label unreadNewsBadge;

    public MainMenuScreen(PVZApplication game) { super(game); }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        buildUi();
    }

    private void buildUi() {
        Skin skin = GameAssets.getInstance().getSkin();
        User user = facade().getCurrentUser();

        // ---- پس‌زمینه‌ی تمام‌صفحه (اول از همه اضافه می‌شود تا زیر بقیه‌ی UI باشد) ----
        if (!AssetIds.MAIN_MENU_BACKGROUND.isEmpty()) {
            TextureRegion bgRegion = GameAssets.getInstance().region(AssetIds.MAIN_MENU_BACKGROUND);
            Image background = new Image(bgRegion);
            background.setScaling(Scaling.fill);
            Table bgLayer = new Table();
            bgLayer.setFillParent(true);
            bgLayer.add(background).grow();
            stage.addActor(bgLayer);
        }

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        root.add(buildTopBar(skin, user)).fillX().top().row();
        root.add(buildLogo(skin)).padTop(30).padBottom(30).row();
        root.add(buildAdventureButton(skin)).size(260, 90).padBottom(20).row();
        root.add(buildNavGrid(skin)).padBottom(10).row();

        stage.addActor(buildNewsCorner(skin, user));
    }

    private Table buildTopBar(Skin skin, User user) {
        Table bar = new Table(skin);
        bar.setBackground("image_ui_dialog_asset_inner_bkgd_10");
        bar.pad(6, 14, 6, 14);

        String nick = user != null ? user.getNickname() : "?";
        Label welcome = new Label("Welcome, " + nick + "!", skin, "medium");
        bar.add(welcome).expandX().left();

        long coins = user != null ? user.getCoins() : 0;
        int gems = user != null ? user.getGems() : 0;

        if (!AssetIds.ICON_COIN.isEmpty()) {
            bar.add(new Image(GameAssets.getInstance().region(AssetIds.ICON_COIN))).size(26).padRight(4);
        }
        Label coinLbl = new Label(String.valueOf(coins), skin);
        coinLbl.setColor(Color.GOLD);
        bar.add(coinLbl).padRight(20);

        if (!AssetIds.ICON_GEM.isEmpty()) {
            bar.add(new Image(GameAssets.getInstance().region(AssetIds.ICON_GEM))).size(26).padRight(4);
        }
        Label gemLbl = new Label(String.valueOf(gems), skin);
        gemLbl.setColor(Color.CYAN);
        bar.add(gemLbl).padRight(20);

        TextButton logout = new TextButton("Log Out", skin, "brown");
        logout.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                facade().logout();
                goTo(ScreenId.REGISTER);
            }
        });
        bar.add(logout);
        return bar;
    }

    private Actor buildLogo(Skin skin) {
        if (!AssetIds.MAIN_MENU_LOGO.isEmpty()) {
            Image logo = new Image(GameAssets.getInstance().region(AssetIds.MAIN_MENU_LOGO));
            logo.setScaling(Scaling.fit);
            Container<Image> c = new Container<>(logo);
            c.width(420).height(160);
            return c;
        }
        return new Label("Plants vs. Zombies 2", skin, "big_outline");
    }

    private TextButton buildAdventureButton(Skin skin) {
        TextButton btn = new TextButton("Adventure", skin, "green");
        btn.getLabel().setFontScale(1.1f);
        btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                goTo(ScreenId.ADVENTURE);
            }
        });
        return btn;
    }

    private Table buildNavGrid(Skin skin) {
        Table t = new Table();
        t.defaults().pad(6);

        t.add(iconNavButton(skin, "almanac", ScreenId.COLLECTION)).size(64);
        t.add(textNavButton(skin, "Greenhouse", "brown", ScreenId.GREENHOUSE)).size(150, 60);
        t.add(textNavButton(skin, "Shop", "purple", ScreenId.SHOP)).size(150, 60);
        t.add(iconNavButton(skin, "hud_quests", ScreenId.QUEST)).size(64);
        t.add(textNavButton(skin, "Leaderboard", "purple", ScreenId.LEADERBOARD)).size(150, 60);
        t.add(textNavButton(skin, "Profile", "brown", ScreenId.PROFILE)).size(150, 60);
        t.add(iconNavButton(skin, "settings", ScreenId.SETTINGS)).size(64);
        return t;
    }

    private TextButton textNavButton(Skin skin, String label, String style, ScreenId target) {
        TextButton btn = new TextButton(label, skin, style);
        btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                goTo(target);
            }
        });
        return btn;
    }

    private ImageButton iconNavButton(Skin skin, String imageButtonStyle, ScreenId target) {
        ImageButton btn = new ImageButton(skin, imageButtonStyle);
        btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                goTo(target);
            }
        });
        return btn;
    }

    /** دکمه‌ی اخبار بالا-چپ با نشان قرمز تعداد خوانده‌نشده (تنها مورد اجباری سند برای منوی اصلی). */
    private Table buildNewsCorner(Skin skin, User user) {
        Stack newsStack = new Stack();

        Button newsButton;
        if (!AssetIds.ICON_NEWS_BUTTON.isEmpty()) {
            newsButton = new ImageButton(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                    GameAssets.getInstance().region(AssetIds.ICON_NEWS_BUTTON)));
        } else {
            newsButton = new TextButton("News", skin, "brown");
        }
        newsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                goTo(ScreenId.NEWS);
            }
        });
        newsStack.add(newsButton);

        if (!AssetIds.ICON_NEWS_ALERT.isEmpty()) {
            Image alertIcon = new Image(GameAssets.getInstance().region(AssetIds.ICON_NEWS_ALERT));
            Table alertCorner = new Table();
            alertCorner.top().right();
            alertCorner.add(alertIcon).size(20).padTop(-6).padRight(-6);
            newsStack.add(alertCorner);
        }

        unreadNewsBadge = new Label("", skin, "default");
        unreadNewsBadge.setColor(Color.RED);
        unreadNewsBadge.setAlignment(Align.topRight);
        newsStack.add(unreadNewsBadge);

        int unreadCount = user != null ? facade().news().getUnreadCount(user) : 0;
        unreadNewsBadge.setVisible(unreadCount > 0);
        unreadNewsBadge.setText(unreadCount > 0 ? String.valueOf(unreadCount) : "");

        Table corner = new Table();
        corner.setFillParent(true);
        corner.top().left().pad(60, 10, 0, 0);
        corner.add(newsStack).size(AssetIds.ICON_NEWS_BUTTON.isEmpty() ? 120 : 64,
                AssetIds.ICON_NEWS_BUTTON.isEmpty() ? 50 : 64);
        return corner;
    }

    @Override
    public void render(float delta) {
        GameAssets.getInstance().update();
        ScreenUtils.clear(0.05f, 0.03f, 0.07f, 1);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int w, int h) { stage.getViewport().update(w, h, true); }

    @Override
    public void dispose() { if (stage != null) stage.dispose(); }
}
