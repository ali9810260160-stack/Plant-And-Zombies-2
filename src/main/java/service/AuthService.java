package service;

import model.User;
import model.enums.Gender;
import model.enums.SecurityQuestion;

/**
 * سرویس احراز هویت: ثبت‌نام، ورود، بازیابی رمز.
 * اعتبارسنجی تمام فیلدها اینجا انجام می‌شود.
 */
public class AuthService {

    private final repository.UserRepository userRepository;

    public AuthService(repository.UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * کاربر جدید ثبت می‌کند.
     * تمام اعتبارسنجی‌ها (username، password، email، nickname) اینجاست.
     * @return کاربر ساخته‌شده
     * @throws exception.ValidationException در صورت خطای اعتبارسنجی
     */
    public User register(String username, String password, String confirmPassword,
                         String nickname, String email, Gender gender) {
        return null;
    }

    /**
     * سوال امنیتی را برای کاربر ثبت می‌کند.
     * @param username نام کاربری
     * @param question سوال انتخابی
     * @param answer پاسخ
     * @param confirmAnswer تکرار پاسخ
     */
    public void setSecurityQuestion(String username, SecurityQuestion question,
                                    String answer, String confirmAnswer) { }

    /**
     * ورود کاربر را انجام می‌دهد.
     * @param username نام کاربری
     * @param password رمز عبور
     * @param stayLoggedIn آیا stay logged in
     * @return کاربر یافت‌شده
     * @throws exception.AuthException در صورت خطا
     */
    public User login(String username, String password, boolean stayLoggedIn) {
        return null;
    }

    /**
     * فراموشی رمز عبور - ارسال سوال امنیتی.
     * @param username نام کاربری
     * @param email ایمیل
     * @return سوال امنیتی کاربر
     */
    public SecurityQuestion initiatePasswordRecovery(String username, String email) {
        return null;
    }

    /**
     * پاسخ سوال امنیتی را بررسی می‌کند.
     * @param username نام کاربری
     * @param answer پاسخ
     * @return true اگر صحیح باشد
     */
    public boolean verifySecurityAnswer(String username, String answer) { return false; }

    /**
     * رمز عبور جدید را ست می‌کند (بعد از تأیید سوال امنیتی).
     * @param username نام کاربری
     * @param newPassword رمز عبور جدید
     */
    public void resetPassword(String username, String newPassword) { }

    /**
     * اعتبارسنجی نام کاربری (فقط حروف، اعداد، خط‌تیره).
     * @param username نام کاربری
     * @return true اگر معتبر باشد
     */
    public boolean isValidUsername(String username) { return false; }

    /**
     * اعتبارسنجی رمز عبور (حداقل 8 حرف، بزرگ، کوچک، عدد، نماد).
     * @param password رمز
     * @return پیام خطا یا null اگر معتبر
     */
    public String validatePassword(String password) { return null; }

    /**
     * اعتبارسنجی ایمیل با regex کامل.
     * @param email ایمیل
     * @return true اگر معتبر باشد
     */
    public boolean isValidEmail(String email) { return false; }

    /**
     * رمز عبور را با SHA-256 هش می‌کند.
     * @param password رمز خام
     * @return رمز هش‌شده
     */
    public String hashPassword(String password) { return null; }
}
