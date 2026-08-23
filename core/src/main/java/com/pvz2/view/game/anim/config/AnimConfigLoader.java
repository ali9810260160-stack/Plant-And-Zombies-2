package com.pvz2.view.game.anim.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.pvz2.model.enums.PlantType;
import com.pvz2.model.enums.ZombieType;

import java.util.EnumMap;
import java.util.Map;

/**
 * بارگذاری و پارس فایل character_animations.json.
 *
 * ساختار JSON:
 * <pre>
 * {
 *   "version": "1.0",
 *   "plants":  { "SUNFLOWER": {...}, "PEASHOOTER": {...}, ... },
 *   "zombies": { "NORMAL": {...}, "CONEHEAD": {...}, ... }
 * }
 * </pre>
 *
 * استفاده:
 * <pre>
 *   AnimConfigLoader loader = new AnimConfigLoader("data/character_animations.json");
 *
 *   PlantAnimConfig  pCfg = loader.getPlantConfig(PlantType.PEASHOOTER);
 *   ZombieAnimConfig zCfg = loader.getZombieConfig(ZombieType.CONEHEAD);
 * </pre>
 *
 * نکته:
 *   از JsonReader استفاده می‌شود (نه Json serializer) تا کنترل کامل روی
 *   پارس داشته باشیم و با تمام نسخه‌های libGDX سازگار باشد.
 */
public class AnimConfigLoader {

    private static final String TAG = "AnimConfigLoader";

    private final Map<PlantType,  PlantAnimConfig>  plantConfigs  = new EnumMap<>(PlantType.class);
    private final Map<ZombieType, ZombieAnimConfig> zombieConfigs = new EnumMap<>(ZombieType.class);
    /**
     * ذخیره تمام entry های زامبی با کلید رشته‌ای خام JSON (شامل
     * کلیدهای ترکیبی مثل "NORMAL__ANCIENT_EGYPT" که در enum ZombieType
     * قابل parse نیستند). resolveZombieConfig از این map استفاده می‌کند.
     */
    private final Map<String, ZombieAnimConfig> zombieConfigsByRawKey = new java.util.LinkedHashMap<>();

    // ─── محیطی (ویژگی جدید نسخه ۲) ──────────────────────────────
    private final Map<String, EnvironmentAnimConfig> gravestoneConfigs = new java.util.LinkedHashMap<>();
    private final Map<String, EnvironmentAnimConfig> mowerConfigs      = new java.util.LinkedHashMap<>();
    private final Map<String, EnvironmentAnimConfig> miscConfigs       = new java.util.LinkedHashMap<>();

    private boolean loaded = false;

    /** بارگذاری فقط character_animations.json (بدون داده محیطی). */
    public AnimConfigLoader(String jsonPath) {
        this(jsonPath, null);
    }

    /**
     * بارگذاری کامل character_animations.json + environment_animations.json
     * (سنگ‌قبر/مورر/گردباد/مه/بلوک یخ — استفاده‌شده توسط GridRenderer).
     * اگر environmentJsonPath خالی/null باشد یا فایل پیدا نشود، فقط
     * character را بارگذاری می‌کند (بدون کرش — GridRenderer به fallback
     * رویه‌ای رنگی برمی‌گردد).
     */
    public AnimConfigLoader(String jsonPath, String environmentJsonPath) {
        try {
            String raw = Gdx.files.internal(jsonPath).readString("UTF-8");
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(raw);
            parsePlants(root.get("plants"));
            parseZombies(root.get("zombies"));
            loaded = true;
            Gdx.app.log(TAG, "بارگذاری موفق: "
                + plantConfigs.size() + " گیاه، "
                + zombieConfigsByRawKey.size() + " ورودی زامبی");
        } catch (Exception e) {
            Gdx.app.error(TAG, "خطا در بارگذاری " + jsonPath + ": " + e.getMessage());
        }

        if (environmentJsonPath != null && !environmentJsonPath.isEmpty()) {
            loadEnvironment(environmentJsonPath);
        }
    }

