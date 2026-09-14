package com.pvz2.graphics.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import pvz.skin.BorderedTable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.pvz2.PVZApplication;
import com.pvz2.graphics.*;
import com.pvz2.graphics.actors.PamIdleActor;
import com.pvz2.graphics.actors.PlantAlmanacCardActor;
import com.pvz2.graphics.actors.ToastActor;
import com.pvz2.graphics.actors.ZombieAlmanacCardActor;
import com.pvz2.graphics.assets.CollectionAssetPaths;
import com.pvz2.graphics.assets.CollectionAssets;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.graphics.assets.AssetIds;
import com.pvz2.model.User;
import com.pvz2.model.enums.PlantFamily;
import com.pvz2.model.enums.PlantType;
import com.pvz2.model.zombies.ZombieStats;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * صفحه‌ی کلکسیون (Almanac) — مطابق تصاویر مرجع:
 * <ul>
 *   <li>هدر: تب گیاهان/زامبی‌ها (چپ) + سکه/الماس + دکمه بستن (راست)</li>
 *   <li>تب گیاهان: گرید کارت‌ها با سطح، نوار پیشرفت، قفل نقره‌ای، پس‌زمینه طلایی boost</li>
 *   <li>تب زامبی‌ها: قاب {@code ready.png} برای هرکدام — خالی اگر دیده نشده</li>
 *   <li>کلیک روی هر کارت → صفحه‌ی جزئیات (تصویر + آمار + توضیح + فلش قبلی/بعدی)</li>
 * </ul>
 *
 * <p>تصاویر خام از {@link CollectionAssets} (پوشه‌ی {@code assets/collection/…})
 * خونده می‌شن — نه از اطلس اصلی بازی. مسیرها در {@link CollectionAssetPaths} است.
 */
public class CollectionScreen extends BaseScreen {

    private enum Mode { GRID, DETAIL }

    private static final int GRID_COLUMNS = 9;

    private Stage stage;
    private Skin skin;

    private boolean plantsTab = true;
    private Mode mode = Mode.GRID;

    private List<GameFacade.PlantEntry> plantEntries;
    private List<GameFacade.ZombieEntry> zombieEntries;

    /** فقط ورودی‌های آنلاک/دیده‌شده — برای شمارشِ فوترِ «Collected». */
    private List<GameFacade.PlantEntry> unlockedPlants;
    private List<GameFacade.ZombieEntry> seenZombies;
    private int detailIndex;

    /** لیستی که ناوبریِ قبلی/بعدیِ جزئیاتِ گیاه روی آن انجام می‌شود (لیستِ فیلترشده). */
    private List<GameFacade.PlantEntry> detailPlantList = new ArrayList<>();

    // ─── وضعیتِ فیلترِ گیاهان (AA3) ─────────────────────────────────────────
    private final Set<PlantFamily> filterFamilies = new HashSet<>();
    private boolean filterLockedOnly     = false;
    private boolean filterUpgradableOnly = false;

