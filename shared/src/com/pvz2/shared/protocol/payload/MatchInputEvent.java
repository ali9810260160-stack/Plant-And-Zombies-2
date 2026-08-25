package com.pvz2.shared.protocol.payload;

/**
 * Payload for {@code INPUT_EVT}: a player action relayed to the opponent.
 *
 * <p>In the 2-player I, Zombie game the guest (ZOMBIE side) sends these to the
 * host (authoritative simulator), which applies them to the live game. The
 * server forwards the packet to the match opponent without interpreting it.
 */
public class MatchInputEvent {
    public String matchId;
    public String action;      // e.g. "PLACE_ZOMBIE"
    public String zombieType;  // ZombieType name, for PLACE_ZOMBIE
    public int col;
    public int row;

    public MatchInputEvent() { }
    public MatchInputEvent(String matchId, String action, String zombieType, int col, int row) {
        this.matchId = matchId;
        this.action = action;
        this.zombieType = zombieType;
        this.col = col;
        this.row = row;
    }
}
