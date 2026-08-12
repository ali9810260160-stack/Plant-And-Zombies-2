package com.pvz2.graphics.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.pvz2.graphics.GameStateSnapshot;
import com.pvz2.graphics.GameStateSnapshot.*;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.graphics.util.GameCoords;
import com.pvz2.graphics.util.PamPaths;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * رندر تمام موجودیت‌های داخل بازی با ترتیب عمقی (depth-sorting).
 *
 * <p>ترتیب رندر در هر ردیف: row ۱ (بالا) ابتدا، row ۵ (پایین) آخر،
 * تا موجودیت‌های پایین‌تر روی بالاتری‌ها نمایش داده شوند.
 */
public class EntityRenderer {

    private float stateTime;
    private float shakeX, shakeY;

    // ─── Screen-shake ─────────────────────────────────────────────────────────
    private float shakeDuration;
    private float shakeMagnitude;

    public void triggerShake(float magnitude, float duration) {
        this.shakeMagnitude = magnitude;
        this.shakeDuration  = duration;
    }

    public void update(float delta) {
        stateTime += delta;
        if (shakeDuration > 0) {
            shakeDuration -= delta;
            float t = (float) Math.sin(stateTime * 40) * shakeMagnitude
                    * (shakeDuration > 0 ? 1 : 0);
            shakeX = t;
            shakeY = t * 0.6f;
        } else {
            shakeX = shakeY = 0;
        }
    }

