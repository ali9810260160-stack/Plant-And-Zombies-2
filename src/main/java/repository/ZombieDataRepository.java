package repository;

import util.FileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * بارگذاری و مدیریت داده‌های ثابت زامبی‌ها و زره‌ها از فایل data/zombies/zombies.json.
 *
 * <p><b>سیستم Data-Driven:</b> برای اضافه کردن زامبی جدید،
 * کافی است یک شیء به آرایه "zombies" در zombies.json اضافه شود.
 * هیچ تغییری در کد Java لازم نیست.</p>
 *
 * <p>فرمت هر زامبی در JSON:</p>
 * <pre>
 * {
 *   "alias": "ZombieMummyDefault",
 *   "chapter": "ANCIENT_EGYPT",
 *   "hp": 190,
 *   "eatDps": 100,
 *   "speed": 0.185,
 *   "waveCost": 100,
 *   "canSpawnPlantFood": true,
 *   "armors": ["ConeDefault"],
 *   "specialClass": "standard",
 *   "specialProps": {}
 * }
 * </pre>
 *
 * <p>فرمت هر زره در JSON:</p>
 * <pre>
 * {
 *   "alias": "ConeDefault",
 *   "type": "Cone",
 *   "baseHealth": 370,
 *   "magnetShroom": false
 * }
 * </pre>
 */
public class ZombieDataRepository {

    private static final String ZOMBIES_FILE = "data/zombies/zombies.json";

    /** کش زامبی‌ها */
    private List<ZombieStats> cachedZombies;
    /** کش زره‌ها */
    private List<ArmorStats> cachedArmors;
    /** ایندکس alias → ZombieStats */
    private Map<String, ZombieStats> zombiesByAlias;
    /** ایندکس alias → ArmorStats */
    private Map<String, ArmorStats> armorsByAlias;

    // ---- بارگذاری ----

    /**
     * تمام زامبی‌ها را از فایل بارگذاری می‌کند.
     *
     * @return لیست تمام زامبی‌ها
     */
    public List<ZombieStats> loadAllZombies() {
        if (cachedZombies != null) {
            return cachedZombies;
        }
        loadFile();
        return cachedZombies;
    }

    /**
     * تمام زره‌ها را از فایل بارگذاری می‌کند.
     *
     * @return لیست تمام زره‌ها
     */
    public List<ArmorStats> loadAllArmors() {
        if (cachedArmors != null) {
            return cachedArmors;
        }
        loadFile();
        return cachedArmors;
    }

    /**
     * زامبی را با alias پیدا می‌کند.
     *
     * @param alias مثل "ZombieMummyDefault"
     * @return ZombieStats یا null
     */
    public ZombieStats getZombieByAlias(String alias) {
        if (zombiesByAlias == null) {
            loadFile();
        }
        return alias != null ? zombiesByAlias.get(alias) : null;
    }

    /**
     * زره را با alias پیدا می‌کند.
     *
     * @param alias مثل "ConeDefault"
     * @return ArmorStats یا null
     */
    public ArmorStats getArmorByAlias(String alias) {
        if (armorsByAlias == null) {
            loadFile();
        }
        return alias != null ? armorsByAlias.get(alias) : null;
    }

    /**
     * لیست زامبی‌های مجاز در یک فصل را برمی‌گرداند.
     * زامبی‌هایی که chapter = "ALL" هستند در همه فصل‌ها ظاهر می‌شوند.
     *
     * @param chapter نام فصل (مثل "ANCIENT_EGYPT")
     * @return لیست زامبی‌های آن فصل + زامبی‌های ALL
     */
    public List<ZombieStats> getZombiesForChapter(String chapter) {
        List<ZombieStats> result = new ArrayList<>();
        for (ZombieStats z : loadAllZombies()) {
            if ("ALL".equals(z.chapter) || (chapter != null && chapter.equals(z.chapter))) {
                result.add(z);
            }
        }
        return result;
    }

    /**
     * لیست زامبی‌های یک فصل که می‌توانند در موج ظاهر شوند
     * (بر اساس waveCost مثبت) را برمی‌گرداند.
     *
     * @param chapter نام فصل
     * @return لیست زامبی‌های قابل spawn
     */
    public List<ZombieStats> getSpawnableZombies(String chapter) {
        List<ZombieStats> result = new ArrayList<>();
        for (ZombieStats z : getZombiesForChapter(chapter)) {
            if (z.waveCost > 0) {
                result.add(z);
            }
        }
        return result;
    }

