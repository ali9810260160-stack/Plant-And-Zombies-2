package repository;

import model.enums.PlantFamily;
import model.enums.PlantTag;
import util.FileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * بارگذاری و مدیریت داده‌های ثابت گیاهان از فایل data/plants/plants.json.
 *
 * <p><b>سیستم Data-Driven:</b> برای اضافه کردن گیاه جدید به بازی،
 * کافی است یک شیء جدید به آرایه "plants" در فایل plants.json اضافه شود.
 * هیچ تغییری در کد Java لازم نیست.</p>
 *
 * <p>فرمت هر گیاه در JSON:</p>
 * <pre>
 * {
 *   "id": 1,
 *   "name": "Sunflower",
 *   "internalName": "sunflower",
 *   "family": "Sun Producer",
 *   "tags": ["Day"],
 *   "sunCost": 50,
 *   "baseHp": 300,
 *   "damage": "0",
 *   "baseAbility": "...",
 *   "plantFoodEffect": "...",
 *   "upgrades": { "level2": "...", "level3": "...", "level4": "..." },
 *   "actionIntervalSeconds": 24.0,
 *   "rechargeSeconds": 5.0,
 *   "isUnlocked": false,
 *   "isBonus": false,
 *   "currentLevel": 1,
 *   "seedPacketsOwned": 0
 * }
 * </pre>
 */
public class PlantDataRepository {

    /** مسیر فایل داده گیاهان */
    private static final String PLANTS_FILE = "data/plants/plants.json";

    /** کش داده‌های گیاهان (بارگذاری یک‌بار در اجرا) */
    private List<PlantStats> cachedPlants;

    /** Map از internalName به PlantStats برای دسترسی سریع */
    private Map<String, PlantStats> plantsByName;

    // ---- بارگذاری ----

    /**
     * تمام داده‌های گیاهان را از فایل JSON می‌خواند.
     * اولین فراخوانی فایل را می‌خواند و نتیجه را کش می‌کند.
     *
     * @return لیست تمام گیاهان
     * @throws RuntimeException اگر فایل وجود نداشته باشد یا فرمت آن اشتباه باشد
     */
    public List<PlantStats> loadAll() {
        if (cachedPlants != null) {
            return cachedPlants;
        }
        try {
            String json = FileUtil.readFile(PLANTS_FILE);
            if (json == null) {
                throw new RuntimeException("plants.json not found at: " + PLANTS_FILE);
            }
            cachedPlants = parsePlantsJson(json);
            buildNameIndex();
            return cachedPlants;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load plants.json", e);
        }
    }

    /**
     * داده‌های یک گیاه خاص را با internalName پیدا می‌کند.
     *
     * @param internalName نام داخلی گیاه (مثل "sunflower" یا "snow_pea")
     * @return PlantStats یا null اگر پیدا نشد
     */
    public PlantStats getByName(String internalName) {
        if (plantsByName == null) {
            loadAll();
        }
        if (internalName == null) {
            return null;
        }
        return plantsByName.get(normalizeKey(internalName));
    }

    /**
     * داده‌های یک گیاه را با شماره ID پیدا می‌کند.
     *
     * @param id شماره ID گیاه (از فایل JSON)
     * @return PlantStats یا null
     */
    public PlantStats getById(int id) {
        for (PlantStats p : loadAll()) {
            if (p.id == id) {
                return p;
            }
        }
        return null;
    }

