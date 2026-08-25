package com.pvz2.server.admin;

import com.google.gson.JsonObject;
import com.pvz2.server.log.Log;
import com.pvz2.server.net.ClientConnection;
import com.pvz2.server.net.GameServer;
import com.pvz2.server.store.UserStore;
import com.pvz2.shared.protocol.MessageType;
import com.pvz2.shared.protocol.Packet;
import com.pvz2.shared.protocol.Wire;
import com.pvz2.shared.protocol.payload.NoticePayload;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Swing admin dashboard — the "admin panel" (doc + user request).
 *
 * <p>Two tabs:
 * <ul>
 *   <li><b>Server</b>: live status, connection table (with Kick), broadcast a
 *       notice, live log, graceful shutdown.</li>
 *   <li><b>Users</b>: every account with coins/gems/ban/online status; ban/unban,
 *       kick, and grant currency.</li>
 * </ul>
 * Runs inside the server process, so it calls {@link GameServer}/{@link UserStore}
 * directly (no network protocol needed for admin actions). All work on the EDT.
 */
public final class AdminDashboard {

    private final GameServer server;
    private final UserStore store;
    private final Runnable onShutdown;

    private JFrame frame;
    private JLabel status;
    private ConnTableModel connModel;
    private UserTableModel userModel;
    private JTable connTable;
    private JTable userTable;
    private JTextArea logArea;

    public AdminDashboard(GameServer server, UserStore store, Runnable onShutdown) {
        this.server = server;
        this.store = store;
        this.onShutdown = onShutdown;
    }

    public void show() { SwingUtilities.invokeLater(this::build); }

    private void build() {
        frame = new JFrame("PvZ2 Server — Admin");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setSize(900, 620);
        frame.setLayout(new BorderLayout(6, 6));

        status = new JLabel(" ");
        status.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        status.setFont(status.getFont().deriveFont(Font.BOLD, 13f));
        frame.add(status, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Server", buildServerTab());
        tabs.addTab("Users", buildUsersTab());
        frame.add(tabs, BorderLayout.CENTER);

        Log.addListener(this::appendLog);
        new Timer(1000, e -> refresh()).start();

        refresh();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ─── Server tab ─────────────────────────────────────────────────────────────
    private JComponent buildServerTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));

        connModel = new ConnTableModel();
        connTable = new JTable(connModel);
        connTable.setFillsViewportHeight(true);
        JScrollPane tableScroll = new JScrollPane(connTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Connections"));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        for (String line : Log.recent()) logArea.append(line + "\n");
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, logScroll);
        split.setResizeWeight(0.4);
        panel.add(split, BorderLayout.CENTER);
        panel.add(buildServerBottom(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildServerBottom() {
        JPanel bottom = new JPanel(new BorderLayout(6, 6));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));

        JPanel row = new JPanel(new BorderLayout(6, 0));
        JTextField msg = new JTextField();
        JButton send = new JButton("Broadcast");
        Runnable doBroadcast = () -> {
            String text = msg.getText().trim();
            if (text.isEmpty()) return;
            server.broadcast(Packet.of(Wire.GSON, MessageType.BROADCAST_EVT, null,
                    new NoticePayload(text)));
            Log.info("[ADMIN] Broadcast: " + text);
            msg.setText("");
        };
        send.addActionListener(e -> doBroadcast.run());
        msg.addActionListener(e -> doBroadcast.run());
        row.add(new JLabel("Notice: "), BorderLayout.WEST);
        row.add(msg, BorderLayout.CENTER);
        row.add(send, BorderLayout.EAST);

        JButton kick = new JButton("Kick Selected");
        kick.addActionListener(e -> kickSelectedConnection());
        JButton shutdown = new JButton("Shutdown Server");
        shutdown.setForeground(new Color(0x9c0000));
        shutdown.addActionListener(e -> confirmShutdown());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(kick);
        buttons.add(shutdown);

        bottom.add(row, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.SOUTH);
        return bottom;
    }