    private void loadEnvironment(String path) {
        try {
            String raw = Gdx.files.internal(path).readString("UTF-8");
            JsonValue root = new JsonReader().parse(raw);
            parseEnvGroup(root.get("gravestones"), gravestoneConfigs);
            parseEnvGroup(root.get("mowers"), mowerConfigs);
            parseEnvGroup(root.get("misc"), miscConfigs);
            Gdx.app.log(TAG, "environment_animations.json بارگذاری شد: "
                + gravestoneConfigs.size() + " سنگ‌قبر، " + mowerConfigs.size() + " مورر، "
                + miscConfigs.size() + " مورد دیگر");
        } catch (Exception e) {
            Gdx.app.log(TAG, "environment_animations.json بارگذاری نشد (اختیاری، fallback رویه‌ای فعال است): "
                + e.getMessage());
        }
    }

    private void parseEnvGroup(JsonValue groupNode, Map<String, EnvironmentAnimConfig> target) {
        if (groupNode == null) return;
        for (JsonValue e = groupNode.child; e != null; e = e.next) {
            EnvironmentAnimConfig cfg = new EnvironmentAnimConfig();
            cfg.pamPath = e.getString("pamPath", "");
            JsonValue clipsNode = e.get("clips");
            if (clipsNode != null && clipsNode.isArray()) {
                for (JsonValue c = clipsNode.child; c != null; c = c.next) {
                    cfg.clips.add(c.asString());
                }
            }
            target.put(e.name, cfg);
        }
    }

    // ════════════════════════════════════════════════════════════
    //  پارس گیاهان
    // ════════════════════════════════════════════════════════════

    private void parsePlants(JsonValue plantsNode) {
        if (plantsNode == null) return;
        for (JsonValue entry = plantsNode.child; entry != null; entry = entry.next) {
            try {
                PlantType type = PlantType.valueOf(entry.name);
                plantConfigs.put(type, parsePlantConfig(entry.name, entry));
            } catch (IllegalArgumentException e) {
                Gdx.app.error(TAG, "PlantType ناشناخته در JSON: '" + entry.name + "'");
            }
        }
    }

    private PlantAnimConfig parsePlantConfig(String typeName, JsonValue node) {
        PlantAnimConfig cfg = new PlantAnimConfig();
        cfg.plantType = typeName;
        cfg.pamPath   = node.getString("pamPath", "");
        cfg.scaleX    = node.getFloat("scaleX", 1.0f);
        cfg.scaleY    = node.getFloat("scaleY", 1.0f);
        cfg.offsetX   = node.getFloat("offsetX", 0f);
        cfg.offsetY   = node.getFloat("offsetY", 0f);

        // States
        JsonValue statesNode = node.get("states");
        if (statesNode != null) {
            for (JsonValue s = statesNode.child; s != null; s = s.next) {
                cfg.states.put(s.name, parseStateConfig(s));
            }
        }

        // Projectiles
        JsonValue projNode = node.get("projectiles");
        if (projNode != null && projNode.isArray()) {
            for (JsonValue p = projNode.child; p != null; p = p.next) {
                cfg.projectiles.add(parseProjectileConfig(p));
            }
        }

        // Damage Variants (ویژگی جدید — مثل Wall-nut: damage/damage2/damage3)
        JsonValue dmgNode = node.get("damageVariants");
        if (dmgNode != null && dmgNode.isArray()) {
            for (JsonValue d = dmgNode.child; d != null; d = d.next) {
                cfg.damageVariants.add(d.asString());
            }
        }

        return cfg;
    }

    // ════════════════════════════════════════════════════════════
    //  پارس زامبی‌ها
    // ════════════════════════════════════════════════════════════

    private void parseZombies(JsonValue zombiesNode) {
        if (zombiesNode == null) return;
        for (JsonValue entry = zombiesNode.child; entry != null; entry = entry.next) {
            ZombieAnimConfig cfg = parseZombieConfig(entry.name, entry);

            // همیشه با کلید رشته‌ای خام ذخیره کن (پشتیبانی از کلیدهای
            // ترکیبی مثل "NORMAL__ANCIENT_EGYPT" که در enum قابل parse نیستند)
            zombieConfigsByRawKey.put(entry.name, cfg);

            // اگر کلید ساده (بدون __) و مطابق یک ZombieType واقعی بود،
            // در enum map هم برای دسترسی سریع/سازگاری با کد قدیمی ذخیره کن
            if (!entry.name.contains("__")) {
                try {
                    zombieConfigs.put(ZombieType.valueOf(entry.name), cfg);
                } catch (IllegalArgumentException e) {
                    Gdx.app.error(TAG, "ZombieType ناشناخته در JSON: '" + entry.name + "'");
                }
            }
        }
    }

