package minigame;

import model.enums.ZombieType;
import model.zombies.ZombieDataRegistry;
import model.zombies.ZombieStats;
import util.RandomUtil;
import view.ConsoleView;

import java.util.ArrayList;
import java.util.List;

/**
 * مینی‌گیم I, Zombie — بازیکن کنترل زامبی‌ها را دارد.
 * هدف: خوردن همه مغزهای انتهای ردیف‌ها.
 * منبع درآمد: SunProducerZombie که در هر ردیف قرار دارد.
 */
public class IZombieMinigame {

    public static class IZombiePlant {
        private final String name;
        private int hp;
        private final int x;
        private final int y;
        private int damage;
        private int attackTimer;
        private final int attackPeriod;
        private boolean alive;

        public IZombiePlant(String name, int hp, int x, int y,
                             int damage, int attackPeriod) {
            this.name = name;
            this.hp = hp;
            this.x = x;
            this.y = y;
            this.damage = damage;
            this.attackPeriod = attackPeriod;
            this.attackTimer = 0;
            this.alive = true;
        }

        public void takeDamage(int dmg) {
            hp -= dmg;
            if (hp <= 0) {
                alive = false;
            }
        }

        public boolean canAttack() {
            attackTimer++;
            return attackTimer >= attackPeriod;
        }

        public void resetAttackTimer() {
            attackTimer = 0;
        }

        public String getName() { return name; }
        public int getHp() { return hp; }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getDamage() { return damage; }
        public boolean isAlive() { return alive; }
        public int getAttackPeriod() { return attackPeriod; }
    }

    public static class PlacedZombie {
        private double x;
        private int y;
        private ZombieType type;
        private int hp;
        private boolean alive;
        private int attackTimer;

        public PlacedZombie(ZombieType type, double x, int y, int hp) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.hp = hp;
            this.alive = true;
            this.attackTimer = 0;
        }

        public void takeDamage(int dmg) {
            hp -= dmg;
            if (hp <= 0) {
                alive = false;
            }
        }

        public boolean canAttack() {
            attackTimer++;
            return attackTimer >= 10;
        }

        public void resetAttackTimer() { attackTimer = 0; }

