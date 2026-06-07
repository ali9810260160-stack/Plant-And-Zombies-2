package model;

import model.enums.MenuType;

/**
 * وضعیت کلی برنامه - Singleton.
 * شامل کاربر لاگین‌شده، منوی فعلی و session جاری.
 */
public class AppState {

    private static AppState instance;

    /** کاربر فعلی لاگین‌شده (null اگر لاگین نشده) */
    private User currentUser;

    /** منوی فعلی */
    private MenuType currentMenu;

    /** session بازی جاری (null اگر در بازی نیست) */
    private GameSession currentSession;

    /** صفحه فعلی Travel Log */
    private String currentTravelLogPage;

    private AppState() {
        this.currentMenu = MenuType.REGISTER;
    }

    /** نمونه Singleton را برمی‌گرداند */
    public static AppState getInstance() {
        if (instance == null) {
            instance = new AppState();
        }
        return instance;
    }

    /** بررسی می‌کند آیا کاربری لاگین کرده */
    public boolean isLoggedIn() { return currentUser != null; }

    /** کاربر را لاگ اوت می‌کند */
    public void logout() { this.currentUser = null; }

    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User currentUser) { this.currentUser = currentUser; }
    public MenuType getCurrentMenu() { return currentMenu; }
    public void setCurrentMenu(MenuType currentMenu) { this.currentMenu = currentMenu; }
    public GameSession getCurrentSession() { return currentSession; }
    public void setCurrentSession(GameSession session) { this.currentSession = session; }
    public String getCurrentTravelLogPage() { return currentTravelLogPage; }
    public void setCurrentTravelLogPage(String page) { this.currentTravelLogPage = page; }
}
