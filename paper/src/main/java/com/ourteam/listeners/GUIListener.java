package com.ourteam.listeners;

import com.ourteam.OurTeam;
import com.ourteam.gui.TeamGUIHolder;
import com.ourteam.model.Team;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * Listens for inventory navigation clicks inside custom OurTeam inventories and executes actions.
 */
public class GUIListener implements Listener {

    private final OurTeam plugin;

    public GUIListener(OurTeam plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) {
            return;
        }

        // Check if the holder is our TeamGUIHolder
        if (!(clickedInv.getHolder() instanceof TeamGUIHolder)) {
            return;
        }

        // Cancel event to prevent taking items out of layout
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        TeamGUIHolder holder = (TeamGUIHolder) clickedInv.getHolder();
        String menu = holder.getMenuType();

        // 1. Check if we are clicking in "/team list" GUI (doesn't require a player-specific team loaded in the holder)
        if ("list".equalsIgnoreCase(menu)) {
            // Standard clicking sound effect helper
            try {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            } catch (Exception e) {
                // Silently skip if Sound enum differs
            }

            int slot = event.getSlot();
            if (slot == 49) {
                player.closeInventory();
                return;
            }

            // If they clicked on a team item (slots 9 to 44)
            if (slot >= 9 && slot <= 44) {
                org.bukkit.inventory.ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() != org.bukkit.Material.AIR && clickedItem.getType() != org.bukkit.Material.GRAY_STAINED_GLASS_PANE) {
                    org.bukkit.inventory.meta.ItemMeta meta = clickedItem.getItemMeta();
                    if (meta != null && meta.hasDisplayName()) {
                        String displayName = meta.getDisplayName();
                        String cleanTeamName = org.bukkit.ChatColor.stripColor(displayName);

                        Team targetTeam = plugin.getTeamManager().getTeamByName(cleanTeamName);
                        if (targetTeam != null) {
                            // Check if they are already in THAT specific team
                            if (targetTeam.isMember(player.getUniqueId())) {
                                player.sendMessage(plugin.colorize("&cError: You are already a member of this team!"));
                                player.closeInventory();
                                return;
                            }

                            // Check if they are already in ANY team
                            Team anyTeam = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
                            if (anyTeam != null) {
                                player.sendMessage(plugin.getMsg("already-in-team"));
                                player.closeInventory();
                                return;
                            }

                            // Send a request directly! (Simulate `/team request <teamName>`)
                            player.closeInventory();
                            // Run the command for them directly and programmatically
                            player.performCommand("team request " + targetTeam.getName());
                        }
                    }
                }
            }
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(holder.getTeamName());

        if (team == null) {
            player.closeInventory();
            player.sendMessage(plugin.colorize("&c[Error] This team no longer exists or cannot be loaded."));
            return;
        }

        int slot = event.getSlot();

