package com.pvz2.map;

import com.badlogic.gdx.Gdx;
import com.pvz2.model.Level;

/**
 * لایه اتصال بین GameScreen (فاز ۲) و {@link MapLoader}.
 *
 * <p>مسئولیت‌ها:
 * <ul>
 *   <li>ساخت {@link LevelMapConfig} از {@link Level} فعلی</li>
 *   <li>بارگذاری {@code world.tmx} با {@link MapLoader}</li>
 *   <li>اگر فایل نقشه هنوز طراحی نشده یا مشکلی در بارگذاری بود، به‌جای crash
 *       کردن بازی، {@code null} برمی‌گرداند — {@code GridRenderer} در این حالت
 *       به رندر رویه‌ای (مستطیل‌های رنگی فصل) برمی‌گردد، دقیقاً مثل رفتار
 *       {@code GameAssets} وقتی {@code pvz.assets} تنظیم نشده.</li>
 * </ul>
 */
public final class MapService {

    private MapService() {}

    /**
     * مسیر فایل TMX نسبت به پوشه assets.
     * قابل override با {@code -Dpvz.map=...} برای تست نقشه‌های دیگر.
     */
    public static final String DEFAULT_TMX_PATH =
            System.getProperty("pvz.map", "maps/world.tmx");

    /**
     * تلاش برای بارگذاری نقشه مرحله جاری.
     *
     * @param level مرحله جاری (از {@code GameFacade.get().getCurrentLevel()}) — می‌تواند null باشد
     * @return {@link GameMap} بارگذاری‌شده، یا {@code null} اگر {@code world.tmx} هنوز
     *         در {@code assets/maps/} وجود نداشته باشد یا پارس آن شکست بخورد
     */
    public static GameMap tryLoad(Level level) {
        try {
            LevelMapConfig config = LevelMapConfig.fromLevel(level);
            GameMap map = MapLoader.load(DEFAULT_TMX_PATH, config);
            Gdx.app.log("MapService", "world.tmx loaded — chapter="
                    + config.getChapter() + " specials=" + config.getSpecialIds()
                    + " minigame=" + config.getMinigameId());
            return map;
        } catch (Exception e) {
            // world.tmx هنوز طراحی نشده (یا لایه/آبجکتی طبق TILED_MAP_GUIDE.md نیست) —
            // GridRenderer به رندر رویه‌ای برمی‌گردد، بازی crash نمی‌کند.
            Gdx.app.log("MapService", "world.tmx not available yet — falling back to "
                    + "procedural grid rendering (" + e.getClass().getSimpleName()
                    + (e.getMessage() != null ? ": " + e.getMessage() : "") + ")");
            return null;
        }
    }
}
