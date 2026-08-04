package com.pvz2.minigame;

import com.pvz2.model.enums.PlantType;
import com.pvz2.util.RandomUtil;
import com.pvz2.view.ConsoleView;

import java.util.ArrayList;
import java.util.List;

/**
 * مینی‌گیم Beghouled — Match-3 با گیاهان.
 */
public class BeghouledMinigame {

    private static final int ROWS = 5;
    private static final int COLS = 9;
    private static final PlantType[] PLANT_POOL = {
        PlantType.PEASHOOTER, PlantType.SUNFLOWER, PlantType.WALL_NUT,
        PlantType.SNOW_PEA, PlantType.CHERRY_BOMB
    };

    private final int level;
    private final ConsoleView view;
    private PlantType[][] grid;
    private boolean[][] crater;
    private int sunAmount;
    private int matchCount;
    private int targetMatches;
    private boolean gameOver;
    private boolean won;

    public BeghouledMinigame(int level, ConsoleView view) {
        this.level = level;
        this.view = view;
        this.grid = new PlantType[ROWS][COLS];
        this.crater = new boolean[ROWS][COLS];
        this.sunAmount = 0;
        this.matchCount = 0;
        this.targetMatches = getTargetForLevel(level);
        this.gameOver = false;
        this.won = false;
        fillGrid();
    }

    private int getTargetForLevel(int lvl) {
        switch (lvl) {
            case 1: return 10;
            case 2: return 20;
            case 3: return 35;
            default: return 10;
        }
    }

