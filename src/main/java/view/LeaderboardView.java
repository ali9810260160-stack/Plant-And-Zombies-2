package view;

/**
 * کلاس View مخصوص منوی لیدربورد.
 */
public class LeaderboardView {

    /**
     * چاپ هدر جدول لیدربورد.
     */
    public void printTableHeader() {
        System.out.printf(ConsoleView.BOLD + ConsoleView.CYAN
                + "  %-4s %-15s %-12s %-10s %-10s %-12s %-10s%n"
                + ConsoleView.RESET,
                "Rank", "Username", "Level", "Minigames",
                "DailyQ", "RegularQ", "MeoPoint");
    }

    /**
     * چاپ یک ردیف کاربر در جدول لیدربورد.
     */
    public void printRow(int rank, String username, String level,
                          int minigames, int dailyQ, int regularQ, long meoPoint) {
        System.out.printf("  %-4d %-15s %-12s %-10d %-10d %-12d %-10d%n",
                rank, username, level, minigames, dailyQ, regularQ, meoPoint);
    }
}
