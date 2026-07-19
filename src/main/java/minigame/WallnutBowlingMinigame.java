package minigame;

import model.enums.PlantType;
import model.enums.ZombieType;
import model.zombies.ZombieDataRegistry;
import model.zombies.ZombieStats;
import util.RandomUtil;
import view.ConsoleView;

import java.util.ArrayList;
import java.util.List;

/**
 * مینی‌گیم بولینگ گردویی.
 * گردوها پس از کاشت مانند توپ بولینگ حرکت می‌کنند.
 * برخورد با زامبی → ۴۵ درجه چرخش مسیر.
 * برخورد با BIG_WALLNUT → مستقیم و له کردن.
 */
public class WallnutBowlingMinigame {

    public static class BowlingBall {
        private double x;
        private int y;
        private int dirX;
        private int dirY;
        private PlantType type;
        private boolean active;
        private boolean exploded;

        public BowlingBall(PlantType type, double x, int y) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.dirX = 1;
            this.dirY = 0;
            this.active = true;
            this.exploded = false;
        }

        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public int getDirX() { return dirX; }
        public void setDirX(int dx) { this.dirX = dx; }
        public int getDirY() { return dirY; }
        public void setDirY(int dy) { this.dirY = dy; }
        public PlantType getType() { return type; }
        public boolean isActive() { return active; }
        public void setActive(boolean a) { this.active = a; }
        public boolean isExploded() { return exploded; }
        public void setExploded(boolean e) { this.exploded = e; }
        public int getDamage() {
            switch (type) {
                case WALLNUT_BOWLING:       return 190;
                case EXPLODE_O_NUT_BOWLING: return 180;
                case BIG_WALLNUT:           return 200;
                default:                    return 190;
            }
        }
    }

    public static class BowlingZombie {
        private double x;
        private int y;
        private ZombieType type;
        private int hp;
        private boolean alive;

        public BowlingZombie(ZombieType type, double x, int y, int hp) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.hp = hp;
            this.alive = true;
        }

        public ZombieType getType() { return type; }
        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public int getY() { return y; }
        public int getHp() { return hp; }
        public void takeDamage(int dmg) {
            hp -= dmg;
            if (hp <= 0) {
                alive = false;
            }
        }
        public boolean isAlive() { return alive; }
    }

    private final int level;
    private final ConsoleView view;
    private final int rows;
    private final int cols;
    private final int deadlineCol;
    private List<BowlingZombie> zombies;
    private List<BowlingBall> balls;
    private List<PlantType> conveyorQueue;
    private int conveyorTimer;
    private boolean gameOver;
    private boolean won;
    private int totalKilled;
    private int targetKills;
    private int sunAmount;

    public WallnutBowlingMinigame(int level, ConsoleView view) {
        this.level = level;
        this.view = view;
        this.rows = 5;
        this.cols = 9;
        this.deadlineCol = 4;
        this.zombies = new ArrayList<>();
        this.balls = new ArrayList<>();
        this.conveyorQueue = new ArrayList<>();
        this.conveyorTimer = 0;
        this.gameOver = false;
        this.won = false;
        this.totalKilled = 0;
        this.targetKills = getTargetKills(level);
        this.sunAmount = 0;
        initZombies();
        fillConveyorQueue();
    }

    private int getTargetKills(int lvl) {
        switch (lvl) {
            case 1: return 10;
            case 2: return 20;
            case 3: return 30;
            default: return 10;
        }
    }

    private void initZombies() {
        int count = 5 + level * 5;
        ZombieType[] pool = {
            ZombieType.NORMAL, ZombieType.CONEHEAD,
            ZombieType.BUCKETHEAD, ZombieType.NEWSPAPER_ZOMBIE
        };
        for (int i = 0; i < count; i++) {
            ZombieType t = pool[RandomUtil.nextInt(pool.length)];
            ZombieStats stats = ZombieDataRegistry.getInstance().getStats(t);
            int hp = stats != null ? stats.getHp() + (stats.getArmors().values()
                    .stream().mapToInt(Integer::intValue).sum()) : 190;
            int x = cols - RandomUtil.nextInt(3);
            int y = RandomUtil.between(1, rows);
            zombies.add(new BowlingZombie(t, x, y, hp));
        }
    }

    private void fillConveyorQueue() {
        PlantType[] bowlingTypes = {
            PlantType.WALLNUT_BOWLING,
            PlantType.WALLNUT_BOWLING,
            PlantType.WALLNUT_BOWLING,
            PlantType.EXPLODE_O_NUT_BOWLING,
            PlantType.BIG_WALLNUT
        };
        for (int i = 0; i < 15 + level * 5; i++) {
            conveyorQueue.add(bowlingTypes[RandomUtil.nextInt(bowlingTypes.length)]);
        }
    }

    /** برای پیشروی زمان در بازی */
    public void tick() {
        conveyorTimer++;
        if (conveyorTimer >= 120 && !conveyorQueue.isEmpty()) {
            PlantType next = conveyorQueue.remove(0);
            System.out.println(ConsoleView.CYAN + "  🎳 Conveyor: "
                + next.name() + " is ready to place!" + ConsoleView.RESET);
            conveyorTimer = 0;
        }
        moveBalls();
        moveZombies();
        checkWin();
    }

    private void moveBalls() {
        List<BowlingBall> toRemove = new ArrayList<>();
        for (BowlingBall ball : balls) {
            if (!ball.isActive()) {
                toRemove.add(ball);
                continue;
            }
            ball.setX(ball.getX() + ball.getDirX() * 0.5);
            int newY = ball.getY() + ball.getDirY();
            if (newY < 1 || newY > rows) {
                ball.setDirY(-ball.getDirY());
                newY = ball.getY();
            }
            ball.setY(newY);
            if (ball.getX() > cols + 1 || ball.getX() < 0) {
                ball.setActive(false);
                toRemove.add(ball);
                continue;
            }
            checkBallHitsZombie(ball);
        }
        balls.removeAll(toRemove);
    }

    private void checkBallHitsZombie(BowlingBall ball) {
        for (BowlingZombie z : zombies) {
            if (!z.isAlive()) {
                continue;
            }
            if (Math.abs(z.getX() - ball.getX()) < 0.8
                    && z.getY() == ball.getY()) {
                handleBallHit(ball, z);
                break;
            }
        }
    }

    private void handleBallHit(BowlingBall ball, BowlingZombie zombie) {
        switch (ball.getType()) {
            case WALLNUT_BOWLING:
                zombie.takeDamage(ball.getDamage());
                rotateBall45(ball);
                printHit(ball, zombie);
                break;
            case EXPLODE_O_NUT_BOWLING:
                if (!ball.isExploded()) {
                    explodeArea(ball);
                    ball.setExploded(true);
                    ball.setActive(false);
                }
                break;
            case BIG_WALLNUT:
                zombie.takeDamage(ball.getDamage());
                printHit(ball, zombie);
                break;
            default:
                break;
        }
        if (!zombie.isAlive()) {
            System.out.println(ConsoleView.GREEN + "  💀 "
                + zombie.getType().name() + " crushed!" + ConsoleView.RESET);
            totalKilled++;
        }
    }

    private void rotateBall45(BowlingBall ball) {
        int oldDx = ball.getDirX();
        int oldDy = ball.getDirY();
        if (oldDy == 0) {
            ball.setDirY(RandomUtil.chance(0.5) ? 1 : -1);
            ball.setDirX(oldDx);
        } else {
            ball.setDirX(oldDy);
            ball.setDirY(0);
        }
        System.out.println(ConsoleView.YELLOW
            + "  🎳 Ball deflects 45° after hit!" + ConsoleView.RESET);
    }

    private void explodeArea(BowlingBall ball) {
        System.out.println(ConsoleView.RED
            + "  💥 EXPLODE-O-NUT explodes in 3x3 area!" + ConsoleView.RESET);
        int cx = (int) ball.getX();
        int cy = ball.getY();
        for (BowlingZombie z : zombies) {
            if (Math.abs(z.getX() - cx) <= 1 && Math.abs(z.getY() - cy) <= 1) {
                z.takeDamage(180);
                if (!z.isAlive()) {
                    totalKilled++;
                    System.out.println(ConsoleView.GREEN
                        + "  💀 " + z.getType().name()
                        + " destroyed by explosion!" + ConsoleView.RESET);
                }
            }
        }
    }

    private void printHit(BowlingBall ball, BowlingZombie zombie) {
        System.out.printf("  🎳 %s hit %s (HP: %d → %d)%n",
            ball.getType().name(), zombie.getType().name(),
            zombie.getHp() + ball.getDamage(), zombie.getHp());
    }

    private void moveZombies() {
        for (BowlingZombie z : zombies) {
            if (!z.isAlive()) {
                continue;
            }
            z.setX(z.getX() - 0.1);
            if (z.getX() <= deadlineCol) {
                gameOver = true;
                view.printError(
                    "A zombie crossed the red line! You lose!");
                return;
            }
        }
    }

    /** کاربر یک گردو می‌گذارد */
    public void placeBowling(PlantType type, int x, int y) {
        if (x > deadlineCol) {
            view.printError("You can only place walnut bowling between columns 1 and "
                + deadlineCol + ".");
            return;
        }
        if (y < 1 || y > rows) {
            view.printError("Invalid row: " + y);
            return;
        }
        BowlingBall ball = new BowlingBall(type, x, y);
        balls.add(ball);
        System.out.println(ConsoleView.GREEN + "  🎳 " + type.name()
            + " placed at (" + x + "," + y + ") — rolling!" + ConsoleView.RESET);
        for (int t = 0; t < 20; t++) {
            tick();
            if (!ball.isActive() || gameOver) {
                break;
            }
        }
        printBoard();
    }

    private void checkWin() {
        long alive = zombies.stream().filter(BowlingZombie::isAlive).count();
        if (alive == 0 && !conveyorQueue.isEmpty()) {
            return;
        }
        if (totalKilled >= targetKills || alive == 0) {
            won = true;
            gameOver = true;
            view.printGameWon();
            System.out.println(ConsoleView.GREEN
                + "  Crushed " + totalKilled + " zombies!" + ConsoleView.RESET);
        }
    }

    public void printBoard() {
        view.printHeader("🎳 Wallnut Bowling — Level " + level
            + "  Killed: " + totalKilled + "/" + targetKills);
        System.out.println("    Red line at column " + deadlineCol);
        System.out.print("     ");
        for (int c = 1; c <= cols; c++) {
            System.out.printf("%-4d", c);
        }
        System.out.println();
        System.out.println("    " + "────".repeat(cols));
        for (int r = 1; r <= rows; r++) {
            System.out.printf(" %d │ ", r);
            for (int c = 1; c <= cols; c++) {
                if (c == deadlineCol) {
                    System.out.print(ConsoleView.RED + "│" + ConsoleView.RESET);
                }
                String cell = getCellDisplay(c, r);
                System.out.print(cell);
            }
            System.out.println();
        }
        System.out.println("    " + "────".repeat(cols));
        System.out.println(ConsoleView.CYAN + "  Conveyor queue: "
            + conveyorQueue.size() + " balls remaining" + ConsoleView.RESET);
        System.out.println("  Use: place bowling -t <TYPE> -l (<col>, <row>)");
        System.out.println("  Types: WALLNUT_BOWLING | EXPLODE_O_NUT_BOWLING | BIG_WALLNUT");
    }

    private String getCellDisplay(int c, int r) {
        for (BowlingBall b : balls) {
            if ((int) b.getX() == c && b.getY() == r && b.isActive()) {
                return ConsoleView.YELLOW + "🎳  " + ConsoleView.RESET;
            }
        }
        for (BowlingZombie z : zombies) {
            if ((int) z.getX() == c && z.getY() == r && z.isAlive()) {
                return ConsoleView.RED + "Z   " + ConsoleView.RESET;
            }
        }
        return "    ";
    }

    public boolean isGameOver() { return gameOver; }
    public boolean isWon() { return won; }
    public int getTotalKilled() { return totalKilled; }
}
