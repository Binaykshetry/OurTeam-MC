package com.ourteam.model;

import java.util.UUID;

/**
 * Stores comprehensive individual player statistics for both standalone players
 * and team members, used for leaderboards and PlaceholderAPI expansions.
 */
public class PlayerStats {

    private final UUID uuid;
    private String playerName;
    private int kills;
    private int deaths;
    private int blocksPlaced;
    private int blocksBroken;
    private int mobKills;
    private long playtimeMs;
    private int grindingPoints;
    private long lastSeen;
    private int killstreak;
    private int bestKillstreak;

    public PlayerStats(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName != null ? playerName : "Unknown";
        this.kills = 0;
        this.deaths = 0;
        this.blocksPlaced = 0;
        this.blocksBroken = 0;
        this.mobKills = 0;
        this.playtimeMs = 0;
        this.grindingPoints = 0;
        this.lastSeen = System.currentTimeMillis();
        this.killstreak = 0;
        this.bestKillstreak = 0;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        if (playerName != null) {
            this.playerName = playerName;
        }
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public void addKill() {
        this.kills++;
        this.killstreak++;
        if (this.killstreak > this.bestKillstreak) {
            this.bestKillstreak = this.killstreak;
        }
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public void addDeath() {
        this.deaths++;
        this.killstreak = 0;
    }

    public int getBlocksPlaced() {
        return blocksPlaced;
    }

    public void setBlocksPlaced(int blocksPlaced) {
        this.blocksPlaced = blocksPlaced;
    }

    public void addBlockPlaced() {
        this.blocksPlaced++;
    }

    public int getBlocksBroken() {
        return blocksBroken;
    }

    public void setBlocksBroken(int blocksBroken) {
        this.blocksBroken = blocksBroken;
    }

    public void addBlockBroken() {
        this.blocksBroken++;
    }

    public int getMobKills() {
        return mobKills;
    }

    public void setMobKills(int mobKills) {
        this.mobKills = mobKills;
    }

    public void addMobKill() {
        this.mobKills++;
    }

    public long getPlaytimeMs() {
        return playtimeMs;
    }

    public void setPlaytimeMs(long playtimeMs) {
        this.playtimeMs = playtimeMs;
    }

    public void addPlaytimeMs(long ms) {
        this.playtimeMs += ms;
    }

    public int getGrindingPoints() {
        return grindingPoints;
    }

    public void setGrindingPoints(int grindingPoints) {
        this.grindingPoints = grindingPoints;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public int getKillstreak() {
        return killstreak;
    }

    public void setKillstreak(int killstreak) {
        this.killstreak = killstreak;
        if (this.killstreak > this.bestKillstreak) {
            this.bestKillstreak = this.killstreak;
        }
    }

    public int getBestKillstreak() {
        return bestKillstreak;
    }

    public void setBestKillstreak(int bestKillstreak) {
        this.bestKillstreak = bestKillstreak;
    }

    public double getKDR() {
        if (deaths == 0) {
            return kills;
        }
        return Math.round(((double) kills / deaths) * 100.0) / 100.0;
    }
}
