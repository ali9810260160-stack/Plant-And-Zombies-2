package com.pvz2.graphics.screens;

import com.badlogic.gdx.Gdx;
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
import com.pvz2.graphics.actors.ToastActor;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.graphics.util.GameConfig;
import com.pvz2.model.AppState;
import com.pvz2.model.User;
import pvz.skin.BorderedTable;

/**
 * منوی تنظیمات — طبق سند: سرعت پیشروی بازی (۱× تا ۳×)، نمایش شبکه‌بندی زمین،
 * و حالت دیباگ (تقلب سکه/الماس/plant food) — علاوه بر انتخاب میزان سختی
 * (به همون شکل فاز یک). منطق ذخیره‌سازی (روی {@link User}) از فاز یک است.
 */
public class SettingsScreen extends BaseScreen {

    private Stage stage;

    public SettingsScreen(PVZApplication game) { super(game); }

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
        root.add(new Label("Settings", skin, "big_outline")).padBottom(20).row();

        BorderedTable panel = new BorderedTable();
        panel.pad(30);

        addDifficultyRow(panel, skin);
        addSpeedRow(panel, skin);
        addVolumeRow(panel, skin);
        addToggle(panel, skin, "Show grid lines (red overlay in-game)",
                GameConfig.showGrid, val -> GameConfig.showGrid = val);
        addToggle(panel, skin, "Debug mode (cheat buttons in-game)",
                GameConfig.debugMode, val -> GameConfig.debugMode = val);
        com.pvz2.model.GameSettings gs = com.pvz2.model.GameSettings.getInstance();
        addToggle(panel, skin, "Music", gs.isMusicEnabled(), val -> {
            gs.setMusicEnabled(val);
            com.pvz2.graphics.audio.SoundManager.get().applySettings();
            if (val) com.pvz2.graphics.audio.SoundManager.get()
                    .playMusic(com.pvz2.graphics.audio.SoundManager.MUSIC_MENU);
        });
        addToggle(panel, skin, "Sound effects", gs.isSfxEnabled(), gs::setSfxEnabled);
        addSaveButton(panel, skin);

        root.add(panel).padBottom(20).row();

