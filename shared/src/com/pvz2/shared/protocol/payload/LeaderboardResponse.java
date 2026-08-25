package com.pvz2.shared.protocol.payload;

import java.util.List;

/** Reply for {@code LEADERBOARD_RES}: the sorted rows. */
public class LeaderboardResponse {
    public List<LeaderboardEntryDTO> entries;

    public LeaderboardResponse() { }
    public LeaderboardResponse(List<LeaderboardEntryDTO> entries) { this.entries = entries; }
}
