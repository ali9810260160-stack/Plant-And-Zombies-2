package controller;

import exception.AuthException;
import exception.ValidationException;
import model.AppState;
import model.User;
import model.enums.Gender;
import model.enums.MenuType;
import model.enums.SecurityQuestion;
import service.AuthService;
import service.UserService;
import view.ConsoleView;

import java.util.regex.Matcher;

/**
 * کنترلر احراز هویت.
 */
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final ConsoleView view;

    private String pendingRecoveryUsername;
    private boolean awaitingAnswer;
    private boolean awaitingNewPassword;

    public AuthController(AuthService authService,
                          UserService userService,
                          ConsoleView view) {
        this.authService = authService;
        this.userService = userService;
        this.view = view;
    }

    public void register(Matcher m, AppState appState) {
        String username = m.group(1);
        String password = m.group(2);
        String confirm = m.group(3);
        String nickname = m.group(4);
        String email = m.group(5);
        String genderStr = m.group(6);
        Gender gender = genderStr.equalsIgnoreCase("male")
                        ? Gender.MALE : Gender.FEMALE;
        User user = authService.register(username, password, confirm,
                                         nickname, email, gender);
        view.printSuccess("User registered! Please set a security question.");
        view.printSecurityQuestions();
        appState.setCurrentUser(user);
    }

    public void pickQuestion(Matcher m, AppState appState) {
        int qNum = Integer.parseInt(m.group(1));
        String answer = m.group(2);
        String confirm = m.group(3);
        SecurityQuestion[] questions = SecurityQuestion.values();
        if (qNum < 1 || qNum > questions.length) {
            view.printError("Invalid question number. Choose 1-"
                + questions.length);
            return;
        }
        SecurityQuestion question = questions[qNum - 1];
        String username = appState.getCurrentUser() != null
                          ? appState.getCurrentUser().getUsername() : "";
        authService.setSecurityQuestion(username, question, answer, confirm);
        view.printSuccess("Security question set! Redirecting to login...");
        appState.setCurrentUser(null);
        appState.setCurrentMenu(MenuType.LOGIN);
    }

    public void login(Matcher m, AppState appState) {
        String username = m.group(1);
        String password = m.group(2);
        boolean stayLoggedIn = m.group(3) != null;
        User user = authService.login(username, password, stayLoggedIn);
        appState.setCurrentUser(user);
        appState.setCurrentMenu(MenuType.MAIN);
        view.printSuccess("Welcome, " + user.getNickname() + "!");
    }

    public void forgetPassword(Matcher m, AppState appState) {
        String username = m.group(1);
        String email = m.group(2);
        SecurityQuestion question =
            authService.initiatePasswordRecovery(username, email);
        pendingRecoveryUsername = username;
        awaitingAnswer = true;
        view.printInfo("Security question: " + question.getDisplayText());
        view.printInfo("Please answer with: answer -a <your_answer>");
    }

    public void answerSecurity(Matcher m, AppState appState) {
        if (!awaitingAnswer || pendingRecoveryUsername == null) {
            view.printError("No active password recovery session.");
            return;
        }
        String answer = m.group(1);
        boolean correct = authService.verifySecurityAnswer(
            pendingRecoveryUsername, answer);
        if (!correct) {
            view.printError("Incorrect answer. Password recovery cancelled.");
            pendingRecoveryUsername = null;
            awaitingAnswer = false;
            appState.setCurrentMenu(MenuType.LOGIN);
            return;
        }
        awaitingAnswer = false;
        awaitingNewPassword = true;
        view.printSuccess("Correct! Enter new password:");
        view.printInfo("Use: new password -p <newpwd> -c <confirm>");
    }

    public void setNewPassword(Matcher m, AppState appState) {
        if (!awaitingNewPassword || pendingRecoveryUsername == null) {
            view.printError("No active password reset session.");
            return;
        }
        String newPwd = m.group(1);
        String confirm = m.group(2);
        if (!newPwd.equals(confirm)) {
            view.printError("Passwords do not match. Try again.");
            return;
        }
        authService.resetPassword(pendingRecoveryUsername, newPwd);
        pendingRecoveryUsername = null;
        awaitingNewPassword = false;
        view.printSuccess("Password reset successfully! Please log in.");
    }

    public void logout(AppState appState) {
        if (!appState.isLoggedIn()) {
            view.printError("No user is logged in.");
            return;
        }
        model.User user = appState.getCurrentUser();
        user.setStayLoggedIn(false);
        userService.save(user);
        appState.logout();
        appState.setCurrentMenu(MenuType.REGISTER);
        view.printSuccess("Logged out successfully.");
    }
}
