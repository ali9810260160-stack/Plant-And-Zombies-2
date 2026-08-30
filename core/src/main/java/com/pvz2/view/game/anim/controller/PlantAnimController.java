package com.pvz2.view.game.anim.controller;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.util.GameCoords;
import com.pvz2.model.enums.AnimEvent;
import com.pvz2.model.plants.Plant;
import com.pvz2.view.game.anim.config.PlantAnimConfig;
import com.pvz2.view.game.anim.config.StateConfig;
import com.pvz2.view.game.anim.state.PlantAnimState;

/**
 * State Machine انیمیشن برای یک گیاه — نسخه ۲.
 *
 * ─── تغییرات نسخه ۲ ──────────────────────────────────────────────
 *  ۱. رفع باگ مختصات: مثل ZombieAnimController، اکنون از
 *     {@link GameCoords} استفاده می‌شود (نه فرمول محلی که محور row
 *     را برعکس نمی‌کرد).
 *  ۲. damageVariants: گیاهانی مثل Wall-nut واقعاً چند سطح آسیب فیزیکی
 *     دارند (clip های "damage"، "damage2"، "damage3" — جدا از یخ‌زدگی).
 *     بر اساس نسبت HP فعلی، یکی از این‌ها به‌جای IDLE پخش می‌شود.
 */
public class PlantAnimController {

    public static final float CELL_W = 100f;
    public static final float CELL_H = 100f;

    private final Plant           plant;
    private final PlantAnimConfig config;

    private PlantAnimState current    = PlantAnimState.IDLE;
    private PlantAnimState requested  = PlantAnimState.IDLE;
    private float          stateTime  = 0f;
    private boolean        stateDone  = false;

    private int   lastFreezeLevel = 0;

    private float flashTimer = 0f;
    private static final float FLASH_DURATION = 0.12f;

    private float plantFoodGlowTimer = 0f;
    private static final float PF_GLOW_DURATION = 1.6f;

    // ─── حاله‌ی نورِ غذای گیاه (PAM: PLANTFOOD_FX، پشتِ گیاه) ─────────────────
    //   ترتیبِ پخش: plantfood_on → plantfood (loop تا پایانِ عملکرد) → plantfood_off
    private static final String PF_HALO_PAM   = "768/INITIAL/EFFECTS/PLANTFOOD_FX/PLANTFOOD_FX.PAM";
    private static final String PF_CLIP_ON     = "plantfood_on";
    private static final String PF_CLIP_LOOP   = "plantfood";
    private static final String PF_CLIP_OFF    = "plantfood_off";
    private static final float  PF_ON_FALLBACK  = 0.40f;  // اگر طولِ واقعیِ clip هنوز حل نشده
    private static final float  PF_OFF_FALLBACK  = 0.40f;
    private static final float  PF_MIN_LOOP      = 0.50f;  // حداقلِ زمانِ نمایشِ فازِ میانی
    private static final float  PF_MAX_LOOP      = 3.50f;  // سقفِ ایمنی (اگر گیاه از PLANT_FOOD خارج نشد)

    /** طولِ واقعیِ کلیپ‌های on/off — یک‌بار از PamPlayer حل و بینِ همه‌ی گیاهان share می‌شود. */
    private static float pfOnDur = -1f, pfOffDur = -1f;

    private enum HaloPhase { NONE, ON, LOOP, OFF }
    private HaloPhase haloPhase   = HaloPhase.NONE;
    private float     haloTime    = 0f;   // زمان در فازِ فعلی (ON/OFF)
    private float     haloLoopTime = 0f;  // زمانِ تجمعیِ فازِ LOOP (کلیپِ loop + حداقل/سقف)

    /** اگر غیر null باشد، این clip به‌جای clip حالت IDLE استفاده می‌شود (آسیب فیزیکی). */
    private String damageClipOverride = null;

    /** شمارنده‌ی زمانِ واقعیِ مرگ — تضمینِ حذفِ گیاهِ مرده حتی اگر یخ‌زده باشد
     *  یا config مرحله‌ی DYING نداشته باشد. */
    private float deathRealTime = 0f;
    private static final float MAX_DEATH_TIME = 2.5f;

    public PlantAnimController(Plant plant, PlantAnimConfig config) {
        this.plant  = plant;
        this.config = config;
    }

    // ════════════════════════════════════════════════════════════
    //  مصرف رویداد از مدل
    // ════════════════════════════════════════════════════════════

