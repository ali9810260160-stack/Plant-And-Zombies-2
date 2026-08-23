package com.pvz2.graphics.actors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.pvz2.graphics.assets.CollectionAssetPaths;
import com.pvz2.graphics.assets.CollectionAssets;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.graphics.assets.PlantSelectAssetPaths;
import com.pvz2.model.enums.PlantType;

/**
 * کارت گیاه در صفحه‌ی انتخاب گیاه (Seed Selection) — هم در گرید قابل‌اسکرول
 * انتخاب و هم در ۸ اسلات گیاهان انتخاب‌شده استفاده می‌شه.
 *
 * <p>حالت‌های ظاهری:
 * <ul>
 *   <li><b>قفل</b>: لایه‌ی خاکستری + آیکون قفل طلایی، غیرقابل‌کلیک منطقی (کنترلش با صفحه‌ست)</li>
 *   <li><b>boost شده</b>: پس‌زمینه با پرتوهای طلایی ({@code boostcard.png})</li>
 *   <li><b>highlighted/انتخاب‌شده</b>: قاب سبز گوشه‌دار ({@code select.png}) دور کارت</li>
 * </ul>
 */
public class PlantSelectCardActor extends Stack {

    public static final float CARD_W = 96f;
    public static final float CARD_H = 122f;

    private final PlantType plantType;
    private final Skin skin;
    private final boolean showSunCost;

    private final Image backgroundImage;
    private final Image plantImage;
    private final Label levelLabel;
    private final Label sunCostLabel;
    private final Image lockOverlay;
    private final Image lockIcon;
    private final Image selectFrame;

    private boolean locked;
    private boolean boosted;
    private boolean highlighted;

    public PlantSelectCardActor(PlantType type, Skin skin) { this(type, skin, true); }

    public PlantSelectCardActor(PlantType type, Skin skin, boolean showSunCost) {
        this.plantType = type;
        this.skin = skin;
        this.showSunCost = showSunCost;
        setSize(CARD_W, CARD_H);

        CollectionAssets assets = CollectionAssets.getInstance();

        backgroundImage = new Image();
        applyBackground(false);
        add(backgroundImage);

        Table content = new Table();
        content.top();

        Table topRow = new Table();
        levelLabel = new Label("", skin, hasLabelStyle("small") ? "small" : "default");
        levelLabel.setColor(Color.GOLD);
        topRow.add(levelLabel).left().pad(3);
        content.add(topRow).expandX().fillX().row();

        TextureRegion plantRegion = assets.region(CollectionAssetPaths.plantPath(type));
        plantImage = plantRegion != null
                ? new Image(plantRegion)
                : new Image(GameAssets.getInstance().getWhiteRegion());
        content.add(plantImage).expand().size(CARD_W * 0.62f, CARD_H * 0.5f).row();

        if (showSunCost) {
            Table bottomRow = new Table();
            sunCostLabel = new Label("", skin, hasLabelStyle("small") ? "small" : "default");
            sunCostLabel.setColor(Color.YELLOW);
            bottomRow.add(sunCostLabel).left().pad(2);
            content.add(bottomRow).expandX().fillX().left();
        } else {
            sunCostLabel = null;
        }

        add(content);

        lockOverlay = new Image(GameAssets.getInstance().getWhiteRegion());
        lockOverlay.setColor(0.15f, 0.15f, 0.15f, 0.72f);
        lockOverlay.setVisible(false);
        add(lockOverlay);

        TextureRegion lockRegion = assets.region(PlantSelectAssetPaths.LOCK_GOLD);
        lockIcon = lockRegion != null ? new Image(lockRegion) : new Image();
        lockIcon.setVisible(false);
        add(lockIcon);

        TextureRegion frameRegion = assets.region(PlantSelectAssetPaths.SELECT_FRAME);
        selectFrame = frameRegion != null ? new Image(frameRegion) : new Image();
        if (frameRegion != null) selectFrame.setScaling(com.badlogic.gdx.utils.Scaling.stretch);
        selectFrame.setVisible(false);
        add(selectFrame);
    }

    private void applyBackground(boolean boostedBg) {
        CollectionAssets assets = CollectionAssets.getInstance();
        TextureRegion bg = boostedBg ? assets.region(PlantSelectAssetPaths.BOOST_CARD_BG) : null;
        if (bg != null) {
            backgroundImage.setDrawable(new TextureRegionDrawable(bg));
            backgroundImage.setColor(Color.WHITE);
        } else if (skin.has("image_ui_cards_almanac_plant_card_10", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            backgroundImage.setDrawable(skin.getDrawable("image_ui_cards_almanac_plant_card_10"));
        } else {
            backgroundImage.setDrawable(new TextureRegionDrawable(GameAssets.getInstance().getWhiteRegion()));
            backgroundImage.setColor(0.2f, 0.15f, 0.08f, 1f);
        }
    }

    private boolean hasLabelStyle(String name) {
        try { return skin.has(name, Label.LabelStyle.class); } catch (Exception e) { return false; }
    }

    // ─── Setters ────────────────────────────────────────────────────────────

    public void setLocked(boolean locked) {
        this.locked = locked;
        lockOverlay.setVisible(locked);
        lockIcon.setVisible(locked);
        plantImage.setVisible(!locked);
    }

    public void setBoosted(boolean boosted) {
        this.boosted = boosted;
        applyBackground(boosted);
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
        selectFrame.setVisible(highlighted);
    }

    public void setLevel(int upgradeLevel) {
        levelLabel.setText("Lv" + (Math.max(0, Math.min(3, upgradeLevel)) + 1));
    }

    public void setSunCost(int cost) {
        if (sunCostLabel != null) sunCostLabel.setText(String.valueOf(cost));
    }

    public void onClick(Runnable action) {
        addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { action.run(); }
        });
    }

    public PlantType getPlantType() { return plantType; }
    public boolean isLocked()       { return locked; }
    public boolean isBoosted()      { return boosted; }
    public boolean isHighlighted()  { return highlighted; }
}
