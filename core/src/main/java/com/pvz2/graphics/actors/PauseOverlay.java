package com.pvz2.graphics.actors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.model.GameSettings;

/**
 * پنجره‌ی توقفِ بازی — مطابقِ تصویرِ مرجع: پنلِ کاغذیِ مرکزی با توپرِ چمن+آفتاب‌گردان،
 * جعبه‌ی هدفِ مرحله، اسلایدرهای Music / Sound FX، و دکمه‌های Save & Exit / Restart / Resume.
 * پس‌زمینه فقط کمی تیره می‌شود تا صحنه‌ی بازی از پشت دیده شود.
 */
public class PauseOverlay extends Table {

    public interface Listener {
        void onResume();
        void onRestart();
        void onQuit();
    }

    private final Skin skin;
    private final Listener listener;
    private String objective = "Don't let the zombies reach your house!";

    public PauseOverlay(Skin skin, Listener l) {
        super(skin);
        this.skin = skin;
        this.listener = l;
        setFillParent(true);
        // dimِ تیره (نه پنلِ تمام‌صفحه) — صحنه از پشت پیداست
        setBackground(tint(0f, 0f, 0f, 0.55f));
        setTouchable(Touchable.enabled);   // بلعیدنِ کلیک تا از پشتْ کاشت انجام نشود
        rebuild();
    }

    /** متنِ هدفِ مرحله (از GameScreen بر اساسِ نوعِ مرحله پاس داده می‌شود). */
    public void setObjective(String text) {
        if (text != null && !text.isEmpty()) { objective = text; rebuild(); }
    }

    private void rebuild() {
        clearChildren();

        Table panel = new Table(skin);
        panel.setBackground("image_ui_dialog_asset_inner_bkgd_10");
        panel.pad(26, 34, 24, 34);

        Label title = new Label("GAME PAUSED", skin, "big");
        title.setColor(Color.valueOf("5a3a1a"));
        panel.add(title).padBottom(14).row();

        // جعبه‌ی هدف
        Label obj = new Label(objective, skin, "medium");
        obj.setColor(Color.valueOf("4a3a12"));
        obj.setWrap(true);
        obj.setAlignment(com.badlogic.gdx.utils.Align.center);
        Table objBox = new Table(skin);
        objBox.setBackground(tint(0.98f, 0.94f, 0.72f, 1f));
        objBox.pad(16);
        objBox.add(obj).width(440f);
        panel.add(objBox).width(500f).padBottom(18).row();

        // اسلایدرهای صدا
        panel.add(buildAudioControls()).padBottom(18).row();

        // دکمه‌ها
        Table buttons = new Table();
        buttons.add(button("Save & Exit", "brown", listener::onQuit)).width(190).height(50).padRight(14);
        buttons.add(button("Restart",     "brown", listener::onRestart)).width(160).height(50).padRight(14);
        buttons.add(button("Resume",      "green", listener::onResume)).width(160).height(50);
        panel.add(buttons).row();

        // توپر (چمن+چرخ‌دنده) با آفتاب‌گردانِ مرکزی، روی لبه‌ی بالای پنل
        Stack card = new Stack();
        Table base = new Table();
        base.add(panel);
        card.add(base);

        TextureRegion grass = GameAssets.getInstance().local("Exports/pause menu/windowtopper.png");
        TextureRegion sun   = GameAssets.getInstance().local("Exports/pause menu/sunflower_topper.png");
        if (has(grass)) {
            Table topLayer = new Table();
            topLayer.top();
            Stack topper = new Stack();
            Image grassImg = new Image(grass);
            grassImg.setScaling(com.badlogic.gdx.utils.Scaling.fit);
            Table grassCell = new Table(); grassCell.add(grassImg).size(560f, 96f);
            topper.add(grassCell);
            if (has(sun)) {
                Image sunImg = new Image(sun);
                sunImg.setScaling(com.badlogic.gdx.utils.Scaling.fit);
                Table sunCell = new Table(); sunCell.add(sunImg).size(96f, 82f);
                topper.add(sunCell);
            }
            topLayer.add(topper).padTop(-58f);   // بیرون‌زدگی به سمتِ بالای پنل
            card.add(topLayer);
        }

        add(card).center().expand();
    }

    /** دو اسلایدرِ Music / Sound FX؛ اگر اسکین اسلایدر نداشت، به چک‌باکس برمی‌گردد. */
    private Table buildAudioControls() {
        Table t = new Table(skin);
        GameSettings gs = GameSettings.getInstance();
        boolean hasSlider = skin.has("default-horizontal", Slider.SliderStyle.class);

        if (hasSlider) {
            t.add(audioLabel("Music")).right().padRight(12).padBottom(8);
            Slider music = new Slider(0f, 1f, 0.05f, false, skin, "default-horizontal");
            music.setValue(gs.isMusicEnabled() ? gs.getMasterVolume() : 0f);
            music.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent e, com.badlogic.gdx.scenes.scene2d.Actor a) {
                    float v = music.getValue();
                    gs.setMusicEnabled(v > 0.01f);
                    gs.setMasterVolume(v);
                    applyAudio();
                }
            });
            t.add(music).width(300f).padBottom(8).row();

            t.add(audioLabel("Sound FX")).right().padRight(12);
            Slider sfx = new Slider(0f, 1f, 0.05f, false, skin, "default-horizontal");
            sfx.setValue(gs.isSfxEnabled() ? 1f : 0f);
            sfx.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent e, com.badlogic.gdx.scenes.scene2d.Actor a) {
                    gs.setSfxEnabled(sfx.getValue() > 0.01f);
                    applyAudio();
                }
            });
            t.add(sfx).width(300f).row();
        } else {
            CheckBox music = new CheckBox("  Music", skin);
            music.setChecked(gs.isMusicEnabled());
            music.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent e, com.badlogic.gdx.scenes.scene2d.Actor a) {
                    gs.setMusicEnabled(music.isChecked()); applyAudio();
                }
            });
            CheckBox sfx = new CheckBox("  Sound FX", skin);
            sfx.setChecked(gs.isSfxEnabled());
            sfx.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent e, com.badlogic.gdx.scenes.scene2d.Actor a) {
                    gs.setSfxEnabled(sfx.isChecked()); applyAudio();
                }
            });
            t.add(music).left().padBottom(8).row();
            t.add(sfx).left().row();
        }
        return t;
    }

    private void applyAudio() {
        try {
            GameSettings.getInstance().save();
            com.pvz2.graphics.audio.SoundManager.get().applySettings();
        } catch (Exception ignored) { }
    }

    private Label audioLabel(String s) {
        Label l = new Label(s, skin, "medium");
        l.setColor(Color.valueOf("4a3a12"));
        return l;
    }

    private TextButton button(String lbl, String style, Runnable action) {
        TextButton b = new TextButton(lbl, skin, style);
        b.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { action.run(); }
        });
        return b;
    }

    private boolean has(TextureRegion r) {
        return r != null && r != GameAssets.getInstance().getWhiteRegion();
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable tint(float r, float g, float b, float a) {
        // TextureRegionDrawable.tint() یک SpriteDrawable برمی‌گرداند (نه TextureRegionDrawable)،
        // پس نوعِ برگشتی باید Drawable باشد تا cast نترکد.
        return new TextureRegionDrawable(GameAssets.getInstance().getWhiteRegion())
                .tint(new Color(r, g, b, a));
    }

    public void show(Stage stage) { stage.addActor(this); }
    public void hide()            { remove(); }
}
