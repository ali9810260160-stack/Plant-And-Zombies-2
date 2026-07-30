package controller;

import minigame.*;
import model.AppState;
import model.enums.LevelType;
import model.enums.MenuType;
import model.enums.PlantType;
import model.enums.ZombieType;
import service.ScoredGameService;
import service.UserService;
import view.ConsoleView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * کنترلر یکپارچه مینی‌گیم‌ها.
 * همه ۴ مینی‌گیم + بازی امتیازی از اینجا مدیریت می‌شوند.
 */
public class MinigameController {

    private final ConsoleView view;
    private final ScoredGameService scoredGameService;
    private final UserService userService;

    private VasebreakerMinigame vasebreakerGame;
    private WallnutBowlingMinigame bowlingGame;
    private IZombieMinigame iZombieGame;
    private BeghouledMinigame beghouledGame;
    private ZombotanyMinigame zombotanyGame;
    private String activeMinigame;

    public MinigameController(ConsoleView view, UserService userService) {
        this.view = view;
        this.userService = userService;
        this.scoredGameService = new ScoredGameService(view);
        this.activeMinigame = null;
    }

    // ──────────────────────────────
    //  شروع مینی‌گیم‌ها
    // ──────────────────────────────

    public void startVasebreaker(int level, AppState appState) {
        validateLevel(level);
        vasebreakerGame = new VasebreakerMinigame(level, view);
        activeMinigame = "VASEBREAKER";
        appState.setCurrentMenu(MenuType.IN_GAME);
        appState.setActiveMinigame("VASEBREAKER");
        view.printSuccess("🏺 Vasebreaker Level " + level + " started!");
        vasebreakerGame.printBoard();
    }

    public void startBowling(int level, AppState appState) {
        validateLevel(level);
        bowlingGame = new WallnutBowlingMinigame(level, view);
        activeMinigame = "BOWLING";
        appState.setCurrentMenu(MenuType.IN_GAME);
        appState.setActiveMinigame("BOWLING");
        view.printSuccess("🎳 Wallnut Bowling Level " + level + " started!");
        bowlingGame.printBoard();
    }

    public void startIZombie(int level, AppState appState) {
        validateLevel(level);
        iZombieGame = new IZombieMinigame(level, view);
        activeMinigame = "IZOMBIE";
        appState.setCurrentMenu(MenuType.IN_GAME);
        appState.setActiveMinigame("IZOMBIE");
        view.printSuccess("🧟 I, Zombie Level " + level + " started!");
        iZombieGame.printBoard();
    }

    public void startBeghouled(int level, AppState appState) {
        validateLevel(level);
        beghouledGame = new BeghouledMinigame(level, view);
        activeMinigame = "BEGHOULED";
        appState.setCurrentMenu(MenuType.IN_GAME);
        appState.setActiveMinigame("BEGHOULED");
        view.printSuccess("🧩 Beghouled Level " + level + " started!");
        beghouledGame.printBoard();
    }

    public void startZombotany(int level, AppState appState) {
        validateLevel(level);
        List<PlantType> defaultPlants = Arrays.asList(
                PlantType.PEASHOOTER, PlantType.REPEATER,
                PlantType.SNOW_PEA, PlantType.WALL_NUT,
                PlantType.CHERRY_BOMB, PlantType.CABBAGE_PULT
        );
        zombotanyGame = new ZombotanyMinigame(level, view, defaultPlants);
        activeMinigame = "ZOMBOTANY";
        appState.setCurrentMenu(MenuType.IN_GAME);
        appState.setActiveMinigame("ZOMBOTANY");
        view.printSuccess("⚡ Zombotany Level " + level + " started!");
        zombotanyGame.printBoard();
    }

    public void startScoredGame(AppState appState) {
        scoredGameService.reset();
        List<PlantType> plants = scoredGameService.getDefaultScoredPlants();
        view.printHeader("🏆 Scored Game — Daily Challenge");
        view.printRaw(ConsoleView.YELLOW
                + "  Daily Seed: " + scoredGameService.getDailySeed() + ConsoleView.RESET);
        view.printRaw(ConsoleView.CYAN
                + "  All players use the same zombie patterns today!" + ConsoleView.RESET);
        view.printRaw("  Your plants: " + plants);
        view.printRaw("");
        view.printRaw("  MeoPoint Scoring Patterns:");
        view.printRaw("  1. Multi-Kill:       Hit 2+ zombies with one projectile → 500pt×count");
        view.printRaw("  2. Speed-Kill Combo: Kill zombies in <3s each → 300pt×combo");
        view.printRaw("  3. AoE Simultaneous: Kill 3+ at once → 750pt×count");
        view.printRaw("  4. Clean Wave:       No plants lost → 1000pt×waveNum");
        view.printRaw("  5. Item Collector:   Collect 5+ sun in 10 ticks → 200pt×count");
        view.printRaw("");
        view.printInfo("Use 'advance time -t <n> ticks' to play.");
        view.printInfo("Type 'show scored-game score' to see your current score.");
        activeMinigame = "SCORED";
        appState.setCurrentMenu(MenuType.IN_GAME);
        appState.setActiveMinigame("SCORED");
    }

