package controller;

import model.AppState;
import model.GameSession;
import model.Level;
import model.User;
import model.enums.MenuType;
import model.enums.PlantType;
import service.GameService;
import service.UserService;
import view.ConsoleView;

import java.util.ArrayList;
import java.util.List;

/**
 * کنترلر صفحه انتخاب گیاه قبل از شروع مرحله.
 * دستورات add plant، remove plant، boost plant و start game.
 */
public class PlantSelectController {

    private final GameService gameService;
    private final UserService userService;
    private final AppState appState;
    private final ConsoleView view;

    /** مرحله‌ای که قرار است بازی شود */
    private Level currentLevel;

    /** گیاهان انتخاب‌شده تا الان */
    private List<String> selectedPlants;

    /** گیاهان boost شده */
    private List<String> boostedPlants;

    public PlantSelectController(GameService gameService,UserService userService, AppState appState,
                                 ConsoleView view) {
        this.gameService = gameService;
        this.userService = userService;
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
    public void removePlant(String typeName) {
        User currentUser = appState.getCurrentUser();
        PlantType type = parsePlantType(typeName);
        if (type == null){
            view.printError("Invalid plant type.");
            return;
        }
        if (!selectedPlants.contains(type.name())){
            view.printError("Plnat " + typeName + " is not in your selection.");
            return;
        }
        selectedPlants.remove(type.name());
        boostedPlants.remove(type.name());
        view.printSuccess("Plnat" + typeName + " removed.");
    }

    /**
     * دستور "boost plant -t <type>" را پردازش می‌کند.
     * 2 الماس خرج می‌کند.
     * @param typeName نام گیاه
     */
    public void boostPlant(String typeName) {
        User currentUser = appState.getCurrentUser();
        PlantType type = parsePlantType(typeName);

        if (type == null){
            view.printError("Invalid plant type.");
            return;
        }
        if (!selectedPlants.contains(type.name())){
            view.printError("Plant " + typeName + " is not in your selection. Add it first.");
            return;
        }
        if (boostedPlants.contains(type.name())){
            view.printError("Plant " + typeName + " is already boosted.");
            return;
        }
        if (currentUser.getGems() < 2){
            view.printError("Not enough gems.");
            return;
        }
        try {
            userService.deductGems(currentUser, 2);
            boostedPlants.add(type.name());
            view.printSuccess("Plant" + typeName + " boosted.");
        }
        catch (RuntimeException e){
            view.printError(e.getMessage());
        }
    }

    /** دستور "start game" را پردازش می‌کند */
    public void startGame() {
        if (selectedPlants.isEmpty()) {
            view.printError("You must select at least one plant before starting.");
            return;
        }

        if (currentLevel == null) {
            view.printError("No level selected.");
            return;
        }

        List<PlantType> plantTypes = new ArrayList<>();
        for (String name : selectedPlants) {
            PlantType t = parsePlantType(name);
            if (t != null) plantTypes.add(t);
        }

        List<PlantType> boostedTypes = new ArrayList<>();
        for (String name : boostedPlants) {
            PlantType t = parsePlantType(name);
            if (t != null) boostedTypes.add(t);
        }
        GameSession session = gameService.createSession(
                currentLevel,
                plantTypes,
                appState.getCurrentUser()
        );
        if (session == null) {
            view.printError("Failed to start game.");
            return;
        }
        session.setBoostedPlants(boostedTypes);
        appState.setCurrentSession(session);
        appState.setCurrentMenu(MenuType.IN_GAME);
        view.printSuccess("Game started! Good luck!");
    }

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
