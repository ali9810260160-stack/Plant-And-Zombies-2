package com.pvz2.shared.protocol.payload;

/**
 * Payload for {@code ERROR_RES}: a machine-readable code plus a human message.
 * The {@code id} of the enclosing packet echoes the failed request's id.
 */
public class ErrorPayload {

    /** Stable, machine-readable error code (e.g. "BAD_CREDENTIALS", "USERNAME_TAKEN"). */
    public String code;

    /** English, user-presentable message. */
    public String message;

    public ErrorPayload() { }

    public ErrorPayload(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