    /**
     * لیست تمام گیاهان یک خانواده را برمی‌گرداند.
     *
     * @param family خانواده (مثل "Shooter" یا "Sun Producer")
     * @return لیست گیاهان آن خانواده
     */
    public List<PlantStats> getByFamily(String family) {
        List<PlantStats> result = new ArrayList<>();
        for (PlantStats p : loadAll()) {
            if (p.family.equalsIgnoreCase(family)) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * لیست تمام گیاهانی که تگ مشخصی دارند برمی‌گرداند.
     *
     * @param tag تگ مورد نظر (مثل "Pea" یا "Fire")
     * @return لیست گیاهان با آن تگ
     */
    public List<PlantStats> getByTag(String tag) {
        List<PlantStats> result = new ArrayList<>();
        for (PlantStats p : loadAll()) {
            for (String t : p.tags) {
                if (t.equalsIgnoreCase(tag)) {
                    result.add(p);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * بررسی می‌کند آیا گیاهی با این internalName در داده‌ها وجود دارد.
     *
     * @param internalName نام داخلی
     * @return true اگر وجود داشته باشد
     */
    public boolean exists(String internalName) {
        return getByName(internalName) != null;
    }

    /**
     * کش را پاک می‌کند تا فایل دوباره از دیسک خوانده شود.
     * برای hot-reload در زمان توسعه مفید است.
     */
    public void invalidateCache() {
        cachedPlants = null;
        plantsByName = null;
    }

    // ---- پارسر JSON دستی ----
    // از کتابخانه خارجی استفاده نمی‌شود تا dependency اضافه نشود.

    /**
     * رشته JSON را به لیست PlantStats تبدیل می‌کند.
     * پارسر دستی ساده بر اساس ساختار شناخته‌شده فایل.
     *
     * @param json محتوای فایل JSON
     * @return لیست گیاهان پارس‌شده
     */
    private List<PlantStats> parsePlantsJson(String json) {
        List<PlantStats> plants = new ArrayList<>();
        // استخراج آرایه plants
        int arrStart = json.indexOf("\"plants\"");
        if (arrStart == -1) {
            throw new RuntimeException("Key 'plants' not found in plants.json");
        }
        arrStart = json.indexOf('[', arrStart);
        int arrEnd = findMatchingBracket(json, arrStart, '[', ']');
        String plantsArray = json.substring(arrStart + 1, arrEnd);

        // هر شیء گیاه را جدا می‌کنیم
        List<String> objects = splitJsonObjects(plantsArray);
        for (String obj : objects) {
            PlantStats stats = parsePlantObject(obj);
            if (stats != null) {
                plants.add(stats);
            }
        }
        return plants;
    }

    /**
     * یک شیء JSON گیاه را به PlantStats تبدیل می‌کند.
     *
     * @param obj رشته JSON یک گیاه
     * @return PlantStats پارس‌شده
     */
    private PlantStats parsePlantObject(String obj) {
        try {
            PlantStats stats = new PlantStats();
            stats.id = parseIntField(obj, "id");
            stats.name = parseStringField(obj, "name");
            stats.internalName = parseStringField(obj, "internalName");
            stats.family = parseStringField(obj, "family");
            stats.tags = parseStringArray(obj, "tags");
            stats.sunCost = parseIntField(obj, "sunCost");
            stats.baseHp = parseIntField(obj, "baseHp");
            stats.damage = parseStringField(obj, "damage");
            stats.baseAbility = parseStringField(obj, "baseAbility");
            stats.plantFoodEffect = parseStringField(obj, "plantFoodEffect");
            stats.actionIntervalSeconds = parseDoubleField(obj, "actionIntervalSeconds");
            stats.rechargeSeconds = parseDoubleField(obj, "rechargeSeconds");
            stats.isBonus = parseBoolField(obj, "isBonus");

            // upgrades sub-object
            int upgStart = obj.indexOf("\"upgrades\"");
            if (upgStart != -1) {
                int brace = obj.indexOf('{', upgStart);
                int braceEnd = findMatchingBracket(obj, brace, '{', '}');
                String upgradesObj = obj.substring(brace, braceEnd + 1);
                stats.upgradeLevel2 = parseStringField(upgradesObj, "level2");
                stats.upgradeLevel3 = parseStringField(upgradesObj, "level3");
                stats.upgradeLevel4 = parseStringField(upgradesObj, "level4");
            }
            return stats;
        } catch (Exception e) {
            System.err.println("[PlantDataRepository] Failed to parse plant object: " + e.getMessage());
            return null;
        }
    }

    // ---- ایندکس‌سازی ----

    /** Map internalName → PlantStats را می‌سازد */
    private void buildNameIndex() {
        plantsByName = new LinkedHashMap<>();
        for (PlantStats p : cachedPlants) {
            if (p.internalName != null) {
                plantsByName.put(normalizeKey(p.internalName), p);
            }
            if (p.name != null) {
                plantsByName.put(normalizeKey(p.name), p);
            }
        }
    }

    /** نام را نرمال می‌کند: lowercase، فاصله و خط‌تیره → underscore */
    private String normalizeKey(String name) {
        return name.toLowerCase().replace(' ', '_').replace('-', '_');
    }

    // ---- ابزارهای پارس JSON ----

    private int parseIntField(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return 0;
        idx = json.indexOf(':', idx) + 1;
        StringBuilder sb = new StringBuilder();
        while (idx < json.length()) {
            char c = json.charAt(idx);
            if (Character.isDigit(c) || c == '-') {
                sb.append(c);
            } else if (sb.length() > 0) {
                break;
            }
            idx++;
        }
        if (sb.length() == 0) return 0;
        try {
            return Integer.parseInt(sb.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseDoubleField(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return -1.0;
        idx = json.indexOf(':', idx) + 1;
        StringBuilder sb = new StringBuilder();
        while (idx < json.length()) {
            char c = json.charAt(idx);
            if (Character.isDigit(c) || c == '-' || c == '.') {
                sb.append(c);
            } else if (sb.length() > 0) {
                break;
            }
            idx++;
        }
        if (sb.length() == 0) return -1.0;
        try {
            return Double.parseDouble(sb.toString());
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    private String parseStringField(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return "";
        idx = json.indexOf(':', idx) + 1;
        while (idx < json.length() && json.charAt(idx) != '"') {
            if (json.charAt(idx) == 'n') return null; // null value
            idx++;
        }
        if (idx >= json.length()) return "";
        idx++; // skip opening quote
        StringBuilder sb = new StringBuilder();
        while (idx < json.length() && json.charAt(idx) != '"') {
            if (json.charAt(idx) == '\\' && idx + 1 < json.length()) {
                idx++;
                char escaped = json.charAt(idx);
                switch (escaped) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    default: sb.append(escaped);
                }
            } else {
                sb.append(json.charAt(idx));
            }
            idx++;
        }
        return sb.toString();
    }

    private boolean parseBoolField(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return false;
        idx = json.indexOf(':', idx) + 1;
        while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) idx++;
        return json.startsWith("true", idx);
    }

    private List<String> parseStringArray(String json, String key) {
        List<String> result = new ArrayList<>();
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return result;
        int arrStart = json.indexOf('[', idx);
        if (arrStart == -1) return result;
        int arrEnd = findMatchingBracket(json, arrStart, '[', ']');
        String arrContent = json.substring(arrStart + 1, arrEnd);
        int i = 0;
        while (i < arrContent.length()) {
            int q = arrContent.indexOf('"', i);
            if (q == -1) break;
            q++;
            StringBuilder sb = new StringBuilder();
            while (q < arrContent.length() && arrContent.charAt(q) != '"') {
                sb.append(arrContent.charAt(q++));
            }
            result.add(sb.toString());
            i = q + 1;
        }
        return result;
    }

    /**
     * براکت تطبیق‌یافته را پیدا می‌کند.
     *
     * @param s      رشته
     * @param start  موقعیت براکت باز
     * @param open   کاراکتر باز کننده
     * @param close  کاراکتر بسته کننده
     * @return ایندکس براکت بسته متناظر
     */
    private int findMatchingBracket(String s, int start, char open, char close) {
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == open) depth++;
                else if (c == close) {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return s.length() - 1;
    }

    /**
     * یک رشته JSON آرایه را به لیست اشیاء JSON تقسیم می‌کند.
     *
     * @param array محتوای داخل براکت آرایه
     * @return لیست رشته‌های شیء JSON
     */
    private List<String> splitJsonObjects(String array) {
        List<String> objects = new ArrayList<>();
        int i = 0;
        while (i < array.length()) {
            int objStart = array.indexOf('{', i);
            if (objStart == -1) break;
            int objEnd = findMatchingBracket(array, objStart, '{', '}');
            objects.add(array.substring(objStart, objEnd + 1));
            i = objEnd + 1;
        }
        return objects;
    }

    // ---- کلاس داده PlantStats ----

    /**
     * نگه‌دارنده تمام مشخصات ثابت یک گیاه که از JSON خوانده می‌شود.
     * این کلاس immutable در نظر گرفته می‌شود (فقط توسط پارسر پر می‌شود).
     */
    public static class PlantStats {
        /** شناسه عددی (از JSON) */
        public int id;
        /** نام نمایشی (مثل "Snow Pea") */
        public String name;
        /** نام داخلی برای کد (مثل "snow_pea") */
        public String internalName;
        /** خانواده گیاه (مثل "Shooter") */
        public String family;
        /** لیست تگ‌ها (مثل ["Ice","Pea"]) */
        public List<String> tags;
        /** هزینه خورشید برای کاشت */
        public int sunCost;
        /** HP پایه در سطح 1 */
        public int baseHp;
        /** رشته دمیج (ممکن است "20x2" یا "Insta-kill" باشد) */
        public String damage;
        /** توضیح قابلیت پایه (فارسی) */
        public String baseAbility;
        /** توضیح اثر plant food (فارسی) */
        public String plantFoodEffect;
        /** فاصله زمانی بین اکشن‌ها به ثانیه (-1 اگر ندارد) */
        public double actionIntervalSeconds;
        /** زمان recharge به ثانیه */
        public double rechargeSeconds;
        /** آیا این گیاه bonus (امتیازی) است */
        public boolean isBonus;
        /** توضیح ارتقای سطح 2 */
        public String upgradeLevel2;
        /** توضیح ارتقای سطح 3 */
        public String upgradeLevel3;
        /** توضیح ارتقای سطح 4 */
        public String upgradeLevel4;

        /** بررسی می‌کند آیا گیاه تگ مشخصی دارد */
        public boolean hasTag(String tag) {
            if (tags == null) return false;
            for (String t : tags) {
                if (t.equalsIgnoreCase(tag)) return true;
            }
            return false;
        }

        /** بررسی می‌کند آیا گیاه نخود (Pea family) است */
        public boolean isPea() {
            return hasTag("Pea");
        }

        /** بررسی می‌کند آیا گیاه قارچ (Shroom) است */
        public boolean isShroom() {
            return hasTag("Shroom");
        }

        /** بررسی می‌کند آیا گیاه Mint است */
        public boolean isMint() {
            return name != null && name.toLowerCase().endsWith("-mint");
        }

        /** بررسی می‌کند آیا گیاه آنی مصرفی (instakill/consumable) است */
        public boolean isInstant() {
            return baseHp == 0;
        }

        @Override
        public String toString() {
            return "PlantStats{id=" + id + ", name='" + name
                    + "', family='" + family + "', cost=" + sunCost + "}";
        }
    }
}