        public ZombieType getType() { return type; }
        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public int getY() { return y; }
        public int getHp() { return hp; }
        public boolean isAlive() { return alive; }
    }

    private final int level;
    private final ConsoleView view;
    private static final int ROWS = 5;
    private static final int COLS = 9;
    private final int plantCols;

    private List<IZombiePlant> plants;
    private List<PlacedZombie> zombies;
    private int[] brains;
    private int[] sunProducerHp;
    private int[] sunProducerProductionTimer;
    private int sunAmount;
    private int totalTicks;
    private boolean gameOver;
    private boolean won;

    /** جدول هزینه زامبی‌های قابل استفاده در این مینی‌گیم */
    private static final ZombieType[] AVAILABLE_ZOMBIES_L1 = {
        ZombieType.NORMAL, ZombieType.CONEHEAD,
        ZombieType.IMP, ZombieType.NEWSPAPER_ZOMBIE,
        ZombieType.BUCKETHEAD
    };
    private static final ZombieType[] AVAILABLE_ZOMBIES_L2 = {
        ZombieType.CONEHEAD, ZombieType.BUCKETHEAD,
        ZombieType.KNIGHT, ZombieType.ALL_STAR,
        ZombieType.GARGANTUAR
    };
    private static final ZombieType[] AVAILABLE_ZOMBIES_L3 = {
        ZombieType.BUCKETHEAD, ZombieType.KNIGHT,
        ZombieType.GARGANTUAR, ZombieType.ALL_STAR,
        ZombieType.PROSPECTOR_ZOMBIE, ZombieType.ARCADE_ZOMBIE,
        ZombieType.PIANIST_ZOMBIE, ZombieType.TURQUOISE_ZOMBIE,
        ZombieType.PARASOL_ZOMBIE, ZombieType.BARREL_ROLLER
    };

    public IZombieMinigame(int level, ConsoleView view) {
        this.level = level;
        this.view = view;
        this.plantCols = 4;
        this.plants = new ArrayList<>();
        this.zombies = new ArrayList<>();
        this.brains = new int[ROWS];
        this.sunProducerHp = new int[ROWS];
        this.sunProducerProductionTimer = new int[ROWS];
        this.sunAmount = 150;
        this.totalTicks = 0;
        this.gameOver = false;
        this.won = false;
        initField();
    }

    private void initField() {
        for (int r = 0; r < ROWS; r++) {
            brains[r] = 1;
            sunProducerHp[r] = 490;
            sunProducerProductionTimer[r] = 0;
        }
        generatePlants();
    }

    private void generatePlants() {
        String[][] plantConfigs = getPlantConfigs();
        for (String[] cfg : plantConfigs) {
            int col = Integer.parseInt(cfg[0]);
            int row = Integer.parseInt(cfg[1]);
            String name = cfg[2];
            int hp = Integer.parseInt(cfg[3]);
            int dmg = Integer.parseInt(cfg[4]);
            int period = Integer.parseInt(cfg[5]);
            plants.add(new IZombiePlant(name, hp, col, row, dmg, period));
        }
    }

    private String[][] getPlantConfigs() {
        switch (level) {
            case 1:
                return new String[][]{
                    {"1","1","Sunflower","300","0","0"},
                    {"2","2","Peashooter","300","20","15"},
                    {"1","3","WallNut","4000","0","0"},
                    {"3","4","Peashooter","300","20","15"},
                    {"2","5","Sunflower","300","0","0"}
                };
            case 2:
                return new String[][]{
                    {"1","1","Repeater","300","40","10"},
                    {"2","1","WallNut","4000","0","0"},
                    {"1","2","SnowPea","300","20","12"},
                    {"3","3","WallNut","4000","0","0"},
                    {"1","3","Repeater","300","40","10"},
                    {"2","4","TallNut","8000","0","0"},
                    {"1","5","Repeater","300","40","10"}
                };
            case 3:
                return new String[][]{
                    {"1","1","GatlingPea","300","80","8"},
                    {"2","1","TallNut","8000","0","0"},
                    {"3","1","Torchwood","500","0","0"},
                    {"1","2","WinterMelon","300","80","10"},
                    {"2","2","TallNut","8000","0","0"},
                    {"1","3","GatlingPea","300","80","8"},
                    {"1","4","Snapdragon","300","80","8"},
                    {"2","4","TallNut","8000","0","0"},
                    {"1","5","GatlingPea","300","80","8"}
                };
            default:
                return new String[][]{
                    {"1","1","Sunflower","300","0","0"}
                };
        }
    }

    /** پیشروی زمان */
    public void tick() {
        totalTicks++;
        produceSun();
        plantAttack();
        moveZombies();
        checkWin();
        checkLose();
    }

    private void produceSun() {
        for (int r = 0; r < ROWS; r++) {
            if (sunProducerHp[r] <= 0) {
                continue;
            }
            sunProducerProductionTimer[r]++;
            int period = getSunProducerPeriod();
            if (sunProducerProductionTimer[r] >= period) {
                int sunGain = getSunProducerAmount();
                sunAmount += sunGain;
                sunProducerProductionTimer[r] = 0;
                System.out.println(ConsoleView.YELLOW + "  ☀ SunProducer in row "
                    + (r + 1) + " produced " + sunGain + " sun! Total: "
                    + sunAmount + ConsoleView.RESET);
            }
        }
    }

    private int getSunProducerPeriod() {
        int basePeriod = 200;
        int reduction = Math.min(180, totalTicks / 5);
        return Math.max(20, basePeriod - reduction);
    }

    private int getSunProducerAmount() {
        if (totalTicks < 100) {
            return 25;
        } else if (totalTicks < 300) {
            return 50;
        }
        return 75;
    }

    private void plantAttack() {
        for (IZombiePlant plant : plants) {
            if (!plant.isAlive() || plant.getAttackPeriod() == 0) {
                continue;
            }
            if (plant.canAttack()) {
                plant.resetAttackTimer();
                attackNearestZombie(plant);
            }
        }
    }

    private void attackNearestZombie(IZombiePlant plant) {
        PlacedZombie nearest = null;
        double minDist = Double.MAX_VALUE;
        for (PlacedZombie z : zombies) {
            if (!z.isAlive() || z.getY() != plant.getY()) {
                continue;
            }
            if (z.getX() >= plant.getX()) {
                double dist = z.getX() - plant.getX();
                if (dist < minDist) {
                    minDist = dist;
                    nearest = z;
                }
            }
        }
        if (nearest != null) {
            nearest.takeDamage(plant.getDamage());
            if (!nearest.isAlive()) {
                System.out.println(ConsoleView.RED + "  💀 "
                    + nearest.getType().name()
                    + " killed by plant!" + ConsoleView.RESET);
            }
        }
    }

    private void moveZombies() {
        for (PlacedZombie z : zombies) {
            if (!z.isAlive()) {
                continue;
            }
            IZombiePlant blocking = findBlockingPlant(z);
            if (blocking != null) {
                if (z.canAttack()) {
                    blocking.takeDamage(100);
                    z.resetAttackTimer();
                    if (!blocking.isAlive()) {
                        System.out.println(ConsoleView.GREEN
                            + "  🧟 " + z.getType().name()
                            + " destroyed " + blocking.getName()
                            + "!" + ConsoleView.RESET);
                    }
                }
            } else {
                z.setX(z.getX() - 0.2);
                if (z.getX() <= 0 && brains[z.getY() - 1] > 0) {
                    brains[z.getY() - 1]--;
                    System.out.println(ConsoleView.GREEN
                        + "  🧠 Brain eaten in row " + z.getY()
                        + "!" + ConsoleView.RESET);
                    z.setX(z.getX() - 1);
                }
            }
        }
        zombies.removeIf(z -> !z.isAlive() || z.getX() < -1);
    }

    private IZombiePlant findBlockingPlant(PlacedZombie zombie) {
        for (IZombiePlant p : plants) {
            if (!p.isAlive()) {
                continue;
            }
            if (p.getY() == zombie.getY()
                    && Math.abs(p.getX() - (int) zombie.getX()) < 1) {
                return p;
            }
        }
        return null;
    }

    private void checkWin() {
        int totalBrains = 0;
        for (int b : brains) {
            totalBrains += b;
        }
        if (totalBrains == 0) {
            won = true;
            gameOver = true;
            view.printGameWon();
            System.out.println(ConsoleView.GREEN
                + "  🏆 All brains eaten! You win!" + ConsoleView.RESET);
        }
    }

    private void checkLose() {
        if (sunAmount <= 0 && zombies.isEmpty()) {
            int totalBrains = 0;
            for (int b : brains) {
                totalBrains += b;
            }
            if (totalBrains > 0) {
                gameOver = true;
                view.printError(
                    "No sun left and no zombies — plants survived! You lose.");
            }
        }
    }

    /** بازیکن زامبی می‌گذارد */
    public void placeZombie(ZombieType type, int x, int y) {
        if (x < plantCols + 1 || x > COLS) {
            view.printError("Zombies must be placed right of the red line (col > "
                + plantCols + ").");
            return;
        }
        if (y < 1 || y > ROWS) {
            view.printError("Invalid row: " + y);
            return;
        }
        int cost = getZombieSunCost(type);
        if (!isZombieAvailable(type)) {
            view.printError(type.name()
                + " is not available in this minigame level.");
            return;
        }
        if (sunAmount < cost) {
            view.printError("Need " + cost + " sun. You have " + sunAmount + ".");
            return;
        }
        sunAmount -= cost;
        ZombieStats stats = ZombieDataRegistry.getInstance().getStats(type);
        int hp = stats != null ? stats.getHp() : 190;
        PlacedZombie z = new PlacedZombie(type, x, y, hp);
        zombies.add(z);
        System.out.println(ConsoleView.GREEN + "  🧟 "
            + type.name() + " placed at (" + x + "," + y
            + ") for " + cost + " sun!" + ConsoleView.RESET);
    }

    private int getZombieSunCost(ZombieType type) {
        switch (type) {
            case NORMAL:        return 75;
            case CONEHEAD:      return 125;
            case BUCKETHEAD:    return 175;
            case KNIGHT:        return 225;
            case IMP:           return 50;
            case GARGANTUAR:    return 300;
            case ALL_STAR:      return 200;
            case NEWSPAPER_ZOMBIE: return 100;
            case PARASOL_ZOMBIE:   return 150;
            case PROSPECTOR_ZOMBIE: return 175;
            case PIANIST_ZOMBIE: return 200;
            case TURQUOISE_ZOMBIE: return 250;
            case BARREL_ROLLER:  return 125;
            default:             return 100;
        }
    }

    private boolean isZombieAvailable(ZombieType type) {
        ZombieType[] available = getAvailableZombies();
        for (ZombieType t : available) {
            if (t == type) {
                return true;
            }
        }
        return false;
    }

    private ZombieType[] getAvailableZombies() {
        switch (level) {
            case 1: return AVAILABLE_ZOMBIES_L1;
            case 2: return AVAILABLE_ZOMBIES_L2;
            case 3: return AVAILABLE_ZOMBIES_L3;
            default: return AVAILABLE_ZOMBIES_L1;
        }
    }

    public void printBoard() {
        view.printHeader("🧟 I, Zombie — Level " + level
            + "  ☀ " + sunAmount);
        int totalBrains = 0;
        for (int b : brains) {
            totalBrains += b;
        }
        System.out.println("  Brains remaining: " + totalBrains
            + "   Zombies on field: "
            + zombies.stream().filter(PlacedZombie::isAlive).count());
        System.out.print("     ");
        for (int c = 1; c <= COLS; c++) {
            if (c == plantCols + 1) {
                System.out.print(ConsoleView.RED + "│" + ConsoleView.RESET);
            }
            System.out.printf("%-3d", c);
        }
        System.out.println();
        System.out.println("    " + "───".repeat(COLS + 1));
        for (int r = 1; r <= ROWS; r++) {
            System.out.printf(" %d │ ", r);
            for (int c = 1; c <= COLS; c++) {
                if (c == plantCols + 1) {
                    System.out.print(ConsoleView.RED + "│ " + ConsoleView.RESET);
                }
                System.out.print(getCell(c, r) + " ");
            }
            String brainStr = brains[r - 1] > 0
                ? ConsoleView.RED + "🧠" + ConsoleView.RESET : "  ";
            String sunProdStr = sunProducerHp[r - 1] > 0
                ? ConsoleView.YELLOW + "S" + ConsoleView.RESET : " ";
            System.out.println(brainStr + sunProdStr);
        }
        System.out.println("    " + "───".repeat(COLS + 1));
        printAvailableZombies();
    }

    private String getCell(int c, int r) {
        for (IZombiePlant p : plants) {
            if (p.getX() == c && p.getY() == r && p.isAlive()) {
                return ConsoleView.GREEN + "P" + ConsoleView.RESET;
            }
        }
        for (PlacedZombie z : zombies) {
            if ((int) z.getX() == c && z.getY() == r && z.isAlive()) {
                return ConsoleView.RED + "Z" + ConsoleView.RESET;
            }
        }
        return ".";
    }

    private void printAvailableZombies() {
        System.out.println(ConsoleView.CYAN
            + "  Available Zombies:" + ConsoleView.RESET);
        for (ZombieType t : getAvailableZombies()) {
            System.out.printf("    %-25s cost: %d sun%n",
                t.name(), getZombieSunCost(t));
        }
        System.out.println("  Use: place zombie -t <TYPE> -l (<col>, <row>)");
    }

    public boolean isGameOver() { return gameOver; }
    public boolean isWon() { return won; }
    public int getSunAmount() { return sunAmount; }
}
