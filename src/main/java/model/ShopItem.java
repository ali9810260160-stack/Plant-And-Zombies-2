package model;

import model.enums.ShopItemType;

/**
 * یک آیتم در فروشگاه بازی.
 * می‌تواند دائمی یا پیشنهاد روزانه باشد.
 */
public class ShopItem {

    /** شناسه آیتم */
    private String itemId;

    /** نوع آیتم */
    private ShopItemType itemType;

    /** نام نمایشی */
    private String displayName;

    /** قیمت (واحد بسته به نوع: سکه یا الماس) */
    private int price;

    /** آیا قیمت به سکه است (false = الماس) */
    private boolean pricedInCoins;

    /** تعداد آیتم در هر خرید */
    private int quantityPerPurchase;

    /** آیا این پیشنهاد روزانه است */
    private boolean dailyOffer;

    /** درصد تخفیف (برای پیشنهاد روزانه = 20) */
    private int discountPercent;

    public ShopItem(String itemId, ShopItemType itemType, String displayName,
                    int price, boolean pricedInCoins, int quantityPerPurchase) {
        this.itemId = itemId;
        this.itemType = itemType;
        this.displayName = displayName;
        this.price = price;
        this.pricedInCoins = pricedInCoins;
        this.quantityPerPurchase = quantityPerPurchase;
    }

    /** قیمت نهایی با احتساب تخفیف را برمی‌گرداند */
    public int getFinalPrice() { return 0; }

    public String getItemId() { return itemId; }
    public ShopItemType getItemType() { return itemType; }
    public String getDisplayName() { return displayName; }
    public int getPrice() { return price; }
    public boolean isPricedInCoins() { return pricedInCoins; }
    public int getQuantityPerPurchase() { return quantityPerPurchase; }
    public boolean isDailyOffer() { return dailyOffer; }
    public void setDailyOffer(boolean dailyOffer) { this.dailyOffer = dailyOffer; }
    public int getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }
}
