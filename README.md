# 🏆 OurTeam | Advanced Minecraft Teams, Guilds & Dynamic Leaderboards

**OurTeam** is an all-in-one, enterprise-grade Teams, Guilds, and Leaderboards plugin engineered for high-concurrency **Paper, Purpur, and Spigot (1.20 - 1.21+)** servers. 

Designed with modern aesthetic GUIs, native **SQLite (`data.db`) & MySQL** persistence, full **PlaceholderAPI (`%ourteam_*%` & `%nteam_*%`)** expansions, and a standalone **Leaderboard GUI system (`/leaderboard`, `/lb`)** with dual **ajLeaderboards (`ajlb`)** and built-in database support.

---

## 🌟 Key Highlights & Features

- ⚡ **20 TPS Immunity Architecture:** Roster queries, bank accounts, and leaderboard ranking calculations are backed by a high-speed memory cache to guarantee zero main-thread lag.
- 💾 **Dual Database Storage Engine (`data.db` & MySQL):**
  - **SQLite by Default:** All team and player statistics are persistently stored in `plugins/OurTeam/data.db`.
  - **MySQL Support:** Built-in connection pooling for multi-server networks and BungeeCord/Velocity setups.
  - **Auto-Migration:** Automatically converts legacy `teams.json` or `teams/` directory files to database tables on launch.
- 🎨 **Modern Visual GUI Engine:** Custom BananaSMP-style menus for discovery, creation confirmation, team dashboard, member management, statistics, and alliances.
- 📊 **Dynamic Leaderboard System (`/leaderboard` or `/lb`):**
  - Native replacement for DeluxeMenus with multi-page GUI navigation.
  - Main menu (36 slots) featuring custom category items and descriptions.
  - Category pages (54 slots) showing 28 player heads per page with real player skins, formatted numbers (`35.1b`, `12.5m`, `800k`), previous/next page controls, and live refresh.
  - **Dual-Mode Data Resolution:** Evaluates `ajLeaderboards` (`ajlb`) placeholders dynamically, with seamless automatic fallback to OurTeam's native database.
  - **100% Configurable:** Add unlimited new leaderboards, edit items, slots, titles, and lores in `LeaderboardGUI/leaderboards.yml`.
- 🧩 **Comprehensive PlaceholderAPI Support:**
  - Fully registered under both `%ourteam_*%` and `%nteam_*%`.
  - Team leaderboards, player leaderboards, personal leaderboard ranks, and individual/team stats.
- 🏦 **Team Economy & Bank:** Vault-backed banking with compound interest, customizable deposit/withdraw interfaces, and passive hourly earnings.
- ⚔️ **PvP & Battle Protection:** Friendly fire toggles, ally damage reduction, TNT protection, and splash potion safety.
- 🌌 **Teleportation System:** Named team warps and home checkpoints with particle trails, warmup delays, and combat-damage cancellation.
- 📦 **Shared Virtual Enderchest:** Multi-tier team Enderchest with customizable sizes (up to 54 slots) and blacklist protection.

---

## 🖥️ Interactive GUI System Overview

### 1. No-Team Discovery Hub (`/team` when without a team)
| Slot | Item | Action |
| :---: | :--- | :--- |
| **12** | **Chest** (`TEAM INVITES`) | Opens received team invitations |
| **14** | **Lime Bed** (`CREATE TEAM`) | Opens the team creation confirmation dialog |

### 2. Create Team Confirmation Dialog
- **Layout:** 1 row (9 slots).
- **Slots 0-3:** Red Stained Glass Pane (`CANCEL`) &rarr; returns to Hub.
- **Slot 4:** Lime Stained Glass Pane (`CONFIRM`) &rarr; prompts in chat: `TEAMS » Write the team tag in the chat (Max Length 12)`.
- **Slots 5-8:** Red Stained Glass Pane (`CANCEL`).

