package com.pvz2.graphics.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.pvz2.graphics.GameConstants;
import com.pvz2.graphics.GameStateSnapshot;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.graphics.util.GameCoords;

/**
 * بلاکِ یخِ شفاف روی گیاهان و زامبی‌هایِ یخ‌زده را رندر می‌کند.
 *
 * <p>هنگامی که یک گیاه {@code isFrozen()} یا یک زامبی اثرِ {@code FROZEN} می‌گیرد،
 * یک بلاکِ یخِ نیمه‌شفاف روی آن قرار می‌گیرد. تیرهای آتشین بلاک را مرحله‌به‌مرحله
 * ذوب می‌کنند؛ مرحله‌ی ذوب در {@code iceBlockStage} اسنپ‌شات حمل می‌شود:
 * <ul>
 *   <li>{@code -1} = بدونِ بلاک (رندر نمی‌شود)</li>
 *   <li>{@code 0}  = بلاکِ کامل (total)</li>
 *   <li>{@code 1..N} = مراحلِ ذوب (damage1..damageN)</li>
 * </ul>
 *
 * <p>این پاس عمداً از اسنپ‌شات (نه مدلِ خام) کار می‌کند و مستقل از
 * {@code AnimationSystem} است تا کم‌ریسک بماند؛ در {@code GameRenderer} بلافاصله
 * پس از رندرِ موجودیت‌ها صدا زده می‌شود تا یخ «روی» آن‌ها بنشیند.
 */
public final class IceBlockRenderer {

    /** ترتیبِ فایل‌هایِ بلاکِ گیاه بر حسبِ مرحله‌ی ذوب (۰..۴). */
    private static final String[] PLANT_FILES = {
        "ice blocks/plant/frostbite_ice_block_plant_total.png",
        "ice blocks/plant/frostbite_ice_block_plant_damage1.png",
        "ice blocks/plant/frostbite_ice_block_plant_damage2.png",
        "ice blocks/plant/frostbite_ice_block_plant_damage3.png",
        "ice blocks/plant/frostbite_ice_block_plant_damage4.png",
    };

    /** ترتیبِ فایل‌هایِ بلاکِ زامبی بر حسبِ مرحله‌ی ذوب (۰..۵) — نام‌گذاریِ اسست‌ها نامنظم است. */
    private static final String[] ZOMBIE_FILES = {
        "ice blocks/zombie/frostbite_ice_block_zombie_total.png",
        "ice blocks/zombie/frostbite_ice_block_zombie_damage1.png",
        "ice blocks/zombie/frostbite_ice_block_zombie_damage2.png",
        "ice blocks/zombie/frostbite_ice_block_zombie_3.png",
        "ice blocks/zombie/frostbite_ice_block_zombie_damage4.png",
        "ice blocks/zombie/frostbite_ice_block_zombie_damag5.png",
    };

    /** شفافیتِ بلاک تا موجودیتِ زیرش دیده شود. */
    private static final float ALPHA = 0.72f;

    public void render(SpriteBatch batch, GameStateSnapshot snap) {
        if (snap == null) return;
        GameAssets assets = GameAssets.getInstance();
        Color saved = batch.getColor().cpy();
        batch.setColor(1f, 1f, 1f, ALPHA);

        if (snap.plants != null) {
            for (GameStateSnapshot.PlantInfo p : snap.plants) {
                if (p.iceBlockStage < 0) continue;
                TextureRegion r = assets.local(fileFor(PLANT_FILES, p.iceBlockStage));
                float cx = GameCoords.toScreenX(p.phase1X);
                float cy = GameCoords.toScreenY(p.phase1Y) - GameConstants.TH * 0.10f;
                float w  = GameConstants.TW * 1.28f;
                float h  = GameConstants.TH * 1.42f;
                batch.draw(r, cx - w / 2f, cy - h / 2f, w, h);
            }
        }

        if (snap.zombies != null) {
            for (GameStateSnapshot.ZombieInfo z : snap.zombies) {
                if (z.iceBlockStage < 0) continue;
                TextureRegion r = assets.local(fileFor(ZOMBIE_FILES, z.iceBlockStage));
                float cx = GameCoords.toScreenX(z.phase1X);
                float cy = GameCoords.toScreenY(z.phase1Y) + GameConstants.TH * 0.05f;
                float w  = GameConstants.TW * 1.18f;
                float h  = GameConstants.TH * 1.62f;
                batch.draw(r, cx - w / 2f, cy - h / 2f, w, h);
            }
        }

        batch.setColor(saved);
    }

    private static String fileFor(String[] files, int stage) {
        int s = Math.max(0, Math.min(files.length - 1, stage));
        return files[s];
    }
}
