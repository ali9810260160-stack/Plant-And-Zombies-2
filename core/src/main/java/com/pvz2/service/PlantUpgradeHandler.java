package com.pvz2.service;

import com.pvz2.model.plants.GenericPlant;
import com.pvz2.model.plants.PlantStats;

/**
 * هندلر ارتقای دائمی گیاهان.
 *
 * رشته‌های ارتقا از فایل JSON خوانده می‌شوند (مثل "Dmg +10"، "HP +150").
 * این هندلر آن‌ها را parse کرده و روی نمونه گیاه اعمال می‌کند.
 * چون ارتقا دائمی است، هر بار که گیاه ساخته می‌شود باید اعمال شود.
 */
public final class PlantUpgradeHandler {

    private PlantUpgradeHandler() { }

    /**
     * تمام سطوح ارتقای انباشته‌شده را روی گیاه اعمال می‌کند.
     *
     * @param plant        نمونه گیاه تازه‌ساخته‌شده
     * @param upgradeLevel سطح ارتقای ذخیره‌شده برای کاربر (0 = بدون ارتقا، 1..3 = ارتقاهای اول تا سوم)
     * @param stats        آمار پایه گیاه از registry
     */
    public static void applyUpgrades(GenericPlant plant, int upgradeLevel,
                                     PlantStats stats) {
        if (stats == null || upgradeLevel <= 0) {
            return;
        }
        // هر سطح به‌صورت انباشته اعمال می‌شود
        for (int lvl = 1; lvl <= upgradeLevel; lvl++) {
            String effect = stats.getUpgradeEffect(lvl);
            if (effect != null && !effect.isEmpty()) {
                applySingleEffect(effect.trim(), plant);
            }
        }
    }

