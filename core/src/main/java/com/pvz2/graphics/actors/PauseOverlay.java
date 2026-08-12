package com.pvz2.graphics.actors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/** پنجره توقف بازی — نمایش روی HUD Stage. */
public class PauseOverlay extends Table {

    public interface Listener {
        void onResume();
        void onRestart();
        void onQuit();
    }

    public PauseOverlay(com.badlogic.gdx.scenes.scene2d.ui.Skin skin, Listener l) {
        super(skin);
        setFillParent(true);
        setBackground("image_ui_dialog_asset_inner_bkgd_10");
        buildContent(skin, l);
    }

    private void buildContent(com.badlogic.gdx.scenes.scene2d.ui.Skin skin, Listener l) {
        Table dlg = new Table(skin);
        dlg.setBackground("image_ui_dialog_asset_inner_bkgd_10");
        dlg.pad(28);

        Label title = new Label("PAUSED", skin, "big");
        title.setColor(Color.YELLOW);
        dlg.add(title).padBottom(22).row();

        addBtn(dlg, skin, "▶  Resume", "green", l::onResume);
        addBtn(dlg, skin, "🔄  Restart", "brown", l::onRestart);
        addBtn(dlg, skin, "🚪  Exit Level", null, l::onQuit);

        add(dlg).center().expand();
    }

    private void addBtn(Table t, com.badlogic.gdx.scenes.scene2d.ui.Skin skin,
                         String lbl, String style, Runnable action) {
        TextButton btn = style != null ? new TextButton(lbl, skin, style)
                                      : new TextButton(lbl, skin);
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { action.run(); }
        });
        t.add(btn).width(240).height(52).pad(6).row();
    }

    public void show(Stage stage) { stage.addActor(this); }
    public void hide()            { remove(); }
}