    public CollectionScreen(PVZApplication game) { super(game); }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        skin = GameAssets.getInstance().getSkin();
        loadData();
        buildUi();
    }

    private void loadData() {
        plantEntries = facade().getAllPlants();
        zombieEntries = facade().getAllZombies();

        unlockedPlants = new ArrayList<>();
        for (GameFacade.PlantEntry e : plantEntries) if (e.unlocked) unlockedPlants.add(e);

        seenZombies = new ArrayList<>();
        for (GameFacade.ZombieEntry e : zombieEntries) if (e.seen) seenZombies.add(e);
    }

    private void rebuildUi() { stage.clear(); buildUi(); }

    // =========================================================================
    //  UI ROOT
    // =========================================================================

    private void buildUi() {
        // پس‌زمینه‌ی تِم‌دار به‌جای مشکیِ خالی (asset اختصاصیِ کلکسیون موجود نبود؛
        // از پنلِ کاغذیِ اسکین استفاده می‌شود تا با بقیه‌ی صفحه‌ها هماهنگ باشد).
        Image bg = new Image(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        bg.setScaling(com.badlogic.gdx.utils.Scaling.stretch);
        bg.setFillParent(true);
        bg.setColor(0.32f, 0.22f, 0.12f, 1f);   // تهِ رنگِ چوبیِ گرم
        bg.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        stage.addActor(bg);

        Table root = new Table();
        root.setFillParent(true);
        root.top();
        stage.addActor(root);

        root.add(buildHeader()).growX().height(78f).row();

        if (mode == Mode.GRID) {
            root.add(buildGridContent()).grow().row();
            root.add(buildCollectedFooter()).growX().height(34f).row();
        } else {
            root.add(buildDetailContent()).grow().row();
        }
    }

    /** نوارِ پایین: «Plants Collected: X of Y» / «Zombies Seen: X of Y». */
    private Table buildCollectedFooter() {
        Table footer = new Table();
        int have = plantsTab ? unlockedPlants.size() : seenZombies.size();
        int total = plantsTab ? plantEntries.size() : zombieEntries.size();
        String txt = plantsTab
                ? "Plants Collected: " + have + " of " + total
                : "Zombies Seen: " + have + " of " + total;
        Label lbl = new Label(txt, skin, "medium");
        lbl.setColor(Color.valueOf("ffe9a8"));
        footer.add(lbl).expandX().center().padBottom(6);
        return footer;
    }

    // =========================================================================
    //  HEADER (tabs + currency + close/back)
    // =========================================================================

    private Table buildHeader() {
        Table header = new Table();
        header.pad(8, 16, 4, 16);

        // ─── سمت چپ: تب‌ها (فقط در حالت گرید) یا دکمه‌ی برگشت (در جزئیات) ────
        Table left = new Table();
        if (mode == Mode.GRID) {
            left.add(buildTabButton(true)).padRight(6);
            left.add(buildTabButton(false));
            if (plantsTab) {
                TextButton filterBtn = new TextButton(
                        filtersActive() ? "Filter *" : "Filter", skin, "brown");
                filterBtn.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) { showFilterDialog(); }
                });
                left.add(filterBtn).height(46f).padLeft(16);
            }
        } else {
            left.add(buildIconButton(CollectionAssetPaths.BUTTON_BACK, 56f, this::backToGrid));
        }
        header.add(left).left().expandX();

        // ─── سمت راست: سکه/الماس + بستن ────────────────────────────────────
        Table right = new Table();
        right.add(buildCurrencyBadge(AssetIds.ICON_GEM, () -> String.valueOf(currentUser() != null ? currentUser().getGems() : 0)))
                .padRight(14);
        right.add(buildCurrencyBadge(AssetIds.ICON_COIN, () -> String.valueOf(currentUser() != null ? currentUser().getCoins() : 0)))
                .padRight(14);
        if (mode == Mode.GRID) {
            right.add(buildIconButton(CollectionAssetPaths.BUTTON_CLOSE_TAB, 44f, () -> goTo(ScreenId.MAIN_MENU)));
        }
        header.add(right).right();

        return header;
    }

    private User currentUser() { return facade().getCurrentUser(); }

    private Table buildTabButton(boolean isPlantsButton) {
        boolean active = (isPlantsButton == plantsTab);
        String path = isPlantsButton
                ? (active ? CollectionAssetPaths.TAB_PLANTS_ACTIVE : CollectionAssetPaths.TAB_PLANTS_DOWN)
                : (active ? CollectionAssetPaths.TAB_ZOMBIES_ACTIVE : CollectionAssetPaths.TAB_ZOMBIES_DOWN);

        Table cell = new Table();
        TextureRegion region = CollectionAssets.getInstance().region(path);
        Image img = region != null ? new Image(region) : new Image(GameAssets.getInstance().getWhiteRegion());
        if (region == null) img.setColor(isPlantsButton ? Color.FOREST : Color.PURPLE);
        cell.add(img).size(56f, active ? 74f : 56f);
        cell.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (plantsTab != isPlantsButton) { plantsTab = isPlantsButton; rebuildUi(); }
            }
        });
        return cell;
    }

    private Table buildIconButton(String assetPath, float size, Runnable onClick) {
        Table cell = new Table();
        TextureRegion region = CollectionAssets.getInstance().region(assetPath);
        Image img = region != null ? new Image(region) : new Image(GameAssets.getInstance().getWhiteRegion());
        cell.add(img).size(size);
        cell.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { onClick.run(); }
        });
        return cell;
    }

    private interface StringSupplier { String get(); }

    private Table buildCurrencyBadge(String pvzAssetId, StringSupplier valueSupplier) {
        Table t = new Table();
        TextureRegion icon = GameAssets.getInstance().region(pvzAssetId);
        t.add(new Image(icon)).size(32f).padRight(6f);
        Label label = new Label(valueSupplier.get(), skin, "default");
        label.setColor(Color.WHITE);
        t.add(label);
        return t;
    }

    // =========================================================================
    //  GRID CONTENT
    // =========================================================================

    private Table buildGridContent() {
        Table wrap = new Table();
        Table grid = new Table();
        grid.defaults().pad(6);

        if (plantsTab) {
            int col = 0;
            List<GameFacade.PlantEntry> visible = visiblePlants();
            for (GameFacade.PlantEntry e : visible) {
                PlantAlmanacCardActor card = new PlantAlmanacCardActor(e.type, skin);
                card.setLocked(!e.unlocked);
                card.setBoosted(e.boosted);
                card.setLevel(e.level);
                card.setSunCost(e.stats != null ? e.stats.getSunCost() : 0);
                // کارت‌های قفل هم قابلِ کلیک‌اند تا بتوان از صفحه‌ی جزئیات خریدشان (Y3)
                card.onClick(() -> openPlantDetail(e));
                grid.add(card).size(PlantAlmanacCardActor.CARD_W, PlantAlmanacCardActor.CARD_H);
                if (++col % GRID_COLUMNS == 0) grid.row();
            }
        } else {
            int col = 0;
            for (GameFacade.ZombieEntry e : zombieEntries) {
                ZombieAlmanacCardActor card = new ZombieAlmanacCardActor(e.type);
                card.setSeen(e.seen);
                if (e.seen) {
                    card.onClick(() -> openZombieDetail(e));
                }
                grid.add(card).size(ZombieAlmanacCardActor.CARD_W, ZombieAlmanacCardActor.CARD_H);
                if (++col % GRID_COLUMNS == 0) grid.row();
            }
        }

        ScrollPane scroll = new ScrollPane(grid, skin);
        scroll.setFadeScrollBars(false);
        wrap.add(scroll).grow().pad(4, 16, 16, 16);
        return wrap;
    }

    // =========================================================================
    //  DETAIL CONTENT
    // =========================================================================

    private void openPlantDetail(GameFacade.PlantEntry entry) {
        detailPlantList = visiblePlants();
        detailIndex = detailPlantList.indexOf(entry);
        if (detailIndex < 0) detailIndex = 0;
        mode = Mode.DETAIL;
        rebuildUi();
    }

    private void openZombieDetail(GameFacade.ZombieEntry entry) {
        detailIndex = seenZombies.indexOf(entry);
        if (detailIndex < 0) detailIndex = 0;
        mode = Mode.DETAIL;
        rebuildUi();
    }

    private void backToGrid() {
        mode = Mode.GRID;
        rebuildUi();
    }

    private Table buildDetailContent() {
        return plantsTab ? buildPlantDetail() : buildZombieDetail();
    }

    private Table buildPlantDetail() {
        if (detailPlantList.isEmpty()) return emptyDetailFallback("No plants to show.");
        if (detailIndex < 0 || detailIndex >= detailPlantList.size()) detailIndex = 0;
        GameFacade.PlantEntry entry = detailPlantList.get(detailIndex);

        Table panel = new Table();
        panel.pad(20);
        panel.setBackground(new TextureRegionDrawable(GameAssets.getInstance().getWhiteRegion()));
        panel.setColor(0.10f, 0.08f, 0.28f, 1f); // پس‌زمینه بنفش تیره (جایگزین موقت — asset اختصاصی موجود نبود)

        Label title = new Label(displayName(entry.type.name()), skin, "big");
        title.setColor(Color.WHITE);
        panel.add(title).colspan(2).left().padBottom(16).row();

        Table body = new Table();

        // ─── ستون چپ: انیمیشنِ idle + سطح/نوار پیشرفت + کیسه‌های بذر ─────────
        Table left = new Table();
        // V3: انیمیشنِ حالتِ بیکار به‌جای عکسِ ثابت (با fallback به همان عکس).
        TextureRegion plantRegion = CollectionAssets.getInstance().region(CollectionAssetPaths.plantPath(entry.type));
        PamIdleActor idle = PamIdleActor.forPlant(entry.type, plantRegion);
        left.add(idle).size(220f, 260f).row();

        if (entry.unlocked) {
            Label lvlLabel = new Label("Level " + (entry.level + 1), skin, "default");
            lvlLabel.setColor(Color.GOLD);
            left.add(lvlLabel).padTop(10).row();

            ProgressBar.ProgressBarStyle pbStyle = safeProgressBarStyle();
            if (pbStyle != null) {
                ProgressBar bar = new ProgressBar(0f, 1f, 0.001f, false, skin,
                        skin.has("level-bar", ProgressBar.ProgressBarStyle.class) ? "level-bar" : "default-horizontal");
                bar.setValue(entry.level / 3f);
                left.add(bar).width(220f).height(16f).padTop(6).row();
            }
            Label xpLabel = new Label(entry.level + " / 3", skin, "default");
            left.add(xpLabel).padTop(2).row();

            // T3: تعداد کیسه‌های بذرِ موجود + تعدادِ لازم برای ارتقای بعدی
            int have = facade().getSeedPacketCount(entry.type);
            Label seedLbl = new Label("Seed packets: " + have, skin, "default");
            seedLbl.setColor(Color.valueOf("ffe9a8"));
            left.add(seedLbl).padTop(8).row();
            if (entry.level < 3) {
                Label needLbl = new Label("Need " + facade().getUpgradeSeedCost(entry.type)
                        + " to upgrade", skin, "default");
                needLbl.setColor(Color.LIGHT_GRAY);
                needLbl.setFontScale(0.85f);
                left.add(needLbl).padTop(2);
            }
        } else {
            Label lockLbl = new Label("LOCKED", skin, "medium");
            lockLbl.setColor(Color.valueOf("c0c0c0"));
            left.add(lockLbl).padTop(10);
        }

        body.add(left).padRight(30).top();

        // ─── ستون راست: آمار + توضیحات ──────────────────────────────────────
        Table right = new Table();
        right.top().left();
        if (entry.stats != null) {
            addStatRow(right, CollectionAssetPaths.STAT_ICON_SUN_COST, "Sun Cost", String.valueOf(entry.stats.getSunCost()));
            addStatRow(right, CollectionAssetPaths.STAT_ICON_ARMING_TIME, "Recharge", String.format("%.1fs", entry.stats.getRechargeTime()));
            addStatRow(right, CollectionAssetPaths.STAT_ICON_FAMILY, "Family", displayName(entry.stats.getFamily() != null ? entry.stats.getFamily().name() : "-"));
            if (entry.stats.getActionIntervalSeconds() > 0 && entry.stats.getFamily() != null
                    && entry.stats.getFamily().name().contains("SUN")) {
                addStatRow(right, CollectionAssetPaths.STAT_ICON_SUN_PRODUCTION, "Sun Every",
                        String.format("%.0fs", entry.stats.getActionIntervalSeconds()));
            }
            if (entry.stats.getBaseAbility() != null && !entry.stats.getBaseAbility().isEmpty()) {
                addStatRow(right, CollectionAssetPaths.STAT_ICON_SPECIAL, "Special", entry.stats.getBaseAbility());
            }
            if (entry.stats.getPlantFoodEffect() != null && !entry.stats.getPlantFoodEffect().isEmpty()) {
                addStatRow(right, CollectionAssetPaths.STAT_ICON_PLANT_FOOD, "Plant Food", entry.stats.getPlantFoodEffect());
            }

            right.row();
            Label desc = new Label(entry.stats.getDescription() != null ? entry.stats.getDescription() : "", skin, "default");
            desc.setWrap(true);
            desc.setColor(Color.LIGHT_GRAY);
            right.add(desc).width(420f).padTop(14).left();
        }
        body.add(right).top();

        panel.add(body).colspan(2).row();

        // ─── دکمه‌ی اقدام: خرید (قفل) / ارتقا (آنلاک) / حداکثر ───────────────
        panel.add(buildPlantActionRow(entry)).colspan(2).padTop(16);

        Table root = new Table();
        root.add(navArrow(true, () -> stepDetail(-1))).expandY().left().pad(20);
        root.add(panel).grow();
        root.add(navArrow(false, () -> stepDetail(1))).expandY().right().pad(20);
        return root;
    }

    /** ردیفِ دکمه‌ی اقدام در جزئیاتِ گیاه: خرید (Y3) / ارتقا (X3) + خطا (Z3). */
    private Table buildPlantActionRow(GameFacade.PlantEntry entry) {
        Table actions = new Table();
        final PlantType type = entry.type;

        if (!entry.unlocked) {
            TextButton buy = new TextButton(
                    "Buy  (" + facade().getPlantUnlockCostCoins() + " coins)", skin, "green");
            buy.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    String err = facade().tryBuyPlant(type);
                    if (err != null) { showToast(ToastActor.error(err)); return; }
                    showToast(ToastActor.success("Purchased " + displayName(type.name()) + "!"));
                    reloadDetail(type);
                }
            });
            actions.add(buy).height(54f).width(360f);
        } else if (entry.level >= 3) {
            Label max = new Label("MAX LEVEL", skin, "medium");
            max.setColor(Color.GOLD);
            actions.add(max);
        } else {
            int coinCost = facade().getUpgradeCoinCostCollection(type);
            int seedCost = facade().getUpgradeSeedCost(type);
            TextButton up = new TextButton(
                    "Upgrade  (" + coinCost + " coins + " + seedCost + " seeds)", skin, "green");
            final int nextLevelForMsg = entry.level + 2; // نمایش 1..4 مطابقِ «Level x+1»
            up.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    String err = facade().tryUpgradePlantCollection(type);
                    if (err != null) { showToast(ToastActor.error(err)); return; }
                    showToast(ToastActor.success(
                            displayName(type.name()) + " upgraded to Level " + nextLevelForMsg + "!"));
                    reloadDetail(type);
                }
            });
            actions.add(up).height(54f).width(420f);
        }
        return actions;
    }

    /** بعد از خرید/ارتقا: داده را دوباره بخوان و روی همان گیاه در جزئیات بمان. */
    private void reloadDetail(PlantType type) {
        loadData();
        detailPlantList = visiblePlants();
        detailIndex = 0;
        for (int i = 0; i < detailPlantList.size(); i++) {
            if (detailPlantList.get(i).type == type) { detailIndex = i; break; }
        }
        rebuildUi();
    }

    private Table buildZombieDetail() {
        if (seenZombies.isEmpty()) return emptyDetailFallback("No zombies encountered yet.");
        GameFacade.ZombieEntry entry = seenZombies.get(detailIndex);
        ZombieStats stats = entry.stats;

        Table panel = new Table();
        panel.pad(20);
        panel.setBackground(new TextureRegionDrawable(GameAssets.getInstance().getWhiteRegion()));
        panel.setColor(0.13f, 0.10f, 0.32f, 1f); // پس‌زمینه بنفش (مطابق مرجع almanac-plant-details)

        Label title = new Label(displayName(entry.type.name()) + " Zombie", skin, "big");
        title.setColor(Color.WHITE);
        panel.add(title).colspan(2).left().padBottom(16).row();

        Table body = new Table();

        Table left = new Table();
        // W3: انیمیشنِ حالتِ بیکار به‌جای عکسِ ثابت (با fallback به همان عکس).
        TextureRegion zRegion = CollectionAssets.getInstance().region(CollectionAssetPaths.zombiePath(entry.type));
        PamIdleActor zIdle = PamIdleActor.forZombie(entry.type, zRegion);
        left.add(zIdle).size(200f, 300f);
        body.add(left).padRight(30).top();

        Table right = new Table();
        right.top().left();
        if (stats != null) {
            addStatRow(right, null, "Toughness", toughnessLabel(stats.getHp()));
            addStatRow(right, null, "Speed", speedLabel(stats.getMoveSpeed()));
            right.row();
            Label desc = new Label(stats.getDescription() != null ? stats.getDescription() : "", skin, "default");
            desc.setWrap(true);
            desc.setColor(Color.LIGHT_GRAY);
            right.add(desc).width(420f).padTop(14).left();
        }
        body.add(right).top();

        panel.add(body).colspan(2).row();

        Table root = new Table();
        root.add(navArrow(true, () -> stepDetail(-1))).expandY().left().pad(20);
        root.add(panel).grow();
        root.add(navArrow(false, () -> stepDetail(1))).expandY().right().pad(20);
        return root;
    }

    /** بازه‌های سختی/سرعت حدسی (بر مبنای hp/moveSpeed پیش‌فرض ZombieDataRegistry) — قابل تنظیم. */
    private String toughnessLabel(int hp) {
        if (hp <= 250)  return "Low";
        if (hp <= 500)  return "Normal";
        if (hp <= 1000) return "Solid";
        if (hp <= 2000) return "Hardy";
        return "Massive";
    }

    private String speedLabel(double moveSpeed) {
        if (moveSpeed <= 0.0)   return "Stationary";
        if (moveSpeed < 0.15)   return "Slow";
        if (moveSpeed < 0.20)   return "Basic";
        if (moveSpeed < 0.28)   return "Fast";
        return "Very Fast";
    }

    private void stepDetail(int delta) {
        int size = plantsTab ? detailPlantList.size() : seenZombies.size();
        if (size == 0) return;
        detailIndex = ((detailIndex + delta) % size + size) % size;
        rebuildUi();
    }

    // =========================================================================
    //  FILTER (AA3) + toast
    // =========================================================================

    private boolean filtersActive() {
        return !filterFamilies.isEmpty() || filterLockedOnly || filterUpgradableOnly;
    }

    /** لیستِ گیاهانِ قابل‌نمایش پس از اعمالِ فیلترها. */
    private List<GameFacade.PlantEntry> visiblePlants() {
        if (!filtersActive()) return plantEntries;
        List<GameFacade.PlantEntry> out = new ArrayList<>();
        for (GameFacade.PlantEntry e : plantEntries) {
            if (filterLockedOnly && e.unlocked) continue;
            if (filterUpgradableOnly && !(e.unlocked && e.level < 3)) continue;
            if (!filterFamilies.isEmpty()) {
                PlantFamily fam = e.stats != null ? e.stats.getFamily() : null;
                if (fam == null || !filterFamilies.contains(fam)) continue;
            }
            out.add(e);
        }
        return out;
    }

    /**
     * popupِ فیلتر: بر اساس خانواده / قفل‌بودن / قابل‌ارتقا‌بودن + دکمه‌ی Reset.
     * روی {@link BorderedTable} در یک overlayِ مودالِ دستی ساخته شده (نه scene2d
     * {@code Dialog}) چون اسکینِ pvz-skin استایلِ Window ندارد و Dialog کرش می‌کند.
     */
    private void showFilterDialog() {
        final Group overlay = new Group();
        overlay.setSize(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT);

        Image dim = new Image(new TextureRegionDrawable(GameAssets.getInstance().getWhiteRegion()));
        dim.setColor(0f, 0f, 0f, 0.55f);
        dim.setSize(overlay.getWidth(), overlay.getHeight());
        dim.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent e, float x, float y, int p, int b) { return true; }
        });
        overlay.addActor(dim);

        BorderedTable panel = new BorderedTable();
        panel.pad(18);
        panel.add(new Label("Filter Plants", skin, "medium")).colspan(3).padBottom(12).row();

        panel.add(new Label("By family:", skin, "default")).left().colspan(3).padBottom(4).row();
        final java.util.Map<PlantFamily, CheckBox> famBoxes = new java.util.EnumMap<>(PlantFamily.class);
        int col = 0;
        Table famGrid = new Table();
        for (PlantFamily fam : PlantFamily.values()) {
            CheckBox cb = new CheckBox("  " + displayName(fam.name()), skin);
            cb.setChecked(filterFamilies.contains(fam));
            famBoxes.put(fam, cb);
            famGrid.add(cb).left().pad(3).width(190f);
            if (++col % 3 == 0) famGrid.row();
        }
        panel.add(famGrid).colspan(3).left().row();

        final CheckBox lockedBox = new CheckBox("  Locked only", skin);
        lockedBox.setChecked(filterLockedOnly);
        final CheckBox upgBox = new CheckBox("  Upgradable only", skin);
        upgBox.setChecked(filterUpgradableOnly);
        Table flags = new Table();
        flags.add(lockedBox).left().padRight(30);
        flags.add(upgBox).left();
        panel.add(flags).colspan(3).left().padTop(10).row();

        Table buttons = new Table();
        TextButton cancel = new TextButton("Cancel", skin, "brown");
        cancel.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { overlay.remove(); }
        });
        TextButton reset = new TextButton("Reset", skin, "brown");
        reset.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                filterFamilies.clear();
                filterLockedOnly = false;
                filterUpgradableOnly = false;
                overlay.remove();
                rebuildUi();
            }
        });
        TextButton apply = new TextButton("Filter", skin, "green");
        apply.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                filterFamilies.clear();
                for (java.util.Map.Entry<PlantFamily, CheckBox> en : famBoxes.entrySet()) {
                    if (en.getValue().isChecked()) filterFamilies.add(en.getKey());
                }
                filterLockedOnly = lockedBox.isChecked();
                filterUpgradableOnly = upgBox.isChecked();
                overlay.remove();
                rebuildUi();
            }
        });
        buttons.add(cancel).width(120f).height(46f).padRight(10);
        buttons.add(reset).width(120f).height(46f).padRight(10);
        buttons.add(apply).width(140f).height(46f);
        panel.add(buttons).colspan(3).padTop(16);

        panel.pack();
        panel.setPosition((overlay.getWidth() - panel.getWidth()) / 2f,
                          (overlay.getHeight() - panel.getHeight()) / 2f);
        overlay.addActor(panel);
        stage.addActor(overlay);
    }

    private void showToast(ToastActor t) {
        t.setPosition(GameConstants.VIEWPORT_WIDTH / 2f - t.getWidth() * 0.5f, 90);
        stage.addActor(t);
    }

    private Table navArrow(boolean isPrev, Runnable onClick) {
        Table t = new Table();
        String path = isPrev ? CollectionAssetPaths.ARROW_PREV : CollectionAssetPaths.ARROW_NEXT;
        TextureRegion region = CollectionAssets.getInstance().region(path);
        Image img = region != null ? new Image(region) : new Image(GameAssets.getInstance().getWhiteRegion());
        t.add(img).size(52f);
        t.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { onClick.run(); }
        });
        return t;
    }

    private void addStatRow(Table container, String iconPath, String label, String value) {
        Table row = new Table();
        if (iconPath != null) {
            TextureRegion icon = CollectionAssets.getInstance().region(iconPath);
            if (icon != null) row.add(new Image(icon)).size(36f).padRight(8f);
        }
        Table textCol = new Table();
        Label lbl = new Label(label.toUpperCase(), skin, "default");
        lbl.setColor(Color.GRAY);
        lbl.setFontScale(0.8f);
        textCol.add(lbl).left().row();
        Label val = new Label(value, skin, "default");
        val.setColor(Color.WHITE);
        val.setWrap(true);
        textCol.add(val).width(300f).left();
        row.add(textCol);
        container.add(row).left().padBottom(10).row();
    }

    private Table emptyDetailFallback(String message) {
        Table t = new Table();
        Label l = new Label(message, skin, "default");
        l.setColor(Color.LIGHT_GRAY);
        t.add(l);
        TextButton back = new TextButton("Back", skin, "brown");
        back.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { backToGrid(); }
        });
        t.row();
        t.add(back).padTop(16).size(140, 50);
        return t;
    }

    private ProgressBar.ProgressBarStyle safeProgressBarStyle() {
        try {
            String style = skin.has("level-bar", ProgressBar.ProgressBarStyle.class) ? "level-bar" : "default-horizontal";
            return skin.get(style, ProgressBar.ProgressBarStyle.class);
        } catch (Exception e) {
            return null;
        }
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

    // =========================================================================

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
