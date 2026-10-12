package fr.hash014.dragontravel;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scheduler.BukkitRunnable;

/** Only available from console with -Ddragontravel.reforged.test=true. */
final class ServerTests {
  final DragonTravelReforged p;
  int checks;

  ServerTests(DragonTravelReforged p) {
    this.p = p;
  }

  void check(boolean condition, String label) {
    if (!condition) throw new IllegalStateException(label);
    checks++;
  }

  Location pad(double x, double z) {
    World w = Bukkit.getWorlds().get(0);
    for (int dx = -2; dx <= 2; dx++)
      for (int dz = -2; dz <= 2; dz++) {
        w.getBlockAt((int) x + dx, 79, (int) z + dz).setType(Material.STONE);
        for (int y = 80; y <= 94; y++)
          w.getBlockAt((int) x + dx, y, (int) z + dz).setType(Material.AIR);
      }
    return new Location(w, x + .5, 80, z + .5);
  }

  void command(String sub, String[] args) throws Exception {
    if (sub.equals("crashfixture")) {
      final Player player = Bukkit.getPlayerExact(args[1]);
      if (player == null) throw new IllegalStateException("Crash-test player missing");
      final Location origin = pad(20, 20), target = pad(240, 20);
      p.teleport(player, origin);
      Bukkit.getScheduler()
          .runTaskLater(
              p,
              () -> {
                try {
                  player.saveData();
                  p.cooldowns.clear();
                  final DragonTravelReforged.Journey flight =
                      p.start(player, Collections.singletonList(target), "crash-test", false);
                  Bukkit.getScheduler()
                      .runTaskLater(
                          p,
                          () -> {
                            try {
                              check(
                                  p.journeys.containsKey(player.getUniqueId())
                                      && flight.distance > 2,
                                  "crash fixture actually flying");
                              origin.getWorld().save();
                              // Chunk saves are asynchronous: let the fixture floor
                              // reach disk before deliberately killing the JVM.
                              Bukkit.getScheduler()
                                  .runTaskLater(
                                      p,
                                      () -> {
                                        p.getLogger()
                                            .info("CRASH FLIGHT READY origin=" + flight.origin);
                                      },
                                      60);
                            } catch (Exception ex) {
                              p.getLogger()
                                  .log(java.util.logging.Level.SEVERE, "PLAYERTEST FAIL", ex);
                            }
                          },
                          8);
                } catch (Exception ex) {
                  p.getLogger().log(java.util.logging.Level.SEVERE, "PLAYERTEST FAIL", ex);
                }
              },
              10);
    }
    if (sub.equals("recoverycheck")) {
      Player player = Bukkit.getPlayerExact(args[1]);
      check(player != null, "reconnected player");
      check(
          !p.store.data.isConfigurationSection("recovery." + player.getUniqueId()),
          "journal consumed on rejoin");
      Location departure = new Location(Bukkit.getWorlds().get(0), 20.5, 80, 20.5);
      departure.getChunk().load();
      boolean fallback = !DragonTravelReforged.safe(departure);
      Location expected = fallback ? departure.getWorld().getSpawnLocation() : departure;
      check(
          player.getLocation().distance(expected) < .2,
          "rejoin restores safe location actual=" + player.getLocation());
      check(player.getVehicle() == null, "no orphan vehicle");
      p.getLogger().info("RECOVERY PASS checks=" + checks + " fallback=" + fallback);
    }
    if (sub.equals("selftest")) selftest();
    if (sub.equals("playercheck")) {
      Player player = Bukkit.getPlayerExact(args[1]);
      if (player == null) throw new IllegalStateException("Test player missing");
      playercheck(player);
    }
    if (sub.equals("protocolinfo")) protocolInfo();
    if (sub.equals("persistfixture")) {
      TravelStore.put(p.store.data, "stations.persisted", pad(40, 40));
      p.store.route("persisted", Arrays.asList(pad(44, 40), pad(48, 40)));
      TravelStore.put(p.store.data, "recovery.be6955af-6a68-35cc-b589-af0cdd5c2e0d", pad(20, 20));
      TravelStore.put(
          p.store.data,
          "statues.persisted",
          new Location(Bukkit.getWorlds().get(0), 50.5, 88, 50.5));
      p.store.save();
      p.getLogger().info("PERSISTENCE FIXTURE READY");
    }
    if (sub.equals("persistcheck")) {
      check(TravelStore.get(p.store.data, "stations.persisted") != null, "station restart");
      check(p.store.route("persisted").size() == 2, "route restart");
      check(
          p.store.data.isConfigurationSection("recovery.be6955af-6a68-35cc-b589-af0cdd5c2e0d"),
          "crash recovery restart");
      p.restoreStatues();
      check(
          p.statues.containsKey("persisted") && !p.statues.get("persisted").isDead(),
          "decorative dragon restart");
      p.getLogger().info("PERSISTENCE PASS checks=" + checks);
    }
  }

