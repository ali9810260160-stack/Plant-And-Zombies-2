package com.pvz2.minigame;

import com.pvz2.model.enums.PlantType;
import com.pvz2.model.enums.ZombieType;
import com.pvz2.util.RandomUtil;
import com.pvz2.view.ConsoleView;

import java.util.ArrayList;
import java.util.List;

/**
 * مینی‌گیم Vasebreaker — شکستن کوزه‌ها.
 */
public class VasebreakerMinigame {

    public enum VaseContent { EMPTY, ZOMBIE, PLANT_SEED, PLANT_SEED_GUARANTEED,
                               GARGANTUAR }

    public static class Vase {
        private final int x;
        private final int y;
        private VaseContent content;
        private ZombieType zombieType;
        private PlantType plantType;
        private boolean broken;
        private PlantType seedDrop;
        private boolean seedPickedUp;

        public Vase(int x, int y, VaseContent content) {
            this.x = x;
            this.y = y;
            this.content = content;
            this.broken = false;
            this.seedPickedUp = false;
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public VaseContent getContent() { return content; }
        public ZombieType getZombieType() { return zombieType; }
        public void setZombieType(ZombieType t) { this.zombieType = t; }
        public PlantType getPlantType() { return plantType; }
        public void setPlantType(PlantType p) { this.plantType = p; }
        public boolean isBroken() { return broken; }
        public void setBroken(boolean broken) { this.broken = broken; }
        public PlantType getSeedDrop() { return seedDrop; }
        public void setSeedDrop(PlantType s) { this.seedDrop = s; }
        public boolean isSeedPickedUp() { return seedPickedUp; }
        public void setSeedPickedUp(boolean b) { this.seedPickedUp = b; }
    }

    private final int level;
    private final ConsoleView view;
    private List<Vase> vases;
    private List<PlantType> availablePlants;
    private boolean gameOver;
    private boolean won;


    public VasebreakerMinigame(int level, ConsoleView view) {
        this.level = level;
        this.view = view;
        this.vases = new ArrayList<>();
        this.availablePlants = new ArrayList<>();
        this.gameOver = false;
        this.won = false;
        generateVases();
    }

    private void generateVases() {
        int rows = 5;
        int cols = getColsForLevel();
        ZombieType[] zombiePool = {
            ZombieType.NORMAL, ZombieType.CONEHEAD, ZombieType.BUCKETHEAD
        };
        PlantType[] plantPool = {
            PlantType.PEASHOOTER, PlantType.SUNFLOWER,
            PlantType.WALL_NUT, PlantType.SNOW_PEA
        };
        for (int r = 1; r <= rows; r++) {
            for (int c = 1; c <= cols; c++) {
                VaseContent content = pickContent(c, cols);
                Vase vase = new Vase(c, r, content);
                if (content == VaseContent.ZOMBIE) {
                    vase.setZombieType(
                        zombiePool[RandomUtil.nextInt(zombiePool.length)]);
                } else if (content == VaseContent.PLANT_SEED) {
                    vase.setSeedDrop(
                        plantPool[RandomUtil.nextInt(plantPool.length)]);
                } else if (content == VaseContent.PLANT_SEED_GUARANTEED) {
                    vase.setSeedDrop(
                        plantPool[RandomUtil.nextInt(plantPool.length)]);
                } else if (content == VaseContent.GARGANTUAR) {
                    vase.setZombieType(ZombieType.GARGANTUAR);
                }
                vases.add(vase);
            }
        }
    }

    private int getColsForLevel() {
        switch (level) {
            case 1: return 5;
            case 2: return 7;
            case 3: return 9;
            default: return 5;
        }
    }

    private VaseContent pickContent(int col, int totalCols) {
        if (col == 1) {
            return VaseContent.PLANT_SEED_GUARANTEED;
        }
        if (col == totalCols && level >= 2) {
            return VaseContent.GARGANTUAR;
        }
        double roll = RandomUtil.nextDouble();
        if (roll < 0.10) {
            return VaseContent.PLANT_SEED_GUARANTEED;
        }
        if (roll < 0.30) {
            return VaseContent.PLANT_SEED;
        }
        if (roll < 0.75) {
            return VaseContent.ZOMBIE;
        }
        return VaseContent.EMPTY;
    }

