package com.pvz2.server;

import com.pvz2.server.admin.AdminDashboard;
import com.pvz2.server.auth.AuthModule;
import com.pvz2.server.auth.SessionManager;
import com.pvz2.server.log.Log;
import com.pvz2.server.net.Dispatcher;
import com.pvz2.server.net.GameServer;
import com.pvz2.server.store.UserStore;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Server entry point. Wires the dispatcher + server, opens the admin dashboard,
 * installs a graceful-shutdown hook, and runs the accept loop.
 *
 * <p>Runs standalone with plain {@code java} (no gradle). See {@code run.bat} /
 * {@code run.sh}. Feature modules (auth, data, matchmaking, relay, admin) are
 * registered onto the {@link Dispatcher} in later steps.
 */
public final class Main {

    public static void main(String[] args) {
        Log.info("=== " + ServerConfig.SERVER_NAME + " starting ===");
        Log.info("Data dir: " + ServerConfig.DATA_DIR.getAbsolutePath());

        Dispatcher dispatcher = new Dispatcher();
        GameServer server = new GameServer(ServerConfig.PORT, dispatcher);

        // ─── Feature modules ──────────────────────────────────────────────────
        UserStore store = new UserStore();
        store.load();
        SessionManager sessions = new SessionManager();
        new AuthModule(store, sessions).register(dispatcher);
        new com.pvz2.server.profile.ProfileModule(store, sessions).register(dispatcher);
        new com.pvz2.server.leaderboard.LeaderboardModule(store, sessions).register(dispatcher);
        com.pvz2.server.match.MatchRegistry matches = new com.pvz2.server.match.MatchRegistry();
        new com.pvz2.server.match.MatchmakingModule(server, sessions, matches).register(dispatcher);
        new com.pvz2.server.match.MatchRelayModule(server, sessions, matches, store).register(dispatcher);
        // Later steps: admin.

        final boolean[] shuttingDown = {false};
        Runnable shutdown = () -> {
            synchronized (shuttingDown) {
                if (shuttingDown[0]) return;
                shuttingDown[0] = true;
            }
            server.shutdown();
        };

        Runtime.getRuntime().addShutdownHook(new Thread(shutdown, "pvz-shutdown"));

        boolean headless = GraphicsEnvironment.isHeadless();
        if (ServerConfig.ADMIN_UI && !headless) {
            new AdminDashboard(server, store, shutdown).show();
        } else {
            Log.info("Admin UI disabled" + (headless ? " (headless env)" : "")
                    + " — use the console commands.");
        }

        startConsole(server, shutdown);

        try {
            server.start(); // blocks in the accept loop until shutdown
        } catch (Exception e) {
            Log.error("Server failed to start", e);
            shutdown.run();
        }
    }

    /** Minimal stdin command console (works alongside the Swing dashboard). */
    private static void startConsole(GameServer server, Runnable shutdown) {
        Thread t = new Thread(() -> {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(System.in, StandardCharsets.UTF_8));
            String line;
            try {
                while ((line = br.readLine()) != null) {
                    String cmd = line.trim().toLowerCase();
                    switch (cmd) {
                        case "":      break;
                        case "help":  System.out.println("commands: list, stats, stop, help"); break;
                        case "list":
                            for (var c : server.connections()) {
                                System.out.println("  " + c.connId() + "  " + c.remote()
                                        + "  " + (c.getUsername() == null ? "-" : c.getUsername()));
                            }
                            break;
                        case "stats":
                            System.out.println("  connections=" + server.connectionCount()
                                    + " authenticated=" + server.onlineUsernames().size());
                            break;
                        case "stop":
                        case "quit":
                        case "exit":
                            System.out.println("Stopping...");
                            shutdown.run();
                            return;
                        default:
                            System.out.println("unknown command: " + cmd + " (try 'help')");
                    }
                }
            } catch (Exception e) {
                Log.debug("console ended: " + e.getMessage());
            }
        }, "pvz-console");
        t.setDaemon(true);
        t.start();
    }
}
