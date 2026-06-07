package controller;

import model.AppState;
import model.Level;
import service.GameService;
import view.ConsoleView;

import java.util.ArrayList;
import java.util.List;

/**
 * کنترلر صفحه انتخاب گیاه قبل از شروع مرحله.
 * دستورات add plant، remove plant، boost plant و start game.
 */
public class PlantSelectController {

    private final GameService gameService;
    private final AppState appState;
    private final ConsoleView view;

    /** مرحله‌ای که قرار است بازی شود */
    private Level currentLevel;

    /** گیاهان انتخاب‌شده تا الان */
    private List<String> selectedPlants;

    /** گیاهان boost شده */
    private List<String> boostedPlants;

    public PlantSelectController(GameService gameService, AppState appState,
                                 ConsoleView view) {
        this.gameService = gameService;
        this.appState = appState;
        this.view = view;
        this.selectedPlants = new ArrayList<String>();
        this.boostedPlants = new ArrayList<String>();
    }

    /** دستور "show all plants" را پردازش می‌کند */
    public void showAllPlants() { }

    /** دستور "show available plants" را پردازش می‌کند */
    public void showAvailablePlants() { }

    /**
     * دستور "add plant -t <type>" را پردازش می‌کند.
     * @param typeName نام گیاه
     */
    public void addPlant(String typeName) { }

    /**
     * دستور "remove plant -t <type>" را پردازش می‌کند.
     * @param typeName نام گیاه
     */
    public void removePlant(String typeName) { }

    /**
     * دستور "boost plant -t <type>" را پردازش می‌کند.
     * 2 الماس خرج می‌کند.
     * @param typeName نام گیاه
     */
    public void boostPlant(String typeName) { }

    /** دستور "start game" را پردازش می‌کند */
    public void startGame() { }

    /** مرحله جاری را تنظیم می‌کند */
    public void setCurrentLevel(Level level) { this.currentLevel = level; }
}
