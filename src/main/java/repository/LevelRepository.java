package repository;

import util.FileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * بارگذاری تعریف مراحل از فایل data/levels/levels.json
 * و ذخیره‌سازی پیشرفت کاربر (unlocked/completed) در data/levels/progress_{username}.json.
 *
 * <p><b>سیستم Data-Driven:</b> برای اضافه کردن مرحله جدید،
 * کافی است یک شیء به آرایه "levels" در levels.json اضافه شود.
 * هیچ تغییری در کد Java لازم نیست.</p>
 *
 * <p>فرمت هر مرحله در JSON:</p>
 * <pre>
 * {
 *   "id": "egypt_1",
 *   "chapter": "ANCIENT_EGYPT",
 *   "levelNumber": 1,
 *   "levelType": "NORMAL",
 *   "rows": 5, "cols": 9, "plantSlots": 8,
 *   "initialWaveDifficulty": 200,
 *   "waveCount": 3,
 *   "allowedZombies": ["ZombieMummyDefault", ...],
 *   "lockedPlants": [],
 *   "specialProps": { ... },
 *   "isUnlocked": true,
 *   "isCompleted": false
 * }
 * </pre>
 */
public class LevelRepository {

    /** مسیر فایل تعریف مراحل */
    private static final String LEVELS_FILE = "data/levels/levels.json";

    /** مسیر پایه پیشرفت کاربر */
    private static final String PROGRESS_DIR = "data/levels";

    /** کش تعریف‌های مراحل (تغییر نمی‌کند) */
    private List<LevelDefinition> cachedLevels;

    /** ایندکس id → LevelDefinition */
    private Map<String, LevelDefinition> levelsById;

    // ---- بارگذاری تعریف‌ها ----

    /**
     * تمام تعریف‌های مرحله را از فایل بارگذاری می‌کند.
     *
     * @return لیست تمام مراحل
     */
    public List<LevelDefinition> loadAll() {
        if (cachedLevels != null) {
            return cachedLevels;
        }
        try {
            String json = FileUtil.readFile(LEVELS_FILE);
            if (json == null) {
                throw new RuntimeException("levels.json not found at: " + LEVELS_FILE);
            }
            cachedLevels = parseLevelsJson(json);
            buildIndex();
            return cachedLevels;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load levels.json", e);
        }
    }

    /**
     * یک مرحله را با ID پیدا می‌کند.
     *
     * @param id شناسه مرحله (مثل "egypt_1")
     * @return LevelDefinition یا null
     */
    public LevelDefinition getById(String id) {
        if (levelsById == null) {
            loadAll();
        }
        return id != null ? levelsById.get(id) : null;
    }

    /**
     * لیست مراحل یک فصل را برمی‌گرداند.
     *
     * @param chapter نام فصل (مثل "ANCIENT_EGYPT")
     * @return لیست مراحل آن فصل
     */
    public List<LevelDefinition> getByChapter(String chapter) {
        List<LevelDefinition> result = new ArrayList<>();
        for (LevelDefinition l : loadAll()) {
            if (chapter.equalsIgnoreCase(l.chapter)) {
                result.add(l);
            }
        }
        result.sort((a, b) -> a.levelNumber - b.levelNumber);
        return result;
    }

    /**
     * لیست مراحل یک نوع خاص را برمی‌گرداند.
     *
     * @param levelType نوع مرحله (مثل "NORMAL", "BOSS", "CONVEYOR_BELT")
     * @return لیست مراحل آن نوع
     */
    public List<LevelDefinition> getByType(String levelType) {
        List<LevelDefinition> result = new ArrayList<>();
        for (LevelDefinition l : loadAll()) {
            if (levelType.equalsIgnoreCase(l.levelType)) {
                result.add(l);
            }
        }
        return result;
    }

    /**
     * کش را پاک می‌کند.
     */
    public void invalidateCache() {
        cachedLevels = null;
        levelsById = null;
    }

    // ---- ذخیره‌سازی پیشرفت کاربر ----

