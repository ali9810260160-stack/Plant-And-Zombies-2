package com.pvz2.view.game.anim;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.pvz2.controller.GameController;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.graphics.util.GameCoords;
import com.pvz2.model.enums.ArmorType;
import com.pvz2.view.game.anim.state.ZombieAnimState;
import com.pvz2.model.GameSession;
import com.pvz2.model.Projectile;
import com.pvz2.model.Sun;
import com.pvz2.model.enums.AnimEvent;
import com.pvz2.model.enums.ChapterType;
import com.pvz2.model.enums.PlantType;
import com.pvz2.model.plants.GenericPlant;
import com.pvz2.model.plants.Plant;
import com.pvz2.view.game.anim.state.PlantAnimState;
import com.pvz2.model.tiles.Tile;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.view.game.anim.action.ZombieAction;
import com.pvz2.view.game.anim.action.ZombieActionRegistry;
import com.pvz2.view.game.anim.action.impl.ZombieActionFactory;
import com.pvz2.view.game.anim.config.AnimConfigLoader;
import com.pvz2.view.game.anim.config.PlantAnimConfig;
import com.pvz2.view.game.anim.config.ProjectileAnimConfig;
import com.pvz2.view.game.anim.config.ZombieAnimConfig;
import com.pvz2.view.game.anim.controller.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * سیستم مرکزی انیمیشن — هماهنگ‌کننده کل View انیمیشن.
 *
 * ─── مسئولیت‌ها ──────────────────────────────────────────────────
 *   ۱. نگهداری یک PlantAnimController به ازای هر گیاه روی نقشه
 *   ۲. نگهداری یک ZombieAnimController به ازای هر زامبی فعال
 *   ۳. خواندن AnimEvent از مدل، دادن به controller، پاک کردن آن
 *   ۴. به‌روزرسانی همه controller ها هر فریم
 *   ۵. رندر همه entity ها به ترتیب صحیح (depth sorting)
 *   ۶. مدیریت life cycle: spawn (اضافه‌شدن entity جدید) و death (حذف)
 *
 * ─── ترتیب رندر ──────────────────────────────────────────────────
 *   (هر لایه back-to-front رندر می‌شود)
 *   ۱. گیاهان لایه دوم (Lily Pad، Pumpkin): ردیف ۱ به ۵
 *   ۲. گیاهان لایه اول: ردیف ۱ به ۵
 *   ۳. زامبی‌ها: depth sorted by Y (ردیف پایین‌تر جلوتر)
 *   ۴. پرتابه‌ها
 *   ۵. خورشیدها (روی همه چیز)
 *
 * ─── نحوه استفاده در GameScreen ─────────────────────────────────
 * <pre>
 *   // در create():
 *   animSystem = new AnimationSystem(pamPlayer, gameController);
 *
 *   // در render():
 *   GameSession session = appState.getCurrentSession();
 *   animSystem.update(delta, session);
 *
 *   batch.begin();
 *   animSystem.render(batch, session, delta);
 *   batch.end();
 *
 *   // در dispose():
 *   animSystem.dispose();
 * </pre>
 */
public class AnimationSystem {

    private static final String TAG = "AnimationSystem";

    /** کسری از کلیپِ حمله که در آن پرتابه شلیک می‌شود (شلیکِ طبیعی‌تر، ~۵۳٪). */
    private static final float FIRE_FRACTION = 0.53f;
    /** طولِ پیش‌فرضِ کلیپِ حمله تا وقتی PAM هنوز حل نشده (ثانیه). */
    private static final float DEFAULT_ATTACK_DUR = 0.6f;

    // ─── ابزارهای اصلی ───────────────────────────────────────────
    private final AnimConfigLoader    configLoader;
    private final ZombieActionRegistry actionRegistry;
    private final Object              pamPlayer;   // PamPlayer از libPVZ

    // ─── Controller pools ────────────────────────────────────────
    /**
     * یک PlantAnimController به ازای هر Plant روی نقشه.
     * کلید: plant instance (identity hash) — تضمین می‌کند دو گیاه در یک خانه
     * (secondLayer) هر کدام controller جداگانه دارند.
     */
    private final Map<Plant,  PlantAnimController>  plantControllers  = new IdentityHashMap<>();
    private final Map<Zombie, ZombieAnimController> zombieControllers = new IdentityHashMap<>();

    // ─── رندرکننده‌های مشترک ─────────────────────────────────────
    private final ProjectileRenderer projectileRenderer;
    private final SunAnimController  sunAnimController;

    // ─── ردیابی ID پرتابه‌ها ─────────────────────────────────────
    private int nextProjectileId = 0;
    private final Map<Projectile, Integer> projectileIds = new IdentityHashMap<>();

    // ─── لیست موقت برای depth sort ──────────────────────────────
    private final List<ZombieAnimController> sortedZombies = new ArrayList<>();