    /**
     * لیست زامبی‌های با specialClass مشخص را برمی‌گرداند.
     *
     * @param specialClass مثل "gargantuar" یا "imp"
     * @return لیست زامبی‌های با آن کلاس
     */
    public List<ZombieStats> getBySpecialClass(String specialClass) {
        List<ZombieStats> result = new ArrayList<>();
        for (ZombieStats z : loadAllZombies()) {
            if (specialClass.equals(z.specialClass)) {
                result.add(z);
            }
        }
        return result;
    }

    /**
     * کش را پاک می‌کند تا فایل دوباره خوانده شود.
     */
    public void invalidateCache() {
        cachedZombies = null;
        cachedArmors = null;
        zombiesByAlias = null;
        armorsByAlias = null;
    }

    // ---- بارگذاری فایل ----

    /** فایل JSON را می‌خواند و هر دو لیست را پر می‌کند */
    private void loadFile() {
        try {
            String json = FileUtil.readFile(ZOMBIES_FILE);
            if (json == null) {
                throw new RuntimeException("zombies.json not found at: " + ZOMBIES_FILE);
            }
            cachedZombies = parseZombiesArray(json);
            cachedArmors = parseArmorsArray(json);
            buildIndexes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load zombies.json", e);
        }
    }

    /** ایندکس‌های سریع را می‌سازد */
    private void buildIndexes() {
        zombiesByAlias = new LinkedHashMap<>();
        for (ZombieStats z : cachedZombies) {
            zombiesByAlias.put(z.alias, z);
        }
        armorsByAlias = new LinkedHashMap<>();
        for (ArmorStats a : cachedArmors) {
            armorsByAlias.put(a.alias, a);
        }
    }

    // ---- پارسر آرایه zombies ----

    private List<ZombieStats> parseZombiesArray(String json) {
        List<ZombieStats> list = new ArrayList<>();
        String arrayContent = extractArrayContent(json, "zombies");
        if (arrayContent == null) return list;
        for (String obj : splitJsonObjects(arrayContent)) {
            ZombieStats z = parseZombieObject(obj);
            if (z != null) list.add(z);
        }
        return list;
    }

    private ZombieStats parseZombieObject(String obj) {
        try {
            ZombieStats z = new ZombieStats();
            z.alias           = parseStringField(obj, "alias");
            z.chapter         = parseStringField(obj, "chapter");
            z.hp              = parseIntField(obj, "hp");
            z.eatDps          = parseIntField(obj, "eatDps");
            z.speed           = parseDoubleField(obj, "speed");
            z.waveCost        = parseIntField(obj, "waveCost");
            z.canSpawnPlantFood = parseBoolField(obj, "canSpawnPlantFood");
            z.armors          = parseStringArray(obj, "armors");
            z.specialClass    = parseStringField(obj, "specialClass");
            z.specialPropsRaw = extractObjectContent(obj, "specialProps");
            return z;
        } catch (Exception e) {
            System.err.println("[ZombieDataRepository] Failed to parse zombie: " + e.getMessage());
            return null;
        }
    }

    // ---- پارسر آرایه armors ----

    private List<ArmorStats> parseArmorsArray(String json) {
        List<ArmorStats> list = new ArrayList<>();
        String arrayContent = extractArrayContent(json, "armors");
        if (arrayContent == null) return list;
        for (String obj : splitJsonObjects(arrayContent)) {
            ArmorStats a = parseArmorObject(obj);
            if (a != null) list.add(a);
        }
        return list;
    }

    private ArmorStats parseArmorObject(String obj) {
        try {
            ArmorStats a = new ArmorStats();
            a.alias        = parseStringField(obj, "alias");
            a.type         = parseStringField(obj, "type");
            a.baseHealth   = parseIntField(obj, "baseHealth");
            a.magnetShroom = parseBoolField(obj, "magnetShroom");
            return a;
        } catch (Exception e) {
            System.err.println("[ZombieDataRepository] Failed to parse armor: " + e.getMessage());
            return null;
        }
    }

    // ---- ابزارهای پارس JSON ----

    private String extractArrayContent(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return null;
        int arrStart = json.indexOf('[', idx);
        if (arrStart == -1) return null;
        int arrEnd = findMatchingBracket(json, arrStart, '[', ']');
        return json.substring(arrStart + 1, arrEnd);
    }

    private String extractObjectContent(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return "{}";
        int brace = json.indexOf('{', idx);
        if (brace == -1) return "{}";
        int braceEnd = findMatchingBracket(json, brace, '{', '}');
        return json.substring(brace, braceEnd + 1);
    }

