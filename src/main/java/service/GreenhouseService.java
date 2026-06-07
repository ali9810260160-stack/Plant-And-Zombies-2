package service;

import model.Greenhouse;
import model.User;
import model.enums.PlantType;

/**
 * سرویس مدیریت گلخانه.
 * کاشت، رشد، برداشت و تسریع گیاهان در گلدان.
 */
public class GreenhouseService {

    private final UserService userService;

    public GreenhouseService(UserService userService) {
        this.userService = userService;
    }

    /**
     * یک گیاه تصادفی در گلدان می‌کارد.
     * 50% marigold، 50% از گیاهان آنلاک‌شده کاربر.
     * @param user کاربر
     * @param greenhouse گلخانه
     * @param x ستون (1-5)
     * @param y ردیف (1-4)
     */
    public void plantPot(User user, Greenhouse greenhouse, int x, int y) { }

    /**
     * گیاه آماده را برداشت می‌کند.
     * marigold → 500 سکه. گیاه دیگر → boost ذخیره‌ای.
     * @param user کاربر
     * @param greenhouse گلخانه
     * @param x ستون
     * @param y ردیف
     */
    public void collectPot(User user, Greenhouse greenhouse, int x, int y) { }

    /**
     * رشد گیاه را تسریع می‌کند (1 الماس = 1 ساعت کمتر).
     * هزینه = سقف ساعت‌های باقیمانده.
     * @param user کاربر
     * @param greenhouse گلخانه
     * @param x ستون
     * @param y ردیف
     */
    public void accelerateGrowth(User user, Greenhouse greenhouse, int x, int y) { }

    /**
     * یک گلدان جدید آزاد می‌کند (2000 سکه).
     * @param user کاربر
     * @param greenhouse گلخانه
     * @param x ستون
     * @param y ردیف
     */
    public void unlockPot(User user, Greenhouse greenhouse, int x, int y) { }

    /**
     * وضعیت رشد همه گلدان‌ها را به‌روز می‌کند.
     * @param greenhouse گلخانه
     */
    public void updateGrowthStatus(Greenhouse greenhouse) { }

    /**
     * یک نوع گیاه تصادفی برای گلدان انتخاب می‌کند.
     * @param user کاربر
     * @return نوع گیاه یا null برای marigold
     */
    public PlantType selectRandomPlantForPot(User user) { return null; }
}
