# ⚔️ PvP Practice Bot Mod — Fabric 1.21.11

A Fabric mod for Minecraft 1.21.11 that spawns PvP practice bots with
basic anti-mace prediction and low-health gapping behavior.

---

## 📦 Requirements

| Software | Version |
|---|---|
| Java JDK | 21 |
| Minecraft | 1.21.11 |
| Fabric Loader | 0.18.1+ |
| Fabric API | 0.141.3+1.21.11 |

---

## 🔨 How to Build

From inside `pvpbot-mod`:

```bash
gradle build
```

The compiled jar will be under `build/libs/`.

### Build in GitHub (no local Java setup)

This repo includes a GitHub Actions workflow at
`.github/workflows/build-pvpbot-mod.yml` that builds with **Java 21** and uploads
the jar as an artifact.

1. Push this repo to GitHub.
2. Open **Actions** → **Build PvP Bot Mod**.
3. Click **Run workflow**.
4. After it finishes, open the run and download artifact: **`pvpbot-mod-jar`**.

---

## 🎮 In-Game Commands

Commands are currently open to anyone who can run chat commands on the server (permission checks were removed to stay compatible with newer 1.21.11 APIs).

### Primary command tree

| Command | Description |
|---|---|
| `/pvpbot spawn <name>` | Spawn a bot at your current position |
| `/pvpbot attack <botName> <player>` | Make a bot target and fight a player |
| `/pvpbot stop <botName>` | Make bot passive |
| `/pvpbot remove <botName>` | Remove bot from world |
| `/pvpbot list` | List tracked bots |

### Quick aliases

| Command | Description |
|---|---|
| `/spawnbot67 [name]` | Spawn bot (default name: `bot67`) |
| `/botattack67 <botName> <player>` | Attack alias |
| `/botstop67 <botName>` | Stop alias |
| `/removebot67 <botName>` | Remove alias |
| `/listbots67` | List alias |
| `/pvpbot67 ...` | Full shorthand root for `/pvpbot ...` |

---

## 🤖 Bot Behavior

- Uses a **Mace** + **Shield** loadout.
- **Shield prediction**: if the target is around 8+ blocks above the bot (likely slam setup), the bot raises shield briefly.
- **Gap system**: when the bot drops to low HP, it pauses and “gaps”, then gets a burst heal + regen/absorption effects.
- Visible name tag (`[PvP Bot] <name>`), persistent (no natural despawn), and normal pathfinding.
- Command handlers are wrapped with defensive error handling so command failures return chat errors instead of crashing the server.

---

## Notes

This version is standalone and does **not** require HeroBrine/HeroBot external APIs.
