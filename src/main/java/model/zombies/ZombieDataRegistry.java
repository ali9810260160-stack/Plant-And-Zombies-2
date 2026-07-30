package model.zombies;

import model.enums.ArmorType;
import model.enums.ZombieType;
import util.FileUtil;
import util.SimpleJsonParser;

import java.util.*;

/**
 * رجیستری زامبی‌ها — از data/zombies.json بارگذاری می‌شود.
 */
public class ZombieDataRegistry {

    private static ZombieDataRegistry instance;
    private final Map<ZombieType, ZombieStats> map;

    private ZombieDataRegistry() {
        map = new EnumMap<>(ZombieType.class);
        loadFromJson();
    }

    public static ZombieDataRegistry getInstance() {
        if (instance == null) {
            instance = new ZombieDataRegistry();
        }
        return instance;
    }

    public ZombieStats getStats(ZombieType type) {
        return map.get(type);
    }

    public void register(ZombieStats stats) {
        map.put(stats.getType(), stats);
    }

    private void loadFromJson() {
        try {
            String json = FileUtil.readFile("data/zombies.json");
            List<Map<String, String>> entries = SimpleJsonParser.parseArray(json);
            for (Map<String, String> m : entries) {
                ZombieStats s = parseEntry(m);
                if (s != null) {
                    map.put(s.getType(), s);
                }
            }
            // zombies loaded successfully
        } catch (Exception e) {
            System.err.println("[ZombieDataRegistry] Cannot load JSON: "
                    + e.getMessage() + " — using built-in defaults.");
            loadDefaults();
        }
    }