### 3. Main Team Dashboard (`/team`)
| Slot | Item | Function |
| :---: | :--- | :--- |
| **4** | **Red Bed** | `TEAM INVITES` - Manage pending requests & outgoing invites |
| **10** | **Chest** | `TEAM BANK` - Deposit/withdraw funds and view balance |
| **11** | **Clock** | `TEAM STATS` - View team statistics & member breakdown |
| **12** | **Orange Bed** | `TEAM HOME` - Teleport to team home spawn |
| **13** | **Ender Chest** | `TEAM ENDERCHEST` - Shared virtual team inventory |
| **14** | **Book** | `TEAM MEMBERS` - Roster management & rank controls |
| **15** | **Bell** | `TEAM SETTINGS` - Friendly fire, privacy, and team toggles |
| **16** | **Iron Sword** | `TEAM ALLIES` - Diplomatic relations & alliance management |

### 4. Team Statistics Overview (`Teams | Statistics`)
5-row menu displaying core team metrics:
- **Slot 10:** Lectern (`LEVELS`)
- **Slot 12:** Gold Ingot (`MONEY`)
- **Slot 14:** Clock (`PLAYTIME`)
- **Slot 16:** Diamond Sword (`KILLS`)
- **Slot 28:** Diamond Helmet (`DEATHS`)
- **Slot 30:** Wooden Sword (`KDR`)
- **Slot 32:** Dirt (`PLACED BLOCKS`)
- **Slot 34:** Diamond Pickaxe (`BROKEN BLOCKS`)
- **Slot 40:** Barrier (`BACK`)
- Clicking any category opens the **Member Contribution Detail GUI** displaying each member's skull with exact stats (`This player has placed X blocks.`, etc.).

---

## 📈 Leaderboard System (`/leaderboard`, `/lb`)

Access global rankings anytime using `/leaderboard`, `/lb`, or `/leaderboards`.

### Main Leaderboard Menu (36 slots)
- **Slot 11:** `EMERALD` &rarr; **MOST MONEY** (`/lb money`)
- **Slot 12:** `PAPER` &rarr; **MOST VOTES** (`/lb votes`)
- **Slot 13:** `DIAMOND_PICKAXE` &rarr; **BLOCKS MINED** (`/lb mined`)
- **Slot 14:** `DIAMOND_HELMET` &rarr; **KD RATIO** (`/lb kd`)
- **Slot 15:** `CLOCK` &rarr; **PLAYTIME** (`/lb playtime`)
- **Slot 20:** `AMETHYST_SHARD` &rarr; **SHARDS** (`/lb shards`)
- **Slot 21:** `COMPASS` &rarr; **DISTANCE TRAVELLED** (`/lb distance`)
- **Slot 22:** `NETHERITE_HELMET` &rarr; **MOST DEATHS** (`/lb deaths`)
- **Slot 23:** `MILK_BUCKET` &rarr; **MOST COINS** (`/lb coins`)
- **Slot 24:** `NETHERITE_SWORD` &rarr; **MOST KILLS** (`/lb kills`)
- **Slot 4:** `WHITE_BANNER` &rarr; **TOP TEAMS** (`/lb teams`)
- *Also includes `placed`, `mobkills`, and `killstreaks`.*

### Category Ranking Page (54 slots)
- **28 Entries Per Page:** Slots `10-16`, `19-25`, `28-34`, `37-43`.
- **Player Heads:** Displays player skulls with custom lore:
  ```
  &a#1 PlayerName
  &7Leaderboard

  &7Position: #1
  &7Money: &a35.1b
  ```
- **Controls:**
  - **Slot 48:** `RED_STAINED_GLASS_PANE` &rarr; `PREVIOUS PAGE`
  - **Slot 49:** `BELL` &rarr; `REFRESH` (Live refresh current page)
  - **Slot 50:** `LIME_STAINED_GLASS_PANE` &rarr; `NEXT PAGE`
  - **Slot 45:** `ARROW` &rarr; `◀ BACK` (Return to main leaderboards)

---

## 🗄️ Database & Storage Configuration

