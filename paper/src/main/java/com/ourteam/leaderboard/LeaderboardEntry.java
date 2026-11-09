package com.ourteam.leaderboard;

import java.util.UUID;

/**
 * Represents a single ranked leaderboard position with its score/value.
 */
public class LeaderboardEntry {

    private final int position;
    private final String name;
    private final String formattedValue;
    private final UUID playerUuid;

    public LeaderboardEntry(int position, String name, String formattedValue) {
        this(position, name, formattedValue, null);
    }

    public LeaderboardEntry(int position, String name, String formattedValue, UUID playerUuid) {
        this.position = position;
        this.name = name != null ? name : "---";
        this.formattedValue = formattedValue != null ? formattedValue : "0";
        this.playerUuid = playerUuid;
    }

    public int getPosition() {
        return position;
    }

    public String getName() {
        return name;
    }

    public String getFormattedValue() {
        return formattedValue;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }
}
