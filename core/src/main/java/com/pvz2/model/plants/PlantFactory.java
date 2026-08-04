package com.pvz2.model.plants;

import com.pvz2.model.User;
import com.pvz2.model.enums.PlantTag;
import com.pvz2.model.enums.PlantType;
import com.pvz2.service.PlantUpgradeHandler;

/**
 * Factory برای ساخت نمونه‌های گیاه بر اساس نوع.
 * داده‌های گیاه از PlantDataRegistry خوانده می‌شود (data-driven).
 */
public class PlantFactory {

    private PlantFactory() { }

    /**
     * ساخت گیاه بدون اعمال ارتقا.
     */
    public static Plant create(PlantType type) {
        PlantStats stats = PlantDataRegistry.getInstance().getStats(type);
        if (stats == null) return null;

        switch (type) {
            // ── بلعنده: هضم + Instant Kill ──
            case CHOMPER:
                return new ChomperPlant(stats);

            // ── Pea Pod: چند سر قابل انباشت ──
            case PEA_POD:
                return new PeaPodPlant(stats);

            // ── Kernel-pult: تناوب ذرت/کره ──
            case KERNEL_PULT:
                return new KernelPultPlant(stats);

            // ── بقیه: GenericPlant (همه توانایی‌های تخصصی اینجا پیاده شده) ──
            default:
                return new GenericPlant(stats);
        }
    }

    /**
     * ساخت گیاه با اعمال خودکار تمام ارتقاهای ذخیره‌شده کاربر.
     * باید در همه مکان‌هایی که گیاه برای کاشت ساخته می‌شود استفاده شود.
     *
     * @param type نوع گیاه
     * @param user کاربر جاری (می‌تواند null باشد)
     */
    public static Plant createWithUpgrade(PlantType type, User user) {
        PlantStats stats = PlantDataRegistry.getInstance().getStats(type);
        if (stats == null) return null;
        GenericPlant plant = new GenericPlant(stats);
        if (user != null) {
            int upgradeLevel = user.getPlantUpgradeLevel(type.name());
            if (upgradeLevel > 0) {
                PlantUpgradeHandler.applyUpgrades(plant, upgradeLevel, stats);
            }
        }
        return plant;
    }

    // ---- متدهای کمکی ----

    public static int getSunCost(PlantType type) {
        // اگر کاربر ارتقا داشته، sun cost ممکن است کاهش یافته باشد؛
        // برای نمایش در منو، از stats پایه استفاده می‌کنیم.
        PlantStats stats = PlantDataRegistry.getInstance().getStats(type);
        return stats != null ? stats.getSunCost() : 0;
    }

    /**
     * هزینه sun گیاه با در نظر گرفتن ارتقای کاربر.
     */
    public static int getEffectiveSunCost(PlantType type, User user) {
        Plant p = createWithUpgrade(type, user);
        return p != null ? p.getSunCost() : getSunCost(type);
    }

    public static boolean isWaterPlant(PlantType type) {
        return type == PlantType.LILY_PAD || type == PlantType.TANGLE_KELP
                || type == PlantType.SEA_SHROOM;
    }

    public static boolean isStackable(PlantType type) {
        return type == PlantType.PUMPKIN || type == PlantType.LILY_PAD
                || type == PlantType.PEA_POD;
    }

    public static double getRechargeTime(PlantType type) {
        PlantStats stats = PlantDataRegistry.getInstance().getStats(type);
        return stats != null ? stats.getRechargeTime() : 7.5;
    }

    public static int getBaseHp(PlantType type) {
        PlantStats stats = PlantDataRegistry.getInstance().getStats(type);
        return stats != null ? stats.getBaseHp() : 300;
    }

    public static String getDescription(PlantType type) {
        PlantStats stats = PlantDataRegistry.getInstance().getStats(type);
        return stats != null
                ? (stats.getBaseAbility().isEmpty() ? stats.getDescription() : stats.getBaseAbility())
                : type.name() + " - no description available.";
    }

    public static String getCategory(PlantType type) {
        PlantStats stats = PlantDataRegistry.getInstance().getStats(type);
        return stats != null ? stats.getCategory() : "Unknown";
    }

    /** آیا این گیاه تگ مشخصی دارد؟ */
    public static boolean hasTag(PlantType type, PlantTag tag) {
        PlantStats stats = PlantDataRegistry.getInstance().getStats(type);
        return stats != null && stats.getTags().contains(tag);
    }
}