    /**
     * ═══ فیلد جدید نسخه ۲ ═══
     * فصل فعلی — برای resolve کردن config زامبی‌های وابسته به فصل
     * (NORMAL/GARGANTUAR/IMP و مشتقات مجازی CONEHEAD/BUCKETHEAD/...).
     * هر فریم در update() از session.getGameMap().getChapter() خوانده می‌شود.
     */
    private ChapterType currentChapter = ChapterType.ANCIENT_EGYPT;

    /** موقعیتِ نشانگرِ موس در مختصاتِ world — برای درخششِ hover خورشیدها (CN3). */
    private float cursorX = -1e5f, cursorY = -1e5f;

    /** فراخوانی هر فریم از GameScreen (از طریق GameRenderer) با مختصاتِ world نشانگر. */
    public void setCursorWorld(float x, float y) { cursorX = x; cursorY = y; }

    // ─── ET2: افتادنِ سر/زره‌ی زامبی‌ها هنگامِ مرگ ─────────────────────────────
    /** ذرّاتِ پرتاب‌شونده (سر/زره) با مسیرِ سهموی. */
    private static final class DeathPart {
        float x, y, vx, vy, rot, rotSpeed, size, life, maxLife;
        boolean head;
        float cr, cg, cb;
    }
    private final List<DeathPart> deathParts = new ArrayList<>();
    private final Set<Zombie> deathPartsSpawned =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final java.util.Random rng = new java.util.Random();
    private static final float PART_GRAVITY = 700f;

    // ─── Request 2: پرتابِ کلیپِ «particles» (سر+دست) هنگامِ مرگ ────────────────
    /** یک گروهِ particles که به‌صورتِ سهموی پرتاب می‌شود و بعد محو می‌شود. */
    private static final class ParticlesThrow {
        String pam;
        float x0, y, vx, vy;   // موقعیت + سرعت (پیکسل/ثانیه)
        float life, maxLife;
        float groundY;         // سطحِ زمین (توقفِ سقوط)
        boolean landed;
    }
    private final List<ParticlesThrow> particleThrows = new ArrayList<>();
    private static final String PARTICLES_CLIP = "particles";

    // ─── Request 1: افتادنِ پارتِ واقعیِ بدن بر اساسِ جان ──────────────────────
    /** کلیدواژه‌ی نامِ پارتِ قابل‌افتادن (به‌ترتیبِ افتادن) + آستانه‌ی جانِ متناظر. */
    private static final String[] DROP_KEYWORDS   = { "arm_outer_lower", "arm_outer_upper" };
    private static final float[]  DROP_THRESHOLDS = { 0.6f, 0.3f };
    /** کشِ نامِ واقعیِ پارت‌های قابل‌افتادن برای هر PAM (از getParts). null = هنوز حل نشده. */
    private final Map<String, List<String>> droppableCache = new HashMap<>();

    /** یک پارتِ افتاده که با drawPart رندر و به‌صورتِ سهموی می‌افتد. */
    private static final class FallingPart {
        String pam, clip, part;
        float clipTime;
        float x, y, vx, vy, groundY;
        float life, maxLife;
        boolean landed;
    }
    private final List<FallingPart> fallingParts = new ArrayList<>();

    // ════════════════════════════════════════════════════════════
    //  سازنده
    // ════════════════════════════════════════════════════════════

    /**
     * @param pamPlayer      نمونه PamPlayer از libGDX — به عنوان Object
     *                        برای جلوگیری از وابستگی مستقیم در skeleton
     * @param gameController  کنترلر اصلی بازی — برای action ها
     */
    public AnimationSystem(Object pamPlayer, GameController gameController) {
        this.pamPlayer = pamPlayer;

        // بارگذاری config (شامل environment_animations.json برای سنگ‌قبر/
        // مورر/گردباد/مه/بلوک یخ — استفاده‌شده توسط GridRenderer)
        this.configLoader = new AnimConfigLoader(
            "data/character_animations.json",
            "data/environment_animations.json"
        );

        // ساخت action registry
        // ═══ ویژگی جدید نسخه ۲ ═══ — this پاس داده می‌شود تا Action هایی
        // که ability چندمرحله‌ای دارند (مثل COLLECT_SUN/HOOK_NEAREST_PLANT)
        // بتوانند پس از پایان منطق خودشان animationSystem.endAbilityFor(zombie)
        // را صدا بزنند تا فاز EXIT (مثل power_down/reel) پخش شود.
        this.actionRegistry = new ZombieActionRegistry();
        for (ZombieAction action : ZombieActionFactory.createAll(gameController, this)) {
            actionRegistry.register(action);
        }

        // ساخت رندرکننده‌های مشترک
        this.projectileRenderer = new ProjectileRenderer(buildProjectileConfigMap());
        this.sunAnimController  = new SunAnimController();

        Gdx.app.log(TAG, "AnimationSystem آماده — "
            + configLoader.getPlantCount() + " گیاه، "
            + configLoader.getZombieCount() + " زامبی");
    }

