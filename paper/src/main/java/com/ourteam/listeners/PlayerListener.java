package com.ourteam.commands;

import com.ourteam.OurTeam;
import com.ourteam.model.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Executes player-facing team commands.
 */
public class TeamCommand implements CommandExecutor, TabCompleter {

    private final OurTeam plugin;

    public TeamCommand(OurTeam plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMsg("only-players"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("ourteam.use")) {
            player.sendMessage(plugin.getMsg("no-permission"));
            return true;
        }

        if (args.length == 0) {
            Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            if (team == null) {
                plugin.getGuiManager().openNoTeamMenu(player);
            } else {
                plugin.getGuiManager().openMainMenu(player, team);
                player.sendMessage(plugin.colorize("&a[OurTeam] Opening Team GUI menu..."));
            }
            return true;
        }

        // Intercept allthecommandseen and shift arguments for execution
        if (args[0].equalsIgnoreCase("allthecommandseen")) {
            if (args.length == 1) {
                sendHelp(player);
                return true;
            } else {
                String[] shiftedArgs = new String[args.length - 1];
                System.arraycopy(args, 1, shiftedArgs, 0, shiftedArgs.length);
                args = shiftedArgs;
            }
        }

        String subCommand = args[0].toLowerCase();
        String canonical = getCanonicalSubCommand(subCommand);
        if (!isCommandEnabled(canonical)) {
            player.sendMessage(plugin.colorize("&c[OurTeam] The '/team " + canonical + "' command feature is currently disabled."));
            return true;
        }

        Team commandTeam = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (commandTeam != null) {
            commandTeam.updateActiveTime();
        }

        // Target team lock check on joining / requesting
        if (canonical.equals("join") || canonical.equals("request")) {
            if (args.length >= 2) {
                Team targetTeam = plugin.getTeamManager().getTeamByName(args[1]);
                if (targetTeam != null && targetTeam.isSystemLocked() && !player.hasPermission("ourteam.admin") && !player.isOp()) {
                    player.sendMessage(plugin.colorize("&c[OurTeam] That team is currently LOCKED by an administrator and cannot be joined."));
                    return true;
                }
            }
        }

        if (commandTeam != null && commandTeam.isSystemLocked() && !player.hasPermission("ourteam.admin") && !player.isOp()) {
            if (isMutatingCommand(canonical)) {
                player.sendMessage(plugin.colorize("&c[OurTeam] Your team is currently LOCKED by a server administrator!"));
                player.sendMessage(plugin.colorize("&cReason: &7" + commandTeam.getLockReason()));
                return true;
            }
        }

        switch (subCommand) {
            case "menu":
            case "gui": {
                Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
                if (team == null) {
                    player.sendMessage(plugin.colorize("&cYou must be in a team to open the menu."));
                } else {
                    plugin.getGuiManager().openMainMenu(player, team);
                }
                break;
            }
            case "settings": {
                Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
                if (team == null) {
                    player.sendMessage(plugin.colorize("&cYou must belong to a team first!"));
                } else {
                    plugin.getGuiManager().openSettingsMenu(player, team);
                }
                break;
            }
            case "create":
                handleCreate(player, args);
                break;
            case "invite":
            case "invited":
                handleInvite(player, args);
                break;
            case "join":
            case "accept":
                handleAccept(player, args);
                break;
            case "request":
                handleRequest(player, args);
                break;
            case "acceptrequest":
                handleAcceptRequest(player, args);
                break;
            case "leave":
                handleLeave(player);
                break;
            case "echest":
            case "chest":
                handleEchest(player);
                break;
            case "kick":
                handleKick(player, args);
                break;
            case "disband":
                handleDisband(player);
                break;
            case "friendlyfire":
            case "ff":
            case "pvp":
                handleFriendlyFire(player);
                break;
            case "admin":
            case "promote":
                handlePromote(player, args);
                break;
            case "demote":
                handleDemote(player, args);
                break;
            case "msg":
                handleMsg(player, args);
                break;
            case "warp":
            case "home":
                handleHome(player, args);
                break;
            case "setwarp":
            case "sethome":
                handleSetHome(player, args);
                break;
            case "delwarp":
            case "delhome":
                handleDelHome(player, args);
                break;
            case "chat":
            case "c":
                handleChatToggle(player);
                break;
            case "info":
                handleInfo(player, args);
                break;
            case "bank":
                handleBank(player);
                break;
            case "list":
                handleList(player);
                break;
            case "top":
                handleTop(player);
                break;
            case "paytoggle":
            case "pay":
                handlePayToggle(player);
                break;
            default:
                player.sendMessage(plugin.colorize("&c[OurTeam] Unknown command. Type &e/team allthecommandseen &cto see all subcommands."));
                break;
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            if (plugin.getTeamManager().getPlayerTeam(player.getUniqueId()) != null) {
                player.sendMessage(plugin.getMsg("already-in-team"));
                return;
            }
            plugin.getGuiManager().openNoTeamMenu(player);
            return;
        }

        if (plugin.getTeamManager().getPlayerTeam(player.getUniqueId()) != null) {
            player.sendMessage(plugin.getMsg("already-in-team"));
            return;
        }

        String teamName = args[1];
        int minLen = plugin.getConfig().getInt("team-settings.min-name-length", 3);
        int maxLen = plugin.getConfig().getInt("team-settings.max-name-length", 12);

        if (teamName.length() < minLen || teamName.length() > maxLen) {
            player.sendMessage(plugin.colorize("&cTeam names must be between " + minLen + " and " + maxLen + " characters long."));
            return;
        }

        for (String blocked : plugin.getConfig().getStringList("team-settings.blocked-names")) {
            if (teamName.toLowerCase().contains(blocked.toLowerCase())) {
                player.sendMessage(plugin.colorize("&cThis team name is blocked or contains inappropriate words."));
                return;
            }
        }

        if (plugin.getTeamManager().getTeamByName(teamName) != null) {
            player.sendMessage(plugin.getMsg("team-already-exists"));
            return;
        }

        Team team = plugin.getTeamManager().createTeam(teamName, player);
        player.sendMessage(plugin.getMsg("team-created").replace("{team}", team.getName()));
        plugin.updateTabFormatting(player);
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.colorize("&cUsage: /team invite <player>"));
            return;
        }

        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(plugin.getMsg("not-in-team"));
            return;
        }

        if (!plugin.hasTeamPermission(team, player.getUniqueId(), "invite")) {
            player.sendMessage(plugin.colorize("&cOnly authorized team ranks can send invites."));
            return;
        }

        if (!team.isMemberInviteEnabled() && !team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
            player.sendMessage(plugin.colorize("&cError: Member inviting is currently disabled by team settings (admins/owner only)."));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(plugin.getMsg("player-not-found"));
            return;
        }

        int maxPlayers = plugin.getConfig().getInt("team-settings.max-players-per-team", 8);
        if (maxPlayers != -1 && team.getMembers().size() >= maxPlayers) {
            player.sendMessage(plugin.colorize("&cYour team is already full (max " + maxPlayers + " players)."));
            return;
        }

        team.invitePlayer(target.getUniqueId());
        player.sendMessage(plugin.getMsg("player-invited").replace("{player}", target.getName()));
        target.sendMessage(plugin.getMsg("invited-by").replace("{team}", team.getName()));
    }

    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.colorize("&cUsage: /team accept <teamName|playerName>"));
            return;
        }

        Team cmdPlayerTeam = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        String argument = args[1];

        // If the sender is already in a team and is authorized, check if they are accepting a player's request
        if (cmdPlayerTeam != null && plugin.hasTeamPermission(cmdPlayerTeam, player.getUniqueId(), "acceptrequest")) {
            Player target = Bukkit.getPlayer(argument);
            UUID targetUuid = target != null ? target.getUniqueId() : Bukkit.getOfflinePlayer(argument).getUniqueId();
            if (cmdPlayerTeam.hasRequest(targetUuid)) {
                handleAcceptRequest(player, args);
                return;
            }
        }

        // Otherwise, standard behavior: accept a team invitation
        if (cmdPlayerTeam != null) {
            player.sendMessage(plugin.getMsg("already-in-team"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(argument);

        if (team == null) {
            player.sendMessage(plugin.colorize("&cError: Specified team '" + argument + "' not found."));
            return;
        }

        boolean directJoin = team.isOpenJoin();
        if (!team.hasInvite(player.getUniqueId()) && !directJoin) {
            player.sendMessage(plugin.colorize("&cError: You do not have an active invitation to join &b" + team.getName() + "&c. Use &e/team request " + team.getName() + " &cto apply to join!"));
            return;
        }

        plugin.getTeamManager().addPlayerToTeam(player, team);
        player.sendMessage(plugin.colorize("&aYou successfully joined team &b" + team.getName() + "&a!" + (directJoin && !team.hasInvite(player.getUniqueId()) ? " &7(Direct Join)" : "")));
        
        // Broadcast to team
        for (UUID memberId : team.getMembers()) {
            Player p = Bukkit.getPlayer(memberId);
            if (p != null) {
                p.sendMessage(plugin.getMsg("player-joined").replace("{player}", player.getName()));
                plugin.updateTabFormatting(p);
            }
        }
    }

    private void handleLeave(Player player) {
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(plugin.getMsg("not-in-team"));
            return;
        }

        if (team.getOwner().equals(player.getUniqueId())) {
            // Notify teammates before disbanding from owner leaving
            for (UUID memberId : team.getMembers()) {
                Player p = Bukkit.getPlayer(memberId);
                if (p != null) {
                    p.sendMessage(plugin.colorize("&c&l[OurTeam] The team owner &e" + player.getName() + " &chas left the team. The team has been automatically disbanded!"));
                    plugin.updateTabFormatting(p);
                }
            }
            
            plugin.getTeamManager().disbandTeam(team);
            player.sendMessage(plugin.colorize("&eYou left your team &b" + team.getName() + "&e, so the team has been automatically disbanded."));
            plugin.updateTabFormatting(player);
            return;
        }

        plugin.getTeamManager().removePlayerFromTeam(player, team);
        player.sendMessage(plugin.getMsg("left-success"));
        plugin.updateTabFormatting(player);

        // Notify teammates
        for (UUID memberId : team.getMembers()) {
            Player p = Bukkit.getPlayer(memberId);
            if (p != null) {
                p.sendMessage(plugin.getMsg("player-left").replace("{player}", player.getName()));
            }
        }
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.colorize("&cUsage: /team kick <player>"));
            return;
        }

        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(plugin.getMsg("not-in-team"));
            return;
        }

        UUID playerUuid = player.getUniqueId();
        if (!plugin.hasTeamPermission(team, playerUuid, "kick")) {
            player.sendMessage(plugin.colorize("&cOnly authorized team ranks can kick members."));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUuid;
        String targetName;

        if (target != null) {
            targetUuid = target.getUniqueId();
            targetName = target.getName();
        } else {
            // Offline player support
            targetUuid = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
            targetName = args[1];
        }

        if (targetUuid.equals(player.getUniqueId())) {
            player.sendMessage(plugin.getMsg("cannot-kick-self"));
            return;
        }

        if (!team.isMember(targetUuid)) {
            player.sendMessage(plugin.colorize("&cThat player is not in your team."));
            return;
        }

        if (team.getRole(targetUuid).equalsIgnoreCase("OWNER")) {
            player.sendMessage(plugin.colorize("&cYou cannot kick the Team Owner!"));
            return;
        }

        String kickerRole = team.getRole(playerUuid);
        String targetRole = team.getRole(targetUuid);
        if ((kickerRole.equalsIgnoreCase("ADMIN") || kickerRole.equalsIgnoreCase("MODERATOR")) && 
                (targetRole.equalsIgnoreCase("ADMIN") || targetRole.equalsIgnoreCase("MODERATOR") || targetRole.equalsIgnoreCase("OWNER"))) {
            player.sendMessage(plugin.colorize("&cAdmins cannot kick other admins or the owner."));
            return;
        }

        team.removeMember(targetUuid);
        plugin.getTeamManager().saveAll();

        player.sendMessage(plugin.getMsg("player-kicked").replace("{player}", targetName));

        if (target != null) {
            target.sendMessage(plugin.getMsg("kicked-success"));
            plugin.updateTabFormatting(target);
        }

        for (UUID memberId : team.getMembers()) {
            Player p = Bukkit.getPlayer(memberId);
            if (p != null) {
                p.sendMessage(plugin.colorize("&e" + targetName + " has been kicked from the team."));
            }
        }
    }

    private void handleFriendlyFire(Player player) {
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(plugin.getMsg("not-in-team"));
            return;
        }

        if (!plugin.hasTeamPermission(team, player.getUniqueId(), "pvp")) {
            player.sendMessage(plugin.colorize("&cOnly authorized team ranks can toggle friendly fire."));
            return;
        }

        boolean newVal = !team.isFriendlyFireEnabled();
        team.setFriendlyFire(newVal);
        plugin.getTeamManager().saveAll();

        String stateStr = newVal ? "ENABLED" : "DISABLED";
        String formattedMsg = plugin.getMsg("friendly-fire-toggle").replace("{state}", stateStr);
        
        for (UUID memberId : team.getMembers()) {
            Player p = Bukkit.getPlayer(memberId);
            if (p != null) {
                p.sendMessage(formattedMsg);
            }
        }
    }

    private void handleSetHome(Player player, String[] args) {
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(plugin.getMsg("not-in-team"));
            return;
        }

        boolean isWarp = args[0].equalsIgnoreCase("setwarp");

        // Rank Check
        if (!plugin.hasTeamPermission(team, player.getUniqueId(), isWarp ? "setwarp" : "sethome")) {
            player.sendMessage(plugin.colorize("&cHey, your rank does not have permission to set team " + (isWarp ? "warps" : "homes") + "."));
            return;
        }

        // Permission check
        String requiredPerm = isWarp ? "ourteam.setwarp" : "ourteam.sethome";
        if (!player.hasPermission(requiredPerm)) {
            player.sendMessage(plugin.colorize("&cYou do not have the '" + requiredPerm + "' permission."));
            return;
        }

        String labelName = args.length >= 2 ? args[1].toLowerCase() : (isWarp ? "warp" : "home");

        // Check for sethome.others / setwarp.others
        if (args.length >= 3) {
            String othersPerm = isWarp ? "ourteam.setwarp.others" : "ourteam.sethome.others";
            if (!player.hasPermission(othersPerm)) {
                player.sendMessage(plugin.colorize("&cYou do not have the '" + othersPerm + "' permission to set " + (isWarp ? "warps" : "homes") + " for others."));
                return;
            }
            Player targetPlayer = Bukkit.getPlayer(args[2]);
            if (targetPlayer == null) {
                player.sendMessage(plugin.getMsg("player-not-found"));
                return;
            }
            if (!team.isMember(targetPlayer.getUniqueId())) {
                player.sendMessage(plugin.colorize("&cThat player is not in your team."));
                return;
            }

            int maxLimit = isWarp ? getMaxWarps(player) : getMaxHomes(player);
            int currentCount = isWarp ? team.getMultiWarps().size() : team.getMultiHomes().size();
            boolean alreadyHas = isWarp ? team.hasWarp(labelName) : team.hasHome(labelName);
            if (!alreadyHas && currentCount >= maxLimit) {
                player.sendMessage(plugin.colorize("&cYour team has reached the maximum limit of " + maxLimit + " " + (isWarp ? "warp(s)" : "home(s)") + " for your current rank."));
                return;
            }

            Location loc = targetPlayer.getLocation();
            if (isWarp) {
                team.setWarp(labelName, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            } else {
                team.setHome(labelName, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            }
            plugin.getTeamManager().saveAll();

            player.sendMessage(plugin.colorize("&aTeam " + (isWarp ? "warp" : "home") + " '&e" + labelName + "&a' set successfully at &e" + targetPlayer.getName() + "&a's location!"));
            targetPlayer.sendMessage(plugin.colorize("&e" + player.getName() + " &aset your team " + (isWarp ? "warp" : "home") + " '&e" + labelName + "&a' at your location."));
            return;
        }

        // Standard sethome / setwarp
        int maxLimit = isWarp ? getMaxWarps(player) : getMaxHomes(player);
        int currentCount = isWarp ? team.getMultiWarps().size() : team.getMultiHomes().size();
        boolean alreadyHas = isWarp ? team.hasWarp(labelName) : team.hasHome(labelName);
        if (!alreadyHas && currentCount >= maxLimit) {
            player.sendMessage(plugin.colorize("&cYour team has reached the maximum limit of " + maxLimit + " " + (isWarp ? "warp(s)" : "home(s)") + " for your current rank."));
            return;
        }

        Location loc = player.getLocation();
        if (isWarp) {
            team.setWarp(labelName, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        } else {
            team.setHome(labelName, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        }
        plugin.getTeamManager().saveAll();

        player.sendMessage(plugin.colorize("&aTeam " + (isWarp ? "warp" : "home") + " '&e" + labelName + "&a' set successfully at your current location!"));
    }

    private void handleHome(Player player, String[] args) {
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(plugin.getMsg("not-in-team"));
            return;
        }

        boolean isWarp = args[0].equalsIgnoreCase("warp");

        // Permission check
        if (!player.hasPermission("ourteam.warp") && !player.hasPermission("ourteam.home")) {
            player.sendMessage(plugin.colorize("&cYou do not have permission to teleport."));
            return;
        }

        String labelName = args.length >= 2 ? args[1].toLowerCase() : (isWarp ? "warp" : "home");

        boolean exists = isWarp ? team.hasWarp(labelName) : team.hasHome(labelName);
        if (!exists) {
            player.sendMessage(plugin.colorize("&cThat team " + (isWarp ? "warp" : "home") + " '&e" + labelName + "&c' does not exist."));
            return;
        }

        Team.TeamHome targetLoc = isWarp ? team.getWarp(labelName) : team.getHome(labelName);
        World world = Bukkit.getWorld(targetLoc.getWorld());
        if (world == null) {
            player.sendMessage(plugin.colorize("&cYour target world is currently inactive."));
            return;
        }

        Location loc = new Location(world, targetLoc.getX(), targetLoc.getY(), targetLoc.getZ(), targetLoc.getYaw(), targetLoc.getPitch());
        plugin.startTeleport(player, loc, labelName, isWarp);
    }

    private void handleDelHome(Player player, String[] args) {
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(plugin.getMsg("not-in-team"));
            return;
        }

        boolean isWarp = args[0].equalsIgnoreCase("delwarp");

        if (!plugin.hasTeamPermission(team, player.getUniqueId(), isWarp ? "delwarp" : "delhome")) {
            player.sendMessage(plugin.colorize("&cHey, your rank does not have permission to delete team " + (isWarp ? "warps" : "homes") + "."));
            return;
        }

        if (!player.hasPermission("ourteam.delhome") && !player.hasPermission("ourteam.delwarp")) {
            player.sendMessage(plugin.colorize("&cYou do not have permission to delete."));
            return;
        }

        String labelName = args.length >= 2 ? args[1].toLowerCase() : (isWarp ? "warp" : "home");

        boolean exists = isWarp ? team.hasWarp(labelName) : team.hasHome(labelName);
        if (!exists) {
            player.sendMessage(plugin.colorize("&cSpecified team " + (isWarp ? "warp" : "home") + " '&e" + labelName + "&c' does not exist."));
            return;
        }

        if (isWarp) {
            team.deleteWarp(labelName);
        } else {
            team.deleteHome(labelName);
        }
        plugin.getTeamManager().saveAll();

        player.sendMessage(plugin.colorize("&eSuccessfully deleted team " + (isWarp ? "warp" : "home") + " '&6" + labelName + "&e'."));
    }

    private int getMaxHomes(Player player) {
        if (player.hasPermission("ourteam.homes.unlimited")) {
            return 999;
        }
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("ourteam.homes." + i)) {
                return i;
            }
        }
        return plugin.getConfig().getInt("cooldowns-and-teleportation.max-homes-per-team", 1);
    }

    private int getMaxWarps(Player player) {
        if (player.hasPermission("ourteam.warps.unlimited")) {
            return 999;
        }
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("ourteam.warps." + i)) {
                return i;
            }
        }
        return plugin.getConfig().getInt("cooldowns-and-teleportation.max-warps-per-team", 1);
    }

    private void handleChatToggle(Player player) {
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(plugin.getMsg("not-in-team"));
            return;
        }

        if (!team.isTeamChatEnabled()) {
            player.sendMessage(plugin.colorize("&cError: Team chat is currently disabled by team settings."));
            return;
        }

        plugin.getTeamManager().toggleTeamChat(player.getUniqueId());
        boolean isToggled = plugin.getTeamManager().isTeamChatToggled(player.getUniqueId());
        String modeStr = isToggled ? "TEAM" : "PUBLIC";
        player.sendMessage(plugin.getMsg("chat-channel-toggled").replace("{state}", modeStr));
    }

    private void handleInfo(Player player, String[] args) {
        Team team;
        if (args.length < 2) {
            team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            if (team == null) {
                player.sendMessage(plugin.getMsg("not-in-team"));
                return;
            }
        } else {
            team = plugin.getTeamManager().getTeamByName(args[1]);
        }

        if (team == null) {
            player.sendMessage(plugin.colorize("&cSpecified team not found."));
            return;
        }

        Player ownerPlayer = Bukkit.getPlayer(team.getOwner());
        String ownerName = ownerPlayer != null ? ownerPlayer.getName() : Bukkit.getOfflinePlayer(team.getOwner()).getName();

        player.sendMessage(plugin.colorize("&8&m========================================"));
        player.sendMessage(plugin.colorize("&3&lTEAM DETAILS: &b&l" + team.getName()));
        player.sendMessage(plugin.colorize("&fDescription: &7" + team.getDescription()));
        player.sendMessage(plugin.colorize("&fOwner: &e" + ownerName));
        player.sendMessage(plugin.colorize("&fPvP Friendly Fire: &e" + (team.isFriendlyFireEnabled() ? "&aEnabled" : "&cDisabled")));
        player.sendMessage(plugin.colorize("&fHas Home Spawn: &e" + (team.hasHome() ? "&aYes" : "&cNo")));
        player.sendMessage(plugin.colorize("&fMember Count: &7" + team.getMembers().size()));
        player.sendMessage(plugin.colorize("&fRoster:"));
        
        StringBuilder roster = new StringBuilder();
        for (UUID memberUuid : team.getMembers()) {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null) {
                roster.append("&a").append(p.getName()).append("&7, ");
            } else {
                roster.append("&7").append(Bukkit.getOfflinePlayer(memberUuid).getName()).append("&7, ");
            }
        }
        if (roster.length() > 4) {
            roster.setLength(roster.length() - 4);
        }
        player.sendMessage(plugin.colorize("  &7" + roster.toString()));
        player.sendMessage(plugin.colorize("&8&m========================================"));
    }

    private void handleList(Player player) {
        plugin.getGuiManager().openTeamsListMenu(player);
    }

    private void handleTop(Player player) {
        // Fetch all teams
        java.util.List<Team> sorted = new java.util.ArrayList<>(plugin.getTeamManager().getAllTeams());
        // For each, recalculate score
        for (Team t : sorted) {
            t.recalculateScore(plugin);
        }
        // Sort from highest to lowest score
        sorted.sort((t1, t2) -> Integer.compare(t2.getCachedScore(), t1.getCachedScore()));

        player.sendMessage(plugin.colorize("&8&m========================================"));
        player.sendMessage(plugin.colorize("&#33CCFF&lL E A D E R B O A R D &7- &#FFCC00&lTOP 10 TEAMS"));

        // Add upper message
        Team myTeam = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (myTeam != null) {
            myTeam.recalculateScore(plugin);
            int myRank = myTeam.getRankPosition(plugin);
            int total = sorted.size();
            player.sendMessage(plugin.colorize("&#00FFCCYour Team: &f" + myTeam.getName() + " &7| &#FFCC00Rank: &e#" + myRank + "/" + total + " &7| &e" + myTeam.getCachedScore() + " pts"));
        } else {
            player.sendMessage(plugin.colorize("&cYou are not in a team. Join or create a team to compete!"));
        }
        player.sendMessage(plugin.colorize("&8&m========================================"));

        int limit = Math.min(10, sorted.size());
        if (limit == 0) {
            player.sendMessage(plugin.colorize("&7No teams have registered yet!"));
        } else {
            for (int i = 0; i < limit; i++) {
                Team team = sorted.get(i);
                String prefix = "";
                switch (i) {
                    case 0: prefix = "&#FFD700&l1st "; break;
                    case 1: prefix = "&#C0C0C0&l2nd "; break;
                    case 2: prefix = "&#CD7F32&l3rd "; break;
                    default: prefix = "&f&l" + (i + 1) + "th "; break;
                }
                player.sendMessage(plugin.colorize(prefix + " &#00FFCC" + team.getName() + " &7- &e" + team.getCachedScore() + " pts &7(&e" + team.getMembers().size() + " &7members)"));
            }
        }
        player.sendMessage(plugin.colorize("&8&m========================================"));
    }

    private void handleEchest(Player player) {
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(plugin.colorize("&c[OurTeam] You must be in a team to access the team Enderchest!"));
            return;
        }
        if (team.isEchestLocked() && !player.hasPermission("ourteam.admin") && !player.isOp()) {
            player.sendMessage(plugin.colorize("&c[OurTeam] Your team's shared virtual Enderchest is currently LOCKED by an administrator!"));
            return;
        }
        if (team.isSystemLocked() && !player.hasPermission("ourteam.admin") && !player.isOp()) {
            player.sendMessage(plugin.colorize("&c[OurTeam] Your team is currently LOCKED by a server administrator!"));
            player.sendMessage(plugin.colorize("&cReason: &7" + team.getLockReason()));
            return;
        }
        if (!plugin.hasChestPermission(team, player.getUniqueId(), "open-chest")) {
            player.sendMessage(plugin.colorize("&cError: You do not have the 'open-chest' permission for this team container."));
            return;
        }
        player.openInventory(team.getEchest());
        player.sendMessage(plugin.colorize("&a[OurTeam] Opening team's shared virtual Enderchest inventory... Close it to save."));
    }

    private void handleBank(Player player) {
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(plugin.getMsg("not-in-team"));
            return;
        }
        if (!plugin.hasTeamPermission(team, player.getUniqueId(), "bank")) {
            player.sendMessage(plugin.colorize("&cError: Your rank is not authorized to access the Team Bank."));
            return;
        }
        openBankInventory(player, team, plugin);
    }

    public static void openBankInventory(Player player, Team team, OurTeam plugin) {
        plugin.getGuiManager().openBankMenu(player, team);
    }

    private static org.bukkit.inventory.ItemStack createGuiItem(org.bukkit.Material mat, String name, String lore, OurTeam plugin) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(mat);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.colorize(name));
        java.util.List<String> l = new java.util.ArrayList<>();
        l.add(plugin.colorize(lore));
        l.add(plugin.colorize("&eClick to execute!"));
        meta.setLore(l);
        item.setItemMeta(meta);
        return item;
    }

    private void handleDisband(Player player) {
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(plugin.getMsg("not-in-team"));
            return;
        }

        if (!team.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(plugin.colorize("&cOnly the Team Owner has the authority to disband the team."));
            return;
        }

        // Notify teammates before disbanding
        for (UUID memberId : team.getMembers()) {
            Player p = Bukkit.getPlayer(memberId);
            if (p != null) {
                p.sendMessage(plugin.colorize("&c&l[OurTeam] Your team " + team.getName() + " has been disbanded by the owner."));
            }
        }
        
        plugin.getTeamManager().disbandTeam(team);
        player.sendMessage(plugin.colorize("&aYour team has been successfully disbanded."));
    }

    private void handlePromote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.colorize("&cUsage: /team admin <player>"));
            return;
        }

        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(plugin.getMsg("not-in-team"));
            return;
        }

        if (!team.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(plugin.colorize("&cOnly the Team Owner can promote teammates."));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUuid;
        String targetName;

        if (target != null) {
            targetUuid = target.getUniqueId();
            targetName = target.getName();
        } else {
            targetUuid = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
            targetName = args[1];
        }

        if (!team.isMember(targetUuid)) {
            player.sendMessage(plugin.colorize("&cThat player is not in your team."));
            return;
        }

        if (team.getRole(targetUuid).equalsIgnoreCase("OWNER")) {
            player.sendMessage(plugin.colorize("&cYou cannot promote the Team Owner!"));
            return;
        }

        boolean success = team.promote(targetUuid);
        if (success) {
            plugin.getTeamManager().saveAll();
            player.sendMessage(plugin.colorize("&aSuccessfully promoted &e" + targetName + " &ato &bAdmin&a!"));
            if (target != null && target.isOnline()) {
                target.sendMessage(plugin.colorize("&a&lYou have been promoted to Admin of team &b" + team.getName() + "&a!"));
            }
            // Notify team members
            for (UUID memberId : team.getMembers()) {
                Player p = Bukkit.getPlayer(memberId);
                if (p != null && !p.getUniqueId().equals(player.getUniqueId()) && (!p.getUniqueId().equals(targetUuid))) {
                    p.sendMessage(plugin.colorize("&e" + targetName + " has been promoted to Admin."));
                }
            }
        } else {
            player.sendMessage(plugin.colorize("&cThat player is already an Admin or higher rank."));
        }
    }

    private void handleDemote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.colorize("&cUsage: /team demote <player>"));
            return;
        }

        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(plugin.getMsg("not-in-team"));
            return;
        }

        if (!team.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(plugin.colorize("&cOnly the Team Owner can demote teammates."));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUuid;
        String targetName;

        if (target != null) {
            targetUuid = target.getUniqueId();
            targetName = target.getName();
        } else {
            targetUuid = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
            targetName = args[1];
        }

        if (!team.isMember(targetUuid)) {
            player.sendMessage(plugin.colorize("&cThat player is not in your team."));
            return;
        }

        if (team.getRole(targetUuid).equalsIgnoreCase("OWNER")) {
            player.sendMessage(plugin.colorize("&cYou cannot demote the Team Owner!"));
            return;
        }

        boolean success = team.demote(targetUuid);
        if (success) {
            plugin.getTeamManager().saveAll();
            player.sendMessage(plugin.colorize("&eSuccessfully demoted &c" + targetName + " &eto &7Member&e."));
            if (target != null && target.isOnline()) {
                target.sendMessage(plugin.colorize("&c&lYou have been demoted to Member of team &b" + team.getName() + "&c."));
            }
            // Notify team members
            for (UUID memberId : team.getMembers()) {
                Player p = Bukkit.getPlayer(memberId);
                if (p != null && !p.getUniqueId().equals(player.getUniqueId()) && (!p.getUniqueId().equals(targetUuid))) {
                    p.sendMessage(plugin.colorize("&e" + targetName + " has been demoted to standard Member."));
                }
            }
        } else {
            player.sendMessage(plugin.colorize("&cThat player is already a standard Member."));
        }
    }

    private void handleMsg(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.colorize("&cUsage: /team msg <message>"));
            return;
        }

        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(plugin.getMsg("not-in-team"));
            return;
        }

        StringBuilder msgBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            msgBuilder.append(args[i]).append(" ");
        }
        String message = msgBuilder.toString().trim();

        String format = plugin.getConfig().getString("chat-settings.team-chat-format", "&3[Team Chat] &b{player}&7: &f{message}");
        String formattedMessage = plugin.colorize(format
                .replace("{player}", player.getName())
                .replace("{team}", team.getName())
                .replace("{message}", message)
        );

        for (UUID memberUuid : team.getMembers()) {
            Player teammate = Bukkit.getPlayer(memberUuid);
            if (teammate != null && teammate.isOnline()) {
                teammate.sendMessage(formattedMessage);
            }
        }
        plugin.getLogger().info("[OurTeam Msg - " + team.getName() + "] " + player.getName() + ": " + message);
    }

    private void handleRequest(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.colorize("&cUsage: /team request <teamName>"));
            return;
        }

        if (!player.hasPermission("ourteam.request")) {
            player.sendMessage(plugin.colorize("&cYou do not have permission to request to join teams (ourteam.request)."));
            return;
        }

        String teamName = args[1];
        Team targetTeam = plugin.getTeamManager().getTeamByName(teamName);

        if (targetTeam == null) {
            player.sendMessage(plugin.colorize("&cSpecified team '&e" + teamName + "&c' not found."));
            return;
        }

        if (targetTeam.isMember(player.getUniqueId())) {
            player.sendMessage(plugin.colorize("&cYou are already a member of this team."));
            return;
        }

        if (targetTeam.hasRequest(player.getUniqueId())) {
            player.sendMessage(plugin.colorize("&cYou have already sent a pending join request to &b" + targetTeam.getName() + "&c."));
            return;
        }

        // Add request
        targetTeam.addRequest(player.getUniqueId());
        plugin.getTeamManager().saveAll();

        player.sendMessage(plugin.colorize("&aYour request to join &b" + targetTeam.getName() + " &ahas been successfully transmitted!"));

        // Notify officers
        for (UUID memberId : targetTeam.getMembers()) {
            if (targetTeam.isModeratorOrHigher(memberId)) {
                Player officer = Bukkit.getPlayer(memberId);
                if (officer != null && officer.isOnline()) {
                    officer.sendMessage(plugin.colorize("&b&l[OurTeam] &e" + player.getName() + " &7has requested to join your team! Use &a/team accept " + player.getName() + " &7or &a/team acceptrequest " + player.getName() + " &7to accept him."));
                }
            }
        }
    }

    private void handleAcceptRequest(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.colorize("&cUsage: /team acceptrequest <playerName>"));
            return;
        }

        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(plugin.getMsg("not-in-team"));
            return;
        }

        if (!plugin.hasTeamPermission(team, player.getUniqueId(), "acceptrequest")) {
            player.sendMessage(plugin.colorize("&cHey, your rank does not have permission to accept team join requests."));
            return;
        }

        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        UUID targetUuid = targetPlayer != null ? targetPlayer.getUniqueId() : Bukkit.getOfflinePlayer(targetName).getUniqueId();

        if (!team.hasRequest(targetUuid)) {
            player.sendMessage(plugin.colorize("&cPlayer '&e" + targetName + "&c' has not requested to join your team."));
            return;
        }

        int maxPlayers = plugin.getConfig().getInt("team-settings.max-players-per-team", 8);
        if (maxPlayers != -1 && team.getMembers().size() >= maxPlayers) {
            player.sendMessage(plugin.colorize("&cYour team is already full (max " + maxPlayers + " players)."));
            return;
        }

        // Remove teammate from their old team if currently in one (fluid transition)
        Team oldTeam = plugin.getTeamManager().getPlayerTeam(targetUuid);
        if (oldTeam != null) {
            if (targetPlayer != null) {
                plugin.getTeamManager().removePlayerFromTeam(targetPlayer, oldTeam);
            } else {
                oldTeam.removeMember(targetUuid);
                plugin.getTeamManager().saveAll();
            }
        }

        // Remove from requests list
        team.removeRequest(targetUuid);

        // Add to team
        if (targetPlayer != null) {
            plugin.getTeamManager().addPlayerToTeam(targetPlayer, team);
        } else {
            team.addMember(targetUuid);
        }
        plugin.getTeamManager().saveAll();

        player.sendMessage(plugin.colorize("&aRequest from &e" + targetName + " &aaccepted. They are now on the roster!"));

        if (targetPlayer != null) {
            targetPlayer.sendMessage(plugin.colorize("&a&lYour request to join &b" + team.getName() + " &awas accepted by &e" + player.getName() + "&a! Welcome."));
            plugin.updateTabFormatting(targetPlayer);
        }

        // Notify team
        for (UUID memberId : team.getMembers()) {
            Player p = Bukkit.getPlayer(memberId);
            if (p != null) {
                p.sendMessage(plugin.colorize("&e" + targetName + " has joined the team."));
                plugin.updateTabFormatting(p);
            }
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(plugin.colorize("&8&m========================================"));
        player.sendMessage(plugin.colorize("&3&lOurTeam Command Reference:"));
        player.sendMessage(plugin.colorize("&e/team create <name> &7- Form a new team"));
        player.sendMessage(plugin.colorize("&e/team invite <player> &7- Invite teammate (Admin+)"));
        player.sendMessage(plugin.colorize("&e/team request <team> &7- Request to join another team"));
        player.sendMessage(plugin.colorize("&e/team accept <team|player> &7- Accept invite or player join request"));
        player.sendMessage(plugin.colorize("&e/team leave &7- Exit your current team (Owner leaves = disband)"));
        player.sendMessage(plugin.colorize("&e/team kick <player> &7- Remove player from team (Admin+)"));
        player.sendMessage(plugin.colorize("&e/team disband &7- Completely delete the team (Owner only)"));
        player.sendMessage(plugin.colorize("&e/team admin <player> &7- Give Admin role to teammate (Owner only)"));
        player.sendMessage(plugin.colorize("&e/team demote <player> &7- Lower teammate's internal rank (Owner only)"));
        player.sendMessage(plugin.colorize("&e/team msg <message> &7- Message online teammates quickly"));
        player.sendMessage(plugin.colorize("&e/team echest &7- Access the shared team virtual Enderchest"));
        player.sendMessage(plugin.colorize("&e/team bank &7- Access the dynamic team bank and earn interest"));
        player.sendMessage(plugin.colorize("&e/team chat &7- Toggle team-only chat channels"));
        player.sendMessage(plugin.colorize("&e/team pvp &7- Toggle friendly-fire PvP protection"));
        player.sendMessage(plugin.colorize("&e/team sethome [name] &7- Set named/default home spawn (Admin+)"));
        player.sendMessage(plugin.colorize("&e/team home [name] &7- Teleport to a team home/spawn"));
        player.sendMessage(plugin.colorize("&e/team setwarp <name> &7- Set a named team warp (Admin+)"));
        player.sendMessage(plugin.colorize("&e/team warp <name> &7- Teleport to a team warp"));
        player.sendMessage(plugin.colorize("&e/team delhome [name] &7- Delete a team home/warp (Admin+)"));
        player.sendMessage(plugin.colorize("&e/team info [name] &7- Display details of your/any team"));
        player.sendMessage(plugin.colorize("&e/team list &7- Show all registered server teams"));
        player.sendMessage(plugin.colorize("&8&m========================================"));
    }

    public String getCanonicalSubCommand(String sub) {
        if (sub == null) return "";
        sub = sub.toLowerCase();
        switch (sub) {
            case "menu":
            case "gui":
                return "menu";
            case "settings":
                return "settings";
            case "create":
                return "create";
            case "invite":
            case "invited":
                return "invite";
            case "join":
            case "accept":
                return "join";
            case "request":
                return "request";
            case "acceptrequest":
                return "acceptrequest";
            case "leave":
                return "leave";
            case "echest":
            case "chest":
                return "echest";
            case "kick":
                return "kick";
            case "disband":
                return "disband";
            case "friendlyfire":
            case "ff":
            case "pvp":
                return "friendlyfire";
            case "admin":
            case "promote":
                return "promote";
            case "demote":
                return "demote";
            case "msg":
                return "msg";
            case "warp":
            case "home":
                return "warp";
            case "setwarp":
            case "sethome":
                return "setwarp";
            case "delwarp":
            case "delhome":
                return "delwarp";
            case "chat":
            case "c":
                return "chat";
            case "info":
                return "info";
            case "bank":
                return "bank";
            case "list":
                return "list";
            case "top":
                return "top";
            default:
                return sub;
        }
    }

    public boolean isCommandEnabled(String canonical) {
        String path = "command-toggles." + canonical;
        if (plugin.getConfig().contains(path)) {
            Object val = plugin.getConfig().get(path);
            if (val instanceof Boolean) {
                return (Boolean) val;
            }
            if (val instanceof String) {
                String str = (String) val;
                return !str.equalsIgnoreCase("off") && !str.equalsIgnoreCase("false") && !str.equalsIgnoreCase("disabled");
            }
        }
        return true;
    }

    private boolean isMutatingCommand(String canonical) {
        switch (canonical) {
            case "settings":
            case "join":
            case "leave":
            case "disband":
            case "invite":
            case "kick":
            case "promote":
            case "demote":
            case "friendlyfire":
            case "setwarp":
            case "delwarp":
            case "acceptrequest":
            case "request":
            case "bank":
                return true;
            default:
                return false;
        }
    }

    private void handlePayToggle(Player player) {
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(plugin.getMsg("not-in-team"));
            return;
        }

        if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
            player.sendMessage(plugin.colorize("&cOnly the Team Owner and Admins can toggle TeamPay settings."));
            return;
        }

        boolean newVal = !team.isPayToggle();
        team.setPayToggle(newVal);
        plugin.getTeamManager().saveTeam(team);

        String stateStr = newVal ? "&a&lENABLED" : "&c&lDISABLED";
        String msg = plugin.colorize("&6&l[TeamPay] &fDeposits for our team bank have been set to: " + stateStr);

        for (UUID memberId : team.getMembers()) {
            Player p = Bukkit.getServer().getPlayer(memberId);
            if (p != null) {
                p.sendMessage(msg);
            }
        }
    }

    private void sendNoTeamMenu(Player player) {
        player.sendMessage(plugin.colorize("&8&m========================================"));
        player.sendMessage(plugin.colorize("&6&l          » NO TEAM DETECTED «"));
        player.sendMessage(plugin.colorize("&7You do not belong to any team currently."));
        player.sendMessage(plugin.colorize("&7Please select one of the options below:"));
        player.sendMessage("");

        // Option 1: Create Team
        try {
            net.md_5.bungee.api.chat.TextComponent title1 = new net.md_5.bungee.api.chat.TextComponent(plugin.colorize("&e&l[Option 1] &6&lCreate a New Team"));
            net.md_5.bungee.api.chat.TextComponent subtitle1 = new net.md_5.bungee.api.chat.TextComponent(plugin.colorize("\n&7Form an organization. Click to type: "));
            net.md_5.bungee.api.chat.TextComponent cmd1 = new net.md_5.bungee.api.chat.TextComponent(plugin.colorize("&a&n/team create <name>"));
            cmd1.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND, "/team create "));
            cmd1.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, 
                new net.md_5.bungee.api.chat.BaseComponent[]{ new net.md_5.bungee.api.chat.TextComponent(plugin.colorize("&eClick to suggest the creation command!")) }));
            
            net.md_5.bungee.api.chat.TextComponent option1 = new net.md_5.bungee.api.chat.TextComponent();
            option1.addExtra(title1);
            option1.addExtra(subtitle1);
            option1.addExtra(cmd1);
            player.spigot().sendMessage(option1);
        } catch (Throwable t) {
            player.sendMessage(plugin.colorize("&e&l[Option 1] &6&lCreate a New Team"));
            player.sendMessage(plugin.colorize("&7Use command: &a/team create <name>"));
        }

        player.sendMessage("");
        player.sendMessage(plugin.colorize("&e&l[Option 2] &6&lJoin or Request an Existing Team"));
        
        java.util.Collection<Team> allTeams = plugin.getTeamManager().getAllTeams();
        if (allTeams.isEmpty()) {
            player.sendMessage(plugin.colorize("&7There are currently no existing teams on the server. Be the first to create one!"));
        } else {
            player.sendMessage(plugin.colorize("&7Click on a team's request button to send a join request:"));
            for (Team t : allTeams) {
                try {
                    net.md_5.bungee.api.chat.TextComponent teamLine = new net.md_5.bungee.api.chat.TextComponent(plugin.colorize("&8 - &b" + t.getName() + " &7(Score: &e" + t.getCachedScore() + " pts&7) "));
                    net.md_5.bungee.api.chat.TextComponent reqButton = new net.md_5.bungee.api.chat.TextComponent(plugin.colorize("&a&l[Send Request]"));
                    reqButton.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/team request " + t.getName()));
                    reqButton.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, 
                        new net.md_5.bungee.api.chat.BaseComponent[]{ new net.md_5.bungee.api.chat.TextComponent(plugin.colorize("&bClick to send a join request to team " + t.getName() + "!")) }));
                    
                    teamLine.addExtra(reqButton);
                    player.spigot().sendMessage(teamLine);
                } catch (Throwable ex) {
                    player.sendMessage(plugin.colorize("&8 - &b" + t.getName() + " &7- Request Join command: &a/team request " + t.getName()));
                }
            }
        }
        player.sendMessage(plugin.colorize("&8&m========================================"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = java.util.Arrays.asList(
                "create", "invite", "join", "accept", "request", "acceptrequest", "leave", "kick", "disband", "promote", "demote", "msg", "chat", "pvp", "sethome", "home", "setwarp", "warp", "delhome", "delwarp", "info", "bank", "list", "paytoggle", "gui", "settings", "top", "allthecommandseen"
            );
            List<String> suggestions = new ArrayList<>();
            for (String sub : subCommands) {
                if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                    suggestions.add(sub);
                }
            }
            return suggestions;
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("request") || args[0].equalsIgnoreCase("disband") || args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("bank"))) {
            List<String> suggestions = new ArrayList<>();
            String query = args[1].toLowerCase();
            for (Team t : plugin.getTeamManager().getAllTeams()) {
                if (t.getName().toLowerCase().startsWith(query)) {
                    suggestions.add(t.getName());
                }
            }
            return suggestions;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("allthecommandseen")) {
            List<String> subCommands = java.util.Arrays.asList(
                "create", "invite", "join", "accept", "request", "acceptrequest", "leave", "kick", "disband", "promote", "demote", "msg", "chat", "pvp", "sethome", "home", "setwarp", "warp", "delhome", "delwarp", "info", "bank", "list", "paytoggle", "gui", "settings", "top"
            );
            List<String> suggestions = new ArrayList<>();
            for (String sub : subCommands) {
                if (sub.toLowerCase().startsWith(args[1].toLowerCase())) {
                    suggestions.add(sub);
                }
            }
            return suggestions;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("allthecommandseen") && (args[1].equalsIgnoreCase("join") || args[1].equalsIgnoreCase("accept") || args[1].equalsIgnoreCase("request") || args[1].equalsIgnoreCase("disband") || args[1].equalsIgnoreCase("info") || args[1].equalsIgnoreCase("bank"))) {
            List<String> suggestions = new ArrayList<>();
            String query = args[2].toLowerCase();
            for (Team t : plugin.getTeamManager().getAllTeams()) {
                if (t.getName().toLowerCase().startsWith(query)) {
                    suggestions.add(t.getName());
                }
            }
            return suggestions;
        }
        return Collections.emptyList();
    }
}
