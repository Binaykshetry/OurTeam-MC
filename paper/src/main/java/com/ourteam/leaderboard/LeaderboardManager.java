package com.ourteam.leaderboard;

import com.ourteam.OurTeam;
import com.ourteam.gui.TeamGUIHolder;
import com.ourteam.model.PlayerStats;
import com.ourteam.model.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages leaderboard menus, categories, data caching, ajLeaderboards integration,
 * and built-in stat calculations.
 */
public class LeaderboardManager {

    private final OurTeam plugin;
    private final File configFile;
    private FileConfiguration config;

    private String mainTitle;
    private int mainSize;
    private Material mainBorderMaterial;
    private String mainBorderName;
    private List<Integer> mainBorderSlots;

    private int categorySize;
    private List<Integer> entrySlots;

    private int prevSlot;
    private Material prevMaterial;
    private String prevName;
    private List<String> prevLore;

    private int refreshSlot;
    private Material refreshMaterial;
    private String refreshName;
    private List<String> refreshLore;

    private int nextSlot;
    private Material nextMaterial;
    private String nextName;
    private List<String> nextLore;

    private int backSlot;
    private Material backMaterial;
    private String backName;
    private List<String> backLore;

    private final Map<String, LeaderboardCategory> categories = new LinkedHashMap<>();
    private final Map<String, CachedPage> cache = new HashMap<>();
    private static final long CACHE_DURATION_MS = 20_000L; // 20 seconds cache

    private static class CachedPage {
        final List<LeaderboardEntry> entries;
        final long timestamp;

        CachedPage(List<LeaderboardEntry> entries) {
            this.entries = entries;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
        }
    }

