package service;

import model.User;
import model.enums.Gender;
import model.enums.SecurityQuestion;
import repository.UserRepository;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * سرویس احراز هویت: ثبت‌نام، ورود، بازیابی رمز.
 * اعتبارسنجی تمام فیلدها اینجا انجام می‌شود.
 */
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(repository.UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    /**
     * کاربر جدید ثبت می‌کند.
     * تمام اعتبارسنجی‌ها (username، password، email، nickname) اینجاست.
     * @return کاربر ساخته‌شده
     * @throws exception.ValidationException در صورت خطای اعتبارسنجی
     */
    public User register(String username, String password, String confirmPassword,
                         String nickname, String email, Gender gender) {
        User user = new User(username,password,nickname,email,gender);
        userRepository.save(user);
        return user;
    }

    /**
     * سوال امنیتی را برای کاربر ثبت می‌کند.
     * @param username نام کاربری
     * @param question سوال انتخابی
     * @param answer پاسخ
     * @param confirmAnswer تکرار پاسخ
     */
    public void setSecurityQuestion(String username, SecurityQuestion question,
                                    String answer, String confirmAnswer) {
        User user = userRepository.findByUsername(username);
        user.setSecurityQuestion(question);
        user.setSecurityAnswerHash(answer);
    }

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
    public boolean isValidUsername(String username) {
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9-]+$");
        Matcher matcher = pattern.matcher(username);

        if(!matcher.matches())
            return false;

        return true;
    }

    /**
     * اعتبارسنجی رمز عبور (حداقل 8 حرف، بزرگ، کوچک، عدد، نماد).
     * @param password رمز
     * @return پیام خطا یا null اگر معتبر
     */
    public String validatePassword(String password) {
        if(password.length() < 8){
            return "Your password is too short";
        }

        Pattern pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)" +
                "(?=.*[!#$%^&*()=+{}\\[\\]|/\\\\:;'\",<>?]).*$");
        Matcher matcher = pattern.matcher(password);

        if(!matcher.matches()){
            return "Incorrect password format";
        }

        return null;
    }

    /**
     * اعتبارسنجی ایمیل با regex کامل.
     * @param email ایمیل
     * @return true اگر معتبر باشد
     */
    public boolean isValidEmail(String email) {
        Pattern pattern = Pattern.compile("^(?!.*\\.\\.)[a-zA-Z0-9](?:[a-zA-Z0-9._-]*[a-zA-Z0-9])?@[a-zA-Z0-9]" +
                "(?:[a-zA-Z0-9-]*[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?)*\\.[a-zA-Z]{2,}$");
        Matcher matcher = pattern.matcher(email);

        if(!matcher.matches()){
            return false;
        }

        return true;
    }

    /**
     * رمز عبور را با SHA-256 هش می‌کند.
     * @param password رمز خام
     * @return رمز هش‌شده
     */
    public String hashPassword(String password) { return null; }
}
