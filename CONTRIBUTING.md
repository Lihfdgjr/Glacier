# Contributing to Glacier

Thanks for considering a contribution. This guide covers the project layout and the concrete steps for adding a new check.

## Project layout

```
gg.glacier.Glacier               plugin entry, lifecycle, scheduling
gg.glacier.alert.*             alerts + sinks (Discord, future SQL/REST)
gg.glacier.check.Check         abstract base for every detection
gg.glacier.check.CheckManager  registry + per-player instantiation
gg.glacier.check.impl.*        check implementations, split per category
gg.glacier.command.*           /glacier handler + tab completion
gg.glacier.config.GlacierConfig  cached config view
gg.glacier.data.PlayerData     per-player state (timing, deltas, history)
gg.glacier.data.PlayerDataMgr  lifecycle of PlayerData instances
gg.glacier.hook.*              external integrations (PlaceholderAPI, ...)
gg.glacier.listener.*          Bukkit + ProtocolLib listeners
gg.glacier.storage.VlStorage   YAML-backed VL persistence
gg.glacier.util.*              ColorUtil, EntityUtil, MathUtil
```

## Build

```bash
mvn package
```

Output: `target/Glacier-<version>.jar`. JDK 8 or newer required.

## Coding conventions

- Java 8 source / target.
- Public mutable fields on `PlayerData` are intentional; the class is a per-player data bag co-owned by the listeners and the checks.
- Avoid referencing `Material.XXX` or `EntityType.XXX` constants that didn't exist in 1.8 OR don't exist in 1.13+. Use `name()` string matching or reflection. See `PhaseA`, `NoFallA`, `EntityUtil` for examples.
- Reach into `cfgD(key, def)` / `cfgI` / `cfgL` on `Check` rather than `section() != null ? section().getDouble(...) : default` boilerplate.
- Don't allocate inside `onMove` / `onFlyingPacket` hot paths unless you have to. The check fires up to 20 times per second per player.

## Adding a check

The minimum viable check is one Java file, one config block, one line in `CheckManager`.

### 1. Pick a category folder

Drop the new class under `src/main/java/gg/glacier/check/impl/<category>/`. Existing categories: `combat`, `movement`, `world`, `inventory`, `protocol`, `player`. If your check doesn't fit any, add a new folder and use a fresh category string in the constructor.

### 2. Write the class

```java
package gg.glacier.check.impl.combat;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class MyCheck extends Check {

    public MyCheck(Glacier plugin, PlayerData data) {
        super(plugin, data, "my-check", "combat");
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event) {
        double max = cfgD("max-something", 1.0);
        if (someValue > max) {
            flag(String.format("value=%.2f max=%.2f", someValue, max));
        } else {
            reward();
        }
    }
}
```

The `(Glacier, PlayerData)` constructor signature is required — `CheckManager` calls it reflectively.

Hooks available on `Check` (override what you need):

| Hook | Fires on |
|---|---|
| `onAttack(EntityDamageByEntityEvent)` | Player attacks an entity |
| `onMove(PlayerMoveEvent)` | Bukkit move event (position OR rotation change) |
| `onRotation()` | Pure rotation packet (ProtocolLib only) |
| `onFlyingPacket()` | Any flying packet (ProtocolLib only) |
| `onKeepAliveResponse(long rttMs)` | Keep-alive ACK |
| `onBlockPlace(BlockPlaceEvent)` | Block placed |
| `onBlockBreak(BlockBreakEvent)` | Block broken |
| `onInventoryClick(InventoryClickEvent)` | Inventory slot clicked |
| `onVelocityApplied()` | Server sent a velocity packet to the client |

### 3. Register

Add to `CheckManager.java`:

```java
register(MyCheck.class);
```

Categories aren't enforced — the registry is just an ordered list.

### 4. Add config block

In `src/main/resources/config.yml`, under the appropriate category banner:

```yaml
my-check:
  enabled: true
  max-vl: 10
  max-something: 1.0
  punishment: "kick {player} &cMy check"
```

The base `Check` already reads `enabled`, `max-vl`, and `punishment`; everything else you read via `cfgD/I/L`.

### 5. Test

- Build: `mvn package`
- Drop the jar into a test server's `plugins/`.
- Trigger the check, watch alerts via `/glacier alerts`, inspect `plugins/Glacier/logs/<date>.log`.
- Tune the threshold in `config.yml`, run `/glacier reload`.

## Common pitfalls

- **Material.XXX field references**: anything renamed during the 1.13 flattening (`STEP`, `WOOD_STEP`, `STATIONARY_WATER`, ...) will throw `NoSuchFieldError` at class load on 1.13+. Use `m.name()` string matching.
- **Calling `getEyeLocation()` / `getLocation()` repeatedly** in hot paths — each call allocates a fresh `Location`. Cache it.
- **HashMap on a packet-handler thread** — packet listeners can run off-main-thread. Use `ConcurrentHashMap` for any state that the main thread also touches.
- **Forgetting `reward()`** — without negative reinforcement, transient noise will eventually trip the punishment threshold. Always call `reward()` on the success branch.

## Reporting bugs

Issues should include:

- Server software + version (`Paper 1.20.4 build 437`)
- Client version
- The check id (`/glacier vl <name>`)
- The alert text or `plugins/Glacier/logs/<date>.log` excerpt
- A reproduction recipe if possible

## Pull requests

- One feature per PR, please.
- New checks: include a paragraph describing the false-positive surface (which legit edge cases will fire it, and what guard prevents that).
- Run `mvn package` locally first.
- The CI workflow at `.github/workflows/build.yml` will run on push.
