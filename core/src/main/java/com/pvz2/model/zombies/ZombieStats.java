package com.pvz2.model.zombies;

import com.pvz2.model.enums.ArmorType;
import com.pvz2.model.enums.ZombieType;

import java.util.HashMap;
import java.util.Map;

/**
 * داده‌های ثابت یک زامبی.
 */
public class ZombieStats {
    private final ZombieType type;
    private final int hp;
    private final int dps;
    private final double moveSpeed;
    private final int waveCost;
    private final Map<ArmorType, Integer> armors;
    private final String description;

    public ZombieStats(ZombieType type, int hp, int dps, double moveSpeed,
                       int waveCost, String description) {
        this.type = type;
        this.hp = hp;
        this.dps = dps;
        this.moveSpeed = moveSpeed;
        this.waveCost = waveCost;
        this.armors = new HashMap<>();
        this.description = description;
    }

    public ZombieType getType() { return type; }
    public int getHp() { return hp; }
    public int getDps() { return dps; }
    public double getMoveSpeed() { return moveSpeed; }
    public int getWaveCost() { return waveCost; }
    public Map<ArmorType, Integer> getArmors() { return armors; }
    public String getDescription() { return description; }

    public ZombieStats withArmor(ArmorType at, int hp2) {
        armors.put(at, hp2);
        return this;
    }
}
