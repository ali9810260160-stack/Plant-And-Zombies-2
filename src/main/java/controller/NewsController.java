package controller;

import model.AppState;
import service.UserService;
import view.ConsoleView;

/**
 * کنترلر منوی اخبار.
 * نمایش اخبار خوانده‌نشده و تمام اخبار کاربر.
 */
public class NewsController {

    private final UserService userService;
    private final AppState appState;
    private final ConsoleView view;

    public NewsController(UserService userService, AppState appState, ConsoleView view) {
        this.userService = userService;
        this.appState = appState;
        this.view = view;
    }

    /** دستور "menu news show-unread" - اخبار خوانده‌نشده را نمایش می‌دهد و mark می‌کند */
    public void showUnread() { }

    /** دستور "menu news show-all" - تمام اخبار کاربر را نمایش می‌دهد */
    public void showAll() { }
}