        // Standard clicking sound effect helper
        try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        } catch (Exception e) {
            // Silently skip if Sound enum differs in older versions
        }

        if ("main".equalsIgnoreCase(menu)) {
            switch (slot) {
                case 10: // Open Bank Menu
                    plugin.getGuiManager().openBankMenu(player, team);
                    break;
                case 11: // Members list details sub-gui
                    plugin.getGuiManager().openMembersMenu(player, team);
                    break;
                case 12: // Join Requests List Sub-menu
                    plugin.getGuiManager().openRequestsMenu(player, team);
                    break;
                case 13: // Open Homes & Warps Menu
                    if (team.getMultiHomes().isEmpty() && team.getMultiWarps().isEmpty()) {
                        break;
                    }
                    plugin.getGuiManager().openHomesWarpsMenu(player, team);
                    break;
                case 14: // Team Enderchest virtual inventory
                    player.closeInventory();
                    player.performCommand("team echest");
                    break;
                case 15: // Team Leaderboard GUI
                    plugin.getGuiManager().openLeaderboardMenu(player, team);
                    break;
                case 16: // Open settings sub-menu
                    if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                        player.sendMessage(plugin.colorize("&cOnly Admin or Team Owners can modify team settings."));
                        break;
                    }
                    plugin.getGuiManager().openSettingsMenu(player, team);
                    break;
                case 18: // Leave Team Button clicked
                    player.closeInventory();
                    player.performCommand("team leave");
                    break;
            }
        } 
        
        else if ("settings".equalsIgnoreCase(menu)) {
            switch (slot) {
                case 10: // Toggle PvP friendly fire
                    if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                        player.sendMessage(plugin.colorize("&cOnly Team Admins or Owners can toggle Friendly Fire PvP."));
                        break;
                    }
                    team.setFriendlyFire(!team.isFriendlyFireEnabled());
                    player.sendMessage(plugin.colorize("&a&l[Settings] &fFriendly Fire has been set to: " + 
                        (team.isFriendlyFireEnabled() ? "&a&lENABLED" : "&c&lDISABLED")));
                    plugin.getTeamManager().saveTeam(team);
                    // Refresh inventory
                    plugin.getGuiManager().openSettingsMenu(player, team);
                    break;
                case 11: // Toggle TeamPay Toggle
                    if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                        player.sendMessage(plugin.colorize("&cOnly Team Admins or Owners can toggle TeamPay."));
                        break;
                    }
                    team.setPayToggle(!team.isPayToggle());
                    player.sendMessage(plugin.colorize("&a&l[Settings] &fTeamPay deposits have been set to: " + 
                        (team.isPayToggle() ? "&a&lENABLED" : "&c&lDISABLED")));
                    plugin.getTeamManager().saveTeam(team);
                    plugin.getGuiManager().openSettingsMenu(player, team);
                    break;
                case 12: // Toggle Enderchest Access Lock
                    if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                        player.sendMessage(plugin.colorize("&cOnly Team Admins or Owners can lock/unlock the Enderchest."));
                        break;
                    }
                    team.setEchestLocked(!team.isEchestLocked());
                    player.sendMessage(plugin.colorize("&a&l[Settings] &fTeam Enderchest access lock is now: " + 
                        (team.isEchestLocked() ? "&c&lLOCKED &7(Admin/Owner Only)" : "&a&lUNLOCKED &7(All Members)")));
                    plugin.getTeamManager().saveTeam(team);
                    plugin.getGuiManager().openSettingsMenu(player, team);
                    break;
                case 14: // Toggle Join Registration Policy (Requested toggle)
                    if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                        player.sendMessage(plugin.colorize("&cOnly Team Admins or Owners can toggle Join Policy."));
                        break;
                    }
                    team.setOpenJoin(!team.isOpenJoin());
                    player.sendMessage(plugin.colorize("&a&l[Settings] &fTeam Join Policy has been set to: " + 
                        (team.isOpenJoin() ? "&a&lOPEN JOIN (direct entry)" : "&c&lINVITE/APPLICATION REQUIRED")));
                    plugin.getTeamManager().saveTeam(team);
                    plugin.getGuiManager().openSettingsMenu(player, team);
                    break;
                case 15: // Toggle Team Chat enabling/disabling
                    if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                        player.sendMessage(plugin.colorize("&cOnly Team Admins or Owners can toggle Team Chat."));
                        break;
                    }
                    team.setTeamChatEnabled(!team.isTeamChatEnabled());
                    player.sendMessage(plugin.colorize("&a&l[Settings] &fTeam Chat channel is now: " + 
                        (team.isTeamChatEnabled() ? "&a&lENABLED" : "&c&lDISABLED")));
                    plugin.getTeamManager().saveTeam(team);
                    plugin.getGuiManager().openSettingsMenu(player, team);
                    break;
                case 16: // Toggle Member Inviting perms
                    if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                        player.sendMessage(plugin.colorize("&cOnly Team Admins or Owners can toggle Member Invite permissions."));
                        break;
                    }
                    team.setMemberInviteEnabled(!team.isMemberInviteEnabled());
                    player.sendMessage(plugin.colorize("&a&l[Settings] &fMember Inviting has been: " + 
                        (team.isMemberInviteEnabled() ? "&a&lENABLED" : "&c&lDISABLED &7(Only admins can invite)")));
                    plugin.getTeamManager().saveTeam(team);
                    plugin.getGuiManager().openSettingsMenu(player, team);
                    break;
                case 17: // Toggle Teammate Login alerts
                    if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                        player.sendMessage(plugin.colorize("&cOnly Team Admins or Owners can toggle Teammate Alerts."));
                        break;
                    }
                    team.setLoginAlertsEnabled(!team.isLoginAlertsEnabled());
                    player.sendMessage(plugin.colorize("&a&l[Settings] &fTeammate Login notifications are now: " + 
                        (team.isLoginAlertsEnabled() ? "&a&lENABLED" : "&c&lDISABLED")));
                    plugin.getTeamManager().saveTeam(team);
                    plugin.getGuiManager().openSettingsMenu(player, team);
                    break;
                case 22: // Go back arrow
                    plugin.getGuiManager().openMainMenu(player, team);
                    break;
            }
        }

        else if ("members".equalsIgnoreCase(menu)) {
            if (slot == 22) {
                plugin.getGuiManager().openMainMenu(player, team);
                return;
            }

            int[] memberSlots = { 10, 11, 12, 13, 14, 15, 16 };
            int foundIndex = -1;
            for (int i = 0; i < memberSlots.length; i++) {
                if (memberSlots[i] == slot) {
                    foundIndex = i;
                    break;
                }
            }

            if (foundIndex != -1) {
                java.util.List<java.util.UUID> memberList = new java.util.ArrayList<>(team.getMembers());
                if (foundIndex < memberList.size()) {
                    java.util.UUID targetUuid = memberList.get(foundIndex);
                    org.bukkit.OfflinePlayer targetPlayer = org.bukkit.Bukkit.getOfflinePlayer(targetUuid);
                    plugin.getGuiManager().openMemberDetailMenu(player, team, targetPlayer);
                }
            }
        }
        
        else if ("bank".equalsIgnoreCase(menu)) {
            switch (slot) {
                case 10: // Direct Deposit $100
                    if (!team.isPayToggle() && !team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                        player.sendMessage(plugin.colorize("&cError: Team deposits are currently disabled (paytoggle is OFF)."));
                        break;
                    }
                    if (plugin.getEconomy() != null) {
                        if (plugin.getEconomy().has(player, 100)) {
                            net.milkbowl.vault.economy.EconomyResponse response = plugin.getEconomy().withdrawPlayer(player, 100);
                            if (response.transactionSuccess()) {
                                team.addBankBalance(100);
                                team.addMemberDeposit(player.getUniqueId(), 100);
                                plugin.getTeamManager().saveTeam(team);
                                player.sendMessage(plugin.colorize("&a&l[Bank] &fDeposited &e$100.00 &fto team bank from your account!"));
                            } else {
                                player.sendMessage(plugin.colorize("&cError: Deposit failed! " + response.errorMessage));
                            }
                        } else {
                            player.sendMessage(plugin.colorize("&c&l[Bank] &fYou do not have enough funds ($100.00) in your wallet."));
                        }
                    } else {
                        // Simulated Deposit
                        team.addBankBalance(100);
                        team.addMemberDeposit(player.getUniqueId(), 100);
                        plugin.getTeamManager().saveTeam(team);
                        player.sendMessage(plugin.colorize("&a&l[Simulated Bank] &fDeposited &e$100.00 &finto your team bank. Balance: &a$" + String.format("%,.2f", team.getBankBalance())));
                    }
                    plugin.getGuiManager().openBankMenu(player, team);
                    break;

                case 11: // Custom Deposit (Writable Book)
                    if (!team.isPayToggle() && !team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                        player.sendMessage(plugin.colorize("&cError: Team deposits are currently disabled (paytoggle is OFF)."));
                        break;
                    }
                    player.closeInventory();
                    plugin.getActiveBankAction().put(player.getUniqueId(), "DEPOSIT");
                    player.sendMessage(plugin.colorize("&a&l[Bank custom Deposit] &fPlease enter your custom deposit amount in chat (or type &ccancel&f):"));
                    break;

                case 15: // Custom Withdraw (Redstone)
                    if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                        player.sendMessage(plugin.colorize("&cError: Only Team Admins or Owners can withdraw team funds."));
                        break;
                    }
                    player.closeInventory();
                    plugin.getActiveBankAction().put(player.getUniqueId(), "WITHDRAW");
                    player.sendMessage(plugin.colorize("&c&l[Bank custom Withdraw] &fPlease enter your custom withdrawal amount in chat (or type &ccancel&f):"));
                    break;

                case 16: // Direct Withdraw $100
                    if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                        player.sendMessage(plugin.colorize("&cError: Only Team Admins or Owners can withdraw team funds."));
                        break;
                    }
                    if (team.getBankBalance() >= 100) {
                        if (plugin.getEconomy() != null) {
                            net.milkbowl.vault.economy.EconomyResponse response = plugin.getEconomy().depositPlayer(player, 100);
                            if (response.transactionSuccess()) {
                                team.removeBankBalance(100);
                                plugin.getTeamManager().saveTeam(team);
                                player.sendMessage(plugin.colorize("&a&l[Bank] &fWithdrew &e$100.00 &ffrom team bank to your wallet."));
                            } else {
                                player.sendMessage(plugin.colorize("&cError: Withdrawal failed! " + response.errorMessage));
                            }
                        } else {
                            // Simulated Withdraw
                            team.removeBankBalance(100);
                            plugin.getTeamManager().saveTeam(team);
                            player.sendMessage(plugin.colorize("&a&l[Simulated Bank] &fWithdrew &e$100.00 &ffrom your team bank. Balance: &a$" + String.format("%,.2f", team.getBankBalance())));
                        }
                    } else {
                        player.sendMessage(plugin.colorize("&c&l[Bank] &eThe team bank does not have enough balance ($100) to withdraw!"));
                    }
                    plugin.getGuiManager().openBankMenu(player, team);
                    break;

                case 22: // Go back arrow
                    plugin.getGuiManager().openMainMenu(player, team);
                    break;
            }
        }

        else if ("homes_warps".equalsIgnoreCase(menu)) {
            switch (slot) {
                case 11: // Homes list list
                    if (!team.getMultiHomes().isEmpty()) {
                        plugin.getGuiManager().openHomesListMenu(player, team);
                    }
                    break;
                case 15: // Warps list list
                    if (!team.getMultiWarps().isEmpty()) {
                        plugin.getGuiManager().openWarpsListMenu(player, team);
                    }
                    break;
                case 22: // Go back to main
                    plugin.getGuiManager().openMainMenu(player, team);
                    break;
            }
        }

        else if ("homes_list".equalsIgnoreCase(menu)) {
            if (slot == 22) { // Go back
                plugin.getGuiManager().openHomesWarpsMenu(player, team);
                return;
            }

            int[] slots = { 10, 11, 12, 13, 14, 15, 16 };
            int matchedIdx = -1;
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] == slot) {
                    matchedIdx = i;
                    break;
                }
            }

            if (matchedIdx != -1) {
                java.util.List<String> homeNames = new java.util.ArrayList<>(team.getMultiHomes().keySet());
                if (matchedIdx < homeNames.size()) {
                    String homeName = homeNames.get(matchedIdx);
                    if (event.isRightClick()) {
                        // Delete requested
                        if (!plugin.hasTeamPermission(team, player.getUniqueId(), "delhome")) {
                            player.sendMessage(plugin.colorize("&cHey, your rank does not have permission to delete team homes."));
                            return;
                        }
                        team.deleteHome(homeName);
                        plugin.getTeamManager().saveAll();
                        player.sendMessage(plugin.colorize("&eSuccessfully deleted team home '&6" + homeName + "&e'."));
                        if (team.getMultiHomes().isEmpty()) {
                            if (team.getMultiWarps().isEmpty()) {
                                plugin.getGuiManager().openMainMenu(player, team);
                            } else {
                                plugin.getGuiManager().openHomesWarpsMenu(player, team);
                            }
                        } else {
                            plugin.getGuiManager().openHomesListMenu(player, team);
                        }
                    } else {
                        // Teleport requested
                        player.closeInventory();
                        player.performCommand("team home " + homeName);
                    }
                }
            }
        }

        else if ("warps_list".equalsIgnoreCase(menu)) {
            if (slot == 22) { // Go back
                plugin.getGuiManager().openHomesWarpsMenu(player, team);
                return;
            }

            int[] slots = { 10, 11, 12, 13, 14, 15, 16 };
            int matchedIdx = -1;
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] == slot) {
                    matchedIdx = i;
                    break;
                }
            }

            if (matchedIdx != -1) {
                java.util.List<String> warpNames = new java.util.ArrayList<>(team.getMultiWarps().keySet());
                if (matchedIdx < warpNames.size()) {
                    String warpName = warpNames.get(matchedIdx);
                    if (event.isRightClick()) {
                        // Delete requested
                        if (!plugin.hasTeamPermission(team, player.getUniqueId(), "delwarp")) {
                            player.sendMessage(plugin.colorize("&cHey, your rank does not have permission to delete team warps."));
                            return;
                        }
                        team.deleteWarp(warpName);
                        plugin.getTeamManager().saveAll();
                        player.sendMessage(plugin.colorize("&eSuccessfully deleted team warp '&6" + warpName + "&e'."));
                        if (team.getMultiWarps().isEmpty()) {
                            if (team.getMultiHomes().isEmpty()) {
                                plugin.getGuiManager().openMainMenu(player, team);
                            } else {
                                plugin.getGuiManager().openHomesWarpsMenu(player, team);
                            }
                        } else {
                            plugin.getGuiManager().openWarpsListMenu(player, team);
                        }
                    } else {
                        // Teleport requested
                        player.closeInventory();
                        player.performCommand("team warp " + warpName);
                    }
                }
            }
        }

        else if (menu.startsWith("member_detail:")) {
            String targetUuidStr = menu.substring("member_detail:".length());
            java.util.UUID targetUuid = java.util.UUID.fromString(targetUuidStr);
            org.bukkit.OfflinePlayer targetPlayer = org.bukkit.Bukkit.getOfflinePlayer(targetUuid);
            String targetName = targetPlayer.getName() != null ? targetPlayer.getName() : "Player";

            if (slot == 22) {
                plugin.getGuiManager().openMembersMenu(player, team);
                return;
            }

            if (slot == 11) {
                // PROMOTE to ADMIN
                if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                    player.sendMessage(plugin.colorize("&cError: Only Team Admins or Owners can promote members."));
                    return;
                }
                String role = team.getRole(targetUuid);
                if ("MEMBER".equalsIgnoreCase(role)) {
                    team.promote(targetUuid);
                    plugin.getTeamManager().saveTeam(team);
                    player.sendMessage(plugin.colorize("&a&l[Roster] &e" + targetName + " &fhas been promoted to &b&lADMIN&f!"));
                    Player tgt = org.bukkit.Bukkit.getPlayer(targetUuid);
                    if (tgt != null) tgt.sendMessage(plugin.colorize("&a&l[OurTeam] &fYou have been promoted to &b&lADMIN &fof your team!"));
                } else {
                    player.sendMessage(plugin.colorize("&cError: This player is already Admin/Owner."));
                }
                plugin.getGuiManager().openMemberDetailMenu(player, team, targetPlayer);
            }

            else if (slot == 15) {
                // DEMOTE or KICK
                if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                    player.sendMessage(plugin.colorize("&cError: Only Team Admins or Owners can demote/kick members."));
                    return;
                }
                String role = team.getRole(targetUuid);
                if ("ADMIN".equalsIgnoreCase(role) || "MODERATOR".equalsIgnoreCase(role)) {
                    team.demote(targetUuid);
                    plugin.getTeamManager().saveTeam(team);
                    player.sendMessage(plugin.colorize("&e&l[Roster] &e" + targetName + " &fhas been demoted to &7MEMBER&f."));
                    Player tgt = org.bukkit.Bukkit.getPlayer(targetUuid);
                    if (tgt != null) tgt.sendMessage(plugin.colorize("&c&l[OurTeam] &fYou have been demoted to &7MEMBER &fof your team."));
                    plugin.getGuiManager().openMemberDetailMenu(player, team, targetPlayer);
                } else {
                    team.removeMember(targetUuid);
                    plugin.getTeamManager().saveTeam(team);
                    player.sendMessage(plugin.colorize("&c&l[Kick] &e" + targetName + " &fhas been kicked from the team!"));
                    Player tgt = org.bukkit.Bukkit.getPlayer(targetUuid);
                    if (tgt != null) tgt.sendMessage(plugin.colorize("&c&l[OurTeam] &fYou have been kicked from the team!"));
                    plugin.getGuiManager().openMembersMenu(player, team);
                }
            }
        }

        else if ("leaderboard".equalsIgnoreCase(menu)) {
            if (slot == 22) {
                plugin.getGuiManager().openMainMenu(player, team);
            }
        }

        else if ("requests_list".equalsIgnoreCase(menu)) {
            if (slot == 22) {
                plugin.getGuiManager().openMainMenu(player, team);
                return;
            }

            int[] rSlots = { 10, 11, 12, 13, 14, 15, 16 };
            int matchedIdx = -1;
            for (int i = 0; i < rSlots.length; i++) {
                if (rSlots[i] == slot) {
                    matchedIdx = i;
                    break;
                }
            }

            if (matchedIdx != -1) {
                if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                    player.sendMessage(plugin.colorize("&cError: Only Team Admins or Owners can manage join requests."));
                    return;
                }

                java.util.List<java.util.UUID> reqList = new java.util.ArrayList<>(team.getRequests());
                if (matchedIdx < reqList.size()) {
                    java.util.UUID targetUuid = reqList.get(matchedIdx);
                    org.bukkit.OfflinePlayer targetPlayer = org.bukkit.Bukkit.getOfflinePlayer(targetUuid);
                    String targetName = targetPlayer.getName() != null ? targetPlayer.getName() : "Requester";

                    if (!event.isRightClick()) {
                        // ACCEPT REQUEST
                        if (team.getMembers().size() >= 8) {
                            player.sendMessage(plugin.colorize("&cError: Your team has reached the maximum capacity of 8 members."));
                            return;
                        }
                        team.addMember(targetUuid);
                        team.removeRequest(targetUuid);
                        plugin.getTeamManager().saveTeam(team);

                        player.sendMessage(plugin.colorize("&a&l[Recruit] &fYou have accepted &b" + targetName + " &finto the team!"));
                        Player tgt = org.bukkit.Bukkit.getPlayer(targetUuid);
                        if (tgt != null) {
                            tgt.sendMessage(plugin.colorize("&a&l[OurTeam] &fYour join request to &b" + team.getName() + " &fwas accepted!"));
                        }
                    } else {
                        // DECLINE REQUEST
                        team.removeRequest(targetUuid);
                        plugin.getTeamManager().saveTeam(team);

                        player.sendMessage(plugin.colorize("&e&l[Recruit] &fYou have declined the request from &e" + targetName + "&f."));
                        Player tgt = org.bukkit.Bukkit.getPlayer(targetUuid);
                        if (tgt != null) {
                            tgt.sendMessage(plugin.colorize("&c&l[OurTeam] &fYour request to join &b" + team.getName() + " &fwas declined."));
                        }
                    }
                    plugin.getGuiManager().openRequestsMenu(player, team);
                }
            }
        }
    }
}
