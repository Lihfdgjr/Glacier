# Glacier

Glacier is an open-source Bukkit / Spigot / Paper anticheat. Single jar, runs on every server version from **1.8.x to 1.21.x**, supports every client version from **1.6.4 onward** through ViaVersion. 29 checks across 7 categories, transaction-based lag compensation, persistent violation levels, PlaceholderAPI and Discord integrations.

> Status: pre-1.0. The detection coverage and false-positive guards are solid, but the project is young — expect tuning.

---

## Features

- **29 detections**, configured per check with three knobs each: `enabled`, `max-vl`, `punishment`.
- **Transaction-based lag compensation** — sub-tick latency probe via `Play.Server.TRANSACTION` (falls back to keep-alive RTT).
- **Five-layer false-positive defence** — join/respawn grace, bypass permission, alert cooldown, knockback grace, lag-spike clamp.
- **Cross-version** — same jar on every server version from 1.8 to 1.21. Material/Entity references are version-agnostic.
- **Cross-client** — pair with ViaVersion + ViaBackwards + ViaRewind to accept clients from 1.6.4 to latest.
- **Persistence** — per-player violation levels survive restarts (`plugins/Glacier/vl.yml`, debounced async writes).
- **Integrations** — PlaceholderAPI placeholders, Discord webhook, all soft-dependent.

### Detection list

| Category | Check | Description |
|---|---|---|
| Combat | `autoclicker-a` | Sustained CPS above threshold |
| Combat | `autoclicker-b` | Robotic regularity (coefficient of variation) |
| Combat | `autoclicker-c` | Bimodal click distribution (burst-pause macros) |
| Combat | `reach` | Latency-adaptive attack distance |
| Combat | `killaura-a` | Target outside field of view on hit |
| Combat | `killaura-b` | Two attacks with no flying packet between |
| Combat | `killaura-c` | Rotation GCD (quantized aim modules) |
| Combat | `aim-a` | Rotation snap on attack |
| Combat | `hitbox-a` | Ray from eye does not pierce target's bounding box |
| Combat | `criticals-a` | Critical hit registered while stably on the ground |
| Movement | `speed-a` | Buffered speed cap |
| Movement | `speed-b` | Predictive speed (friction + input model) |
| Movement | `fly-a` | Sustained airtime without descent |
| Movement | `nofall-a` | Client claims onGround while clearly airborne |
| Movement | `phase-a` | Body inside a non-passable solid block |
| Movement | `velocity-a` | Ignored knockback (absorbed fraction below threshold) |
| Movement | `noweb-a` | Cobweb speed bypass |
| Movement | `noslow-a` | Sprint-speed while using items |
| World | `scaffold-a` | Below-feet placement against face matching player yaw |
| World | `scaffold-b` | Rotation snap immediately before below-feet place |
| World | `scaffold-c` | Below-feet placement without looking down |
| World | `fastplace-a` | Block place interval below threshold |
| World | `fastbreak-a` | Block break interval below threshold |
| Inventory | `inventory-move-a` | Movement / rotation while a container is open |
| Protocol | `bad-packets-a` | Pitch outside `[-90, 90]` |
| Protocol | `bad-packets-b` | Flying-packet burst (sub-15 ms gaps) |
| Player | `timer` | Client tick rate outpacing server |
| Player | `timer-b` | Transaction-latency-compensated tick drift |

---

## Install

1. Drop `Glacier-x.y.z.jar` into your server's `plugins/` directory.
2. (Recommended) Install **ProtocolLib** — enables packet-level checks (Timer, BadPacketsB, KillAuraB/C, transaction lag-comp).
3. (For multi-version client support) Install **ViaVersion** + ViaBackwards + ViaRewind.
4. (Optional) Install **PlaceholderAPI** for scoreboard / tablist integration.
5. Start the server. `plugins/Glacier/config.yml` is generated on first run.
6. Run `/glacier reload` after editing config.

### Supported versions

- Server: Bukkit / Spigot / Paper from **1.8.x** through **1.21.x**.
- Client: through ViaVersion ecosystem, **1.6.4** through latest.
- Java: **8 or newer** at runtime (jar is built targeting Java 8 bytecode).

---

## Build from source

Requirements: JDK 8+, Maven 3.6+.

```bash
git clone https://github.com/<your-org>/glacier.git
cd glacier
mvn package
```

