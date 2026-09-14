package com.pvz2.graphics.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.pvz2.PVZApplication;
import com.pvz2.graphics.*;
import com.pvz2.graphics.actors.PauseOverlay;
import com.pvz2.graphics.actors.PlantSelectCardActor;
import com.pvz2.graphics.actors.ToastActor;
import com.pvz2.graphics.assets.CollectionAssetPaths;
import com.pvz2.graphics.assets.CollectionAssets;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.graphics.assets.PlantSelectAssetPaths;
import com.pvz2.model.User;
import com.pvz2.model.enums.PlantType;

import java.util.ArrayList;
import java.util.List;

/**
 * صفحه‌ی انتخاب گیاه (Seed Selection) — بازطراحی مطابق تصویر مرجع
 * {@code seed-selection-screen.png}.
 *
 * <p>ساختار:
 * <ul>
 *   <li>پس‌زمینه‌ی تمام‌صفحه ({@code background.jpg})</li>
 *   <li>هدر: نوار سکه/الماس (با دکمه‌ی خرید) + دکمه‌ی pause</li>
 *   <li>ستون باریک چپ: ۸ اسلات گیاه انتخاب‌شده (قابل‌اسکرول عمودی)</li>
 *   <li>پنل مرکزی: باکس ثابت جزئیات گیاه (تصویر/توضیح/ارتقا/boost) بالا +
 *       گرید قابل‌اسکرول انتخاب گیاه پایین</li>
 *   <li>پایین صفحه: دکمه‌ی Refresh (چپ) و Let's Rock! (راست)</li>
 * </ul>
 *
 * <p><b>منطق دو-کلیکی:</b> کلیک اول روی یک کارت در گرید → آن گیاه
 * «highlight» می‌شود (باکس جزئیات بالا آپدیت می‌شود + قاب سبز {@code select.png}
 * دور کارتش می‌افتد). کلیک دوم (روی همون کارت highlight شده) → به اولین
 * اسلات خالی اضافه می‌شود. کلیک روی کارت داخل یک اسلات → آن گیاه از اسلات
 * حذف می‌شود.
 */
public class PlantSelectScreen extends BaseScreen {

    private static final int GRID_COLUMNS = 5;

    private Stage stage;
    private Skin skin;

    private int maxSlots;
    /** اسلات‌های قابلِ استفاده (کلِ اسلات‌ها منهایِ اسلات‌های قفل‌شده‌ی مرحله). */
    private int usableSlots;
    private List<GameFacade.PlantEntry> allPlants;
    private final List<PlantType> chosenSlots = new ArrayList<>();
    private PlantType highlighted;

    private PauseOverlay pauseOverlay;
    private boolean paused;

    public PlantSelectScreen(PVZApplication game) { super(game); }

    @Override
    public void show() {
        maxSlots = GameConstants.MAX_PLANT_SLOTS;
        // مرحله‌ی ویژه‌ی Locked Plants دو اسلات را قفل می‌کند (فقط ۶ گیاه انتخاب‌پذیر).
        int lockedSlots = 0;
        try {
            lockedSlots = facade().getLockedPlantSlots(game.getCurrentChapter(), game.getCurrentLevel());
        } catch (Exception ignored) { }
        usableSlots = Math.max(1, maxSlots - lockedSlots);
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        skin = GameAssets.getInstance().getSkin();

        loadData();
        buildUi();
    }

    private void loadData() {
        allPlants = facade().getAllPlants();
        chosenSlots.clear();
        if (highlighted == null) {
            for (GameFacade.PlantEntry e : allPlants) {
                if (e.unlocked) { highlighted = e.type; break; }
            }
        }
    }

    private void rebuildUi() { stage.clear(); buildUi(); }

    // =========================================================================
    //  ROOT
    // =========================================================================

    private void buildUi() {
        CollectionAssets assets = CollectionAssets.getInstance();

        TextureRegion bgRegion = assets.region(PlantSelectAssetPaths.BACKGROUND);
        Image bg = bgRegion != null ? new Image(bgRegion) : new Image(GameAssets.getInstance().getWhiteRegion());
        bg.setFillParent(true);
        bg.setScaling(com.badlogic.gdx.utils.Scaling.fill);
        if (bgRegion == null) bg.setColor(0.08f, 0.1f, 0.22f, 1f);
        stage.addActor(bg);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        root.add(buildHeader()).growX().height(70f).padTop(6).row();

        Table middle = new Table();
        middle.add(buildSlotColumn()).width(96f).growY().padLeft(8).padRight(4);
        middle.add(buildCenterPanel()).grow().padLeft(4).padRight(8);
        root.add(middle).grow().row();

        root.add(buildBottomBar()).growX().height(70f).padBottom(6);

        if (paused) showPauseOverlay();
    }

