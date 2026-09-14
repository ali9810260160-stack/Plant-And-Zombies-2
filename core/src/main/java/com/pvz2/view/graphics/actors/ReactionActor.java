package com.pvz2.graphics.actors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

import com.pvz2.graphics.net.ReactionCatalog;

/**
 * A single incoming reaction shown in the corner of the opponent's screen: it
 * pops in, holds, then fades out and removes itself. Stickers additionally pulse
 * for an "animated" feel (doc bonus). Built from skin labels only.
 */
public final class ReactionActor extends Container<Label> {

    public ReactionActor(Skin skin, String kind, String value, String fromName) {
        boolean big = ReactionCatalog.KIND_EMOJI.equals(kind)
                || ReactionCatalog.KIND_STICKER.equals(kind);
        String text = ReactionCatalog.KIND_TEXT.equals(kind)
                ? (fromName != null ? fromName + ": " + value : value)
                : value;

        Label label = new Label(text, skin, big ? "big" : "medium");
        label.setColor(Color.WHITE);
        setActor(label);
        pad(10, 16, 10, 16);

        // subtle dark plate behind the text for readability
        if (skin.has("image_ui_dialog_asset_inner_bkgd_10", Drawable.class)) {
            setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        }
        pack();

        // pop in → hold → fade out → remove
        setTransform(true);
        setOrigin(getWidth() / 2f, getHeight() / 2f);
        setScale(0.3f);
        getColor().a = 0f;
        addAction(Actions.sequence(
                Actions.parallel(Actions.fadeIn(0.18f),
                        Actions.scaleTo(1f, 1f, 0.22f, com.badlogic.gdx.math.Interpolation.swingOut)),
                Actions.delay(2.4f),
                Actions.parallel(Actions.fadeOut(0.5f), Actions.moveBy(0, 18f, 0.5f)),
                Actions.removeActor()));

        // stickers pulse while visible (removed with the actor)
        if (ReactionCatalog.KIND_STICKER.equals(kind)) {
            addAction(Actions.forever(Actions.sequence(
                    Actions.scaleBy(0.12f, 0.12f, 0.35f),
                    Actions.scaleBy(-0.12f, -0.12f, 0.35f))));
        }
    }
}
