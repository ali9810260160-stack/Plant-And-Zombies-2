package com.pvz2.server.log;

import com.pvz2.server.ServerConfig;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Thread-safe file + console logger for the server.
 *
 * <p>Writes each line to {@code data/logs/server-<date>.log} and echoes to
 * stdout. Also keeps a bounded in-memory ring buffer of recent lines and
 * notifies registered listeners (used by the Swing admin dashboard to show a
 * live log). All public methods are safe to call from any thread.
 */
public final class Log {

    public enum Level { DEBUG, INFO, WARN, ERROR }

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final int RING_CAPACITY = 500;

    private static final Object LOCK = new Object();
    private static final Deque<String> RING = new ArrayDeque<>();
    private static final List<Consumer<String>> LISTENERS = new CopyOnWriteArrayList<>();

    private static PrintWriter fileOut;
    private static String currentDay;

    private Log() { }

    public static void debug(String msg) { log(Level.DEBUG, msg, null); }
    public static void info(String msg)  { log(Level.INFO,  msg, null); }
    public static void warn(String msg)  { log(Level.WARN,  msg, null); }
    public static void error(String msg) { log(Level.ERROR, msg, null); }
    public static void error(String msg, Throwable t) { log(Level.ERROR, msg, t); }

    public static void log(Level level, String msg, Throwable t) {
        LocalDateTime now = LocalDateTime.now();
        String line = "[" + now.format(TS) + "] [" + level + "] " + msg;
        synchronized (LOCK) {
            ensureFile(now);
            System.out.println(line);
            if (fileOut != null) fileOut.println(line);
            pushRing(line);
            if (t != null) {
                for (StackTraceElement e : t.getStackTrace()) {
                    String tl = "    at " + e;
                    System.out.println(tl);
                    if (fileOut != null) fileOut.println(tl);
                    pushRing(tl);
                }
                if (fileOut != null) fileOut.flush();
            }
            if (fileOut != null) fileOut.flush();
        }
        for (Consumer<String> l : LISTENERS) {
            try { l.accept(line); } catch (Exception ignored) { }
        }
        if (t != null) {
            for (Consumer<String> l : LISTENERS) {
                try { l.accept("    " + t); } catch (Exception ignored) { }
            }
        }
    }

    private static void pushRing(String line) {
        RING.addLast(line);
        while (RING.size() > RING_CAPACITY) RING.removeFirst();
    }

    private static void ensureFile(LocalDateTime now) {
        String day = now.format(DAY);
        if (fileOut != null && day.equals(currentDay)) return;
        try {
            if (fileOut != null) fileOut.close();
            Files.createDirectories(ServerConfig.LOG_DIR.toPath());
            File f = new File(ServerConfig.LOG_DIR, "server-" + day + ".log");
            Writer w = Files.newBufferedWriter(
                    f.toPath(), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
            fileOut = new PrintWriter(w, true);
            currentDay = day;
        } catch (IOException e) {
            // Fall back to console only.
            System.err.println("Log file unavailable: " + e.getMessage());
            fileOut = null;
        }
    }

    /** Snapshot of the recent-lines ring buffer (for the admin dashboard). */
    public static List<String> recent() {
        synchronized (LOCK) { return new ArrayList<>(RING); }
    }

    /** Register a listener called for each new log line (e.g. dashboard append). */
    public static void addListener(Consumer<String> l) { LISTENERS.add(l); }
    public static void removeListener(Consumer<String> l) { LISTENERS.remove(l); }

    /** Flush and close the log file (called on shutdown). */
    public static void close() {
        synchronized (LOCK) {
            if (fileOut != null) { fileOut.flush(); fileOut.close(); fileOut = null; }
        }
    }
}
