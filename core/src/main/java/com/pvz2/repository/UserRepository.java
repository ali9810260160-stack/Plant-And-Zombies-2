package com.pvz2.repository;

import com.pvz2.model.User;
import com.pvz2.model.enums.Gender;
import com.pvz2.model.enums.SecurityQuestion;
import com.pvz2.util.FileUtil;
import com.pvz2.util.SimpleJsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ذخیره‌سازی و بارگذاری کاربران از/در فایل JSON.
 * سطح ارتقای گیاهان و موجودی بسته‌بذر نیز persist می‌شوند.
 */
public class UserRepository {

    private static final String FILE_PATH = "data/users.json";
    private List<User> cache;

    /**
     * Optional phase-3 hook: when set (online mode), invoked after every local
     * {@link #save(User)} so the user's data can be pushed to the server too.
     * Kept as a callback to avoid a repository→network layer dependency.
     */
    public interface RemoteSync { void push(User user); }
    private RemoteSync remoteSync;
    public void setRemoteSync(RemoteSync rs) { this.remoteSync = rs; }

    public UserRepository() {
        this.cache = new ArrayList<>();
        loadAll();
    }

    public List<User> loadAll() {
        if (!FileUtil.exists(FILE_PATH)) {
            cache = new ArrayList<>();
            return cache;
        }
        try {
            String content = FileUtil.readFile(FILE_PATH);
            cache = parseUsers(content);
        } catch (IOException e) {
            cache = new ArrayList<>();
        }
        return cache;
    }

