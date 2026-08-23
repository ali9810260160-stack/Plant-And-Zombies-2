package com.pvz2.graphics.actors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.assets.GameAssets;

/**
 * اعلان موقت (toast) که بعد از مدتی محو می‌شود.
 *
 * <p>برای استفاده:
 * <pre>
 * ToastActor t = new ToastActor("پیام", skin);
 * t.setPosition(x, y);
 * stage.addActor(t);
 * </pre>
 */
public class ToastActor extends Group {

    private static final float SHOW = GameConstants.TOAST_DURATION - 0.5f;
    private static final float FADE = 0.5f;

    public ToastActor(String message, Skin skin) {
        Label lbl = new Label(message, skin, "default");
        lbl.setColor(Color.WHITE);

        Table bg = new Table(skin);
        bg.setBackground("image_ui_dialog_asset_inner_bkgd_10");
        bg.add(lbl).pad(8, 20, 8, 20);
        bg.pack();

        addActor(bg);
        setSize(bg.getWidth(), bg.getHeight());

        addAction(Actions.sequence(
                Actions.delay(SHOW),
                Actions.fadeOut(FADE),
                Actions.removeActor()
        ));
    }

    /** Toast قرمز برای خطا */
    public static ToastActor error(String message) {
        Skin skin = GameAssets.getInstance().getSkin();
        ToastActor t = new ToastActor(message, skin);
        t.setColor(1f, 0.3f, 0.3f, 1f);
        return t;
    }

    /** Toast سبز برای موفقیت */
    public static ToastActor success(String message) {
        Skin skin = GameAssets.getInstance().getSkin();
        ToastActor t = new ToastActor(message, skin);
        t.setColor(0.3f, 1f, 0.3f, 1f);
        return t;
    }

    /** Toast آبی برای پیام اطلاع‌رسانی خنثی */
    public static ToastActor info(String message) {
        Skin skin = GameAssets.getInstance().getSkin();
        ToastActor t = new ToastActor(message, skin);
        t.setColor(0.45f, 0.7f, 1f, 1f);
        return t;
    }
}
