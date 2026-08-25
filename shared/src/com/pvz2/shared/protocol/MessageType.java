package com.pvz2.shared.protocol;

/**
 * All message {@code type} strings used on the wire, shared by server and client.
 *
 * <p>Kept as plain String constants (no enum) so unknown/newer types from a
 * different protocol version never crash deserialization — the dispatcher can
 * decide what to do with an unrecognized type. The numeric protocol version
 * lives in {@link Packet#v} and {@link #VERSION}.
 *
 * <p>Naming convention: {@code *_REQ} = client→server request expecting a reply
 * with the same correlation id; {@code *_RES} = the reply; {@code *_EVT} =
 * unsolicited server→client push (or client→server fire-and-forget).
 */
public final class MessageType {

    private MessageType() { }

    /** Current protocol version. Bump when the wire format changes incompatibly. */
    public static final int VERSION = 1;

    // ─── Connection / liveness ────────────────────────────────────────────────
    public static final String PING          = "PING";           // either side
    public static final String PONG          = "PONG";           // reply to PING
    public static final String HELLO_EVT     = "HELLO_EVT";      // server greets on connect
    public static final String ERROR_RES     = "ERROR_RES";      // generic error reply
    public static final String BYE_EVT       = "BYE_EVT";        // graceful disconnect notice

    // ─── Authentication / accounts (step 2) ──────────────────────────────────
    public static final String REGISTER_REQ  = "REGISTER_REQ";
    public static final String REGISTER_RES  = "REGISTER_RES";
    public static final String LOGIN_REQ     = "LOGIN_REQ";
    public static final String LOGIN_RES     = "LOGIN_RES";
    public static final String RESUME_REQ    = "RESUME_REQ";     // reconnect with token
    public static final String RESUME_RES    = "RESUME_RES";
    public static final String LOGOUT_REQ    = "LOGOUT_REQ";
    public static final String LOGOUT_RES    = "LOGOUT_RES";
    public static final String SET_SECURITY_REQ = "SET_SECURITY_REQ";
    public static final String SET_SECURITY_RES = "SET_SECURITY_RES";
    public static final String RECOVER_REQ   = "RECOVER_REQ";    // initiate password recovery
    public static final String RECOVER_RES   = "RECOVER_RES";
    public static final String RESET_PW_REQ  = "RESET_PW_REQ";
    public static final String RESET_PW_RES  = "RESET_PW_RES";

    // ─── User data sync (step 3) ─────────────────────────────────────────────
    public static final String GET_PROFILE_REQ = "GET_PROFILE_REQ";
    public static final String GET_PROFILE_RES = "GET_PROFILE_RES";
    public static final String SAVE_PROFILE_REQ = "SAVE_PROFILE_REQ"; // push full user data
    public static final String SAVE_PROFILE_RES = "SAVE_PROFILE_RES";

    // ─── Leaderboard + scored game (step 4) ──────────────────────────────────
    public static final String LEADERBOARD_REQ = "LEADERBOARD_REQ";
    public static final String LEADERBOARD_RES = "LEADERBOARD_RES";
    public static final String SUBMIT_SCORE_REQ = "SUBMIT_SCORE_REQ";
    public static final String SUBMIT_SCORE_RES = "SUBMIT_SCORE_RES";

    // ─── Matchmaking / rooms (step 5) ────────────────────────────────────────
    public static final String ONLINE_LIST_REQ = "ONLINE_LIST_REQ";
    public static final String ONLINE_LIST_RES = "ONLINE_LIST_RES";
    public static final String INVITE_REQ      = "INVITE_REQ";      // invite by username
    public static final String INVITE_RES      = "INVITE_RES";
    public static final String INVITE_EVT      = "INVITE_EVT";      // pushed to invitee
    public static final String INVITE_ANSWER_REQ = "INVITE_ANSWER_REQ"; // accept/reject
    public static final String QUEUE_REQ       = "QUEUE_REQ";       // random matchmaking join
    public static final String QUEUE_RES       = "QUEUE_RES";
    public static final String QUEUE_CANCEL_REQ = "QUEUE_CANCEL_REQ";
    public static final String MATCH_FOUND_EVT = "MATCH_FOUND_EVT"; // both players notified
    public static final String MATCH_LEAVE_REQ = "MATCH_LEAVE_REQ";
    public static final String MATCH_ENDED_EVT = "MATCH_ENDED_EVT";

    // ─── In-match sync (step 6) ──────────────────────────────────────────────
    public static final String MATCH_READY_REQ = "MATCH_READY_REQ"; // client → server: my screen is up
    public static final String MATCH_START_EVT = "MATCH_START_EVT"; // server → both: both ready, begin
    public static final String INPUT_EVT       = "INPUT_EVT";       // player action → server → host
    public static final String SNAPSHOT_EVT    = "SNAPSHOT_EVT";    // host → server → guest
    public static final String MATCH_RESULT_REQ = "MATCH_RESULT_REQ"; // host reports outcome

    // ─── Reactions (step 7) ──────────────────────────────────────────────────
    public static final String REACTION_EVT    = "REACTION_EVT";    // text/emoji/sticker to opponent

    // ─── Admin (step 8) ──────────────────────────────────────────────────────
    public static final String BROADCAST_EVT   = "BROADCAST_EVT";   // server → all (news/notice)
    public static final String KICK_EVT        = "KICK_EVT";        // server → one (forced disconnect)
}
