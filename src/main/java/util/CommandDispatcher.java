package util;

import controller.AuthController;
import controller.CollectionController;
import controller.GameController;
import controller.GreenhouseController;
import controller.LeaderboardController;
import controller.MenuController;
import controller.NewsController;
import controller.PlantSelectController;
import controller.ProfileController;
import controller.SettingsController;
import controller.TravelLogController;
import model.AppState;
import model.enums.CommandRegex;
import model.enums.MenuType;

import java.util.Scanner;
import java.util.regex.Matcher;

/**
 * حلقه اصلی برنامه و توزیع‌کننده دستورات.
 * ورودی کاربر را می‌خواند و بر اساس منوی فعلی
 * به متد handle مناسب هدایت می‌کند.
 */
public class CommandDispatcher {

    private static final String UNKNOWN_CMD = "Invalid command!";

    private final AppState appState;
    private final AuthController authController;
    private final MenuController menuController;
    private final ProfileController profileController;
    private final SettingsController settingsController;
    private final NewsController newsController;
    private final GameController gameController;
    private final PlantSelectController plantSelectController;
    private final CollectionController collectionController;
    private final GreenhouseController greenhouseController;
    private final TravelLogController travelLogController;
    private final LeaderboardController leaderboardController;

    /** آیا حلقه اصلی در حال اجراست */
    private boolean running;

    /** آیا در جریان بازیابی رمز عبور هستیم */
    private boolean inPasswordRecovery;

    /** آیا منتظر رمز جدید پس از تأیید سوال امنیتی هستیم */
    private boolean awaitingNewPassword;

    /** آیا منتظر انتخاب سوال امنیتی پس از ثبت‌نام هستیم */
    private boolean awaitingSecurityQuestion;

    // ---- Constructor ----

    public CommandDispatcher(AppState appState,
                             AuthController authController,
                             MenuController menuController,
                             ProfileController profileController,
                             SettingsController settingsController,
                             NewsController newsController,
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
        this.settingsController = settingsController;
        this.newsController = newsController;
        this.gameController = gameController;
        this.plantSelectController = plantSelectController;
        this.collectionController = collectionController;
        this.greenhouseController = greenhouseController;
        this.travelLogController = travelLogController;
        this.leaderboardController = leaderboardController;
        this.running = false;
        this.inPasswordRecovery = false;
        this.awaitingNewPassword = false;
        this.awaitingSecurityQuestion = false;
    }

    // ---- حلقه اصلی ----

    /**
     * حلقه اصلی برنامه را شروع می‌کند.
     * تا زمانی که running = false نشود ادامه می‌دهد.
     */
    public void run() {
        running = true;
        Scanner scanner = new Scanner(System.in);
        while (running && scanner.hasNextLine()) {
            String input = scanner.nextLine();
            if (InputParser.isEmpty(input)) {
                continue;
            }
            dispatch(InputParser.normalize(input));
        }
        scanner.close();
    }

    /**
     * یک دستور را بر اساس منوی فعلی پردازش می‌کند.
     *
     * @param input رشته ورودی نرمال‌شده
     */
    public void dispatch(String input) {
        // دستورات مشترک همه منوها اول بررسی می‌شوند
        if (handleCommonCommands(input)) {
            return;
        }
        MenuType current = appState.getCurrentMenu();
        switch (current) {
            case REGISTER:
                handleRegisterMenu(input);
                break;
            case LOGIN:
                handleLoginMenu(input);
                break;
            case MAIN:
                handleMainMenu(input);
                break;
            case GAME:
                handleGameMenu(input);
                break;
            case COLLECTION:
                handleCollectionMenu(input);
                break;
            case SETTINGS:
                handleSettingsMenu(input);
                break;
            case NEWS:
                handleNewsMenu(input);
                break;
            case PROFILE:
                handleProfileMenu(input);
                break;
            case GREENHOUSE:
                handleGreenhouseMenu(input);
                break;
            case SHOP:
                handleShopMenu(input);
                break;
            case TRAVEL_LOG:
                handleTravelLogMenu(input);
                break;
            case LEADERBOARD:
                handleLeaderboardMenu(input);
                break;
            case PLANT_SELECT:
                handlePlantSelectMenu(input);
                break;
            case IN_GAME:
                handleInGameMenu(input);
                break;
            default:
                System.out.println(UNKNOWN_CMD);
        }
    }

