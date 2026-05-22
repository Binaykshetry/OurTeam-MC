package com.ourteam.gui;

import com.ourteam.OurTeam;
import com.ourteam.model.Team;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates custom Minecraft Chest GUIs (Team menus) filled with interactive options.
 */
public class TeamGUIManager {

    private final OurTeam plugin;

    public TeamGUIManager(OurTeam plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates and opens the Main Team Dashboard (Menu: main)
     */
    public void openMainMenu(Player player, Team team) {
        String title = plugin.colorize("&3&lTeam Dashboard: &8" + team.getName());
        TeamGUIHolder holder = new TeamGUIHolder("main", team.getName());
        Inventory inv = Bukkit.createInventory(holder, 27, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, marker);
        }

        // Slot 10: Team Bank
        inv.setItem(10, createGuiItem(Material.GOLD_INGOT, 
            "&6&lTeam Bank Balance", 
            "&7Status: Economy Management",
            "",
            "&fBalance: &a$" + String.format("%,.2f", team.getBankBalance()),
            "",
            "&a▶ Click to open Bank Menu"
        ));

        // Slot 11: Team Members
        inv.setItem(11, createMemberSkullItem(player, 
            "&d&lTeam Members &7(" + team.getMembers().size() + ")", 
            "&7Status: Roster Management",
            "",
            "&fTeammates: &e" + team.getMembers().size() + " Online",
            "",
            "&a▶ Click to edit promotions / demotions"
        ));

        // Slot 12: Ally System
        inv.setItem(12, createGuiItem(Material.SHIELD, 
            "&b&lAlly & Diplomacy System", 
            "&7Status: Alliances",
            "",
            "&fAllies count: &91",
            "",
            "&a▶ Click to manage Alliances"
        ));

        // Slot 13: Team Home
        inv.setItem(13, createGuiItem(Material.RED_BED, 
            "&e&lTeam Home Point", 
            "&7Status: Headquarters Teleportation",
            "",
            "&fHas Home: " + (team.hasHome() ? "&aYes" : "&cNo"),
            "",
            "&a▶ Click to teleport / warp"
        ));

        // Slot 14: Team Enderchest
        inv.setItem(14, createGuiItem(Material.ENDER_CHEST, 
            "&d&lTeam Shared Enderchest", 
            "&7Status: Public vault of valuables",
            "",
            "&fSlots: &727 Slots",
            "",
            "&a▶ Click to access virtual chest storage"
        ));

        // Slot 15: Team Combat Stats
        inv.setItem(15, createGuiItem(Material.DIAMOND_SWORD, 
            "&c&lTeam Combat Stats", 
            "&7Status: Statistics & Achievements",
            "",
            "&fKills: &a12 &7| &fDeaths: &c4",
            "&fKDR: &e3.00 Ratio",
            ""
        ));

        // Slot 16: Settings Menu Toggle
        inv.setItem(16, createGuiItem(Material.COMPARATOR, 
            "&b&lTeam Settings Panel", 
            "&7Status: Configurations",
            "",
            "&fFriendly Fire: " + (team.isFriendlyFireEnabled() ? "&aON" : "&cOFF"),
            "",
            "&a▶ Click to manage rules and toggles"
        ));

        player.openInventory(inv);
    }

    /**
     * Creates and opens the Team Settings sub-menu (Menu: settings)
     */
    public void openSettingsMenu(Player player, Team team) {
        String title = plugin.colorize("&c&lTeam Settings Panel");
        TeamGUIHolder holder = new TeamGUIHolder("settings", team.getName());
        Inventory inv = Bukkit.createInventory(holder, 27, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, marker);
        }

        // Slot 10: Toggle PvP friendly fire
        String pvpStatus = team.isFriendlyFireEnabled() ? "&a&lENABLED &7(Damage ON)" : "&c&lDISABLED &7(Damage OFF)";
        inv.setItem(10, createGuiItem(Material.IRON_SWORD, 
            "&c&lFriendly Fire Toggle", 
            "&7Enables or disables PvP among team-members",
            "",
            "&fPvP Status: " + pvpStatus,
            "",
            "&e▶ Click to TOGGLE friendly fire pvp!"
        ));

        // Slot 11: Toggle TeamPay (Payment Sharing settings)
        inv.setItem(11, createGuiItem(Material.SUNFLOWER, 
            "&6&lTeamPay Toggle", 
            "&7Toggles sharing gold rewards or bank taxes with teammates",
            "",
            "&fTeamPay Status: &a&lENABLED &7(Sharing ON)",
            "",
            "&e▶ Click to TOGGLE TeamPay settings!"
        ));

        // Slot 13: Member info card
        String myRole = team.getRole(player.getUniqueId());
        inv.setItem(13, createMemberSkullItem(player, 
            "&e&lYour Account Roster role", 
            "&7Information panel",
            "",
            "&fYour Role: &b&l" + myRole,
            "&fTotal Players in team: &d" + team.getMembers().size() + "/8",
            "&fEstablished: &7May 18, 2026"
        ));

        // Slot 14: Open Join policy (registration tag)
        inv.setItem(14, createGuiItem(Material.OAK_DOOR, 
            "&b&lOpen Join Registration", 
            "&7Toggle if outsiders can join without invites",
            "",
            "&fJoin Policy: &c&lINVITE-ONLY",
            "",
            "&e▶ Click to TOGGLE join restriction details"
        ));

        // Slot 26: Go back
        inv.setItem(22, createGuiItem(Material.ARROW, 
            "&e&l◀ Return to Dashboard", 
            "&7Go back to main team GUI panel"
        ));

        player.openInventory(inv);
    }

    /**
     * Creates and opens the Team Bank sub-menu (Menu: bank)
     */
    public void openBankMenu(Player player, Team team) {
        String title = plugin.colorize("&6&lTeam Bank: Deposit/Widthdraw");
        TeamGUIHolder holder = new TeamGUIHolder("bank", team.getName());
        Inventory inv = Bukkit.createInventory(holder, 27, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, marker);
        }

        // Slot 11: Deposit Emerald
        inv.setItem(11, createGuiItem(Material.EMERALD, 
            "&a&lDeposit Team Funds", 
            "&7Add some currency to the team vault",
            "",
            "&a▶ Click to DEPOSIT $100.00"
        ));

        // Slot 13: Gold Block of current ledger info
        inv.setItem(13, createGuiItem(Material.GOLD_BLOCK, 
            "&6&lAccount Balance Info", 
            "&7Bank details & interest stats",
            "",
            "&fLEDGER: &e$" + String.format("%,.2f", team.getBankBalance()),
            "&fInterest Rate: &b5.0% accrual",
            ""
        ));

        // Slot 15: Withdraw Redstone
        inv.setItem(15, createGuiItem(Material.REDSTONE, 
            "&c&lWithdraw Team Funds", 
            "&7Withdraw currency from the team vault",
            "",
            "&c▶ Click to WITHDRAW $100.00"
        ));

        // Slot 22: Go back Arrow
        inv.setItem(22, createGuiItem(Material.ARROW, 
            "&e&l◀ Return to Dashboard", 
            "&7Go back to main team GUI panel"
        ));

        player.openInventory(inv);
    }

    /* Interface helpers */
    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.colorize(name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(plugin.colorize(line));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMemberSkullItem(Player player, String name, String... lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(plugin.colorize(name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(plugin.colorize(line));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
