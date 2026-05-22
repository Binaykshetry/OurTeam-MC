package com.ourteam.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * A custom InventoryHolder used to identify OurTeam GUI screens securely when handling click activities.
 */
public class TeamGUIHolder implements InventoryHolder {

    private final String menuType;
    private final String teamName;

    public TeamGUIHolder(String menuType, String teamName) {
        this.menuType = menuType;
        this.teamName = teamName;
    }

    public String getMenuType() {
        return menuType;
    }

    public String getTeamName() {
        return teamName;
    }

    @Override
    public Inventory getInventory() {
        return null; // Implemented dynamically by Bukkit.createInventory
    }
}
