package com.pvz2.shared.protocol.payload;

/** Reply for {@code SUBMIT_SCORE_RES}: the account's My Point after submission. */
public class SubmitScoreResponse {
    public long myPoint;
    public boolean updated;   // true if this submission set a new record

    public SubmitScoreResponse() { }
    public SubmitScoreResponse(long myPoint, boolean updated) {
        this.myPoint = myPoint;
        this.updated = updated;
    }
}
