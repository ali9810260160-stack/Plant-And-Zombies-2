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

import java.util.List;

public class QuestScreen extends BaseScreen {

    private static final String[] PAGE_NAMES = {"Story", "Epic", "Daily", "Mini-Games"};
    private static final String[] PAGE_KEYS  = {"story",   "epic", "daily",  "minigame"};

    private Stage stage;
    private int   activePage = 0;

    public QuestScreen(PVZApplication game) { super(game); }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        buildUi();
    }

    private void buildUi() {
        Skin skin = GameAssets.getInstance().getSkin();
        Table root = new Table(skin); root.setFillParent(true); root.pad(14);

        Label title = new Label("📋 Travel Log", skin, "big");
        title.setColor(Color.YELLOW);
        root.add(title).colspan(PAGE_NAMES.length).padBottom(10).row();

        buildTabs(root, skin);
        buildContent(root, skin);

        TextButton back = new TextButton("← Back", skin);
        back.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { goTo(ScreenId.MAIN_MENU); }
        });
        root.add(back).colspan(PAGE_NAMES.length).padTop(10).row();
        stage.addActor(root);
    }

    private void buildTabs(Table root, Skin skin) {
        Table tabs = new Table();
        for (int i = 0; i < PAGE_NAMES.length; i++) {
            final int idx = i;
            TextButton tab = new TextButton(PAGE_NAMES[i], skin,
                    i == activePage ? "green" : "brown");
            tab.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    activePage = idx; stage.clear(); buildUi();
                }
            });
            tabs.add(tab).width(198).height(44).padRight(6);
        }
        root.add(tabs).colspan(PAGE_NAMES.length).padBottom(10).row();
    }

    private void buildContent(Table root, Skin skin) {
        Table content = PAGE_KEYS[activePage].equals("minigame")
                ? buildMinigamePage(skin)
                : buildQuestPage(skin, PAGE_KEYS[activePage]);
        ScrollPane scroll = new ScrollPane(content, skin);
        scroll.setFadeScrollBars(false);
        root.add(scroll).expand().fill().colspan(PAGE_NAMES.length).row();
    }

    private Table buildQuestPage(Skin skin, String pageKey) {
        Table page = new Table(); page.defaults().fillX().padBottom(8);

        // PHASE1: QuestService.getQuestsByCategory(pageKey)
        List<?> quests = null;
        try {
            quests = ServiceLocator.getInstance().getQuestService()
                    .filterByPage(pageKey);
        } catch (Exception ignored) {}

        if (quests == null || quests.isEmpty()) {
            page.add(new Label("No quests found.", skin)).padTop(20);
            return page;
        }
        for (Object q : quests) buildQuestRow(page, skin, q);
        return page;
    }

    private void buildQuestRow(Table page, Skin skin, Object q) {
        Table row = new Table(skin);
        row.setBackground("image_ui_dialog_asset_inner_bkgd_10");
        row.pad(10);

        String name    = getField(q, "getName",        "Quest");
        String desc    = getField(q, "getDescription", "");
        int    target  = getIntField(q, "getTarget",   1);
        int    current = getIntField(q, "getCurrent",  0);
        String reward  = getField(q, "getRewardDesc",  "Reward");
        boolean done   = current >= target;

        Label nameLbl = new Label(name, skin, "medium");
        nameLbl.setColor(done ? Color.GREEN : Color.WHITE);
        row.add(nameLbl).expandX().left().row();

        if (!desc.isEmpty()) {
            Label dl = new Label(desc, skin); dl.setColor(Color.LIGHT_GRAY); dl.setWrap(true);
            row.add(dl).expandX().left().padBottom(4).row();
        }

        ProgressBar pb = new ProgressBar(0, target, 1, false, skin);
        pb.setValue(current);
        row.add(pb).fillX().padBottom(3).row();

        Table bot = new Table();
        Label progLbl = new Label(current + "/" + target, skin);
        progLbl.setColor(Color.GOLD);
        Label rwdLbl  = new Label("Reward: " + reward, skin);
        rwdLbl.setColor(Color.CYAN);
        bot.add(progLbl).expandX().left();
        bot.add(rwdLbl).right();
        row.add(bot).fillX().row();
        page.add(row).row();
    }

    private Table buildMinigamePage(Skin skin) {
        Table page = new Table(); page.defaults().fillX().padBottom(10);
        String[][] mgs = {
            {"vasebreaker", "Vasebreaker"},
            {"bowling",     "Walnut Bowling"},
            {"izombie",     "I, Zombie"},
            {"beghouled",   "Beghouled ⭐"},
            {"zombotany",   "Zombotany ⭐"},
        };
        for (String[] mg : mgs) buildMinigameRow(page, skin, mg[0], mg[1]);
        return page;
    }

    private void buildMinigameRow(Table page, Skin skin, String key, String name) {
        Table row = new Table(skin);
        row.setBackground("image_ui_dialog_asset_inner_bkgd_10"); row.pad(10);
        row.add(new Label(name, skin, "medium")).expandX().left();
        for (int lvl = 1; lvl <= 3; lvl++) {
            // PHASE1: MinigameService.isLevelUnlocked(key, lvl)
            boolean unlocked = lvl == 1;
            final int lv = lvl;
            TextButton btn = new TextButton("Level " + lvl, skin, unlocked ? "brown" : "default");
            btn.setDisabled(!unlocked);
            btn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    if (unlocked) launchMinigame(key, lv);
                }
            });
            row.add(btn).width(115).padLeft(6);
        }
        page.add(row).row();
    }

    private void launchMinigame(String key, int level) {
        // PHASE1: MinigameService.startMinigame(key, level)
        // TODO: صفحه مینی‌گیم مربوطه را باز کن
    }

    // ─── Reflection-safe getters ──────────────────────────────────────────────
    private String getField(Object o, String method, String def) {
        try { Object r = o.getClass().getMethod(method).invoke(o);
              return r != null ? r.toString() : def; }
        catch (Exception e) { return def; }
    }
    private int getIntField(Object o, String method, int def) {
        try { Object r = o.getClass().getMethod(method).invoke(o);
              return r instanceof Number ? ((Number) r).intValue() : def; }
        catch (Exception e) { return def; }
    }

    @Override public void render(float delta) {
        ScreenUtils.clear(0.06f, 0.04f, 0.08f, 1);
        stage.act(delta); stage.draw();
    }
    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void dispose() { if (stage != null) stage.dispose(); }
}
