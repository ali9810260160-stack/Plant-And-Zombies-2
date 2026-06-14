package controller;

import model.AppState;
import model.User;
import service.UserService;
import view.ConsoleView;

/**
 * کنترلر منوی پروفایل.
 * دستورات تغییر اطلاعات و نمایش پروفایل کاربر.
 */
public class ProfileController {

    private final UserService userService;
    private final AppState appState;
    private final ConsoleView view;

    public ProfileController(UserService userService, AppState appState,
                             ConsoleView view) {
        this.userService = userService;
        this.appState = appState;
        this.view = view;
    }

    /** دستور "menu profile change-username" را پردازش می‌کند */
    public void changeUsername(String newUsername) {
        try {
            userService.changeUsername(appState.getCurrentUser(), newUsername);
            view.printSuccess("Username changed successfully.");
        } catch (RuntimeException e) {
            view.printError(e.getMessage());
        }
    }

    /** دستور "menu profile change-nickname" را پردازش می‌کند */
    public void changeNickname(String newNickname) {
        try {
            userService.changeNickname(appState.getCurrentUser(), newNickname);
            view.printSuccess("Nickname changed successfully.");
        } catch (RuntimeException e) {
            view.printError(e.getMessage());
        }
    }

    /** دستور "menu profile change-email" را پردازش می‌کند */
    public void changeEmail(String newEmail) {
        try {
            userService.changeEmail(appState.getCurrentUser(), newEmail);
            view.printSuccess("Email changed successfully.");
        } catch (RuntimeException e) {
            view.printError(e.getMessage());
        }
    }

    /** دستور "menu profile change-password" را پردازش می‌کند */
    public void changePassword(String newPassword, String oldPassword) {
        try {
            userService.changePassword(appState.getCurrentUser(), oldPassword, newPassword);
            view.printSuccess("Password changed successfully.");
        } catch (RuntimeException e) {
            view.printError(e.getMessage());
        }
    }

    /** دستور "menu profile show-info" را پردازش می‌کند */
    public void showInfo() {
        User user = appState.getCurrentUser();
        view.printInfo(
                "Username: " + user.getUsername() + "\n" +
                        "Nickname: " + user.getNickname() + "\n" +
                        "Games Played: " + user.getGamesPlayed() + "\n" +
                        "Coins: " + user.getCoins() + "\n" +
                        "Gems: " + user.getGems() + "\n" +
                        "Levels Completed: " + user.getLevelsCompleted() + "\n" +
                        "Highest Meo Point: " + user.getHighestMeoPoint()
        );
    }

}
