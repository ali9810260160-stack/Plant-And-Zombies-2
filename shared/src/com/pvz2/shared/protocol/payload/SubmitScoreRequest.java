package com.pvz2.shared.protocol.payload;

/**
 * Payload for {@code SUBMIT_SCORE_REQ}: a networked SCORED-game result. The
 * server updates the account's My Point only if this score beats the record.
 */
public class SubmitScoreRequest {
    public String token;
    public long score;

    public SubmitScoreRequest() { }
    public SubmitScoreRequest(String token, long score) {
        this.token = token;
        this.score = score;
    }
}
