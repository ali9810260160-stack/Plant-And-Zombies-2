package com.pvz2.view.game.anim.action.impl;

import com.pvz2.controller.GameController;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.view.game.anim.action.ZombieAction;

import java.util.Map;

/**
 * Action: Octopus Zombie (Big Wave Beach) یک گیاه را می‌گیرد و به پشت
 * صفوف زامبی‌ها پرت می‌کند (بر اساس clip واقعی "toss" در
 * ZOMBIE_BEACH_OCTOPUS). trigger این قابلیت ON_ENTER است — یعنی
 * معمولاً بلافاصله پس از ظاهر شدن این زامبی رخ می‌دهد.
 *
 * TODO در GameController: void tossPlantToBack(int lane)
 */
public class TossPlantBehindAction implements ZombieAction {

    private final GameController gameController;

    public TossPlantBehindAction(GameController gameController) {
        this.gameController = gameController;
    }

    @Override
    public String getActionId() { return "TOSS_PLANT_BEHIND"; }

    @Override
    public void onStart(Zombie zombie, Map<String, String> params) {
        int lane = zombie.getY();
        // TODO: gameController.tossPlantToBack(lane);
    }

    @Override
    public void onEnd(Zombie zombie) { }
}
