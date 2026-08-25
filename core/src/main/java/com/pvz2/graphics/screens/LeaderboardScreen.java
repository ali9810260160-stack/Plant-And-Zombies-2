package com.pvz2.graphics.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.pvz2.PVZApplication;
import com.pvz2.graphics.*;
import com.pvz2.graphics.assets.GameAssets;
import pvz.skin.BorderedTable;

import java.util.List;

/**
 * جدول امتیازات — طبق سند: «فهرست کاربران به‌همراه اطلاعاتشان مطابق فاز یک
 * نمایش داده می‌شود، با امکان مرتب‌سازی مطابق فاز یک». منطق sort/داده از
 * {@link GameFacade#getLeaderboard} فاز یک می‌آید — چیزی دوباره‌نویسی نشده،
 * فقط چیدمان کارتی‌تر شده.
 */
public class LeaderboardScreen extends BaseScreen {

    private static final String[] HEADERS = {"User", "Last Level", "Mini-game", "Daily Quest", "Quest", "MeoPoint"};
    private static final String[] KEYS    = {"username", "lastLevel", "minigame", "daily", "quest", "meopoint"};
    private static final float[]  WIDTHS  = {150, 170, 100, 120, 80, 130};

    private Stage   stage;
    private String  sortKey   = "meopoint";
    private boolean ascending = false;

    public LeaderboardScreen(PVZApplication game) { super(game); }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        buildUi();
    }

    private void buildUi() {
        Skin skin = GameAssets.getInstance().getSkin();

        // پس‌زمینه‌ی فضاییِ منوی اصلی (به‌جای مشکیِ خالی)
        if (!com.pvz2.graphics.assets.AssetIds.MAIN_MENU_BACKGROUND.isEmpty()) {
            Image sky = new Image(GameAssets.getInstance().region(
                    com.pvz2.graphics.assets.AssetIds.MAIN_MENU_BACKGROUND));
            sky.setScaling(com.badlogic.gdx.utils.Scaling.fill);
            sky.setFillParent(true);
            sky.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
            stage.addActor(sky);
        }

        Table root = new Table();
        root.setFillParent(true);
        root.pad(24);

        root.add(new Label("Leaderboard", skin, "big_outline")).padBottom(12).row();

        // نوارِ کنترلِ مرتب‌سازی (انتخابِ پارامتر + صعودی/نزولی) — علاوه بر کلیکِ هدرها.
        root.add(buildSortControls(skin)).padBottom(12).row();

        BorderedTable panel = new BorderedTable();
        Table table = new Table(skin);
        buildHeaderRow(table, skin);
        buildDataRows(table, skin);

        ScrollPane scroll = new ScrollPane(table, skin);
        scroll.setFadeScrollBars(false);
        panel.add(scroll).width(770).height(430);

        root.add(panel).padBottom(16).row();

        TextButton back = new TextButton("Back", skin, "brown");
        back.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { goTo(ScreenId.MAIN_MENU); }
        });
        root.add(back).size(140, 60);
        stage.addActor(root);
    }

    private static final String SORT_ASC_ICON  = "Exports/sort_ascending_up.png";
    private static final String SORT_DESC_ICON = "Exports/sort_descending_up.png";

    /** نوارِ گرافیکیِ مرتب‌سازی: انتخابِ پارامتر (SelectBox) + دکمه‌ی صعودی/نزولی. */
    private Table buildSortControls(Skin skin) {
        Table bar = new Table(skin);
        bar.setBackground("image_ui_dialog_asset_inner_bkgd_10");
        bar.pad(8, 16, 8, 16);

        Label sortLbl = new Label("Sort by:", skin, "medium");
        sortLbl.setColor(Color.valueOf("ffe9a8"));
        bar.add(sortLbl).padRight(10);

        final SelectBox<String> sortBox = new SelectBox<>(skin);
        sortBox.setItems(HEADERS);
        sortBox.setSelectedIndex(indexOfKey(sortKey));
        sortBox.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                int idx = sortBox.getSelectedIndex();
                if (idx >= 0 && idx < KEYS.length && !KEYS[idx].equals(sortKey)) {
                    sortKey = KEYS[idx];
                    stage.clear();
                    buildUi();
                }
            }
        });
        bar.add(sortBox).width(190).height(44).padRight(18);

        // دکمه‌ی صعودی/نزولی با آیکونِ assets/Exports (کلیک = تعویضِ جهت).
        Table dirBtn = new Table();
        TextureRegion dirIcon = GameAssets.getInstance().local(ascending ? SORT_ASC_ICON : SORT_DESC_ICON);
        Image dirImg = new Image(dirIcon);
        dirImg.setScaling(com.badlogic.gdx.utils.Scaling.fit);
        dirBtn.add(dirImg).size(34).padRight(8);
        Label dirLbl = new Label(ascending ? "Ascending" : "Descending", skin, "medium");
        dirLbl.setColor(Color.WHITE);
        dirBtn.add(dirLbl);
        dirBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                ascending = !ascending;
                stage.clear();
                buildUi();
            }
        });
        bar.add(dirBtn);

        return bar;
    }

    private int indexOfKey(String key) {
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equals(key)) return i;
        return 0;
    }

    private void buildHeaderRow(Table table, Skin skin) {
        table.setBackground("image_ui_dialog_asset_inner_bkgd_10");
        table.add(new Label("#", skin, "default")).width(40).height(42);
        for (int i = 0; i < HEADERS.length; i++) {
            final String key = KEYS[i];
            String arrow = key.equals(sortKey) ? (ascending ? " \u25B2" : " \u25BC") : "";
            TextButton btn = new TextButton(HEADERS[i] + arrow, skin, "brown");
            btn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    if (sortKey.equals(key)) ascending = !ascending;
                    else { sortKey = key; ascending = false; }
                    stage.clear();
                    buildUi();
                }
            });
            table.add(btn).width(WIDTHS[i]).height(42);
        }
        table.row();
    }

    private void buildDataRows(Table table, Skin skin) {
        List<GameFacade.LeaderboardEntry> entries = facade().getLeaderboard(sortKey, ascending);
        String currentUser = facade().getCurrentUsername();
        if (entries.isEmpty()) {
            table.add(new Label("No users registered yet", skin, "default"))
                    .colspan(HEADERS.length + 1).padTop(16).row();
            return;
        }
        int rank = 1;
        for (GameFacade.LeaderboardEntry entry : entries) {
            boolean isMine = entry.username.equals(currentUser);
            buildRow(table, skin, rank++, entry, isMine);
        }
    }

    private void buildRow(Table table, Skin skin, int rank, GameFacade.LeaderboardEntry e, boolean highlight) {
        Label rankLbl = new Label(String.valueOf(rank), skin, "default");
        if (highlight) rankLbl.setColor(Color.YELLOW);
        table.add(rankLbl).width(40).height(36).pad(2);

        String[] cells = {
                e.username, e.lastLevel,
                String.valueOf(e.minigameCount), String.valueOf(e.dailyQuestCount),
                String.valueOf(e.questCount),
                // My Point: "-" for players who never played the networked scored game.
                e.bestMeopoint < 0 ? "-" : String.valueOf(e.bestMeopoint)
        };
        for (int i = 0; i < cells.length; i++) {
            Label lbl = new Label(cells[i], skin, "default");
            if (highlight) lbl.setColor(Color.YELLOW);
            table.add(lbl).width(WIDTHS[i]).height(36).pad(2);
        }
        table.row();
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
