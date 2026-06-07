package controller;

import model.AppState;
import service.UserService;
import view.ConsoleView;

/**
 * کنترلر منوی تنظیمات.
 * تغییر سختی بازی و سایر تنظیمات.
 */
public class SettingsController {

    private final UserService userService;
    private final AppState appState;
    private final ConsoleView view;

    public SettingsController(UserService userService, AppState appState,
                              ConsoleView view) {
        this.userService = userService;
        this.appState = appState;
        this.view = view;
    }

    /**
     * دستور "menu settings change-difficulty -l <level>" را پردازش می‌کند.
     * سطح باید بین 1 تا 5 باشد.
     * @param level سطح سختی
     */
    public void changeDifficulty(int level) { }
}
