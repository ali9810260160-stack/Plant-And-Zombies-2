package com.pvz2.shared.protocol.payload;

/**
 * Payload for {@code HELLO_EVT}: the server's greeting sent right after a
 * client connects, so the client can verify protocol compatibility before
 * attempting to authenticate.
 */
public class HelloPayload {

    public int protocolVersion;
    public String serverName;
    public long serverTime;

    public HelloPayload() { }

    public HelloPayload(int protocolVersion, String serverName, long serverTime) {
        this.protocolVersion = protocolVersion;
        this.serverName = serverName;
        this.serverTime = serverTime;
    }
}
