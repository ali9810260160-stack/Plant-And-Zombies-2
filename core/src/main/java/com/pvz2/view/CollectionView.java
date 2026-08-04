package com.pvz2.view;

/**
 * کلاس View مخصوص منوی کلکسیون گیاهان و زامبی‌ها.
 */
public class CollectionView {

    /**
     * چاپ یک ردیف از لیست همه گیاهان با فرمت‌بندی.
     */
    public void printPlantRow(int index, String type, int cost, int hp, String desc) {
        System.out.printf(ConsoleView.GREEN + "  %2d. %-30s"
                + ConsoleView.RESET
                + " Cost: %-4d HP: %-5d  %s%n",
                index, type, cost, hp, desc);
    }

    /**
     * چاپ یک ردیف از لیست همه زامبی‌ها با فرمت‌بندی.
     */
    public void printZombieRow(int index, String type, int hp, int cost, String desc) {
        System.out.printf(ConsoleView.RED + "  %2d. %-30s"
                + ConsoleView.RESET
                + " HP: %-5d Cost: %-4d  %s%n",
                index, type, hp, cost, desc);
    }
}
