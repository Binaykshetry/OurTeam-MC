package com.ourteam.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * A custom InventoryHolder used to identify OurTeam GUI screens securely when handling click activities.
 */
public class TeamGUIHolder implements InventoryHolder {

    private final String menuType;
    private final String teamName;
    private int page = 1;
    private String extraData = "";

    public TeamGUIHolder(String menuType, String teamName) {
        this(menuType, teamName, 1, "");
    }

    public TeamGUIHolder(String menuType, String teamName, int page) {
        this(menuType, teamName, page, "");
    }

    public TeamGUIHolder(String menuType, String teamName, int page, String extraData) {
        this.menuType = menuType;
        this.teamName = teamName;
        this.page = page;
        this.extraData = extraData == null ? "" : extraData;
    }

    public String getMenuType() {
        return menuType;
    }

    public String getTeamName() {
        return teamName;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public String getExtraData() {
        return extraData;
    }

    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

    @Override
    public Inventory getInventory() {
        return null; // Implemented dynamically by Bukkit.createInventory
    }
}