    // ---- دستورات مشترک ----

    /**
     * دستوراتی که در همه منوها کار می‌کنند را پردازش می‌کند.
     *
     * @param input ورودی
     * @return true اگر دستور توسط این متد مصرف شد
     */
    private boolean handleCommonCommands(String input) {
        if (InputParser.matches(input, CommandRegex.MENU_SHOW_CURRENT)) {
            menuController.showCurrentMenu();
            return true;
        }
        if (InputParser.matches(input, CommandRegex.MENU_EXIT)) {
            menuController.exitMenu();
            // اگر منو ثبت‌نام بود و exit زد، برنامه تمام می‌شود
            if (appState.getCurrentMenu() == MenuType.REGISTER
                    && !appState.isLoggedIn()) {
                running = false;
            }
            return true;
        }
        if (InputParser.matches(input, CommandRegex.MENU_LOGOUT)) {
            menuController.logout();
            inPasswordRecovery = false;
            awaitingNewPassword = false;
            awaitingSecurityQuestion = false;
            return true;
        }
        return false;
    }

    // ---- منوی ثبت‌نام ----

    /**
     * دستورات منوی ثبت‌نام را پردازش می‌کند.
     * شامل: register، pick question، و menu enter login.
     */
    private void handleRegisterMenu(String input) {
        // state: منتظر انتخاب سوال امنیتی بعد از register موفق
        if (awaitingSecurityQuestion) {
            Matcher mPick = InputParser.match(input, CommandRegex.PICK_QUESTION);
            if (mPick != null) {
                authController.pickQuestion(
                        InputParser.getGroup(mPick, 1),
                        InputParser.getGroup(mPick, 2),
                        InputParser.getGroup(mPick, 3)
                );
                awaitingSecurityQuestion = false;
                return;
            }
            System.out.println("Please pick a security question first.");
            System.out.println("Usage: pick question -q <number> -a <answer> -c <confirm>");
            return;
        }

        Matcher mRegister = InputParser.match(input, CommandRegex.REGISTER);
        if (mRegister != null) {
            authController.register(
                    InputParser.getGroup(mRegister, 1),
                    InputParser.getGroup(mRegister, 2),
                    InputParser.getGroup(mRegister, 3),
                    InputParser.getGroup(mRegister, 4),
                    InputParser.getGroup(mRegister, 5),
                    InputParser.getGroup(mRegister, 6)
            );
            // بعد از register موفق، controller منو را به REGISTER نگه می‌دارد
            // تا سوال امنیتی انتخاب شود
            awaitingSecurityQuestion = true;
            return;
        }

        Matcher mEnter = InputParser.match(input, CommandRegex.MENU_ENTER);
        if (mEnter != null) {
            menuController.enterMenu(InputParser.getGroup(mEnter, 1));
            return;
        }

        System.out.println(UNKNOWN_CMD);
    }

    // ---- منوی ورود ----

    /**
     * دستورات منوی ورود را پردازش می‌کند.
     * شامل: login، forget password، answer، و تنظیم رمز جدید.
     */
    private void handleLoginMenu(String input) {
        // state: منتظر رمز جدید پس از تأیید سوال امنیتی
        if (awaitingNewPasswordHandler(input)) {return;};

        // state: منتظر پاسخ سوال امنیتی
        if (inPasswordRecoveryHandler(input)) {return;};

        Matcher mLogin = InputParser.match(input, CommandRegex.LOGIN);
        if (mLogin != null) {
            boolean stayLoggedIn = InputParser.hasStayLoggedIn(input);
            authController.login(
                    InputParser.getGroup(mLogin, 1),
                    InputParser.getGroup(mLogin, 2),
                    stayLoggedIn
            );
            return;
        }

        Matcher mForget = InputParser.match(input, CommandRegex.FORGET_PASSWORD);
        if (mForget != null) {
            authController.forgetPassword(
                    InputParser.getGroup(mForget, 1),
                    InputParser.getGroup(mForget, 2)
            );
            inPasswordRecovery = true;
            return;
        }

        Matcher mEnter = InputParser.match(input, CommandRegex.MENU_ENTER);
        if (mEnter != null) {
            menuController.enterMenu(InputParser.getGroup(mEnter, 1));
            return;
        }

        System.out.println(UNKNOWN_CMD);
    }

