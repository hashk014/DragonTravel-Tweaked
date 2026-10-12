![DragonTravel Reforged banner showing a rider travelling on a block-style dragon](https://raw.githubusercontent.com/hashk014/DragonTravel-Tweaked/codex/dragontravel-reforged/assets/listing/banner.png)

# DragonTravel Reforged

**Turn the journey into part of the adventure.**

Give your players a ride on a real Ender Dragon. Connect towns with stations, build scenic tours, let players fly home, and add stationary dragons to your server's landmarks.

DragonTravel Reforged revives the original DragonTravel as a Java server plugin, with English messages, an editor available in game, and one JAR tested across 55 Paper versions from **1.7.10 to 26.3**. Players need no client mod or resource pack.

## What you can do

- **Dragon transport:** take off, cruise and land at a station, with progress and estimated arrival time.
- **Scenic routes:** create tours with up to 64 waypoints using commands in game.
- **Destination menu:** browse stations and routes in a paginated inventory menu.
- **Personal homes:** save a home and return to it by dragon.
- **Player travel:** visit another player, with an opt-out for privacy.
- **Travel signs:** create clickable signs for stations and scenic flights.
- **Decorative dragons:** place stationary dragons at hubs and landmarks, with locations restored after restart.
- **Flexible fares:** offer free flights, charge ordinary resources, or use an optional Vault economy provider.
- **Server controls:** configure speed, altitude, cooldowns, distance limits, active dragon limits, disabled worlds and station-only departures.
- **Interrupted journey recovery:** save departure before mounting and return players safely when they reconnect after an interrupted journey. If departure is unsafe, recovery uses world spawn.
- **Protection hooks:** integrate other plugins with cancellable pre-flight and journey-end events.

The travel dragons have destructive AI suppressed. Players receive protection during flight and temporary protection against fall damage afterwards. Landing checks require two clear blocks above solid ground.

## Installation

1. Stop your server and back up its worlds and configuration.
2. Remove any older DragonTravel JAR; both versions use `/dt`.
3. Put `DragonTravel-Reforged-2.0.0.jar` in your server's `plugins/` directory.
4. Start the server, then join as an operator.
5. Stand on solid ground and run `/dt setstat village`.
6. Move to another safe location and run `/dt travel village`.

Open `/dt menu` to choose a destination. Settings are saved in `plugins/DragonTravelReforged/config.yml`. Speed is measured in blocks per second. Travel between worlds is disabled by default.

## Useful commands

```text
/dt help                         Show command help
/dt menu                         Open the destination menu
/dt stations                     List stations
/dt routes                       List scenic routes
/dt travel <station>             Fly to a station
/dt flight <route>               Follow a scenic route
/dt sethome                      Save your home
/dt home                         Fly home
/dt ptravel <player>             Fly to another player
/dt ptoggle                      Allow or block travel to you
/dt status                       Check flight progress
/dt stop                         Cancel and return to departure
```

Administrators can manage stations, routes and decorative dragons:

```text
/dt setstat <name>
/dt remstat <name>
/dt createflight <name>
/dt setwp
/dt remlastwp
/dt saveflight
/dt showflight <name>
/dt remflight <name>
/dt addstatdragon <name> [display-name]
/dt remstatdragon <name>
/dt ctravel <x> <y> <z> [world]
/dt reload
/dt importlegacy
```

Use names containing 1-32 ASCII letters, digits, `_` or `-`. Routes stay in one world. Intermediate waypoints may be in the air; the final waypoint must be a safe landing. Sneaking or `/dt stop` returns the rider to departure.

## Permissions and payments

Basic use, home and player travel permissions are enabled by default. Administration, coordinate travel and fare bypass default to operators. Control individual destinations with `dragontravel.destination.<name>` and `dragontravel.route.<name>`, removing the corresponding wildcard permission when restricting access.

Flights are free by default. Item fares consume only ordinary resources without custom metadata. Voluntary cancellation keeps the fare; technical interruptions refund it. The recovery journal protects player position and does not guarantee a financial refund after a forced process shutdown. Vault mode requires Vault and an installed economy provider.

## Compatibility and testing

**One JAR passed tests on 55 individual Paper versions from 1.7.10 through 26.3.** These were actual server runs with connected network clients, covering dragon mounting, physical travel, arrival, homes, scenic routes, item fares, cancellation, refunds, permission checks, protection vetoes, restart and recovery after forcibly terminating the server during flight. Eight unit tests also passed.

Paper **26.3 build 174 was a beta build** in this matrix. Some historical builds were alpha or beta too. The exact versions, builds, channels and results are recorded in the [compatibility report](https://github.com/hashk014/DragonTravel-Tweaked/blob/codex/dragontravel-reforged/docs/reforged/COMPATIBILITY.md).

The plugin targets Bukkit/Spigot/Paper APIs; runtime results apply to the listed Paper builds. Unlisted patches, other server distributions and third-party plugin combinations are not automatically covered. Use the Java runtime required by your server. **Folia is unsupported.** The network tests do not render a Minecraft window, and Vault with a real economy provider needs separate integration testing.

## Upgrading from DragonTravel

`/dt importlegacy` reads stations and flights from `plugins/DragonTravel/databases/`, preserves those files, and avoids overwriting existing names. Set your home again with `/dt sethome`; historical homes stored by player name are not automatically assigned to UUIDs.

Reforged has a new addon API. Historical Factions, Towny, Heroes and anticheat integrations, the old binary API, and every original setting or permission are not reproduced. See the [documentation](https://github.com/hashk014/DragonTravel-Tweaked/blob/codex/dragontravel-reforged/README.md) for migration details.

## Source, support and credits

- [Source code](https://github.com/hashk014/DragonTravel-Tweaked/tree/codex/dragontravel-reforged)
- [Documentation](https://github.com/hashk014/DragonTravel-Tweaked/blob/codex/dragontravel-reforged/README.md)
- [Report an issue](https://github.com/hashk014/DragonTravel-Tweaked/issues)
- [Addon API](https://github.com/hashk014/DragonTravel-Tweaked/blob/codex/dragontravel-reforged/docs/reforged/API.md)

Original DragonTravel by Philipp "Phiwa" Vesper and Oliver "Seyah" Hayes. Reforged maintained by **hash014**. Released under the original **BSD 2-Clause license**, with historical source and attribution preserved.

Reforged code and this listing were developed with generative AI assistance. The banner and icon are AI-generated promotional illustrations, not gameplay screenshots.
