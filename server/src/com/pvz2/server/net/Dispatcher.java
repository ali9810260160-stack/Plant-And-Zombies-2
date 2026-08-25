package com.pvz2.server.net;

import com.pvz2.server.ServerConfig;
import com.pvz2.server.log.Log;
import com.pvz2.shared.protocol.MessageType;
import com.pvz2.shared.protocol.Packet;
import com.pvz2.shared.protocol.Wire;
import com.pvz2.shared.protocol.payload.HelloPayload;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Routes incoming {@link Packet}s to registered handlers by {@code type}.
 *
 * <p>Feature modules (auth, data sync, matchmaking, match relay, admin) call
 * {@link #register(String, Handler)} at startup, keeping this class free of a
 * monolithic switch. Liveness ({@code PING}/{@code PONG}) is built in.
 */
public final class Dispatcher {

    /** A handler for one message type. */
    @FunctionalInterface
    public interface Handler {
        void handle(ClientConnection conn, Packet p) throws Exception;
    }

    private final ConcurrentHashMap<String, Handler> handlers = new ConcurrentHashMap<>();
    private GameServer server;

    void attach(GameServer server) { this.server = server; }
    public GameServer server() { return server; }

    /** Register a handler for {@code type}. Later registrations override earlier. */
    public void register(String type, Handler handler) {
        handlers.put(type, handler);
    }

    // ─── Lifecycle hooks (called by ClientConnection / GameServer) ─────────────

    void onConnect(ClientConnection conn) {
        conn.send(Packet.of(Wire.GSON, MessageType.HELLO_EVT, null,
                new HelloPayload(MessageType.VERSION, ServerConfig.SERVER_NAME,
                        System.currentTimeMillis())));
    }

    void onDisconnect(ClientConnection conn, String reason) {
        Handler h = handlers.get("__disconnect__");
        if (h != null) {
            try { h.handle(conn, null); }
            catch (Exception e) { Log.error("disconnect hook", e); }
        }
    }

    void onServerShutdown() { /* modules may hook via register if needed */ }

    /**
     * Register a pseudo-handler invoked whenever any connection drops (its
     * {@code Packet} argument is null). Used by modules that must clean up
     * per-connection state (sessions, queues, rooms).
     */
    public void onDisconnect(Handler h) { handlers.put("__disconnect__", h); }

    // ─── Main routing ─────────────────────────────────────────────────────────

    void handle(ClientConnection conn, Packet p) throws Exception {
        if (p.type == null) {
            conn.sendError(p.id, "BAD_PACKET", "Missing type");
            return;
        }
        // Built-in liveness handling.
        switch (p.type) {
            case MessageType.PING:
                conn.send(new Packet(MessageType.PONG, p.id, null));
                return;
            case MessageType.PONG:
                return; // touch() already happened in the read loop
            default:
                break;
        }
        Handler h = handlers.get(p.type);
        if (h == null) {
            Log.warn(conn.connId() + " unsupported type: " + p.type);
            conn.sendError(p.id, "UNSUPPORTED", "Unsupported message type: " + p.type);
            return;
        }
        h.handle(conn, p);
    }
}
