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
        List<Entry> list = new ArrayList<>();
        list.add(new Entry("POT", "Greenhouse Pot", "Unlocks an extra pot in your Greenhouse.",
                AssetIds.ICON_SHOP_POT, 2000, true, 1));
        list.add(new Entry("PLANT_FOOD", "Plant Food", "Instantly powers up a plant on the lawn.",
                AssetIds.ICON_SHOP_PLANT_FOOD, 3, false, 1));
        list.add(new Entry("SEED_RANDOM", "Random Seed Packet", "5 seed packets for a random unlocked plant.",
                AssetIds.ICON_SHOP_SEED_RANDOM, 1000, true, 5));
        list.add(new Entry("SEED_CHOICE", "Chosen Seed Packet", "10 seed packets for a plant you pick.",
                AssetIds.ICON_SHOP_SEED_CHOICE, 5, false, 10));
        list.add(new Entry("CURRENCY", "Currency Exchange", "Trade 5 gems for 500 coins.",
                AssetIds.ICON_SHOP_CURRENCY, 5, false, 1));
        list.add(new Entry("DAILY", "Daily Offer", "Refreshes every 24 hours.",
                AssetIds.ICON_SHOP_DAILY, 0, true, 1));
        return list;
    }

    private Stage stage;

    public ShopScreen(PVZApplication game) { super(game); }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        buildUi();
    }

    private void buildUi() {
        Skin skin = GameAssets.getInstance().getSkin();

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        root.add(new Label("Shop", skin, "big_outline")).padTop(20).padBottom(10).row();

        Table itemsTable = new Table();
        itemsTable.defaults().pad(10).top();
        int column = 0;
        for (Entry entry : CATALOG) {
            itemsTable.add(buildItemCard(skin, entry)).width(220).height(280);
            column++;
            if (column % COLUMNS == 0) itemsTable.row();
        }

        ScrollPane scroll = new ScrollPane(itemsTable, skin);
        scroll.setFadeScrollBars(false);
        root.add(scroll).width(760).height(480).padBottom(10).row();

        TextButton back = new TextButton("Back", skin, "brown");
        back.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                goTo(ScreenId.MAIN_MENU);
            }
        });
        root.add(back).size(140, 60).padBottom(20);
    }

    /** کارت عمودی PvZ2: تصویر کالا (اگه ست شده باشه)، عنوان، توضیح، قیمت، دکمه خرید. */
    private Table buildItemCard(Skin skin, Entry entry) {
        Table card = new Table(skin);
        card.setBackground("image_ui_cards_almanac_plant_card_10");
        card.top().pad(10);

        if (!entry.iconResourceId.isEmpty()) {
            TextureRegion icon = GameAssets.getInstance().region(entry.iconResourceId);
            card.add(new Image(icon)).size(96).padBottom(8).row();
        } else {
            card.add().height(96).padBottom(8).row();
        }

        Label nameLabel = new Label(entry.displayName, skin, "default");
        nameLabel.setWrap(true);
        card.add(nameLabel).width(180).padBottom(6).row();

        Label descLabel = new Label(entry.description, skin, "default");
        descLabel.setWrap(true);
        descLabel.setFontScale(0.8f);
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

    @Override
    public void render(float delta) {
        GameAssets.getInstance().update();
        ScreenUtils.clear(0.04f, 0.06f, 0.1f, 1);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int w, int h) { stage.getViewport().update(w, h, true); }

    @Override
    public void dispose() { if (stage != null) stage.dispose(); }
}
