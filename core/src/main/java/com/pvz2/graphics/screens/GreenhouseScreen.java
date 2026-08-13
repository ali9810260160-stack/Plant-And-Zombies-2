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
import com.pvz2.graphics.assets.AssetIds;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.model.AppState;
import com.pvz2.model.Greenhouse;
import com.pvz2.model.Pot;
import com.pvz2.model.User;
import com.pvz2.service.GreenhouseService;
import pvz.skin.BorderedTable;

import java.time.ZoneId;

/**
 * صفحه گلخانه — مطابق تصویر ۸ سند فاز دوم: grid کارت‌های گلدان روی یک پنل،
 * نوار بالا با سکه/الماس، هر گلدان یا خالی/در حال رشد (با نشان تایمر و
 * هزینه‌ی تسریع با الماس) یا آماده‌ی برداشت یا قفل (با قیمت باز کردن).
 * <p>
 * منطق واقعی (buyPot/plantPot/collectPot/growPot) از {@link GreenhouseService}
 * فاز یک می‌آید — چیزی دوباره پیاده‌سازی نشده.
 */
public class GreenhouseScreen extends BaseScreen {

    private Stage stage;

    public GreenhouseScreen(PVZApplication game) { super(game); }

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

        root.add(buildHeader(skin)).fillX().padBottom(14).row();
        ScrollPane potScroll = new ScrollPane(buildPotPanel(skin), skin);
        potScroll.setFadeScrollBars(false);
        root.add(potScroll).width(1000).height(430).padBottom(14).row();

