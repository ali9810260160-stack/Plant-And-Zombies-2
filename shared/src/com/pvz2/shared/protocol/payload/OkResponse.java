package com.pvz2.shared.protocol.payload;

/** Generic success/ack reply with an optional message. */
public class OkResponse {
    public boolean ok;
    public String message;

    public OkResponse() { }
    public OkResponse(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }
    public static OkResponse ok() { return new OkResponse(true, null); }
    public static OkResponse ok(String m) { return new OkResponse(true, m); }
}
