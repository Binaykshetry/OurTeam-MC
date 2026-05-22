package com.ourteam.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ourteam.OurTeam;
import com.ourteam.model.Team;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles saving and loading team data using individual JSON files in a "teams" folder.
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

    /**
     * Loads teams by reading individual files from the "teams" folder.
     */
    public Map<UUID, Team> loadTeams() {
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
    }

    /**
     * Saves all teams back to individual files in the local "teams" folder.
     */
    public void saveTeams(Map<UUID, Team> teams) {
        if (!teamsFolder.exists()) {
            teamsFolder.mkdirs();
        }

        for (Team team : teams.values()) {
            saveTeam(team);
        }
    }

    /**
     * Saves a single team to its corresponding file inside "teams" directory.
     */
    public void saveTeam(Team team) {
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
    }

    /**
     * Flags a team as disbanded in the local JSON file instead of physically deleting it.
     */
    public void deleteTeam(Team team) {
        team.setDisbanded(true);
        saveTeam(team);
    }
}
