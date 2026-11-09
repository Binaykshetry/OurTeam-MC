package com.ourteam.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ourteam.OurTeam;
import com.ourteam.model.PlayerStats;
import com.ourteam.model.Team;

import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles saving and loading team and player data using SQLite (data.db), MySQL databases, or JSON files.
 */
public class StorageManager {
    private final OurTeam plugin;
    private final File teamsFolder;
    private final File legacyDataFile;
    private final File sqliteDbFile;
    private final Gson gson;

    public StorageManager(OurTeam plugin) {
        this.plugin = plugin;
        this.teamsFolder = new File(plugin.getDataFolder(), "teams");
        this.legacyDataFile = new File(plugin.getDataFolder(), "teams.json");
        String dbName = plugin.getConfig().getString("storage.sqlite.file", "data.db");
        this.sqliteDbFile = new File(plugin.getDataFolder(), dbName);
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        // Migrate older ourteam.db to data.db if needed
        File oldDb = new File(plugin.getDataFolder(), "ourteam.db");
        if (oldDb.exists() && !sqliteDbFile.exists()) {
            oldDb.renameTo(sqliteDbFile);
        }
    }

    private Connection getConnection() throws SQLException {
        String type = plugin.getConfig().getString("storage.type", "SQLITE").toUpperCase();
        if (type.equals("MYSQL")) {
            String host = plugin.getConfig().getString("storage.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("storage.mysql.port", 3306);
            String db = plugin.getConfig().getString("storage.mysql.database", "ourteam_db");
            String user = plugin.getConfig().getString("storage.mysql.username", "administrator");
            String pass = plugin.getConfig().getString("storage.mysql.password", "secure_password_here");
            boolean useSSL = plugin.getConfig().getBoolean("storage.mysql.use-ssl", false);
            String url = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=" + useSSL + "&allowPublicKeyRetrieval=true&autoReconnect=true";
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                try {
                    Class.forName("com.mysql.jdbc.Driver");
                } catch (ClassNotFoundException ignored) {}
            }
            return DriverManager.getConnection(url, user, pass);
        } else {
            // Default: SQLITE using data.db
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            String url = "jdbc:sqlite:" + sqliteDbFile.getAbsolutePath();
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException ignored) {}
            return DriverManager.getConnection(url);
        }
    }

    private void initDatabase() {
        String type = plugin.getConfig().getString("storage.type", "SQLITE").toUpperCase();
        if (type.equals("JSON")) return;
        try (Connection conn = getConnection()) {
            if (conn == null) return;
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS ourteam_teams (" +
                        "id VARCHAR(36) PRIMARY KEY, " +
                        "team_name VARCHAR(64) UNIQUE, " +
                        "data LONGTEXT, " +
                        "disbanded BOOLEAN DEFAULT FALSE)");

                stmt.execute("CREATE TABLE IF NOT EXISTS ourteam_players (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "player_name VARCHAR(64), " +
                        "kills INT DEFAULT 0, " +
                        "deaths INT DEFAULT 0, " +
                        "blocks_placed INT DEFAULT 0, " +
                        "blocks_broken INT DEFAULT 0, " +
                        "mob_kills INT DEFAULT 0, " +
                        "playtime_ms BIGINT DEFAULT 0, " +
                        "grinding_points INT DEFAULT 0, " +
                        "last_seen BIGINT DEFAULT 0)");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "SQLite/MySQL Database Table Initialization Failed!", e);
        }
    }

    /**
     * Loads teams by reading from data.db / MySQL (or JSON fallback).
     */
    public Map<UUID, Team> loadTeams() {
        String type = plugin.getConfig().getString("storage.type", "SQLITE").toUpperCase();
        if (type.equals("JSON")) {
            return loadTeamsFromJson();
        }

        initDatabase();
        Map<UUID, Team> loadedTeams = new HashMap<>();
        try (Connection conn = getConnection()) {
            if (conn != null) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT data FROM ourteam_teams WHERE disbanded = FALSE")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String data = rs.getString("data");
                            Team team = gson.fromJson(data, Team.class);
                            if (team != null && !team.isDisbanded()) {
                                team.getEchest();
                                loadedTeams.put(team.getId(), team);
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load teams from SQL Database (" + type + ")!", e);
        }

        // Automatic migration: If database has 0 teams, check if JSON files exist and import them
        if (loadedTeams.isEmpty() && (legacyDataFile.exists() || (teamsFolder.exists() && teamsFolder.listFiles() != null && teamsFolder.listFiles().length > 0))) {
            Map<UUID, Team> jsonTeams = loadTeamsFromJson();
            if (!jsonTeams.isEmpty()) {
                plugin.getLogger().info("Migrated " + jsonTeams.size() + " teams from JSON storage into " + type + " (data.db)!");
                for (Team t : jsonTeams.values()) {
                    saveTeam(t);
                }
                loadedTeams = jsonTeams;
            }
        }

        return loadedTeams;
    }

    private Map<UUID, Team> loadTeamsFromJson() {
        Map<UUID, Team> loadedTeams = new HashMap<>();

        if (legacyDataFile.exists() && (!teamsFolder.exists() || teamsFolder.listFiles() == null || teamsFolder.listFiles().length == 0)) {
            try (Reader reader = new FileReader(legacyDataFile)) {
                com.google.gson.reflect.TypeToken<HashMap<UUID, Team>> typeToken = new com.google.gson.reflect.TypeToken<HashMap<UUID, Team>>() {};
                Map<UUID, Team> legacyTeams = gson.fromJson(reader, typeToken.getType());
                if (legacyTeams != null) {
                    return legacyTeams;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not migrate legacy teams.json data file!", e);
            }
        }

        if (!teamsFolder.exists()) {
            return loadedTeams;
        }

        File[] files = teamsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (files == null) {
            return loadedTeams;
        }

        for (File file : files) {
            try (Reader reader = new FileReader(file)) {
                Team team = gson.fromJson(reader, Team.class);
                if (team != null && !team.isDisbanded()) {
                    team.getEchest();
                    loadedTeams.put(team.getId(), team);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not load team file: " + file.getName(), e);
            }
        }

        return loadedTeams;
    }

    /**
     * Saves all teams back to data.db or MySQL or JSON.
     */
    public void saveTeams(Map<UUID, Team> teams) {
        String type = plugin.getConfig().getString("storage.type", "SQLITE").toUpperCase();
        if (type.equals("JSON")) {
            if (!teamsFolder.exists()) {
                teamsFolder.mkdirs();
            }
            for (Team team : teams.values()) {
                saveTeam(team);
            }
        } else {
            initDatabase();
            for (Team team : teams.values()) {
                saveTeam(team);
            }
        }
    }

    /**
     * Saves a single team.
     */
    public void saveTeam(Team team) {
        String type = plugin.getConfig().getString("storage.type", "SQLITE").toUpperCase();
        if (type.equals("JSON")) {
            if (!teamsFolder.exists()) {
                teamsFolder.mkdirs();
            }
            team.updateEchestData();
            String filename = team.getName().toLowerCase().replaceAll("[^a-zA-Z0-9_-]", "") + ".json";
            File file = new File(teamsFolder, filename);

            try (Writer writer = new FileWriter(file)) {
                gson.toJson(team, writer);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save team data for team " + team.getName() + "!", e);
            }
        } else {
            initDatabase();
            team.updateEchestData();
            String json = gson.toJson(team);
            try (Connection conn = getConnection()) {
                if (conn != null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "REPLACE INTO ourteam_teams (id, team_name, data, disbanded) VALUES (?, ?, ?, ?)")) {
                        ps.setString(1, team.getId().toString());
                        ps.setString(2, team.getName());
                        ps.setString(3, json);
                        ps.setBoolean(4, team.isDisbanded());
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save team data to SQL Database for " + team.getName() + "!", e);
            }
        }
    }

    /**
     * Flags a team as disbanded.
     */
    public void deleteTeam(Team team) {
        team.setDisbanded(true);
        String type = plugin.getConfig().getString("storage.type", "SQLITE").toUpperCase();
        if (type.equals("JSON")) {
            String filename = team.getName().toLowerCase().replaceAll("[^a-zA-Z0-9_-]", "") + ".json";
            File file = new File(teamsFolder, filename);
            if (file.exists()) {
                file.delete();
            }
        } else {
            try (Connection conn = getConnection()) {
                if (conn != null) {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ourteam_teams WHERE id = ?")) {
                        ps.setString(1, team.getId().toString());
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not delete team from SQL Database for " + team.getName() + "!", e);
            }
        }
    }

    /**
     * Loads individual player statistics from data.db / MySQL.
     */
    public Map<UUID, PlayerStats> loadPlayers() {
        String type = plugin.getConfig().getString("storage.type", "SQLITE").toUpperCase();
        Map<UUID, PlayerStats> map = new HashMap<>();
        if (type.equals("JSON")) {
            return map;
        }

        initDatabase();
        try (Connection conn = getConnection()) {
            if (conn != null) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM ourteam_players")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            UUID uuid = UUID.fromString(rs.getString("uuid"));
                            String name = rs.getString("player_name");
                            PlayerStats psObj = new PlayerStats(uuid, name);
                            psObj.setKills(rs.getInt("kills"));
                            psObj.setDeaths(rs.getInt("deaths"));
                            psObj.setBlocksPlaced(rs.getInt("blocks_placed"));
                            psObj.setBlocksBroken(rs.getInt("blocks_broken"));
                            psObj.setMobKills(rs.getInt("mob_kills"));
                            psObj.setPlaytimeMs(rs.getLong("playtime_ms"));
                            psObj.setGrindingPoints(rs.getInt("grinding_points"));
                            psObj.setLastSeen(rs.getLong("last_seen"));
                            map.put(uuid, psObj);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load players from database!", e);
        }
        return map;
    }

    /**
     * Saves individual player statistics to data.db / MySQL.
     */
    public void savePlayer(PlayerStats ps) {
        String type = plugin.getConfig().getString("storage.type", "SQLITE").toUpperCase();
        if (type.equals("JSON")) {
            return;
        }

        initDatabase();
        try (Connection conn = getConnection()) {
            if (conn != null) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "REPLACE INTO ourteam_players (uuid, player_name, kills, deaths, blocks_placed, blocks_broken, mob_kills, playtime_ms, grinding_points, last_seen) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    stmt.setString(1, ps.getUuid().toString());
                    stmt.setString(2, ps.getPlayerName());
                    stmt.setInt(3, ps.getKills());
                    stmt.setInt(4, ps.getDeaths());
                    stmt.setInt(5, ps.getBlocksPlaced());
                    stmt.setInt(6, ps.getBlocksBroken());
                    stmt.setInt(7, ps.getMobKills());
                    stmt.setLong(8, ps.getPlaytimeMs());
                    stmt.setInt(9, ps.getGrindingPoints());
                    stmt.setLong(10, ps.getLastSeen());
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player statistics for " + ps.getPlayerName() + "!", e);
        }
    }
}
