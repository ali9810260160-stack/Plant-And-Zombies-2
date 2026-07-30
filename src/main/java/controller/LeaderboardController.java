package controller;

import model.AppState;
import model.User;
import service.UserService;
import view.ConsoleView;

import java.util.Comparator;
import java.util.List;

/**
 * کنترلر لیدربورد.
 */
public class LeaderboardController {

    private final UserService userService;
    private final ConsoleView view;
    private String sortField = "highestMeoPoint";
    private boolean ascending = false;

    public LeaderboardController(UserService userService, ConsoleView view) {
        this.userService = userService;
        this.view = view;
    }

    public void show(AppState appState) {
        List<User> users = userService.getAllUsers();
        sortUsers(users);
        view.printHeader("🏆 Leaderboard");
        printTableHeader();
        int rank = 1;
        for (User u : users) {
            printRow(rank++, u);
        }
        view.printSeparator();
    }

    public void sort(String field, String order, AppState appState) {
        this.sortField = field;
        this.ascending = order.equalsIgnoreCase("asc");
        view.printSuccess("Sorted by " + field + " (" + order + ").");
        show(appState);
    }

    private void sortUsers(List<User> users) {
        Comparator<User> comp = getComparator();
        if (!ascending) {
            comp = comp.reversed();
        }
        users.sort(comp);
    }

    private Comparator<User> getComparator() {
        switch (sortField.toLowerCase()) {
            case "level":
                return Comparator.comparing(u ->
                    u.getLastReachedLevel() != null
                    ? u.getLastReachedLevel() : "");
            case "minigames":
                return Comparator.comparingInt(User::getMinigamesCompleted);
            case "dailyquests":
                return Comparator.comparingInt(User::getDailyQuestsCompleted);
            case "quests":
                return Comparator.comparingInt(User::getRegularQuestsCompleted);
            case "meopoint":
            default:
                return Comparator.comparingLong(User::getHighestMeoPoint);
        }
    }

    private void printTableHeader() {
        view.getLeaderboardView().printTableHeader();
        view.printRaw(ConsoleView.CYAN
            + "  " + "─".repeat(75) + ConsoleView.RESET);
    }

    private void printRow(int rank, User u) {
        view.getLeaderboardView().printRow(rank, u.getUsername(),
            u.getLastReachedLevel() != null ? u.getLastReachedLevel() : "-",
            u.getMinigamesCompleted(), u.getDailyQuestsCompleted(),
            u.getRegularQuestsCompleted(), u.getHighestMeoPoint());
    }
}
