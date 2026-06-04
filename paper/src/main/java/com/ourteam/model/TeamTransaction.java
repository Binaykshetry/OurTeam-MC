package com.ourteam.model;

import java.util.UUID;

/**
 * Represents a persistent bank deposit or withdrawal transaction in a team.
 */
public class TeamTransaction {
    private final String playerName;
    private final String playerUuid;
    private final String type; // "DEPOSIT" or "WITHDRAW"
    private final double amount;
    private final long timestamp;

    public TeamTransaction(String playerName, UUID playerUuid, String type, double amount) {
        this.playerName = playerName;
        this.playerUuid = playerUuid != null ? playerUuid.toString() : "";
        this.type = type;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
    }

    public String getPlayerName() {
        return playerName != null ? playerName : "Unknown";
    }

    public UUID getPlayerUuid() {
        if (playerUuid == null || playerUuid.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(playerUuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getType() {
        return type != null ? type : "DEPOSIT";
    }

    public double getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
