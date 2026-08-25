package com.pvz2.server.match;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe registry of active matches, indexed by id and by player. */
public final class MatchRegistry {

    private final ConcurrentHashMap<String, Match> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> userToMatch = new ConcurrentHashMap<>();

    public synchronized Match create(String host, String guest) {
        String id = UUID.randomUUID().toString().replace("-", "");
        Match m = new Match(id, host, guest);
        byId.put(id, m);
        userToMatch.put(host.toLowerCase(), id);
        userToMatch.put(guest.toLowerCase(), id);
        return m;
    }

    public Match get(String matchId) {
        return matchId == null ? null : byId.get(matchId);
    }

    public Match byUser(String username) {
        if (username == null) return null;
        String id = userToMatch.get(username.toLowerCase());
        return id == null ? null : byId.get(id);
    }

    public boolean inMatch(String username) {
        return byUser(username) != null;
    }

    public synchronized void remove(String matchId) {
        Match m = byId.remove(matchId);
        if (m != null) {
            userToMatch.remove(m.host.toLowerCase());
            userToMatch.remove(m.guest.toLowerCase());
        }
    }

    public int count() { return byId.size(); }
}
