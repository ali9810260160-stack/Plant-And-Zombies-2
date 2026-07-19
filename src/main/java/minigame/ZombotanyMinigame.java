package minigame;

import model.enums.PlantType;
import model.enums.ZombieType;
import util.RandomUtil;
import view.ConsoleView;

import java.util.ArrayList;
import java.util.List;

/**
 * مینی‌گیم Zombotany (بخش امتیازی ⭐).
 * زامبی‌ها توانایی گیاهان را دارند.
 * بقیه اجزا مثل مرحله عادی است.
 */
public class ZombotanyMinigame {

    public enum ZombotanyType {
        PEASHOOTER_ZOMBIE("ZomboPea", ZombieType.ZOMBOTANY_PEASHOOTER,
            "Fires peas leftward at your plants!", 190, 100, 0.185),
        WALLNUT_ZOMBIE("ZomboNut", ZombieType.ZOMBOTANY_WALLNUT,
            "Extremely tough — 4000 HP!", 4000, 100, 0.185),
        JALAPENO_ZOMBIE("ZomboJala", ZombieType.ZOMBOTANY_JALAPENO,
            "Burns entire row if alive for 10+ seconds!", 190, 100, 0.185),
        SQUASH_ZOMBIE("ZomboSquash", ZombieType.ZOMBOTANY_SQUASH,
            "Moves fast; destroys itself and the plant it hits!", 190, 500, 0.4);

        public final String displayName;
        public final ZombieType zombieType;
        public final String description;
        public final int hp;
        public final int dps;
        public final double speed;

        ZombotanyType(String n, ZombieType zt, String d,
                      int hp, int dps, double speed) {
            this.displayName = n;
            this.zombieType = zt;
            this.description = d;
            this.hp = hp;
            this.dps = dps;
            this.speed = speed;
        }
    }

    public static class ZombotanyZombie {
        private final ZombotanyType zombotanyType;
        private double x;
        private int y;
        private int hp;
        private boolean alive;
        private int specialTimer;
        private boolean jalapenoTriggered;

        public ZombotanyZombie(ZombotanyType type, double x, int y) {
            this.zombotanyType = type;
            this.x = x;
            this.y = y;
            this.hp = type.hp;
            this.alive = true;
            this.specialTimer = 0;
            this.jalapenoTriggered = false;
        }

        public void takeDamage(int dmg) {
            hp -= dmg;
            if (hp <= 0) {
                alive = false;
            }
        }

        public ZombotanyType getZombotanyType() { return zombotanyType; }
        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public int getY() { return y; }
        public int getHp() { return hp; }
        public boolean isAlive() { return alive; }
        public int getSpecialTimer() { return specialTimer; }
        public void tickSpecialTimer() { specialTimer++; }
        public boolean isJalapenoTriggered() { return jalapenoTriggered; }
        public void setJalapenoTriggered(boolean b) { jalapenoTriggered = b; }
    }

    public static class ZombotanyPlant {
        private final PlantType type;
        private int hp;
        private final int x;
        private final int y;
        private boolean alive;

        public ZombotanyPlant(PlantType type, int hp, int x, int y) {
            this.type = type;
            this.hp = hp;
            this.x = x;
            this.y = y;
            this.alive = true;
        }

        public void takeDamage(int dmg) {
            hp -= dmg;
            if (hp <= 0) {
                alive = false;
            }
        }

        public PlantType getType() { return type; }
        public int getHp() { return hp; }
        public int getX() { return x; }
        public int getY() { return y; }
        public boolean isAlive() { return alive; }
    }

    private final int level;
    private final ConsoleView view;
    private static final int ROWS = 5;
    private static final int COLS = 9;

    private List<ZombotanyZombie> zombies;
    private List<ZombotanyPlant> plants;
    private boolean[] lawnMowers;
    private List<PlantType> selectedPlayerPlants;
    private int totalTicks;
    private boolean gameOver;
    private boolean won;

    public ZombotanyMinigame(int level, ConsoleView view,
                              List<PlantType> playerSelectedPlants) {
        this.level = level;
        this.view = view;
        this.zombies = new ArrayList<>();
        this.plants = new ArrayList<>();
        this.lawnMowers = new boolean[ROWS];
        this.selectedPlayerPlants = playerSelectedPlants;
        this.totalTicks = 0;
        this.gameOver = false;
        this.won = false;
        for (int i = 0; i < ROWS; i++) {
            lawnMowers[i] = true;
        }
        spawnInitialZombies();
    }

