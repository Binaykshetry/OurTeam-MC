package com.ourteam.commands;

import com.ourteam.OurTeam;
import com.ourteam.leaderboard.LeaderboardCategory;
import com.ourteam.leaderboard.LeaderboardManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles /leaderboard, /lb, and /leaderboards commands.
 */
public class LeaderboardCommand implements CommandExecutor, TabCompleter {

    private final OurTeam plugin;

    public LeaderboardCommand(OurTeam plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LeaderboardManager lm = plugin.getLeaderboardManager();
        if (lm == null) {
            sender.sendMessage(LeaderboardManager.colorize("&cLeaderboard system is currently initializing..."));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be run by in-game players.");
                return true;
            }
            Player player = (Player) sender;
            lm.openMainMenu(player);
            return true;
        }

        // Subcommand: reload
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("ourteam.admin")) {
                sender.sendMessage(LeaderboardManager.colorize("&cYou do not have permission to reload leaderboards!"));
                return true;
            }
            lm.load();
            sender.sendMessage(LeaderboardManager.colorize("&a[OurTeam] Leaderboard configurations successfully reloaded!"));
            return true;
        }

        // Subcommand: category name
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by in-game players.");
            return true;
        }

        Player player = (Player) sender;
        String catId = args[0].toLowerCase();
        LeaderboardCategory cat = lm.getCategory(catId);
        if (cat != null) {
            int page = 1;
            if (args.length >= 2) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException ignored) {}
            }
            lm.openCategoryMenu(player, cat.getId(), page);
            return true;
        }

        // Unknown category
        player.sendMessage(LeaderboardManager.colorize("&cUnknown leaderboard category '&e" + args[0] + "&c'. Opening main menu..."));
        lm.openMainMenu(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        LeaderboardManager lm = plugin.getLeaderboardManager();
        if (lm == null) return list;

        if (args.length == 1) {
            list.addAll(lm.getCategories().keySet());
            if (sender.hasPermission("ourteam.admin")) {
                list.add("reload");
            }
            String current = args[0].toLowerCase();
            return list.stream()
                    .filter(s -> s.toLowerCase().startsWith(current))
                    .collect(Collectors.toList());
        }
        return list;
    }
}
