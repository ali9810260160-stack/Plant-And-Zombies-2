package com.pvz2.shared.protocol.payload;

/**
 * Payload for {@code MATCH_RESULT_REQ}: the host reports the finished match's
 * outcome. The server persists stats and notifies both players (each with a
 * win/lose message for their own role) via {@code MATCH_ENDED_EVT}.
 */
public class MatchResultRequest {
    public String token;
    public String matchId;
    public String winnerRole;   // "PLANT" | "ZOMBIE"

    public MatchResultRequest() { }
    public MatchResultRequest(String token, String matchId, String winnerRole) {
        this.token = token;
        this.matchId = matchId;
        this.winnerRole = winnerRole;
    }
}
