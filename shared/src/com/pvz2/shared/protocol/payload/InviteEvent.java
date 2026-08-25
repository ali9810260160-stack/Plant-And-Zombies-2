package com.pvz2.shared.protocol.payload;

/** Server push {@code INVITE_EVT}: someone is challenging the recipient. */
public class InviteEvent {
    public String inviteId;
    public String fromUsername;

    public InviteEvent() { }
    public InviteEvent(String inviteId, String fromUsername) {
        this.inviteId = inviteId;
        this.fromUsername = fromUsername;
    }
}