        TextButton back = new TextButton("Back", skin, "brown");
        back.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { goTo(ScreenId.MAIN_MENU); }
        });
        root.add(back).size(140, 60);
        stage.addActor(root);
    }

    private Table buildHeader(Skin skin) {
        Table hdr = new Table(skin);
        hdr.setBackground("image_ui_dialog_asset_inner_bkgd_10");
        hdr.pad(6, 14, 6, 14);

        Label title = new Label("Greenhouse", skin, "medium");
        hdr.add(title).expandX().left();

        User user = AppState.getInstance().getCurrentUser();
        long coins = user != null ? user.getCoins() : 0;
        int gems = user != null ? user.getGems() : 0;

        if (!AssetIds.ICON_COIN.isEmpty()) {
            hdr.add(new Image(GameAssets.getInstance().region(AssetIds.ICON_COIN))).size(24).padRight(4);
        }
        Label coinLbl = new Label(String.valueOf(coins), skin);
        coinLbl.setColor(Color.GOLD);
        hdr.add(coinLbl).padRight(16);

        if (!AssetIds.ICON_GEM.isEmpty()) {
            hdr.add(new Image(GameAssets.getInstance().region(AssetIds.ICON_GEM))).size(24).padRight(4);
        }
        Label gemLbl = new Label(String.valueOf(gems), skin);
        gemLbl.setColor(Color.CYAN);
        hdr.add(gemLbl).padRight(16);

        TextButton shopBtn = new TextButton("Shop", skin, "purple");
        shopBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { goTo(ScreenId.SHOP); }
        });
        hdr.add(shopBtn);
        return hdr;
    }

    private BorderedTable buildPotPanel(Skin skin) {
        BorderedTable panel = new BorderedTable();
        User user = AppState.getInstance().getCurrentUser();
        GreenhouseService gs = ServiceLocator.getInstance().getGreenhouseService();

        Greenhouse greenhouse;
        try {
            greenhouse = gs.getOrCreateGreenhouse(user);
        } catch (Exception e) {
            panel.add(new Label("Error loading greenhouse: " + e.getMessage(), skin, "default"));
            return panel;
        }

        Pot[][] pots = greenhouse.getPots();
        if (pots == null) return panel;

        for (Pot[] rowPots : pots) {
            for (Pot pot : rowPots) {
                if (pot == null) continue;
                panel.add(buildPotCell(skin, pot, user, gs)).size(160, 155).pad(5);
            }
            panel.row();
        }
        return panel;
    }

    private Table buildPotCell(Skin skin, Pot pot, User user, GreenhouseService gs) {
        Table cell = new Table(skin);
        cell.setBackground("image_ui_dialog_asset_inner_bkgd_10");
        cell.pad(8);

        final int px = pot.getX(), py = pot.getY();

        if (pot.isLocked()) {
            buildLockedCell(cell, skin, px, py, user, gs);
        } else if (isReady(pot)) {
            buildReadyCell(cell, skin, pot, px, py, user, gs);
        } else if (isEmpty(pot)) {
            buildEmptyCell(cell, skin, px, py, user, gs);
        } else {
            buildGrowingCell(cell, skin, pot, px, py, user, gs);
        }
        return cell;
    }

    private void buildLockedCell(Table cell, Skin skin, int px, int py, User user, GreenhouseService gs) {
        if (!AssetIds.ICON_LOCK.isEmpty()) {
            cell.add(new Image(GameAssets.getInstance().region(AssetIds.ICON_LOCK))).size(48).padBottom(8).row();
        } else {
            cell.add(new Label("Locked", skin, "default")).padBottom(8).row();
        }
        Label cost = new Label("2000 Coins", skin, "default");
        cost.setColor(Color.GOLD);
        cell.add(cost).padBottom(8).row();

        TextButton btn = new TextButton("Unlock", skin, "green");
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                try { gs.buyPot(user, px, py); showToast(ToastActor.success("Pot unlocked!")); }
                catch (Exception ex) { showToast(ToastActor.error(ex.getMessage())); }
                refresh();
            }
        });
        cell.add(btn).width(130).height(38);
    }

    private void buildEmptyCell(Table cell, Skin skin, int px, int py, User user, GreenhouseService gs) {
        if (!AssetIds.ICON_POT_EMPTY.isEmpty()) {
            cell.add(new Image(GameAssets.getInstance().region(AssetIds.ICON_POT_EMPTY))).size(48).padBottom(8).row();
        } else {
            cell.add().height(48).padBottom(8).row();
        }
        Label empty = new Label("Empty Pot", skin, "default");
        empty.setColor(Color.LIGHT_GRAY);
        cell.add(empty).padBottom(8).row();

        TextButton btn = new TextButton("Plant", skin, "brown");
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                try { gs.plantPot(user, px, py); showToast(ToastActor.success("Plant seeded!")); }
                catch (Exception ex) { showToast(ToastActor.error(ex.getMessage())); }
                refresh();
            }
        });
        cell.add(btn).width(120).height(38);
    }

    private void buildGrowingCell(Table cell, Skin skin, Pot pot, int px, int py, User user, GreenhouseService gs) {
        cell.add().height(48).padBottom(4).row();

        String plantName = getPlantName(pot);
        Label nameLbl = new Label(plantName, skin, "default");
        cell.add(nameLbl).padBottom(4).row();

        double hoursLeft = getRemainingHours(pot);
        String timerStr = hoursLeft > 0 ? formatTime(hoursLeft) : "Growing...";
        Label timer = new Label(timerStr, skin, "default");
        timer.setFontScale(0.85f);
        timer.setColor(Color.LIGHT_GRAY);
        cell.add(timer).padBottom(6).row();

        int gemCost = (int) Math.ceil(Math.max(1, hoursLeft));
        Table speedRow = new Table();
        if (!AssetIds.ICON_GEM.isEmpty()) {
            speedRow.add(new Image(GameAssets.getInstance().region(AssetIds.ICON_GEM))).size(18).padRight(3);
        }
        TextButton speedBtn = new TextButton(String.valueOf(gemCost), skin, "brown");
        speedBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                try { gs.growPot(user, px, py); showToast(ToastActor.success("Growth sped up (-" + gemCost + " gems)")); }
                catch (Exception ex) { showToast(ToastActor.error(ex.getMessage())); }
                refresh();
            }
        });
        speedRow.add(speedBtn).width(90).height(34);
        cell.add(speedRow);
    }

    private void buildReadyCell(Table cell, Skin skin, Pot pot, int px, int py, User user, GreenhouseService gs) {
        if (!AssetIds.ICON_POT_READY_GLOW.isEmpty()) {
            cell.add(new Image(GameAssets.getInstance().region(AssetIds.ICON_POT_READY_GLOW))).size(48).padBottom(4).row();
        } else {
            cell.add().height(48).padBottom(4).row();
        }

        Label readyLbl = new Label("Ready to Harvest!", skin, "default");
        readyLbl.setColor(Color.GREEN);
        cell.add(readyLbl).padBottom(4).row();

        String plantName = getPlantName(pot);
        cell.add(new Label(plantName, skin, "default")).padBottom(8).row();

        TextButton btn = new TextButton("Harvest", skin, "green");
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                try {
                    String reward = gs.collectPot(user, px, py);
                    showToast(ToastActor.success("Harvested! " + (reward != null ? reward : "")));
                } catch (Exception ex) { showToast(ToastActor.error(ex.getMessage())); }
                refresh();
            }
        });
        cell.add(btn).width(130).height(38);
    }

    // ─── Pot helper methods ────────────────────────────────────────────────

    private boolean isEmpty(Pot pot) {
        try { return pot.getPlantType() == null; } catch (Exception e) { return true; }
    }

    private boolean isReady(Pot pot) {
        try { return pot.isReady(); }
        catch (Exception e) { return getRemainingHours(pot) <= 0 && !isEmpty(pot); }
    }

    private double getRemainingHours(Pot pot) {
        try { return pot.getRemainingHours(); } catch (Exception ignored) { }
        try {
            long plantedAt = pot.getPlantedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            double growHours = pot.getGrowthHours();
            double elapsedH = (System.currentTimeMillis() - plantedAt) / 3_600_000.0;
            return Math.max(0, growHours - elapsedH);
        } catch (Exception e) { return 0; }
    }

    private String getPlantName(Pot pot) {
        try {
            Object t = pot.getPlantType();
            if (t == null) return "Plant";
            return t.toString();
        } catch (Exception e) { return "Plant"; }
    }

    private String formatTime(double hours) {
        int h = (int) hours;
        int m = (int) Math.round((hours - h) * 60);
        return h + "h " + m + "m";
    }

    private void refresh() { stage.clear(); buildUi(); }

    private void showToast(ToastActor t) {
        t.setPosition(GameConstants.VIEWPORT_WIDTH / 2f - t.getWidth() * 0.5f, 80);
        stage.addActor(t);
    }

    @Override
    public void render(float delta) {
        GameAssets.getInstance().update();
        ScreenUtils.clear(0.05f, 0.12f, 0.05f, 1);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void dispose() { if (stage != null) stage.dispose(); }
}
