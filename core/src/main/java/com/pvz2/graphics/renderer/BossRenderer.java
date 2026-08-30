package com.pvz2.graphics.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.GameStateSnapshot;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.graphics.util.GameCoords;
import com.pvz2.view.game.anim.controller.PamDrawUtil;

/**
 * رندرِ رئیس (Zomboss)ِ اختصاصیِ هر فصل + توانایی‌های آن.
 *
 * <p>هر فصل PAMِ واقعیِ خودش را با کلیپ‌های اختصاصی پخش می‌کند (intro/idle/
 * attack/summon/die)، و توانایی‌ها افکتِ مخصوص دارند:
 * <ul>
 *   <li>موشک/توپِ آتش/بچه‌کوسه: هدف‌گیریِ روی کاشی → سقوطِ پرتابه → انفجار.</li>
 *   <li>شارژِ مصر: لومیدنِ رئیس به جلو (chargeOffset).</li>
 *   <li>بادِ یخی/توربین: نوارِ افکت روی ۲ سطر.</li>
 *   <li>یخ‌زدنِ ستون: افکتِ یخچال روی ستون.</li>
 * </ul>
 */
public class BossRenderer {

    /** ارتفاعِ مبنا (مقیاسِ ۱× — سایرِ فصل‌ها ضریب می‌خورند). */
    private static final float BOSS_BASE_H = 260f;
    private static final float BOSS_FALLBACK_W = 150f;
    private static final float BOSS_LOOM_UP = 80f;
    /** سقفِ بزرگ‌نمایی برای رئیس (اجازه‌ی چند برابرِ اندازه‌ی نیتیو). */
    private static final float BOSS_MAX_SCALE = 8f;
    private static final Color BOSS_FALLBACK = new Color(0.3f, 0.42f, 0.25f, 1f);

    private static final float ATTACK_TARGET_H = 110f;
    private static final float ATTACK_FALL_H = 340f;
    private static final float TARGETING_FRACTION = 0.4f;

    private final Object pamPlayer;

    public BossRenderer(Object pamPlayer) {
        this.pamPlayer = pamPlayer;
    }

    public void render(Batch batch, GameStateSnapshot snap) {
        if (snap == null || !snap.bossActive) return;
        renderAreaEffects(batch, snap);       // نوارها/یخچال (پشت)
        renderProjectiles(batch, snap, false); // سقوط/هدف‌گیری (پشتِ رئیس)
        renderBoss(batch, snap);
        renderProjectiles(batch, snap, true);  // انفجار (جلوی رئیس)
    }

    // ─── رئیس ────────────────────────────────────────────────────────────────────

    private void renderBoss(Batch batch, GameStateSnapshot snap) {
        String chapter = snap.bossChapter;
        int bottomRow = snap.bossLane + Math.max(1, snap.bossLaneSpan) - 1;

        // ── اندازه و لنگرِ عمودیِ اختصاصیِ هر فصل ──
        float targetH;
        float loom = BOSS_LOOM_UP;
        int anchorRow = snap.bossLane;
        switch (chapter) {
            case "frostbite_caves":
                // ماموت: بزرگ (مثلِ اژدها، داخلِ canvas کوچک است پس ضریبِ درشت لازم
                // دارد) تا کلِ ارتفاعِ ردیف‌ها را بگیرد؛ لنگر ~۳ کاشی بالاتر از
                // پایین‌ترین ردیف تا روی کلِ گرید مرکز شود.
                targetH = BOSS_BASE_H * 3.5f;
                anchorRow = bottomRow;
                loom = GameConstants.TH * 3f;
                break;
            case "dark_ages":
                targetH = BOSS_BASE_H * 6f;   // اژدهای بزرگ
                loom = 0f;
                break;
            case "ancient_egypt":
                targetH = BOSS_BASE_H * 2.5f; // ربات چهارپا
                break;
            case "big_wave_beach":
            default:
                targetH = BOSS_BASE_H;        // کوسه (اندازه‌ی خوب)
                break;
        }

        float cx = GameCoords.toScreenX(snap.bossPhase1X + snap.bossChargeOffset);
        float cy = GameCoords.toScreenY(anchorRow) + loom;
        float half = BOSS_FALLBACK_W * 0.5f;
        cx = Math.min(cx, GameConstants.VIEWPORT_WIDTH - half);
        String pam  = bossPam(chapter);
        ClipSel sel = selectBossClip(pam, chapter, snap.bossState, snap.bossAbility,
                snap.bossStateTime, snap);
        PamDrawUtil.draw(pamPlayer, batch, pam, sel.clip, sel.time,
                cx, cy, sel.loop, BOSS_FALLBACK_W, targetH, BOSS_FALLBACK, BOSS_MAX_SCALE);
    }

