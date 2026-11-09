package com.ourteam.manager;

import com.ourteam.OurTeam;
import com.ourteam.model.PlayerStats;
import com.ourteam.model.Team;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Robust PlaceholderAPI Expansion mapping for our team plugin,
 * supporting team leaderboards, normal player leaderboards (place, break, build, mobkill, kd, etc.),
 * and player/team statistics.
 */
public class OurTeamPlaceholders extends PlaceholderExpansion {

    protected final OurTeam plugin;

    public OurTeamPlaceholders(OurTeam plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ourteam";
    }

    @Override
    public @NotNull String getAuthor() {
        return "OurTeamDev";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String lower = params.toLowerCase();

        // ==========================================
        // 1. TEAM LEADERBOARDS
        // Patterns:
        // %ourteam_leaderboard_<stat>_name_<pos>%
        // %ourteam_leaderboard_<stat>_value_<pos>%
        // %ourteam_top_team_<stat>_name_<pos>%
        // %ourteam_top_team_<stat>_value_<pos>%
        // %ourteam_team_top_<stat>_name_<pos>%
        // %ourteam_team_top_<stat>_value_<pos>%
        // ==========================================
        if (lower.startsWith("leaderboard_team_") || lower.startsWith("top_team_") || lower.startsWith("team_top_")) {
            String sub = lower.replaceFirst("^(leaderboard_team_|top_team_|team_top_)", "");
            String[] parts = sub.split("_");
            if (parts.length >= 3) {
                String stat = parts[0];
                String field = parts[1]; // "name" or "value"
                try {
                    int pos = Integer.parseInt(parts[2]);
                    return getTeamLeaderboard(stat, field, pos);
                } catch (NumberFormatException ignored) {}
            }
        }

        if (lower.startsWith("leaderboard_") && !lower.startsWith("leaderboard_player_")) {
            String sub = lower.substring("leaderboard_".length());
            String[] parts = sub.split("_");
            if (parts.length >= 3) {
                String stat = parts[0];
                String field = parts[1]; // "name" or "value"
                try {
                    int pos = Integer.parseInt(parts[2]);
                    return getTeamLeaderboard(stat, field, pos);
                } catch (NumberFormatException ignored) {}
            } else if (parts.length == 2) {
                // E.g. %ourteam_leaderboard_place_1% -> defaults to name
                String stat = parts[0];
                try {
                    int pos = Integer.parseInt(parts[1]);
                    return getTeamLeaderboard(stat, "name", pos);
                } catch (NumberFormatException ignored) {}
            }
        }

        // Legacy BetterTeams position patterns:
        // %ourteam_position_<type>_<pos>%
        if (lower.startsWith("position_")) {
            String[] parts = params.split("_", 3);
            if (parts.length >= 3) {
                String type = parts[1];
                try {
                    int rank = Integer.parseInt(parts[2]);
                    List<Team> all = new ArrayList<>(plugin.getTeamManager().getAllTeams());
                    all.sort((t1, t2) -> Integer.compare(t2.getCachedScore(), t1.getCachedScore()));
                    if (rank >= 1 && rank <= all.size()) {
                        return getTeamPlaceholder(all.get(rank - 1), null, type);
                    }
                } catch (NumberFormatException ignored) {}
            }
            return "";
        }

        // %ourteam_balanceposition_<type>_<pos>%
        if (lower.startsWith("balanceposition_")) {
            String[] parts = params.split("_", 3);
            if (parts.length >= 3) {
                String type = parts[1];
                try {
                    int rank = Integer.parseInt(parts[2]);
                    List<Team> all = new ArrayList<>(plugin.getTeamManager().getAllTeams());
                    all.sort((t1, t2) -> Double.compare(t2.getBankBalance(), t1.getBankBalance()));
                    if (rank >= 1 && rank <= all.size()) {
                        return getTeamPlaceholder(all.get(rank - 1), null, type);
                    }
                } catch (NumberFormatException ignored) {}
            }
            return "";
        }

        // %ourteam_membersposition_<type>_<pos>%
        if (lower.startsWith("membersposition_")) {
            String[] parts = params.split("_", 3);
            if (parts.length >= 3) {
                String type = parts[1];
                try {
                    int rank = Integer.parseInt(parts[2]);
                    List<Team> all = new ArrayList<>(plugin.getTeamManager().getAllTeams());
                    all.sort((t1, t2) -> Integer.compare(t2.getMembers().size(), t1.getMembers().size()));
                    if (rank >= 1 && rank <= all.size()) {
                        return getTeamPlaceholder(all.get(rank - 1), null, type);
                    }
                } catch (NumberFormatException ignored) {}
            }
            return "";
        }

        // ==========================================
        // 2. NORMAL / PLAYER LEADERBOARDS
        // Patterns:
        // %ourteam_top_<stat>_name_<pos>%
        // %ourteam_top_<stat>_value_<pos>%
        // %ourteam_top_player_<stat>_name_<pos>%
        // %ourteam_top_player_<stat>_value_<pos>%
        // %ourteam_player_top_<stat>_name_<pos>%
        // %ourteam_player_top_<stat>_value_<pos>%
        // %ourteam_leaderboard_player_<stat>_name_<pos>%
        // %ourteam_leaderboard_player_<stat>_value_<pos>%
        // ==========================================
        if (lower.startsWith("top_player_") || lower.startsWith("player_top_") || lower.startsWith("leaderboard_player_")) {
            String sub = lower.replaceFirst("^(top_player_|player_top_|leaderboard_player_)", "");
            String[] parts = sub.split("_");
            if (parts.length >= 3) {
                String stat = parts[0];
                String field = parts[1]; // "name" or "value"
                try {
                    int pos = Integer.parseInt(parts[2]);
                    return getPlayerLeaderboard(stat, field, pos);
                } catch (NumberFormatException ignored) {}
            }
        }

        if (lower.startsWith("top_") && !lower.startsWith("top_team_")) {
            String sub = lower.substring("top_".length());
            String[] parts = sub.split("_");
            if (parts.length >= 3) {
                String stat = parts[0];
                String field = parts[1]; // "name" or "value"
                try {
                    int pos = Integer.parseInt(parts[2]);
                    return getPlayerLeaderboard(stat, field, pos);
                } catch (NumberFormatException ignored) {}
            } else if (parts.length == 2) {
                // E.g. %ourteam_top_place_1% -> defaults to name
                String stat = parts[0];
                try {
                    int pos = Integer.parseInt(parts[1]);
                    return getPlayerLeaderboard(stat, "name", pos);
                } catch (NumberFormatException ignored) {}
            }
        }

        // ==========================================
        // 3. STATIC TEAM / PLAYER LOOKUPS
        // ==========================================
        if (lower.startsWith("static_")) {
            String[] parts = params.split("_", 3);
            if (parts.length >= 3) {
                String type = parts[1];
                String teamName = parts[2];
                Team targetTeam = plugin.getTeamManager().getTeamByName(teamName);
                if (targetTeam != null) {
                    return getTeamPlaceholder(targetTeam, null, type);
                }
            }
            return "";
        }

        if (lower.startsWith("staticplayer_")) {
            String[] parts = params.split("_", 3);
            if (parts.length >= 3) {
                String type = parts[1];
                String playerName = parts[2];
                OfflinePlayer targetPlayer = org.bukkit.Bukkit.getOfflinePlayer(playerName);
                if (targetPlayer != null) {
                    Team targetTeam = plugin.getTeamManager().getPlayerTeam(targetPlayer.getUniqueId());
                    if (targetTeam != null) {
                        return getTeamPlaceholder(targetTeam, targetPlayer, type);
                    }
                }
            }
            return "";
        }

        // Check if player context exists
        if (player == null) {
            return "";
        }

        // ==========================================
        // 4. PLAYER-SPECIFIC STATS (NORMAL)
        // ==========================================
        PlayerStats ps = plugin.getTeamManager().getPlayerStats(player.getUniqueId());
        switch (lower) {
            case "place":
            case "build":
            case "blocks_placed":
            case "player_place":
            case "player_build":
                return String.valueOf(ps.getBlocksPlaced());
            case "break":
            case "blocks_broken":
            case "player_break":
                return String.valueOf(ps.getBlocksBroken());
            case "mobkill":
            case "mobkills":
            case "mobs":
            case "player_mobkill":
                return String.valueOf(ps.getMobKills());
            case "kd":
            case "kdr":
            case "player_kd":
                return String.format("%.2f", ps.getKDR());
            case "kills":
            case "player_kills":
                return String.valueOf(ps.getKills());
            case "deaths":
            case "player_deaths":
                return String.valueOf(ps.getDeaths());
            case "killstreak":
            case "player_killstreak":
                return String.valueOf(ps.getKillstreak());
            case "best_killstreak":
            case "player_best_killstreak":
                return String.valueOf(ps.getBestKillstreak());
            case "playtime": {
                long totalSecs = ps.getPlaytimeMs() / 1000L;
                long hours = totalSecs / 3600L;
                long mins = (totalSecs % 3600L) / 60L;
                if (hours > 0) return hours + "h " + mins + "m";
                return Math.max(1, mins) + "m";
            }
        }

        // Player position / rank in leaderboard
        if (lower.startsWith("rank_") || lower.startsWith("my_rank_") || lower.startsWith("position_player_") || lower.startsWith("player_rank_")) {
            String stat = lower.replaceFirst("^(rank_|my_rank_|position_player_|player_rank_)", "");
            return getPlayerRank(player, stat);
        }

        // ==========================================
        // 5. PLAYER'S TEAM STATS
        // ==========================================
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());