    // =========================================================================
    //  HEADER
    // =========================================================================

    private Table buildHeader() {
        Table header = new Table();
        header.pad(4, 16, 0, 16);

        Table right = new Table();
        right.add(buildCurrencyBar(PlantSelectAssetPaths.GEM_BUY_BAR,
                () -> String.valueOf(currentUser() != null ? currentUser().getGems() : 0))).padRight(10);
        right.add(buildCurrencyBar(PlantSelectAssetPaths.COIN_BUY_BAR,
                () -> String.valueOf(currentUser() != null ? currentUser().getCoins() : 0))).padRight(16);

        Table pauseBtn = new Table();
        TextureRegion pauseRegion = CollectionAssets.getInstance().region(PlantSelectAssetPaths.PAUSE_BUTTON);
        Image pauseImg = pauseRegion != null ? new Image(pauseRegion) : new Image(GameAssets.getInstance().getWhiteRegion());
        pauseBtn.add(pauseImg).size(52f);
        pauseBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { togglePause(); }
        });
        right.add(pauseBtn);

        header.add(right).right().expandX();
        return header;
    }

    private User currentUser() { return facade().getCurrentUser(); }

    private interface StringSupplier { String get(); }

    private Stack buildCurrencyBar(String barAsset, StringSupplier valueSupplier) {
        Stack stack = new Stack();
        TextureRegion barRegion = CollectionAssets.getInstance().region(barAsset);
        Image bar = barRegion != null ? new Image(barRegion) : new Image(GameAssets.getInstance().getWhiteRegion());
        stack.add(bar);

        Table overlay = new Table();
        overlay.right();
        Label label = new Label(valueSupplier.get(), skin, "default");
        label.setColor(Color.WHITE);
        overlay.add(label).padRight(46f);
        stack.add(overlay);
        return stack;
    }

    // =========================================================================
    //  LEFT SLOT COLUMN (۸ اسلات)
    // =========================================================================

    private Table buildSlotColumn() {
        Table wrap = new Table();

        Table slotsTable = new Table();
        slotsTable.defaults().pad(4);
        for (int i = 0; i < maxSlots; i++) {
            slotsTable.add(buildSlotCell(i)).size(80f, 104f).row();
        }
        ScrollPane scroll = new ScrollPane(slotsTable, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        wrap.add(scroll).grow();
        return wrap;
    }

    private Actor buildSlotCell(int index) {
        // اسلات‌های قفل‌شده‌ی مرحله (آخرین اسلات‌ها) — غیرقابلِ استفاده.
        if (index >= usableSlots) {
            return buildLockedSlotCell();
        }
        if (index >= chosenSlots.size()) {
            Table empty = new Table(skin);
            if (skin.has("image_ui_dialog_asset_inner_bkgd_10", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                empty.setBackground("image_ui_dialog_asset_inner_bkgd_10");
            } else {
                empty.setBackground(new TextureRegionDrawable(GameAssets.getInstance().getWhiteRegion()));
                empty.setColor(0f, 0f, 0f, 0.28f);
            }
            return empty;
        }

        PlantType type = chosenSlots.get(index);
        GameFacade.PlantEntry entry = findEntry(type);
        PlantSelectCardActor card = new PlantSelectCardActor(type, skin, false);
        card.setLevel(entry != null ? entry.level : 0);
        card.setBoosted(entry != null && entry.boosted);
        card.setHighlighted(true); // اسلات‌ها همیشه قاب سبز دارند (نشانه‌ی «انتخاب‌شده»)
        card.onClick(() -> {
            chosenSlots.remove(type);
            rebuildUi();
        });
        return card;
    }

    /** اسلاتِ قفل‌شده‌ی مرحله: پس‌زمینه‌ی تیره + آیکونِ قفل (غیرقابلِ کلیک). */
    private Actor buildLockedSlotCell() {
        Stack stack = new Stack();
        Table bg = new Table(skin);
        if (skin.has("image_ui_dialog_asset_inner_bkgd_10", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            bg.setBackground("image_ui_dialog_asset_inner_bkgd_10");
        } else {
            bg.setBackground(new TextureRegionDrawable(GameAssets.getInstance().getWhiteRegion()));
        }
        stack.add(bg);

        Image dim = new Image(GameAssets.getInstance().getWhiteRegion());
        dim.setColor(0.06f, 0.06f, 0.08f, 0.60f);
        stack.add(dim);

        TextureRegion lockRegion = CollectionAssets.getInstance().region(PlantSelectAssetPaths.LOCK_GOLD);
        if (lockRegion != null) {
            Table lockWrap = new Table();
            lockWrap.add(new Image(lockRegion)).size(42f);
            stack.add(lockWrap);
        }
        stack.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        return stack;
    }

    // =========================================================================
    //  CENTER PANEL (جزئیات ثابت + گرید اسکرول)
    // =========================================================================

    private Table buildCenterPanel() {
        Table panel = new Table(skin);
        if (skin.has("image_ui_dialog_asset_inner_bkgd_10", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            panel.setBackground("image_ui_dialog_asset_inner_bkgd_10");
        } else {
            panel.setBackground(new TextureRegionDrawable(GameAssets.getInstance().getWhiteRegion()));
            panel.setColor(0.22f, 0.16f, 0.08f, 0.92f);
        }
        panel.pad(10);

        panel.add(buildDetailBox()).growX().height(170f).padBottom(8).row();
        panel.add(buildPlantGridScroll()).grow().row();
        return panel;
    }

    // ─── باکس جزئیات ثابت (بالا) ─────────────────────────────────────────────

    private Table buildDetailBox() {
        Table box = new Table();
        box.setBackground(new TextureRegionDrawable(GameAssets.getInstance().getWhiteRegion()));
        box.setColor(0.94f, 0.92f, 0.82f, 1f);
        box.pad(10);

        GameFacade.PlantEntry entry = findEntry(highlighted);
        if (entry == null) {
            Label placeholder = new Label("Select a plant below", skin, "default");
            placeholder.setColor(Color.DARK_GRAY);
            box.add(placeholder).expand();
            return box;
        }

        TextureRegion plantRegion = CollectionAssets.getInstance().region(CollectionAssetPaths.plantPath(entry.type));
        Image plantImg = plantRegion != null ? new Image(plantRegion) : new Image(GameAssets.getInstance().getWhiteRegion());
        box.add(plantImg).size(120f, 140f).padRight(14);

        Table mid = new Table();
        mid.top().left();
        Label name = new Label(displayName(entry.type.name()), skin, "big");
        name.setColor(new Color(0.15f, 0.1f, 0.02f, 1f));
        mid.add(name).left().padBottom(6).row();

        String desc = entry.stats != null ? entry.stats.getDescription() : "";
        Label descLabel = new Label(desc != null ? desc : "", skin, "default");
        descLabel.setWrap(true);
        descLabel.setColor(new Color(0.2f, 0.15f, 0.05f, 1f));
        mid.add(descLabel).width(300f).left().padBottom(6).row();

        Label lvl = new Label("Level " + (entry.level + 1) + (entry.boosted ? "  (Boosted)" : ""), skin, "default");
        lvl.setColor(entry.boosted ? new Color(0.7f, 0.5f, 0f, 1f) : Color.DARK_GRAY);
        mid.add(lvl).left();

        box.add(mid).growX().top();

        Table actions = new Table();
        actions.top();

        boolean maxLevel = facade().isPlantMaxLevel(entry.type);
        int upgradeCost = facade().getUpgradeCostCoins(entry.type);
        TextButton upgradeBtn = new TextButton(
                maxLevel ? "MAX LEVEL" : "UPGRADE  " + upgradeCost, skin, hasButtonStyle("green") ? "green" : "default");
        boolean canUpgrade = !maxLevel && currentUser() != null && currentUser().getCoins() >= upgradeCost;
        upgradeBtn.setColor(canUpgrade ? new Color(0.55f, 0.3f, 0.85f, 1f) : new Color(0.5f, 0.5f, 0.5f, 1f));
        if (canUpgrade) {
            upgradeBtn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) { doUpgrade(entry.type); }
            });
        }
        actions.add(upgradeBtn).width(150f).height(46f).padBottom(8).row();

        int boostCost = facade().getBoostCostGems();
        TextButton boostBtn = new TextButton(
                entry.boosted ? "BOOSTED" : "BOOST  " + boostCost, skin, hasButtonStyle("green") ? "green" : "default");
        boolean canBoost = !entry.boosted && currentUser() != null && currentUser().getGems() >= boostCost;
        boostBtn.setColor(entry.boosted ? new Color(0.45f, 0.45f, 0.45f, 1f) : new Color(0.25f, 0.7f, 0.3f, 1f));
        if (canBoost) {
            boostBtn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) { doBoost(entry.type); }
            });
        }
        actions.add(boostBtn).width(150f).height(46f);

        box.add(actions).padLeft(14).top();
        return box;
    }

    private boolean hasButtonStyle(String name) {
        try { return skin.has(name, TextButton.TextButtonStyle.class); } catch (Exception e) { return false; }
    }

    private void doUpgrade(PlantType type) {
        if (facade().upgradePlant(type)) {
            rebuildUi();
        } else {
            showToast(ToastActor.error("Not enough coins"));
        }
    }

    private void doBoost(PlantType type) {
        if (facade().boostPlant(type)) {
            rebuildUi();
        } else {
            showToast(ToastActor.error("Not enough gems"));
        }
    }

    // ─── گرید انتخاب (قابل‌اسکرول) ───────────────────────────────────────────

    private ScrollPane buildPlantGridScroll() {
        Table grid = new Table();
        grid.top();
        grid.defaults().pad(5);

        int col = 0;
        for (GameFacade.PlantEntry entry : allPlants) {
            PlantSelectCardActor card = new PlantSelectCardActor(entry.type, skin, true);
            card.setLocked(!entry.unlocked);
            card.setBoosted(entry.boosted);
            card.setLevel(entry.level);
            card.setSunCost(entry.stats != null ? entry.stats.getSunCost() : 0);
            boolean inSlot = chosenSlots.contains(entry.type);
            card.setHighlighted(inSlot || entry.type == highlighted);

            card.onClick(() -> onGridCardClicked(entry.type, entry.unlocked, inSlot));

            grid.add(card).size(PlantSelectCardActor.CARD_W, PlantSelectCardActor.CARD_H);
            if (++col % GRID_COLUMNS == 0) grid.row();
        }

        ScrollPane scroll = new ScrollPane(grid, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        return scroll;
    }

    private void onGridCardClicked(PlantType type, boolean unlocked, boolean alreadyInSlot) {
        if (!unlocked) {
            showToast(ToastActor.error("This plant is locked"));
            return;
        }
        if (alreadyInSlot) {
            // از قبل داخل یک اسلاته — برای حذف باید روی خودِ اسلات کلیک بشه
            highlighted = type;
            rebuildUi();
            return;
        }
        if (highlighted == type) {
            // کلیک دوم روی همون کارت highlight شده → اضافه به اسلات
            if (chosenSlots.size() >= usableSlots) {
                showToast(ToastActor.error("Max " + usableSlots + " plants allowed"));
                return;
            }
            chosenSlots.add(type);
            rebuildUi();
        } else {
            // کلیک اول → فقط highlight/نمایش جزئیات
            highlighted = type;
            rebuildUi();
        }
    }

    private GameFacade.PlantEntry findEntry(PlantType type) {
        if (type == null) return null;
        for (GameFacade.PlantEntry e : allPlants) if (e.type == type) return e;
        return null;
    }

    // =========================================================================
    //  BOTTOM BAR (Refresh + Let's Rock!)
    // =========================================================================

    private Table buildBottomBar() {
        Table bar = new Table();
        bar.pad(0, 16, 0, 16);

        TextButton refresh = new TextButton("\u21BB Refresh", skin, hasButtonStyle("brown") ? "brown" : "default");
        refresh.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                chosenSlots.clear();
                rebuildUi();
            }
        });
        bar.add(refresh).size(150f, 54f).left();

        bar.add().growX();

        boolean canStart = !chosenSlots.isEmpty();
        TextButton start = new TextButton("Let's Rock!", skin, hasButtonStyle("green") ? "green" : "default");
        start.setColor(canStart ? new Color(0.55f, 0.3f, 0.85f, 1f) : new Color(0.5f, 0.5f, 0.5f, 1f));
        if (canStart) {
            start.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) { handleStart(); }
            });
        }
        bar.add(start).size(200f, 54f).right();

        return bar;
    }

    private void handleStart() {
        if (chosenSlots.isEmpty()) {
            showToast(ToastActor.error("Select at least one plant"));
            return;
        }
        startGame(game.getCurrentChapter(), game.getCurrentLevel(), new ArrayList<>(chosenSlots));
    }

    // =========================================================================
    //  PAUSE
    // =========================================================================

    private void togglePause() {
        paused = !paused;
        if (paused) showPauseOverlay(); else hidePauseOverlay();
    }

    private void showPauseOverlay() {
        if (pauseOverlay == null) {
            pauseOverlay = new PauseOverlay(skin, new PauseOverlay.Listener() {
                @Override public void onResume() { togglePause(); }
                @Override public void onRestart() {
                    chosenSlots.clear();
                    paused = false;
                    rebuildUi();
                }
                @Override public void onQuit() { goTo(ScreenId.ADVENTURE); }
            });
        }
        stage.addActor(pauseOverlay);
    }

    private void hidePauseOverlay() {
        if (pauseOverlay != null) pauseOverlay.remove();
    }

    // =========================================================================

    private void showToast(ToastActor t) {
        t.setPosition(GameConstants.VIEWPORT_WIDTH / 2f - t.getWidth() * 0.5f, 90f);
        stage.addActor(t);
    }

    private String displayName(String enumName) {
        if (enumName == null) return "";
        String[] parts = enumName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    @Override
    public void render(float delta) {
        GameAssets.getInstance().update();
        ScreenUtils.clear(0.05f, 0.05f, 0.1f, 1);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void dispose() { if (stage != null) stage.dispose(); }
}
