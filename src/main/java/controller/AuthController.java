package controller;

import model.AppState;
import model.User;
import model.enums.Gender;
import model.enums.MenuType;
import model.enums.SecurityQuestion;
import service.AuthService;
import util.HashUtil;
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
        view.printSuccess("Please pick a security question first.");
        view.printSuccess("Usage: pick question -q <number> -a <answer> -c <confirm>");
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
            break;
            case 2 : securityQuestion = SecurityQuestion.Q2;
            break;
            case 3 : securityQuestion = SecurityQuestion.Q3;
            break;
            case 4 : securityQuestion = SecurityQuestion.Q4;
            break;
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
    public void login(String username, String password, boolean stayLoggedIn) {
        if(!authService.getUserRepository().existsByUsername(username)){
            view.printError("User not found. Please sign up first");
            return;
        }

        User user = authService.getUserRepository().findByUsername(username);

        if(!HashUtil.verify(password,user.getPasswordHash())){
            view.printError("Incorrect password!!!");
            return;
        }

        authService.login(username,password,stayLoggedIn);
        AppState.getInstance().setCurrentMenu(MenuType.MAIN);
        view.printSuccess("Logged in successfully");
    }

    /**
     * دستور "forget password" را پردازش می‌کند.
     * @param username نام کاربری
     * @param email ایمیل
     */
    public boolean forgetPassword(String username, String email) {
        if(!authService.getUserRepository().existsByUsername(username)){
            view.printError("User not found");
            return false;
        }

        User user = authService.getUserRepository().findByUsername(username);

        if(!user.getEmail().equals(email)){
            view.printError("Email does not match");
            return false;
        }

        SecurityQuestion securityQuestion = authService.initiatePasswordRecovery(username,email);
        view.printSuccess("Please answer this question");
        view.printSuccess(securityQuestion.getQuestionText());
        view.printSuccess("Usage: answer -a <your_answer>");
        pendingRecoveryUsername = username;
        return true;
    }

    /**
     * دستور "answer -a <answer>" را پردازش می‌کند (در جریان بازیابی رمز).
     * @param answer پاسخ
     */
    public Boolean answerSecurityQuestion(String answer) {
        if(!authService.verifySecurityAnswer(pendingRecoveryUsername,answer)){
            view.printError("Your answer to security question is wrong.\n" +
                    "Password recovery process has ended.");
            pendingRecoveryUsername = null;
            return false;
        }

        view.printSuccess("Please enter new password.");
        view.printSuccess("Usage: menu profile change-password -p <new> -o <confirm>");
        return true;
    }

    /**
     * رمز عبور جدید را تنظیم می‌کند (بعد از تأیید سوال امنیتی).
     * @param newPassword رمز جدید
     * @param confirmPassword تکرار رمز
     */
    public void setNewPassword(String newPassword, String confirmPassword) {
        String error = authService.validatePassword(newPassword);

        if(error != null){
            view.printError("Your password is weak\n" + error + "\nPassword recovery process has ended.");
            pendingRecoveryUsername = null;
            return;
        }

        if(!newPassword.equals(confirmPassword)){
            view.printError("Your password and its confirmation do not match!!!\n" +
                    "Password recovery process has ended.");
            pendingRecoveryUsername = null;
            return;
        }

        authService.resetPassword(pendingRecoveryUsername,newPassword);
        view.printSuccess("Your password changed successfully");
    }

    /**
     * کاربر stay-logged-in را در شروع برنامه بارگذاری می‌کند.
     */
    public void tryAutoLogin() {
        User user = authService.getUserRepository().loadStayLoggedInUser();

        if (user != null && user.isStayLoggedIn()) {
            appState.setCurrentUser(user);
            appState.setCurrentMenu(MenuType.MAIN);
            view.printSuccess("Welcome back, " + user.getNickname() + "!");
        }
    }
}
