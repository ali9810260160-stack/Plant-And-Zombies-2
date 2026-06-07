package controller;

import model.AppState;
import service.CollectionService;
import view.ConsoleView;

/**
 * کنترلر منوی کلکسیون.
 * نمایش گیاهان و زامبی‌ها، ارتقا و خرید گیاه.
 */
public class CollectionController {

    private final CollectionService collectionService;
    private final AppState appState;
    private final ConsoleView view;

    public CollectionController(CollectionService collectionService,
                                AppState appState, ConsoleView view) {
        this.collectionService = collectionService;
        this.appState = appState;
        this.view = view;
    }

    /** دستور "menu collection show-plants" */
    public void showUnlockedPlants() { }

    /** دستور "menu collection show-all-plants" */
    public void showAllPlants() { }

    /** دستور "menu collection show-zombies" */
    public void showSeenZombies() { }

    /** دستور "menu collection show-all-zombies" */
    public void showAllZombies() { }

    /** دستور "menu collection show-plant -p <name>" */
    public void showPlantDetails(String plantName) { }

    /** دستور "menu collection show-zombie -z <name>" */
    public void showZombieDetails(String zombieName) { }

    /** دستور "menu collection upgrade-plant -p <name>" */
    public void upgradePlant(String plantName) { }

    /** دستور "menu collection purchase-plant -p <name>" */
    public void purchasePlant(String plantName) { }
}