    /**
     * وضعیتی که در آن منتظر وارد کردن پسوورد جدید(البته پس  از سوال امنیتی) هستیم را
     * هندل میکند و در شکسته شده بخشی از متد handleLoginMenu است .
     * @param input کامند ورودی
     * @return آیا نیاز به خروج از متد handleLoginMenu داریم یا نه؟
     */
    private boolean awaitingNewPasswordHandler(String input) {
        if (awaitingNewPassword) {
            // از regex CHANGE_PASSWORD برای دریافت رمز جدید استفاده می‌کنیم
            Matcher mPass = InputParser.match(input, CommandRegex.CHANGE_PASSWORD);
            if (mPass != null) {
                authController.setNewPassword(
                        InputParser.getGroup(mPass, 1),
                        InputParser.getGroup(mPass, 2)
                );
                awaitingNewPassword = false;
                inPasswordRecovery = false;
                return true;
            }
            System.out.println("Please enter new password.");
            System.out.println("Usage: menu profile change-password -p <new> -o <old>");
            return true;
        }
        return false;
    }

    /**
     * وضعیتی که در آن منتظر پاسخ سوال امنیتی هستیم .
     * این متد بخشی از متد handleLoginMenu است که شکسته شده .
     * @param input کامند ورودی
     * @return آیا نیاز به خروج از متد handleLoginMenu داریم یا نه؟
     */
    private boolean inPasswordRecoveryHandler(String input) {
        if (inPasswordRecovery) {
            Matcher mAnswer = InputParser.match(input, CommandRegex.ANSWER_SECURITY);
            if (mAnswer != null) {
                authController.answerSecurityQuestion(
                        InputParser.getGroup(mAnswer, 1)
                );
                // اگر پاسخ درست بود، controller این فلگ را true می‌کند
                awaitingNewPassword = true;
                inPasswordRecovery = false;
                return true;
            }
            System.out.println("Please answer the security question.");
            System.out.println("Usage: answer -a <your_answer>");
            return true;
        }
        return false;
    }

    // ---- منوی اصلی ----

    /**
     * دستورات منوی اصلی را پردازش می‌کند.
     * شامل: ورود به زیرمنوها.
     */
    private void handleMainMenu(String input) {
        Matcher mEnterChapter = InputParser.match(input, CommandRegex.MENU_ENTER_CHAPTER);
        if (mEnterChapter != null) {
            menuController.enterMenu("chapter");
            return;
        }

        Matcher mEnter = InputParser.match(input, CommandRegex.MENU_ENTER);
        if (mEnter != null) {
            menuController.enterMenu(InputParser.getGroup(mEnter, 1));
            return;
        }

        if (InputParser.matches(input, CommandRegex.MENU_GREENHOUSE)) {
            menuController.enterMenu("greenhouse");
            return;
        }

        if (InputParser.matches(input, CommandRegex.MENU_TRAVEL_LOG)) {
            menuController.enterMenu("travel-log");
            return;
        }

        if (InputParser.matches(input, CommandRegex.MENU_LEADERBOARD)) {
            menuController.enterMenu("leaderboard");
            return;
        }

        System.out.println(UNKNOWN_CMD);
    }

    // ---- منوی بازی (انتخاب chapter) ----