    private void fillGrid() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (!crater[r][c]) {
                    grid[r][c] = randomPlant();
                }
            }
        }
    }

    private PlantType randomPlant() {
        return PLANT_POOL[RandomUtil.nextInt(PLANT_POOL.length)];
    }

    public void swapPlants(int x1, int y1, int x2, int y2) {
        if (!isValidPos(x1, y1) || !isValidPos(x2, y2)) {
            view.printError("Invalid position.");
            return;
        }
        if (crater[y1 - 1][x1 - 1] || crater[y2 - 1][x2 - 1]) {
            view.printError("Cannot swap on a crater.");
            return;
        }
        PlantType tmp = grid[y1 - 1][x1 - 1];
        grid[y1 - 1][x1 - 1] = grid[y2 - 1][x2 - 1];
        grid[y2 - 1][x2 - 1] = tmp;
        List<int[]> matches = findMatches();
        if (matches.isEmpty()) {
            PlantType tmp2 = grid[y1 - 1][x1 - 1];
            grid[y1 - 1][x1 - 1] = grid[y2 - 1][x2 - 1];
            grid[y2 - 1][x2 - 1] = tmp2;
            view.printError("That swap doesn't create a match.");
            return;
        }
        processMatches(matches);
        applyGravity();
        fillGrid();
        processChainMatches();
        checkWin();
        printBoard();
    }

    private List<int[]> findMatches() {
        List<int[]> matched = new ArrayList<>();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c <= COLS - 3; c++) {
                if (grid[r][c] != null
                        && grid[r][c] == grid[r][c + 1]
                        && grid[r][c] == grid[r][c + 2]) {
                    addMatch(matched, r, c);
                    addMatch(matched, r, c + 1);
                    addMatch(matched, r, c + 2);
                }
            }
        }
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r <= ROWS - 3; r++) {
                if (grid[r][c] != null
                        && grid[r][c] == grid[r + 1][c]
                        && grid[r][c] == grid[r + 2][c]) {
                    addMatch(matched, r, c);
                    addMatch(matched, r + 1, c);
                    addMatch(matched, r + 2, c);
                }
            }
        }
        return matched;
    }

    private void addMatch(List<int[]> matched, int r, int c) {
        for (int[] pos : matched) {
            if (pos[0] == r && pos[1] == c) {
                return;
            }
        }
        matched.add(new int[]{r, c});
    }

    private void processMatches(List<int[]> matches) {
        int size = matches.size();
        int sunReward = 50 * (size - 2);
        sunAmount += sunReward;
        matchCount++;
        view.printRaw(ConsoleView.GREEN + "  ✔ Match of "
            + size + "! +" + sunReward + " sun ☀" + ConsoleView.RESET);
        if (size >= 4) {
            view.printRaw(ConsoleView.YELLOW
                + "  🌟 Bonus match! Extra sun!" + ConsoleView.RESET);
        }
        for (int[] pos : matches) {
            grid[pos[0]][pos[1]] = null;
        }
    }

    private void processChainMatches() {
        List<int[]> chainMatches;
        while (!(chainMatches = findMatches()).isEmpty()) {
            int baseSun = 50 * (chainMatches.size() - 2);
            sunAmount += baseSun + 50;
            matchCount++;
            view.printRaw(ConsoleView.MAGENTA + "  ⚡ Chain match! +"
                + (baseSun + 50) + " sun (bonus!)" + ConsoleView.RESET);
            for (int[] pos : chainMatches) {
                grid[pos[0]][pos[1]] = null;
            }
            applyGravity();
            fillGrid();
        }
    }

    private void applyGravity() {
        for (int c = 0; c < COLS; c++) {
            int writeRow = ROWS - 1;
            for (int r = ROWS - 1; r >= 0; r--) {
                if (grid[r][c] != null && !crater[r][c]) {
                    grid[writeRow][c] = grid[r][c];
                    if (writeRow != r) {
                        grid[r][c] = null;
                    }
                    writeRow--;
                }
            }
            while (writeRow >= 0) {
                grid[writeRow][c] = null;
                writeRow--;
            }
        }
    }

    public void upgradeOnBoard(String plantTypeName) {
        PlantType from;
        try {
            from = PlantType.valueOf(plantTypeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            view.printError("Unknown plant: " + plantTypeName);
            return;
        }
        PlantType to = getUpgradeTarget(from);
        if (to == null) {
            view.printError("No upgrade available for " + plantTypeName);
            return;
        }
        int upgradeCost = getUpgradeCost(from);
        if (sunAmount < upgradeCost) {
            view.printError("Need " + upgradeCost
                + " sun to upgrade. You have " + sunAmount + ".");
            return;
        }
        sunAmount -= upgradeCost;
        int count = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] == from) {
                    grid[r][c] = to;
                    count++;
                }
            }
        }
        view.printSuccess("Upgraded " + count + " " + from.name()
            + " → " + to.name() + "! Cost: " + upgradeCost + " sun.");
        printBoard();
    }

    private PlantType getUpgradeTarget(PlantType from) {
        switch (from) {
            case PEASHOOTER:       return PlantType.REPEATER;
            case REPEATER:         return PlantType.MEGA_GATLING_PEA;
            case WALL_NUT:         return PlantType.TALL_NUT;
            case CABBAGE_PULT:     return PlantType.MELON_PULT;
            case MELON_PULT:       return PlantType.WINTER_MELON;
            case PUFF_SHROOM:      return PlantType.FUME_SHROOM;
            default: return null;
        }
    }

    private int getUpgradeCost(PlantType from) {
        switch (from) {
            case PEASHOOTER: return 500;
            case REPEATER:   return 1500;
            case WALL_NUT:   return 500;
            case CABBAGE_PULT: return 1000;
            case MELON_PULT: return 750;
            case PUFF_SHROOM: return 250;
            default: return 500;
        }
    }

    private void checkWin() {
        if (matchCount >= targetMatches) {
            won = true;
            gameOver = true;
            view.printGameWon();
            view.printRaw(ConsoleView.GREEN
                + "  🏆 Created " + matchCount
                + "/" + targetMatches + " matches!" + ConsoleView.RESET);
        }
    }

    public void markCrater(int x, int y) {
        if (isValidPos(x, y)) {
            crater[y - 1][x - 1] = true;
            grid[y - 1][x - 1] = null;
        }
    }

    private boolean isValidPos(int x, int y) {
        return x >= 1 && x <= COLS && y >= 1 && y <= ROWS;
    }

    private boolean hasValidMoves() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS - 1; c++) {
                if (trySwap(r, c, r, c + 1)) {
                    return true;
                }
            }
        }
        for (int r = 0; r < ROWS - 1; r++) {
            for (int c = 0; c < COLS; c++) {
                if (trySwap(r, c, r + 1, c)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean trySwap(int r1, int c1, int r2, int c2) {
        PlantType tmp = grid[r1][c1];
        grid[r1][c1] = grid[r2][c2];
        grid[r2][c2] = tmp;
        boolean hasMatch = !findMatches().isEmpty();
        grid[r2][c2] = grid[r1][c1];
        grid[r1][c1] = tmp;
        return hasMatch;
    }

    public void checkAndResetIfNoMoves() {
        if (!hasValidMoves()) {
            view.printRaw(ConsoleView.YELLOW
                + "  🔄 No valid moves! Resetting board..." + ConsoleView.RESET);
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    if (!crater[r][c]) {
                        grid[r][c] = randomPlant();
                    }
                }
            }
        }
    }

    public void printBoard() {
        view.printHeader("🧩 Beghouled — Level " + level
            + "  Matches: " + matchCount + "/" + targetMatches
            + "  ☀ " + sunAmount);
        StringBuilder header = new StringBuilder("    ");
        for (int c = 1; c <= COLS; c++) {
            header.append(String.format(" %-4d", c));
        }
        view.printRaw(header.toString());
        view.printRaw("    " + "─────".repeat(COLS));
        for (int r = 0; r < ROWS; r++) {
            StringBuilder rowSb = new StringBuilder(String.format(" %d │ ", r + 1));
            for (int c = 0; c < COLS; c++) {
                if (crater[r][c]) {
                    rowSb.append(ConsoleView.RED).append("💥   ").append(ConsoleView.RESET);
                } else if (grid[r][c] == null) {
                    rowSb.append("     ");
                } else {
                    rowSb.append(getPlantEmoji(grid[r][c])).append("   ");
                }
            }
            view.printRaw(rowSb.toString());
        }
        view.printRaw("    " + "─────".repeat(COLS));
        printUpgradeTable();
    }

    private String getPlantEmoji(PlantType type) {
        switch (type) {
            case PEASHOOTER:        return ConsoleView.GREEN + "P" + ConsoleView.RESET;
            case SUNFLOWER:         return ConsoleView.YELLOW + "S" + ConsoleView.RESET;
            case WALL_NUT:          return ConsoleView.YELLOW + "W" + ConsoleView.RESET;
            case SNOW_PEA:          return ConsoleView.CYAN + "N" + ConsoleView.RESET;
            case CHERRY_BOMB:       return ConsoleView.RED + "C" + ConsoleView.RESET;
            case REPEATER:          return ConsoleView.GREEN + "R" + ConsoleView.RESET;
            case MEGA_GATLING_PEA:  return ConsoleView.GREEN + "G" + ConsoleView.RESET;
            case TALL_NUT:          return ConsoleView.YELLOW + "T" + ConsoleView.RESET;
            case MELON_PULT:        return ConsoleView.GREEN + "M" + ConsoleView.RESET;
            case WINTER_MELON:      return ConsoleView.CYAN + "I" + ConsoleView.RESET;
            default:                return ConsoleView.WHITE + "?" + ConsoleView.RESET;
        }
    }

    private void printUpgradeTable() {
        view.printRaw(ConsoleView.CYAN
            + "  Upgrades: P(500)→R(1500)→G  W(500)→T  M(1000)→MELON(750)→I"
            + ConsoleView.RESET);
        view.printRaw("  Use: upgrade plant -t <TYPE>");
    }

    public int getSunAmount() { return sunAmount; }
    public int getMatchCount() { return matchCount; }
    public boolean isGameOver() { return gameOver; }
    public boolean isWon() { return won; }
}
