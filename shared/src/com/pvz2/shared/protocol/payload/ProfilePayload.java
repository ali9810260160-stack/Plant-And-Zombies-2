package com.pvz2.shared.protocol.payload;

import com.google.gson.JsonObject;

/** Reply for {@code GET_PROFILE_RES}: the server's current user document. */
public class ProfilePayload {
    public JsonObject user;

    public ProfilePayload() { }
    public ProfilePayload(JsonObject user) { this.user = user; }
}
