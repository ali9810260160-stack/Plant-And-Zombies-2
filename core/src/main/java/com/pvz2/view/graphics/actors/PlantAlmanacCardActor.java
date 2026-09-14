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
import com.pvz2.model.enums.PlantType;

/**
 * کارت گیاه در گرید صفحه‌ی کلکسیون (تب Plants).
 *
 * <p>ظاهر منطبق با تصویر مرجع «almanac-zombie-details» (که در واقع گرید گیاهان
 * است): تصویر گیاه، برچسب سطح ارتقا («Lvl NN») در گوشه‌ی بالا-چپ، نوار پیشرفت
 * سطح (به‌سبک خود pvz-skin) در پایین با متن کسری، و آیکون هزینه‌ی خورشید
 * گوشه‌ی پایین-چپ. اگر آنلاک نشده باشه یک لایه‌ی نقره‌ای + قفل روش می‌افته؛
 * اگر boost شده باشه به‌جای پس‌زمینه‌ی معمولی از تصویر {@code boost.png}
 * (پرتوهای طلایی) استفاده می‌شه.
 *
 * <p><b>یادداشت مدل داده:</b> {@code User} فعلاً فقط سطح ارتقای گسسته‌ی
 * ۰..۳ رو نگه می‌داره (بدون XP جزئی)، پس نوار پیشرفت این کارت
 * {@code upgradeLevel/3} رو نشون می‌ده، نه یک شمارنده‌ی XP واقعی. «boosted»
 * هم چون در مدل فعلی پرچم دائمی مشخصی نداره، با فرض «سطح ارتقای کامل (۳)
 * = boosted» محاسبه شده — با {@link #setBoosted(boolean)} قابل override است.
 */
public class PlantAlmanacCardActor extends Stack {

    public static final float CARD_W = 108f;
    public static final float CARD_H = 140f;

    private final PlantType plantType;
    private final Skin skin;

    private final Image plantImage;
    private final Image backgroundImage;
    private final Image lockOverlay;
    private final Image lockIcon;
    private final ProgressBar levelBar;
    private final Label levelBarLabel;
    private final Label levelTagLabel;
    private final Label sunCostLabel;

    private boolean locked;
    private boolean boosted;

    public PlantAlmanacCardActor(PlantType type, Skin skin) {
        this.plantType = type;
        this.skin = skin;
        setSize(CARD_W, CARD_H);

        CollectionAssets assets = CollectionAssets.getInstance();

        // ─── لایه ۱: پس‌زمینه (معمولی یا boost طلایی) ───────────────────────
        backgroundImage = new Image();
        applyBackground(assets, false);
        add(backgroundImage);

        // ─── لایه ۲: تصویر گیاه + برچسب سطح + نوار پیشرفت + هزینه ────────────
        Table content = new Table();
        content.top();

        Table topRow = new Table();
        levelTagLabel = new Label("", skin, hasStyle(skin, "small") ? "small" : "default");
        levelTagLabel.setColor(Color.GOLD);
        topRow.add(levelTagLabel).top().left().pad(4);
        content.add(topRow).expandX().fillX().row();

        TextureRegion plantRegion = assets.region(CollectionAssetPaths.plantPath(type));
        plantImage = plantRegion != null
                ? new Image(plantRegion)
                : new Image(GameAssets.getInstance().getWhiteRegion());
        content.add(plantImage).expand().size(CARD_W * 0.62f, CARD_H * 0.5f).padTop(4).row();

        Stack barStack = new Stack();
        String barStyle = hasProgressBarStyle(skin, "level-bar") ? "level-bar" : "default-horizontal";
        ProgressBar.ProgressBarStyle pbStyle = null;
        try { pbStyle = skin.get(barStyle, ProgressBar.ProgressBarStyle.class); }
        catch (Exception ignored) { /* استایل در دسترس نیست — بار بدون گرافیک رسم می‌شه */ }
        if (pbStyle != null) {
            levelBar = new ProgressBar(0f, 1f, 0.001f, false, skin, barStyle);
        } else {
            levelBar = null;
        }
        if (levelBar != null) barStack.add(levelBar);
        levelBarLabel = new Label("", skin, hasStyle(skin, "small") ? "small" : "default");
        levelBarLabel.setFontScale(0.72f);
        barStack.add(levelBarLabel);
        content.add(barStack).width(CARD_W - 12f).height(14f).padTop(2).row();

        Table bottomRow = new Table();
        sunCostLabel = new Label("", skin, hasStyle(skin, "small") ? "small" : "default");
        sunCostLabel.setColor(Color.YELLOW);
        bottomRow.add(sunCostLabel).left().pad(2);
        content.add(bottomRow).expandX().fillX().left();

        add(content);

        // ─── لایه ۳: overlay نقره‌ای برای قفل ─────────────────────────────────
        lockOverlay = new Image(GameAssets.getInstance().getWhiteRegion());
        lockOverlay.setColor(0.72f, 0.75f, 0.8f, 0.78f); // نقره‌ای نیمه‌شفاف
        lockOverlay.setVisible(false);
        add(lockOverlay);

        TextureRegion lockRegion = assets.region(CollectionAssetPaths.ICON_LOCK_SMALL);
        lockIcon = lockRegion != null ? new Image(lockRegion) : new Image();
        lockIcon.setVisible(false);
        add(lockIcon);
    }

    private void applyBackground(CollectionAssets assets, boolean boostedBg) {
        TextureRegion bg = boostedBg
                ? assets.region(CollectionAssetPaths.PLANT_BOOST_BG)
                : null;
        if (bg != null) {
            backgroundImage.setDrawable(new TextureRegionDrawable(bg));
            backgroundImage.setColor(Color.WHITE);
        } else if (skin.has("image_ui_cards_almanac_plant_card_10", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            backgroundImage.setDrawable(skin.getDrawable("image_ui_cards_almanac_plant_card_10"));
        } else {
            backgroundImage.setDrawable(new TextureRegionDrawable(GameAssets.getInstance().getWhiteRegion()));
            backgroundImage.setColor(0.22f, 0.16f, 0.1f, 1f);
        }
    }

    private static boolean hasStyle(Skin skin, String name) {
        try { return skin.has(name, Label.LabelStyle.class); }
        catch (Exception e) { return false; }
    }

    private static boolean hasProgressBarStyle(Skin skin, String name) {
        try { return skin.has(name, ProgressBar.ProgressBarStyle.class); }
        catch (Exception e) { return false; }
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
        applyBackground(CollectionAssets.getInstance(), boosted);
    }

    /** سطح ارتقا (۰..۳) و کسری نمایش‌داده‌شده روی نوار. */
    public void setLevel(int upgradeLevel) {
        int lvl = Math.max(0, Math.min(3, upgradeLevel));
        levelTagLabel.setText("Lvl " + (lvl + 1));
        float fraction = lvl / 3f;
        if (levelBar != null) levelBar.setValue(fraction);
        levelBarLabel.setText(lvl + "/3");
    }

    public void setSunCost(int cost) {
        sunCostLabel.setText(String.valueOf(cost));
    }

    public void onClick(Runnable action) {
        addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { action.run(); }
        });
    }

    public PlantType getPlantType() { return plantType; }
    public boolean isLocked()       { return locked; }
    public boolean isBoosted()      { return boosted; }
}
