package controller;

import model.AppState;
import model.enums.MenuType;
import view.ConsoleView;

/**
 * کنترلر اصلی ناوبری بین منوها.
 * دستورات menu enter، menu exit و menu show current را پردازش می‌کند.
 */
public class MenuController {

    private final AppState appState;
    private final ConsoleView view;

    public MenuController(AppState appState, ConsoleView view) {
        this.appState = appState;
        this.view = view;
    }

    /**
     * دستور "menu enter <menu_name>" را پردازش می‌کند.
     * @param menuName نام منوی مقصد
     */
    public void enterMenu(String menuName) { }

    /**
     * دستور "menu exit" را پردازش می‌کند.
     * بر اساس منوی فعلی، به منوی مناسب می‌رود یا برنامه را تمام می‌کند.
     */
    public void exitMenu() { }

    /**
     * دستور "menu show current" را پردازش می‌کند.
     */
    public void showCurrentMenu() { }

    /**
     * دستور "menu logout" را پردازش می‌کند.
     */
    public void logout() { }

    /**
     * بر اساس منوی فعلی، منوی قبلی را برمی‌گرداند.
     * @param current منوی فعلی
     * @return منوی قبلی
     */
    private MenuType getPreviousMenu(MenuType current) { return null; }

    /**
     * نام رشته‌ای منو را به MenuType تبدیل می‌کند.
     * @param name نام
     * @return MenuType یا null اگر نامعتبر باشد
     */
    private MenuType parseMenuName(String name) { return null; }
}
