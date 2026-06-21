import controller.*;
import model.AppState;
import repository.LevelRepository;
import repository.PlantDataRepository;
import repository.UserRepository;
import service.*;
import util.CommandDispatcher;
import view.ConsoleView;
import view.MapView;

/**
 * نقطه ورود اصلی برنامه.
 * Wiring تمام وابستگی‌ها (Dependency Injection دستی) اینجاست.
 */
public class Main {

    /**
     * متد main - شروع برنامه.
     * @param args آرگومان‌های خط فرمان
     */
    public static void main(String[] args) {
        // --- Repositories ---
        UserRepository userRepository = new UserRepository();
        LevelRepository levelRepository = new LevelRepository();
        PlantDataRepository plantDataRepository = new PlantDataRepository();

        // --- Services ---
        AuthService authService = new AuthService(userRepository);
        UserService userService = new UserService(userRepository);
        ScoreService scoreService = new ScoreService();
        SunService sunService = new SunService();
        WaveService waveService = new WaveService();
        GameService gameService = new GameService(waveService, sunService, scoreService);
        CollectionService collectionService = new CollectionService(userService);
        GreenhouseService greenhouseService = new GreenhouseService(userService);
        ShopService shopService = new ShopService(userService);
        QuestService questService = new QuestService(userService);
        CombatService combatService = new CombatService();

        // --- Views ---
        ConsoleView consoleView = new ConsoleView();
        MapView mapView = new MapView();

        // --- AppState ---
        AppState appState = AppState.getInstance();

        // --- Controllers ---
        AuthController authController = new AuthController(authService, appState, consoleView);
        MenuController menuController = new MenuController(appState, consoleView);
        ProfileController profileController = new ProfileController(userService, appState, consoleView);
        SettingsController settingsController = new SettingsController(userService, appState, consoleView);
        NewsController newsController = new NewsController(userService, appState, consoleView);
        GameController gameController = new GameController(gameService, appState, consoleView, mapView);
        PlantSelectController plantSelectController = new PlantSelectController(gameService, userService, appState, consoleView);
        CollectionController collectionController = new CollectionController(collectionService, appState, consoleView);
        GreenhouseController greenhouseController = new GreenhouseController(greenhouseService, shopService, appState, consoleView);
        TravelLogController travelLogController = new TravelLogController(questService, appState, consoleView);
        LeaderboardController leaderboardController = new LeaderboardController(scoreService, userService, appState, consoleView);

        // --- Dispatcher ---
        CommandDispatcher dispatcher = new CommandDispatcher(
                appState, authController, menuController, profileController,
                settingsController, newsController, gameController,
                plantSelectController, collectionController,
                greenhouseController, travelLogController, leaderboardController
        );

        // بارگذاری کاربر stay-logged-in
        authController.tryAutoLogin();

        // شروع حلقه اصلی
        dispatcher.run();
    }
}
