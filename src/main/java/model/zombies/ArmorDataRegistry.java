package model.zombies;

import model.enums.ArmorType;
import util.FileUtil;
import util.SimpleJsonParser;

import java.util.*;

/**
 * رجیستری زره‌ها — از data/armors.json بارگذاری می‌شود.
 */
public class ArmorDataRegistry {

    private static ArmorDataRegistry instance;

    public static class ArmorData {
        public final ArmorType enumType;
        public final String armorTypeName;
        public final int baseHealth;
        public final boolean metallic;
        public final boolean magnetAttracts;
        public final boolean droppable;
        public final String description;

        public ArmorData(ArmorType e, String name, int hp,
                         boolean metal, boolean magnet,
                         boolean drop, String desc) {
            this.enumType = e;
            this.armorTypeName = name;
            this.baseHealth = hp;
            this.metallic = metal;
            this.magnetAttracts = magnet;
            this.droppable = drop;
            this.description = desc;
        }
    }

    private final Map<ArmorType, ArmorData> dataMap;

    private ArmorDataRegistry() {
        dataMap = new EnumMap<>(ArmorType.class);
        loadFromJson();
    }

    public static ArmorDataRegistry getInstance() {
        if (instance == null) {
            instance = new ArmorDataRegistry();
        }
        return instance;
    }

    public ArmorData getData(ArmorType type) {
        return dataMap.get(type);
    }

    public int getBaseHealth(ArmorType type) {
        ArmorData d = dataMap.get(type);
        return d != null ? d.baseHealth : 0;
    }

    public boolean isMagnetAttracts(ArmorType type) {
        ArmorData d = dataMap.get(type);
        return d != null && d.magnetAttracts;
    }

    public Collection<ArmorData> getAllArmors() {
        return dataMap.values();
    }

    private void loadFromJson() {
        try {
            String json = FileUtil.readFile("data/armors.json");
            List<Map<String, String>> entries = SimpleJsonParser.parseArray(json);
            for (Map<String, String> m : entries) {
                ArmorData d = parseEntry(m);
                if (d != null) {
                    dataMap.put(d.enumType, d);
                }
            }
            System.out.println("[ArmorDataRegistry] Loaded "
                + dataMap.size() + " armor types from data/armors.json");
        } catch (Exception e) {
            System.err.println("[ArmorDataRegistry] Cannot load JSON: "
                + e.getMessage() + " — using built-in defaults.");
            loadDefaults();
        }
    }

    private ArmorData parseEntry(Map<String, String> m) {
        String enumStr = m.get("enumType");
        if (enumStr == null || enumStr.isEmpty()) {
            return null;
        }
        ArmorType type;
        try {
            type = ArmorType.valueOf(enumStr.trim().replace("\"",""));
        } catch (IllegalArgumentException e) {
            return null;
        }
        int hp       = parseInt(m.get("baseHealth"), 0);
        boolean met  = "true".equalsIgnoreCase(m.getOrDefault("metallic","false").trim());
        boolean mag  = "true".equalsIgnoreCase(m.getOrDefault("magnetAttracts","false").trim());
        boolean drop = "true".equalsIgnoreCase(m.getOrDefault("droppable","false").trim());
        String desc  = m.getOrDefault("description", type.name() + " armor.");
        String name  = m.getOrDefault("armorType", type.name());
        return new ArmorData(type, name, hp, met, mag, drop, desc);
    }

    private int parseInt(String s, int def) {
        if (s == null || s.isEmpty() || s.equals("null")) {
            return def;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private void loadDefaults() {
        dataMap.put(ArmorType.CONE,
            new ArmorData(ArmorType.CONE,"Cone",370,false,false,true,
                "Traffic cone: 370 HP."));
        dataMap.put(ArmorType.BUCKET,
            new ArmorData(ArmorType.BUCKET,"Bucket",1100,true,true,true,
                "Bucket: 1100 HP, magnetic."));
        dataMap.put(ArmorType.BLOCK,
            new ArmorData(ArmorType.BLOCK,"Brick",2200,false,false,true,
                "Ice block: 2200 HP."));
        dataMap.put(ArmorType.HELMET,
            new ArmorData(ArmorType.HELMET,"Crown",1600,true,true,true,
                "Knight helmet: 1600 HP, magnetic."));
        dataMap.put(ArmorType.SHOULDER_ARMOR,
            new ArmorData(ArmorType.SHOULDER_ARMOR,"ShoulderArmor",1600,
                false,false,true,"Knight shoulders: 1600 HP."));
        dataMap.put(ArmorType.NEWSPAPER,
            new ArmorData(ArmorType.NEWSPAPER,"Newspaper",800,false,false,
                false,"Newspaper: 800 HP. Rage on destroy!"));
        dataMap.put(ArmorType.BARREL,
            new ArmorData(ArmorType.BARREL,"Barrel",1100,true,true,true,
                "Barrel: 1100 HP, blocks shots."));
    }
}
