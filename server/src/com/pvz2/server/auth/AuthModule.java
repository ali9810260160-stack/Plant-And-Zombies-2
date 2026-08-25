package com.pvz2.server.auth;

import com.google.gson.JsonObject;
import com.pvz2.server.ServerConfig;
import com.pvz2.server.log.Log;
import com.pvz2.server.net.ClientConnection;
import com.pvz2.server.net.Dispatcher;
import com.pvz2.server.store.UserStore;
import com.pvz2.server.util.HashUtil;
import com.pvz2.server.util.Validators;
import com.pvz2.shared.protocol.MessageType;
import com.pvz2.shared.protocol.Packet;
import com.pvz2.shared.protocol.Wire;
import com.pvz2.shared.protocol.payload.AuthResponse;
import com.pvz2.shared.protocol.payload.LoginRequest;
import com.pvz2.shared.protocol.payload.OkResponse;
import com.pvz2.shared.protocol.payload.RecoverRequest;
import com.pvz2.shared.protocol.payload.RecoverResponse;
import com.pvz2.shared.protocol.payload.RegisterRequest;
import com.pvz2.shared.protocol.payload.ResetPasswordRequest;
import com.pvz2.shared.protocol.payload.ResumeRequest;
import com.pvz2.shared.protocol.payload.SetSecurityRequest;
import com.pvz2.shared.protocol.payload.TokenRequest;

/**
 * Accounts + authentication: register, login, reconnect (resume), logout,
 * security-question setup, and password recovery. Validation mirrors the
 * client's {@code AuthService} but is enforced independently server-side.
 *
 * <p>On success the caller's {@link ClientConnection} is stamped with the token,
 * username, and admin flag so later feature modules can authorize by connection.
 */
public final class AuthModule {

    private final UserStore store;
    private final SessionManager sessions;

    public AuthModule(UserStore store, SessionManager sessions) {
        this.store = store;
        this.sessions = sessions;
    }

    public void register(Dispatcher d) {
        d.register(MessageType.REGISTER_REQ, this::onRegister);
        d.register(MessageType.LOGIN_REQ, this::onLogin);
        d.register(MessageType.RESUME_REQ, this::onResume);
        d.register(MessageType.LOGOUT_REQ, this::onLogout);
        d.register(MessageType.SET_SECURITY_REQ, this::onSetSecurity);
        d.register(MessageType.RECOVER_REQ, this::onRecover);
        d.register(MessageType.RESET_PW_REQ, this::onResetPassword);
        d.onDisconnect((conn, ignored) -> sessions.markDisconnected(conn.connId()));
    }

    // ─── handlers ─────────────────────────────────────────────────────────────

    private void onRegister(ClientConnection conn, Packet p) {
        RegisterRequest r = p.payload(Wire.GSON, RegisterRequest.class);
        if (r == null) { conn.sendError(p.id, "BAD_PACKET", "Missing register data."); return; }
        try {
            Validators.username(r.username);
            Validators.password(r.password);
            if (!r.password.equals(r.confirmPassword))
                throw new Validators.ValidationError("PASSWORD_MISMATCH",
                        "Password and confirmation do not match.");
            Validators.nickname(r.nickname);
            Validators.email(r.email);
        } catch (Validators.ValidationError ve) {
            conn.sendError(p.id, ve.code, ve.getMessage());
            return;
        }
        if (store.exists(r.username)) {
            conn.sendError(p.id, "USERNAME_TAKEN",
                    "Username '" + r.username + "' is already taken.");
            return;
        }
        String hash = HashUtil.sha256(r.password);
        String gender = r.gender == null ? "MALE" : r.gender;
        JsonObject doc = UserStore.newUserDoc(r.username, hash, r.nickname, r.email, gender);
        doc.addProperty("stayLoggedIn", r.stayLoggedIn);
        store.put(doc);
        Log.info("Registered account: " + r.username);
        SessionManager.Session s = openSession(conn, r.username);
        String remember = rememberFor(r.username, r.stayLoggedIn);
        AuthResponse ar = new AuthResponse(s.token, store.get(r.username), s.admin);
        ar.rememberToken = remember;
        conn.send(Packet.of(Wire.GSON, MessageType.REGISTER_RES, p.id, ar));
    }