    /** رندر اصلی — همه موجودیت‌ها با depth-sort */
    public void render(Batch batch, GameStateSnapshot snap) {
        drawPlants(batch, snap);
        drawZombies(batch, snap);
        drawProjectiles(batch, snap);
        drawSuns(batch, snap);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Plants  (ترتیب: row ۱ → ۵)
    // ─────────────────────────────────────────────────────────────────────────

    private void drawPlants(Batch batch, GameStateSnapshot snap) {
        List<PlantInfo> sorted = new ArrayList<>(snap.plants);
        sorted.sort(Comparator.comparingInt(p -> p.phase1Y));
        for (PlantInfo p : sorted) drawPlant(batch, p);
    }

    private void drawPlant(Batch batch, PlantInfo p) {
        float cx = GameCoords.toScreenX(p.phase1X) + shakeX;
        float cy = GameCoords.toScreenY(p.phase1Y) + shakeY;

        if (p.isCat) {
            drawFallback(batch, cx, cy, new Color(0.9f, 0.75f, 0.3f, 1f), 48, 56);
            return;
        }

        String clip = "idle";
        boolean flipped = false;
        drawPamOrFallback(batch, p.type, false, clip, cx, cy, flipped, null);

        if (p.freezeLevel > 0)   drawFreezeOnPlant(batch, cx, cy, p.freezeLevel);
        if (p.boosted)            drawBoostedGlow(batch, cx, cy);
    }

    private void drawFreezeOnPlant(Batch batch, float cx, float cy, int level) {
        float alpha = level / 3f * 0.6f;
        // جستجو کنید: "IMAGE_UI_FROZENPLANT" در asset browser
        // TextureRegion r = GameAssets.get().region("IMAGE_UI_FROZENPLANT_LEVEL" + level);
        batch.setColor(0.55f, 0.82f, 1f, alpha);
        batch.draw(GameAssets.getInstance().getWhiteRegion(), cx - 38, cy - 50, 76, 100);
        batch.setColor(Color.WHITE);
    }

    private void drawBoostedGlow(Batch batch, float cx, float cy) {
        batch.setColor(1f, 0.95f, 0.3f, 0.35f);
        batch.draw(GameAssets.getInstance().getWhiteRegion(), cx - 42, cy - 52, 84, 104);
        batch.setColor(Color.WHITE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Zombies  (ترتیب: row ۱ → ۵ = depth order)
    // ─────────────────────────────────────────────────────────────────────────

    private void drawZombies(Batch batch, GameStateSnapshot snap) {
        List<ZombieInfo> sorted = new ArrayList<>(snap.zombies);
        sorted.sort(Comparator.comparingInt(z -> z.phase1Y));
        for (ZombieInfo z : sorted) drawZombie(batch, z);
    }

    private void drawZombie(Batch batch, ZombieInfo z) {
        float cx = GameCoords.toScreenX(z.phase1X) + shakeX;
        float cy = GameCoords.toScreenY(z.phase1Y) + shakeY;

        Color tint = getZombieTint(z);
        String clip = z.currentClip != null ? z.currentClip : "walk";
        // زامبی‌هایی که به چپ می‌روند رو به چپ هستند (flip=false در بیشتر asset ها)
        boolean flip = z.movingBackward; // اکتشافگر که برعکس می‌رود

        drawPamOrFallback(batch, z.type, true, clip, cx, cy, flip, tint);
        batch.setColor(Color.WHITE);

        for (ZombieInfo.ArmorInfo armor : z.armors) {
            drawArmor(batch, armor, cx, cy);
        }

        if (z.isHypnotized) drawHypnoAura(batch, cx, cy);
    }

    private Color getZombieTint(ZombieInfo z) {
        if (z.effects.contains("frozen"))  return new Color(0.5f, 0.75f, 1f, 1f);
        if (z.effects.contains("chilled")) return new Color(0.75f, 0.88f, 1f, 1f);
        if (z.effects.contains("butter"))  return new Color(1f, 1f, 0.4f, 1f);
        if (z.isHypnotized)                return new Color(0.5f, 1f, 0.5f, 1f);
        // تیره شدن با کاهش HP (زیبایی — مطابق داک)
        if (z.maxHp > 0) {
            float frac = z.hp / z.maxHp;
            float darken = 0.5f + 0.5f * frac;
            return new Color(darken, darken, darken, 1f);
        }
        return null;
    }

    private void drawArmor(Batch batch, ZombieInfo.ArmorInfo armor, float cx, float cy) {
        float hpRatio = armor.maxHp > 0 ? armor.hp / armor.maxHp : 0;
        // تغییر رنگ زره با کاهش HP (زیبایی)
        Color col = new Color(0.8f * hpRatio + 0.2f,
                              0.8f * hpRatio + 0.2f, 0.9f, 1f);
        float offsetY = armorOffsetY(armor.type);
        // جستجو کنید: "IMAGE_ZOMBIE_{ARMOR_TYPE}_LEVEL" در asset browser
        drawFallback(batch, cx, cy + offsetY, col, 30, 22);
    }

    private float armorOffsetY(String type) {
        switch (type) {
            case "cone":          return 56f;
            case "bucket":        return 58f;
            case "helmet":        return 62f;
            case "shoulder_armor":return 44f;
            case "block":         return 60f;
            case "newspaper":     return 8f;
            default:              return 50f;
        }
    }

    private void drawHypnoAura(Batch batch, float cx, float cy) {
        batch.setColor(0.4f, 1f, 0.4f, 0.3f);
        batch.draw(GameAssets.getInstance().getWhiteRegion(), cx - 40, cy - 54, 80, 110);
        batch.setColor(Color.WHITE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Projectiles
    // ─────────────────────────────────────────────────────────────────────────

    private void drawProjectiles(Batch batch, GameStateSnapshot snap) {
        for (ProjectileInfo pr : snap.projectiles) {
            drawProjectile(batch, pr);
        }
    }

    private void drawProjectile(Batch batch, ProjectileInfo pr) {
        float sx = GameCoords.toScreenX(pr.phase1X) + shakeX;
        float sy = GameCoords.toScreenY(pr.phase1Y) + shakeY;
        Color col = projectileColor(pr.type);
        float sz  = isLargeProjectile(pr.type) ? 20f : 11f;

        // برای لابرها: رسم با مسیر قوسی (زیبایی — بصری)
        if (pr.isArc) {
            drawArcProjectile(batch, pr, col, sz);
            return;
        }
        drawFallback(batch, sx, sy, col, sz, sz);
    }

    private void drawArcProjectile(Batch batch, ProjectileInfo pr, Color col, float sz) {
        // موقعیت واقعی همان phase1X/Y است (فاز ۱ این را مدیریت می‌کند)
        float sx = GameCoords.toScreenX(pr.phase1X) + shakeX;
        float sy = GameCoords.toScreenY(pr.phase1Y) + shakeY;
        drawFallback(batch, sx, sy + 20, col, sz, sz); // کمی بالاتر = حس هوایی
    }

    private Color projectileColor(String type) {
        if (type == null) return Color.GREEN;
        if (type.contains("fire"))        return Color.ORANGE;
        if (type.contains("snow") || type.contains("ice") || type.contains("cold"))
                                          return Color.CYAN;
        if (type.contains("poison"))      return new Color(0.6f, 0.2f, 0.8f, 1f);
        if (type.contains("frozen_melon"))return new Color(0.4f, 0.8f, 1f, 1f);
        if (type.contains("melon"))       return new Color(0.8f, 0.3f, 0.4f, 1f);
        if (type.contains("butter"))      return Color.YELLOW;
        if (type.contains("kernel"))      return new Color(1f, 0.9f, 0.5f, 1f);
        if (type.contains("lightning") || type.contains("electric"))
                                          return new Color(1f, 1f, 0.2f, 1f);
        if (type.contains("star"))        return new Color(1f, 0.6f, 0.0f, 1f);
        return new Color(0.3f, 0.8f, 0.2f, 1f); // نخود سبز پیش‌فرض
    }

    private boolean isLargeProjectile(String type) {
        if (type == null) return false;
        return type.contains("melon") || type.contains("bomb") || type.contains("pult");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Suns
    // ─────────────────────────────────────────────────────────────────────────

    private void drawSuns(Batch batch, GameStateSnapshot snap) {
        for (SunInfo sun : snap.sunItems) drawSun(batch, sun);
    }

    private void drawSun(Batch batch, SunInfo sun) {
        float sx = GameCoords.toScreenX(sun.phase1X) + shakeX;
        float sy;
        if (!sun.isLanded) {
            // در حال سقوط
            float topY  = GameCoords.toScreenX(sun.phase1Y) + 600;
            float landY = GameCoords.toScreenY(sun.phase1Y);
            sy = topY + (landY - topY) * sun.fallProgress + shakeY;
        } else {
            sy = GameCoords.toScreenY(sun.phase1Y) + shakeY;
        }

        Color col = sunColor(sun.sunType);
        float sz  = "special".equals(sun.sunType) ? 30f : "radioactive".equals(sun.sunType) ? 28f : 22f;
        // جستجو کنید: "IMAGE_SUN", "IMAGE_SUNSUN" در asset browser
        drawFallback(batch, sx, sy, col, sz, sz);
    }

    private Color sunColor(String type) {
        if ("special".equals(type))     return new Color(1f, 0.95f, 0.3f, 1f);
        if ("radioactive".equals(type)) return new Color(0.8f, 0.3f, 1f, 1f);
        return new Color(1f, 0.85f, 0.1f, 1f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Core draw helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void drawPamOrFallback(Batch batch, String type, boolean isZombie,
                                    String clip, float cx, float cy,
                                    boolean flipX, Color tint) {
        GameAssets assets = GameAssets.getInstance();
        String path = isZombie ? PamPaths.forZombie(type) : PamPaths.forPlant(type);

        if (assets.hasPvzAssets() && path != null && !path.isEmpty()) {
            if (tint != null) batch.setColor(tint);
            assets.getPamPlayer().draw(batch, path, clip, stateTime, cx, cy, flipX);
            batch.setColor(Color.WHITE);
        } else {
            Color fb = isZombie ? new Color(0.7f, 0.3f, 0.3f, 1f)
                                : new Color(0.25f, 0.7f, 0.25f, 1f);
            if (tint != null) fb = tint;
            drawFallback(batch, cx, cy, fb, isZombie ? 46f : 42f, isZombie ? 76f : 68f);
        }
    }

    void drawFallback(Batch batch, float cx, float cy, Color col, float w, float h) {
        batch.setColor(col.r, col.g, col.b, col.a);
        TextureRegion white = GameAssets.getInstance().getWhiteRegion();
        batch.draw(white, cx - w * 0.5f, cy - h * 0.5f, w, h);
        batch.setColor(Color.WHITE);
    }

    public float getShakeX() { return shakeX; }
    public float getShakeY() { return shakeY; }
}
