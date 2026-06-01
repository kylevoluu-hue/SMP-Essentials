# SMP Essentials

A quality-of-life plugin for survival multiplayer (SMP) servers running
**Paper / Bukkit 26.1.2**. It adds a suite of custom **Amethyst & Blaze tools**, a
`/bed` command, and a strict **anti-combat-log** system so PvP fights have stakes.

| | |
|---|---|
| **Platform** | Paper / Bukkit (Spigot-compatible API) |
| **Minecraft / API** | `26.1.2` (api-version `26.1`) |
| **Java** | **25** (required to build and run) |
| **Clients** | Java **and** Bedrock (via Geyser/Floodgate) |
| **Dependencies** | None (single jar) |
| **License** | MIT |

---

## Features

### ⛏️ Amethyst Pickaxe — 3×3 mining
Mines a **3×3 area** centred on the block you break, in the plane you're facing
(dig down → horizontal 3×3 floor; mine into a wall → vertical 3×3 face). Only
blocks a pickaxe is actually the right tool for are affected (it won't vacuum up
dirt or gravel), and unbreakable blocks like bedrock are skipped.

### 🪓 Amethyst Axe — tree feller
Break **one log** and the **entire connected tree** comes down at once — works on
every wood type (including huge jungle/dark-oak trunks and nether stems), with a
configurable safety cap so it never lags the server. Optionally clears the leaves
too.

### 🥄 Amethyst Shovel — 5×5 digging
Digs a **5×5 area** in the plane you're facing. Only shovel-appropriate blocks
(dirt, grass, sand, gravel, clay, soul sand, etc.) are affected, so it won't tear
through stone or ores.

### 🗡️ Amethyst Sword — dripstone strike
Base attack damage **10** (scales with crits and Sharpness). On every hit, a
**pointed dripstone is summoned above your target** and falls onto it for bonus
damage, then **vanishes the instant it lands** (no block, no drop). Bonus damage
and spawn height are configurable.

### 🪣 Amethyst Bucket — infinite fluid tank
Right-click a water or lava pool to **drain the whole connected body** into the
bucket (auto-detecting the fluid), with **unlimited, separate** water and lava
storage. **Sneak + right-click** switches which stored fluid you pour, and
right-clicking a block places a source of it. (A configurable per-drain cap keeps
it from swallowing an entire ocean.)

