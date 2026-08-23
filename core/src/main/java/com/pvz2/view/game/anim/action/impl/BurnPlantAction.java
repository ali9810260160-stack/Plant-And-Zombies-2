package com.pvz2.view.game.anim.action.impl;

import com.pvz2.controller.GameController;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.view.game.anim.action.ZombieAction;

import java.util.Map;

/**
 * Action: Dark Knight (یا هر زامبی آتشین) یک ستون را می‌سوزاند.
 *
 * پارامترهای JSON:
 *   intensity:  ضریب آسیب (1.0 = استاندارد)، پیش‌فرض 1.0
 *   colsAhead:  چند ستون جلوتر از موقعیت فعلی، پیش‌فرض 0
 *
 * مثال JSON:
 * <pre>
 *   "actionParams": { "intensity": "1.0", "colsAhead": "0" }
 * </pre>
 *
 * TODO در GameController:
 *   void burnColumn(int lane, int col, int damage)
 *   → همه گیاهان در خانه (lane, col) این مقدار آسیب FIRE می‌گیرند
 *   → گیاهان با tag FIRE_IMMUNE یا IMMUNE_TO_FIRE مصون هستند
 *   → اثر بصری: particle آتش روی خانه (در EffectSystem)
 */
public class BurnPlantAction implements ZombieAction {

    private static final int BASE_BURN_DAMAGE = 200;

    private final GameController gameController;

    public BurnPlantAction(GameController gameController) {
        this.gameController = gameController;
    }

    @Override
    public String getActionId() { return "BURN_LANE"; }

    @Override
    public void onStart(Zombie zombie, Map<String, String> params) {
        float intensity  = parseFloatParam(params, "intensity", 1.0f);
        int   colsAhead  = parseIntParam(params, "colsAhead", 0);
        int   targetCol  = Math.max(1, (int) Math.round(zombie.getX()) - colsAhead);
        int   lane       = zombie.getY();
        int   damage     = (int)(BASE_BURN_DAMAGE * intensity);

        // TODO: gameController.burnColumn(lane, targetCol, damage);
    }

    @Override
    public void onEnd(Zombie zombie) { }
}
