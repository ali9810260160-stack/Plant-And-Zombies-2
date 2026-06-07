package service;

import model.User;
import model.enums.PlantType;
import model.enums.ZombieType;
import repository.UserRepository;

/**
 * سرویس مدیریت پروفایل کاربر.
 * تغییر اطلاعات، آنلاک گیاه/زامبی، سکه و الماس.
 */
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * نام کاربری را تغییر می‌دهد.
     * @param user کاربر
     * @param newUsername نام کاربری جدید
     * @throws exception.ValidationException در صورت تکراری/نامعتبر بودن
     */
    public void changeUsername(User user, String newUsername) { }

    /**
     * نام مستعار را تغییر می‌دهد.
     * @param user کاربر
     * @param newNickname نام مستعار جدید
     */
    public void changeNickname(User user, String newNickname) { }

    /**
     * ایمیل را تغییر می‌دهد.
     * @param user کاربر
     * @param newEmail ایمیل جدید
     */
    public void changeEmail(User user, String newEmail) { }

    /**
     * رمز عبور را تغییر می‌دهد.
     * @param user کاربر
     * @param oldPassword رمز قدیمی (باید تأیید شود)
     * @param newPassword رمز جدید
     */
    public void changePassword(User user, String oldPassword, String newPassword) { }

    /**
     * سطح سختی را تغییر می‌دهد.
     * @param user کاربر
     * @param level سطح (1-5)
     */
    public void changeDifficulty(User user, int level) { }

    /**
     * یک گیاه را برای کاربر آنلاک می‌کند.
     * @param user کاربر
     * @param type نوع گیاه
     */
    public void unlockPlant(User user, PlantType type) { }

    /**
     * یک زامبی را برای کاربر ثبت می‌کند (دیده شده).
     * @param user کاربر
     * @param type نوع زامبی
     */
    public void registerSeenZombie(User user, ZombieType type) { }

    /**
     * سکه اضافه می‌کند.
     * @param user کاربر
     * @param amount مقدار
     */
    public void addCoins(User user, long amount) { }

    /**
     * الماس اضافه می‌کند.
     * @param user کاربر
     * @param amount مقدار
     */
    public void addGems(User user, int amount) { }

    /**
     * سکه کم می‌کند. اگر کافی نباشد exception می‌دهد.
     * @param user کاربر
     * @param amount مقدار
     */
    public void deductCoins(User user, long amount) { }

    /**
     * الماس کم می‌کند. اگر کافی نباشد exception می‌دهد.
     * @param user کاربر
     * @param amount مقدار
     */
    public void deductGems(User user, int amount) { }

    /**
     * یک گلدان اضافه می‌کند (خرید از فروشگاه).
     * @param user کاربر
     */
    public void addPot(User user) { }

    /**
     * غذای گیاه اضافه می‌کند (حداکثر 3).
     * @param user کاربر
     */
    public void addPlantFood(User user) { }

    /**
     * غذای گیاه مصرف می‌کند.
     * @param user کاربر
     */
    public void usePlantFood(User user) { }

    /**
     * یک خبر به لیست اخبار کاربر اضافه می‌کند.
     * @param user کاربر
     * @param message متن خبر
     */
    public void addNews(User user, String message) { }

    /**
     * اخبار خوانده‌نشده را برمی‌گرداند و به عنوان خوانده علامت‌گذاری می‌کند.
     * @param user کاربر
     * @return متن اخبار
     */
    public String getAndMarkUnreadNews(User user) { return null; }

    /**
     * seed packet یک گیاه را افزایش می‌دهد.
     * @param user کاربر
     * @param type نوع گیاه
     * @param count تعداد
     */
    public void addSeedPackets(User user, PlantType type, int count) { }
}