    // ──────────────────────────────
    //  دستورات درون مینی‌گیم
    // ──────────────────────────────

    public boolean handleMinigameCommand(String input, AppState appState) {
        String mg = appState.getActiveMinigame();
        if (mg == null) {
            return false;
        }
        switch (mg) {
            case "VASEBREAKER": return handleVasebreaker(input, appState);
            case "BOWLING":     return handleBowling(input, appState);
            case "IZOMBIE":     return handleIZombie(input, appState);
            case "BEGHOULED":   return handleBeghouled(input, appState);
            case "ZOMBOTANY":   return handleZombotany(input, appState);
            case "SCORED":      return handleScored(input, appState);
            default:            return false;
        }
    }

    private boolean handleVasebreaker(String input, AppState appState) {
        java.util.regex.Matcher m;
        if ((m = util.InputParser.match(input,
                model.enums.CommandRegex.BREAK_VASE)) != null) {
            int x = Integer.parseInt(m.group(1));
            int y = Integer.parseInt(m.group(2));
            vasebreakerGame.breakVase(x, y);
            vasebreakerGame.printBoard();
            checkMinigameEnd(vasebreakerGame.isGameOver(),
                    vasebreakerGame.isWon(), appState);
            return true;
        }
        if ((m = util.InputParser.match(input,
                model.enums.CommandRegex.PICKUP_SEED)) != null) {
            int x = Integer.parseInt(m.group(1));
            int y = Integer.parseInt(m.group(2));
            vasebreakerGame.pickupSeed(x, y);
            return true;
        }
        if (input.equalsIgnoreCase("show map")) {
            vasebreakerGame.printBoard();
            return true;
        }
        return false;
    }

    private boolean handleBowling(String input, AppState appState) {
        java.util.regex.Matcher m;
        if ((m = util.InputParser.match(input,
                model.enums.CommandRegex.PLACE_BOWLING)) != null) {
            String typeName = m.group(1).toUpperCase();
            int x = Integer.parseInt(m.group(2));
            int y = Integer.parseInt(m.group(3));
            try {
                PlantType type = PlantType.valueOf(typeName);
                bowlingGame.placeBowling(type, x, y);
                checkMinigameEnd(bowlingGame.isGameOver(),
                        bowlingGame.isWon(), appState);
            } catch (IllegalArgumentException e) {
                view.printError("Unknown bowling type: " + typeName
                        + ". Use: WALLNUT_BOWLING, EXPLODE_O_NUT_BOWLING, BIG_WALLNUT");
            }
            return true;
        }
        if (input.equalsIgnoreCase("show map")) {
            bowlingGame.printBoard();
            return true;
        }
        if (input.equalsIgnoreCase("advance time -t 10 ticks")
                || input.matches("advance time -t \\d+ ticks")) {
            int ticks = extractTicks(input);
            for (int i = 0; i < ticks; i++) {
                bowlingGame.tick();
                if (bowlingGame.isGameOver()) {
                    break;
                }
            }
            bowlingGame.printBoard();
            checkMinigameEnd(bowlingGame.isGameOver(),
                    bowlingGame.isWon(), appState);
            return true;
        }
        return false;
    }

    private boolean handleIZombie(String input, AppState appState) {
        java.util.regex.Matcher m;
        if ((m = util.InputParser.match(input,
                model.enums.CommandRegex.PLACE_ZOMBIE)) != null) {
            String typeName = m.group(1).toUpperCase();
            int x = Integer.parseInt(m.group(2));
            int y = Integer.parseInt(m.group(3));
            try {
                ZombieType type = ZombieType.valueOf(typeName);
                iZombieGame.placeZombie(type, x, y);
                iZombieGame.printBoard();
            } catch (IllegalArgumentException e) {
                view.printError("Unknown zombie type: " + typeName);
            }
            return true;
        }
        if (input.equalsIgnoreCase("show map")) {
            iZombieGame.printBoard();
            return true;
        }
        if (input.matches("advance time -t \\d+ ticks")) {
            int ticks = extractTicks(input);
            for (int i = 0; i < ticks; i++) {
                iZombieGame.tick();
                if (iZombieGame.isGameOver()) {
                    break;
                }
            }
            iZombieGame.printBoard();
            checkMinigameEnd(iZombieGame.isGameOver(),
                    iZombieGame.isWon(), appState);
            return true;
        }
        return false;
    }

