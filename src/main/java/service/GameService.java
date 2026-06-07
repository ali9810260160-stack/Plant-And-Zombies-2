package service;

import model.*;
import model.enums.PlantType;
import model.enums.ZombieType;

import java.util.List;

/**
 * سرویس اصلی منطق بازی.
 * مسئول پیش بردن زمان، کاشت گیاه، حمله زامبی، تولید خورشید و ...
 */
public class GameService {

    private final WaveService waveService;
    private final SunService sunService;
    private final ScoreService scoreService;

    public GameService(WaveService waveService, SunService sunService,
                       ScoreService scoreService) {
        this.waveService = waveService;
        this.sunService = sunService;
        this.scoreService = scoreService;
    }

    /**
     * بازی را n تیک جلو می‌برد.
     * @param session session جاری
     * @param ticks تعداد تیک
     */
    public void advanceTime(GameSession session, int ticks) { }

    /**
     * یک گیاه را روی نقشه می‌کارد.
     * @param session session جاری
     * @param type نوع گیاه
     * @param x ستون
     * @param y ردیف
     * @throws exception.GameException در صورت خطا
     */
    public void plantPlant(GameSession session, PlantType type, int x, int y) { }

    /**
     * گیاه را از روی نقشه برمی‌دارد.
     * @param session session جاری
     * @param x ستون
     * @param y ردیف
     */
    public void pluckPlant(GameSession session, int x, int y) { }

    /**
     * خورشید یک گیاه را برداشت می‌کند.
     * @param session session جاری
     * @param x ستون
     * @param y ردیف
     */
    public void collectSun(GameSession session, int x, int y) { }

    /**
     * یک خورشید در حال سقوط را برداشت می‌کند.
     * @param session session جاری
     * @param x ستون
     * @param y ردیف
     */
    public void collectFallingSun(GameSession session, int x, int y) { }

    /**
     * غذای گیاه را به گیاه موردنظر می‌دهد.
     * @param session session جاری
     * @param x ستون
     * @param y ردیف
     */
    public void feedPlant(GameSession session, int x, int y) { }

    /**
     * یک زامبی را به صورت cheat اضافه می‌کند.
     * @param session session جاری
     * @param type نوع زامبی
     * @param x ستون
     * @param y ردیف
     */
    public void spawnZombieCheat(GameSession session, ZombieType type, int x, int y) { }

    /**
     * تمام زامبیها را می‌کشد (nuke cheat).
     * @param session session جاری
     */
    public void releaseNuke(GameSession session) { }

    /**
     * cooldown همه گیاهان را حذف می‌کند (cheat).
     * @param session session جاری
     */
    public void removeCooldowns(GameSession session) { }

    /**
     * بررسی می‌کند آیا گیاه روی این خانه قابل کاشت است.
     * @param session session جاری
     * @param type نوع گیاه
     * @param x ستون
     * @param y ردیف
     * @return پیام خطا یا null اگر مجاز
     */
    public String canPlant(GameSession session, PlantType type, int x, int y) { return null; }

    /**
     * ماشین چمن‌زنی ردیف r را فعال می‌کند.
     * @param session session جاری
     * @param row ردیف
     */
    public void triggerLawnMower(GameSession session, int row) { }

    /**
     * session جدیدی برای یک مرحله ایجاد می‌کند.
     * @param level مرحله
     * @param selectedPlants گیاهان انتخابی
     * @param user کاربر
     * @return session آماده
     */
    public GameSession createSession(Level level, List<PlantType> selectedPlants,
                                     User user) { return null; }
}
