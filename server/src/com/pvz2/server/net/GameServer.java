package com.pvz2.server.net;

import com.pvz2.server.ServerConfig;
import com.pvz2.server.log.Log;
import com.pvz2.shared.protocol.MessageType;
import com.pvz2.shared.protocol.Packet;
import com.pvz2.shared.protocol.Wire;
import com.pvz2.shared.protocol.payload.KickEvent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Accepts TCP connections and owns the live connection registry.
 *
 * <p>Threading model: one accept loop, one reader thread per client (via a
 * cached thread pool), and one scheduled heartbeat task. The registry is a
 * {@link ConcurrentHashMap}; higher-level game/room state uses its own locks.
 */
public final class GameServer {

    private final int port;
    private final Dispatcher dispatcher;
    private final ConcurrentHashMap<String, ClientConnection> connections =
            new ConcurrentHashMap<>();

    private ServerSocket serverSocket;
    private volatile boolean running;

    private final java.util.concurrent.ExecutorService clientPool =
            Executors.newCachedThreadPool(named("pvz-client"));
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(named("pvz-heartbeat"));

    public GameServer(int port, Dispatcher dispatcher) {
        this.port = port;
        this.dispatcher = dispatcher;
        dispatcher.attach(this);
    }

    public Dispatcher dispatcher() { return dispatcher; }
    public boolean isRunning() { return running; }
    public int port() { return port; }
    public int connectionCount() { return connections.size(); }
    public Collection<ClientConnection> connections() { return connections.values(); }

    /** Start listening and serving. Blocks the calling thread in the accept loop. */
    public void start() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(port));
        serverSocket.setSoTimeout(1000); // so the accept loop can notice shutdown
        running = true;
        Log.info("Listening on port " + port + " (protocol v" + MessageType.VERSION + ")");

        scheduler.scheduleAtFixedRate(this::heartbeatSweep,
                ServerConfig.HEARTBEAT_INTERVAL_MS, ServerConfig.HEARTBEAT_INTERVAL_MS,
                TimeUnit.MILLISECONDS);

        while (running) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (SocketTimeoutException t) {
                continue;
            } catch (IOException e) {
                if (running) Log.error("accept() failed", e);
                break;
            }
            try {
                socket.setTcpNoDelay(true);
                ClientConnection conn = new ClientConnection(socket, this);
                connections.put(conn.connId(), conn);
                clientPool.execute(conn);
            } catch (IOException e) {
                Log.error("Failed to set up connection", e);
                try { socket.close(); } catch (IOException ignored) { }
            }
        }
        Log.info("Accept loop ended");
    }

    void deregister(ClientConnection conn) {
        connections.remove(conn.connId());
    }

    /** Send a packet to every open connection (e.g. admin broadcast). */
    public void broadcast(Packet p) {
        for (ClientConnection c : connections.values()) c.send(p);
    }

    /** All authenticated usernames currently online. */
    public List<String> onlineUsernames() {
        List<String> list = new ArrayList<>();
        for (ClientConnection c : connections.values()) {
            if (c.isAuthenticated()) list.add(c.getUsername());
        }
        return list;
    }

    /** Forcibly disconnect a connection (admin kick): notify then close. */
    public void kick(ClientConnection c, String reason) {
        if (c == null) return;
        c.send(Packet.of(Wire.GSON, MessageType.KICK_EVT, null,
                new KickEvent(reason != null ? reason : "Disconnected by an administrator.")));
        c.close("kicked: " + reason);
    }

    /** Kick by username; returns true if a matching connection was found. */
    public boolean kickUser(String username, String reason) {
        ClientConnection c = byUsername(username);
        if (c == null) return false;
        kick(c, reason);
        return true;
    }

    /** Find the live connection authenticated as {@code username}, or null. */
    public ClientConnection byUsername(String username) {
        if (username == null) return null;
        for (ClientConnection c : connections.values()) {
            if (username.equals(c.getUsername())) return c;
        }
        return null;
    }

    private void heartbeatSweep() {
        long now = System.currentTimeMillis();
        for (ClientConnection c : connections.values()) {
            long idle = now - c.lastSeen();
            if (idle > ServerConfig.CONNECTION_TIMEOUT_MS) {
                Log.warn("Timeout " + c.connId() + " (idle " + idle + "ms)");
                c.close("timeout");
            } else if (idle > ServerConfig.HEARTBEAT_INTERVAL_MS) {
                c.send(new Packet(MessageType.PING, null, null));
            }
        }
    }

    /** Gracefully stop: notify clients, close everything, shut down pools. */
    public void shutdown() {
        if (!running) return;
        running = false;
        Log.info("Shutting down — notifying " + connections.size() + " client(s)");
        Packet bye = new Packet(MessageType.BYE_EVT, null, null);
        for (ClientConnection c : new ArrayList<>(connections.values())) {
            c.send(bye);
            c.close("server-shutdown");
        }
        scheduler.shutdownNow();
        clientPool.shutdownNow();
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) { }
        try { dispatcher.onServerShutdown(); } catch (Exception e) { Log.error("shutdown hook", e); }
        Log.info("Server stopped");
        Log.close();
    }

    private static ThreadFactory named(String prefix) {
        AtomicInteger n = new AtomicInteger(1);
        return r -> {
            Thread t = new Thread(r, prefix + "-" + n.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
    }
}
