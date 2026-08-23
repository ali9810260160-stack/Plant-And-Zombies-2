package com.pvz2.graphics.actors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import pvz.skin.BorderedTable;

/**
 * پاپ‌آپ خطا — یه دکمه‌ی OK. برای وقتی که خرید ممکن نیست (مثلاً سکه/الماس
 * کافی نیست) قبل از نمایش تاییدیه خرید.
 */
public class ErrorPopup extends BorderedTable {

    public ErrorPopup(String title, String message, Skin skin) {
        Label titleLbl = new Label(title, skin, "medium");
        titleLbl.setColor(Color.FIREBRICK);
        add(titleLbl).padBottom(10).row();

        Label messageLbl = new Label(message, skin, "default");
        messageLbl.setWrap(true);
        add(messageLbl).width(300).padBottom(20).row();

        TextButton okButton = new TextButton("OK", skin, "brown");
        okButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { remove(); }
        });
        add(okButton).size(120, 50);
    }

    public ErrorPopup show(Stage stage) {
        pack();
        setPosition((stage.getWidth() - getWidth()) / 2f, (stage.getHeight() - getHeight()) / 2f);
        stage.addActor(this);
        return this;
    }
}