    public LeaderboardManager(OurTeam plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "LeaderboardGUI");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.configFile = new File(dir, "leaderboards.yml");
        load();
    }

    public void load() {
        if (!configFile.exists()) {
            try (InputStream in = plugin.getResource("LeaderboardGUI/leaderboards.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not copy default LeaderboardGUI/leaderboards.yml: " + e.getMessage());
            }
        }

        this.config = YamlConfiguration.loadConfiguration(configFile);
        this.categories.clear();
        this.cache.clear();

        // Main Menu settings
        this.mainTitle = config.getString("main_menu.title", "Leaderboards");
        this.mainSize = config.getInt("main_menu.size", 36);
        this.mainBorderMaterial = parseMaterial(config.getString("main_menu.border.material", "GRAY_STAINED_GLASS_PANE"), Material.GRAY_STAINED_GLASS_PANE);
        this.mainBorderName = config.getString("main_menu.border.name", " ");
        this.mainBorderSlots = config.getIntegerList("main_menu.border.slots");
        if (this.mainBorderSlots.isEmpty()) {
            this.mainBorderSlots = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17, 18, 19, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35);
        }

        // Category Menu settings
        this.categorySize = config.getInt("category_menu.size", 54);
        this.entrySlots = config.getIntegerList("category_menu.entry_slots");
        if (this.entrySlots.isEmpty()) {
            this.entrySlots = Arrays.asList(
                    10, 11, 12, 13, 14, 15, 16,
                    19, 20, 21, 22, 23, 24, 25,
                    28, 29, 30, 31, 32, 33, 34,
                    37, 38, 39, 40, 41, 42, 43
            );
        }

        this.prevSlot = config.getInt("category_menu.previous_page.slot", 48);
        this.prevMaterial = parseMaterial(config.getString("category_menu.previous_page.material", "RED_STAINED_GLASS_PANE"), Material.RED_STAINED_GLASS_PANE);
        this.prevName = config.getString("category_menu.previous_page.name", "&c&lPREVIOUS PAGE");
        this.prevLore = config.getStringList("category_menu.previous_page.lore");

        this.refreshSlot = config.getInt("category_menu.refresh.slot", 49);
        this.refreshMaterial = parseMaterial(config.getString("category_menu.refresh.material", "BELL"), Material.BELL);
        this.refreshName = config.getString("category_menu.refresh.name", "&e&lREFRESH");
        this.refreshLore = config.getStringList("category_menu.refresh.lore");

        this.nextSlot = config.getInt("category_menu.next_page.slot", 50);
        this.nextMaterial = parseMaterial(config.getString("category_menu.next_page.material", "LIME_STAINED_GLASS_PANE"), Material.LIME_STAINED_GLASS_PANE);
        this.nextName = config.getString("category_menu.next_page.name", "&a&lNEXT PAGE");
        this.nextLore = config.getStringList("category_menu.next_page.lore");

        this.backSlot = config.getInt("category_menu.back_button.slot", 45);
        this.backMaterial = parseMaterial(config.getString("category_menu.back_button.material", "ARROW"), Material.ARROW);
        this.backName = config.getString("category_menu.back_button.name", "&c&l◀ BACK");
        this.backLore = config.getStringList("category_menu.back_button.lore");

        // Load Categories
        ConfigurationSection sec = config.getConfigurationSection("categories");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                ConfigurationSection catSec = sec.getConfigurationSection(key);
                if (catSec == null) continue;

                LeaderboardCategory cat = new LeaderboardCategory(key);
                cat.setName(catSec.getString("name", "&e" + key.toUpperCase()));
                cat.setIcon(parseMaterial(catSec.getString("icon", "PAPER"), Material.PAPER));
                cat.setSlot(catSec.getInt("slot", -1));
                cat.setTitle(catSec.getString("title", key + " | Page {page}/{max_page}"));
                cat.setDescription(catSec.getString("description", key));
                cat.setValueLabel(catSec.getString("value_label", key));
                cat.setType(catSec.getString("type", "PLAYER"));
                cat.setInternalStat(catSec.getString("internal_stat", key));
                cat.setAjlbNamePlaceholder(catSec.getString("ajlb_name_placeholder", ""));
                cat.setAjlbValuePlaceholder(catSec.getString("ajlb_value_placeholder", ""));
                cat.setMaxPages(catSec.getInt("max_pages", 7));
                cat.setEntryTitle(catSec.getString("entry_title", "&a#{pos} {name}"));
                cat.setEntryLore(catSec.getStringList("entry_lore"));
                cat.setMainLore(catSec.getStringList("main_lore"));

                categories.put(key.toLowerCase(), cat);
            }
        }
        plugin.getLogger().info("Loaded " + categories.size() + " leaderboard categories.");
    }

    public void clearCache(String categoryId) {
        if (categoryId == null) {
            cache.clear();
        } else {
            cache.keySet().removeIf(k -> k.startsWith(categoryId.toLowerCase() + "_"));
        }
    }

    public Map<String, LeaderboardCategory> getCategories() {
        return categories;
    }

    public LeaderboardCategory getCategory(String id) {
        if (id == null) return null;
        return categories.get(id.toLowerCase());
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new TeamGUIHolder("leaderboard_main", ""), mainSize, colorize(mainTitle));

        // Fill border
        ItemStack borderItem = createSimpleItem(mainBorderMaterial, mainBorderName, Collections.emptyList());
        for (int slot : mainBorderSlots) {
            if (slot >= 0 && slot < mainSize) {
                inv.setItem(slot, borderItem);
            }
        }

        // Place categories
        for (LeaderboardCategory cat : categories.values()) {
            int slot = cat.getSlot();
            if (slot >= 0 && slot < mainSize) {
                List<String> lore = new ArrayList<>();
                for (String line : cat.getMainLore()) {
                    lore.add(colorize(line.replace("{description}", cat.getDescription())));
                }
                inv.setItem(slot, createSimpleItem(cat.getIcon(), cat.getName(), lore));
            }
        }

        player.openInventory(inv);
        try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        } catch (Exception ignored) {}
    }

    public void openCategoryMenu(Player player, String categoryId, int page) {
        LeaderboardCategory cat = getCategory(categoryId);
        if (cat == null) {
            player.sendMessage(colorize("&cLeaderboard category '" + categoryId + "' not found!"));
            return;
        }

        int maxPages = cat.getMaxPages();
        if (page < 1) page = 1;
        if (page > maxPages) page = maxPages;

        String title = cat.getTitle()
                .replace("{page}", String.valueOf(page))
                .replace("{max_page}", String.valueOf(maxPages));

        Inventory inv = Bukkit.createInventory(
                new TeamGUIHolder("leaderboard_category", cat.getId(), page),
                categorySize,
                colorize(title)
        );

        List<LeaderboardEntry> entries = resolveEntries(cat, page, entrySlots.size(), player);

        for (int i = 0; i < entrySlots.size(); i++) {
            int slot = entrySlots.get(i);
            if (i < entries.size()) {
                LeaderboardEntry entry = entries.get(i);
                ItemStack item;
                if (cat.getType().equalsIgnoreCase("TEAM")) {
                    item = new ItemStack(Material.WHITE_BANNER);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(colorize(cat.getEntryTitle()
                                .replace("{pos}", String.valueOf(entry.getPosition()))
                                .replace("{name}", entry.getName())));
                        List<String> lore = new ArrayList<>();
                        for (String l : cat.getEntryLore()) {
                            lore.add(colorize(l
                                    .replace("{pos}", String.valueOf(entry.getPosition()))
                                    .replace("{name}", entry.getName())
                                    .replace("{label}", cat.getValueLabel())
                                    .replace("{value}", entry.getFormattedValue())));
                        }
                        meta.setLore(lore);
                        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                        item.setItemMeta(meta);
                    }
                } else {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getName());
                    String nameLine = cat.getEntryTitle()
                            .replace("{pos}", String.valueOf(entry.getPosition()))
                            .replace("{name}", entry.getName());
                    List<String> lore = new ArrayList<>();
                    for (String l : cat.getEntryLore()) {
                        lore.add(colorize(l
                                .replace("{pos}", String.valueOf(entry.getPosition()))
                                .replace("{name}", entry.getName())
                                .replace("{label}", cat.getValueLabel())
                                .replace("{value}", entry.getFormattedValue())));
                    }
                    item = createPlayerSkullItem(op, nameLine, lore);
                }
                inv.setItem(slot, item);
            }
        }

        // Navigation & Controls
        if (prevSlot >= 0 && prevSlot < categorySize) {
            inv.setItem(prevSlot, createSimpleItem(prevMaterial, prevName, prevLore));
        }
        if (refreshSlot >= 0 && refreshSlot < categorySize) {
            inv.setItem(refreshSlot, createSimpleItem(refreshMaterial, refreshName, refreshLore));
        }
        if (nextSlot >= 0 && nextSlot < categorySize) {
            inv.setItem(nextSlot, createSimpleItem(nextMaterial, nextName, nextLore));
        }
        if (backSlot >= 0 && backSlot < categorySize) {
            inv.setItem(backSlot, createSimpleItem(backMaterial, backName, backLore));
        }

        player.openInventory(inv);
        try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        } catch (Exception ignored) {}
    }

    private List<LeaderboardEntry> resolveEntries(LeaderboardCategory cat, int page, int entriesPerPage, Player viewer) {
        String cacheKey = cat.getId().toLowerCase() + "_" + page;
        CachedPage cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.entries;
        }

        int startPos = (page - 1) * entriesPerPage + 1;
        int endPos = page * entriesPerPage;
        List<LeaderboardEntry> list = new ArrayList<>();

        boolean papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        boolean triedPapi = false;

        // Try ajlb / PlaceholderAPI first if placeholder pattern is provided
        if (papi && cat.getAjlbNamePlaceholder() != null && !cat.getAjlbNamePlaceholder().isEmpty()) {
            triedPapi = true;
            for (int pos = startPos; pos <= endPos; pos++) {
                String namePh = cat.getAjlbNamePlaceholder().replace("{pos}", String.valueOf(pos));
                String name = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(viewer, namePh);
                if (name != null && !name.isEmpty() && !name.equals("---") && !name.equalsIgnoreCase(namePh) && !name.equalsIgnoreCase("None")) {
                    String valPh = cat.getAjlbValuePlaceholder().replace("{pos}", String.valueOf(pos));
                    String val = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(viewer, valPh);
                    if (val == null || val.equalsIgnoreCase(valPh)) val = "0";
                    list.add(new LeaderboardEntry(pos, name, val));
                } else {
                    break;
                }
            }
        }

        // If PAPI returned empty or wasn't applicable, fallback to OurTeam native data
        if (list.isEmpty()) {
            list = resolveNativeEntries(cat, startPos, endPos);
        }

        cache.put(cacheKey, new CachedPage(list));
        return list;
    }

    private List<LeaderboardEntry> resolveNativeEntries(LeaderboardCategory cat, int startPos, int endPos) {
        List<LeaderboardEntry> result = new ArrayList<>();
        String stat = cat.getInternalStat().toLowerCase();

        if (cat.getType().equalsIgnoreCase("TEAM") || stat.equals("teams")) {
            List<Team> allTeams = new ArrayList<>(plugin.getTeamManager().getAllTeams());
            allTeams.sort((t1, t2) -> Integer.compare(t2.getCachedScore(), t1.getCachedScore()));
            for (int pos = startPos; pos <= endPos; pos++) {
                if (pos <= allTeams.size()) {
                    Team t = allTeams.get(pos - 1);
                    result.add(new LeaderboardEntry(pos, t.getName(), formatShort(t.getCachedScore())));
                }
            }
            return result;
        }

        List<PlayerStats> players = new ArrayList<>(plugin.getTeamManager().getAllPlayerStats());
        switch (stat) {
            case "money":
            case "balance":
                if (plugin.getEconomy() != null) {
                    players.sort((p1, p2) -> Double.compare(
                            plugin.getEconomy().getBalance(Bukkit.getOfflinePlayer(p2.getUuid())),
                            plugin.getEconomy().getBalance(Bukkit.getOfflinePlayer(p1.getUuid()))
                    ));
                    for (int pos = startPos; pos <= endPos; pos++) {
                        if (pos <= players.size()) {
                            PlayerStats ps = players.get(pos - 1);
                            double bal = plugin.getEconomy().getBalance(Bukkit.getOfflinePlayer(ps.getUuid()));
                            result.add(new LeaderboardEntry(pos, ps.getPlayerName(), formatShort(bal)));
                        }
                    }
                    return result;
                }
                break;
            case "mined":
            case "break":
                players.sort((p1, p2) -> Integer.compare(p2.getBlocksBroken(), p1.getBlocksBroken()));
                for (int pos = startPos; pos <= endPos; pos++) {
                    if (pos <= players.size()) {
                        PlayerStats ps = players.get(pos - 1);
                        result.add(new LeaderboardEntry(pos, ps.getPlayerName(), formatShort(ps.getBlocksBroken())));
                    }
                }
                return result;
            case "placed":
            case "build":
                players.sort((p1, p2) -> Integer.compare(p2.getBlocksPlaced(), p1.getBlocksPlaced()));
                for (int pos = startPos; pos <= endPos; pos++) {
                    if (pos <= players.size()) {
                        PlayerStats ps = players.get(pos - 1);
                        result.add(new LeaderboardEntry(pos, ps.getPlayerName(), formatShort(ps.getBlocksPlaced())));
                    }
                }
                return result;
            case "deaths":
                players.sort((p1, p2) -> Integer.compare(p2.getDeaths(), p1.getDeaths()));
                for (int pos = startPos; pos <= endPos; pos++) {
                    if (pos <= players.size()) {
                        PlayerStats ps = players.get(pos - 1);
                        result.add(new LeaderboardEntry(pos, ps.getPlayerName(), String.valueOf(ps.getDeaths())));
                    }
                }
                return result;
            case "mobkills":
                players.sort((p1, p2) -> Integer.compare(p2.getMobKills(), p1.getMobKills()));
                for (int pos = startPos; pos <= endPos; pos++) {
                    if (pos <= players.size()) {
                        PlayerStats ps = players.get(pos - 1);
                        result.add(new LeaderboardEntry(pos, ps.getPlayerName(), String.valueOf(ps.getMobKills())));
                    }
                }
                return result;
            case "kd":
                players.sort((p1, p2) -> Double.compare(p2.getKDR(), p1.getKDR()));
                for (int pos = startPos; pos <= endPos; pos++) {
                    if (pos <= players.size()) {
                        PlayerStats ps = players.get(pos - 1);
                        result.add(new LeaderboardEntry(pos, ps.getPlayerName(), String.format("%.2f", ps.getKDR())));
                    }
                }
                return result;
            case "playtime":
                players.sort((p1, p2) -> Long.compare(p2.getPlaytimeMs(), p1.getPlaytimeMs()));
                for (int pos = startPos; pos <= endPos; pos++) {
                    if (pos <= players.size()) {
                        PlayerStats ps = players.get(pos - 1);
                        result.add(new LeaderboardEntry(pos, ps.getPlayerName(), formatPlaytime(ps.getPlaytimeMs())));
                    }
                }
                return result;
            case "killstreak":
                players.sort((p1, p2) -> Integer.compare(p2.getBestKillstreak(), p1.getBestKillstreak()));
                for (int pos = startPos; pos <= endPos; pos++) {
                    if (pos <= players.size()) {
                        PlayerStats ps = players.get(pos - 1);
                        result.add(new LeaderboardEntry(pos, ps.getPlayerName(), String.valueOf(ps.getBestKillstreak())));
                    }
                }
                return result;
            case "distance":
                // Sort players by distance walked cm
                players.sort((p1, p2) -> {
                    OfflinePlayer op1 = Bukkit.getOfflinePlayer(p1.getUuid());
                    OfflinePlayer op2 = Bukkit.getOfflinePlayer(p2.getUuid());
                    long d1 = 0, d2 = 0;
                    try {
                        d1 = op1.getStatistic(Statistic.WALK_ONE_CM) + op1.getStatistic(Statistic.SPRINT_ONE_CM);
                        d2 = op2.getStatistic(Statistic.WALK_ONE_CM) + op2.getStatistic(Statistic.SPRINT_ONE_CM);
                    } catch (Exception ignored) {}
                    return Long.compare(d2, d1);
                });
                for (int pos = startPos; pos <= endPos; pos++) {
                    if (pos <= players.size()) {
                        PlayerStats ps = players.get(pos - 1);
                        OfflinePlayer op = Bukkit.getOfflinePlayer(ps.getUuid());
                        long d = 0;
                        try {
                            d = op.getStatistic(Statistic.WALK_ONE_CM) + op.getStatistic(Statistic.SPRINT_ONE_CM);
                        } catch (Exception ignored) {}
                        double meters = d / 100.0;
                        result.add(new LeaderboardEntry(pos, ps.getPlayerName(), formatDistance(meters)));
                    }
                }
                return result;
            case "kills":
            default:
                players.sort((p1, p2) -> Integer.compare(p2.getKills(), p1.getKills()));
                for (int pos = startPos; pos <= endPos; pos++) {
                    if (pos <= players.size()) {
                        PlayerStats ps = players.get(pos - 1);
                        result.add(new LeaderboardEntry(pos, ps.getPlayerName(), String.valueOf(ps.getKills())));
                    }
                }
                return result;
        }
        return result;
    }

    public static String formatShort(double val) {
        if (val < 1000.0) {
            if (val == (long) val) {
                return String.valueOf((long) val);
            }
            return String.format(Locale.US, "%.1f", val);
        } else if (val < 1_000_000.0) {
            return String.format(Locale.US, "%.1fk", val / 1_000.0);
        } else if (val < 1_000_000_000.0) {
            return String.format(Locale.US, "%.1fm", val / 1_000_000.0);
        } else {
            return String.format(Locale.US, "%.1fb", val / 1_000_000_000.0);
        }
    }

    public static String formatPlaytime(long ms) {
        if (ms < 60_000) {
            return (ms / 1000) + "s";
        } else if (ms < 3_600_000) {
            return (ms / 60_000) + "m";
        } else if (ms < 86_400_000) {
            long hours = ms / 3_600_000;
            long mins = (ms % 3_600_000) / 60_000;
            return hours + "h " + mins + "m";
        } else {
            long days = ms / 86_400_000;
            long hours = (ms % 86_400_000) / 3_600_000;
            return days + "d " + hours + "h";
        }
    }

    public static String formatDistance(double meters) {
        if (meters < 1000.0) {
            return String.format(Locale.US, "%.0fm", meters);
        } else {
            return String.format(Locale.US, "%.1fkm", meters / 1000.0);
        }
    }

    private ItemStack createSimpleItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat != null ? mat : Material.STONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) meta.setDisplayName(colorize(name));
            if (lore != null && !lore.isEmpty()) {
                List<String> list = new ArrayList<>();
                for (String l : lore) list.add(colorize(l));
                meta.setLore(list);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPlayerSkullItem(OfflinePlayer player, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            if (player != null) {
                meta.setOwningPlayer(player);
            }
            if (name != null) {
                meta.setDisplayName(colorize(name));
            }
            if (lore != null && !lore.isEmpty()) {
                List<String> list = new ArrayList<>();
                for (String l : lore) list.add(colorize(l));
                meta.setLore(list);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material parseMaterial(String name, Material def) {
        if (name == null || name.isEmpty()) return def;
        try {
            Material m = Material.matchMaterial(name.toUpperCase());
            return m != null ? m : def;
        } catch (Exception e) {
            return def;
        }
    }

    public static String colorize(String text) {
        if (text == null) return "";
        // Modern hex support: &#RRGGBB or <#RRGGBB>
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hexCode.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public int getPrevSlot() {
        return prevSlot;
    }

    public int getRefreshSlot() {
        return refreshSlot;
    }

    public int getNextSlot() {
        return nextSlot;
    }

    public int getBackSlot() {
        return backSlot;
    }
}
