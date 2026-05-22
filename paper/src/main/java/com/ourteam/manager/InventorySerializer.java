package com.ourteam.manager;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            
            // Write the size of the inventory
            dataOutput.writeInt(inventory.getSize());
            
            // Write each item stack
            for (int i = 0; i < inventory.getSize(); i++) {
                dataOutput.writeObject(inventory.getItem(i));
            }
            
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
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
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            
            int size = dataInput.readInt();
            Inventory inventory = Bukkit.createInventory(null, size, title);
            
            // Read each item stack
            for (int i = 0; i < size; i++) {
                inventory.setItem(i, (ItemStack) dataInput.readObject());
            }
            
            dataInput.close();
            return inventory;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not deserialize team enderchest inventory! Creating empty one.", e);
            return Bukkit.createInventory(null, 27, title);
        }
    }
}
