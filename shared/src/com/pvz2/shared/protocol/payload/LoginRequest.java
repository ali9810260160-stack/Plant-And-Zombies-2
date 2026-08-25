package com.pvz2.shared.protocol.payload;

/** Payload for {@code LOGIN_REQ}. */
public class LoginRequest {
    public String username;
    public String password;
    public boolean stayLoggedIn;
}
