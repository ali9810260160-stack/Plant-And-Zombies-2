package controller;

import model.AppState;
import model.User;
import view.ConsoleView;

/**
 * کنترلر Travel Log و کوئست‌ها.
 */
public class TravelLogController {

    private final ConsoleView view;

    public TravelLogController(ConsoleView view) {
        this.view = view;
    }

    public void showPage(String pageName, AppState appState) {
        appState.setCurrentTravelLogPage(pageName.toLowerCase());
        showCurrentPage(appState);
    }

    public void showCurrentPage(AppState appState) {
        String page = appState.getCurrentTravelLogPage();
        if (page == null) {
            page = "story";
        }
        switch (page) {
            case "story":      showStoryQuests(appState); break;
            case "daily":      showDailyQuests(appState); break;
            case "epic":       showEpicQuests(appState); break;
            case "repeatable": showRepeatableQuests(appState); break;
            case "minigame":   showMinigameMenu(appState); break;
            default:
                view.printError("Unknown page: " + page
                        + ". Available: story, daily, epic, repeatable, minigame");
        }
    }

    private void showStoryQuests(AppState appState) {
        view.printHeader("📖 Story Quests");
        User u = appState.getCurrentUser();
        printQuest("Complete Ancient Egypt 1-1", true,
                "Unlock Frostbite Caves", false, u);
        printQuest("Complete All Egypt Levels", false,
                "500 coins + unlock Beach", false, u);
        printQuest("Complete Frostbite Caves 2-1", false,
                "Unlock HunterZombie info", false, u);
        printQuest("Complete All Chapters", false,
                "2 gems + title: Hero", false, u);
    }

    private void showDailyQuests(AppState appState) {
        view.printHeader("📅 Daily Quests");
        printQuest("Kill 10 zombies today",
                true, "100 coins", false, null);
        printQuest("Collect 200 sun",
                false, "50 coins", false, null);
        printQuest("Play 1 game",
                false, "1 gem", false, null);
        view.printInfo("Daily quests reset at midnight.");
    }

    private void showEpicQuests(AppState appState) {
        view.printHeader("⚡ Epic Quests");
        printQuest("Kill 5 zombies with one projectile",
                false, "1 gem", false, null);
        printQuest("Kill 3 zombies simultaneously",
                false, "2 gems", false, null);
        printQuest("Complete a level without losing any plant",
                false, "2 gems", false, null);
        printQuest("Score 10000 MeoPoints in scored game",
                false, "3 gems", false, null);
        printQuest("Complete all minigames",
                false, "5 gems + exclusive title", false, null);
    }

    private void showRepeatableQuests(AppState appState) {
        view.printHeader("🔄 Repeatable Quests");
        printQuest("Collect 500 sun (×10)",
                false, "200 coins", false, null);
        printQuest("Kill 50 zombies (×5)",
                false, "300 coins", false, null);
        printQuest("Win 3 games in a row",
                false, "1 gem", false, null);
    }

    private void showMinigameMenu(AppState appState) {
        User u = appState.getCurrentUser();
        view.printHeader("🎮 Minigames");
        System.out.println(ConsoleView.CYAN
                + "  1. Vasebreaker" + ConsoleView.RESET);
        System.out.println("     Smash vases to find plants and zombies!");
        System.out.println("     Levels: 1-3 | Reward: 200/300/500 coins");
        System.out.println();
        System.out.println(ConsoleView.CYAN
                + "  2. Wallnut Bowling" + ConsoleView.RESET);
        System.out.println("     Roll walnuts to crush zombies!");
        System.out.println("     Levels: 1-3 | Reward: 200/300/500 coins");
        System.out.println();
        System.out.println(ConsoleView.CYAN
                + "  3. I, Zombie" + ConsoleView.RESET);
        System.out.println("     Play as zombies to eat plant brains!");
        System.out.println("     Levels: 1-3 | Reward: 300/400/600 coins");
        System.out.println();
        System.out.println(ConsoleView.CYAN
                + "  4. Beghouled (BONUS)" + ConsoleView.RESET);
        System.out.println("     Match-3 puzzle with plant upgrades!");
        System.out.println("     Levels: 1-3 | Reward: 500/700/1000 coins");
        System.out.println();
        System.out.println(ConsoleView.CYAN
                + "  5. Zombotany (BONUS)" + ConsoleView.RESET);
        System.out.println("     Face zombies with plant powers!");
        System.out.println("     Levels: 1-3 | Reward: 400/600/900 coins");
        view.printInfo("To play: menu enter chapter VASEBREAKER_1 (or _2, _3)");
    }

    private void printQuest(String name, boolean completed,
                            String reward, boolean claimed, User user) {
        String status = completed
                ? ConsoleView.GREEN + "[DONE] " + ConsoleView.RESET
                : ConsoleView.RED   + "[TODO] " + ConsoleView.RESET;
        System.out.println("  " + status + name);
        System.out.println("          Reward: "
                + ConsoleView.YELLOW + reward + ConsoleView.RESET);
    }
}
