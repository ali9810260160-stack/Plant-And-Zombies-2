package com.pvz2.shared.protocol.payload;

/** Payload for {@code INVITE_ANSWER_REQ}: the invitee accepts or rejects. */
public class InviteAnswerRequest {
    public String token;
    public String inviteId;
    public boolean accept;
}
