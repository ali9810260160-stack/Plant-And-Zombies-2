package com.pvz2.view.game.anim.action.impl;

import com.pvz2.controller.GameController;
import com.pvz2.model.enums.ZombieType;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.view.game.anim.action.ZombieAction;

import java.util.Map;

/**
 * Action: زامبی به نوع دیگری تبدیل می‌شود.
 *
 * مثال کلاسیک PvZ2:
 *   Newspaper Zombie پس از از دست دادن روزنامه (NEWSPAPER armor broken)،
 *   سرعتش دو برابر می‌شود — از نظر View، clip ها تغییر می‌کنند.
 *
 * پارامترهای JSON:
 *   targetType: ZombieType جدید (باید در ZombieType enum وجود داشته باشد)
 *
 * مثال JSON:
 * <pre>
 *   "actionParams": { "targetType": "NEWSPAPER_ZOMBIE" }
 * </pre>
 *
 * TODO در GameController:
 *   void transformZombie(Zombie oldZombie, ZombieType newType)
 *   → یک Zombie جدید با newType و همان HP، x، y، y می‌سازد
 *   → oldZombie را از activeZombies حذف می‌کند
 *   → zombie جدید را اضافه می‌کند
 *   → AnimationSystem به‌طور خودکار controller جدیدی می‌سازد چون
 *     instance جدید است و در IdentityHashMap نیست
 *
 * نکته طراحی:
 *   در PvZ2 واقعی، Newspaper Zombie سرعت بالاتر با تغییر clip suffix
 *   مدیریت می‌شود، نه تغییر type. می‌توانید به جای TRANSFORM،
 *   یک field `speedMultiplierOverride` به Zombie اضافه کنید.
 */
public class TransformAction implements ZombieAction {

    private final GameController gameController;

    public TransformAction(GameController gameController) {
        this.gameController = gameController;
    }

    @Override
    public String getActionId() { return "TRANSFORM"; }

    @Override
    public void onStart(Zombie zombie, Map<String, String> params) {
        String targetTypeName = parseStringParam(params, "targetType", "");
        if (targetTypeName.isEmpty()) return;

        ZombieType targetType;
        try {
            targetType = ZombieType.valueOf(targetTypeName);
        } catch (IllegalArgumentException e) {
            return; // نوع نامعتبر
        }

        // TODO: gameController.transformZombie(zombie, targetType);
    }

    @Override
    public void onEnd(Zombie zombie) { }

    @Override
    public boolean canFire(Zombie zombie) {
        return zombie.getCurrentHealth() > 0;
    }
}
