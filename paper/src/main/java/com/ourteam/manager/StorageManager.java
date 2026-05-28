package com.ourteam.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ourteam.OurTeam;
import com.ourteam.model.Team;

import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles saving and loading team data using individual JSON files or SQLite/MySQL databases.
 */
public class StorageManager {
    private final OurTeam plugin;
    private final File teamsFolder;
    private final File legacyDataFile;
    private final Gson gson;

    public StorageManager(OurTeam plugin) {
        this.plugin = plugin;
        this.teamsFolder = new File(plugin.getDataFolder(), "teams");
        this.legacyDataFile = new File(plugin.getDataFolder(), "teams.json");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    private Connection getConnection() throws SQLException {
        String type = plugin.getConfig().getString("storage.type", "JSON").toUpperCase();
        if (type.equals("MYSQL")) {
            String host = plugin.getConfig().getString("storage.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("storage.mysql.port", 3306);
            String db = plugin.getConfig().getString("storage.mysql.database", "ourteam_db");
            String user = plugin.getConfig().getString("storage.mysql.username", "administrator");
            String pass = plugin.getConfig().getString("storage.mysql.password", "secure_password_here");
            boolean useSSL = plugin.getConfig().getBoolean("storage.mysql.use-ssl", true);
            String url = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=" + useSSL + "&allowPublicKeyRetrieval=true&autoReconnect=true";
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                try {
                    Class.forName("com.mysql.jdbc.Driver");
                } catch (ClassNotFoundException ignored) {}
            }
            return DriverManager.getConnection(url, user, pass);
        } else if (type.equals("SQLITE")) {
            File dbFile = new File(plugin.getDataFolder(), "ourteam.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException ignored) {}
            return DriverManager.getConnection(url);
        }
        return null;
    }

    private void initDatabase() {
        String type = plugin.getConfig().getString("storage.type", "JSON").toUpperCase();
        if (type.equals("JSON")) return;
        try (Connection conn = getConnection()) {
            if (conn == null) return;
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS ourteam_teams (" +
                        "id VARCHAR(36) PRIMARY KEY, " +
                        "team_name VARCHAR(64) UNIQUE, " +
                        "data LONGTEXT, " +
                        "disbanded BOOLEAN DEFAULT FALSE)");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "SQLite/MySQL Database Table Initialization Failed!", e);
        }
    }

    /**
     * Loads teams by reading individual files from the "teams" folder or from the database.
     */
    public Map<UUID, Team> loadTeams() {
        String type = plugin.getConfig().getString("storage.type", "JSON").toUpperCase();
        if (type.equals("JSON")) {
            Map<UUID, Team> loadedTeams = new HashMap<>();

            // 1. Try legacy migration if teams folder doesn't exist or is empty
            if (legacyDataFile.exists() && (!teamsFolder.exists() || teamsFolder.listFiles() == null || teamsFolder.listFiles().length == 0)) {
                try (Reader reader = new FileReader(legacyDataFile)) {
                    com.google.gson.reflect.TypeToken<HashMap<UUID, Team>> typeToken = new com.google.gson.reflect.TypeToken<HashMap<UUID, Team>>() {};
                    Map<UUID, Team> legacyTeams = gson.fromJson(reader, typeToken.getType());
                    if (legacyTeams != null) {
                        plugin.getLogger().info("Migrating legacy teams.json to individual team files...");
                        saveTeams(legacyTeams);
                        legacyDataFile.delete(); // Delete old file
                        return legacyTeams;
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not migrate legacy teams.json data file!", e);
                }
            }

            // 2. Load from the "teams" directory
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
                        // Update enderchest values from stored string if any
                        team.getEchest(); 
                        loadedTeams.put(team.getId(), team);
                    }
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not load team file: " + file.getName(), e);
                }
            }

            return loadedTeams;
        } else {
            // Database-based storage loading
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
                                    team.getEchest(); // initialize echest/transit values
                                    loadedTeams.put(team.getId(), team);
                                }
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not load teams from SQL Database!", e);
            }
            return loadedTeams;
        }
    }

    /**
     * Saves all teams back to individual files or database.
     */
    public void saveTeams(Map<UUID, Team> teams) {
        String type = plugin.getConfig().getString("storage.type", "JSON").toUpperCase();
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
        String type = plugin.getConfig().getString("storage.type", "JSON").toUpperCase();
        if (type.equals("JSON")) {
            if (!teamsFolder.exists()) {
                teamsFolder.mkdirs();
            }

            // Ensure echest is synchronized into string data before saving
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
                    // Use REPLACE INTO which works seamlessly for both SQLite and MySQL/MariaDB
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
        saveTeam(team);
        String type = plugin.getConfig().getString("storage.type", "JSON").toUpperCase();
        if (!type.equals("JSON")) {
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
}
