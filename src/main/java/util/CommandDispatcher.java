package util;

import controller.*;
import controller.MinigameController;
import exception.AuthException;
import exception.GameException;
import exception.ValidationException;
import model.AppState;
import model.enums.MenuType;
import repository.UserRepository;
import service.*;
import view.ConsoleView;
import view.MapView;

import java.util.Scanner;
import java.util.regex.Matcher;

/**
 * حلقه اصلی ورودی — دستورات را دریافت و به کنترلر مناسب هدایت می‌کند.
 */
public class CommandDispatcher {

    private final Scanner scanner;
    private final ConsoleView view;
    private final AppState appState;

    private final AuthController authController;
    private final MenuController menuController;
    private final ProfileController profileController;
    private final CollectionController collectionController;
    private final NewsController newsController;
    private final GameController gameController;
    private final GreenhouseController greenhouseController;
    private final TravelLogController travelLogController;
    private final LeaderboardController leaderboardController;
    private final MinigameController minigameController;

    public CommandDispatcher() {
        this.scanner = new Scanner(System.in);
        this.view = new ConsoleView();
        this.appState = AppState.getInstance();
        MapView mapView = new MapView();

        UserRepository userRepo = new UserRepository();
        UserService userService = new UserService(userRepo);
        AuthService authService = new AuthService(userRepo);

        WaveService waveService = new WaveService(view);
        CombatService combatService = new CombatService(view);
        SunService sunService = new SunService(view);
        GameService gameService = new GameService(
            waveService, combatService, sunService, view, mapView);
        GreenhouseService greenhouseService = new GreenhouseService(
            userService, userRepo, view);

        this.authController = new AuthController(authService, userService, view);
        this.menuController = new MenuController(view);
        this.profileController = new ProfileController(userService, view);
        this.collectionController = new CollectionController(userService, view);
        this.newsController = new NewsController(view);
        this.gameController = new GameController(
            gameService, sunService, view, mapView);
        this.greenhouseController = new GreenhouseController(
            greenhouseService, view);
        this.travelLogController = new TravelLogController(view);
        this.leaderboardController = new LeaderboardController(userService, view);
        this.minigameController = new MinigameController(view, userService);
    }

