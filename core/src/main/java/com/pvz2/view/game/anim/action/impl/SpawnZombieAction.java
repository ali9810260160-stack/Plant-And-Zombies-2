package com.pvz2.view.game.anim.action.impl;

import com.pvz2.controller.GameController;
import com.pvz2.model.enums.ZombieType;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.view.game.anim.action.ZombieAction;

import java.util.Map;

/**
 * Action: زامبی یک یا چند زامبی جدید اسپان می‌کند.
 *
 * مثال کلاسیک: Chicken Wrangler جوجه‌های زامبی احضار می‌کند.
 *
 * پارامترهای JSON:
 *   zombieType:   نوع زامبی (ZombieType.name())، پیش‌فرض "NORMAL"
 *   count:        تعداد اسپان، پیش‌فرض 1
 *   spreadLanes:  اگر true، در لاین‌های مجاور هم اسپان می‌شود
 *
 * مثال JSON:
 * <pre>
 *   "actionParams": {
 *     "zombieType": "CHICKEN",
 *     "count": "3",
 *     "spreadLanes": "true"
 *   }
 * </pre>
 *
 * TODO در GameController:
 *   void spawnZombieAt(ZombieType type, int lane, int col)
 */
public class SpawnZombieAction implements ZombieAction {

    private final GameController gameController;

    public SpawnZombieAction(GameController gameController) {
        this.gameController = gameController;
    }

    @Override
    public String getActionId() { return "SPAWN_ZOMBIE"; }

    @Override
    public void onStart(Zombie zombie, Map<String, String> params) {
        String typeName = parseStringParam(params, "zombieType", "NORMAL");
        int    count    = parseIntParam(params, "count", 1);
        boolean spread  = parseBoolParam(params, "spreadLanes", false);

        ZombieType spawnType;
        try {
            spawnType = ZombieType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return; // نوع نامعتبر
        }

        int baseLane = zombie.getY();
        int spawnCol = (int) Math.round(zombie.getX()) + 1;

        if (spread && count > 1) {
            int[] lanes = { baseLane - 1, baseLane, baseLane + 1 };
            int spawned = 0;
            for (int lane : lanes) {
                if (spawned >= count) break;
                if (lane >= 1 && lane <= 5) {
                    // TODO: gameController.spawnZombieAt(spawnType, lane, spawnCol);
                    spawned++;
                }
            }
        } else {
            for (int i = 0; i < count; i++) {
                // TODO: gameController.spawnZombieAt(spawnType, baseLane, spawnCol);
            }
        }
    }

    @Override
    public void onEnd(Zombie zombie) { }
}
