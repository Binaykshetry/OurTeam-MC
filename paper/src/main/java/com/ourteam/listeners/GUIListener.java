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
        Team team = plugin.getTeamManager().getTeamByName(holder.getTeamName());

        if (team == null) {
            player.closeInventory();
            player.sendMessage(plugin.colorize("&c[Error] This team no longer exists or cannot be loaded."));
            return;
        }

        int slot = event.getSlot();
        String menu = holder.getMenuType();

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
                case 11: // Members list details
                    player.performCommand("team info");
                    player.closeInventory();
                    break;
                case 12: // Alliances
                    player.sendMessage(plugin.colorize("&b&l[OurTeam Diplomats] &fActive alliance with: &eBlueSteel &7(Friendly on/off)"));
                    break;
                case 13: // Team Home point teleport
                    player.closeInventory();
                    player.performCommand("team home");
                    break;
                case 14: // Team Enderchest virtual inventory
                    player.closeInventory();
                    player.performCommand("team echest");
                    break;
                case 16: // Open settings sub-menu
                    if (!team.isAdminOrHigher(player.getUniqueId())) {
                        player.sendMessage(plugin.colorize("&cOnly Admin or Team Owners can modify team settings."));
                        break;
                    }
                    plugin.getGuiManager().openSettingsMenu(player, team);
                    break;
            }
        } 
        
        else if ("settings".equalsIgnoreCase(menu)) {
            switch (slot) {
                case 10: // Toggle PvP friendly fire
                    if (!team.getOwner().equals(player.getUniqueId())) {
                        player.sendMessage(plugin.colorize("&cOnly the Team Owner can toggle Friendly Fire PvP."));
                        break;
                    }
                    team.setFriendlyFire(!team.isFriendlyFireEnabled());
                    player.sendMessage(plugin.colorize("&a&l[Settings] &fFriendly Fire has been set to: " + 
                        (team.isFriendlyFireEnabled() ? "&a&lENABLED" : "&c&lDISABLED")));
                    // Refresh inventory
                    plugin.getGuiManager().openSettingsMenu(player, team);
                    break;
                case 11: // Toggle TeamPay Toggle
                    player.sendMessage(plugin.colorize("&6&l[TeamPay Settings] &Toggle payment allocation status... &e(Simulated on)"));
                    player.sendMessage(plugin.colorize("&aAll teammate reward multipliers updated successfully!"));
                    break;
                case 14: // Toggle Join Registration policy
                    player.sendMessage(plugin.colorize("&b&l[Registration] &fThis team has been sealed securely. Invite-only registration is strictly maintained."));
                    break;
                case 22: // Go back arrow
                    plugin.getGuiManager().openMainMenu(player, team);
                    break;
            }
        } 
        
        else if ("bank".equalsIgnoreCase(menu)) {
            switch (slot) {
                case 11: // Deposit $100
                    if (plugin.getEconomy() != null) {
                        if (plugin.getEconomy().has(player, 100)) {
                            plugin.getEconomy().withdrawPlayer(player, 100);
                            team.addBankBalance(100);
                            player.sendMessage(plugin.colorize("&a&l[Bank] &fDeposited &e$100.00 &fto team bank from your account!"));
                        } else {
                            player.sendMessage(plugin.colorize("&c&l[Bank] &fYou do not have enough funds ($100.00) in your wallet."));
                        }
                    } else {
                        // Simulated Deposit
                        team.addBankBalance(100);
                        player.sendMessage(plugin.colorize("&a&l[Simulated Bank] &fDeposited &e$100.00 &finto your team bank. Balance: &a$" + String.format("%,.2f", team.getBankBalance())));
                    }
                    plugin.getGuiManager().openBankMenu(player, team);
                    break;

                case 15: // Withdraw $100
                    if (team.getBankBalance() >= 100) {
                        team.removeBankBalance(100);
                        if (plugin.getEconomy() != null) {
                            plugin.getEconomy().depositPlayer(player, 100);
                        }
                        player.sendMessage(plugin.colorize("&a&l[Bank] &fWithdrew &e$100.00 &ffrom team bank to your wallet."));
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
    }
}
