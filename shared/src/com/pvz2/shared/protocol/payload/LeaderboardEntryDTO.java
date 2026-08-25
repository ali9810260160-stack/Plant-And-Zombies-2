package com.pvz2.shared.protocol.payload;

/**
 * One leaderboard row from the server.
 *
 * <p>{@link #myPoint} is the networked SCORED-game best. {@link #hasMyPoint} is
 * false for players who have never played the networked scored game — their
 * My Point cell must render empty (never a legacy/fake value), per the spec.
 */
public class LeaderboardEntryDTO {
    public String username;
    public String nickname;
    public String lastLevel;
    public int minigameCount;
    public int dailyQuestCount;
    public int questCount;
    public long myPoint;
    public boolean hasMyPoint;
}