    /**
     * دستورات منوی بازی (نقشه chapter) را پردازش می‌کند.
     */
    private void handleGameMenu(String input) {
        Matcher mChapter = InputParser.match(input, CommandRegex.MENU_ENTER_CHAPTER);
        if (mChapter != null) {
            menuController.enterMenu("chapter-" + InputParser.getGroup(mChapter, 1));
            return;
        }

        if (InputParser.matches(input, CommandRegex.MENU_GREENHOUSE)) {
            menuController.enterMenu("greenhouse");
            return;
        }

        if (InputParser.matches(input, CommandRegex.MENU_TRAVEL_LOG)) {
            menuController.enterMenu("travel-log");
            return;
        }

        if (InputParser.matches(input, CommandRegex.MENU_LEADERBOARD)) {
            menuController.enterMenu("leaderboard");
            return;
        }

        if (InputParser.matches(input, CommandRegex.MENU_COIN_WALLET)) {
            menuController.showCurrentMenu();
            return;
        }

        if (InputParser.matches(input, CommandRegex.MENU_GEM_WALLET)) {
            menuController.showCurrentMenu();
            return;
        }

        Matcher mCheatCurrency = InputParser.match(input, CommandRegex.CHEAT_ADD_CURRENCY);
        if (mCheatCurrency != null) {
            gameController.cheatAddSuns(0); // controller داخلی تشخیص می‌دهد
            return;
        }

        Matcher mEnter = InputParser.match(input, CommandRegex.MENU_ENTER);
        if (mEnter != null) {
            menuController.enterMenu(InputParser.getGroup(mEnter, 1));
            return;
        }

        System.out.println(UNKNOWN_CMD);
    }

    // ---- منوی کلکسیون ----

    /**
     * دستورات منوی کلکسیون را پردازش می‌کند.
     */
    private void handleCollectionMenu(String input) {
        if (InputParser.matches(input, CommandRegex.COLLECTION_SHOW_PLANTS)) {
            collectionController.showUnlockedPlants();
            return;
        }

        if (InputParser.matches(input, CommandRegex.COLLECTION_SHOW_ALL_PLANTS)) {
            collectionController.showAllPlants();
            return;
        }

        if (InputParser.matches(input, CommandRegex.COLLECTION_SHOW_ZOMBIES)) {
            collectionController.showSeenZombies();
            return;
        }

        if (InputParser.matches(input, CommandRegex.COLLECTION_SHOW_ALL_ZOMBIES)) {
            collectionController.showAllZombies();
            return;
        }

        Matcher mShowPlant = InputParser.match(input, CommandRegex.COLLECTION_SHOW_PLANT);
        if (mShowPlant != null) {
            collectionController.showPlantDetails(InputParser.getGroup(mShowPlant, 1));
            return;
        }

        Matcher mShowZombie = InputParser.match(input, CommandRegex.COLLECTION_SHOW_ZOMBIE);
        if (mShowZombie != null) {
            collectionController.showZombieDetails(InputParser.getGroup(mShowZombie, 1));
            return;
        }

        Matcher mUpgrade = InputParser.match(input, CommandRegex.COLLECTION_UPGRADE_PLANT);
        if (mUpgrade != null) {
            collectionController.upgradePlant(InputParser.getGroup(mUpgrade, 1));
            return;
        }

        Matcher mPurchase = InputParser.match(input, CommandRegex.COLLECTION_PURCHASE_PLANT);
        if (mPurchase != null) {
            collectionController.purchasePlant(InputParser.getGroup(mPurchase, 1));
            return;
        }

        System.out.println(UNKNOWN_CMD);
    }

    // ---- منوی تنظیمات ----

    /**
     * دستورات منوی تنظیمات را پردازش می‌کند.
     */
    private void handleSettingsMenu(String input) {
        Matcher mDiff = InputParser.match(input, CommandRegex.CHANGE_DIFFICULTY);
        if (mDiff != null) {
            settingsController.changeDifficulty(InputParser.getInt(mDiff, 1));
            return;
        }

        System.out.println(UNKNOWN_CMD);
    }

    // ---- منوی اخبار ----

    /**
     * دستورات منوی اخبار را پردازش می‌کند.
     */
    private void handleNewsMenu(String input) {
        if (InputParser.matches(input, CommandRegex.NEWS_SHOW_UNREAD)) {
            newsController.showUnread();
            return;
        }

        if (InputParser.matches(input, CommandRegex.NEWS_SHOW_ALL)) {
            newsController.showAll();
            return;
        }

        System.out.println(UNKNOWN_CMD);
    }

    // ---- منوی پروفایل ----