  void selftest() throws Exception {
    checks = 0;
    Location from = pad(20, 20), to = pad(36, 20);
    p.validateLanding(from);
    checks++;
    FlightPath path =
        FlightPath.cruise(
            DragonTravelReforged.point(from),
            Collections.singletonList(DragonTravelReforged.point(to)),
            90);
    check(path.length() > 16, "cruise length");
    check(path.at(path.length()).distance(DragonTravelReforged.point(to)) < .001, "exact arrival");
    TravelStore.put(p.store.data, "stations.selftest", from);
    p.store.save();
    TravelStore fresh = new TravelStore(p.getDataFolder());
    check(
        TravelStore.get(fresh.data, "stations.selftest").distance(from) < .001, "atomic save/load");
    final Entity dragon = p.adapter.spawn(from, "Test Dragon");
    check(dragon.getType() == EntityType.ENDER_DRAGON, "real dragon");
    check(DragonAdapter.owned(dragon), "ownership");
    check(
        DragonAdapter.handle(dragon).getClass().getName().contains("ControlledDragon"),
        "controlled subclass");
    final Entity passenger = from.getWorld().spawnEntity(from, EntityType.CHICKEN);
    check(p.adapter.mount(dragon, passenger), "entity passenger");
    p.adapter.move(dragon, to);
    check(dragon.getLocation().distance(to) < .01, "native move");
    new BukkitRunnable() {
      int ticks;

      public void run() {
        try {
          ticks++;
          check(
              !dragon.isDead(),
              "dragon remains alive tick="
                  + ticks
                  + " dead="
                  + dragon.isDead()
                  + " health="
                  + ((org.bukkit.entity.LivingEntity) dragon).getHealth()
                  + " at="
                  + dragon.getLocation()
                  + " metadata="
                  + dragon.getMetadata(DragonAdapter.TAG).size());
          check(dragon.getLocation().distance(to) < .1, "AI remains suppressed tick " + ticks);
          check(
              passenger.getVehicle() != null
                  && passenger.getVehicle().getUniqueId().equals(dragon.getUniqueId()),
              "passenger retained");
          check(
              from.clone().subtract(0, 1, 0).getBlock().getType() == Material.STONE,
              "world untouched");
          if (ticks == 30) {
            dragon.remove();
            passenger.remove();
            cancel();
            p.getLogger()
                .info("SELFTEST PASS checks=" + checks + " minecraft=" + Bukkit.getBukkitVersion());
          }
        } catch (Throwable ex) {
          dragon.remove();
          passenger.remove();
          cancel();
          p.getLogger().log(java.util.logging.Level.SEVERE, "SELFTEST FAIL", ex);
        }
      }
    }.runTaskTimer(p, 1, 1);
  }

  void playercheck(final Player player) throws Exception {
    final Location origin = pad(20, 20), landing = pad(36, 20);
    p.teleport(player, origin);
    // Let the client acknowledge its teleport and the server update its
    // watched chunks before immediately mounting a legacy player.
    Bukkit.getScheduler()
        .runTaskLater(
            p,
            () -> {
              try {
                playercheckReady(player, origin, landing);
              } catch (Exception ex) {
                p.getLogger().log(java.util.logging.Level.SEVERE, "PLAYERTEST FAIL", ex);
              }
            },
            10);
  }