Artifact lands at `target/Glacier-<version>.jar`. The build pulls Spigot 1.8.8 API from the Spigot snapshot repo and ProtocolLib from `dmulloy2`'s public repo; both are `provided` scope, so the final jar is small (~95 KB).

---

## Commands

| Command | Description |
|---|---|
| `/glacier alerts` | Toggle violation alerts (chat) |
| `/glacier vl <player>` | Show every check's current VL for a player |
| `/glacier top` | Top-10 current violators (sum of VLs) |
| `/glacier reload` | Reload `config.yml` |
| `/glacier info` | Plugin version + check count |
| `/glacier help` | Command list |

Aliases: `/ac`, `/anticheat`.

## Permissions

| Node | Default | Effect |
|---|---|---|
| `glacier.command` | op | Run `/glacier` |
| `glacier.alerts` | op | Receive alerts after `/glacier alerts` |
| `glacier.bypass` | none | Skip every check (for staff testing) |

---

## Configuration

Each check follows the same pattern:

```yaml
checks:
  reach:
    enabled: true
    max-vl: 12
    base-distance: 3.0
    max-latency-slack: 0.8
    punishment: ""
```

Punishment is a console command with `{player}` replaced — leave empty for "alert only".

### Alert format

```yaml
alerts:
  prefix: "&8[&bGlacier&8]&r "
  format: "&b{player}&7 failed &b{check} &8(&7{type}&8 · &7x{vl}&8) &8{info}"
  cooldown-ms: 250
  log-to-file: true
```

Placeholders: `{player}`, `{check}`, `{type}`, `{vl}`, `{max-vl}`, `{ping}`, `{tps}`, `{info}`.

### Integrations

```yaml
integrations:
  discord:
    enabled: false
    webhook-url: ""
    username: "Glacier"
    min-vl: 5.0
    format: "`{player}` failed `{check}` (vl {vl}/{max-vl}) - {info}"

  persistence:
    enabled: true
    periodic-save-seconds: 300
```

### PlaceholderAPI placeholders

| Placeholder | Returns |
|---|---|
| `%glacier_vl_total%` | Sum of all check VLs |
| `%glacier_vl_worst%` | Highest single check VL |
| `%glacier_vl_worst_name%` | Id of the worst-offending check, or `-` |
| `%glacier_ping_tx%` | Transaction-measured RTT in ms, or `-` |
| `%glacier_ping_ka%` | Keep-alive RTT in ms |
| `%glacier_checks_count%` | Number of checks loaded |

---

## Architecture

```
gg.glacier
├── Glacier                    plugin entry, lifecycle, scheduling
├── alert/                   AlertManager + AlertSink + DiscordWebhook
├── check/                   Check (abstract) + CheckManager
│   └── impl/                checks split per category
├── command/                 /glacier handler + tab completion
├── config/                  cached config view
├── data/                    PlayerData + PlayerDataManager
├── hook/                    PlaceholderHook
├── listener/                BukkitListener + PacketListener (ProtocolLib)
├── storage/                 VlStorage (YAML persistence)
└── util/                    ColorUtil, EntityUtil, MathUtil
```

Each check is a self-contained class extending `Check` with one or more event hooks. New checks are added by writing a class and registering it in `CheckManager`. See [CONTRIBUTING.md](CONTRIBUTING.md).

### Why a 1.8.8-targeted jar runs on 1.21

Material and EntityType enum constants that were renamed during the 1.13 flattening are not referenced directly. String-based name matching (`m.name().contains("SLAB")`) works on every version. `Entity#getHeight()` is invoked reflectively when present and falls back to a hard-coded table on 1.8. Same approach for `Player#getPing()`.

---

## Contributing

Pull requests welcome — see [CONTRIBUTING.md](CONTRIBUTING.md) for the project layout and a how-to-add-a-check walk-through.

If you find a false positive, please open an issue with:
- Server version
- Client version
- Check id (`/glacier vl <yourname>` to find it)
- A description of what you were doing

## License

GPL-3.0-or-later — see [LICENSE](LICENSE).

Glacier is **copyleft**. Any fork, redistribution, or derived work must remain open source under the same license. This matches the rest of the Bukkit/Spigot/Paper ecosystem and prevents Glacier from being repackaged into closed-source paid plugins.
