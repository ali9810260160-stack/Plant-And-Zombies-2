package com.pvz2.graphics.actors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.assets.AssetIds;
import com.pvz2.graphics.assets.GameAssets;

/**
 * صفحه‌ی برد/باخت (BK2). روی HUD Stage نمایش داده می‌شود.
 *
 * <p><b>باخت (BK5):</b> صفحه اندکی سیاه می‌شود (آلفای کم تا مپ دیده شود)، تصویر
 * مغزِ خورده‌شده روی بشقاب به‌همراه نوشته‌ی باخت نمایش داده می‌شود و دکمه‌ی
 * «شروع مجدد» این مرحله (BM3/BM5) موجود است.
 * <p><b>برد:</b> نوشته‌ی برد نمایش داده می‌شود.
 */
public class EndScreenOverlay extends Table {

    public interface Listener {
        void onRestart();
        void onExit();
    }

    public EndScreenOverlay(Skin skin, boolean won, Listener l) {
        super(skin);
        setFillParent(true);

        // ─── لایه‌ی تیره‌کننده (آلفای کم تا مپ زیر آن دیده شود) ───
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        Image dim = new Image(new TextureRegionDrawable(white));
        // باخت کمی تیره‌تر از برد
        dim.setColor(0f, 0f, 0f, won ? 0.30f : 0.42f);
        dim.setFillParent(true);
        // جذبِ کلیک‌ها تا به شبکه‌ی زیرِ overlay نرسند
        dim.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override public boolean touchDown(InputEvent e, float x, float y,
                                               int pointer, int button) { return true; }
        });
        addActor(dim);

        // ─── محتوای مرکزی ───
        Table content = new Table(skin);
        content.setFillParent(true);
        addActor(content);

        if (!won) {
            Image brain = localImage(AssetIds.END_FAIL_BRAIN);
            if (brain != null) {
                content.add(brain).size(320, 320).padBottom(6).row();
            }
        }

        Image text = localImage(won ? AssetIds.END_WON_TEXT : AssetIds.END_LOST_TEXT);
        if (text != null) {
            float w = won ? 560f : 500f;
            float h = won ? w * 216f / 715f : w * 208f / 699f;
            content.add(text).size(w, h).padBottom(26).row();
        } else {
            // پشتیبانِ متنی اگر asset پیدا نشد
            com.badlogic.gdx.scenes.scene2d.ui.Label lbl =
                    new com.badlogic.gdx.scenes.scene2d.ui.Label(
                            won ? "YOU WON!" : "YOU LOST!", skin, "big");
            lbl.setColor(won ? Color.YELLOW : new Color(1f, 0.4f, 0.4f, 1f));
            content.add(lbl).padBottom(26).row();
        }

        Table buttons = new Table(skin);
        if (!won) {
            addBtn(buttons, skin, "🔄  Try Again", "green", l::onRestart);
        }
        addBtn(buttons, skin, "🚪  Exit", "brown", l::onExit);
        content.add(buttons);
    }

    private Image localImage(String path) {
        TextureRegion r = GameAssets.getInstance().local(path);
        if (r == null) return null;
        Image img = new Image(new TextureRegionDrawable(r));
        img.setScaling(Scaling.fit);
        return img;
    }

    private void addBtn(Table t, Skin skin, String lbl, String style, Runnable action) {
        TextButton btn = new TextButton(lbl, skin, style);
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { action.run(); }
        });
        t.add(btn).width(220).height(56).pad(8);
    }

    public void show(Stage stage) {
        setSize(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT);
        stage.addActor(this);
    }
}