    /**
     * یک رشته اثر واحد را parse کرده و روی گیاه اعمال می‌کند.
     * قالب‌های پشتیبانی‌شده:
     *   "Dmg +N"               ← افزایش آسیب پایه
     *   "HP +N"                ← افزایش HP پایه
     *   "Cost -N"              ← کاهش هزینه خورشید
     *   "Cooldown -Ns"         ← کاهش زمان شارژ
     *   "Prod. Time -Ns"       ← کاهش فاصله تولید خورشید
     *   "Atk Speed +N%"        ← افزایش سرعت حمله (درصد)
     *   "Chill Time +Ns"       ← افزایش مدت کند‌شدگی
     *   "Freeze Time +Ns"      ← افزایش مدت یخ‌زدگی
     *   "Arm Time -Ns"         ← کاهش زمان آماده‌سازی تله
     *   "Digest -Ns"           ← کاهش زمان هضم (Chomper)
     *   "Range +N Tile"        ← افزایش برد
     *   "Grow Time -Ns"        ← کاهش زمان رشد (SunShroom)
     *   "Regen -Ns"            ← کاهش زمان تجدید
     *   "AoE Dmg +N"           ← افزایش آسیب منطقه‌ای
     *   "Double Sun Chance"    ← فعال کردن شانس خورشید دو‌برابر
     *   "Butter +N%"           ← افزایش شانس خواباندن زامبی
     *   "Pierce +N"            ← افزایش نفوذ پرتابه
     *   "Lifespan +Ns"         ← افزایش طول عمر گیاه
     *   "Plant Food Chance +N%"← افزایش شانس دریافت plant food
     *   "Max Size +N"          ← افزایش سایز نهایی
     *   "Can crush 2x"         ← قابلیت له‌کردن دو بار
     *   "Bounces +N"           ← افزایش برخوردهای پرتابه
     *   "Targets +N"           ← افزایش هدف همزمان
     *   "Target Priority Up"   ← ارتقای اولویت هدف‌گیری
     *   "Warmth Radius +N"     ← افزایش شعاع گرمایش
     *   "Dmg/Tick +N"          ← افزایش آسیب در هر تیک (poison)
     *   "Sun +N"               ← افزایش مقدار خورشید تولیدی
     */
    private static void applySingleEffect(String effect, GenericPlant plant) {
        if (effect.startsWith("Dmg +")) {
            int val = parseTrailingInt(effect, "Dmg +");
            plant.addBonusDamage(val);

        } else if (effect.startsWith("HP +")) {
            int val = parseTrailingInt(effect, "HP +");
            plant.addBonusHp(val);

        } else if (effect.startsWith("Cost -")) {
            int val = parseTrailingInt(effect, "Cost -");
            plant.reduceSunCost(val);

        } else if (effect.startsWith("Cooldown -")) {
            double secs = parseTrailingDouble(effect, "Cooldown -", "s");
            plant.reduceCooldown(secs);

        } else if (effect.startsWith("Prod. Time -")) {
            double secs = parseTrailingDouble(effect, "Prod. Time -", "s");
            plant.reduceSunProdInterval(secs);

        } else if (effect.startsWith("Atk Speed +")) {
            double pct = parseTrailingDouble(effect, "Atk Speed +", "%");
            plant.increaseAttackSpeedPct(pct);

        } else if (effect.startsWith("Arm Time -")) {
            double secs = parseTrailingDouble(effect, "Arm Time -", "s");
            plant.reduceArmTime(secs);

        } else if (effect.startsWith("Digest -")) {
            double secs = parseTrailingDouble(effect, "Digest -", "s");
            plant.reduceDigestTime(secs);

        } else if (effect.startsWith("Grow Time -")) {
            double secs = parseTrailingDouble(effect, "Grow Time -", "s");
            plant.reduceSunProdInterval(secs);

        } else if (effect.startsWith("Regen -")) {
            double secs = parseTrailingDouble(effect, "Regen -", "s");
            plant.reduceCooldown(secs);

        } else if (effect.startsWith("AoE Dmg +")) {
            int val = parseTrailingInt(effect, "AoE Dmg +");
            plant.addBonusAoeDamage(val);

        } else if (effect.startsWith("Range +")) {
            int val = parseLeadingInt(effect.replace("Range +", "").trim()
                    .replace(" Tile", "").replace(" tile", "").trim());
            plant.addBonusRange(val);

        } else if (effect.startsWith("Lifespan +")) {
            double secs = parseTrailingDouble(effect, "Lifespan +", "s");
            plant.addBonusLifespan((int) secs);

        } else if (effect.startsWith("Chill Time +")) {
            double secs = parseTrailingDouble(effect, "Chill Time +", "s");
            plant.addBonusChillTime((int)(secs * 10));

        } else if (effect.startsWith("Freeze Time +")) {
            double secs = parseTrailingDouble(effect, "Freeze Time +", "s");
            plant.addBonusChillTime((int)(secs * 10));

        } else if (effect.startsWith("Warmth Radius +")) {
            int val = parseTrailingInt(effect, "Warmth Radius +");
            plant.addBonusRange(val);

        } else if (effect.startsWith("Pierce +")) {
            int val = parseTrailingInt(effect, "Pierce +");
            plant.addBonusPierce(val);

        } else if (effect.startsWith("Bounces +")) {
            int val = parseTrailingInt(effect, "Bounces +");
            plant.addBonusBounces(val);

        } else if (effect.startsWith("Targets +")) {
            int val = parseTrailingInt(effect, "Targets +");
            plant.addBonusTargets(val);

        } else if (effect.startsWith("Max Size +")) {
            int val = parseTrailingInt(effect, "Max Size +");
            plant.addBonusMaxSize(val);

        } else if (effect.startsWith("Butter +")) {
            double pct = parseTrailingDouble(effect, "Butter +", "%");
            plant.addBonusSpecialChance(pct);

        } else if (effect.startsWith("Plant Food Chance +")) {
            double pct = parseTrailingDouble(effect, "Plant Food Chance +", "%");
            plant.addBonusSpecialChance(pct);

        } else if (effect.startsWith("Dmg/Tick +")) {
            int val = parseTrailingInt(effect, "Dmg/Tick +");
            plant.addBonusDamage(val);

        } else if (effect.startsWith("Sun +")) {
            int val = parseTrailingInt(effect, "Sun +");
            plant.addBonusSunAmount(val);

        } else if (effect.equals("Double Sun Chance")) {
            plant.setDoubleSunChance(true);

        } else if (effect.equals("Can crush 2x")) {
            plant.setCanCrushTwice(true);

        } else if (effect.equals("Target Priority Up")) {
            plant.setTargetPriorityUp(true);
        }
        // سایر موارد ناشناخته نادیده گرفته می‌شوند
    }

    // --------- helpers ---------

    /** عدد صحیح در انتهای رشته بعد از prefix */
    private static int parseTrailingInt(String effect, String prefix) {
        String raw = effect.replace(prefix, "").trim();
        // حذف پسوند‌های احتمالی
        raw = raw.replaceAll("[^0-9]", "");
        if (raw.isEmpty()) return 0;
        try { return Integer.parseInt(raw); } catch (NumberFormatException e) { return 0; }
    }

    /** عدد اعشاری در انتهای رشته بعد از prefix و قبل از suffix */
    private static double parseTrailingDouble(String effect, String prefix, String suffix) {
        String raw = effect.replace(prefix, "");
        if (suffix != null && !suffix.isEmpty()) {
            raw = raw.replace(suffix, "");
        }
        raw = raw.trim();
        if (raw.isEmpty()) return 0;
        try { return Double.parseDouble(raw); } catch (NumberFormatException e) { return 0; }
    }

    /** پارس عدد از ابتدای رشته */
    private static int parseLeadingInt(String s) {
        String num = s.replaceAll("[^0-9]", "");
        if (num.isEmpty()) return 0;
        try { return Integer.parseInt(num); } catch (NumberFormatException e) { return 0; }
    }
}
