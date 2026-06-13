package repository;

import util.FileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * بارگذاری و مدیریت داده‌های کوئست‌ها از فایل data/quests/quests.json.
 *
 * <p><b>سیستم Data-Driven:</b> برای اضافه کردن کوئست جدید،
 * کافی است یک شیء به آرایه "quests" در quests.json اضافه شود.
 * هیچ تغییری در کد Java لازم نیست.</p>
 *
 * <p>فرمت هر کوئست در JSON:</p>
 * <pre>
 * {
 *   "id": "daily_sun_collector",
 *   "name": "آفتاب‌گیر روزانه",
 *   "category": "DAILY",
 *   "priority": "MEDIUM",
 *   "conditionType": "COLLECT_SUN",
 *   "conditionDescription": "جمع‌آوری {param1} واحد خورشید",
 *   "params": { "param1": [3000, 4000, 5000] },
 *   "rewardType": "COINS",
 *   "rewardAmount": 100,
 *   "rewardFormula": "param1_div_100",
 *   "isParametric": true,
 *   "isDaily": true
 * }
 * </pre>
 *
 * <p><b>پارامتریک بودن:</b> اگر isParametric = true باشد، بازی هنگام assign کردن
 * کوئست به کاربر، یک مقدار تصادفی از آرایه params انتخاب می‌کند.</p>
 */
public class QuestRepository {

    private static final String QUESTS_FILE = "data/quests/quests.json";

    /** کش همه تعریف‌های کوئست */
    private List<QuestDefinition> cachedQuests;

    /** ایندکس id → QuestDefinition */
    private Map<String, QuestDefinition> questsById;

    // ---- بارگذاری ----

    /**
     * تمام تعریف‌های کوئست را از فایل بارگذاری می‌کند.
     *
     * @return لیست تمام کوئست‌ها
     */
    public List<QuestDefinition> loadAll() {
        if (cachedQuests != null) {
            return cachedQuests;
        }
        try {
            String json = FileUtil.readFile(QUESTS_FILE);
            if (json == null) {
                throw new RuntimeException("quests.json not found at: " + QUESTS_FILE);
            }
            cachedQuests = parseQuestsJson(json);
            buildIndex();
            return cachedQuests;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load quests.json", e);
        }
    }

    /**
     * یک کوئست را با ID پیدا می‌کند.
     *
     * @param id شناسه کوئست
     * @return QuestDefinition یا null
     */
    public QuestDefinition getById(String id) {
        if (questsById == null) {
            loadAll();
        }
        return id != null ? questsById.get(id) : null;
    }

    /**
     * لیست کوئست‌های یک دسته را برمی‌گرداند.
     *
     * @param category نام دسته (مثل "DAILY", "MAIN", "EPIC_CHALLENGE")
     * @return لیست کوئست‌های آن دسته
     */
    public List<QuestDefinition> getByCategory(String category) {
        List<QuestDefinition> result = new ArrayList<>();
        for (QuestDefinition q : loadAll()) {
            if (category.equalsIgnoreCase(q.category)) {
                result.add(q);
            }
        }
        return result;
    }

    /**
     * لیست کوئست‌های روزانه را برمی‌گرداند.
     *
     * @return لیست کوئست‌های isDaily = true
     */
    public List<QuestDefinition> getDailyQuests() {
        List<QuestDefinition> result = new ArrayList<>();
        for (QuestDefinition q : loadAll()) {
            if (q.isDaily) {
                result.add(q);
            }
        }
        return result;
    }

    /**
     * لیست کوئست‌های غیر روزانه را برمی‌گرداند.
     *
     * @return لیست کوئست‌های isDaily = false
     */
    public List<QuestDefinition> getPermanentQuests() {
        List<QuestDefinition> result = new ArrayList<>();
        for (QuestDefinition q : loadAll()) {
            if (!q.isDaily) {
                result.add(q);
            }
        }
        return result;
    }

    /**
     * کوئست‌ها را بر اساس اولویت مرتب می‌کند.
     * ترتیب: CRITICAL > HIGH > MEDIUM > LOW
     *
     * @param quests لیست ورودی
     * @return لیست مرتب‌شده
     */
    public List<QuestDefinition> sortByPriority(List<QuestDefinition> quests) {
        List<QuestDefinition> sorted = new ArrayList<>(quests);
        sorted.sort((a, b) -> priorityValue(b.priority) - priorityValue(a.priority));
        return sorted;
    }

    /**
     * کش را پاک می‌کند.
     */
    public void invalidateCache() {
        cachedQuests = null;
        questsById = null;
    }

    // ---- پارسر JSON ----

    private List<QuestDefinition> parseQuestsJson(String json) {
        List<QuestDefinition> quests = new ArrayList<>();
        String arrayContent = extractArrayContent(json, "quests");
        if (arrayContent == null) {
            throw new RuntimeException("Key 'quests' not found in quests.json");
        }
        for (String obj : splitJsonObjects(arrayContent)) {
            QuestDefinition q = parseQuestObject(obj);
            if (q != null) {
                quests.add(q);
            }
        }
        return quests;
    }