    public void consumeEvent(AnimEvent event) {
        if (event == null || event == AnimEvent.NONE) return;

        switch (event) {
            case ATTACK_TRIGGER:
                requestState(PlantAnimState.ATTACK);
                break;
            case PLANT_FOOD_ACTIVATED:
                plantFoodGlowTimer = PF_GLOW_DURATION;
                startHalo();
                forceTransition(PlantAnimState.PLANT_FOOD);
                break;
            case TOOK_DAMAGE:
                flashTimer = FLASH_DURATION;
                syncDamageVariant();
                break;
            case FREEZE_LEVEL_CHANGED:
                syncFreezeState();
                break;
            case DIED:
                forceTransition(PlantAnimState.DYING);
                break;
            default:
                break;
        }
    }

    // ════════════════════════════════════════════════════════════
    //  Update
    // ════════════════════════════════════════════════════════════

    public void update(float delta) {
        syncFreezeIfChanged();

        if (requested.canOverride(current, stateDone)) {
            transitionTo(requested);
        }
        requested = PlantAnimState.IDLE;

        StateConfig sc = getStateConfig(current);
        float speed = (sc != null) ? sc.speedMul : 1.0f;
        if (current == PlantAnimState.FREEZE_3) speed = 0f;

        stateTime += delta * speed;

        if (sc != null && !sc.loop && !stateDone) {
            float dur = estimateClipDuration(sc.clip);
            if (stateTime >= dur) {
                stateDone = true;
                if (sc.returnTo != null) {
                    transitionTo(PlantAnimState.valueOf(sc.returnTo));
                    syncDamageVariant(); // بعد از بازگشت به IDLE، دوباره چک کن آسیب هنوز پابرجاست
                }
            }
        }

        if (current == PlantAnimState.DYING) deathRealTime += delta;

        if (flashTimer         > 0) flashTimer         = Math.max(0, flashTimer - delta);
        if (plantFoodGlowTimer > 0) plantFoodGlowTimer = Math.max(0, plantFoodGlowTimer - delta);

        updateHalo(delta);
    }

    // ════════════════════════════════════════════════════════════
    //  حاله‌ی نورِ غذای گیاه — ماشینِ حالتِ on → loop → off
    // ════════════════════════════════════════════════════════════

    private void startHalo() {
        haloPhase    = HaloPhase.ON;
        haloTime     = 0f;
        haloLoopTime = 0f;
    }

    private void updateHalo(float delta) {
        if (haloPhase == HaloPhase.NONE) return;
        haloTime += delta;
        switch (haloPhase) {
            case ON:
                if (haloTime >= onDur()) {           // پایانِ «باز شدنِ حاله» → فازِ میانی
                    haloPhase = HaloPhase.LOOP;
                    haloTime = 0f;
                    haloLoopTime = 0f;
                }
                break;
            case LOOP:
                haloLoopTime += delta;
                // عملکردِ گیاه (کلیپِ plantfood) وقتی تمام است که از حالتِ PLANT_FOOD خارج شده باشد.
                boolean performing = (current == PlantAnimState.PLANT_FOOD);
                if ((!performing && haloLoopTime >= PF_MIN_LOOP) || haloLoopTime >= PF_MAX_LOOP) {
                    haloPhase = HaloPhase.OFF;       // → «بسته شدنِ حاله»
                    haloTime = 0f;
                }
                break;
            case OFF:
                if (haloTime >= offDur()) haloPhase = HaloPhase.NONE;
                break;
            default:
                break;
        }
    }

    private static float onDur()  { return pfOnDur  > 0f ? pfOnDur  : PF_ON_FALLBACK;  }
    private static float offDur() { return pfOffDur > 0f ? pfOffDur : PF_OFF_FALLBACK; }

    // ════════════════════════════════════════════════════════════
    //  Render
    // ════════════════════════════════════════════════════════════