  void playercheckReady(final Player player, final Location origin, final Location landing)
      throws Exception {
    checks = 0;
    final PermissionAttachment attachment = player.addAttachment(p);
    attachment.setPermission("dragontravel.use", true);
    attachment.setPermission("dragontravel.home", true);
    attachment.setPermission("dragontravel.admin", true);
    attachment.setPermission("dragontravel.bypasscost", false);
    player.getInventory().clear();
    player.getInventory().addItem(new ItemStack(Material.EMERALD, 20));
    p.getConfig().set("fare.mode", "ITEM");
    p.getConfig().set("fare.amount", 2);
    p.getConfig().set("cooldown-seconds", 0);
    p.getConfig().set("cruise-height", 90);
    p.readSettings();
    attachment.setPermission("dragontravel.use", false);
    boolean blocked = false;
    try {
      p.start(player, Collections.singletonList(landing), "permission-denied", false);
    } catch (IllegalArgumentException ex) {
      blocked = true;
    }
    check(blocked, "permission denial");
    check(emeralds(player) == 20, "denial takes no fare");
    attachment.setPermission("dragontravel.use", true);
    org.bukkit.event.Listener veto =
        new org.bukkit.event.Listener() {
          @org.bukkit.event.EventHandler
          public void before(JourneyStartEvent event) {
            event.setCancelled(true);
          }
        };
    Bukkit.getPluginManager().registerEvents(veto, p);
    try {
      blocked = false;
      try {
        p.start(player, Collections.singletonList(landing), "protection-veto", false);
      } catch (IllegalArgumentException ex) {
        blocked = true;
      }
      check(blocked, "protection event veto");
      check(emeralds(player) == 20, "veto takes no fare");
      check(
          !p.store.data.isConfigurationSection("recovery." + player.getUniqueId()),
          "veto creates no journal");
    } finally {
      org.bukkit.event.HandlerList.unregisterAll(veto);
    }
    // Exercise the actual command handler with a connected player. Recent Paper
    // queues recursive dispatch, so invoke the handler synchronously here.
    commandAs(player, "dt sethome");
    check(
        TravelStore.get(p.store.data, "homes." + player.getUniqueId()) != null, "sethome command");
    TravelStore.put(p.store.data, "stations.testdest", landing);
    p.store.save();
    p.openMenu(player, 0);
    check(
        player.getOpenInventory().getTopInventory().getHolder()
            instanceof DragonTravelReforged.Menu,
        "editable destination menu");
    player.closeInventory();
    commandAs(player, "dt travel testdest");
    final DragonTravelReforged.Journey j = p.journeys.get(player.getUniqueId());
    check(j != null, "travel command");
    check(
        player.getVehicle() != null && player.getVehicle().getType() == EntityType.ENDER_DRAGON,
        "player riding real dragon");
    check(emeralds(player) == 18, "item fare charged exactly once");
    check(
        p.store.data.isConfigurationSection("recovery." + player.getUniqueId()),
        "journal before departure");
    recordDragon(j, true);
    new BukkitRunnable() {
      int tick, phase;
      DragonTravelReforged.Journey current = j;

      public void run() {
        try {
          tick++;
          if (p.journeys.containsKey(player.getUniqueId())) {
            check(
                player.getVehicle() != null
                    && player.getVehicle().getType() == EntityType.ENDER_DRAGON,
                "rider mounted during flight");
            if (tick == 15) {
              check(current.distance > 5, "flight advances");
              check(current.dragon.getLocation().distance(origin) > 1, "dragon physically moves");
              check(player.getLocation().distance(origin) > 1, "player physically moves");
            }
            if (tick > 500) throw new IllegalStateException("Journey never arrives");
            return;
          }
          if (phase == 0) {
            check(player.getVehicle() == null, "dismounted at arrival");
            check(player.getLocation().distance(landing) < .2, "arrival exact");
            check(!current.dragon.isValid(), "dragon removed");
            check(
                !p.store.data.isConfigurationSection("recovery." + player.getUniqueId()),
                "journal cleared on arrival");
            p.cooldowns.clear();
            commandAs(player, "dt home");
            current = p.journeys.get(player.getUniqueId());
            check(current != null, "home journey");
            recordDragon(current, false);
            phase = 1;
            tick = 0;
            return;
          }
          if (phase == 1) {
            check(player.getLocation().distance(origin) < .2, "home arrival");
            p.cooldowns.clear();
            p.store.route("testroute", Arrays.asList(origin.clone().add(8, 10, 0), landing));
            commandAs(player, "dt flight testroute");
            current = p.journeys.get(player.getUniqueId());
            check(current != null, "scenic flight command");
            recordDragon(current, false);
            phase = 2;
            tick = 0;
            return;
          }
          if (phase == 2) {
            check(player.getLocation().distance(landing) < .2, "scenic route arrival");
            p.cooldowns.clear();
            int before = emeralds(player);
            p.start(player, Collections.singletonList(origin), "cancel", false);
            commandAs(player, "dt stop");
            check(!p.journeys.containsKey(player.getUniqueId()), "cancel command");
            check(player.getLocation().distance(landing) < .2, "cancel returns to departure");
            check(emeralds(player) == before - 2, "voluntary cancel keeps fare");
            p.cooldowns.clear();
            before = emeralds(player);
            DragonTravelReforged.Journey failed =
                p.start(player, Collections.singletonList(origin), "failure", false);
            p.finish(failed, false, "Test refund", true);
            check(emeralds(player) == before, "failure refunds once");
            failed.fare.refund(p, player);
            check(emeralds(player) == before, "refund idempotent");
            player.removeAttachment(attachment);
            p.getConfig().set("fare.mode", "FREE");
            cancel();
            p.getLogger().info("PLAYERTEST PASS checks=" + checks);
          }
        } catch (Throwable ex) {
          DragonTravelReforged.Journey active = p.journeys.get(player.getUniqueId());
          if (active != null) p.finish(active, false, "Test failure", true);
          player.removeAttachment(attachment);
          cancel();
          p.getLogger().log(java.util.logging.Level.SEVERE, "PLAYERTEST FAIL", ex);
        }
      }
    }.runTaskTimer(p, 1, 1);
  }

