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
    private Set<UUID> invites; // non-final so we can re-initialize on GSON Load
    private Set<UUID> requests; // non-final so we can re-initialize on GSON Load
    private java.util.Map<String, Double> memberDeposits; // persistent map for member contributions (donations)
    private boolean friendlyFire;
    private String description;
    private boolean disbanded;
    private java.util.Map<String, String> roles;
    private java.util.Map<String, TeamHome> multiHomes;
    private java.util.Map<String, TeamHome> multiWarps;
    private java.util.Map<String, MemberStats> memberStatsMap;
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
    private boolean payToggle;
    private boolean openJoin;
    private boolean teamChatDisabled;
    private boolean memberInviteDisabled;
    private boolean loginAlertsDisabled;

    public Team(String name, UUID owner) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.owner = owner;
        this.members = new HashSet<>();
        this.members.add(owner);
        this.invites = new HashSet<>();
        this.requests = new HashSet<>();
        this.memberDeposits = new java.util.HashMap<>();
        this.memberStatsMap = new java.util.HashMap<>();
        this.memberStatsMap.put(owner.toString(), new MemberStats());
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
        this.multiWarps = new java.util.HashMap<>();
        this.bankBalance = 0.0;
        this.systemLocked = false;
        this.lockReason = "";
        this.pvpForceOverride = "NONE";
        this.lastActiveTime = System.currentTimeMillis();
        this.echestLocked = false;
        this.payToggle = true;
        this.openJoin = false;
        this.teamChatDisabled = false;
        this.memberInviteDisabled = false;
        this.loginAlertsDisabled = false;
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
        getMemberStatsMap().put(uuid.toString(), new MemberStats());
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        getMemberStatsMap().remove(uuid.toString());
    }

    public Set<UUID> getInvites() {
        if (invites == null) {
            invites = new HashSet<>();
        }
        return invites;
    }

    public void invitePlayer(UUID uuid) {
        getInvites().add(uuid);
    }

    public boolean hasInvite(UUID uuid) {
        return getInvites().contains(uuid);
    }

    public void removeInvite(UUID uuid) {
        getInvites().remove(uuid);
    }

    public Set<UUID> getRequests() {
        if (requests == null) {
            requests = new java.util.HashSet<>();
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

    public java.util.Map<String, Double> getMemberDeposits() {
        if (memberDeposits == null) {
            memberDeposits = new java.util.HashMap<>();
        }
        return memberDeposits;
    }

    public double getMemberDeposits(UUID uuid) {
        if (memberDeposits == null) {
            memberDeposits = new java.util.HashMap<>();
        }
        return memberDeposits.getOrDefault(uuid.toString(), 0.0);
    }

    public void addMemberDeposit(UUID uuid, double amount) {
        if (memberDeposits == null) {
            memberDeposits = new java.util.HashMap<>();
        }
        memberDeposits.put(uuid.toString(), getMemberDeposits(uuid) + amount);
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

    public java.util.Map<String, TeamHome> getMultiWarps() {
        if (multiWarps == null) {
            multiWarps = new java.util.HashMap<>();
        }
        return multiWarps;
    }

    public boolean hasWarp(String warpName) {
        return getMultiWarps().containsKey(warpName.toLowerCase());
    }

    public void setWarp(String warpName, String world, double x, double y, double z, float yaw, float pitch) {
        getMultiWarps().put(warpName.toLowerCase(), new TeamHome(world, x, y, z, yaw, pitch));
    }

    public TeamHome getWarp(String warpName) {
        return getMultiWarps().get(warpName.toLowerCase());
    }

    public boolean deleteWarp(String warpName) {
        return getMultiWarps().remove(warpName.toLowerCase()) != null;
    }

    public double getHomeX() { return homeX; }
    public double getHomeY() { return homeY; }
    public double getHomeZ() { return homeZ; }
    public String getHomeWorld() { return homeWorld; }
    public float getHomeYaw() { return homeYaw; }
    public float getHomePitch() { return homePitch; }

    public int getKills() {
        int total = 0;
        for (UUID m : getMembers()) {
            MemberStats ms = getMemberStatsMap().get(m.toString());
            if (ms != null) {
                total += ms.getKills();
            }
        }
        return total;
    }
    public void addKill() {
        MemberStats ms = getMemberStatsMap().computeIfAbsent(owner.toString(), k -> new MemberStats());
        ms.addKill();
    }
    public void setKills(int kills) {
        MemberStats ms = getMemberStatsMap().computeIfAbsent(owner.toString(), k -> new MemberStats());
        ms.setKills(kills);
    }
    
    public int getDeaths() {
        int total = 0;
        for (UUID m : getMembers()) {
            MemberStats ms = getMemberStatsMap().get(m.toString());
            if (ms != null) {
                total += ms.getDeaths();
            }
        }
        return total;
    }
    public void addDeath() {
        MemberStats ms = getMemberStatsMap().computeIfAbsent(owner.toString(), k -> new MemberStats());
        ms.addDeath();
    }
    public void setDeaths(int deaths) {
        MemberStats ms = getMemberStatsMap().computeIfAbsent(owner.toString(), k -> new MemberStats());
        ms.setDeaths(deaths);
    }

    public int getGrindingPoints() {
        int total = 0;
        for (UUID m : getMembers()) {
            MemberStats ms = getMemberStatsMap().get(m.toString());
            if (ms != null) {
                total += ms.getGrindingPoints();
            }
        }
        return total;
    }

    public void setGrindingPoints(int grindingPoints) {
        MemberStats ms = getMemberStatsMap().computeIfAbsent(owner.toString(), k -> new MemberStats());
        ms.setGrindingPoints(grindingPoints);
    }

    public void addGrindingPoints(int points) {
        MemberStats ms = getMemberStatsMap().computeIfAbsent(owner.toString(), k -> new MemberStats());
        ms.setGrindingPoints(Math.max(0, ms.getGrindingPoints() + points));
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
        return r.toUpperCase();
    }

    public boolean isAdminOrHigher(UUID uuid) {
        String r = getRole(uuid);
        return "ADMIN".equalsIgnoreCase(r) || "OWNER".equalsIgnoreCase(r);
    }

    public boolean isModeratorOrHigher(UUID uuid) {
        String r = getRole(uuid);
        return "ADMIN".equalsIgnoreCase(r) || "OWNER".equalsIgnoreCase(r) || "MODERATOR".equalsIgnoreCase(r);
    }

    public boolean promote(UUID uuid) {
        String role = getRole(uuid);
        if ("MEMBER".equalsIgnoreCase(role)) {
            getRoles().put(uuid.toString(), "MODERATOR");
            return true;
        } else if ("MODERATOR".equalsIgnoreCase(role)) {
            getRoles().put(uuid.toString(), "ADMIN");
            return true;
        }
        return false;
    }

    public boolean demote(UUID uuid) {
        String role = getRole(uuid);
        if ("ADMIN".equalsIgnoreCase(role)) {
            getRoles().put(uuid.toString(), "MODERATOR");
            return true;
        } else if ("MODERATOR".equalsIgnoreCase(role)) {
            getRoles().remove(uuid.toString());
            return true;
        }
        return false;
    }

    public boolean isPayToggle() {
        return payToggle;
    }

    public void setPayToggle(boolean payToggle) {
        this.payToggle = payToggle;
    }

    public boolean isOpenJoin() {
        return openJoin;
    }

    public void setOpenJoin(boolean openJoin) {
        this.openJoin = openJoin;
    }

    public boolean isTeamChatEnabled() {
        return !teamChatDisabled;
    }

    public void setTeamChatEnabled(boolean enabled) {
        this.teamChatDisabled = !enabled;
    }

    public boolean isMemberInviteEnabled() {
        return !memberInviteDisabled;
    }

    public void setMemberInviteEnabled(boolean enabled) {
        this.memberInviteDisabled = !enabled;
    }

    public boolean isLoginAlertsEnabled() {
        return !loginAlertsDisabled;
    }

    public void setLoginAlertsEnabled(boolean enabled) {
        this.loginAlertsDisabled = !enabled;
    }

    public int getRankPosition(OurTeam plugin) {
        java.util.List<Team> sorted = new java.util.ArrayList<>(plugin.getTeamManager().getAllTeams());
        for (Team t : sorted) {
            t.recalculateScore(plugin);
        }
        sorted.sort((t1, t2) -> Integer.compare(t2.getCachedScore(), t1.getCachedScore()));
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getId().equals(this.id)) {
                return i + 1;
            }
        }
        return 1;
    }

    public java.util.Map<String, MemberStats> getMemberStatsMap() {
        if (memberStatsMap == null) {
            memberStatsMap = new java.util.HashMap<>();
        }
        for (UUID m : getMembers()) {
            if (!memberStatsMap.containsKey(m.toString())) {
                memberStatsMap.put(m.toString(), new MemberStats());
            }
        }
        return memberStatsMap;
    }

    public static class MemberStats {
        private int kills = 0;
        private int deaths = 0;
        private int grindingPoints = 0;
        private long joinTime = 0;
        private long playtimeMs = 0;

        public MemberStats() {
            this.joinTime = System.currentTimeMillis();
        }

        public int getKills() { return kills; }
        public void addKill() { this.kills++; }
        public void setKills(int kills) { this.kills = kills; }

        public int getDeaths() { return deaths; }
        public void addDeath() { this.deaths++; }
        public void setDeaths(int deaths) { this.deaths = deaths; }

        public int getGrindingPoints() { return grindingPoints; }
        public void setGrindingPoints(int grindingPoints) { this.grindingPoints = grindingPoints; }

        public long getJoinTime() { return joinTime; }
        public void setJoinTime(long joinTime) { this.joinTime = joinTime; }

        public long getPlaytimeMs() { return playtimeMs; }
        public void addPlaytimeMs(long ms) { this.playtimeMs += ms; }
        public void setPlaytimeMs(long ms) { this.playtimeMs = ms; }
    }
}
