package com.pvz2.view.game.anim.action.impl;

import com.pvz2.controller.GameController;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.view.game.anim.AnimationSystem;
import com.pvz2.view.game.anim.action.ZombieAction;

import java.util.Map;

/**
 * Action: Gargantuar در HP پایین یک Imp به سمت چپ پرت می‌کند.
 *
 * ═══ ویژگی جدید نسخه ۲ ═══
 * طبق بررسی assets واقعی، خود Imp دو state اضافی دارد: FLYING
 * (clip="fly") و LANDING (clip="land"). پس از اسپان شدن Imp توسط این
 * action، باید بلافاصله ZombieAnimController آن Imp را در حالت FLYING
 * قرار دهیم (نه WALK)، سپس با رسیدن به مقصد به LANDING و در پایان WALK
 * عادی برگردد.
 *
 * پارامترهای JSON: x_offset (پیش‌فرض -3)
 *
 * TODO در GameController:
 *   Zombie spawnZombieAt(ZombieType type, int lane, int col)
 *   (باید نمونه Zombie تازه‌ساخته‌شده را برگرداند تا این action بتواند
 *   controller آن را از AnimationSystem بگیرد و triggerFlyingFor بزند)
 */
public class SpawnImpAction implements ZombieAction {

    private final GameController gameController;
    private final AnimationSystem animationSystem;

    public SpawnImpAction(GameController gameController, AnimationSystem animationSystem) {
        this.gameController = gameController;
        this.animationSystem = animationSystem;
    }

    @Override
    public String getActionId() { return "SPAWN_IMP"; }

    @Override
    public void onStart(Zombie zombie, Map<String, String> params) {
        int xOffset   = parseIntParam(params, "x_offset", -3);
        int targetCol = Math.max(1, (int) Math.round(zombie.getX()) + xOffset);
        int lane      = zombie.getY();

        // TODO: Zombie imp = gameController.spawnZombieAt(ZombieType.IMP, lane, targetCol);
        // TODO: if (imp != null && animationSystem != null) animationSystem.triggerFlyingFor(imp);
    }

    @Override
    public void onEnd(Zombie zombie) { }
}
