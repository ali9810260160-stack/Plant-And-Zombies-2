package com.pvz2.model.plants;

import com.pvz2.model.enums.PlantFamily;
import com.pvz2.model.enums.PlantTag;
import com.pvz2.model.enums.PlantType;
import com.pvz2.util.FileUtil;
import com.pvz2.util.SimpleJsonParser;

import java.util.*;

/**
 * رجیستری گیاهان — از data/plants.json بارگذاری می‌شود.
 * data-driven: اضافه کردن گیاه جدید = فقط JSON را ویرایش کن.
 */
public class PlantDataRegistry {

    private static PlantDataRegistry instance;
    private final Map<PlantType, PlantStats> statsMap;

    private PlantDataRegistry() {
        statsMap = new EnumMap<>(PlantType.class);
        loadFromJson();
        loadMinigameExtras();
    }

    public static PlantDataRegistry getInstance() {
        if (instance == null) {
            instance = new PlantDataRegistry();
        }
        return instance;
    }

    public PlantStats getStats(PlantType type) {
        return statsMap.get(type);
    }

    public void registerStats(PlantStats stats) {
        statsMap.put(stats.getType(), stats);
    }

    public Collection<PlantStats> getAllStats() {
        return statsMap.values();
    }

    private void loadFromJson() {
        try {
            String json = FileUtil.readFile("data/plants.json");
            List<Map<String, String>> entries = SimpleJsonParser.parseArray(json);
            for (Map<String, String> m : entries) {
                PlantStats s = parseEntry(m);
                if (s != null) {
                    statsMap.put(s.getType(), s);
                }
            }
            // plants loaded successfully
        } catch (Exception e) {
            System.err.println("[PlantDataRegistry] Cannot load JSON: "
                    + e.getMessage() + " — using built-in defaults.");
            loadBuiltinDefaults();
        }
    }

