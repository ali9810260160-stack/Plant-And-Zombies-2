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
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.model.AppState;
import com.pvz2.model.User;
import com.pvz2.service.NewsService;
import pvz.skin.BorderedTable;

import java.util.List;

/**
 * منوی اخبار — پنل شناور، مطابق تصویر ۱۱ سند فاز دوم: عنوان «News and Updates»
 * بالای پنل، فلش بازگشت بالا-چپ، فهرست خبرها با تاریخ و متن.
 * <p>
 * از {@link NewsService} فاز یک برای خواندن/ذخیره استفاده می‌شود — چیزی
 * دوباره پیاده‌سازی نشده.
 */
public class NewsScreen extends BaseScreen {

    private Stage   stage;
    private boolean showAll = false;

    public NewsScreen(PVZApplication game) { super(game); }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        buildUi();
    }

    private void buildUi() {
        Skin skin = GameAssets.getInstance().getSkin();

        // پس‌زمینه‌ی فضاییِ منوی اصلی (مطابق مرجع: پنلِ خبر روی همان زمینه)
        if (!com.pvz2.graphics.assets.AssetIds.MAIN_MENU_BACKGROUND.isEmpty()) {
            Image sky = new Image(GameAssets.getInstance().region(
                    com.pvz2.graphics.assets.AssetIds.MAIN_MENU_BACKGROUND));
            sky.setScaling(com.badlogic.gdx.utils.Scaling.fill);
            sky.setFillParent(true);
            sky.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
            stage.addActor(sky);
        }

        BorderedTable panel = new BorderedTable();
        panel.top();

        Table titleBar = new Table();
        ImageButton backBtn = new ImageButton(skin, "previous");
        backBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { goTo(ScreenId.MAIN_MENU); }
        });
        titleBar.add(backBtn).size(44).padRight(10);

        Label title = new Label("News and Updates", skin, "big");
        title.setColor(Color.valueOf("ffe36e"));
        titleBar.add(title).expandX().center();

        int unread = getUnreadCount();
        if (unread > 0) {
            Label badge = new Label("(" + unread + " new)", skin, "default");
            badge.setColor(Color.RED);
            titleBar.add(badge).padLeft(8);
        }
        panel.add(titleBar).fillX().colspan(2).padBottom(12).row();

        Table toggleBar = new Table();
        TextButton toggleBtn = new TextButton(showAll ? "Unread Only" : "All News", skin, "brown");
        toggleBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                showAll = !showAll;
                stage.clear();
                buildUi();
            }
        });
        toggleBar.add(toggleBtn).padRight(8);

        if (unread > 0) {
            TextButton markAllBtn = new TextButton("Mark All Read", skin, "brown");
            markAllBtn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    User user = AppState.getInstance().getCurrentUser();
                    if (user != null) ServiceLocator.getInstance().getNewsService().markAllAsRead(user);
                    stage.clear();
                    buildUi();
                }
            });
            toggleBar.add(markAllBtn);
        }
        panel.add(toggleBar).colspan(2).padBottom(14).row();

        panel.add(buildNewsList(skin)).width(460).height(360).colspan(2).row();

        panel.pack();
        panel.setPosition((stage.getWidth() - panel.getWidth()) / 2f, (stage.getHeight() - panel.getHeight()) / 2f);
        stage.addActor(panel);
    }

    private ScrollPane buildNewsList(Skin skin) {
        Table list = new Table();
        list.defaults().fillX().padBottom(10);

        User user = AppState.getInstance().getCurrentUser();
        if (user == null) {
            list.add(new Label("Please log in first.", skin, "default"));
            return new ScrollPane(list, skin);
        }

        NewsService ns = ServiceLocator.getInstance().getNewsService();
        List<NewsService.NewsItem> items = showAll ? ns.getAllNews(user) : ns.getUnreadNews(user);

        if (items.isEmpty()) {
            Label empty = new Label(showAll ? "No news yet." : "You're all caught up.", skin, "default");
            empty.setColor(Color.LIGHT_GRAY);
            list.add(empty);
            ScrollPane scroll = new ScrollPane(list, skin);
            scroll.setFadeScrollBars(false);
            return scroll;
        }

        for (NewsService.NewsItem item : items) {
            list.add(buildNewsRow(skin, item)).row();
            if (!item.read) ns.markAsRead(user, item.id);
        }

        ScrollPane scroll = new ScrollPane(list, skin);
        scroll.setFadeScrollBars(false);
        return scroll;
    }

    private Table buildNewsRow(Skin skin, NewsService.NewsItem item) {
        Table row = new Table();

        Table topBar = new Table();
        Label titleLbl = new Label(item.title, skin, "default");
        titleLbl.setColor(item.read ? Color.GRAY : Color.BLACK);
        topBar.add(titleLbl).expandX().left();

        if (!item.read) {
            Label newBadge = new Label("NEW", skin, "default");
            newBadge.setColor(Color.RED);
            topBar.add(newBadge).padRight(6);
        }

        Label dateLbl = new Label(item.getDisplayDate(), skin, "default");
        dateLbl.setFontScale(0.8f);
        dateLbl.setColor(Color.DARK_GRAY);
        topBar.add(dateLbl);

        row.add(topBar).fillX().padBottom(4).row();

        if (item.content != null && !item.content.isEmpty()) {
            Label bodyLbl = new Label(item.content, skin, "default");
            bodyLbl.setFontScale(0.85f);
            bodyLbl.setColor(new Color(0.25f, 0.22f, 0.18f, 1f));
            bodyLbl.setWrap(true);
            row.add(bodyLbl).fillX().row();
        }

        return row;
    }

    private int getUnreadCount() {
        User user = AppState.getInstance().getCurrentUser();
        if (user == null) return 0;
        try { return ServiceLocator.getInstance().getNewsService().getUnreadCount(user); }
        catch (Exception e) { return 0; }
    }

    @Override
    public void render(float delta) {
        GameAssets.getInstance().update();
        ScreenUtils.clear(0.05f, 0.04f, 0.09f, 1);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void dispose() { if (stage != null) stage.dispose(); }
}
