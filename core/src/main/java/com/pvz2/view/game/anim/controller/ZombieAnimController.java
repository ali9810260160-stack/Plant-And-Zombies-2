package com.pvz2.view.game.anim.controller;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.util.GameCoords;
import com.pvz2.model.enums.AnimEvent;
import com.pvz2.model.enums.ArmorType;
import com.pvz2.model.enums.ZombieEffect;
import com.pvz2.model.zombies.ArmorDataRegistry;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.view.game.anim.action.ZombieAction;
import com.pvz2.view.game.anim.action.ZombieActionRegistry;
import com.pvz2.view.game.anim.config.*;
import com.pvz2.view.game.anim.state.ZombieAnimState;

import java.util.*;

/**
 * State Machine انیمیشن برای یک زامبی — نسخه ۲ (بر اساس بررسی مستقیم assets واقعی).
 *
 * ─── تغییرات نسخه ۲ نسبت به نسخه اول ────────────────────────────
 *
 *  ۱. رفع باگ مختصات: قبلاً {@code GRID_ORIGIN_Y + (row-1)*CELL_H} به‌کار
 *     می‌رفت که محور row را برعکس نمی‌کرد؛ اکنون از {@link GameCoords}
 *     استفاده می‌شود (که {@code TILE_ROWS - row} را اعمال می‌کند)، درست
 *     مثل GridRenderer و EntityRenderer. قبلاً همه زامبی‌ها وارونه
 *     (ردیف ۱ پایین صفحه، ردیف ۵ بالا) رندر می‌شدند.
 *
 *  ۲. forcedArmor: زامبی‌های «مجازی» مثل CONEHEAD/BUCKETHEAD/KNIGHT/BLOCKHEAD
 *     در واقعیت PAM جداگانه ندارند — همان NORMAL هر فصل هستند با یک armor
 *     ثابت. AnimConfigLoader.resolveZombieConfig این را resolve می‌کند و
 *     یک {@code forcedArmor} در ZombieAnimConfig می‌گذارد که این کنترلر
 *     باید همیشه به armor های واقعی زامبی اضافه کند.
 *
 *  ۳. دو الگوی armor: Overlay ساده (visMap) و Clip-Suffix کامل (مثل
 *     Newspaper Zombie: idle_newspaper ↔ idle + یک‌بار newspaper_defeat
 *     در لحظه گذار). به تفصیل در {@link ArmorAnimConfig}.
 *
 *  ۴. قابلیت‌های چندمرحله‌ای (enterClip→loopClip→exitClip) مثل Ra Zombie
 *     (power_up→power→power_down).
 */
public class ZombieAnimController {

    // ─── ثابت‌های اندازه (برای fallback rect؛ مختصات از GameCoords) ────
    public static final float CELL_W = 100f;
    public static final float CELL_H = 100f;

    private static final Color COLOR_CHILLED = new Color(0.55f, 0.75f, 1.00f, 1f);
    private static final Color COLOR_FROZEN  = new Color(0.30f, 0.50f, 1.00f, 1f);
    private static final Color COLOR_BURNING = new Color(1.00f, 0.40f, 0.10f, 1f);
    /** زامبیِ حاملِ غذای گیاه — سبزتر نمایش داده می‌شود. */
    private static final Color COLOR_GLOW    = new Color(0.45f, 1.00f, 0.45f, 1f);

    private final Zombie               zombie;
    private final ZombieAnimConfig     config;
    private final ZombieActionRegistry actionRegistry;

    private ZombieAnimState current    = ZombieAnimState.WALK;
    private ZombieAnimState requested  = ZombieAnimState.WALK;
    private float           stateTime  = 0f;
    private boolean         stateDone  = false;

    private String currentVariantId = "NORMAL";

    /** آخرین وضعیت هر armor (سالم/شکسته) برای تشخیص لحظه شکستن. */
    private final Map<ArmorType, Boolean> lastArmorPresent = new EnumMap<>(ArmorType.class);
    /** آیا در حال پخش breakEventClip یک armor خاص هستیم؟ */
    private ArmorType breakingArmor  = null;
    private float     breakEventTime = 0f;

    private final Map<String, Boolean> visMap = new LinkedHashMap<>();

