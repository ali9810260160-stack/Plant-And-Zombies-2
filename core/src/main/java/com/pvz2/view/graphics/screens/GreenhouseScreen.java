package com.pvz2.graphics.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.pvz2.PVZApplication;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.pvz2.graphics.*;
import com.pvz2.graphics.actors.PamIdleActor;
import com.pvz2.graphics.actors.ToastActor;
import com.pvz2.graphics.assets.AssetIds;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.model.AppState;
import com.pvz2.model.Greenhouse;
import com.pvz2.model.Pot;
import com.pvz2.model.User;
import com.pvz2.model.enums.PlantType;
import com.pvz2.service.GreenhouseService;

import java.util.ArrayList;
import java.util.List;

/**
 * صفحه گلخانه — بازطراحی مطابق مرجع {@code zen-garden-pots.png}: پس‌زمینه‌ی
 * ۱۲‌جایگاهی، گلدان طلایی/قهوه‌ای برای گیاه کاشته‌شده، برچسب شمارش‌معکوس
 * زنده + دکمه‌ی تسریع با الماس، تیکت کاشت روی جایگاه‌های خالی، قفل روی
 * جایگاه‌های بسته و بیل برای برداشت.
 * <p>
 * هیچ منطق بازی این‌جا نیست: کاشت/برداشت/تسریع/باز کردن جایگاه همه از
 * {@link GreenhouseService} فاز یک میان — این کلاس فقط می‌چیند، می‌خونه و
 * صدا می‌زنه.
 */
public class GreenhouseScreen extends BaseScreen {

    // ─── هم‌ترازی شبکه‌ی گلدان‌ها با تصویر background.png (۱۱۹۵×۸۹۶) ──────
    // این کسرها از روی خود عکس اندازه‌گیری شدن (مرکز هر جعبه‌ی چوبی). اگه
    // بعد از تست روی صفحه‌ی واقعی چند پیکسل جابه‌جا بود، همین‌جا فاین‌تیون کن.
    private static final float BG_IMG_W = 1195f, BG_IMG_H = 896f;
    private static final float[] COL_X_FRAC = {0.2485f, 0.4100f, 0.5858f, 0.7448f};
    private static final float[] ROW_Y_FRAC_FROM_TOP = {0.3984f, 0.6027f, 0.8259f};
    // گلدان‌ها کوچک‌تر شدند (قبلاً 150×140) و انیمیشنِ idle گیاه داخلشان پخش می‌شود.
    private static final float SLOT_W = 118f;
    private static final float SLOT_H = 112f;
    /** گلدانِ چوبی نسبت به کلِ جایگاه کوچک‌تر است تا گیاه از داخلش بیرون بزند. */
    private static final float POT_W = 84f;
    private static final float POT_H = 66f;

    private Stage stage;

    /** فعال‌شدن با کلیک روی دکمه‌ی بیل — تا کلیک بعدی روی یک گلدان، اون رو برداشت می‌کنه. */
    private boolean shovelMode = false;
    private Actor shovelActor;

    /** برای بروزرسانی زنده‌ی شمارش‌معکوس، بدون rebuild کل صفحه هر فریم. */
    private static final class LiveTimer {
        final Pot pot;
        final Label label;
        boolean wasReady;
        LiveTimer(Pot pot, Label label, boolean wasReady) {
            this.pot = pot; this.label = label; this.wasReady = wasReady;
        }
    }
    private final List<LiveTimer> liveTimers = new ArrayList<>();
    private float timerAccumulator = 0f;