    private PlantStats parseEntry(Map<String, String> m) {
        String typeStr = m.get("type");
        if (typeStr == null || typeStr.trim().isEmpty()) {
            return null;
        }
        PlantType type;
        try {
            type = PlantType.valueOf(typeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
        PlantFamily family = parseFamily(m.getOrDefault("family", "SHOOTER"));
        int hp       = parseInt(m.get("baseHp"), 300);
        int cost     = parseInt(m.get("sunCost"), 100);
        double rch   = parseDouble(m.get("rechargeSeconds"), 7.5);
        int dmg      = parseInt(m.get("damage"), 0);
        double spd   = parseDouble(m.get("attackSpeed"), 0);
        String cat   = m.getOrDefault("category", "Shooter");
        String base  = m.getOrDefault("baseAbility", "");
        String desc  = base.isEmpty()
                ? type.name().replace("_", " ") + " plant." : base;
        PlantStats s = new PlantStats(type, family, hp, cost,
                rch, dmg, spd, 9, cat, desc);
        s.setTags(parseTags(m.getOrDefault("tags", "[]")));
        s.setBaseAbility(base);
        s.setPlantFoodEffect(m.getOrDefault("plantFoodEffect", ""));
        // فاصله تولید خورشید (ثانیه)
        double interval = parseDouble(m.get("actionIntervalSeconds"), 24.0);
        s.setActionIntervalSeconds(interval);
        // رشته‌های ارتقا
        s.setLvl2Upgrade(m.getOrDefault("lvl2Upgrade", ""));
        s.setLvl3Upgrade(m.getOrDefault("lvl3Upgrade", ""));
        s.setLvl4Upgrade(m.getOrDefault("lvl4Upgrade", ""));
        return s;
    }

    private PlantFamily parseFamily(String raw) {
        try {
            return PlantFamily.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PlantFamily.SHOOTER;
        }
    }

    private List<PlantTag> parseTags(String raw) {
        List<PlantTag> tags = new ArrayList<>();
        if (raw == null || raw.equals("[]")) {
            return tags;
        }
        String inner = raw.replace("[", "").replace("]", "");
        for (String part : inner.split(",")) {
            String t = part.trim().replace("\"", "");
            if (t.isEmpty()) {
                continue;
            }
            try {
                tags.add(PlantTag.valueOf(t));
            } catch (IllegalArgumentException ignored) { }
        }
        return tags;
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

    private void loadMinigameExtras() {
        reg(PlantType.WALLNUT_BOWLING, PlantFamily.WALL_NUT, 4000, 0, 0,
                180, "MinigameOnly", "Bowling walnut.",
                PlantTag.DAY);
        reg(PlantType.EXPLODE_O_NUT_BOWLING, PlantFamily.EXPLOSIVE,
                300, 0, 0, 180, "MinigameOnly", "Explodes in 3x3.",
                PlantTag.DAY, PlantTag.EXPLOSIVE);
        reg(PlantType.BIG_WALLNUT, PlantFamily.WALL_NUT, 8000, 0, 0,
                200, "MinigameOnly", "Crushes all in path.",
                PlantTag.DAY);
    }

    private void reg(PlantType t, PlantFamily f, int hp, int cost,
                     double rch, int dmg, String cat, String desc,
                     PlantTag... tagsArr) {
        if (statsMap.containsKey(t)) {
            return;
        }
        PlantStats s = new PlantStats(t, f, hp, cost, rch, dmg, 0, 9, cat, desc);
        List<PlantTag> tl = new ArrayList<>(Arrays.asList(tagsArr));
        s.setTags(tl);
        statsMap.put(t, s);
    }

    private void loadBuiltinDefaults() {
        reg(PlantType.SUNFLOWER, PlantFamily.SUN_PRODUCER, 300, 50, 7.5, 0,
                "SunProducer", "Sunflower produces 25 sun per cycle.",
                PlantTag.DAY, PlantTag.SUN);
        reg(PlantType.TWIN_SUNFLOWER, PlantFamily.SUN_PRODUCER, 300, 125, 7.5, 0,
                "SunProducer", "Twin Sunflower produces 50 sun.",
                PlantTag.DAY, PlantTag.SUN);
        reg(PlantType.SUN_SHROOM, PlantFamily.SUN_PRODUCER, 300, 25, 7.5, 0,
                "SunProducer", "Sun Shroom grows stronger over time.",
                PlantTag.NIGHT, PlantTag.SHROOM, PlantTag.SUN);
        reg(PlantType.PEASHOOTER, PlantFamily.SHOOTER, 300, 100, 7.5, 20,
                "Shooter", "Fires peas at zombies.",
                PlantTag.DAY, PlantTag.PEA);
        reg(PlantType.SNOW_PEA, PlantFamily.SHOOTER, 300, 175, 7.5, 20,
                "Shooter", "Ice peas chill zombies.",
                PlantTag.DAY, PlantTag.PEA, PlantTag.ICE);
        reg(PlantType.WALL_NUT, PlantFamily.WALL_NUT, 4000, 50, 30, 0,
                "WallNut", "Blocks zombie progress.",
                PlantTag.DAY, PlantTag.STACK);
        reg(PlantType.TALL_NUT, PlantFamily.WALL_NUT, 8000, 125, 30, 0,
                "WallNut", "Cannot be vaulted.",
                PlantTag.DAY, PlantTag.STACK);
        reg(PlantType.PUMPKIN, PlantFamily.WALL_NUT, 4000, 125, 30, 0,
                "WallNut", "Protects plant inside.",
                PlantTag.DAY, PlantTag.STACK);
        reg(PlantType.CHERRY_BOMB, PlantFamily.EXPLOSIVE, 300, 150, 50, 1800,
                "Explosive", "3x3 instant explosion.",
                PlantTag.DAY, PlantTag.EXPLOSIVE, PlantTag.AOE);
        reg(PlantType.POTATO_MINE, PlantFamily.EXPLOSIVE, 300, 25, 30, 180,
                "Explosive", "Arms after 14s, kills on step.",
                PlantTag.DAY, PlantTag.TRAP, PlantTag.EXPLOSIVE);
        reg(PlantType.JALAPENO, PlantFamily.EXPLOSIVE, 300, 125, 50, 1800,
                "Explosive", "Burns entire row.",
                PlantTag.DAY, PlantTag.FIRE, PlantTag.EXPLOSIVE, PlantTag.AOE);
        reg(PlantType.CABBAGE_PULT, PlantFamily.LOBBER, 300, 100, 7.5, 40,
                "Lobber", "Lobs cabbages over obstacles.",
                PlantTag.DAY);
        reg(PlantType.MELON_PULT, PlantFamily.LOBBER, 300, 300, 7.5, 80,
                "Lobber", "Heavy AoE melon lob.",
                PlantTag.DAY, PlantTag.AOE);
        reg(PlantType.WINTER_MELON, PlantFamily.LOBBER, 300, 200, 7.5, 80,
                "Lobber", "AoE+chill icy melon.",
                PlantTag.DAY, PlantTag.ICE, PlantTag.AOE);
        reg(PlantType.CHOMPER, PlantFamily.MELEE, 300, 150, 7.5, 9999,
                "MeleeAttacker", "Swallows zombie whole.",
                PlantTag.DAY);
        reg(PlantType.BONK_CHOY, PlantFamily.MELEE, 300, 125, 7.5, 40,
                "MeleeAttacker", "Punches front and back.",
                PlantTag.DAY);
        reg(PlantType.GARLIC, PlantFamily.MODIFIER, 600, 50, 30, 0,
                "Modifier", "Forces zombies to change lanes.",
                PlantTag.DAY, PlantTag.MOVE_ZOMBIES);
        reg(PlantType.SWEET_POTATO, PlantFamily.MODIFIER, 800, 125, 30, 0,
                "Modifier", "Attracts zombies from adjacent lanes.",
                PlantTag.DAY, PlantTag.MOVE_ZOMBIES);
        reg(PlantType.TORCHWOOD, PlantFamily.MODIFIER, 500, 175, 5, 0,
                "Modifier", "Converts peas to fireballs.",
                PlantTag.DAY, PlantTag.PEA, PlantTag.FIRE);
        reg(PlantType.MAGNET_SHROOM, PlantFamily.MODIFIER, 300, 100, 7.5, 0,
                "Modifier", "Removes metal armor.",
                PlantTag.NIGHT, PlantTag.SHROOM);
        reg(PlantType.HYPNO_SHROOM, PlantFamily.MODIFIER, 300, 75, 30, 0,
                "Modifier", "Hypnotizes zombies.",
                PlantTag.NIGHT, PlantTag.SHROOM, PlantTag.MAGIC);
        reg(PlantType.LILY_PAD, PlantFamily.MODIFIER, 400, 25, 7.5, 0,
                "WaterPlant", "Floats on water.",
                PlantTag.WATER, PlantTag.STACK);
        reg(PlantType.PUFF_SHROOM, PlantFamily.MODIFIER, 100, 0, 7.5, 20,
                "Shooter", "Free but temporary.",
                PlantTag.NIGHT, PlantTag.SHROOM);
        reg(PlantType.FUME_SHROOM, PlantFamily.MODIFIER, 300, 75, 7.5, 20,
                "StrikeThrough", "Fumes pass through obstacles.",
                PlantTag.NIGHT, PlantTag.SHROOM);
        reg(PlantType.ICE_SHROOM, PlantFamily.MODIFIER, 300, 75, 30, 0,
                "Modifier", "Freezes all zombies.",
                PlantTag.NIGHT, PlantTag.SHROOM, PlantTag.ICE);
        reg(PlantType.GRAVE_BUSTER, PlantFamily.MODIFIER, 300, 75, 7.5, 0,
                "Modifier", "Removes tombstones.",
                PlantTag.DAY);
        loadMinigameExtras();
    }
}
