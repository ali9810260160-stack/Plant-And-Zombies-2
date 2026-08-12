package com.pvz2.graphics.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.pvz2.PVZApplication;
import com.pvz2.graphics.*;
import com.pvz2.graphics.assets.AssetIds;
import com.pvz2.graphics.assets.GameAssets;

/**
 * انتخاب چپتر و مرحله برای ماجراجویی — چیدمان کارتی مطابق سبک اصلی PvZ2
 * (تصویر ۱ سند: کارت‌های رنگی با آیکون، نام و وضعیت قفل/باز).
 * <p>
 * وضعیت باز/قفل و پیشرفت هر چپتر مستقیم از {@link #facade()}.isLevelUnlocked
 * فاز یک خونده می‌شه — چیزی شبیه‌سازی/حدسی نیست.
 */
public class AdventureScreen extends BaseScreen {

    private static final int LEVELS_PER_CHAPTER = 4;

    private static final String[][] CHAPTERS = {
        // {enum_key, نام نمایشی, شناسه‌ی آیکون}
        {"ANCIENT_EGYPT",   "Ancient Egypt",   AssetIds.ICON_CHAPTER_ANCIENT_EGYPT},
        {"FROSTBITE_CAVES", "Frostbite Caves", AssetIds.ICON_CHAPTER_FROSTBITE_CAVES},
        {"BIG_WAVE_BEACH",  "Big Wave Beach",  AssetIds.ICON_CHAPTER_BIG_WAVE_BEACH},
        {"DARK_AGES",       "Dark Ages",       AssetIds.ICON_CHAPTER_DARK_AGES},
    };

    private static final String[] LEVEL_NAMES = {
        "Level 1 \u2014 Normal",
        "Level 2 \u2014 Special",
        "Level 3 \u2014 Special",
        "Level 4 \u2014 Boss",
    };

    private Stage stage;

    public AdventureScreen(PVZApplication game) { super(game); }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        showChapters();
    }

    private void showChapters() {
        stage.clear();
        Skin skin = GameAssets.getInstance().getSkin();
        Table root = root(skin);
        titleLbl(root, skin, "Adventure \u2014 Select World");

        Table row = new Table();
        row.defaults().pad(10).top();
        for (String[] ch : CHAPTERS) {
            row.add(buildChapterCard(skin, ch[0], ch[1], ch[2])).width(200).height(240);
        }
        root.add(row).padBottom(20).row();

        backBtn(root, skin, () -> goTo(ScreenId.MAIN_MENU));
        stage.addActor(root);
    }

    /** کارت چپتر: آیکون، نام، پیشرفت (x/4)، و اگه چپتر باز نباشه یه لایه‌ی خاکستری + قفل. */
    private Table buildChapterCard(Skin skin, String key, String name, String iconResourceId) {
        boolean chapterOpen = facade().isLevelUnlocked(key, 1);
        int unlockedCount = countUnlockedLevels(key);

        Table card = new Table(skin);
        card.setBackground("image_ui_cards_almanac_plant_card_10");
        card.top().pad(10);

        if (!iconResourceId.isEmpty()) {
            TextureRegion icon = GameAssets.getInstance().region(iconResourceId);
            card.add(new Image(icon)).size(96).padBottom(8).row();
        } else {
            card.add().height(96).padBottom(8).row();
        }

        Label nameLabel = new Label(name, skin, "default");
        nameLabel.setWrap(true);
        card.add(nameLabel).width(170).padBottom(6).row();

        Label progressLabel = new Label(unlockedCount + "/" + LEVELS_PER_CHAPTER, skin, "default");
        progressLabel.setColor(unlockedCount == LEVELS_PER_CHAPTER ? Color.GREEN : Color.LIGHT_GRAY);
        card.add(progressLabel).padBottom(10).row();

        if (chapterOpen) {
            TextButton openButton = new TextButton("Play", skin, "green");
            openButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent e, float x, float y) {
                    showLevels(key, name);
                }
            });
            card.add(openButton).size(120, 50);
        } else {
            if (!AssetIds.ICON_LOCK.isEmpty()) {
                card.add(new Image(GameAssets.getInstance().region(AssetIds.ICON_LOCK))).size(40);
            } else {
                Label lockLabel = new Label("Locked", skin, "default");
                lockLabel.setColor(Color.GRAY);
                card.add(lockLabel);
            }
            card.setColor(0.7f, 0.7f, 0.7f, 1f);
        }

        return card;
    }

    private int countUnlockedLevels(String chapterKey) {
        int count = 0;
        for (int level = 1; level <= LEVELS_PER_CHAPTER; level++) {
            if (facade().isLevelUnlocked(chapterKey, level)) count++;
        }
        return count;
    }

    private void showLevels(String chapterKey, String chapterName) {
        stage.clear();
        Skin skin = GameAssets.getInstance().getSkin();
        Table root = root(skin);
        titleLbl(root, skin, chapterName + " \u2014 Select Level");

        for (int i = 0; i < LEVELS_PER_CHAPTER; i++) {
            int level = i + 1;
            boolean unlocked = facade().isLevelUnlocked(chapterKey, level);
            addLevelRow(root, skin, chapterKey, level, LEVEL_NAMES[i], unlocked);
        }
        backBtn(root, skin, this::showChapters);
        stage.addActor(root);
    }

    private void addLevelRow(Table root, Skin skin, String chapterKey,
                              int level, String name, boolean unlocked) {
        TextButton btn = new TextButton(unlocked ? level + ".  " + name : "Locked \u2014 " + name,
                skin, unlocked ? "brown" : "default");
        btn.setDisabled(!unlocked);
        btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                if (unlocked) {
                    game.setPlantSelectParams(chapterKey, level);
                    goTo(ScreenId.PLANT_SELECT);
                }
            }
        });
        root.add(btn).fillX().height(52).padBottom(10).row();
    }

    private Table root(Skin skin) {
        Table t = new Table(skin);
        t.setFillParent(true);
        t.pad(32);
        return t;
    }

    private void titleLbl(Table t, Skin skin, String text) {
        Label l = new Label(text, skin, "big_outline");
        t.add(l).padBottom(24).row();
    }

    private void backBtn(Table root, Skin skin, Runnable action) {
        TextButton b = new TextButton("Back", skin, "brown");
        b.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) { action.run(); }
        });
        root.add(b).size(140, 60).padTop(16).row();
    }

    @Override
    public void render(float delta) {
        GameAssets.getInstance().update();
        ScreenUtils.clear(0.06f, 0.04f, 0.08f, 1);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int w, int h) { stage.getViewport().update(w, h, true); }

    @Override
    public void dispose() { if (stage != null) stage.dispose(); }
}
