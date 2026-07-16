package controller;

import model.AppState;
import model.User;
import model.enums.PlantType;
import model.enums.ZombieType;
import model.plants.PlantDataRegistry;
import model.plants.PlantStats;
import model.zombies.ZombieDataRegistry;
import model.zombies.ZombieStats;
import service.UserService;
import view.ConsoleView;

/**
 * کنترلر کلکسیون گیاهان و زامبی‌ها.
 */
public class CollectionController {

    private final UserService userService;
    private final ConsoleView view;

    public CollectionController(UserService userService, ConsoleView view) {
        this.userService = userService;
        this.view = view;
    }

    public void showPlants(AppState appState) {
        User user = appState.getCurrentUser();
        view.printHeader("🌱 Your Unlocked Plants");
        if (user.getUnlockedPlants() == null
                || user.getUnlockedPlants().isEmpty()) {
            view.printInfo("You have no unlocked plants yet.");
            return;
        }
        int i = 1;
        for (String plantName : user.getUnlockedPlants()) {
            PlantStats stats = getPlantStatsForName(plantName);
            String desc = stats != null ? stats.getDescription() : "";
            System.out.println(ConsoleView.GREEN + "  " + i++ + ". "
                    + ConsoleView.BOLD + plantName + ConsoleView.RESET
                    + "  — " + desc);
        }
    }

    public void showAllPlants() {
        view.printHeader("🌿 All Plants in the Game");
        int i = 1;
        for (PlantType type : PlantType.values()) {
            PlantStats stats = PlantDataRegistry.getInstance().getStats(type);
            if (stats == null) {
                continue;
            }
            System.out.printf(ConsoleView.GREEN + "  %2d. %-30s"
                            + ConsoleView.RESET
                            + " Cost: %-4d HP: %-5d  %s%n",
                    i++, type.name(), stats.getSunCost(),
                    stats.getBaseHp(), stats.getDescription());
        }
    }

    public void showZombies(AppState appState) {
        User user = appState.getCurrentUser();
        view.printHeader("🧟 Seen Zombies");
        if (user.getSeenZombies() == null || user.getSeenZombies().isEmpty()) {
            view.printInfo("You haven't seen any zombies yet.");
            return;
        }
        int i = 1;
        for (String zombieName : user.getSeenZombies()) {
            ZombieStats stats = getZombieStatsForName(zombieName);
            String desc = stats != null ? stats.getDescription() : "";
            System.out.println(ConsoleView.RED + "  " + i++ + ". "
                    + ConsoleView.BOLD + zombieName + ConsoleView.RESET
                    + "  — " + desc);
        }
    }

    public void showAllZombies() {
        view.printHeader("🧟 All Zombies in the Game");
        int i = 1;
        for (ZombieType type : ZombieType.values()) {
            ZombieStats stats = ZombieDataRegistry.getInstance().getStats(type);
            if (stats == null) {
                continue;
            }
            System.out.printf(ConsoleView.RED + "  %2d. %-30s"
                            + ConsoleView.RESET
                            + " HP: %-5d Cost: %-4d  %s%n",
                    i++, type.name(), stats.getHp(),
                    stats.getWaveCost(), stats.getDescription());
        }
    }

