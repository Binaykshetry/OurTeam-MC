package com.ourteam.manager;

import com.ourteam.OurTeam;
import com.ourteam.model.Team;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Manages runtime teams, player mappings, invites, and persistence triggers.
 */
public class TeamManager {
    private final OurTeam plugin;
    private final StorageManager storageManager;
    private final Map<UUID, Team> teams; // Team ID -> Team
    private final Map<UUID, UUID> playerTeamMap; // Player UUID -> Team ID
    private final Set<UUID> teamChatToggle; // Players toggled into team-chat channel

    public TeamManager(OurTeam plugin) {
        this.plugin = plugin;
        this.storageManager = new StorageManager(plugin);
        this.teams = storageManager.loadTeams();
        this.playerTeamMap = new HashMap<>();
        this.teamChatToggle = new HashSet<>();
        rebuildPlayerMapping();
        recalculateAllScores();
    }

    public void recalculateAllScores() {
        for (Team team : teams.values()) {
            team.recalculateScore(plugin);
        }
    }

    private void rebuildPlayerMapping() {
        playerTeamMap.clear();
        for (Team team : teams.values()) {
            for (UUID memberId : team.getMembers()) {
                playerTeamMap.put(memberId, team.getId());
            }
        }
    }

    public void saveAll() {
        recalculateAllScores();
        storageManager.saveTeams(teams);
    }

    public Team createTeam(String name, Player owner) {
        Team team = new Team(name, owner.getUniqueId());
        teams.put(team.getId(), team);
        playerTeamMap.put(owner.getUniqueId(), team.getId());
        saveAll();
        return team;
    }

    public synchronized void disbandTeam(Team team) {
        for (UUID member : team.getMembers()) {
            playerTeamMap.remove(member);
            teamChatToggle.remove(member);
        }
        
        // Comprehensive wipe to remove all existence of money, score, stats, homes, and alliances
        team.setBankBalance(0);
        team.setKills(0);
        team.setDeaths(0);
        team.setGrindingPoints(0);
        if (team.getMemberDeposits() != null) {
            team.getMemberDeposits().clear();
        }
        if (team.getRoles() != null) {
            team.getRoles().clear();
        }
        if (team.getInvites() != null) {
            team.getInvites().clear();
        }
        if (team.getRequests() != null) {
            team.getRequests().clear();
        }
        if (team.getMultiHomes() != null) {
            team.getMultiHomes().clear();
        }
        if (team.getMultiWarps() != null) {
            team.getMultiWarps().clear();
        }

        teams.remove(team.getId());
        storageManager.deleteTeam(team);
        saveAll();
    }

    public Team getTeam(UUID teamId) {
        return teams.get(teamId);
    }

    public Team getTeamByName(String name) {
        for (Team team : teams.values()) {
            if (team.getName().equalsIgnoreCase(name)) {
                return team;
            }
        }
        return null;
    }

    public Team getPlayerTeam(UUID playerUuid) {
        UUID teamId = playerTeamMap.get(playerUuid);
        return teamId != null ? teams.get(teamId) : null;
    }

    public Collection<Team> getAllTeams() {
        List<Team> activeTeams = new ArrayList<>();
        for (Team team : teams.values()) {
            if (!team.isSystemLocked()) {
                activeTeams.add(team);
            }
        }
        return activeTeams;
    }

    public Map<UUID, Team> getTeams() {
        return teams;
    }

    public void addPlayerToTeam(Player player, Team team) {
        team.addMember(player.getUniqueId());
        playerTeamMap.put(player.getUniqueId(), team.getId());
        team.removeInvite(player.getUniqueId());
        saveAll();
    }

    public void removePlayerFromTeam(Player player, Team team) {
        team.removeMember(player.getUniqueId());
        playerTeamMap.remove(player.getUniqueId());
        teamChatToggle.remove(player.getUniqueId());
        
        if (team.getMembers().isEmpty()) {
            teams.remove(team.getId());
            storageManager.deleteTeam(team);
        } else {
            if (team.getOwner().equals(player.getUniqueId())) {
                // Pick a new random owner if owner is leaving
                UUID nextOwner = team.getMembers().iterator().next();
                team.setOwner(nextOwner);
            }
            storageManager.saveTeam(team);
        }
        saveAll();
    }

