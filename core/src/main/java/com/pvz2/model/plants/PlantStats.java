package com.pvz2.model.plants;

import com.pvz2.model.enums.PlantFamily;
import com.pvz2.model.enums.PlantTag;
import com.pvz2.model.enums.PlantType;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO آمار ثابت یک گیاه — از JSON بارگذاری می‌شود.
 */
public class PlantStats {

    private PlantType type;
    private PlantFamily family;
    private List<PlantTag> tags;
    private int baseHp;
    private int sunCost;
    private double rechargeTime;
    private int baseDamage;
    private double attackSpeed;
    private int range;
    private String category;
    private String description;
    private String baseAbility;
    private String plantFoodEffect;
    /** فاصله زمانی تولید خورشید (برای تولیدکنندگان) — ثانیه */
    private double actionIntervalSeconds;
    /** رشته ارتقا سطح ۱→۲ */
    private String lvl2Upgrade;
    /** رشته ارتقا سطح ۲→۳ */
    private String lvl3Upgrade;
    /** رشته ارتقا سطح ۳→۴ */
    private String lvl4Upgrade;

    public PlantStats(PlantType type, PlantFamily family, int baseHp,
                      int sunCost, double rechargeTime, int baseDamage,
                      double attackSpeed, int range, String category,
                      String description) {
        this.type = type;
        this.family = family;
        this.baseHp = baseHp;
        this.sunCost = sunCost;
        this.rechargeTime = rechargeTime;
        this.baseDamage = baseDamage;
        this.attackSpeed = attackSpeed;
        this.range = range;
        this.category = category;
        this.description = description;
        this.tags = new ArrayList<>();
        this.baseAbility = "";
        this.plantFoodEffect = "";
        this.actionIntervalSeconds = 24.0;
        this.lvl2Upgrade = "";
        this.lvl3Upgrade = "";
        this.lvl4Upgrade = "";
    }

    /**
     * رشته ارتقا برای یک سطح مشخص (۱ = سطح اول ارتقا، ۲ = دوم، ۳ = سوم).
     */
    public String getUpgradeEffect(int upgradeLevel) {
        switch (upgradeLevel) {
            case 1: return lvl2Upgrade != null ? lvl2Upgrade : "";
            case 2: return lvl3Upgrade != null ? lvl3Upgrade : "";
            case 3: return lvl4Upgrade != null ? lvl4Upgrade : "";
            default: return "";
        }
    }

    public PlantType getType() { return type; }
    public PlantFamily getFamily() { return family; }
    public List<PlantTag> getTags() { return tags; }
    public void setTags(List<PlantTag> tags) { this.tags = tags; }
    public int getBaseHp() { return baseHp; }
    public int getSunCost() { return sunCost; }
    public double getRechargeTime() { return rechargeTime; }
    public int getBaseDamage() { return baseDamage; }
    public double getAttackSpeed() { return attackSpeed; }
    public int getRange() { return range; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getBaseAbility() { return baseAbility; }
    public void setBaseAbility(String s) { this.baseAbility = s; }
    public String getPlantFoodEffect() { return plantFoodEffect; }
    public void setPlantFoodEffect(String s) { this.plantFoodEffect = s; }
    public double getActionIntervalSeconds() { return actionIntervalSeconds; }
    public void setActionIntervalSeconds(double s) { this.actionIntervalSeconds = s; }
    public String getLvl2Upgrade() { return lvl2Upgrade; }
    public void setLvl2Upgrade(String s) { this.lvl2Upgrade = s != null ? s : ""; }
    public String getLvl3Upgrade() { return lvl3Upgrade; }
    public void setLvl3Upgrade(String s) { this.lvl3Upgrade = s != null ? s : ""; }
    public String getLvl4Upgrade() { return lvl4Upgrade; }
    public void setLvl4Upgrade(String s) { this.lvl4Upgrade = s != null ? s : ""; }
}