    /** پارت‌های بدنی که بر اثرِ آسیب افتاده‌اند و باید روی بدن مخفی بمانند (Request 1). */
    private final Set<String> hiddenBodyParts = new HashSet<>();
    /** پارت‌هایی که همین‌الان افتادند و باید توسط AnimationSystem به‌صورتِ افتان spawn شوند. */
    private final List<String> pendingFallSpawns = new ArrayList<>();

    /** فاز فعلی special ability چندمرحله‌ای در حال اجرا: ENTER, LOOP, EXIT، یا null */
    private String activeAbilityPhase = null;
    private SpecialAbilityConfig activeAbility = null;

    private final Set<String> firedAbilities = new HashSet<>();
    private final Map<String, Float> periodicTimers = new LinkedHashMap<>();

    private float flashTimer = 0f;
    private static final float FLASH_DURATION = 0.12f;

    private boolean lastHypnotized = false;

    // ─── مرگِ سوخته (ash) — وقتی زامبی در حالِ سوختن می‌میرد، به‌جای انیمیشنِ
    //     مرگِ عادی، افکتِ خاکسترشدن پخش می‌شود (سوختن با جالاپینو/نخودِ آتشین). ──
    private static final String ASH_PAM      =
            "768/FULL/EFFECTS/ZOMBIE_BIGHEAD_ASH/ZOMBIE_BIGHEAD_ASH.PAM";
    private static final String ASH_PAM_GARG =
            "768/FULL/EFFECTS/ZOMBIE_BIGHEAD_GARGANTUAR_ASH/ZOMBIE_BIGHEAD_GARGANTUAR_ASH.PAM";
    private static final String ASH_PAM_IMP  =
            "768/FULL/EFFECTS/ZOMBIE_BIGHEAD_IMP_ASH/ZOMBIE_BIGHEAD_IMP_ASH.PAM";
    /** آیا زامبی هنگام مرگ در حالِ سوختن بود؟ (برای انتخابِ افکتِ خاکستر) */
    private boolean wasBurning = false;

    /** شمارنده‌ی زمانِ واقعیِ مرگ (مستقل از speedMul) — تضمینِ حذفِ جسد حتی اگر
     *  زامبی یخ‌زده بمیرد یا config مرحله‌ی DYING نداشته باشد. */
    private float deathRealTime = 0f;
    private static final float MAX_DEATH_TIME = 3.0f;

    public ZombieAnimController(Zombie zombie,
                                ZombieAnimConfig config,
                                ZombieActionRegistry actionRegistry) {
        this.zombie         = zombie;
        this.config         = config;
        this.actionRegistry = actionRegistry;

        for (SpecialAbilityConfig sa : config.specialAbilities) {
            if ("PERIODIC".equals(sa.trigger)) periodicTimers.put(sa.id, 0f);
        }

        rebuildVisMap();
        checkOnEnterAbilities();
    }

    // ════════════════════════════════════════════════════════════
    //  مصرف رویداد از مدل
    // ════════════════════════════════════════════════════════════

    public void consumeEvent(AnimEvent event) {
        if (event == null || event == AnimEvent.NONE) return;

        switch (event) {
            case STARTED_EATING:
                requestState(ZombieAnimState.EAT);
                break;
            case STOPPED_EATING:
                if (current == ZombieAnimState.EAT) requestState(ZombieAnimState.WALK);
                break;
            case ARMOR_BROKEN:
                rebuildVisMap();
                break;
            case HEALTH_VARIANT_CHANGED:
                break; // خودکار در update() بررسی می‌شود
            case TOOK_DAMAGE_ZOMBIE:
                flashTimer = FLASH_DURATION;
                break;
            case DYING_STARTED:
                forceTransition(ZombieAnimState.DYING);
                break;
            case SPECIAL_ABILITY_FIRED:
                requestState(ZombieAnimState.SPECIAL);
                break;
            case SPECIAL_ABILITY_ENDED:
                endActiveAbility();   // فازِ EXIT (مثلِ power_down) سپس بازگشت به WALK
                break;
            case HYPNOTIZED:
                break; // جهت مستقیماً در render() از zombie.isHypnotized() خوانده می‌شود
            default:
                break;
        }
    }

