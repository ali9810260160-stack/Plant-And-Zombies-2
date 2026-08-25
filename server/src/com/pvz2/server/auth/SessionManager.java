package com.pvz2.server.auth;

import com.pvz2.server.ServerConfig;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Issues and tracks authenticated sessions (tokens).
 *
 * <p>One live session per account: a fresh login supersedes the previous token.
 * When a connection drops, its session is kept for {@link ServerConfig#RESUME_GRACE_MS}
 * so the client can {@code RESUME} after a brief network hiccup; after the grace
 * window it is purged. Thread-safe.
 */
public final class SessionManager {

    /** One authenticated session. */
    public static final class Session {
        public final String token;
        public final String username;
        public final boolean admin;
        volatile String connId;          // current connection, null while disconnected
        volatile long lastSeen;
        volatile long disconnectedAt;    // 0 while connected

        Session(String token, String username, boolean admin, String connId) {
            this.token = token;
            this.username = username;
            this.admin = admin;
            this.connId = connId;
            this.lastSeen = System.currentTimeMillis();
        }
        public boolean isConnected() { return connId != null; }
    }

    private final ConcurrentHashMap<String, Session> byToken = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Session> byUser = new ConcurrentHashMap<>();

    /** Create a new session, replacing any prior one for the same user. */
    public synchronized Session create(String username, boolean admin, String connId) {
        Session prev = byUser.get(username.toLowerCase());
        if (prev != null) byToken.remove(prev.token);
        String token = UUID.randomUUID().toString().replace("-", "");
        Session s = new Session(token, username, admin, connId);
        byToken.put(token, s);
        byUser.put(username.toLowerCase(), s);
        return s;
    }

    /** Look up a live session by token (purging it if the grace window elapsed). */
    public Session byToken(String token) {
        if (token == null) return null;
        Session s = byToken.get(token);
        if (s == null) return null;
        if (!s.isConnected()
                && System.currentTimeMillis() - s.disconnectedAt > ServerConfig.RESUME_GRACE_MS) {
            invalidate(token);
            return null;
        }
        return s;
    }

    /** Rebind a disconnected session to a new connection (reconnect). */
    public synchronized Session resume(String token, String newConnId) {
        Session s = byToken(token);
        if (s == null) return null;
        s.connId = newConnId;
        s.disconnectedAt = 0;
        s.lastSeen = System.currentTimeMillis();
        return s;
    }

    /** Mark the session on {@code connId} as disconnected (starts the grace timer). */
    public synchronized void markDisconnected(String connId) {
        if (connId == null) return;
        for (Session s : byToken.values()) {
            if (connId.equals(s.connId)) {
                s.connId = null;
                s.disconnectedAt = System.currentTimeMillis();
            }
        }
    }

    public synchronized void invalidate(String token) {
        Session s = byToken.remove(token);
        if (s != null) byUser.remove(s.username.toLowerCase(), s);
    }

    public void touch(String token) {
        Session s = byToken.get(token);
        if (s != null) s.lastSeen = System.currentTimeMillis();
    }

    public int activeCount() { return byToken.size(); }
}
