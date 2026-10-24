# PotatoStats

A **Paper 26.3** Minecraft plugin that collects server/player events and pushes them to the PotatoSMP website REST API (`https://potato-smp.pages.dev/api/collect`), so the site can show live stats without the browser ever talking to the game server directly.

## Badges

[![CI build](https://img.shields.io/github/actions/workflow/status/OmHackz/potato-smp-plugin/ci.yml?branch=master&label=build)](https://github.com/OmHackz/potato-smp-plugin/actions/workflows/ci.yml)
[![Paper 26.3](https://img.shields.io/badge/Paper-26.3-blue?logo=minecraft)](https://papermc.io/)
[![Java 25](https://img.shields.io/badge/Java-25-orange?logo=java)](https://adoptium.net/)
[![api-version 26.3](https://img.shields.io/badge/api--version-26.3-purple)](https://papermc.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

## What it does

| Stat key | Source event | Coalesced? |
|---|---|---|
| `player_join` | `PlayerJoinEvent` | No |
| `player_leave` | `PlayerQuitEvent` (+ session playtime) | No |
| `block_mined` | `BlockBreakEvent` | Yes (per-player count) |
| `block_placed` | `BlockPlaceEvent` | Yes (per-player count) |
| `kill` | `EntityDeathEvent` (player victim, player damager) | No |
| `death` | `PlayerDeathEvent` | No |
| `mob_kill` | `EntityDeathEvent` (hostile/neutral mobs) | No |
| `stat` | `PlayerStatisticIncrementEvent` | Yes (per player + per stat) |

- Batches are POSTed to `POST https://potato-smp.pages.dev/api/collect` with `Authorization: Bearer <secret>` every 30s off the main thread.
- Backoff on failure: 30s → 60s → 120s, capped at 5 min; failed events are re-queued.
- Per-player counters persist to `plugins/PotatoStats/data/<uuid>.json` (flush on quit + every 5 min), so restarts don't lose data.
- Optional read-only HTTP API (`0.0.0.0:8790`, disabled by default): `GET /v1/players`, `GET /v1/players/{uuid}`, `GET /v1/stats/leaderboard/{type}`.
- Public Paper API only — no NMS / CraftBukkit, no shaded dependencies.

## Install

1. Build: `./gradlew build` → `build/libs/potato-stats-1.0.0.jar`
2. Copy `potato-stats-1.0.0.jar` to your Paper 26.3 server's `plugins/` folder.
3. Start the server once, then edit `plugins/PotatoStats/config.yml`:
   - `push.secret` must match the website's `API_SECRET` (dev default `change-me`).
   - `push.enabled: true`
4. Restart the server.

## Config (`config.yml`)

```yaml
server-id: "potatosmp"
push:
  enabled: true
  url: "https://potato-smp.pages.dev/api/collect"
  secret: "change-me"
  interval-seconds: 30
  flush-max-events: 1000
http:
  enabled: false
  port: 8790
data:
  dir: "plugins/PotatoStats/data"
```

## Build requirements

- JDK 25 (Temurin recommended) — Paper 26.3 requires Java 25.
- Gradle wrapper included (`./gradlew`).

## License

MIT — see [LICENSE](LICENSE).