    private ZombieAnimConfig parseZombieConfig(String typeName, JsonValue node) {
        ZombieAnimConfig cfg = new ZombieAnimConfig();
        cfg.zombieType = typeName;
        cfg.pamPath    = node.getString("pamPath", "");
        cfg.baseZombieType = node.getString("baseZombieType", null);
        cfg.forcedArmor    = node.getString("forcedArmor", null);
        cfg.scaleX     = node.getFloat("scaleX", 1.0f);
        cfg.scaleY     = node.getFloat("scaleY", 1.0f);
        cfg.offsetX    = node.getFloat("offsetX", 0f);
        cfg.offsetY    = node.getFloat("offsetY", 0f);

        // States
        JsonValue statesNode = node.get("states");
        if (statesNode != null) {
            for (JsonValue s = statesNode.child; s != null; s = s.next) {
                cfg.states.put(s.name, parseStateConfig(s));
            }
        }

        // Health Variants
        JsonValue hvNode = node.get("healthVariants");
        if (hvNode != null && hvNode.isArray()) {
            for (JsonValue h = hvNode.child; h != null; h = h.next) {
                HealthVariantConfig hv = new HealthVariantConfig();
                hv.id         = h.getString("id", "NORMAL");
                hv.minHpRatio = h.getFloat("minHpRatio", 0.0f);
                hv.maxHpRatio = h.getFloat("maxHpRatio", 1.0f);
                hv.clipSuffix = h.getString("clipSuffix", "");
                cfg.healthVariants.add(hv);
            }
        }

        // Armors
        JsonValue armorsNode = node.get("armors");
        if (armorsNode != null) {
            for (JsonValue a = armorsNode.child; a != null; a = a.next) {
                ArmorAnimConfig armor = new ArmorAnimConfig();
                JsonValue vmNode = a.get("visMap");
                if (vmNode != null) {
                    for (JsonValue vm = vmNode.child; vm != null; vm = vm.next) {
                        armor.visMap.put(vm.name, vm.asBoolean());
                    }
                }
                armor.clipOverride = a.getString("clipOverride", null);
                armor.clipSuffixPresent = a.getString("clipSuffixPresent", "");
                armor.clipSuffixBroken  = a.getString("clipSuffixBroken", "");
                armor.breakEventClip    = a.getString("breakEventClip", null);
                cfg.armors.put(a.name, armor);
            }
        }

        // Special Abilities
        JsonValue saNode = node.get("specialAbilities");
        if (saNode != null && saNode.isArray()) {
            for (JsonValue sa = saNode.child; sa != null; sa = sa.next) {
                SpecialAbilityConfig sac = new SpecialAbilityConfig();
                sac.id           = sa.getString("id", "");
                sac.trigger      = sa.getString("trigger", "HP_BELOW");
                sac.triggerParam = sa.getFloat("triggerParam", 0.5f);
                sac.once         = sa.getBoolean("once", true);
                sac.animState    = sa.getString("animState", "SPECIAL");
                sac.clip         = sa.getString("clip", null);
                sac.enterClip    = sa.getString("enterClip", null);
                sac.exitClip     = sa.getString("exitClip", null);
                sac.action       = sa.getString("action", "");
                JsonValue apNode = sa.get("actionParams");
                if (apNode != null) {
                    for (JsonValue ap = apNode.child; ap != null; ap = ap.next) {
                        sac.actionParams.put(ap.name, ap.asString());
                    }
                }
                cfg.specialAbilities.add(sac);
            }
        }

        return cfg;
    }

    // ════════════════════════════════════════════════════════════
    //  پارس مشترک
    // ════════════════════════════════════════════════════════════

    private StateConfig parseStateConfig(JsonValue node) {
        StateConfig sc = new StateConfig();
        sc.clip      = node.getString("clip", "");
        sc.loop      = node.getBoolean("loop", true);
        sc.returnTo  = node.getString("returnTo", null);
        sc.speedMul  = node.getFloat("speedMul", 1.0f);
        sc.tintColor = node.getString("tintColor", null);
        sc.tintAlpha = node.getFloat("tintAlpha", 0.4f);
        return sc;
    }