    private int parseIntField(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return 0;
        idx = json.indexOf(':', idx) + 1;
        StringBuilder sb = new StringBuilder();
        while (idx < json.length()) {
            char c = json.charAt(idx);
            if (Character.isDigit(c) || c == '-') sb.append(c);
            else if (sb.length() > 0) break;
            idx++;
        }
        if (sb.length() == 0) return 0;
        try { return Integer.parseInt(sb.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private double parseDoubleField(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return 0.0;
        idx = json.indexOf(':', idx) + 1;
        StringBuilder sb = new StringBuilder();
        while (idx < json.length()) {
            char c = json.charAt(idx);
            if (Character.isDigit(c) || c == '-' || c == '.') sb.append(c);
            else if (sb.length() > 0) break;
            idx++;
        }
        if (sb.length() == 0) return 0.0;
        try { return Double.parseDouble(sb.toString()); } catch (NumberFormatException e) { return 0.0; }
    }

    private String parseStringField(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return "";
        idx = json.indexOf(':', idx) + 1;
        while (idx < json.length() && json.charAt(idx) != '"') {
            if (json.charAt(idx) == 'n') return null;
            idx++;
        }
        if (idx >= json.length()) return "";
        idx++;
        StringBuilder sb = new StringBuilder();
        while (idx < json.length() && json.charAt(idx) != '"') {
            if (json.charAt(idx) == '\\' && idx + 1 < json.length()) {
                idx++;
            }
            sb.append(json.charAt(idx++));
        }
        return sb.toString();
    }

    private boolean parseBoolField(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return false;
        idx = json.indexOf(':', idx) + 1;
        while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) idx++;
        return json.startsWith("true", idx);
    }

    private List<String> parseStringArray(String json, String key) {
        List<String> result = new ArrayList<>();
        int idx = json.indexOf("\"" + key + "\"");
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
            while (q < arrContent.length() && arrContent.charAt(q) != '"') sb.append(arrContent.charAt(q++));
            result.add(sb.toString());
            i = q + 1;
        }
        return result;
    }

    private int findMatchingBracket(String s, int start, char open, char close) {
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) inString = !inString;
            if (!inString) {
                if (c == open) depth++;
                else if (c == close) { depth--; if (depth == 0) return i; }
            }
        }
        return s.length() - 1;
    }

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

    // ---- کلاس‌های داده ----

    /**
     * مشخصات ثابت یک زامبی که از zombies.json خوانده می‌شود.
     */
    public static class ZombieStats {
        /** نام داخلی (مثل "ZombieMummyDefault") */
        public String alias;
        /** فصل مخصوص ("ALL", "ANCIENT_EGYPT", ...) */
        public String chapter;
        /** HP پایه */
        public int hp;
        /** آسیب خوردن گیاه در ثانیه */
        public int eatDps;
        /** سرعت حرکت (خانه/ثانیه) */
        public double speed;
        /** هزینه موج */
        public int waveCost;
        /** آیا می‌تواند plant food بیندازد */
        public boolean canSpawnPlantFood;
        /** لیست alias زره‌های این زامبی */
        public List<String> armors;
        /** نوع رفتار خاص (مثل "gargantuar", "standard", "jester") */
        public String specialClass;
        /** محتوای خام specialProps به صورت رشته JSON */
        public String specialPropsRaw;

        /** بررسی می‌کند آیا زامبی زره دارد */
        public boolean hasArmor() {
            return armors != null && !armors.isEmpty();
        }

        /** بررسی می‌کند آیا زامبی از نوع boss/gargantuar است */
        public boolean isBoss() {
            return "gargantuar".equals(specialClass)
                    || (specialClass != null && specialClass.startsWith("zomboss"));
        }

        /** بررسی می‌کند آیا زامبی imp است */
        public boolean isImp() {
            return "imp".equals(specialClass) || "dragon_imp".equals(specialClass);
        }

        /** بررسی می‌کند آیا زامبی flag است */
        public boolean isFlag() {
            return "flag".equals(specialClass);
        }

        @Override
        public String toString() {
            return "ZombieStats{alias='" + alias + "', chapter='" + chapter
                    + "', hp=" + hp + ", waveCost=" + waveCost + "}";
        }
    }

    /**
     * مشخصات ثابت یک زره که از zombies.json خوانده می‌شود.
     */
    public static class ArmorStats {
        /** نام داخلی زره (مثل "ConeDefault") */
        public String alias;
        /** نوع نمایشی (مثل "Cone", "Bucket") */
        public String type;
        /** HP پایه زره */
        public int baseHealth;
        /** آیا magnetshroom می‌تواند این زره را بدزدد */
        public boolean magnetShroom;

        @Override
        public String toString() {
            return "ArmorStats{alias='" + alias + "', type='" + type
                    + "', hp=" + baseHealth + "}";
        }
    }
}