    private void onLogin(ClientConnection conn, Packet p) {
        LoginRequest r = p.payload(Wire.GSON, LoginRequest.class);
        if (r == null) { conn.sendError(p.id, "BAD_PACKET", "Missing login data."); return; }
        JsonObject doc = store.get(r.username);
        if (doc == null) { conn.sendError(p.id, "USER_NOT_FOUND", "Username not found."); return; }
        if (isBanned(doc)) { conn.sendError(p.id, "BANNED", "This account has been banned."); return; }
        String hash = UserStore.str(doc, "passwordHash");
        if (!HashUtil.verify(r.password, hash)) {
            conn.sendError(p.id, "BAD_CREDENTIALS", "Incorrect password.");
            return;
        }
        // Persist the stay-logged-in preference on the account.
        String canonical = UserStore.str(doc, "username");
        store.update(canonical, o -> o.addProperty("stayLoggedIn", r.stayLoggedIn));
        Log.info("Login: " + canonical + (r.stayLoggedIn ? " (stay)" : ""));
        SessionManager.Session s = openSession(conn, canonical);
        String remember = rememberFor(canonical, r.stayLoggedIn);
        AuthResponse ar = new AuthResponse(s.token, store.get(canonical), s.admin);
        ar.rememberToken = remember;
        conn.send(Packet.of(Wire.GSON, MessageType.LOGIN_RES, p.id, ar));
    }

    private void onResume(ClientConnection conn, Packet p) {
        ResumeRequest r = p.payload(Wire.GSON, ResumeRequest.class);
        String tok = r == null ? null : r.token;
        SessionManager.Session s = tok == null ? null : sessions.resume(tok, conn.connId());
        String rememberEcho = null;
        if (s == null && tok != null) {
            // Not a live session — try it as a persistent remember-me token
            // (seamless online auto-login across app/server restarts).
            JsonObject doc = store.findByRememberToken(tok);
            if (doc != null && isBanned(doc)) {
                conn.sendError(p.id, "BANNED", "This account has been banned.");
                return;
            }
            if (doc != null) {
                String uname = UserStore.str(doc, "username");
                s = openSession(conn, uname);
                rememberEcho = tok; // keep the same remember token on the client
                Log.info("Auto-login via remember-token: " + uname);
            }
        }
        if (s == null) {
            conn.sendError(p.id, "SESSION_EXPIRED", "Session expired; please log in again.");
            return;
        }
        conn.setToken(s.token);
        conn.setUsername(s.username);
        conn.setAdmin(s.admin);
        if (rememberEcho == null) Log.info("Resumed session for " + s.username + " on " + conn.connId());
        AuthResponse ar = new AuthResponse(s.token, store.get(s.username), s.admin);
        ar.rememberToken = rememberEcho;
        conn.send(Packet.of(Wire.GSON, MessageType.RESUME_RES, p.id, ar));
    }

    private void onLogout(ClientConnection conn, Packet p) {
        TokenRequest r = p.payload(Wire.GSON, TokenRequest.class);
        String token = r != null ? r.token : conn.getToken();
        if (token != null) sessions.invalidate(token);
        if (conn.getUsername() != null) {
            store.update(conn.getUsername(), o -> {
                o.addProperty("stayLoggedIn", false);
                o.remove("rememberToken"); // invalidate persistent auto-login
            });
            Log.info("Logout: " + conn.getUsername());
        }
        conn.setToken(null);
        conn.setUsername(null);
        conn.setAdmin(false);
        conn.send(Packet.of(Wire.GSON, MessageType.LOGOUT_RES, p.id, OkResponse.ok()));
    }