    // ════════════════════════════════════════════════════════════
    //  Update  (هر فریم، قبل از render)
    // ════════════════════════════════════════════════════════════

    /**
     * به‌روزرسانی کامل همه controller ها.
     * باید قبل از render() و بعد از CombatService.processTick() صدا زده شود.
     *
     * @param delta   ثانیه گذشته از آخرین فریم
     * @param session وضعیت فعلی بازی
     */
    public void update(float delta, GameSession session) {
        if (session == null || session.getGameMap() == null) return;

        // ═══ ویژگی جدید نسخه ۲: به‌روزرسانی فصل فعلی برای resolve زامبی‌ها ═══
        if (session.getGameMap().getChapter() != null) {
            currentChapter = session.getGameMap().getChapter();
        }

        // ── sync entity ها ────────────────────────────────────────
        syncPlantControllers(session);
        syncZombieControllers(session);
        syncProjectileIds(session);

        // ── گیاهان ───────────────────────────────────────────────
        for (Map.Entry<Plant, PlantAnimController> e : plantControllers.entrySet()) {
            Plant plant = e.getKey();
            PlantAnimController ctrl = e.getValue();

            // خواندن و پاک کردن event از مدل
            AnimEvent ev = plant.getPendingAnimEvent();
            plant.clearPendingAnimEvent();
            ctrl.consumeEvent(ev);

            ctrl.update(delta);

            // شلیکِ پرتابه در ~۵۳٪ کلیپِ حمله — تا شلیک با انیمیشن هماهنگ و طبیعی‌تر شود.
            if (plant instanceof GenericPlant) {
                GenericPlant gp = (GenericPlant) plant;
                if (gp.isShotPending()) {
                    if (ctrl.getCurrentState() != PlantAnimState.ATTACK) {
                        gp.releaseShot(session);   // انیمیشنِ حمله فعال نیست → فوری شلیک کن
                    } else {
                        float dur = PamDrawUtil.clipDuration(
                                pamPlayer, ctrl.getPamPath(), ctrl.getAttackClip());
                        if (dur <= 0f) dur = DEFAULT_ATTACK_DUR;
                        if (ctrl.getStateTime() >= dur * FIRE_FRACTION) gp.releaseShot(session);
                    }
                }
            }
        }

        // ── زامبی‌ها ─────────────────────────────────────────────
        for (Map.Entry<Zombie, ZombieAnimController> e : zombieControllers.entrySet()) {
            Zombie zombie = e.getKey();
            ZombieAnimController ctrl = e.getValue();

            AnimEvent ev = zombie.getPendingAnimEvent();
            zombie.clearPendingAnimEvent();
            ctrl.consumeEvent(ev);

            ctrl.update(delta);

            // ET2/Request2: هنگامِ ورود به حالتِ مرگ، کلیپِ «particles» (سر+دست)
            // به‌صورتِ سهموی پرتاب می‌شود — هم‌زمان با کلیپِ die. زره‌های باقی‌مانده هم می‌افتند.
            if (ctrl.getCurrentState() == ZombieAnimState.DYING
                    && !deathPartsSpawned.contains(zombie)) {
                deathPartsSpawned.add(zombie);
                spawnParticlesThrow(zombie, ctrl.getPamPath());
                spawnDeathArmorParts(zombie);
            }

            // Request 1: افتادنِ پارتِ واقعیِ بدن بر اساسِ جان (جایگزینِ hand-fallِ رویه‌ای).
            if (ctrl.getCurrentState() != ZombieAnimState.DYING
                    && zombie.getCurrentHealth() > 0 && zombie.getMaxHealth() > 0
                    && !isGargantuar(zombie)) {
                checkBodyPartDrops(zombie, ctrl);
            }
            for (String part : ctrl.drainPendingFallSpawns()) {
                spawnFallingPart(zombie, ctrl.getPamPath(), ctrl.getRenderClip(),
                        ctrl.getStateTime(), part);
            }
        }

        updateDeathParts(delta);
        updateParticleThrows(delta);
        updateFallingParts(delta);

        // ── خورشیدها ─────────────────────────────────────────────
        Set<Sun> activeSuns = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Sun sun : session.getActiveSuns()) {
            activeSuns.add(sun);
            sunAnimController.update(delta, sun);
        }
        // خورشیدهایی که از activeSuns حذف شده‌اند ولی انیمیشنِ جمع‌آوریشان هنوز
        // تمام نشده را ادامه بده؛ بقیه را از state پاک کن (جلوگیری از نشتِ حافظه).
        for (Sun sun : new ArrayList<>(sunAnimController.trackedSuns())) {
            if (activeSuns.contains(sun)) continue;
            if (sunAnimController.shouldKeepAnimating(sun)) {
                sunAnimController.update(delta, sun);
            } else {
                sunAnimController.removeSun(sun);
            }
        }

