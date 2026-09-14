package com.pvz2.graphics.net;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tiny file logger for the client's network layer. The user launches the game by
 * double-click and never sees a console, so runtime diagnostics go to a fixed
 * file: {@code data/net-client.log} (client cwd is the assets dir).
 *
 * <p>Best-effort and crash-proof: every failure is swallowed so logging never
 * takes down the game.
 */
public final class NetLog {

    private NetLog() { }

    private static final String FILE = "data/net-client.log";
    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final Object LOCK = new Object();

    public static void info(String msg) { write("INFO", msg); }
    public static void warn(String msg) { write("WARN", msg); }

    private static void write(String level, String msg) {
        String line = "[" + LocalDateTime.now().format(TS) + "] [" + level + "] " + msg;
        synchronized (LOCK) {
            try (PrintWriter w = new PrintWriter(new FileWriter(FILE, true))) {
                w.println(line);
            } catch (Exception ignored) { }
        }
    }
}
