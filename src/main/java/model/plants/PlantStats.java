package model.plants;

import model.enums.PlantFamily;
import model.enums.PlantTag;
import model.enums.PlantType;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO حاوی آمار ثابت یک گیاه (از JSON بارگذاری می‌شود).
 * این داده‌ها به ازای هر نوع گیاه یک بار در حافظه موجودند.
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
}
