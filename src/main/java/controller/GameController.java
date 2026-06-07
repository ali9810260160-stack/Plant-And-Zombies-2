package controller;

import model.AppState;
import service.GameService;
import view.ConsoleView;
import view.MapView;

/**
 * کنترلر اصلی حین بازی (in-game).
 * تمام دستورات داخل مرحله بازی اینجا پردازش می‌شوند.
 */
public class GameController {

    private final GameService gameService;
    private final AppState appState;
    private final ConsoleView view;
    private final MapView mapView;

    public GameController(GameService gameService, AppState appState,
                          ConsoleView view, MapView mapView) {
        this.gameService = gameService;
        this.appState = appState;
        this.view = view;
        this.mapView = mapView;
    }

    /**
     * دستور "advance time -t <n> ticks" را پردازش می‌کند.
     * @param ticks تعداد تیک
     */
    public void advanceTime(int ticks) { }

    /**
     * دستور "plant plant -t <type> -l (x,y)" را پردازش می‌کند.
     * @param typeName نام گیاه
     * @param x ستون
     * @param y ردیف
     */
    public void plantPlant(String typeName, int x, int y) { }

    /**
     * دستور "pluck plant -l (x,y)" را پردازش می‌کند.
     * @param x ستون
     * @param y ردیف
     */
    public void pluckPlant(int x, int y) { }

    /**
     * دستور "collect sun -l (x,y)" را پردازش می‌کند.
     * @param x ستون
     * @param y ردیف
     */
    public void collectSun(int x, int y) { }

    /**
     * دستور "feed plant -l (x,y)" را پردازش می‌کند.
     * @param x ستون
     * @param y ردیف
     */
    public void feedPlant(int x, int y) { }

    /** دستور "show map" را پردازش می‌کند */
    public void showMap() { }

    /** دستور "show plants status" را پردازش می‌کند */
    public void showPlantsStatus() { }

    /**
     * دستور "show tile status -l (x,y)" را پردازش می‌کند.
     * @param x ستون
     * @param y ردیف
     */
    public void showTileStatus(int x, int y) { }

    /** دستور "show sun amount" را پردازش می‌کند */
    public void showSunAmount() { }

    /** دستور "zombies info" را پردازش می‌کند */
    public void showZombiesInfo() { }

    // ---- Cheat Commands ----

    /** دستور "cheat add -n <n> suns" را پردازش می‌کند */
    public void cheatAddSuns(int count) { }

    /** دستور "release the nuke" را پردازش می‌کند */
    public void releaseNuke() { }

    /** دستور "cheat remove-cooldown" را پردازش می‌کند */
    public void removeCooldown() { }

    /** دستور "cheat add-plant-food" را پردازش می‌کند */
    public void addPlantFood() { }

    /**
     * دستور "cheat spawn-zombie -t <type> -l <x,y>" را پردازش می‌کند.
     * @param typeName نام زامبی
     * @param x ستون
     * @param y ردیف
     */
    public void spawnZombie(String typeName, int x, int y) { }

    /**
     * دستور "start zombie waves" را پردازش می‌کند (مخصوص Plant What You Get).
     */
    public void startZombieWaves() { }
}
