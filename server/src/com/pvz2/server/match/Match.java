package com.pvz2.server.match;

/**
 * A 2-player match. The {@code host} runs the authoritative simulation (plant
 * side) and streams snapshots; the {@code guest} (zombie side) sends inputs and
 * renders. Roles are fixed for the match's lifetime.
 */
public final class Match {

    public enum State { STARTING, IN_PROGRESS, ENDED }

    public final String matchId;
    public final String host;   // PLANT role, authoritative simulator
    public final String guest;  // ZOMBIE role
    public volatile State state = State.STARTING;

    public Match(String matchId, String host, String guest) {
        this.matchId = matchId;
        this.host = host;
        this.guest = guest;
    }

    public boolean involves(String username) {
        return host.equalsIgnoreCase(username) || guest.equalsIgnoreCase(username);
    }

    /** The other player's username, given one side. */
    public String opponentOf(String username) {
        return host.equalsIgnoreCase(username) ? guest : host;
    }

    public String roleOf(String username) {
        return host.equalsIgnoreCase(username) ? "PLANT" : "ZOMBIE";
    }
}