    /**
     * دستورات منوی پروفایل را پردازش می‌کند.
     */
    private void handleProfileMenu(String input) {
        if (InputParser.matches(input, CommandRegex.SHOW_PROFILE_INFO)) {
            profileController.showInfo();
            return;
        }

        Matcher mUsername = InputParser.match(input, CommandRegex.CHANGE_USERNAME);
        if (mUsername != null) {
            profileController.changeUsername(InputParser.getGroup(mUsername, 1));
            return;
        }

        Matcher mNickname = InputParser.match(input, CommandRegex.CHANGE_NICKNAME);
        if (mNickname != null) {
            profileController.changeNickname(InputParser.getGroup(mNickname, 1));
            return;
        }

        Matcher mEmail = InputParser.match(input, CommandRegex.CHANGE_EMAIL);
        if (mEmail != null) {
            profileController.changeEmail(InputParser.getGroup(mEmail, 1));
            return;
        }

        Matcher mPassword = InputParser.match(input, CommandRegex.CHANGE_PASSWORD);
        if (mPassword != null) {
            profileController.changePassword(
                    InputParser.getGroup(mPassword, 1),
                    InputParser.getGroup(mPassword, 2)
            );
            return;
        }

        System.out.println(UNKNOWN_CMD);
    }

    // ---- منوی گلخانه ----

    /**
     * دستورات منوی گلخانه را پردازش می‌کند.
     */
    private void handleGreenhouseMenu(String input) {
        if (InputParser.matches(input, CommandRegex.SHOW_GREENHOUSE)) {
            greenhouseController.showGreenhouse();
            return;
        }

        Matcher mPlantPot = InputParser.match(input, CommandRegex.PLANT_POT);
        if (mPlantPot != null) {
            greenhouseController.plantPot(
                    InputParser.getInt(mPlantPot, 1),
                    InputParser.getInt(mPlantPot, 2)
            );
            return;
        }

        Matcher mCollect = InputParser.match(input, CommandRegex.COLLECT_POT);
        if (mCollect != null) {
            greenhouseController.collectPot(
                    InputParser.getInt(mCollect, 1),
                    InputParser.getInt(mCollect, 2)
            );
            return;
        }

        Matcher mGrow = InputParser.match(input, CommandRegex.GROW_POT);
        if (mGrow != null) {
            greenhouseController.accelerateGrowth(
                    InputParser.getInt(mGrow, 1),
                    InputParser.getInt(mGrow, 2)
            );
            return;
        }

        if (InputParser.matches(input, CommandRegex.ENTER_SHOP)) {
            greenhouseController.enterShop();
            appState.setCurrentMenu(MenuType.SHOP);
            return;
        }

        System.out.println(UNKNOWN_CMD);
    }

    // ---- منوی فروشگاه ----

    /**
     * دستورات منوی فروشگاه را پردازش می‌کند.
     */
    private void handleShopMenu(String input) {
        if (InputParser.matches(input, CommandRegex.SHOP_LIST)) {
            greenhouseController.showShopList();
            return;
        }

        if (InputParser.matches(input, CommandRegex.SHOP_DAILY)) {
            greenhouseController.showDailyOffer();
            return;
        }

        Matcher mBuy = InputParser.match(input, CommandRegex.SHOP_BUY);
        if (mBuy != null) {
            // گروه سوم (نوع گیاه) اختیاری است و ممکن است null باشد
            greenhouseController.buyShopItem(
                    InputParser.getGroup(mBuy, 1),
                    InputParser.getInt(mBuy, 2),
                    InputParser.getGroup(mBuy, 3)
            );
            return;
        }

        System.out.println(UNKNOWN_CMD);
    }

    // ---- منوی Travel Log ----

    /**
     * دستورات منوی Travel Log را پردازش می‌کند.
     */
    private void handleTravelLogMenu(String input) {
        Matcher mPage = InputParser.match(input, CommandRegex.TRAVEL_LOG_PAGE);
        if (mPage != null) {
            travelLogController.showPage(InputParser.getGroup(mPage, 1));
            return;
        }

        System.out.println(UNKNOWN_CMD);
    }

    // ---- منوی لیدربورد ----

    /**
     * دستورات منوی لیدربورد را پردازش می‌کند.
     */
    private void handleLeaderboardMenu(String input) {
        Matcher mSort = InputParser.match(input, CommandRegex.LEADERBOARD_SORT);
        if (mSort != null) {
            String sortBy = InputParser.getGroup(mSort, 1);
            String direction = InputParser.getGroup(mSort, 2);
            leaderboardController.showLeaderboard(
                    sortBy,
                    "asc".equalsIgnoreCase(direction)
            );
            return;
        }

        // نمایش پیش‌فرض بدون مرتب‌سازی
        leaderboardController.showLeaderboard();
    }

