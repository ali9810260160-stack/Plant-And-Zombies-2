package com.pvz2.shared.protocol.payload;

/** Generic token-only request (e.g. {@code LOGOUT_REQ}). */
public class TokenRequest {
    public String token;

    public TokenRequest() { }
    public TokenRequest(String token) { this.token = token; }
}
