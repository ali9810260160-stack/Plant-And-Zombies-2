package com.pvz2.graphics.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.pvz2.PVZApplication;
import com.pvz2.graphics.*;
import com.pvz2.graphics.actors.ToastActor;
import com.pvz2.graphics.assets.AssetIds;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.model.QuestDefinition;
import com.pvz2.model.QuestProgress;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * منوی مأموریت‌ها (Quests) — بازطراحی مطابق مرجع {@code quests-menu-list.png}.
 * <p>
 * دو تب «Daily» (سبز) و «Events» (آبی — دسته‌ی {@code epic} در QuestService)
 * که هرکدام فهرستی از کوئست‌ها را با آیکون، نام، توضیح، نوار پیشرفت، آیکون
 * جایزه و دکمه‌ی PLAY/CLAIM نشان می‌دهند — دقیقاً طبق سند فاز ۲
 * («نام، توضیح مختصر، جایزه، میزان پیشرفت... به ترتیب اولویت»).
 * <p>
 * هیچ منطق بازی این‌جا نیست: همه‌چیز از {@link #facade()} (پل فاز ۱) خوانده
 * می‌شود. جایزه‌ی کوئست همان لحظه‌ی تکمیل (در {@code QuestService}) به کاربر
 * داده می‌شود؛ دکمه‌ی CLAIM اینجا فقط ردیف را از حالت «آماده‌ی دریافت» خارج
 * می‌کند (نگاه کنید به {@code QuestService#claimQuest}).
 */
public class QuestScreen extends BaseScreen {

    private static final float ROW_H = 96f;
    private static final float ROW_GAP = 12f;

    private Stage stage;
    /** دسته‌ی فعال — کلید همانی است که {@code QuestService.filterByPage} می‌فهمد. */
    private String activeCategory = "daily";

    private Label refreshLabel;
    private float refreshAccum = 0f;

    public QuestScreen(PVZApplication game) { super(game); }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        buildUi();
    }

    // =========================================================================
    //  چیدمان کلی
    // =========================================================================

    private void buildUi() {
        stage.clear();
        Skin skin = GameAssets.getInstance().getSkin();

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        root.add(buildHeader(skin)).growX().height(70f).row();

        Image woodBar = new Image(GameAssets.getInstance().local(AssetIds.QUEST_PANEL_EDGE));
        woodBar.setScaling(Scaling.stretch);
        root.add(woodBar).growX().height(14f).row();

        refreshLabel = new Label(refreshCountdownText(), skin, "medium");
        refreshLabel.setColor(1f, 0.82f, 0.35f, 1f);
        refreshLabel.setAlignment(Align.center);
        refreshLabel.setVisible("daily".equals(activeCategory));
        Table refreshRow = new Table();
        refreshRow.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        refreshRow.add(refreshLabel).pad(6, 0, 6, 0);
        root.add(refreshRow).growX().height(30f).row();

        // BO3: مینی‌گیم‌ها حالا یک «تبِ» مستقل کنارِ Daily/Events هستند (نه دکمه‌ی
        // گوشه‌ای + Dialog که با نبودِ WindowStyle در اسکین کرش می‌کرد).
        if ("minigames".equals(activeCategory)) {
            root.add(buildMinigamesList(skin)).grow().pad(10, 24, 16, 24).row();
        } else {
            root.add(buildList(skin)).grow().pad(10, 24, 16, 24).row();
        }
    }

    /** تبِ مینی‌گیم‌ها: فهرستِ کوزه‌شکنی/بولینگ/آی‌زامبی/زامبوتنی/امتیازی/بِگولد. */
    private ScrollPane buildMinigamesList(Skin skin) {
        Table content = new Table();
        content.top();
        content.defaults().growX().padBottom(ROW_GAP);

        java.util.List<com.pvz2.model.enums.PlantType> none = new java.util.ArrayList<>();
        addMinigameRow(content, skin, "🏺 Vasebreaker",
                com.pvz2.model.enums.LevelType.VASEBREAKER, none);
        addMinigameRow(content, skin, "🎳 Wallnut Bowling",
                com.pvz2.model.enums.LevelType.WALLNUT_BOWLING, none);
        addMinigameRow(content, skin, "🧟 I, Zombie",
                com.pvz2.model.enums.LevelType.I_ZOMBIE, none);
        addMinigameRow(content, skin, "⚡ Zombotany",
                com.pvz2.model.enums.LevelType.ZOMBOTANY, defaultMinigamePlants());
        // بازیِ امتیازی: تک‌دکمه‌ی Play که «آخرین مرحله‌ی باز‌شده» را با قواعدِ SCORED باز می‌کند.
        addScoredRow(content, skin);

        // بِگولد (ترکیبِ سه‌تایی) — حالا مرحله‌ی واقعی روی مپ (match-3 با گیاهانِ واقعی).
        addMinigameRow(content, skin, "🧩 Beghouled (Match-3)",
                com.pvz2.model.enums.LevelType.BEGHOULED, new java.util.ArrayList<>());

        ScrollPane scroll = new ScrollPane(content, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        return scroll;
    }

    /**
     * ردیفِ بازیِ امتیازی — یک دکمه‌ی Play که «آخرین مرحله‌ای که کاربر باز کرده»
     * را با قواعدِ SCORED باز می‌کند (طبق داک، به‌جای انتخابِ Lv1-3).
     */
    private void addScoredRow(Table content, Skin skin) {
        Table row = new Table();
        row.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        row.pad(10, 14, 10, 14);
        Label lbl = new Label("🏆 Scored Game (MeoPoints)", skin, "medium");
        lbl.setColor(Color.WHITE);
        row.add(lbl).left().expandX();
        com.badlogic.gdx.scenes.scene2d.ui.TextButton play =
                new com.badlogic.gdx.scenes.scene2d.ui.TextButton("Play", skin, "green");
        play.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                String[] cl = lastReachedChapterLevel();
                game.startMinigame(cl[0], Integer.parseInt(cl[1]),
                        com.pvz2.model.enums.LevelType.SCORED,
                        new java.util.ArrayList<>(defaultMinigamePlants()));
            }
        });
        row.add(play).size(112f, 46f);
        content.add(row).growX().height(ROW_H).row();
    }

    /** آخرین مرحله‌ی باز‌شده به‌صورتِ [chapterName, levelNumber] — پیش‌فرض مصر مرحله ۱. */
    private String[] lastReachedChapterLevel() {
        String def0 = "ANCIENT_EGYPT", def1 = "1";
        com.pvz2.model.User u = com.pvz2.model.AppState.getInstance().getCurrentUser();
        if (u == null) return new String[]{def0, def1};
        String key = u.getLastReachedLevel();
        if (key == null || key.isEmpty()) return new String[]{def0, def1};
        try {
            String[] parts = key.split("_(?=[0-9])");   // جدا از عددِ آخر (نامِ فصل خودش _ دارد)
            String chapter = parts[0];
            int level = Integer.parseInt(parts[parts.length - 1]);
            return new String[]{chapter, String.valueOf(level)};
        } catch (Exception ex) {
            return new String[]{def0, def1};
        }
    }

    /** یک ردیفِ مینی‌گیم با دکمه‌های Lv1..3 (پنلِ درون‌تب، بدونِ Dialog). */
    private void addMinigameRow(Table content, Skin skin, String title,
                                final com.pvz2.model.enums.LevelType type,
                                final java.util.List<com.pvz2.model.enums.PlantType> plants) {
        Table row = new Table();
        row.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        row.pad(10, 14, 10, 14);
        Label lbl = new Label(title, skin, "medium");
        lbl.setColor(Color.WHITE);
        row.add(lbl).left().expandX();
        for (int lvl = 1; lvl <= 3; lvl++) {
            final int level = lvl;
            com.badlogic.gdx.scenes.scene2d.ui.TextButton b =
                    new com.badlogic.gdx.scenes.scene2d.ui.TextButton("Lv " + lvl, skin, "green");
            b.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    game.startMinigame("ANCIENT_EGYPT", level, type, new java.util.ArrayList<>(plants));
                }
            });
            row.add(b).size(74f, 44f).padLeft(6f);
        }
        content.add(row).growX().height(ROW_H).row();
    }

    private java.util.List<com.pvz2.model.enums.PlantType> defaultMinigamePlants() {
        return java.util.Arrays.asList(
                com.pvz2.model.enums.PlantType.SUNFLOWER,
                com.pvz2.model.enums.PlantType.PEASHOOTER,
                com.pvz2.model.enums.PlantType.REPEATER,
                com.pvz2.model.enums.PlantType.SNOW_PEA,
                com.pvz2.model.enums.PlantType.WALL_NUT,
                com.pvz2.model.enums.PlantType.CHERRY_BOMB,
                com.pvz2.model.enums.PlantType.CABBAGE_PULT,
                com.pvz2.model.enums.PlantType.POTATO_MINE);
    }

    /** بازسازی کامل — بعد از claim یا تعویض تب (مطابق الگوی نسخه‌ی قبلی همین صفحه). */
    private void refresh() { buildUi(); }

    // =========================================================================
    //  هدر: تب‌های Daily/Events سمت چپ + سکه/الماس/بستن سمت راست
    // =========================================================================

    private Table buildHeader(Skin skin) {
        Table header = new Table();
        header.pad(10, 20, 0, 20);

        Table tabs = new Table();
        // چهار تب بر مبنای دسته‌بندیِ CSV: روزانه/اصلی/چالش + مینی‌گیم.
        tabs.add(buildTab("daily", AssetIds.QUEST_TAB_DAILY_ACTIVE, AssetIds.QUEST_TAB_DAILY_INACTIVE))
                .padRight(6).bottom();
        tabs.add(buildTab("story", AssetIds.QUEST_TAB_MAIN_ACTIVE, AssetIds.QUEST_TAB_MAIN_INACTIVE))
                .padRight(6).bottom();
        tabs.add(buildTab("epic", AssetIds.QUEST_TAB_EVENTS_ACTIVE, AssetIds.QUEST_TAB_EVENTS_INACTIVE))
                .padRight(6).bottom();
        tabs.add(buildTab("minigames", AssetIds.QUEST_TAB_MINI_ACTIVE, AssetIds.QUEST_TAB_MINI_INACTIVE))
                .bottom();

        com.pvz2.model.User user = facade().getCurrentUser();
        long coins = user != null ? user.getCoins() : 0;
        int gems = user != null ? user.getGems() : 0;

        Table currency = new Table();
        currency.add(currencyPair(AssetIds.QUEST_ICON_GEM, gems, new Color(0.55f, 0.85f, 1f, 1f))).padRight(18f);
        currency.add(currencyPair(AssetIds.QUEST_ICON_COIN, coins, Color.GOLD)).padRight(18f);
        currency.add(closeButton()).size(46f);

        header.add(tabs).left().bottom().expandX();
        header.add(currency).right().top();
        return header;
    }

    /** یک تب دسته با نشان قرمز تعداد کوئست‌های «آماده‌ی دریافت» آن دسته. */
    private Stack buildTab(final String category, String activeAsset, String inactiveAsset) {
        boolean active = activeCategory.equals(category);
        String asset = active ? activeAsset : inactiveAsset;
        TextureRegion region = GameAssets.getInstance().local(asset);

        float scale = 0.92f;
        float w = region.getRegionWidth() * scale;
        float h = region.getRegionHeight() * scale;

        Image tabImg = new Image(region);
        tabImg.setScaling(Scaling.stretch);
        tabImg.setTouchable(Touchable.disabled);

        String label = tabLabel(category);
        Label lbl = new Label(label, GameAssets.getInstance().getSkin(), "medium");
        lbl.setColor(Color.WHITE);
        lbl.setAlignment(Align.center);

        Stack stack = new Stack();
        Container<Image> imgC = new Container<>(tabImg);
        imgC.size(w, h);
        imgC.setTouchable(Touchable.disabled);
        stack.add(imgC);
        Container<Label> lblC = new Container<>(lbl);
        lblC.size(w, h);
        lblC.padBottom(active ? 14f : 4f);
        lblC.setTouchable(Touchable.disabled);
        stack.add(lblC);

        int claimable = countClaimable(category);
        if (claimable > 0) {
            Label badge = new Label(String.valueOf(claimable), GameAssets.getInstance().getSkin(), "medium");
            badge.setColor(Color.WHITE);
            badge.setAlignment(Align.center);
            Image dot = new Image(com.pvz2.graphics.util.IconFactory.dot(22, new Color(0.85f, 0.1f, 0.1f, 1f)));
            Stack badgeStack = new Stack();
            badgeStack.add(dot);
            badgeStack.add(badge);
            Table corner = new Table();
            corner.top().right();
            corner.add(badgeStack).size(22f).padTop(-6f).padRight(-6f);
            Container<Table> cornerC = new Container<>(corner);
            cornerC.size(w, h);
            cornerC.top().right();
            cornerC.setTouchable(Touchable.disabled);
            stack.add(cornerC);
        }

        // ⚠️ باگِ قبلی: listener روی tabImg بود ولی containerِ لیبل (siblingِ رویش)
        // کلیک را می‌گرفت و به آن نمی‌رسید → تب باز نمی‌شد. حالا listener روی خودِ
        // Stack (ancestorِ مشترک) است و بقیه‌ی اکتورها touchable=disabled شده‌اند.
        stack.setTouchable(Touchable.enabled);
        stack.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                if (!activeCategory.equals(category)) {
                    activeCategory = category;
                    refresh();
                }
            }
        });
        return stack;
    }

    private String tabLabel(String category) {
        switch (category) {
            case "daily":     return "Daily";
            case "story":     return "Main";
            case "epic":      return "Challenge";
            case "minigames": return "Minigames";
            default:          return category;
        }
    }

    private Table currencyPair(String iconAsset, long amount, Color color) {
        Table t = new Table();
        Image icon = new Image(GameAssets.getInstance().local(iconAsset));
        icon.setScaling(Scaling.fit);
        Label lbl = new Label(String.valueOf(amount), GameAssets.getInstance().getSkin(), "medium");
        lbl.setColor(color);
        t.add(icon).size(30f).padRight(5f);
        t.add(lbl);
        return t;
    }

    private Image closeButton() {
        Image img = new Image(GameAssets.getInstance().local(AssetIds.QUEST_CLOSE_TAB));
        img.setScaling(Scaling.fit);
        img.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { goTo(ScreenId.MAIN_MENU); }
        });
        return img;
    }

    // =========================================================================
    //  فهرست کوئست‌ها
    // =========================================================================

    private ScrollPane buildList(Skin skin) {
        Table content = new Table();
        content.top();
        content.defaults().growX().padBottom(ROW_GAP);

        List<QuestDefinition> quests = facade().getQuestsByCategory(activeCategory);
        if (quests.isEmpty()) {
            Label empty = new Label("در حال حاضر کوئستی در این دسته موجود نیست.", skin);
            empty.setColor(Color.LIGHT_GRAY);
            content.add(empty).padTop(30).center();
        } else {
            for (QuestDefinition qd : quests) {
                content.add(buildRow(skin, qd)).height(ROW_H).row();
            }
        }

        ScrollPane scroll = new ScrollPane(content, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        return scroll;
    }

    private Table buildRow(Skin skin, QuestDefinition qd) {
        QuestProgress qp = facade().getQuestProgress(qd.getId());
        boolean completed = qp.isCompleted();
        boolean claimed = qp.isClaimed();
        boolean claimable = completed && !claimed;
        int target = qd.getTargetForLevel(qp.getCurrentLevel());
        int current = Math.min(qp.getCurrentValue(), target);

        Table row = new Table();
        row.setBackground(rowBackground(skin, claimable));
        row.pad(10, 14, 10, 14);

        // ── آیکون فعالیت سمت چپ ردیف (مطابق مرجع) + نشان «!» اگر claimable ──
        Stack iconStack = new Stack();
        Image icon = new Image(GameAssets.getInstance().local(AssetIds.ADV_ICON_QUESTS));
        icon.setScaling(Scaling.fit);
        iconStack.add(icon);
        if (claimable) {
            Table corner = new Table();
            corner.top().left();
            Image alert = new Image(GameAssets.getInstance().region(AssetIds.ICON_NEWS_ALERT));
            corner.add(alert).size(20f).padTop(-6f).padLeft(-6f);
            iconStack.add(corner);
        }
        row.add(iconStack).size(72f).padRight(14f);

        // ── نام + توضیح + نوار پیشرفت ──
        Table mid = new Table();
        mid.top();

        Label nameLbl = new Label(qd.getNameFA(), skin, "medium");
        nameLbl.setColor(new Color(0.32f, 0.2f, 0.08f, 1f));
        mid.add(nameLbl).left().row();

        String descText = qd.getDescription().replace("{target}", String.valueOf(target));
        Label descLbl = new Label(descText, skin, "medium");
        descLbl.setColor(new Color(0.45f, 0.34f, 0.2f, 1f));
        descLbl.setWrap(true);
        mid.add(descLbl).left().growX().padBottom(6f).row();

        String barStyle = skin.has("level-bar", ProgressBar.ProgressBarStyle.class)
                ? "level-bar" : "default-horizontal";
        ProgressBar bar = new ProgressBar(0, Math.max(target, 1), 1, false, skin, barStyle);
        bar.setValue(completed ? target : current); // کوئستِ کامل‌شده = نوارِ پُر
        bar.setAnimateDuration(0.2f);

        Label fraction = new Label(current + "/" + target, skin, "medium");
        fraction.setColor(Color.WHITE);
        fraction.setAlignment(Align.center);

        Stack barStack = new Stack();
        barStack.add(bar);
        Container<Label> fractionC = new Container<>(fraction);
        fractionC.fill();
        barStack.add(fractionC);
        mid.add(barStack).growX().height(24f);

        row.add(mid).grow().padRight(14f);

        // ── آیکون جایزه + مقدار ──
        Table rewardBox = new Table();
        int amount = qd.isRewardMultiplier() ? qd.getRewardBase() * qp.getCurrentLevel() : qd.getRewardBase();
        String rewardIcon;
        switch (qd.getRewardType()) {
            case COIN: rewardIcon = AssetIds.QUEST_REWARD_COINS; break;
            case GEM:  rewardIcon = AssetIds.QUEST_REWARD_GEMS; break;
            default:   rewardIcon = null;
        }
        if (rewardIcon != null) {
            Image rImg = new Image(GameAssets.getInstance().local(rewardIcon));
            rImg.setScaling(Scaling.fit);
            rewardBox.add(rImg).size(46f).row();
        }
        Label amountLbl = new Label("x" + amount, skin, "medium");
        amountLbl.setColor(new Color(0.32f, 0.2f, 0.08f, 1f));
        amountLbl.setAlignment(Align.center);
        rewardBox.add(amountLbl);
        row.add(rewardBox).width(64f).padRight(14f);

        // ── وضعیت: Achieved / Not Achieved (به‌جای دکمه‌ی PLAY) ──
        row.add(questStatus(skin, completed)).width(120f).center();

        return row;
    }

    private Drawable rowBackground(Skin skin, boolean claimable) {
        if (claimable) {
            TextureRegion tr = GameAssets.getInstance().local(AssetIds.QUEST_ROW_CLAIMABLE_BG);
            return new TextureRegionDrawable(tr);
        }
        return skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10");
    }

    /**
     * وضعیتِ کوئست به‌صورتِ متنِ انگلیسی (به‌جای دکمه‌ی PLAY): «Achieved» اگر
     * کوئست کامل شده، وگرنه «Not Achieved». جایزه هنگامِ تکمیلِ کوئست حین بازی
     * به‌صورتِ خودکار به کاربر داده و ذخیره می‌شود (نیازی به دکمه‌ی CLAIM نیست).
     */
    private Label questStatus(Skin skin, boolean completed) {
        Label lbl = new Label(completed ? "Achieved" : "Not Achieved", skin, "medium");
        lbl.setColor(completed ? new Color(0.25f, 0.78f, 0.25f, 1f)
                               : new Color(0.62f, 0.5f, 0.34f, 1f));
        lbl.setAlignment(Align.center);
        lbl.setWrap(true);
        return lbl;
    }

    private ToastActor centeredToast(ToastActor t) {
        t.setPosition(GameConstants.VIEWPORT_WIDTH / 2f - t.getWidth() * 0.5f, 60f);
        return t;
    }

    // =========================================================================
    //  کمکی‌ها
    // =========================================================================

    private int countClaimable(String category) {
        int n = 0;
        for (QuestDefinition qd : facade().getQuestsByCategory(category)) {
            QuestProgress qp = facade().getQuestProgress(qd.getId());
            if (qp.isCompleted() && !qp.isClaimed()) n++;
        }
        return n;
    }

    /** متن شمارش‌معکوس تا نیمه‌شب بعدی — کوئست‌های روزانه در آن لحظه ریست می‌شوند. */
    private String refreshCountdownText() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
        long secs = Math.max(0, Duration.between(now, nextMidnight).getSeconds());
        long h = secs / 3600;
        long m = (secs % 3600) / 60;
        return "Daily Activities refresh in " + h + "h " + m + "min";
    }

    @Override
    public void render(float delta) {
        GameAssets.getInstance().update();
        ScreenUtils.clear(0.22f, 0.11f, 0.06f, 1f);

        // متن شمارش‌معکوس منطقاً باید هر دقیقه به‌روزرسانی شود، نه هر فریم.
        if (refreshLabel != null && "daily".equals(activeCategory)) {
            refreshAccum += delta;
            if (refreshAccum >= 60f) {
                refreshAccum = 0f;
                refreshLabel.setText(refreshCountdownText());
            }
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int w, int h) { stage.getViewport().update(w, h, true); }

    @Override
    public void dispose() { if (stage != null) stage.dispose(); }
}
