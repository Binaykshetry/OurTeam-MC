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
        String title = plugin.colorize("&#33CCFF&lTeam Dashboard &7» &f" + team.getName());
        TeamGUIHolder holder = new TeamGUIHolder("main", team.getName());
        Inventory inv = Bukkit.createInventory(holder, 27, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, marker);
        }

        // Slot 10: Team Bank
        inv.setItem(10, createGuiItem(Material.GOLD_INGOT, 
            "&#FFCC00&lTeam Bank Balance", 
            "&7Status: Economy Management",
            "",
            "&fBalance: &a$" + String.format("%,.2f", team.getBankBalance()),
            "",
            "&a▶ Click to open Bank Menu"
        ));

        // Slot 11: Team Members
        inv.setItem(11, createMemberSkullItem(player, 
            "&#CC66FF&lTeam Members &7(" + team.getMembers().size() + ")", 
            "&7Status: Roster Management",
            "",
            "&fTotal Members: &e" + team.getMembers().size() + "/8",
            "",
            "&a▶ Click to view team members roster"
        ));

        // Slot 12: Join Requests
        int requestCount = team.getRequests().size();
        inv.setItem(12, createGuiItem(Material.SHIELD, 
            "&#33CCFF&lJoin Requests &7(" + requestCount + ")", 
            "&7Status: Pending Applications Hub",
            "",
            "&fPending Requests: &e" + requestCount,
            "",
            "&a▶ Click to manage Join Requests"
        ));

        // Slot 13: Team Homes & Warps. Dynamically shown ONLY if at least 1 home or 1 warp is set!
        if (!team.getMultiHomes().isEmpty() || !team.getMultiWarps().isEmpty()) {
            inv.setItem(13, createGuiItem(Material.COMPASS, 
                "&#FF3366&lHOMES & &#FF9933&lWARPS", 
                "&7Status: Navigation Hub",
                "",
                "&fTotal Homes Set: &e" + team.getMultiHomes().size(),
                "&fTotal Warps Set: &b" + team.getMultiWarps().size(),
                "",
                "&a▶ Click to open Homes & Warps Menu"
            ));
        } else {
            inv.setItem(13, marker);
        }

        // Slot 14: Team Enderchest
        inv.setItem(14, createGuiItem(Material.ENDER_CHEST, 
            "&#CC99FF&lTeam Shared Enderchest", 
            "&7Status: Public vault of valuables",
            "",
            "&fSlots: &727 Slots",
            "",
            "&a▶ Click to access virtual chest storage"
        ));

        // Slot 15: Team Statistics & Leaderboards
        double kdr = team.getDeaths() > 0 ? (double) team.getKills() / team.getDeaths() : team.getKills();
        inv.setItem(15, createGuiItem(Material.DIAMOND_SWORD, 
            "&#FF3333&lTeam Statistics & Leaderboards", 
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
            "&#FF6600&lTeam Settings Panel", 
            "&7Status: Configurations",
            "",
            "&fFriendly Fire: " + (team.isFriendlyFireEnabled() ? "&aON" : "&cOFF"),
            "",
            "&a▶ Click to manage rules and toggles"
        ));

        // Slot 18: Leave Team Button (Accessible to all members)
        inv.setItem(18, createGuiItem(Material.RED_TULIP, 
            "&#FF3333&lLeave Team Option", 
            "&7Abandon or quit this team.",
            "",
            "&7Warning: If you are the owner, leaving",
            "&7will automatically disband the team!",
            "",
            "&c▶ Click to LEAVE the team safely"
        ));

        player.openInventory(inv);
    }

    /**
     * Creates and opens the Team Settings sub-menu (Menu: settings)
     */
    public void openSettingsMenu(Player player, Team team) {
        String title = plugin.colorize("&#FF6600&lTeam Settings Panel");
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
            "&#FF3333&lFriendly Fire Toggle", 
            "&7Enables or disables PvP among team-members",
            "",
            "&fPvP Status: " + pvpStatus,
            "",
            "&e▶ Click to TOGGLE friendly fire pvp!"
        ));

        // Slot 11: Toggle TeamPay (Payment Sharing settings)
        String payStatus = team.isPayToggle() ? "&a&lENABLED &7(Deposits allowed)" : "&c&lDISABLED &7(Deposits blocked)";
        inv.setItem(11, createGuiItem(Material.SUNFLOWER, 
            "&#FFCC00&lTeamPay Toggle", 
            "&7Toggles whether team members can deposit into bank",
            "",
            "&fTeamPay Status: " + payStatus,
            "",
            "&e▶ Click to TOGGLE TeamPay deposits!"
        ));

        // Slot 12: Enderchest Access Lock
        String echestStatus = team.isEchestLocked() ? "&c&lLOCKED &7(Admin/Owner Only)" : "&a&lUNLOCKED &7(All Members)";
        inv.setItem(12, createGuiItem(Material.CHEST, 
            "&#CC99FF&lEnderchest Access Lock", 
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
            "&#00FFCC&lTeam Standing & Score Metrics", 
            "&7Active Leaderboard Standing",
            "",
            "&fTeam Rank Position: &b&l#" + rank + " &7of &f" + totalTeams + " Teams",
            "&fTotal Team Score: &a&l" + score + " Points",
            "",
            "&e▶ What does this mean? (Hover details)",
            "&7Your TeamScore measures competitive grinding:",
            "&f- Active Members: &7+50 pts each",
            "&f- Team Bank Vault: &7+1 pt per $10k",
            "&f- Combat Grinding: &7Pts from PvP kills",
            "&f  &7- Earn &e+5 pts &7per Kill",
            "&f  &7- Lose &c-2 pts &7per Death"
        ));

        // Slot 14: Open Join policy (registration tag)
        String joinPolicyStatus = team.isOpenJoin() ? "&a&lOPEN JOIN &7(No invite needed)" : "&c&lINVITE/REQUESTS &7(Invite/Apply required)";
        inv.setItem(14, createGuiItem(Material.OAK_DOOR, 
            "&#33CCFF&lOpen Join Policy", 
            "&7Toggle if outsiders can join without invites",
            "",
            "&fJoin Policy: " + joinPolicyStatus,
            "",
            "&e▶ Click to TOGGLE open join registration"
        ));

        // Slot 15: Team Chat Toggle
        String chatPolicyStatus = team.isTeamChatEnabled() ? "&a&lENABLED &7(Teammates can chat)" : "&c&lDISABLED &7(Team chat locked)";
        inv.setItem(15, createGuiItem(Material.PAPER, 
            "&#33CC99&lTeam Chat Toggle", 
            "&7Toggles whether the team chat channel is active",
            "",
            "&fTeam Chat: " + chatPolicyStatus,
            "",
            "&e▶ Click to TOGGLE team-wide chat"
        ));

        // Slot 16: Member Invite Toggle
        String invitePolicyStatus = team.isMemberInviteEnabled() ? "&a&lENABLED &7(Members can invite)" : "&c&lDISABLED &7(Admins/Owner only)";
        inv.setItem(16, createGuiItem(Material.WRITABLE_BOOK, 
            "&#FF66CC&lMember Invite Toggle", 
            "&7Toggles if ordinary members can invite others",
            "",
            "&fMember Inviting: " + invitePolicyStatus,
            "",
            "&e▶ Click to TOGGLE invite permissions"
        ));

        // Slot 17: Teammate Login Alert Toggle
        String alertPolicyStatus = team.isLoginAlertsEnabled() ? "&a&lENABLED &7(Login broadcasts ON)" : "&c&lDISABLED &7(Broadcasts OFF)";
        inv.setItem(17, createGuiItem(Material.REDSTONE_LAMP, 
            "&#E0C068&lTeammate Login Alerts", 
            "&7Toggles notifications when teammates join/quit",
            "",
            "&fTeammate Logs: " + alertPolicyStatus,
            "",
            "&e▶ Click to TOGGLE login system alerts"
        ));

        // Slot 22: Go back
        inv.setItem(22, createGuiItem(Material.ARROW, 
            "&e&l◀ Return to Dashboard", 
            "&7Go back to main team GUI panel"
        ));

        player.openInventory(inv);
    }

    /**
     * Creates and opens the Team Members sub-menu (Menu: members)
     */
    public void openMembersMenu(Player player, Team team) {
        String title = plugin.colorize("&#CC66FF&lMembers &7» &f" + team.getName());
        TeamGUIHolder holder = new TeamGUIHolder("members", team.getName());
        Inventory inv = Bukkit.createInventory(holder, 27, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, marker);
        }

        // Layout members skulls
        int index = 0;
        int[] memberSlots = { 10, 11, 12, 13, 14, 15, 16 };

        for (UUID memberId : team.getMembers()) {
            if (index >= memberSlots.length) {
                break;
            }
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberId);
            String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown Player";
            String role = team.getRole(memberId);

            // Query details for lore
            String status = offlinePlayer.isOnline() ? "&a● Online" : "&7○ Offline";

            ItemStack skull = createMemberSkullItem(offlinePlayer, "&#CC66FF&l" + name,
                "&7Status: " + status,
                "&fRole: &b&l" + role,
                "",
                "&f- UUID: &7" + memberId.toString().substring(0, 8),
                "",
                "&e▶ Click to view detailed profile & actions"
            );

            inv.setItem(memberSlots[index], skull);
            index++;
        }

        // Slot 22: Return to Dashboard
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
        String title = plugin.colorize("&#FF3366&lHomes & &#FF9933&lWarps");
        TeamGUIHolder holder = new TeamGUIHolder("homes_warps", team.getName());
        Inventory inv = Bukkit.createInventory(holder, 27, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, marker);
        }

        // Homes Button (Slot 11) - Only shown if at least one home exists
        if (!team.getMultiHomes().isEmpty()) {
            int maxHomes = plugin.getConfig().getInt("cooldowns-and-teleportation.max-homes-per-team", 3);
            inv.setItem(11, createGuiItem(Material.RED_BED,
                "&#FF3366&lTeam Homes list",
                "&7Status: Active Registered Homes",
                "",
                "&fTotal Set: &e" + team.getMultiHomes().size() + " &7/ &e" + maxHomes,
                "",
                "&a▶ Click to open Homes list"
            ));
        }

        // Warps Button (Slot 15) - Only shown if at least one warp exists
        if (!team.getMultiWarps().isEmpty()) {
            int maxWarps = plugin.getConfig().getInt("cooldowns-and-teleportation.max-warps-per-team", 5);
            inv.setItem(15, createGuiItem(Material.ENDER_PEARL,
                "&#FF9933&lTeam Warps list",
                "&7Status: Active Registered Warps",
                "",
                "&fTotal Set: &b" + team.getMultiWarps().size() + " &7/ &b" + maxWarps,
                "",
                "&a▶ Click to open Warps list"
            ));
        }

        // Slot 22: Return to Dashboard
        inv.setItem(22, createGuiItem(Material.ARROW,
            "&e&l◀ Return to Dashboard",
            "&7Go back to main team GUI panel"
        ));

        player.openInventory(inv);
    }

    /**
     * Creates and opens the Team Homes listing menu (Menu: homes_list)
     */
    public void openHomesListMenu(Player player, Team team) {
        String title = plugin.colorize("&#FF3366&lTeam Homes");
        TeamGUIHolder holder = new TeamGUIHolder("homes_list", team.getName());
        Inventory inv = Bukkit.createInventory(holder, 27, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, marker);
        }

        int[] slots = { 10, 11, 12, 13, 14, 15, 16 };
        int idx = 0;
        for (java.util.Map.Entry<String, Team.TeamHome> entry : team.getMultiHomes().entrySet()) {
            if (idx >= slots.length) break;
            Team.TeamHome home = entry.getValue();
            String name = entry.getKey();

            inv.setItem(slots[idx], createGuiItem(Material.RED_BED,
                "&#FF3366&lHome: " + name,
                "&7World: &e" + home.getWorld(),
                "&7Coords: &fX: " + (int)home.getX() + " Y: " + (int)home.getY() + " Z: " + (int)home.getZ(),
                "",
                "&a▶ Left-Click &f(TELEPORT)",
                "&c◀ Right-Click &f(DELETE - Admin+)"
            ));
            idx++;
        }

        // Slot 22: Return to selector
        inv.setItem(22, createGuiItem(Material.ARROW,
            "&e&l◀ Return to Homes & Warps Menu",
            "&7Go back to navigation options"
        ));

        player.openInventory(inv);
    }

    /**
     * Creates and opens the Team Warps listing menu (Menu: warps_list)
     */
    public void openWarpsListMenu(Player player, Team team) {
        String title = plugin.colorize("&#FF9933&lTeam Warps");
        TeamGUIHolder holder = new TeamGUIHolder("warps_list", team.getName());
        Inventory inv = Bukkit.createInventory(holder, 27, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, marker);
        }

        int[] slots = { 10, 11, 12, 13, 14, 15, 16 };
        int idx = 0;
        for (java.util.Map.Entry<String, Team.TeamHome> entry : team.getMultiWarps().entrySet()) {
            if (idx >= slots.length) break;
            Team.TeamHome warp = entry.getValue();
            String name = entry.getKey();

            inv.setItem(slots[idx], createGuiItem(Material.ENDER_PEARL,
                "&#FF9933&lWarp: " + name,
                "&7World: &e" + warp.getWorld(),
                "&7Coords: &fX: " + (int)warp.getX() + " Y: " + (int)warp.getY() + " Z: " + (int)warp.getZ(),
                "",
                "&a▶ Left-Click &f(TELEPORT)",
                "&c◀ Right-Click &f(DELETE - Admin+)"
            ));
            idx++;
        }

        // Slot 22: Return to selector
        inv.setItem(22, createGuiItem(Material.ARROW,
            "&e&l◀ Return to Homes & Warps Menu",
            "&7Go back to navigation options"
        ));

        player.openInventory(inv);
    }

    /**
     * Opens a sub-menu showing detailed statistics and actions for a specific team member.
     */
    public void openMemberDetailMenu(Player viewer, Team team, org.bukkit.OfflinePlayer target) {
        String title = plugin.colorize("&#CC66FF&lMember Detail &7» &f" + target.getName());
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

        inv.setItem(13, createMemberSkullItem(target, "&#CC66FF&l" + target.getName(),
            "&7Status details for this member",
            "",
            "&f⚡ Role: &#33CCFF&l" + team.getRole(target.getUniqueId()),
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
            String role = team.getRole(target.getUniqueId());
            // Slot 11: Promote button if role is MEMBER
            if ("MEMBER".equalsIgnoreCase(role)) {
                inv.setItem(11, createGuiItem(Material.GOLD_INGOT,
                    "&#00FF99&lPromote to Admin",
                    "&7Grant this member administrator rights,",
                    "&7allowing them to manage settings,",
                    "&7warps/homes and invites.",
                    "",
                    "&a▶ Click to PROMOTE to ADMIN"
                ));
            } else {
                inv.setItem(11, createGuiItem(Material.BARRIER,
                    "&c&lCannot Promote",
                    "&7This player is already an Admin or Owner!"
                ));
            }

            // Slot 15: Demote/Kick button
            if ("ADMIN".equalsIgnoreCase(role) || "MODERATOR".equalsIgnoreCase(role)) {
                inv.setItem(15, createGuiItem(Material.REDSTONE,
                    "&#FF3366&lDemote to Member",
                    "&7Strip administrator permissions from",
                    "&7this user, returning them to Member.",
                    "",
                    "&c▶ Click to DEMOTE to MEMBER"
                ));
            } else {
                inv.setItem(15, createGuiItem(Material.LAVA_BUCKET,
                    "&#FF3333&lKick Member",
                    "&7Remove this member from the team.",
                    "&7They will lose access to team resources.",
                    "",
                    "&c▶ Click to KICK from Team"
                ));
            }
        } else {
            inv.setItem(11, createGuiItem(Material.BARRIER, "&7Information Only", "&fYou do not have administrative permissions", "&fto promote/demote this member."));
            inv.setItem(15, createGuiItem(Material.BARRIER, "&7Information Only", "&fYou do not have administrative permissions", "&fto kick this member."));
        }

        // Slot 22: Go back
        inv.setItem(22, createGuiItem(Material.ARROW,
            "&e&l◀ Return to Members Roster",
            "&7Go back to roster menu"
        ));

        viewer.openInventory(inv);
    }

    /**
     * Creates and opens the Team Leaderboard GUI (Menu: leaderboard)
     */
    public void openLeaderboardMenu(Player player, Team viewerTeam) {
        String title = plugin.colorize("&#00FFCC&lL E A D E R B O A R D &7- &#FFCC00&lTop Teams");
        TeamGUIHolder holder = new TeamGUIHolder("leaderboard", viewerTeam.getName());
        Inventory inv = Bukkit.createInventory(holder, 27, title);

        // Fill background with decorative panes
        ItemStack marker = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "&7Decoration slot");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, marker);
        }

        // Slot  4: Info marker of current viewer's team
        viewerTeam.recalculateScore(plugin);
        int vRank = viewerTeam.getRankPosition(plugin);
        int vScore = viewerTeam.getCachedScore();
        int totalTeamsCount = plugin.getTeamManager().getAllTeams().size();

        inv.setItem(4, createGuiItem(Material.NETHER_STAR,
            "&#00FFCC&lYour Team Standing",
            "&7Core competitive evaluation",
            "",
            "&fTeam Profile: &e" + viewerTeam.getName(),
            "&fLeaderboard Rank: &#FFD700&l#" + vRank + " &7of &f" + totalTeamsCount,
            "&fTeam Score: &a&l" + vScore + " Points",
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

        int[] slots = { 10, 11, 12, 13, 14, 15, 16 };
        int limit = Math.min(7, sorted.size());

        for (int i = 0; i < limit; i++) {
            Team currentTeam = sorted.get(i);
            String rankPrefix = "";
            Material blockMaterial = Material.STONE_BUTTON; // default

            switch (i) {
                case 0:
                    rankPrefix = "&#FFD700&l【1st】";
                    blockMaterial = Material.GOLD_BLOCK;
                    break;
                case 1:
                    rankPrefix = "&#C0C0C0&l【2nd】";
                    blockMaterial = Material.IRON_BLOCK;
                    break;
                case 2:
                    rankPrefix = "&#CD7F32&l【3rd】";
                    blockMaterial = Material.COPPER_BLOCK;
                    break;
                default:
                    rankPrefix = "&7&l【" + (i + 1) + "th】";
                    blockMaterial = Material.COAL_BLOCK;
                    break;
            }

            double kdr = currentTeam.getDeaths() > 0 ? (double) currentTeam.getKills() / currentTeam.getDeaths() : currentTeam.getKills();

            inv.setItem(slots[i], createGuiItem(blockMaterial,
                rankPrefix + " &#00FFCC&l" + currentTeam.getName(),
                "&7Competitive Rank Standing",
                "",
                "&f⚡ Score: &#FFCC00&l" + currentTeam.getCachedScore() + " Points",
                "&f⚡ Members: &e" + currentTeam.getMembers().size() + "/8",
                "&f⚡ Bank Balance: &a$" + String.format("%,.2f", currentTeam.getBankBalance()),
                "&f⚡ Combat Stats: &c" + currentTeam.getKills() + " Kills &7| &c" + currentTeam.getDeaths() + " Deaths (KDR: " + String.format("%.2f", kdr) + ")"
            ));
        }

        // Slot 22: Go back Arrow
        inv.setItem(22, createGuiItem(Material.ARROW,
            "&e&l◀ Return to Dashboard",
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
        java.util.List<Team> teams = plugin.getTeamManager().getAllTeams();
        int slotIdx = 9;
        for (Team team : teams) {
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
