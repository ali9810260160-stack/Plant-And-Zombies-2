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
import com.pvz2.model.enums.LevelType;
import com.pvz2.model.enums.PlantType;

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

    // ─── HUD widgets ─────────────────────────────────────────────────────────
    private Label sunLabel, foodLabel, meoLabel;
    private PauseOverlay pauseOverlay;
    private Button speedBtn;
    private int speedIndex = 0;

    // ─── Interaction state ────────────────────────────────────────────────────
    private CursorMode cursorMode = CursorMode.NONE;
    private PlantType  plantingType;
    private boolean    paused;

    // ─── Meopoints popup tracking ─────────────────────────────────────────────
    private long lastMeoPoints;

    public GameScreen(PVZApplication game) { super(game); }

    // ─────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void show() {
        setupRenderer();
        setupHudStage();
        setupInput();
        snap = facade().buildSnapshot();
    }

    private void setupRenderer() {
        camera = new OrthographicCamera();
        gameViewport = new FitViewport(
                GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT, camera);
        camera.position.set(GameConstants.VIEWPORT_WIDTH * 0.5f,
                GameConstants.VIEWPORT_HEIGHT * 0.5f, 0);
        camera.update();
        renderer = new GameRenderer(game.getCurrentChapter());
    }

    private void setupHudStage() {
        hudStage = new Stage(new FitViewport(
                GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Skin skin = GameAssets.getInstance().getSkin();
        buildSeedBank(skin);
        buildHudButtons(skin);
        buildSpecialLevelUI(skin);
        pauseOverlay = new PauseOverlay(skin, buildPauseListener());
    }

    private void buildSeedBank(Skin skin) {
        List<PlantType> plants    = game.getSelectedPlants();
        Map<PlantType, Integer> costs = loadPlantCosts(plants);

        seedBank = new SeedBankActor(plants, costs, skin);
        seedBank.setPosition(4, 2);
        seedBank.setOnPlantSelected(type -> {
            cursorMode   = type != null ? CursorMode.PLANTING : CursorMode.NONE;
            plantingType = type;
        });
        hudStage.addActor(seedBank);
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
        float btnY = GameConstants.TOP_BAR_Y + 12;

        // دکمه بیلچه
        TextButton shovelBtn = new TextButton("⛏", skin);
        shovelBtn.setBounds(GameConstants.VIEWPORT_WIDTH - 188, btnY, 56, 56);
        shovelBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                toggleMode(CursorMode.SHOVEL);
            }
        });
        hudStage.addActor(shovelBtn);

        // دکمه غذای گیاه
        TextButton foodBtn = new TextButton("🌿", skin);
        foodBtn.setBounds(GameConstants.VIEWPORT_WIDTH - 128, btnY, 56, 56);
        foodBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                toggleMode(CursorMode.PLANT_FOOD);
            }
        });
        hudStage.addActor(foodBtn);

        // دکمه سرعت
        speedBtn = new TextButton("1×", skin, "brown");
        speedBtn.setBounds(GameConstants.VIEWPORT_WIDTH - 68, btnY + 30, 60, 26);
        speedBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { cycleSpeed(); }
        });
        hudStage.addActor(speedBtn);

        // دکمه pause
        TextButton pauseBtn = new TextButton("⏸", skin);
        pauseBtn.setBounds(GameConstants.VIEWPORT_WIDTH - 68, btnY, 56, 28);
        pauseBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { togglePause(); }
        });
        hudStage.addActor(pauseBtn);

        buildDebugButtons(skin, btnY);
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

    private void buildSpecialLevelUI(Skin skin) {
        LevelType lt = (snap != null && snap.levelType != null)
                ? snap.levelType : LevelType.NORMAL;

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
        if (!paused) advanceGame(delta);

        snap = facade().buildSnapshot();
        updateSeedBankCooldowns();
        detectMeoPointGains();

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        gameViewport.apply();
        renderer.render(snap, delta, camera.combined, speedIndex);

        hudStage.act(delta);
        hudStage.draw();

        checkEndCondition();
    }

    private void advanceGame(float delta) {
        float speed = GameConstants.SPEED_MULTIPLIERS[speedIndex];
        tickAccum += delta * speed;
        while (tickAccum >= TICK_INTERVAL) {
            facade().advanceTicks(1);
            tickAccum -= TICK_INTERVAL;
        }
    }

    private void updateSeedBankCooldowns() {
        if (snap == null || seedBank == null) return;
        // PHASE1: از PlantDataRegistry cooldown واقعی بگیر
        // فعلاً از fraction داده‌شده توسط فاز ۱ استفاده می‌کنیم
        Map<PlantType, Float> fracs = new HashMap<>();
        for (GameStateSnapshot.PlantInfo p : snap.plants) {
            try {
                PlantType type = PlantType.valueOf(p.type.toUpperCase());
                fracs.put(type, p.cooldownFraction);
            } catch (IllegalArgumentException ignored) {}
        }
        seedBank.updateCooldowns(fracs);
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

    private void checkEndCondition() {
        if (snap == null) return;
        if (snap.status == GameStateSnapshot.GameStatus.WON)  showEndDialog(true);
        else if (snap.status == GameStateSnapshot.GameStatus.LOST) showEndDialog(false);
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
                if (GameCoords.inGrid(w.x, w.y)) handleHover(w.x, w.y);
                return false;
            }
            @Override
            public boolean keyDown(int key) {
                if (key == Input.Keys.ESCAPE) togglePause();
                if (key == Input.Keys.S)      toggleMode(CursorMode.SHOVEL);
                if (key == Input.Keys.F)      toggleMode(CursorMode.PLANT_FOOD);
                return false;
            }
        };
    }

    private void handleGridClick(int wx, float wy) {
        int col = GameCoords.toPhase1Col(wx);
        int row = GameCoords.toPhase1Row(wy);
        switch (cursorMode) {
            case PLANTING:   doPlant(col, row);   break;
            case SHOVEL:     doShovel(col, row);  break;
            case PLANT_FOOD: doFeed(col, row);    break;
            default: break;
        }
    }

    private void handleHover(float wx, float wy) {
        int col = GameCoords.toPhase1Col(wx);
        int row = GameCoords.toPhase1Row(wy);
        // برداشت آفتاب با hover
        if (cursorMode == CursorMode.NONE) {
            facade().collectSun(col, row);
        }
    }

    private void doPlant(int col, int row) {
        if (plantingType == null) return;
        String err = facade().plantPlant(plantingType, col, row);
        if (err != null) {
            showToast(ToastActor.error(err));
        } else {
            seedBank.onPlanted(plantingType, 1f);
            cursorMode = CursorMode.NONE;
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
    }

    private void cycleSpeed() {
        speedIndex = (speedIndex + 1) % GameConstants.SPEED_MULTIPLIERS.length;
        String[] labels = {"1×", "1.5×", "2×"};
        if (speedBtn != null) ((TextButton) speedBtn).setText(labels[speedIndex]);
    }

    private void togglePause() {
        paused = !paused;
        if (paused) pauseOverlay.show(hudStage);
        else         pauseOverlay.hide();
    }

    private PauseOverlay.Listener buildPauseListener() {
        return new PauseOverlay.Listener() {
            @Override public void onResume()  { togglePause(); }
            @Override public void onRestart() {
                game.startGame(game.getCurrentChapter(),
                        game.getCurrentLevel(), game.getSelectedPlants());
            }
            @Override public void onQuit()    { goTo(ScreenId.ADVENTURE); }
        };
    }

    private void showEndDialog(boolean won) {
        Skin skin = GameAssets.getInstance().getSkin();
        String title = won ? "🎉 You Win!" : "💀 You Lose!";
        Dialog dlg = new Dialog(title, skin) {
            @Override protected void result(Object obj) {
                if (Boolean.TRUE.equals(obj)) {
                    game.startGame(game.getCurrentChapter(),
                            game.getCurrentLevel(), game.getSelectedPlants());
                } else goTo(ScreenId.ADVENTURE);
                hide();
            }
        };
        String msg = won
                ? "Dear humanz, we will come back to eat your brainz!"
                : "The zombie ate your brain; LOSER!!!";
        dlg.text(msg);
        if (!won) dlg.button("Try Again", true);
        dlg.button("Exit", false);
        dlg.show(hudStage);
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
        if (renderer != null) renderer.dispose();
        if (hudStage  != null) hudStage.dispose();
    }
}
