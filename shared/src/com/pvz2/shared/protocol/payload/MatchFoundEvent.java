package com.pvz2.shared.protocol.payload;

/**
 * Server push {@code MATCH_FOUND_EVT}: a 2-player match is ready.
 *
 * <p>{@link #role} is "PLANT" or "ZOMBIE". {@link #host} is true for the client
 * that runs the authoritative simulation and streams snapshots (the plant side).
 */
public class MatchFoundEvent {
    public String matchId;
    public String opponentUsername;
    public String role;      // "PLANT" | "ZOMBIE"
    public boolean host;     // true => this client simulates and streams snapshots

    public MatchFoundEvent() { }
    public MatchFoundEvent(String matchId, String opponentUsername, String role, boolean host) {
        this.matchId = matchId;
        this.opponentUsername = opponentUsername;
        this.role = role;
        this.host = host;
    }
}
