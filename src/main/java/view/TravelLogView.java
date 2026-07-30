package view;

/**
 * کلاس View مخصوص منوی Travel Log و کوئست‌ها.
 */
public class TravelLogView {

    /**
     * چاپ یک ردیف کوئست در صفحه Travel Log.
     *
     * @param status     نشانگر وضعیت (مثلاً "[DONE]" یا "[TODO]") با رنگ‌بندی
     * @param priority   اولویت کوئست
     * @param nameFA     نام کوئست
     * @param desc       توضیح کوئست (با target جایگزین‌شده)
     * @param reward     مقدار جایزه
     * @param rewardType نوع جایزه (مثلاً COIN یا GEM)
     */
    public void printQuestRow(String status, String priority,
                               String nameFA, String desc,
                               int reward, String rewardType) {
        System.out.println("  " + status + " ["
                + priority + "] "
                + ConsoleView.BOLD + nameFA + ConsoleView.RESET);
        System.out.println("         " + desc);
        System.out.println("         Reward: "
                + ConsoleView.YELLOW + reward + " " + rewardType
                + ConsoleView.RESET);
    }
}
