package com.pvz2.server;

import java.io.File;

/**
 * Central server configuration. Values are read once at startup from optional
 * JVM system properties (e.g. {@code -Dpvz.port=6000}) with sensible defaults,
 * so the server needs no config file to run out of the box.
 */
public final class ServerConfig {

    private ServerConfig() { }

    /** Human-readable server name sent in the HELLO greeting. */
    public static final String SERVER_NAME =
            System.getProperty("pvz.name", "PvZ2 Server");

    /** TCP port the server listens on. */
    public static final int PORT =
            Integer.getInteger("pvz.port", 5599);

    /** Root data directory (users store, logs). Relative to the server's cwd. */
    public static final File DATA_DIR =
            new File(System.getProperty("pvz.data", "data"));

    /** Log directory. */
    public static final File LOG_DIR = new File(DATA_DIR, "logs");

    /** Server-side user store file (same schema as the client's users.json). */
    public static final File USERS_FILE = new File(DATA_DIR, "users.json");

    /**
     * One-time seed source: the client's phase-1 users.json (relative to the
     * server's working dir, i.e. the repo's {@code assets/data/users.json}).
     * Copied into {@link #USERS_FILE} on first run so existing accounts carry over.
     */
    public static final File SEED_USERS_FILE = new File(
            System.getProperty("pvz.seedUsers", "../assets/data/users.json"));

    /**
     * Usernames granted admin rights (comma-separated via {@code -Dpvz.admins=}).
     * Compared case-insensitively; also honored is a {@code "admin":true} flag in
     * a user's document.
     */
    public static final java.util.Set<String> ADMIN_USERS = parseAdmins(
            System.getProperty("pvz.admins", "admin"));

    private static java.util.Set<String> parseAdmins(String csv) {
        java.util.Set<String> set = new java.util.HashSet<>();
        if (csv != null) {
            for (String s : csv.split(",")) {
                String t = s.trim().toLowerCase();
                if (!t.isEmpty()) set.add(t);
            }
        }
        return set;
    }

    /** Whether to open the Swing admin dashboard window (off in headless envs). */
    public static final boolean ADMIN_UI =
            Boolean.parseBoolean(System.getProperty("pvz.adminUi", "true"));

    // ─── Liveness tuning ──────────────────────────────────────────────────────

    /** How often the server pings idle clients (ms). */
    public static final long HEARTBEAT_INTERVAL_MS = 15_000L;

    /** A connection with no traffic for this long is considered dead (ms). */
    public static final long CONNECTION_TIMEOUT_MS = 45_000L;

    /** Grace window during which a dropped session can be resumed by token (ms). */
    public static final long RESUME_GRACE_MS = 60_000L;
}
