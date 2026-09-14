package com.pvz2.graphics.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.pvz2.PVZApplication;
import com.pvz2.exception.GameException;
import com.pvz2.graphics.*;
import com.pvz2.graphics.actors.ConfirmDialog;
import com.pvz2.graphics.actors.ToastActor;
import com.pvz2.graphics.assets.AssetIds;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.model.User;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * صفحه فروشگاه — چیدمان grid کارت‌های عمودی، دقیقاً مطابق تصویر ۹ سند فاز دوم:
 * تصویر کالا بالا، عنوان، توضیح کوتاه، قیمت + دکمه خرید پایین.
 * <p>
 * منطق واقعی خرید از {@link com.pvz2.service.GreenhouseService#shopBuy} فاز یک
 * می‌آید (از طریق {@link ServiceLocator}) — چیزی دوباره پیاده‌سازی نشده.
 */
public class ShopScreen extends BaseScreen {

    private static final int COLUMNS = 3;

    private static final class Entry {
        final String itemId, displayName, description, iconResourceId;
        final int price;
        final boolean pricedInCoins;
        final int quantity;

        Entry(String itemId, String displayName, String description, String iconResourceId,
              int price, boolean pricedInCoins, int quantity) {
            this.itemId = itemId;
            this.displayName = displayName;
            this.description = description;
            this.iconResourceId = iconResourceId;
            this.price = price;
            this.pricedInCoins = pricedInCoins;
            this.quantity = quantity;
        }
    }

    private static final List<Entry> CATALOG = buildCatalog();

    private static List<Entry> buildCatalog() {
        // آیکون‌ها از تصاویرِ محلیِ فروشگاه (assets/shop/) خوانده می‌شوند.
        List<Entry> list = new ArrayList<>();
        list.add(new Entry("POT", "Greenhouse Pot", "Unlocks an extra pot in your Greenhouse.",
                "shop/shop-pot.png", 2000, true, 1));
        list.add(new Entry("PLANT_FOOD", "Plant Food", "Instantly powers up a plant on the lawn.",
                "shop/plant-food.png", 3, false, 1));
        list.add(new Entry("SEED_RANDOM", "Random Seed Packet", "5 seed packets for a random unlocked plant.",
                "shop/seed-random.png", 1000, true, 5));
        list.add(new Entry("SEED_CHOICE", "Chosen Seed Packet", "10 seed packets for a plant you pick.",
                "shop/seed-choice.png", 5, false, 10));
        list.add(new Entry("CURRENCY", "Currency Exchange", "Trade 5 gems for 500 coins.",
                "shop/currency-exchange.png", 5, false, 1));
        list.add(new Entry("DAILY", "Daily Offer", "Refreshes every 24 hours.",
                "shop/shop-daily.png", 0, true, 1));
        return list;
    }

    private Stage stage;

    /** لیبلِ شمارش‌معکوسِ کالای روزانه (AL3) — هر دقیقه در render به‌روز می‌شود. */
    private Label dailyCountdownLabel;
    private float dailyAccum = 0f;

    public ShopScreen(PVZApplication game) { super(game); }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        buildUi();
    }

    private void buildUi() {
        Skin skin = GameAssets.getInstance().getSkin();

        // ── پس‌زمینه‌ی تمام‌صفحه ──
        Image bg = new Image(GameAssets.getInstance().local(AssetIds.SHOP_BACKGROUND));
        bg.setScaling(com.badlogic.gdx.utils.Scaling.stretch);
        bg.setFillParent(true);
        bg.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        stage.addActor(bg);

        Table root = new Table();
        root.setFillParent(true);
        root.top();
        stage.addActor(root);

        root.add(buildTopBar(skin)).growX().padTop(8).row();

        Table itemsTable = new Table();
        itemsTable.defaults().pad(10).top();
        int column = 0;
        for (Entry entry : CATALOG) {
            itemsTable.add(buildItemCard(skin, entry)).width(224).height(300);
            column++;
            if (column % COLUMNS == 0) itemsTable.row();
        }

        ScrollPane scroll = new ScrollPane(itemsTable, skin);
        scroll.setFadeScrollBars(false);
        root.add(scroll).width(780).height(500).expand().top().padTop(6);
    }

    /** نوارِ بالا: ارز (سکه/الماس) در چپ + دکمه‌ی بستن (X) در راست. */
    private Table buildTopBar(Skin skin) {
        User user = facade().getCurrentUser();
        long coins = user != null ? user.getCoins() : 0;
        int  gems  = user != null ? user.getGems()  : 0;

        Table bar = new Table();
        bar.pad(4, 18, 4, 14);

        Table currency = new Table();
        if (!AssetIds.ICON_COIN.isEmpty()) {
            currency.add(new Image(GameAssets.getInstance().region(AssetIds.ICON_COIN))).size(30).padRight(5);
        }
        Label coinLbl = new Label(String.valueOf(coins), skin, "medium");
        coinLbl.setColor(Color.GOLD);
        currency.add(coinLbl).padRight(22);
        if (!AssetIds.ICON_GEM.isEmpty()) {
            currency.add(new Image(GameAssets.getInstance().region(AssetIds.ICON_GEM))).size(30).padRight(5);
        }
        Label gemLbl = new Label(String.valueOf(gems), skin, "medium");
        gemLbl.setColor(Color.CYAN);
        currency.add(gemLbl);
        bar.add(currency).left().expandX();

        Image close = new Image(GameAssets.getInstance().local("auth/icon_exit.png"));
        close.setScaling(com.badlogic.gdx.utils.Scaling.fit);
        close.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { goTo(ScreenId.MAIN_MENU); }
        });
        bar.add(close).size(48, 44).right();
        return bar;
    }

    /** کارت عمودی PvZ2: تصویر کالا (اگه ست شده باشه)، عنوان، توضیح، قیمت، دکمه خرید. */
    private Table buildItemCard(Skin skin, Entry entry) {
        Table card = new Table(skin);
        TextureRegion cardBg = GameAssets.getInstance().local(AssetIds.SHOP_PRODUCT_CARD);
        if (cardBg != null && cardBg != GameAssets.getInstance().getWhiteRegion()) {
            card.setBackground(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(cardBg));
        } else {
            card.setBackground("image_ui_cards_almanac_plant_card_10");
        }
        card.top().pad(14, 16, 14, 16);

        Label nameLabel = new Label(entry.displayName, skin, "default");
        nameLabel.setWrap(true);
        nameLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        card.add(nameLabel).width(186).padBottom(6).row();

        if (!entry.iconResourceId.isEmpty()) {
            TextureRegion icon = GameAssets.getInstance().local(entry.iconResourceId);
            Image iconImg = new Image(icon);
            iconImg.setScaling(com.badlogic.gdx.utils.Scaling.fit);
            card.add(iconImg).size(100).padBottom(8).row();
        } else {
            card.add().height(100).padBottom(8).row();
        }

        // AL3: کالای روزانه به‌جای متنِ ثابت، شمارش‌معکوسِ زنده تا ریستِ بعدی دارد.
        boolean isDaily = entry.itemId.equals("DAILY");
        Label descLabel = new Label(isDaily ? dailyCountdownText() : entry.description, skin, "default");
        descLabel.setWrap(true);
        descLabel.setFontScale(0.8f);
        if (isDaily) {
            descLabel.setColor(1f, 0.82f, 0.35f, 1f);
            dailyCountdownLabel = descLabel;
        }
        card.add(descLabel).width(180).padBottom(10).row();

        String priceText = entry.itemId.equals("DAILY")
                ? "Free today"
                : entry.price + (entry.pricedInCoins ? " Coins" : " Gems");
        Label priceLabel = new Label(priceText, skin, "default");
        priceLabel.setColor(entry.pricedInCoins ? Color.GOLD : Color.CYAN);
        card.add(priceLabel).padBottom(6).row();

        TextButton buyButton = new TextButton("Buy", skin, "green");
        buyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                onBuyClicked(entry);
            }
        });
        card.add(buyButton).size(120, 50);

        return card;
    }

    private void onBuyClicked(Entry entry) {
        if (entry.itemId.equals("SEED_CHOICE")) {
            showPlantPicker(entry);
            return;
        }
        showConfirmDialog(entry, null);
    }

    private void showConfirmDialog(Entry entry, String plantType) {
        Skin skin = GameAssets.getInstance().getSkin();
        String message = "Would you like to purchase " + entry.displayName + "?";
        new ConfirmDialog("Purchase Confirmation", message, "Confirm", skin)
                .onConfirm(() -> doPurchase(entry, plantType))
                .show(stage);
    }

    private void doPurchase(Entry entry, String plantType) {
        User user = facade().getCurrentUser();
        try {
            ServiceLocator.getInstance().getGreenhouseService()
                    .shopBuy(user, entry.itemId, entry.quantity, plantType);
            showToast(ToastActor.success("Purchased: " + entry.displayName));
        } catch (GameException e) {
            showToast(ToastActor.error(e.getMessage()));
        }
    }

    /** انتخاب گیاه برای «بسته بذر انتخابی»، سپس Dialog تأیید خرید. */
    private void showPlantPicker(Entry entry) {
        Skin skin = GameAssets.getInstance().getSkin();
        List<GameFacade.PlantEntry> plants = facade().getAllPlants();

        pvz.skin.BorderedTable dialog = new pvz.skin.BorderedTable();
        dialog.add(new Label("Choose a plant", skin, "default")).padBottom(10).row();

        Table list = new Table();
        boolean any = false;
        for (GameFacade.PlantEntry p : plants) {
            if (!p.unlocked) continue;
            any = true;
            TextButton plantButton = new TextButton(p.type.name(), skin, "brown");
            plantButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent e, float x, float y) {
                    dialog.remove();
                    showConfirmDialog(entry, p.type.name());
                }
            });
            list.add(plantButton).size(220, 50).padBottom(6).row();
        }
        if (!any) {
            list.add(new Label("No unlocked plants yet.", skin, "default")).pad(10);
        }

        ScrollPane scroll = new ScrollPane(list, skin);
        scroll.setFadeScrollBars(false);
        dialog.add(scroll).width(260).height(240).row();

        TextButton cancel = new TextButton("Cancel", skin, "brown");
        cancel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                dialog.remove();
            }
        });
        dialog.add(cancel).size(140, 50).padTop(10);

        dialog.pack();
        dialog.setPosition((stage.getWidth() - dialog.getWidth()) / 2f,
                (stage.getHeight() - dialog.getHeight()) / 2f);
        stage.addActor(dialog);
    }

    private void showToast(ToastActor t) {
        t.setPosition(GameConstants.VIEWPORT_WIDTH / 2f - t.getWidth() * 0.5f, 80);
        stage.addActor(t);
    }

    /** زمانِ باقی‌مانده تا نیمه‌شبِ بعدی — کالای روزانه در آن لحظه ریست می‌شود (AL3). */
    private String dailyCountdownText() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
        long secs = Math.max(0, Duration.between(now, nextMidnight).getSeconds());
        long h = secs / 3600, m = (secs % 3600) / 60;
        return "New offer in " + h + "h " + m + "m";
    }

    @Override
    public void render(float delta) {
        GameAssets.getInstance().update();
        ScreenUtils.clear(0.04f, 0.06f, 0.1f, 1);

        // شمارش‌معکوسِ کالای روزانه هر ~۳۰ ثانیه به‌روز می‌شود (نه هر فریم).
        if (dailyCountdownLabel != null) {
            dailyAccum += delta;
            if (dailyAccum >= 30f) {
                dailyAccum = 0f;
                dailyCountdownLabel.setText(dailyCountdownText());
            }
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int w, int h) { stage.getViewport().update(w, h, true); }

    @Override
    public void dispose() { if (stage != null) stage.dispose(); }
}
