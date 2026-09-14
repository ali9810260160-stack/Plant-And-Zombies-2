package com.pvz2.graphics.net;

/**
 * Client-side network configuration (server address + timeouts).
 *
 * <p>Defaults target a local server; override via {@code -Dpvz.server.host} /
 * {@code -Dpvz.server.port}, or at runtime through the setters (a settings
 * screen can call these). Kept tiny and libGDX-free.
 */
public final class NetConfig {

    private NetConfig() { }

    private static volatile String host =
            System.getProperty("pvz.server.host", "127.0.0.1");
    private static volatile int port =
            Integer.getInteger("pvz.server.port", 5599);

    /** How long the boot-time connect attempt waits before falling back to local. */
    private static volatile int connectTimeoutMs =
            Integer.getInteger("pvz.server.connectTimeout", 1500);

    /** How long a synchronous request waits for its reply. */
    private static volatile int requestTimeoutMs =
            Integer.getInteger("pvz.server.requestTimeout", 6000);

    public static String host() { return host; }
    public static int port() { return port; }
    public static int connectTimeoutMs() { return connectTimeoutMs; }
    public static int requestTimeoutMs() { return requestTimeoutMs; }

    public static void setHost(String h) { host = h; }
    public static void setPort(int p) { port = p; }
    public static void setConnectTimeoutMs(int ms) { connectTimeoutMs = ms; }
    public static void setRequestTimeoutMs(int ms) { requestTimeoutMs = ms; }
}