    private boolean handleBeghouled(String input, AppState appState) {
        java.util.regex.Matcher m;
        if ((m = util.InputParser.match(input,
                model.enums.CommandRegex.SWAP_PLANTS)) != null) {
            int x1 = Integer.parseInt(m.group(1));
            int y1 = Integer.parseInt(m.group(2));
            int x2 = Integer.parseInt(m.group(3));
            int y2 = Integer.parseInt(m.group(4));
            beghouledGame.swapPlants(x1, y1, x2, y2);
            beghouledGame.checkAndResetIfNoMoves();
            checkMinigameEnd(beghouledGame.isGameOver(),
                    beghouledGame.isWon(), appState);
            return true;
        }
        if ((m = util.InputParser.match(input,
                model.enums.CommandRegex.UPGRADE_PLANT_BEGHOULED)) != null) {
            beghouledGame.upgradeOnBoard(m.group(1));
            return true;
        }
        if (input.equalsIgnoreCase("show map")) {
            beghouledGame.printBoard();
            return true;
        }
        return false;
    }

    private boolean handleZombotany(String input, AppState appState) {
        java.util.regex.Matcher m;
        if ((m = util.InputParser.match(input,
                model.enums.CommandRegex.PLANT_PLANT)) != null) {
            String typeName = m.group(1).toUpperCase();
            int x = Integer.parseInt(m.group(2));
            int y = Integer.parseInt(m.group(3));
            try {
                PlantType type = PlantType.valueOf(typeName);
                zombotanyGame.plantPlant(type, x, y);
                zombotanyGame.printBoard();
            } catch (IllegalArgumentException e) {
                view.printError("Unknown plant: " + typeName);
            }
            return true;
        }
        if (input.equalsIgnoreCase("show map")) {
            zombotanyGame.printBoard();
            return true;
        }
        if (input.matches("advance time -t \\d+ ticks")) {
            int ticks = extractTicks(input);
            for (int i = 0; i < ticks; i++) {
                zombotanyGame.tick();
                if (zombotanyGame.isGameOver()) {
                    break;
                }
            }
            zombotanyGame.printBoard();
            checkMinigameEnd(zombotanyGame.isGameOver(),
                    zombotanyGame.isWon(), appState);
            return true;
        }
        return false;
    }

    private boolean handleScored(String input, AppState appState) {
        if (input.equalsIgnoreCase("show scored-game score")) {
            view.printInfo("Current MeoPoints: "
                    + scoredGameService.getMeoPoints());
            return true;
        }
        if (input.equalsIgnoreCase("end scored game")) {
            scoredGameService.printFinalScore(appState.getCurrentUser());
            userService.updateHighScore(appState.getCurrentUser(),
                    scoredGameService.getMeoPoints());
            userService.save(appState.getCurrentUser());
            appState.setActiveMinigame(null);
            appState.setCurrentMenu(MenuType.MAIN);
            return true;
        }
        return false;
    }

    private void checkMinigameEnd(boolean over, boolean won, AppState appState) {
        if (!over) {
            return;
        }
        if (won) {
            model.User user = appState.getCurrentUser();
            user.setMinigamesCompleted(user.getMinigamesCompleted() + 1);
            userService.save(user);
            view.printSuccess("Minigame complete! Returning to main menu.");
        } else {
            view.printError("Minigame over. Better luck next time!");
        }
        appState.setActiveMinigame(null);
        appState.setCurrentMenu(MenuType.GAME);
    }

    private void validateLevel(int level) {
        if (level < 1 || level > 3) {
            throw new exception.GameException(
                    "Level must be 1, 2, or 3. Got: " + level);
        }
    }

    private int extractTicks(String input) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("advance time -t (\\d+) ticks")
                .matcher(input);
        if (m.matches()) {
            return Integer.parseInt(m.group(1));
        }
        return 10;
    }

    public ScoredGameService getScoredGameService() {
        return scoredGameService;
    }
}