    public void run() {
        view.printWelcomeBanner();
        tryAutoLogin();
        promptCurrentMenu();

        while (scanner.hasNextLine()) {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                continue;
            }
            handleInput(input);
        }
    }

    private void tryAutoLogin() {
        try {
            UserRepository userRepo = new UserRepository();
            model.User stayUser = userRepo.loadStayLoggedInUser();
            if (stayUser != null) {
                appState.setCurrentUser(stayUser);
                appState.setCurrentMenu(MenuType.MAIN);
                view.printSuccess("Welcome back, "
                    + stayUser.getNickname() + "!");
            }
        } catch (Exception ignored) { }
    }

    private void handleInput(String input) {
        try {
            MenuType menu = appState.getCurrentMenu();
            dispatch(menu, input);
        } catch (AuthException | ValidationException | GameException e) {
            view.printError(e.getMessage());
        } catch (Exception e) {
            view.printError("Unexpected error: " + e.getMessage());
        }
        promptCurrentMenu();
    }

    private void dispatch(MenuType menu, String input) {
        if (handleUniversalCommands(input)) {
            return;
        }
        switch (menu) {
            case REGISTER:   handleRegisterMenu(input); break;
            case LOGIN:      handleLoginMenu(input); break;
            case MAIN:       handleMainMenu(input); break;
            case SETTINGS:   handleSettingsMenu(input); break;
            case PROFILE:    handleProfileMenu(input); break;
            case NEWS:       handleNewsMenu(input); break;
            case GAME:       handleGameMenu(input); break;
            case COLLECTION: handleCollectionMenu(input); break;
            case PLANT_SELECT: handlePlantSelectMenu(input); break;
            case IN_GAME:    handleInGameMenu(input); break;
            case GREENHOUSE: handleGreenhouseMenu(input); break;
            case SHOP:       handleShopMenu(input); break;
            case TRAVEL_LOG: handleTravelLogMenu(input); break;
            case LEADERBOARD: handleLeaderboardMenu(input); break;
            default:         view.printError("Unknown menu state."); break;
        }
    }

    private boolean handleUniversalCommands(String input) {
        Matcher m;
        if ((m = InputParser.match(input, model.enums.CommandRegex.MENU_SHOW_CURRENT))
                != null) {
            view.printCurrentMenu(appState.getCurrentMenu().name());
            return true;
        }
        if ((m = InputParser.match(input, model.enums.CommandRegex.MENU_EXIT)) != null) {
            menuController.handleExit(appState);
            return true;
        }
        if ((m = InputParser.match(input, model.enums.CommandRegex.MENU_LOGOUT)) != null) {
            authController.logout(appState);
            return true;
        }
        return false;
    }

    private void handleRegisterMenu(String input) {
        Matcher m;
        if ((m = InputParser.match(input,
                model.enums.CommandRegex.REGISTER)) != null) {
            authController.register(m, appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.PICK_QUESTION)) != null) {
            authController.pickQuestion(m, appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.MENU_ENTER)) != null) {
            menuController.handleEnter(m.group(1), appState);
        } else {
            view.printError("Unknown command in Register menu.");
        }
    }

    private void handleLoginMenu(String input) {
        Matcher m;
        if ((m = InputParser.match(input,
                model.enums.CommandRegex.LOGIN)) != null) {
            authController.login(m, appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.FORGET_PASSWORD)) != null) {
            authController.forgetPassword(m, appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.ANSWER_SECURITY)) != null) {
            authController.answerSecurity(m, appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.SET_NEW_PASSWORD)) != null) {
            authController.setNewPassword(m, appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.MENU_ENTER)) != null) {
            menuController.handleEnter(m.group(1), appState);
        } else {
            view.printError("Unknown command in Login menu.");
        }
    }

    private void handleMainMenu(String input) {
        Matcher m;
        if ((m = InputParser.match(input,
                model.enums.CommandRegex.MENU_ENTER)) != null) {
            menuController.handleEnter(m.group(1), appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.MENU_ENTER_CHAPTER)) != null) {
            handleChapterEntry(m.group(1), appState);
        } else if (InputParser.matches(input,
                model.enums.CommandRegex.MENU_GREENHOUSE)) {
            appState.setCurrentMenu(MenuType.GREENHOUSE);
        } else if (InputParser.matches(input,
                model.enums.CommandRegex.MENU_TRAVEL_LOG)) {
            appState.setCurrentMenu(MenuType.TRAVEL_LOG);
        } else if (InputParser.matches(input,
                model.enums.CommandRegex.MENU_LEADERBOARD)) {
            appState.setCurrentMenu(MenuType.LEADERBOARD);
            leaderboardController.show(appState);
        } else if (InputParser.matches(input,
                model.enums.CommandRegex.MENU_COIN_WALLET)) {
            view.printInfo("Coins: "
                + appState.getCurrentUser().getCoins());
        } else if (InputParser.matches(input,
                model.enums.CommandRegex.MENU_GEM_WALLET)) {
            view.printInfo("Gems: "
                + appState.getCurrentUser().getGems());
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.CHEAT_ADD_CURRENCY)) != null) {
            gameController.cheatCurrency(m, appState);
        } else {
            view.printError("Unknown command in Main menu.");
        }
    }


    /**
     * روتینگ دستور "menu enter chapter" از هر منو.
     * @return true اگر هندل شد
     */
    private boolean handleChapterEntry(String chapterArg, AppState appState) {
        String arg = chapterArg.toUpperCase();
        try {
            if (arg.startsWith("VASEBREAKER_")) {
                int lvl = Integer.parseInt(arg.replace("VASEBREAKER_", ""));
                minigameController.startVasebreaker(lvl, appState);
                return true;
            }
            if (arg.startsWith("BOWLING_")) {
                int lvl = Integer.parseInt(arg.replace("BOWLING_", ""));
                minigameController.startBowling(lvl, appState);
                return true;
            }
            if (arg.startsWith("IZOMBIE_")) {
                int lvl = Integer.parseInt(arg.replace("IZOMBIE_", ""));
                minigameController.startIZombie(lvl, appState);
                return true;
            }
            if (arg.startsWith("BEGHOULED_")) {
                int lvl = Integer.parseInt(arg.replace("BEGHOULED_", ""));
                minigameController.startBeghouled(lvl, appState);
                return true;
            }
            if (arg.startsWith("ZOMBOTANY_")) {
                int lvl = Integer.parseInt(arg.replace("ZOMBOTANY_", ""));
                minigameController.startZombotany(lvl, appState);
                return true;
            }
            if (arg.equals("SCORED_GAME")) {
                minigameController.startScoredGame(appState);
                return true;
            }
            gameController.enterChapter(chapterArg, appState);
            return true;
        } catch (NumberFormatException e) {
            view.printError("Invalid minigame level in: " + chapterArg);
            return false;
        }
    }

    private void handleSettingsMenu(String input) {
        Matcher m;
        if ((m = InputParser.match(input,
                model.enums.CommandRegex.CHANGE_DIFFICULTY)) != null) {
            profileController.changeDifficulty(m, appState);
        } else {
            view.printError("Unknown settings command.");
        }
    }

    private void handleProfileMenu(String input) {
        Matcher m;
        if ((m = InputParser.match(input,
                model.enums.CommandRegex.CHANGE_USERNAME)) != null) {
            profileController.changeUsername(m, appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.CHANGE_NICKNAME)) != null) {
            profileController.changeNickname(m, appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.CHANGE_EMAIL)) != null) {
            profileController.changeEmail(m, appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.CHANGE_PASSWORD)) != null) {
            profileController.changePassword(m, appState);
        } else if (InputParser.matches(input,
                model.enums.CommandRegex.SHOW_PROFILE_INFO)) {
            profileController.showInfo(appState);
        } else {
            view.printError("Unknown profile command.");
        }
    }

    private void handleNewsMenu(String input) {
        if (InputParser.matches(input, model.enums.CommandRegex.NEWS_SHOW_UNREAD)) {
            newsController.showUnread(appState);
        } else if (InputParser.matches(input,
                model.enums.CommandRegex.NEWS_SHOW_ALL)) {
            newsController.showAll(appState);
        } else {
            view.printError("Unknown news command.");
        }
    }

    private void handleGameMenu(String input) {
        Matcher m;
        if ((m = InputParser.match(input,
                model.enums.CommandRegex.MENU_ENTER_CHAPTER)) != null) {
            handleChapterEntry(m.group(1), appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.MENU_ENTER)) != null) {
            menuController.handleEnter(m.group(1), appState);
        } else {
            view.printError("Unknown game menu command.");
        }
    }

    private void handleCollectionMenu(String input) {
        Matcher m;
        if (InputParser.matches(input,
                model.enums.CommandRegex.COLLECTION_SHOW_PLANTS)) {
            collectionController.showPlants(appState);
        } else if (InputParser.matches(input,
                model.enums.CommandRegex.COLLECTION_SHOW_ALL_PLANTS)) {
            collectionController.showAllPlants();
        } else if (InputParser.matches(input,
                model.enums.CommandRegex.COLLECTION_SHOW_ZOMBIES)) {
            collectionController.showZombies(appState);
        } else if (InputParser.matches(input,
                model.enums.CommandRegex.COLLECTION_SHOW_ALL_ZOMBIES)) {
            collectionController.showAllZombies();
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.COLLECTION_SHOW_PLANT)) != null) {
            collectionController.showPlant(m.group(1));
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.COLLECTION_SHOW_ZOMBIE)) != null) {
            collectionController.showZombie(m.group(1));
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.COLLECTION_UPGRADE_PLANT)) != null) {
            collectionController.upgradePlant(m.group(1), appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.COLLECTION_PURCHASE_PLANT)) != null) {
            collectionController.purchasePlant(m.group(1), appState);
        } else {
            view.printError("Unknown collection command.");
        }
    }

    private void handlePlantSelectMenu(String input) {
        Matcher m;
        if (InputParser.matches(input, model.enums.CommandRegex.SHOW_ALL_PLANTS)) {
            gameController.showAllPlantsForSelect(appState);
        } else if (InputParser.matches(input,
                model.enums.CommandRegex.SHOW_AVAILABLE_PLANTS)) {
            gameController.showAvailablePlants(appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.ADD_PLANT_SELECT)) != null) {
            gameController.addPlantToSelect(m.group(1), appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.REMOVE_PLANT_SELECT)) != null) {
            gameController.removePlantFromSelect(m.group(1), appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.BOOST_PLANT_SELECT)) != null) {
            gameController.boostPlant(m.group(1), appState);
        } else if (InputParser.matches(input, model.enums.CommandRegex.START_GAME)) {
            gameController.startGame(appState);
        } else {
            view.printError("Unknown plant select command.");
        }
    }
    private void handleInGameMenu(String input) {
        Matcher m;
        // اگر مینی‌گیمی فعال است، دستور ابتدا به آن داده می‌شود
        if (appState.getActiveMinigame() != null
                && minigameController.handleMinigameCommand(input, appState)) {
            return;
        }
        if ((m = InputParser.match(input,
                model.enums.CommandRegex.ADVANCE_TIME)) != null) {
            gameController.advanceTime(
                Integer.parseInt(m.group(1)), appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.PLANT_PLANT)) != null) {
            gameController.plantPlant(m, appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.PLUCK_PLANT)) != null) {
            gameController.pluckPlant(m, appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.COLLECT_SUN)) != null) {
            gameController.collectSun(m, appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.FEED_PLANT)) != null) {
            gameController.feedPlant(m, appState);
        } else if (InputParser.matches(input, model.enums.CommandRegex.SHOW_MAP)) {
            gameController.showMap(appState);
        } else if (InputParser.matches(input,
                model.enums.CommandRegex.SHOW_PLANTS_STATUS)) {
            gameController.showPlantsStatus(appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.SHOW_TILE_STATUS)) != null) {
            gameController.showTileStatus(m, appState);
        } else if (InputParser.matches(input,
                model.enums.CommandRegex.SHOW_SUN_AMOUNT)) {
            gameController.showSunAmount(appState);
        } else if (InputParser.matches(input,
                model.enums.CommandRegex.ZOMBIES_INFO)) {
            gameController.showZombiesInfo(appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.CHEAT_ADD_SUNS)) != null) {
            gameController.cheatAddSuns(
                Integer.parseInt(m.group(1)), appState);
        } else if (InputParser.matches(input,
                model.enums.CommandRegex.CHEAT_RELEASE_NUKE)) {
            gameController.releaseNuke(appState);
        } else if (InputParser.matches(input,
                model.enums.CommandRegex.CHEAT_REMOVE_COOLDOWN)) {
            gameController.removeCooldown(appState);
        } else if (InputParser.matches(input,
                model.enums.CommandRegex.CHEAT_ADD_PLANT_FOOD)) {
            gameController.addPlantFood(appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.CHEAT_SPAWN_ZOMBIE)) != null) {
            gameController.spawnZombie(m, appState);
        } else if (InputParser.matches(input,
                model.enums.CommandRegex.START_ZOMBIE_WAVES)) {
            gameController.startZombieWaves(appState);
        } else {
            view.printError("Unknown in-game command.");
        }
    }
    private void handleGreenhouseMenu(String input) {
        Matcher m;
        if (InputParser.matches(input, model.enums.CommandRegex.SHOW_GREENHOUSE)) {
            greenhouseController.showGreenhouse(appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.PLANT_POT)) != null) {
            greenhouseController.plantPot(m, appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.COLLECT_POT)) != null) {
            greenhouseController.collectPot(m, appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.GROW_POT)) != null) {
            greenhouseController.growPot(m, appState);
        } else if (InputParser.matches(input, model.enums.CommandRegex.ENTER_SHOP)) {
            appState.setCurrentMenu(MenuType.SHOP);
            view.printSuccess("Entered the Shop!");
        } else {
            view.printError("Unknown greenhouse command.");
        }
    }
    private void handleShopMenu(String input) {
        Matcher m;
        if (InputParser.matches(input, model.enums.CommandRegex.SHOP_LIST)) {
            greenhouseController.showShopList();
        } else if (InputParser.matches(input, model.enums.CommandRegex.SHOP_DAILY)) {
            greenhouseController.showDailyOffer(appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.SHOP_BUY)) != null) {
            greenhouseController.shopBuy(m, appState);
        } else {
            view.printError("Unknown shop command.");
        }
    }
    private void handleTravelLogMenu(String input) {
        Matcher m;
        if ((m = InputParser.match(input,
                model.enums.CommandRegex.TRAVEL_LOG_PAGE)) != null) {
            travelLogController.showPage(m.group(1), appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.MENU_ENTER_CHAPTER)) != null) {
            handleChapterEntry(m.group(1), appState);
        } else if ((m = InputParser.match(input,
                model.enums.CommandRegex.MENU_ENTER)) != null) {
            menuController.handleEnter(m.group(1), appState);
        } else {
            view.printError("Unknown travel-log command. Use 'travel log page <name>' "
                + "or 'menu enter chapter <MINIGAME_N>'.");
        }
    }
    private void handleLeaderboardMenu(String input) {
        Matcher m;
        if ((m = InputParser.match(input,
                model.enums.CommandRegex.LEADERBOARD_SORT)) != null) {
            leaderboardController.sort(m.group(1), m.group(2), appState);
        } else {
            leaderboardController.show(appState);
        }
    }
    private void promptCurrentMenu() {
        System.out.print(
            ConsoleView.CYAN + "["
            + appState.getCurrentMenu().name().toLowerCase()
            + "]> " + ConsoleView.RESET);
    }
}