package com.ourteam.manager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;

/**
 * Utility to serialize and deserialize Bukkit Inventories to and from Base64 strings.
 */
public final class InventorySerializer {

    private InventorySerializer() {
        // Private constructor to prevent instantiation
    }

    /**
     * Serializes an Inventory's contents to a Base64 string.
     */
    public static String toBase64(Inventory inventory) {
        if (inventory == null) {
            return "";
        }
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.set("size", inventory.getSize());
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item != null) {
                    config.set("items." + i, item);
                }
            }
            String yamlString = config.saveToString();
            return Base64.getEncoder().encodeToString(yamlString.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not serialize team enderchest inventory!", e);
            return "";
        }
    }

    /**
     * Deserializes a Base64 string back into a Bukkit Inventory.
     */
    public static Inventory fromBase64(String data, String title) {
        if (data == null || data.isEmpty()) {
            return Bukkit.createInventory(null, 27, title);
        }
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(data);
            String yamlString = new String(decodedBytes, StandardCharsets.UTF_8);
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yamlString);

            int size = config.getInt("size", 27);
            Inventory inventory = Bukkit.createInventory(null, size, title);

            if (config.isConfigurationSection("items")) {
                ConfigurationSection section = config.getConfigurationSection("items");
                for (String key : section.getKeys(false)) {
                    int slot = Integer.parseInt(key);
                    ItemStack item = section.getItemStack(key);
                    if (slot >= 0 && slot < size) {
                        inventory.setItem(slot, item);
                    }
                }
            }
            return inventory;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not deserialize team enderchest inventory! Creating empty one.", e);
            return Bukkit.createInventory(null, 27, title);
        }
    }
}
