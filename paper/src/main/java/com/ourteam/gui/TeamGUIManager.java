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
import java.util.UUID;

/**
 * Generates custom Minecraft Chest GUIs (Team menus) filled with interactive options.
 */
public class TeamGUIManager {

    private final OurTeam plugin;
    private final java.util.Map<String, org.bukkit.configuration.file.FileConfiguration> guiConfigs = new java.util.HashMap<>();

    public TeamGUIManager(OurTeam plugin) {
        this.plugin = plugin;
    }

    public void clearCache() {
        guiConfigs.clear();
    }

    public org.bukkit.configuration.file.FileConfiguration getGuiConfig(String menuKey) {
        String key = menuKey.toLowerCase();
        if (guiConfigs.containsKey(key)) {
            return guiConfigs.get(key);
        }
        java.io.File file = new java.io.File(plugin.getDataFolder(), "TeamGUI/" + key + ".yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                java.io.InputStream in = plugin.getResource("TeamGUI/" + key + ".yml");
                if (in != null) {
                    java.nio.file.Files.copy(in, file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } else {
                    file.createNewFile();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not save default GUI config for: " + key);
            }
        }
        org.bukkit.configuration.file.FileConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        guiConfigs.put(key, cfg);
        return cfg;
    }

    public int getMenuSize(String menuKey, int defaultSize) {
        int configuredSize = getGuiConfig(menuKey).getInt("size", -1);
        if (configuredSize == -1) {
            configuredSize = plugin.getConfig().getInt("gui-settings." + menuKey + ".size", defaultSize);
        }
        if (configuredSize > 0 && configuredSize <= 54 && configuredSize % 9 == 0) {
            return configuredSize;
        }
        return defaultSize;
    }

    public String getMenuTitle(String menuKey, String defaultTitle) {
        String title = getGuiConfig(menuKey).getString("title", null);
        if (title == null) {
            title = plugin.getConfig().getString("gui-settings." + menuKey + ".title", defaultTitle);
        }
        return plugin.colorize(title);
    }

    public String getMenuTitle(String menuKey, String defaultTitle, Team team) {
        String title = getGuiConfig(menuKey).getString("title", null);
        if (title == null) {
            title = plugin.getConfig().getString("gui-settings." + menuKey + ".title", defaultTitle);
        }
        if (team != null) {
            title = title.replace("{team}", team.getName());
        } else {
            title = title.replace("{team}", "None");
        }
        return plugin.colorize(title);
    }

    public String getMenuTitle(String menuKey, String defaultTitle, String placeholder, String replacement) {
        String title = getGuiConfig(menuKey).getString("title", null);
        if (title == null) {
            title = plugin.getConfig().getString("gui-settings." + menuKey + ".title", defaultTitle);
        }
        if (placeholder != null && replacement != null) {
            title = title.replace(placeholder, replacement);
        }
        return plugin.colorize(title);
    }

    public int getMenuSlot(String menuKey, String path, int defaultSlot, int maxSlot) {
        int slot = getGuiConfig(menuKey).getInt(path, -1);
        if (slot == -1) {
            slot = plugin.getConfig().getInt("gui-settings." + menuKey + "." + path, defaultSlot);
        }
        if (slot >= 0 && slot < maxSlot) {
            return slot;
        }
        return defaultSlot;
    }

    /**
     * Creates and opens the Main Team Dashboard (Menu: main)
     */
    public void openMainMenu(Player player, Team team) {
        String title = getMenuTitle("main", "&#33CCFFTeam Dashboard &7» &f" + team.getName(), team);
        int size = getMenuSize("main", 27);
        TeamGUIHolder holder = new TeamGUIHolder("main", team.getName());
        Inventory inv = Bukkit.createInventory(holder, size, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < size; i++) {
            inv.setItem(i, marker);
        }

        // Team Bank item placement
        int bankSlot = getMenuSlot("main", "bank-slot", 10, size);
        boolean bankEnabled = plugin.getConfig().getBoolean("team-bank.enable", true);
        if (bankSlot >= 0 && bankSlot < size) {
            if (bankEnabled) {
                inv.setItem(bankSlot, createGuiItem(Material.GOLD_INGOT, 
                    "&#FFCC00Team Bank Balance", 
                    "&7Status: Economy Management",
                    "",
                    "&fBalance: &a$" + String.format("%,.2f", team.getBankBalance()),
                    "",
                    "&a▶ Click to open Bank Menu"
                ));
            } else {
                inv.setItem(bankSlot, createGuiItem(Material.BARRIER, 
                    "&c&lTeam Bank Balance &7(DISABLED)", 
                    "&7Status: Economy Management &c(Disabled)",
                    "",
                    "&cThe Team Bank function has been disabled",
                    "&cby the server administration.",
                    ""
                ));
            }
        }

        // Team Members
        int membersSlot = getMenuSlot("main", "members-slot", 11, size);
        if (membersSlot >= 0 && membersSlot < size) {
            inv.setItem(membersSlot, createMemberSkullItem(player, 
                "&#CC66FFTeam Members &7(" + team.getMembers().size() + ")", 
                "&7Status: Roster Management",
                "",
                "&fTotal Members: &e" + team.getMembers().size() + "/8",
                "",
                "&a▶ Click to view team members roster"
            ));
        }

        // Ally Diplomacy / System
        int alliesSlot = getMenuSlot("main", "allies-slot", 12, size);
        if (alliesSlot >= 0 && alliesSlot < size) {
            inv.setItem(alliesSlot, createGuiItem(Material.SHIELD, 
                "&#33CCFFAlly Diplomacy Hub", 
                "&7Status: Active Treaties & Pacts",
                "",
                "&fForm coalitions, declare non-aggression",
                "&fpacts, and reinforce allied groups.",
                "",
                "&a▶ Click to manage team alliances"
            ));
        }

        // Team Homes & Warps
        int homesWarpsSlot = getMenuSlot("main", "homes-warps-slot", 13, size);
        if (homesWarpsSlot >= 0 && homesWarpsSlot < size) {
            inv.setItem(homesWarpsSlot, createGuiItem(Material.COMPASS, 
                "&#FF3336Homes & Warp locations", 
                "&7Status: Navigation Hub",
                "",
                "&fTotal Homes Set: &e" + team.getMultiHomes().size(),
                "&fTotal Warps Set: &b" + team.getMultiWarps().size(),
                "",
                "&a▶ Click to open Homes & Warps Menu"
            ));
        }

        // Team Enderchest
        int echestSlot = getMenuSlot("main", "echest-slot", 14, size);
        if (echestSlot >= 0 && echestSlot < size) {
            inv.setItem(echestSlot, createGuiItem(Material.ENDER_CHEST, 
                "&#CC99FFTeam Shared Enderchest", 
                "&7Status: Public vault of valuables",
                "",
                "&fSlots: &727 Slots",
                "",
                "&a▶ Click to access virtual chest storage"
            ));
        }

        // Team Statistics & Leaderboards
        int leaderboardSlot = getMenuSlot("main", "leaderboard-slot", 15, size);
        if (leaderboardSlot >= 0 && leaderboardSlot < size) {
            double kdr = team.getDeaths() > 0 ? (double) team.getKills() / team.getDeaths() : team.getKills();
            inv.setItem(leaderboardSlot, createGuiItem(Material.DIAMOND_SWORD, 
                "&#FF3333Team Statistics & Leaderboards", 
                "&7Status: Core Competitive Metrics",
                "",
                "&fKills: &a" + team.getKills() + " &7| &fDeaths: &c" + team.getDeaths(),
                "&fKDR: &e" + String.format("%.2f", kdr) + " Ratio",
                "&fPoints: &6" + team.getGrindingPoints() + " pts",
                "",
                "&e▶ Click to view Leaderboards"
            ));
        }

        // Settings Menu Toggle
        int settingsSlot = getMenuSlot("main", "settings-slot", 16, size);
        if (settingsSlot >= 0 && settingsSlot < size) {
            inv.setItem(settingsSlot, createGuiItem(Material.COMPARATOR, 
                "&#FF6600Team Settings Panel", 
                "&7Status: Configurations",
                "",
                "&fFriendly Fire: " + (team.isFriendlyFireEnabled() ? "&aON" : "&cOFF"),
                "",
                "&a▶ Click to manage rules and toggles"
            ));
        }

        // Leave Option (Slot 18 by default)
        int leaveSlot = getMenuSlot("main", "leave-slot", 18, size);
        if (leaveSlot >= 0 && leaveSlot < size) {
            inv.setItem(leaveSlot, createGuiItem(Material.RED_TULIP,
                "&#FF3333Leave Team Option",
                "&7Abandon or quit this team.",
                "",
                "&c▶ Click to LEAVE the team safely"
            ));
        }

        player.openInventory(inv);
    }

    /**
     * Creates and opens the Ally Diplomacy / System GUI (Menu: alliances)
     */
    public void openAlliesMenu(Player player, Team team) {
        String title = plugin.colorize("&#33CCFFAlly Diplomacy Hub");
        TeamGUIHolder holder = new TeamGUIHolder("alliances", team.getName());
        Inventory inv = Bukkit.createInventory(holder, 54, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, marker);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, marker);
        }

