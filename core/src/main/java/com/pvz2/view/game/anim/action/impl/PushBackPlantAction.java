package com.pvz2.view.game.anim.action.impl;

import com.pvz2.controller.GameController;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.view.game.anim.action.ZombieAction;

import java.util.Map;

/**
 * Action: Troglobite (Frostbite Caves) گیاه جلوی خودش را عقب هل می‌دهد
 * (بر اساس clip واقعی "push" در ZOMBIE_ICEAGE_TROGLOBITE). معادل حمله
 * ویژه‌ای که گیاه را از خانه فعلی به خانه پشتی (اگر خالی باشد) منتقل
 * می‌کند، مشابه رفتار Gargantuar در بازی واقعی.
 *
 * TODO در GameController: void pushBackPlant(int lane, int fromCol)
 */
public class PushBackPlantAction implements ZombieAction {

    private final GameController gameController;

    public PushBackPlantAction(GameController gameController) {
        this.gameController = gameController;
    }

    @Override
    public String getActionId() { return "PUSH_BACK_PLANT"; }

    @Override
    public void onStart(Zombie zombie, Map<String, String> params) {
        int lane = zombie.getY();
        int col  = (int) Math.round(zombie.getX());
        // TODO: gameController.pushBackPlant(lane, col);
    }

    @Override
    public void onEnd(Zombie zombie) { }
}
