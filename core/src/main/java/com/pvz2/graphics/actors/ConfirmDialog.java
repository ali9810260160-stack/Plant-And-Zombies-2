package com.pvz2.graphics.actors;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import pvz.skin.BorderedTable;

/**
 * دیالوگ تأیید عمومی (خرید، خروج، ...) — شبیه «Purchase Confirmation» سند فاز ۲.
 * <p>
 * عمداً روی {@link BorderedTable} ساخته شده، نه {@code com.badlogic.gdx.scenes.scene2d.ui.Dialog}،
 * چون اسکین pvz-skin استایل Window ندارد و Dialog با استایل "default" کرش می‌کند.
 */
public class ConfirmDialog extends BorderedTable {

    private Runnable onConfirmCallback;

    public ConfirmDialog(String title, String message, String confirmLabel, Skin skin) {
        add(new Label(title, skin, "medium")).colspan(2).padBottom(10).row();

        Label messageLabel = new Label(message, skin, "default");
        messageLabel.setWrap(true);
        add(messageLabel).width(320).colspan(2).padBottom(20).row();

        TextButton confirmButton = new TextButton(confirmLabel, skin, "green");
        confirmButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                remove();
                if (onConfirmCallback != null) onConfirmCallback.run();
            }
        });
        add(confirmButton).size(140, 60).padRight(10);

        TextButton cancelButton = new TextButton("Cancel", skin, "brown");
        cancelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                remove();
            }
        });
        add(cancelButton).size(140, 60);
    }

    public ConfirmDialog onConfirm(Runnable callback) {
        this.onConfirmCallback = callback;
        return this;
    }

    public ConfirmDialog show(Stage stage) {
        pack();
        setPosition((stage.getWidth() - getWidth()) / 2f, (stage.getHeight() - getHeight()) / 2f);
        stage.addActor(this);
        return this;
    }
}