        TextButton back = new TextButton("Back", skin, "brown");
        back.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                // اگر کاربر لاگین نیست (ورود از صفحه‌ی Welcome) به Welcome برگرد، نه منوی اصلی.
                goTo(facade().getCurrentUser() != null ? ScreenId.MAIN_MENU : ScreenId.WELCOME);
            }
        });
        root.add(back).size(140, 60);
        stage.addActor(root);
    }

    private void addDifficultyRow(Table root, Skin skin) {
        root.add(new Label("Difficulty (1-5):", skin, "default")).right().padRight(16).padBottom(16);

        Table btns = new Table();
        for (int d = 1; d <= 5; d++) {
            final int diff = d;
            boolean active = d == GameConfig.difficultyLevel;
            TextButton btn = new TextButton(String.valueOf(d), skin, active ? "green" : "brown");
            btn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    GameConfig.difficultyLevel = diff;
                    saveDifficultyToUser(diff);
                    stage.clear();
                    buildUi();
                }
            });
            btns.add(btn).size(52, 44).padRight(5);
        }
        root.add(btns).left().padBottom(16).row();
    }

    /** ذخیره سطح سختی روی User فاز یک. */
    private void saveDifficultyToUser(int diff) {
        try {
            User user = AppState.getInstance().getCurrentUser();
            if (user == null) return;
            user.setDifficultyLevel(diff);
            ServiceLocator.getInstance().getUserRepository().save(user);
        } catch (Exception ignored) { }
    }

    private void addSpeedRow(Table root, Skin skin) {
        root.add(new Label("Game Speed:", skin, "default")).right().padRight(16).padBottom(16);

        Table btns = new Table();
        String[] labels = {"1x", "1.5x", "2x"};
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            boolean active = i == GameConfig.gameSpeed;
            TextButton btn = new TextButton(labels[i], skin, active ? "green" : "brown");
            btn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    GameConfig.gameSpeed = idx;
                    stage.clear();
                    buildUi();
                }
            });
            btns.add(btn).size(88, 44).padRight(6);
        }
        root.add(btns).left().padBottom(16).row();
    }

    /**
     * اسلایدرِ کنترلِ میزانِ صدا (بلوک ۵). زنده روی {@link com.pvz2.model.GameSettings}
     * و SoundManager اعمال می‌شود و با دکمه‌ی Save به‌ازای هر user سیو می‌شود.
     * اگر اسکین سبکِ Slider نداشت، به دکمه‌های پیش‌تنظیم برمی‌گردد.
     */
    private void addVolumeRow(Table root, Skin skin) {
        root.add(new Label("Sound Volume:", skin, "default")).right().padRight(16).padBottom(16);
        final com.pvz2.model.GameSettings gs = com.pvz2.model.GameSettings.getInstance();

        Table row = new Table();
        if (skin.has("default-horizontal", Slider.SliderStyle.class)) {
            final Slider slider = new Slider(0f, 1f, 0.05f, false, skin, "default-horizontal");
            slider.setValue(gs.getMasterVolume());
            final Label pct = new Label(pct(gs.getMasterVolume()), skin, "default");
            slider.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent e, Actor a) {
                    applyVolume(slider.getValue());
                    pct.setText(pct(slider.getValue()));
                }
            });
            row.add(slider).width(230).padRight(10);
            row.add(pct).width(54);
        } else {
            // fallback: دکمه‌های پیش‌تنظیم اگر اسکین Slider نداشت.
            int[] presets = {0, 25, 50, 75, 100};
            for (int p : presets) {
                final float v = p / 100f;
                boolean active = Math.round(gs.getMasterVolume() * 100) == p;
                TextButton b = new TextButton(p + "%", skin, active ? "green" : "brown");
                b.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent e, float x, float y) {
                        applyVolume(v);
                        stage.clear();
                        buildUi();
                    }
                });
                row.add(b).size(58, 40).padRight(4);
            }
        }
        root.add(row).left().padBottom(16).row();
    }

    private String pct(float v) { return Math.round(v * 100) + "%"; }

    /** اعمالِ زنده‌ی میزانِ صدا روی GameSettings + GameConfig + SoundManager. */
    private void applyVolume(float v) {
        com.pvz2.model.GameSettings.getInstance().setMasterVolume(v);
        GameConfig.musicVolume = v;
        com.pvz2.graphics.audio.SoundManager.get().applySettings();
    }

    private void addToggle(Table root, Skin skin, String label, boolean current, BoolSetter setter) {
        root.add(new Label(label + ":", skin, "default")).right().padRight(16).padBottom(14);
        CheckBox cb = new CheckBox("", skin);
        cb.setChecked(current);
        cb.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { setter.set(cb.isChecked()); }
        });
        root.add(cb).left().padBottom(14).row();
    }

    private void addSaveButton(Table root, Skin skin) {
        TextButton save = new TextButton("Save Settings", skin, "green");
        save.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                persistSettings();
                showToast(ToastActor.success("Settings saved"));
            }
        });
        root.add(save).colspan(2).width(240).height(50).padTop(20);
    }

    /**
     * تنظیمات را از GameConfig به GameSettings آینه می‌کند و ذخیره می‌کند —
     * {@code GameSettings.save()} علاوه بر فایلِ سراسری، از طریق persister روی
     * کاربرِ لاگین‌شده هم سیو می‌کند (per-user، رفعِ TODO فاز ۲).
     */
    private void persistSettings() {
        com.pvz2.model.GameSettings gs = com.pvz2.model.GameSettings.getInstance();
        gs.setShowGrid(GameConfig.showGrid);
        // ایندکسِ سرعت (0/1/2) به ضریبِ float نگاشت می‌شود.
        float[] speeds = {1.0f, 1.5f, 2.0f};
        int idx = Math.max(0, Math.min(speeds.length - 1, GameConfig.gameSpeed));
        gs.setGameSpeed(speeds[idx]);
        gs.setMasterVolume(GameConfig.musicVolume);
        gs.save();
    }

    private void showToast(ToastActor t) {
        t.setPosition(GameConstants.VIEWPORT_WIDTH / 2f - t.getWidth() * 0.5f, 100);
        stage.addActor(t);
    }

    @FunctionalInterface
    private interface BoolSetter { void set(boolean value); }

    @Override
    public void render(float delta) {
        GameAssets.getInstance().update();
        ScreenUtils.clear(0.06f, 0.05f, 0.10f, 1);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void dispose() { if (stage != null) stage.dispose(); }
}