    /**
     * پیشرفت مراحل یک کاربر را از فایل بارگذاری می‌کند.
     * فایل پیشرفت در data/levels/progress_{username}.json ذخیره می‌شود.
     *
     * @param username نام کاربری
     * @return Map از levelId به LevelProgress
     */
    public Map<String, LevelProgress> loadProgress(String username) {
        Map<String, LevelProgress> progress = new LinkedHashMap<>();
        String path = progressFilePath(username);
        try {
            String json = FileUtil.readFile(path);
            if (json == null) {
                return initDefaultProgress(username);
            }
            return parseProgressJson(json);
        } catch (IOException e) {
            return initDefaultProgress(username);
        }
    }

    /**
     * پیشرفت مراحل یک کاربر را در فایل ذخیره می‌کند.
     *
     * @param username نام کاربری
     * @param progress Map پیشرفت
     */
    public void saveProgress(String username, Map<String, LevelProgress> progress) {
        String path = progressFilePath(username);
        String json = buildProgressJson(progress);
        try {
            FileUtil.writeFile(path, json);
        } catch (IOException e) {
            System.err.println("[LevelRepository] Failed to save progress for " + username + ": " + e.getMessage());
        }
    }

    /**
     * یک مرحله را برای کاربر به عنوان تکمیل‌شده علامت می‌زند
     * و مرحله بعدی را آنلاک می‌کند.
     *
     * @param username  نام کاربری
     * @param levelId   شناسه مرحله تکمیل‌شده
     */
    public void markCompleted(String username, String levelId) {
        Map<String, LevelProgress> progress = loadProgress(username);
        LevelProgress lp = progress.computeIfAbsent(levelId, k -> new LevelProgress(levelId));
        lp.isCompleted = true;
        lp.isUnlocked = true;
        unlockNextLevel(progress, levelId);
        saveProgress(username, progress);
    }

    /**
     * بررسی می‌کند آیا مرحله برای کاربر آنلاک شده است.
     *
     * @param username نام کاربری
     * @param levelId  شناسه مرحله
     * @return true اگر آنلاک شده باشد
     */
    public boolean isUnlocked(String username, String levelId) {
        Map<String, LevelProgress> progress = loadProgress(username);
        LevelProgress lp = progress.get(levelId);
        return lp != null && lp.isUnlocked;
    }

    // ---- ابزارهای کمکی ----

    /** مسیر فایل پیشرفت کاربر را برمی‌گرداند */
    private String progressFilePath(String username) {
        return PROGRESS_DIR + "/progress_" + username.toLowerCase() + ".json";
    }

    /**
     * پیشرفت پیش‌فرض را ایجاد می‌کند.
     * اولین مرحله هر فصل که isUnlocked = true است در JSON آنلاک می‌شود.
     */
    private Map<String, LevelProgress> initDefaultProgress(String username) {
        Map<String, LevelProgress> progress = new LinkedHashMap<>();
        for (LevelDefinition l : loadAll()) {
            LevelProgress lp = new LevelProgress(l.id);
            lp.isUnlocked = l.isUnlockedByDefault;
            lp.isCompleted = false;
            progress.put(l.id, lp);
        }
        saveProgress(username, progress);
        return progress;
    }

