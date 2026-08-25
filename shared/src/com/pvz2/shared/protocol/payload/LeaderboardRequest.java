package com.pvz2.shared.protocol.payload;

/** Payload for {@code LEADERBOARD_REQ}. */
public class LeaderboardRequest {
    /** One of: username, lastLevel, minigame, daily, quest, meopoint. */
    public String sortKey;
    public boolean ascending;

    public LeaderboardRequest() { }
    public LeaderboardRequest(String sortKey, boolean ascending) {
        this.sortKey = sortKey;
        this.ascending = ascending;
    }
}