    // ---- منوی انتخاب گیاه ----

    /**
     * دستورات صفحه انتخاب گیاه قبل از شروع مرحله را پردازش می‌کند.
     */
    private void handlePlantSelectMenu(String input) {
        if (InputParser.matches(input, CommandRegex.SHOW_ALL_PLANTS)) {
            plantSelectController.showAllPlants();
            return;
        }

        if (InputParser.matches(input, CommandRegex.SHOW_AVAILABLE_PLANTS)) {
            plantSelectController.showAvailablePlants();
            return;
        }

        Matcher mAdd = InputParser.match(input, CommandRegex.ADD_PLANT_SELECT);
        if (mAdd != null) {
            plantSelectController.addPlant(InputParser.getGroup(mAdd, 1));
            return;
        }

        Matcher mRemove = InputParser.match(input, CommandRegex.REMOVE_PLANT_SELECT);
        if (mRemove != null) {
            plantSelectController.removePlant(InputParser.getGroup(mRemove, 1));
            return;
        }

        Matcher mBoost = InputParser.match(input, CommandRegex.BOOST_PLANT_SELECT);
        if (mBoost != null) {
            plantSelectController.boostPlant(InputParser.getGroup(mBoost, 1));
            return;
        }

        if (InputParser.matches(input, CommandRegex.START_GAME)) {
            plantSelectController.startGame();
            return;
        }

        System.out.println(UNKNOWN_CMD);
    }

    // ---- منوی درون بازی ----

    /**
     * دستورات حین بازی را پردازش می‌کند.
     * این متد طولانی‌ترین handle است چون دستورات بازی زیاد هستند.
     */
    private void handleInGameMenu(String input) {
        // ---- پیشروی زمان ----
        Matcher mAdvance = InputParser.match(input, CommandRegex.ADVANCE_TIME);
        if (mAdvance != null) {
            gameController.advanceTime(InputParser.getInt(mAdvance, 1));
            return;
        }

        // ---- کاشت و برداشت گیاه ----
        if (plantingAndHarvestingHandler(input)) {return;}

        // ---- خورشید ----
        Matcher mCollectSun = InputParser.match(input, CommandRegex.COLLECT_SUN);
        if (mCollectSun != null) {
            gameController.collectSun(
                    InputParser.getInt(mCollectSun, 1),
                    InputParser.getInt(mCollectSun, 2)
            );
            return;
        }

        if (InputParser.matches(input, CommandRegex.SHOW_SUN_AMOUNT)) {
            gameController.showSunAmount();
            return;
        }

        // ---- غذای گیاه ----
        Matcher mFeed = InputParser.match(input, CommandRegex.FEED_PLANT);
        if (mFeed != null) {
            gameController.feedPlant(
                    InputParser.getInt(mFeed, 1),
                    InputParser.getInt(mFeed, 2)
            );
            return;
        }

        // ---- نمایش اطلاعات ----
        if (showInfoHandler(input)) {return;};

        // ---- چیت‌کدها ----
        if (cheatCodeHandler(input)) {return;};

        // ---- مخصوص Plant What You Get ----
        if (InputParser.matches(input, CommandRegex.START_ZOMBIE_WAVES)) {
            gameController.startZombieWaves();
            return;
        }

        System.out.println(UNKNOWN_CMD);
    }

    /**
     *
     * این متد شکسته شده متد handleInGameMenu است .
     * @param input کامند ورودی
     * @return آیا نیاز به خروج از متد handleInGameMenu داریم یا نه؟
     */
    private boolean plantingAndHarvestingHandler(String input) {
        Matcher mPlant = InputParser.match(input, CommandRegex.PLANT_PLANT);
        if (mPlant != null) {
            gameController.plantPlant(
                    InputParser.getGroup(mPlant, 1),
                    InputParser.getInt(mPlant, 2),
                    InputParser.getInt(mPlant, 3)
            );
            return true;
        }

        Matcher mPluck = InputParser.match(input, CommandRegex.PLUCK_PLANT);
        if (mPluck != null) {
            gameController.pluckPlant(
                    InputParser.getInt(mPluck, 1),
                    InputParser.getInt(mPluck, 2)
            );
            return true;
        }
        return false;
    }