    // ─── انتخابِ کلیپِ Zomboss (با توالیِ start→loop→end برای حالاتِ چندفازی) ────────

    /** نتیجه‌ی انتخابِ کلیپ: نامِ کلیپ + زمانِ داخلِ کلیپ + آیا حلقه بزند. */
    private static final class ClipSel {
        String clip; float time; boolean loop;
        ClipSel(String c, float t, boolean l) { clip = c; time = t; loop = l; }
    }

    // مدتِ حالت‌ها (ثانیه) — هماهنگ با BossService (تیک/۱۰).
    private static final float ATTACK_ANIM = 2.2f;   // ATTACK_ANIM_TICKS = 22
    private static final float STUN_TOTAL  = 3.5f;   // STUN_TICKS = 35
    private static final float MOVING_TOTAL = 0.7f;  // MOVING_TICKS = 7

    private ClipSel selectBossClip(String pam, String chapter, String state, String ability,
                                    float t, GameStateSnapshot snap) {
        boolean egypt = "ancient_egypt".equals(chapter);
        boolean dark  = "dark_ages".equals(chapter);
        boolean ice   = "frostbite_caves".equals(chapter);
        boolean beach = "big_wave_beach".equals(chapter);
        switch (state) {
            case "entering":
                return new ClipSel("intro", t, false);
            case "summoning":
                if (dark)  return new ClipSel("summoning", t, false);
                if (egypt) return new ClipSel("zombie_portal_start", t, false);
                return new ClipSel("idle", t, true);
            case "attacking":
                return attackSel(pam, chapter, ability, t, snap);
            case "stunned":
                if (ice)          return seq(pam, "reveal", "stun", "cover_up", t, STUN_TOTAL);
                if (dark || beach) return seq(pam, "stun_start", "stun_loop", "stun_end", t, STUN_TOTAL);
                return new ClipSel("idle", t, true); // مصر: بدونِ کلیپِ گیج
            case "moving":
                if (beach) {
                    // تغییرِ لِین: submerge (خروج) → emerge (ورود).
                    float dSub = clampDur(pam, "submerge", 0.35f, MOVING_TOTAL * 0.6f);
                    return t < dSub ? new ClipSel("submerge", t, false)
                                    : new ClipSel("emerge", t - dSub, false);
                }
                return new ClipSel("idle", t, true);
            case "dying":
            case "dead":
                return new ClipSel(egypt ? "die_idle" : "die", t, false);
            case "idle":
            default:
                return new ClipSel("idle", t, true);
        }
    }

    private ClipSel attackSel(String pam, String chapter, String ability, float t,
                              GameStateSnapshot snap) {
        if (ability == null) ability = "";
        switch (ability) {
            case "dark_fireball":      // آتشِ یک‌خانه
                return seq(pam, "fire_bomb", "fire_bomb_loop", "fire_bomb_end", t, ATTACK_ANIM);
            case "dark_fire_tworow":   // آتشِ ۲-سطری
                return seq(pam, "fire_attack", "fire_attack_idle", "fire_attack_end", t, ATTACK_ANIM);
            case "beach_turbine":      // توربین/مکش
                return seq(pam, "suction_on", "suction_loop", "suction_off", t, ATTACK_ANIM);
            case "beach_shark":        return new ClipSel("spawn", t, false);
            case "ice_missile":        return new ClipSel("slingshot", t, false);
            case "ice_wind":           return new ClipSel("wind_1", t, true);
            case "ice_freeze_column":  return new ClipSel("glacier_column_" + glacierN(snap), t, false);
            case "egypt_charge":       return new ClipSel("walk_forward", t, true);
            case "egypt_missile":      return new ClipSel("rocket_launch", t, false);
            default:                   return new ClipSel("ancient_egypt".equals(chapter)
                    ? "rocket_launch" : "idle", t, false);
        }
    }

    /** ستونِ یخچال بر اساسِ فاصله‌ی ستونِ هدف تا زامباس (glacier_column_1..6). */
    private int glacierN(GameStateSnapshot snap) {
        for (GameStateSnapshot.BossAttackInfo atk : snap.bossAttacks) {
            if ("ice_freeze_column".equals(atk.type)) {
                int d = Math.round((float) (snap.bossPhase1X - atk.targetCol));
                return Math.max(1, Math.min(6, d));
            }
        }
        return 1;
    }

