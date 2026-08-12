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
import com.pvz2.graphics.actors.PlantCardActor;
import com.pvz2.graphics.actors.ToastActor;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.model.enums.PlantType;

import java.util.*;
import java.util.List;

public class PlantSelectScreen extends BaseScreen {

    private Stage stage;
    private Table selectedBar;
    private final List<PlantType> chosen = new ArrayList<>();
    private int maxSlots;

    public PlantSelectScreen(PVZApplication game) { super(game); }

    @Override
    public void show() {
        // PHASE1: maxSlots از تعریف مرحله
        maxSlots = GameConstants.MAX_PLANT_SLOTS;
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        buildUi();
    }

    private void buildUi() {
        Skin skin = GameAssets.getInstance().getSkin();
        Table root = new Table(skin);
        root.setFillParent(true);
        root.pad(12);

        addHeader(root, skin);
        addSplitContent(root, skin);
        addBottomBar(root, skin);
        stage.addActor(root);
    }

    private void addHeader(Table root, Skin skin) {
        Table hdr = new Table();
        Label title = new Label("Select Plants — Max " + maxSlots, skin, "big");
        title.setColor(Color.YELLOW);
        hdr.add(title).expandX().left();

        // فیلتر
        SelectBox<String> filter = new SelectBox<>(skin);
        filter.setItems("All", "Upgradeable", "Unlocked", "Locked");
        hdr.add(new Label("Filter: ", skin)).padRight(6);
        hdr.add(filter).width(140);
        root.add(hdr).fillX().padBottom(12).row();
    }

    private void addSplitContent(Table root, Skin skin) {
        Table left = buildPlantGrid(skin);
        ScrollPane scroll = new ScrollPane(left, skin);
        scroll.setFadeScrollBars(false);
        root.add(scroll).expand().fill().row();

        // نوار گیاهان انتخابی
        selectedBar = new Table(skin);
        selectedBar.setBackground("image_ui_dialog_asset_inner_bkgd_10");
        selectedBar.pad(6);
        refreshSelectedBar(skin);
        root.add(selectedBar).fillX().padTop(8).row();
    }

    private Table buildPlantGrid(Skin skin) {
        Table grid = new Table();
        grid.defaults().pad(5);

        List<GameFacade.PlantEntry> allPlants = facade().getAllPlants();
        int col = 0;
        for (GameFacade.PlantEntry entry : allPlants) {
            int cost = entry.stats != null ? entry.stats.getSunCost() : 100;
            PlantCardActor card = new PlantCardActor(entry.type, cost, skin);
            card.setLocked(!entry.unlocked);

            PlantType type = entry.type;
            card.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    if (!entry.unlocked) {
                        showToast(ToastActor.error("This plant is locked"));
                        return;
                    }
                    toggleChosen(type, skin);
                }
            });
            grid.add(card).size(GameConstants.CARD_W, GameConstants.CARD_H);
            if (++col % 8 == 0) grid.row();
        }
        return grid;
    }

    private void toggleChosen(PlantType type, Skin skin) {
        if (chosen.contains(type)) {
            chosen.remove(type);
        } else if (chosen.size() < maxSlots) {
            chosen.add(type);
        } else {
            showToast(ToastActor.error("Max " + maxSlots + " plants allowed"));
            return;
        }
        refreshSelectedBar(skin);
    }

    private void refreshSelectedBar(Skin skin) {
        selectedBar.clear();
        for (int i = 0; i < maxSlots; i++) {
            if (i < chosen.size()) {
                int cost = getCost(chosen.get(i));
                PlantCardActor card = new PlantCardActor(chosen.get(i), cost, skin);
                card.setSelected(true);
                PlantType t = chosen.get(i);
                card.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent e, float x, float y) {
                        chosen.remove(t);
                        refreshSelectedBar(skin);
                    }
                });
                selectedBar.add(card).size(GameConstants.CARD_W, GameConstants.CARD_H).pad(3);
            } else {
                Table empty = new Table(skin);
                empty.setBackground("image_ui_dialog_asset_inner_bkgd_10");
                selectedBar.add(empty).size(GameConstants.CARD_W, GameConstants.CARD_H).pad(3);
            }
        }
    }

    private int getCost(PlantType type) {
        try {
            com.pvz2.model.plants.PlantStats s =
                    com.pvz2.model.plants.PlantDataRegistry.getInstance().getStats(type);
            return s != null ? s.getSunCost() : 100;
        } catch (Exception e) { return 100; }
    }

    private void addBottomBar(Table root, Skin skin) {
        Table bar = new Table();
        TextButton back = new TextButton("← Back", skin);
        back.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { goTo(ScreenId.ADVENTURE); }
        });
        TextButton start = new TextButton("▶  Start Game", skin, "green");
        start.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { handleStart(skin); }
        });
        bar.add(back).padRight(20);
        bar.add(start).width(220).height(54);
        root.add(bar).padTop(8).row();
    }

    private void handleStart(Skin skin) {
        if (chosen.isEmpty()) {
            showToast(ToastActor.error("Select at least one plant"));
            return;
        }
        startGame(game.getCurrentChapter(), game.getCurrentLevel(), new ArrayList<>(chosen));
    }

    private void showToast(ToastActor t) {
        t.setPosition(640 - t.getWidth() * 0.5f, 300);
        stage.addActor(t);
    }

    @Override public void render(float delta) {
        ScreenUtils.clear(0.06f, 0.04f, 0.08f, 1);
        stage.act(delta); stage.draw();
    }
    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void dispose() { if (stage != null) stage.dispose(); }
}
