package view;

import java.util.List;

/**
 * کلاس مرکزی چاپ خروجی به ترمینال.
 * از ANSI برای رنگ‌آمیزی و قاب‌بندی استفاده می‌شود.
 */
public class ConsoleView {

    // ANSI Color Codes
    public static final String RESET   = "\u001B[0m";
    public static final String BOLD    = "\u001B[1m";
    public static final String RED     = "\u001B[31m";
    public static final String GREEN   = "\u001B[32m";
    public static final String YELLOW  = "\u001B[33m";
    public static final String BLUE    = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN    = "\u001B[36m";
    public static final String WHITE   = "\u001B[37m";
    public static final String BG_DARK = "\u001B[40m";

    private static final String SEPARATOR =
            CYAN + "═══════════════════════════════════════════════════" + RESET;

    public void printSuccess(String message) {
        System.out.println(GREEN + "✔ " + message + RESET);
    }

    public void printError(String message) {
        System.out.println(RED + "✘ Error: " + message + RESET);
    }

    public void printInfo(String message) {
        System.out.println(CYAN + "ℹ " + message + RESET);
    }

    public void printWarning(String message) {
        System.out.println(YELLOW + "⚠ " + message + RESET);
    }

    public void printRaw(String message) {
        System.out.println(message);
    }

    public void printSeparator() {
        System.out.println(SEPARATOR);
    }

    public void printHeader(String title) {
        System.out.println(BOLD + CYAN);
        System.out.println("╔══════════════════════════════════════════════════╗");
        String padded = String.format("║  %-48s║", title);
        System.out.println(padded);
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.print(RESET);
    }

    public void printNumberedList(List<String> items) {
        for (int i = 0; i < items.size(); i++) {
            System.out.println(YELLOW + "  " + (i + 1) + ". " + RESET + items.get(i));
        }
    }

    public void printZombiesInfo(List<String> zombieInfoLines) {
        System.out.println(BOLD + RED + "\n🧟 Zombies on field:" + RESET);
        System.out.println(RED + "─────────────────────────────────────────" + RESET);
        for (String line : zombieInfoLines) {
            System.out.println(line);
        }
        System.out.println(RED + "─────────────────────────────────────────" + RESET);
    }

    public void printPlantsStatus(List<String> statusLines) {
        System.out.println(BOLD + GREEN + "\n🌱 Plants Status:" + RESET);
        System.out.println(GREEN + "─────────────────────────────────────────" + RESET);
        for (String line : statusLines) {
            System.out.println(line);
        }
        System.out.println(GREEN + "─────────────────────────────────────────" + RESET);
    }

    public void printWaveStarted(int waveNumber, boolean isFinal) {
        if (isFinal) {
            System.out.println(BOLD + RED
                    + "\n⚠️  THE FINAL WAVE HAS COME! ⚠️" + RESET);
        } else {
            System.out.println(BOLD + YELLOW
                    + "\n🌊 Wave " + waveNumber + " started." + RESET);
        }
    }

    public void printZombieSpawned(String type, int wave, int lane, int cost) {
        System.out.println(RED + "🧟 Zombie " + type
                + " spawned at wave " + wave
                + " in lane " + lane
                + " which costed " + cost + "." + RESET);
    }

    public void printZombieDead(String type, double x, int y) {
        System.out.println(GREEN + "💀 Zombie of type " + type
                + " is dead at (" + String.format("%.1f", x) + ", " + y + ")" + RESET);
    }

    public void printPlantDestroyed(String type, int x, int y) {
        System.out.println(YELLOW + "🌿 Plant " + type
                + " at (" + x + ", " + y + ") is destroyed." + RESET);
    }

    public void printPlantProducedSun(String plantType, int x, int y) {
        System.out.println(YELLOW + "☀ plant " + plantType
                + " produced a sun at (" + x + ", " + y + ")" + RESET);
    }

    public void printSunDropping(String type, int x, int y) {
        System.out.println(YELLOW + "☀ New " + type
                + " sun is dropping at position (" + x + ", " + y + ")" + RESET);
    }

    public void printSunLanded(int x, int y) {
        System.out.println(YELLOW + "☀ Sun reached the ground at position ("
                + x + ", " + y + ")" + RESET);
    }

    public void printZombieDropped(String item, int count) {
        String plural = item.equals("coin") ? "coins"
                : item.equals("diamond") ? "diamonds" : "pots";
        System.out.println(MAGENTA + "💎 A zombie dropeed a " + item
                + "; you have " + count + " " + plural + " now." + RESET);
    }

    public void printLawnMowerTriggered(int row, List<String> killed) {
        System.out.println(CYAN + "🚜 The lawn mower in the row "
                + row + " is triggered and killed these zombies:" + RESET);
        for (String z : killed) {
            System.out.println(CYAN + "   • " + z + RESET);
        }
    }

    public void printGameOver() {
        System.out.println(RED + BOLD);
        System.out.println("╔═══════════════════════════════════════╗");
        System.out.println("║   💀 The zombie ate your brain;       ║");
        System.out.println("║           LOSER!!!                    ║");
        System.out.println("╚═══════════════════════════════════════╝");
        System.out.print(RESET);
    }

    public void printGameWon() {
        System.out.println(GREEN + BOLD);
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║  🏆 Dear humanz, zis is not done yet;        ║");
        System.out.println("║     we will come back to eat your brainz,    ║");
        System.out.println("║                    humanz.                   ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.print(RESET);
    }

    public void printGlowingZombieDroppedFood(int currentCount) {
        System.out.println(MAGENTA + "✨ The glowing zombie dropped a plant food;"
                + " you have " + currentCount + " plant foods now." + RESET);
    }

    public void printLawnMowerGameOver() {
        printGameOver();
    }

    public void printSecurityQuestions() {
        System.out.println(BOLD + CYAN + "\n🔒 Security Questions:" + RESET);
        System.out.println(CYAN + "  1. What is the name of your first pet?" + RESET);
        System.out.println(CYAN + "  2. What is your mother's maiden name?" + RESET);
        System.out.println(CYAN + "  3. What was the name of your first school?" + RESET);
        System.out.println(CYAN + "  4. What is your favorite book?" + RESET);
        System.out.println(CYAN + "  5. What city were you born in?" + RESET);
    }

    public void printCurrentMenu(String menuName) {
        System.out.println(CYAN + "📍 Current menu: " + BOLD + menuName + RESET);
    }

    public void printWelcomeBanner() {
        System.out.println(BOLD + GREEN);
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║    🌿  Plants vs Zombies 2 — SUT Edition  🧟    ║");
        System.out.println("║         Advanced Programming Project 1404        ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println(RESET);
    }
}