    public void breakVase(int x, int y) {
        Vase vase = findVase(x, y);
        if (vase == null) {
            view.printError("No vase at (" + x + ", " + y + ").");
            return;
        }
        if (vase.isBroken()) {
            view.printError("Vase already broken.");
            return;
        }
        vase.setBroken(true);
        handleVaseOpen(vase);
        checkWin();
    }

    private void handleVaseOpen(Vase vase) {
        switch (vase.getContent()) {
            case EMPTY:
                view.printRaw(ConsoleView.WHITE
                    + "  💨 Vase was empty!" + ConsoleView.RESET);
                break;
            case ZOMBIE:
            case GARGANTUAR:
                view.printRaw(ConsoleView.RED
                    + "  🧟 A " + vase.getZombieType().name()
                    + " emerged!" + ConsoleView.RESET);
                break;
            case PLANT_SEED:
            case PLANT_SEED_GUARANTEED:
                view.printRaw(ConsoleView.GREEN
                    + "  🌱 Seed packet found: "
                    + vase.getSeedDrop().name()
                    + "! Pick it up with: pickup seed -l ("
                    + vase.getX() + "," + vase.getY() + ")"
                    + ConsoleView.RESET);
                break;
            default:
                break;
        }
    }

    public void pickupSeed(int x, int y) {
        Vase vase = findVase(x, y);
        if (vase == null || !vase.isBroken()) {
            view.printError("No open vase at (" + x + ", " + y + ").");
            return;
        }
        if (vase.getSeedDrop() == null) {
            view.printError("No seed in this vase.");
            return;
        }
        if (vase.isSeedPickedUp()) {
            view.printError("Seed already picked up.");
            return;
        }
        vase.setSeedPickedUp(true);
        availablePlants.add(vase.getSeedDrop());
        view.printSuccess("Picked up "
            + vase.getSeedDrop().name() + " seed!");
    }

    private void checkWin() {
        boolean allBroken = vases.stream().allMatch(Vase::isBroken);
        if (allBroken) {
            won = true;
            gameOver = true;
            view.printGameWon();
        }
    }

    private Vase findVase(int x, int y) {
        for (Vase v : vases) {
            if (v.getX() == x && v.getY() == y) {
                return v;
            }
        }
        return null;
    }

    public void printBoard() {
        view.printHeader("🏺 Vasebreaker — Level " + level);
        int maxCols = getColsForLevel();
        view.printRaw(ConsoleView.CYAN
            + "  " + "─".repeat(maxCols * 6) + ConsoleView.RESET);
        for (int r = 1; r <= 5; r++) {
            StringBuilder row = new StringBuilder("  ");
            for (int c = 1; c <= maxCols; c++) {
                Vase v = findVase(c, r);
                row.append(formatVase(v)).append(" ");
            }
            view.printRaw(row.toString());
        }
        view.printRaw(ConsoleView.CYAN
            + "  " + "─".repeat(maxCols * 6) + ConsoleView.RESET);
        view.printRaw(ConsoleView.YELLOW
            + "  Available plants: " + availablePlants + ConsoleView.RESET);
        view.printInfo("break vase -l (x,y) | pickup seed -l (x,y)");
    }

    private String formatVase(Vase v) {
        if (v == null) {
            return "     ";
        }
        if (v.isBroken()) {
            if (v.getSeedDrop() != null && !v.isSeedPickedUp()) {
                return ConsoleView.GREEN + "[🌱] " + ConsoleView.RESET;
            }
            return ConsoleView.WHITE + "[   ] " + ConsoleView.RESET;
        }
        return ConsoleView.YELLOW + "[🏺] " + ConsoleView.RESET;
    }

    public boolean isGameOver() { return gameOver; }
    public boolean isWon() { return won; }
    public List<PlantType> getAvailablePlants() { return availablePlants; }
}
