package com.pvz2.view.game.anim.action.impl;

import com.pvz2.controller.GameController;
import com.pvz2.model.zombies.Zombie;
import com.pvz2.view.game.anim.action.ZombieAction;

import java.util.Map;

/**
 * Action: Dark Wizard (و نسخه Veteran) یک گیاه تصادفی جلوی خودش را به
 * گوسفند تبدیل می‌کند (بر اساس clip واقعی "sheep" در ZOMBIE_DARK_WIZARD
 * / ZOMBIE_DARK_WIZARD_VETERAN که در بررسی assets واقعی کشف شد).
 *
 * گیاه تبدیل‌شده دیگر نمی‌تواند حمله کند تا زمانی که با ضربه (Bonk Choy)
 * یا گذر زمان به حالت عادی برگردد.
 *
 * ═══ نکته پیاده‌سازی فاز ۱ ═══
 * این قابلیت در کد فاز ۱ فعلی (WizardZombie و مدل Plant) پیاده‌سازی
 * نشده است. برای فعال‌سازی کامل، باید:
 *   ۱. یک فیلد {@code transient boolean isSheep} به Plant.java اضافه شود
 *   ۲. در CombatService، گیاه‌های isSheep=true نباید بتوانند حمله کنند
 *   ۳. GameController.transformPlantToSheep(lane, rangeCols) باید یک
 *      گیاه تصادفی در محدوده را پیدا کرده و isSheep=true کند
 * تا آن زمان، این action صرفاً انیمیشن "sheep" زامبی را پخش می‌کند
 * (بدون تاثیر واقعی روی گیاه) — که حداقل رفتار بصری صحیح را نشان می‌دهد.
 */
public class TransformPlantToSheepAction implements ZombieAction {

    private final GameController gameController;

    public TransformPlantToSheepAction(GameController gameController) {
        this.gameController = gameController;
    }

    @Override
    public String getActionId() { return "TRANSFORM_PLANT_TO_SHEEP"; }

    @Override
    public void onStart(Zombie zombie, Map<String, String> params) {
        int lane = zombie.getY();
        // TODO (نیازمند تغییر فاز ۱ — به کامنت بالای کلاس مراجعه کنید):
        // gameController.transformPlantToSheep(lane, 9);
    }

    @Override
    public void onEnd(Zombie zombie) { }
}
