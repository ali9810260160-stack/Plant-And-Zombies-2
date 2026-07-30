package view;

/**
 * کلاس View مخصوص منوی پروفایل و تنظیمات.
 */
public class ProfileView {

    /**
     * چاپ اثرات سطح دشواری.
     *
     * @param level سطح دشواری (1 تا 5)
     * @param mult  ضریب محاسبه‌شده
     */
    public void printDifficultyEffects(int level, double mult) {
        System.out.printf("  Zombie HP multiplier:     %.2fx%n", mult);
        System.out.printf("  Zombie damage multiplier: %.2fx%n", mult);
        System.out.printf("  Wave cost multiplier:     %.2fx%n", 3.0 / level);
        System.out.printf("  Sun drop rate:            %.2fx%n", 3.0 / level);
        System.out.printf("  Game speed:               %.2fx%n", mult);
    }
}