    /**
     * توالیِ سه‌کلیپیِ start→loop→end در بازه‌ی زمانیِ {@code total}: کلیپِ start
     * تا مدتِ خودش، سپس loop (حلقه) در میانه، و end در انتها — طوری که هر سه دیده شوند.
     */
    private ClipSel seq(String pam, String start, String loop, String end, float t, float total) {
        float dStart = clampDur(pam, start, 0.30f, total * 0.45f);
        float dEnd   = clampDur(pam, end,   0.30f, total * 0.35f);
        if (t < dStart)              return new ClipSel(start, t, false);
        if (t >= total - dEnd)       return new ClipSel(end, t - (total - dEnd), false);
        return new ClipSel(loop, t - dStart, true);
    }

    /** طولِ کلیپ (ثانیه) با fallback و سقف. */
    private float clampDur(String pam, String clip, float fallback, float cap) {
        float d = PamDrawUtil.clipDuration(pamPlayer, pam, clip);
        if (d <= 0f) d = fallback;
        return Math.min(d, cap);
    }

    private String bossPam(String chapter) {
        switch (chapter) {
            case "frostbite_caves":
                return "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_ZOMBOSS/ZOMBIE_ICEAGE_ZOMBOSS.PAM";
            case "big_wave_beach":
                return "768/FULL/ZOMBIE/ZOMBIE_BEACH_ZOMBOSS/ZOMBIE_BEACH_ZOMBOSS.PAM";
            case "dark_ages":
                return "768/FULL/ZOMBIE/ZOMBIE_DARK_ZOMBOSS/ZOMBIE_DARK_ZOMBOSS.PAM";
            case "ancient_egypt":
            default:
                return "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_ZOMBOSS/ZOMBIE_EGYPT_ZOMBOSS.PAM";
        }
    }

    // ─── توانایی‌های پرتابه‌ای (موشک/توپِ آتش/بچه‌کوسه) ────────────────────────────

    private boolean isProjectile(String type) {
        return "egypt_missile".equals(type) || "ice_missile".equals(type)
                || "dark_fireball".equals(type) || "beach_shark".equals(type);
    }

    private void renderProjectiles(Batch batch, GameStateSnapshot snap, boolean impactPass) {
        for (GameStateSnapshot.BossAttackInfo atk : snap.bossAttacks) {
            if (!isProjectile(atk.type)) continue;
            boolean impact = "impact".equals(atk.phase);
            if (impact != impactPass) continue;
            if (impact) renderImpact(batch, atk);
            else        renderIncoming(batch, atk);
        }
    }

    private void renderIncoming(Batch batch, GameStateSnapshot.BossAttackInfo atk) {
        float tx = GameCoords.toScreenX(atk.targetCol);
        float landY = GameCoords.toScreenY(atk.targetRow);
        Color col = attackColor(atk.type);
        if (atk.progress < TARGETING_FRACTION) {
            String[] ret = reticlePamClip(atk.type);
            float t = atk.progress / TARGETING_FRACTION;
            if (ret != null) {
                PamDrawUtil.draw(pamPlayer, batch, ret[0], ret[1], t * 0.8f,
                        tx, landY, true, 60f, ATTACK_TARGET_H * 0.8f, col);
            } else {
                drawReticle(batch, tx, landY, t, col);
            }
        } else {
            float fall = (atk.progress - TARGETING_FRACTION) / (1f - TARGETING_FRACTION);
            float y = landY + (1f - fall) * ATTACK_FALL_H;
            drawShadow(batch, tx, landY, fall);
            String[] mis = missilePamClip(atk.type);
            PamDrawUtil.draw(pamPlayer, batch, mis[0], mis[1], fall * 0.6f,
                    tx, y, true, 60f, ATTACK_TARGET_H, col);
        }
    }

    private void renderImpact(Batch batch, GameStateSnapshot.BossAttackInfo atk) {
        float tx = GameCoords.toScreenX(atk.targetCol);
        float ty = GameCoords.toScreenY(atk.targetRow);
        String[] imp = impactPamClip(atk.type);
        PamDrawUtil.draw(pamPlayer, batch, imp[0], imp[1], atk.progress,
                tx, ty, false, 100f, ATTACK_TARGET_H * 1.3f, attackColor(atk.type));
    }

    // ─── توانایی‌های ناحیه‌ای (باد/توربین/یخ‌ستون/شارژ) ───────────────────────────

