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

        // Bug 6: If the item clicked is null or AIR, return early to prevent NullPointerExceptions on rapid clicking empty slots
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == org.bukkit.Material.AIR) {
            return;
        }

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
            int maxSize = plugin.getGuiManager().getMenuSize("list", 54);
            int backSlot = plugin.getGuiManager().getMenuSlot("list", "back-slot", maxSize - 5, maxSize);
            int listStart = plugin.getGuiManager().getMenuSlot("list", "list-start", 9, maxSize);
            int listEnd = plugin.getGuiManager().getMenuSlot("list", "list-end", 44, maxSize);

            if (slot == backSlot) {
                Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
                if (team == null) {
                    plugin.getGuiManager().openNoTeamMenu(player);
                } else {
                    player.closeInventory();
                }
                return;
            }

            // If they clicked on a team item inside the dynamic range
            if (slot >= listStart && slot <= listEnd) {
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

        // 1b. Check if clicking in No Team / Creation Hub GUI
        if ("noteam".equalsIgnoreCase(menu)) {
            try {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            } catch (Exception e) {}

            int slot = event.getSlot();
            int maxSize = plugin.getGuiManager().getMenuSize("noteam", 27);
            int createSlot = plugin.getGuiManager().getMenuSlot("noteam", "create-slot", 14, maxSize);
            int invitesSlot = plugin.getGuiManager().getMenuSlot("noteam", "invites-slot", 12, maxSize);

            if (slot == createSlot) {
                // Open confirmation dialog (Screenshot 2026-11-08 220043.png)
                plugin.getGuiManager().openCreateConfirmMenu(player);
            } else if (slot == invitesSlot) {
                // View received invitations (Screenshot 2026-11-08 215957.png)
                plugin.getGuiManager().openInvitesMenu(player, null, 1);
            }
            return;
        }

        // 1c. Check if clicking in Create Team Confirmation GUI
        if ("create_confirm".equalsIgnoreCase(menu)) {
            try {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            } catch (Exception e) {}

            int slot = event.getSlot();
            if (slot >= 0 && slot <= 3) {
                // Cancel
                plugin.getGuiManager().openNoTeamMenu(player);
            } else if (slot == 4) {
                // Confirm -> Prompt chat
                player.closeInventory();
                plugin.getActiveGeneralAction().put(player.getUniqueId(), "CREATE_TEAM");
                player.sendMessage(plugin.colorize("&6&lTEAMS &8» &7Write the team tag in the chat &8(Max Length 12)"));
            }
            return;
        }

        // Leaderboard Menus (accessible to everyone, no team required)
        if ("leaderboard_main".equalsIgnoreCase(menu)) {
            com.ourteam.leaderboard.LeaderboardManager lm = plugin.getLeaderboardManager();
            if (lm != null) {
                int clickedSlot = event.getSlot();
                for (com.ourteam.leaderboard.LeaderboardCategory cat : lm.getCategories().values()) {
                    if (cat.getSlot() == clickedSlot) {
                        lm.openCategoryMenu(player, cat.getId(), 1);
                        return;
                    }
                }
            }
            return;
        }

        if ("leaderboard_category".equalsIgnoreCase(menu)) {
            com.ourteam.leaderboard.LeaderboardManager lm = plugin.getLeaderboardManager();
            if (lm != null) {
                String catId = holder.getTeamName();
                int page = holder.getPage();
                com.ourteam.leaderboard.LeaderboardCategory cat = lm.getCategory(catId);
                int clickedSlot = event.getSlot();
                if (cat != null) {
                    if (clickedSlot == lm.getPrevSlot()) {
                        if (page > 1) {
                            lm.openCategoryMenu(player, catId, page - 1);
                        } else {
                            try { player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f); } catch (Exception ignored) {}
                        }
                    } else if (clickedSlot == lm.getRefreshSlot()) {
                        lm.clearCache(catId);
                        lm.openCategoryMenu(player, catId, page);
                    } else if (clickedSlot == lm.getNextSlot()) {
                        if (page < cat.getMaxPages()) {
                            lm.openCategoryMenu(player, catId, page + 1);
                        } else {
                            try { player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f); } catch (Exception ignored) {}
                        }
                    } else if (clickedSlot == lm.getBackSlot()) {
                        lm.openMainMenu(player);
                    }
                }
            }
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(holder.getTeamName());

        // For menus that don't strictly require team (e.g. invites for non-team player)
        if ("invites".equalsIgnoreCase(menu) && team == null) {
            try {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            } catch (Exception e) {}

            int slot = event.getSlot();
            int page = holder.getPage();

            if (slot == 18) {
                if (page > 1) plugin.getGuiManager().openInvitesMenu(player, null, page - 1);
            } else if (slot == 22) {
                plugin.getGuiManager().openNoTeamMenu(player);
            } else if (slot == 26) {
                plugin.getGuiManager().openInvitesMenu(player, null, page + 1);
            } else if (slot >= 9 && slot <= 17) {
                org.bukkit.inventory.ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
                    String targetName = org.bukkit.ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
                    player.closeInventory();
                    if (event.isRightClick()) {
                        player.performCommand("team deny " + targetName);
                    } else {
                        player.performCommand("team accept " + targetName);
                    }
                }
            }
            return;
        }

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
            int maxSlot = plugin.getGuiManager().getMenuSize("main", 27);
            int invitesSlot = plugin.getGuiManager().getMenuSlot("main", "invites-slot", 4, maxSlot);
            int bankSlot = plugin.getGuiManager().getMenuSlot("main", "bank-slot", 10, maxSlot);
            int statsSlot = plugin.getGuiManager().getMenuSlot("main", "stats-slot", 11, maxSlot);
            int homeSlot = plugin.getGuiManager().getMenuSlot("main", "home-slot", 12, maxSlot);
            int echestSlot = plugin.getGuiManager().getMenuSlot("main", "echest-slot", 13, maxSlot);
            int membersSlot = plugin.getGuiManager().getMenuSlot("main", "members-slot", 14, maxSlot);
            int settingsSlot = plugin.getGuiManager().getMenuSlot("main", "settings-slot", 15, maxSlot);
            int alliesSlot = plugin.getGuiManager().getMenuSlot("main", "allies-slot", 16, maxSlot);

            if (slot == invitesSlot) {
                plugin.getGuiManager().openInvitesMenu(player, team, 1);
            } else if (slot == bankSlot) {
                if (!plugin.getConfig().getBoolean("team-bank.enable", true)) {
                    player.sendMessage(plugin.colorize("&cError: Team Bank feature is currently disabled by the server administration."));
                    return;
                }
                plugin.getGuiManager().openBankMenu(player, team);
            } else if (slot == statsSlot) {
                plugin.getGuiManager().openStatsMenu(player, team);
            } else if (slot == homeSlot) {
                if (team.hasHome() || !team.getMultiHomes().isEmpty()) {
                    player.closeInventory();
                    player.performCommand("team home");
                } else {
                    player.sendMessage(plugin.colorize("&c[OurTeam] Your team does not have a home set! Use &e/team sethome &cto set one."));
                }
            } else if (slot == echestSlot) {
                player.closeInventory();
                player.performCommand("team echest");
            } else if (slot == membersSlot) {
                plugin.getGuiManager().openMembersMenu(player, team);
            } else if (slot == settingsSlot) {
                if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                    player.sendMessage(plugin.colorize("&cOnly Admin or Team Owners can modify team settings."));
                    return;
                }
                plugin.getGuiManager().openSettingsMenu(player, team);
            } else if (slot == alliesSlot) {
                plugin.getGuiManager().openAlliesMenu(player, team, 1);
            }
        }

        else if ("stats".equalsIgnoreCase(menu)) {
            switch (slot) {
                case 12: // Levels
                    plugin.getGuiManager().openStatsDetailMenu(player, team, "LEVELS", 1);
                    break;
                case 13: // Money
                    plugin.getGuiManager().openStatsDetailMenu(player, team, "MONEY", 1);
                    break;
                case 14: // Playtime
                    plugin.getGuiManager().openStatsDetailMenu(player, team, "PLAYTIME", 1);
                    break;
                case 20: // Kills
                    plugin.getGuiManager().openStatsDetailMenu(player, team, "KILLS", 1);
                    break;
                case 21: // Deaths
                    plugin.getGuiManager().openStatsDetailMenu(player, team, "DEATHS", 1);
                    break;
                case 22: // KDR
                    plugin.getGuiManager().openStatsDetailMenu(player, team, "KDR", 1);
                    break;
                case 23: // Placed Blocks
                    plugin.getGuiManager().openStatsDetailMenu(player, team, "PLACED_BLOCKS", 1);
                    break;
                case 24: // Broken Blocks
                    plugin.getGuiManager().openStatsDetailMenu(player, team, "BROKEN_BLOCKS", 1);
                    break;
                case 40: // Back
                    plugin.getGuiManager().openMainMenu(player, team);
                    break;
            }
        }

        else if ("stats_detail".equalsIgnoreCase(menu)) {
            int page = holder.getPage();
            String category = holder.getExtraData();
            if (slot == 18) {
                if (page > 1) {
                    plugin.getGuiManager().openStatsDetailMenu(player, team, category, page - 1);
                }
            } else if (slot == 22) {
                plugin.getGuiManager().openStatsMenu(player, team);
            } else if (slot == 26) {
                plugin.getGuiManager().openStatsDetailMenu(player, team, category, page + 1);
            }
        }

        else if ("allies".equalsIgnoreCase(menu)) {
            int page = holder.getPage();
            if (slot == 4) {
                // Invite ally
                player.closeInventory();
                player.sendMessage(plugin.colorize("&6&lTEAMS &8» &7Write the team name to send an alliance proposal in chat &8(or type cancel)"));
                plugin.getActiveGeneralAction().put(player.getUniqueId(), "ALLY_INVITE");
            } else if (slot == 18) {
                if (page > 1) plugin.getGuiManager().openAlliesMenu(player, team, page - 1);
            } else if (slot == 22) {
                plugin.getGuiManager().openMainMenu(player, team);
            } else if (slot == 26) {
                plugin.getGuiManager().openAlliesMenu(player, team, page + 1);
            } else if (slot >= 9 && slot <= 17) {
                org.bukkit.inventory.ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
                    String targetName = org.bukkit.ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
                    player.closeInventory();
                    player.performCommand("team ally " + targetName);
                }
            }
        }

        else if ("invites".equalsIgnoreCase(menu)) {
            int page = holder.getPage();
            if (slot == 18) {
                if (page > 1) plugin.getGuiManager().openInvitesMenu(player, team, page - 1);
            } else if (slot == 22) {
                plugin.getGuiManager().openMainMenu(player, team);
            } else if (slot == 26) {
                plugin.getGuiManager().openInvitesMenu(player, team, page + 1);
            } else if (slot >= 9 && slot <= 17) {
                org.bukkit.inventory.ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
                    String targetName = org.bukkit.ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
                    player.closeInventory();
                    if (event.isRightClick()) {
                        player.performCommand("team decline " + targetName);
                    } else {
                        player.performCommand("team accept " + targetName);
                    }
                }
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
                    player.sendMessage(plugin.colorize("&a[Settings] &fFriendly Fire has been set to: " + 
                        (team.isFriendlyFireEnabled() ? "&aENABLED" : "&cDISABLED")));
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
                    player.sendMessage(plugin.colorize("&a[Settings] &fTeamPay deposits have been set to: " + 
                        (team.isPayToggle() ? "&aENABLED" : "&cDISABLED")));
                    plugin.getTeamManager().saveTeam(team);
                    plugin.getGuiManager().openSettingsMenu(player, team);
                    break;
                case 12: // Toggle Enderchest Access Lock
                    if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                        player.sendMessage(plugin.colorize("&cOnly Team Admins or Owners can lock/unlock the Enderchest."));
                        break;
                    }
                    team.setEchestLocked(!team.isEchestLocked());
                    player.sendMessage(plugin.colorize("&a[Settings] &fTeam Enderchest access lock is now: " + 
                        (team.isEchestLocked() ? "&cLOCKED &7(Admin/Owner Only)" : "&aUNLOCKED &7(All Members)")));
                    plugin.getTeamManager().saveTeam(team);
                    plugin.getGuiManager().openSettingsMenu(player, team);
                    break;
                case 14: // Toggle Join Registration Policy (Requested toggle)
                    if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                        player.sendMessage(plugin.colorize("&cOnly Team Admins or Owners can toggle Join Policy."));
                        break;
                    }
                    team.setOpenJoin(!team.isOpenJoin());
                    player.sendMessage(plugin.colorize("&a[Settings] &fTeam Join Policy has been set to: " + 
                        (team.isOpenJoin() ? "&aOPEN JOIN (direct entry)" : "&cINVITE/APPLICATION REQUIRED")));
                    plugin.getTeamManager().saveTeam(team);
                    plugin.getGuiManager().openSettingsMenu(player, team);
                    break;
                case 15: // Toggle Team Chat enabling/disabling
                    if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                        player.sendMessage(plugin.colorize("&cOnly Team Admins or Owners can toggle Team Chat."));
                        break;
                    }
                    team.setTeamChatEnabled(!team.isTeamChatEnabled());
                    player.sendMessage(plugin.colorize("&a[Settings] &fTeam Chat channel is now: " + 
                        (team.isTeamChatEnabled() ? "&aENABLED" : "&cDISABLED")));
                    plugin.getTeamManager().saveTeam(team);
                    plugin.getGuiManager().openSettingsMenu(player, team);
                    break;
                case 16: // Toggle Member Inviting perms
                    if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                        player.sendMessage(plugin.colorize("&cOnly Team Admins or Owners can toggle Member Invite permissions."));
                        break;
                    }
                    team.setMemberInviteEnabled(!team.isMemberInviteEnabled());
                    player.sendMessage(plugin.colorize("&a[Settings] &fMember Inviting has been: " + 
                        (team.isMemberInviteEnabled() ? "&aENABLED" : "&cDISABLED &7(Only admins can invite)")));
                    plugin.getTeamManager().saveTeam(team);
                    plugin.getGuiManager().openSettingsMenu(player, team);
                    break;
                case 19: // Toggle Teammate Login alerts (formerly 17)
                    if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                        player.sendMessage(plugin.colorize("&cOnly Team Admins or Owners can toggle Teammate Alerts."));
                        break;
                    }
                    team.setLoginAlertsEnabled(!team.isLoginAlertsEnabled());
                    player.sendMessage(plugin.colorize("&a[Settings] &fTeammate Login notifications are now: " + 
                        (team.isLoginAlertsEnabled() ? "&aENABLED" : "&cDISABLED")));
                    plugin.getTeamManager().saveTeam(team);
                    plugin.getGuiManager().openSettingsMenu(player, team);
                    break;
                case 20: // Join Requests List
                    if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                        player.sendMessage(plugin.colorize("&cOnly Team Admins or Owners can manage Join Requests."));
                        break;
                    }
                    plugin.getGuiManager().openRequestsMenu(player, team);
                    break;
                case 21: // Leave Team Option
                    player.closeInventory();
                    player.performCommand("team leave");
                    break;
                case 22: // Team Ban / Disband Option (chat confirmation)
                    if (!team.getOwner().equals(player.getUniqueId())) {
                        player.sendMessage(plugin.colorize("&cOnly the Team Owner has the authority to disband or ban this team."));
                        break;
                    }
                    player.closeInventory();
                    PlayerListener.pendingDisbands.add(player.getUniqueId());
                    player.sendMessage(plugin.colorize("&e[Confirmation] Are you sure you want ban the team? if yes type yes or for no type no"));
                    break;
                case 26: // Go back arrow
                    plugin.getGuiManager().openMainMenu(player, team);
                    break;
            }
        }

        else if ("members".equalsIgnoreCase(menu)) {
            if (slot == 49) {
                plugin.getGuiManager().openMainMenu(player, team);
                return;
            }

            if (slot >= 9 && slot <= 44) {
                java.util.List<java.util.UUID> memberList = new java.util.ArrayList<>(team.getMembers());
                int foundIndex = slot - 9;
                if (foundIndex >= 36) return; // safeguard within slots bounds
                if (foundIndex >= 0 && foundIndex < memberList.size()) {
                    java.util.UUID targetUuid = memberList.get(foundIndex);
                    org.bukkit.OfflinePlayer targetPlayer = org.bukkit.Bukkit.getOfflinePlayer(targetUuid);
                    plugin.getGuiManager().openMemberDetailMenu(player, team, targetPlayer);
                }
            }
        }
        
        else if ("bank".equalsIgnoreCase(menu)) {
            int maxSlot = plugin.getGuiManager().getMenuSize("bank", 27);
            
            // Anti-dupe system: rate limit click events on banking actions
            if (plugin.checkAndSetBankCooldown(player.getUniqueId())) {
                player.sendMessage(plugin.colorize("&c[Anti-Dupe] &7Please wait a moment between banking interactions!"));
                return;
            }
            
            if (slot == plugin.getGuiManager().getMenuSlot("bank", "deposit100-slot", 10, maxSlot)) {
                // Direct Deposit $100
                if (!team.isPayToggle() && !team.isModeratorOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                    player.sendMessage(plugin.colorize("&cError: Team deposits are currently disabled (paytoggle is OFF)."));
                    return;
                }
                plugin.getTeamManager().depositToBank(player, team, 100);
                plugin.getGuiManager().openBankMenu(player, team);
            } else if (slot == plugin.getGuiManager().getMenuSlot("bank", "customdeposit-slot", 11, maxSlot)) {
                // Custom Deposit (Writable Book)
                if (!team.isPayToggle() && !team.isModeratorOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                    player.sendMessage(plugin.colorize("&cError: Team deposits are currently disabled (paytoggle is OFF)."));
                    return;
                }
                player.closeInventory();
                plugin.getActiveBankAction().put(player.getUniqueId(), "DEPOSIT");
                player.sendMessage(plugin.colorize("&a&l[Bank custom Deposit] &fPlease enter your custom deposit amount in chat (or type &ccancel&f):"));
            } else if (slot == plugin.getGuiManager().getMenuSlot("bank", "customwithdraw-slot", 15, maxSlot)) {
                // Custom Withdraw (Redstone)
                if (!team.isModeratorOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                    player.sendMessage(plugin.colorize("&cError: Only Team Admins, Moderators or Owners can withdraw team funds."));
                    return;
                }
                player.closeInventory();
                plugin.getActiveBankAction().put(player.getUniqueId(), "WITHDRAW");
                player.sendMessage(plugin.colorize("&c&l[Bank custom Withdraw] &fPlease enter your custom withdrawal amount in chat (or type &ccancel&f):"));
            } else if (slot == plugin.getGuiManager().getMenuSlot("bank", "withdraw100-slot", 16, maxSlot)) {
                // Direct Withdraw $100
                if (!team.isModeratorOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                    player.sendMessage(plugin.colorize("&cError: Only Team Admins, Moderators or Owners can withdraw team funds."));
                    return;
                }
                plugin.getTeamManager().withdrawFromBank(player, team, 100);
                plugin.getGuiManager().openBankMenu(player, team);
            } else if (slot == plugin.getGuiManager().getMenuSlot("bank", "history-slot", 12, maxSlot)) {
                plugin.getGuiManager().openBankHistoryMenu(player, team);
            } else if (slot == plugin.getGuiManager().getMenuSlot("bank", "back-slot", 22, maxSlot)) {
                plugin.getGuiManager().openMainMenu(player, team);
            }
        }

        else if ("bank_history".equalsIgnoreCase(menu)) {
            int maxSlot = plugin.getGuiManager().getMenuSize("bank_history", 27);
            if (slot == plugin.getGuiManager().getMenuSlot("bank_history", "back-slot", 22, maxSlot)) {
                plugin.getGuiManager().openBankMenu(player, team);
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

            int matchedIdx = slot - 9;

            if (matchedIdx >= 0 && matchedIdx < 9) {
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
                            plugin.getGuiManager().openHomesWarpsMenu(player, team);
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

            int matchedIdx = slot - 9;

            if (matchedIdx >= 0 && matchedIdx < 9) {
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
                            plugin.getGuiManager().openHomesWarpsMenu(player, team);
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

            int maxSlot = plugin.getGuiManager().getMenuSize("member_detail", 27);
            int backSlot = plugin.getGuiManager().getMenuSlot("member_detail", "back-slot", 22, maxSlot);
            int adminSlot = plugin.getGuiManager().getMenuSlot("member_detail", "role-admin-slot", 10, maxSlot);
            int modSlot = plugin.getGuiManager().getMenuSlot("member_detail", "role-mod-slot", 11, maxSlot);
            int memberSlot = plugin.getGuiManager().getMenuSlot("member_detail", "role-member-slot", 15, maxSlot);
            int kickSlot = plugin.getGuiManager().getMenuSlot("member_detail", "kick-slot", 16, maxSlot);

            if (slot == backSlot) {
                plugin.getGuiManager().openMembersMenu(player, team);
                return;
            }

            if (!team.isAdminOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                player.sendMessage(plugin.colorize("&cError: Only Team Admins or Owners can manage member roles."));
                return;
            }

            // Can't manage self or owner
            if (targetUuid.equals(player.getUniqueId()) || targetUuid.equals(team.getOwner())) {
                player.sendMessage(plugin.colorize("&cError: You cannot manage your own role or the team owner's role."));
                return;
            }

            if (slot == adminSlot) {
                team.getRoles().put(targetUuid.toString(), "ADMIN");
                plugin.getTeamManager().saveTeam(team);
                player.sendMessage(plugin.colorize("&a[Roster] &b" + targetName + " &fhas been set to &b&lADMIN&f!"));
                Player tgtAdmin = org.bukkit.Bukkit.getPlayer(targetUuid);
                if (tgtAdmin != null) {
                    tgtAdmin.sendMessage(plugin.colorize("&a[OurTeam] &fYou have been set to &b&lADMIN &fof your team!"));
                }
                plugin.getGuiManager().openMemberDetailMenu(player, team, targetPlayer);
            } else if (slot == modSlot) {
                team.getRoles().put(targetUuid.toString(), "MODERATOR");
                plugin.getTeamManager().saveTeam(team);
                player.sendMessage(plugin.colorize("&a[Roster] &e" + targetName + " &fhas been set to &e&lMODERATOR&f!"));
                Player tgtMod = org.bukkit.Bukkit.getPlayer(targetUuid);
                if (tgtMod != null) {
                    tgtMod.sendMessage(plugin.colorize("&a[OurTeam] &fYou have been set to &e&lMODERATOR &fof your team!"));
                }
                plugin.getGuiManager().openMemberDetailMenu(player, team, targetPlayer);
            } else if (slot == memberSlot) {
                team.getRoles().remove(targetUuid.toString());
                plugin.getTeamManager().saveTeam(team);
                player.sendMessage(plugin.colorize("&a[Roster] &e" + targetName + " &fhas been set to &7&lMEMBER&f!"));
                Player tgtMem = org.bukkit.Bukkit.getPlayer(targetUuid);
                if (tgtMem != null) {
                    tgtMem.sendMessage(plugin.colorize("&a[OurTeam] &fYours role has been changed to &7&lMEMBER&f of your team."));
                }
                plugin.getGuiManager().openMemberDetailMenu(player, team, targetPlayer);
            } else if (slot == kickSlot) {
                team.removeMember(targetUuid);
                team.getRoles().remove(targetUuid.toString());
                plugin.getTeamManager().saveTeam(team);
                player.sendMessage(plugin.colorize("&c[Kick] &e" + targetName + " &fhas been kicked from the team!"));
                Player tgtKick = org.bukkit.Bukkit.getPlayer(targetUuid);
                if (tgtKick != null) {
                    tgtKick.sendMessage(plugin.colorize("&c[OurTeam] &fYou have been kicked from your team."));
                }
                plugin.getGuiManager().openMembersMenu(player, team);
            }
        }

        else if ("leaderboard".equalsIgnoreCase(menu)) {
            if (slot == 49) {
                plugin.getGuiManager().openMainMenu(player, team);
            }
        }

        else if ("alliances".equalsIgnoreCase(menu)) {
            if (slot == 49) {
                plugin.getGuiManager().openMainMenu(player, team);
                return;
            }

            if (slot >= 9 && slot <= 44) {
                java.util.List<Team> otherTeams = new java.util.ArrayList<>();
                for (Team t : plugin.getTeamManager().getAllTeams()) {
                    if (!t.getName().equalsIgnoreCase(team.getName())) {
                        otherTeams.add(t);
                    }
                }
                int idx = slot - 9;
                if (idx >= 0 && idx < otherTeams.size()) {
                    Team targetTeam = otherTeams.get(idx);
                    player.closeInventory();
                    player.sendMessage(plugin.colorize("&b&l[Diplomacy] &fYour alliance proposal sent to: &d" + targetTeam.getName()));
                }
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
