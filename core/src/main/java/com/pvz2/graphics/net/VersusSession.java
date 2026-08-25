package com.pvz2.graphics.net;

import com.pvz2.graphics.GameStateSnapshot;
import com.pvz2.shared.protocol.MessageType;
import com.pvz2.shared.protocol.Packet;
import com.pvz2.shared.protocol.Wire;
import com.pvz2.shared.protocol.payload.MatchInputEvent;
import com.pvz2.shared.protocol.payload.MatchResultRequest;
import com.pvz2.shared.protocol.payload.SnapshotEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side driver for one 2-player VERSUS match (host-authoritative snapshot
 * model). Created on {@code MATCH_FOUND_EVT} and handed to {@code GameScreen}.
 *
 * <p>All callbacks run on the render thread (dispatched by {@link NetClient#update()}),
 * so the queues/fields need no extra synchronization.
 *
 * <ul>
 *   <li><b>HOST</b> (PLANT): runs the real sim; each tick applies queued guest
 *       inputs and streams a snapshot; reports the winner once decided.</li>
 *   <li><b>GUEST</b> (ZOMBIE): renders the latest received snapshot into a mirror
 *       session and sends its zombie placements as inputs.</li>
 * </ul>
 */
public final class VersusSession {

    public enum Role { HOST, GUEST }

    private final Role role;
    private final String matchId;

    private volatile boolean started;   // both players ready (MATCH_START_EVT)
    private volatile boolean ended;

    // GUEST: latest snapshot to render + the mirror reconciler
    private volatile GameStateSnapshot latest;
    private final SnapshotApplier applier = new SnapshotApplier();

    // HOST: inbound guest inputs, applied on the next tick
    private final List<MatchInputEvent> inputs = new ArrayList<>();
    private boolean resultReported;
    private int streamTick;
    private long lastStreamMs;
    /** حداقل فاصله‌ی بین snapshotها (میلی‌ثانیه) — تقریباً ۲۵ بار در ثانیه. */
    private static final long STREAM_INTERVAL_MS = 40;

    // Retained listener references so end() removes exactly our own (the global
    // MultiplayerService also listens on MATCH_ENDED_EVT — must not remove that).
    private java.util.function.Consumer<Packet> lStart, lEnded, lSnapshot, lInput, lReaction;

    /** Called (render thread) when the opponent sends a reaction. */
    private java.util.function.Consumer<
            com.pvz2.shared.protocol.payload.ReactionEvent> onReaction;
    public void setOnReaction(
            java.util.function.Consumer<com.pvz2.shared.protocol.payload.ReactionEvent> c) {
        this.onReaction = c;
    }

    public VersusSession(Role role, String matchId) {
        this.role = role;
        this.matchId = matchId;
    }

    public boolean isHost() { return role == Role.HOST; }
    public boolean isGuest() { return role == Role.GUEST; }
    public boolean isStarted() { return started; }
    public boolean isEnded() { return ended; }
    public SnapshotApplier applier() { return applier; }
    public GameStateSnapshot latestSnapshot() { return latest; }

    /** Register listeners and tell the server this side's screen is up. */
    public void begin() {
        NetClient n = NetClient.get();
        lStart = p -> started = true;
        lEnded = p -> ended = true;
        n.on(MessageType.MATCH_START_EVT, lStart);
        n.on(MessageType.MATCH_ENDED_EVT, lEnded);
        if (isGuest()) { lSnapshot = this::onSnapshot; n.on(MessageType.SNAPSHOT_EVT, lSnapshot); }
        if (isHost())  { lInput = this::onInput; n.on(MessageType.INPUT_EVT, lInput); }
        lReaction = this::onReactionPacket;
        n.on(MessageType.REACTION_EVT, lReaction);
        n.send(MessageType.MATCH_READY_REQ, null);
    }

    /** Stop listening (screen disposed / match over) — removes only our listeners. */
    public void end() {
        NetClient n = NetClient.get();
        if (lStart != null)    n.off(MessageType.MATCH_START_EVT, lStart);
        if (lEnded != null)    n.off(MessageType.MATCH_ENDED_EVT, lEnded);
        if (lSnapshot != null) n.off(MessageType.SNAPSHOT_EVT, lSnapshot);
        if (lInput != null)    n.off(MessageType.INPUT_EVT, lInput);
        if (lReaction != null) n.off(MessageType.REACTION_EVT, lReaction);
        applier.reset();
    }

    /** Send a reaction (text/emoji/sticker) to the opponent. */
    public void sendReaction(String kind, String value) {
        NetClient.get().send(MessageType.REACTION_EVT,
                new com.pvz2.shared.protocol.payload.ReactionEvent(matchId, kind, value));
    }

    private void onReactionPacket(Packet p) {
        com.pvz2.shared.protocol.payload.ReactionEvent re =
                p.payload(Wire.GSON, com.pvz2.shared.protocol.payload.ReactionEvent.class);
        if (re != null && onReaction != null) onReaction.accept(re);
    }

    // ─── HOST ──────────────────────────────────────────────────────────────────

    /** Drain guest inputs to apply this tick (host). */
    public List<MatchInputEvent> drainInputs() {
        if (inputs.isEmpty()) return java.util.Collections.emptyList();
        List<MatchInputEvent> out = new ArrayList<>(inputs);
        inputs.clear();
        return out;
    }

    /** Stream the host's authoritative snapshot to the guest (throttled). */
    public void stream(GameStateSnapshot snap) {
        if (snap == null || !started) return;
        long now = System.currentTimeMillis();
        if (now - lastStreamMs < STREAM_INTERVAL_MS) return;
        lastStreamMs = now;
        SnapshotEvent se = new SnapshotEvent(matchId, streamTick++,
                Wire.GSON.toJsonTree(snap).getAsJsonObject());
        NetClient.get().send(MessageType.SNAPSHOT_EVT, se);
    }

    /** Report the winner to the server (once). */
    public void reportResult(String winnerRole) {
        if (resultReported) return;
        resultReported = true;
        NetClient.get().send(MessageType.MATCH_RESULT_REQ,
                new MatchResultRequest(NetClient.get().token(), matchId, winnerRole));
    }

    private void onInput(Packet p) {
        MatchInputEvent e = p.payload(Wire.GSON, MatchInputEvent.class);
        if (e != null) inputs.add(e);
    }

    // ─── GUEST ─────────────────────────────────────────────────────────────────

    private void onSnapshot(Packet p) {
        SnapshotEvent se = p.payload(Wire.GSON, SnapshotEvent.class);
        if (se != null && se.state != null) {
            latest = Wire.GSON.fromJson(se.state, GameStateSnapshot.class);
        }
    }

    /** Guest sends a zombie placement to the host (via the server). */
    public void sendPlaceZombie(String zombieType, int col, int row) {
        NetClient.get().send(MessageType.INPUT_EVT,
                new MatchInputEvent(matchId, "PLACE_ZOMBIE", zombieType, col, row));
    }

    /** Tell the server we are leaving the match. */
    public void leave() {
        NetClient.get().send(MessageType.MATCH_LEAVE_REQ,
                new com.pvz2.shared.protocol.payload.TokenRequest(NetClient.get().token()));
    }
}
