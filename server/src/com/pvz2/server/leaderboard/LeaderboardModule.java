package com.pvz2.server.leaderboard;

import com.google.gson.JsonObject;
import com.pvz2.server.auth.SessionManager;
import com.pvz2.server.log.Log;
import com.pvz2.server.net.ClientConnection;
import com.pvz2.server.net.Dispatcher;
import com.pvz2.server.store.UserStore;
import com.pvz2.shared.protocol.MessageType;
import com.pvz2.shared.protocol.Packet;
import com.pvz2.shared.protocol.Wire;
import com.pvz2.shared.protocol.payload.LeaderboardEntryDTO;
import com.pvz2.shared.protocol.payload.LeaderboardRequest;
import com.pvz2.shared.protocol.payload.LeaderboardResponse;
import com.pvz2.shared.protocol.payload.SubmitScoreRequest;
import com.pvz2.shared.protocol.payload.SubmitScoreResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Server-sourced leaderboard + networked SCORED-game score submission.
 *
 * <p>The leaderboard is built live from the user store, so it reflects the
 * latest data. The "My Point" column is the networked scored best ({@code
 * myPoint}); players who never played it have no My Point (never a legacy value).
 */
public final class LeaderboardModule {

    private final UserStore store;
    private final SessionManager sessions;

    public LeaderboardModule(UserStore store, SessionManager sessions) {
        this.store = store;
        this.sessions = sessions;
    }

    public void register(Dispatcher d) {
        d.register(MessageType.LEADERBOARD_REQ, this::onLeaderboard);
        d.register(MessageType.SUBMIT_SCORE_REQ, this::onSubmitScore);
    }

    private void onLeaderboard(ClientConnection conn, Packet p) {
        LeaderboardRequest r = p.payload(Wire.GSON, LeaderboardRequest.class);
        String key = r != null && r.sortKey != null ? r.sortKey : "meopoint";
        boolean asc = r != null && r.ascending;

        List<LeaderboardEntryDTO> rows = new ArrayList<>();
        for (JsonObject o : store.all()) {
            LeaderboardEntryDTO e = new LeaderboardEntryDTO();
            e.username = UserStore.str(o, "username");
            e.nickname = UserStore.str(o, "nickname");
            String last = UserStore.str(o, "lastReachedLevel");
            e.lastLevel = (last == null || last.isEmpty()) ? "-" : last;
            e.minigameCount = UserStore.getInt(o, "minigamesCompleted", 0);
            e.dailyQuestCount = UserStore.getInt(o, "dailyQuestsCompleted", 0);
            e.questCount = UserStore.getInt(o, "regularQuestsCompleted", 0);
            e.hasMyPoint = UserStore.has(o, "myPoint");
            e.myPoint = e.hasMyPoint ? UserStore.getLong(o, "myPoint", 0) : 0;
            rows.add(e);
        }
        rows.sort(comparator(key, asc));
        conn.send(Packet.of(Wire.GSON, MessageType.LEADERBOARD_RES, p.id,
                new LeaderboardResponse(rows)));
    }

    private void onSubmitScore(ClientConnection conn, Packet p) {
        SubmitScoreRequest r = p.payload(Wire.GSON, SubmitScoreRequest.class);
        if (r == null) { conn.sendError(p.id, "BAD_PACKET", "Missing score."); return; }
        SessionManager.Session s = sessions.byToken(r.token != null ? r.token : conn.getToken());
        if (s == null) { conn.sendError(p.id, "UNAUTHORIZED", "Not authenticated."); return; }

        final long score = Math.max(0, r.score);
        final boolean[] updated = {false};
        JsonObject doc = store.update(s.username, o -> {
            long cur = UserStore.has(o, "myPoint") ? UserStore.getLong(o, "myPoint", 0) : -1;
            if (score > cur) { o.addProperty("myPoint", score); updated[0] = true; }
        });
        long best = doc != null ? UserStore.getLong(doc, "myPoint", score) : score;
        if (updated[0]) Log.info("My Point updated for " + s.username + " -> " + best);
        conn.send(Packet.of(Wire.GSON, MessageType.SUBMIT_SCORE_RES, p.id,
                new SubmitScoreResponse(best, updated[0])));
    }

    private Comparator<LeaderboardEntryDTO> comparator(String key, boolean asc) {
        Comparator<LeaderboardEntryDTO> c;
        switch (key) {
            case "minigame":  c = Comparator.comparingInt(e -> e.minigameCount); break;
            case "daily":     c = Comparator.comparingInt(e -> e.dailyQuestCount); break;
            case "quest":     c = Comparator.comparingInt(e -> e.questCount); break;
            case "lastLevel": c = Comparator.comparing(e -> e.lastLevel == null ? "" : e.lastLevel); break;
            case "meopoint":  c = Comparator.comparingLong(e -> e.hasMyPoint ? e.myPoint : Long.MIN_VALUE); break;
            default:          c = Comparator.comparing(e -> e.username == null ? "" : e.username.toLowerCase()); break;
        }
        return asc ? c : c.reversed();
    }
}
