package controller;

import model.AppState;
import service.QuestService;
import view.ConsoleView;

/**
 * کنترلر منوی Travel Log (کوئست‌ها و مینی‌گیم‌ها).
 */
public class TravelLogController {

    private final QuestService questService;
    private final AppState appState;
    private final ConsoleView view;

    public TravelLogController(QuestService questService, AppState appState,
                               ConsoleView view) {
        this.questService = questService;
        this.appState = appState;
        this.view = view;
    }

    /**
     * دستور "travel log page <page_name>" را پردازش می‌کند.
     * @param pageName نام صفحه (adventure, special, minigame, ...)
     */
    public void showPage(String pageName) { }

    /**
     * ورود به یک مینی‌گیم از Travel Log.
     * @param minigameName نام مینی‌گیم
     * @param levelNumber سطح (1-3)
     */
    public void enterMinigame(String minigameName, int levelNumber) { }
}
