package com.pvz2.graphics.net;

import com.google.gson.Gson;
import com.pvz2.shared.protocol.MessageType;
import com.pvz2.shared.protocol.Packet;
import com.pvz2.shared.protocol.Wire;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Client-side networking: the game's single connection to the server.
 *
 * <p><b>Threading.</b> A dedicated reader thread blocks on the socket and:
 * <ul>
 *   <li>answers server {@code PING}s with {@code PONG} immediately,</li>
 *   <li>completes a pending {@link #requestSync} future when a reply's id matches
 *       (so synchronous auth calls from UI handlers work without the render loop),</li>
 *   <li>otherwise enqueues the packet for {@link #update()} to dispatch to event
 *       listeners <em>on the render thread</em> (so scene2d is only touched there).</li>
 * </ul>
 * Writes are serialized by a lock. This class imports no libGDX — it is pure Java
 * + the shared protocol, so it stays testable and thread-clean.
 */
public final class NetClient {

    public enum State { DISCONNECTED, CONNECTING, CONNECTED }

    private static final NetClient INSTANCE = new NetClient();
    public static NetClient get() { return INSTANCE; }

    private final Gson gson = Wire.GSON;

    private volatile State state = State.DISCONNECTED;
    private volatile Socket socket;
    private volatile DataInputStream in;
    private volatile DataOutputStream out;
    private final Object writeLock = new Object();
    private Thread reader;

    private volatile String token;
    private volatile String serverName = "?";
    private volatile String host = NetConfig.host();
    private volatile int port = NetConfig.port();

    private final ConcurrentHashMap<String, CompletableFuture<Packet>> pending =
            new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Packet> inbound = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, List<Consumer<Packet>>> listeners = new ConcurrentHashMap<>();
    private final List<Consumer<State>> stateListeners = new CopyOnWriteArrayList<>();

    private NetClient() { }

    // ─── state ────────────────────────────────────────────────────────────────

    public State state() { return state; }
    public boolean isConnected() { return state == State.CONNECTED; }
    public String serverName() { return serverName; }
    public String token() { return token; }
    public void setToken(String t) { this.token = t; }
    public void setServer(String host, int port) { this.host = host; this.port = port; }

    public void addStateListener(Consumer<State> l) { stateListeners.add(l); }
    public void removeStateListener(Consumer<State> l) { stateListeners.remove(l); }

    private void setState(State s) {
        if (state == s) return;
        state = s;
        for (Consumer<State> l : stateListeners) {
            try { l.accept(s); } catch (Exception ignored) { }
        }
    }

    // ─── connection ───────────────────────────────────────────────────────────

    /**
     * Connect and complete the HELLO handshake, blocking up to {@code timeoutMs}.
     * Safe to call from the render thread at boot. Returns true on success.
     */
    public synchronized boolean connect(int timeoutMs) {
        if (state == State.CONNECTED) return true;
        setState(State.CONNECTING);
        try {
            Socket s = new Socket();
            s.setTcpNoDelay(true);
            s.connect(new InetSocketAddress(host, port), timeoutMs);
            s.setSoTimeout(timeoutMs);
            DataInputStream din = new DataInputStream(new java.io.BufferedInputStream(s.getInputStream()));
            DataOutputStream dout = new DataOutputStream(new java.io.BufferedOutputStream(s.getOutputStream()));

            Packet hello = Wire.read(din);
            if (hello == null || !MessageType.HELLO_EVT.equals(hello.type)) {
                s.close();
                setState(State.DISCONNECTED);
                return false;
            }
            serverName = hello.data != null && hello.data.has("serverName")
                    ? hello.data.get("serverName").getAsString() : "server";
            int ver = hello.data != null && hello.data.has("protocolVersion")
                    ? hello.data.get("protocolVersion").getAsInt() : -1;
            if (ver != MessageType.VERSION) {
                NetLog.warn("Protocol version mismatch: server " + ver
                        + " vs client " + MessageType.VERSION);
            }
            s.setSoTimeout(0); // blocking reads in the loop
            this.socket = s; this.in = din; this.out = dout;
            reader = new Thread(this::readLoop, "pvz-net-reader");
            reader.setDaemon(true);
            reader.start();
            setState(State.CONNECTED);
            NetLog.info("Connected to " + serverName + " at " + host + ":" + port);
            return true;
        } catch (Exception e) {
            NetLog.info("Connect failed (" + host + ":" + port + "): " + e.getMessage());
            setState(State.DISCONNECTED);
            return false;
        }
    }

    /** Try to connect using the configured server; returns success. */
    public boolean tryConnect() { return connect(NetConfig.connectTimeoutMs()); }

    public synchronized void disconnect() {
        Socket s = socket;
        socket = null;
        if (s != null) { try { s.close(); } catch (IOException ignored) { } }
        failAllPending("disconnected");
        setState(State.DISCONNECTED);
    }

    // ─── sending ──────────────────────────────────────────────────────────────

    /** Fire-and-forget send. No-op if not connected. */
    public void send(String type, Object payload) {
        if (!isConnected()) return;
        writePacket(Packet.of(gson, type, null, payload));
    }

    public void sendPacket(Packet p) {
        if (!isConnected()) return;
        writePacket(p);
    }

    private void writePacket(Packet p) {
        DataOutputStream o = out;
        if (o == null) return;
        synchronized (writeLock) {
            try {
                Wire.write(o, p);
            } catch (IOException e) {
                NetLog.info("Write failed: " + e.getMessage());
                disconnect();
            }
        }
    }

    /**
     * Send a request and block (on the calling thread) until the correlated
     * reply arrives or {@code timeoutMs} elapses. Intended for the synchronous
     * auth calls invoked from UI button handlers.
     *
     * @return the reply packet (which may be an {@code ERROR_RES}).
     * @throws NetException on timeout, disconnect, or IO failure.
     */
    public Packet requestSync(String type, Object payload, long timeoutMs) {
        if (!isConnected()) throw new NetException("Not connected to server.");
        String id = java.util.UUID.randomUUID().toString();
        CompletableFuture<Packet> fut = new CompletableFuture<>();
        pending.put(id, fut);
        try {
            writePacket(Packet.of(gson, type, id, payload));
            return fut.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            throw new NetException("Server did not respond in time.");
        } catch (ExecutionException ee) {
            throw new NetException("Request failed: " + ee.getMessage());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new NetException("Interrupted.");
        } finally {
            pending.remove(id);
        }
    }

    public Packet requestSync(String type, Object payload) {
        return requestSync(type, payload, NetConfig.requestTimeoutMs());
    }

    // ─── receiving ────────────────────────────────────────────────────────────

    /** Register a listener for an unsolicited server push of {@code type}.
     *  Multiple listeners per type are supported (e.g. a global service and a
     *  transient match session both watching MATCH_ENDED_EVT). */
    public void on(String type, Consumer<Packet> listener) {
        listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(listener);
    }
    /** Remove a specific listener previously registered with {@link #on}. */
    public void off(String type, Consumer<Packet> listener) {
        List<Consumer<Packet>> list = listeners.get(type);
        if (list != null) list.remove(listener);
    }

    /**
     * Drain queued server pushes and dispatch them to listeners. MUST be called
     * once per frame on the render thread (e.g. from the app's render loop).
     */
    public void update() {
        Packet p;
        while ((p = inbound.poll()) != null) {
            List<Consumer<Packet>> list = p.type == null ? null : listeners.get(p.type);
            if (list != null) {
                for (Consumer<Packet> l : list) {
                    try { l.accept(p); } catch (Exception e) { NetLog.warn("listener " + p.type + ": " + e); }
                }
            }
        }
    }

    private void readLoop() {
        DataInputStream din = in;
        try {
            while (socket != null && !socket.isClosed()) {
                Packet p = Wire.read(din);
                if (p == null) break; // clean EOF
                if (MessageType.PING.equals(p.type)) {
                    writePacket(new Packet(MessageType.PONG, p.id, null));
                    continue;
                }
                if (p.id != null) {
                    CompletableFuture<Packet> fut = pending.remove(p.id);
                    if (fut != null) { fut.complete(p); continue; }
                }
                inbound.add(p); // event → render-thread dispatch
            }
        } catch (IOException io) {
            if (socket != null) NetLog.info("Read ended: " + io.getMessage());
        } catch (Exception e) {
            NetLog.warn("Reader error: " + e);
        } finally {
            failAllPending("connection lost");
            setState(State.DISCONNECTED);
        }
    }

    private void failAllPending(String reason) {
        for (Map.Entry<String, CompletableFuture<Packet>> e : pending.entrySet()) {
            e.getValue().completeExceptionally(new NetException(reason));
        }
        pending.clear();
    }
}