    public void render(SpriteBatch batch, Object pamPlayer) {
        // ── مختصات world (از GameCoords — شامل معکوس‌سازی صحیح محور row).
        //    اندازه/آفست بر مبنای اندازه‌ی واقعیِ خانه (GameConstants.TH) است،
        //    نه CELL_H نامیِ ۱۰۰. گیاه کمی پایین‌تر از مرکز کشیده می‌شود تا
        //    پایه‌اش نزدیکِ کفِ خانه بنشیند. ──
        float worldX = GameCoords.toScreenX(plant.getX()) + config.offsetX;
        float worldY = GameCoords.toScreenY(plant.getY()) - GameConstants.TH * 0.15f + config.offsetY;

        String clip = resolveClip();
        boolean loop = resolveLoop();

        // ── حاله‌ی نورِ غذای گیاه (PAM): پشتِ گیاه (قبل از خودِ گیاه) رندر می‌شود
        //    و ترتیبِ on → loop → off را می‌پیماید (updateHalo). ──
        if (haloPhase != HaloPhase.NONE) drawPlantFoodHalo(batch, pamPlayer, worldX, worldY);

        Color saved = batch.getColor().cpy();
        applyTint(batch);

        if (flashTimer > 0) {
            float flashIntensity = flashTimer / FLASH_DURATION;
            batch.setColor(1f, 1f - flashIntensity * 0.5f, 1f - flashIntensity * 0.5f, 1f);
        }

        if (plantFoodGlowTimer > 0 && current != PlantAnimState.PLANT_FOOD) {
            float t = (float) Math.sin((PF_GLOW_DURATION - plantFoodGlowTimer) / PF_GLOW_DURATION * Math.PI * 4);
            batch.setColor(1f, 1f, 0.5f + t * 0.5f, 1f);
        }

        Color fallback = current == PlantAnimState.DYING
                ? new Color(0.35f, 0.25f, 0.15f, 0.85f)
                : new Color(0.25f, 0.65f, 0.25f, 0.95f);
        PamDrawUtil.draw(pamPlayer, batch, config.pamPath, clip, stateTime,
                worldX, worldY, loop, GameConstants.TW * 1.15f, GameConstants.TH * 1.7f, fallback);

        batch.setColor(saved);
    }

    // ════════════════════════════════════════════════════════════
    //  منطق داخلی
    // ════════════════════════════════════════════════════════════

    /**
     * حاله‌ی نورِ PAMِ غذای گیاه، پشتِ گیاه. کلیپِ فازِ فعلی را رندر می‌کند:
     *   ON → plantfood_on (یک‌بار) | LOOP → plantfood (حلقه) | OFF → plantfood_off (یک‌بار).
     * fallback نامرئی است تا اگر PAM هنوز bake نشده، مستطیلِ رنگی دیده نشود.
     */
    private void drawPlantFoodHalo(SpriteBatch batch, Object pamPlayer,
                                   float worldX, float worldY) {
        // حلِ یک‌باره‌ی طولِ کلیپ‌های on/off (share بینِ همه‌ی گیاهان).
        if (pfOnDur  <= 0f) { float d = PamDrawUtil.clipDuration(pamPlayer, PF_HALO_PAM, PF_CLIP_ON);  if (d > 0f) pfOnDur  = d; }
        if (pfOffDur <= 0f) { float d = PamDrawUtil.clipDuration(pamPlayer, PF_HALO_PAM, PF_CLIP_OFF); if (d > 0f) pfOffDur = d; }

        String clip; float clipTime; boolean loop;
        switch (haloPhase) {
            case ON:   clip = PF_CLIP_ON;   clipTime = haloTime;     loop = false; break;
            case LOOP: clip = PF_CLIP_LOOP; clipTime = haloLoopTime; loop = true;  break;
            case OFF:  clip = PF_CLIP_OFF;  clipTime = haloTime;     loop = false; break;
            default:   return;
        }
        float w  = GameConstants.TW * 1.9f;
        float h  = GameConstants.TH * 1.9f;
        // مرکزِ حاله حدودِ نیم‌کاشی بالاتر از پایه‌ی گیاه تا پشتِ بدنه‌اش بنشیند.
        float hy = worldY + GameConstants.TH * 0.55f;
        Color saved = batch.getColor().cpy();
        batch.setColor(1f, 1f, 1f, 1f);
        PamDrawUtil.draw(pamPlayer, batch, PF_HALO_PAM, clip, clipTime,
                worldX, hy, loop, w, h, new Color(0f, 0f, 0f, 0f));
        batch.setColor(saved);
    }

    private String resolveClip() {
        StateConfig sc = getStateConfig(current);
        if (sc != null && !sc.clip.isEmpty()) return sc.clip;
        StateConfig idle = getStateConfig(PlantAnimState.IDLE);
        return idle != null ? idle.clip : null;
    }

    /** آیا کلیپِ فعلی حلقه بزند؟ (IDLE/FREEZE=true، ATTACK/PLANT_FOOD/DYING=false).
     *  بدونِ این، انیمیشن روی فریمِ آخر فریز می‌شود. */
    private boolean resolveLoop() {
        StateConfig sc = getStateConfig(current);
        if (sc != null) return sc.loop;
        StateConfig idle = getStateConfig(PlantAnimState.IDLE);
        return idle == null || idle.loop;
    }

