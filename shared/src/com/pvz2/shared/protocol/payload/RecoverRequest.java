package com.pvz2.shared.protocol.payload;

/** Payload for {@code RECOVER_REQ}: begin password recovery by username+email. */
public class RecoverRequest {
    public String username;
    public String email;
}
