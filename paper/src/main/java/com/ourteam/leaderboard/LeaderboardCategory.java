package com.ourteam.leaderboard;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single configurable leaderboard category (e.g. Money, Kills, Teams, Mined).
 */
public class LeaderboardCategory {

    private final String id;
    private String name;
    private Material icon;
    private int slot;
    private String title;
    private String description;
    private String valueLabel;
    private String type; // "PLAYER" or "TEAM"
    private String internalStat;
    private String ajlbNamePlaceholder;
    private String ajlbValuePlaceholder;
    private int maxPages;
    private String entryTitle;
    private List<String> entryLore;
    private List<String> mainLore;

    public LeaderboardCategory(String id) {
        this.id = id;
        this.name = "&e" + id.toUpperCase();
        this.icon = Material.PAPER;
        this.slot = -1;
        this.title = id.substring(0, 1).toUpperCase() + id.substring(1) + " | Page {page}/{max_page}";
        this.description = id;
        this.valueLabel = id;
        this.type = "PLAYER";
        this.internalStat = id;
        this.ajlbNamePlaceholder = "";
        this.ajlbValuePlaceholder = "";
        this.maxPages = 7;
        this.entryTitle = "&a#{pos} {name}";
        this.entryLore = new ArrayList<>();
        this.mainLore = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Material getIcon() {
        return icon;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getValueLabel() {
        return valueLabel;
    }

    public void setValueLabel(String valueLabel) {
        this.valueLabel = valueLabel;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getInternalStat() {
        return internalStat;
    }

    public void setInternalStat(String internalStat) {
        this.internalStat = internalStat;
    }

    public String getAjlbNamePlaceholder() {
        return ajlbNamePlaceholder;
    }

    public void setAjlbNamePlaceholder(String ajlbNamePlaceholder) {
        this.ajlbNamePlaceholder = ajlbNamePlaceholder;
    }

    public String getAjlbValuePlaceholder() {
        return ajlbValuePlaceholder;
    }

    public void setAjlbValuePlaceholder(String ajlbValuePlaceholder) {
        this.ajlbValuePlaceholder = ajlbValuePlaceholder;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = Math.max(1, maxPages);
    }

    public String getEntryTitle() {
        return entryTitle;
    }

    public void setEntryTitle(String entryTitle) {
        this.entryTitle = entryTitle;
    }

    public List<String> getEntryLore() {
        return entryLore;
    }

    public void setEntryLore(List<String> entryLore) {
        this.entryLore = entryLore != null ? entryLore : new ArrayList<>();
    }

    public List<String> getMainLore() {
        return mainLore;
    }

    public void setMainLore(List<String> mainLore) {
        this.mainLore = mainLore != null ? mainLore : new ArrayList<>();
    }
}