    private void spawnInitialZombies() {
        ZombotanyType[] wave1 = { ZombotanyType.PEASHOOTER_ZOMBIE,
            ZombotanyType.WALLNUT_ZOMBIE };
        ZombotanyType[] wave2 = { ZombotanyType.PEASHOOTER_ZOMBIE,
            ZombotanyType.WALLNUT_ZOMBIE, ZombotanyType.JALAPENO_ZOMBIE };
        ZombotanyType[] wave3 = { ZombotanyType.PEASHOOTER_ZOMBIE,
            ZombotanyType.WALLNUT_ZOMBIE, ZombotanyType.JALAPENO_ZOMBIE,
            ZombotanyType.SQUASH_ZOMBIE };

        ZombotanyType[] pool = level == 1 ? wave1 : level == 2 ? wave2 : wave3;
        int count = 3 + level * 2;

        for (int i = 0; i < count; i++) {
            ZombotanyType t = pool[RandomUtil.nextInt(pool.length)];
            int row = RandomUtil.between(1, ROWS);
            zombies.add(new ZombotanyZombie(t, COLS, row));
        }
        printSpawnMessage();
    }

    private void printSpawnMessage() {
        System.out.println(ConsoleView.RED
            + "  🧟 Zombotany zombies incoming!" + ConsoleView.RESET);
        for (ZombotanyType t : ZombotanyType.values()) {
            System.out.println(ConsoleView.RED + "  ⚡ " + t.displayName
                + ": " + t.description + ConsoleView.RESET);
        }
    }

    public void tick() {
        totalTicks++;
        handleZombotanySpecials();
        moveZombies();
        checkLawnMowers();
        checkWin();
    }

    private void handleZombotanySpecials() {
        List<int[]> rowsToFire = new ArrayList<>();

        for (ZombotanyZombie z : zombies) {
            if (!z.isAlive()) {
                continue;
            }
            z.tickSpecialTimer();

            if (z.getZombotanyType() == ZombotanyType.PEASHOOTER_ZOMBIE
                    && z.getSpecialTimer() % 15 == 0) {
                firePeaAtPlant(z);
            }

            if (z.getZombotanyType() == ZombotanyType.JALAPENO_ZOMBIE
                    && z.getSpecialTimer() >= 100
                    && !z.isJalapenoTriggered()) {
                z.setJalapenoTriggered(true);
                burnEntireRow(z.getY());
                z.takeDamage(z.getHp());
            }

            if (z.getZombotanyType() == ZombotanyType.SQUASH_ZOMBIE) {
                handleSquash(z);
            }
        }
    }

    private void firePeaAtPlant(ZombotanyZombie zombie) {
        ZombotanyPlant target = findNearestPlantLeft(zombie);
        if (target == null) {
            return;
        }
        target.takeDamage(20);
        System.out.printf("  🟢 ZomboPea fires at %s at (%d,%d)! HP: %d%n",
            target.getType().name(), target.getX(), target.getY(),
            target.getHp());
        if (!target.isAlive()) {
            System.out.println(ConsoleView.YELLOW + "  🌿 Plant "
                + target.getType().name() + " destroyed by ZomboPea!"
                + ConsoleView.RESET);
        }
    }

    private ZombotanyPlant findNearestPlantLeft(ZombotanyZombie zombie) {
        ZombotanyPlant nearest = null;
        int minDist = Integer.MAX_VALUE;
        for (ZombotanyPlant p : plants) {
            if (!p.isAlive()) {
                continue;
            }
            if (p.getY() == zombie.getY() && p.getX() < (int) zombie.getX()) {
                int dist = (int) zombie.getX() - p.getX();
                if (dist < minDist) {
                    minDist = dist;
                    nearest = p;
                }
            }
        }
        return nearest;
    }

    private void burnEntireRow(int row) {
        System.out.println(ConsoleView.RED
            + "  🌶 ZomboJalapeno burns entire row " + row + "!"
            + ConsoleView.RESET);
        for (ZombotanyPlant p : plants) {
            if (p.getY() == row && p.isAlive()) {
                p.takeDamage(9999);
                System.out.println(ConsoleView.YELLOW + "  🌿 "
                    + p.getType().name() + " burned!" + ConsoleView.RESET);
            }
        }
    }

    private void handleSquash(ZombotanyZombie zombie) {
        ZombotanyPlant target = findNearestPlantLeft(zombie);
        if (target != null && Math.abs(target.getX() - zombie.getX()) < 1) {
            System.out.println(ConsoleView.RED + "  💥 ZomboSquash crushes "
                + target.getType().name() + " — both destroyed!"
                + ConsoleView.RESET);
            target.takeDamage(9999);
            zombie.takeDamage(zombie.getHp());
        }
    }

