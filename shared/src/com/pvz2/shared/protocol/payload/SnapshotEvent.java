package com.pvz2.shared.protocol.payload;

import com.google.gson.JsonObject;

/**
 * Payload for {@code SNAPSHOT_EVT}: the host's authoritative game state for one
 * frame, streamed to the guest to render. {@link #state} is a serialized
 * {@code GameStateSnapshot} (opaque to the server, which just relays it).
 */
public class SnapshotEvent {
    public String matchId;
    public int tick;
    public JsonObject state;

    public SnapshotEvent() { }
    public SnapshotEvent(String matchId, int tick, JsonObject state) {
        this.matchId = matchId;
        this.tick = tick;
        this.state = state;
    }
}