    private ZombieStats parseEntry(Map<String, String> m) {
        String typeStr = m.get("type");
        if (typeStr == null || typeStr.trim().isEmpty()) {
            return null;
        }
        ZombieType type;
        try {
            type = ZombieType.valueOf(typeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
        int hp      = parseInt(m.get("hp"), 190);
        int dps     = parseInt(m.get("dps"), 100);
        double spd  = parseDouble(m.get("moveSpeed"), 0.185);
        int cost    = parseInt(m.get("waveCost"), 100);
        String desc = m.getOrDefault("description", type.name() + " zombie.");
        ZombieStats s = new ZombieStats(type, hp, dps, spd, cost, desc);
        parseArmors(m.getOrDefault("armors", "[]"), s);
        return s;
    }

    private void parseArmors(String raw, ZombieStats s) {
        if (raw == null || raw.equals("[]") || raw.isEmpty()) {
            return;
        }
        // Parse: [{"alias":"...","type":"CONE","hp":370}, ...]
        String inner = raw.trim();
        if (!inner.startsWith("[")) {
            return;
        }
        inner = inner.substring(1);
        if (inner.endsWith("]")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        // Split on "},{" to get individual armor entries
        String[] items = inner.split("\\},\\s*\\{");
        for (String item : items) {
            item = item.replace("{", "").replace("}", "").trim();
            // Parse key:value pairs
            String armorTypeStr = extractJsonField(item, "type");
            String hpStr = extractJsonField(item, "hp");
            if (armorTypeStr == null || armorTypeStr.isEmpty()) {
                continue;
            }
            try {
                ArmorType armorType = ArmorType.valueOf(
                        armorTypeStr.trim().replace("\"", ""));
                int armorHp = parseInt(hpStr, 0);
                if (armorHp > 0) {
                    s.withArmor(armorType, armorHp);
                }
            } catch (IllegalArgumentException ignored) { }
        }
    }

    private String extractJsonField(String obj, String field) {
        String search = "\"" + field + "\"";
        int idx = obj.indexOf(search);
        if (idx < 0) {
            return null;
        }
        int colon = obj.indexOf(":", idx);
        if (colon < 0) {
            return null;
        }
        String rest = obj.substring(colon + 1).trim();
        if (rest.startsWith("\"")) {
            int end = rest.indexOf("\"", 1);
            return end > 0 ? rest.substring(1, end) : rest;
        }
        int comma = rest.indexOf(",");
        return comma > 0 ? rest.substring(0, comma).trim() : rest.trim();
    }

    private int parseInt(String s, int def) {
        if (s == null || s.isEmpty() || s.equals("null")) {
            return def;
        }
        try {
            return (int) Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private double parseDouble(String s, double def) {
        if (s == null || s.isEmpty() || s.equals("null")) {
            return def;
        }
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private void loadDefaults() {
        reg(ZombieType.NORMAL,190,100,0.185,100,"Basic zombie.");
        reg(ZombieType.CONEHEAD,190,100,0.185,200,"Cone gives extra protection.")
                .withArmor(ArmorType.CONE,370);
        reg(ZombieType.BUCKETHEAD,190,100,0.185,400,"Bucket protection.")
                .withArmor(ArmorType.BUCKET,1100);
        reg(ZombieType.KNIGHT,190,100,0.185,550,"Medieval armor.")
                .withArmor(ArmorType.HELMET,1600)
                .withArmor(ArmorType.SHOULDER_ARMOR,1600);
        reg(ZombieType.BLOCKHEAD,190,100,0.185,700,"Block protection.")
                .withArmor(ArmorType.BLOCK,2200);
        reg(ZombieType.IMP,100,100,0.22,50,"Small fast zombie.");
        reg(ZombieType.GARGANTUAR,3000,500,0.15,1500,"One-hit plant killer.");
        reg(ZombieType.ALL_STAR,1100,200,0.16,1000,"Charges at full speed.");
        reg(ZombieType.ARCADE_ZOMBIE,490,100,0.19,600,"Pushes arcade machine.")
                .withArmor(ArmorType.BARREL,1100);
        reg(ZombieType.NEWSPAPER_ZOMBIE,460,100,0.22,200,"Rages when paper breaks.")
                .withArmor(ArmorType.NEWSPAPER,800);
        reg(ZombieType.BARREL_ROLLER,190,100,0.185,100,"Barrel spawns imps.")
                .withArmor(ArmorType.BARREL,1100);
        reg(ZombieType.TURQUOISE_ZOMBIE,250,100,0.185,500,"Steals sun, fires laser.");
        reg(ZombieType.PROSPECTOR_ZOMBIE,190,100,0.16,200,"Dynamite reverses him.");
        reg(ZombieType.PIANIST_ZOMBIE,840,100,0.12,450,"Music shifts zombie lanes.");
        reg(ZombieType.RA_ZOMBIE,190,100,0.2,100,"Raises sun from ground.");
        reg(ZombieType.EXPLORER_ZOMBIE,250,150,0.25,250,"Torch burns nearby plants.");
        reg(ZombieType.TOMB_RAISER,380,100,0.185,300,"Creates tombstones.");
        reg(ZombieType.DODO_RIDER,490,150,0.3,600,"Flies over obstacles.");
        reg(ZombieType.HUNTER_ZOMBIE,700,100,0.12,500,"Ice throw +1 freeze.");
        reg(ZombieType.TROGLOBITE,470,100,0.185,600,"Pushes ice blocks.");
        reg(ZombieType.FISHERMAN_ZOMBIE,1000,100,0.0,700,"Hooks and pulls plants.");
        reg(ZombieType.SNORKEL_ZOMBIE,350,100,0.185,200,"Swims underwater.");
        reg(ZombieType.OCTOPUS_ZOMBIE,910,100,0.12,900,"Throws freezing octopuses.");
        reg(ZombieType.JESTER_ZOMBIE,490,100,0.12,450,"Deflects projectiles.");
        reg(ZombieType.WIZARD_ZOMBIE,490,0,0.12,800,"Turns plants to cats.");
        reg(ZombieType.KING_ZOMBIE,1000,0,0.0,750,"Upgrades nearby zombies.");
        reg(ZombieType.DRAGON_IMP,150,100,0.22,150,"Fire-immune Imp.");
        reg(ZombieType.ZOMBOTANY_PEASHOOTER,190,100,0.185,100,"Fires peas at plants.");
        reg(ZombieType.ZOMBOTANY_WALLNUT,4000,100,0.185,100,"4000 HP zombie.");
        reg(ZombieType.ZOMBOTANY_JALAPENO,190,100,0.185,100,"Burns row after 10s.");
        reg(ZombieType.ZOMBOTANY_SQUASH,190,500,0.4,100,"Fast crusher.");
        reg(ZombieType.SUN_PRODUCER_ZOMBIE,490,100,0.185,100,"Produces sun.");
        reg(ZombieType.PARASOL_ZOMBIE,350,100,0.25,200,"Deflects lobbers.");
    }

    private ZombieStats reg(ZombieType t, int hp, int dps, double spd,
                            int cost, String desc) {
        ZombieStats s = new ZombieStats(t, hp, dps, spd, cost, desc);
        map.put(t, s);
        return s;
    }
}
