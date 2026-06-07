package repository;

import model.User;

import java.util.List;

/**
 * مسئول ذخیره و بارگذاری اطلاعات کاربران از/در فایل.
 * داده‌ها به صورت JSON در فایل users.json ذخیره می‌شوند.
 */
public class UserRepository {

    /** مسیر فایل ذخیره‌سازی کاربران */
    private static final String FILE_PATH = "data/users.json";

    /**
     * تمام کاربران را از فایل بارگذاری می‌کند.
     * @return لیست کاربران
     */
    public List<User> loadAll() { return null; }

    /**
     * یک کاربر را در فایل ذخیره می‌کند (اگر وجود دارد update، وگرنه insert).
     * @param user کاربر
     */
    public void save(User user) { }

    /**
     * کاربر را با نام کاربری پیدا می‌کند.
     * @param username نام کاربری
     * @return کاربر یا null
     */
    public User findByUsername(String username) { return null; }

    /**
     * بررسی می‌کند آیا نام کاربری از قبل وجود دارد.
     * @param username نام کاربری
     * @return true اگر تکراری باشد
     */
    public boolean existsByUsername(String username) { return false; }

    /**
     * کاربر stay-logged-in را بارگذاری می‌کند.
     * @return کاربر یا null
     */
    public User loadStayLoggedInUser() { return null; }

    /**
     * تمام کاربران را یکجا ذخیره می‌کند.
     * @param users لیست کاربران
     */
    public void saveAll(List<User> users) { }
}
