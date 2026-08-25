package com.pvz2;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.GameFacade;
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

    // ─── اورلیِ سراسری (فاز ۳): Pop-upهای شبکه که روی هر صفحه‌ای ظاهر می‌شوند ──
    private Stage overlayStage;
    private InputProcessor prevInput;

    // ─── مالتی‌پلیر (فاز ۳): جلسه‌ی VERSUS در انتظارِ باز شدنِ GameScreen ──────────
    private com.pvz2.graphics.net.VersusSession pendingVersus;
    /** Couch Play (بونوس): دونفره‌ی محلی روی یک دستگاه، بدونِ شبکه. */
    private boolean pendingCouch;

    @Override
    public void create() {
        // ۱. راه‌اندازی سرویس‌های فاز ۱
        ServiceLocator.init();

        // ۲. راه‌اندازی asset های گرافیکی (skin + libPVZ)
        GameAssets.init();

        // اورلیِ سراسری برای Pop-upهای شبکه (دعوت/مسابقه/اطلاع).
        overlayStage = new Stage(new FitViewport(
                GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));

        // ۳. اتصال به سرور (فاز ۳) — آنلاین-اول با fallback محلی.
        boolean serverUp = com.pvz2.graphics.net.NetClient.get().tryConnect();

        // ۳.۵ هوکِ همگام‌سازیِ پروفایل: هر ذخیره‌ی محلیِ کاربرِ فعلی، در حالتِ
        //     آنلاین به سرور هم push می‌شود (سکه/الماس/پیشرفت مستقل از دستگاه).
        wireProfileSync();

        // ۳.۶ سرویسِ مالتی‌پلیر: گوش‌دادن به دعوت/مسابقه/پایانِ مسابقه (push).
        com.pvz2.graphics.net.MultiplayerService.get().init(this);

        // ۴. auto-login:
        //    آنلاین → با توکنِ «مرا به خاطر بسپار» به‌صورت بی‌درز resume می‌شود.
        //    آفلاین → از روی users.jsonِ محلی (کش) بازیابی می‌شود (رفتارِ فاز ۲).
        boolean loggedIn = false;
        if (serverUp) {
            loggedIn = GameFacade.get().tryResumeOnline();
        } else {
            com.pvz2.model.User stay =
                    ServiceLocator.getInstance().getUserRepository().loadStayLoggedInUser();
            if (stay != null) {
                AppState.getInstance().setCurrentUser(stay);
                ServiceLocator.getInstance().getQuestService().loadForUser(stay);
                applyUserSettings(stay);
                loggedIn = true;
            }
        }
        ScreenId dest = loggedIn ? ScreenId.MAIN_MENU : ScreenId.WELCOME;
        // صفحه‌ی لودینگِ ۱۰ ثانیه‌ای در اولِ باز شدنِ بازی؛ سپس به مقصد می‌رود.
        setScreen(new com.pvz2.graphics.screens.LoadingScreen(this, dest));
        updateMusicFor(dest);
    }

    /**
     * Install the profile-sync hook: whenever the current user's data is saved
     * locally while online, push it to the server (fire-and-forget) so the
     * account stays device-independent. Credential fields are ignored server-side.
     */
    private void wireProfileSync() {
        final com.pvz2.repository.UserRepository repo =
                ServiceLocator.getInstance().getUserRepository();
        repo.setRemoteSync(user -> {
            com.pvz2.graphics.net.NetClient net = com.pvz2.graphics.net.NetClient.get();
            if (!net.isConnected() || net.token() == null) return;
            com.pvz2.model.User cur = AppState.getInstance().getCurrentUser();
            if (cur == null || user == null
                    || !cur.getUsername().equals(user.getUsername())) return; // only the live account
            com.pvz2.shared.protocol.payload.ProfileSaveRequest req =
                    new com.pvz2.shared.protocol.payload.ProfileSaveRequest();
            req.token = net.token();
            req.user = com.pvz2.graphics.net.UserCodec.toJson(repo, user);
            net.send(com.pvz2.shared.protocol.MessageType.SAVE_PROFILE_REQ, req);
        });
    }

    /**
     * Pump the network layer once per frame (on the render thread) so queued
     * server pushes are dispatched to listeners safely, then render the screen.
     */
    @Override
    public void render() {
        com.pvz2.graphics.net.NetClient.get().update();
        super.render();
        if (overlayStage != null) {
            overlayStage.act(Gdx.graphics.getDeltaTime());
            overlayStage.draw();
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (overlayStage != null) overlayStage.getViewport().update(width, height, true);
    }

    /**
     * Show a modal actor on the global overlay (e.g. a network invite popup),
     * capturing input until {@link #hideOverlay()}. Works over any screen.
     */
    public void showOverlay(Actor content) {
        if (overlayStage == null) return;
        prevInput = Gdx.input.getInputProcessor();
        overlayStage.clear();
        overlayStage.addActor(content);
        Gdx.input.setInputProcessor(overlayStage);
    }

    /** Dismiss the global overlay and restore input to the underlying screen. */
    public void hideOverlay() {
        if (overlayStage != null) overlayStage.clear();
        if (prevInput != null) { Gdx.input.setInputProcessor(prevInput); prevInput = null; }
    }

    /**
     * اعمالِ تنظیماتِ ذخیره‌شده‌ی یک کاربر روی سیستم‌های runtime — پس از
     * لاگین یا auto-login. {@link com.pvz2.model.GameSettings} را از روی کاربر
     * پر می‌کند و به {@link com.pvz2.graphics.util.GameConfig} و SoundManager
     * آینه می‌کند.
     */
    public static void applyUserSettings(com.pvz2.model.User u) {
        if (u == null) return;
        com.pvz2.model.GameSettings gs = com.pvz2.model.GameSettings.getInstance();
        gs.applyFromUser(u);
        com.pvz2.graphics.util.GameConfig.showGrid = gs.isShowGrid();
        com.pvz2.graphics.util.GameConfig.musicVolume = gs.getMasterVolume();
        com.pvz2.graphics.audio.SoundManager.get().applySettings();
    }

    /**
     * شروعِ یک مسابقه‌ی دونفره‌ی VERSUS (فاز ۳): یک جلسه‌ی VERSUS می‌سازد و
     * {@link GameScreen} را باز می‌کند؛ صفحه {@code pendingVersus} را برمی‌دارد و
     * نقشِ میزبان/مهمان را می‌فهمد.
     */
    public void startVersus(com.pvz2.graphics.net.VersusSession vs) {
        this.pendingVersus = vs;
        this.currentChapter = "ANCIENT_EGYPT";
        this.currentLevel = 1;
        java.util.List<PlantType> plants = java.util.Arrays.asList(
                PlantType.PEASHOOTER, PlantType.SUNFLOWER, PlantType.WALL_NUT,
                PlantType.SNOW_PEA, PlantType.POTATO_MINE, PlantType.CHERRY_BOMB);
        this.selectedPlants = plants;
        String err = GameFacade.get().startMinigame("ANCIENT_EGYPT", 1,
                com.pvz2.model.enums.LevelType.VERSUS, plants);
        if (err != null) Gdx.app.error("PVZApplication", "startVersus failed: " + err);
        goTo(ScreenId.GAME);
    }

    /** صفحه‌ی بازی جلسه‌ی VERSUS در انتظار را برمی‌دارد (null اگر تک‌نفره باشد). */
    public com.pvz2.graphics.net.VersusSession consumePendingVersus() {
        com.pvz2.graphics.net.VersusSession v = pendingVersus;
        pendingVersus = null;
        return v;
    }

    /**
     * Couch Play (بونوس): بازیِ دونفره‌ی محلی روی یک دستگاه بدونِ شبکه — گیاه‌کار
     * با ماوس، زامبی‌گذار با کیبورد. یک مرحله‌ی VERSUS به‌صورتِ محلی اجرا می‌شود.
     */
    public void startCouchPlay() {
        this.pendingCouch = true;
        this.currentChapter = "ANCIENT_EGYPT";
        this.currentLevel = 1;
        java.util.List<PlantType> plants = java.util.Arrays.asList(
                PlantType.PEASHOOTER, PlantType.SUNFLOWER, PlantType.WALL_NUT,
                PlantType.SNOW_PEA, PlantType.POTATO_MINE, PlantType.CHERRY_BOMB);
        this.selectedPlants = plants;
        String err = GameFacade.get().startMinigame("ANCIENT_EGYPT", 1,
                com.pvz2.model.enums.LevelType.VERSUS, plants);
        if (err != null) Gdx.app.error("PVZApplication", "startCouchPlay failed: " + err);
        goTo(ScreenId.GAME);
    }

    public boolean consumePendingCouch() {
        boolean c = pendingCouch;
        pendingCouch = false;
        return c;
    }

    /** انتقال به صفحه مشخص — صفحه قبلی dispose می‌شود. */
    public void goTo(ScreenId id) {
        Screen next = buildScreen(id);
        if (next == null) return;
        Screen prev = getScreen();
        setScreen(next);
        if (prev != null) prev.dispose();
        updateMusicFor(id);
    }

    /** انتخابِ موسیقیِ پس‌زمینه بر اساسِ صفحه‌ی مقصد (FQ2). */
    private void updateMusicFor(ScreenId id) {
        com.pvz2.graphics.audio.SoundManager sound = com.pvz2.graphics.audio.SoundManager.get();
        if (id == ScreenId.GAME) {
            boolean boss = false;
            try {
                com.pvz2.model.GameSession s = GameFacade.get().getCurrentSession();
                boss = s != null && s.getLevel() != null
                        && s.getLevel().getLevelType() == com.pvz2.model.enums.LevelType.BOSS;
            } catch (Exception ignored) { }
            sound.playMusic(boss
                    ? com.pvz2.graphics.audio.SoundManager.MUSIC_BOSS
                    : com.pvz2.graphics.audio.SoundManager.chapterMusic(currentChapter));
        } else {
            sound.playMusic(com.pvz2.graphics.audio.SoundManager.MUSIC_MENU);
        }
    }

    /** شروع مرحله بازی — پارامترها ذخیره، session ساخته، GameScreen باز می‌شود. */
    public void startGame(String chapter, int level, List<PlantType> plants) {
        this.currentChapter  = chapter;
        this.currentLevel    = level;
        this.selectedPlants  = plants;
        // ساخت GameSession واقعی فاز ۱ (Level + GameController.buildLevel + GameService.createSession)
        // نکته: قبلاً این فراخوانی جایی انجام نمی‌شد؛ در نتیجه GameScreen با یک session
        // خالی باز می‌شد. اینجا تنها نقطه‌ای است که همه مسیرهای «شروع مرحله»
        // (PlantSelectScreen، restart از GameScreen، retry از صفحه باخت) از آن رد می‌شوند.
        String error = GameFacade.get().startLevel(chapter, level, plants);
        if (error != null) {
            Gdx.app.error("PVZApplication", "startLevel failed: " + error);
        }
        goTo(ScreenId.GAME);
    }

    /**
     * شروع یک مینی‌گیم (کوزه‌شکنی/بولینگ/من‌زامبی) با نوع مرحله‌ی اجباری.
     * برخلاف {@link #startGame}، از انتخاب گیاه عبور می‌کند (گیاهان از داخل خود
     * مینی‌گیم تأمین می‌شوند) و مستقیماً GameScreen را باز می‌کند.
     */
    public void startMinigame(String chapter, int level,
                              com.pvz2.model.enums.LevelType type,
                              List<PlantType> plants) {
        this.currentChapter = chapter;
        this.currentLevel   = level;
        this.selectedPlants = plants != null ? plants : new java.util.ArrayList<>();
        String error = GameFacade.get().startMinigame(chapter, level, type, this.selectedPlants);
        if (error != null) {
            Gdx.app.error("PVZApplication", "startMinigame failed: " + error);
        }
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
            case WELCOME:     return new WelcomeScreen(this);
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
            case BEGHOULED:   return new BeghouledScreen(this);
            case MULTIPLAYER: return new LobbyScreen(this);
            default:          return null;
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (overlayStage != null) overlayStage.dispose();
        com.pvz2.graphics.net.NetClient.get().disconnect();
        com.pvz2.graphics.audio.SoundManager.get().dispose();
        GameAssets.getInstance().dispose();
        com.pvz2.graphics.assets.CollectionAssets.getInstance().dispose();
    }
}
