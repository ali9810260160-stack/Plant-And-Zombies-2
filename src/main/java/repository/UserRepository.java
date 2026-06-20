package repository;

import model.NewsItem;
import model.User;
import model.enums.Gender;
import model.enums.SecurityQuestion;
import util.FileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * مسئول ذخیره و بارگذاری اطلاعات کاربران از فایل data/users.json.
 *
 * <p>هر اجرا اطلاعات کاربران از فایل خوانده می‌شود.
 * هر تغییر (ثبت‌نام، تغییر پروفایل، پیشرفت بازی) بلافاصله در فایل ذخیره می‌شود.</p>
 *
 * <p>ساختار فایل data/users.json:</p>
 * <pre>
 * {
 *   "version": 1,
 *   "stayLoggedInUser": "ali",
 *   "users": [
 *     {
 *       "username": "ali",
 *       "passwordHash": "...(SHA-256)...",
 *       "nickname": "علی",
 *       "email": "ali@example.com",
 *       "gender": "MALE",
 *       "securityQuestion": "Q1",
 *       "securityAnswerHash": "...",
 *       "stayLoggedIn": false,
 *       "difficultyLevel": 3,
 *       "coins": 500,
 *       "gems": 5,
 *       "gamesPlayed": 2,
 *       "levelsCompleted": 3,
 *       "highestMeoPoint": 1200,
 *       "minigamesCompleted": 0,
 *       "dailyQuestsCompleted": 1,
 *       "regularQuestsCompleted": 2,
 *       "lastReachedLevel": "egypt_3",
 *       "pots": 5,
 *       "plantFoodCount": 1,
 *       "lastDailyOfferDate": "2026-06-09",
 *       "unlockedPlants": ["sunflower","peashooter"],
 *       "seenZombies": ["ZombieMummyDefault"],
 *       "seedPackets": {"sunflower": 3, "peashooter": 0},
 *       "plantLevels": {"sunflower": 2, "peashooter": 1},
 *       "news": [
 *         { "message": "گیاه جدید آنلاک شد!", "timestamp": 1234567890, "read": false }
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 */
public class UserRepository {

    private static final String USERS_FILE = FileUtil.USERS_FILE;

    /** کش کاربران در حافظه */
    private Map<String, User> usersCache;

    /** نام کاربری stay-logged-in (ممکن است null باشد) */
    private String stayLoggedInUsername;

    // ---- بارگذاری ----

    /**
     * تمام کاربران را از فایل بارگذاری می‌کند.
     * اگر فایل وجود نداشته باشد، لیست خالی برمی‌گرداند.
     *
     * @return لیست کاربران
     */
    public List<User> loadAll() {
        ensureLoaded();
        return new ArrayList<>(usersCache.values());
    }

    /**
     * کاربر را با نام کاربری پیدا می‌کند.
     *
     * @param username نام کاربری
     * @return کاربر یا null
     */
    public User findByUsername(String username) {
        ensureLoaded();
        if (username == null) return null;
        return usersCache.get(username.toLowerCase());
    }

    /**
     * بررسی می‌کند آیا نام کاربری از قبل وجود دارد.
     *
     * @param username نام کاربری
     * @return true اگر تکراری باشد
     */
    public boolean existsByUsername(String username) {
        return findByUsername(username) != null;
    }

    /**
     * کاربر stay-logged-in را بارگذاری می‌کند.
     *
     * @return کاربر یا null اگر stay-logged-in تنظیم نشده باشد
     */
    public User loadStayLoggedInUser() {
        ensureLoaded();
        if (stayLoggedInUsername == null || stayLoggedInUsername.isEmpty()) {
            return null;
        }
        return findByUsername(stayLoggedInUsername);
    }

    // ---- ذخیره‌سازی ----

    /**
     * یک کاربر را ذخیره یا به‌روز می‌کند.
     * اگر کاربر جدید باشد اضافه می‌شود؛ اگر قبلاً وجود داشت آپدیت می‌شود.
     *
     * @param user کاربر
     */
    public void save(User user) {
        ensureLoaded();
        if (user == null || user.getUsername() == null) return;
        usersCache.put(user.getUsername().toLowerCase(), user);
        persist();
    }

    /**
     * تمام کاربران را یکجا ذخیره می‌کند.
     *
     * @param users لیست کاربران
     */
    public void saveAll(List<User> users) {
        ensureLoaded();
        usersCache.clear();
        for (User u : users) {
            if (u.getUsername() != null) {
                usersCache.put(u.getUsername().toLowerCase(), u);
            }
        }
        persist();
    }

    /**
     * نام کاربری stay-logged-in را تنظیم می‌کند.
     *
     * @param username نام کاربری یا null برای پاک کردن
     */
    public void setStayLoggedIn(String username) {
        ensureLoaded();
        this.stayLoggedInUsername = username;
        persist();
    }

    // ---- بارگذاری lazy ----

    /** اگر کش بارگذاری نشده، از فایل می‌خواند */
    private void ensureLoaded() {
        if (usersCache != null) return;
        usersCache = new LinkedHashMap<>();
        stayLoggedInUsername = null;

        try {
            String json = FileUtil.readFile(USERS_FILE);
            if (json == null) return; // فایل وجود ندارد — کاربری ثبت نشده
            parseUsersJson(json);
        } catch (IOException e) {
            System.err.println("[UserRepository] Failed to load users.json: " + e.getMessage());
        }
    }

    // ---- سریال‌سازی به فایل ----

    /** وضعیت فعلی را در فایل می‌نویسد */
    private void persist() {
        String json = buildUsersJson();
        try {
            FileUtil.writeFile(USERS_FILE, json);
        } catch (IOException e) {
            System.err.println("[UserRepository] Failed to save users.json: " + e.getMessage());
        }
    }

    /**
     * Map کاربران را به رشته JSON تبدیل می‌کند.
     */
    private String buildUsersJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"version\": 1,\n");
        sb.append("  \"stayLoggedInUser\": ");
        if (stayLoggedInUsername != null) {
            sb.append('"').append(escapeJson(stayLoggedInUsername)).append('"');
        } else {
            sb.append("null");
        }
        sb.append(",\n  \"users\": [\n");

        boolean firstUser = true;
        for (User u : usersCache.values()) {
            if (!firstUser) sb.append(",\n");
            firstUser = false;
            sb.append(serializeUser(u));
        }
        sb.append("\n  ]\n}");
        return sb.toString();
    }

    /** یک کاربر را به رشته JSON تبدیل می‌کند */
    private String serializeUser(User u) {
        StringBuilder sb = new StringBuilder();
        sb.append("    {\n");
        appendStr(sb, "username",           u.getUsername());
        appendStr(sb, "passwordHash",       u.getPasswordHash());
        appendStr(sb, "nickname",           u.getNickname());
        appendStr(sb, "email",              u.getEmail());
        appendStr(sb, "gender",             u.getGender() != null ? u.getGender().name() : "MALE");
        appendStr(sb, "securityQuestion",   u.getSecurityQuestion() != null ? u.getSecurityQuestion().name() : null);
        appendStr(sb, "securityAnswerHash", u.getSecurityAnswerHash());
        appendBool(sb, "stayLoggedIn",       u.isStayLoggedIn());
        appendInt(sb, "difficultyLevel",    u.getDifficultyLevel());
        appendLong(sb, "coins",              u.getCoins());
        appendInt(sb, "gems",               u.getGems());
        appendInt(sb, "gamesPlayed",        u.getGamesPlayed());
        appendInt(sb, "levelsCompleted",    u.getLevelsCompleted());
        appendLong(sb, "highestMeoPoint",    u.getHighestMeoPoint());
        appendInt(sb, "minigamesCompleted", u.getMinigamesCompleted());
        appendInt(sb, "dailyQuestsCompleted",   u.getDailyQuestsCompleted());
        appendInt(sb, "regularQuestsCompleted", u.getRegularQuestsCompleted());
        appendStr(sb, "lastReachedLevel",   u.getLastReachedLevel());
        appendInt(sb, "pots",               u.getPots());
        appendInt(sb, "plantFoodCount",     u.getPlantFoodCount());
        appendStr(sb, "lastDailyOfferDate", u.getLastDailyOfferDate());

        // unlockedPlants array
        sb.append("      \"unlockedPlants\": ");
        sb.append(serializeStringList(u.getUnlockedPlants()));
        sb.append(",\n");

        // seenZombies array
        sb.append("      \"seenZombies\": ");
        sb.append(serializeStringList(u.getSeenZombies()));
        sb.append(",\n");

        // news array
        sb.append("      \"news\": ");
        sb.append(serializeNews(u.getNews()));
        sb.append("\n    }");
        return sb.toString();
    }

    // ---- پارسر JSON ----

    private void parseUsersJson(String json) {
        // stayLoggedInUser
        stayLoggedInUsername = parseStringField(json, "stayLoggedInUser");

        // users array
        String arrayContent = extractArrayContent(json, "users");
        if (arrayContent == null) return;

        for (String obj : splitJsonObjects(arrayContent)) {
            User u = parseUserObject(obj);
            if (u != null && u.getUsername() != null) {
                usersCache.put(u.getUsername().toLowerCase(), u);
            }
        }
    }

    private User parseUserObject(String obj) {
        try {
            String username = parseStringField(obj, "username");
            if (username == null || username.isEmpty()) return null;

            User u = new User(
                username,
                parseStringField(obj, "passwordHash"),
                parseStringField(obj, "nickname"),
                parseStringField(obj, "email"),
                parseGender(parseStringField(obj, "gender"))
            );

            String sqStr = parseStringField(obj, "securityQuestion");
            if (sqStr != null && !sqStr.isEmpty()) {
                try { u.setSecurityQuestion(SecurityQuestion.valueOf(sqStr)); }
                catch (IllegalArgumentException ignored) { }
            }
            u.setSecurityAnswerHash(parseStringField(obj, "securityAnswerHash"));
            u.setStayLoggedIn(parseBoolField(obj, "stayLoggedIn"));
            u.setDifficultyLevel(parseIntField(obj, "difficultyLevel", 3));
            u.setCoins(parseLongField(obj, "coins"));
            u.setGems(parseIntField(obj, "gems", 0));
            u.setGamesPlayed(parseIntField(obj, "gamesPlayed", 0));
            u.setLevelsCompleted(parseIntField(obj, "levelsCompleted", 0));
            u.setHighestMeoPoint(parseLongField(obj, "highestMeoPoint"));
            u.setMinigamesCompleted(parseIntField(obj, "minigamesCompleted", 0));
            u.setDailyQuestsCompleted(parseIntField(obj, "dailyQuestsCompleted", 0));
            u.setRegularQuestsCompleted(parseIntField(obj, "regularQuestsCompleted", 0));
            u.setLastReachedLevel(parseStringField(obj, "lastReachedLevel"));
            u.setPots(parseIntField(obj, "pots", 5));
            u.setPlantFoodCount(parseIntField(obj, "plantFoodCount", 0));
            u.setLastDailyOfferDate(parseStringField(obj, "lastDailyOfferDate"));
            u.setUnlockedPlants(parseStringArray(obj, "unlockedPlants"));
            u.setSeenZombies(parseStringArray(obj, "seenZombies"));
            u.setNews(parseNewsArray(obj));
            return u;
        } catch (Exception e) {
            System.err.println("[UserRepository] Failed to parse user: " + e.getMessage());
            return null;
        }
    }

    private List<NewsItem> parseNewsArray(String obj) {
        List<NewsItem> items = new ArrayList<>();
        String arrayContent = extractArrayContent(obj, "news");
        if (arrayContent == null) return items;
        for (String newsObj : splitJsonObjects(arrayContent)) {
            String msg = parseStringField(newsObj, "message");
            long ts = parseLongField(newsObj, "timestamp");
            boolean read = parseBoolField(newsObj, "read");
            NewsItem item = new NewsItem(msg, ts);
            item.setRead(read);
            items.add(item);
        }
        return items;
    }

    private Gender parseGender(String s) {
        if (s == null) return Gender.MALE;
        try { return Gender.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return Gender.MALE; }
    }

    // ---- سریال‌ساز کمکی ----

    private String serializeStringList(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append('"').append(escapeJson(list.get(i))).append('"');
        }
        sb.append("]");
        return sb.toString();
    }

    private String serializeNews(List<NewsItem> news) {
        if (news == null || news.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < news.size(); i++) {
            if (i > 0) sb.append(",\n");
            NewsItem item = news.get(i);
            sb.append("        { \"message\": \"")
              .append(escapeJson(item.getMessage()))
              .append("\", \"timestamp\": ").append(item.getTimestamp())
              .append(", \"read\": ").append(item.isRead())
              .append(" }");
        }
        sb.append("\n      ]");
        return sb.toString();
    }

    private void appendStr(StringBuilder sb, String key, String value) {
        sb.append("      \"").append(key).append("\": ");
        if (value == null) sb.append("null");
        else sb.append('"').append(escapeJson(value)).append('"');
        sb.append(",\n");
    }

    private void appendInt(StringBuilder sb, String key, int value) {
        sb.append("      \"").append(key).append("\": ").append(value).append(",\n");
    }

    private void appendLong(StringBuilder sb, String key, long value) {
        sb.append("      \"").append(key).append("\": ").append(value).append(",\n");
    }

    private void appendBool(StringBuilder sb, String key, boolean value) {
        sb.append("      \"").append(key).append("\": ").append(value).append(",\n");
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

    private int parseIntField(String json, String key, int defaultValue) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return defaultValue;
        idx = json.indexOf(':', idx) + 1;
        StringBuilder sb = new StringBuilder();
        while (idx < json.length()) {
            char c = json.charAt(idx);
            if (Character.isDigit(c) || c == '-') sb.append(c);
            else if (sb.length() > 0) break;
            idx++;
        }
        if (sb.length() == 0) return defaultValue;
        try { return Integer.parseInt(sb.toString()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    private long parseLongField(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return 0L;
        idx = json.indexOf(':', idx) + 1;
        StringBuilder sb = new StringBuilder();
        while (idx < json.length()) {
            char c = json.charAt(idx);
            if (Character.isDigit(c) || c == '-') sb.append(c);
            else if (sb.length() > 0) break;
            idx++;
        }
        if (sb.length() == 0) return 0L;
        try { return Long.parseLong(sb.toString()); }
        catch (NumberFormatException e) { return 0L; }
    }

    private String parseStringField(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return null;
        idx = json.indexOf(':', idx) + 1;
        while (idx < json.length() && json.charAt(idx) != '"') {
            char c = json.charAt(idx);
            if (c == 'n' && json.startsWith("null", idx)) return null;
            if (!Character.isWhitespace(c) && c != ':') idx++;
            else idx++;
        }
        if (idx >= json.length()) return null;
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
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    /**
     * کاربر را از کش و فایل حذف می‌کند.
     * برای تغییر username استفاده می‌شود.
     */
    public void deleteByUsername(String username) {
        ensureLoaded();
        if (username == null) return;
        usersCache.remove(username.toLowerCase());
        persist();
    }

}
