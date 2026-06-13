package controller;

import model.AppState;
import model.User;
import model.enums.Gender;
import model.enums.MenuType;
import model.enums.SecurityQuestion;
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

    /** نام کاربری در حال بازیابی رمز (برای نگه داشتن state چند مرحله‌ای). */
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
    public User register(String username, String password, String confirmPassword
            , String nickname, String email, String genderStr) {
        if(!authService.isValidUsername(username)){
            view.printError("Incorrect username format.");
            return null;
        }

        if(authService.getUserRepository().existsByUsername(username)){
            view.printError("Username already exists.");
            return null;
        }

        String error = authService.validatePassword(password);
        
        if(error != null){
            view.printError("Your password is weak\n" + error);
            return null;
        }

        if(!password.equals(confirmPassword)){
            view.printError("Your password and its confirmation do not match!!!\n" +
                    "Please re-enter your confirmation password with this format " +
                    "\"password : <password> confirm password : <confirmpassword>\"\nor return to the start menu");
            return new User(username,null,nickname,email,Gender.valueOf(genderStr.toUpperCase()));
        }

        if(nickname.length() < 3 || nickname.length() > 30){
            view.printError("Your nickname length is invalid");
            return null;
        }

        if(!authService.isValidEmail(email)){
            view.printError("Incorrect email format");
            return null;
        }

        authService.register(username,password,confirmPassword,nickname,email, Gender.valueOf(genderStr.toUpperCase()));
        view.printInfo("1." + SecurityQuestion.Q1.getQuestionText() + "\n" +
                "2." + SecurityQuestion.Q2.getQuestionText() + "\n" +
                "3." + SecurityQuestion.Q3.getQuestionText() + "\n" +
                "4." + SecurityQuestion.Q4.getQuestionText() + "\n" +
                "5." + SecurityQuestion.Q5.getQuestionText());
        view.printSuccess("Please pick a security question first.\n" +
                "Usage: pick question -q <number> -a <answer> -c <confirm>");
        return new User(username,password,nickname,email,Gender.valueOf(genderStr.toUpperCase()));
    }

    /**
     * دستور "pick question" را پردازش می‌کند.
     * @param questionNumber شماره سوال
     * @param answer پاسخ
     * @param confirmAnswer تکرار پاسخ
     */
    public void pickQuestion(String questionNumber, String answer,
                             String confirmAnswer, String username) {
        int questionNumber1 = Integer.parseInt(questionNumber);
        SecurityQuestion securityQuestion = null;

        switch (questionNumber1){
            case 1 : securityQuestion = SecurityQuestion.Q1;
            case 2 : securityQuestion = SecurityQuestion.Q2;
            case 3 : securityQuestion = SecurityQuestion.Q3;
            case 4 : securityQuestion = SecurityQuestion.Q4;
            case 5 : securityQuestion = SecurityQuestion.Q5;
        }

        authService.setSecurityQuestion(username,securityQuestion,answer,confirmAnswer);
        view.printSuccess("You registered successfully");
        AppState.getInstance().setCurrentMenu(MenuType.LOGIN);
    }

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
