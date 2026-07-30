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
 * شامل منطق کامل ارتقای گیاه با seed packet و coin.
 */
public class CollectionController {

    /** هزینه سکه برای هر سطح ارتقا */
    private static final long[] UPGRADE_COIN_COST = { 500L, 1500L, 3000L };
    /** هزینه بسته‌بذر برای هر سطح ارتقا */
    private static final int[] UPGRADE_SEED_COST  = { 5, 10, 20 };
    /** حداکثر سطح ارتقا */
    private static final int MAX_UPGRADE_LEVEL = 3;

    private final UserService userService;
    private final ConsoleView view;

    public CollectionController(UserService userService, ConsoleView view) {
        this.userService = userService;
        this.view = view;
    }

    // ----------------------------------------------------------------
    //  نمایش گیاهان
    // ----------------------------------------------------------------

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
            int lvl = user.getPlantUpgradeLevel(plantName);
            String lvlStr = lvl > 0 ? " [Lv" + lvl + "]" : "";
            view.printRaw(ConsoleView.GREEN + "  " + i++ + ". "
                + ConsoleView.BOLD + plantName + lvlStr
                + ConsoleView.RESET + "  — " + desc);
        }
    }

    public void showAllPlants() {
        view.printHeader("🌿 All Plants in the Game");
        int i = 1;
        for (PlantType type : PlantType.values()) {
            PlantStats stats = PlantDataRegistry.getInstance().getStats(type);
            if (stats == null) continue;
            view.printRaw(ConsoleView.WHITE + "  " + i++ + ". "
                + ConsoleView.BOLD + type.name() + ConsoleView.RESET
                + " [" + stats.getCategory() + "]"
                + "  Cost:" + stats.getSunCost()
                + " HP:" + stats.getBaseHp()
                + "  — " + stats.getDescription());
        }
    }

    public void showPlantDetail(String plantName, AppState appState) {
        PlantStats stats = getPlantStatsForName(plantName.toUpperCase());
        if (stats == null) {
            view.printError("Plant not found: " + plantName);
            return;
        }
        User user = appState != null ? appState.getCurrentUser() : null;
        int currentLvl = user != null
                ? user.getPlantUpgradeLevel(stats.getType().name()) : 0;

        view.printHeader("🌱 " + plantName.toUpperCase() + " — Level " + currentLvl);
        view.printRaw(ConsoleView.CYAN + "  Category:    " + ConsoleView.RESET
            + stats.getCategory());
        view.printRaw(ConsoleView.YELLOW + "  Sun Cost:    " + ConsoleView.RESET
            + stats.getSunCost());
        view.printRaw(ConsoleView.GREEN + "  Base HP:     " + ConsoleView.RESET
            + stats.getBaseHp());
        view.printRaw(ConsoleView.RED + "  Base Damage: " + ConsoleView.RESET
            + stats.getBaseDamage());
        view.printRaw(ConsoleView.WHITE + "  Ability:     " + ConsoleView.RESET
            + stats.getBaseAbility());
        view.printRaw(ConsoleView.YELLOW + "  Plant Food:  " + ConsoleView.RESET
            + stats.getPlantFoodEffect());
        view.printRaw(ConsoleView.WHITE + "  Description: " + ConsoleView.RESET
            + stats.getDescription());

        // نمایش ارتقاهای موجود
        view.printRaw(ConsoleView.BOLD + "\n  Upgrade Path:" + ConsoleView.RESET);
        for (int lvl = 1; lvl <= MAX_UPGRADE_LEVEL; lvl++) {
            String effect = stats.getUpgradeEffect(lvl);
            if (!effect.isEmpty()) {
                String marker = currentLvl >= lvl ? "✅" : "🔒";
                long coinCost = UPGRADE_COIN_COST[lvl - 1];
                int seedCost = UPGRADE_SEED_COST[lvl - 1];
                view.printRaw("    " + marker + " Lv" + lvl + ": " + effect
                    + "  (Cost: " + coinCost + " coins + "
                    + seedCost + " seed packets)");
            }
        }

        // نمایش موجودی seed packet
        if (user != null) {
            int seeds = user.getSeedPackets(stats.getType());
            view.printRaw(ConsoleView.GREEN + "\n  Your seed packets: "
                + ConsoleView.RESET + seeds);
        }
    }

    // ----------------------------------------------------------------
    //  نمایش زامبی‌ها
    // ----------------------------------------------------------------

    public void showZombies(AppState appState) {
        User user = appState.getCurrentUser();
        view.printHeader("🧟 Seen Zombies");
        if (user.getSeenZombies() == null || user.getSeenZombies().isEmpty()) {
            view.printInfo("You haven't seen any zombies yet. Play some levels!");
            return;
        }
        int i = 1;
        for (String name : user.getSeenZombies()) {
            ZombieStats stats = getZombieStatsForName(name);
            String desc = stats != null ? stats.getDescription() : "";
            view.printRaw(ConsoleView.RED + "  " + i++ + ". "
                + ConsoleView.BOLD + name + ConsoleView.RESET
                + "  — " + desc);
        }
    }

    public void showAllZombies() {
        view.printHeader("🧟 All Zombies in the Game");
        int i = 1;
        for (ZombieType type : ZombieType.values()) {
            ZombieStats stats = ZombieDataRegistry.getInstance().getStats(type);
            if (stats == null) continue;
            view.printRaw(ConsoleView.WHITE + "  " + i++ + ". "
                + ConsoleView.BOLD + type.name() + ConsoleView.RESET
                + "  HP:" + stats.getHp()
                + "  — " + stats.getDescription());
        }
    }

    public void showZombieDetail(String zombieName) {
        ZombieStats stats = getZombieStatsForName(zombieName.toUpperCase());
        if (stats == null) {
            view.printError("Zombie not found: " + zombieName);
            return;
        }
        view.printHeader("🧟 " + zombieName.toUpperCase());
        view.printRaw(ConsoleView.RED + "  HP:          " + ConsoleView.RESET
            + stats.getHp());
        view.printRaw(ConsoleView.YELLOW + "  Speed:       " + ConsoleView.RESET
            + stats.getMoveSpeed());
        view.printRaw(ConsoleView.RED + "  Damage:      " + ConsoleView.RESET
            + stats.getDps());
        view.printRaw(ConsoleView.CYAN + "  Wave Cost:   " + ConsoleView.RESET
            + stats.getWaveCost());
        if (!stats.getArmors().isEmpty()) {
            view.printRaw(ConsoleView.CYAN + "  Armor:       " + ConsoleView.RESET
                + stats.getArmors());
        }
        view.printRaw(ConsoleView.WHITE + "  Description: " + ConsoleView.RESET
            + stats.getDescription());
    }

    // ----------------------------------------------------------------
    //  خرید گیاه
    // ----------------------------------------------------------------

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
        userService.save(user);
        view.printSuccess("🌱 " + plantName + " purchased for 2000 coins!");
    }

    // ----------------------------------------------------------------
    //  ارتقای گیاه — منطق کامل
    // ----------------------------------------------------------------

    public void upgradePlant(String plantName, AppState appState) {
        User user = appState.getCurrentUser();
        PlantStats stats = getPlantStatsForName(plantName.toUpperCase());

        // اعتبارسنجی: وجود گیاه
        if (stats == null) {
            view.printError("Plant not found: " + plantName);
            return;
        }

        // اعتبارسنجی: مالکیت
        if (!userService.hasPlant(user, stats.getType())) {
            view.printError("You don't own '" + plantName
                + "'. Purchase it first.");
            return;
        }

        // اعتبارسنجی: سطح فعلی
        int currentLevel = user.getPlantUpgradeLevel(stats.getType().name());
        if (currentLevel >= MAX_UPGRADE_LEVEL) {
            view.printError(plantName + " is already at max upgrade level ("
                + MAX_UPGRADE_LEVEL + ").");
            return;
        }

        // رشته اثر برای سطح بعدی
        int nextLevel = currentLevel + 1;
        String upgradeEffect = stats.getUpgradeEffect(nextLevel);
        if (upgradeEffect == null || upgradeEffect.isEmpty()) {
            view.printError(plantName
                + " has no upgrade defined for level " + nextLevel + ".");
            return;
        }

        // هزینه
        long coinCost = UPGRADE_COIN_COST[currentLevel];
        int seedCost  = UPGRADE_SEED_COST[currentLevel];

        // اعتبارسنجی: سکه
        if (user.getCoins() < coinCost) {
            view.printError("Not enough coins! Need: " + coinCost
                + " | Have: " + user.getCoins());
            return;
        }

        // اعتبارسنجی: seed packet
        int availableSeeds = user.getSeedPackets(stats.getType());
        if (availableSeeds < seedCost) {
            view.printError("Not enough seed packets for "
                + plantName + "!\n"
                + "  Need: " + seedCost
                + " | Have: " + availableSeeds + "\n"
                + "  Buy seed packets from the shop with:\n"
                + "    shop buy -i SEED_RANDOM -n 5\n"
                + "    shop buy -i SEED_CHOICE -n 10 -t " + plantName.toUpperCase());
            return;
        }

        // اعمال هزینه
        user.setCoins(user.getCoins() - coinCost);
        user.spendSeedPackets(stats.getType().name(), seedCost);

        // ثبت سطح ارتقا
        int newLevel = user.incrementPlantUpgradeLevel(stats.getType().name());

        // ذخیره
        userService.save(user);

        // پیام موفقیت
        view.printSuccess(
            "⬆️  " + plantName + " upgraded to Level " + newLevel + "!\n"
            + "   Effect: " + upgradeEffect + "\n"
            + "   Cost paid: " + coinCost + " coins + " + seedCost
            + " seed packets\n"
            + "   This upgrade is permanent and applies in all future battles.");

        // اگر سطح بالاتری وجود دارد، هزینه آن را نشان بده
        if (newLevel < MAX_UPGRADE_LEVEL) {
            String nextEffect = stats.getUpgradeEffect(newLevel + 1);
            if (nextEffect != null && !nextEffect.isEmpty()) {
                long nextCoin = UPGRADE_COIN_COST[newLevel];
                int nextSeed  = UPGRADE_SEED_COST[newLevel];
                view.printInfo("Next upgrade (Lv" + (newLevel + 1)
                    + "): " + nextEffect
                    + "  — Cost: " + nextCoin + " coins + "
                    + nextSeed + " seed packets");
            }
        } else {
            view.printInfo("🏆 " + plantName + " is now at MAX level!");
        }
    }

    // ----------------------------------------------------------------
    //  متدهای کمکی
    // ----------------------------------------------------------------

    private PlantStats getPlantStatsForName(String name) {
        try {
            PlantType type = PlantType.valueOf(name.trim().toUpperCase());
            return PlantDataRegistry.getInstance().getStats(type);
        } catch (IllegalArgumentException e) {
            // جستجوی fuzzy
            for (PlantType t : PlantType.values()) {
                if (t.name().replace("_", "").equalsIgnoreCase(
                        name.replace("-", "").replace(" ", ""))) {
                    return PlantDataRegistry.getInstance().getStats(t);
                }
            }
            return null;
        }
    }

    private ZombieStats getZombieStatsForName(String name) {
        try {
            ZombieType type = ZombieType.valueOf(name.trim().toUpperCase());
            return ZombieDataRegistry.getInstance().getStats(type);
        } catch (IllegalArgumentException e) {
            for (ZombieType t : ZombieType.values()) {
                if (t.name().replace("_", "").equalsIgnoreCase(
                        name.replace("-", "").replace(" ", ""))) {
                    return ZombieDataRegistry.getInstance().getStats(t);
                }
            }
            return null;
        }
    }
}
