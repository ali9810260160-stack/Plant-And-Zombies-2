package com.pvz2.graphics.actors;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.pvz2.exception.GameException;
import com.pvz2.graphics.GameFacade;
import com.pvz2.graphics.ServiceLocator;
import com.pvz2.graphics.assets.AssetIds;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * پاپ‌آپ فروشگاه — دقیقاً با asset هایی که خودت فرستادی:
 *   - پس‌زمینه‌ی پاپ‌آپ: shop-background.png
 *   - قالب هر کارت محصول: product-card.png (عکس/اسم/توضیح/دکمه همه داخل خودِ کارتن)
 *   - دکمه‌ی بستن بالا: close_tab.png
 *   - آیکون هر محصول: shop-pot.png / plant-food.png / seed-random.png /
 *     seed-choice.png / currency-exchange.png / shop-daily.png
 * <p>
 * روی Stage صفحه‌ی فعلی (Main Menu یا Greenhouse) به‌عنوان overlay باز می‌شه؛
 * صفحه عوض نمی‌شه. منطق واقعی خرید از {@link com.pvz2.service.GreenhouseService#shopBuy}
 * فاز یک میاد.
 */
public class ShopPopup extends Group {

    private static final class Entry {
        final String itemId, displayName, description, iconLocalPath;
        final int price;
        final boolean pricedInCoins;
        final int quantity;

        Entry(String itemId, String displayName, String description, String iconLocalPath,
              int price, boolean pricedInCoins, int quantity) {
            this.itemId = itemId;
            this.displayName = displayName;
            this.description = description;
            this.iconLocalPath = iconLocalPath;
            this.price = price;
            this.pricedInCoins = pricedInCoins;
            this.quantity = quantity;
        }
    }

    private static final List<Entry> CATALOG = buildCatalog();

    private static List<Entry> buildCatalog() {
        List<Entry> list = new ArrayList<>();
        list.add(new Entry("POT", "Greenhouse Pot", "Unlocks an extra pot in your Greenhouse.",
                AssetIds.SHOP_ICON_POT_LOCAL, 2000, true, 1));
        list.add(new Entry("PLANT_FOOD", "Plant Food", "Instantly powers up a plant on the lawn.",
                AssetIds.SHOP_ICON_PLANT_FOOD_LOCAL, 3, false, 1));
        list.add(new Entry("SEED_RANDOM", "Random Seed Packet", "5 seed packets for a random unlocked plant.",
                AssetIds.SHOP_ICON_SEED_RANDOM_LOCAL, 1000, true, 5));
        list.add(new Entry("SEED_CHOICE", "Chosen Seed Packet", "10 seed packets for a plant you pick.",
                AssetIds.SHOP_ICON_SEED_CHOICE_LOCAL, 5, false, 10));
        list.add(new Entry("CURRENCY", "Currency Exchange", "Trade 5 gems for 500 coins.",
                AssetIds.SHOP_ICON_CURRENCY_LOCAL, 5, false, 1));
        list.add(new Entry("DAILY", "Daily Offer", "Refreshes every 24 hours.",
                AssetIds.SHOP_ICON_DAILY_LOCAL, 0, true, 1));
        return list;
    }

    // نسبت‌های ناحیه‌ی «باکس آبی» تصویر محصول داخل product-card.png (اندازه‌گیری‌شده
    // از خودِ فایل: جعبه‌ی آبی از ۱۱.۸٪ تا ۵۱.۸٪ ارتفاع کارت است).
    private static final float ICON_BOX_TOP    = 0.14f;
    private static final float ICON_BOX_BOTTOM = 0.50f;

    private final Stage stage;
    private final Skin skin;
    private final Runnable onCurrencyChanged;

    public ShopPopup(Skin skin, Stage stage, Runnable onCurrencyChanged) {
        this.stage = stage;
        this.skin = skin;
        this.onCurrencyChanged = onCurrencyChanged;

        addActor(dimBackdrop(stage));
        buildPanel();
        setSize(stage.getWidth(), stage.getHeight());
    }

    static Actor dimBackdrop(Stage stage) {
        Image dim = new Image(new TextureRegionDrawable(GameAssets.getInstance().getWhiteRegion()));
        dim.setColor(0, 0, 0, 0.55f);
        dim.setSize(stage.getWidth(), stage.getHeight());
        dim.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { /* عمداً چیزی نمی‌بندد */ }
        });
        return dim;
    }

    private void buildPanel() {
        float panelW = 1080, panelH = 520;

        // ---- پس‌زمینه‌ی خودِ shop-background.png ----
        TextureRegion bgRegion = GameAssets.getInstance().local(AssetIds.SHOP_BACKGROUND);
        Image bg = new Image(bgRegion);
        bg.setScaling(Scaling.stretch);
        bg.setSize(panelW, panelH);
        bg.setPosition((stage.getWidth() - panelW) / 2f, (stage.getHeight() - panelH) / 2f);
        addActor(bg);

        float cardW = 210, cardH = 320;
        Table cardsRow = new Table();
        cardsRow.defaults().pad(8).top();
        for (Entry entry : CATALOG) {
            cardsRow.add(buildItemCard(entry, cardW, cardH)).width(cardW).height(cardH);
        }

        ScrollPane scroll = new ScrollPane(cardsRow, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(false, true);
        float scrollW = panelW - 90, scrollH = cardH + 20;
        scroll.setSize(scrollW, scrollH);
        // دقیقاً وسط پنل، هم افقی هم عمودی
        scroll.setPosition(bg.getX() + (panelW - scrollW) / 2f, bg.getY() + (panelH - scrollH) / 2f);
        addActor(scroll);

        // ---- دکمه‌ی close_tab -- چسبیده به گوشه‌ی بالا-راستِ خودِ بک‌گراند ----
        TextureRegion closeRegion = GameAssets.getInstance().local(AssetIds.SHOP_CLOSE_BUTTON);
        ImageButton closeBtn = new ImageButton(new TextureRegionDrawable(closeRegion));
        closeBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { close(); }
        });
        closeBtn.setSize(46, 46);
        closeBtn.setPosition(bg.getX() + panelW - 100, bg.getY() + panelH - 10);
        addActor(closeBtn);
    }

    /** کارت محصول کامل روی قالب product-card.png -- عکس/اسم/توضیح/دکمه همه داخل خودِ کارت. */
    private Stack buildItemCard(Entry entry, float cardW, float cardH) {
        Stack stack = new Stack();

        Image cardBg = new Image(GameAssets.getInstance().local(AssetIds.SHOP_PRODUCT_CARD));
        cardBg.setScaling(Scaling.stretch);
        stack.add(cardBg);

        Table content = new Table();
        content.top();

        // اسم محصول -- تو نوار سبز بالای کارت (سمت چپ، تا با آیکون ⓘ گوشه‌ی
        // بالا-راستِ خودِ قالب برخورد نکنه).
        Label nameLabel = new Label(entry.displayName, skin, "default");
        nameLabel.setWrap(true);
        nameLabel.setAlignment(Align.left);
        nameLabel.setFontScale(1.05f);
        content.add(nameLabel).width(cardW * 0.62f).height(cardH * ICON_BOX_TOP)
                .padLeft(cardW * 0.10f).padTop(cardH * 0.025f).left().row();

        // عکس محصول -- دقیقاً داخل باکس آبیِ خودِ قالب
        Image icon = new Image(GameAssets.getInstance().local(entry.iconLocalPath));
        icon.setScaling(Scaling.fit);
        float iconBoxH = cardH * (ICON_BOX_BOTTOM - ICON_BOX_TOP);
        content.add(icon).width(cardW * 0.60f).height(iconBoxH).padBottom(6).row();

        // توضیح محصول -- زیر باکس آبی، تو ناحیه‌ی کرم
        Label descLabel = new Label(entry.description, skin, "default");
        descLabel.setWrap(true);
        descLabel.setAlignment(Align.center);
        descLabel.setFontScale(0.88f);
        descLabel.setColor(0.28f, 0.2f, 0.12f, 1f);
        content.add(descLabel).width(cardW * 0.82f).padTop(4).expandY().top().row();

        // دکمه‌ی خرید -- پایین کارت
        String priceText = entry.itemId.equals("DAILY")
                ? "Free" : entry.price + (entry.pricedInCoins ? " Coins" : " Gems");
        TextButton buyButton = new TextButton(priceText, skin, "green");
        buyButton.getLabel().setFontScale(0.95f);
        buyButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { onBuyClicked(entry); }
        });
        content.add(buyButton).size(140, 44).padBottom(16);

        stack.add(content);
        return stack;
    }

    // ─── منطق خرید ──────────────────────────────────────────────────────────

    private void onBuyClicked(Entry entry) {
        User user = GameFacade.get().getCurrentUser();
        if (user == null) return;

        if (!canAfford(user, entry)) {
            String needed = entry.price + (entry.pricedInCoins ? " coins" : " gems");
            new ErrorPopup("Not Enough Currency",
                    "You need " + needed + " to buy " + entry.displayName + ".", skin).show(stage);
            return;
        }

        if (entry.itemId.equals("SEED_CHOICE")) {
            showPlantPicker(entry);
            return;
        }
        showConfirm(entry, null);
    }

    private boolean canAfford(User user, Entry entry) {
        if (entry.itemId.equals("DAILY")) return true;
        return entry.pricedInCoins ? user.getCoins() >= entry.price : user.getGems() >= entry.price;
    }

    private void showConfirm(Entry entry, String plantType) {
        String message = "Would you like to purchase " + entry.displayName + "?";
        String priceLabel = entry.itemId.equals("DAILY")
                ? "Free" : entry.price + (entry.pricedInCoins ? " Coins" : " Gems");
        new ConfirmDialog("Purchase Confirmation", message, priceLabel, skin)
                .onConfirm(() -> doPurchase(entry, plantType))
                .show(stage);
    }

    private void doPurchase(Entry entry, String plantType) {
        User user = GameFacade.get().getCurrentUser();
        try {
            ServiceLocator.getInstance().getGreenhouseService()
                    .shopBuy(user, entry.itemId, entry.quantity, plantType);
            if (onCurrencyChanged != null) onCurrencyChanged.run();
        } catch (GameException e) {
            new ErrorPopup("Purchase Failed", e.getMessage(), skin).show(stage);
        }
    }

    /** انتخاب گیاه برای «بسته بذر انتخابی»، بعد چک موجودی و تاییدیه. */
    private void showPlantPicker(Entry entry) {
        User user = GameFacade.get().getCurrentUser();
        List<String> owned = user != null ? user.getUnlockedPlants() : null;

        pvz.skin.BorderedTable dialog = new pvz.skin.BorderedTable();
        dialog.add(new Label("Choose a plant", skin, "default")).padBottom(10).row();

        Table list = new Table();
        boolean any = false;
        if (owned != null) {
            for (String plantName : owned) {
                any = true;
                TextButton plantButton = new TextButton(plantName, skin, "brown");
                plantButton.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent e, float x, float y) {
                        dialog.remove();
                        showConfirm(entry, plantName);
                    }
                });
                list.add(plantButton).size(220, 50).padBottom(6).row();
            }
        }
        if (!any) list.add(new Label("No unlocked plants yet.", skin, "default")).pad(10);

        ScrollPane scroll = new ScrollPane(list, skin);
        scroll.setFadeScrollBars(false);
        dialog.add(scroll).width(260).height(240).row();

        TextButton cancel = new TextButton("Cancel", skin, "brown");
        cancel.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { dialog.remove(); }
        });
        dialog.add(cancel).size(140, 50).padTop(10);

        dialog.pack();
        dialog.setPosition((stage.getWidth() - dialog.getWidth()) / 2f, (stage.getHeight() - dialog.getHeight()) / 2f);
        stage.addActor(dialog);
    }

    private void close() { remove(); }

    public ShopPopup show(Stage stage) {
        stage.addActor(this);
        return this;
    }
}