    /**
     * مرحله بعدی در همان فصل را آنلاک می‌کند.
     * اگر آخرین مرحله فصل بود، اولین مرحله فصل بعد آنلاک می‌شود.
     */
    private void unlockNextLevel(Map<String, LevelProgress> progress, String completedId) {
        LevelDefinition completed = getById(completedId);
        if (completed == null) return;

        List<LevelDefinition> chapterLevels = getByChapter(completed.chapter);
        boolean foundCurrent = false;
        for (LevelDefinition l : chapterLevels) {
            if (foundCurrent) {
                LevelProgress lp = progress.computeIfAbsent(l.id, k -> new LevelProgress(l.id));
                lp.isUnlocked = true;
                return;
            }
            if (l.id.equals(completedId)) {
                foundCurrent = true;
            }
        }

        // اگر آخرین مرحله فصل تکمیل شد، فصل بعدی را آنلاک کن
        String[] chapterOrder = {
            "ANCIENT_EGYPT", "FROSTBITE_CAVES", "BIG_WAVE_BEACH", "DARK_AGES"
        };
        for (int i = 0; i < chapterOrder.length - 1; i++) {
            if (chapterOrder[i].equals(completed.chapter)) {
                List<LevelDefinition> nextChapter = getByChapter(chapterOrder[i + 1]);
                if (!nextChapter.isEmpty()) {
                    LevelProgress lp = progress.computeIfAbsent(
                        nextChapter.get(0).id,
                        k -> new LevelProgress(nextChapter.get(0).id)
                    );
                    lp.isUnlocked = true;
                }
                return;
            }
        }
    }

    // ---- پارسر JSON ----

    private List<LevelDefinition> parseLevelsJson(String json) {
        List<LevelDefinition> levels = new ArrayList<>();
        String arrayContent = extractArrayContent(json, "levels");
        if (arrayContent == null) {
            throw new RuntimeException("Key 'levels' not found in levels.json");
        }
        for (String obj : splitJsonObjects(arrayContent)) {
            LevelDefinition l = parseLevelObject(obj);
            if (l != null) levels.add(l);
        }
        return levels;
    }

    private LevelDefinition parseLevelObject(String obj) {
        try {
            LevelDefinition l = new LevelDefinition();
            l.id                    = parseStringField(obj, "id");
            l.chapter               = parseStringField(obj, "chapter");
            l.levelNumber           = parseIntField(obj, "levelNumber");
            l.levelType             = parseStringField(obj, "levelType");
            l.rows                  = parseIntField(obj, "rows");
            l.cols                  = parseIntField(obj, "cols");
            l.plantSlots            = parseIntField(obj, "plantSlots");
            l.initialWaveDifficulty = parseIntField(obj, "initialWaveDifficulty");
            l.waveCount             = parseIntField(obj, "waveCount");
            l.allowedZombies        = parseStringArray(obj, "allowedZombies");
            l.lockedPlants          = parseStringArray(obj, "lockedPlants");
            l.isUnlockedByDefault   = parseBoolField(obj, "isUnlocked");
            l.specialPropsRaw       = extractObjectContent(obj, "specialProps");
            return l;
        } catch (Exception e) {
            System.err.println("[LevelRepository] Failed to parse level: " + e.getMessage());
            return null;
        }
    }

    private Map<String, LevelProgress> parseProgressJson(String json) {
        Map<String, LevelProgress> map = new LinkedHashMap<>();
        String arrayContent = extractArrayContent(json, "progress");
        if (arrayContent == null) return map;
        for (String obj : splitJsonObjects(arrayContent)) {
            String id = parseStringField(obj, "id");
            if (id == null || id.isEmpty()) continue;
            LevelProgress lp = new LevelProgress(id);
            lp.isUnlocked  = parseBoolField(obj, "isUnlocked");
            lp.isCompleted = parseBoolField(obj, "isCompleted");
            map.put(id, lp);
        }
        return map;
    }