    private QuestDefinition parseQuestObject(String obj) {
        try {
            QuestDefinition q = new QuestDefinition();
            q.id                   = parseStringField(obj, "id");
            q.name                 = parseStringField(obj, "name");
            q.category             = parseStringField(obj, "category");
            q.priority             = parseStringField(obj, "priority");
            q.conditionType        = parseStringField(obj, "conditionType");
            q.conditionDescription = parseStringField(obj, "conditionDescription");
            q.rewardType           = parseStringField(obj, "rewardType");
            q.rewardDescription    = parseStringField(obj, "rewardDescription");
            q.rewardFormula        = parseStringField(obj, "rewardFormula");
            q.rewardAmount         = parseIntField(obj, "rewardAmount");
            q.isParametric         = parseBoolField(obj, "isParametric");
            q.isDaily              = parseBoolField(obj, "isDaily");
            q.paramsRaw            = extractObjectContent(obj, "params");
            return q;
        } catch (Exception e) {
            System.err.println("[QuestRepository] Failed to parse quest: " + e.getMessage());
            return null;
        }
    }

    private void buildIndex() {
        questsById = new LinkedHashMap<>();
        for (QuestDefinition q : cachedQuests) {
            if (q.id != null) {
                questsById.put(q.id, q);
            }
        }
    }

    private int priorityValue(String priority) {
        if (priority == null) return 0;
        switch (priority.toUpperCase()) {
            case "CRITICAL": return 4;
            case "HIGH":     return 3;
            case "MEDIUM":   return 2;
            case "LOW":      return 1;
            default:         return 0;
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

    // ---- کلاس داده QuestDefinition ----

    /**
     * تعریف ثابت یک کوئست که از quests.json خوانده می‌شود.
     * این کلاس template کوئست است — وضعیت پیشرفت هر کاربر
     * در مدل Quest جداگانه نگه‌داری می‌شود.
     */
    public static class QuestDefinition {

        /** شناسه یکتا (مثل "daily_sun_collector") */
        public String id;

        /** نام نمایشی فارسی */
        public String name;

        /**
         * دسته‌بندی کوئست.
         * مقادیر مجاز: DAILY, MAIN, EPIC_CHALLENGE
         */
        public String category;

        /**
         * اولویت نمایش در Travel Log.
         * مقادیر مجاز: CRITICAL, HIGH, MEDIUM, LOW
         */
        public String priority;

        /**
         * نوع شرط تکمیل.
         * سرویس QuestService این رشته را به منطق مناسب تبدیل می‌کند.
         * مثال: "COLLECT_SUN", "KILL_ZOMBIES_IN_CHAPTER", "WIN_WITH_MAX_PLANTS_LOST"
         */
        public String conditionType;

        /**
         * توضیح شرط با placeholder‌ها.
         * مثال: "جمع‌آوری {param1} واحد خورشید"
         */
        public String conditionDescription;

        /**
         * نوع پاداش.
         * مقادیر مجاز: COINS, GEMS, SEED_PACKETS, RANDOM_PLANT, UNLOCK_PLANT
         */
        public String rewardType;

        /** توضیح پاداش (ممکن است placeholder داشته باشد) */
        public String rewardDescription;

        /**
         * فرمول محاسبه پاداش (اختیاری).
         * اگر null باشد، rewardAmount مستقیم استفاده می‌شود.
         * مثال: "param1_div_100" یعنی param1 تقسیم بر 100
         */
        public String rewardFormula;

        /** مقدار ثابت پاداش (اگر rewardFormula نداشته باشد) */
        public int rewardAmount;

        /**
         * آیا کوئست پارامتریک است.
         * اگر true باشد، هنگام assign به کاربر یک مقدار از params انتخاب می‌شود.
         */
        public boolean isParametric;

        /** آیا کوئست روزانه است (هر روز ریست می‌شود) */
        public boolean isDaily;

        /** محتوای خام params به صورت رشته JSON */
        public String paramsRaw;

        /**
         * توضیح شرط را با مقدار پارامتر پر می‌کند.
         *
         * @param paramValue مقدار پارامتر
         * @return توضیح نهایی
         */
        public String buildDescription(String paramValue) {
            if (conditionDescription == null) return "";
            if (paramValue == null) return conditionDescription;
            return conditionDescription.replace("{param1}", paramValue);
        }

        /**
         * مقدار پاداش را با توجه به فرمول و پارامتر محاسبه می‌کند.
         *
         * @param paramValue مقدار پارامتر (عدد به صورت رشته)
         * @return مقدار پاداش محاسبه‌شده
         */
        public int calculateReward(String paramValue) {
            if (rewardFormula == null || rewardFormula.isEmpty()) {
                return rewardAmount;
            }
            try {
                int param = Integer.parseInt(paramValue);
                switch (rewardFormula) {
                    case "param1":          return param;
                    case "param1_div_100":  return param / 100;
                    case "20_minus_param1": return Math.max(0, 20 - param);
                    default:                return rewardAmount;
                }
            } catch (NumberFormatException e) {
                return rewardAmount;
            }
        }

        @Override
        public String toString() {
            return "QuestDefinition{id='" + id + "', category='" + category
                    + "', type='" + conditionType + "', daily=" + isDaily + "}";
        }
    }
}
