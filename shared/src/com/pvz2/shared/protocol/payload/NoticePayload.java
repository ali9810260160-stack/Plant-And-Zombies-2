package com.pvz2.shared.protocol.payload;

/** Payload for {@code BROADCAST_EVT}: an admin notice/news shown to all clients. */
public class NoticePayload {
    public String message;

    public NoticePayload() { }
    public NoticePayload(String message) { this.message = message; }
}