    /**
     * Map پیشرفت را به رشته JSON تبدیل می‌کند.
     */
    private String buildProgressJson(Map<String, LevelProgress> progress) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"version\": 1,\n  \"progress\": [\n");
        boolean first = true;
        for (Map.Entry<String, LevelProgress> entry : progress.entrySet()) {
            if (!first) sb.append(",\n");
            first = false;
            LevelProgress lp = entry.getValue();
            sb.append("    {\n");
            sb.append("      \"id\": \"").append(escapeJson(lp.levelId)).append("\",\n");
            sb.append("      \"isUnlocked\": ").append(lp.isUnlocked).append(",\n");
            sb.append("      \"isCompleted\": ").append(lp.isCompleted).append("\n");
            sb.append("    }");
        }
        sb.append("\n  ]\n}");
        return sb.toString();
    }

    private void buildIndex() {
        levelsById = new LinkedHashMap<>();
        for (LevelDefinition l : cachedLevels) {
            if (l.id != null) levelsById.put(l.id, l);
        }
    }

    // ---- ابزارهای پارس JSON (مشترک) ----

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
        try { return Integer.parseInt(sb.toString()); }
        catch (NumberFormatException e) { return 0; }
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
                char esc = json.charAt(idx);
                switch (esc) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case 'n':  sb.append('\n'); break;
                    case 't':  sb.append('\t'); break;
                    default:   sb.append(esc);
                }
            } else {
                sb.append(json.charAt(idx));
            }
            idx++;
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
        String content = json.substring(arrStart + 1, arrEnd);
        int i = 0;
        while (i < content.length()) {
            int q = content.indexOf('"', i);
            if (q == -1) break;
            q++;
            StringBuilder sb = new StringBuilder();
            while (q < content.length() && content.charAt(q) != '"') sb.append(content.charAt(q++));
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
            int start = array.indexOf('{', i);
            if (start == -1) break;
            int end = findMatchingBracket(array, start, '{', '}');
            objects.add(array.substring(start, end + 1));
            i = end + 1;
        }
        return objects;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ---- کلاس‌های داده ----

    /**
     * تعریف ثابت یک مرحله که از levels.json خوانده می‌شود.
     */
    public static class LevelDefinition {

        /** شناسه یکتا (مثل "egypt_1") */
        public String id;
        /** فصل (مثل "ANCIENT_EGYPT") */
        public String chapter;
        /** شماره مرحله در فصل */
        public int levelNumber;
        /** نوع مرحله (مثل "NORMAL", "BOSS", "CONVEYOR_BELT") */
        public String levelType;
        /** تعداد ردیف‌های نقشه */
        public int rows;
        /** تعداد ستون‌های نقشه */
        public int cols;
        /** تعداد اسلات انتخاب گیاه */
        public int plantSlots;
        /** سختی اولیه موج اول */
        public int initialWaveDifficulty;
        /** تعداد امواج */
        public int waveCount;
        /** لیست alias زامبی‌های مجاز در این مرحله */
        public List<String> allowedZombies;
        /** لیست نام گیاهانی که در این مرحله قفل هستند */
        public List<String> lockedPlants;
        /** آیا این مرحله از ابتدای بازی آنلاک است */
        public boolean isUnlockedByDefault;
        /** محتوای خام specialProps به صورت رشته JSON */
        public String specialPropsRaw;

        /** بررسی می‌کند آیا مرحله از نوع boss است */
        public boolean isBossLevel() {
            return "BOSS".equalsIgnoreCase(levelType);
        }

        /** بررسی می‌کند آیا مرحله شب است (بدون خورشید آسمان) */
        public boolean isNight() {
            return "NIGHT_OPS".equalsIgnoreCase(levelType)
                    || (specialPropsRaw != null && specialPropsRaw.contains("\"noSkySun\": true"));
        }

        /** بررسی می‌کند آیا مرحله Conveyor Belt است */
        public boolean isConveyorBelt() {
            return "CONVEYOR_BELT".equalsIgnoreCase(levelType);
        }

        @Override
        public String toString() {
            return "LevelDefinition{id='" + id + "', chapter='" + chapter
                    + "', type='" + levelType + "', waves=" + waveCount + "}";
        }
    }

    /**
     * پیشرفت یک کاربر برای یک مرحله خاص.
     * این داده در فایل progress_{username}.json ذخیره می‌شود.
     */
    public static class LevelProgress {

        /** شناسه مرحله */
        public String levelId;
        /** آیا این مرحله برای کاربر آنلاک شده */
        public boolean isUnlocked;
        /** آیا کاربر این مرحله را تکمیل کرده */
        public boolean isCompleted;

        public LevelProgress(String levelId) {
            this.levelId = levelId;
            this.isUnlocked = false;
            this.isCompleted = false;
        }
    }
}
