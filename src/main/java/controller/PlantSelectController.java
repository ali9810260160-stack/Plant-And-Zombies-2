package controller;

import model.AppState;
import model.Level;
import model.User;
import model.enums.PlantType;
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
    public void showAllPlants() {
        view.printInfo("All plants in game : ");
        for (PlantType type : PlantType.values()){
            view.printInfo("---\"" + type.name() + "\"---");
        }
    }

    /** دستور "show available plants" را پردازش می‌کند */
    public void showAvailablePlants() {
        User currentUser = appState.getCurrentUser();
        List<String> unlockedPlants = currentUser.getUnlockedPlants();
        if (unlockedPlants == null || unlockedPlants.isEmpty()){
            view.printInfo("You have no unclocked plants");
            return;
        }
        List<String> lockedInLevel = new ArrayList<>();
        if (currentLevel != null && currentLevel.getLockedPlants() != null) {
            for (PlantType p : currentLevel.getLockedPlants()) {
                lockedInLevel.add(p.name());
            }
        }
        view.printInfo("Available plants:");
        for (String plant : unlockedPlants) {
            if (!lockedInLevel.contains(plant.toUpperCase())) {
                view.printInfo("---\"" + plant + "\"---");
            }
        }
    }

    /**
     * دستور "add plant -t <type>" را پردازش می‌کند.
     * @param typeName نام گیاه
     */
    public void addPlant(String typeName) {
        User currentUser = appState.getCurrentUser();
        PlantType type = parsePlantType(typeName);
        if (type == null){
            view.printError("Invalid plant type");
            return;
        }
        if (!isUnlocked(currentUser, type)){
            view.printError("Plant " + typeName + " is not unlocked.");
            return;
        }
        if (isLockedInLevel(type)){
            view.printError("Plant " + typeName + " is locked in this level.");
            return;
        }
        if (selectedPlants.contains(type.name())){
            view.printError("Plant " + typeName + " is already selected.");
            return;
        }
        int slots = currentLevel != null ? currentLevel.getPlantSlots() : 8;
        if (selectedPlants.size() >= slots){
            view.printError("Plant slots are full. max: (" + slots + ")");
            return;
        }
        selectedPlants.add(type.name());
        view.printSuccess("Plant " + typeName + " added");
    }

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
    public void setCurrentLevel(Level level) {
        this.currentLevel = level;
        this.selectedPlants = new ArrayList<>();
        this.boostedPlants = new ArrayList<>();
    }

    /** نام گیاه را به PlantType تبدیل می کند. */
    private PlantType parsePlantType(String name){
        if (name == null || name.trim().isEmpty()) return null;
        try {
            return PlantType.valueOf(name.toUpperCase());
        }
        catch (IllegalArgumentException e){
            return null;
        }
    }

    /** چک می کند که کاربر این گیاه را آنلاک کرده یا نه. */
    private boolean isUnlocked(User user, PlantType type){
        if (user == null) return false;
        List<String> unlocked = user.getUnlockedPlants();
        if (unlocked == null) return false;
        return unlocked.contains(type.name()) || unlocked.contains(type.name().toLowerCase());
    }

    /** چک می کند که گیاه در این مرحله قفل است یا خیر*/

    private boolean isLockedInLevel(PlantType type){
        if (currentLevel == null) return false;
        List<PlantType> locked = currentLevel.getLockedPlants();
        if (locked == null) return false;
        return locked.contains(type);
    }
}
