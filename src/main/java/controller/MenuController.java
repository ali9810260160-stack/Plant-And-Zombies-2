package controller;

import model.AppState;
import model.enums.MenuType;
import view.ConsoleView;

import java.awt.*;

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
    public void enterMenu(String menuName) {
        MenuType current = appState.getCurrentMenu();
        MenuType target = parseMenuName(menuName);

        if (target == null){
            view.printError("Invalid menu name");
            return;
        }
        if (!isTransitionAllowed(current, target)){
            view.printError("You can't enter this menu from here");
            return;
        }
        appState.setPreviousMenu(current);
        appState.setCurrentMenu(target);
        view.printSuccess("You are entered in " + menuName + " menu");
    }

    /**
     * دستور "menu exit" را پردازش می‌کند.
     * بر اساس منوی فعلی، به منوی مناسب می‌رود یا برنامه را تمام می‌کند.
     */
    public void exitMenu() {
        MenuType previous = getPreviousMenu(appState.getCurrentMenu());

        if (previous == null) {
            view.printSuccess("Exiting program...");
            return;
        }

        appState.setCurrentMenu(previous);
    }

    /**
     * دستور "menu show current" را پردازش می‌کند.
     */
    public void showCurrentMenu() {
        MenuType current = appState.getCurrentMenu();
        view.printSuccess("current menu: " + current);
    }

    /**
     * دستور "menu logout" را پردازش می‌کند.
     */
    public void logout() {
        if (!appState.isLoggedIn()){
            view.printError("You are not logged in!");
            return;
        }
        appState.logout();
        appState.setCurrentMenu(MenuType.REGISTER);
        appState.setPreviousMenu(null);
        view.printSuccess("Logged in successfully.");
    }

    /**
     * بر اساس منوی فعلی، منوی قبلی را برمی‌گرداند.
     * @param current منوی فعلی
     * @return منوی قبلی
     */
    private MenuType getPreviousMenu(MenuType current) {
        switch (current) {
            case LOGIN:
                return MenuType.REGISTER;
            case MAIN:
                return MenuType.LOGIN;
            case SETTINGS:
            case NEWS:
            case PROFILE:
            case GAME:
                return MenuType.MAIN;
            case SHOP:
                return MenuType.GREENHOUSE;
            case PLANT_SELECT:
            case COLLECTION:
                return MenuType.GAME;
            case IN_GAME:
                return MenuType.PLANT_SELECT;
            case GREENHOUSE:
            case TRAVEL_LOG:
            case LEADERBOARD:
                return appState.getPreviousMenu();
            default:
                return null;
        }
    }

    /**
     * نام رشته‌ای منو را به MenuType تبدیل می‌کند.
     * @param name نام
     * @return MenuType یا null اگر نامعتبر باشد
     */
    private MenuType parseMenuName(String name) {
        if (name == null) return null;

        switch (name.toLowerCase().trim()) {
            case "login":           return MenuType.LOGIN;
            case "register":        return MenuType.REGISTER;
            case "main":            return MenuType.MAIN;
            case "chapter":         return MenuType.GAME;
            case "collection":      return MenuType.COLLECTION;
            case "settings":        return MenuType.SETTINGS;
            case "news":            return MenuType.NEWS;
            case "profile":         return MenuType.PROFILE;
            case "greenhouse":      return MenuType.GREENHOUSE;
            case "shop":            return MenuType.SHOP;
            case "travel-log":      return MenuType.TRAVEL_LOG;
            case "leaderboard":     return MenuType.LEADERBOARD;
            default:                return null;
        }
    }

    /**
     * بررسی می کند که امکان انتقال از منوی فعلی به منوی مقصد وجود دارد یا خیر.
     * @param current منوی فعلی
     * @param target منوی مقصد و نهایی
     * @return
     */
    private boolean isTransitionAllowed(MenuType current, MenuType target) {
        switch (current) {
            case REGISTER:
                // از منوی ثبت نام فقط می تواند به منوی ورود برود.
                return target == MenuType.LOGIN;

            case LOGIN:
                // بعد از login موفق فقط می تواند به منوی اصلی برود.
                // یا اینکه برگردد به منوی ثبت نام.
                return target == MenuType.REGISTER;

            case MAIN:
                // منوی اصلی میتونه بره به:
                return target == MenuType.GAME ||
                        target == MenuType.SETTINGS ||
                        target == MenuType.NEWS ||
                        target == MenuType.PROFILE ||
                        target == MenuType.COLLECTION ||
                        target == MenuType.GREENHOUSE ||
                        target == MenuType.TRAVEL_LOG ||
                        target == MenuType.LEADERBOARD;

            case GAME:
                // از منوی بازی هم میتونه بره به:
                return target == MenuType.PLANT_SELECT ||
                        target == MenuType.COLLECTION ||
                        target == MenuType.GREENHOUSE ||
                        target == MenuType.TRAVEL_LOG ||
                        target == MenuType.LEADERBOARD;

            default:
                return false;
        }
    }
}