Data storage is configured in [`config.yml`](file:///c:/Users/barsha/Desktop/ourteam-plugin-studio/paper/src/main/resources/config.yml) under `storage:`:

```yaml
storage:
  # Storage backends: SQLITE, MYSQL, or JSON
  type: SQLITE

  # SQLite configuration (creates data.db in plugin directory)
  sqlite:
    file: "data.db"

  # MySQL configuration (for networks / cross-server syncing)
  mysql:
    host: "localhost"
    port: 3306
    database: "ourteam_db"
    username: "administrator"
    password: "secure_password_here"
    use-ssl: false
    pool-properties:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout-ms: 30000
      idle-timeout-ms: 600000
      max-lifetime-ms: 1800000
```

### Database Tables:
1. **`ourteam_teams`**: Stores team ID, team name, disbanded status, and JSON data (members, roles, bank balance, warps, level, stats).
2. **`ourteam_players`**: Tracks player UUID, username, kills, deaths, blocks placed, blocks broken, mob kills, playtime (ms), grinding points, and last seen.

---

## 📋 Comprehensive PlaceholderAPI Guide

Registered under both `%ourteam_*%` and `%nteam_*%`.

### 1. Team Leaderboards
Format: `%ourteam_leaderboard_<stat>_<name|value>_<pos>%` or `%ourteam_top_team_<stat>_<name|value>_<pos>%`

| Statistic | Team Name Placeholder | Team Value Placeholder |
| :--- | :--- | :--- |
| **Blocks Placed / Build** | `%ourteam_leaderboard_place_name_1%` | `%ourteam_leaderboard_place_value_1%` |
| **Blocks Broken / Mined** | `%ourteam_leaderboard_break_name_1%` | `%ourteam_leaderboard_break_value_1%` |
| **Mob Kills** | `%ourteam_leaderboard_mobkill_name_1%` | `%ourteam_leaderboard_mobkill_value_1%` |
| **K/D Ratio** | `%ourteam_leaderboard_kd_name_1%` | `%ourteam_leaderboard_kd_value_1%` |
| **Kills** | `%ourteam_leaderboard_kills_name_1%` | `%ourteam_leaderboard_kills_value_1%` |
| **Deaths** | `%ourteam_leaderboard_deaths_name_1%` | `%ourteam_leaderboard_deaths_value_1%` |
| **Team Bank Balance** | `%ourteam_leaderboard_balance_name_1%` | `%ourteam_leaderboard_balance_value_1%` |
| **Team Level** | `%ourteam_leaderboard_level_name_1%` | `%ourteam_leaderboard_level_value_1%` |
| **Team Score / Points** | `%ourteam_leaderboard_score_name_1%` | `%ourteam_leaderboard_score_value_1%` |
| **Member Count** | `%ourteam_leaderboard_members_name_1%` | `%ourteam_leaderboard_members_value_1%` |

> **Legacy BetterTeams / DeluxeMenus Compatibility:**
> - `%ourteam_position_name_1%`, `%ourteam_position_score_1%`
> - `%ourteam_balanceposition_name_1%`, `%ourteam_balanceposition_money_1%`
> - `%ourteam_membersposition_name_1%`, `%ourteam_membersposition_members_1%`

---

### 2. Normal (Player) Leaderboards
Format: `%ourteam_top_<stat>_<name|value>_<pos>%` or `%ourteam_top_player_<stat>_<name|value>_<pos>%`

| Statistic | Player Name Placeholder | Player Value Placeholder |
| :--- | :--- | :--- |
| **Top Blocks Placed / Build** | `%ourteam_top_place_name_1%` | `%ourteam_top_place_value_1%` |
| **Top Blocks Broken / Mined** | `%ourteam_top_break_name_1%` | `%ourteam_top_break_value_1%` |
| **Top Mob Kills** | `%ourteam_top_mobkill_name_1%` | `%ourteam_top_mobkill_value_1%` |
| **Top K/D Ratio** | `%ourteam_top_kd_name_1%` | `%ourteam_top_kd_value_1%` |
| **Top Kills** | `%ourteam_top_kills_name_1%` | `%ourteam_top_kills_value_1%` |
| **Top Deaths** | `%ourteam_top_deaths_name_1%` | `%ourteam_top_deaths_value_1%` |
| **Top Killstreak** | `%ourteam_top_killstreak_name_1%` | `%ourteam_top_killstreak_value_1%` |
| **Top Playtime** | `%ourteam_top_playtime_name_1%` | `%ourteam_top_playtime_value_1%` |
| **Top Balance / Money** | `%ourteam_top_money_name_1%` | `%ourteam_top_money_value_1%` |

---

### 3. Personal Player Rankings (Your Position)
Shows the numerical rank position of the viewing player:
- `%ourteam_rank_kills%` &rarr; Your rank position in Kills (e.g. `1`, `14`)
- `%ourteam_rank_deaths%` &rarr; Your rank position in Deaths
- `%ourteam_rank_kd%` &rarr; Your rank position in K/D ratio
- `%ourteam_rank_place%` &rarr; Your rank position in Blocks Placed
- `%ourteam_rank_break%` &rarr; Your rank position in Blocks Broken
- `%ourteam_rank_mobkill%` &rarr; Your rank position in Mob Kills
- `%ourteam_rank_playtime%` &rarr; Your rank position in Playtime
- `%ourteam_rank_money%` &rarr; Your rank position in Balance
- `%ourteam_team_rank%` &rarr; Your team's rank position among all teams

---

### 4. Player Individual Statistics
- `%ourteam_place%` / `%ourteam_build%` &rarr; Blocks placed
- `%ourteam_break%` / `%ourteam_mined%` &rarr; Blocks broken
- `%ourteam_mobkill%` &rarr; Mob kills
- `%ourteam_kd%` &rarr; K/D ratio
- `%ourteam_kills%` &rarr; Player PvP kills
- `%ourteam_deaths%` &rarr; Deaths
- `%ourteam_killstreak%` &rarr; Current killstreak
- `%ourteam_best_killstreak%` &rarr; Best killstreak
- `%ourteam_playtime%` &rarr; Playtime (e.g. `2d 4h` or `45m`)

---

### 5. Player's Team Statistics
- `%ourteam_name%` / `%ourteam_tag%` &rarr; Team name
- `%ourteam_score%` &rarr; Team score points
- `%ourteam_level%` &rarr; Team level tier
- `%ourteam_balance%` / `%ourteam_money%` &rarr; Bank balance
- `%ourteam_moneyshort%` &rarr; Formatted balance (`25.5k`, `1.2m`)
- `%ourteam_members%` &rarr; Total members count
- `%ourteam_online%` &rarr; Online members count
- `%ourteam_maxmembers%` &rarr; Maximum allowed members
- `%ourteam_team_place%` &rarr; Total team blocks placed
- `%ourteam_team_break%` &rarr; Total team blocks broken
- `%ourteam_team_mobkill%` &rarr; Total team mob kills
- `%ourteam_team_kd%` &rarr; Combined team K/D ratio
- `%ourteam_team_kills%` &rarr; Total team kills
- `%ourteam_team_deaths%` &rarr; Total team deaths
- `%ourteam_pvp%` &rarr; Friendly fire status (`true`/`false`)
- `%ourteam_rank%` &rarr; Player's role in the team (`OWNER`, `ADMIN`, `MODERATOR`, `MEMBER`)

---

## 🛠️ Commands & Permissions Directory

### Player Commands (`ourteam.use`)
| Command | Aliases | Description |
| :--- | :--- | :--- |
| `/team` | `/t`, `/teams` | Opens the Main Team Dashboard (or Discovery Hub if no team) |
| `/team create <name>` | `/team c` | Creates a new team with specified tag |
| `/team invite <player>` | `/team i` | Invites a player to join your team |
| `/team join <team>` | `/team j` | Accepts an invite or joins an open team |
| `/team request <team>` | `/team req` | Sends a join request to a team |
| `/team leave` | `/team l` | Leaves your current team |
| `/team kick <player>` | `/team k` | Kicks a member from the team (Admin/Owner) |
| `/team disband` | `/team dis` | Disbands your team permanently (Owner only) |
| `/team promote <player>` | `/team prom` | Promotes a teammate's rank |
| `/team demote <player>` | `/team dem` | Demotes a teammate's rank |
| `/team chat` | `/team c` | Toggles between Team Chat and Global Chat |
| `/team msg <message>` | `/team m` | Sends a private message to online teammates |
| `/team pvp` | `/team ff` | Toggles friendly fire damage |
| `/team bank` | `/team b` | Opens the Team Bank GUI |
| `/team echest` | `/team ec` | Opens the shared Team Enderchest |
| `/team sethome [name]` | — | Sets a team home location |
| `/team home [name]` | — | Teleports to a team home |
| `/team delhome [name]` | — | Deletes a team home |
| `/team setwarp <name>` | — | Sets a public/team warp point |
| `/team warp <name>` | — | Teleports to a warp point |
| `/team delwarp <name>` | — | Deletes a warp point |
| `/team list` | — | Opens the active teams directory |
| `/team top` | — | Opens the team leaderboard |
| `/leaderboard` | `/lb`, `/leaderboards` | Opens the Main Leaderboard GUI |
| `/leaderboard <category>` | `/lb <category>` | Directly opens a leaderboard category |

### Admin Commands (`ourteam.admin`)
| Command | Alias | Description |
| :--- | :--- | :--- |
| `/adminteam reload` | `/ta reload` | Reloads all configurations, messages, and leaderboards |
| `/leaderboard reload` | `/lb reload` | Reloads leaderboard categories and menus |
| `/adminteam disband <team>` | — | Force disbands any team |
| `/adminteam forcejoin <player> <team>` | — | Force adds a player into a team |
| `/adminteam forcekick <player>` | — | Force kicks a player from their team |
| `/adminteam transfer <team> <player>` | — | Transfers team ownership |
| `/adminteam score add/take/set <team> <amount>` | — | Manually modifies team score |
| `/adminteam resetbank <team>` | — | Clears a team's bank balance |
| `/adminteam spy` | — | Toggles Team Chat surveillance for staff |

---

## 🔧 Adding Custom Leaderboards

You can add unlimited new leaderboards directly in [`LeaderboardGUI/leaderboards.yml`](file:///c:/Users/barsha/Desktop/ourteam-plugin-studio/paper/src/main/resources/LeaderboardGUI/leaderboards.yml).

```yaml
categories:
  custom_stat:
    name: "&6&lCUSTOM STAT"
    icon: DIAMOND
    slot: 28 # Slot in /leaderboard main menu (-1 for command-only)
    title: "Custom Stat | Page {page}/{max_page}"
    description: "custom stat"
    value_label: "Score"
    type: PLAYER # or TEAM
    internal_stat: "kills" # Built-in fallback stat
    ajlb_name_placeholder: "%ajlb_lb_my_custom_stat_{pos}_alltime_name%"
    ajlb_value_placeholder: "%ajlb_lb_my_custom_stat_{pos}_alltime_value_formatted%"
    max_pages: 7
    entry_title: "&a#{pos} {name}"
    entry_lore:
      - "&7Leaderboard"
      - ""
      - "&7Position: #{pos}"
      - "&7Score: &a{value}"
    main_lore:
      - "&7Leaderboard"
      - ""
      - "&7View the top players in the"
      - "&7custom stat leaderboard"
      - ""
      - "&e▶ &e&lCLICK &eto View"
```

Run `/lb reload` or `/adminteam reload` to apply changes instantly without a server restart!

---

## 🔨 Compilation & Build Instructions

### Requirements
- **JDK 21** or higher
- **Apache Maven 3.8+**

### Build Commands:
```bash
# Navigate to the paper module
cd paper

# Build and package the shaded jar
mvn clean package
```

The compiled jar will be created at:
```
paper/target/OurTeam-1.0.0.jar
```

Copy `OurTeam-1.0.0.jar` into your server's `plugins/` directory and restart.

---

## 📜 License
Distributed under the **MIT License**. See `LICENSE` for details.