        // ── پاکسازی entity های مرده ──────────────────────────────
        plantControllers.entrySet().removeIf(e -> e.getValue().isDeadAndDone());
        Iterator<Map.Entry<Zombie, ZombieAnimController>> zit =
                zombieControllers.entrySet().iterator();
        while (zit.hasNext()) {
            Map.Entry<Zombie, ZombieAnimController> e = zit.next();
            if (e.getValue().isDeadAndDone()) {
                deathPartsSpawned.remove(e.getKey()); // جلوگیری از نشتِ Set
                zit.remove();
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Render  (هر فریم، داخل batch.begin()...batch.end())
    // ════════════════════════════════════════════════════════════

    /**
     * رندر کامل همه entity های متحرک.
     * باید بین batch.begin() و batch.end() صدا زده شود.
     *
     * @param batch   SpriteBatch در حالت begin()
     * @param session وضعیت فعلی بازی
     * @param delta   ثانیه گذشته (برای ProjectileRenderer)
     */
    public void render(SpriteBatch batch, GameSession session, float delta) {
        if (session == null || session.getGameMap() == null) return;

        // ── ۱. گیاهان لایه دوم (ردیف ۱ به ۵) ─────────────────
        renderSecondLayerPlants(batch, session);

        // ── ۲. گیاهان اصلی (ردیف ۱ به ۵) ──────────────────────
        renderMainPlants(batch, session);

        // ── ۳. زامبی‌ها (depth sorted) ─────────────────────────
        renderZombiesSorted(batch);

        // ── ۴. پرتابه‌ها ────────────────────────────────────────
        renderProjectiles(batch, session, delta);

        // ── ۵. خورشیدها ─────────────────────────────────────────
        renderSuns(batch, session);

        // ── ۶. قطعاتِ پرتاب‌شده‌ی مرگِ زامبی (زره) + کلیپِ particles (سر/دست) ──
        renderDeathParts(batch);
        renderParticleThrows(batch);
        renderFallingParts(batch);
    }

    // ════════════════════════════════════════════════════════════
    //  ET2: قطعاتِ مرگِ زامبی (سر/زره با مسیرِ سهموی)
    // ════════════════════════════════════════════════════════════

    /** زره‌های باقی‌مانده هنگامِ مرگ می‌افتند (سر/دست حالا از کلیپِ particles می‌آیند). */
    private void spawnDeathArmorParts(Zombie z) {
        float sx = GameCoords.toScreenX(z.getX());
        float sy = GameCoords.toScreenY(z.getY());

        if (z.getArmors() != null) {
            for (Map.Entry<ArmorType, Integer> e : z.getArmors().entrySet()) {
                if (e.getValue() <= 0) continue;
                DeathPart a = new DeathPart();
                a.head = false;
                a.x = sx; a.y = sy + 30f;
                a.vx = (rng.nextFloat() - 0.5f) * 200f;
                a.vy = 160f + rng.nextFloat() * 120f;
                a.rotSpeed = (rng.nextFloat() - 0.5f) * 540f;
                a.size = 22f;
                a.maxLife = a.life = 1.2f;
                float[] c = armorColor(e.getKey());
                a.cr = c[0]; a.cg = c[1]; a.cb = c[2];
                deathParts.add(a);
            }
        }
    }

    private boolean isGargantuar(Zombie z) {
        return z.getType() != null && z.getType().name().contains("GARGANTUAR");
    }

    // ─── Request 2: پرتابِ کلیپِ particles هنگامِ مرگ ─────────────────────────

    private void spawnParticlesThrow(Zombie z, String pam) {
        if (pam == null || pam.isEmpty()) return;
        ParticlesThrow pt = new ParticlesThrow();
        pt.pam = pam;
        float sx = GameCoords.toScreenX(z.getX());
        float sy = GameCoords.toScreenY(z.getY());
        pt.x0 = sx;
        pt.y  = sy + GameConstants.TH * 0.4f;      // شروع از حوالیِ سر
        pt.groundY = sy - GameConstants.TH * 0.1f; // سطحِ توقفِ سقوط
        float dir = rng.nextBoolean() ? 1f : -1f;
        pt.vx = dir * (40f + rng.nextFloat() * 90f);
        pt.vy = 240f + rng.nextFloat() * 130f;     // پرتاب به بالا (سهموی)
        pt.maxLife = pt.life = 2.0f;
        particleThrows.add(pt);
    }

    private void updateParticleThrows(float delta) {
        Iterator<ParticlesThrow> it = particleThrows.iterator();
        while (it.hasNext()) {
            ParticlesThrow pt = it.next();
            pt.life -= delta;
            if (pt.life <= 0f) { it.remove(); continue; }
            if (!pt.landed) {
                pt.vy -= PART_GRAVITY * delta;
                pt.x0 += pt.vx * delta;
                pt.y  += pt.vy * delta;
                if (pt.y <= pt.groundY) { pt.y = pt.groundY; pt.landed = true; }
            }
        }
    }

    private void renderParticleThrows(SpriteBatch batch) {
        if (particleThrows.isEmpty()) return;
        Color saved = batch.getColor().cpy();
        for (ParticlesThrow pt : particleThrows) {
            float a = Math.min(1f, pt.life / 0.5f); // محوِ نرمِ ۰٫۵ ثانیه‌ی آخر
            batch.setColor(1f, 1f, 1f, a);
            // کلیپِ particles «ثابت» (time=0) پرتاب‌شده به (x,y)؛ هم‌مقیاسِ بدن.
            // fallbackِ نامرئی (alpha 0) تا اگر کلیپ نبود، مستطیلی دیده نشود.
            PamDrawUtil.draw(pamPlayer, batch, pt.pam, PARTICLES_CLIP, 0f,
                    pt.x0, pt.y, false, GameConstants.TW * 1.1f, GameConstants.TH * 1.9f,
                    new Color(0f, 0f, 0f, 0f));
        }
        batch.setColor(saved);
    }

    // ─── Request 1: افتادنِ پارتِ بدن ────────────────────────────────────────

    private void checkBodyPartDrops(Zombie z, ZombieAnimController ctrl) {
        String pam = ctrl.getPamPath();
        if (pam == null || pam.isEmpty()) return;
        if (!(pamPlayer instanceof pvz.libpvz.pam.PamPlayer)) return;
        pvz.libpvz.pam.PamPlayer p = (pvz.libpvz.pam.PamPlayer) pamPlayer;

        // فقط وقتی PAM از قبل bake شده (جلوگیری از loadِ همگام و هیچِ getParts).
        String clip = ctrl.getRenderClip();
        if (clip == null || p.getClip(pam, clip) == null) return;

        List<String> parts = resolveDroppableParts(p, pam);
        if (parts == null) return;

        float frac = (float) z.getCurrentHealth() / z.getMaxHealth();
        for (int i = 0; i < parts.size() && i < DROP_THRESHOLDS.length; i++) {
            String partName = parts.get(i);
            if (partName == null) continue;
            if (frac <= DROP_THRESHOLDS[i] && !ctrl.hasDroppedPart(partName)) {
                ctrl.dropBodyPart(partName);
            }
        }
    }

    /** نامِ واقعیِ پارت‌های قابل‌افتادن برای این PAM (یکی برای هر DROP_KEYWORD؛ ممکن است null). */
    private List<String> resolveDroppableParts(pvz.libpvz.pam.PamPlayer p, String pam) {
        List<String> cached = droppableCache.get(pam);
        if (cached != null) return cached;
        pvz.libpvz.pam.PamPlayer.AnimationPart root;
        try { root = p.getParts(pam); } catch (Exception e) { return null; }
        if (root == null) return null;
        List<String> result = new ArrayList<>();
        for (String kw : DROP_KEYWORDS) result.add(findPartByKeyword(root, kw));
        droppableCache.put(pam, result);
        return result;
    }

    private String findPartByKeyword(pvz.libpvz.pam.PamPlayer.AnimationPart node, String kw) {
        if (node == null) return null;
        if (node.name != null && node.name.contains(kw)) return node.name;
        for (pvz.libpvz.pam.PamPlayer.AnimationPart c : node.children) {
            String r = findPartByKeyword(c, kw);
            if (r != null) return r;
        }
        return null;
    }

    private void spawnFallingPart(Zombie z, String pam, String clip, float clipTime, String part) {
        if (pam == null || clip == null || part == null) return;
        FallingPart fp = new FallingPart();
        fp.pam = pam; fp.clip = clip; fp.part = part; fp.clipTime = clipTime;
        float sx = GameCoords.toScreenX(z.getX());
        float sy = GameCoords.toScreenY(z.getY());
        fp.x = sx; fp.y = sy;                       // شروع دقیقاً هم‌ترازِ بدن (drawPart آفستِ canvas را دارد)
        fp.groundY = sy - GameConstants.TH * 0.12f;
        float dir = rng.nextBoolean() ? 1f : -1f;
        fp.vx = dir * (30f + rng.nextFloat() * 70f);
        fp.vy = 80f + rng.nextFloat() * 80f;        // کمی پرش سپس سقوط
        fp.maxLife = fp.life = 2.0f;                // ۲ ثانیه سپس محو
        fallingParts.add(fp);
    }

    private void updateFallingParts(float delta) {
        Iterator<FallingPart> it = fallingParts.iterator();
        while (it.hasNext()) {
            FallingPart fp = it.next();
            fp.life -= delta;
            if (fp.life <= 0f) { it.remove(); continue; }
            if (!fp.landed) {
                fp.vy -= PART_GRAVITY * delta;
                fp.x += fp.vx * delta;
                fp.y += fp.vy * delta;
                if (fp.y <= fp.groundY) { fp.y = fp.groundY; fp.landed = true; }
            }
        }
    }

    private void renderFallingParts(SpriteBatch batch) {
        if (fallingParts.isEmpty()) return;
        Color saved = batch.getColor().cpy();
        for (FallingPart fp : fallingParts) {
            float a = Math.min(1f, fp.life / 0.5f);
            batch.setColor(1f, 1f, 1f, a);
            PamDrawUtil.drawPart(pamPlayer, batch, fp.pam, fp.clip, fp.clipTime,
                    fp.x, fp.y, fp.part, GameConstants.TH * 1.9f);
        }
        batch.setColor(saved);
    }

    private float[] armorColor(ArmorType t) {
        switch (t) {
            case CONE:   return new float[]{0.95f, 0.55f, 0.15f}; // نارنجیِ مخروط
            case BUCKET: return new float[]{0.70f, 0.72f, 0.75f}; // فلزیِ سطل
            default:     return new float[]{0.60f, 0.60f, 0.62f};
        }
    }

    private void updateDeathParts(float delta) {
        Iterator<DeathPart> it = deathParts.iterator();
        while (it.hasNext()) {
            DeathPart p = it.next();
            p.life -= delta;
            if (p.life <= 0f) { it.remove(); continue; }
            p.vy -= PART_GRAVITY * delta;
            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.rot += p.rotSpeed * delta;
        }
    }

    private void renderDeathParts(SpriteBatch batch) {
        if (deathParts.isEmpty()) return;
        TextureRegion disc  = GameAssets.getInstance().getDiscRegion();
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        for (DeathPart p : deathParts) {
            float a = Math.min(1f, p.life / 0.4f); // محوِ نرم در ۰٫۴ ثانیه‌ی آخر
            if (p.head) {
                // سر: دیسکِ سبز-خاکستری + یک لکه‌ی تیره‌ی چرخان (صورت) برای حسِ چرخش
                batch.setColor(p.cr, p.cg, p.cb, a);
                batch.draw(disc, p.x - p.size / 2f, p.y - p.size / 2f, p.size, p.size);
                float fr = p.size * 0.22f;
                float fx = p.x + (float) Math.cos(Math.toRadians(p.rot)) * p.size * 0.18f;
                float fy = p.y + (float) Math.sin(Math.toRadians(p.rot)) * p.size * 0.18f;
                batch.setColor(0.18f, 0.22f, 0.14f, a);
                batch.draw(disc, fx - fr, fy - fr, fr * 2f, fr * 2f);
            } else {
                // زره: مربعِ چرخان
                batch.setColor(p.cr, p.cg, p.cb, a);
                batch.draw(white, p.x - p.size / 2f, p.y - p.size / 2f,
                        p.size / 2f, p.size / 2f, p.size, p.size, 1f, 1f, p.rot);
            }
        }
        batch.setColor(Color.WHITE);
    }

    // ════════════════════════════════════════════════════════════
    //  Sync — هماهنگ‌سازی controller ها با مدل
    // ════════════════════════════════════════════════════════════

    /**
     * اضافه کردن controller برای گیاهان جدید روی نقشه.
     * هر گیاهی که controller ندارد، یکی برایش ساخته می‌شود.
     */
    private void syncPlantControllers(GameSession session) {
        for (int row = 1; row <= 5; row++) {
            for (int col = 1; col <= 9; col++) {
                Tile tile = session.getGameMap().getTile(col, row);
                if (tile == null) continue;

                // لایه اول
                if (tile.getPlant() != null) {
                    ensurePlantController(tile.getPlant());
                }
                // لایه دوم (Lily Pad، Pumpkin)
                if (tile.getSecondLayerPlant() != null) {
                    ensurePlantController(tile.getSecondLayerPlant());
                }
            }
        }
    }

    private void ensurePlantController(Plant plant) {
        if (plantControllers.containsKey(plant)) return;

        PlantAnimConfig cfg = configLoader.getPlantConfig(plant.getType());
        if (cfg == null) {
            Gdx.app.log(TAG, "config گیاه یافت نشد: " + plant.getType().name());
            return;
        }
        plantControllers.put(plant, new PlantAnimController(plant, cfg));
    }

    /**
     * اضافه کردن controller برای زامبی‌های جدید.
     */
    private void syncZombieControllers(GameSession session) {
        for (Zombie zombie : session.getActiveZombies()) {
            if (!zombieControllers.containsKey(zombie)) {
                ensureZombieController(zombie);
            }
        }
    }

    private void ensureZombieController(Zombie zombie) {
        // ═══ ویژگی جدید نسخه ۲: resolve با آگاهی از فصل ═══
        // ظاهر NORMAL/GARGANTUAR/IMP در هر فصل فرق دارد؛ CONEHEAD/BUCKETHEAD/
        // KNIGHT/BLOCKHEAD هم زامبی «مجازی» هستند (armor روی NORMAL همان فصل).
        // resolveZombieConfig هر دو مورد را به‌طور خودکار حل می‌کند.
        ChapterType chapter = currentChapter;
        ZombieAnimConfig cfg = configLoader.resolveZombieConfig(zombie.getType(), chapter);
        if (cfg == null) {
            Gdx.app.log(TAG, "config زامبی یافت نشد: " + zombie.getType().name()
                + " @ " + (chapter != null ? chapter.name() : "?"));
            return;
        }
        zombieControllers.put(zombie, new ZombieAnimController(zombie, cfg, actionRegistry));
    }

    /**
     * تخصیص ID به پرتابه‌های جدید و پاکسازی پرتابه‌های حذف‌شده.
     */
    private void syncProjectileIds(GameSession session) {
        Set<Projectile> active = new HashSet<>(session.getActiveProjectiles());

        // پاکسازی پرتابه‌های حذف‌شده
        Iterator<Map.Entry<Projectile, Integer>> it = projectileIds.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Projectile, Integer> e = it.next();
            if (!active.contains(e.getKey())) {
                projectileRenderer.removeProjectile(e.getValue());
                it.remove();
            }
        }

        // تخصیص ID به پرتابه‌های جدید
        for (Projectile proj : session.getActiveProjectiles()) {
            projectileIds.computeIfAbsent(proj, k -> nextProjectileId++);
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Render helpers
    // ════════════════════════════════════════════════════════════

    /**
     * گیاهان لایه دوم (Lily Pad زیر گیاه، یا Pumpkin روی گیاه) — طبق
     * {@code tile.getSecondLayerPlant()} دقیقاً همانی که در GameService هنگام
     * کاشت لایه‌ای ست می‌شود، نه یک حدس بر اساس نوع گیاه.
     *
     * <p>ترتیب کلی (این متد قبل از {@code renderMainPlants} صدا زده می‌شود)
     * یعنی لایه دوم <b>پشت</b> گیاه اصلی رسم می‌شود — درست برای Lily Pad
     * (که باید زیر گیاه رویش باشد). برای Pumpkin (که باید جلوی گیاه محافظت‌شده
     * باشد) این ترتیب برعکس ایده‌آل است؛ چون داده فاز ۱ کدام گیاه در کدام
     * لایه ذخیره شده را مشخص نمی‌کند (فقط «اول»/«دوم»)، این یک ساده‌سازی
     * شناخته‌شده است — برای عمده گیاهان لایه‌ای (Lily Pad که خیلی رایج‌تر
     * است) درست کار می‌کند.
     */
    private void renderSecondLayerPlants(SpriteBatch batch, GameSession session) {
        for (int row = 1; row <= 5; row++) {
            for (int col = 1; col <= 9; col++) {
                Tile tile = session.getGameMap().getTile(col, row);
                if (tile == null || tile.getSecondLayerPlant() == null) continue;
                PlantAnimController ctrl = plantControllers.get(tile.getSecondLayerPlant());
                if (ctrl != null) ctrl.render(batch, pamPlayer);
            }
        }
    }

    private void renderMainPlants(SpriteBatch batch, GameSession session) {
        // ترتیب: ردیف ۱ (بالا) به ردیف ۵ (پایین) — back-to-front
        for (int row = 1; row <= 5; row++) {
            for (int col = 1; col <= 9; col++) {
                Tile tile = session.getGameMap().getTile(col, row);
                if (tile == null || tile.getPlant() == null) continue;
                PlantAnimController ctrl = plantControllers.get(tile.getPlant());
                if (ctrl != null) ctrl.render(batch, pamPlayer);
            }
        }
    }

    /**
     * رندر زامبی‌ها با depth sorting:
     * زامبی‌های در ردیف‌های بالاتر (Y بزرگتر) پشت‌تر هستند و اول رندر می‌شوند.
     * زامبی‌های در یک ردیف: X بزرگتر (سمت راست، دورتر از خانه) پشت‌تر.
     */
    private void renderZombiesSorted(SpriteBatch batch) {
        sortedZombies.clear();
        sortedZombies.addAll(zombieControllers.values());

        // sort: Y کاهشی (ردیف بالا اول)، سپس X کاهشی
        sortedZombies.sort((a, b) -> {
            int yComp = Integer.compare(b.getZombie().getY(), a.getZombie().getY());
            if (yComp != 0) return yComp;
            return Double.compare(b.getZombie().getX(), a.getZombie().getX());
        });

        for (ZombieAnimController ctrl : sortedZombies) {
            ctrl.render(batch, pamPlayer);
        }
    }

    private void renderProjectiles(SpriteBatch batch, GameSession session, float delta) {
        for (Projectile proj : session.getActiveProjectiles()) {
            Integer id = projectileIds.get(proj);
            if (id != null) {
                projectileRenderer.render(batch, proj, delta, id, pamPlayer);
            }
        }
    }

    private void renderSuns(SpriteBatch batch, GameSession session) {
        Set<Sun> activeSuns = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Sun sun : session.getActiveSuns()) {
            activeSuns.add(sun);
            boolean isHovered = sunAnimController.isUnderCursor(sun, cursorX, cursorY);
            sunAnimController.render(batch, sun, pamPlayer, isHovered);
        }
        // خورشیدهای در حالِ جمع‌آوری که از activeSuns حذف شده‌اند (پروازِ به شمارنده).
        for (Sun sun : sunAnimController.trackedSuns()) {
            if (activeSuns.contains(sun)) continue;
            if (sunAnimController.shouldKeepAnimating(sun)) {
                sunAnimController.render(batch, sun, pamPlayer, false);
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Config helpers
    // ════════════════════════════════════════════════════════════

    /**
     * ساخت map از ProjectileType.name() به ProjectileAnimConfig — با ادغام
     * لیست projectiles همه گیاهانی که در JSON بارگذاری شده‌اند. اولین گیاهی
     * که یک ProjectileType را تعریف کند برنده می‌شود (اکثر گیاهان هم‌خانواده
     * پرتابه یکسان دارند، پس تداخل واقعی کم است).
     */
    private Map<String, ProjectileAnimConfig> buildProjectileConfigMap() {
        Map<String, ProjectileAnimConfig> map = new LinkedHashMap<>();
        for (PlantType type : PlantType.values()) {
            PlantAnimConfig pc = configLoader.getPlantConfig(type);
            if (pc == null || pc.projectiles == null) continue;
            for (ProjectileAnimConfig proj : pc.projectiles) {
                if (proj.id != null && !proj.id.isEmpty() && !map.containsKey(proj.id)) {
                    map.put(proj.id, proj);
                }
            }
        }
        return map;
    }

    // ════════════════════════════════════════════════════════════
    //  Public API
    // ════════════════════════════════════════════════════════════

    /**
     * اطلاع رسانی خارجی برای ارسال event به یک گیاه خاص.
     * مثال: هنگام Plant Food فعال شدن از GameController.
     */
    public void sendEventToPlant(Plant plant, AnimEvent event) {
        PlantAnimController ctrl = plantControllers.get(plant);
        if (ctrl != null) ctrl.consumeEvent(event);
    }

    /**
     * اطلاع رسانی خارجی برای ارسال event به یک زامبی خاص.
     */
    public void sendEventToZombie(Zombie zombie, AnimEvent event) {
        ZombieAnimController ctrl = zombieControllers.get(zombie);
        if (ctrl != null) ctrl.consumeEvent(event);
    }

    /**
     * ═══ متد جدید نسخه ۲ ═══ — برای SpawnImpAction: زامبی تازه‌اسپان‌شده
     * (Imp پرت‌شده توسط Gargantuar) را در حالت پرواز قرار می‌دهد.
     * اگر controller آن هنوز ساخته نشده (چون همین فریم اسپان شده)،
     * ابتدا آن را می‌سازد.
     */
    public void triggerFlyingFor(Zombie imp) {
        ZombieAnimController ctrl = zombieControllers.get(imp);
        if (ctrl == null) {
            ensureZombieController(imp);
            ctrl = zombieControllers.get(imp);
        }
        if (ctrl != null) ctrl.triggerFlying();
    }

    /** ═══ متد جدید نسخه ۲ ═══ — فرود Imp پس از پایان مسیر پرتابه‌ای. */
    public void triggerLandingFor(Zombie imp) {
        ZombieAnimController ctrl = zombieControllers.get(imp);
        if (ctrl != null) ctrl.triggerLanding();
    }

    /**
     * ═══ متد جدید نسخه ۲ ═══ — برای Action های چندمرحله‌ای (مثل
     * CollectSunAction برای Ra Zombie یا HookNearestPlantAction برای
     * Fisherman Zombie): پایان دستی فاز LOOP یک ability، برای شروع
     * فاز EXIT (مثل "power_down" یا "reel").
     */
    public void endAbilityFor(Zombie zombie) {
        ZombieAnimController ctrl = zombieControllers.get(zombie);
        if (ctrl != null) ctrl.endActiveAbility();
    }

    /** پاکسازی کامل (مثلاً هنگام بازگشت به منوی اصلی) */
    /**
     * ═══ متد جدید نسخه ۲ ═══
     * دسترسی خارجی به AnimConfigLoader (که environment_animations.json
     * را هم بارگذاری کرده) — برای اینکه GridRenderer بتواند سنگ‌قبر/مورر/
     * گردباد/مه را با PamDrawUtil واقعی رندر کند بدون parse دوباره JSON.
     */
    public AnimConfigLoader getConfigLoader() { return configLoader; }

    /** دسترسی خارجی به همان نمونه pamPlayer — برای GridRenderer. */
    public Object getPamPlayer() { return pamPlayer; }

    public void dispose() {
        plantControllers.clear();
        zombieControllers.clear();
        projectileIds.clear();
        sortedZombies.clear();
        sunAnimController.clear();
        projectileRenderer.clear();
        deathParts.clear();
        deathPartsSpawned.clear();
        nextProjectileId = 0;
    }

    /** تعداد controller های فعال (برای debug) */
    public int getActivePlantCount()  { return plantControllers.size(); }
    public int getActiveZombieCount() { return zombieControllers.size(); }
}
