package com.pvz2.shared.protocol.payload;

/**
 * Server push {@code MATCH_ENDED_EVT}: a match (or pending invite) is over —
 * opponent left/disconnected, invite declined, or the game finished.
 * {@link #matchId} may be null for a declined invitation.
 */
public class MatchEndedEvent {
    public String matchId;
    public String reason;

    public MatchEndedEvent() { }
    public MatchEndedEvent(String matchId, String reason) {
        this.matchId = matchId;
        this.reason = reason;
    }
}
