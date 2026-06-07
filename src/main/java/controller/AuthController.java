package controller;

import model.AppState;
import service.AuthService;
import view.ConsoleView;

/**
 * کنترلر منوهای ثبت‌نام و ورود.
 * دستورات register، login، forget password و answer را پردازش می‌کند.
 */
public class AuthController {

    private final AuthService authService;
    private final AppState appState;
    private final ConsoleView view;

    /** نام کاربری در حال بازیابی رمز (برای نگه داشتن state چند مرحله‌ای) */
    private String pendingRecoveryUsername;

    public AuthController(AuthService authService, AppState appState, ConsoleView view) {
        this.authService = authService;
        this.appState = appState;
        this.view = view;
    }

    /**
     * دستور register را پردازش می‌کند.
     * @param username نام کاربری
     * @param password رمز عبور
     * @param confirmPassword تکرار رمز
     * @param nickname نام مستعار
     * @param email ایمیل
     * @param genderStr جنسیت (male/female)
     */
    public void register(String username, String password, String confirmPassword,
                         String nickname, String email, String genderStr) { }

    /**
     * دستور "pick question" را پردازش می‌کند.
     * @param questionNumber شماره سوال
     * @param answer پاسخ
     * @param confirmAnswer تکرار پاسخ
     */
    public void pickQuestion(String questionNumber, String answer,
                             String confirmAnswer) { }

    /**
     * دستور login را پردازش می‌کند.
     * @param username نام کاربری
     * @param password رمز عبور
     * @param stayLoggedIn آیا stay-logged-in
     */
    public void login(String username, String password, boolean stayLoggedIn) { }

    /**
     * دستور "forget password" را پردازش می‌کند.
     * @param username نام کاربری
     * @param email ایمیل
     */
    public void forgetPassword(String username, String email) { }

    /**
     * دستور "answer -a <answer>" را پردازش می‌کند (در جریان بازیابی رمز).
     * @param answer پاسخ
     */
    public void answerSecurityQuestion(String answer) { }

    /**
     * رمز عبور جدید را تنظیم می‌کند (بعد از تأیید سوال امنیتی).
     * @param newPassword رمز جدید
     * @param confirmPassword تکرار رمز
     */
    public void setNewPassword(String newPassword, String confirmPassword) { }

    /**
     * کاربر stay-logged-in را در شروع برنامه بارگذاری می‌کند.
     */
    public void tryAutoLogin() { }
}
