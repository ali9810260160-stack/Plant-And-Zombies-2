package util;

import java.util.Random;

/**
 * ابزار تصادفی‌سازی.
 */
public class RandomUtil {

    private static final Random RANDOM = new Random();

    public static int nextInt(int bound) {
        return RANDOM.nextInt(bound);
    }

    public static double nextDouble() {
        return RANDOM.nextDouble();
    }

    public static boolean chance(double probability) {
        return RANDOM.nextDouble() < probability;
    }

    public static int between(int min, int max) {
        return min + RANDOM.nextInt(max - min + 1);
    }

    /** یک seed ثابت برای بازی امتیازی روزانه */
    public static Random getDailyRandom(long seed) {
        return new Random(seed);
    }
}
