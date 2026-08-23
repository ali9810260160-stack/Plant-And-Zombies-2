package com.pvz2.view.game.anim.action.impl;

import com.pvz2.controller.GameController;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.view.game.anim.action.ZombieAction;

import java.util.Map;

/**
 * Action: Necromancer یک سنگ قبر در ستون جلوی خودش ایجاد می‌کند.
 *
 * پارامترهای JSON:
 *   x_offset: آفست ستون نسبت به زامبی، پیش‌فرض -1 (یک ستون جلوتر)
 *
 * مثال JSON:
 * <pre>
 *   "actionParams": { "x_offset": "-1" }
 * </pre>
 *
 * TODO در GameController:
 *   void summonGrave(int lane, int col)
 *   → TileType خانه مورد نظر را به TOMBSTONE یا DARK_TOMBSTONE تغییر می‌دهد
 *   → سنگ قبر با GRAVE_BUSTER قابل حذف خواهد بود
 */
public class SummonGraveAction implements ZombieAction {

    private final GameController gameController;

    public SummonGraveAction(GameController gameController) {
        this.gameController = gameController;
    }

    @Override
    public String getActionId() { return "SUMMON_GRAVE"; }

    @Override
    public void onStart(Zombie zombie, Map<String, String> params) {
        int xOffset   = parseIntParam(params, "x_offset", -1);
        int targetCol = Math.max(1, (int) Math.round(zombie.getX()) + xOffset);
        int lane      = zombie.getY();

        // TODO: gameController.summonGrave(lane, targetCol);
    }

    @Override
    public void onEnd(Zombie zombie) { }
}
