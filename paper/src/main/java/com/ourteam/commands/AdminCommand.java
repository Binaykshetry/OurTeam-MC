package com.ourteam.commands;

import com.ourteam.OurTeam;
import com.ourteam.model.Team;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Advanced Admin Command System for OurTeam administrators/operators.
 */
public class AdminCommand implements CommandExecutor, TabCompleter {

    private final OurTeam plugin;
    private final List<String> SUB_COMMANDS = Arrays.asList(
        "reload", "disband", "forcejoin", "forcekick", "transfer", "info", "auditlog",
        "eco", "setlevel", "rename", "lock", "unlock", "pvp", "purge", "spy",
        "lockchest", "cleanchest", "backupchest", "restorechest", "resetbank",
        "addscore", "setscore", "forcecreate", "delallwarps", "sethome", "setwarp", "delhome", "delwarp", "home", "warp"
    );

    public AdminCommand(OurTeam plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ourteam.admin") && !sender.isOp()) {
            sender.sendMessage(plugin.getMsg("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendAdminHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload":
                handleReload(sender);
                break;
            case "disband":
                handleForceDisband(sender, args);
                break;
            case "forcejoin":
                handleForceJoin(sender, args);
                break;
            case "forcekick":
                handleForceKick(sender, args);
                break;
            case "transfer":
            case "setowner":
                handleTransfer(sender, args);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            case "auditlog":
                handleAuditLog(sender, args);
                break;
            case "eco":
            case "bank":
                handleEco(sender, args);
                break;
            case "setlevel":
                handleSetLevel(sender, args);
                break;
            case "rename":
                handleRename(sender, args);
                break;
            case "lock":
                handleLock(sender, args);
                break;
            case "unlock":
                handleUnlock(sender, args);
                break;
            case "pvp":
                handlePvPOverride(sender, args);
                break;
            case "purge":
                handlePurge(sender, args);
                break;
            case "spy":
                handleSpy(sender);
                break;
            case "lockchest":
                handleLockChest(sender, args);
                break;
            case "cleanchest":
                handleCleanChest(sender, args);
                break;
            case "backupchest":
                handleBackupChest(sender, args);
                break;
            case "restorechest":
                handleRestoreChest(sender, args);
                break;
            case "resetbank":
                handleResetBank(sender, args);
                break;
            case "addscore":
                handleScoreMod(sender, args, true);
                break;
            case "setscore":
                handleScoreMod(sender, args, false);
                break;
            case "score":
                handleScoreSubcommand(sender, args);
                break;
            case "forcecreate":
                handleForceCreate(sender, args);
                break;
            case "delallwarps":
                handleDelAllWarps(sender, args);
                break;
            case "sethome":
            case "setwarp":
                handleAdminSetHome(sender, args);
                break;
            case "delhome":
            case "delwarp":
                handleAdminDelHome(sender, args);
                break;
            case "home":
            case "warp":
                handleAdminHome(sender, args);
                break;
            default:
                sendAdminHelp(sender);
                break;
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage(plugin.getMsg("config-reloaded"));
        logAction(sender.getName(), "RELOAD", "SYSTEM", "Reloaded configs and messages successfully.");
    }

    private void handleForceDisband(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam disband <teamName>"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(args[1]);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cNo active team identified by '&e" + args[1] + "&c'."));
            return;
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (team.isMember(p.getUniqueId())) {
                p.sendMessage(plugin.getMsg("team-disbanded"));
            }
        }

