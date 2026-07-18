package com.ourteam.manager;

import com.ourteam.OurTeam;
import com.ourteam.model.Team;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

/**
 * Robust PlaceholderAPI Expansion mapping for our team plugin,
 * fully aligned with BetterTeams placeholder patterns.
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
        // 1. Leaderboard Rank by Score
        if (params.toLowerCase().startsWith("position_")) {
            String[] parts = params.split("_", 3);
            if (parts.length >= 3) {
                String type = parts[1];
                try {
                    int rank = Integer.parseInt(parts[2]);
                    List<Team> all = new ArrayList<>(plugin.getTeamManager().getAllTeams());
                    all.sort((t1, t2) -> Integer.compare(t2.getCachedScore(), t1.getCachedScore()));
                    if (rank >= 1 && rank <= all.size()) {
                        Team rankedTeam = all.get(rank - 1);
                        return getTeamPlaceholder(rankedTeam, null, type);
                    }
                } catch (NumberFormatException ignored) {}
            }
            return "";
        }

        // 2. Leaderboard Rank by Balance
        if (params.toLowerCase().startsWith("balanceposition_")) {
            String[] parts = params.split("_", 3);
            if (parts.length >= 3) {
                String type = parts[1];
                try {
                    int rank = Integer.parseInt(parts[2]);
                    List<Team> all = new ArrayList<>(plugin.getTeamManager().getAllTeams());
                    all.sort((t1, t2) -> Double.compare(t2.getBankBalance(), t1.getBankBalance()));
                    if (rank >= 1 && rank <= all.size()) {
                        Team rankedTeam = all.get(rank - 1);
                        return getTeamPlaceholder(rankedTeam, null, type);
                    }
                } catch (NumberFormatException ignored) {}
            }
            return "";
        }

        // 3. Leaderboard Rank by Members size
        if (params.toLowerCase().startsWith("membersposition_")) {
            String[] parts = params.split("_", 3);
            if (parts.length >= 3) {
                String type = parts[1];
                try {
                    int rank = Integer.parseInt(parts[2]);
                    List<Team> all = new ArrayList<>(plugin.getTeamManager().getAllTeams());
                    all.sort((t1, t2) -> Integer.compare(t2.getMembers().size(), t1.getMembers().size()));
                    if (rank >= 1 && rank <= all.size()) {
                        Team rankedTeam = all.get(rank - 1);
                        return getTeamPlaceholder(rankedTeam, null, type);
                    }
                } catch (NumberFormatException ignored) {}
            }
            return "";
        }

        // 4. Static Reference by Team name
        if (params.toLowerCase().startsWith("static_")) {
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

        // 5. Static Reference by Player name
        if (params.toLowerCase().startsWith("staticplayer_")) {
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

        // 5.5 Global command toggle status placeholders (returns "on"/"off")
        String cleanParam = params.toLowerCase();
        if (cleanParam.startsWith("command_")) {
            cleanParam = cleanParam.substring("command_".length());
        }
        switch (cleanParam) {
            case "create":
            case "invite":
            case "join":
            case "request":
            case "leave":
            case "kick":
            case "disband":
            case "friendlyfire":
            case "ff":
            case "pvp":
            case "promote":
            case "admin":
            case "demote":
            case "msg":
            case "warp":
            case "home":
            case "setwarp":
            case "sethome":
            case "delwarp":
            case "delhome":
            case "chat":
            case "c":
            case "info":
            case "bank":
            case "list":
            case "echest":
            case "chest": {
                String canonical;
                if (cleanParam.equals("ff") || cleanParam.equals("pvp")) canonical = "friendlyfire";
                else if (cleanParam.equals("admin")) canonical = "promote";
                else if (cleanParam.equals("home")) canonical = "warp";
                else if (cleanParam.equals("sethome")) canonical = "setwarp";
                else if (cleanParam.equals("delhome")) canonical = "delwarp";
                else if (cleanParam.equals("c")) canonical = "chat";
                else if (cleanParam.equals("chest")) canonical = "echest";
                else canonical = cleanParam;

                String path = "command-toggles." + canonical;
                boolean enabled = true;
                if (plugin.getConfig().contains(path)) {
                    Object val = plugin.getConfig().get(path);
                    if (val instanceof Boolean) {
                        enabled = (Boolean) val;
                    } else if (val instanceof String) {
                        String s = (String) val;
                        enabled = !s.equalsIgnoreCase("off") && !s.equalsIgnoreCase("false") && !s.equalsIgnoreCase("disabled");
                    }
                }
                return enabled ? "on" : "off";
            }
        }

        if (player == null) {
            return "";
        }

        // 6. InTeam general check (context-dependent)
        if (params.equalsIgnoreCase("inteam")) {
            Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            return (team != null && !team.isSystemLocked()) ? "Yes" : "No";
        }

        // 7. General player-context placeholder
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        return getTeamPlaceholder(team, player, params);
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

        int level = 1 + (team.getKills() / 10);

        switch (type.toLowerCase()) {
            case "name":
            case "tag":
            case "displayname":
                return team.getName();
            case "description":
                return team.getDescription();
            case "open":
                return "false"; // Defaulting to invite-only registration
            case "score":
            case "points":
                return String.valueOf(team.getCachedScore());
            case "money":
                return String.format("%.2f", team.getBankBalance());
            case "moneyshort":
                return formatShort(team.getBankBalance());
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
            case "positionscore": {
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