    private void onSetSecurity(ClientConnection conn, Packet p) {
        SetSecurityRequest r = p.payload(Wire.GSON, SetSecurityRequest.class);
        if (r == null) { conn.sendError(p.id, "BAD_PACKET", "Missing data."); return; }
        SessionManager.Session s = sessions.byToken(r.token != null ? r.token : conn.getToken());
        if (s == null) { conn.sendError(p.id, "UNAUTHORIZED", "Not authenticated."); return; }
        if (r.answer == null || !r.answer.equals(r.confirmAnswer)) {
            conn.sendError(p.id, "ANSWER_MISMATCH", "Answer and confirmation do not match.");
            return;
        }
        String answerHash = HashUtil.sha256(r.answer.toLowerCase());
        JsonObject updated = store.update(s.username, o -> {
            o.addProperty("securityQuestion", r.question == null ? "" : r.question);
            o.addProperty("securityAnswerHash", answerHash);
        });
        if (updated == null) { conn.sendError(p.id, "USER_NOT_FOUND", "User not found."); return; }
        Log.info("Security question set for " + s.username);
        conn.send(Packet.of(Wire.GSON, MessageType.SET_SECURITY_RES, p.id,
                OkResponse.ok("Security question saved.")));
    }

    private void onRecover(ClientConnection conn, Packet p) {
        RecoverRequest r = p.payload(Wire.GSON, RecoverRequest.class);
        if (r == null) { conn.sendError(p.id, "BAD_PACKET", "Missing data."); return; }
        JsonObject doc = store.get(r.username);
        if (doc == null) { conn.sendError(p.id, "USER_NOT_FOUND", "Username not found."); return; }
        String email = UserStore.str(doc, "email");
        if (email == null || !email.equals(r.email)) {
            conn.sendError(p.id, "EMAIL_MISMATCH", "Email does not match account.");
            return;
        }
        String q = UserStore.str(doc, "securityQuestion");
        if (q == null || q.isEmpty()) {
            conn.sendError(p.id, "NO_SECURITY_Q", "No security question set for this account.");
            return;
        }
        conn.send(Packet.of(Wire.GSON, MessageType.RECOVER_RES, p.id, new RecoverResponse(q)));
    }

    private void onResetPassword(ClientConnection conn, Packet p) {
        ResetPasswordRequest r = p.payload(Wire.GSON, ResetPasswordRequest.class);
        if (r == null) { conn.sendError(p.id, "BAD_PACKET", "Missing data."); return; }
        JsonObject doc = store.get(r.username);
        if (doc == null) { conn.sendError(p.id, "USER_NOT_FOUND", "User not found."); return; }
        String stored = UserStore.str(doc, "securityAnswerHash");
        String given = r.answer == null ? "" : HashUtil.sha256(r.answer.toLowerCase());
        if (stored == null || stored.isEmpty() || !stored.equals(given)) {
            conn.sendError(p.id, "BAD_ANSWER", "Incorrect security answer.");
            return;
        }
        try {
            Validators.password(r.newPassword);
        } catch (Validators.ValidationError ve) {
            conn.sendError(p.id, ve.code, ve.getMessage());
            return;
        }
        String canonical = UserStore.str(doc, "username");
        store.update(canonical, o -> o.addProperty("passwordHash", HashUtil.sha256(r.newPassword)));
        Log.info("Password reset for " + canonical);
        conn.send(Packet.of(Wire.GSON, MessageType.RESET_PW_RES, p.id,
                OkResponse.ok("Password updated.")));
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    /**
     * Issue (or clear) the account's persistent remember-me token based on the
     * stay-logged-in choice, and return it (null if not staying logged in).
     */
    private String rememberFor(String username, boolean stay) {
        if (!stay) {
            store.update(username, o -> o.remove("rememberToken"));
            return null;
        }
        String rt = java.util.UUID.randomUUID().toString().replace("-", "");
        store.update(username, o -> o.addProperty("rememberToken", rt));
        return rt;
    }

    private SessionManager.Session openSession(ClientConnection conn, String username) {
        boolean admin = isAdmin(username);
        SessionManager.Session s = sessions.create(username, admin, conn.connId());
        conn.setToken(s.token);
        conn.setUsername(username);
        conn.setAdmin(admin);
        return s;
    }

    private boolean isBanned(JsonObject doc) {
        return doc != null && doc.has("banned")
                && doc.get("banned").isJsonPrimitive()
                && doc.get("banned").getAsBoolean();
    }

    private boolean isAdmin(String username) {
        if (username == null) return false;
        if (ServerConfig.ADMIN_USERS.contains(username.toLowerCase())) return true;
        JsonObject doc = store.get(username);
        return doc != null && doc.has("admin")
                && doc.get("admin").isJsonPrimitive()
                && doc.get("admin").getAsBoolean();
    }
}
