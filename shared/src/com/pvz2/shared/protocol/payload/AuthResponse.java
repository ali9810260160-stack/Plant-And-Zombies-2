package com.pvz2.shared.protocol.payload;

import com.google.gson.JsonObject;

/**
 * Reply for {@code REGISTER_RES} / {@code LOGIN_RES} / {@code RESUME_RES}.
 *
 * <p>{@link #token} authenticates subsequent requests and enables reconnect.
 * {@link #user} is the full server-side user document (same schema as the
 * client's users.json); the client rebuilds its {@code User} from it.
 */
public class AuthResponse {
    public String token;
    public JsonObject user;
    public boolean admin;

    /**
     * Long-lived "remember me" token (present only when the user asked to stay
     * logged in). The client persists it and replays it via {@code RESUME_REQ}
     * on the next launch for seamless online auto-login. Null otherwise.
     */
    public String rememberToken;

    public AuthResponse() { }

    public AuthResponse(String token, JsonObject user, boolean admin) {
        this.token = token;
        this.user = user;
        this.admin = admin;
    }
}
