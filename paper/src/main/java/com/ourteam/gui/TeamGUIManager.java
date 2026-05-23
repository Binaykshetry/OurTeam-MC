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

    public TeamGUIManager(OurTeam plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates and opens the Main Team Dashboard (Menu: main)
     */
    public void openMainMenu(Player player, Team team) {
        String title = plugin.colorize("&#33CCFFTeam Dashboard &7» &f" + team.getName());
        TeamGUIHolder holder = new TeamGUIHolder("main", team.getName());
        Inventory inv = Bukkit.createInventory(holder, 27, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, marker);
        }

        // Slot 10: Team Bank
        inv.setItem(10, createGuiItem(Material.GOLD_INGOT, 
            "&#FFCC00Team Bank Balance", 
            "&7Status: Economy Management",
            "",
            "&fBalance: &a$" + String.format("%,.2f", team.getBankBalance()),
            "",
            "&a▶ Click to open Bank Menu"
        ));

        // Slot 11: Team Members
        inv.setItem(11, createMemberSkullItem(player, 
            "&#CC66FFTeam Members &7(" + team.getMembers().size() + ")", 
            "&7Status: Roster Management",
            "",
            "&fTotal Members: &e" + team.getMembers().size() + "/8",
            "",
            "&a▶ Click to view team members roster"
        ));

        // Slot 12: Ally Diplomacy / System
        inv.setItem(12, createGuiItem(Material.SHIELD, 
            "&#33CCFFAlly Diplomacy Hub", 
            "&7Status: Active Treaties & Pacts",
            "",
            "&fForm coalitions, declare non-aggression",
            "&fpacts, and reinforce allied groups.",
            "",
            "&a▶ Click to manage team alliances"
        ));

        // Slot 13: Team Homes & Warps. ALWAYS SHOWN!
        inv.setItem(13, createGuiItem(Material.COMPASS, 
            "&#FF3336Homes & Warp locations", 
            "&7Status: Navigation Hub",
            "",
            "&fTotal Homes Set: &e" + team.getMultiHomes().size(),
            "&fTotal Warps Set: &b" + team.getMultiWarps().size(),
            "",
            "&a▶ Click to open Homes & Warps Menu"
        ));

        // Slot 14: Team Enderchest
        inv.setItem(14, createGuiItem(Material.ENDER_CHEST, 
            "&#CC99FFTeam Shared Enderchest", 
            "&7Status: Public vault of valuables",
            "",
            "&fSlots: &727 Slots",
            "",
            "&a▶ Click to access virtual chest storage"
        ));

        // Slot 15: Team Statistics & Leaderboards
        double kdr = team.getDeaths() > 0 ? (double) team.getKills() / team.getDeaths() : team.getKills();
        inv.setItem(15, createGuiItem(Material.DIAMOND_SWORD, 
            "&#FF3333Team Statistics & Leaderboards", 
            "&7Status: Core Competitive Metrics",
            "",
            "&fKills: &a" + team.getKills() + " &7| &fDeaths: &c" + team.getDeaths(),
            "&fKDR: &e" + String.format("%.2f", kdr) + " Ratio",
            "&fPoints: &6" + team.getGrindingPoints() + " pts",
            "",
            "&e▶ Click to view Leaderboards"
        ));

        // Slot 16: Settings Menu Toggle
        inv.setItem(16, createGuiItem(Material.COMPARATOR, 
            "&#FF6600Team Settings Panel", 
            "&7Status: Configurations",
            "",
            "&fFriendly Fire: " + (team.isFriendlyFireEnabled() ? "&aON" : "&cOFF"),
            "",
            "&a▶ Click to manage rules and toggles"
        ));

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
        String title = plugin.colorize("&#FFCC00&lTeam Bank &7» &fDeposit/Withdraw");
        TeamGUIHolder holder = new TeamGUIHolder("bank", team.getName());
        Inventory inv = Bukkit.createInventory(holder, 27, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, marker);
        }

        // Slot 10: Deposit $100
        inv.setItem(10, createGuiItem(Material.EMERALD, 
            "&#00FF99&lDeposit $100.00", 
            "&7Directly deposit $100.00 from your hand.",
            "",
            "&e▶ Click to deposit $100"
        ));

        // Slot 11: Custom Donation / Deposit (Writable Book)
        inv.setItem(11, createGuiItem(Material.WRITABLE_BOOK, 
            "&#00FFBC&lCustom Deposit / Donate", 
            "&7Deposit or donate any custom amount.",
            "&7Click here, then type the amount in chat.",
            "",
            "&e▶ Click to enter custom amount in chat"
        ));

        // Slot 13: Gold Block of current ledger info
        inv.setItem(13, createGuiItem(Material.GOLD_BLOCK, 
            "&#FFCC00&lAccount Balance Info", 
            "&7Bank details & interest stats",
            "",
            "&fLEDGER: &e$" + String.format("%,.2f", team.getBankBalance()),
            "&fInterest Rate: &b" + plugin.getConfig().getDouble("team-bank.interest-rate", 5.0) + "% accrual",
            "&fMax Accrual Cap: &b$" + plugin.getConfig().getDouble("team-bank.max-accrual", 15.0),
            ""
        ));

        // Slot 15: Custom Withdraw (Redstone)
        inv.setItem(15, createGuiItem(Material.REDSTONE, 
            "&#FF3366&lCustom Withdraw", 
            "&7Withdraw any custom amount of money.",
            "&7Click here, then type the amount in chat.",
            "&7&o(Admins/Owner Only)",
            "",
            "&c▶ Click to enter withdrawal amount in chat"
        ));

        // Slot 16: Withdraw $100
        inv.setItem(16, createGuiItem(Material.ANVIL, 
            "&#FF3333&lWithdraw $100.00", 
            "&7Directly withdraw $100.00 from the vault.",
            "&7&o(Admins/Owner Only)",
            "",
            "&c▶ Click to withdraw $100"
        ));

        // Slot 22: Go back Arrow
        inv.setItem(22, createGuiItem(Material.ARROW, 
            "&e&l◀ Return to Dashboard", 
            "&7Go back to main team GUI panel"
        ));

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
        String joinDate = firstPlayed > 0 ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(firstPlayed)) : "Never";
        double totalDeposited = team.getMemberDeposits(target.getUniqueId());

        inv.setItem(13, createMemberSkullItem(target, "&#33CCFF" + target.getName(),
            "&7Status details for this member",
            "",
            "&f⚡ Role: &#FFCC00" + team.getRole(target.getUniqueId()),
            "&f⚡ Server Joined: &e" + joinDate,
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
        String title = plugin.colorize("&#33CCFFLeaderboard &7- &#A9C9FFTop Teams");
        TeamGUIHolder holder = new TeamGUIHolder("leaderboard", viewerTeam.getName());
        Inventory inv = Bukkit.createInventory(holder, 54, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, marker);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, marker);
        }

        // Slot 4: Info marker of current viewer's team
        viewerTeam.recalculateScore(plugin);
        int vRank = viewerTeam.getRankPosition(plugin);
        int vScore = viewerTeam.getCachedScore();
        int totalTeamsCount = plugin.getTeamManager().getAllTeams().size();

        inv.setItem(4, createGuiItem(Material.NETHER_STAR,
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

        // Let's sort the teams from highest to lowest score
        java.util.List<Team> sorted = new java.util.ArrayList<>(plugin.getTeamManager().getAllTeams());
        for (Team t : sorted) {
            t.recalculateScore(plugin);
        }
        sorted.sort((t1, t2) -> Integer.compare(t2.getCachedScore(), t1.getCachedScore()));

        // Let's populate the active teams in slots 9-44
        int limit = Math.min(36, sorted.size());
        for (int i = 0; i < limit; i++) {
            Team currentTeam = sorted.get(i);
            int slotIdx = 9 + i;

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

        // Slot 49: Go back Arrow
        inv.setItem(49, createGuiItem(Material.ARROW,
            "&e◀ Return to Dashboard",
            "&7Go back to main team GUI panel"
        ));

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
        String title = plugin.colorize("&#33CCFF&lALL ACTIVE TEAMS");
        Team viewerTeam = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        String tName = viewerTeam != null ? viewerTeam.getName() : "none";
        TeamGUIHolder holder = new TeamGUIHolder("list", tName);
        Inventory inv = Bukkit.createInventory(holder, 54, title);

        // Fill top and bottom with decorative glass panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, marker);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, marker);
        }

        // Slot 4: Info book
        inv.setItem(4, createGuiItem(Material.BOOK,
            "&#33CCFF&lOurTeam Guilds Directory",
            "&7A directory of all active groups.",
            "",
            "&f💡 &bLeft/Right click &7on any team item",
            "   &7to directly transmit a Join Request!",
            "",
            "&f💡 &7Pending requests can be approved",
            "   &7by that team's moderators/owner."
        ));

        // Slot 49: Close barrier
        inv.setItem(49, createGuiItem(Material.BARRIER,
            "&c&lClose Menu",
            "&7Return to game"
        ));

        // Let's populate the active teams in the grid slots (9 to 44)
        int slotIdx = 9;
        for (Team team : plugin.getTeamManager().getAllTeams()) {
            if (slotIdx > 44) break; // limit to 36 teams per page to stay in grid

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