  void commandAs(Player player, String text) {
    p.onCommand(player, p.getCommand("dragontravel"), "dt", text.substring(3).split(" "));
  }

  void recordDragon(DragonTravelReforged.Journey journey, boolean reset) throws IOException {
    java.nio.file.Path file = new File(p.getDataFolder(), "test-dragon-ids.txt").toPath();
    if (reset) Files.write(file, new byte[0]);
    Files.write(
        file,
        (journey.dragon.getEntityId() + "\n").getBytes(StandardCharsets.UTF_8),
        java.nio.file.StandardOpenOption.CREATE,
        java.nio.file.StandardOpenOption.APPEND);
  }

  int emeralds(Player player) {
    int count = 0;
    for (ItemStack s : player.getInventory().getContents())
      if (s != null && s.getType() == Material.EMERALD) count += s.getAmount();
    return count;
  }

  void protocolInfo() throws Exception {
    Class<?> provider = Class.forName("net.minecraft.network.ProtocolInfo$DetailsProvider"),
        detailsClass = Class.forName("net.minecraft.network.ProtocolInfo$Details"),
        visitorClass = Class.forName("net.minecraft.network.ProtocolInfo$Details$PacketVisitor"),
        packetClass = Class.forName("net.minecraft.network.protocol.PacketType");
    StringBuilder json = new StringBuilder("{");
    boolean first = true;
    for (String state : Arrays.asList("login", "configuration", "game")) {
      String prefix =
          state.equals("game") ? "Game" : state.equals("login") ? "Login" : "Configuration";
      Class<?> protocols =
          Class.forName("net.minecraft.network.protocol." + state + "." + prefix + "Protocols");
      for (String flow : Arrays.asList("CLIENTBOUND", "SERVERBOUND")) {
        Object details =
            provider.getMethod("details").invoke(protocols.getField(flow + "_TEMPLATE").get(null));
        final Map<String, Integer> ids = new LinkedHashMap<String, Integer>();
        Object visitor =
            Proxy.newProxyInstance(
                visitorClass.getClassLoader(),
                new Class<?>[] {visitorClass},
                (proxy, method, args) -> {
                  if (method.getName().equals("accept"))
                    ids.put(
                        packetClass.getMethod("id").invoke(args[0]).toString(), (Integer) args[1]);
                  return null;
                });
        detailsClass.getMethod("listPackets", visitorClass).invoke(details, visitor);
        if (!first) json.append(',');
        first = false;
        json.append('"')
            .append(state)
            .append('_')
            .append(flow.toLowerCase(Locale.ROOT))
            .append("\":{");
        boolean entryFirst = true;
        for (Map.Entry<String, Integer> e : ids.entrySet()) {
          if (!entryFirst) json.append(',');
          entryFirst = false;
          json.append('"').append(e.getKey()).append("\":").append(e.getValue());
        }
        json.append('}');
      }
    }
    json.append('}');
    Files.write(
        new File(p.getDataFolder(), "protocol.json").toPath(),
        json.toString().getBytes(StandardCharsets.UTF_8));
    p.getLogger().info("PROTOCOL INFO READY");
  }
}