    private void moveZombies() {
        for (ZombotanyZombie z : zombies) {
            if (!z.isAlive()) {
                continue;
            }
            ZombotanyPlant blocked = findBlockingPlant(z);
            if (blocked != null) {
                blocked.takeDamage(z.getZombotanyType().dps / 10);
                if (!blocked.isAlive()) {
                    System.out.println(ConsoleView.YELLOW + "  🌿 Plant "
                        + blocked.getType().name()
                        + " eaten by " + z.getZombotanyType().displayName
                        + "!" + ConsoleView.RESET);
                }
            } else {
                z.setX(z.getX() - z.getZombotanyType().speed * 0.1);
            }
        }
        zombies.removeIf(z -> !z.isAlive());
    }

    private ZombotanyPlant findBlockingPlant(ZombotanyZombie z) {
        for (ZombotanyPlant p : plants) {
            if (!p.isAlive()) {
                continue;
            }
            if (p.getY() == z.getY()
                    && Math.abs(p.getX() - (int) z.getX()) < 1) {
                return p;
            }
        }
        return null;
    }

    private void checkLawnMowers() {
        for (ZombotanyZombie z : zombies) {
            if (!z.isAlive() || z.getX() > 0) {
                continue;
            }
            int rowIdx = z.getY() - 1;
            if (lawnMowers[rowIdx]) {
                lawnMowers[rowIdx] = false;
                killAllInRow(z.getY());
                System.out.println(ConsoleView.CYAN
                    + "  🚜 Lawn mower triggered in row "
                    + z.getY() + "!" + ConsoleView.RESET);
            } else {
                gameOver = true;
                view.printGameOver();
                return;
            }
        }
    }

    private void killAllInRow(int row) {
        for (ZombotanyZombie z : zombies) {
            if (z.getY() == row) {
                z.takeDamage(z.getHp());
            }
        }
    }

    private void checkWin() {
        if (zombies.stream().noneMatch(ZombotanyZombie::isAlive)) {
            won = true;
            gameOver = true;
            view.printGameWon();
        }
    }

    public void plantPlant(PlantType type, int x, int y) {
        if (!selectedPlayerPlants.contains(type)) {
            view.printError(type.name() + " not selected for this level.");
            return;
        }
        plants.add(new ZombotanyPlant(type, getPlantHp(type), x, y));
        System.out.println(ConsoleView.GREEN + "  🌱 "
            + type.name() + " planted at (" + x + "," + y + ")"
            + ConsoleView.RESET);
    }

    private int getPlantHp(PlantType type) {
        switch (type) {
            case WALL_NUT: case TALL_NUT: return 4000;
            case PUMPKIN:  return 4000;
            default:       return 300;
        }
    }

    public void printBoard() {
        view.printHeader("⚡ Zombotany — Level " + level);
        System.out.println(ConsoleView.RED
            + "  Zombies with plant powers!" + ConsoleView.RESET);
        System.out.print("     ");
        for (int c = 1; c <= COLS; c++) {
            System.out.printf("%-3d", c);
        }
        System.out.println();
        System.out.println("    " + "───".repeat(COLS));
        for (int r = 1; r <= ROWS; r++) {
            System.out.printf(" %d │ ", r);
            for (int c = 1; c <= COLS; c++) {
                System.out.print(getCell(c, r) + "  ");
            }
            String mower = lawnMowers[r - 1]
                ? ConsoleView.GREEN + "🚜" + ConsoleView.RESET
                : ConsoleView.RED + "✗" + ConsoleView.RESET;
            System.out.println(mower);
        }
        System.out.println("    " + "───".repeat(COLS));
        System.out.println(ConsoleView.YELLOW
            + "  Legend: 🟢=ZomboPea ⬜=ZomboNut 🌶=ZomboJala 💥=ZomboSquash"
            + ConsoleView.RESET);
    }

    private String getCell(int c, int r) {
        for (ZombotanyPlant p : plants) {
            if (p.getX() == c && p.getY() == r && p.isAlive()) {
                return ConsoleView.GREEN + "P" + ConsoleView.RESET;
            }
        }
        for (ZombotanyZombie z : zombies) {
            if ((int) z.getX() == c && z.getY() == r && z.isAlive()) {
                switch (z.getZombotanyType()) {
                    case PEASHOOTER_ZOMBIE: return ConsoleView.GREEN + "Z" + ConsoleView.RESET;
                    case WALLNUT_ZOMBIE:    return ConsoleView.YELLOW + "W" + ConsoleView.RESET;
                    case JALAPENO_ZOMBIE:   return ConsoleView.RED + "J" + ConsoleView.RESET;
                    case SQUASH_ZOMBIE:     return ConsoleView.MAGENTA + "S" + ConsoleView.RESET;
                    default:                return ConsoleView.RED + "Z" + ConsoleView.RESET;
                }
            }
        }
        return ".";
    }

    public boolean isGameOver() { return gameOver; }
    public boolean isWon() { return won; }
}
