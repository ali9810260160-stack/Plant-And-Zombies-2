package service;

import model.NewsItem;
import model.User;
import model.AppState;
import model.enums.PlantType;
import model.enums.ZombieType;
import repository.UserRepository;
import util.HashUtil;

import javax.print.DocFlavor;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * سرویس مدیریت پروفایل کاربر.
 * تغییر اطلاعات، آنلاک گیاه/زامبی، سکه و الماس.
 */
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;

    public UserService(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    /**
     * نام کاربری را تغییر می‌دهد.
     * @param user کاربر
     * @param newUsername نام کاربری جدید
     * @throws exception.ValidationException در صورت تکراری/نامعتبر بودن
     */
    public void changeUsername(User user, String newUsername) {
        if (!authService.isValidUsername(newUsername)) {
            throw new RuntimeException("Invalid username format. Only letters, numbers, and hyphens allowed.");
        }
        if (userRepository.existsByUsername(newUsername)) {
            throw new RuntimeException("Username already taken.");
        }

        String oldUsername = user.getUsername();
        user.setUsername(newUsername);

        userRepository.deleteByUsername(oldUsername);
        userRepository.save(user);

        // اگر کاربر stay-logged-in بود، به‌روز کن
        if (user.isStayLoggedIn()) {
            userRepository.setStayLoggedIn(newUsername);
        }
    }

    /**
     * نام مستعار را تغییر می‌دهد.
     * @param user کاربر
     * @param newNickname نام مستعار جدید
     */
    public void changeNickname(User user, String newNickname) {
        if (newNickname == null || newNickname.trim().isEmpty()) {
            throw new RuntimeException("Nickname cannot be empty.");
        }
        if (newNickname.length() > 32) {
            throw new RuntimeException("Nickname is too long (max 32 characters).");
        }
        if (newNickname.equals(user.getNickname())) {
            throw new RuntimeException("New nickname is the same as current nickname.");
        }

        user.setNickname(newNickname);
        userRepository.save(user);
    }

    /**
     * ایمیل را تغییر می‌دهد.
     * @param user کاربر
     * @param newEmail ایمیل جدید
     */
    public void changeEmail(User user, String newEmail) {
        if (!authService.isValidEmail(newEmail)) {
            throw new RuntimeException("Invalid email format.");
        }
        if (newEmail.equals(user.getEmail())) {
            throw new RuntimeException("New email is the same as current email.");
        }

        user.setEmail(newEmail);
        userRepository.save(user);
    }

    /**
     * رمز عبور را تغییر می‌دهد.
     * @param user کاربر
     * @param oldPassword رمز قدیمی (باید تأیید شود)
     * @param newPassword رمز جدید
     */
    public void changePassword(User user, String oldPassword, String newPassword) {
        String oldHash = HashUtil.sha256(oldPassword);
        if (!oldHash.equals(user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect.");
        }

        String validationError = authService.validatePassword(newPassword);
        if (validationError != null) {
            throw new RuntimeException(validationError);
        }

        if (oldPassword.equals(newPassword)) {
            throw new RuntimeException("New password must be different from current password.");
        }

        user.setPasswordHash(HashUtil.sha256(newPassword));
        userRepository.save(user);
    }

    /**
     * سطح سختی را تغییر می‌دهد.
     * @param user کاربر
     * @param level سطح (1-5)
     */
    public void changeDifficulty(User user, int level) {
        user.setDifficultyLevel(level);
        userRepository.save(user);
    }

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
    public void addNews(User user, String message) {
        if (user == null || message == null || message.trim().isEmpty()) return;

        if (user.getNews() == null){
            user.setNews(new ArrayList<>());
        }

        NewsItem item = new NewsItem(message, System.currentTimeMillis());
        user.getNews().add(item);
        userRepository.save(user);
    }

    /**
     * اخبار خوانده‌نشده را برمی‌گرداند و به عنوان خوانده علامت‌گذاری می‌کند.
     * @param user کاربر
     * @return متن اخبار
     */
    public String getAndMarkUnreadNews(User user) {
        if (user == null) return null;
        List<NewsItem> allNews = user.getNews();
        if (allNews.isEmpty() || allNews == null) return null;

        StringBuilder sb = new StringBuilder();
        boolean hasUnread = false;

        for (NewsItem item : allNews){
            if (!item.isRead()){
                if (hasUnread) sb.append("\n");
                sb.append(item.getMessage());
                item.setRead(true);
                hasUnread = true;
            }
        }
        userRepository.save(user);
        if (!hasUnread) return null;
        return sb.toString();
    }

    /**
     * seed packet یک گیاه را افزایش می‌دهد.
     * @param user کاربر
     * @param type نوع گیاه
     * @param count تعداد
     */
    public void addSeedPackets(User user, PlantType type, int count) { }
}
