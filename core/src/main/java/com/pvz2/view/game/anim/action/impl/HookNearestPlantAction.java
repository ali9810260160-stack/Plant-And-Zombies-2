package com.pvz2.view.game.anim.action.impl;

import com.pvz2.controller.GameController;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.view.game.anim.AnimationSystem;
import com.pvz2.view.game.anim.action.ZombieAction;

import java.util.Map;

/**
 * Action: Fisherman Zombie (Big Wave Beach) با قلاب ماهیگیری یک گیاه را
 * به سمت خودش می‌کشد (بر اساس clip های واقعی چندمرحله‌ای "cast"→
 * "cast_loop"→"reel" در ZOMBIE_BEACH_FISHERMAN — این ability باید در
 * JSON با enterClip="cast"، clip="cast_loop"، exitClip="reel" تعریف شود
 * تا ZombieAnimController فاز به فاز آن را مدیریت کند).
 *
 * پارامترهای JSON: range (پیش‌فرض 4.0)
 * TODO در GameController: void hookAndPullPlant(int lane, float range)
 */
public class HookNearestPlantAction implements ZombieAction {

    private final GameController gameController;
    private final AnimationSystem animationSystem;

    public HookNearestPlantAction(GameController gameController, AnimationSystem animationSystem) {
        this.gameController = gameController;
        this.animationSystem = animationSystem;
    }

    @Override
    public String getActionId() { return "HOOK_NEAREST_PLANT"; }

    @Override
    public void onStart(Zombie zombie, Map<String, String> params) {
        float range = parseFloatParam(params, "range", 4.0f);
        int lane = zombie.getY();
        // TODO: gameController.hookAndPullPlant(lane, range);
        // نکته: کشیدن واقعی گیاه باید در انتقال فاز LOOP→EXIT اجرا شود:
        if (animationSystem != null) animationSystem.endAbilityFor(zombie);
    }

    @Override
    public void onEnd(Zombie zombie) { }
}
