package util;

import java.util.Random;

/**
 * ابزار تصادفی‌سازی برای بازی.
 * برای انتخاب ردیف، نوع خورشید، زامبی glowing و ... استفاده می‌شود.
 */
public class RandomUtil {

    private static final Random RANDOM = new Random();

    /**
     * یک عدد صحیح تصادفی در بازه [min, max] برمی‌گرداند.
     * @param min حداقل
     * @param max حداکثر
     * @return عدد تصادفی
     */
    public static int nextInt(int min, int max) { return 0; }

    /**
     * با احتمال مشخص true یا false برمی‌گرداند.
     * @param probability احتمال (0.0 تا 1.0)
     * @return true یا false
     */
    public static boolean chance(double probability) { return false; }

    /**
     * یک عنصر تصادفی از آرایه برمی‌گرداند.
     * @param array آرایه
     * @return عنصر تصادفی
     */
    public static <T> T pick(T[] array) { return null; }

    /**
     * سید random را تنظیم می‌کند (برای بازی امتیازی با الگوریتم یکسان).
     * @param seed سید
     */
    public static void setSeed(long seed) { RANDOM.setSeed(seed); }
}
