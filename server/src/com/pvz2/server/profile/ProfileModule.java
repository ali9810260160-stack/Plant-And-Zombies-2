package com.pvz2.server.profile;

import com.google.gson.JsonObject;
import com.pvz2.server.auth.SessionManager;
import com.pvz2.server.log.Log;
import com.pvz2.server.net.ClientConnection;
import com.pvz2.server.net.Dispatcher;
import com.pvz2.server.store.UserStore;
import com.pvz2.shared.protocol.MessageType;
import com.pvz2.shared.protocol.Packet;
import com.pvz2.shared.protocol.Wire;
import com.pvz2.shared.protocol.payload.OkResponse;
import com.pvz2.shared.protocol.payload.ProfilePayload;
import com.pvz2.shared.protocol.payload.ProfileSaveRequest;
import com.pvz2.shared.protocol.payload.TokenRequest;

/**
 * Server-side persistence of user game data (currencies, progress, plants,
 * greenhouse, settings, …). The client pushes its full user document on save;
 * the server keeps it so accounts are device-independent.
 *
 * <p><b>Anti-tamper:</b> credential and authority fields are never taken from
 * the client — the server always keeps its own {@code username}, {@code passwordHash},
 * {@code securityQuestion}, {@code securityAnswerHash}, {@code rememberToken},
 * {@code stayLoggedIn}, and {@code admin}. A client may only save its own profile
 * (the token's account).
 */
public final class ProfileModule {

    /** Fields the client is never allowed to set via a profile save. */
    private static final String[] PROTECTED = {
        "username", "passwordHash", "securityQuestion", "securityAnswerHash",
        "rememberToken", "stayLoggedIn", "admin"
    };

    private final UserStore store;
    private final SessionManager sessions;

    public ProfileModule(UserStore store, SessionManager sessions) {
        this.store = store;
        this.sessions = sessions;
    }

    public void register(Dispatcher d) {
        d.register(MessageType.SAVE_PROFILE_REQ, this::onSave);
        d.register(MessageType.GET_PROFILE_REQ, this::onGet);
    }

    private void onSave(ClientConnection conn, Packet p) {
        ProfileSaveRequest r = p.payload(Wire.GSON, ProfileSaveRequest.class);
        if (r == null || r.user == null) { conn.sendError(p.id, "BAD_PACKET", "Missing profile."); return; }
        SessionManager.Session s = sessions.byToken(r.token != null ? r.token : conn.getToken());
        if (s == null) { conn.sendError(p.id, "UNAUTHORIZED", "Not authenticated."); return; }

        JsonObject stored = store.get(s.username);
        if (stored == null) { conn.sendError(p.id, "USER_NOT_FOUND", "Account not found."); return; }

        // A client may only save its own account.
        String docName = UserStore.str(r.user, "username");
        if (docName != null && !docName.equalsIgnoreCase(s.username)) {
            conn.sendError(p.id, "FORBIDDEN", "Cannot save another user's profile.");
            return;
        }

        JsonObject merged = mergeProtected(stored, r.user);
        store.put(merged);
        conn.send(Packet.of(Wire.GSON, MessageType.SAVE_PROFILE_RES, p.id, OkResponse.ok()));
    }

    private void onGet(ClientConnection conn, Packet p) {
        TokenRequest r = p.payload(Wire.GSON, TokenRequest.class);
        SessionManager.Session s = sessions.byToken(r != null ? r.token : conn.getToken());
        if (s == null) { conn.sendError(p.id, "UNAUTHORIZED", "Not authenticated."); return; }
        JsonObject doc = store.get(s.username);
        if (doc == null) { conn.sendError(p.id, "USER_NOT_FOUND", "Account not found."); return; }
        conn.send(Packet.of(Wire.GSON, MessageType.GET_PROFILE_RES, p.id, new ProfilePayload(doc)));
    }

    /**
     * Build the doc to store: take the client's game data but force the protected
     * fields to the server's stored values (dropping any the server doesn't have).
     */
    private JsonObject mergeProtected(JsonObject stored, JsonObject incoming) {
        JsonObject out = incoming.deepCopy();
        for (String f : PROTECTED) {
            out.remove(f);
            if (stored.has(f)) out.add(f, stored.get(f).deepCopy());
        }
        return out;
    }
}
