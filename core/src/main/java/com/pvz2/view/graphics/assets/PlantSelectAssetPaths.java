package com.pvz2.graphics.assets;

/**
 * مسیر فایل‌های خام صفحه‌ی انتخاب گیاه (Seed Selection) — پس‌زمینه و عناصر UI.
 *
 * <p>مثل {@link CollectionAssetPaths}، این‌ها هم باید داخل پوشه‌ی {@code assets/}
 * پروژه‌ی libGDX قرار بگیرن (نه اطلس اصلی بازی):
 *
 * <pre>
 *   assets/plantselect/background.jpg
 *   assets/plantselect/ui/&lt;file&gt;.png
 * </pre>
 *
 * <p>بارگذاری با همون {@link CollectionAssets} (کش عمومی Texture خام) انجام
 * می‌شه — این کلاس فقط مسیرها رو نگه می‌داره. تصاویر خود گیاهان (برای اسلات‌ها،
 * گرید انتخاب، و باکس جزئیات) از {@link CollectionAssetPaths#plantPath} استفاده
 * می‌کنن، چون قبلاً برای صفحه‌ی کلکسیون مپ شدن — نیازی به مپ دوباره نیست.
 */
public final class PlantSelectAssetPaths {

    private PlantSelectAssetPaths() {}

    public static final String ROOT   = "plantselect/";
    public static final String UI_DIR = ROOT + "ui/";

    public static final String BACKGROUND = ROOT + "background.jpg";

    /** پس‌زمینه‌ی طلایی کارت گیاه boost‌شده (گرید و اسلات). */
    public static final String BOOST_CARD_BG = UI_DIR + "boostcard.png";
    /** قاب سبزرنگ گوشه‌دار دور کارت انتخاب‌شده/در‌حال‌مشاهده. */
    public static final String SELECT_FRAME  = UI_DIR + "select.png";
    /** قفل طلایی روی کارت‌های آنلاک‌نشده. */
    public static final String LOCK_GOLD     = UI_DIR + "lock_small_gold.png";

    /** نوار سکه + دکمه‌ی خرید (سمت راست هدر). */
    public static final String COIN_BUY_BAR   = UI_DIR + "buttons_coin_buy_normal.png";
    /** نوار الماس + دکمه‌ی خرید (سمت راست هدر). */
    public static final String GEM_BUY_BAR    = UI_DIR + "buttons_premium_normal.png";
    /** دکمه‌ی توقف بازی (گوشه‌ی بالا-راست). */
    public static final String PAUSE_BUTTON   = UI_DIR + "pause_button.png";
}
