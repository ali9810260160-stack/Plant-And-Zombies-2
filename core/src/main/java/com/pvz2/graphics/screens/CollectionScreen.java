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
import com.pvz2.graphics.assets.GameAssets;

/**
 * صفحه کلکسیون — گیاهان و زامبی‌ها.
 * دو تب: گیاهان (با کارت‌های PlantCardActor) و زامبی‌ها.
 */
public class CollectionScreen extends BaseScreen {

    private Stage stage;
    private boolean plantsTab = true;

    public CollectionScreen(PVZApplication game) { super(game); }

    @Override public void show() {
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        buildUi();
    }

    private void buildUi() {
        Skin skin = GameAssets.getInstance().getSkin();
        Table root = new Table(skin); root.setFillParent(true); root.pad(14);

        Label title = new Label("📚 Collection", skin, "big");
        title.setColor(Color.YELLOW);
        root.add(title).colspan(2).padBottom(12).row();

        // تب‌ها
        Table tabs = new Table();
        TextButton ptab = new TextButton("🌱 Plants", skin, plantsTab ? "green" : "brown");
        TextButton ztab = new TextButton("🧟 Zombies", skin, plantsTab ? "brown" : "green");
        ptab.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                plantsTab = true; rebuildUi();
            }
        });
        ztab.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                plantsTab = false; rebuildUi();
            }
        });
        tabs.add(ptab).width(200).height(44).padRight(10);
        tabs.add(ztab).width(200).height(44);
        root.add(tabs).colspan(2).padBottom(10).row();

        // محتوا
        Table content = plantsTab ? buildPlantGrid(skin) : buildZombieGrid(skin);
        root.add(new ScrollPane(content, skin)).expand().fill().colspan(2).row();

        TextButton back = new TextButton("← Back", skin);
        back.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { goTo(ScreenId.MAIN_MENU); }
        });
        root.add(back).colspan(2).padTop(10).row();
        stage.addActor(root);
    }

    private Table buildPlantGrid(Skin skin) {
        Table grid = new Table(); grid.defaults().pad(6);
        java.util.List<GameFacade.PlantEntry> all = facade().getAllPlants();
        int col = 0;
        for (GameFacade.PlantEntry e : all) {
            int cost = e.stats != null ? e.stats.getSunCost() : 100;
            PlantCardActor card = new PlantCardActor(e.type, cost, skin);
            card.setLocked(!e.unlocked);
            card.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e2, float x, float y) {
                    showPlantDetail(skin, e);
                }
            });
            grid.add(card).size(GameConstants.CARD_W, GameConstants.CARD_H);
            if (++col % 9 == 0) grid.row();
        }
        return grid;
    }

    private Table buildZombieGrid(Skin skin) {
        Table grid = new Table(); grid.defaults().pad(8);
        java.util.List<String> seen = facade().getSeenZombies();
        com.pvz2.model.enums.ZombieType[] all = com.pvz2.model.enums.ZombieType.values();
        int col = 0;
        for (com.pvz2.model.enums.ZombieType zt : all) {
            boolean hasSeen = seen.contains(zt.name());
            Table cell = buildZombieCell(skin, zt.name(), hasSeen);
            grid.add(cell).size(100, 120);
            if (++col % 8 == 0) grid.row();
        }
        return grid;
    }

    private Table buildZombieCell(Skin skin, String name, boolean seen) {
        Table cell = new Table(skin);
        cell.setBackground("image_ui_dialog_asset_inner_bkgd_10");
        if (seen) {
            cell.add(new Label(name.toLowerCase().replace("_", " "), skin, "default"))
                    .expand().center();
        } else {
            Label q = new Label("?", skin, "big"); q.setColor(Color.DARK_GRAY);
            cell.add(q).expand().center();
        }
        return cell;
    }

    private void showPlantDetail(Skin skin, GameFacade.PlantEntry e) {
        Dialog dlg = new Dialog("Plant Info", skin) {
            @Override protected void result(Object obj) { hide(); }
        };
        String info = "Type: " + e.type.name().toLowerCase() + "\n"
                + "Cost: " + (e.stats != null ? e.stats.getSunCost() : "?") + " ☀\n"
                + "HP: " + (e.stats != null ? e.stats.getBaseHp() : "?") + "\n"
                + "Family: " + (e.stats != null ? e.stats.getFamily() : "?") + "\n"
                + "Status: " + (e.unlocked ? "✅ Unlocked" : "🔒 Locked");
        dlg.text(info);
        dlg.button("Close", true);
        dlg.show(stage);
    }

    private void rebuildUi() { stage.clear(); buildUi(); }

    @Override public void render(float delta) {
        ScreenUtils.clear(0.06f, 0.04f, 0.08f, 1);
        stage.act(delta); stage.draw();
    }
    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void dispose() { if (stage != null) stage.dispose(); }
}