    public GreenhouseScreen(PVZApplication game) { super(game); }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(GameConstants.VIEWPORT_WIDTH, GameConstants.VIEWPORT_HEIGHT));
        Gdx.input.setInputProcessor(stage);
        buildUi();
    }

    private void buildUi() {
        liveTimers.clear();
        shovelMode = false;
        Skin skin = GameAssets.getInstance().getSkin();

        addBackground();

        Table root = new Table();
        root.setFillParent(true);
        root.top();
        root.add(buildHeader(skin)).growX().row();
        stage.addActor(root);

        buildPotGrid(skin);
        stage.addActor(buildShovelButton());
    }

    // =========================================================================
    //  پس‌زمینه — با Scaling.fit تا شبکه‌ی جایگاه‌ها کامل و بدون کراپ دیده بشه
    // =========================================================================

    private void addBackground() {
        TextureRegion bg = GameAssets.getInstance().local(AssetIds.GH_BG);
        Image bgImage = new Image(bg);
        bgImage.setScaling(Scaling.fit);
        bgImage.setFillParent(true);
        stage.addActor(bgImage);
    }

    /** مستطیل واقعیِ نمایش‌داده‌شده‌ی بک‌گراند روی صحنه (بعد از Scaling.fit) — برای نگاشت جایگاه‌ها. */
    private float[] computeBackgroundRect() {
        float scale = Math.min(GameConstants.VIEWPORT_WIDTH / BG_IMG_W, GameConstants.VIEWPORT_HEIGHT / BG_IMG_H);
        float dispW = BG_IMG_W * scale, dispH = BG_IMG_H * scale;
        float x0 = (GameConstants.VIEWPORT_WIDTH - dispW) / 2f;
        float y0 = (GameConstants.VIEWPORT_HEIGHT - dispH) / 2f;
        return new float[]{x0, y0, dispW, dispH};
    }

    // =========================================================================
    //  شبکه‌ی ۱۲ جایگاهی
    // =========================================================================

    private void buildPotGrid(Skin skin) {
        User user = AppState.getInstance().getCurrentUser();
        GreenhouseService gs = ServiceLocator.getInstance().getGreenhouseService();
        Greenhouse greenhouse;
        try {
            greenhouse = gs.getOrCreateGreenhouse(user);
        } catch (Exception e) {
            showToast(ToastActor.error("Error loading greenhouse: " + e.getMessage()));
            return;
        }

        float[] rect = computeBackgroundRect();
        float x0 = rect[0], y0 = rect[1], dispW = rect[2], dispH = rect[3];

        for (int r = 0; r < Greenhouse.ROWS; r++) {
            for (int c = 0; c < Greenhouse.COLS; c++) {
                Pot pot = greenhouse.getPot(c + 1, r + 1);
                if (pot == null) continue;

                float cx = x0 + COL_X_FRAC[c] * dispW;
                float topY = ROW_Y_FRAC_FROM_TOP[r] * dispH;
                float cy = y0 + (dispH - topY);

                Actor cell = buildPotSlot(skin, pot, user, gs);
                cell.setSize(SLOT_W, SLOT_H);
                cell.setPosition(cx - SLOT_W / 2f, cy - SLOT_H / 2f);
                stage.addActor(cell);
            }
        }
    }

    /** یک جایگاه: بسته به وضعیت پات، قفل / تیکت کاشت / گلدان در حال رشد / گلدان آماده رو می‌سازه. */
    private Actor buildPotSlot(Skin skin, Pot pot, User user, GreenhouseService gs) {
        final int px = pot.getX(), py = pot.getY();
        Stack stack = new Stack();

        if (pot.isLocked()) {
            Image lock = new Image(GameAssets.getInstance().local(AssetIds.GH_ICON_LOCKED));
            lock.setScaling(Scaling.fit);
            Table center = new Table();
            center.add(lock).size(40f, 52f);
            stack.add(center);

            stack.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent e, float x, float y) {
                    try {
                        gs.buyPot(user, px, py);
                        showToast(ToastActor.success("Pot unlocked! (-" + gs.getUnlockCostCoins() + " coins)"));
                    } catch (Exception ex) {
                        showToast(ToastActor.error(ex.getMessage()));
                    }
                    refresh();
                }
            });
            return stack;
        }

        if (pot.getPlantType() == null) {
            // جایگاه باز ولی خالی → تیکت کاشت
            Image ticket = new Image(GameAssets.getInstance().local(AssetIds.GH_TICKET_DIAMOND));
            ticket.setScaling(Scaling.fit);
            stack.add(ticket);

            Label cost = new Label(String.valueOf(gs.getPlantCostGems()), skin, "medium_outline");
            Table costLayer = new Table();
            costLayer.right().padRight(SLOT_W * 0.22f);
            costLayer.add(cost);
            stack.add(costLayer);

            stack.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent e, float x, float y) {
                    try {
                        gs.plantPot(user, px, py);
                        showToast(ToastActor.success("Planted! (-" + gs.getPlantCostGems() + " gems)"));
                    } catch (Exception ex) {
                        showToast(ToastActor.error(ex.getMessage()));
                    }
                    refresh();
                }
            });
            return stack;
        }

        // جایگاه با گیاه کاشته‌شده (در حال رشد یا آماده)
        boolean isMarigold = "MARIGOLD".equals(pot.getPlantType());
        String potAsset = isMarigold ? AssetIds.GH_POT_BROWN : AssetIds.GH_POT_GOLDEN;
        Image potImage = new Image(GameAssets.getInstance().local(potAsset));
        potImage.setScaling(Scaling.fit);
        // گلدانِ چوبیِ کوچک‌شده در پایینِ جایگاه.
        Table potLayer = new Table();
        potLayer.bottom();
        potLayer.add(potImage).size(POT_W, POT_H);
        stack.add(potLayer);

        // انیمیشنِ idle گیاهِ کاشته‌شده روی گلدان (رفعِ TODO فاز ۲) — با
        // PamIdleActor واقعی. کلیک‌ها را نمی‌گیرد تا برداشت با بیل کار کند.
        PamIdleActor plantIdle = createPlantIdle(pot.getPlantType());
        if (plantIdle != null) {
            plantIdle.setTouchable(Touchable.disabled);
            Table plantLayer = new Table();
            plantLayer.bottom();
            plantLayer.add(plantIdle).size(SLOT_W * 0.9f, SLOT_H * 0.92f).padBottom(POT_H * 0.42f);
            stack.add(plantLayer);
        }

        boolean ready = pot.isReady();
        if (!ready) {
            Table bottomOverlay = new Table();
            bottomOverlay.bottom();

            Label timerLabel = new Label(formatRemaining(pot.getRemainingSeconds()), skin, "default");
            timerLabel.setFontScale(0.8f);
            timerLabel.setColor(Color.WHITE);

            int speedupCost = gs.getSpeedupCostGems(pot);
            Stack speedupBtn = new Stack();
            Image speedupBg = new Image(GameAssets.getInstance().local(AssetIds.GH_BUTTON_SPEEDUP));
            speedupBg.setScaling(Scaling.fit);
            speedupBtn.add(speedupBg);
            Label speedupCostLbl = new Label(String.valueOf(speedupCost), skin, "default");
            speedupCostLbl.setFontScale(0.75f);
            Table speedupLblLayer = new Table();
            speedupLblLayer.center().padTop(10f);
            speedupLblLayer.add(speedupCostLbl);
            speedupBtn.add(speedupLblLayer);
            speedupBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent e, float x, float y) {
                    try {
                        gs.growPot(user, px, py);
                        showToast(ToastActor.success("Growth sped up!"));
                    } catch (Exception ex) {
                        showToast(ToastActor.error(ex.getMessage()));
                    }
                    refresh();
                }
            });

            Table row = new Table();
            row.add(timerLabel).padRight(4f);
            row.add(speedupBtn).size(58f, 37f);
            bottomOverlay.add(row).padBottom(-14f);
            stack.add(bottomOverlay);

            liveTimers.add(new LiveTimer(pot, timerLabel, false));
        }

        // کلیک روی کلِ جایگاه: فقط وقتی حالت بیل فعاله، برداشت می‌کنه.
        stack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                if (!shovelMode) return;
                try {
                    String reward = gs.collectPot(user, px, py);
                    showToast(ToastActor.success(reward != null ? stripAnsi(reward) : "Harvested!"));
                } catch (Exception ex) {
                    showToast(ToastActor.error(ex.getMessage()));
                }
                shovelMode = false;
                updateShovelVisual();
                refresh();
            }
        });

        return stack;
    }

    /**
     * انیمیشنِ idle گیاهِ داخلِ گلدان را می‌سازد. نامِ گیاه در Pot به‌صورت String
     * ذخیره شده؛ به {@link PlantType} تبدیل می‌کنیم. اگر نگاشت نشد (مثلاً MARIGOLD
     * که PlantType نیست) → null و فقط تصویرِ گلدان نمایش داده می‌شود.
     */
    private PamIdleActor createPlantIdle(String plantTypeName) {
        if (plantTypeName == null) return null;
        try {
            PlantType type = PlantType.valueOf(plantTypeName.toUpperCase());
            return PamIdleActor.forPlant(type, null);
        } catch (Exception e) {
            return null;
        }
    }

    // =========================================================================
    //  بیل — حالت برداشت
    // =========================================================================

    private Actor buildShovelButton() {
        Image shovel = new Image(GameAssets.getInstance().local(AssetIds.GH_ICON_SHOVEL));
        shovel.setScaling(Scaling.fit);
        shovel.setSize(64f, 64f);
        shovel.setOrigin(32f, 32f);
        shovel.setPosition(GameConstants.VIEWPORT_WIDTH - 84f, 20f);
        shovel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                shovelMode = !shovelMode;
                updateShovelVisual();
            }
        });
        shovelActor = shovel;
        updateShovelVisual();
        return shovel;
    }

    private void updateShovelVisual() {
        if (shovelActor == null) return;
        if (shovelMode) shovelActor.setColor(1f, 0.85f, 0.4f, 1f); // هایلایت زرد وقتی فعاله
        else shovelActor.setColor(1f, 1f, 1f, 1f);
        shovelActor.setScale(shovelMode ? 1.15f : 1f);
    }

    // =========================================================================
    //  هدر — مطابق مرجع: چپ (Back/Quests/Minigames)، راست (PlantFood/Gems+Earn/Coins+Earn/Store)
    // =========================================================================

    private Table buildHeader(Skin skin) {
        User user = AppState.getInstance().getCurrentUser();

        Table header = new Table();
        header.pad(14f, 18f, 0f, 18f);

        Table left = new Table();
        left.add(clickableIcon(AssetIds.GH_ICON_BACK, 60f, 57f, () -> goTo(ScreenId.MAIN_MENU))).padRight(8f);
        left.add(clickableIcon(AssetIds.GH_ICON_QUESTS, 56f, 54f, () -> goTo(ScreenId.QUEST))).padRight(8f);
        left.add(clickableIcon(AssetIds.GH_ICON_MINIGAMES, 56f, 54f,
                () -> goTo(ScreenId.QUEST)));

        long coins = user != null ? user.getCoins() : 0;
        int gems = user != null ? user.getGems() : 0;
        int plantFood = user != null ? user.getPlantFoodCount() : 0;

        Table right = new Table();

        Table plantFoodCol = new Table();
        plantFoodCol.add(currencyWidget(skin, AssetIds.GH_WIDGET_PLANTFOOD, 150f, 54f,
                String.valueOf(plantFood), Color.LIME, () -> goTo(ScreenId.SHOP))).row();
        right.add(plantFoodCol).padRight(10f);

        Table gemsCol = new Table();
        gemsCol.add(currencyWidget(skin, AssetIds.GH_WIDGET_GEM, 150f, 54f,
                String.valueOf(gems), Color.CYAN, () -> goTo(ScreenId.SHOP))).padBottom(4f).row();
        TextButton earnGems = new TextButton("EARN GEMS!", skin, "purple");
        earnGems.getLabel().setFontScale(0.7f);
        earnGems.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { goTo(ScreenId.SHOP); }
        });
        gemsCol.add(earnGems).width(150f).height(34f);
        right.add(gemsCol).padRight(10f);

        Table coinsCol = new Table();
        coinsCol.add(currencyWidget(skin, AssetIds.GH_WIDGET_COIN, 170f, 54f,
                String.valueOf(coins), Color.GOLD, () -> goTo(ScreenId.SHOP))).padBottom(4f).row();
        TextButton earnCoins = new TextButton("EARN COINS!", skin, "brown");
        earnCoins.getLabel().setFontScale(0.7f);
        earnCoins.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { goTo(ScreenId.SHOP); }
        });
        coinsCol.add(earnCoins).width(170f).height(34f);
        right.add(coinsCol).padRight(10f);

        right.add(clickableIcon(AssetIds.GH_ICON_STORE, 64f, 64f, () -> goTo(ScreenId.SHOP)));

        header.add(left).left().top().expandX();
        header.add(right).right().top();
        return header;
    }

    private Image clickableIcon(String localAsset, float w, float h, Runnable action) {
        Image img = new Image(GameAssets.getInstance().local(localAsset));
        img.setScaling(Scaling.fit);
        img.setSize(w, h);
        img.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { action.run(); }
        });
        return img;
    }

    /** ویجت پیل‌مانند سکه/الماس/بذر: تصویر کامل + عدد روی نوار مشکی. */
    private Stack currencyWidget(Skin skin, String widgetAsset, float w, float h,
                                 String amountText, Color amountColor, Runnable onClick) {
        Image bg = new Image(GameAssets.getInstance().local(widgetAsset));
        bg.setScaling(Scaling.stretch);
        bg.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { onClick.run(); }
        });

        Label amount = new Label(amountText, skin, "default");
        amount.setColor(amountColor);
        Table amountLayer = new Table();
        amountLayer.left();
        amountLayer.add(amount).padLeft(w * 0.28f);

        Stack stack = new Stack();
        stack.setSize(w, h);
        stack.add(bg);
        stack.add(amountLayer);
        return stack;
    }

    // =========================================================================
    //  کمکی
    // =========================================================================

    private String formatRemaining(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        return h + "h " + m + "m";
    }

    private String stripAnsi(String s) {
        return s.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    private void refresh() {
        stage.clear();
        buildUi();
    }

    private void showToast(ToastActor t) {
        t.setPosition(GameConstants.VIEWPORT_WIDTH / 2f - t.getWidth() * 0.5f, 80);
        stage.addActor(t);
    }

    @Override
    public void render(float delta) {
        GameAssets.getInstance().update();
        ScreenUtils.clear(0.05f, 0.12f, 0.05f, 1);

        timerAccumulator += delta;
        if (timerAccumulator >= 1f) {
            timerAccumulator = 0f;
            boolean needsRefresh = false;
            for (LiveTimer t : liveTimers) {
                if (t.pot.isReady()) { needsRefresh = true; break; }
                t.label.setText(formatRemaining(t.pot.getRemainingSeconds()));
            }
            if (needsRefresh) { refresh(); }
        }

        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void dispose() { if (stage != null) stage.dispose(); }
}
