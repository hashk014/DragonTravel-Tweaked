# DragonTravel Reforged - hash014

A revival of [DragonTravel](https://github.com/Phiwa/DragonTravel), maintained in the [DragonTravel-Tweaked](https://github.com/hashk014/DragonTravel-Tweaked) fork.

A **Java server plugin** for Bukkit/Spigot/Paper. Players ride a real Ender Dragon without a client mod or resource pack. One JAR is compiled for Java 8; newer servers run with their required Java version. The matrix in `docs/reforged/compatibility-results.json` records the versions and builds actually tested, up to **26.3**. Unlisted versions are not claimed as tested. Folia is unsupported. All bundled player messages, help text and documentation are in English.

## Installation

1. Stop the server and back up its worlds and configuration.
2. Remove the old DragonTravel JAR: both plugins register `/dt`.
3. Copy `DragonTravel-Reforged-2.0.0.jar` into `plugins/`, then start the server.
4. As an operator, stand on solid ground and run `/dt setstat village`.
5. Create a second station, then use `/dt travel village` or `/dt menu`.

`plugins/DragonTravelReforged/config.yml` controls speed, altitude, limits, cooldown, worlds, station departures and fares. Speed is measured in **blocks per second**, rather than blocks per tick. Landing requires two clear blocks above solid ground. If the destination becomes unsafe, the journey returns to its departure point. `/dt stop` and dismounting also return the player to the departure point.

## Features

- Real dragon flight with takeoff, cruising altitude, landing and progress updates with estimated time remaining.
- Persistent stations, homes stored by UUID and travel to players who can opt out with `/dt ptoggle`.
- Scenic routes with up to 64 waypoints and an editor available in game.
- Paginated station and route menu, with permissions for individual destinations.
- Persistent decorative dragons; chunks stay loaded while the plugin uses them.
- Signs with `[DTR]` on line 1, `travel` or `flight` on line 2 and the destination name on line 3.
- Free journeys, ordinary resource fares or optional Vault integration.
- An atomic departure journal saved before mounting: interrupted journeys recover on the next login, returning to departure or to spawn if departure is no longer safe.
- Dragons with destructive AI suppressed, protection during flight and temporary fall damage protection.
- An API with cancellable `JourneyStartEvent` for third-party protection plugins, plus `JourneyEndEvent`.

## Commands

| Command | Purpose |
| --- | --- |
| `/dt menu` | Open the station and route menu |
| `/dt travel <station>` | Travel to a station |
| `/dt stations`, `/dt routes` | List destinations |
| `/dt flight <route>` | Follow a route's waypoints |
| `/dt sethome`, `/dt home` | Save your home / travel home |
| `/dt ptravel <player>`, `/dt ptoggle` | Travel to a player / toggle privacy |
| `/dt status`, `/dt stop` | Show progress / return to departure |
| `/dt setstat <name>`, `/dt remstat <name>` | Manage stations (admin) |
| `/dt createflight <name>` | Open the route editor (admin) |
| `/dt setwp`, `/dt remlastwp`, `/dt saveflight` | Add, remove and save waypoints (admin) |
| `/dt remflight <name>` | Remove a route (admin) |
| `/dt showflight <name>` | Show the waypoint count |
| `/dt flight <route> <player>` | Send a player on a route, subject to their permissions (admin) |
| `/dt addstatdragon <name> [display-name]` | Create a decorative dragon (admin) |
| `/dt remstatdragon <name>` | Remove a decorative dragon (admin) |
| `/dt ctravel <x> <y> <z> [world]` | Travel to coordinates (separate permission) |
| `/dt reload` | Reload the configuration (admin) |
| `/dt importlegacy` | Import historical stations and routes (admin) |

Identifiers accept 1 to 32 ASCII letters, digits, `_` or `-`. A route's final waypoint must be a safe landing; intermediate waypoints may be in the air. Each route stays in one world. Travel between worlds is disabled by default. When enabled, an explicit transition to the target world's spawn precedes the local flight.

## Permissions and fares

`dragontravel.use`, `dragontravel.home` and `dragontravel.player` are granted by default. `dragontravel.admin`, `dragontravel.coords` and `dragontravel.bypasscost` default to operators. `dt.admin` is also recognized for historical administration commands.

By default, `dragontravel.destination.*` and `dragontravel.route.*` allow every destination. To restrict a player, remove the wildcard in your permissions manager, then grant individual permissions such as `dragontravel.destination.village` or `dragontravel.route.tour`.

`ITEM` mode consumes only ordinary resources without names, enchantments or other metadata in the 36 storage slots. `VAULT` mode rejects a journey if the economy provider is missing or denies payment. Technical interruptions refund the fare; voluntary cancellation keeps the charge. The recovery journal protects the player's position after a crash. It is not a financial ledger and does not guarantee a refund after forcibly terminating the process. Vault integration requires an installed economy provider and is not covered by the matrix's emerald payment tests.

## Migration and differences from the original plugin

Historical code and its `pom.xml` are retained in `legacy/`. The archived French and German message filenames now contain English translations; the filenames remain compatible with the historical locale selector. `/dt importlegacy` reads the old `plugins/DragonTravel/databases/` directory without modifying it. The import accepts `Stations` and `Flights` with `x%y%z%world` waypoints, without overwriting existing names. Historical homes stored by player name are not automatically assigned to UUIDs: use `/dt sethome`.

This revival includes dragon travel, stations, routes, homes, signs and decorative dragons. Historical Factions, Towny, Heroes and anticheat integrations, binary compatibility with the old API, and every historical option or permission are not reproduced. Addons can use the pre-flight event and the Reforged API. Install only one DragonTravel JAR at a time.

## Building and reproducible tests

Run `mvn verify` with a modern JDK; the resulting bytecode targets Java 8. No version-specific NMS dependency is required. ASM is bundled and relocated inside the JAR; the server supplies its own API.

On Windows, the Python scripts require Python 3.9+ and Node.js for the test clients:

```powershell
python tools/bootstrap.py
./tools/build.ps1
python tools/prepare_servers.py --all
python tools/cache_vanilla.py
New-Item -ItemType Directory -Force .tools/protocol
Copy-Item tools/protocol-package.json .tools/protocol/package.json
npm install --prefix .tools/protocol
python tools/run_matrix.py --workers 3 --crash
```

`bootstrap.py` installs JDKs 8, 16, 17, 21 and 25, plus Maven, into `.tools/`, verifying checksums. The matrix selects the appropriate Java runtime automatically. `prepare_servers.py` downloads published Paper builds and verifies SHA-256. Prerelease version names with suffixes are excluded from `--all`; a beta build with an unsuffixed version remains explicitly identified in the report.

Tests start disposable servers **only on 127.0.0.1**, create clean test worlds, accept the EULA for these runs and stop the processes after checking them. Personal servers are untouched. Test commands are disabled in normal production use: they require the console and `-Ddragontravel.reforged.test=true`.

Server tests check the real entity, mounting, suppressed AI, movement, storage, journeys with a connected player, fares, cancellation and refunds, then restart and reconnect. With `--crash`, the Java process is forcibly killed during a fourth real flight before the recovery journal is checked after restart. The network client also verifies dragon spawn, passenger and movement packets. Travel commands run through their actual handlers with the connected player; console dispatch verifies `/dt` registration. This client does not render a Minecraft window: visual smoothness, camera comfort and interactions with a real server's plugins still require testing in game.

These tests do not establish universal compatibility with third-party plugins, production load or Spigot/CraftBukkit distributions different from the tested Paper builds. Exact builds and diagnostics are recorded; results are not extrapolated from version numbers.

## License and attribution

The two-clause BSD license is preserved in `LICENSE.md` and inside the JAR. Original DragonTravel authors: Philipp "Phiwa" Vesper and Oliver "Seyah" Hayes. Reforged implementation: hash014, 2026. Historical sources remain available in this fork.
