package com.pvz2.server.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pvz2.server.ServerConfig;
import com.pvz2.server.log.Log;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Server-side authoritative user store.
 *
 * <p>User records are kept as opaque {@link JsonObject} documents (same schema
 * as the client's {@code users.json}) so game-specific fields round-trip
 * untouched; the server only reads/writes the handful of fields it validates
 * (auth, currencies, score). Persisted as a JSON array to
 * {@code data/users.json}, seeded once from the client's {@code assets} copy so
 * existing accounts carry over. Thread-safe via a read/write lock; writes are
 * atomic (temp file + move).
 */
public final class UserStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson COMPACT = new Gson();

    private final Path file;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    /** username(lowercase) → document, preserving insertion order. */
    private final Map<String, JsonObject> byName = new LinkedHashMap<>();

    public UserStore() {
        this.file = ServerConfig.USERS_FILE.toPath();
    }

    /** Load from disk, seeding from the client's users.json on first run. */
    public void load() {
        lock.writeLock().lock();
        try {
            Files.createDirectories(file.getParent());
            Path source = file;
            if (!Files.exists(file) && ServerConfig.SEED_USERS_FILE.exists()) {
                source = ServerConfig.SEED_USERS_FILE.toPath();
                Log.info("Seeding user store from " + source.toAbsolutePath());
            }
            if (Files.exists(source)) {
                try (Reader r = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
                    JsonElement root = GSON.fromJson(r, JsonElement.class);
                    if (root != null && root.isJsonArray()) {
                        for (JsonElement e : root.getAsJsonArray()) {
                            if (!e.isJsonObject()) continue;
                            JsonObject o = e.getAsJsonObject();
                            String uname = str(o, "username");
                            if (uname != null && !uname.isEmpty()) {
                                byName.put(uname.toLowerCase(), o);
                            }
                        }
                    }
                }
            }
            Log.info("User store loaded: " + byName.size() + " account(s)");
            if (source != file) persistLocked(); // write seeded copy to server's own file
        } catch (Exception e) {
            Log.error("Failed to load user store", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Deep copy of the user document (so callers can't mutate the stored one). */
    public JsonObject get(String username) {
        if (username == null) return null;
        lock.readLock().lock();
        try {
            JsonObject o = byName.get(username.toLowerCase());
            return o == null ? null : o.deepCopy();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean exists(String username) {
        if (username == null) return false;
        lock.readLock().lock();
        try { return byName.containsKey(username.toLowerCase()); }
        finally { lock.readLock().unlock(); }
    }

    /** Find a user document by its persistent remember-me token, or null. */
    public JsonObject findByRememberToken(String rememberToken) {
        if (rememberToken == null || rememberToken.isEmpty()) return null;
        lock.readLock().lock();
        try {
            for (JsonObject o : byName.values()) {
                if (rememberToken.equals(str(o, "rememberToken"))) return o.deepCopy();
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /** All user documents (deep copies), for leaderboard etc. */
    public List<JsonObject> all() {
        lock.readLock().lock();
        try {
            List<JsonObject> list = new ArrayList<>(byName.size());
            for (JsonObject o : byName.values()) list.add(o.deepCopy());
            return list;
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Insert or replace a user document by its {@code username}, then persist. */
    public void put(JsonObject doc) {
        String uname = str(doc, "username");
        if (uname == null || uname.isEmpty())
            throw new IllegalArgumentException("user document has no username");
        lock.writeLock().lock();
        try {
            byName.put(uname.toLowerCase(), doc.deepCopy());
            persistLocked();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Atomically apply {@code mutator} to the stored document for {@code username}
     * (a mutable working copy) and persist. Returns the updated copy, or null if
     * no such user. Serializes concurrent updates to the same account.
     */
    public JsonObject update(String username, java.util.function.Consumer<JsonObject> mutator) {
        if (username == null) return null;
        lock.writeLock().lock();
        try {
            JsonObject o = byName.get(username.toLowerCase());
            if (o == null) return null;
            JsonObject work = o.deepCopy();
            mutator.accept(work);
            byName.put(username.toLowerCase(), work);
            persistLocked();
            return work.deepCopy();
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ─── new-account baseline ─────────────────────────────────────────────────

    /**
     * Build a fresh user document with baseline defaults matching the client's
     * {@code new User(...)} + {@code UserRepository} serialization, so the client
     * can rebuild it via {@code mapToUser}. String-encoded fields (greenhouse,
     * upgrade maps) start empty; {@code unlockedLevels} starts at the first level.
     */
    public static JsonObject newUserDoc(String username, String passwordHash,
                                        String nickname, String email, String gender) {
        JsonObject o = new JsonObject();
        o.addProperty("username", username);
        o.addProperty("passwordHash", passwordHash);
        o.addProperty("nickname", nickname);
        o.addProperty("email", email);
        o.addProperty("gender", gender == null ? "MALE" : gender);
        o.addProperty("securityQuestion", "");
        o.addProperty("securityAnswerHash", "");
        o.addProperty("stayLoggedIn", false);
        o.addProperty("coins", 0L);
        o.addProperty("gems", 0);
        o.addProperty("pots", 0);
        o.addProperty("plantFoodCount", 0);
        o.addProperty("difficultyLevel", 3);
        o.addProperty("gamesPlayed", 0);
        o.addProperty("levelsCompleted", 0);
        o.addProperty("highestMeoPoint", 0L);
        o.addProperty("minigamesCompleted", 0);
        o.addProperty("dailyQuestsCompleted", 0);
        o.addProperty("regularQuestsCompleted", 0);
        o.addProperty("lastReachedLevel", "");
        o.addProperty("lastDailyOfferDate", "");
        JsonArray plants = new JsonArray();
        o.add("unlockedPlants", plants);
        o.add("seenZombies", new JsonArray());
        JsonArray levels = new JsonArray();
        levels.add("ANCIENT_EGYPT_1");
        o.add("unlockedLevels", levels);
        o.addProperty("storedPlantBoosts", "[]");
        o.addProperty("completedQuests", "[]");
        o.addProperty("dailyOfferPlant", "");
        o.addProperty("dailyOfferGeneratedDate", "");
        o.addProperty("plantUpgradeLevels", "");
        o.addProperty("plantSeedPackets", "");
        o.addProperty("greenhouse", "");
        o.addProperty("masterVolume", 0.8f);
        o.addProperty("musicEnabled", true);
        o.addProperty("sfxEnabled", true);
        o.addProperty("showGrid", false);
        o.addProperty("gameSpeed", 1.0f);
        return o;
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    public static String str(JsonObject o, String field) {
        if (o == null || !o.has(field) || o.get(field).isJsonNull()) return null;
        JsonElement e = o.get(field);
        return e.isJsonPrimitive() ? e.getAsString() : e.toString();
    }

    public static boolean has(JsonObject o, String field) {
        return o != null && o.has(field) && !o.get(field).isJsonNull();
    }

    /** Read a numeric field tolerantly (native number OR numeric string). */
    public static long getLong(JsonObject o, String field, long def) {
        String s = str(o, field);
        if (s == null || s.isEmpty()) return def;
        try { return (long) Double.parseDouble(s); } catch (NumberFormatException e) { return def; }
    }

    public static int getInt(JsonObject o, String field, int def) {
        return (int) getLong(o, field, def);
    }

    private void persistLocked() {
        JsonArray arr = new JsonArray();
        for (JsonObject o : byName.values()) arr.add(o);
        try {
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(arr, w);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Log.error("Failed to persist user store", e);
        }
    }

    /** Compact-JSON serialize a document (used when logging). */
    public static String compact(JsonObject o) { return COMPACT.toJson(o); }
}
