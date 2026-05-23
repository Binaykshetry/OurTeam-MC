package com.ourteam;

import com.ourteam.commands.AdminCommand;
import com.ourteam.commands.TeamCommand;
import com.ourteam.listeners.PlayerListener;
import com.ourteam.manager.TeamManager;
import com.ourteam.model.Team;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

/**
 * Main activator class for the OurTeam Minecraft plugin.
 */
public final class OurTeam extends JavaPlugin {

    private TeamManager teamManager;
    private com.ourteam.gui.TeamGUIManager guiManager;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private FileConfiguration teamPermissionsConfig;
    private File teamPermissionsFile;
    private FileConfiguration chestPermissionsConfig;
    private File chestPermissionsFile;
    private net.milkbowl.vault.economy.Economy econ;
    private final java.util.Map<java.util.UUID, String> activeBankAction = new java.util.HashMap<>();
    private final java.util.Set<java.util.UUID> chatSpyPlayers = new java.util.HashSet<>();
    private final java.util.Map<java.util.UUID, java.util.Map<java.util.UUID, Long>> lastKillTimestamps = new java.util.HashMap<>();

    public java.util.Map<java.util.UUID, java.util.Map<java.util.UUID, Long>> getLastKillTimestamps() {
        return lastKillTimestamps;
    }

    @Override
    public void onEnable() {
        // Save default config.yml if it doesn't exist
        saveDefaultConfig();

        // Save default templates if they don't exist
        if (!new File(getDataFolder(), "team.yml").exists()) {
            saveResource("team.yml", false);
        }

        // Initialize directories
        File extensionsDir = new File(getDataFolder(), "extensions");
        if (!extensionsDir.exists()) {
            extensionsDir.mkdirs();
        }
        File teamInfoDir = new File(getDataFolder(), "teamInfo");
        if (!teamInfoDir.exists()) {
            teamInfoDir.mkdirs();
        }

        // Load custom config files
        loadMessages();
        loadTeamPermissions();
        loadChestPermissions();

        // Initialize managers
        this.teamManager = new TeamManager(this);
        this.guiManager = new com.ourteam.gui.TeamGUIManager(this);

        // Register Commands
        TeamCommand teamCmd = new TeamCommand(this);
        getCommand("team").setExecutor(teamCmd);
        getCommand("team").setTabCompleter(teamCmd);
        AdminCommand adminCmd = new AdminCommand(this);
        getCommand("teamadmin").setExecutor(adminCmd);
        getCommand("teamadmin").setTabCompleter(adminCmd);

        // Register Listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new com.ourteam.listeners.GUIListener(this), this);

        // Optional PlaceholderAPI Integration
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.ourteam.manager.OurTeamPlaceholders(this).register();
            new com.ourteam.manager.NTeamPlaceholders(this).register();
            getLogger().info("Successfully loaded PlaceholderAPI placeholders (%ourteam_* and %nteam_*)");
        }

