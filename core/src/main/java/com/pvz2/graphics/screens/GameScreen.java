package com.pvz2.graphics.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.pvz2.PVZApplication;
import com.pvz2.graphics.*;
import com.pvz2.graphics.actors.*;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.graphics.renderer.GameRenderer;
import com.pvz2.graphics.util.GameConfig;
import com.pvz2.graphics.util.GameCoords;
import com.pvz2.model.Level;
import com.pvz2.model.enums.LevelType;
import com.pvz2.model.enums.PlantType;
import com.pvz2.model.enums.ZombieType;

import java.util.*;
import java.util.List;

/**
 * صفحه اصلی بازی.
 *
 * <p>حلقه بازی: هر ۰.۱ ثانیه واقعی = ۱ تیک (سرعت ۱×).
 * <p>تعامل کاربر: hover → آفتاب | کلیک → کاشت/بیلچه/غذا
 * <p>تمام مراحل ویژه (Conveyor, TimedWar, DeadLine, …) پشتیبانی می‌شود.
 */
public class GameScreen extends BaseScreen {

    private enum CursorMode { NONE, PLANTING, SHOVEL, PLANT_FOOD }

    // ─── Game loop ────────────────────────────────────────────────────────────
    private static final float TICK_INTERVAL = 1f / GameConstants.TICKS_PER_SECOND;
    private float tickAccum;

    // ─── Rendering ────────────────────────────────────────────────────────────
    private GameRenderer       renderer;
    private OrthographicCamera camera;
    private FitViewport        gameViewport;
    private Stage              hudStage;
    private GameStateSnapshot  snap;

    // ─── Seed Bank ────────────────────────────────────────────────────────────
    private SeedBankActor seedBank;
    /** true اگر مرحله نوار کناری یا کوزه‌شکنی باشد — seed bank از snap.conveyorQueue پر می‌شود */
    private boolean conveyorMode;
    /** true فقط در مرحله کوزه‌شکنی — کلیک روی کوزه آن را می‌شکند */
    private boolean vasebreakerMode;

    // ─── مینی‌گیم Beghouled (match-3 روی مپ) ─────────────────────────────────────
    private boolean beghouledMode;
    /** خانه‌ی انتخاب‌شده برای جابه‌جایی (۱-based؛ ۰=هیچ). */
    private int begSelCol, begSelRow;
    /** دکمه‌های ارتقا + هزینه‌شان (برای فعال/غیرفعال‌سازی بر اساس خورشید). */
    private final java.util.List<com.badlogic.gdx.scenes.scene2d.ui.TextButton> begUpgradeBtns
            = new java.util.ArrayList<>();
    private final java.util.List<Integer> begUpgradeCosts = new java.util.ArrayList<>();

    // ─── مینی‌گیم من زامبی ─────────────────────────────────────────────────────
    private boolean            izombieMode;
    private ZombieBankActor    zombieBank;
    private ZombieType         placingZombie;
    private int                hoveredRow;
    private Label              cursorZombieLabel;
    /** آخرین صف نوار که در UI نمایش داده شد — برای rebuild فقط هنگام تغییر */
    private List<PlantType> lastConveyor = new ArrayList<>();
    /** لیبل وضعیت مرحله ویژه (نبرد زماندار / از دست نده) — بالای وسط صفحه */
    private Label specialStatusLabel;

    // ─── HUD widgets ─────────────────────────────────────────────────────────
    private Label sunLabel, foodLabel, meoLabel;
    private PauseOverlay pauseOverlay;
    private Button speedBtn;
    private Label  speedLabel;
    private int speedIndex = 0;

    // ─── Interaction state ────────────────────────────────────────────────────
    private CursorMode cursorMode = CursorMode.NONE;
    private PlantType  plantingType;
    private boolean    paused;

    // ─── نشانگرِ موس: انیمیشنِ گیاه / آیکنِ ابزار که همراهِ موس حرکت می‌کند ──────────
    private PamIdleActor cursorPlant;
    private PlantType    cursorPlantType;   // نوعِ گیاهی که اکنون روی نشانگر است
    private com.badlogic.gdx.scenes.scene2d.ui.Image cursorTool;
    private com.badlogic.gdx.scenes.scene2d.utils.Drawable shovelDrawable, plantfoodDrawable;

    // ─── Meopoints popup tracking ─────────────────────────────────────────────
    private long lastMeoPoints;
    /** برای جلوگیری از نمایش مکرر دیالوگ برد/باخت (هر فریم). */
    private boolean endDialogShown;
    /** آخرین شماره‌ی موج که دیده شد — برای پخشِ صدای آغازِ موج (BR5/FQ2). */
    private int lastWaveNumber = 0;
    // ─── اعلانِ نکرومنسی (Dark Ages) و جزر/مدِ ساحل (Big Wave Beach) ────────────
    private int lastNecroCount = -1;
    private int lastWaterCount = -1;
    private int tideDir = 0; // -1 = در حالِ عقب‌نشینیِ آب، +1 = در حالِ بالا آمدن
    /** دیالوگِ آغازِ مرحله — بازی تا بسته‌شدنِ آن متوقف می‌ماند. */
    private LevelStartDialog startDialog;

    // ─── مالتی‌پلیر VERSUS (فاز ۳) ──────────────────────────────────────────────
    private com.pvz2.graphics.net.VersusSession versus;   // null اگر تک‌نفره
    private boolean versusEndShown;

    // ─── Couch Play (بونوس): دونفره‌ی محلی، زامبی با کیبورد ──────────────────────
    private boolean couch;
    private int couchSelIndex = -1;   // ایندکسِ زامبیِ انتخاب‌شده‌ی بازیکنِ کیبورد
    private boolean couchEndShown;
    private Label couchInfo;

    public GameScreen(PVZApplication game) { super(game); }

    private boolean isVersusGuest() { return versus != null && versus.isGuest(); }
    private boolean isVersusHost()  { return versus != null && versus.isHost(); }

