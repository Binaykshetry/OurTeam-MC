# 🏆 OurTeam | Professional & Competitive Minecraft Guilds System

OurTeam is a high-performance, enterprise-grade competitive team/faction-guilds plugin built to power high-concurrency **Paper, Purpur, and Spigot (1.20 - 1.21+)** servers. Engineered with modern architectural design, **OurTeam** balances pristine graphical GUIs, a dynamic micro-economy bank system, automated team settings, and an highly optimized, ultra-scalable in-memory score caching registry designed to hold **stable 20 TPS performance** even at extreme player configurations.

---

## ✨ Outstanding Features & Capabilities

*   **⚡ 20-TPS Immunity Cache Registry:** Roster-based checks, bank balances, and PAPI query calculations are conducted entirely in a fast, thread-safe, memory-mapped local layout to prevent database locking or main-thread bottlenecks.
*   **📂 Multi-engine Local JSON Persistence:** Thread-safe, atomic GSON structure writes team profiles and transactional rosters to direct individual files instantly upon saves, safely avoiding any data corruption or race conditions.
*   **💼 Enterprise-Grade Guild Economy (Team Bank):** Native Vault economy integration supports direct quick deposits/withdrawals or custom chat-prompted transactions. It includes automatic recurring **compound interest distribution** and precise deposit-contribution logging.
*   **🌌 Dynamic Home & Warp Navigation:** Complete with sound indicators, aesthetic portal particle trails, and standard **cooldown timers / movement-damage disruption logic** to maintain strict PvP and gameplay balancing.
*   **👁️ Visual Control Panels (GUI Engine):** Features elegant, responsive double-row and custom-grid inventories built for rapid, mouse-driven team administration.
*   **📁 Direct Interactive Team Directory:** Access all active teams in the server via `/team list` with an interactive menu. Players can browse teams and **instantly send active requests** by left/right-clicking on any team card.
*   **🛡️ Role-based Permission Hierarchy:** Four organizational ranks (Owner, Admin, Moderator, Member) manage specific command actions, controlled by a customizable `teampermissions.yml` configuration.
*   **💬 Dual-Channel Team Chat:** A dedicated private communication frequency with custom formatting and built-in setting checks that auto-reverts players to global chat if a team's moderators toggle their private channel off.

---

## 🏎️ Key Structural Columns

### 1. Hardened TeamScore™ Competition Algorithm
To eliminate passive alternate account padding and preserve actual competitive integrity on leaderboard scoreboards, **OurTeam** calculates live competitive standings using a unique, triple-axis validation check:

$$TeamScore = \sum (ActiveMemberPoints) + \frac{BankBalance}{BankScaleValue} + PvPPoints$$

*   **Roster Member Validation & Decay:** Active roster players award a base point value (default: `+50` points). The core registry continuously queries the `getLastPlayed()` attribute. Any account inert/offline for longer than **24 hours** automatically decays to a **0-point contribution**—guaranteeing that active, grinding guilds dominate the standings over dead, inflated ones.
*   **Financial Liquid Conversions:** Cash deposited inside the Team Bank translates to permanent leaderboard standing score. For example, at `10000.0` conversion, every `$10,000` accrued triggers `+1` TeamScore point.
*   **Pro-PvP Active Combat Grinding:** Defeating an enemy clan member injects instant score credits to the team pool (default: `+5`) and extracts score from the victim's pool (default: `-2`).
*   **Anti-Farming Exploitation Protection:** Uses a sliding 60-second in-memory tracker to block repeated farming of the same target, logging fraudulent kills as 0-point spam events.
*   **External Integration Events:** Dedicated admin command entries allow seamless integration with automated server events (e.g., King of the Hill wins, Dungeons, Envoy conquers) via direct console commands.

---

## 📋 Comprehensive PlaceholderAPI Expansion Guide

OurTeam hosts a complete native **PlaceholderAPI (PAPI)** expansion directory under the `%ourteam_*%` namespace. It exposes everything required to feed real-time team stats into scoreboards (e.g. FeatherBoard, TitleManager), dynamic TAB headers, chat formatting engines, or holograms.

### 1. Play-Context Placeholders (Evaluates for the Subject Player)

