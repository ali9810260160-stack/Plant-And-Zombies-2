package com.pvz2.server.match;

import com.pvz2.server.auth.SessionManager;
import com.pvz2.server.log.Log;
import com.pvz2.server.net.ClientConnection;
import com.pvz2.server.net.Dispatcher;
import com.pvz2.server.net.GameServer;
import com.pvz2.shared.protocol.MessageType;
import com.pvz2.shared.protocol.Packet;
import com.pvz2.shared.protocol.Wire;
import com.pvz2.shared.protocol.payload.InviteAnswerRequest;
import com.pvz2.shared.protocol.payload.InviteEvent;
import com.pvz2.shared.protocol.payload.InviteRequest;
import com.pvz2.shared.protocol.payload.MatchEndedEvent;
import com.pvz2.shared.protocol.payload.MatchFoundEvent;
import com.pvz2.shared.protocol.payload.OkResponse;
import com.pvz2.shared.protocol.payload.OnlineListResponse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Opponent selection for the 2-player I-Zombie game (doc §"بازی چندنفره"):
 *
 * <ul>
 *   <li><b>Invite a specific user</b> — {@code INVITE_REQ}: errors if the target
 *       is invalid/offline/busy; otherwise the target gets an {@code INVITE_EVT}
 *       pop-up to accept/reject. On accept both get {@code MATCH_FOUND_EVT}.</li>
 *   <li><b>Random match</b> — {@code QUEUE_REQ}: pairs with a waiting player, or
 *       waits for one.</li>
 * </ul>
 *
 * <p>Role assignment: the inviter / first-waiting player is the <b>host</b>
 * (PLANT, authoritative simulator); the other is the <b>guest</b> (ZOMBIE).
 */
public final class MatchmakingModule {

    private static final class Invite {
        final String from, to;
        Invite(String from, String to) { this.from = from; this.to = to; }
    }

    private final GameServer server;
    private final SessionManager sessions;
    private final MatchRegistry matches;

    private final Object lock = new Object();
    private final Deque<String> waiting = new ArrayDeque<>();       // random-match queue
    private final Map<String, Invite> invites = new HashMap<>();    // inviteId → invite

    public MatchmakingModule(GameServer server, SessionManager sessions, MatchRegistry matches) {
        this.server = server;
        this.sessions = sessions;
        this.matches = matches;
    }

    public void register(Dispatcher d) {
        d.register(MessageType.ONLINE_LIST_REQ, this::onOnlineList);
        d.register(MessageType.INVITE_REQ, this::onInvite);
        d.register(MessageType.INVITE_ANSWER_REQ, this::onInviteAnswer);
        d.register(MessageType.QUEUE_REQ, this::onQueue);
        d.register(MessageType.QUEUE_CANCEL_REQ, this::onQueueCancel);
        d.register(MessageType.MATCH_LEAVE_REQ, this::onLeave);
        d.onDisconnect((conn, ignored) -> onDisconnect(conn));
    }

    // ─── handlers ─────────────────────────────────────────────────────────────

    private void onOnlineList(ClientConnection conn, Packet p) {
        if (!auth(conn, p)) return;
        List<String> list = new ArrayList<>();
        for (String u : server.onlineUsernames()) {
            if (!u.equalsIgnoreCase(conn.getUsername())) list.add(u);
        }
        conn.send(Packet.of(Wire.GSON, MessageType.ONLINE_LIST_RES, p.id,
                new OnlineListResponse(list)));
    }

    private void onInvite(ClientConnection conn, Packet p) {
        if (!auth(conn, p)) return;
        InviteRequest r = p.payload(Wire.GSON, InviteRequest.class);
        String target = r != null ? r.targetUsername : null;
        String me = conn.getUsername();
        if (target == null || target.isEmpty()) {
            conn.sendError(p.id, "BAD_TARGET", "No opponent specified."); return;
        }
        if (target.equalsIgnoreCase(me)) {
            conn.sendError(p.id, "SELF", "You cannot invite yourself."); return;
        }
        ClientConnection tc = server.byUsername(target);
        if (tc == null || !tc.isAuthenticated()) {
            conn.sendError(p.id, "USER_OFFLINE", "User '" + target + "' is not online."); return;
        }
        if (matches.inMatch(me)) { conn.sendError(p.id, "BUSY", "You are already in a match."); return; }
        if (matches.inMatch(target)) { conn.sendError(p.id, "TARGET_BUSY", "That player is busy."); return; }

        String inviteId = UUID.randomUUID().toString().replace("-", "");
        synchronized (lock) { invites.put(inviteId, new Invite(me, tc.getUsername())); }
        tc.send(Packet.of(Wire.GSON, MessageType.INVITE_EVT, null,
                new InviteEvent(inviteId, me)));
        Log.info("Invite " + me + " -> " + target + " (" + inviteId + ")");
        conn.send(Packet.of(Wire.GSON, MessageType.INVITE_RES, p.id,
                OkResponse.ok("Invitation sent to " + target + ".")));
    }

