package view;

/**
 * کلاس View مخصوص منوهای بازی (GAME, PLANT_SELECT, IN_GAME).
 */
public class GameView {

    /**
     * چاپ یک ردیف در لیست انتخاب گیاه — همه گیاهان موجود در بازی.
     */
    public void printAllPlantsForSelectRow(String type, int cost, String status) {
        System.out.printf("  %-30s  Cost:%-5d  %s%n", type, cost, status);
    }

    /**
     * چاپ وضعیت یک گیاه روی نقشه.
     */
    public void printPlantStatus(int x, int y, String type,
                                  int curHp, int maxHp, int cost,
                                  String cooldown, boolean frozen) {
        System.out.printf("  (%d,%d) %-20s  HP:%d/%d  Cost:%-4d  CD:%s%s%n",
                x, y, type, curHp, maxHp, cost, cooldown,
                frozen ? ConsoleView.CYAN + "  [FROZEN]" + ConsoleView.RESET : "");
    }

    /**
     * چاپ وضعیت یک زامبی روی نقشه.
     */
    public void printZombieStatus(String type, int hp, double x, int y, String armors) {
        System.out.printf(ConsoleView.RED
                        + "  Zombie: %-20s  HP:%d  X:%.2f  Y:%d  Armor:%s%n"
                        + ConsoleView.RESET,
                type, hp, x, y, armors);
    }

    /**
     * چاپ موقعیت زامبی (برای نمایش جزئیات).
     */
    public void printZombiePosition(double x, int y) {
        System.out.printf("  position: %.1f, %d%n", x, y);
    }

    /**
     * چاپ یک افکت فعال روی زامبی.
     */
    public void printZombieEffect(String effectName, double seconds) {
        System.out.printf("    %s: %.1fs%n", effectName, seconds);
    }
}