        // Slot 4: Info plaque
        inv.setItem(4, createGuiItem(Material.NETHER_STAR,
            "&#33CCFFAlliance & Diplomacy Overview",
            "&7Collaborate with other peer groups on the server.",
            "",
            "&f⚡ &7Allies cannot deal friendly fire to one another.",
            "&f⚡ &7Form strong networks to coordinate base defense."
        ));

        // Slot 49: Return Arrow
        inv.setItem(49, createGuiItem(Material.ARROW,
            "&e◀ Return to Dashboard",
            "&7Go back to main team GUI panel"
        ));

        // List other online/active teams in slots 9-44!
        int slotIdx = 9;
        java.util.List<Team> sorted = new java.util.ArrayList<>(plugin.getTeamManager().getAllTeams());
        for (Team otherTeam : sorted) {
            if (otherTeam.getName().equalsIgnoreCase(team.getName())) continue; // Skip own team
            if (slotIdx > 44) break;

            org.bukkit.OfflinePlayer owner = Bukkit.getOfflinePlayer(otherTeam.getOwner());
            inv.setItem(slotIdx, createMemberSkullItem(owner, "&#33CCFF" + otherTeam.getName(),
                "&7Status: Active Team",
                "&fOwner: &e" + (owner.getName() != null ? owner.getName() : "Unknown"),
                "&fMembers: &7" + otherTeam.getMembers().size() + "/8",
                "",
                "&a▶ Click to send alliance proposal!"
            ));
            slotIdx++;
        }

