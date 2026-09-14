package com.pvz2.graphics.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.pvz2.PVZApplication;
import com.pvz2.graphics.*;
import com.pvz2.graphics.actors.ToastActor;
import com.pvz2.graphics.assets.AssetIds;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.graphics.util.IconFactory;
import com.pvz2.model.User;

/**
 * انتخاب چپتر (کاروسل کارتی) و انتخاب مرحله برای ماجراجویی — بازطراحی
 * مطابق مرجع‌های {@code adventure-map-chapters.png} و
 * {@code adventure-map-levels.png}.
 * <p>
 * هیچ منطق بازی این‌جا نیست: وضعیت باز/قفل، تعداد مراحل هر فصل و نوع هر
 * مرحله همه مستقیم از {@link #facade()} (پل به سرویس‌های فاز یک) خونده
 * می‌شن. این کلاس فقط می‌چیند و نمایش می‌ده.
 */
public class AdventureScreen extends BaseScreen {

    // ─── چیدمان کاروسل چپترها ───────────────────────────────────────────
    private static final float CARD_W = 300f;
    private static final float CARD_H = 560f;
    private static final float CARD_SPACING = 340f;
    private static final float CAROUSEL_CENTER_X = GameConstants.VIEWPORT_WIDTH / 2f;
    private static final float CAROUSEL_BOTTOM_Y = 96f;
    private static final float SIDE_SCALE = 0.78f;
    private static final float SIDE_ALPHA = 0.55f;
    private static final float SLIDE_DURATION = 0.32f;

    /** اطلاعات ثابت هر چپتر — کلید enum فاز یک + asset های بازطراحی. */
    private static final class ChapterInfo {
        final String key, displayName, cardAsset, bgAsset;
        /** سختی نمایشی ۱..۵ (فلفل) — فیلدی برای این در فاز یک نیست، این‌جا به‌صورت ثابت تعیین شده. */
        final int difficulty;
        final String[] thumbnails;

        ChapterInfo(String key, String displayName, String cardAsset, String bgAsset,
                    int difficulty, String[] thumbnails) {
            this.key = key;
            this.displayName = displayName;
            this.cardAsset = cardAsset;
            this.bgAsset = bgAsset;
            this.difficulty = difficulty;
            this.thumbnails = thumbnails;
        }
    }

    private static final ChapterInfo[] CHAPTERS = {
        new ChapterInfo("ANCIENT_EGYPT", "Ancient Egypt",
                AssetIds.ADV_CARD_EGYPT, AssetIds.ADV_BG_EGYPT, 2, AssetIds.LEVEL_THUMBNAILS_EGYPT),
        new ChapterInfo("FROSTBITE_CAVES", "Frostbite Caves",
                AssetIds.ADV_CARD_ICEAGE, AssetIds.ADV_BG_ICEAGE, 3, AssetIds.LEVEL_THUMBNAILS_ICEAGE),
        new ChapterInfo("BIG_WAVE_BEACH", "Big Wave Beach",
                AssetIds.ADV_CARD_BEACH, AssetIds.ADV_BG_BEACH, 4, AssetIds.LEVEL_THUMBNAILS_BEACH),
        new ChapterInfo("DARK_AGES", "Dark Ages",
                AssetIds.ADV_CARD_DARK, AssetIds.ADV_BG_DARK, 5, AssetIds.LEVEL_THUMBNAILS_DARK),
    };

    private Stage stage;
    private int currentChapterIndex = 0;

    private Group carouselGroup;
    private final Actor[] chapterCards = new Actor[CHAPTERS.length];
    private Table dotsRow;

