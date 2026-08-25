package com.pvz2.graphics.net;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.FileReader;
import java.io.FileWriter;

/**
 * Persists the client's "remember me" token so online auto-login survives app
 * restarts. Stored at {@code data/session.json} (client cwd is the assets dir),
 * alongside {@code users.json}. Best-effort and crash-proof.
 */
public final class SessionStore {

    private SessionStore() { }

    private static final String FILE = "data/session.json";
    private static final Gson GSON = new Gson();

    /** A saved remember-me record. */
    public static final class Saved {
        public String username;
        public String rememberToken;
    }

    public static void save(String username, String rememberToken) {
        if (rememberToken == null || rememberToken.isEmpty()) { clear(); return; }
        JsonObject o = new JsonObject();
        o.addProperty("username", username);
        o.addProperty("rememberToken", rememberToken);
        try (FileWriter w = new FileWriter(FILE)) { GSON.toJson(o, w); }
        catch (Exception e) { NetLog.warn("session save failed: " + e.getMessage()); }
    }

    public static Saved load() {
        try (FileReader r = new FileReader(FILE)) {
            Saved s = GSON.fromJson(r, Saved.class);
            if (s != null && s.rememberToken != null && !s.rememberToken.isEmpty()) return s;
        } catch (Exception ignored) { }
        return null;
    }

    public static void clear() {
        try { new java.io.File(FILE).delete(); } catch (Exception ignored) { }
    }
}
