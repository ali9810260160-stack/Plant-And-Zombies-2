package com.pvz2.server.match;

import com.pvz2.server.auth.SessionManager;
import com.pvz2.server.log.Log;
import com.pvz2.server.net.ClientConnection;
import com.pvz2.server.net.Dispatcher;
import com.pvz2.server.net.GameServer;
import com.pvz2.server.store.UserStore;
import com.pvz2.shared.protocol.MessageType;
import com.pvz2.shared.protocol.Packet;
import com.pvz2.shared.protocol.Wire;
import com.pvz2.shared.protocol.payload.MatchEndedEvent;
import com.pvz2.shared.protocol.payload.MatchResultRequest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * In-match relay for the 2-player game (host-authoritative snapshot model):
 *
 * <ul>
 *   <li>{@code MATCH_READY_REQ} — both clients signal their game screen is up;
 *       when both are ready the server pushes {@code MATCH_START_EVT} so the host
 *       starts streaming and the guest starts rendering in sync.</li>
 *   <li>{@code INPUT_EVT} / {@code SNAPSHOT_EVT} — forwarded verbatim to the
 *       sender's opponent (the server never parses the game state).</li>
 *   <li>{@code MATCH_RESULT_REQ} — the host reports the winner; the server
 *       notifies both players and persists a game-played stat.</li>
 * </ul>
 */
public final class MatchRelayModule {

    private final GameServer server;
    private final SessionManager sessions;
    private final MatchRegistry matches;
    private final UserStore store;

    private final Object lock = new Object();
    private final Map<String, Set<String>> ready = new HashMap<>(); // matchId → ready usernames

    public MatchRelayModule(GameServer server, SessionManager sessions,
                            MatchRegistry matches, UserStore store) {
        this.server = server;
        this.sessions = sessions;
        this.matches = matches;
        this.store = store;
    }

    public void register(Dispatcher d) {
        d.register(MessageType.MATCH_READY_REQ, this::onReady);
        d.register(MessageType.INPUT_EVT, this::onInput);
        d.register(MessageType.SNAPSHOT_EVT, this::onSnapshot);
        d.register(MessageType.REACTION_EVT, this::onReaction);
        d.register(MessageType.MATCH_RESULT_REQ, this::onResult);
    }

    private void onReady(ClientConnection conn, Packet p) {
        String me = conn.getUsername();
        Match m = matches.byUser(me);
        if (m == null) return;
        boolean both;
        synchronized (lock) {
            Set<String> s = ready.computeIfAbsent(m.matchId, k -> new HashSet<>());
            s.add(me.toLowerCase());
            both = s.contains(m.host.toLowerCase()) && s.contains(m.guest.toLowerCase());
        }
        if (both) {
            m.state = Match.State.IN_PROGRESS;
            pushTo(m.host, new Packet(MessageType.MATCH_START_EVT, null, null));
            pushTo(m.guest, new Packet(MessageType.MATCH_START_EVT, null, null));
            Log.info("Match " + m.matchId + " started (both ready)");
        }
    }

    /** Forward a player action to the opponent (guest → host). */
    private void onInput(ClientConnection conn, Packet p) {
        forwardToOpponent(conn, p);
    }

    /** Forward the host's snapshot to the guest. */
    private void onSnapshot(ClientConnection conn, Packet p) {
        forwardToOpponent(conn, p);
    }

    /** Forward an in-match reaction (text/emoji/sticker) to the opponent. */
    private void onReaction(ClientConnection conn, Packet p) {
        forwardToOpponent(conn, p);
    }

    private void forwardToOpponent(ClientConnection conn, Packet p) {
        Match m = matches.byUser(conn.getUsername());
        if (m == null) return;
        String opp = m.opponentOf(conn.getUsername());
        pushTo(opp, new Packet(p.type, null, p.data)); // relay verbatim, as an event
    }

    private void onResult(ClientConnection conn, Packet p) {
        MatchResultRequest r = p.payload(Wire.GSON, MatchResultRequest.class);
        Match m = matches.byUser(conn.getUsername());
        if (m == null || r == null) return;
        // Only the host is authoritative for the result.
        if (!m.host.equalsIgnoreCase(conn.getUsername())) return;

        String winnerRole = r.winnerRole != null ? r.winnerRole : "PLANT";
        m.state = Match.State.ENDED;
        notifyResult(m.host, m.roleOf(m.host), winnerRole);
        notifyResult(m.guest, m.roleOf(m.guest), winnerRole);
        // Persist a game-played stat for both.
        bumpGamesPlayed(m.host);
        bumpGamesPlayed(m.guest);
        Log.info("Match " + m.matchId + " result: " + winnerRole + " wins");
        cleanup(m.matchId);
    }

    private void notifyResult(String username, String myRole, String winnerRole) {
        boolean won = myRole.equalsIgnoreCase(winnerRole);
        String reason = won ? "You won the match!" : "You lost the match.";
        pushTo(username, Packet.of(Wire.GSON, MessageType.MATCH_ENDED_EVT, null,
                new MatchEndedEvent(null, reason)));
    }

    private void bumpGamesPlayed(String username) {
        store.update(username, o -> {
            int gp = UserStore.getInt(o, "gamesPlayed", 0);
            o.addProperty("gamesPlayed", gp + 1);
        });
    }

    private void cleanup(String matchId) {
        synchronized (lock) { ready.remove(matchId); }
        matches.remove(matchId);
    }

    private void pushTo(String username, Packet p) {
        ClientConnection c = server.byUsername(username);
        if (c != null) c.send(p);
    }
}
