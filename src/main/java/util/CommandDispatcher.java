package util;

import controller.*;
import model.AppState;

/**
 * حلقه اصلی برنامه و توزیع‌کننده دستورات.
 * ورودی کاربر را می‌خواند و بر اساس منوی فعلی به کنترلر مناسب هدایت می‌کند.
 */
public class CommandDispatcher {

    private final AppState appState;
    private final AuthController authController;
    private final MenuController menuController;
    private final ProfileController profileController;
    private final GameController gameController;
    private final PlantSelectController plantSelectController;
    private final CollectionController collectionController;
    private final GreenhouseController greenhouseController;
    private final TravelLogController travelLogController;
    private final LeaderboardController leaderboardController;

    public CommandDispatcher(AppState appState,
                             AuthController authController,
                             MenuController menuController,
                             ProfileController profileController,
                             GameController gameController,
                             PlantSelectController plantSelectController,
                             CollectionController collectionController,
                             GreenhouseController greenhouseController,
                             TravelLogController travelLogController,
                             LeaderboardController leaderboardController) {
        this.appState = appState;
        this.authController = authController;
        this.menuController = menuController;
        this.profileController = profileController;
        this.gameController = gameController;
        this.plantSelectController = plantSelectController;
        this.collectionController = collectionController;
        this.greenhouseController = greenhouseController;
        this.travelLogController = travelLogController;
        this.leaderboardController = leaderboardController;
    }

    /**
     * حلقه اصلی برنامه را اجرا می‌کند.
     * تا زمانی که برنامه پایان نیافته ورودی می‌گیرد.
     */
    public void run() { }

    /**
     * یک دستور را بر اساس منوی فعلی پردازش می‌کند.
     * @param input رشته ورودی
     */
    public void dispatch(String input) { }

    /** دستورات منوی ثبت‌نام را پردازش می‌کند */
    private void handleRegisterMenu(String input) { }

    /** دستورات منوی ورود را پردازش می‌کند */
    private void handleLoginMenu(String input) { }

    /** دستورات منوی اصلی را پردازش می‌کند */
    private void handleMainMenu(String input) { }

    /** دستورات منوی بازی را پردازش می‌کند */
    private void handleGameMenu(String input) { }

    /** دستورات منوی کلکسیون را پردازش می‌کند */
    private void handleCollectionMenu(String input) { }

    /** دستورات منوی تنظیمات را پردازش می‌کند */
    private void handleSettingsMenu(String input) { }

    /** دستورات منوی اخبار را پردازش می‌کند */
    private void handleNewsMenu(String input) { }

    /** دستورات منوی پروفایل را پردازش می‌کند */
    private void handleProfileMenu(String input) { }

    /** دستورات منوی گلخانه را پردازش می‌کند */
    private void handleGreenhouseMenu(String input) { }

    /** دستورات فروشگاه را پردازش می‌کند */
    private void handleShopMenu(String input) { }

    /** دستورات Travel Log را پردازش می‌کند */
    private void handleTravelLogMenu(String input) { }

    /** دستورات لیدربورد را پردازش می‌کند */
    private void handleLeaderboardMenu(String input) { }

    /** دستورات انتخاب گیاه را پردازش می‌کند */
    private void handlePlantSelectMenu(String input) { }

    /** دستورات حین بازی را پردازش می‌کند */
    private void handleInGameMenu(String input) { }

    /** دستورات عمومی همه منوها (مثل menu exit) را پردازش می‌کند */
    private boolean handleCommonCommands(String input) { return false; }
}