    private ProjectileAnimConfig parseProjectileConfig(JsonValue node) {
        ProjectileAnimConfig pc = new ProjectileAnimConfig();
        pc.id            = node.getString("id", "");
        pc.pamPath       = node.getString("pamPath", null);
        pc.clip          = node.getString("clip", null);
        pc.texturePath   = node.getString("texturePath", null);
        pc.regionName    = node.getString("regionName", null);
        pc.scale         = node.getFloat("scale", 1.0f);
        pc.isArc         = node.getBoolean("isArc", false);
        pc.arcHeight     = node.getFloat("arcHeight", 150f);
        pc.tintColor     = node.getString("tintColor", null);
        pc.rotates       = node.getBoolean("rotates", false);
        pc.rotationSpeed = node.getFloat("rotationSpeed", 180f);
        return pc;
    }

    // ════════════════════════════════════════════════════════════
    //  Public API
    // ════════════════════════════════════════════════════════════

    /** دریافت config گیاه (null اگر تعریف نشده باشد) */
    public PlantAnimConfig getPlantConfig(PlantType type) {
        return plantConfigs.get(type);
    }

    /**
     * دریافت config زامبی (روش قدیمی — بدون آگاهی از فصل).
     * برای زامبی‌های per-chapter (NORMAL/GARGANTUAR/IMP) این ممکن است
     * پیدا نکند چون کلید آن‌ها ترکیبی است (مثلاً "NORMAL__ANCIENT_EGYPT").
     * برای این موارد از resolveZombieConfig(type, chapter) استفاده کنید.
     */
    public ZombieAnimConfig getZombieConfig(ZombieType type) {
        return zombieConfigs.get(type);
    }

    /**
     * ═══ متد جدید نسخه ۲ — resolve کامل با آگاهی از فصل ═══
     *
     * برای زامبی‌های عمومی (NORMAL/GARGANTUAR/IMP) که در هر فصل ظاهر
     * متفاوتی دارند، و زامبی‌های «مجازی» (CONEHEAD/BUCKETHEAD/KNIGHT/
     * BLOCKHEAD که از NORMAL همان فصل + یک armor مشتق می‌شوند)، این متد
     * باید به‌جای getZombieConfig ساده استفاده شود.
     *
     * الگوریتم:
     *   ۱. ابتدا کلید ترکیبی "{type}__{chapter}" را در JSON جستجو می‌کند.
     *   ۲. اگر نبود، به کلید ساده "{type}" برمی‌گردد (زامبی‌های اختصاصی
     *      یک فصل که در JSON کلید ساده دارند، یا زامبی‌هایی که اصلاً
     *      per-chapter تعریف نشده‌اند).
     *   ۳. اگر entry پیداشده «مجازی» بود (baseZombieType != null)،
     *      به‌صورت بازگشتی base را resolve کرده و یک کپی flatten‌شده
     *      (با pamPath واقعی + forcedArmor) برمی‌گرداند.
     *
     * نتیجه در یک کش داخلی نگه‌داشته می‌شود.
     */
    public ZombieAnimConfig resolveZombieConfig(ZombieType type, com.pvz2.model.enums.ChapterType chapter) {
        String cacheKey = type.name() + "__" + (chapter != null ? chapter.name() : "NONE");
        ZombieAnimConfig cached = resolvedZombieCache.get(cacheKey);
        if (cached != null) return cached;

        ZombieAnimConfig resolved = resolveZombieInternal(type, chapter, new java.util.HashSet<>());
        if (resolved == null) {
            // fallback نهایی: کلید ساده بدون فصل (سازگاری با JSON های قدیمی)
            resolved = zombieConfigs.get(type);
        }
        if (resolved != null) {
            resolvedZombieCache.put(cacheKey, resolved);
        } else {
            Gdx.app.error(TAG, "config زامبی یافت نشد: " + type.name()
                + (chapter != null ? " @ " + chapter.name() : ""));
        }
        return resolved;
    }

    private final Map<String, ZombieAnimConfig> resolvedZombieCache = new java.util.HashMap<>();

    /** جستجوی raw config با کلید ترکیبی رشته‌ای (برای resolve بازگشتی baseZombieType). */
    private ZombieAnimConfig getRawByStringKey(String rawKey) {
        // اگر رشته شامل __ است، سعی کن به‌عنوان "TYPE__CHAPTER" پارس کنی
        String[] parts = rawKey.split("__");
        if (parts.length == 2) {
            try {
                ZombieType t = ZombieType.valueOf(parts[0]);
                // زامبی‌های per-chapter در zombieConfigsRaw با کلید رشته‌ای کامل ذخیره شده‌اند
                ZombieAnimConfig direct = zombieConfigsByRawKey.get(rawKey);
                if (direct != null) return direct;
                return zombieConfigs.get(t); // fallback به نسخه بدون فصل
            } catch (IllegalArgumentException ignored) { }
        }
        try {
            return zombieConfigs.get(ZombieType.valueOf(rawKey));
        } catch (IllegalArgumentException e) {
            return zombieConfigsByRawKey.get(rawKey);
        }
    }