    /** فراخوانی مستقیم برای شروع پرواز (توسط SpawnImpAction هنگام پرتاب Imp). */
    public void triggerFlying() {
        if (config.getState("FLYING") != null) forceTransition(ZombieAnimState.FLYING);
    }

    /** فراخوانی مستقیم برای فرود Imp پس از پایان مسیر پرتابه‌ای. */
    public void triggerLanding() {
        if (config.getState("LANDING") != null) {
            forceTransition(ZombieAnimState.LANDING);
        } else {
            requestState(ZombieAnimState.WALK);
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Update
    // ════════════════════════════════════════════════════════════

    public void update(float delta) {
        // ردیابیِ سوختن: اگر همین حالا در حالِ سوختن است ثبت کن؛ در غیرِ این صورت
        // فقط تا وقتی زنده است reset کن (پس از آغازِ DYING مقدارش حفظ می‌شود).
        if (zombie.getActiveEffects().containsKey(ZombieEffect.BURNING)) {
            wasBurning = true;
        } else if (current != ZombieAnimState.DYING) {
            wasBurning = false;
        }

        syncStateFromModel();
        checkArmorChanges();

        if (requested.canOverride(current, stateDone)) {
            transitionTo(requested);
        }
        requested = ZombieAnimState.WALK;

        float speedMul = resolveSpeedMultiplier();
        StateConfig sc = getStateConfig(current);
        if (sc != null) speedMul *= sc.speedMul;
        stateTime += delta * speedMul;

        if (sc != null && !sc.loop && !stateDone) {
            float dur = estimateClipDuration(resolveClip());
            if (stateTime >= dur) {
                stateDone = true;
                advanceAbilityPhaseOrReturn(sc);
            }
        }

        if (breakingArmor != null) {
            breakEventTime += delta;
            if (breakEventTime >= 1.0f) breakingArmor = null;
        }

        checkSpecialAbilities(delta);
        updateHealthVariant();

        if (current == ZombieAnimState.DYING) deathRealTime += delta;

        if (flashTimer > 0) flashTimer = Math.max(0, flashTimer - delta);
    }

    private void advanceAbilityPhaseOrReturn(StateConfig sc) {
        if (activeAbility != null && "ENTER".equals(activeAbilityPhase) && activeAbility.clip != null) {
            activeAbilityPhase = "LOOP";
            stateTime = 0f;
            stateDone = false;
        } else if (activeAbility != null && "EXIT".equals(activeAbilityPhase)) {
            activeAbility = null;
            activeAbilityPhase = null;
            if (sc.returnTo != null) transitionTo(ZombieAnimState.valueOf(sc.returnTo));
        } else if (activeAbility != null && "LOOP".equals(activeAbilityPhase)) {
            if (activeAbility.exitClip == null) {
                activeAbility = null;
                activeAbilityPhase = null;
                if (sc.returnTo != null) transitionTo(ZombieAnimState.valueOf(sc.returnTo));
            }
        } else if (sc.returnTo != null) {
            transitionTo(ZombieAnimState.valueOf(sc.returnTo));
        }
    }

    private void syncStateFromModel() {
        if (zombie.getCurrentHealth() <= 0) {
            requestState(ZombieAnimState.DYING);
            return;
        }
        if (zombie.isAttacking()) {
            requestState(ZombieAnimState.EAT);
        }
        lastHypnotized = zombie.isHypnotized();
    }

    // ════════════════════════════════════════════════════════════
    //  Special Abilities
    // ════════════════════════════════════════════════════════════

    private void checkOnEnterAbilities() {
        for (SpecialAbilityConfig sa : config.specialAbilities) {
            if ("ON_ENTER".equals(sa.trigger) && !firedAbilities.contains(sa.id)) {
                fireAbility(sa);
                if (sa.once) firedAbilities.add(sa.id);
            }
        }
    }

    private void checkSpecialAbilities(float delta) {
        float hpRatio = getHpRatio();

        for (SpecialAbilityConfig sa : config.specialAbilities) {
            if (sa.once && firedAbilities.contains(sa.id)) continue;
            if (activeAbility != null) continue;

            boolean fire = false;
            switch (sa.trigger) {
                case "HP_BELOW":
                    fire = hpRatio <= sa.triggerParam;
                    break;
                case "PERIODIC":
                    float timer = periodicTimers.getOrDefault(sa.id, 0f) + delta;
                    periodicTimers.put(sa.id, timer);
                    if (timer >= sa.triggerParam) {
                        periodicTimers.put(sa.id, 0f);
                        fire = true;
                    }
                    break;
                case "ON_EAT":
                    fire = (current == ZombieAnimState.EAT);
                    break;
                default: break;
            }

            if (fire) {
                fireAbility(sa);
                if (sa.once) firedAbilities.add(sa.id);
            }
        }
    }

    private void fireAbility(SpecialAbilityConfig sa) {
        try {
            requestState(ZombieAnimState.valueOf(sa.animState));
        } catch (Exception ignored) { }

        if (sa.enterClip != null) {
            activeAbility = sa;
            activeAbilityPhase = "ENTER";
        }

        if (actionRegistry != null && sa.action != null && !sa.action.isEmpty()) {
            ZombieAction action = actionRegistry.getAction(sa.action);
            if (action != null) action.onStart(zombie, sa.actionParams);
        }
    }

    /** پایان دستی فاز LOOP یک ability چندمرحله‌ای (مثلاً وقتی COLLECT_SUN کامل شد). */
    public void endActiveAbility() {
        if (activeAbility == null) return;
        if (activeAbility.exitClip != null) {
            activeAbilityPhase = "EXIT";
            stateTime = 0f;
            stateDone = false;
        } else {
            activeAbility = null;
            activeAbilityPhase = null;
            requestState(ZombieAnimState.WALK);
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Armor: تشخیص تغییر + rebuild
    // ════════════════════════════════════════════════════════════

    private void checkArmorChanges() {
        for (Map.Entry<ArmorType, Integer> e : effectiveArmors().entrySet()) {
            boolean nowPresent = e.getValue() > 0;
            Boolean was = lastArmorPresent.get(e.getKey());
            if (was != null && was && !nowPresent) {
                onSingleArmorBroken(e.getKey());
            }
            lastArmorPresent.put(e.getKey(), nowPresent);
        }
    }

    private void onSingleArmorBroken(ArmorType type) {
        rebuildVisMap();
        throwBrokenArmorPart(type);      // زره‌ی شکسته پرت شود و مدتی روی زمین بماند
        ArmorAnimConfig armCfg = config.armors.get(type.name());
        if (armCfg != null && armCfg.isClipSuffixStyle() && armCfg.breakEventClip != null) {
            breakingArmor = type;
            breakEventTime = 0f;
        }
    }

    /**
     * پارتِ زرهِ شکسته را برای «پرتاب‌شدن و ماندنِ کوتاه روی زمین» به AnimationSystem
     * می‌سپارد — دقیقاً مثلِ دستِ افتاده‌ی زامبی (مسیرِ {@code pendingFallSpawns} →
     * {@code spawnFallingPart}: پرش سهموی، فرود، سپس محو). برای زره‌های overlayِ عمومی
     * (cone/bucket/brick) از نامِ پارتِ واقعی در آخرین مرحله‌ی آسیب ({@code _damage_02})
     * استفاده می‌شود؛ برای زره‌های overlayِ ویژه (visMap) از خودِ spriteها.
     */
    private void throwBrokenArmorPart(ArmorType type) {
        String base = armorPartBase(type);
        if (base != null) {
            pendingFallSpawns.add(base + "_damage_02");   // شکلِ همان لحظه‌ی شکستن
            return;
        }
        ArmorAnimConfig armCfg = config.armors.get(type.name());
        if (armCfg != null && armCfg.isOverlayStyle()) {
            for (String sprite : armCfg.visMap.keySet()) pendingFallSpawns.add(sprite);
        }
    }

    private void rebuildVisMap() {
        visMap.clear();
        for (ArmorAnimConfig armCfg : config.armors.values()) {
            for (String spriteName : armCfg.visMap.keySet()) visMap.put(spriteName, false);
        }
        for (Map.Entry<ArmorType, Integer> entry : effectiveArmors().entrySet()) {
            if (entry.getValue() > 0) {
                ArmorAnimConfig armCfg = config.armors.get(entry.getKey().name());
                if (armCfg != null && armCfg.isOverlayStyle()) {
                    visMap.putAll(armCfg.visMap);
                }
            }
        }
    }

    /**
     * زره‌های «مؤثر»: ترکیب زره‌های واقعی مدل (فاز ۱) + forcedArmor
     * (برای زامبی‌های مجازی مثل Conehead که ارثبری از NORMAL شده‌اند).
     * forcedArmor می‌تواند چند armor با "+" جدا شده باشد (Knight: HELMET+SHOULDER_ARMOR).
     */
    private Map<ArmorType, Integer> effectiveArmors() {
        if (config.forcedArmor == null) return zombie.getArmors();

        Map<ArmorType, Integer> merged = new EnumMap<>(ArmorType.class);
        merged.putAll(zombie.getArmors());
        for (String part : config.forcedArmor.split("\\+")) {
            try {
                merged.putIfAbsent(ArmorType.valueOf(part.trim()), 1);
            } catch (IllegalArgumentException e) {
                com.badlogic.gdx.Gdx.app.error("ZombieAnimController",
                    "forcedArmor نامعتبر: '" + part + "' برای " + config.zombieType);
            }
        }
        return merged;
    }

    // ════════════════════════════════════════════════════════════
    //  Health Variant (فقط اگر صراحتاً در JSON تعریف شده باشد)
    // ════════════════════════════════════════════════════════════

    private void updateHealthVariant() {
        float ratio = getHpRatio();
        String newVariant = config.getHealthVariantId(ratio);
        if (!newVariant.equals(currentVariantId)) currentVariantId = newVariant;
    }

    // ════════════════════════════════════════════════════════════
    //  Render
    // ════════════════════════════════════════════════════════════

    public void render(SpriteBatch batch, Object pamPlayer) {
        // ── مختصات world (از GameCoords — شامل معکوس‌سازی صحیح محور row) ──
        // اندازه/آفست بر مبنای اندازه‌ی واقعیِ خانه (GameConstants.TH)، نه CELL_H نامیِ ۱۰۰.
        float worldX = GameCoords.toScreenX(zombie.getX()) + config.offsetX;
        float worldY = GameCoords.toScreenY(zombie.getY()) - GameConstants.TH * 0.15f + config.offsetY;

        // ── مرگِ سوخته/منفجرشده: افکتِ خاکسترشدن به‌جای انیمیشنِ مرگِ عادی ──
        if (current == ZombieAnimState.DYING && (wasBurning || zombie.isPulverized())) {
            String ashPam = isImp() ? ASH_PAM_IMP
                          : isGargantuar() ? ASH_PAM_GARG : ASH_PAM;
            PamDrawUtil.draw(pamPlayer, batch, ashPam, "animation", stateTime,
                    worldX, worldY, false, null,
                    GameConstants.TW * 1.1f, GameConstants.TH * 1.9f, new Color(0.15f, 0.12f, 0.10f, 0.9f));
            return;
        }

        String clip = resolveClip();
        boolean loop = resolveLoop();

        Color saved = batch.getColor().cpy();
        applyStatusColor(batch);

        if (flashTimer > 0) {
            float f = flashTimer / FLASH_DURATION;
            batch.setColor(1f, 1f - f * 0.4f, 1f - f * 0.4f, 1f);
        }

        Color fallback = current == ZombieAnimState.DYING
                ? new Color(0.2f, 0.2f, 0.2f, 0.8f)
                : new Color(0.45f, 0.55f, 0.4f, 0.95f);
        // visMapِ مؤثر = آرمور (visMap) + پارت‌های بدنیِ افتاده (مخفی) — Request 1.
        Map<String, Boolean> effVis = visMap;
        if (!hiddenBodyParts.isEmpty()) {
            effVis = new LinkedHashMap<>(visMap);
            for (String p : hiddenBodyParts) effVis.put(p, false);
        }
        // زامبی‌های Zombotany از انیمیشنِ گیاه استفاده می‌کنند که رو به راست است؛
        // چون به چپ حرکت می‌کنند، افقی آینه می‌شوند (ماتریسِ scale منفی حولِ worldX).
        boolean flip = needsHorizontalFlip();
        com.badlogic.gdx.math.Matrix4 savedM = null;
        if (flip) {
            savedM = batch.getTransformMatrix().cpy();
            batch.setTransformMatrix(savedM.cpy()
                    .translate(worldX, 0f, 0f).scale(-1f, 1f, 1f).translate(-worldX, 0f, 0f));
        }
        PamDrawUtil.draw(pamPlayer, batch, config.pamPath, clip, stateTime,
                worldX, worldY, loop, effVis.isEmpty() ? null : effVis,
                GameConstants.TW * 1.1f, GameConstants.TH * 1.9f, fallback);

        // ── overlayِ آرمورِ ۳-وضعیتیِ عمومی (cone/bucket/brick) روی همان فریمِ بدن ──
        // آرمورها در PAM به‌طور پیش‌فرض مخفی‌اند؛ drawPart قیدِ flag را دور می‌زند و
        // پارتِ درست را بر اساسِ جانِ آرمور رسم می‌کند. فقط در حالت‌های زنده/متحرک.
        if (current == ZombieAnimState.WALK || current == ZombieAnimState.IDLE
                || current == ZombieAnimState.EAT) {
            drawArmorOverlay(batch, pamPlayer, clip, worldX, worldY);
        }
        if (flip) batch.setTransformMatrix(savedM);

        batch.setColor(saved);
    }

    // ─── آرمورِ ۳-وضعیتی (Request 3) ─────────────────────────────────────────

    /** پایه‌ی نامِ پارتِ آرمور برای هر نوع (cone/bucket/brick)؛ null = بدونِ overlayِ عمومی. */
    private static String armorPartBase(ArmorType type) {
        switch (type) {
            case CONE:   return "zombie_armor_cone";
            case BUCKET: return "zombie_armor_bucket";
            case BLOCK:  return "zombie_armor_brick";
            default:     return null; // HELMET/SHOULDER/NEWSPAPER/BARREL: سیستمِ موجود/بدونِ overlay
        }
    }

    /** آیا این زامبی باید افقی آینه شود؟ (زامبی‌های گیاهیِ Zombotany رو به راست‌اند). */
    private boolean needsHorizontalFlip() {
        return zombie.getType() != null
                && zombie.getType().name().startsWith("ZOMBOTANY");
    }

    private void drawArmorOverlay(SpriteBatch batch, Object pamPlayer, String clip,
                                  float worldX, float worldY) {
        for (Map.Entry<ArmorType, Integer> e : effectiveArmors().entrySet()) {
            String base = armorPartBase(e.getKey());
            if (base == null) continue;
            int hp = e.getValue();
            if (hp <= 0) continue;                 // آرمور از بین رفته → محو
            String state = armorDamageState(e.getKey());
            PamDrawUtil.drawPart(pamPlayer, batch, config.pamPath, clip, stateTime,
                    worldX, worldY, base + state, GameConstants.TH * 1.9f);
        }
    }

    /** وضعیتِ آسیبِ آرمور بر اساسِ کسرِ جانش: _norm (>⅔) / _damage_01 (⅓..⅔) / _damage_02 (<⅓). */
    private String armorDamageState(ArmorType type) {
        Integer modelHp = zombie.getArmors().get(type);
        int max = ArmorDataRegistry.getInstance().getBaseHealth(type);
        // آرمورِ forced-only (بدونِ HPِ واقعی در مدل) یا max نامعتبر → سالمِ کامل.
        float frac = (modelHp != null && max > 0) ? (float) modelHp / max : 1f;
        if (frac > 2f / 3f) return "_norm";
        if (frac > 1f / 3f) return "_damage_01";
        return "_damage_02";
    }

    // ════════════════════════════════════════════════════════════
    //  منطق داخلی — resolve کردن clip نهایی
    // ════════════════════════════════════════════════════════════

    private String resolveClip() {
        if (breakingArmor != null) {
            ArmorAnimConfig armCfg = config.armors.get(breakingArmor.name());
            if (armCfg != null && armCfg.breakEventClip != null) return armCfg.breakEventClip;
        }

        if (activeAbility != null) {
            if ("ENTER".equals(activeAbilityPhase) && activeAbility.enterClip != null) return activeAbility.enterClip;
            if ("EXIT".equals(activeAbilityPhase)  && activeAbility.exitClip  != null) return activeAbility.exitClip;
            if ("LOOP".equals(activeAbilityPhase)  && activeAbility.clip     != null) return activeAbility.clip;
        }

        // غول‌پیکر: هنگامِ حمله به گیاه، ضربه‌ی دومرحله‌ایِ «smash_left» (بالا بردنِ
        // چماق + کوبیدن) پخش می‌شود، نه انیمیشنِ خوردن. (config به‌اشتباه به "eat"
        // اشاره می‌کرد؛ همه‌ی PAMهای غول‌پیکر کلیپِ smash_left دارند.)
        if (current == ZombieAnimState.EAT && isGargantuar()) return "smash_left";

        StateConfig sc = getStateConfig(current);
        if (sc == null || sc.clip.isEmpty()) {
            StateConfig walk = getStateConfig(ZombieAnimState.WALK);
            if (walk == null) return null;
            sc = walk;
        }

        String base = sc.clip;

        if (current == ZombieAnimState.WALK || current == ZombieAnimState.EAT || current == ZombieAnimState.IDLE) {
            for (Map.Entry<ArmorType, Integer> entry : effectiveArmors().entrySet()) {
                if (entry.getValue() <= 0) continue;
                ArmorAnimConfig armCfg = config.armors.get(entry.getKey().name());
                if (armCfg == null) continue;

                if (armCfg.clipOverride != null) return armCfg.clipOverride;
                if (armCfg.isClipSuffixStyle()) return base + armCfg.clipSuffixPresent;
            }
            for (ArmorAnimConfig armCfg : config.armors.values()) {
                if (armCfg.isClipSuffixStyle() && !armCfg.clipSuffixBroken.isEmpty()) {
                    return base + armCfg.clipSuffixBroken;
                }
            }
        }

        String suffix = config.getHealthVariantSuffix(getHpRatio());
        return base + suffix;
    }

    /**
     * آیا کلیپِ فعلی باید حلقه بزند؟ باید با شاخه‌بندیِ {@link #resolveClip()}
     * هماهنگ باشد. کلیپ‌های یک‌بارمصرف (شکستنِ زره، فازِ ENTER/EXITِ ability،
     * و state هایی که در JSON با {@code loop:false} تعریف شده‌اند مثل DYING/SPECIAL)
     * حلقه نمی‌زنند؛ بقیه (WALK/IDLE/EAT/FLYING و LOOPِ ability) حلقه می‌زنند.
     * <p>بدونِ این، {@code PamPlayer.draw} روی فریمِ آخر «فریز» می‌کند و راه‌رفتنِ
     * زامبی‌ها ثابت دیده می‌شود.
     */
    private boolean resolveLoop() {
        if (breakingArmor != null) return false;
        if (activeAbility != null) {
            if ("ENTER".equals(activeAbilityPhase)) return false;
            if ("EXIT".equals(activeAbilityPhase))  return false;
            if ("LOOP".equals(activeAbilityPhase))  return true;
        }
        StateConfig sc = getStateConfig(current);
        if (sc == null) {
            StateConfig walk = getStateConfig(ZombieAnimState.WALK);
            return walk == null || walk.loop;
        }
        return sc.loop;
    }

    private float resolveSpeedMultiplier() {
        Map<ZombieEffect, Integer> effects = zombie.getActiveEffects();
        if (effects.containsKey(ZombieEffect.FROZEN))  return 0.0f;
        if (effects.containsKey(ZombieEffect.CHILLED)) return 0.5f;
        if (effects.containsKey(ZombieEffect.SLOWED))  return 0.7f;
        return 1.0f;
    }

    private void applyStatusColor(SpriteBatch batch) {
        Map<ZombieEffect, Integer> effects = zombie.getActiveEffects();
        // غواص (Snorkel): هنگامِ حرکت زیرِ آب است → نیمه‌شفافِ آبی؛ موقعِ خوردن سر بیرون می‌آورد.
        if (zombie.getType() == com.pvz2.model.enums.ZombieType.SNORKEL_ZOMBIE
                && !zombie.isAttacking()) {
            batch.setColor(0.45f, 0.65f, 1f, 0.55f);
            return;
        }
        Color tint = null;
        if      (effects.containsKey(ZombieEffect.FROZEN))  tint = COLOR_FROZEN;
        else if (effects.containsKey(ZombieEffect.CHILLED)) tint = COLOR_CHILLED;
        else if (effects.containsKey(ZombieEffect.BURNING)) tint = COLOR_BURNING;
        else if (zombie.isGlowing())                        tint = COLOR_GLOW;
        if (tint != null) batch.setColor(tint);
    }

    private void requestState(ZombieAnimState newState) {
        if (newState.priority > requested.priority) requested = newState;
    }

    private void forceTransition(ZombieAnimState newState) {
        if (newState == ZombieAnimState.DYING || newState.priority > current.priority || stateDone) {
            transitionTo(newState);
        }
    }

    private void transitionTo(ZombieAnimState newState) {
        if (newState == current && !stateDone) return;
        current   = newState;
        stateTime = 0f;
        stateDone = false;
    }

    private StateConfig getStateConfig(ZombieAnimState state) {
        return config.states.get(state.name());
    }

    private float getHpRatio() {
        return zombie.getMaxHealth() > 0
            ? (float) zombie.getCurrentHealth() / zombie.getMaxHealth() : 0f;
    }

    /** آیا این زامبی از خانواده‌ی غول‌پیکر است؟ (برای انتخابِ افکتِ خاکسترِ بزرگ‌تر) */
    private boolean isGargantuar() {
        return zombie.getType() != null
                && zombie.getType().name().contains("GARGANTUAR");
    }

    /** آیا این زامبی امپ است؟ (برای انتخابِ افکتِ خاکسترِ کوچک‌ترِ امپ) */
    private boolean isImp() {
        return zombie.getType() != null
                && zombie.getType().name().contains("IMP");
    }

    private float estimateClipDuration(String clip) {
        if (clip == null) return 1.0f;
        if (clip.contains("die") || clip.contains("death")) return 2.0f;
        if (clip.contains("fire") || clip.contains("throw")) return 1.2f;
        if (clip.contains("smash")) return 0.8f;
        if (clip.contains("power_up") || clip.contains("power_down")) return 0.5f;
        if (clip.contains("defeat") || clip.contains("break")) return 0.7f;
        if (clip.equals("land")) return 0.4f;
        if (clip.contains("summon")) return 1.5f;
        return 1.0f;
    }

    // ════════════════════════════════════════════════════════════
    //  Public Getters
    // ════════════════════════════════════════════════════════════

    public ZombieAnimState getCurrentState() { return current; }
    public float getStateTime() { return stateTime; }
    public String getPamPath() { return config.pamPath; }
    /** کلیپِ نهاییِ در حالِ رندر (برای هم‌پوزِ کردنِ پارتِ افتاده). */
    public String getRenderClip() { return resolveClip(); }

    // ─── Request 1: افتادنِ پارتِ بدن ────────────────────────────────────────

    /** آیا این پارت قبلاً افتاده (مخفی شده)؟ */
    public boolean hasDroppedPart(String partName) { return hiddenBodyParts.contains(partName); }

    /** انداختنِ یک پارتِ بدن: روی بدن مخفی می‌شود و برای رندرِ افتان صف می‌شود. */
    public void dropBodyPart(String partName) {
        if (partName == null || partName.isEmpty() || hiddenBodyParts.contains(partName)) return;
        hiddenBodyParts.add(partName);
        pendingFallSpawns.add(partName);
    }

    /** پارت‌هایی که همین‌الان افتادند (برای spawnِ افتان) — پس از خواندن پاک می‌شوند. */
    public List<String> drainPendingFallSpawns() {
        if (pendingFallSpawns.isEmpty()) return Collections.emptyList();
        List<String> out = new ArrayList<>(pendingFallSpawns);
        pendingFallSpawns.clear();
        return out;
    }
    public Map<String, Boolean> getVisMap() { return Collections.unmodifiableMap(visMap); }

    public boolean isDeadAndDone() {
        return current == ZombieAnimState.DYING
                && (stateDone || deathRealTime > MAX_DEATH_TIME);
    }

    public Zombie getZombie() { return zombie; }
}
