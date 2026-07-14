package model.plants;

import model.enums.PlantType;

/**
 * Factory برای ساخت نمونه‌های گیاه بر اساس نوع.
 * داده‌های گیاه از PlantDataRegistry خوانده می‌شود (data-driven).
 */
public class PlantFactory {

    public static Plant create(PlantType type) {
        PlantStats stats = PlantDataRegistry.getInstance().getStats(type);
        if (stats == null) {
            return null;
        }
        return new GenericPlant(stats);
    }

    public static int getSunCost(PlantType type) {
        PlantStats stats = PlantDataRegistry.getInstance().getStats(type);
        return stats != null ? stats.getSunCost() : 0;
    }

    public static boolean isWaterPlant(PlantType type) {
        if (type == PlantType.LILY_PAD || type == PlantType.TANGLE_KELP) {
            return true;
        }
        return false;
    }

    public static boolean isStackable(PlantType type) {
        if (type == PlantType.PUMPKIN || type == PlantType.LILY_PAD) {
            return true;
        }
        return false;
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
        return stats != null ? stats.getDescription()
               : type.name() + " - no description available.";
    }

    public static String getCategory(PlantType type) {
        PlantStats stats = PlantDataRegistry.getInstance().getStats(type);
        return stats != null ? stats.getCategory() : "Unknown";
    }
}
