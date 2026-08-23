package com.pvz2.view.game.anim.action.impl;

import com.pvz2.controller.GameController;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.view.game.anim.action.ZombieAction;

import java.util.Map;

/**
 * Action: King Zombie زامبی‌های اطراف را برای مدتی تقویت می‌کند
 * (بر اساس clip واقعی "special" در ZOMBIE_DARK_KING).
 *
 * پارامترهای JSON: range (پیش‌فرض 3.0)
 *
 * TODO در GameController:
 *   void buffNearbyZombies(Zombie source, float range)
 *   → افزایش موقت سرعت یا آسیب زامبی‌های داخل range (پیاده‌سازی دقیق
 *     بستگی به تصمیم طراحی شما دارد — در بازی واقعی King Zombie
 *     زامبی‌های اطراف را ارتقا می‌دهد)
 */
public class BuffNearbyZombiesAction implements ZombieAction {

    private final GameController gameController;

    public BuffNearbyZombiesAction(GameController gameController) {
        this.gameController = gameController;
    }

    @Override
    public String getActionId() { return "BUFF_NEARBY_ZOMBIES"; }

    @Override
    public void onStart(Zombie zombie, Map<String, String> params) {
        float range = parseFloatParam(params, "range", 3.0f);
        // TODO: gameController.buffNearbyZombies(zombie, range);
    }

    @Override
    public void onEnd(Zombie zombie) { }
}
