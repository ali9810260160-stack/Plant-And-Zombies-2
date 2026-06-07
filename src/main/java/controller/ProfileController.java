package controller;

import model.AppState;
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
    public void changeUsername(String newUsername) { }

    /** دستور "menu profile change-nickname" را پردازش می‌کند */
    public void changeNickname(String newNickname) { }

    /** دستور "menu profile change-email" را پردازش می‌کند */
    public void changeEmail(String newEmail) { }

    /** دستور "menu profile change-password" را پردازش می‌کند */
    public void changePassword(String newPassword, String oldPassword) { }

    /** دستور "menu profile show-info" را پردازش می‌کند */
    public void showInfo() { }
}
