# Reforged API

Use the public API synchronously on Bukkit's server thread. Depend on `DragonTravelReforged` in your addon `plugin.yml` and use the Reforged JAR as a provided compile dependency.

```java
DragonTravelReforged plugin = (DragonTravelReforged)
    Bukkit.getPluginManager().getPlugin("DragonTravelReforged");
plugin.travel(player, destination);
boolean flying = plugin.isTravelling(player);
```

`travel` applies use permission, world/landing checks, limits, cooldown and configured fares. It throws if the trip cannot start. Stations/routes additionally check their own permissions in the command handler. Calls must run on the main server thread.

Protection addons can listen to `JourneyStartEvent` and cancel it before entity creation, charge, journal or cross-world transition. The event exposes the player, departure, final destination and label. Locations are copies. Intermediate scenic waypoints are configured by administrators; this event is not a complete region-path authorization API.

```java
@EventHandler
public void beforeJourney(JourneyStartEvent event) {
    if (!myProtectionAllows(event.getPlayer(), event.getDestination())) {
        event.setCancelled(true);
    }
}
```

`JourneyEndEvent` reports the player, journey name, completion boolean and reason after cleanup. This is a new API; binary compatibility with the historical `eu.phiwa.dragontravel.api` API is not claimed.
