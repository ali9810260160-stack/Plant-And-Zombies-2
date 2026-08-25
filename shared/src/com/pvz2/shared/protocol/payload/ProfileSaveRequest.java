package com.pvz2.shared.protocol.payload;

import com.google.gson.JsonObject;

/**
 * Payload for {@code SAVE_PROFILE_REQ}: the client pushes its current user
 * document so game data (currencies, progress, plants, greenhouse, settings, …)
 * is persisted server-side and stays device-independent.
 *
 * <p>The server ignores credential/authority fields in {@link #user} and keeps
 * its own (username, passwordHash, security answer, remember token, admin).
 */
public class ProfileSaveRequest {
    public String token;
    public JsonObject user;
}