    private void applyTint(SpriteBatch batch) {
        StateConfig sc = getStateConfig(current);
        if (sc == null || sc.tintColor == null || sc.tintColor.isEmpty()) return;
        try {
            Color tint = Color.valueOf(sc.tintColor + "FF");
            Color cur  = batch.getColor();
            float a = sc.tintAlpha;
            batch.setColor(
                cur.r * (1 - a) + tint.r * a,
                cur.g * (1 - a) + tint.g * a,
                cur.b * (1 - a) + tint.b * a,
                cur.a
            );
        } catch (Exception ignored) { }
    }

    private void syncFreezeIfChanged() {
        int freezeLevel = plant.getFreezeLevel();
        if (freezeLevel != lastFreezeLevel) {
            lastFreezeLevel = freezeLevel;
            syncFreezeState();
        }
    }

    private void syncFreezeState() {
        int lvl = plant.getFreezeLevel();
        switch (lvl) {
            case 1: requestState(PlantAnimState.FREEZE_1); break;
            case 2: requestState(PlantAnimState.FREEZE_2); break;
            case 3: requestState(PlantAnimState.FREEZE_3); break;
            default:
                if (isFreezeState(current)) requestState(PlantAnimState.IDLE);
                break;
        }
    }

    private boolean isFreezeState(PlantAnimState s) {
        return s == PlantAnimState.FREEZE_1 || s == PlantAnimState.FREEZE_2 || s == PlantAnimState.FREEZE_3;
    }

    /**
     * اگر گیاه damageVariants دارد (مثل Wall-nut)، بر اساس HP فعلی
     * نام clip آسیب مناسب را در damageClipOverride قرار می‌دهد.
     * این override فقط وقتی current==IDLE اعمال می‌شود (resolveClip از
     * طریق getStateConfig(IDLE) این را می‌خواند).
     */
    private void syncDamageVariant() {
        if (!config.hasDamageVariants()) return;
        float ratio = plant.getMaxHealth() > 0
            ? (float) plant.getCurrentHealth() / plant.getMaxHealth() : 1f;
        if (ratio >= 0.99f) { damageClipOverride = null; return; }
        damageClipOverride = config.getDamageClipForRatio(ratio);
    }

    private void requestState(PlantAnimState newState) {
        if (newState.priority > requested.priority) requested = newState;
    }

    private void forceTransition(PlantAnimState newState) {
        if (newState == PlantAnimState.DYING || newState.priority > current.priority || stateDone) {
            transitionTo(newState);
        }
    }

    private void transitionTo(PlantAnimState newState) {
        if (newState == current && !stateDone) return;
        current   = newState;
        stateTime = 0f;
        stateDone = false;
    }

    private StateConfig getStateConfig(PlantAnimState state) {
        if (state == PlantAnimState.IDLE && damageClipOverride != null) {
            StateConfig override = new StateConfig();
            override.clip = damageClipOverride;
            override.loop = true;
            return override;
        }
        return config.states.get(state.name());
    }

    private float estimateClipDuration(String clip) {
        if (clip == null) return 1.0f;
        if (clip.contains("death") || clip.contains("disappear") || clip.contains("detach")) return 2.0f;
        if (clip.contains("plant_food") || clip.contains("plantfood")) return 3.0f;
        if (clip.contains("shoot") || clip.contains("attack") || clip.equals("special")) return 0.6f;
        return 1.2f;
    }

    // ════════════════════════════════════════════════════════════
    //  Public Getters
    // ════════════════════════════════════════════════════════════

    public PlantAnimState getCurrentState() { return current; }
    public float getStateTime()             { return stateTime; }

    /** مسیرِ PAMِ این گیاه (برای محاسبه‌ی طولِ کلیپِ حمله). */
    public String getPamPath() { return config.pamPath; }

    /** نامِ کلیپِ حالتِ حمله (برای هماهنگیِ نقطه‌ی شلیک). null اگر تعریف نشده باشد. */
    public String getAttackClip() {
        StateConfig sc = config.states.get(PlantAnimState.ATTACK.name());
        return sc != null ? sc.clip : null;
    }

    public boolean isDeadAndDone() {
        return current == PlantAnimState.DYING
                && (stateDone || deathRealTime > MAX_DEATH_TIME);
    }

    public Plant getPlant() { return plant; }
}