        plugin.getTeamManager().disbandTeam(team);
        sender.sendMessage(plugin.colorize("&a[Admin] Successfully disbanded and deleted team &b" + team.getName() + "&a."));
        logAction(sender.getName(), "FORCE_DISBAND", team.getName(), "Team forcefully obliterated from the directory.");
    }

    private void handleForceJoin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam forcejoin <player> <teamName>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMsg("player-not-found"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(args[2]);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cTeam not found."));
            return;
        }

        Team oldTeam = plugin.getTeamManager().getPlayerTeam(target.getUniqueId());
        if (oldTeam != null) {
            plugin.getTeamManager().removePlayerFromTeam(target, oldTeam);
        }

        plugin.getTeamManager().addPlayerToTeam(target, team);
        sender.sendMessage(plugin.colorize("&a[Admin] Forcefully aligned &b" + target.getName() + " &awith team &e" + team.getName() + "&a."));
        target.sendMessage(plugin.colorize("&6An administrator has forcefully assigned you to team &e" + team.getName() + "&6."));
        logAction(sender.getName(), "FORCE_JOIN", team.getName(), "Aligned player " + target.getName() + " securely.");
    }

    private void handleForceKick(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam forcekick <player>"));
            return;
        }

        UUID targetUuid;
        String targetName;
        Player targetPlayer = Bukkit.getPlayer(args[1]);

        if (targetPlayer != null) {
            targetUuid = targetPlayer.getUniqueId();
            targetName = targetPlayer.getName();
        } else {
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
            if (offline == null || offline.getUniqueId() == null) {
                sender.sendMessage(plugin.getMsg("player-not-found"));
                return;
            }
            targetUuid = offline.getUniqueId();
            targetName = offline.getName();
        }

        Team team = plugin.getTeamManager().getPlayerTeam(targetUuid);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cPlayer is not in any team."));
            return;
        }

        if (targetPlayer != null) {
            plugin.getTeamManager().removePlayerFromTeam(targetPlayer, team);
        } else {
            team.removeMember(targetUuid);
            team.getRoles().remove(targetUuid.toString());
            plugin.getTeamManager().saveTeam(team);
        }

        if (team.getOwner().equals(targetUuid)) {
            if (team.getMembers().isEmpty()) {
                plugin.getTeamManager().disbandTeam(team);
                sender.sendMessage(plugin.colorize("&eThe team has been disbanded as all members were evicted."));
            } else {
                UUID nextOwner = team.getMembers().iterator().next();
                team.setOwner(nextOwner);
                plugin.getTeamManager().saveTeam(team);
                Player nextOwnerPlayer = Bukkit.getPlayer(nextOwner);
                if (nextOwnerPlayer != null) {
                    nextOwnerPlayer.sendMessage(plugin.colorize("&6You are now the team owner due to administrative reassignment."));
                }
            }
        }

        sender.sendMessage(plugin.colorize("&a[Admin] Forcefully kicked &b" + targetName + " &afrom team &b" + team.getName() + "&a."));
        if (targetPlayer != null) {
            targetPlayer.sendMessage(plugin.colorize("&cAn administrator has evicted you from your team."));
        }
        logAction(sender.getName(), "FORCE_KICK", team.getName(), "Kicked player " + targetName + ".");
    }

    private void handleTransfer(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam transfer <teamName> <player>"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(args[1]);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cTeam not found."));
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(plugin.getMsg("player-not-found"));
            return;
        }

        if (!team.isMember(target.getUniqueId())) {
            Team old = plugin.getTeamManager().getPlayerTeam(target.getUniqueId());
            if (old != null) {
                plugin.getTeamManager().removePlayerFromTeam(target, old);
            }
            team.addMember(target.getUniqueId());
        }

        team.setOwner(target.getUniqueId());
        team.getRoles().put(target.getUniqueId().toString(), "OWNER");
        plugin.getTeamManager().saveTeam(team);

        sender.sendMessage(plugin.colorize("&a[Admin] Transferred ownership of &b" + team.getName() + " &asafely to &e" + target.getName() + "&a."));
        target.sendMessage(plugin.colorize("&6An administrator promoted you to owner of team &e" + team.getName() + "&6."));
        logAction(sender.getName(), "TRANSFER_OWNER", team.getName(), "Assigned owner to " + target.getName());
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam info <teamName | playerName>"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(args[1]);
        if (team == null) {
            Player p = Bukkit.getPlayer(args[1]);
            if (p != null) {
                team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
            }
        }

        if (team == null) {
            sender.sendMessage(plugin.colorize("&cNo active team found matches '&e" + args[1] + "&c'."));
            return;
        }

        sender.sendMessage(plugin.colorize("&8&m========================================"));
        sender.sendMessage(plugin.colorize("&6&lForensic Team Analysis: &e" + team.getName()));
        sender.sendMessage(plugin.colorize("&7» &bRegistry UUID: &e" + team.getId().toString()));
        sender.sendMessage(plugin.colorize("&7» &bOwner: &e" + Bukkit.getOfflinePlayer(team.getOwner()).getName()));
        sender.sendMessage(plugin.colorize("&7» &bBank Balance: &a$" + String.format("%,.2f", team.getBankBalance())));
        
        int lvl = 1 + (team.getKills() / 10);
        sender.sendMessage(plugin.colorize("&7» &bCalculated Level: &dTier " + lvl + " &7(" + team.getKills() + " Kills / " + team.getDeaths() + " Deaths)"));
        sender.sendMessage(plugin.colorize("&7» &bFriendly Fire FF: &e" + (team.isFriendlyFireEnabled() ? "&aON" : "&cOFF") + " &7(Core override: &d" + team.getPvpForceOverride() + "&7)"));
        sender.sendMessage(plugin.colorize("&7» &bLOCKED Status: " + (team.isSystemLocked() ? "&c&lYES &7(Reason: " + team.getLockReason() + ")" : "&aNO")));
        sender.sendMessage(plugin.colorize("&7» &bEnderchest Lock: " + (team.isEchestLocked() ? "&c&lLOCKED" : "&aUNLOCKED")));
        
        java.util.Date activeDate = new java.util.Date(team.getLastActiveTime());
        sender.sendMessage(plugin.colorize("&7» &bLast Active Time: &e" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(activeDate)));

        StringBuilder roster = new StringBuilder();
        for (UUID uuid : team.getMembers()) {
            roster.append("&f").append(Bukkit.getOfflinePlayer(uuid).getName()).append(" &7(").append(team.getRole(uuid)).append("), ");
        }
        String ros = roster.toString();
        if (ros.endsWith(", ")) ros = ros.substring(0, ros.length() - 2);
        sender.sendMessage(plugin.colorize("&7» &bRoster: " + ros));
        
        // Homes
        if (!team.getMultiHomes().isEmpty()) {
            sender.sendMessage(plugin.colorize("&7» &bHomes / Warps: &f" + String.join(", ", team.getMultiHomes().keySet())));
        }
        sender.sendMessage(plugin.colorize("&8&m========================================"));
    }

    private void handleAuditLog(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam auditlog <teamName> [page]"));
            return;
        }

        String query = args[1].toLowerCase();
        int page = 1;
        if (args.length >= 3) {
            try {
                page = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.colorize("&cInvalid page index. Defaulting to 1."));
            }
        }

        File logFile = new File(new File(plugin.getDataFolder(), "admin_logs"), "security_audit.log");
        if (!logFile.exists()) {
            sender.sendMessage(plugin.colorize("&eNo administrative operations have been logged yet."));
            return;
        }

        List<String> matched = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.toLowerCase().contains(query)) {
                    matched.add(line);
                }
            }
        } catch (IOException e) {
            sender.sendMessage(plugin.colorize("&cError read security log."));
            return;
        }

        if (matched.isEmpty()) {
            sender.sendMessage(plugin.colorize("&cNo records found matching '&e" + query + "&c'."));
            return;
        }

        Collections.reverse(matched); // Chronological Reverse

        int pageSize = 8;
        int total = (int) Math.ceil((double) matched.size() / pageSize);
        if (page < 1) page = 1;
        if (page > total) page = total;

        sender.sendMessage(plugin.colorize("&8&m========================================"));
        sender.sendMessage(plugin.colorize("&4&lSecurity Log: &e" + args[1] + " &7(Page " + page + "/" + total + ")"));
        
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, matched.size());

        for (int i = start; i < end; i++) {
            sender.sendMessage(plugin.colorize("&7" + matched.get(i)));
        }
        sender.sendMessage(plugin.colorize("&8&m========================================"));
    }

    private void handleEco(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam eco <give | take | set> <teamName> <amount>"));
            return;
        }

        String mode = args[1].toLowerCase();
        Team team = plugin.getTeamManager().getTeamByName(args[2]);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cTeam not found."));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[3]);
            if (amount < 0) {
                sender.sendMessage(plugin.colorize("&cAmount cannot be negative."));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.colorize("&cInvalid amount: " + args[3]));
            return;
        }

        double oldBal = team.getBankBalance();
        double newBal = oldBal;

        switch (mode) {
            case "give":
                newBal = oldBal + amount;
                team.setBankBalance(newBal);
                sender.sendMessage(plugin.colorize("&a[Admin] Added &e$" + amount + " &ato bank of &b" + team.getName() + "&a."));
                break;
            case "take":
                newBal = Math.max(0, oldBal - amount);
                team.setBankBalance(newBal);
                sender.sendMessage(plugin.colorize("&a[Admin] Subtracted &e$" + amount + " &afrom bank of &b" + team.getName() + "&a."));
                break;
            case "set":
                newBal = amount;
                team.setBankBalance(newBal);
                sender.sendMessage(plugin.colorize("&a[Admin] Set bank balance of &g" + team.getName() + " &ato &e$" + amount + "&a."));
                break;
            default:
                sender.sendMessage(plugin.colorize("&cInvalid format. Use give, take, or set."));
                return;
        }

        plugin.getTeamManager().saveTeam(team);
        logAction(sender.getName(), "ECONOMY_" + mode.toUpperCase(), team.getName(), "Bank balance transfer: $" + oldBal + " -> $" + newBal);
    }

    private void handleSetLevel(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam setlevel <teamName> <level>"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(args[1]);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cTeam not found."));
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
            if (level < 1) {
                sender.sendMessage(plugin.colorize("&cLevel must be 1 or higher."));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.colorize("&cInvalid levels digit."));
            return;
        }

        // Standard dynamic calculation: level = 1 + (kills / 10). Rebuilt to configure kills.
        int kills = (level - 1) * 10;
        int oldKills = team.getKills();
        team.setKills(kills);
        plugin.getTeamManager().saveTeam(team);

        sender.sendMessage(plugin.colorize("&a[Admin] Rebuilt kills on &e" + team.getName() + " &aconfiguring team tier level as &b" + level + " &7(Kills set to " + kills + ")"));
        logAction(sender.getName(), "SET_LEVEL", team.getName(), "Tier Level structured from " + (1 + (oldKills / 10)) + " to " + level);
    }

    private void handleRename(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam rename <teamName> <newUniqueName>"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(args[1]);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cTeam not found."));
            return;
        }

        String newName = args[2];
        String oldName = team.getName();

        // Bypasses regex and checks completely!
        team.setName(newName);
        plugin.getTeamManager().saveTeam(team);

        sender.sendMessage(plugin.colorize("&a[Admin] Forcefully renamed team &b" + oldName + " &ato &e" + newName + " &7(Limits bypassed)"));
        logAction(sender.getName(), "FORCE_RENAME", oldName, "Renamed to " + newName + " successfully.");
    }

    private void handleLock(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam lock <teamName> [reason]"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(args[1]);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cTeam not found."));
            return;
        }

        StringBuilder reason = new StringBuilder();
        if (args.length >= 3) {
            for (int i = 2; i < args.length; i++) {
                reason.append(args[i]).append(" ");
            }
        } else {
            reason.append("Administrative Freeze under investigation.");
        }

        String rString = reason.toString().trim();
        team.setSystemLocked(true);
        team.setLockReason(rString);
        plugin.getTeamManager().saveTeam(team);

        sender.sendMessage(plugin.colorize("&a[Admin] Administratively LOCKED team &e" + team.getName() + " &afor &7" + rString));
        logAction(sender.getName(), "LOCK_TEAM", team.getName(), "Locked: " + rString);
    }

    private void handleUnlock(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam unlock <teamName>"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(args[1]);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cTeam not found."));
            return;
        }

        team.setSystemLocked(false);
        team.setLockReason("");
        plugin.getTeamManager().saveTeam(team);

        sender.sendMessage(plugin.colorize("&a[Admin] UNLOCKED operations on team &b" + team.getName() + "&a successfully."));
        logAction(sender.getName(), "UNLOCK_TEAM", team.getName(), "Operations unfrozen.");
    }

    private void handlePvPOverride(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam pvp <teamName> <forceon | forceoff | none>"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(args[1]);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cTeam not found."));
            return;
        }

        String mode = args[2].toLowerCase();
        String display = "?";

        switch (mode) {
            case "on":
            case "forceon":
                team.setPvpForceOverride("FORCE_ON");
                team.setFriendlyFire(true);
                display = "&a&lFORCED ON";
                break;
            case "off":
            case "forceoff":
                team.setPvpForceOverride("FORCE_OFF");
                team.setFriendlyFire(false);
                display = "&c&lFORCED OFF";
                break;
            case "none":
            case "normal":
                team.setPvpForceOverride("NONE");
                display = "&d&lSTANDARD GUEST RULES";
                break;
            default:
                sender.sendMessage(plugin.colorize("&cInvalid parameter. Use forceon, forceoff, or none."));
                return;
        }

        plugin.getTeamManager().saveTeam(team);
        sender.sendMessage(plugin.colorize("&a[Admin] PvP Override rules on &b" + team.getName() + " &asaved: " + display));
        logAction(sender.getName(), "PVP_OVERRIDE", team.getName(), "Mode mapped to: " + mode.toUpperCase());
    }

    private void handlePurge(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam purge <days>"));
            return;
        }

        int days;
        try {
            days = Integer.parseInt(args[1]);
            if (days < 1) {
                sender.sendMessage(plugin.colorize("&cDays cannot be lower than 1."));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.colorize("&cInvalid digit days parameter."));
            return;
        }

        long threshold = days * 24L * 60L * 60L * 1000L;
        long timeNow = System.currentTimeMillis();
        int count = 0;

        List<Team> toDisband = new ArrayList<>();
        for (Team team : plugin.getTeamManager().getTeams().values()) {
            if (timeNow - team.getLastActiveTime() > threshold) {
                toDisband.add(team);
            }
        }

        for (Team t : toDisband) {
            plugin.getTeamManager().disbandTeam(t);
            count++;
        }

        sender.sendMessage(plugin.colorize("&a[Admin] Successfully purged/deleted &b" + count + " inactive &ateams inactive for &b" + days + " &adays!"));
        logAction(sender.getName(), "PURGE_DB", "ALL", "Purged " + count + " squads inactive for over " + days + " days.");
    }

    private void handleSpy(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.colorize("&cOnly players can utilize chat espionage spy."));
            return;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (plugin.getChatSpyPlayers().contains(uuid)) {
            plugin.getChatSpyPlayers().remove(uuid);
            sender.sendMessage(plugin.colorize("&e[Admin] Chat ESP Spy mode toggled: &c&lOFF"));
            logAction(sender.getName(), "CHATSPY_TOGGLE", "NONE", "Espionage deactivated.");
        } else {
            plugin.getChatSpyPlayers().add(uuid);
            sender.sendMessage(plugin.colorize("&e[Admin] Chat ESP Spy mode toggled: &a&lON"));
            logAction(sender.getName(), "CHATSPY_TOGGLE", "NONE", "Espionage activated.");
        }
    }

    private void handleLockChest(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam lockchest <teamName>"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(args[1]);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cTeam not found."));
            return;
        }

        boolean newVal = !team.isEchestLocked();
        team.setEchestLocked(newVal);
        plugin.getTeamManager().saveTeam(team);

        sender.sendMessage(plugin.colorize("&a[Admin] Enderchest locked status for &b" + team.getName() + " &aset to: " + (newVal ? "&c&lLOCKED" : "&a&lUNLOCKED")));
        logAction(sender.getName(), "LOCK_CHEST", team.getName(), "Echest lock toggled to " + newVal);
    }

    private void handleCleanChest(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam cleanchest <teamName>"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(args[1]);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cTeam not found."));
            return;
        }

        team.getEchest().clear();
        team.updateEchestData();
        plugin.getTeamManager().saveTeam(team);

        sender.sendMessage(plugin.colorize("&a[Admin] Successfully cleared and wiped Enderchest of &b" + team.getName() + "&a."));
        logAction(sender.getName(), "CLEAN_CHEST", team.getName(), "Echest wiped of all contents.");
    }

    private void handleBackupChest(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam backupchest <teamName>"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(args[1]);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cTeam not found."));
            return;
        }

        // Capture data
        team.updateEchestData();
        String rawBase64 = team.getEchestData();

        File backupDir = new File(plugin.getDataFolder(), "backups");
        if (!backupDir.exists()) backupDir.mkdirs();

        File file = new File(backupDir, "echest_" + team.getName().toLowerCase().replaceAll("[^a-zA-Z0-9_-]", "") + ".txt");
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file, false)))) {
            writer.println(rawBase64);
            sender.sendMessage(plugin.colorize("&a[Admin] Created an offline Base64 backup of &e" + team.getName() + " &aEnderchest contents."));
            logAction(sender.getName(), "BACKUP_CHEST", team.getName(), "Backup dumped into: backups/" + file.getName());
        } catch (IOException e) {
            sender.sendMessage(plugin.colorize("&cException while dumping echest backup file."));
        }
    }

    private void handleRestoreChest(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam restorechest <teamName>"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(args[1]);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cTeam not found."));
            return;
        }

        File file = new File(new File(plugin.getDataFolder(), "backups"), "echest_" + team.getName().toLowerCase().replaceAll("[^a-zA-Z0-9_-]", "") + ".txt");
        if (!file.exists()) {
            sender.sendMessage(plugin.colorize("&cNo echest backup found under name: " + file.getName()));
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String rawBase64 = reader.readLine();
            if (rawBase64 != null && !rawBase64.isEmpty()) {
                team.setEchestData(rawBase64);
                plugin.getTeamManager().saveTeam(team);
                sender.sendMessage(plugin.colorize("&a[Admin] Restored Enderchest successfully on &e" + team.getName() + " &afrom latest backup."));
                logAction(sender.getName(), "RESTORE_CHEST", team.getName(), "Restored inventory from backup file");
            } else {
                sender.sendMessage(plugin.colorize("&cBackup file was empty."));
            }
        } catch (IOException e) {
            sender.sendMessage(plugin.colorize("&cException reading echest backup file."));
        }
    }

    private void handleResetBank(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam resetbank <teamName>"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(args[1]);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cTeam not found."));
            return;
        }

        double old = team.getBankBalance();
        team.setBankBalance(0);
        plugin.getTeamManager().saveTeam(team);

        sender.sendMessage(plugin.colorize("&a[Admin] Bank balance of team &e" + team.getName() + " &areset to $0."));
        logAction(sender.getName(), "RESET_BANK", team.getName(), "Reset balance (cleared $" + old + ")");
    }

    private void handleScoreMod(CommandSender sender, String[] args, boolean addition) {
        if (args.length < 3) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam " + (addition ? "addscore" : "setscore") + " <teamName> <points>"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(args[1]);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cTeam not found."));
            return;
        }

        int points;
        try {
            points = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.colorize("&cInvalid parameters score points count."));
            return;
        }

        long oldPoints = team.getGrindingPoints();
        long newPoints = addition ? (oldPoints + points) : points;
        if (newPoints < 0) newPoints = 0;

        team.setGrindingPoints((int) newPoints);
        plugin.getTeamManager().saveTeam(team);

        sender.sendMessage(plugin.colorize("&a[Admin] Modded score (grinding points) of team &b" + team.getName() + " &afrom &e" + oldPoints + " &ato &e" + newPoints + " &7(Total Live TeamScore: &f" + team.getCachedScore() + "&7)"));
        logAction(sender.getName(), "SCORE_MOD", team.getName(), "Modified score grinding points from " + oldPoints + " -> " + newPoints);
    }

    private void handleScoreSubcommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam score <add|set|take> <team> <amount>"));
            return;
        }

        String action = args[1].toLowerCase();
        Team team = plugin.getTeamManager().getTeamByName(args[2]);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cTeam not found."));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.colorize("&cInvalid parameters points amount."));
            return;
        }

        int oldGrindingPower = team.getGrindingPoints();
        int newGrindingPower = oldGrindingPower;

        if (action.equals("add")) {
            newGrindingPower += amount;
        } else if (action.equals("set")) {
            newGrindingPower = amount;
        } else if (action.equals("take") || action.equals("subtract")) {
            newGrindingPower -= amount;
        } else {
            sender.sendMessage(plugin.colorize("&cUnknown score action: " + action + ". Use add, set, or take."));
            return;
        }

        if (newGrindingPower < 0) {
            newGrindingPower = 0;
        }

        team.setGrindingPoints(newGrindingPower);
        plugin.getTeamManager().saveTeam(team);

        sender.sendMessage(plugin.colorize("&a[Admin] Modified score (grinding points) of team &b" + team.getName() + " &afrom &e" + oldGrindingPower + " &ato &e" + newGrindingPower + " &7(Total Live TeamScore: &f" + team.getCachedScore() + "&7)"));
        logAction(sender.getName(), "SCORE_SUBCOMMAND", team.getName(), "Action: " + action + " for points: " + amount + " (old: " + oldGrindingPower + " -> new: " + newGrindingPower + ")");
    }

    private void handleForceCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam forcecreate <teamName> <ownerPlayer>"));
            return;
        }

        String teamName = args[1];
        Player p = Bukkit.getPlayer(args[2]);
        if (p == null) {
            sender.sendMessage(plugin.getMsg("player-not-found"));
            return;
        }

        Team oldTeam = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
        if (oldTeam != null) {
            plugin.getTeamManager().removePlayerFromTeam(p, oldTeam);
        }

        Team team = plugin.getTeamManager().createTeam(teamName, p);
        plugin.updateTabFormatting(p);
        sender.sendMessage(plugin.colorize("&a[Admin] Forcefully created team &e" + team.getName() + " &awith owner &b" + p.getName()));
        p.sendMessage(plugin.colorize("&6An administrator has registered a team for you named &e" + team.getName()));
        logAction(sender.getName(), "FORCE_CREATE", team.getName(), "Forced created squad with owner " + p.getName());
    }

    private void handleDelAllWarps(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam delallwarps <teamName>"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(args[1]);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cTeam not found."));
            return;
        }

        team.getMultiHomes().clear();
        plugin.getTeamManager().saveTeam(team);

        sender.sendMessage(plugin.colorize("&a[Admin] Successfully cleared and deleted ALL warp channels for &b" + team.getName() + "&a."));
        logAction(sender.getName(), "DEL_WARPS", team.getName(), "Cleared all warp endpoints.");
    }

    private void handleAdminSetHome(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.colorize("&cOnly in-game players can execute this command."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam sethome <teamName> [homeName]"));
            return;
        }

        Player player = (Player) sender;
        Team team = plugin.getTeamManager().getTeamByName(args[1]);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cTeam not found."));
            return;
        }

        String homeName = args.length >= 3 ? args[2].toLowerCase() : "home";
        org.bukkit.Location loc = player.getLocation();
        team.setHome(homeName, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        plugin.getTeamManager().saveTeam(team);

        sender.sendMessage(plugin.colorize("&aAdmin successfully set team home '&e" + homeName + "&a' for team &b" + team.getName() + "&a."));
        logAction(sender.getName(), "ADMIN_SETHOME", team.getName(), "Forced set home " + homeName);
    }

    private void handleAdminDelHome(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam delhome <teamName> [homeName]"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(args[1]);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cTeam not found."));
            return;
        }

        String homeName = args.length >= 3 ? args[2].toLowerCase() : "home";
        if (!team.hasHome(homeName)) {
            sender.sendMessage(plugin.colorize("&cTeam home '&e" + homeName + "&c' does not exist for team &b" + team.getName() + "&c."));
            return;
        }

        team.deleteHome(homeName);
        plugin.getTeamManager().saveTeam(team);

        sender.sendMessage(plugin.colorize("&eAdmin successfully deleted team home '&6" + homeName + "&e' from &b" + team.getName() + "&e."));
        logAction(sender.getName(), "ADMIN_DELHOME", team.getName(), "Forced deleted home " + homeName);
    }

    private void handleAdminHome(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.colorize("&cOnly in-game players can execute this command."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.colorize("&cUsage: /adteam home <teamName> [homeName]"));
            return;
        }

        Player player = (Player) sender;
        Team team = plugin.getTeamManager().getTeamByName(args[1]);
        if (team == null) {
            sender.sendMessage(plugin.colorize("&cTeam not found."));
            return;
        }

        String homeName = args.length >= 3 ? args[2].toLowerCase() : "home";
        if (!team.hasHome(homeName)) {
            sender.sendMessage(plugin.colorize("&cTeam home '&e" + homeName + "&c' does not exist for team &b" + team.getName() + "&c."));
            return;
        }

        Team.TeamHome home = team.getHome(homeName);
        org.bukkit.World world = Bukkit.getWorld(home.getWorld());
        if (world == null) {
            sender.sendMessage(plugin.colorize("&cTarget home world is inactive."));
            return;
        }

        org.bukkit.Location loc = new org.bukkit.Location(world, home.getX(), home.getY(), home.getZ(), home.getYaw(), home.getPitch());
        player.teleport(loc);
        sender.sendMessage(plugin.colorize("&aAdmin teleported successfully to team home '&e" + homeName + "&a' of team &b" + team.getName() + "&a."));
        logAction(sender.getName(), "ADMIN_HOME", team.getName(), "Forced warp sethold to " + homeName);
    }

    private void logAction(String operator, String action, String target, String details) {
        String logLine = String.format("[%s] [OVERRIDE] Operator: %s | Action: %s | Target: %s | %s",
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
            operator, action, target, details);

        try {
            File logDir = new File(plugin.getDataFolder(), "admin_logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            File logFile = new File(logDir, "security_audit.log");
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)))) {
                out.println(logLine);
            }
        } catch (IOException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not write to security_audit.log", e);
        }
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(plugin.colorize("&8&m========================================"));
        sender.sendMessage(plugin.colorize("&c&lOurTeam Elite Administrator Center &8(OP / Permission)"));
        sender.sendMessage(plugin.colorize("&e/adteam reload &7- Reload config files and messages"));
        sender.sendMessage(plugin.colorize("&e/adteam disband <team> &7- Force disbands any active team"));
        sender.sendMessage(plugin.colorize("&e/adteam forcejoin <player> <team> &7- Force join player"));
        sender.sendMessage(plugin.colorize("&e/adteam forcekick <player> &7- Force kick player"));
        sender.sendMessage(plugin.colorize("&e/adteam transfer <team> <player> &7- Transfer ownership"));
        sender.sendMessage(plugin.colorize("&e/adteam info <team | player> &7- Forensic details details"));
        sender.sendMessage(plugin.colorize("&e/adteam auditlog <query> [page] &7- Search security audits"));
        sender.sendMessage(plugin.colorize("&e/adteam eco <give|take|set> <team> <amt> &7- Manage economy"));
        sender.sendMessage(plugin.colorize("&e/adteam setlevel <team> <lvl> &7- Override team level"));
        sender.sendMessage(plugin.colorize("&e/adteam rename <team> <name> &7- Direct rename"));
        sender.sendMessage(plugin.colorize("&e/adteam lock <team> [reason] &7- Freeze team actions"));
        sender.sendMessage(plugin.colorize("&e/adteam unlock <team> &7- Unfreeze team actions"));
        sender.sendMessage(plugin.colorize("&e/adteam pvp <team> <forceon|forceoff|none> &7- FriendlyFire force"));
        sender.sendMessage(plugin.colorize("&e/adteam purge <days> &7- Purge teams inactive for X days"));
        sender.sendMessage(plugin.colorize("&e/adteam spy &7- Toggle espionage chat spy"));
        sender.sendMessage(plugin.colorize("&e/adteam lockchest <team> &7- Lock/Unlock team's virtual echest"));
        sender.sendMessage(plugin.colorize("&e/adteam cleanchest <team> &7- Wipe team's echest contents"));
        sender.sendMessage(plugin.colorize("&e/adteam backupchest <team> &7- Base64 backup team's echest"));
        sender.sendMessage(plugin.colorize("&e/adteam restorechest <team> &7- Restore team's echest"));
        sender.sendMessage(plugin.colorize("&e/adteam resetbank <team> &7- Wipes bank holdings"));
        sender.sendMessage(plugin.colorize("&e/adteam addscore <team> <kills> &7- Add kills to team"));
        sender.sendMessage(plugin.colorize("&e/adteam setscore <team> <kills> &7- Explicitly set kills"));
        sender.sendMessage(plugin.colorize("&e/adteam forcecreate <name> <owner> &7- Force register squad"));
        sender.sendMessage(plugin.colorize("&e/adteam delallwarps <team> &7- Erase all team multiwarps"));
        sender.sendMessage(plugin.colorize("&8&m========================================"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("ourteam.admin") && !sender.isOp()) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String search = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();
            for (String sub : SUB_COMMANDS) {
                if (sub.startsWith(search)) {
                    suggestions.add(sub);
                }
            }
            return suggestions;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("disband") || sub.equals("transfer") || sub.equals("info") || sub.equals("auditlog") ||
                sub.equals("eco") || sub.equals("setlevel") || sub.equals("rename") || sub.equals("lock") ||
                sub.equals("unlock") || sub.equals("pvp") || sub.equals("lockchest") || sub.equals("cleanchest") ||
                sub.equals("backupchest") || sub.equals("restorechest") || sub.equals("resetbank") ||
                sub.equals("addscore") || sub.equals("setscore") || sub.equals("forcecreate") || sub.equals("delallwarps") ||
                sub.equals("sethome") || sub.equals("setwarp") || sub.equals("delhome") || sub.equals("delwarp") ||
                sub.equals("home") || sub.equals("warp")) {

                String search = args[1].toLowerCase();
                List<String> teamNames = new ArrayList<>();
                for (Team t : plugin.getTeamManager().getTeams().values()) {
                    if (t.getName().toLowerCase().startsWith(search)) {
                        teamNames.add(t.getName());
                    }
                }
                return teamNames;
            }

            if (sub.equals("forcejoin") || sub.equals("forcekick")) {
                String search = args[1].toLowerCase();
                List<String> playerNames = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(search)) {
                        playerNames.add(p.getName());
                    }
                }
                return playerNames;
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("forcejoin")) {
                String search = args[2].toLowerCase();
                List<String> teamNames = new ArrayList<>();
                for (Team t : plugin.getTeamManager().getTeams().values()) {
                    if (t.getName().toLowerCase().startsWith(search)) {
                        teamNames.add(t.getName());
                    }
                }
                return teamNames;
            }

            if (sub.equals("transfer") || sub.equals("forcecreate")) {
                String search = args[2].toLowerCase();
                List<String> playerNames = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(search)) {
                        playerNames.add(p.getName());
                    }
                }
                return playerNames;
            }

            if (sub.equals("eco")) {
                return Arrays.asList("give", "take", "set");
            }

            if (sub.equals("pvp")) {
                return Arrays.asList("forceon", "forceoff", "none");
            }
        }

        return Collections.emptyList();
    }
}