        // Check for Vault / LuckPerms presence
        if (setupEconomy()) {
            getLogger().info("Successfully hooked into Vault Economy systems!");
        } else {
            getLogger().warning("Vault Economy not found! Some economy commands might not work.");
        }
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            getLogger().info("Successfully detected LuckPerms API! Node permissions fully loaded.");
        }

        // Register interest scheduler task for Team Bank
        registerInterestTask();

        getLogger().log(Level.INFO, "OurTeam plugin successfully enabled! Ready for cooperation.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private void registerInterestTask() {
        long interval = getConfig().getLong("team-bank.interest-interval-ticks", 12000L);
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (!getConfig().getBoolean("team-bank.enable", true)) {
                return;
            }
            double rateVal = getConfig().getDouble("team-bank.interest-rate", 5.0);
            double rate = rateVal / 100.0;
            double cap = getConfig().getDouble("team-bank.max-accrual", 15.0);

            for (Team team : teamManager.getTeams().values()) {
                double balance = team.getBankBalance();
                if (balance <= 0) continue;

                double interest = balance * rate;

                // Dynamic factor: member-size amplification (gives different-different money based on roster scale!)
                double memberFactor = 1.0 + (team.getMembers().size() * 0.05);
                interest = interest * memberFactor;

                if (interest > cap) {
                    interest = cap;
                }
                if (interest < 0.01) {
                    interest = 0.01;
                }

                // Round to 2 decimals
                interest = Math.round(interest * 100.0) / 100.0;
                team.addBankBalance(interest);
                teamManager.saveTeam(team);

                // Msg online players
                for (java.util.UUID memberUuid : team.getMembers()) {
                    org.bukkit.entity.Player teammate = getServer().getPlayer(memberUuid);
                    if (teammate != null && teammate.isOnline()) {
                        teammate.sendMessage(colorize("&a&l[Team Bank] &fYour team bank accrued &e$" + interest + " &fin interest! New bank balance: &a$" + String.format("%,.2f", team.getBankBalance())));
                    }
                }
            }
        }, interval, interval);
    }

    public net.milkbowl.vault.economy.Economy getEconomy() {
        return econ;
    }

    public java.util.Map<java.util.UUID, String> getActiveBankAction() {
        return activeBankAction;
    }

    @Override
    public void onDisable() {
        // Force save all data
        if (teamManager != null) {
            teamManager.saveAll();
        }
        getLogger().log(Level.INFO, "OurTeam plugin disabled. Data gracefully saved.");
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public com.ourteam.gui.TeamGUIManager getGuiManager() {
        return guiManager;
    }

    /**
     * Loads the messages.yml configuration, creating it with defaults if it doesn't exist.
     */
    public void loadMessages() {
        if (messagesFile == null) {
            messagesFile = new File(getDataFolder(), "messages.yml");
        }
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    /**
     * Retrieves the messages configuration instance.
     */
    public FileConfiguration getMessagesConfig() {
        if (messagesConfig == null) {
            loadMessages();
        }
        return messagesConfig;
    }

    /**
     * Loads the teampermissions.yml configuration, creating it with defaults if it doesn't exist.
     */
    public void loadTeamPermissions() {
        if (teamPermissionsFile == null) {
            teamPermissionsFile = new File(getDataFolder(), "teampermissions.yml");
        }
        if (!teamPermissionsFile.exists()) {
            saveResource("teampermissions.yml", false);
        }
        teamPermissionsConfig = YamlConfiguration.loadConfiguration(teamPermissionsFile);
    }

    /**
     * Retrieves the team permissions configuration instance.
     */
    public FileConfiguration getTeamPermissionsConfig() {
        if (teamPermissionsConfig == null) {
            loadTeamPermissions();
        }
        return teamPermissionsConfig;
    }

    /**
     * Loads the chestpermissions.yml configuration, creating it with defaults if it doesn't exist.
     */
    public void loadChestPermissions() {
        if (chestPermissionsFile == null) {
            chestPermissionsFile = new File(getDataFolder(), "chestpermissions.yml");
        }
        if (!chestPermissionsFile.exists()) {
            saveResource("chestpermissions.yml", false);
        }
        chestPermissionsConfig = YamlConfiguration.loadConfiguration(chestPermissionsFile);
    }

    /**
     * Retrieves the chest permissions configuration instance.
     */
    public FileConfiguration getChestPermissionsConfig() {
        if (chestPermissionsConfig == null) {
            loadChestPermissions();
        }
        return chestPermissionsConfig;
    }

    /**
     * Evaluates whether a player's rank inside a team authorizes some action according to teampermissions.yml.
     */
    public boolean hasTeamPermission(Team team, java.util.UUID playerUuid, String action) {
        if (team == null) return false;
        if (team.getOwner().equals(playerUuid)) {
            return true;
        }
        String role = team.getRole(playerUuid);
        java.util.List<String> allowedRanks = getTeamPermissionsConfig().getStringList("permissions." + action);
        if (allowedRanks == null || allowedRanks.isEmpty()) {
            // Default fallbacks in case of missing paths or unconfigured actions
            if (action.equalsIgnoreCase("invite") || action.equalsIgnoreCase("kick") || 
                action.equalsIgnoreCase("sethome") || action.equalsIgnoreCase("delhome") || 
                action.equalsIgnoreCase("setwarp") || action.equalsIgnoreCase("delwarp") || 
                action.equalsIgnoreCase("acceptrequest") || action.equalsIgnoreCase("pvp")) {
                return role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("OWNER") || role.equalsIgnoreCase("MODERATOR");
            }
            return true;
        }
        for (String allowed : allowedRanks) {
            if (allowed.equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Evaluates whether a player's rank inside a team authorizes some chest action according to chestpermissions.yml.
     */
    public boolean hasChestPermission(Team team, java.util.UUID playerUuid, String action) {
        if (team == null) return false;
        if (team.getOwner().equals(playerUuid)) {
            return true;
        }
        String role = team.getRole(playerUuid);
        java.util.List<String> allowedRanks = getChestPermissionsConfig().getStringList("chest-permissions." + action);
        if (allowedRanks == null || allowedRanks.isEmpty()) {
            // Default fallbacks
            if (action.equalsIgnoreCase("take-items") || action.equalsIgnoreCase("upgrade-slots")) {
                return role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("OWNER") || role.equalsIgnoreCase("MODERATOR");
            }
            return true;
        }
        for (String allowed : allowedRanks) {
            if (allowed.equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reloads all configuration files and custom messages/permissions.
     */
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        loadMessages();
        loadTeamPermissions();
        loadChestPermissions();
    }

    /**
     * Formatting color helper supporting rgb/hex: &#ffffff, <#ffffff>, #ffffff
     */
    public String colorize(String message) {
        if (message == null || message.isEmpty()) return "";

        // RGB / Hex parser using Bungee's ChatColor of 1.16+
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:&#|<#|#)([A-Fa-f0-9]{6})(?:>)?");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            try {
                net.md_5.bungee.api.ChatColor color = net.md_5.bungee.api.ChatColor.of("#" + hexCode);
                matcher.appendReplacement(buffer, color.toString());
            } catch (Exception e) {
                matcher.appendReplacement(buffer, matcher.group(0));
            }
        }
        matcher.appendTail(buffer);

        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    /**
     * Translates placeholder terms from custom messages.yml file.
     */
    public String getMsg(String key) {
        String msg = getMessagesConfig().getString(key);
        if (msg == null) {
            // Check fallback path in original config in case of older version template config key
            msg = getConfig().getString("messages." + key);
        }
        return msg != null ? colorize(msg) : colorize("&cMessage missing: " + key);
    }

    /**
     * Formats player TAB text based on their team affiliations.
     */
    public void updateTabFormatting(Player player) {
        String method = getConfig().getString("display-settings.tab-display-method", "PREFIX");
        if (method.equalsIgnoreCase("NONE")) {
            player.setPlayerListName(player.getName());
            return;
        }

        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.setPlayerListName(player.getName());
            return;
        }

        String format = getConfig().getString("display-settings.tab-format", "&b[{team}] &f");
        String formattedTag = colorize(format.replace("{team}", team.getName()));

        if (method.equalsIgnoreCase("PREFIX")) {
            player.setPlayerListName(formattedTag + player.getName());
        } else if (method.equalsIgnoreCase("SUFFIX")) {
            player.setPlayerListName(player.getName() + " " + formattedTag);
        }
    }

    public java.util.Set<java.util.UUID> getChatSpyPlayers() {
        return chatSpyPlayers;
    }

    private final java.util.Map<java.util.UUID, org.bukkit.scheduler.BukkitTask> activeTeleports = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, java.lang.Long> teleportCooldowns = new java.util.HashMap<>();

    public java.util.Map<java.util.UUID, org.bukkit.scheduler.BukkitTask> getActiveTeleports() {
        return activeTeleports;
    }

    public void startTeleport(Player player, org.bukkit.Location destination, String locationName, boolean isWarp) {
        // Cooldown check
        long cooldownSecs = getConfig().getLong("cooldowns-and-teleportation.teleport-cooldown", 45L);
        java.util.UUID uuid = player.getUniqueId();
        if (teleportCooldowns.containsKey(uuid)) {
            long remaining = (teleportCooldowns.get(uuid) + (cooldownSecs * 1000L)) - System.currentTimeMillis();
            if (remaining > 0) {
                player.sendMessage(colorize("&c[OurTeam] Teleport cooldown is active! Please wait " + (remaining / 1000L + 1) + "s."));
                return;
            }
        }

        // Cancel any existing teleport before starting a new one
        cancelTeleport(player, false);

        int warmupSecs = getConfig().getInt("cooldowns-and-teleportation.warp-warmup-seconds", 5);
        if (warmupSecs <= 0) {
            // Instant teleport
            executeTeleport(player, destination, locationName);
            teleportCooldowns.put(uuid, System.currentTimeMillis());
            return;
        }

        org.bukkit.Location startLoc = player.getLocation().clone();
        
        // Schedule a repeating task that ticks every second (20 ticks)
        org.bukkit.scheduler.BukkitRunnable runnable = new org.bukkit.scheduler.BukkitRunnable() {
            int secondsLeft = warmupSecs;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelTeleport(player, false);
                    return;
                }

                // Check movement as a backup
                if (getConfig().getBoolean("cooldowns-and-teleportation.cancel-on-movement", true)) {
                    org.bukkit.Location curLoc = player.getLocation();
                    if (startLoc.getWorld() != curLoc.getWorld() || startLoc.distanceSquared(curLoc) > 0.1) {
                        cancelTeleport(player, false, true);
                        return;
                    }
                }

                if (secondsLeft <= 0) {
                    executeTeleport(player, destination, locationName);
                    cancelTeleport(player, false, false);
                    teleportCooldowns.put(uuid, System.currentTimeMillis());
                    return;
                }

                // Emit particles
                String particleType = getConfig().getString("cooldowns-and-teleportation.teleport-particle", "PORTAL");
                if (!"NONE".equalsIgnoreCase(particleType)) {
                    try {
                        org.bukkit.Particle particle = org.bukkit.Particle.valueOf(particleType.toUpperCase());
                        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), 15, 0.4, 0.5, 0.4, 0.1);
                    } catch (Exception ignored) {}
                }

                // Per-second countdown message
                player.sendMessage(colorize("&aTeleporting to " + (isWarp ? "warp" : "home") + " '&e" + locationName + "&a' in &e" + secondsLeft + " &aseconds... Do not move!"));
                secondsLeft--;
            }
        };

        org.bukkit.scheduler.BukkitTask task = runnable.runTaskTimer(this, 0L, 20L);
        activeTeleports.put(uuid, task);
    }

    private void executeTeleport(Player player, org.bukkit.Location loc, String locationName) {
        player.teleport(loc);
        player.sendMessage(colorize("&aTeleported successfully!"));

        // Sound effect
        String soundType = getConfig().getString("cooldowns-and-teleportation.teleport-sound", "ENTITY_ENDERMAN_TELEPORT");
        if (!"NONE".equalsIgnoreCase(soundType)) {
            try {
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundType.toUpperCase());
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (Exception ignored) {}
        }
    }

    public void cancelTeleport(Player player, boolean isDamage) {
        cancelTeleport(player, isDamage, false);
    }

    public void cancelTeleport(Player player, boolean isDamage, boolean isMovement) {
        java.util.UUID uuid = player.getUniqueId();
        if (activeTeleports.containsKey(uuid)) {
            activeTeleports.get(uuid).cancel();
            activeTeleports.remove(uuid);
            if (isDamage) {
                player.sendMessage(colorize("&c[OurTeam] Teleportation cancelled due to taking physical damage!"));
            } else if (isMovement) {
                player.sendMessage(colorize("&c[OurTeam] Teleportation cancelled: You moved!"));
            }
        }
    }
}
