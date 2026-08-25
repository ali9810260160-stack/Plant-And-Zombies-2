package com.pvz2.shared.protocol.payload;

/** Payload for {@code KICK_EVT}: the server (admin) is disconnecting this client. */
public class KickEvent {
    public String reason;

    public KickEvent() { }
    public KickEvent(String reason) { this.reason = reason; }
}
