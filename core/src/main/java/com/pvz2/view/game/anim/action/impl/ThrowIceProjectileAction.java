package com.pvz2.view.game.anim.action.impl;

import com.pvz2.controller.GameController;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.view.game.anim.action.ZombieAction;

import java.util.Map;

/**
 * Action: Hunter Zombie (Frostbite Caves) یک نیزه/تیر یخی به سمت گیاهان
 * پرتاب می‌کند (بر اساس clip واقعی "throw" در ZOMBIE_ICEAGE_HUNTER).
 * این زامبی برخلاف اکثر زامبی‌های عادی می‌تواند از فاصله به گیاهان
 * آسیب بزند.
 *
 * TODO در GameController:
 *   void zombieThrowProjectileAt(Zombie source, int targetLane, int targetCol, int damage)
 */
public class ThrowIceProjectileAction implements ZombieAction {

    private final GameController gameController;

    public ThrowIceProjectileAction(GameController gameController) {
        this.gameController = gameController;
    }

    @Override
    public String getActionId() { return "THROW_ICE_PROJECTILE"; }

    @Override
    public void onStart(Zombie zombie, Map<String, String> params) {
        int lane = zombie.getY();
        int targetCol = Math.max(1, (int) Math.round(zombie.getX()) - 3);
        // TODO: gameController.zombieThrowProjectileAt(zombie, lane, targetCol, 40);
    }

    @Override
    public void onEnd(Zombie zombie) { }
}