    private void renderAreaEffects(Batch batch, GameStateSnapshot snap) {
        for (GameStateSnapshot.BossAttackInfo atk : snap.bossAttacks) {
            float intensity = "impact".equals(atk.phase) ? 1f : Math.max(0.35f, atk.progress);
            switch (atk.type) {
                case "ice_wind":
                    drawRowBand(batch, atk.targetRow, new Color(0.6f, 0.9f, 1f, 0.28f * intensity));
                    drawRowBand(batch, atk.targetRow2, new Color(0.6f, 0.9f, 1f, 0.28f * intensity));
                    drawWindAccent(batch, atk.targetRow);
                    drawWindAccent(batch, atk.targetRow2);
                    break;
                case "beach_turbine":
                    drawRowBand(batch, atk.targetRow, new Color(0.3f, 0.6f, 0.95f, 0.30f * intensity));
                    drawRowBand(batch, atk.targetRow2, new Color(0.3f, 0.6f, 0.95f, 0.30f * intensity));
                    drawWindAccent(batch, atk.targetRow);
                    drawWindAccent(batch, atk.targetRow2);
                    break;
                case "ice_freeze_column":
                    drawColumnBand(batch, (int) Math.round(atk.targetCol),
                            new Color(0.55f, 0.85f, 1f, 0.32f * intensity));
                    drawGlacierAccent(batch, (int) Math.round(atk.targetCol), atk.progress);
                    break;
                case "dark_fire_tworow":
                    drawRowBand(batch, atk.targetRow, new Color(1f, 0.45f, 0.12f, 0.30f * intensity));
                    drawRowBand(batch, atk.targetRow2, new Color(1f, 0.45f, 0.12f, 0.30f * intensity));
                    break;
                case "egypt_charge":
                    if ("impact".equals(atk.phase)) {
                        drawRowBand(batch, atk.targetRow, new Color(0.7f, 0.5f, 0.25f, 0.30f));
                        drawRowBand(batch, atk.targetRow2, new Color(0.7f, 0.5f, 0.25f, 0.30f));
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void drawRowBand(Batch batch, int row, Color color) {
        if (row < 1) return;
        TextureRegion w = GameAssets.getInstance().getWhiteRegion();
        Color saved = batch.getColor().cpy();
        batch.setColor(color);
        batch.draw(w, GameConstants.G_X, GameCoords.tileBottom(row),
                GameConstants.TILE_COLS * GameConstants.TW, GameConstants.TH);
        batch.setColor(saved);
    }

    private void drawColumnBand(Batch batch, int col, Color color) {
        if (col < 1) return;
        TextureRegion w = GameAssets.getInstance().getWhiteRegion();
        Color saved = batch.getColor().cpy();
        batch.setColor(color);
        batch.draw(w, GameCoords.tileLeft(col), GameConstants.G_Y,
                GameConstants.TW, GameConstants.TILE_ROWS * GameConstants.TH);
        batch.setColor(saved);
    }

    private void drawWindAccent(Batch batch, int row) {
        if (row < 1) return;
        float cy = GameCoords.toScreenY(row);
        float cx = GameCoords.toScreenX(GameConstants.TILE_COLS * 0.5);
        PamDrawUtil.draw(pamPlayer, batch,
                "768/FULL/EFFECTS/ZOMBOSS_TURBINE_WIND/ZOMBOSS_TURBINE_WIND.PAM", "animation",
                0.3f, cx, cy, true, GameConstants.TILE_COLS * GameConstants.TW * 0.6f,
                GameConstants.TH, new Color(0.7f, 0.9f, 1f, 0.5f));
    }

    private void drawGlacierAccent(Batch batch, int col, float progress) {
        if (col < 1) return;
        float cx = GameCoords.toScreenX(col);
        float cy = GameCoords.toScreenY((GameConstants.TILE_ROWS + 1) / 2);
        PamDrawUtil.draw(pamPlayer, batch,
                "768/FULL/EFFECTS/ZOMBOSS_GLACIER_MIDDLE/ZOMBOSS_GLACIER_MIDDLE.PAM", "animation2",
                progress * 1.3f, cx, cy, false, GameConstants.TW,
                GameConstants.TILE_ROWS * GameConstants.TH * 0.9f, new Color(0.6f, 0.85f, 1f, 1f));
    }

    // ─── نگاشتِ نوعِ توانایی → افکتِ PAM ──────────────────────────────────────────

    private String[] reticlePamClip(String type) {
        switch (type) {
            case "ice_missile":
                return new String[]{"768/FULL/EFFECTS/ZOMBOSS_MISSILE_EXPLOSION_ICEAGE/"
                        + "ZOMBOSS_MISSILE_EXPLOSION_ICEAGE.PAM", "missile_lock_reticle"};
            case "dark_fireball":
                return new String[]{"768/FULL/EFFECTS/ZOMBOSS_MISSILE_EXPLOSION_DARK/"
                        + "ZOMBOSS_MISSILE_EXPLOSION_DARK.PAM", "missile_lock_reticle"};
            case "egypt_missile":
                return new String[]{"768/INITIAL/EFFECTS/ZOMBOSS_MISSILE_EXPLOSION_EGYPT/"
                        + "ZOMBOSS_MISSILE_EXPLOSION_EGYPT.PAM", "missile_lock_reticle"};
            case "beach_shark":
            default:
                return null; // بچه‌کوسه — reticle رویه‌ای
        }
    }

    private String[] missilePamClip(String type) {
        switch (type) {
            case "ice_missile":
                return new String[]{"768/FULL/EFFECTS/ZOMBOSS_MISSILE_EXPLOSION_ICEAGE/"
                        + "ZOMBOSS_MISSILE_EXPLOSION_ICEAGE.PAM", "missile"};
            case "dark_fireball":
                return new String[]{"768/FULL/EFFECTS/ZOMBOSS_DARK_FIREBALL/"
                        + "ZOMBOSS_DARK_FIREBALL.PAM", "fall"};
            case "beach_shark":
                return new String[]{"768/FULL/EFFECTS/ZOMBOSS_SHARK_PROJECTILE/"
                        + "ZOMBOSS_SHARK_PROJECTILE.PAM", "attack"};
            case "egypt_missile":
            default:
                return new String[]{"768/INITIAL/EFFECTS/ZOMBOSS_MISSILE_EXPLOSION_EGYPT/"
                        + "ZOMBOSS_MISSILE_EXPLOSION_EGYPT.PAM", "missile"};
        }
    }

    private String[] impactPamClip(String type) {
        switch (type) {
            case "ice_missile":
                return new String[]{"768/FULL/EFFECTS/ZOMBOSS_MISSILE_EXPLOSION_ICEAGE/"
                        + "ZOMBOSS_MISSILE_EXPLOSION_ICEAGE.PAM", "missile_explosion"};
            case "dark_fireball":
                return new String[]{"768/FULL/EFFECTS/ZOMBOSS_DARK_FIREBALL/"
                        + "ZOMBOSS_DARK_FIREBALL.PAM", "impact"};
            case "beach_shark":
                return new String[]{"768/FULL/EFFECTS/ZOMBOSS_SHARK_PROJECTILE/"
                        + "ZOMBOSS_SHARK_PROJECTILE.PAM", "submerge"};
            case "egypt_missile":
            default:
                return new String[]{"768/INITIAL/EFFECTS/ZOMBOSS_MISSILE_EXPLOSION_EGYPT/"
                        + "ZOMBOSS_MISSILE_EXPLOSION_EGYPT.PAM", "missile_explosion"};
        }
    }

    private Color attackColor(String type) {
        if (type.startsWith("ice"))   return new Color(0.6f, 0.85f, 1f, 1f);
        if (type.startsWith("dark"))  return new Color(0.7f, 0.3f, 1f, 1f);
        if (type.startsWith("beach")) return new Color(0.3f, 0.6f, 0.9f, 1f);
        return new Color(0.9f, 0.55f, 0.2f, 1f);
    }

    // ─── کمکی‌های رویه‌ای ─────────────────────────────────────────────────────────

    private void drawReticle(Batch batch, float cx, float cy, float t, Color col) {
        TextureRegion disc = GameAssets.getInstance().getDiscRegion();
        if (disc == null) return;
        Color saved = batch.getColor().cpy();
        float pulse = 22f + 8f * (float) Math.sin(t * 12f);
        batch.setColor(col.r, col.g, col.b, 0.5f);
        batch.draw(disc, cx - pulse, cy - pulse * 0.5f, pulse * 2f, pulse);
        batch.setColor(col.r, col.g, col.b, 0.85f);
        batch.draw(disc, cx - 5f, cy - 3f, 10f, 6f);
        batch.setColor(saved);
    }

    private void drawShadow(Batch batch, float cx, float cy, float progress) {
        TextureRegion disc = GameAssets.getInstance().getDiscRegion();
        if (disc == null) return;
        float r = 14f + progress * 26f;
        Color saved = batch.getColor().cpy();
        batch.setColor(0f, 0f, 0f, 0.3f * progress);
        batch.draw(disc, cx - r, cy - r * 0.4f, r * 2f, r * 0.8f);
        batch.setColor(saved);
    }
}