    public AdventureScreen(PVZApplication game) { super(game); }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        showChapters();
    }

    // =========================================================================
    //  صفحه ۱ — انتخاب چپتر (کاروسل)
    // =========================================================================

    private void showChapters() {
        stage.clear();
        Skin skin = GameAssets.getInstance().getSkin();

        addFullscreenBackground(AssetIds.ADV_BG_CHAPTERS);

        Table root = new Table();
        root.setFillParent(true);
        root.top();
        root.add(buildHeader(skin, () -> goTo(ScreenId.MAIN_MENU))).growX().row();
        stage.addActor(root);

        carouselGroup = new Group();
        carouselGroup.setSize(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT);
        // فقط کارت‌ها (فرزندان) کلیک بگیرند؛ فضای خالیِ گروهِ تمام‌صفحه نباید کلیکِ
        // هدر (دکمه‌های Back/Collection/…) را که زیرش است ببلعد.
        carouselGroup.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.childrenOnly);
        stage.addActor(carouselGroup);

        for (int i = 0; i < CHAPTERS.length; i++) {
            Actor card = buildChapterCard(skin, CHAPTERS[i], i);
            chapterCards[i] = card;
            carouselGroup.addActor(card);
        }

        stage.addActor(buildNavArrow(false, this::goPrevChapter));
        stage.addActor(buildNavArrow(true, this::goNextChapter));

        dotsRow = buildDots();
        stage.addActor(dotsRow);

        carouselGroup.addListener(new ActorGestureListener() {
            @Override
            public void fling(InputEvent event, float velocityX, float velocityY, int button) {
                if (velocityX < -80) goNextChapter();
                else if (velocityX > 80) goPrevChapter();
            }
        });

        layoutCarousel(false);
    }

    /** کارت یک چپتر: تصویر جزیره + زیرش نام/سختی/پیشرفت/دکمه Review (یا قفل اگه بسته باشه). */
    private Actor buildChapterCard(Skin skin, ChapterInfo info, int chapterIndex) {
        boolean unlocked = facade().isLevelUnlocked(info.key, 1);
        int total = facade().getLevelsInChapter(info.key);
        int unlockedCount = countUnlockedLevels(info.key, total);

        Table card = new Table();
        card.setSize(CARD_W, CARD_H);
        card.top();

        TextureRegion artRegion = GameAssets.getInstance().local(info.cardAsset);
        Image art = new Image(artRegion);
        art.setScaling(Scaling.fit);
        if (!unlocked) art.setColor(0.42f, 0.42f, 0.42f, 1f);
        card.add(art).size(CARD_W, 380f).padBottom(10f).row();

        Label nameLabel = new Label(info.displayName, skin, "medium_outline");
        nameLabel.setAlignment(Align.center);
        card.add(nameLabel).width(CARD_W).padBottom(8f).row();

        if (unlocked) {
            Table difficultyRow = new Table();
            TextureRegion pepper = GameAssets.getInstance().local(AssetIds.ADV_ICON_DIFFICULTY);
            for (int p = 0; p < info.difficulty; p++) {
                difficultyRow.add(new Image(pepper)).size(26f).padRight(2f);
            }
            card.add(difficultyRow).padBottom(8f).row();

            ProgressBar bar = new ProgressBar(0, total, 1, false, skin, "default-horizontal");
            bar.setValue(unlockedCount);
            bar.setDisabled(true);
            bar.setAnimateDuration(0.25f);
            card.add(bar).width(200f).padBottom(4f).row();

            Label progressLabel = new Label(unlockedCount + "/" + total, skin, "default");
            progressLabel.setColor(unlockedCount >= total ? Color.GREEN : Color.LIGHT_GRAY);
            card.add(progressLabel).padBottom(10f).row();

            TextButton review = new TextButton("REVIEW", skin, "purple");
            review.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent e, float x, float y) {
                    if (chapterIndex != currentChapterIndex) return; // فقط کارت وسط قابل کلیکه
                    showLevels(chapterIndex);
                }
            });
            card.add(review).size(150f, 54f);
        } else {
            TextureRegion lock = IconFactory.lock(56, new Color(0.95f, 0.95f, 0.98f, 1f));
            card.add(new Image(lock)).size(56f).padTop(6f);
        }

        return card;
    }

    private int countUnlockedLevels(String chapterKey, int total) {
        int count = 0;
        for (int level = 1; level <= total; level++) {
            if (facade().isLevelUnlocked(chapterKey, level)) count++;
        }
        return count;
    }

    private void goPrevChapter() {
        if (currentChapterIndex == 0) return;
        currentChapterIndex--;
        layoutCarousel(true);
    }

    private void goNextChapter() {
        if (currentChapterIndex == CHAPTERS.length - 1) return;
        currentChapterIndex++;
        layoutCarousel(true);
    }

    /** موقعیت/مقیاس/شفافیت هر کارت رو بر اساس فاصله‌اش تا currentChapterIndex تنظیم می‌کنه (اسلاید کنار رفتن). */
    private void layoutCarousel(boolean animate) {
        for (int i = 0; i < CHAPTERS.length; i++) {
            Actor card = chapterCards[i];
            int offset = i - currentChapterIndex;
            float targetX = CAROUSEL_CENTER_X + offset * CARD_SPACING - CARD_W / 2f;
            float targetY = CAROUSEL_BOTTOM_Y;
            float targetScale = (offset == 0) ? 1f : SIDE_SCALE;
            float targetAlpha = (offset == 0) ? 1f : SIDE_ALPHA;

            card.setOrigin(CARD_W / 2f, 0f);
            card.clearActions();
            if (animate) {
                card.addAction(Actions.parallel(
                        Actions.moveTo(targetX, targetY, SLIDE_DURATION, Interpolation.smooth),
                        Actions.scaleTo(targetScale, targetScale, SLIDE_DURATION, Interpolation.smooth),
                        Actions.alpha(targetAlpha, SLIDE_DURATION, Interpolation.smooth)
                ));
            } else {
                card.setPosition(targetX, targetY);
                card.setScale(targetScale);
                card.getColor().a = targetAlpha;
            }
        }
        refreshDots();
    }

    private Table buildDots() {
        Table row = new Table();
        row.setSize(GameConstants.VIEWPORT_WIDTH, 24f);
        row.setPosition(0, 44f);
        for (int i = 0; i < CHAPTERS.length; i++) {
            final int idx = i;
            Image dot = new Image(IconFactory.dot(14, Color.WHITE));
            dot.setColor(1f, 1f, 1f, i == currentChapterIndex ? 1f : 0.4f);
            dot.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent e, float x, float y) {
                    currentChapterIndex = idx;
                    layoutCarousel(true);
                }
            });
            row.add(dot).size(14f).pad(4f);
        }
        return row;
    }

    private void refreshDots() {
        if (dotsRow == null) return;
        for (int i = 0; i < dotsRow.getChildren().size; i++) {
            Actor a = dotsRow.getChildren().get(i);
            a.setColor(1f, 1f, 1f, i == currentChapterIndex ? 1f : 0.4f);
        }
    }

    private Actor buildNavArrow(boolean pointingRight, Runnable action) {
        TextureRegion chevron = IconFactory.chevron(40, pointingRight, Color.WHITE);
        Image btn = clickableImage(chevron, Scaling.fit, action);
        btn.setSize(48f, 48f);
        btn.setPosition(pointingRight ? GameConstants.VIEWPORT_WIDTH - 70f : 22f,
                CAROUSEL_BOTTOM_Y + CARD_H / 2f - 24f);
        return btn;
    }

    // =========================================================================
    //  صفحه ۲ — انتخاب مرحله داخل یک چپتر
    // =========================================================================

    private void showLevels(int chapterIndex) {
        ChapterInfo info = CHAPTERS[chapterIndex];
        stage.clear();
        Skin skin = GameAssets.getInstance().getSkin();

        addFullscreenBackground(info.bgAsset);

        Table root = new Table();
        root.setFillParent(true);
        root.top();
        root.add(buildHeader(skin, this::showChapters)).growX().row();

        int total = facade().getLevelsInChapter(info.key);
        Table levelsRow = new Table();
        levelsRow.defaults().pad(14f).top();
        for (int i = 1; i <= total; i++) {
            levelsRow.add(buildLevelCard(skin, info, i)).size(220f, 460f);
        }

        ScrollPane scroll = new ScrollPane(levelsRow, skin);
        scroll.setScrollingDisabled(false, true);
        scroll.setFadeScrollBars(false);
        root.add(scroll).grow().padTop(20f).row();

        stage.addActor(root);
    }

    /** کارت یک مرحله: شماره + تامبنیل + نام + نوع + دکمه ورود (یا قفل). */
    private Table buildLevelCard(Skin skin, ChapterInfo info, int levelNumber) {
        boolean unlocked = facade().isLevelUnlocked(info.key, levelNumber);
        String typeName = facade().getLevelTypeName(info.key, levelNumber);
        String thumbId = (info.thumbnails != null && info.thumbnails.length >= levelNumber)
                ? info.thumbnails[levelNumber - 1] : "";

        Table card = new Table(skin);
        card.setBackground("image_ui_dialog_asset_inner_bkgd_10");
        card.top().pad(10f);

        Stack thumbStack = new Stack();
        TextureRegion thumb = GameAssets.getInstance().region(thumbId);
        Image thumbImage = new Image(thumb);
        thumbImage.setScaling(Scaling.fill);
        if (!unlocked) thumbImage.setColor(0.4f, 0.4f, 0.4f, 1f);
        thumbStack.add(thumbImage);

        Label numberLabel = new Label(String.valueOf(levelNumber), skin, "big_outline");
        Table numberCorner = new Table();
        numberCorner.top().left();
        numberCorner.add(numberLabel).pad(4f, 8f, 0f, 0f);
        thumbStack.add(numberCorner);

        if (!unlocked) {
            TextureRegion lock = IconFactory.lock(44, new Color(0.95f, 0.95f, 0.98f, 1f));
            Table lockCenter = new Table();
            lockCenter.add(new Image(lock)).size(44f);
            thumbStack.add(lockCenter);
        }

        card.add(thumbStack).size(196f, 240f).padBottom(8f).row();

        Label nameLabel = new Label("Level " + levelNumber, skin, "default");
        nameLabel.setAlignment(Align.center);
        card.add(nameLabel).width(196f).padBottom(2f).row();

        Label typeLabel = new Label(typeName, skin, "default");
        typeLabel.setFontScale(0.85f);
        typeLabel.setColor(Color.LIGHT_GRAY);
        typeLabel.setAlignment(Align.center);
        card.add(typeLabel).width(196f).padBottom(8f).row();

        if (unlocked) {
            TextButton enter = new TextButton("Enter", skin, "green");
            enter.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent e, float x, float y) {
                    game.setPlantSelectParams(info.key, levelNumber);
                    goTo(ScreenId.PLANT_SELECT);
                }
            });
            card.add(enter).size(140f, 50f);
        } else {
            TextureRegion lock = IconFactory.lock(32, new Color(0.7f, 0.7f, 0.7f, 1f));
            card.add(new Image(lock)).size(32f);
        }

        return card;
    }

    // =========================================================================
    //  مشترک — پس‌زمینه، هدر، ویجت سکه/الماس
    // =========================================================================

    private void addFullscreenBackground(String localAsset) {
        TextureRegion bg = GameAssets.getInstance().local(localAsset);
        Image bgImage = new Image(bg);
        bgImage.setScaling(Scaling.fill);
        Table layer = new Table();
        layer.setFillParent(true);
        layer.add(bgImage).grow();
        stage.addActor(layer);
    }

    /**
     * هدر مشترک بالای هر دو صفحه (چپترها/مراحل): دکمه Back (با عمل مخصوص هر
     * صفحه)، ردیف آیکون‌های ناوبری (Collection/Greenhouse/Store/Quests/
     * Minigames)، و ویجت‌های سکه/الماس بالا-راست.
     */
    private Table buildHeader(Skin skin, Runnable backAction) {
        User user = facade().getCurrentUser();

        Table header = new Table();
        header.pad(14f, 18f, 0f, 18f);

        Table left = new Table();
        left.add(iconButton(AssetIds.ADV_ICON_BACK, backAction)).size(60f, 57f).padRight(10f);
        left.add(iconButton(AssetIds.ADV_ICON_COLLECTION, () -> goTo(ScreenId.COLLECTION))).size(56f).padRight(8f);
        left.add(iconButton(AssetIds.ADV_ICON_GREENHOUSE, () -> goTo(ScreenId.GREENHOUSE))).size(56f).padRight(8f);
        left.add(iconButton(AssetIds.ADV_ICON_STORE, () -> goTo(ScreenId.SHOP))).size(56f).padRight(8f);
        left.add(iconButton(AssetIds.ADV_ICON_QUESTS, () -> goTo(ScreenId.QUEST))).size(56f).padRight(8f);
        // \u0622\u06cc\u06a9\u0646\u0650 \u0645\u06cc\u0646\u06cc\u200c\u06af\u06cc\u0645\u200c\u0647\u0627 \u0628\u0647 \u0635\u0641\u062d\u0647\u200c\u06cc Quests (\u06a9\u0647 \u062a\u0628\u0650 minigames \u0631\u0627 \u062f\u0627\u0631\u062f) \u0645\u06cc\u200c\u0631\u0648\u062f.
        left.add(iconButton(AssetIds.ADV_ICON_MINIGAMES,
                () -> goTo(ScreenId.QUEST))).size(56f);

        long coins = user != null ? user.getCoins() : 0;
        int gems = user != null ? user.getGems() : 0;

        Table right = new Table();
        right.add(currencyWidget(AssetIds.ADV_WIDGET_COIN, 170f, 54f, String.valueOf(coins), Color.GOLD,
                () -> goTo(ScreenId.SHOP))).padRight(10f);
        right.add(currencyWidget(AssetIds.ADV_WIDGET_GEM, 150f, 56f, String.valueOf(gems), Color.CYAN,
                () -> goTo(ScreenId.SHOP)));

        header.add(left).left().expandX();
        header.add(right).right();
        return header;
    }

    private Image iconButton(String localAsset, Runnable action) {
        TextureRegion region = GameAssets.getInstance().local(localAsset);
        return clickableImage(region, Scaling.fit, action);
    }

    /**
     * Image ساده و قابل‌کلیک با مقیاس‌بندی دلخواه — برخلاف ImageButton،
     * وقتی سایز سلولش رو با {@code .size(...)} عوض کنیم درست fit/stretch
     * می‌شه (ImageButton گاهی آیکون داخلیش رو در سایز اصلی خودش ثابت نگه
     * می‌داره و کش می‌کنه).
     */
    private Image clickableImage(TextureRegion region, Scaling scaling, Runnable action) {
        Image img = new Image(region);
        img.setScaling(scaling);
        img.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) { action.run(); }
        });
        return img;
    }

    /**
     * ویجت سکه/الماس: تصویر pill کامل (آیکون+نوار مشکی+دکمه سبز افزودن) +
     * عدد روی نوار مشکی. عرض/ارتفاع رو با نسبت واقعی فایل PNG صداش کن.
     * padLeft زیر رو اگه لازم شد با چشم بر اساس عکس خودت تنظیم کن.
     */
    private Stack currencyWidget(String widgetAsset, float w, float h,
                                  String amountText, Color amountColor, Runnable onAdd) {
        TextureRegion region = GameAssets.getInstance().local(widgetAsset);
        Image bg = clickableImage(region, Scaling.stretch, onAdd);

        Label amount = new Label(amountText, GameAssets.getInstance().getSkin(), "default");
        amount.setColor(amountColor);
        Table amountLayer = new Table();
        amountLayer.left();
        amountLayer.add(amount).padLeft(w * 0.28f);

        Stack stack = new Stack();
        stack.setSize(w, h);
        stack.add(bg);
        stack.add(amountLayer);
        return stack;
    }

    private void showToast(ToastActor t) {
        t.setPosition(GameConstants.VIEWPORT_WIDTH / 2f - t.getWidth() * 0.5f, 80);
        stage.addActor(t);
    }

    @Override
    public void render(float delta) {
        GameAssets.getInstance().update();
        ScreenUtils.clear(0.02f, 0.05f, 0.09f, 1);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int w, int h) { stage.getViewport().update(w, h, true); }

    @Override
    public void dispose() { if (stage != null) stage.dispose(); }
}
