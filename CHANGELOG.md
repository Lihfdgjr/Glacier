# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned

- Punishment ladder (multi-stage commands per VL threshold)
- bStats integration (anonymized usage metrics)
- More scaffold variants (rotation acceleration over time)
- Web dashboard / REST endpoint for VL queries

## [0.1.0] - 2026-04-25

First public release.

### Added

#### Detection coverage (29 checks across 7 categories)

- **Combat** — `autoclicker-a` (peak CPS), `autoclicker-b` (CV regularity),
  `autoclicker-c` (bimodal click distribution), `reach` (latency-adaptive
  AABB distance), `killaura-a` (yaw-to-target), `killaura-b` (no flying
  packet between attacks), `killaura-c` (rotation GCD), `aim-a` (snap on
  attack), `hitbox-a` (raycast vs target AABB), `criticals-a`.
- **Movement** — `speed-a` (buffered), `speed-b` (predictive friction +
  input model), `fly-a`, `nofall-a`, `phase-a` (NoClip), `velocity-a`
  (ignored knockback), `noweb-a` (cobweb bypass), `noslow-a` (sprint
  while using items).
- **World** — `scaffold-a/b/c`, `fastplace-a`, `fastbreak-a`.
- **Inventory** — `inventory-move-a`.
- **Protocol** — `bad-packets-a` (invalid pitch), `bad-packets-b`
  (flying-packet burst).
- **Player** — `timer` (flying-interval), `timer-b`
  (transaction-latency-compensated).

#### Infrastructure

- Per-player Violation Level system with `reward()` decay and configurable
  per-check punishments.
- Five-layer false-positive defence: join/respawn grace, bypass
  permission, alert cooldown, knockback grace, lag-spike clamp.
- Transaction-based lag compensation: `Play.Server.TRANSACTION` probe
  every 5 ticks, sub-tick RTT measurement; falls back to keep-alive RTT.
- Cross-version support: same jar runs on Bukkit/Spigot/Paper from 1.8.x
  to 1.21.x. Material/Entity references are name-string-based and load
  on every version.
- Cross-client support via ViaVersion + ViaBackwards + ViaRewind, 1.6.4
  through latest.
- Persistent Violation Levels via YAML (`plugins/Glacier/vl.yml`),
  debounced async writes, periodic snapshots.
- Citizens / ZNPCs / MythicMobs NPC exemption (via `NPC` metadata key).
- Alert log files at `plugins/Glacier/logs/<date>.log`.

#### Integrations

- **ProtocolLib** (soft-dep) — packet-level checks and transaction probe.
- **PlaceholderAPI** (soft-dep) — `glacier_vl_total`, `glacier_vl_worst`,
  `glacier_vl_worst_name`, `glacier_ping_tx`, `glacier_ping_ka`,
  `glacier_checks_count`.
- **Discord webhook** — alert sink, async POST via HttpURLConnection,
  configurable VL threshold.
- **AlertSink** interface — pluggable sinks for future SQL / REST / Slack
  consumers.

#### Commands

- `/glacier alerts` — toggle staff alerts
- `/glacier vl <player>` — show every check's VL for a player
- `/glacier top` — top 10 current violators
- `/glacier reload` — reload `config.yml`
- `/glacier info` — version + check count
- `/glacier help`
- TAB completion on subcommands and player arguments.

#### Documentation

- README with detection list, install/build, config, integrations,
  architecture overview.
- CONTRIBUTING with how-to-add-a-check walk-through.
- GitHub Actions CI workflow building on JDK 8.
- Issue templates for bug reports and check ideas.

### License

GPL-3.0-or-later.
