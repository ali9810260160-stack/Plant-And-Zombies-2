package com.pvz2.graphics.net;

import com.pvz2.PVZApplication;
import com.pvz2.graphics.actors.NetPopups;
import com.pvz2.graphics.assets.GameAssets;
import com.pvz2.shared.protocol.MessageType;
import com.pvz2.shared.protocol.Packet;
import com.pvz2.shared.protocol.Wire;
import com.pvz2.shared.protocol.payload.ErrorPayload;
import com.pvz2.shared.protocol.payload.InviteAnswerRequest;
import com.pvz2.shared.protocol.payload.InviteEvent;
import com.pvz2.shared.protocol.payload.InviteRequest;
import com.pvz2.shared.protocol.payload.MatchEndedEvent;
import com.pvz2.shared.protocol.payload.MatchFoundEvent;
import com.pvz2.shared.protocol.payload.OkResponse;
import com.pvz2.shared.protocol.payload.OnlineListResponse;
import com.pvz2.shared.protocol.payload.TokenRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Client-side multiplayer/matchmaking: online list, invites, random queue, and
 * the current match. Server pushes (invite, match-found, match-ended) are
 * handled here — they arrive on the render thread via {@link NetClient#update()}
 * so it is safe to show scene2d popups on the app's global overlay.
 */
public final class MultiplayerService {

    private static final MultiplayerService INSTANCE = new MultiplayerService();
    public static MultiplayerService get() { return INSTANCE; }

    private PVZApplication app;
    private MatchFoundEvent currentMatch;
    private Consumer<MatchFoundEvent> matchFoundListener; // set by the lobby, else a popup is shown

    private MultiplayerService() { }

    /** Wire server-push listeners once (called from the app at startup). */
    public void init(PVZApplication app) {
        this.app = app;
        NetClient n = NetClient.get();
        n.on(MessageType.INVITE_EVT, this::onInvite);
        n.on(MessageType.MATCH_FOUND_EVT, this::onMatchFound);
        n.on(MessageType.MATCH_ENDED_EVT, this::onMatchEnded);
        n.on(MessageType.BROADCAST_EVT, this::onBroadcast);
        n.on(MessageType.KICK_EVT, this::onKick);
    }

    /** Admin broadcast/news → show a notice popup. */
    private void onBroadcast(Packet p) {
        if (app == null) return;
        com.pvz2.shared.protocol.payload.NoticePayload np =
                p.payload(Wire.GSON, com.pvz2.shared.protocol.payload.NoticePayload.class);
        String msg = np != null && np.message != null ? np.message : "";
        var skin = GameAssets.getInstance().getSkin();
        app.showOverlay(NetPopups.info(skin, "Server News", msg, app::hideOverlay));
    }

    /** Admin kicked/banned this client → notify, disconnect, and log out. */
    private void onKick(Packet p) {
        com.pvz2.shared.protocol.payload.KickEvent ke =
                p.payload(Wire.GSON, com.pvz2.shared.protocol.payload.KickEvent.class);
        String reason = ke != null && ke.reason != null ? ke.reason
                : "You were disconnected by an administrator.";
        currentMatch = null;
        NetClient.get().disconnect();
        if (app != null) {
            com.pvz2.graphics.GameFacade.get().logout();
            var skin = GameAssets.getInstance().getSkin();
            app.showOverlay(NetPopups.info(skin, "Disconnected", reason, () -> {
                app.hideOverlay();
                app.goTo(com.pvz2.graphics.ScreenId.WELCOME);
            }));
        }
    }

    public MatchFoundEvent currentMatch() { return currentMatch; }
    public void clearMatch() { currentMatch = null; }
    public void setMatchFoundListener(Consumer<MatchFoundEvent> l) { matchFoundListener = l; }

    // ─── server pushes (render thread) ─────────────────────────────────────────

    private void onInvite(Packet p) {
        if (app == null) return;
        InviteEvent ie = p.payload(Wire.GSON, InviteEvent.class);
        if (ie == null) return;
        var skin = GameAssets.getInstance().getSkin();
        app.showOverlay(NetPopups.invite(skin, ie.fromUsername,
                () -> { app.hideOverlay(); answerInvite(ie.inviteId, true); },
                () -> { app.hideOverlay(); answerInvite(ie.inviteId, false); }));
    }

    private void onMatchFound(Packet p) {
        currentMatch = p.payload(Wire.GSON, MatchFoundEvent.class);
        if (currentMatch == null || app == null) return;
        if (matchFoundListener != null) {
            matchFoundListener.accept(currentMatch);
            return;
        }
        // Launch the 2-player VERSUS game (host simulates, guest renders snapshots).
        app.hideOverlay();
        VersusSession.Role role = currentMatch.host
                ? VersusSession.Role.HOST : VersusSession.Role.GUEST;
        app.startVersus(new VersusSession(role, currentMatch.matchId));
    }

    private void onMatchEnded(Packet p) {
        currentMatch = null;
        MatchEndedEvent ev = p.payload(Wire.GSON, MatchEndedEvent.class);
        String reason = (ev != null && ev.reason != null) ? ev.reason : "The match has ended.";
        if (app != null) {
            var skin = GameAssets.getInstance().getSkin();
            app.showOverlay(NetPopups.info(skin, "Multiplayer", reason, app::hideOverlay));
        }
    }

    private void answerInvite(String inviteId, boolean accept) {
        InviteAnswerRequest r = new InviteAnswerRequest();
        r.token = NetClient.get().token(); r.inviteId = inviteId; r.accept = accept;
        NetClient.get().send(MessageType.INVITE_ANSWER_REQ, r); // match-found arrives as a push
    }

    // ─── lobby API (called from the render thread; brief blocking) ─────────────

    public boolean isOnline() { return NetClient.get().isConnected(); }

    /** @return list of online usernames (empty on failure). */
    public List<String> onlineList() {
        if (!isOnline()) return new ArrayList<>();
        try {
            Packet resp = NetClient.get().requestSync(MessageType.ONLINE_LIST_REQ,
                    new TokenRequest(NetClient.get().token()));
            if (MessageType.ONLINE_LIST_RES.equals(resp.type)) {
                OnlineListResponse r = resp.payload(Wire.GSON, OnlineListResponse.class);
                return r != null && r.usernames != null ? r.usernames : new ArrayList<>();
            }
        } catch (NetException e) { NetLog.info("onlineList: " + e.getMessage()); }
        return new ArrayList<>();
    }

    /** Invite by username. @return null on success, else an error message. */
    public String invite(String username) {
        if (!isOnline()) return "Not connected to server.";
        try {
            InviteRequest r = new InviteRequest();
            r.token = NetClient.get().token(); r.targetUsername = username;
            Packet resp = NetClient.get().requestSync(MessageType.INVITE_REQ, r);
            if (MessageType.INVITE_RES.equals(resp.type)) return null;
            return errMsg(resp);
        } catch (NetException e) { return e.getMessage(); }
    }

    /** Join the random-match queue. @return status/notice text or an error. */
    public String queue() {
        if (!isOnline()) return "Not connected to server.";
        try {
            Packet resp = NetClient.get().requestSync(MessageType.QUEUE_REQ,
                    new TokenRequest(NetClient.get().token()));
            if (MessageType.QUEUE_RES.equals(resp.type)) {
                OkResponse ok = resp.payload(Wire.GSON, OkResponse.class);
                return ok != null && ok.message != null ? ok.message : "Searching...";
            }
            return errMsg(resp);
        } catch (NetException e) { return e.getMessage(); }
    }

    public void cancelQueue() {
        if (!isOnline()) return;
        NetClient.get().send(MessageType.QUEUE_CANCEL_REQ, new TokenRequest(NetClient.get().token()));
    }

    public void leaveMatch() {
        currentMatch = null;
        if (!isOnline()) return;
        NetClient.get().send(MessageType.MATCH_LEAVE_REQ, new TokenRequest(NetClient.get().token()));
    }

    private String errMsg(Packet resp) {
        try {
            ErrorPayload ep = resp.payload(Wire.GSON, ErrorPayload.class);
            if (ep != null && ep.message != null) return ep.message;
        } catch (Exception ignored) { }
        return "Server error.";
    }
}