    public void saveTeam(Team team) {
        team.recalculateScore(plugin);
        storageManager.saveTeam(team);
    }

    public boolean isTeamChatToggled(UUID playerUuid) {
        return teamChatToggle.contains(playerUuid);
    }

    public void toggleTeamChat(UUID playerUuid) {
        if (teamChatToggle.contains(playerUuid)) {
            teamChatToggle.remove(playerUuid);
        } else {
            teamChatToggle.add(playerUuid);
        }
    }

    private final Map<UUID, TransactionLock> teamLocks = new java.util.concurrent.ConcurrentHashMap<>();

    public static class TransactionLock {
        private final java.util.concurrent.locks.ReentrantLock lock = new java.util.concurrent.locks.ReentrantLock();

        public boolean tryLock() {
            return lock.tryLock();
        }

        public void unlock() {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public TransactionLock getTeamLock(UUID teamId) {
        return teamLocks.computeIfAbsent(teamId, k -> new TransactionLock());
    }

    /**
     * Attempts to deposit money into the team's bank.
     * Uses a TransactionLock to prevent concurrent modifiers.
     */
    public boolean depositToBank(Player player, Team team, double amount) {
        TransactionLock lock = getTeamLock(team.getId());
        if (!lock.tryLock()) {
            player.sendMessage(plugin.colorize("&c[Anti-Dupe] &7A transaction is already in progress for your team. This event has been canceled."));
            return false;
        }
        try {
            if (plugin.getEconomy() == null) {
                player.sendMessage(plugin.colorize("&cError: Economy plugin is currently unavailable."));
                return false;
            }
            double walletBalance = plugin.getEconomy().getBalance(player);
            if (walletBalance < amount) {
                player.sendMessage(plugin.colorize("&cError: You only have $" + String.format("%,.2f", walletBalance) + " on hand. You need $" + String.format("%,.2f", amount) + " to deposit."));
                return false;
            }
            if (plugin.withdrawMoney(player, amount)) {
                team.addBankBalance(amount);
                team.addMemberDeposit(player.getUniqueId(), amount);
                team.addTransaction(player.getName(), player.getUniqueId(), "DEPOSIT", amount);
                saveTeam(team);
                player.sendMessage(plugin.colorize("&#00FF99&l[Bank] &fDeposited &#FFCC00$" + String.format("%,.2f", amount) + " &fto team bank from your account!"));
                return true;
            } else {
                player.sendMessage(plugin.colorize("&cError: Deposit failed! Transaction could not be completed."));
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Attempts to withdraw money from the team's bank.
     * Uses a TransactionLock to prevent concurrent modifiers.
     */
    public boolean withdrawFromBank(Player player, Team team, double amount) {
        TransactionLock lock = getTeamLock(team.getId());
        if (!lock.tryLock()) {
            player.sendMessage(plugin.colorize("&c[Anti-Dupe] &7A transaction is already in progress for your team. This event has been canceled."));
            return false;
        }
        try {
            if (plugin.getEconomy() == null) {
                player.sendMessage(plugin.colorize("&cError: Economy plugin is currently unavailable."));
                return false;
            }
            if (team.getBankBalance() < amount) {
                player.sendMessage(plugin.colorize("&cError: Your team bank only holds $" + String.format("%,.2f", team.getBankBalance()) + ". Cannot withdraw $" + String.format("%,.2f", amount) + "."));
                return false;
            }
            if (plugin.depositMoney(player, amount)) {
                team.removeBankBalance(amount);
                team.addTransaction(player.getName(), player.getUniqueId(), "WITHDRAW", amount);
                saveTeam(team);
                player.sendMessage(plugin.colorize("&#FF3366&l[Bank] &fWithdrew &#FFCC00$" + String.format("%,.2f", amount) + " &ffrom team bank to your wallet."));
                return true;
            } else {
                player.sendMessage(plugin.colorize("&cError: Withdrawal failed! Transaction could not be completed."));
                return false;
            }
        } finally {
            lock.unlock();
        }
    }
}