### 🔥 Blaze Sword — fire & fireballs
A **gold-sword** design with base attack damage **12**. Every hit **sets the
target on fire** and **launches a fireball** at it that deals **8 extra damage**
(both amounts configurable). Crafted from blaze rods, a magma block and nether
stars — not amethyst (see [Recipes](#recipes)).

### 🪄 Blaze Wand — multi-mode
A blaze-rod wand with three modes — **sneak + right-click** to cycle, right-click
to use:
- **Summon** — spawns a blaze that fights for ~6 seconds, then despawns.
- **Range** — fires a timed volley of 5 fireballs (3 hearts each).
- **Bow** — lobs primed TNT that explodes and sets the area on fire.

All amounts/timers are configurable under `blaze-wand:`. Crafted from blaze rods,
a magma block and a nether star.

### 🔮 Amethyst tools are real tools
- **Netherite design** — they use the netherite item models (and netherite
  durability / fire-resistance), just renamed *Amethyst …* with a glint.
- **Craftable but expensive** — every tool costs **1 amethyst block + 2 netherite
  blocks** (see [Recipes](#recipes)).
- **Enchantable** — apply Efficiency, Fortune, Silk Touch, Unbreaking, Sharpness,
  Mending, etc. Fortune and Silk Touch apply to **every** block an ability breaks.
- **Durability** — area tools only lose 1 point per swing by default (full
  netherite lifespan); see `damage-tool` in the config.
- **Amethyst chime sound** plays when you mine with one and when you select it on
  your hotbar.
- Also obtainable via `/smpe give <tool>` for admins.

### ⚔️ Anti-combat-log
Combat is **strictly player-vs-player**: the timer is only ever started by a
player hitting another player (melee or a player-fired projectile) — mob and
environmental damage never tag you. When tagged, a player is **in combat for 20
seconds**, and **every new hit resets the timer back to the full 20s**. While in
combat:
- A live countdown is shown as a **boss bar** (a draining bar at the top of the
  screen) and on the **action bar** — both configurable (`boss-bar`,
  `boss-bar-color`, `action-bar`).
- **Escape commands** (`/tp`, `/tpa`, `/home`, `/spawn`, `/warp`, `/rtp`, `/back`, …)
  are blocked.
- **Teleports** from any plugin (command/plugin/spectate) are blocked. **Ender
  pearls and chorus fruit are still allowed** — they're legitimate combat movement.

Exactly **what is disabled in combat is fully configurable** — every form of
transport has its own on/off toggle:

| Toggle | Blocks |
|---|---|
| `block-commands` | the commands in `blocked-commands` |
| `block-teleports` | generic teleports in `blocked-teleport-causes` (COMMAND/PLUGIN/SPECTATE/…) |
| `block-elytra` | elytra gliding |
| `block-flight` | flying (creative/allow-flight) |
| `block-mounting` | boarding boats, minecarts, horses, pigs, striders, camels, llamas, … |
| `block-dismounting` | leaving a vehicle or mount |
| `block-ender-pearl` | ender pearl teleports |
| `block-chorus-fruit` | chorus fruit teleports |
| `block-end-gateway` | end gateway teleports |
| `block-exit-bed` | the teleport when leaving a bed |

Everything except `block-commands`/`block-teleports` is off by default, and all
of these (plus the editable command/teleport-cause lists) can be set per-preset.

If a player **disconnects while in combat** ("combat logging"):
- They are **slain**: their full **inventory + armor** drops as loot at the spot
  they logged out (so their opponent can collect it), and their **XP** drops too.
- When they reconnect they come back **empty** and are **teleported to spawn**.
- A server message announces the kill.

---

## Requirements

- A **Paper 26.1.2** (or compatible) server. Spigot works too, but Paper is
  recommended (the plugin uses Paper's Adventure messaging APIs).
- **Java 25** — Minecraft 26.1 requires it.

### Bedrock + Java (Geyser) compatibility
The plugin is fully server-side and works for **both Java and Bedrock players**.
Tools are deliberately built from plain item names and lore (no custom textures or
custom model data), so Bedrock players connecting through **Geyser/Floodgate** see
fully functional Amethyst tools and all mechanics behave identically.

---

## Installation

1. Make sure your server is running **Paper 26.1.2** on **Java 25**.
2. Download `SMPEssentials.jar` (from the releases page, or build it — see below).
3. Drop the jar into your server's `plugins/` folder.
4. Restart the server (or run `/reload confirm`, though a full restart is preferred).
5. A `plugins/SMPEssentials/config.yml` is generated on first start. Edit it and run
   `/smpe reload` to apply changes.

---

## Full setup tutorial (from scratch)

This walks you through standing up a brand-new server with the plugin, for both
Java and Bedrock players.

### 1. Install Java 25

**Debian / Ubuntu**
```bash
sudo apt-get update
sudo apt-get install -y openjdk-25-jre-headless
java -version   # should report 25.x
```

**Other platforms** — download a JDK/JRE 25 build from
[Adoptium (Eclipse Temurin)](https://adoptium.net/) and install it.

### 2. Download a Paper 26.1.2 server jar

Grab the latest 26.1.2 build from <https://papermc.io/downloads/paper> and save it
into a fresh folder, e.g. `paper-26.1.2.jar`.

### 3. First run + EULA

```bash
mkdir my-smp && cd my-smp
cp /path/to/paper-26.1.2.jar paper.jar

# First launch generates eula.txt and server files, then stops:
java -Xms2G -Xmx2G -jar paper.jar nogui

# Accept the EULA:
echo "eula=true" > eula.txt
```

A few recommended `server.properties` values for an SMP:
```properties
online-mode=true          # set false ONLY if you use a proxy with Floodgate
difficulty=hard
spawn-protection=0
allow-flight=true         # avoids false "flying" kicks with some plugins
```

### 4. Install SMP Essentials

```bash
mkdir -p plugins
cp /path/to/SMPEssentials.jar plugins/
java -Xms2G -Xmx2G -jar paper.jar nogui
```

In the console you should see:
```
[SMPEssentials] SMP Essentials enabled (Amethyst tools + anti-combat-log).
```

### 5. (Optional) Let Bedrock players join — Geyser + Floodgate

To accept Bedrock Edition players on the same server:
1. Download **Geyser-Spigot** and **Floodgate** from
   <https://geysermc.org/download> and place both jars in `plugins/`.
2. Restart the server. Geyser listens on UDP `19132` by default.
3. Bedrock players connect to your server IP on port `19132`.
4. Floodgate lets them join without a Java/Microsoft account. (If you run
   Floodgate, set `online-mode=false` in `server.properties` **only** behind a
   proper proxy/Floodgate setup.)

SMP Essentials needs no extra configuration for Bedrock — it just works.

### 6. Verify it loaded

Run in console or in-game:
```
/plugins
```
`SMPEssentials` should be listed in green.

### 7. In-game smoke test

```
/smpe give pickaxe          # get the Amethyst Pickaxe
/smpe give axe              # get the Amethyst Axe
```
- Mine a block of stone → a **3×3** area breaks and you hear the amethyst chime.
- Break one log of a tree → the **whole tree** falls.
- Hit another player, then disconnect within 20 seconds → your items + XP drop
  where you logged out, and you respawn empty at spawn when you rejoin.
- While in combat, try `/spawn` or `/home` → it's blocked.

---

## Building from source

You need **JDK 25** and Maven. The build depends on the Paper API from the PaperMC
repository (configured in `pom.xml`), so the build machine must be able to reach
`https://repo.papermc.io`.

```bash
git clone https://github.com/kylevoluu-hue/smp-essentials.git
cd smp-essentials
JAVA_HOME=/path/to/jdk-25 mvn -DskipTests clean package
```

The compiled plugin is written to:
```
target/SMPEssentials.jar
```

> **Note:** The project targets `maven.compiler.release=25` and
> `io.papermc.paper:paper-api:[26.1.2.build,)`. Older JDKs cannot compile against
> the 26.1.2 API.

---

## Recipes

Every Amethyst tool is intentionally **expensive**: each costs **1 amethyst block
+ 2 netherite blocks** (`N` = netherite block, `A` = amethyst block, `S` = stick).

```
Amethyst Pickaxe     Amethyst Axe      Amethyst Shovel   Amethyst Sword    Amethyst Bucket
   N A N                A N                 A                A                 N . N
   . S .                N S                 N                N                 . A .
   . S .                . S                 N                N
```
`N` = netherite block, `A` = amethyst block, `S` = stick.

The **Blaze tools** are the exception — forged from Nether materials
(`R` = blaze rod, `M` = magma block, `*` = nether star):
```
Blaze Sword     Blaze Wand
   * . *          R * R
   . M .          R M R
   . R .          . R .
```

Crafting can be disabled (`amethyst-tools.recipes-enabled: false`) if you want the
tools to be admin-only (via `/smpe give`).

---

## Commands

| Command | Description | Permission |
|---|---|---|
| `/smpe give <pickaxe\|axe\|shovel\|sword\|bucket\|blaze\|wand> [player]` | Give a custom tool | `smpessentials.give` |
| `/smpe combat [player]` | Show how long you (or another player) are in combat | `smpessentials.combat.check` |
| `/smpe preset [list\|<name>]` | List the available presets, or apply one live | `smpessentials.reload` |
| `/smpe reload` | Reload `config.yml` | `smpessentials.reload` |
| `/bed` | Teleport to your bed / respawn point (warns if none; blocked in combat) | `smpessentials.bed` |

Alias: `/smpessentials`.

## Permissions

| Permission | Description | Default |
|---|---|---|
| `smpessentials.*` | All permissions | op |
| `smpessentials.give` | Use `/smpe give` | op |
| `smpessentials.reload` | Use `/smpe reload` | op |
| `smpessentials.combat.check` | Check combat status | everyone |
| `smpessentials.bed` | Use `/bed` | everyone |
| `smpessentials.combat.bypass` | Exempt from combat tagging, command/teleport blocking, and combat-log penalties | nobody |

---

## Presets

Instead of tweaking every option by hand, pick a **preset** — a named bundle of
settings applied on top of the rest of your config. Set `active-preset` at the
top of `config.yml`:

| Preset | What it does |
|---|---|
| `custom` | Ignore presets; use the exact values in `config.yml` (default) |
| `default` | Balanced settings (same as the shipped defaults) |
| `hardcore` | 30s combat timer, no escaping by flight/glide/ride/pearl/chorus, trees drop their leaves |
| `casual` | No combat-log penalties at all; tools wear at the normal 1x1 rate |

```yaml
active-preset: hardcore
```
then run `/smpe reload`. You can also switch **live** (until the next restart)
with `/smpe preset hardcore`, and list options with `/smpe preset list`. Presets
live in the `presets:` section at the bottom of `config.yml` — add your own there.

## Configuration

The full `config.yml` (every key is documented in-file):

```yaml
amethyst-tools:
  enabled: true            # master switch for the tools
  recipes-enabled: true    # register the crafting recipes
  sound-effects: true      # amethyst chime when mining/holding a tool
  pickaxe:
    damage-tool: false     # false = 1 durability per swing (like 1x1 mining); true = charge per extra block
  axe:
    damage-tool: false     # false = 1 durability per chop; true = charge per extra log
    max-logs: 256          # safety cap on logs felled per chop
    break-leaves: false    # also clear leaves when felling

combat:
  enabled: true
  duration-seconds: 20     # player-vs-player only; resets to full on each new hit
  drop-experience: true    # drop XP when someone combat-logs
  # --- what is disabled while in combat (each is its own toggle) ---
  block-commands: true     # block the commands in blocked-commands
  block-teleports: true    # block the teleport causes in blocked-teleport-causes
  block-elytra: false      # prevent gliding away with an elytra
  block-flight: false      # cancel flight (creative/allow-flight)
  block-mounting: false    # boarding boats/minecarts/horses/etc.
  block-dismounting: false # leaving a vehicle or mount
  block-ender-pearl: false # ender pearl teleports
  block-chorus-fruit: false# chorus fruit teleports
  block-end-gateway: false # end gateway teleports
  block-exit-bed: false    # the teleport when leaving a bed
  blocked-commands: [tp, tpa, tpaccept, tpahere, tpyes, teleport, home, sethome,
    homes, spawn, warp, warps, rtp, wild, randomtp, back, lobby, hub, server]
  blocked-teleport-causes: [COMMAND, PLUGIN, SPECTATE]  # generic causes (pearl/chorus have own toggles)
  teleport-to-spawn-on-rejoin: true
  action-bar: true         # text countdown above the hotbar
  boss-bar: true           # draining boss-bar countdown at the top of the screen
  boss-bar-color: RED      # RED, PINK, BLUE, GREEN, YELLOW, PURPLE, WHITE

messages:
  prefix: "&5&lSMP &8» &r"
  # ... all user-facing messages, with &-color codes and {placeholders}
```

`&`-style colour codes are supported in every message. Placeholders such as
`{player}`, `{seconds}`, `{tool}` and `{command}` are substituted automatically.

---

## How it works (deep dive)

- **3×3 mining** uses a ray trace from the player's eyes to find the block face
  being mined, then breaks the 8 neighbouring blocks in the perpendicular plane.
  Each extra block fires its **own `BlockBreakEvent`**, so land-protection plugins
  (WorldGuard, GriefPrevention, towny claims, …) can veto individual blocks. A
  per-player guard prevents the simulated events from looping back on themselves.
- **Tree feller** flood-fills connected logs through the 26-block neighbourhood up
  to `max-logs`, firing a `BlockBreakEvent` per log for the same protection safety.
- **Durability**: by default the tool only loses 1 point per swing/chop — the
  extra blocks the ability breaks are free, so a netherite-tier tool lasts its
  full ~2031 uses. Set `damage-tool: true` to charge per extra block instead
  (Unbreaking respected; the ability stops if the tool would break).
- **Combat tagging** stamps an expiry timestamp on both fighters; every new hit
  re-stamps it to the full duration. A once-per-second task refreshes the action
  bar and clears expired tags.
- **Combat logging** is handled in the quit event (before player data is saved),
  so the cleared inventory persists — the player genuinely comes back empty. The
  list of slain combat-loggers is persisted to `combat-loggers.yml` so the rejoin
  penalty survives restarts.

---

## Compatibility notes

- **Multi-version:** the plugin only uses stable Bukkit/Paper API and a low
  `api-version`, so it runs across 26.1.x and forward.
- **Folia:** not supported out of the box (it uses the standard `BukkitScheduler`).
- **Protection plugins:** supported — the area abilities respect any plugin that
  cancels `BlockBreakEvent`.

## Troubleshooting / FAQ

- **Tools won't craft.** Check `amethyst-tools.recipes-enabled` and that you're
  using the exact layout above (1 amethyst shard + 2 netherite ingots + 2 sticks).
- **3×3 breaks nothing extra.** The pickaxe only breaks blocks it's the correct
  tool for, and skips protected/unbreakable blocks.
- **Players keep their items after combat logging.** Ensure `combat.enabled: true`
  and that they don't have `smpessentials.combat.bypass`.
- **Server won't start / plugin won't load.** Confirm you're on **Java 25** and a
  **26.1.2** server build.

---

## License

Released under the [MIT License](LICENSE).
