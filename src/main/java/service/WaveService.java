package service;

import model.GameSession;
import model.Wave;
import model.enums.ChapterType;
import model.enums.ZombieType;
import model.zombies.Zombie;

import java.util.List;

/**
 * سرویس مدیریت امواج زامبی.
 * مسئول تولید، زمان‌بندی و spawn زامبی‌ها در هر موج.
 */
public class WaveService {

    /**
     * لیست امواج یک مرحله را بر اساس سختی اولیه می‌سازد.
     * هر موج 25% سخت‌تر از قبلی، موج آخر 2 برابر موج قبل.
     * @param initialDifficulty سختی اولیه
     * @param waveCount تعداد امواج
     * @param difficultyLevel سطح سختی کاربر (1-5)
     * @return لیست امواج
     */
    public List<Wave> generateWaves(int initialDifficulty, int waveCount,
                                    int difficultyLevel) { return null; }

    /**
     * زامبی‌های یک موج را به صورت تصادفی انتخاب می‌کند.
     * مجموع waveCost باید برابر سختی موج باشد.
     * @param wave موج
     * @param chapter فصل (برای انتخاب زامبی‌های مجاز)
     * @return لیست زامبی‌های تولیدشده
     */
    public List<Zombie> spawnZombiesForWave(Wave wave, ChapterType chapter) { return null; }

    /**
     * بررسی می‌کند آیا موج بعدی باید شروع شود (75% HP از دست رفته).
     * @param session session جاری
     * @return true اگر باید موج بعد شروع شود
     */
    public boolean shouldStartNextWave(GameSession session) { return false; }

    /**
     * موج بعدی را شروع می‌کند.
     * @param session session جاری
     */
    public void startNextWave(GameSession session) { }

    /**
     * یک زامبی تصادفی با توجه به سختی باقیمانده موج انتخاب می‌کند.
     * @param remainingCost بودجه باقیمانده
     * @param allowedTypes انواع مجاز در این فصل
     * @return نوع زامبی انتخاب‌شده
     */
    public ZombieType selectRandomZombieType(int remainingCost,
                                             ZombieType[] allowedTypes) { return null; }

    /**
     * ردیف تصادفی برای spawn زامبی انتخاب می‌کند.
     * @param mapRows تعداد ردیف‌ها
     * @return شماره ردیف (1-based)
     */
    public int selectRandomLane(int mapRows) { return 0; }
}
