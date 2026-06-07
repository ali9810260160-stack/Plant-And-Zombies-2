package service;

import model.ShopItem;
import model.User;
import model.enums.PlantType;

import java.util.List;

/**
 * سرویس فروشگاه بازی.
 * مدیریت کالاهای دائمی، پیشنهاد روزانه و خرید.
 */
public class ShopService {

    private final UserService userService;

    public ShopService(UserService userService) {
        this.userService = userService;
    }

    /**
     * لیست کالاهای دائمی فروشگاه را برمی‌گرداند.
     * @return لیست آیتم‌های دائمی
     */
    public List<ShopItem> getPermanentItems() { return null; }

    /**
     * پیشنهاد روزانه را برمی‌گرداند.
     * اگر روز جدید باشد، پیشنهاد جدید تولید می‌کند.
     * @param user کاربر
     * @return آیتم پیشنهاد روزانه
     */
    public ShopItem getDailyOffer(User user) { return null; }

    /**
     * خرید را انجام می‌دهد.
     * @param user کاربر
     * @param itemId شناسه آیتم
     * @param count تعداد
     * @param selectedPlantType برای بسته بذر انتخابی (می‌تواند null باشد)
     * @throws exception.GameException اگر موجودی کافی نباشد یا آیتم نامعتبر
     */
    public void purchase(User user, String itemId, int count,
                         PlantType selectedPlantType) { }

    /**
     * بررسی می‌کند آیا پیشنهاد روزانه برای این کاربر قابل خرید است.
     * @param user کاربر
     * @return true اگر هنوز امروز نخریده
     */
    public boolean canBuyDailyOffer(User user) { return false; }

    /**
     * پیشنهاد روزانه جدید تولید می‌کند.
     * @param user کاربر
     * @return آیتم جدید
     */
    private ShopItem generateDailyOffer(User user) { return null; }

    /**
     * تاریخ امروز را به فرمت رشته برمی‌گرداند.
     * @return تاریخ (yyyy-MM-dd)
     */
    private String getTodayDate() { return null; }
}
