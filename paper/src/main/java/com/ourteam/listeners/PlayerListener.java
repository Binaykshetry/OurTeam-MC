package com.ourteam.listeners;

import com.ourteam.OurTeam;
import com.ourteam.model.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

/**
 * Intercepts Minecraft events to enforce friendly fire PvP rules, team chat formatting, and TAB lists.
 */
public class PlayerListener implements Listener {

    public static final java.util.Set<UUID> pendingDisbands = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final OurTeam plugin;

    public PlayerListener(OurTeam plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Enforce TAB format formatting immediately when player connects
        plugin.updateTabFormatting(player);

        // Teammate login alert
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team != null && team.isLoginAlertsEnabled()) {
            for (UUID memberId : team.getMembers()) {
                if (memberId.equals(player.getUniqueId())) continue;
                Player teammate = Bukkit.getPlayer(memberId);
                if (teammate != null && teammate.isOnline()) {
                    teammate.sendMessage(plugin.colorize("&7[Teammate Log] &b" + player.getName() + " &7joined the server!"));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Bug 5: Clear player's data from lastKillTimestamps map when they quit
        plugin.getLastKillTimestamps().remove(player.getUniqueId());
        for (java.util.Map<UUID, Long> inner : plugin.getLastKillTimestamps().values()) {
            inner.remove(player.getUniqueId());
        }

        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team != null && team.isLoginAlertsEnabled()) {
            for (UUID memberId : team.getMembers()) {
                if (memberId.equals(player.getUniqueId())) continue;
                Player teammate = Bukkit.getPlayer(memberId);
                if (teammate != null && teammate.isOnline()) {
                    teammate.sendMessage(plugin.colorize("&7[Teammate Log] &b" + player.getName() + " &7quit the server!"));
                }
            }
        }
    }

    @EventHandler
    public void onFriendlyFire(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        Team victimTeam = plugin.getTeamManager().getPlayerTeam(victim.getUniqueId());
        Team attackerTeam = plugin.getTeamManager().getPlayerTeam(attacker.getUniqueId());

        // Bug 1: Early return if either team is null to prevent null dereferencing or crashes
        if (victimTeam == null || attackerTeam == null) {
            return;
        }

        // Check if they are in the exact same team
        if (victimTeam.getId().equals(attackerTeam.getId())) {
            String pvpOverride = victimTeam.getPvpForceOverride();
            if (pvpOverride.equalsIgnoreCase("FORCE_ON")) {
                // Combat is administratively forced ON
                return;
            } else if (pvpOverride.equalsIgnoreCase("FORCE_OFF")) {
                // Combat is administratively forced OFF
                event.setCancelled(true);
                attacker.sendMessage(plugin.colorize("&c[OurTeam] Friendly-fire is administratively FORCED OFF for your team."));
                return;
            }
            if (!victimTeam.isFriendlyFireEnabled()) {
                event.setCancelled(true);
                attacker.sendMessage(plugin.getMsg("cannot-hurt-teammate"));
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());

        if (pendingDisbands.contains(player.getUniqueId())) {
            event.setCancelled(true);
            pendingDisbands.remove(player.getUniqueId());
            String msg = event.getMessage().trim();
            if (msg.equalsIgnoreCase("yes")) {
                if (team != null && team.getOwner().equals(player.getUniqueId())) {
                    for (UUID memberUuid : team.getMembers()) {
                        Player p = Bukkit.getPlayer(memberUuid);
                        if (p != null && p.isOnline()) {
                            p.sendMessage(plugin.colorize("&c&l[OurTeam] &e" + player.getName() + " &fhas &c&lBANNED and DISBANDED &fthe team. All data removed!"));
                        }
                    }
                    plugin.getTeamManager().disbandTeam(team);
                    player.sendMessage(plugin.colorize("&aYour team has been successfully banned and disbanded."));
                } else {
                    player.sendMessage(plugin.colorize("&cError: You are no longer the owner or the team has disbanded."));
                }
            } else if (msg.equalsIgnoreCase("no")) {
                player.sendMessage(plugin.colorize("&eBan application cancelled. Your team remains active."));
            } else {
                player.sendMessage(plugin.colorize("&cInvalid response. Type &eyes &cor &eno. &cBan application aborted."));
            }
            return;
        }

        if (team == null) {
            return;
        }

        // 1. Is team chat toggled?
        if (plugin.getTeamManager().isTeamChatToggled(player.getUniqueId())) {
            if (!team.isTeamChatEnabled()) {
                plugin.getTeamManager().toggleTeamChat(player.getUniqueId());
                player.sendMessage(plugin.colorize("&c[OurTeam] Team Chat has been disabled by team settings. Switching you back to PUBLIC chat."));
                event.setCancelled(true);
                return;
            }
            event.setCancelled(true); // Stop standard public chat distribution

            String format = plugin.getConfig().getString("chat-settings.team-chat-format", "&3[Team Chat] &b{player}&7: &f{message}");
            String formattedMessage = plugin.colorize(format
                    .replace("{player}", player.getName())
                    .replace("{team}", team.getName())
                    .replace("{message}", event.getMessage())
            );

            // Output message solely to team members
            for (UUID memberUuid : team.getMembers()) {
                Player teammate = Bukkit.getPlayer(memberUuid);
                if (teammate != null && teammate.isOnline()) {
                    teammate.sendMessage(formattedMessage);
                }
            }

            // Chat Spy to active admins
            String spyPrefix = plugin.colorize("&c[&4TeamSpy &7- &c" + team.getName() + "&c] &f");
            for (UUID spyUuid : plugin.getChatSpyPlayers()) {
                if (team.isMember(spyUuid)) continue; // Avoid duplicate message if they are in the team
                Player spyAdmin = Bukkit.getPlayer(spyUuid);
                if (spyAdmin != null && spyAdmin.isOnline()) {
                    spyAdmin.sendMessage(spyPrefix + plugin.colorize("&7" + player.getName() + ": &f" + event.getMessage()));
                }
            }
            plugin.getLogger().info("[OurTeam Chat - " + team.getName() + "] " + player.getName() + ": " + event.getMessage());
        } else {
            // 2. Add prefix to public chat
            String prefixFormat = plugin.getConfig().getString("chat-settings.public-name-prefix", "&3[{team}] &f");
            String prefixedText = plugin.colorize(prefixFormat.replace("{team}", team.getName()));
            event.setFormat(prefixedText + event.getFormat());
        }
    }

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (title.startsWith("Team Enderchest (")) {
            // Find team from title "Team Enderchest (TeamName)"
            String teamName = title.substring("Team Enderchest (".length(), title.length() - 1);
            Team team = plugin.getTeamManager().getTeamByName(teamName);
            if (team != null) {
                team.updateEchestData();
                plugin.getTeamManager().saveTeam(team);
                event.getPlayer().sendMessage(plugin.colorize("&a[OurTeam] Team enderchest contents saved successfully!"));
            }
        }
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        org.bukkit.inventory.InventoryView view = event.getView();
        String title = view.getTitle();
        if (title.startsWith("Team Enderchest (")) {
            Player player = (Player) event.getWhoClicked();
            String teamName = title.substring("Team Enderchest (".length(), title.length() - 1);
            Team team = plugin.getTeamManager().getTeamByName(teamName);
            if (team == null) return;

            java.util.UUID playerUuid = player.getUniqueId();
            org.bukkit.inventory.Inventory clickedInv = event.getClickedInventory();
            org.bukkit.event.inventory.ClickType clickType = event.getClick();

            // 1. Enforce general container access permission
            if (!plugin.hasChestPermission(team, playerUuid, "open-chest")) {
                event.setCancelled(true);
                player.sendMessage(plugin.colorize("&cError: You do not have the 'open-chest' permission for this team container."));
                player.closeInventory();
                return;
            }

            // 2. Enforce slot interactions (withdrawal or deposit inside the top container)
            if (clickedInv != null && clickedInv.equals(view.getTopInventory())) {
                org.bukkit.inventory.ItemStack currentSlotItem = event.getCurrentItem();
                org.bukkit.inventory.ItemStack cursorItem = event.getCursor();

                boolean isTaking = (currentSlotItem != null && currentSlotItem.getType() != org.bukkit.Material.AIR);
                boolean isStoring = (cursorItem != null && cursorItem.getType() != org.bukkit.Material.AIR);

                if (isTaking && !plugin.hasChestPermission(team, playerUuid, "take-items")) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.colorize("&cError: You do not have permission to withdraw items from this container."));
                    return;
                }
                if (isStoring && !plugin.hasChestPermission(team, playerUuid, "store-items")) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.colorize("&cError: You do not have permission to deposit items into this container."));
                    return;
                }
            } 
            // 3. Enforce shift-clicks or keyboard swaps from bottom inventory moving into top container
            else if (clickedInv != null && clickedInv.equals(view.getBottomInventory())) {
                if (clickType.isShiftClick()) {
                    org.bukkit.inventory.ItemStack currentSlotItem = event.getCurrentItem();
                    if (currentSlotItem != null && currentSlotItem.getType() != org.bukkit.Material.AIR) {
                        if (!plugin.hasChestPermission(team, playerUuid, "store-items")) {
                            event.setCancelled(true);
                            player.sendMessage(plugin.colorize("&cError: You do not have permission to deposit items into this container."));
                            return;
                        }
                    }
                }
            }
        } else if (title.startsWith("Team Bank (")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            String teamName = title.substring("Team Bank (".length(), title.length() - 1);
            Team team = plugin.getTeamManager().getTeamByName(teamName);
            if (team == null) return;

            org.bukkit.inventory.Inventory clickedInv = event.getClickedInventory();
            if (clickedInv == null || !clickedInv.equals(view.getTopInventory())) {
                return;
            }

            int slot = event.getSlot();
            if (plugin.getEconomy() == null) {
                player.sendMessage(plugin.colorize("&cError: Vault Economy is currently unavailable."));
                return;
            }

            if (slot == 3) {
                // Custom Deposit
                player.closeInventory();
                plugin.getActiveBankAction().put(player.getUniqueId(), "DEPOSIT");
                player.sendMessage(plugin.colorize("&a&l[Bank custom Deposit] &fPlease enter your custom deposit amount in chat (or type &ccancel&f):"));
            } else if (slot == 5) {
                // Custom Withdraw
                player.closeInventory();
                plugin.getActiveBankAction().put(player.getUniqueId(), "WITHDRAW");
                player.sendMessage(plugin.colorize("&c&l[Bank custom Withdraw] &fPlease enter your custom withdrawal amount in chat (or type &ccancel&f):"));
            } else if (slot == 9) {
                executeDirectTransaction(player, team, 1.0, true);
            } else if (slot == 10) {
                executeDirectTransaction(player, team, 10.0, true);
            } else if (slot == 11) {
                executeDirectTransaction(player, team, 5000.0, true);
            } else if (slot == 12) {
                executeDirectTransaction(player, team, 10000.0, true);
            } else if (slot == 14) {
                executeDirectTransaction(player, team, 10000.0, false);
            } else if (slot == 15) {
                executeDirectTransaction(player, team, 5000.0, false);
            } else if (slot == 16) {
                executeDirectTransaction(player, team, 10.0, false);
            } else if (slot == 17) {
                executeDirectTransaction(player, team, 1.0, false);
            }
        }
    }

    private void executeDirectTransaction(Player player, Team team, double amount, boolean isDeposit) {
        if (plugin.getEconomy() == null) {
            player.sendMessage(plugin.colorize("&cError: Economy plugin is unavailable."));
            return;
        }
        if (isDeposit) {
            if (!team.isPayToggle() && !team.isModeratorOrHigher(player.getUniqueId())) {
                player.sendMessage(plugin.colorize("&cError: Team deposits are currently disabled (paytoggle is OFF)."));
                return;
            }
            double pBalance = plugin.getEconomy().getBalance(player);
            if (pBalance < amount) {
                player.sendMessage(plugin.colorize("&cError: You only have $" + String.format("%,.2f", pBalance) + " on hand. You need $" + String.format("%,.2f", amount) + " to deposit."));
                return;
            }
            net.milkbowl.vault.economy.EconomyResponse response = plugin.getEconomy().withdrawPlayer(player, amount);
            if (response.transactionSuccess()) {
                team.addBankBalance(amount);
                team.addMemberDeposit(player.getUniqueId(), amount);
                plugin.getTeamManager().saveTeam(team);
                player.sendMessage(plugin.colorize("&a[Bank] Deposited &e$" + String.format("%,.0f", amount) + " &ainto team bank!"));
            } else {
                player.sendMessage(plugin.colorize("&cError: Deposit failed! Reason: " + response.errorMessage));
            }
        } else {
            if (!team.isModeratorOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                player.sendMessage(plugin.colorize("&cError: Only Team Admins, Moderators or Owners can withdraw team funds."));
                return;
            }
            double tBalance = team.getBankBalance();
            if (tBalance < amount) {
                player.sendMessage(plugin.colorize("&cError: Team bank only has $" + String.format("%,.2f", tBalance) + ". Cannot withdraw $" + String.format("%,.2f", amount) + "."));
                return;
            }
            if (team.removeBankBalance(amount)) {
                net.milkbowl.vault.economy.EconomyResponse response = plugin.getEconomy().depositPlayer(player, amount);
                if (response.transactionSuccess()) {
                    plugin.getTeamManager().saveTeam(team);
                    player.sendMessage(plugin.colorize("&a[Bank] Withdrew &e$" + String.format("%,.0f", amount) + " &afrom team bank!"));
                } else {
                    team.addBankBalance(amount); // refund
                    player.sendMessage(plugin.colorize("&cError: Withdrawal failed! Reason: " + response.errorMessage));
                }
            } else {
                player.sendMessage(plugin.colorize("&cError: Could not deduct funds from the team bank."));
            }
        }
        com.ourteam.commands.TeamCommand.openBankInventory(player, team, plugin);
    }

    @EventHandler
    public void onPlayerChatInteraction(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getActiveBankAction().containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            String action = plugin.getActiveBankAction().remove(player.getUniqueId());
            String text = event.getMessage().trim();

            if (text.equalsIgnoreCase("cancel")) {
                player.sendMessage(plugin.colorize("&cCancelled custom bank interaction."));
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(text);
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.colorize("&cError: Invalid positive number. Custom bank action cancelled."));
                return;
            }

            if (amount <= 0) {
                player.sendMessage(plugin.colorize("&cError: Amount must be greater than zero. Custom bank action cancelled."));
                return;
            }

            // Execute economy mutation on primary thread
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
                if (team == null) {
                    player.sendMessage(plugin.colorize("&cError: You are no longer in a team."));
                    return;
                }

                if (plugin.getEconomy() == null) {
                    if (action.equalsIgnoreCase("DEPOSIT")) {
                        if (!team.isPayToggle() && !team.isModeratorOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                            player.sendMessage(plugin.colorize("&cError: Team deposits are currently disabled (paytoggle is OFF)."));
                            com.ourteam.commands.TeamCommand.openBankInventory(player, team, plugin);
                            return;
                        }
                        team.addBankBalance(amount);
                        team.addMemberDeposit(player.getUniqueId(), amount);
                        plugin.getTeamManager().saveTeam(team);
                        player.sendMessage(plugin.colorize("&a[Simulated Bank] Deposited &e$" + String.format("%,.2f", amount) + " &ainto team bank!"));
                    } else if (action.equalsIgnoreCase("WITHDRAW")) {
                        if (!team.isModeratorOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                            player.sendMessage(plugin.colorize("&cError: Only Team Admins, Moderators or Owners can withdraw team funds."));
                            com.ourteam.commands.TeamCommand.openBankInventory(player, team, plugin);
                            return;
                        }
                        double tBalance = team.getBankBalance();
                        if (tBalance < amount) {
                            player.sendMessage(plugin.colorize("&cError: Your team bank only holds $" + String.format("%,.2f", tBalance) + ". Cannot withdraw $" + String.format("%,.2f", amount) + "."));
                            com.ourteam.commands.TeamCommand.openBankInventory(player, team, plugin);
                            return;
                        }
                        team.removeBankBalance(amount);
                        plugin.getTeamManager().saveTeam(team);
                        player.sendMessage(plugin.colorize("&a[Simulated Bank] Withdrew &e$" + String.format("%,.2f", amount) + " &afrom team bank!"));
                    }
                    com.ourteam.commands.TeamCommand.openBankInventory(player, team, plugin);
                    return;
                }

                if (action.equalsIgnoreCase("DEPOSIT")) {
                    if (!team.isPayToggle() && !team.isModeratorOrHigher(player.getUniqueId())) {
                        player.sendMessage(plugin.colorize("&cError: Team deposits are currently disabled (paytoggle is OFF)."));
                        return;
                    }
                    double pBalance = plugin.getEconomy().getBalance(player);
                    if (pBalance < amount) {
                        player.sendMessage(plugin.colorize("&cError: You only have $" + String.format("%,.2f", pBalance) + " on hand. You need $" + String.format("%,.2f", amount) + " to deposit."));
                        return;
                    }
                    net.milkbowl.vault.economy.EconomyResponse response = plugin.getEconomy().withdrawPlayer(player, amount);
                    if (response.transactionSuccess()) {
                        team.addBankBalance(amount);
                        team.addMemberDeposit(player.getUniqueId(), amount);
                        plugin.getTeamManager().saveTeam(team);
                        player.sendMessage(plugin.colorize("&a[Bank] Custom deposited &e$" + String.format("%,.2f", amount) + " &ainto team bank!"));
                    } else {
                        player.sendMessage(plugin.colorize("&cError: Custom deposit failed! Reason: " + response.errorMessage));
                    }
                } else if (action.equalsIgnoreCase("WITHDRAW")) {
                    if (!team.isModeratorOrHigher(player.getUniqueId()) && !player.isOp() && !player.hasPermission("ourteam.admin")) {
                        player.sendMessage(plugin.colorize("&cError: Only Team Admins, Moderators or Owners can withdraw team funds."));
                        return;
                    }
                    double tBalance = team.getBankBalance();
                    if (tBalance < amount) {
                        player.sendMessage(plugin.colorize("&cError: Your team bank only holds $" + String.format("%,.2f", tBalance) + ". Cannot withdraw $" + String.format("%,.2f", amount) + "."));
                        return;
                    }
                    net.milkbowl.vault.economy.EconomyResponse response = plugin.getEconomy().depositPlayer(player, amount);
                    if (response.transactionSuccess()) {
                        team.removeBankBalance(amount);
                        plugin.getTeamManager().saveTeam(team);
                        player.sendMessage(plugin.colorize("&a[Bank] Custom withdrew &e$" + String.format("%,.2f", amount) + " &afrom team bank!"));
                    } else {
                        player.sendMessage(plugin.colorize("&cError: Custom withdrawal failed! Reason: " + response.errorMessage));
                    }
                }

                // Re-open GUI
                com.ourteam.commands.TeamCommand.openBankInventory(player, team, plugin);
            });
        }
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) {
            return;
        }

        if (killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        Team killerTeam = plugin.getTeamManager().getPlayerTeam(killer.getUniqueId());
        Team victimTeam = plugin.getTeamManager().getPlayerTeam(victim.getUniqueId());

        if (killerTeam != null && victimTeam != null && !killerTeam.getId().equals(victimTeam.getId())) {
            Team.MemberStats killerStats = killerTeam.getMemberStatsMap().computeIfAbsent(killer.getUniqueId().toString(), k -> new Team.MemberStats());
            Team.MemberStats victimStats = victimTeam.getMemberStatsMap().computeIfAbsent(victim.getUniqueId().toString(), k -> new Team.MemberStats());

            killerStats.addKill();
            victimStats.addDeath();

            long spamThresholdMs = plugin.getConfig().getLong("team-score.spam-threshold-seconds", 60L) * 1000L;
            UUID killerUuid = killer.getUniqueId();
            UUID victimUuid = victim.getUniqueId();
            
            boolean isSpam = false;
            long now = System.currentTimeMillis();
            if (plugin.getLastKillTimestamps().containsKey(killerUuid)) {
                java.util.Map<UUID, Long> victimMap = plugin.getLastKillTimestamps().get(killerUuid);
                if (victimMap.containsKey(victimUuid)) {
                    long lastKillTime = victimMap.get(victimUuid);
                    if (now - lastKillTime < spamThresholdMs) {
                        isSpam = true;
                    }
                }
            }

            if (isSpam) {
                killer.sendMessage(plugin.colorize("&c[OurTeam] Target kill flagged as dynamic spam / alt-farming! Score gain nullified."));
                return;
            }

            plugin.getLastKillTimestamps().computeIfAbsent(killerUuid, k -> new java.util.HashMap<>()).put(victimUuid, now);

            int pointsPerKill = plugin.getConfig().getInt("team-score.points-per-kill", 5);
            int pointsLostPerDeath = plugin.getConfig().getInt("team-score.points-lost-per-death", 2);

            killerStats.setGrindingPoints(killerStats.getGrindingPoints() + pointsPerKill);
            victimStats.setGrindingPoints(Math.max(0, victimStats.getGrindingPoints() - pointsLostPerDeath));

            plugin.getTeamManager().saveTeam(killerTeam);
            plugin.getTeamManager().saveTeam(victimTeam);

            killer.sendMessage(plugin.colorize("&a[OurTeam] Defeated &e" + victim.getName() + "&a! Your team gained &e+" + pointsPerKill + " &agrinding points."));
            victim.sendMessage(plugin.colorize("&c[OurTeam] Defeated by &e" + killer.getName() + "&c! Your team lost &e-" + pointsLostPerDeath + " &agrinding points."));
        }
    }

    @EventHandler
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
            event.getFrom().getBlockY() != event.getTo().getBlockY() ||
            event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            
            Player player = event.getPlayer();
            if (plugin.getConfig().getBoolean("cooldowns-and-teleportation.cancel-on-movement", true)) {
                if (plugin.getActiveTeleports().containsKey(player.getUniqueId())) {
                    plugin.cancelTeleport(player, false, true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (plugin.getConfig().getBoolean("cooldowns-and-teleportation.cancel-on-damage", true)) {
                if (plugin.getActiveTeleports().containsKey(player.getUniqueId())) {
                    plugin.cancelTeleport(player, true);
                }
            }
        }
    }
}