    public void save(User user) {
        boolean found = false;
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).getUsername().equals(user.getUsername())) {
                cache.set(i, user);
                found = true;
                break;
            }
        }
        if (!found) {
            cache.add(user);
        }
        saveAll(cache);
        if (remoteSync != null) {
            try { remoteSync.push(user); } catch (Exception ignored) { }
        }
    }

    public User findByUsername(String username) {
        for (User u : cache) {
            if (u.getUsername().equalsIgnoreCase(username)) return u;
        }
        return null;
    }

    public boolean existsByUsername(String username) {
        return findByUsername(username) != null;
    }

    public User loadStayLoggedInUser() {
        for (User u : cache) {
            if (u.isStayLoggedIn()) return u;
        }
        return null;
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(cache);
    }

    public void saveAll(List<User> users) {
        List<Map<String, String>> list = new ArrayList<>();
        for (User u : users) {
            list.add(userToMap(u));
        }
        String json = SimpleJsonParser.toJsonArray(list);
        try {
            FileUtil.ensureDirectory("data");
            FileUtil.writeFile(FILE_PATH, json);
        } catch (IOException e) {
            System.err.println("Failed to save users: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    //  PHASE 3 — network bridge (server user document ↔ User)
    // ----------------------------------------------------------------

    /**
     * Flatten a {@link User} to the same {@code String→String} map the local
     * JSON uses. The phase-3 network layer turns this into the JSON document it
     * pushes to the server (so client and server agree on the schema).
     */
    public Map<String, String> toFlatMap(User u) { return userToMap(u); }

    /**
     * Rebuild a {@link User} from a flat {@code String→String} map (the server's
     * user document, flattened by the network layer). Reuses the exact local
     * deserializer so all fields (currencies, plants, greenhouse, settings, …)
     * round-trip identically.
     */
    public User fromFlatMap(Map<String, String> m) { return mapToUser(m); }

    // ----------------------------------------------------------------
    //  SERIALIZATION
    // ----------------------------------------------------------------

    private Map<String, String> userToMap(User u) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("username", u.getUsername());
        m.put("passwordHash", u.getPasswordHash());
        m.put("nickname", u.getNickname());
        m.put("email", u.getEmail());
        m.put("gender", u.getGender() != null ? u.getGender().name() : "MALE");
        m.put("securityQuestion",
                u.getSecurityQuestion() != null ? u.getSecurityQuestion().name() : "");
        m.put("securityAnswerHash",
                u.getSecurityAnswerHash() != null ? u.getSecurityAnswerHash() : "");
        m.put("stayLoggedIn", String.valueOf(u.isStayLoggedIn()));
        m.put("coins", String.valueOf(u.getCoins()));
        m.put("gems", String.valueOf(u.getGems()));
        m.put("pots", String.valueOf(u.getPots()));
        m.put("plantFoodCount", String.valueOf(u.getPlantFoodCount()));
        m.put("difficultyLevel", String.valueOf(u.getDifficultyLevel()));
        m.put("gamesPlayed", String.valueOf(u.getGamesPlayed()));
        m.put("levelsCompleted", String.valueOf(u.getLevelsCompleted()));
        m.put("highestMeoPoint", String.valueOf(u.getHighestMeoPoint()));
        m.put("minigamesCompleted", String.valueOf(u.getMinigamesCompleted()));
        m.put("dailyQuestsCompleted", String.valueOf(u.getDailyQuestsCompleted()));
        m.put("regularQuestsCompleted", String.valueOf(u.getRegularQuestsCompleted()));
        m.put("lastReachedLevel",
                u.getLastReachedLevel() != null ? u.getLastReachedLevel() : "");
        m.put("lastDailyOfferDate",
                u.getLastDailyOfferDate() != null ? u.getLastDailyOfferDate() : "");
        m.put("unlockedPlants", listToJsonArray(u.getUnlockedPlants()));
        m.put("seenZombies", listToJsonArray(u.getSeenZombies()));
        m.put("unlockedLevels", listToJsonArray(u.getUnlockedLevels()));
        m.put("storedPlantBoosts", listToJsonArray(u.getBoostedPlants()));
        m.put("completedQuests", listToJsonArray(u.getCompletedQuests()));
        m.put("dailyOfferPlant", u.getDailyOfferPlant() != null ? u.getDailyOfferPlant() : "");
        m.put("dailyOfferGeneratedDate",
                u.getDailyOfferGeneratedDate() != null ? u.getDailyOfferGeneratedDate() : "");
        // ---- ارتقاهای گیاه و بسته‌بذر ----
        m.put("plantUpgradeLevels", mapToString(u.getPlantUpgradeLevels()));
        m.put("plantSeedPackets", mapToString(u.getPlantSeedPackets()));
        // ---- گلخانه و تنظیماتِ صوت/گرافیکِ per-user (فاز ۲) ----
        m.put("greenhouse", serializeGreenhouse(u.getGreenhouse()));
        m.put("masterVolume", String.valueOf(u.getMasterVolume()));
        m.put("musicEnabled", String.valueOf(u.isMusicEnabled()));
        m.put("sfxEnabled", String.valueOf(u.isSfxEnabled()));
        m.put("showGrid", String.valueOf(u.isShowGrid()));
        m.put("gameSpeed", String.valueOf(u.getGameSpeed()));
        return m;
    }

    /**
     * سریال‌سازیِ گلخانه در یک رشته‌ی فشرده. هر گلدان با ';' جدا و فیلدهایش
     * با ':' — به‌شکلِ {@code x:y:locked:plantType:plantedAtEpochSec:growthHours}.
     * plantedAt خالی → "0" و plantType خالی → "-".
     */
    private String serializeGreenhouse(com.pvz2.model.Greenhouse gh) {
        if (gh == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int y = 1; y <= com.pvz2.model.Greenhouse.ROWS; y++) {
            for (int x = 1; x <= com.pvz2.model.Greenhouse.COLS; x++) {
                com.pvz2.model.Pot p = gh.getPot(x, y);
                if (p == null) continue;
                if (sb.length() > 0) sb.append(';');
                long epoch = 0L;
                if (p.getPlantedAt() != null) {
                    epoch = p.getPlantedAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
                }
                String type = p.getPlantType() != null ? p.getPlantType() : "-";
                sb.append(p.getX()).append(':').append(p.getY()).append(':')
                  .append(p.isLocked()).append(':').append(type).append(':')
                  .append(epoch).append(':').append(p.getGrowthHours());
            }
        }
        return sb.toString();
    }

    private com.pvz2.model.Greenhouse deserializeGreenhouse(String raw) {
        com.pvz2.model.Greenhouse gh = new com.pvz2.model.Greenhouse();
        if (raw == null || raw.trim().isEmpty()) return gh;
        for (String potStr : raw.split(";")) {
            String[] f = potStr.split(":");
            if (f.length < 6) continue;
            try {
                int x = Integer.parseInt(f[0].trim());
                int y = Integer.parseInt(f[1].trim());
                com.pvz2.model.Pot p = gh.getPot(x, y);
                if (p == null) continue;
                p.setLocked(Boolean.parseBoolean(f[2].trim()));
                String type = f[3].trim();
                p.setPlantType("-".equals(type) || type.isEmpty() ? null : type);
                long epoch = Long.parseLong(f[4].trim());
                p.setPlantedAt(epoch <= 0 ? null
                        : java.time.Instant.ofEpochSecond(epoch)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                p.setGrowthHours(Integer.parseInt(f[5].trim()));
            } catch (Exception ignored) { }
        }
        return gh;
    }

    /**
     * تبدیل Map<String,Integer> به رشته با فرمت "KEY1=VAL1|KEY2=VAL2"
     * از '|' به عنوان جداکننده استفاده می‌شود تا با ',' درون JSON تداخل نداشته باشد.
     */
    private String mapToString(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (sb.length() > 0) sb.append('|');
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    private String listToJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(list.get(i)).append("\"");
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    // ----------------------------------------------------------------
    //  DESERIALIZATION
    // ----------------------------------------------------------------

    private List<User> parseUsers(String json) {
        List<Map<String, String>> maps = SimpleJsonParser.parseArray(json);
        List<User> users = new ArrayList<>();
        for (Map<String, String> m : maps) {
            User u = mapToUser(m);
            if (u != null) users.add(u);
        }
        return users;
    }

    private User mapToUser(Map<String, String> m) {
        try {
            String uname = m.getOrDefault("username", "");
            String hash = m.getOrDefault("passwordHash", "");
            String nick = m.getOrDefault("nickname", "");
            String email = m.getOrDefault("email", "");
            Gender gender = Gender.valueOf(
                    m.getOrDefault("gender", "MALE").toUpperCase());
            User u = new User(uname, hash, nick, email, gender);

            String sq = m.getOrDefault("securityQuestion", "");
            if (!sq.isEmpty()) {
                try { u.setSecurityQuestion(SecurityQuestion.valueOf(sq)); }
                catch (IllegalArgumentException ignored) { }
            }
            u.setSecurityAnswerHash(m.getOrDefault("securityAnswerHash", ""));
            u.setStayLoggedIn(Boolean.parseBoolean(
                    m.getOrDefault("stayLoggedIn", "false")));
            u.setCoins(parseLong(m.getOrDefault("coins", "0")));
            u.setGems(parseInt(m.getOrDefault("gems", "0")));
            u.setPots(parseInt(m.getOrDefault("pots", "0")));
            u.setPlantFoodCount(parseInt(m.getOrDefault("plantFoodCount", "0")));
            u.setDifficultyLevel(parseInt(m.getOrDefault("difficultyLevel", "3")));
            u.setGamesPlayed(parseInt(m.getOrDefault("gamesPlayed", "0")));
            u.setLevelsCompleted(parseInt(m.getOrDefault("levelsCompleted", "0")));
            u.setHighestMeoPoint(parseLong(m.getOrDefault("highestMeoPoint", "0")));
            u.setMinigamesCompleted(parseInt(
                    m.getOrDefault("minigamesCompleted", "0")));
            u.setDailyQuestsCompleted(parseInt(
                    m.getOrDefault("dailyQuestsCompleted", "0")));
            u.setRegularQuestsCompleted(parseInt(
                    m.getOrDefault("regularQuestsCompleted", "0")));
            u.setLastReachedLevel(m.getOrDefault("lastReachedLevel", ""));
            u.setLastDailyOfferDate(m.getOrDefault("lastDailyOfferDate", ""));
            u.setUnlockedPlants(parseStringArray(
                    m.getOrDefault("unlockedPlants", "[]")));
            u.setSeenZombies(parseStringArray(
                    m.getOrDefault("seenZombies", "[]")));
            u.setBoostedPlants(parseStringArray(
                    m.getOrDefault("storedPlantBoosts", "[]")));
            u.setCompletedQuests(parseStringArray(
                    m.getOrDefault("completedQuests", "[]")));
            u.setDailyOfferPlant(m.getOrDefault("dailyOfferPlant", ""));
            u.setDailyOfferGeneratedDate(m.getOrDefault("dailyOfferGeneratedDate", ""));

            List<String> unlocked = parseStringArray(
                    m.getOrDefault("unlockedLevels", "[\"ANCIENT_EGYPT_1\"]"));
            if (unlocked.isEmpty()) unlocked.add("ANCIENT_EGYPT_1");
            u.setUnlockedLevels(unlocked);

            // ---- ارتقاهای گیاه ----
            u.setPlantUpgradeLevels(parseIntMap(
                    m.getOrDefault("plantUpgradeLevels", "")));
            u.setPlantSeedPackets(parseIntMap(
                    m.getOrDefault("plantSeedPackets", "")));

            // ---- گلخانه و تنظیماتِ per-user (فاز ۲) ----
            u.setGreenhouse(deserializeGreenhouse(m.getOrDefault("greenhouse", "")));
            u.setMasterVolume(parseFloat(m.getOrDefault("masterVolume", "0.8"), 0.8f));
            u.setMusicEnabled(Boolean.parseBoolean(m.getOrDefault("musicEnabled", "true")));
            u.setSfxEnabled(Boolean.parseBoolean(m.getOrDefault("sfxEnabled", "true")));
            u.setShowGrid(Boolean.parseBoolean(m.getOrDefault("showGrid", "false")));
            u.setGameSpeed(parseFloat(m.getOrDefault("gameSpeed", "1.0"), 1.0f));

            u.setUnreadNews(new ArrayList<>());
            return u;
        } catch (Exception e) {
            System.err.println("[UserRepository] Failed to parse user: " + e.getMessage());
            return null;
        }
    }

    /**
     * تبدیل رشته "KEY1=VAL1|KEY2=VAL2" به Map<String,Integer>
     */
    private Map<String, Integer> parseIntMap(String raw) {
        Map<String, Integer> result = new HashMap<>();
        if (raw == null || raw.isEmpty()) return result;
        for (String entry : raw.split("\\|")) {
            String[] parts = entry.split("=", 2);
            if (parts.length == 2) {
                try {
                    result.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                } catch (NumberFormatException ignored) { }
            }
        }
        return result;
    }

    private List<String> parseStringArray(String json) {
        List<String> result = new ArrayList<>();
        if (json == null || json.equals("[]") || json.isEmpty()) return result;
        String inner = json.trim();
        if (inner.startsWith("[")) inner = inner.substring(1);
        if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);
        for (String part : inner.split(",")) {
            String s = part.trim().replace("\"", "");
            if (!s.isEmpty()) result.add(s);
        }
        return result;
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private long parseLong(String s) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0L; }
    }

    private float parseFloat(String s, float def) {
        try { return Float.parseFloat(s.trim()); } catch (Exception e) { return def; }
    }
}