    // ─── Users tab ──────────────────────────────────────────────────────────────
    private JComponent buildUsersTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        userModel = new UserTableModel();
        userTable = new JTable(userModel);
        userTable.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(userTable);
        scroll.setBorder(BorderFactory.createTitledBorder("Accounts"));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> reloadUsers());
        JButton ban = new JButton("Ban");
        ban.addActionListener(e -> setBanSelected(true));
        JButton unban = new JButton("Unban");
        unban.addActionListener(e -> setBanSelected(false));
        JButton kick = new JButton("Kick");
        kick.addActionListener(e -> kickSelectedUser());
        JButton coins = new JButton("+1000 Coins");
        coins.addActionListener(e -> grantSelected("coins", 1000));
        JButton gems = new JButton("+100 Gems");
        gems.addActionListener(e -> grantSelected("gems", 100));
        buttons.add(refresh); buttons.add(ban); buttons.add(unban); buttons.add(kick);
        buttons.add(coins); buttons.add(gems);
        panel.add(buttons, BorderLayout.SOUTH);

        reloadUsers();
        return panel;
    }

    // ─── actions ────────────────────────────────────────────────────────────────
    private void kickSelectedConnection() {
        int r = connTable.getSelectedRow();
        if (r < 0 || r >= connModel.rows.size()) return;
        server.kick(connModel.rows.get(r), "Kicked by an administrator.");
        Log.info("[ADMIN] Kicked connection " + connModel.rows.get(r).connId());
    }

    private String selectedUsername() {
        int r = userTable.getSelectedRow();
        if (r < 0 || r >= userModel.rows.size()) return null;
        return UserStore.str(userModel.rows.get(r), "username");
    }

    private void setBanSelected(boolean banned) {
        String u = selectedUsername();
        if (u == null) return;
        store.update(u, o -> o.addProperty("banned", banned));
        if (banned) server.kickUser(u, "You have been banned.");
        Log.info("[ADMIN] " + (banned ? "Banned " : "Unbanned ") + u);
        reloadUsers();
    }

    private void kickSelectedUser() {
        String u = selectedUsername();
        if (u == null) return;
        if (server.kickUser(u, "Kicked by an administrator.")) Log.info("[ADMIN] Kicked " + u);
        else JOptionPane.showMessageDialog(frame, u + " is not online.");
    }

    private void grantSelected(String field, long amount) {
        String u = selectedUsername();
        if (u == null) return;
        store.update(u, o -> {
            long cur = UserStore.getLong(o, field, 0);
            o.addProperty(field, cur + amount);
        });
        Log.info("[ADMIN] Granted " + amount + " " + field + " to " + u);
        reloadUsers();
    }

    private void confirmShutdown() {
        int r = JOptionPane.showConfirmDialog(frame,
                "Stop the server and disconnect all clients?", "Confirm shutdown",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.OK_OPTION) {
            if (onShutdown != null) onShutdown.run();
            frame.dispose();
        }
    }

    private void reloadUsers() {
        if (userModel != null) userModel.setRows(store.all());
    }

    private void refresh() {
        status.setText("  " + (server.isRunning() ? "● RUNNING" : "○ STOPPED")
                + "   port " + server.port()
                + "   connections: " + server.connectionCount()
                + "   authenticated: " + server.onlineUsernames().size()
                + "   accounts: " + (userModel != null ? userModel.rows.size() : "?"));
        if (connModel != null) connModel.setRows(new ArrayList<>(server.connections()));
        if (userModel != null) userModel.refreshOnline(server.onlineUsernames());
    }

    private void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            if (logArea == null) return;
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // ─── table models ───────────────────────────────────────────────────────────
    private static final class ConnTableModel extends AbstractTableModel {
        private final String[] cols = {"Conn", "Remote", "User", "Idle (ms)"};
        List<ClientConnection> rows = new ArrayList<>();
        void setRows(List<ClientConnection> rows) { this.rows = rows; fireTableDataChanged(); }
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            ClientConnection cc = rows.get(r);
            switch (c) {
                case 0: return cc.connId();
                case 1: return cc.remote();
                case 2: return cc.getUsername() == null ? "-" : cc.getUsername();
                case 3: return System.currentTimeMillis() - cc.lastSeen();
                default: return "";
            }
        }
    }

    private static final class UserTableModel extends AbstractTableModel {
        private final String[] cols = {"Username", "Nickname", "Coins", "Gems", "Banned", "Online"};
        List<JsonObject> rows = new ArrayList<>();
        private java.util.Set<String> online = new java.util.HashSet<>();
        void setRows(List<JsonObject> rows) { this.rows = rows; fireTableDataChanged(); }
        void refreshOnline(List<String> names) {
            java.util.Set<String> s = new java.util.HashSet<>();
            for (String n : names) s.add(n.toLowerCase());
            this.online = s;
            fireTableDataChanged();
        }
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            JsonObject o = rows.get(r);
            String u = UserStore.str(o, "username");
            switch (c) {
                case 0: return u;
                case 1: return UserStore.str(o, "nickname");
                case 2: return UserStore.getLong(o, "coins", 0);
                case 3: return UserStore.getInt(o, "gems", 0);
                case 4: return UserStore.has(o, "banned")
                        && o.get("banned").getAsBoolean() ? "YES" : "";
                case 5: return u != null && online.contains(u.toLowerCase()) ? "●" : "";
                default: return "";
            }
        }
    }
}
