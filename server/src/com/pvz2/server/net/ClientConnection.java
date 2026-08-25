package com.pvz2.server.net;

import com.pvz2.server.log.Log;
import com.pvz2.shared.protocol.MessageType;
import com.pvz2.shared.protocol.Packet;
import com.pvz2.shared.protocol.Wire;
import com.pvz2.shared.protocol.payload.ErrorPayload;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;

/**
 * One connected client, served by its own reader thread (thread-per-client).
 *
 * <p>Reads framed {@link Packet}s in a loop and hands each to the
 * {@link GameServer}'s {@link Dispatcher}. Writes are serialized through a
 * private lock so the dispatcher, heartbeat, and broadcast threads can all send
 * safely. Application state attached later (authenticated user, current room)
 * lives here as mutable fields guarded by the server's higher-level locks.
 */
public final class ClientConnection implements Runnable {

    private static final AtomicLong SEQ = new AtomicLong(1);

    private final String connId;
    private final Socket socket;
    private final GameServer server;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final Object writeLock = new Object();

    private volatile boolean open = true;
    private volatile long lastSeen = System.currentTimeMillis();

    // ─── Application-level state (populated in later steps) ────────────────────
    /** Session token issued at login; null until authenticated. */
    private volatile String token;
    /** Authenticated username; null until authenticated. */
    private volatile String username;
    /** Whether this account has admin rights. */
    private volatile boolean admin;

    public ClientConnection(Socket socket, GameServer server) throws IOException {
        this.connId = "C" + SEQ.getAndIncrement();
        this.socket = socket;
        this.server = server;
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    public String connId() { return connId; }
    public String remote() { return socket.getRemoteSocketAddress().toString(); }
    public boolean isOpen() { return open; }
    public long lastSeen() { return lastSeen; }
    public void touch() { lastSeen = System.currentTimeMillis(); }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public boolean isAuthenticated() { return username != null; }
    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }

    @Override
    public void run() {
        Log.info("Connected " + connId + " from " + remote());
        try {
            server.dispatcher().onConnect(this);
            while (open) {
                Packet p = Wire.read(in);
                if (p == null) break; // clean EOF
                touch();
                try {
                    server.dispatcher().handle(this, p);
                } catch (Exception ex) {
                    Log.error(connId + " handler error for " + p.type, ex);
                    sendError(p.id, "INTERNAL", "Server error handling " + p.type);
                }
            }
        } catch (IOException io) {
            if (open) Log.debug(connId + " read ended: " + io.getMessage());
        } catch (Exception e) {
            Log.error(connId + " fatal", e);
        } finally {
            close("reader-exit");
        }
    }

    /** Send a packet; safe from any thread. Failures close the connection. */
    public void send(Packet p) {
        if (!open) return;
        synchronized (writeLock) {
            try {
                Wire.write(out, p);
            } catch (IOException e) {
                Log.debug(connId + " write failed: " + e.getMessage());
                close("write-failed");
            }
        }
    }

    /** Convenience: send an ERROR_RES echoing a request id. */
    public void sendError(String correlId, String code, String message) {
        send(Packet.of(Wire.GSON, MessageType.ERROR_RES, correlId,
                new ErrorPayload(code, message)));
    }

    /** Close the socket and deregister; idempotent. */
    public void close(String reason) {
        if (!open) return;
        open = false;
        Log.info("Disconnected " + connId
                + (username != null ? " (" + username + ")" : "")
                + " — " + reason);
        try {
            server.dispatcher().onDisconnect(this, reason);
        } catch (Exception e) {
            Log.error(connId + " onDisconnect error", e);
        }
        try { socket.close(); } catch (IOException ignored) { }
        server.deregister(this);
    }
}