    /**
     * این متد مربوط به شرط های بخش نمایش اطلاعات است .
     * این متد شکسته شده متد handleInGameMenu است .
     * @param input کامند ورودی
     * @return آیا نیاز به خروج از متد handleInGameMenu داریم یا نه؟
     */
    private boolean showInfoHandler(String input) {
        if (InputParser.matches(input, CommandRegex.SHOW_MAP)) {
            gameController.showMap();
            return true;
        }

        if (InputParser.matches(input, CommandRegex.SHOW_PLANTS_STATUS)) {
            gameController.showPlantsStatus();
            return true;
        }

        Matcher mTile = InputParser.match(input, CommandRegex.SHOW_TILE_STATUS);
        if (mTile != null) {
            gameController.showTileStatus(
                    InputParser.getInt(mTile, 1),
                    InputParser.getInt(mTile, 2)
            );
            return true;
        }

        if (InputParser.matches(input, CommandRegex.ZOMBIES_INFO)) {
            gameController.showZombiesInfo();
            return true;
        }
        return false;
    }

    /**
     * این متد مربوط به شرط های چیت کد های GameMenu است .
     * این متد شکسته شده متد handleInGameMenu است .
     * @param input کامند ورودی
     * @return آیا نیاز به خروج از متد handleInGameMenu داریم یا نه؟
     */
    private boolean cheatCodeHandler(String input) {
        Matcher mAddSuns = InputParser.match(input, CommandRegex.CHEAT_ADD_SUNS);
        if (mAddSuns != null) {
            gameController.cheatAddSuns(InputParser.getInt(mAddSuns, 1));
            return true;
        }

        if (InputParser.matches(input, CommandRegex.CHEAT_RELEASE_NUKE)) {
            gameController.releaseNuke();
            return true;
        }

        if (InputParser.matches(input, CommandRegex.CHEAT_REMOVE_COOLDOWN)) {
            gameController.removeCooldown();
            return true;
        }

        if (InputParser.matches(input, CommandRegex.CHEAT_ADD_PLANT_FOOD)) {
            gameController.addPlantFood();
            return true;
        }

        Matcher mSpawn = InputParser.match(input, CommandRegex.CHEAT_SPAWN_ZOMBIE);
        if (mSpawn != null) {
            gameController.spawnZombie(
                    InputParser.getGroup(mSpawn, 1),
                    InputParser.getInt(mSpawn, 2),
                    InputParser.getInt(mSpawn, 3)
            );
            return true;
        }

        Matcher mCurrency = InputParser.match(input, CommandRegex.CHEAT_ADD_CURRENCY);
        if (mCurrency != null) {
            // رشته "coin" یا "diamond" به gameController داده می‌شود
            // gameController خودش تشخیص می‌دهد
            String currencyType = InputParser.getGroup(mCurrency, 2);
            int amount = InputParser.getInt(mCurrency, 1);
            if ("coin".equalsIgnoreCase(currencyType)) {
                gameController.cheatAddSuns(amount); // placeholder — فاز 1 پیاده‌سازی می‌شود
            } else {
                gameController.cheatAddSuns(amount); // placeholder
            }
            return true;
        }
        return false;
    }

    // ---- متدهای کمکی public ----

    /**
     * از بیرون (مثلاً AuthController) می‌توان حلقه را متوقف کرد.
     */
    public void stop() {
        running = false;
    }

    /**
     * وضعیت state بازیابی رمز را reset می‌کند.
     * AuthController بعد از بازیابی موفق این را صدا می‌زند.
     */
    public void resetRecoveryState() {
        inPasswordRecovery = false;
        awaitingNewPassword = false;
    }

    /**
     * وضعیت انتظار سوال امنیتی را تنظیم می‌کند.
     * AuthController بعد از register موفق این را صدا می‌زند.
     *
     * @param waiting true اگر منتظر pick question باشیم
     */
    public void setAwaitingSecurityQuestion(boolean waiting) {
        awaitingSecurityQuestion = waiting;
    }
}
