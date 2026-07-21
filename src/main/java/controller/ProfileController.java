package controller;

import model.AppState;
import model.User;
import service.UserService;
import view.ConsoleView;

import java.util.regex.Matcher;

/**
 * کنترلر پروفایل کاربر.
 */
public class ProfileController {

    private final UserService userService;
    private final ConsoleView view;

    public ProfileController(UserService userService, ConsoleView view) {
        this.userService = userService;
        this.view = view;
    }

    public void changeUsername(Matcher m, AppState appState) {
        String newUsername = m.group(1);
        User user = appState.getCurrentUser();
        userService.changeUsername(user, newUsername);
        view.printSuccess("Username changed to: " + newUsername);
    }

    public void changeNickname(Matcher m, AppState appState) {
        String newNickname = m.group(1);
        User user = appState.getCurrentUser();
        userService.changeNickname(user, newNickname);
        view.printSuccess("Nickname changed to: " + newNickname);
    }

    public void changeEmail(Matcher m, AppState appState) {
        String newEmail = m.group(1);
        User user = appState.getCurrentUser();
        userService.changeEmail(user, newEmail);
        view.printSuccess("Email changed to: " + newEmail);
    }

    public void changePassword(Matcher m, AppState appState) {
        String newPwd = m.group(1);
        String oldPwd = m.group(2);
        User user = appState.getCurrentUser();
        userService.changePassword(user, oldPwd, newPwd);
        view.printSuccess("Password changed successfully.");
    }

    public void showInfo(AppState appState) {
        User user = appState.getCurrentUser();
        if (user == null) {
            view.printError("No user logged in.");
            return;
        }
        view.printSeparator();
        view.printHeader("👤 Profile: " + user.getNickname());
        System.out.println(ConsoleView.CYAN + "  Username:          "
            + ConsoleView.WHITE + user.getUsername() + ConsoleView.RESET);
        System.out.println(ConsoleView.CYAN + "  Nickname:          "
            + ConsoleView.WHITE + user.getNickname() + ConsoleView.RESET);
        System.out.println(ConsoleView.CYAN + "  Email:             "
            + ConsoleView.WHITE + user.getEmail() + ConsoleView.RESET);
        System.out.println(ConsoleView.CYAN + "  Gender:            "
            + ConsoleView.WHITE + user.getGender() + ConsoleView.RESET);
        System.out.println(ConsoleView.CYAN + "  Difficulty:        "
            + ConsoleView.WHITE + user.getDifficultyLevel() + "/5"
            + ConsoleView.RESET);
        System.out.println(ConsoleView.YELLOW + "  Coins:             "
            + user.getCoins() + ConsoleView.RESET);
        System.out.println(ConsoleView.YELLOW + "  Gems:              "
            + user.getGems() + ConsoleView.RESET);
        System.out.println(ConsoleView.GREEN + "  Games Played:      "
            + user.getGamesPlayed() + ConsoleView.RESET);
        System.out.println(ConsoleView.GREEN + "  Levels Completed:  "
            + user.getLevelsCompleted() + ConsoleView.RESET);
        System.out.println(ConsoleView.MAGENTA + "  Highest MeoPoint:  "
            + user.getHighestMeoPoint() + ConsoleView.RESET);
        view.printSeparator();
    }

    public void changeDifficulty(Matcher m, AppState appState) {
        int level = Integer.parseInt(m.group(1));
        User user = appState.getCurrentUser();
        userService.changeDifficulty(user, level);
        view.printSuccess("Difficulty set to " + level + "/5.");
        printDifficultyEffects(level);
    }

    private void printDifficultyEffects(int level) {
        double mult = (double) level / 3;
        System.out.println(ConsoleView.YELLOW
            + "  Effects at difficulty " + level + ":" + ConsoleView.RESET);
        System.out.printf("  Zombie HP multiplier:     %.2fx%n", mult);
        System.out.printf("  Zombie damage multiplier: %.2fx%n", mult);
        System.out.printf("  Wave cost multiplier:     %.2fx%n", 3.0 / level);
        System.out.printf("  Sun drop rate:            %.2fx%n", 3.0 / level);
        System.out.printf("  Game speed:               %.2fx%n", mult);
    }
}
