package model.enums;

/**
 * انواع آیتم‌های قابل خرید در فروشگاه.
 */
public enum ShopItemType {
    POT,                    // گلدان گلخانه (2000 سکه)
    PLANT_FOOD,             // غذای گیاه (3 الماس)
    RANDOM_SEED_PACKET,     // بسته بذر تصادفی (1000 سکه / 5 عدد)
    CHOSEN_SEED_PACKET,     // بسته بذر انتخابی (5 الماس / 10 عدد)
    CURRENCY_EXCHANGE,      // تبدیل ارز (5 الماس → 500 سکه)
    DAILY_OFFER             // پیشنهاد روزانه (رفرش هر 24 ساعت)
}