| Placeholder | Outputs | Example Return Value |
| :--- | :--- | :--- |
| `%ourteam_inteam%` | Custom offline check indicator | `Yes` / `No` |
| `%ourteam_name%` | Direct team alphabetical identity | `AlphaFaction` / `No Team` |
| `%ourteam_tag%` | Styled short team label | `AlphaFaction` |
| `%ourteam_displayname%` | Normalized team descriptor | `AlphaFaction` |
| `%ourteam_description%` | Custom descriptive block | `The undisputed champions!` |
| `%ourteam_score%` | Cumulative calculated score weight | `3528` |
| `%ourteam_money%` | Double-precision undivided bank account | `25482.50` |
| `%ourteam_moneyshort%` | Compact scientific numeric conversion | `25.5k` / `1.2M` |
| `%ourteam_members%` | Active count of players on team roster | `6` |
| `%ourteam_online%` | Real-time concurrent online member count | `3` |
| `%ourteam_maxmembers%` | Roster soft-cap limit based on level | `8` |
| `%ourteam_onlinelist%` | Names of all currently online teammates | `Notch, Dinnerbone` |
| `%ourteam_offlinelist%` | Custom offline listing of teammates | `Jeb_` |
| `%ourteam_level%` | Roster competitive leveling tier | `4` |
| `%ourteam_maxmoney%` | Account threshold capacity limit | `200000.00` |
| `%ourteam_maxwarps%` | Total allowed warp checkpoints | `5` |
| `%ourteam_pvp%` | Friendly fire flag status | `true` / `false` |
| `%ourteam_rank%` | Role tier of the querying player | `MODERATOR` |
| `%ourteam_title%` | Localized role title of the player | `Moderator` |
| `%ourteam_owners%` | Clan leader's real-time username | `Steve` |
| `%ourteam_admins%` | Comma-separated owners and admins | `Steve, Notch` |
| `%ourteam_defaultmembers%` | Comma-separated list of standard tier players | `Dinnerbone, Jeb_` |
| `%ourteam_positionscore%` | Top-Score ranking position on the server | `1` |
| `%ourteam_positionbal%` | Wealth ranking position on the server | `3` |
| `%ourteam_positionmembers%` | Roster-size ranking position on the server | `2` |
| `%ourteam_hashome%` | Home coordinates established state | `Yes` / `No` |
| `%ourteam_teamchat%` | Active chat toggle tracking channel | `Team` / `Global` |

### 2. Leaderboard & Universal Standings (Rank-Position Placeholders)
Allows displaying global leaderboard rankings for top teams in holograms or sidebar widgets. Simply replace `<property>` with any general field (e.g., `name`, `score`, `money`, `members`) and `<rank>` with an index (e.g., `1`, `2`, `3`):

*   **🏆 Placeholders Ordered by TeamScore Standings:**
    *   `%ourteam_position_name_<rank>%` — e.g. `%ourteam_position_name_1%` ➔ Returns `DeltaGroup`
    *   `%ourteam_position_score_<rank>%` — e.g. `%ourteam_position_score_1%` ➔ Returns `12980`
    *   `%ourteam_position_money_<rank>%` — e.g. `%ourteam_position_money_2%` ➔ Returns `50000.00`
*   **💰 Placeholders Ordered by Wealth / Bank Balance:**
    *   `%ourteam_balanceposition_name_<rank>%` — e.g. `%ourteam_balanceposition_name_1%` ➔ Returns name of wealthiest team.
    *   `%ourteam_balanceposition_money_<rank>%` — e.g. `%ourteam_balanceposition_money_1%` ➔ Returns `157942.50`
*   **👥 Placeholders Ordered by Faction Roster Sizes:**
    *   `%ourteam_membersposition_name_<rank>%` — e.g. `%ourteam_membersposition_name_1%` ➔ Returns name of largest group.
    *   `%ourteam_membersposition_members_<rank>%` — e.g. `%ourteam_membersposition_members_1%` ➔ Returns `18`

### 3. Static Multi-Target Mapping (Queries Specific Targets)
Evaluate calculations for specific remote targets irrespective of who is querying:

*   **🏡 Query by Raw Team Name:**
    *   `%ourteam_static_<property>_<teamName>%` — e.g. `%ourteam_static_score_DeltaGroup%` ➔ Returns `12980`
    *   `%ourteam_static_money_DeltaGroup%` ➔ Returns `50000.00`
*   **👤 Query by Player Account Target Name:**
    *   `%ourteam_staticplayer_<property>_<playerName>%` — e.g. `%ourteam_staticplayer_name_Notch%` ➔ Returns the name of Notch's team.
    *   `%ourteam_staticplayer_rank_Notch%` ➔ Returns Notch's position/role inside their respective team.

### 4. Global System Toggle Query Interface

*   `%ourteam_command_<feature_or_command_name>%` — Evaluates if a command action is enabled in the global configuration (returns `on`/`off`):
    *   *Examples*: `%ourteam_command_create%`, `%ourteam_command_bank%`, `%ourteam_command_teleport%`, `%ourteam_command_echest%`.

---

## 🛠️ Complete Command Directory & Permissions

### Player Command Hierarchy (Permission Node: `ourteam.use`)