    public void showPlant(String plantName) {
        PlantStats stats = getPlantStatsForName(plantName.toUpperCase());
        if (stats == null) {
            view.printError("Plant not found: " + plantName);
            return;
        }
        view.printHeader("🌱 " + stats.getType().name());
        System.out.println(ConsoleView.CYAN + "  Type:        " + ConsoleView.RESET
                + stats.getType().name());
        System.out.println(ConsoleView.CYAN + "  Family:      " + ConsoleView.RESET
                + stats.getFamily());
        System.out.println(ConsoleView.CYAN + "  Category:    " + ConsoleView.RESET
                + stats.getCategory());
        System.out.println(ConsoleView.YELLOW + "  Sun Cost:    " + ConsoleView.RESET
                + stats.getSunCost());
        System.out.println(ConsoleView.GREEN + "  HP:          " + ConsoleView.RESET
                + stats.getBaseHp());
        System.out.println(ConsoleView.RED + "  Damage:      " + ConsoleView.RESET
                + stats.getBaseDamage());
        System.out.println(ConsoleView.WHITE + "  Atk Speed:   " + ConsoleView.RESET
                + stats.getAttackSpeed());
        System.out.println(ConsoleView.WHITE + "  Range:       " + ConsoleView.RESET
                + stats.getRange());
        System.out.println(ConsoleView.WHITE + "  Recharge:    " + ConsoleView.RESET
                + stats.getRechargeTime() + "s");
        System.out.println(ConsoleView.MAGENTA + "  Tags:        " + ConsoleView.RESET
                + stats.getTags());
        System.out.println(ConsoleView.WHITE + "  Description: " + ConsoleView.RESET
                + stats.getDescription());
    }

    public void showZombie(String zombieName) {
        ZombieStats stats = getZombieStatsForName(zombieName.toUpperCase());
        if (stats == null) {
            view.printError("Zombie not found: " + zombieName);
            return;
        }
        view.printHeader("🧟 " + stats.getType().name());
        System.out.println(ConsoleView.CYAN + "  Type:        " + ConsoleView.RESET
                + stats.getType().name());
        System.out.println(ConsoleView.GREEN + "  HP:          " + ConsoleView.RESET
                + stats.getHp());
        System.out.println(ConsoleView.RED + "  DPS:         " + ConsoleView.RESET
                + stats.getDps());
        System.out.println(ConsoleView.WHITE + "  Move Speed:  " + ConsoleView.RESET
                + stats.getMoveSpeed());
        System.out.println(ConsoleView.YELLOW + "  Wave Cost:   " + ConsoleView.RESET
                + stats.getWaveCost());
        if (!stats.getArmors().isEmpty()) {
            System.out.println(ConsoleView.CYAN + "  Armor:       " + ConsoleView.RESET
                    + stats.getArmors());
        }
        System.out.println(ConsoleView.WHITE + "  Description: " + ConsoleView.RESET
                + stats.getDescription());
    }

    public void upgradePlant(String plantName, AppState appState) {
        User user = appState.getCurrentUser();
        PlantStats stats = getPlantStatsForName(plantName.toUpperCase());
        if (stats == null) {
            view.printError("Plant not found: " + plantName);
            return;
        }
        if (!userService.hasPlant(user, stats.getType())) {
            view.printError("You don't own this plant.");
            return;
        }
        long coinCost = 500L;
        if (user.getCoins() < coinCost) {
            view.printError("Need " + coinCost + " coins to upgrade. "
                    + "You have: " + user.getCoins());
            return;
        }
        user.setCoins(user.getCoins() - coinCost);
        view.printSuccess(plantName + " upgraded! Costs "
                + coinCost + " coins.");
    }

    public void purchasePlant(String plantName, AppState appState) {
        User user = appState.getCurrentUser();
        PlantStats stats = getPlantStatsForName(plantName.toUpperCase());
        if (stats == null) {
            view.printError("Plant not found: " + plantName);
            return;
        }
        if (userService.hasPlant(user, stats.getType())) {
            view.printError("You already own " + plantName + ".");
            return;
        }
        long cost = 2000L;
        if (user.getCoins() < cost) {
            view.printError("Need 2000 coins. You have: " + user.getCoins());
            return;
        }
        user.setCoins(user.getCoins() - cost);
        userService.unlockPlant(user, stats.getType());
        view.printSuccess(plantName + " purchased for 2000 coins!");
    }

    private PlantStats getPlantStatsForName(String name) {
        try {
            PlantType type = PlantType.valueOf(name);
            return PlantDataRegistry.getInstance().getStats(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private ZombieStats getZombieStatsForName(String name) {
        try {
            ZombieType type = ZombieType.valueOf(name);
            return ZombieDataRegistry.getInstance().getStats(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
