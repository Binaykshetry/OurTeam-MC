package com.ourteam.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.ourteam.OurTeam;

/**
 * Represents a Team in the OurTeam plugin.
 */
public class Team {
    private final UUID id;
    private String name;
    private UUID owner;
    private final Set<UUID> members;
    private final transient Set<UUID> invites; // transient so it does not persist to JSON
    private final transient Set<UUID> requests; // transient so it does not persist to JSON
    private boolean friendlyFire;
    private String description;
    private boolean disbanded;
    private java.util.Map<String, String> roles;
    private java.util.Map<String, TeamHome> multiHomes;
    private double homeX;
    private double homeY;
    private double homeZ;
    private String homeWorld;
    private float homeYaw;
    private float homePitch;
    private boolean hasHome;
    private int kills;
    private int deaths;
    private int grindingPoints;
    private transient int cachedScore;
    private String echestData;
    private transient org.bukkit.inventory.Inventory echest;
    private double bankBalance;
    private boolean systemLocked;
    private String lockReason;
    private String pvpForceOverride; // "NONE", "ON", "OFF"
    private long lastActiveTime;
    private boolean echestLocked;

    public Team(String name, UUID owner) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.owner = owner;
        this.members = new HashSet<>();
        this.members.add(owner);
        this.invites = new HashSet<>();
        this.requests = new HashSet<>();
        this.friendlyFire = false;
        this.description = "A strong team in the making!";
        this.hasHome = false;
        this.kills = 0;
        this.deaths = 0;
        this.grindingPoints = 0;
        this.cachedScore = 0;
        this.echestData = "";
        this.disbanded = false;
        this.roles = new java.util.HashMap<>();
        this.roles.put(owner.toString(), "OWNER");
        this.multiHomes = new java.util.HashMap<>();
        this.bankBalance = 0.0;
        this.systemLocked = false;
        this.lockReason = "";
        this.pvpForceOverride = "NONE";
        this.lastActiveTime = System.currentTimeMillis();
        this.echestLocked = false;
    }

    public boolean isSystemLocked() {
        return systemLocked;
    }

    public void setSystemLocked(boolean systemLocked) {
        this.systemLocked = systemLocked;
    }

    public String getLockReason() {
        return lockReason == null ? "" : lockReason;
    }

    public void setLockReason(String lockReason) {
        this.lockReason = lockReason;
    }

    public String getPvpForceOverride() {
        return pvpForceOverride == null ? "NONE" : pvpForceOverride;
    }

    public void setPvpForceOverride(String pvpForceOverride) {
        this.pvpForceOverride = pvpForceOverride;
    }

    public long getLastActiveTime() {
        if (lastActiveTime == 0) {
            lastActiveTime = System.currentTimeMillis();
        }
        return lastActiveTime;
    }

    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public void updateActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }

    public boolean isEchestLocked() {
        return echestLocked;
    }

    public void setEchestLocked(boolean echestLocked) {
        this.echestLocked = echestLocked;
    }

    public double getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(double bankBalance) {
        this.bankBalance = bankBalance;
    }

    public void addBankBalance(double amount) {
        this.bankBalance += amount;
    }

    public boolean removeBankBalance(double amount) {
        if (this.bankBalance >= amount) {
            this.bankBalance -= amount;
            return true;
        }
        return false;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        addMember(owner);
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public Set<UUID> getInvites() {
        return invites;
    }

    public void invitePlayer(UUID uuid) {
        invites.add(uuid);
    }

    public boolean hasInvite(UUID uuid) {
        return invites.contains(uuid);
    }

    public void removeInvite(UUID uuid) {
        invites.remove(uuid);
    }

    public Set<UUID> getRequests() {
        if (requests == null) {
            return new java.util.HashSet<>();
        }
        return requests;
    }

    public void addRequest(UUID uuid) {
        getRequests().add(uuid);
    }

    public boolean hasRequest(UUID uuid) {
        return getRequests().contains(uuid);
    }

    public void removeRequest(UUID uuid) {
        getRequests().remove(uuid);
    }

    public boolean isFriendlyFireEnabled() {
        return friendlyFire;
    }

    public void setFriendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Multi-home support
    public static class TeamHome {
        private String world;
        private double x;
        private double y;
        private double z;
        private float yaw;
        private float pitch;

        public TeamHome() {}

        public TeamHome(String world, double x, double y, double z, float yaw, float pitch) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public String getWorld() { return world; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public float getYaw() { return yaw; }
        public float getPitch() { return pitch; }
    }

    public java.util.Map<String, TeamHome> getMultiHomes() {
        if (multiHomes == null) {
            multiHomes = new java.util.HashMap<>();
        }
        // Seamless backwards compatibility migration
        if (hasHome && !multiHomes.containsKey("home")) {
            multiHomes.put("home", new TeamHome(homeWorld, homeX, homeY, homeZ, homeYaw, homePitch));
        }
        return multiHomes;
    }

    public boolean hasHome() {
        return hasHome("home");
    }

    public boolean hasHome(String homeName) {
        return getMultiHomes().containsKey(homeName.toLowerCase());
    }

    public void setHome(String world, double x, double y, double z, float yaw, float pitch) {
        setHome("home", world, x, y, z, yaw, pitch);
    }

    public void setHome(String homeName, String world, double x, double y, double z, float yaw, float pitch) {
        String nameKey = homeName.toLowerCase();
        TeamHome home = new TeamHome(world, x, y, z, yaw, pitch);
        getMultiHomes().put(nameKey, home);

        // Keep legacy parameters updated if setting the default "home"
        if (nameKey.equals("home")) {
            this.homeWorld = world;
            this.homeX = x;
            this.homeY = y;
            this.homeZ = z;
            this.homeYaw = yaw;
            this.homePitch = pitch;
            this.hasHome = true;
        }
    }

    public TeamHome getHome(String homeName) {
        return getMultiHomes().get(homeName.toLowerCase());
    }

    public boolean deleteHome(String homeName) {
        String nameKey = homeName.toLowerCase();
        if (getMultiHomes().containsKey(nameKey)) {
            getMultiHomes().remove(nameKey);
            if (nameKey.equals("home")) {
                this.hasHome = false;
                this.homeWorld = null;
            }
            return true;
        }
        return false;
    }

    public double getHomeX() { return homeX; }
    public double getHomeY() { return homeY; }
    public double getHomeZ() { return homeZ; }
    public String getHomeWorld() { return homeWorld; }
    public float getHomeYaw() { return homeYaw; }
    public float getHomePitch() { return homePitch; }

    public int getKills() { return kills; }
    public void addKill() { this.kills++; }
    public void setKills(int kills) { this.kills = kills; }
    
    public int getDeaths() { return deaths; }
    public void addDeath() { this.deaths++; }
    public void setDeaths(int deaths) { this.deaths = deaths; }

    public int getGrindingPoints() {
        return grindingPoints;
    }

    public void setGrindingPoints(int grindingPoints) {
        this.grindingPoints = grindingPoints;
    }

    public void addGrindingPoints(int points) {
        this.grindingPoints += points;
    }

    public int getCachedScore() {
        return cachedScore;
    }

    public void recalculateScore(OurTeam plugin) {
        int total = this.grindingPoints;

        int pointsPerActiveMember = plugin.getConfig().getInt("team-score.points-per-active-member", 50);
        long activeTimeframeMs = plugin.getConfig().getLong("team-score.active-member-timeframe-hours", 24L) * 60L * 60L * 1000L;
        
        int activeMembersCount = 0;
        long now = System.currentTimeMillis();
        for (UUID memberUuid : this.members) {
            org.bukkit.entity.Player onlinePlayer = org.bukkit.Bukkit.getPlayer(memberUuid);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                activeMembersCount++;
            } else {
                org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(memberUuid);
                if (offlinePlayer != null) {
                    long lastPlayed = offlinePlayer.getLastPlayed();
                    if (now - lastPlayed <= activeTimeframeMs) {
                        activeMembersCount++;
                    }
                }
            }
        }
        total += activeMembersCount * pointsPerActiveMember;

        double bankCurrencyPerPoint = plugin.getConfig().getDouble("team-score.currency-per-point", 10000.0);
        if (bankCurrencyPerPoint > 0) {
            total += (int) (this.bankBalance / bankCurrencyPerPoint);
        }

        this.cachedScore = total;
    }

    public org.bukkit.inventory.Inventory getEchest() {
        if (echest == null) {
            if (echestData != null && !echestData.isEmpty()) {
                echest = com.ourteam.manager.InventorySerializer.fromBase64(echestData, "Team Enderchest (" + name + ")");
            } else {
                echest = org.bukkit.Bukkit.createInventory(null, 27, "Team Enderchest (" + name + ")");
            }
        }
        return echest;
    }

    public String getEchestData() {
        return echestData;
    }

    public void setEchestData(String echestData) {
        this.echestData = echestData;
        this.echest = null; // Rebuild inventory on next request
    }

    public void updateEchestData() {
        if (echest != null) {
            this.echestData = com.ourteam.manager.InventorySerializer.toBase64(echest);
        }
    }

    public boolean isDisbanded() {
        return disbanded;
    }

    public void setDisbanded(boolean disbanded) {
        this.disbanded = disbanded;
    }

    public java.util.Map<String, String> getRoles() {
        if (roles == null) {
            roles = new java.util.HashMap<>();
        }
        return roles;
    }

    public String getRole(UUID uuid) {
        if (uuid.equals(getOwner())) {
            return "OWNER";
        }
        String r = getRoles().get(uuid.toString());
        if (r == null) {
            return "MEMBER";
        }
        if ("MODERATOR".equalsIgnoreCase(r)) {
            return "ADMIN";
        }
        return r;
    }

    public boolean isAdminOrHigher(UUID uuid) {
        String r = getRole(uuid);
        return "ADMIN".equalsIgnoreCase(r) || "OWNER".equalsIgnoreCase(r) || "MODERATOR".equalsIgnoreCase(r);
    }

    public boolean isModeratorOrHigher(UUID uuid) {
        return isAdminOrHigher(uuid);
    }

    public boolean promote(UUID uuid) {
        String role = getRole(uuid);
        if ("MEMBER".equalsIgnoreCase(role)) {
            getRoles().put(uuid.toString(), "ADMIN");
            return true;
        }
        return false;
    }

    public boolean demote(UUID uuid) {
        String role = getRole(uuid);
        if ("ADMIN".equalsIgnoreCase(role) || "MODERATOR".equalsIgnoreCase(role)) {
            getRoles().put(uuid.toString(), "MEMBER");
            return true;
        }
        return false;
    }
}