| Primary Command | Alias / Subcommand | Action Description |
| :--- | :--- | :--- |
| `/team create <name>` | `c` | Registers and creates a new Team roster under your ownership. |
| `/team invite <player>` | `i` | Invites another player to join your team (Requires Invite Perms). |
| `/team join <team>` | `j` | Directly joins a team with an active invite, or OPEN JOIN teams. |
| `/team request <team>` | `req` | Sends a pending Join Request to a private team. |
| `/team leave` | `l` | Resigns your membership and leaves your current team roster. |
| `/team kick <player>` | `k` | Evicts a member from your team roster (Admin/Owner only). |
| `/team disband` | `dis` | Permanently deletes your team registry (Owner only). |
| `/team promote <player>` | `prom` | Promotes a teammate to the next higher rank status. |
| `/team demote <player>` | `dem` | Demotes a teammate to a lower rank tier. |
| `/team chat` | `c` | Toggles your chat mode between Dedicated Team-Chat and Public-Chat. |
| `/team msg <msg...>` | `m` | Direct private messaging channel to your online teammates. |
| `/team pvp` | `ff` | Toggles whether teammates can deal damage to each other. |
| `/team bank` | `b` | Opens the GUI Team Bank with interest ledger information. |
| `/team list` | `l` | Opens the ALL ACTIVE TEAMS GUI to browse teams or send requests. |
| `/team sethome [name]` | — | Establishes a home checkpoint at your current location. |
| `/team home [name]` | — | Initiates teleport sequence to the selected team home. |
| `/team delhome [name]`| — | Deletes a customized team home checkpoint. |
| `/team setwarp <name>` | — | Sets a dynamic team warp point accessible to teammates. |
| `/team warp <name>` | — | Teleports player to the specified warp checkpoint location. |
| `/team delwarp <name>` | — | Revokes and deletes a team warp checkpoint point. |
| `/team top` | — | Renders a rich list of the top active teams sorted by Score. |
| `/team gui` | `settings` | Opens the main Graphical Settings Menu for customization. |

### Administrative Commands (Permission Node: `ourteam.admin`)

| Admin Command | Alias | Action Description |
| :--- | :--- | :--- |
| `/teamsadmin r` | `reload` | Instantly reloads all configurations, rules and files. |
| `/teamsadmin score add <t> <val>` | — | Directly awards score points to a team's permanent total. |
| `/teamsadmin score take <t> <val>`| — | Penetrates/deducts score points from a team's cache. |
| `/teamsadmin score set <t> <val>` | — | Rewrites and sets a team's score parameter to a strict value. |
| `/teamsadmin resetbank <team>` | — | Clears all cash balance holdings belonging to a target team. |
| `/teamsadmin info <team>` | — | Runs deep structural audits on team storage configurations in chat. |

---

## 🎨 Professional Configuration File (`config.yml`)

Customize parameters, telemetry, banking, limits, and sounds instantly:

```yaml
# =============================================================================
#              OurTeam - Premium Guilds & Roster Ecosystem File
# =============================================================================

# Dynamic Leaderboard Standing calculations config
team-score:
  points-per-active-member: 50
  active-member-timeframe-hours: 24
  currency-per-point: 10000.0
  points-per-kill: 5
  points-lost-per-death: 2
  spam-threshold-seconds: 60

# Teleport delays, movement disruption and particle animations
cooldowns-and-teleportation:
  teleport-cooldown: 45                 # General teleport cooldown (in seconds)
  warp-warmup-seconds: 5                # Teleport delay interval (in seconds)
  cancel-on-movement: true              # Cancel teleport if player moves
  cancel-on-damage: true                # Cancel teleport if player takes damage
  teleport-particle: "PORTAL"           # Particle types spawned at warmup tick
  teleport-sound: "ENTITY_ENDERMAN_TELEPORT" # Sound played upon successful teleport
  max-homes-per-team: 3                 # Max homing locations per team
  max-warps-per-team: 5                 # Max warps per team

# Guild Banking, compound interest ledger
bank-settings:
  starting-balance: 0.0
  max-balance-cap: 10000000.0
  paytoggle-by-default: true
  interest:
    enabled: true
    rate-percent: 0.5                   # Teammates gain 0.5% interest on bank wealth
    interval-seconds: 3600              # Interest accrued and calculated every hour
    minimum-balance-required: 1000.0    # Minimum funds required to gain accrued interest
```

---

## 🛠️ GitHub-Based Packaging & Compilation Instruction

Developing and deploying **OurTeam** is automated using standard dependency tools. Follow these guidelines to compile and deploy the `.jar` files:

### Prerequisites
*   A Java SE Development Kit (**JDK 21** or newer)
*   **Apache Maven** installed on your server or local path.

### 1. Manual Compilation Steps
Build the shaded, ready-to-run binary in your command shell execution channel:

```bash
# 1. Navigate to the core plugin directory
cd paper

# 2. Package and compile shaded libraries
mvn clean package
```

The resulting optimized compilation file will be generated in `/paper/target/OurTeam-1.0.0.jar`. Copy this file directly into your server's `/plugins` directory.

### 2. Fully Automated GitHub Actions CI/CD Pipeline
An automated pipeline configuration file is included at **`.github/workflows/build-plugin.yml`**. It automates building and uploading the artifact upon every code update:

*   **Platform Target:** Standard headless Ubuntu runner (`ubuntu-latest`).
*   **CI Setup:** Loads **Temurin JDK 21**, configures optimal Maven build caches to accelerate dependency checkups, clean packages the resources, and exports a production-ready `.jar` archive download.

---

## 📄 Licensing & Distribution Requirements

OurTeam is licensed and distributed under the permissive **MIT License**. Check the full license description in the [LICENSE](./LICENSE) file contained within the repository. Contributions, forks, and integrations are highly encouraged!

---

*Crafted for premium Minecraft communities by the OurTeam Dev Group.*
