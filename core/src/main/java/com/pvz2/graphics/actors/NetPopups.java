package com.pvz2.graphics.actors;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.assets.GameAssets;

import pvz.skin.BorderedTable;

/**
 * Modal network popups shown on the global overlay stage (invite challenge,
 * match-found, generic notice). Built as a dim + {@code BorderedTable} group
 * (scene2d {@code Dialog} crashes with this skin — no Window style).
 */
public final class NetPopups {

    private NetPopups() { }

    /** Incoming-challenge popup with Accept / Reject. */
    public static Group invite(Skin skin, String fromUser, Runnable onAccept, Runnable onReject) {
        BorderedTable panel = new BorderedTable();
        panel.pad(22);
        panel.add(new Label("Multiplayer Challenge", skin, "medium")).padBottom(14).row();
        panel.add(new Label(fromUser + " challenges you to I, Zombie!", skin)).padBottom(6).row();
        panel.add(new Label("Do you accept?", skin)).padBottom(16).row();

        Table buttons = new Table();
        TextButton accept = new TextButton("Accept", skin, "green");
        accept.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { if (onAccept != null) onAccept.run(); }
        });
        TextButton reject = new TextButton("Reject", skin, "brown");
        reject.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { if (onReject != null) onReject.run(); }
        });
        buttons.add(reject).width(130f).height(48f).padRight(14);
        buttons.add(accept).width(130f).height(48f);
        panel.add(buttons);
        return wrap(panel);
    }

    /** Match-found notice (single OK). */
    public static Group matchFound(Skin skin, String opponent, String role, Runnable onOk) {
        BorderedTable panel = new BorderedTable();
        panel.pad(22);
        panel.add(new Label("Match Found!", skin, "medium")).padBottom(14).row();
        panel.add(new Label("Opponent: " + opponent, skin)).padBottom(4).row();
        panel.add(new Label("You play as: " + role, skin)).padBottom(16).row();
        TextButton ok = new TextButton("OK", skin, "green");
        ok.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { if (onOk != null) onOk.run(); }
        });
        panel.add(ok).width(150f).height(48f);
        return wrap(panel);
    }

    /** Generic single-button notice. */
    public static Group info(Skin skin, String title, String message, Runnable onOk) {
        BorderedTable panel = new BorderedTable();
        panel.pad(22);
        panel.add(new Label(title, skin, "medium")).padBottom(12).row();
        panel.add(new Label(message, skin)).padBottom(16).row();
        TextButton ok = new TextButton("OK", skin, "green");
        ok.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { if (onOk != null) onOk.run(); }
        });
        panel.add(ok).width(150f).height(48f);
        return wrap(panel);
    }

    /** Wrap a panel in a full-screen dim group that swallows input behind it. */
    private static Group wrap(BorderedTable panel) {
        final Group overlay = new Group();
        overlay.setSize(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT);
        Image dim = new Image(new TextureRegionDrawable(GameAssets.getInstance().getWhiteRegion()));
        dim.setColor(0f, 0f, 0f, 0.6f);
        dim.setSize(overlay.getWidth(), overlay.getHeight());
        dim.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent e, float x, float y, int p, int b) { return true; }
        });
        overlay.addActor(dim);
        panel.pack();
        panel.setPosition((overlay.getWidth() - panel.getWidth()) / 2f,
                          (overlay.getHeight() - panel.getHeight()) / 2f);
        overlay.addActor(panel);
        return overlay;
    }
}