        player.openInventory(inv);
    }

    /**
     * Creates and opens the Team Settings sub-menu (Menu: settings)
     */
    public void openSettingsMenu(Player player, Team team) {
        String title = plugin.colorize("&#FF9933Team Settings Panel");
        TeamGUIHolder holder = new TeamGUIHolder("settings", team.getName());
        Inventory inv = Bukkit.createInventory(holder, 27, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, marker);
        }

        // Slot 10: Toggle PvP friendly fire
        String pvpStatus = team.isFriendlyFireEnabled() ? "&aENABLED &7(Damage ON)" : "&cDISABLED &7(Damage OFF)";
        inv.setItem(10, createGuiItem(Material.IRON_SWORD, 
            "&#FF3333Friendly Fire Toggle", 
            "&7Enables or disables PvP among team-members",
            "",
            "&fPvP Status: " + pvpStatus,
            "",
            "&e▶ Click to TOGGLE friendly fire pvp!"
        ));

        // Slot 11: Toggle TeamPay (Payment Sharing settings)
        String payStatus = team.isPayToggle() ? "&aENABLED &7(Deposits allowed)" : "&cDISABLED &7(Deposits blocked)";
        inv.setItem(11, createGuiItem(Material.SUNFLOWER, 
            "&#FFCC00TeamPay Toggle", 
            "&7Toggles whether team members can deposit into bank",
            "",
            "&fTeamPay Status: " + payStatus,
            "",
            "&e▶ Click to TOGGLE TeamPay deposits!"
        ));

        // Slot 12: Enderchest Access Lock
        String echestStatus = team.isEchestLocked() ? "&cLOCKED &7(Admin/Owner Only)" : "&aUNLOCKED &7(All Members)";
        inv.setItem(12, createGuiItem(Material.CHEST, 
            "&#CC99FFEnderchest Access Lock", 
            "&7Controls teammate access to shared enderchest",
            "",
            "&fAccess Lock Status: " + echestStatus,
            "",
            "&e▶ Click to TOGGLE Enderchest access lock"
        ));

        // Slot 13: Team Standing & Score Metrics (including "What does it mean" explanation to avoid GUI text overflow)
        team.recalculateScore(plugin);
        int score = team.getCachedScore();
        int rank = team.getRankPosition(plugin);
        int totalTeams = plugin.getTeamManager().getAllTeams().size();

        inv.setItem(13, createGuiItem(Material.NETHER_STAR, 
            "&#00FFCCTeam Standing & Score Metrics", 
            "&7Active Leaderboard Standing",
            "",
            "&fTeam Rank Position: &b#" + rank + " &7of &f" + totalTeams + " Teams",
            "&fTotal Team Score: &a" + score + " Points",
            "",
            "&e▶ What does this mean? (Hover details)",
            "&7Your TeamScore measures competitive grinding:",
            "&f- Active Members: &7+10 pts each",
            "&f- Team Bank Vault: &7+2 pts per $5,000",
            "&f- Combat Grinding: &7Pts from PvP kills",
            "&f  - Earn &e+5 pts &7per Kill",
            "&f  - Lose &c-2 pts &7per Death"
        ));

        // Slot 14: Open Join policy (registration tag)
        String joinPolicyStatus = team.isOpenJoin() ? "&aOPEN JOIN &7(No invite needed)" : "&cINVITE/REQUESTS &7(Invite/Apply required)";
        inv.setItem(14, createGuiItem(Material.OAK_DOOR, 
            "&#33CCFFOpen Join Policy", 
            "&7Toggle if outsiders can join without invites",
            "",
            "&fJoin Policy: " + joinPolicyStatus,
            "",
            "&e▶ Click to TOGGLE open join registration"
        ));

        // Slot 15: Team Chat Toggle
        String chatPolicyStatus = team.isTeamChatEnabled() ? "&aENABLED &7(Teammates can chat)" : "&cDISABLED &7(Team chat locked)";
        inv.setItem(15, createGuiItem(Material.PAPER, 
            "&#33CC99Team Chat Toggle", 
            "&7Toggles whether the team chat channel is active",
            "",
            "&fTeam Chat: " + chatPolicyStatus,
            "",
            "&e▶ Click to TOGGLE team-wide chat"
        ));

        // Slot 16: Member Invite Toggle
        String invitePolicyStatus = team.isMemberInviteEnabled() ? "&aENABLED &7(Members can invite)" : "&cDISABLED &7(Admins/Owner only)";
        inv.setItem(16, createGuiItem(Material.WRITABLE_BOOK, 
            "&#FF66CCMember Invite Toggle", 
            "&7Toggles if ordinary members can invite others",
            "",
            "&fMember Inviting: " + invitePolicyStatus,
            "",
            "&e▶ Click to TOGGLE invite permissions"
        ));

        // Slot 19: Teammate Login Alert Toggle
        String alertPolicyStatus = team.isLoginAlertsEnabled() ? "&aENABLED &7(Login broadcasts ON)" : "&cDISABLED &7(Broadcasts OFF)";
        inv.setItem(19, createGuiItem(Material.REDSTONE_LAMP, 
            "&#E0C068Teammate Login Alerts", 
            "&7Toggles notifications when teammates join/quit",
            "",
            "&fTeammate Logs: " + alertPolicyStatus,
            "",
            "&e▶ Click to TOGGLE login system alerts"
        ));

        // Slot 20: Join Requests List (Formerly shield in dashboard)
        int requestCount = team.getRequests().size();
        inv.setItem(20, createGuiItem(Material.SHIELD,
            "&#33CCFFJoin Requests Pool &7(" + requestCount + ")",
            "&7Status: Pending Applications Hub",
            "",
            "&fPending Requests: &e" + requestCount,
            "",
            "&a▶ Click to manage pending applications"
        ));

        // Slot 21: Leave Option
        inv.setItem(21, createGuiItem(Material.RED_TULIP,
            "&#FF3333Leave Team Option",
            "&7Abandon or quit this team.",
            "",
            "&7Warning: If you are the owner, leaving",
            "&7will automatically disband the team!",
            "",
            "&c▶ Click to LEAVE the team safely"
        ));

        // Slot 22: Team Ban / Disband Option
        inv.setItem(22, createGuiItem(Material.TNT,
            "&#FF0000Disband & Ban Team",
            "&7Remove the team permanently.",
            "&7Can only be initiated by the primary owner.",
            "",
            "&c▶ Click to BAN/DISBAND this team"
        ));

        // Slot 26: Go back arrow
        inv.setItem(26, createGuiItem(Material.ARROW, 
            "&e◀ Return to Dashboard", 
            "&7Go back to main team GUI panel"
        ));

        player.openInventory(inv);
    }

    /**
     * Creates and opens the Team Members sub-menu (Menu: members)
     */
    public void openMembersMenu(Player player, Team team) {
        String title = plugin.colorize("&#33CCFFMembers Directory &7» &f" + team.getName());
        TeamGUIHolder holder = new TeamGUIHolder("members", team.getName());
        Inventory inv = Bukkit.createInventory(holder, 54, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, marker);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, marker);
        }

        // Slot 4: Info item
        inv.setItem(4, createGuiItem(Material.BOOK,
            "&#33CCFFTeam Members List",
            "&7Roster management control page.",
            "",
            "&f💡 &7Members can access echest, homes/warps",
            "   &7and cooperate inside the team chat.",
            "",
            "&f💡 &7Admins and Owners can manage",
            "   &7roles and kick players directly."
        ));

        // Slot 49: Return Arrow
        inv.setItem(49, createGuiItem(Material.ARROW,
            "&e◀ Return to Dashboard",
            "&7Go back to main team GUI panel"
        ));

        // Let's populate the active members in slots 9-44
        int slotIdx = 9;
        for (UUID memberId : team.getMembers()) {
            if (slotIdx > 44) break;

            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberId);
            String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown Player";
            String role = team.getRole(memberId);
            String status = offlinePlayer.isOnline() ? "&2● Online" : "&7○ Offline";

            inv.setItem(slotIdx, createMemberSkullItem(offlinePlayer, "&#33CCFF" + name,
                "&7Status: " + status,
                "&fRole: &b" + role,
                "",
                "&7Click to manage actions"
            ));
            slotIdx++;
        }

        player.openInventory(inv);
    }

    /**
     * Creates and opens the Team Bank sub-menu (Menu: bank)
     */
    public void openBankMenu(Player player, Team team) {
        String title = getMenuTitle("bank", "&#FFCC00&lTeam Bank &7» &fDeposit/Withdraw");
        int size = getMenuSize("bank", 27);
        TeamGUIHolder holder = new TeamGUIHolder("bank", team.getName());
        Inventory inv = Bukkit.createInventory(holder, size, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < size; i++) {
            inv.setItem(i, marker);
        }

        // Deposit $100
        int dep100Slot = getMenuSlot("bank", "deposit100-slot", 10, size);
        if (dep100Slot >= 0 && dep100Slot < size) {
            inv.setItem(dep100Slot, createGuiItem(Material.EMERALD, 
                "&#00FF99&lDeposit $100.00", 
                "&7Directly deposit $100.00 from your hand.",
                "",
                "&e▶ Click to deposit $100"
            ));
        }

        // Custom Donation / Deposit
        int customDepSlot = getMenuSlot("bank", "customdeposit-slot", 11, size);
        if (customDepSlot >= 0 && customDepSlot < size) {
            inv.setItem(customDepSlot, createGuiItem(Material.WRITABLE_BOOK, 
                "&#00FFBC&lCustom Deposit / Donate", 
                "&7Deposit or donate any custom amount.",
                "&7Click here, then type the amount in chat.",
                "",
                "&e▶ Click to enter custom amount in chat"
            ));
        }

        // Ledger info
        int ledgerSlot = getMenuSlot("bank", "ledger-slot", 13, size);
        if (ledgerSlot >= 0 && ledgerSlot < size) {
            inv.setItem(ledgerSlot, createGuiItem(Material.GOLD_BLOCK, 
                "&#FFCC00&lAccount Balance Info", 
                "&7Bank details & interest stats",
                "",
                "&fLEDGER: &e$" + String.format("%,.2f", team.getBankBalance()),
                "&fInterest Rate: &b" + plugin.getConfig().getDouble("team-bank.interest-rate", 5.0) + "% accrual",
                "&fMax Accrual Cap: &b$" + plugin.getConfig().getDouble("team-bank.max-accrual", 15.0),
                ""
            ));
        }

        // Transaction History button
        int historySlot = getMenuSlot("bank", "history-slot", 12, size);
        if (historySlot >= 0 && historySlot < size) {
            inv.setItem(historySlot, createGuiItem(Material.BOOK, 
                "&#00FFBB&lTransaction History", 
                "&7View detailed logs of team transactions.",
                "&7Lists last 10 deposits and withdrawals.",
                "",
                "&e▶ Click to view bank ledger history"
            ));
        }

        // Custom Withdraw
        int customWithSlot = getMenuSlot("bank", "customwithdraw-slot", 15, size);
        if (customWithSlot >= 0 && customWithSlot < size) {
            inv.setItem(customWithSlot, createGuiItem(Material.REDSTONE, 
                "&#FF3366&lCustom Withdraw", 
                "&7Withdraw any custom amount of money.",
                "&7Click here, then type the amount in chat.",
                "&7&o(Admins/Owner Only)",
                "",
                "&c▶ Click to enter withdrawal amount in chat"
            ));
        }

        // Withdraw $100
        int with100Slot = getMenuSlot("bank", "withdraw100-slot", 16, size);
        if (with100Slot >= 0 && with100Slot < size) {
            inv.setItem(with100Slot, createGuiItem(Material.ANVIL, 
                "&#FF3333&lWithdraw $100.00", 
                "&7Directly withdraw $100.00 from the vault.",
                "&7&o(Admins/Owner Only)",
                "",
                "&c▶ Click to withdraw $100"
            ));
        }

        // Return Arrow
        int backSlot = getMenuSlot("bank", "back-slot", 22, size);
        if (backSlot >= 0 && backSlot < size) {
            inv.setItem(backSlot, createGuiItem(Material.ARROW, 
                "&e&l◀ Return to Dashboard", 
                "&7Go back to main team GUI panel"
            ));
        }

        player.openInventory(inv);
    }

    /**
     * Creates and opens the Bank Transaction History sub-menu (Menu: bank_history)
     */
    public void openBankHistoryMenu(Player player, Team team) {
        String title = getMenuTitle("bank_history", "&#FFCC00&lTransaction History", team);
        int size = getMenuSize("bank_history", 27);
        TeamGUIHolder holder = new TeamGUIHolder("bank_history", team.getName());
        Inventory inv = Bukkit.createInventory(holder, size, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < size; i++) {
            inv.setItem(i, marker);
        }

        // Get last 10 transactions
        List<com.ourteam.model.TeamTransaction> transactions = team.getBankTransactions();
        int maxLogs = Math.min(10, transactions.size());

        int startSlot = getMenuSlot("bank_history", "history-start-slot", 9, size);

        // Lay them out starting from startSlot
        for (int i = 0; i < maxLogs; i++) {
            int currentSlot = startSlot + i;
            if (currentSlot >= size || currentSlot < 0) {
                break;
            }
            com.ourteam.model.TeamTransaction tx = transactions.get(i);
            
            // Format nice relative or absolute timestamp
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String dateFormatted = sdf.format(new java.util.Date(tx.getTimestamp()));

            Material txMat = "DEPOSIT".equalsIgnoreCase(tx.getType()) ? Material.LIME_DYE : Material.ORANGE_DYE;
            String typePrefix = "DEPOSIT".equalsIgnoreCase(tx.getType()) ? "&#00FF66&lDEPOSIT" : "&#FF3355&lWITHDRAWAL";
            String lorePrefix = "DEPOSIT".equalsIgnoreCase(tx.getType()) ? "&7Deposited &a$" : "&7Withdrew &c$";

            inv.setItem(currentSlot, createGuiItem(txMat, 
                "&e&lTransaction #" + (transactions.size() - i),
                "&7Initiated by: &f" + tx.getPlayerName(),
                "&7Transaction Type: " + typePrefix,
                lorePrefix + String.format("%,.2f", tx.getAmount()),
                "&7Timestamp: &b" + dateFormatted
            ));
        }

        // Back button
        int backSlot = getMenuSlot("bank_history", "back-slot", 22, size);
        if (backSlot >= 0 && backSlot < size) {
            inv.setItem(backSlot, createGuiItem(Material.ARROW, 
                "&e&l◀ Return to Bank Menu", 
                "&7Go back to team bank controls"
            ));
        }

        player.openInventory(inv);
    }

    /**
     * Creates and opens the Homes & Warps sub-menu (Menu: homes_warps)
     */
    public void openHomesWarpsMenu(Player player, Team team) {
        String title = plugin.colorize("&#FF3366Homes & &#FF9933Warps");
        TeamGUIHolder holder = new TeamGUIHolder("homes_warps", team.getName());
        Inventory inv = Bukkit.createInventory(holder, 27, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, marker);
        }

        // Homes Button (Slot 11) - ALWAYS shown!
        int maxHomes = plugin.getConfig().getInt("cooldowns-and-teleportation.max-homes-per-team", 3);
        inv.setItem(11, createGuiItem(Material.RED_BED,
            "&#FF3366Team Homes List",
            "&7Status: Active Registered Homes",
            "",
            "&fTotal Set: &e" + team.getMultiHomes().size() + " &7/ &e" + maxHomes,
            "",
            "&a▶ Click to open Homes list"
        ));

        // Warps Button (Slot 15) - ALWAYS shown!
        int maxWarps = plugin.getConfig().getInt("cooldowns-and-teleportation.max-warps-per-team", 5);
        inv.setItem(15, createGuiItem(Material.ENDER_PEARL,
            "&#FF9933Team Warps List",
            "&7Status: Active Registered Warps",
            "",
            "&fTotal Set: &b" + team.getMultiWarps().size() + " &7/ &b" + maxWarps,
            "",
            "&a▶ Click to open Warps list"
        ));

        // Slot 22: Return to Dashboard
        inv.setItem(22, createGuiItem(Material.ARROW,
            "&e◀ Return to Dashboard",
            "&7Go back to main team GUI panel"
        ));

        player.openInventory(inv);
    }

    /**
     * Creates and opens the Team Homes listing menu (Menu: homes_list)
     */
    public void openHomesListMenu(Player player, Team team) {
        String title = plugin.colorize("&#FF3366Team Homes");
        TeamGUIHolder holder = new TeamGUIHolder("homes_list", team.getName());
        Inventory inv = Bukkit.createInventory(holder, 27, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, marker);
        }

        int startSlot = 9;
        for (java.util.Map.Entry<String, Team.TeamHome> entry : team.getMultiHomes().entrySet()) {
            if (startSlot > 17) break;
            Team.TeamHome home = entry.getValue();
            String name = entry.getKey();

            inv.setItem(startSlot, createGuiItem(Material.RED_BED,
                "&#FF3366Home: " + name,
                "&7World: &e" + home.getWorld(),
                "&7Coords: &fX: " + (int)home.getX() + " Y: " + (int)home.getY() + " Z: " + (int)home.getZ(),
                "",
                "&a▶ Left-Click &f(TELEPORT)",
                "&c◀ Right-Click &f(DELETE - Admin+)"
            ));
            startSlot++;
        }

        // Slot 22: Return to selector
        inv.setItem(22, createGuiItem(Material.ARROW,
            "&e◀ Return to Homes & Warps Menu",
            "&7Go back to navigation options"
        ));

        player.openInventory(inv);
    }

    /**
     * Creates and opens the Team Warps listing menu (Menu: warps_list)
     */
    public void openWarpsListMenu(Player player, Team team) {
        String title = plugin.colorize("&#FF9933Team Warps");
        TeamGUIHolder holder = new TeamGUIHolder("warps_list", team.getName());
        Inventory inv = Bukkit.createInventory(holder, 27, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, marker);
        }

        int startSlot = 9;
        for (java.util.Map.Entry<String, Team.TeamHome> entry : team.getMultiWarps().entrySet()) {
            if (startSlot > 17) break;
            Team.TeamHome warp = entry.getValue();
            String name = entry.getKey();

            inv.setItem(startSlot, createGuiItem(Material.ENDER_PEARL,
                "&#FF9933Warp: " + name,
                "&7World: &e" + warp.getWorld(),
                "&7Coords: &fX: " + (int)warp.getX() + " Y: " + (int)warp.getY() + " Z: " + (int)warp.getZ(),
                "",
                "&a▶ Left-Click &f(TELEPORT)",
                "&c◀ Right-Click &f(DELETE - Admin+)"
            ));
            startSlot++;
        }

        // Slot 22: Return to selector
        inv.setItem(22, createGuiItem(Material.ARROW,
            "&e◀ Return to Homes & Warps Menu",
            "&7Go back to navigation options"
        ));

        player.openInventory(inv);
    }

    /**
     * Opens a sub-menu showing detailed statistics and actions for a specific team member.
     */
    public void openMemberDetailMenu(Player viewer, Team team, org.bukkit.OfflinePlayer target) {
        String title = plugin.colorize("&#33CCFFMember Detail &7» &f" + target.getName());
        TeamGUIHolder holder = new TeamGUIHolder("member_detail:" + target.getUniqueId().toString(), team.getName());
        Inventory inv = Bukkit.createInventory(holder, 27, title);

        // Background decoration
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, marker);
        }

        // Slot 13: The member head showing all the requested details
        long firstPlayed = target.getFirstPlayed();
        String serverJoinDate = firstPlayed > 0 ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(firstPlayed)) : "Never";
        double totalDeposited = team.getMemberDeposits(target.getUniqueId());

        Team.MemberStats ms = team.getMemberStatsMap().get(target.getUniqueId().toString());
        int mKills = ms != null ? ms.getKills() : 0;
        int mDeaths = ms != null ? ms.getDeaths() : 0;
        long mPlaytimeMs = ms != null ? ms.getPlaytimeMs() : 0;
        long joinTime = ms != null ? ms.getJoinTime() : firstPlayed;
        String teamJoinDate = joinTime > 0 ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(joinTime)) : "Never";

        long hours = mPlaytimeMs / 3600000L;
        long minutes = (mPlaytimeMs % 3600000L) / 60000L;
        String formattedPlaytime = hours + "h " + minutes + "m";

        inv.setItem(13, createMemberSkullItem(target, "&#33CCFF" + target.getName(),
            "&7Status details for this member",
            "",
            "&f⚡ Role: &#FFCC00" + team.getRole(target.getUniqueId()),
            "&f⚡ Server Joined: &e" + serverJoinDate,
            "&f⚡ Team Joined: &b" + teamJoinDate,
            "&f⚡ Kills since joining: &a" + mKills,
            "&f⚡ Deaths since joining: &c" + mDeaths,
            "&f⚡ Playtime in team: &e" + formattedPlaytime,
            "&f⚡ Total Donated/Deposited: &a$" + String.format("%,.2f", totalDeposited),
            "",
            "&f⚡ UUID: &7" + target.getUniqueId().toString()
        ));

        // Rank management permissions check
        boolean canManage = (team.isAdminOrHigher(viewer.getUniqueId()) || viewer.isOp() || viewer.hasPermission("ourteam.admin"))
                && !target.getUniqueId().equals(viewer.getUniqueId())
                && !target.getUniqueId().equals(team.getOwner());

        if (canManage) {
            // Slot 10: Allocate ADMIN
            inv.setItem(10, createGuiItem(Material.GOLD_BLOCK,
                "&#33CCFFSet ADMIN Role",
                "&7Grants full administrator privileges.",
                "&fCan manage warps, structures, settings,",
                "&fpermissions, and lower ranks.",
                "",
                "&a▶ Click to allocate ADMIN rank"
            ));

            // Slot 11: Allocate MODERATOR
            inv.setItem(11, createGuiItem(Material.IRON_BLOCK,
                "&#FFCC00Set MODERATOR Role",
                "&7Grants group moderator rights.",
                "&fCan initiate invites, accept pending",
                "&fjoining applications, and toggle chat.",
                "",
                "&a▶ Click to allocate MODERATOR rank"
            ));

            // Slot 15: Allocate MEMBER
            inv.setItem(15, createGuiItem(Material.COAL_BLOCK,
                "&#E0E0E0Set MEMBER Role",
                "&7Resets status back to basic Member.",
                "&fRemoves all administrative command",
                "&frights and privileges.",
                "",
                "&a▶ Click to allocate MEMBER rank"
            ));

            // Slot 16: Kick option
            inv.setItem(16, createGuiItem(Material.LAVA_BUCKET,
                "&#FF3333Kick from Team",
                "&7Terminates roster membership.",
                "&fRemoves player completely from the",
                "&fteam roster.",
                "",
                "&c▶ Click to KICK member"
            ));
        } else {
            inv.setItem(10, createGuiItem(Material.BARRIER, "&7Unavailable", "&cYou cannot manage this player."));
            inv.setItem(11, createGuiItem(Material.BARRIER, "&7Unavailable", "&cYou cannot manage this player."));
            inv.setItem(15, createGuiItem(Material.BARRIER, "&7Unavailable", "&cYou cannot manage this player."));
            inv.setItem(16, createGuiItem(Material.BARRIER, "&7Unavailable", "&cYou cannot manage this player."));
        }

        // Slot 22: Go back
        inv.setItem(22, createGuiItem(Material.ARROW,
            "&e◀ Return to Members Roster",
            "&7Go back to roster menu"
        ));

        viewer.openInventory(inv);
    }

    /**
     * Creates and opens the Team Leaderboard GUI (Menu: leaderboard)
     */
    public void openLeaderboardMenu(Player player, Team viewerTeam) {
        String title = getMenuTitle("leaderboard", "&#33CCFFLeaderboard &7- &#A9C9FFTop Teams", viewerTeam);
        int size = getMenuSize("leaderboard", 54);
        TeamGUIHolder holder = new TeamGUIHolder("leaderboard", viewerTeam != null ? viewerTeam.getName() : "none");
        Inventory inv = Bukkit.createInventory(holder, size, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 9; i++) {
            if (i < size) {
                inv.setItem(i, marker);
            }
        }
        for (int i = size - 9; i < size; i++) {
            if (i >= 0 && i < size) {
                inv.setItem(i, marker);
            }
        }

        // Info plaque
        int plaqueSlot = getMenuSlot("leaderboard", "plaque-slot", 4, size);
        if (plaqueSlot >= 0 && plaqueSlot < size) {
            if (viewerTeam != null) {
                viewerTeam.recalculateScore(plugin);
                int vRank = viewerTeam.getRankPosition(plugin);
                int vScore = viewerTeam.getCachedScore();
                int totalTeamsCount = plugin.getTeamManager().getAllTeams().size();

                inv.setItem(plaqueSlot, createGuiItem(Material.NETHER_STAR,
                    "&#33CCFFYour Team Standing",
                    "&7Core competitive evaluation",
                    "",
                    "&fTeam Profile: &e" + viewerTeam.getName(),
                    "&fLeaderboard Rank: &#FFCC00#" + vRank + " &7of &f" + totalTeamsCount,
                    "&fTeam Score: &a" + vScore + " Points",
                    "",
                    "&7Scores update dynamically based on members,",
                    "&7bank content, and grinding activities."
                ));
            } else {
                inv.setItem(plaqueSlot, createGuiItem(Material.NETHER_STAR,
                    "&#33CCFFYour Team Standing",
                    "&7Core competitive evaluation",
                    "",
                    "&cYou are not in a team.",
                    "&7Join or create a team to compete on the leaderboards!"
                ));
            }
        }

        // Let's sort the teams from highest to lowest score
        java.util.List<Team> sorted = new java.util.ArrayList<>(plugin.getTeamManager().getAllTeams());
        for (Team t : sorted) {
            t.recalculateScore(plugin);
        }
        sorted.sort((t1, t2) -> Integer.compare(t2.getCachedScore(), t1.getCachedScore()));

        // Let's populate the active teams dynamically
        int listStart = getMenuSlot("leaderboard", "list-start", 9, size);
        int listEnd = getMenuSlot("leaderboard", "list-end", 44, size);
        int limit = Math.max(0, listEnd - listStart + 1);
        int teamsToShow = Math.min(limit, sorted.size());

        for (int i = 0; i < teamsToShow; i++) {
            Team currentTeam = sorted.get(i);
            int slotIdx = listStart + i;
            if (slotIdx > listEnd || slotIdx >= size) break;

            org.bukkit.OfflinePlayer owner = Bukkit.getOfflinePlayer(currentTeam.getOwner());
            String ownerName = owner.getName() != null ? owner.getName() : "Unknown Owner";
            double kdr = currentTeam.getDeaths() > 0 ? (double) currentTeam.getKills() / currentTeam.getDeaths() : currentTeam.getKills();

            String rankColor = "&f";
            if (i == 0) rankColor = "&#FFD700";
            else if (i == 1) rankColor = "&#C0C0C0";
            else if (i == 2) rankColor = "&#CD7F32";

            inv.setItem(slotIdx, createMemberSkullItem(owner, rankColor + "#" + (i + 1) + ". &#33CCFF" + currentTeam.getName(),
                "&7Competitive Rank Standing",
                "",
                "&f⚡ Score: &#FFCC00" + currentTeam.getCachedScore() + " Points",
                "&f⚡ Owner: &e" + ownerName,
                "&f⚡ Members: &7" + currentTeam.getMembers().size() + "/8",
                "&f⚡ Bank Balance: &a$" + String.format("%,.2f", currentTeam.getBankBalance()),
                "&f⚡ Combat Stats: &c" + currentTeam.getKills() + " Kills &7| &c" + currentTeam.getDeaths() + " Deaths (KDR: " + String.format("%.2f", kdr) + ")"
            ));
        }

        // Return Arrow (Standard slot is size - 5, matching 49 if size is 54)
        int backSlot = getMenuSlot("leaderboard", "back-slot", size - 5, size);
        if (backSlot >= 0 && backSlot < size) {
            inv.setItem(backSlot, createGuiItem(Material.ARROW,
                "&e◀ Return to Dashboard",
                "&7Go back to previous menu"
            ));
        }

        player.openInventory(inv);
    }

    /**
     * Creates and opens the Join Requests list menu (Menu: requests_list)
     */
    public void openRequestsMenu(Player player, Team team) {
        String title = plugin.colorize("&#33CCFF&lJoin Requests &7» &f" + team.getName());
        TeamGUIHolder holder = new TeamGUIHolder("requests_list", team.getName());
        Inventory inv = Bukkit.createInventory(holder, 27, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, marker);
        }

        int[] slots = { 10, 11, 12, 13, 14, 15, 16 };
        int idx = 0;
        java.util.Set<UUID> requestsSet = team.getRequests();

        if (requestsSet.isEmpty()) {
            inv.setItem(13, createGuiItem(Material.BARRIER,
                "&c&lNo Active Requests",
                "&7There are no pending join requests",
                "&7for your team at this time."
            ));
        } else {
            for (UUID requesterId : requestsSet) {
                if (idx >= slots.length) break;
                org.bukkit.OfflinePlayer requester = Bukkit.getOfflinePlayer(requesterId);
                String name = requester.getName() != null ? requester.getName() : "Unknown Requester";
                long firstPlayed = requester.getFirstPlayed();
                String joinDate = firstPlayed > 0 ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(firstPlayed)) : "Never";

                inv.setItem(slots[idx], createMemberSkullItem(requester, "&#33CCFF&l" + name,
                    "&7Requested to join your team!",
                    "",
                    "&f⚡ Server First Joined: &e" + joinDate,
                    "",
                    "&7[Admin Actions]",
                    "&a▶ Left-Click &f(ACCEPT Request)",
                    "&c◀ Right-Click &f(DECLINE/REJECT Request)"
                ));
                idx++;
            }
        }

        // Slot 22: Return to Dashboard
        inv.setItem(22, createGuiItem(Material.ARROW, 
            "&e&l◀ Return to Dashboard", 
            "&7Go back to main team GUI panel"
        ));

        player.openInventory(inv);
    }

    /**
     * Creates and opens the Teams List GUI (Menu: teams_list)
     */
    public void openTeamsListMenu(Player player) {
        Team viewerTeam = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        String title = getMenuTitle("list", "&#33CCFF&lALL ACTIVE TEAMS", viewerTeam);
        int size = getMenuSize("list", 54);
        String tName = viewerTeam != null ? viewerTeam.getName() : "none";
        TeamGUIHolder holder = new TeamGUIHolder("list", tName);
        Inventory inv = Bukkit.createInventory(holder, size, title);

        // Fill top and bottom with decorative glass panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 9; i++) {
            if (i < size) {
                inv.setItem(i, marker);
            }
        }
        for (int i = size - 9; i < size; i++) {
            if (i >= 0 && i < size) {
                inv.setItem(i, marker);
            }
        }

        // Info book plaque
        int infoSlot = getMenuSlot("list", "info-slot", 4, size);
        if (infoSlot >= 0 && infoSlot < size) {
            inv.setItem(infoSlot, createGuiItem(Material.BOOK,
                "&#33CCFF&lOurTeam Guilds Directory",
                "&7A directory of all active groups.",
                "",
                "&f💡 &bLeft/Right click &7on any team item",
                "   &7to directly transmit a Join Request!",
                "",
                "&f💡 &7Pending requests can be approved",
                "   &7by that team's moderators/owner."
            ));
        }

        // Return button (Close Menu / Go back)
        int backSlot = getMenuSlot("list", "back-slot", size - 5, size);
        if (backSlot >= 0 && backSlot < size) {
            if (viewerTeam == null) {
                inv.setItem(backSlot, createGuiItem(Material.ARROW,
                    "&e◀ Return to Discovery",
                    "&7Go back to Team Hub"
                ));
            } else {
                inv.setItem(backSlot, createGuiItem(Material.BARRIER,
                    "&c&lClose Menu",
                    "&7Return to game"
                ));
            }
        }

        // Let's populate the active teams in the grid slots dynamically
        int listStart = getMenuSlot("list", "list-start", 9, size);
        int listEnd = getMenuSlot("list", "list-end", 44, size);
        int limit = Math.max(0, listEnd - listStart + 1);

        int slotIdx = listStart;
        for (Team team : plugin.getTeamManager().getAllTeams()) {
            if (slotIdx > listEnd || slotIdx >= size) break;

            org.bukkit.OfflinePlayer owner = Bukkit.getOfflinePlayer(team.getOwner());
            String ownerName = owner.getName() != null ? owner.getName() : "Unknown";
            
            String status = team.isOpenJoin() ? "&2&lOPEN TO ALL" : "&6&lAPPLICATIONS ONLY";
            inv.setItem(slotIdx, createMemberSkullItem(owner,
                "&#33CCFF&l" + team.getName(),
                "&7Click to apply/join this team",
                "",
                "&fOwner/Leader: &b" + ownerName,
                "&fActive Members: &e" + team.getMembers().size() + "/8",
                "&fPolicy Status: " + status,
                "&fBank Wealth: &a$" + String.format("%,.0f", team.getBankBalance()),
                "",
                "&a▶ Click to send Direct Join Request"
            ));
            slotIdx++;
        }

        player.openInventory(inv);
    }

    /**
     * Creates and opens the No Team / Creation Hub GUI (Menu: noteam)
     */
    public void openNoTeamMenu(Player player) {
        String title = getMenuTitle("noteam", "&#33CCFFTeam Hub &7» &fDiscovery");
        int size = getMenuSize("noteam", 27);
        TeamGUIHolder holder = new TeamGUIHolder("noteam", "none");
        Inventory inv = Bukkit.createInventory(holder, size, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < size; i++) {
            inv.setItem(i, marker);
        }

        // Slot: Create a New Team
        int createSlot = getMenuSlot("noteam", "create-slot", 11, size);
        if (createSlot >= 0 && createSlot < size) {
            inv.setItem(createSlot, createGuiItem(Material.GRASS_BLOCK,
                "&#33CCFF&lCreate a New Team",
                "&7Form an organization to pool your efforts,",
                "&7protect your lands, trade with bank interest,",
                "&7and conquer team leaderboards!",
                "",
                "&a▶ Click to start creation process"
            ));
        }

        // Slot: View Active Teams Directory
        int listSlot = getMenuSlot("noteam", "list-slot", 13, size);
        if (listSlot >= 0 && listSlot < size) {
            inv.setItem(listSlot, createGuiItem(Material.BOOK,
                "&#FFCC00&lActive Teams Directory",
                "&7Expand your network! Look through all existing",
                "&7teams, and submit a Join Request to join one.",
                "",
                "&e▶ Click to browse active teams"
            ));
        }

        // Slot: Your Received Invitations & Options
        int invitationsSlot = getMenuSlot("noteam", "invitations-slot", 15, size);
        if (invitationsSlot >= 0 && invitationsSlot < size) {
            inv.setItem(invitationsSlot, createGuiItem(Material.PAPER,
                "&#CC66FF&lYour Invitations",
                "&7See teams that have invited you, or requests",
                "&7that you have pending.",
                "",
                "&d▶ Click to view active invites"
            ));
        }

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

    private ItemStack createMemberSkullItem(org.bukkit.OfflinePlayer player, String name, String... lore) {
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