        if (lower.startsWith("team_")) {
            String teamSub = lower.substring("team_".length());
            if (team == null) {
                return teamSub.equals("name") || teamSub.equals("tag") ? "No Team" : "0";
            }
            return getTeamPlaceholder(team, player, teamSub);
        }

        return getTeamPlaceholder(team, player, params);
    }

    /**
     * Resolves team leaderboards.
     */
    private String getTeamLeaderboard(String stat, String field, int position) {
        if (position < 1) return "";
        List<Team> teams = new ArrayList<>(plugin.getTeamManager().getAllTeams());
        switch (stat.toLowerCase()) {
            case "place":
            case "build":
            case "blocks_placed":
                teams.sort((t1, t2) -> Integer.compare(t2.getTotalBlocksPlaced(), t1.getTotalBlocksPlaced()));
                break;
            case "break":
            case "blocks_broken":
                teams.sort((t1, t2) -> Integer.compare(t2.getTotalBlocksBroken(), t1.getTotalBlocksBroken()));
                break;
            case "mobkill":
            case "mobkills":
            case "mobs":
                teams.sort((t1, t2) -> Integer.compare(t2.getTotalMobKills(), t1.getTotalMobKills()));
                break;
            case "kd":
            case "kdr":
                teams.sort((t1, t2) -> Double.compare(t2.getKDR(), t1.getKDR()));
                break;
            case "kills":
            case "kill":
                teams.sort((t1, t2) -> Integer.compare(t2.getTotalKills(), t1.getTotalKills()));
                break;
            case "deaths":
            case "death":
                teams.sort((t1, t2) -> Integer.compare(t2.getTotalDeaths(), t1.getTotalDeaths()));
                break;
            case "balance":
            case "money":
            case "bank":
                teams.sort((t1, t2) -> Double.compare(t2.getBankBalance(), t1.getBankBalance()));
                break;
            case "members":
            case "size":
                teams.sort((t1, t2) -> Integer.compare(t2.getMembers().size(), t1.getMembers().size()));
                break;
            case "level":
                teams.sort((t1, t2) -> Integer.compare(t2.getTeamLevel(), t1.getTeamLevel()));
                break;
            case "score":
            case "points":
            default:
                teams.sort((t1, t2) -> Integer.compare(t2.getCachedScore(), t1.getCachedScore()));
                break;
        }

        if (position > teams.size()) {
            return field.equalsIgnoreCase("name") ? "None" : "0";
        }

        Team t = teams.get(position - 1);
        if (field.equalsIgnoreCase("name")) {
            return t.getName();
        }

        switch (stat.toLowerCase()) {
            case "place":
            case "build":
            case "blocks_placed":
                return String.valueOf(t.getTotalBlocksPlaced());
            case "break":
            case "blocks_broken":
                return String.valueOf(t.getTotalBlocksBroken());
            case "mobkill":
            case "mobkills":
            case "mobs":
                return String.valueOf(t.getTotalMobKills());
            case "kd":
            case "kdr":
                return String.format("%.2f", t.getKDR());
            case "kills":
            case "kill":
                return String.valueOf(t.getTotalKills());
            case "deaths":
            case "death":
                return String.valueOf(t.getTotalDeaths());
            case "balance":
            case "money":
            case "bank":
                return String.format("%.2f", t.getBankBalance());
            case "members":
            case "size":
                return String.valueOf(t.getMembers().size());
            case "level":
                return String.valueOf(t.getTeamLevel());
            case "score":
            case "points":
            default:
                return String.valueOf(t.getCachedScore());
        }
    }

    /**
     * Resolves normal individual player leaderboards.
     */
    private String getPlayerLeaderboard(String stat, String field, int position) {
        if (position < 1) return "";
        List<PlayerStats> players = new ArrayList<>(plugin.getTeamManager().getAllPlayerStats());
        switch (stat.toLowerCase()) {
            case "place":
            case "build":
            case "blocks_placed":
                players.sort((p1, p2) -> Integer.compare(p2.getBlocksPlaced(), p1.getBlocksPlaced()));
                break;
            case "break":
            case "blocks_broken":
                players.sort((p1, p2) -> Integer.compare(p2.getBlocksBroken(), p1.getBlocksBroken()));
                break;
            case "mobkill":
            case "mobkills":
            case "mobs":
                players.sort((p1, p2) -> Integer.compare(p2.getMobKills(), p1.getMobKills()));
                break;
            case "kd":
            case "kdr":
                players.sort((p1, p2) -> Double.compare(p2.getKDR(), p1.getKDR()));
                break;
            case "kills":
            case "kill":
                players.sort((p1, p2) -> Integer.compare(p2.getKills(), p1.getKills()));
                break;
            case "deaths":
            case "death":
                players.sort((p1, p2) -> Integer.compare(p2.getDeaths(), p1.getDeaths()));
                break;
            case "playtime":
                players.sort((p1, p2) -> Long.compare(p2.getPlaytimeMs(), p1.getPlaytimeMs()));
                break;
            case "balance":
            case "money":
                if (plugin.getEconomy() != null) {
                    players.sort((p1, p2) -> Double.compare(
                        plugin.getEconomy().getBalance(org.bukkit.Bukkit.getOfflinePlayer(p2.getUuid())),
                        plugin.getEconomy().getBalance(org.bukkit.Bukkit.getOfflinePlayer(p1.getUuid()))
                    ));
                }
                break;
            default:
                players.sort((p1, p2) -> Integer.compare(p2.getKills(), p1.getKills()));
                break;
        }

        if (position > players.size()) {
            return field.equalsIgnoreCase("name") ? "None" : "0";
        }

        PlayerStats ps = players.get(position - 1);
        if (field.equalsIgnoreCase("name")) {
            return ps.getPlayerName();
        }

        switch (stat.toLowerCase()) {
            case "place":
            case "build":
            case "blocks_placed":
                return String.valueOf(ps.getBlocksPlaced());
            case "break":
            case "blocks_broken":
                return String.valueOf(ps.getBlocksBroken());
            case "mobkill":
            case "mobkills":
            case "mobs":
                return String.valueOf(ps.getMobKills());
            case "kd":
            case "kdr":
                return String.format("%.2f", ps.getKDR());
            case "kills":
            case "kill":
                return String.valueOf(ps.getKills());
            case "deaths":
            case "death":
                return String.valueOf(ps.getDeaths());
            case "playtime": {
                long totalSecs = ps.getPlaytimeMs() / 1000L;
                long hours = totalSecs / 3600L;
                long mins = (totalSecs % 3600L) / 60L;
                if (hours > 0) return hours + "h " + mins + "m";
                return Math.max(1, mins) + "m";
            }
            case "balance":
            case "money":
                if (plugin.getEconomy() != null) {
                    return String.format("%.2f", plugin.getEconomy().getBalance(org.bukkit.Bukkit.getOfflinePlayer(ps.getUuid())));
                }
                return "0.00";
            default:
                return String.valueOf(ps.getKills());
        }
    }

    private String getPlayerRank(OfflinePlayer player, String stat) {
        if (player == null) return "N/A";
        List<PlayerStats> players = new ArrayList<>(plugin.getTeamManager().getAllPlayerStats());
        switch (stat.toLowerCase()) {
            case "place":
            case "build":
            case "blocks_placed":
                players.sort((p1, p2) -> Integer.compare(p2.getBlocksPlaced(), p1.getBlocksPlaced()));
                break;
            case "break":
            case "mined":
            case "blocks_broken":
                players.sort((p1, p2) -> Integer.compare(p2.getBlocksBroken(), p1.getBlocksBroken()));
                break;
            case "mobkill":
            case "mobkills":
            case "mobs":
                players.sort((p1, p2) -> Integer.compare(p2.getMobKills(), p1.getMobKills()));
                break;
            case "kd":
            case "kdr":
                players.sort((p1, p2) -> Double.compare(p2.getKDR(), p1.getKDR()));
                break;
            case "deaths":
            case "death":
                players.sort((p1, p2) -> Integer.compare(p2.getDeaths(), p1.getDeaths()));
                break;
            case "playtime":
                players.sort((p1, p2) -> Long.compare(p2.getPlaytimeMs(), p1.getPlaytimeMs()));
                break;
            case "balance":
            case "money":
                if (plugin.getEconomy() != null) {
                    players.sort((p1, p2) -> Double.compare(
                            plugin.getEconomy().getBalance(org.bukkit.Bukkit.getOfflinePlayer(p2.getUuid())),
                            plugin.getEconomy().getBalance(org.bukkit.Bukkit.getOfflinePlayer(p1.getUuid()))
                    ));
                }
                break;
            case "killstreak":
                players.sort((p1, p2) -> Integer.compare(p2.getBestKillstreak(), p1.getBestKillstreak()));
                break;
            case "kills":
            case "kill":
            default:
                players.sort((p1, p2) -> Integer.compare(p2.getKills(), p1.getKills()));
                break;
        }

        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getUuid().equals(player.getUniqueId())) {
                return String.valueOf(i + 1);
            }
        }
        return "N/A";
    }

    /**
     * Resolves the requested type parameter for a specific Team.
     */
    private String getTeamPlaceholder(Team team, OfflinePlayer contextPlayer, String type) {
        if (team == null || team.isSystemLocked()) {
            if (type.equalsIgnoreCase("name") || type.equalsIgnoreCase("displayname") || type.equalsIgnoreCase("tag")) {
                return "N/A";
            }
            if (type.equalsIgnoreCase("score") || type.equalsIgnoreCase("points")) {
                return "0";
            }
            return "";
        }

        int level = team.getTeamLevel();

        switch (type.toLowerCase()) {
            case "name":
            case "tag":
            case "displayname":
                return team.getName();
            case "description":
                return team.getDescription();
            case "open":
                return String.valueOf(team.isOpenJoin());
            case "score":
            case "points":
                return String.valueOf(team.getCachedScore());
            case "money":
            case "balance":
            case "bank":
                return String.format("%.2f", team.getBankBalance());
            case "moneyshort":
                return formatShort(team.getBankBalance());
            case "place":
            case "build":
            case "blocks_placed":
                return String.valueOf(team.getTotalBlocksPlaced());
            case "break":
            case "blocks_broken":
                return String.valueOf(team.getTotalBlocksBroken());
            case "mobkill":
            case "mobkills":
            case "mobs":
                return String.valueOf(team.getTotalMobKills());
            case "kd":
            case "kdr":
                return String.format("%.2f", team.getKDR());
            case "kills":
            case "kill":
                return String.valueOf(team.getTotalKills());
            case "deaths":
            case "death":
                return String.valueOf(team.getTotalDeaths());
            case "color":
                return "§a";
            case "colorname":
                return "GREEN";
            case "onlinelist": {
                List<String> list = new ArrayList<>();
                for (UUID m : team.getMembers()) {
                    org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(m);
                    if (p != null && p.isOnline()) {
                        list.add(p.getName());
                    }
                }
                return list.isEmpty() ? "None" : String.join(", ", list);
            }
            case "offlinelist": {
                List<String> list = new ArrayList<>();
                for (UUID m : team.getMembers()) {
                    org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(m);
                    if (p == null || !p.isOnline()) {
                        String name = org.bukkit.Bukkit.getOfflinePlayer(m).getName();
                        if (name != null) {
                            list.add(name);
                        }
                    }
                }
                return list.isEmpty() ? "None" : String.join(", ", list);
            }
            case "online": {
                int online = 0;
                for (UUID m : team.getMembers()) {
                    org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(m);
                    if (p != null && p.isOnline()) {
                        online++;
                    }
                }
                return String.valueOf(online);
            }
            case "members":
                return String.valueOf(team.getMembers().size());
            case "level":
                return String.valueOf(level);
            case "maxmoney":
                return String.format("%.2f", level * 50000.0);
            case "maxmembers":
                return String.valueOf(8 + (level - 1) * 2);
            case "maxwarps":
                return String.valueOf(3 + (level - 1));
            case "pvp":
                return String.valueOf(team.isFriendlyFireEnabled());
            case "rank":
                if (contextPlayer != null) {
                    return team.getRole(contextPlayer.getUniqueId());
                }
                return "";
            case "title":
                if (contextPlayer != null) {
                    return team.getRole(contextPlayer.getUniqueId());
                }
                return "Teammate";
            case "owners": {
                String name = org.bukkit.Bukkit.getOfflinePlayer(team.getOwner()).getName();
                return name != null ? name : "Unknown";
            }
            case "admins": {
                List<String> list = new ArrayList<>();
                for (UUID m : team.getMembers()) {
                    if (team.getRole(m).equalsIgnoreCase("ADMIN") || team.getRole(m).equalsIgnoreCase("MODERATOR") || team.getRole(m).equalsIgnoreCase("OWNER")) {
                        String name = org.bukkit.Bukkit.getOfflinePlayer(m).getName();
                        if (name != null) {
                            list.add(name);
                        }
                    }
                }
                return list.isEmpty() ? "None" : String.join(", ", list);
            }
            case "defaultmembers": {
                List<String> list = new ArrayList<>();
                for (UUID m : team.getMembers()) {
                    if (team.getRole(m).equalsIgnoreCase("MEMBER")) {
                        String name = org.bukkit.Bukkit.getOfflinePlayer(m).getName();
                        if (name != null) {
                            list.add(name);
                        }
                    }
                }
                return list.isEmpty() ? "None" : String.join(", ", list);
            }
            case "positionscore":
            case "team_position":
            case "team_rank":
            case "teamrank": {
                List<Team> all = new ArrayList<>(plugin.getTeamManager().getAllTeams());
                all.sort((t1, t2) -> Integer.compare(t2.getCachedScore(), t1.getCachedScore()));
                for (int i = 0; i < all.size(); i++) {
                    if (all.get(i).getId().equals(team.getId())) {
                        return String.valueOf(i + 1);
                    }
                }
                return "N/A";
            }
            case "positionbal": {
                List<Team> all = new ArrayList<>(plugin.getTeamManager().getAllTeams());
                all.sort((t1, t2) -> Double.compare(t2.getBankBalance(), t1.getBankBalance()));
                for (int i = 0; i < all.size(); i++) {
                    if (all.get(i).getId().equals(team.getId())) {
                        return String.valueOf(i + 1);
                    }
                }
                return "N/A";
            }
            case "positionmembers": {
                List<Team> all = new ArrayList<>(plugin.getTeamManager().getAllTeams());
                all.sort((t1, t2) -> Integer.compare(t2.getMembers().size(), t1.getMembers().size()));
                for (int i = 0; i < all.size(); i++) {
                    if (all.get(i).getId().equals(team.getId())) {
                        return String.valueOf(i + 1);
                    }
                }
                return "N/A";
            }
            case "inteam":
                return "Yes";
            case "hashome":
                return team.hasHome() ? "Yes" : "No";
            case "anchor":
                return "false";
            case "teamchat":
                if (contextPlayer != null) {
                    return plugin.getTeamManager().isTeamChatToggled(contextPlayer.getUniqueId()) ? "Team" : "Global";
                }
                return "Global";
        }
        return "";
    }

    private String formatShort(double value) {
        if (value < 1000.0) {
            return String.format("%.2f", value);
        } else if (value < 1000000.0) {
            return String.format("%.1fk", value / 1000.0);
        } else if (value < 1000000000.0) {
            return String.format("%.1fM", value / 1000000.0);
        } else {
            return String.format("%.1fB", value / 1000000000.0);
        }
    }
}
