package com.pvz2.shared.protocol.payload;

/** Payload for {@code INVITE_REQ}: challenge a specific online user by name. */
public class InviteRequest {
    public String token;
    public String targetUsername;
}
