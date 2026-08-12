package com.pvz2.graphics;

import com.pvz2.controller.GameController;
import com.pvz2.model.AppState;
import com.pvz2.repository.UserRepository;
import com.pvz2.service.*;
import com.pvz2.util.FileUtil;
import com.pvz2.view.ConsoleView;
import com.pvz2.view.MapView;

/**
 * مدیریت مرکزی تمام سرویس‌ها و کنترلرهای فاز ۱.
 *
 * <p>دقیقاً همان wiring که در CLI انجام می‌شد، به‌صورت singleton.
 * یک‌بار در {@code PVZApplication.create()} فراخوانی شود.
 */
public final class ServiceLocator {

    private static ServiceLocator instance;

    // سرویس‌های داخلی (بدون خروجی به console)
    private static final ConsoleView SILENT_VIEW = new ConsoleView();
    private static final MapView     SILENT_MAP  = new MapView();

    // ─── لایه Repository ──────────────────────────────────────────────────
    private UserRepository userRepository;

    // ─── سرویس‌ها ─────────────────────────────────────────────────────────
    private UserService          userService;
    private AuthService          authService;
    private WaveService          waveService;
    private CombatService        combatService;
    private SunService           sunService;
    private GameService          gameService;
    private GreenhouseService    greenhouseService;
    private LevelProgressService levelProgressService;
    private QuestService         questService;
    private ScoredGameService    scoredGameService;
    private NewsService          newsService;

    // ─── کنترلر (برای buildLevel) ─────────────────────────────────────────
    private GameController gameController;

    private ServiceLocator() {}

    public static void init() {
        if (instance != null) return;
        instance = new ServiceLocator();
        instance.wire();
    }

    public static ServiceLocator getInstance() {
        if (instance == null)
            throw new IllegalStateException("ServiceLocator.init() را ابتدا فراخوانی کنید");
        return instance;
    }

    private void wire() {
        FileUtil.initDataDirectories();

        // Repository
        userRepository = new UserRepository();

        // Services
        userService   = new UserService(userRepository);
        authService   = new AuthService(userRepository);
        waveService   = new WaveService(SILENT_VIEW);
        combatService = new CombatService(SILENT_VIEW);
        sunService    = new SunService(SILENT_VIEW);
        gameService   = new GameService(waveService, combatService,
                                         sunService, SILENT_VIEW, SILENT_MAP);

        levelProgressService = new LevelProgressService(userRepository, SILENT_VIEW);
        greenhouseService    = new GreenhouseService(userService, userRepository, SILENT_VIEW);
        questService         = new QuestService(userRepository, userService, SILENT_VIEW);
        scoredGameService    = new ScoredGameService(userRepository, SILENT_VIEW);
        newsService          = new NewsService(userRepository);

        // Cross-wiring (ترتیب مهم است)
        combatService.setLevelProgressService(levelProgressService);
        combatService.setScoredGameService(scoredGameService);
        combatService.setQuestService(questService);
        sunService.setCombatService(combatService);

        // GameController — برای buildLevel
        gameController = new GameController(
                gameService, sunService, SILENT_VIEW, SILENT_MAP);
    }

    // ─── Getters ──────────────────────────────────────────────────────────────
    public UserRepository        getUserRepository()     { return userRepository; }
    public UserService           getUserService()        { return userService; }
    public AuthService           getAuthService()        { return authService; }
    public WaveService           getWaveService()        { return waveService; }
    public CombatService         getCombatService()      { return combatService; }
    public SunService            getSunService()         { return sunService; }
    public GameService           getGameService()        { return gameService; }
    public GreenhouseService     getGreenhouseService()  { return greenhouseService; }
    public LevelProgressService  getLevelProgressService(){ return levelProgressService; }
    public QuestService          getQuestService()       { return questService; }
    public ScoredGameService     getScoredGameService()  { return scoredGameService; }
    public NewsService           getNewsService()        { return newsService; }
    public GameController        getGameController()     { return gameController; }
}
