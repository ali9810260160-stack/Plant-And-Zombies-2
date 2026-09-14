package com.pvz2.graphics.actors;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.pvz2.graphics.assets.CollectionAssetPaths;
import com.pvz2.graphics.assets.CollectionAssets;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.model.enums.ZombieType;

/**
 * کارت زامبی در گرید صفحه‌ی کلکسیون (تب Zombies).
 *
 * <p>قاب {@code ready.png} همیشه رسم می‌شه؛ اگر زامبی قبلاً در بازی دیده شده
 * باشه ({@code GameFacade#getSeenZombies()}) تصویرش داخل قاب نمایش داده می‌شه،
 * وگرنه قاب خالی می‌مونه (سیلوئت/جای‌خالی).
 */
public class ZombieAlmanacCardActor extends Stack {

    public static final float CARD_W = 100f;
    public static final float CARD_H = 140f;

    private final ZombieType zombieType;
    private final Image frameImage;
    private final Image zombieImage;
    private boolean seen;

    public ZombieAlmanacCardActor(ZombieType type) {
        this.zombieType = type;
        setSize(CARD_W, CARD_H);

        CollectionAssets assets = CollectionAssets.getInstance();

        TextureRegion frameRegion = assets.region(CollectionAssetPaths.ZOMBIE_READY_FRAME);
        frameImage = frameRegion != null
                ? new Image(frameRegion)
                : new Image(GameAssets.getInstance().getWhiteRegion());
        if (frameRegion == null) frameImage.setColor(0.15f, 0.15f, 0.18f, 1f);
        add(frameImage);

        TextureRegion zRegion = assets.region(CollectionAssetPaths.zombiePath(type));
        Table inner = new Table();
        if (zRegion != null) {
            zombieImage = new Image(zRegion);
            inner.add(zombieImage).expand().size(CARD_W * 0.7f, CARD_H * 0.72f);
        } else {
            zombieImage = null;
        }
        add(inner);

        setSeen(false);
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
        if (zombieImage != null) zombieImage.setVisible(seen);
    }

    public void onClick(Runnable action) {
        addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { action.run(); }
        });
    }

    public ZombieType getZombieType() { return zombieType; }
    public boolean isSeen()           { return seen; }
}