    // ─────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void show() {
        // جلسه‌ی VERSUS در انتظار را بردار (قبل از ساختِ HUD تا نوارِ صحیح ساخته شود).
        versus = game.consumePendingVersus();
        couch = game.consumePendingCouch();
        setupRenderer();
        // snapshot را قبل از ساخت HUD بساز تا نوع مرحله (conveyor / plant-what-you-get)
        // هنگام ساخت seed bank و UI مخصوص مرحله در دسترس باشد.
        snap = facade().buildSnapshot();
        setupHudStage();
        setupInput();
        if (versus != null) {
            // مالتی‌پلیر: بدونِ گفت‌وگوی مقدمه؛ با MATCH_START شروع می‌شود.
            versus.begin();
            paused = false;
        } else if (couch) {
            // Couch Play: بدونِ مقدمه، بلافاصله شروع می‌شود.
            paused = false;
        } else {
            showIntroSequence();
        }
    }

    /**
     * توالیِ آغازِ مرحله: ابتدا گفت‌وگوی ان‌پی‌سی (BX2 — فقط مراحلِ داستانی)، سپس
     * دیالوگِ مأموریت (BJ2). بازی تا پایانِ هر دو متوقف می‌ماند.
     */
    private void showIntroSequence() {
        Level level = GameFacade.get().getCurrentSession().getLevel();
        paused = true;
        if (level != null && NpcScripts.shouldShow(level.getLevelType())) {
            Skin skin = GameAssets.getInstance().getSkin();
            List<NpcDialogOverlay.Line> script = NpcScripts.forLevel(
                    level.getChapter().toString(), level.getLevelType(),
                    level.getLevelNumber());
            NpcDialogOverlay npc = new NpcDialogOverlay(skin, script,
                    this::showLevelStartDialog);
            npc.show(hudStage);
        } else {
            showLevelStartDialog();
        }
    }

    /** دیالوگِ آغازِ مرحله (BJ2) — بازی را متوقف می‌کند تا کاربر «Let's Go» را بزند. */
    private void showLevelStartDialog() {
        Level level = GameFacade.get().getCurrentSession().getLevel();
        if (level == null) return;
        Skin skin = GameAssets.getInstance().getSkin();
        paused = true;
        startDialog = new LevelStartDialog(
                skin, level.getLevelType(), level.getChapter().toString(),
                level.getLevelNumber(), () -> {
            paused = false;
            if (startDialog != null) { startDialog.remove(); startDialog = null; }
        });
        startDialog.show(hudStage);
    }

    private void setupRenderer() {
        camera = new OrthographicCamera();
        gameViewport = new FitViewport(
                GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT, camera);
        camera.position.set(GameConstants.VIEWPORT_WIDTH * 0.5f,
                GameConstants.VIEWPORT_HEIGHT * 0.5f, 0);
        camera.update();
        String chapter = GameFacade.get().getCurrentSession().getLevel().getChapter().toString();
        Level level = GameFacade.get().getCurrentSession().getLevel();
        renderer = new GameRenderer(chapter, level );
    }

    private void setupHudStage() {
        hudStage = new Stage(new FitViewport(
                GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Skin skin = GameAssets.getInstance().getSkin();
        buildSeedBank(skin);
        buildHudButtons(skin);
        buildSpecialLevelUI(skin);
        pauseOverlay = new PauseOverlay(skin, buildPauseListener());
        if (snap != null) pauseOverlay.setObjective(objectiveFor(snap.levelType));
        if (versus != null) {
            buildReactionBar(skin);
            versus.setOnReaction(this::onIncomingReaction);
        }
        if (couch) buildCouchHint(skin);
    }

    /** راهنمای کیبوردِ بازیکنِ زامبی در Couch Play + خورشیدِ زامبی. */
    private void buildCouchHint(Skin skin) {
        couchInfo = new Label("", skin);
        couchInfo.setColor(new Color(0.6f, 1f, 0.5f, 1f));
        couchInfo.setPosition(12, GameConstants.VIEWPORT_HEIGHT - 40);
        hudStage.addActor(couchInfo);
    }

    private void updateCouchHint() {
        if (couchInfo == null || snap == null) return;
        StringBuilder sb = new StringBuilder("ZOMBIE (keyboard):  ");
        String[] rowKeys = {"Q", "W", "E", "R", "T"};
        for (int i = 0; i < snap.izombieRoster.size(); i++) {
            String name = snap.izombieRoster.get(i).type.toUpperCase();
            sb.append(couchSelIndex == i ? "[" + (i + 1) + ":" + name + "] " : (i + 1) + ":" + name + " ");
        }
        sb.append("  place row: Q W E R T   Zombie Sun: ").append(snap.versusZombieSun);
        couchInfo.setText(sb.toString());
    }

    // ─── واکنش‌های حین بازی (فاز ۳) ─────────────────────────────────────────────

    /** نوارِ واکنش‌ها (۳ متن + ۳ ایموجی + ۳ استیکر) پایینِ صفحه در مالتی‌پلیر. */
    private void buildReactionBar(Skin skin) {
        com.badlogic.gdx.scenes.scene2d.ui.Table bar =
                new com.badlogic.gdx.scenes.scene2d.ui.Table();
        bar.defaults().pad(3);
        for (String t : com.pvz2.graphics.net.ReactionCatalog.TEXTS) {
            bar.add(reactionButton(skin, t, com.pvz2.graphics.net.ReactionCatalog.KIND_TEXT, t))
                    .height(34);
        }
        for (String e : com.pvz2.graphics.net.ReactionCatalog.EMOJIS) {
            bar.add(reactionButton(skin, e, com.pvz2.graphics.net.ReactionCatalog.KIND_EMOJI, e))
                    .width(46).height(34);
        }
        for (String s : com.pvz2.graphics.net.ReactionCatalog.STICKERS) {
            bar.add(reactionButton(skin, s, com.pvz2.graphics.net.ReactionCatalog.KIND_STICKER, s))
                    .width(46).height(34);
        }
        bar.pack();
        bar.setPosition((GameConstants.VIEWPORT_WIDTH - bar.getWidth()) / 2f, 6);
        hudStage.addActor(bar);
    }

    private com.badlogic.gdx.scenes.scene2d.ui.TextButton reactionButton(
            Skin skin, String label, String kind, String value) {
        com.badlogic.gdx.scenes.scene2d.ui.TextButton b =
                new com.badlogic.gdx.scenes.scene2d.ui.TextButton(label, skin, "brown");
        b.addListener(new ClickListener() {
            @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent e, float x, float y) {
                if (versus != null) versus.sendReaction(kind, value);
            }
        });
        return b;
    }

    /** واکنشِ دریافتی از حریف را در گوشه‌ی صفحه نمایش می‌دهد (اکتورِ متحرک). */
    private void onIncomingReaction(com.pvz2.shared.protocol.payload.ReactionEvent re) {
        if (re == null || hudStage == null) return;
        Skin skin = GameAssets.getInstance().getSkin();
        String from = versus != null && GameFacade.get() != null
                ? opponentName() : null;
        com.pvz2.graphics.actors.ReactionActor actor =
                new com.pvz2.graphics.actors.ReactionActor(skin, re.kind, re.value, from);
        // گوشه‌ی بالا-راستِ صفحه‌ی حریف.
        actor.setPosition(GameConstants.VIEWPORT_WIDTH - actor.getWidth() - 24,
                GameConstants.VIEWPORT_HEIGHT - actor.getHeight() - 90);
        hudStage.addActor(actor);
    }

    private String opponentName() {
        com.pvz2.shared.protocol.payload.MatchFoundEvent m =
                com.pvz2.graphics.net.MultiplayerService.get().currentMatch();
        return m != null ? m.opponentUsername : null;
    }

    /** جمله‌ی کوتاهِ هدفِ مرحله برای نمایش در پنجره‌ی توقف. */
    private String objectiveFor(LevelType type) {
        if (type == null) return "Don't let the zombies reach your house!";
        switch (type) {
            case SAVE_OUR_SEEDS:   return "Protect the endangered plants!";
            case LOVE_YOUR_PLANTS: return "Don't let the zombies trample the flowers!";
            case TIMED_WAR:        return "Survive until the timer runs out!";
            case DEAD_LINE:        return "Never let a zombie cross the forbidden line!";
            case CONVEYOR_BELT:    return "Use the plants the conveyor gives you!";
            case VASEBREAKER:      return "Break every vase to win!";
            case WALLNUT_BOWLING:  return "Bowl the wall-nuts into the zombies!";
            case I_ZOMBIE:         return "Eat all the brains to win!";
            case BEGHOULED:        return "Swap adjacent plants to make matches of 3+!";
            case BOSS:             return "Defeat the Zomboss!";
            default:               return "Don't let the zombies reach your house!";
        }
    }

    private void buildSeedBank(Skin skin) {
        // VERSUS: مهمان (زامبی‌گذار) نوارِ زامبی می‌گیرد؛ میزبان (گیاه‌کار) نوارِ بذرِ عادی.
        izombieMode = (snap != null && snap.levelType == LevelType.I_ZOMBIE)
                || isVersusGuest();
        if (izombieMode) { buildZombieBank(skin); return; }
        // VERSUS میزبان مثلِ مرحله‌ی عادی گیاه می‌کارد → نوارِ بذرِ استاندارد پایین.
        // Beghouled: نوارِ بذر ندارد — بازیکن گیاهانِ روی زمین را جابه‌جا می‌کند،
        // و با خورشیدِ ترکیب‌ها می‌تواند گیاهان را ارتقا دهد.
        beghouledMode = snap != null && snap.levelType == LevelType.BEGHOULED;
        if (beghouledMode) { buildBeghouledUpgrades(skin); return; }
        vasebreakerMode = snap != null && snap.levelType == LevelType.VASEBREAKER;
        // نوار کناری، کوزه‌شکنی و بولینگ همگی از صفِ رایگانِ کارت‌ها استفاده می‌کنند
        conveyorMode = snap != null && (snap.levelType == LevelType.CONVEYOR_BELT
                || snap.levelType == LevelType.WALLNUT_BOWLING || vasebreakerMode);

        if (conveyorMode) {
            // نوار کناری/کوزه‌شکنی: بدون گیاهان از پیش انتخاب‌شده؛ از صف پر می‌شود
            seedBank = new SeedBankActor(new ArrayList<>(),
                    new LinkedHashMap<>(), skin);
            // کوزه‌شکنی از منطق صفِ رایگان استفاده می‌کند ولی نوار نقاله ندارد
            if (vasebreakerMode) seedBank.setBeltVisible(false);
        } else {
            List<PlantType> plants    = game.getSelectedPlants();
            Map<PlantType, Integer> costs = loadPlantCosts(plants);
            seedBank = new SeedBankActor(plants, costs, skin);
        }
        // ستونِ عمودی در لبه‌ی چپ؛ Yِ گروه طوری که بالای ستون نزدیکِ زیرِ نوارِ HUD باشد.
        seedBank.setPosition(6, GameConstants.TOP_BAR_Y - SeedBankActor.MAX_SLOTS * (SeedBankActor.V_CARD_H + SeedBankActor.V_PAD));
        seedBank.setOnPlantSelected(type -> {
            cursorMode   = type != null ? CursorMode.PLANTING : CursorMode.NONE;
            plantingType = type;
        });
        hudStage.addActor(seedBank);
    }

    // ─── Beghouled: پنلِ ارتقای گیاهان ─────────────────────────────────────────

    private void buildBeghouledUpgrades(Skin skin) {
        begUpgradeBtns.clear();
        begUpgradeCosts.clear();
        com.pvz2.model.BeghouledState.Upgrade[] ups = com.pvz2.model.BeghouledState.UPGRADES;
        // ردیفِ افقی در پایینِ صفحه (زیرِ گرید که آزاد است) — مثلِ تصویرِ مرجع.
        float w = 168f, h = 44f, pad = 10f;
        float total = ups.length * w + (ups.length - 1) * pad;
        float x0 = (GameConstants.VIEWPORT_WIDTH - total) * 0.5f;
        float y  = 8f;
        for (int i = 0; i < ups.length; i++) {
            final com.pvz2.model.BeghouledState.Upgrade u = ups[i];
            com.badlogic.gdx.scenes.scene2d.ui.TextButton b =
                    new com.badlogic.gdx.scenes.scene2d.ui.TextButton(
                            u.label + "\n(" + u.cost + "☀)", skin, "green");
            b.setBounds(x0 + i * (w + pad), y, w, h);
            b.getLabel().setFontScale(0.66f);
            b.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
                @Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent e,
                                              float sx, float sy) {
                    if (b.isDisabled()) return;
                    String err = facade().beghouledUpgrade(u.from, u.to);
                    if (err != null) showToast(ToastActor.error(err));
                    else showToast(ToastActor.info("Upgraded!"));
                }
            });
            hudStage.addActor(b);
            begUpgradeBtns.add(b);
            begUpgradeCosts.add(u.cost);
        }
    }

    /** فعال/غیرفعال‌سازیِ دکمه‌های ارتقا بر اساسِ خورشیدِ فعلی. */
    private void refreshBeghouledUpgrades() {
        if (snap == null) return;
        int sun = snap.sunCount;
        for (int i = 0; i < begUpgradeBtns.size(); i++) {
            boolean ok = sun >= begUpgradeCosts.get(i);
            com.badlogic.gdx.scenes.scene2d.ui.TextButton b = begUpgradeBtns.get(i);
            b.setDisabled(!ok);
            b.setColor(ok ? Color.WHITE : new Color(0.6f, 0.6f, 0.6f, 1f));
        }
    }

    // ─── من زامبی: نوار کارت‌های زامبی ──────────────────────────────────────────

    private void buildZombieBank(Skin skin) {
        List<ZombieType> roster = new ArrayList<>();
        List<Integer> costs = new ArrayList<>();
        if (snap != null) {
            for (GameStateSnapshot.ZombieCardInfo ci : snap.izombieRoster) {
                try {
                    roster.add(ZombieType.valueOf(ci.type.toUpperCase()));
                    costs.add(ci.cost);
                } catch (IllegalArgumentException ignored) { }
            }
        }
        zombieBank = new ZombieBankActor(roster, costs, skin);
        // ستونِ عمودی در لبه‌ی چپ — دقیقاً مثلِ نوارِ بذرِ گیاهان.
        float columnH = roster.size() * (ZombieBankActor.V_CARD_H + ZombieBankActor.V_PAD);
        zombieBank.setPosition(6, GameConstants.TOP_BAR_Y - columnH);
        zombieBank.setOnZombieSelected(type -> placingZombie = type);
        hudStage.addActor(zombieBank);

        cursorZombieLabel = new Label("", skin);
        cursorZombieLabel.setColor(new Color(0.6f, 1f, 0.4f, 1f));
        cursorZombieLabel.setVisible(false);
        hudStage.addActor(cursorZombieLabel);
    }

    private void refreshZombieBank() {
        if (zombieBank == null || snap == null) return;
        zombieBank.updateCards(snap.izombieRoster);
    }

    /** روشن‌سازی ردیف + نشانگرِ زامبی روی اشاره‌گر (EB3/EC3). */
    private void updateIZombieHud() {
        if (!izombieMode) return;
        renderer.setHighlightRow(placingZombie != null ? hoveredRow : 0);
        if (cursorZombieLabel == null) return;
        if (placingZombie == null) { cursorZombieLabel.setVisible(false); return; }
        Vector2 v = hudStage.getViewport().unproject(
                new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        cursorZombieLabel.setText("🧟 " + placingZombie.name());
        cursorZombieLabel.setPosition(v.x + 14, v.y - 6);
        cursorZombieLabel.setVisible(true);
    }

    private void doPlaceZombie(int col, int row) {
        if (placingZombie == null) return;
        if (isVersusGuest()) {
            // مهمان: ورودی را به میزبان می‌فرستد؛ نتیجه در snapshotِ بعدی دیده می‌شود.
            versus.sendPlaceZombie(placingZombie.name(), col, row);
            renderer.showTextPopup(GameCoords.toScreenX(col), GameCoords.toScreenY(row),
                    "🧟", new Color(0.6f, 1f, 0.4f, 1f));
            zombieBank.clearSelection();
            placingZombie = null;
            return;
        }
        String err = facade().placeZombie(placingZombie, col, row);
        if (err != null) {
            showToast(ToastActor.error(err));
        } else {
            renderer.showTextPopup(GameCoords.toScreenX(col), GameCoords.toScreenY(row),
                    "🧟", new Color(0.6f, 1f, 0.4f, 1f));
            zombieBank.clearSelection();
            placingZombie = null;
        }
    }

    /** هزینه صفر برای همه گیاهان نوار (رایگان کاشته می‌شوند). */
    private Map<PlantType, Integer> zeroCosts(List<PlantType> queue) {
        Map<PlantType, Integer> m = new LinkedHashMap<>();
        for (PlantType t : queue) m.put(t, 0);
        return m;
    }

    private Map<PlantType, Integer> loadPlantCosts(List<PlantType> plants) {
        Map<PlantType, Integer> costs = new LinkedHashMap<>();
        // PHASE1: PlantDataRegistry.getInstance().getStats(type).getSunCost()
        for (PlantType p : plants) {
            try {
                com.pvz2.model.plants.PlantStats stats =
                        com.pvz2.model.plants.PlantDataRegistry.getInstance().getStats(p);
                costs.put(p, stats != null ? stats.getSunCost() : 100);
            } catch (Exception e) {
                costs.put(p, 100);
            }
        }
        return costs;
    }

    private void buildHudButtons(Skin skin) {
        // ── بالا-راست: فقط سرعت (2x) + توقف ──
        float sz = 52f, gap = 6f;
        float rowY = GameConstants.TOP_BAR_Y + 40f;
        float pauseX = GameConstants.VIEWPORT_WIDTH - sz - 8f;
        float speedX = pauseX - (sz + gap);

        speedBtn = imageButton("Exports/in game/2x.png", skin);
        speedBtn.setBounds(speedX, rowY, sz, sz);
        speedBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { cycleSpeed(); }
        });
        hudStage.addActor(speedBtn);
        speedLabel = new Label("1x", skin);
        speedLabel.setColor(Color.WHITE);
        speedLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        speedLabel.setBounds(speedX, rowY - 2, sz, 18);
        speedLabel.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        hudStage.addActor(speedLabel);

        Button pauseBtn = imageButton("Exports/in game/pause_button.png", skin);
        pauseBtn.setBounds(pauseX, rowY, sz, sz);
        pauseBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { togglePause(); }
        });
        hudStage.addActor(pauseBtn);

        // ── بیلچه: گوشه‌ی پایین-راست ──
        Button shovelBtn = imageButton("Exports/in game/shovel_button.png", skin);
        shovelBtn.setBounds(GameConstants.VIEWPORT_WIDTH - 74f, 12f, 64f, 64f);
        shovelBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { toggleMode(CursorMode.SHOVEL); }
        });
        hudStage.addActor(shovelBtn);

        // ── غذای گیاه: ناحیه‌ی نامرئیِ قابل‌کلیک روی بانکِ پایین-چپ (HudRenderer آن را می‌کشد) ──
        Button foodHotspot = new Button(new Button.ButtonStyle());
        foodHotspot.setBounds(10f, 8f, 190f, 66f);
        foodHotspot.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { toggleMode(CursorMode.PLANT_FOOD); }
        });
        hudStage.addActor(foodHotspot);

        buildDebugButtons(skin, GameConstants.TOP_BAR_Y + 12);
    }

    /** دکمه‌ی تصویری از یک PNGِ لوکال؛ اگر asset نبود، به دکمه‌ی متنیِ ساده برمی‌گردد. */
    private Button imageButton(String localPath, Skin skin) {
        GameAssets a = GameAssets.getInstance();
        com.badlogic.gdx.graphics.g2d.TextureRegion r = a.local(localPath);
        if (r != null && r != a.getWhiteRegion()) {
            return new ImageButton(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(r));
        }
        return new TextButton("•", skin);
    }

    private void buildDebugButtons(Skin skin, float btnY) {
        if (!GameConfig.debugMode) return;
        TextButton addSun = new TextButton("+☀", skin, "brown");
        addSun.setBounds(4, GameConstants.SB_H + 4, 72, 30);
        addSun.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                facade().cheatAddSun(150);
            }
        });
        hudStage.addActor(addSun);

        TextButton addFood = new TextButton("+🌿", skin, "brown");
        addFood.setBounds(82, GameConstants.SB_H + 4, 72, 30);
        addFood.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                facade().cheatAddPlantFood();
            }
        });
        hudStage.addActor(addFood);

        TextButton nuke = new TextButton("☢ Nuke", skin, "brown");
        nuke.setBounds(162, GameConstants.SB_H + 4, 80, 30);
        nuke.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                facade().cheatNuke();
            }
        });
        hudStage.addActor(nuke);
    }

    /**
     * کلیدهای کیبوردِ تقلب (DC3/AV3) — فقط وقتی حالتِ دیباگ روشن است.
     * C = +۱۰۰۰ سکه، G = +۱۰۰ الماس، X = +۲۵۰ خورشید، B = پُر کردنِ غذای گیاه.
     */
    private void handleCheatKey(int key) {
        if (!GameConfig.debugMode) return;
        switch (key) {
            case Input.Keys.C:
                facade().cheatAddCurrency(1000, false);
                showToast(ToastActor.success("+1000 coins"));
                break;
            case Input.Keys.G:
                facade().cheatAddCurrency(100, true);
                showToast(ToastActor.success("+100 gems"));
                break;
            case Input.Keys.X:
                facade().cheatAddSun(250);
                showToast(ToastActor.success("+250 sun"));
                break;
            case Input.Keys.B:
                facade().cheatAddPlantFood();
                showToast(ToastActor.success("Plant food filled"));
                break;
            default:
                break;
        }
    }

    private void buildSpecialLevelUI(Skin skin) {
        LevelType lt = (snap != null && snap.levelType != null)
                ? snap.levelType : LevelType.NORMAL;

        // لیبل وضعیت مرحله ویژه (نبرد زماندار / از دست نده) — بالای وسط صفحه
        specialStatusLabel = new Label("", skin);
        specialStatusLabel.setColor(Color.WHITE);
        specialStatusLabel.setBounds(GameConstants.VIEWPORT_WIDTH * 0.5f - 180,
                GameConstants.VIEWPORT_HEIGHT - 46, 360, 30);
        specialStatusLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        hudStage.addActor(specialStatusLabel);

        if (lt == LevelType.PLANT_WHAT_YOU_GET) {
            TextButton startBtn = new TextButton("▶ Start Waves", skin, "green");
            startBtn.setBounds(GameConstants.VIEWPORT_WIDTH * 0.5f - 100,
                    GameConstants.G_Y + 20, 200, 50);
            startBtn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    facade().startZombieWaves();
                    startBtn.remove();
                }
            });
            hudStage.addActor(startBtn);
        }
    }

    private void setupInput() {
        Gdx.input.setInputProcessor(new InputMultiplexer(
                hudStage, buildGameInputAdapter()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Game Loop
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void render(float delta) {
        // پمپاژِ صفِ بارگذاریِ async ی asset ها (PAM/atlas). بدونِ این،
        // انیمیشن‌های گیاه/زامبی/سنگ‌قبر هرگز bake نمی‌شوند و نامرئی می‌مانند —
        // همه‌ی اسکرین‌های دیگر این را صدا می‌زنند جز همین صفحه‌ی بازی (باگ).
        GameAssets.getInstance().update();

        if (!paused) advanceGame(delta);

        if (isVersusGuest()) {
            // مهمان: آخرین snapshotِ میزبان را رندر می‌کند + به جلسه‌ی آینه اعمال می‌کند.
            GameStateSnapshot ls = versus.latestSnapshot();
            if (ls != null) {
                snap = ls;
                versus.applier().apply(GameFacade.get().getCurrentSession(), snap);
            }
        } else {
            snap = facade().buildSnapshot();
            if (isVersusHost()) {
                versus.stream(snap);                          // پخشِ وضعیت برای مهمان
                if (snap.versusWinnerRole != null && !snap.versusWinnerRole.isEmpty()) {
                    versus.reportResult(snap.versusWinnerRole);
                }
            }
        }
        detectWaveStart();
        detectSpecialAnnouncements();
        refreshSeedBank();
        updateIZombieHud();
        updateSpecialStatus();
        detectMeoPointGains();
        showMeoEvents();
        showQuestEvents();
        showCollectEvents();
        spawnTornadoDrops();
        spawnScreenEffects();
        spawnExplosions();
        spawnOctopusTosses();
        detectBossShake();

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        gameViewport.apply();
        // لرزشِ دوربین (انفجار/حرکتِ رئیس) — HUD جدا می‌ماند چون viewportِ مستقل دارد.
        camera.position.set(GameConstants.VIEWPORT_WIDTH * 0.5f + renderer.getShakeOffsetX(),
                            GameConstants.VIEWPORT_HEIGHT * 0.5f + renderer.getShakeOffsetY(), 0);
        camera.update();
        renderer.render(snap, GameFacade.get().getCurrentSession(), delta, camera, speedIndex);

        hudStage.act(delta);
        hudStage.draw();

        if (versus != null) handleVersusEnd();
        else if (couch) { updateCouchHint(); handleCouchEnd(); }
        else checkEndCondition();
    }

    /**
     * پایانِ مسابقه‌ی دونفره: وقتی سرور {@code MATCH_ENDED} فرستاد (برد/باخت یا
     * قطعِ حریف)، پیام را نشان بده و به لابی برگرد. خودِ MultiplayerService یک
     * Pop-upِ نتیجه روی اورلیِ سراسری نشان می‌دهد؛ اینجا فقط از صفحه خارج می‌شویم.
     */
    private void handleVersusEnd() {
        if (versus.isEnded() && !versusEndShown) {
            versusEndShown = true;
            goTo(ScreenId.MULTIPLAYER);
        }
    }

    /** آغازِ موجِ جدید: صدای موج پخش و یک توست نمایش داده می‌شود (BR5/BT2/FQ2). */
    private void detectWaveStart() {
        if (snap == null) return;
        if (snap.currentWave > lastWaveNumber) {
            if (lastWaveNumber > 0) { // موجِ اول را اعلام نکن (شروعِ عادیِ مرحله)
                com.pvz2.graphics.audio.SoundManager.get()
                        .playSfx(com.pvz2.graphics.audio.SoundManager.SFX_WAVE);
                boolean last = snap.totalWaves > 0 && snap.currentWave >= snap.totalWaves;
                showToast(last ? ToastActor.error("FINAL WAVE!")
                                : ToastActor.info("Wave " + snap.currentWave));
            }
            lastWaveNumber = snap.currentWave;
        }
    }

    /**
     * اعلانِ نکرومنسی (Dark Ages) و جزر/مدِ ساحل (Big Wave Beach) — با شمردنِ
     * کاشی‌های ویژه در snapshot و تشخیصِ تغییرِ آن‌ها (مثلِ {@link #detectWaveStart}).
     */
    private void detectSpecialAnnouncements() {
        if (snap == null || snap.tiles == null) return;
        String chapter = game.getCurrentChapter();

        // نکرومنسی: ظاهر/زیاد شدنِ سنگ‌قبرهای تاریک → اعلانِ «Necromancy!».
        if ("DARK_AGES".equalsIgnoreCase(chapter)) {
            int necro = countTiles(GameStateSnapshot.TileType.NECROMANCY)
                      + countTiles(GameStateSnapshot.TileType.DARK_TOMBSTONE);
            boolean first = lastNecroCount < 0;
            if ((first && necro > 0) || (!first && necro > lastNecroCount)) {
                showBanner(ToastActor.error("☠ Necromancy!"));
            }
            lastNecroCount = necro;
        }

        // جزر/مدِ ساحل: در نقاطِ اوجِ تغییرِ آب اعلانِ Low/High Tide.
        if ("BIG_WAVE_BEACH".equalsIgnoreCase(chapter)) {
            int water = countTiles(GameStateSnapshot.TileType.WATER);
            if (lastWaterCount >= 0 && water != lastWaterCount) {
                int dir = water < lastWaterCount ? -1 : +1;
                if (tideDir != 0 && dir != tideDir) {
                    // جهت عوض شد → به یکی از دو اوجِ جزر/مد رسیدیم.
                    showBanner(dir > 0 ? ToastActor.info("🌊 Low Tide!")
                                       : ToastActor.info("🌊 High Tide!"));
                }
                tideDir = dir;
            }
            lastWaterCount = water;
        }
    }

    private int countTiles(GameStateSnapshot.TileType t) {
        int n = 0;
        for (int c = 0; c < snap.tiles.length; c++) {
            for (int r = 0; r < snap.tiles[c].length; r++) {
                if (snap.tiles[c][r] == t) n++;
            }
        }
        return n;
    }

    /** بنرِ مرکزی‌بالای صفحه برای اعلان‌های مهم (بزرگ‌تر از توستِ معمولی). */
    private void showBanner(ToastActor t) {
        t.setPosition(GameConstants.VIEWPORT_WIDTH / 2f - t.getWidth() * 0.5f,
                GameConstants.VIEWPORT_HEIGHT * 0.62f);
        hudStage.addActor(t);
    }

    private void advanceGame(float delta) {
        if (versus != null) {
            // فقط میزبان شبیه‌سازی می‌کند؛ مهمان snapshot را در render اعمال می‌کند.
            if (versus.isHost()) {
                if (!versus.isStarted()) return;            // منتظرِ آماده‌شدنِ هر دو
                for (com.pvz2.shared.protocol.payload.MatchInputEvent in : versus.drainInputs()) {
                    applyGuestInput(in);
                }
                tickAccum += delta;                          // سرعتِ ثابتِ ۱x در مالتی‌پلیر
                while (tickAccum >= TICK_INTERVAL) {
                    facade().advanceTicks(1);
                    tickAccum -= TICK_INTERVAL;
                }
            }
            return;
        }
        float speed = GameConstants.SPEED_MULTIPLIERS[speedIndex];
        tickAccum += delta * speed;
        while (tickAccum >= TICK_INTERVAL) {
            facade().advanceTicks(1);
            tickAccum -= TICK_INTERVAL;
        }
    }

    /** میزبان: اعمالِ ورودیِ زامبیِ مهمان (گذاشتنِ زامبی) در شبیه‌سازیِ واقعی. */
    private void applyGuestInput(com.pvz2.shared.protocol.payload.MatchInputEvent in) {
        if (in == null || !"PLACE_ZOMBIE".equals(in.action)) return;
        try {
            ZombieType t = ZombieType.valueOf(in.zombieType.toUpperCase());
            facade().placeZombie(t, in.col, in.row);
        } catch (Exception ignored) { }
    }

    /** هر فریم: نوار کناری را از صف snap تازه کن، وگرنه cooldown کارت‌ها. */
    private void refreshSeedBank() {
        if (izombieMode) { refreshZombieBank(); return; }
        if (beghouledMode) { refreshBeghouledUpgrades(); return; }
        if (snap == null || seedBank == null) return;
        if (conveyorMode) {
            // هر فریم هماهنگ کن تا انیمیشن ورود/انباشت روان باشد (mutate فقط هنگام تغییر صف)
            seedBank.syncConveyor(snap.conveyorQueue, zeroCosts(snap.conveyorQueue));
            if (!snap.conveyorQueue.equals(lastConveyor)) {
                lastConveyor = new ArrayList<>(snap.conveyorQueue);
                if (plantingType != null && !snap.conveyorQueue.contains(plantingType)) {
                    cursorMode = CursorMode.NONE;
                    plantingType = null;
                }
            }
        } else {
            updateSeedBankCooldowns();
        }
    }

    private void updateSeedBankCooldowns() {
        if (snap == null || seedBank == null) return;
        // cooldownِ بسته‌ی بذر (recharge کارت) — از snap.seedCooldowns، نه از
        // گیاهانِ کاشته‌شده روی نقشه (که باگِ قبلی بود).
        Map<PlantType, Float> fracs = new HashMap<>();
        if (snap.seedCooldowns != null) {
            for (Map.Entry<String, Float> e : snap.seedCooldowns.entrySet()) {
                try {
                    fracs.put(PlantType.valueOf(e.getKey().toUpperCase()), e.getValue());
                } catch (IllegalArgumentException ignored) {}
            }
        }
        seedBank.updateCooldowns(fracs);
    }

    /** به‌روزرسانی لیبل وضعیت مرحله ویژه بر اساس snapshot. */
    private void updateSpecialStatus() {
        if (specialStatusLabel == null || snap == null) return;
        String txt = "";
        if (snap.levelType == LevelType.TIMED_WAR) {
            String goal = snap.timedWarSunMode
                    ? "Sun " + snap.timedWarSun + "/" + snap.timedWarSunTarget
                    : "Kills " + snap.timedWarKills + "/" + snap.timedWarTarget;
            txt = goal + "    ⏱ " + snap.timedWarSeconds + "s";
        } else if (snap.levelType == LevelType.LOVE_YOUR_PLANTS) {
            txt = "Plants lost  " + snap.plantsLost + " / " + snap.maxPlantsAllowed;
        } else if (snap.levelType == LevelType.CONVEYOR_BELT) {
            txt = "📦 Conveyor  (" + snap.conveyorQueue.size() + "/5)";
        } else if (snap.levelType == LevelType.VASEBREAKER) {
            txt = "🏺 Vases left: " + snap.vasesRemaining;
        } else if (snap.levelType == LevelType.I_ZOMBIE) {
            int brains = 0;
            if (snap.izombieBrains != null)
                for (boolean b : snap.izombieBrains) if (b) brains++;
            txt = "🧠 Brains left: " + brains;
        } else if (snap.levelType == LevelType.VERSUS) {
            int brains = 0;
            if (snap.izombieBrains != null)
                for (boolean b : snap.izombieBrains) if (b) brains++;
            txt = "🧠 Brains: " + brains + "    ⏱ " + snap.versusSecondsLeft + "s";
        } else if (snap.levelType == LevelType.BEGHOULED) {
            txt = "💎 Matches: " + snap.beghouledMatches + " / " + snap.beghouledTarget;
        } else if (snap.levelType == LevelType.SCORED) {
            txt = "🏆 MeoPoints: " + snap.meoPoints;
        }
        specialStatusLabel.setText(txt);
    }

    private void detectMeoPointGains() {
        if (snap == null) return;
        if (snap.meoPoints > lastMeoPoints) {
            long gained = snap.meoPoints - lastMeoPoints;
            lastMeoPoints = snap.meoPoints;
            renderer.showTextPopup(
                    GameConstants.VIEWPORT_WIDTH * 0.5f,
                    GameConstants.VIEWPORT_HEIGHT * 0.5f,
                    "+" + gained + " 🌙 MeoPoints!",
                    Color.GOLD);
        }
    }

    /** اعلانِ الگوهای امتیازی (Speed-Kill / AoE / Item Collector) روی صفحه. */
    private void showMeoEvents() {
        if (snap == null || snap.meoEvents == null || snap.meoEvents.isEmpty()) return;
        float y = 430f;
        for (String ev : snap.meoEvents) {
            ToastActor t = ToastActor.info(ev);
            t.setPosition(GameConstants.VIEWPORT_WIDTH * 0.5f - t.getWidth() * 0.5f, y);
            hudStage.addActor(t);
            y -= 34f;
        }
    }

    /** توستِ تکمیلِ کوئست حین بازی (جایزه از قبل به کاربر اضافه و ذخیره شده است). */
    private void showQuestEvents() {
        java.util.List<String> events = facade().drainQuestEvents();
        if (events == null || events.isEmpty()) return;
        float y = 300f;
        for (String ev : events) {
            ToastActor t = ToastActor.success(ev);
            t.setPosition(GameConstants.VIEWPORT_WIDTH * 0.5f - t.getWidth() * 0.5f, y);
            hudStage.addActor(t);
            y -= 40f;
        }
    }

    /** گردبادهایِ تازه‌ی این فریم را به رندرر می‌سپارد تا انیمیشنشان را بزند. */
    private void spawnTornadoDrops() {
        if (snap == null || snap.tornadoDrops == null || snap.tornadoDrops.isEmpty()) return;
        if (renderer == null) return;
        for (int[] d : snap.tornadoDrops) renderer.spawnTornado(d[0], d[1]);
    }

    /** افکت‌های تمام‌صفحه‌ی تازه (مثلِ بادِ یخیِ IceShroom) را پخش می‌کند. */
    private void spawnScreenEffects() {
        if (snap == null || snap.screenEffects == null || snap.screenEffects.isEmpty()) return;
        if (renderer == null) return;
        for (String name : snap.screenEffects) renderer.spawnScreenEffect(name);
    }

    /** انفجارهایِ تازه (بمب گیلاسی/جالاپینو/دوم‌شروم): جلوه + لرزشِ صفحه. */
    private void spawnExplosions() {
        if (snap == null || snap.explosionEvents == null || snap.explosionEvents.isEmpty()) return;
        if (renderer == null) return;
        for (int[] e : snap.explosionEvents) {
            renderer.triggerExplosion(GameCoords.toScreenX(e[0]),
                                      GameCoords.toScreenY(e[1]), 80f);
        }
    }

    /** پرتابه‌های اختاپوسِ تازه را به رندرر می‌سپارد تا انیمیشنشان را بزند. */
    private void spawnOctopusTosses() {
        if (snap == null || snap.octopusTosses == null || snap.octopusTosses.isEmpty()) return;
        if (renderer == null) return;
        for (double[] t : snap.octopusTosses)
            renderer.spawnOctopusProjectile(t[0], t[1], t[2], t[3]);
    }

    private String lastBossState = "";
    /** لرزشِ صفحه هنگامِ حرکت/کوبشِ رئیس (مصرِ باستان و بقیه). */
    private void detectBossShake() {
        if (snap == null || renderer == null) return;
        String bs = snap.bossActive && snap.bossState != null ? snap.bossState : "";
        if (!bs.equals(lastBossState)
                && ("moving".equals(bs) || "attacking".equals(bs))) {
            renderer.shake(5f, 0.28f);
        }
        lastBossState = bs;
    }

    /** توستِ جمع‌آوریِ سکه/الماس/گلدان/غذای گیاه از زامبی‌های کشته‌شده (هر مرحله). */
    private void showCollectEvents() {
        if (snap == null || snap.collectEvents == null || snap.collectEvents.isEmpty()) return;
        float y = 250f;
        for (String ev : snap.collectEvents) {
            ToastActor t = ToastActor.success(ev);
            t.setPosition(GameConstants.VIEWPORT_WIDTH * 0.5f - t.getWidth() * 0.5f, y);
            hudStage.addActor(t);
            y -= 40f;
        }
    }

    private void checkEndCondition() {
        if (snap == null || endDialogShown) return;
        if (snap.status == GameStateSnapshot.GameStatus.WON)  { endDialogShown = true; showEndDialog(true); }
        else if (snap.status == GameStateSnapshot.GameStatus.LOST) { endDialogShown = true; showEndDialog(false); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Input Handling
    // ─────────────────────────────────────────────────────────────────────────

    private InputAdapter buildGameInputAdapter() {
        return new InputAdapter() {
            @Override
            public boolean touchDown(int sx, int sy, int ptr, int btn) {
                Vector2 w = unproject(sx, sy);
                if (GameCoords.inGrid(w.x, w.y)) handleGridClick((int) w.x, w.y);
                return false;
            }
            @Override
            public boolean mouseMoved(int sx, int sy) {
                Vector2 w = unproject(sx, sy);
                // درخششِ hover خورشیدها (CN3)
                if (renderer != null) renderer.setCursorWorld(w.x, w.y);
                // برداشتِ خورشید با عبورِ نشانگر (CN3) — حتی حین سقوط، پس مستقلِ از grid
                tryCollectSunAtCursor(w.x, w.y);
                if (GameCoords.inGrid(w.x, w.y)) handleHover(w.x, w.y);
                updateCursorVisuals(w.x, w.y);
                return false;
            }
            @Override
            public boolean keyDown(int key) {
                if (key == Input.Keys.ESCAPE) togglePause();
                if (couch) { handleCouchKey(key); return false; }
                if (key == Input.Keys.S)      toggleMode(CursorMode.SHOVEL);
                if (key == Input.Keys.F)      toggleMode(CursorMode.PLANT_FOOD);
                handleCheatKey(key);
                return false;
            }
        };
    }

    /** ورودیِ کیبوردِ بازیکنِ زامبی در Couch Play: 1-5 انتخاب، Q/W/E/R/T کاشتِ ردیف. */
    private void handleCouchKey(int key) {
        int sel = -1;
        if (key == Input.Keys.NUM_1) sel = 0;
        else if (key == Input.Keys.NUM_2) sel = 1;
        else if (key == Input.Keys.NUM_3) sel = 2;
        else if (key == Input.Keys.NUM_4) sel = 3;
        else if (key == Input.Keys.NUM_5) sel = 4;
        if (sel >= 0) {
            if (snap != null && sel < snap.izombieRoster.size()) couchSelIndex = sel;
            return;
        }
        int row = -1;
        if (key == Input.Keys.Q) row = 1;
        else if (key == Input.Keys.W) row = 2;
        else if (key == Input.Keys.E) row = 3;
        else if (key == Input.Keys.R) row = 4;
        else if (key == Input.Keys.T) row = 5;
        if (row > 0) placeCouchZombie(row);
    }

    private void placeCouchZombie(int row) {
        if (couchSelIndex < 0 || snap == null || couchSelIndex >= snap.izombieRoster.size()) return;
        try {
            ZombieType t = ZombieType.valueOf(snap.izombieRoster.get(couchSelIndex).type.toUpperCase());
            int cols = GameFacade.get().getCurrentSession().getGameMap().getCols();
            if (row > GameFacade.get().getCurrentSession().getGameMap().getRows()) return;
            String err = facade().placeZombie(t, cols, row);
            if (err != null) showToast(ToastActor.error(err));
            else renderer.showTextPopup(GameCoords.toScreenX(cols), GameCoords.toScreenY(row),
                    "🧟", new Color(0.6f, 1f, 0.4f, 1f));
        } catch (Exception ignored) { }
    }

    /** پایانِ Couch Play: برنده (PLANT/ZOMBIE) را اعلام و به لابی برمی‌گردد. */
    private void handleCouchEnd() {
        if (snap == null || couchEndShown) return;
        String w = snap.versusWinnerRole;
        if (w != null && !w.isEmpty()) {
            couchEndShown = true;
            Skin skin = GameAssets.getInstance().getSkin();
            game.showOverlay(com.pvz2.graphics.actors.NetPopups.info(skin, "Match Over",
                    w + " player wins!", () -> { game.hideOverlay(); goTo(ScreenId.MULTIPLAYER); }));
        }
    }

    private void handleGridClick(int wx, float wy) {
        int col = GameCoords.toPhase1Col(wx);
        int row = GameCoords.toPhase1Row(wy);
        if (izombieMode) {
            if (placingZombie != null) doPlaceZombie(col, row);
            return;
        }
        if (beghouledMode) { handleBeghouledClick(col, row); return; }
        switch (cursorMode) {
            case PLANTING:   doPlant(col, row);   break;
            case SHOVEL:     doShovel(col, row);  break;
            case PLANT_FOOD: doFeed(col, row);    break;
            default:
                // کوزه‌شکنی: کلیک روی خانه‌ی دارای کوزه آن را می‌شکند
                if (vasebreakerMode) doBreakVase(col, row);
                break;
        }
    }

    /** انتخاب/جابه‌جاییِ گیاهان در Beghouled: کلیک اول انتخاب، کلیکِ مجاور = swap. */
    private void handleBeghouledClick(int col, int row) {
        if (snap == null) return;
        if (col < 1 || col > GameConstants.TILE_COLS
                || row < 1 || row > GameConstants.TILE_ROWS) return;
        if (begSelCol == 0) {                     // هیچ انتخابی نیست → انتخاب کن
            begSelCol = col; begSelRow = row;
        } else if (begSelCol == col && begSelRow == row) {
            begSelCol = begSelRow = 0;            // همان خانه → لغوِ انتخاب
        } else if (Math.abs(begSelCol - col) + Math.abs(begSelRow - row) == 1) {
            String err = facade().beghouledSwap(begSelCol, begSelRow, col, row);
            if (err != null) showToast(ToastActor.error(err));
            begSelCol = begSelRow = 0;
        } else {
            begSelCol = col; begSelRow = row;     // خانه‌ی غیرمجاور → انتخابِ جدید
        }
        renderer.setBeghouledSelection(begSelCol, begSelRow);
    }

    private void doBreakVase(int col, int row) {
        if (!hasVaseAt(col, row)) return;
        String err = facade().breakVase(col, row);
        if (err != null) {
            showToast(ToastActor.error(err));
        } else {
            renderer.triggerHitFlash(
                    GameCoords.toScreenX(col), GameCoords.toScreenY(row),
                    GameConstants.TW * 0.6f, GameConstants.TH * 0.6f);
        }
    }

    private boolean hasVaseAt(int col, int row) {
        if (snap == null || snap.vases == null) return false;
        for (GameStateSnapshot.VaseInfo v : snap.vases) {
            if (v.col == col && v.row == row) return true;
        }
        return false;
    }

    private void handleHover(float wx, float wy) {
        int row = GameCoords.toPhase1Row(wy);
        if (izombieMode) { hoveredRow = row; return; }
        // برداشتِ خورشید حالا در tryCollectSunAtCursor انجام می‌شود (شاملِ خورشیدهای در حالِ سقوط).
    }

    /** ابزارهایِ نشانگر (آیکنِ بیل/غذا) را lazy می‌سازد و به hudStage اضافه می‌کند. */
    private void ensureCursorActors() {
        if (cursorTool != null) return;
        GameAssets a = GameAssets.getInstance();
        shovelDrawable    = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                a.local("Exports/in game/shovel_button.png"));
        plantfoodDrawable = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                a.local("Exports/in game/plantfood_button.png"));
        cursorTool = new com.badlogic.gdx.scenes.scene2d.ui.Image();
        cursorTool.setSize(56f, 56f);
        cursorTool.setVisible(false);
        cursorTool.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        hudStage.addActor(cursorTool);
    }

    /**
     * انیمیشنِ idle گیاه یا آیکنِ ابزار را روی نشانگرِ موس نگه می‌دارد و خانه‌ی
     * زیرِ اشاره‌گر را کمی سفید می‌کند. ورودی مختصاتِ world (پیکسل) است.
     */
    private void updateCursorVisuals(float wx, float wy) {
        if (renderer == null || izombieMode) return;
        ensureCursorActors();
        boolean inGrid = GameCoords.inGrid(wx, wy);
        boolean active = cursorMode != CursorMode.NONE && inGrid;
        renderer.setCursorHighlight(active ? GameCoords.toPhase1Col(wx) : 0,
                                    active ? GameCoords.toPhase1Row(wy) : 0);
        Vector2 v = hudStage.getViewport().unproject(
                new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        // ── انیمیشنِ حالتِ بیکارِ گیاه روی نشانگر (حالتِ کاشت) ──
        if (cursorMode == CursorMode.PLANTING && plantingType != null) {
            if (cursorPlant == null || cursorPlantType != plantingType) {
                if (cursorPlant != null) cursorPlant.remove();
                cursorPlant = PamIdleActor.forPlant(plantingType, null);
                cursorPlant.setSize(GameConstants.TW * 1.2f, GameConstants.TH * 1.6f);
                cursorPlant.setColor(1f, 1f, 1f, 0.85f);
                cursorPlant.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
                hudStage.addActor(cursorPlant);
                cursorPlantType = plantingType;
            }
            cursorPlant.setVisible(true);
            cursorPlant.setPosition(v.x - cursorPlant.getWidth() / 2f,
                                    v.y - cursorPlant.getHeight() / 2f);
        } else if (cursorPlant != null) {
            cursorPlant.setVisible(false);
        }
        // ── آیکنِ بیلچه / غذای گیاه روی نشانگر ──
        if (cursorMode == CursorMode.SHOVEL || cursorMode == CursorMode.PLANT_FOOD) {
            cursorTool.setDrawable(cursorMode == CursorMode.SHOVEL
                    ? shovelDrawable : plantfoodDrawable);
            cursorTool.setVisible(true);
            cursorTool.setPosition(v.x - cursorTool.getWidth() / 2f,
                                   v.y - cursorTool.getHeight() / 2f);
        } else {
            cursorTool.setVisible(false);
        }
    }

    /** پنهان‌سازیِ کاملِ ابزار/گیاهِ نشانگر و هایلایت (پس از کاشت/برداشت/تغییرِ حالت). */
    private void hideCursorVisuals() {
        if (cursorPlant != null) cursorPlant.setVisible(false);
        if (cursorTool != null)  cursorTool.setVisible(false);
        if (renderer != null)    renderer.setCursorHighlight(0, 0);
    }

    /** شعاعِ برداشتِ خورشید بر حسبِ پیکسلِ صفحه. */
    private static final float SUN_PICK_RADIUS = 30f;

    /**
     * هر خورشیدِ قابل‌مشاهده (در حالِ سقوط یا روی زمین) که نشانگر روی آن باشد را برمی‌دارد.
     * موقعیتِ صفحه‌ایِ خورشید دقیقاً مطابقِ فرمولِ رندر ({@code EntityRenderer.drawSun})
     * محاسبه می‌شود تا برداشت با آنچه کاربر می‌بیند هم‌راستا باشد.
     */
    private void tryCollectSunAtCursor(float wx, float wy) {
        if (snap == null || snap.sunItems == null || snap.sunItems.isEmpty()) return;
        for (GameStateSnapshot.SunInfo sun : snap.sunItems) {
            float sxp = GameCoords.toScreenX(sun.phase1X);
            float syp;
            if (!sun.isLanded) {
                float topY  = GameConstants.VIEWPORT_HEIGHT + 60f;
                float landY = GameCoords.toScreenY(sun.phase1Y);
                syp = topY + (landY - topY) * sun.fallProgress;
            } else {
                syp = GameCoords.toScreenY(sun.phase1Y);
            }
            float dx = wx - sxp, dy = wy - syp;
            if (dx * dx + dy * dy <= SUN_PICK_RADIUS * SUN_PICK_RADIUS) {
                facade().collectSun(sun.phase1X, sun.phase1Y);
                return; // یک خورشید در هر حرکتِ نشانگر
            }
        }
    }

    private void doPlant(int col, int row) {
        if (plantingType == null) return;
        String err = facade().plantPlant(plantingType, col, row);
        if (err != null) {
            showToast(ToastActor.error(err));
        } else {
            if (!conveyorMode) seedBank.onPlanted(plantingType, 1f);
            cursorMode = CursorMode.NONE;
            hideCursorVisuals();
            seedBank.clearSelection();
            // جلوه بصری کاشت
            renderer.showTextPopup(
                    GameCoords.toScreenX(col), GameCoords.toScreenY(row),
                    "+🌱", Color.GREEN);
        }
    }

    private void doShovel(int col, int row) {
        String err = facade().pluckPlant(col, row);
        if (err != null) showToast(ToastActor.error(err));
        cursorMode = CursorMode.NONE;
        hideCursorVisuals();
    }

    private void doFeed(int col, int row) {
        String err = facade().feedPlant(col, row);
        if (err != null) {
            showToast(ToastActor.error(err));
        } else {
            float cx = GameCoords.toScreenX(col);
            float cy = GameCoords.toScreenY(row);
            renderer.triggerPlantFoodAura(cx, cy);
        }
        cursorMode = CursorMode.NONE;
        hideCursorVisuals();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UI Actions
    // ─────────────────────────────────────────────────────────────────────────

    private void toggleMode(CursorMode mode) {
        cursorMode = (cursorMode == mode) ? CursorMode.NONE : mode;
        if (cursorMode != CursorMode.PLANTING) {
            seedBank.clearSelection();
            plantingType = null;
        }
        if (cursorMode == CursorMode.NONE) hideCursorVisuals();
    }

    private void cycleSpeed() {
        speedIndex = (speedIndex + 1) % GameConstants.SPEED_MULTIPLIERS.length;
        String[] labels = {"1x", "1.5x", "2x"};
        if (speedLabel != null) speedLabel.setText(labels[speedIndex]);
    }

    private void togglePause() {
        paused = !paused;
        if (paused) pauseOverlay.show(hudStage);
        else         pauseOverlay.hide();
    }

    private PauseOverlay.Listener buildPauseListener() {
        return new PauseOverlay.Listener() {
            @Override public void onResume()  { togglePause(); }
            @Override public void onRestart() { restartLevel(); }
            @Override public void onQuit()    { goTo(ScreenId.ADVENTURE); }
        };
    }

    /** شروع مجدد مرحله — مینی‌گیم‌ها با startMinigame و مراحل عادی با startGame. */
    private void restartLevel() {
        LevelType lt = snap != null ? snap.levelType : null;
        if (lt == LevelType.VASEBREAKER || lt == LevelType.WALLNUT_BOWLING
                || lt == LevelType.I_ZOMBIE || lt == LevelType.BEGHOULED
                || lt == LevelType.ZOMBOTANY || lt == LevelType.SCORED) {
            game.startMinigame(game.getCurrentChapter(),
                    game.getCurrentLevel(), lt, game.getSelectedPlants());
        } else {
            game.startGame(game.getCurrentChapter(),
                    game.getCurrentLevel(), game.getSelectedPlants());
        }
    }

    private void showEndDialog(boolean won) {
        // موسیقیِ برد/باخت (FQ2)
        com.pvz2.graphics.audio.SoundManager.get().playMusicOnce(won
                ? com.pvz2.graphics.audio.SoundManager.MUSIC_WIN
                : com.pvz2.graphics.audio.SoundManager.MUSIC_LOSS);
        // بازی امتیازی: بالاترین میوپوینت را ذخیره کن
        if (snap != null && snap.levelType == LevelType.SCORED) {
            facade().saveScoredResult();
        }
        // آغازِ مرحله دیگر معنی ندارد اگر بازی تمام شده باشد
        if (startDialog != null) { startDialog.remove(); startDialog = null; paused = false; }
        Skin skin = GameAssets.getInstance().getSkin();
        final ScreenId exitTo = isMinigameLevel() ? ScreenId.QUEST : ScreenId.ADVENTURE;
        EndScreenOverlay overlay = new EndScreenOverlay(skin, won,
                new EndScreenOverlay.Listener() {
                    @Override public void onRestart() { restartLevel(); }
                    @Override public void onExit()    { goTo(exitTo); }
                });
        overlay.show(hudStage);
    }

    /** آیا مرحله‌ی جاری یک مینی‌گیم است؟ (برای مسیرِ خروجِ صفحه‌ی پایان). */
    private boolean isMinigameLevel() {
        if (snap == null) return false;
        switch (snap.levelType) {
            case VASEBREAKER: case WALLNUT_BOWLING: case I_ZOMBIE:
            case BEGHOULED:   case ZOMBOTANY:       case SCORED:
                return true;
            default:
                return false;
        }
    }

    private void showToast(ToastActor t) {
        t.setPosition(GameConstants.VIEWPORT_WIDTH * 0.5f - t.getWidth() * 0.5f, 440);
        hudStage.addActor(t);
    }

    // ─── Coordinate helpers ───────────────────────────────────────────────────

    private Vector2 unproject(int sx, int sy) {
        return gameViewport.unproject(new Vector2(sx, sy));
    }

    @Override
    public void resize(int w, int h) {
        gameViewport.update(w, h);
        hudStage.getViewport().update(w, h, true);
    }

    @Override
    public void dispose() {
        if (versus != null) {
            if (!versusEndShown) versus.leave();  // خروجِ زودهنگام → اطلاعِ حریف
            versus.end();
            versus = null;
        }
        if (renderer != null) renderer.dispose();
        if (hudStage  != null) hudStage.dispose();
    }
}