    private ZombieAnimConfig resolveZombieInternal(ZombieType type, com.pvz2.model.enums.ChapterType chapter,
                                                    java.util.Set<String> visited) {
        String comboKey = type.name() + (chapter != null ? "__" + chapter.name() : "");

        ZombieAnimConfig raw = zombieConfigsByRawKey.get(comboKey);
        if (raw == null) raw = zombieConfigs.get(type); // fallback: کلید ساده
        if (raw == null) return null;

        if (!raw.isVirtual()) return raw;

        String guardKey = comboKey + "->" + raw.baseZombieType;
        if (visited.contains(guardKey)) {
            Gdx.app.error(TAG, "حلقه ارجاع baseZombieType شناسایی شد: " + guardKey);
            return null;
        }
        visited.add(guardKey);

        ZombieAnimConfig baseCfg = getRawByStringKey(raw.baseZombieType);
        // اگر base خودش هم virtual بود (نادر)، بازگشتی resolve کن
        if (baseCfg != null && baseCfg.isVirtual()) {
            String[] bparts = raw.baseZombieType.split("__");
            try {
                ZombieType bt = ZombieType.valueOf(bparts[0]);
                com.pvz2.model.enums.ChapterType bc = bparts.length == 2
                    ? com.pvz2.model.enums.ChapterType.valueOf(bparts[1]) : chapter;
                baseCfg = resolveZombieInternal(bt, bc, visited);
            } catch (Exception ignored) { }
        }
        if (baseCfg == null) return null;

        ZombieAnimConfig flat = shallowCopyZombie(baseCfg);
        flat.zombieType = type.name();
        if (raw.forcedArmor != null) flat.forcedArmor = raw.forcedArmor;
        return flat;
    }

    private ZombieAnimConfig shallowCopyZombie(ZombieAnimConfig src) {
        ZombieAnimConfig c = new ZombieAnimConfig();
        c.zombieType        = src.zombieType;
        c.pamPath            = src.pamPath;
        c.states              = src.states;
        c.healthVariants       = src.healthVariants;
        c.armors                 = src.armors;
        c.specialAbilities        = src.specialAbilities;
        c.scaleX = src.scaleX;
        c.scaleY = src.scaleY;
        c.offsetX = src.offsetX;
        c.offsetY = src.offsetY;
        c.forcedArmor = src.forcedArmor;
        return c;
    }

    public boolean hasPlantConfig(PlantType type) {
        return plantConfigs.containsKey(type);
    }

    public boolean hasZombieConfig(ZombieType type) {
        return zombieConfigs.containsKey(type);
    }

    public boolean isLoaded() {
        return loaded;
    }

    /** تعداد گیاهان بارگذاری‌شده */
    public int getPlantCount() {
        return plantConfigs.size();
    }

    /** تعداد زامبی‌های بارگذاری‌شده (شامل ورودی‌های per-chapter و مجازی) */
    public int getZombieCount() {
        return zombieConfigsByRawKey.size();
    }

    // ════════════════════════════════════════════════════════════
    //  Public API — محیطی (ویژگی جدید نسخه ۲، برای GridRenderer)
    // ════════════════════════════════════════════════════════════

    /** کلید: "ANCIENT_EGYPT" یا "DARK_AGES_NORMAL"/"DARK_AGES_PLANTFOOD"/"DARK_AGES_SUN" */
    public EnvironmentAnimConfig getGravestoneConfig(String key) {
        return gravestoneConfigs.get(key);
    }

    /** کلید: نام ChapterType (مثلاً "ANCIENT_EGYPT") */
    public EnvironmentAnimConfig getMowerConfig(String chapterName) {
        return mowerConfigs.get(chapterName);
    }

    /** کلید: "SANDSTORM_REAR"، "SANDSTORM_TOP"، "TOMBTANGLER_FOG"، ... */
    public EnvironmentAnimConfig getMiscConfig(String key) {
        return miscConfigs.get(key);
    }
}
