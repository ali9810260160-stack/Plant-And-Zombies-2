package com.pvz2;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.pvz2.graphics.ServiceLocator;
import com.pvz2.graphics.ScreenId;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.graphics.screens.*;
import com.pvz2.model.AppState;
import com.pvz2.model.enums.PlantType;

import java.util.List;

/**
 * نقطه ورود اصلی بازی — فاز ۲ گرافیک.
 *
 * <p>این فایل جایگزین نسخه CLI فاز ۱ می‌شود.
 * تمام منطق بازی (فاز ۱) از طریق {@link ServiceLocator} در دسترس است.
 *
 * <p>اجرا:
 * <pre>
 *   # بدون asset های بازی اصلی (حالت توسعه — رنگ‌های plain):
 *   ./gradlew lwjgl3:run
 *
 *   # با asset های بازی اصلی (انیمیشن کامل):
 *   ./gradlew lwjgl3:run --args="assets=/path/to/pvz2-extracted-assets"
 *   # یا:
 *   java -Dpvz.assets="/path/to/assets" -jar pvz2.jar
 * </pre>
 */
public class PVZApplication extends Game {

    // ─── پارامترهای مرحله جاری ───────────────────────────────────────────────
    private String       currentChapter  = "ANCIENT_EGYPT";
    private int          currentLevel    = 1;
    private List<PlantType> selectedPlants;

    @Override
    public void create() {
        // ۱. راه‌اندازی سرویس‌های فاز ۱
        ServiceLocator.init();

        // ۲. راه‌اندازی asset های گرافیکی (skin + libPVZ)
        GameAssets.init();

        // ۳. بررسی stay-logged-in
        boolean loggedIn = AppState.getInstance().isLoggedIn();
        goTo(loggedIn ? ScreenId.MAIN_MENU : ScreenId.REGISTER);
    }

    /** انتقال به صفحه مشخص — صفحه قبلی dispose می‌شود. */
    public void goTo(ScreenId id) {
        Screen next = buildScreen(id);
        if (next == null) return;
        Screen prev = getScreen();
        setScreen(next);
        if (prev != null) prev.dispose();
    }

    /** شروع مرحله بازی — پارامترها ذخیره، session ساخته، GameScreen باز می‌شود. */
    public void startGame(String chapter, int level, List<PlantType> plants) {
        this.currentChapter  = chapter;
        this.currentLevel    = level;
        this.selectedPlants  = plants;
        goTo(ScreenId.GAME);
    }

    /** پارامترهای صفحه انتخاب گیاه را ذخیره می‌کند (توسط AdventureScreen فراخوانی می‌شود). */
    public void setPlantSelectParams(String chapter, int level) {
        this.currentChapter = chapter;
        this.currentLevel   = level;
    }

    public String        getCurrentChapter()    { return currentChapter; }
    public int           getCurrentLevel()      { return currentLevel; }
    public List<PlantType> getSelectedPlants()  { return selectedPlants; }

    private Screen buildScreen(ScreenId id) {
        switch (id) {
            case REGISTER:    return new RegisterScreen(this);
            case LOGIN:       return new LoginScreen(this);
            case MAIN_MENU:   return new MainMenuScreen(this);
            case PROFILE:     return new ProfileScreen(this);
            case SETTINGS:    return new SettingsScreen(this);
            case NEWS:        return new NewsScreen(this);
            case ADVENTURE:   return new AdventureScreen(this);
            case PLANT_SELECT:return new PlantSelectScreen(this);
            case GAME:        return new GameScreen(this);
            case COLLECTION:  return new CollectionScreen(this);
            case GREENHOUSE:  return new GreenhouseScreen(this);
            case SHOP:        return new ShopScreen(this);
            case QUEST:       return new QuestScreen(this);
            case LEADERBOARD: return new LeaderboardScreen(this);
            default:          return null;
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        GameAssets.getInstance().dispose();
    }
}