    private void onInviteAnswer(ClientConnection conn, Packet p) {
        if (!auth(conn, p)) return;
        InviteAnswerRequest r = p.payload(Wire.GSON, InviteAnswerRequest.class);
        if (r == null || r.inviteId == null) { conn.sendError(p.id, "BAD_PACKET", "Missing answer."); return; }
        Invite inv;
        synchronized (lock) { inv = invites.remove(r.inviteId); }
        if (inv == null) { conn.sendError(p.id, "INVITE_GONE", "Invitation no longer valid."); return; }
        if (!inv.to.equalsIgnoreCase(conn.getUsername())) {
            conn.sendError(p.id, "FORBIDDEN", "Not your invitation."); return;
        }
        conn.send(Packet.of(Wire.GSON, MessageType.INVITE_RES, p.id, OkResponse.ok()));

        if (!r.accept) {
            pushTo(inv.from, MessageType.MATCH_ENDED_EVT,
                    new MatchEndedEvent(null, inv.to + " declined your invitation."));
            Log.info("Invite declined: " + inv.to + " -> " + inv.from);
            return;
        }
        // Accept: both must still be online and free.
        if (server.byUsername(inv.from) == null) {
            conn.sendError(p.id, "USER_OFFLINE", "The inviter went offline."); return;
        }
        if (matches.inMatch(inv.from) || matches.inMatch(inv.to)) {
            conn.sendError(p.id, "BUSY", "A player is already in a match."); return;
        }
        startMatch(inv.from, inv.to);
    }

    private void onQueue(ClientConnection conn, Packet p) {
        if (!auth(conn, p)) return;
        String me = conn.getUsername();
        if (matches.inMatch(me)) { conn.sendError(p.id, "BUSY", "You are already in a match."); return; }
        String opponent = null;
        synchronized (lock) {
            waiting.remove(me); // avoid duplicates
            // Find the first still-online waiter that isn't us.
            while (!waiting.isEmpty()) {
                String head = waiting.peekFirst();
                if (head.equalsIgnoreCase(me) || server.byUsername(head) == null || matches.inMatch(head)) {
                    waiting.pollFirst();
                    continue;
                }
                opponent = waiting.pollFirst();
                break;
            }
            if (opponent == null) waiting.addLast(me);
        }
        conn.send(Packet.of(Wire.GSON, MessageType.QUEUE_RES, p.id,
                OkResponse.ok(opponent == null ? "Searching for an opponent..." : "Match found!")));
        if (opponent != null) startMatch(opponent, me); // first-waiter hosts (PLANT)
    }

    private void onQueueCancel(ClientConnection conn, Packet p) {
        if (!auth(conn, p)) return;
        synchronized (lock) { waiting.remove(conn.getUsername()); }
        conn.send(Packet.of(Wire.GSON, MessageType.QUEUE_RES, p.id, OkResponse.ok("Left the queue.")));
    }

    private void onLeave(ClientConnection conn, Packet p) {
        if (!auth(conn, p)) return;
        endMatchOf(conn.getUsername(), conn.getUsername() + " left the match.");
        conn.send(Packet.of(Wire.GSON, MessageType.MATCH_LEAVE_REQ, p.id, OkResponse.ok()));
    }

    private void onDisconnect(ClientConnection conn) {
        String me = conn.getUsername();
        if (me == null) return;
        synchronized (lock) {
            waiting.remove(me);
            invites.values().removeIf(inv -> {
                if (inv.from.equalsIgnoreCase(me) || inv.to.equalsIgnoreCase(me)) {
                    String other = inv.from.equalsIgnoreCase(me) ? inv.to : inv.from;
                    pushTo(other, MessageType.MATCH_ENDED_EVT,
                            new MatchEndedEvent(null, me + " is no longer available."));
                    return true;
                }
                return false;
            });
        }
        endMatchOf(me, "Opponent disconnected.");
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private void startMatch(String host, String guest) {
        Match m = matches.create(host, guest);
        m.state = Match.State.STARTING;
        pushTo(host, MessageType.MATCH_FOUND_EVT,
                new MatchFoundEvent(m.matchId, guest, "PLANT", true));
        pushTo(guest, MessageType.MATCH_FOUND_EVT,
                new MatchFoundEvent(m.matchId, host, "ZOMBIE", false));
        Log.info("Match started " + m.matchId + ": " + host + " (PLANT/host) vs " + guest + " (ZOMBIE)");
    }

    private void endMatchOf(String username, String reason) {
        Match m = matches.byUser(username);
        if (m == null) return;
        String other = m.opponentOf(username);
        matches.remove(m.matchId);
        pushTo(other, MessageType.MATCH_ENDED_EVT, new MatchEndedEvent(m.matchId, reason));
        Log.info("Match ended " + m.matchId + ": " + reason);
    }

    private void pushTo(String username, String type, Object payload) {
        ClientConnection c = server.byUsername(username);
        if (c != null) c.send(Packet.of(Wire.GSON, type, null, payload));
    }

    /** Ensure the connection is authenticated; replies with an error if not. */
    private boolean auth(ClientConnection conn, Packet p) {
        if (conn.isAuthenticated()) return true;
        conn.sendError(p.id, "UNAUTHORIZED", "Log in first.");
        return false;
    }
}
