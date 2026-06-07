package service;

import model.User;
import model.enums.PlantType;
import model.enums.ZombieType;
import model.plants.Plant;
import model.zombies.Zombie;

import java.util.List;

/**
 * سرویس کلکسیون گیاهان و زامبی‌ها.
 * ارتقا، خرید و نمایش اطلاعات گیاهان.
 */
public class CollectionService {

    private final UserService userService;

    public CollectionService(UserService userService) {
        this.userService = userService;
    }

    /**
     * لیست گیاهان آنلاک‌شده کاربر را برمی‌گرداند.
     * @param user کاربر
     * @return لیست انواع گیاه
     */
    public List<PlantType> getUnlockedPlants(User user) { return null; }

    /**
     * لیست تمام گیاهان تعریف‌شده در بازی را برمی‌گرداند.
     * @return لیست انواع گیاه
     */
    public List<PlantType> getAllPlants() { return null; }

    /**
     * لیست زامبی‌های دیده‌شده کاربر را برمی‌گرداند.
     * @param user کاربر
     * @return لیست انواع زامبی
     */
    public List<ZombieType> getSeenZombies(User user) { return null; }

    /**
     * لیست تمام زامبی‌های تعریف‌شده در بازی را برمی‌گرداند.
     * @return لیست انواع زامبی
     */
    public List<ZombieType> getAllZombies() { return null; }

    /**
     * مشخصات یک گیاه را برمی‌گرداند.
     * @param type نوع گیاه
     * @return نمونه گیاه با مشخصات کامل
     */
    public Plant getPlantDetails(PlantType type) { return null; }

    /**
     * مشخصات یک زامبی را برمی‌گرداند.
     * @param type نوع زامبی
     * @return نمونه زامبی با مشخصات کامل
     */
    public Zombie getZombieDetails(ZombieType type) { return null; }

    /**
     * گیاه را ارتقا می‌دهد (سکه + seed packet لازم است).
     * @param user کاربر
     * @param type نوع گیاه
     * @throws exception.GameException اگر منابع کافی نباشد
     */
    public void upgradePlant(User user, PlantType type) { }

    /**
     * یک گیاه جدید خریداری می‌کند (2000 سکه).
     * @param user کاربر
     * @param type نوع گیاه
     * @throws exception.GameException اگر سکه کافی نباشد یا قبلاً خریده
     */
    public void purchasePlant(User user, PlantType type) { }

    /**
     * هزینه ارتقای بعدی گیاه را محاسبه می‌کند.
     * هزینه با هر ارتقا افزایش می‌یابد.
     * @param user کاربر
     * @param type نوع گیاه
     * @return (سکه لازم، seed packet لازم)
     */
    public int[] getUpgradeCost(User user, PlantType type) { return null; }
}
