# OurTeam — Advanced Minecraft Teams & TeamScore System

 OurTeam is a high-performance, competitive Minecraft teams plugin built for high-concurrency Paper servers. It is fully compiled with **Java 21** and provides a comprehensive suite of team roster tools, a micro-economy engine, role hierarchies, and a robust **TeamScore leaderboard calculation system** similar to the BetterTeams plugin but heavily optimized for high-TPS environments.

---

## 🚀 Key Architectural Pillars

### 1. Hardened TeamScore System
TeamScore calculates a live competitive score for teams using three distinct channels to promote actual active play over lazy alt roster-filling:

*   **Roster Member Count Scaling & Decay**: Each registered player profile awards a base point value (default: `+50` points). To eliminate alt-account roster padding, the system dynamically checks `getLastPlayed()` timestamps. Any account inactive for more than 24 hours decays to a `0` point contribution.
*   **Team Bank Investments**: Deposits of Vault-integrated economy currency directly convert into permanent/accrued team leaderboard points. By default, every `$10,000` deposited converts into `+1` TeamScore point.
*   **Active Gameplay Grinding Metrics**:
    *   **Competitive PvP Kills**: Defeating an opposing faction player injects customizable grinding points (default: `+5` points) and drains points from the victim team's pool (default: `-2` points).
    *   **Alt-Farming Protection**: Features an automated anti-spam database tracking victim/killer cooldowns (60-second sliding threshold) to ensure friends or alternate profiles cannot be repeatedly farmed for leadership amplification.
    *   **Event integration**: Supports server event handlers like King of the Hill (KOTH) to award points directly from console events via `/teamsadmin score add <team> <points>`.

### 2. High-Performance Cache Registry (20 TPS Immunity)
The scoring algorithm is divided into a fast local registry and memory-mapped models. Rather than querying databases on active visual scoreboards, chat rendering, or placeholder parsing:
*   A localized in-memory `cachedScore` integer maintains the live updated values at all times.
*   Triggers (e.g. money deposits, player kills, roster state modifications) execute localized calculations instantly.
*   This avoids thread-locking blocking calls, guaranteeing stable **20 TPS server performance** even with thousands of aggressive players.

---

## 🛠️ Configuration Specifications (`config.yml`)

The parameters of the dynamic scaling module are fully configurable under the `team-score` node:

```yaml
# =============================================================================
# Advanced Team Score System Configurations
# =============================================================================
team-score:
  # Foundational score granted for each active player profile registered in the team roster.
  points-per-active-member: 50
  
  # Time (in hours) since the member's last network login before they are flagged inactive.
  # Inactive members contribute 0 points to the team's score.
  active-member-timeframe-hours: 24
  
  # Amount of Vault cash deposited in the team bank required to earn 1 permanent TeamScore point.
  # E.g., at 10000.0, every $10,000 in bank balance translates to +1 score point.
  currency-per-point: 10000.0
  
  # Points awarded to the team score when a member scores a PvP kill on an enemy team player.
  points-per-kill: 5
  
  # Points subtracted from the team score when a member suffers a PvP death by an enemy player.
  points-lost-per-death: 2
  
  # Anti-spam farming cooldown (in seconds) between sequential kills on the same victim.
  # Kills within this threshold are dynamic-spam flagged and gain 0 points.
  spam-threshold-seconds: 60
```

---

## 💻 Commands and Permissions Manager

### Player Commands
*   `/team bank` — Open the graphical/interactable team bank menu. Keep track of group funds and let currency compound with interest.
*   `/team sethome` / `/team home` — Update or teleport to your secure team base.
*   `/team chat` — Toggle the dedicated internal team channel communications.

### Admin & Console Hooks
The admin engine is bound to `/teamadmin` with system aliases to seamlessly map `/teamsadmin` console scripts for server automated events:

*   `/teamsadmin score add <team> <amount>` - Award custom-grinded score points (e.g. from KOTH wins).
*   `/teamsadmin score set <team> <amount>` - Synchronize the specified team values manually.
*   `/teamsadmin score take <team> <amount>` - Apply penalties or subtract credits.
*   `/teamsadmin resetbank <team>` - Wipes bank holdings clean.

*Permission requested for administrator actions:* `ourteam.admin`

---

## 🏗️ Compiler Requirements & Deployment

*   **Java Environment**: Target JDK **21**
*   **Build Pipeline**: Native Maven with GitHub Actions mapping (`actions/upload-artifact@v4`)
*   **Native Dependencies**: Papermc 1.21-R0.1-SNAPSHOT Core Engine, Vault Economy hooks, Gson dynamic mapping file storage.
*   **Compiling instructions**:
    ```bash
    # From the paper directory, build and bundle the plugin output:
    mvn clean package
    ```
    The compiled binary will generate in `paper/target/OurTeam-*.jar`.
