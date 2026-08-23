package com.pvz2.view.game.anim.action.impl;

import com.pvz2.controller.GameController;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.view.game.anim.AnimationSystem;
import com.pvz2.view.game.anim.action.ZombieAction;

import java.util.Map;

/**
 * Action: Ra Zombie نزدیک‌ترین خورشید را می‌دزدد.
 *
 * ═══ ویژگی جدید نسخه ۲ ═══
 * طبق بررسی assets واقعی، Ra Zombie این قابلیت را با ۳ فاز نشان می‌دهد:
 * power_up → power (loop) → power_down. این JSON باید با
 * enterClip="power_up"، clip="power"، exitClip="power_down" تعریف شود.
 * پس از پایان منطق دزدی، باید animationSystem.endAbilityFor(zombie)
 * صدا زده شود تا فاز EXIT (power_down) پخش شود.
 *
 * پارامترهای JSON: range (پیش‌فرض 3.0)
 *
 * TODO در GameController:
 *   void zombieStealNearestSun(Zombie zombie, float range)
 */
public class CollectSunAction implements ZombieAction {

    private final GameController gameController;
    private final AnimationSystem animationSystem;

    public CollectSunAction(GameController gameController, AnimationSystem animationSystem) {
        this.gameController = gameController;
        this.animationSystem = animationSystem;
    }

    @Override
    public String getActionId() { return "COLLECT_SUN"; }

    @Override
    public void onStart(Zombie zombie, Map<String, String> params) {
        float range = parseFloatParam(params, "range", 3.0f);

        // TODO: boolean stole = gameController.zombieStealNearestSun(zombie, range);
        // پس از اتمام منطق (حتی اگر خورشیدی پیدا نشد)، فاز EXIT را فعال کن:
        if (animationSystem != null) animationSystem.endAbilityFor(zombie);
    }

    @Override
    public void onEnd(Zombie zombie) { }
}
