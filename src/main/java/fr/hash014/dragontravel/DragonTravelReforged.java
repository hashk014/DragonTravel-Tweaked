package fr.hash014.dragontravel;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class DragonTravelReforged extends JavaPlugin implements Listener, TabCompleter {
  TravelStore store;
  DragonAdapter adapter;
  final Map<UUID, Journey> journeys = new HashMap<UUID, Journey>();
  final Map<String, Entity> statues = new HashMap<String, Entity>();
  final Map<UUID, Editor> editors = new HashMap<UUID, Editor>();
  final Map<UUID, Long> cooldowns = new HashMap<UUID, Long>();
  private final Map<UUID, Long> fallGrace = new HashMap<UUID, Long>();
  private final Set<UUID> internalTeleports = new HashSet<UUID>();
  double speed, altitude, maxDistance;
  int maxDragons, maxPoints, maxSeconds, progressTicks, cooldownSeconds;
  boolean shuttingDown;
  ServerTests tests;

  static final class Editor {
    final String id;
    final List<Location> points = new ArrayList<Location>();

    Editor(String id) {
      this.id = id;
    }
  }

  static final class Journey {
    final Player player;
    final Location origin, landing;
    final FlightPath path;
    final Entity dragon;
    final String label;
    final Fare fare;
    double distance;
    int ticks;

    Journey(
        Player p,
        Location origin,
        Location landing,
        FlightPath path,
        Entity dragon,
        String label,
        Fare fare) {
      this.player = p;
      this.origin = origin;
      this.landing = landing;
      this.path = path;
      this.dragon = dragon;
      this.label = label;
      this.fare = fare;
    }
  }

  @Override
  public void onEnable() {
    try {
      if (getServer().getName().toLowerCase(Locale.ROOT).contains("folia"))
        throw new IllegalStateException("Folia region scheduling is not supported");
      saveDefaultConfig();
      readSettings();
      store = new TravelStore(getDataFolder());
      adapter = new DragonAdapter(this);
      getServer().getPluginManager().registerEvents(this, this);
      getCommand("dragontravel").setTabCompleter(this);
      for (World w : Bukkit.getWorlds())
        for (Entity e : w.getEntities()) if (DragonAdapter.owned(e)) e.remove();
      tests = new ServerTests(this);
      Bukkit.getScheduler()
          .runTaskTimer(
              this,
              new Runnable() {
                public void run() {
                  tick();
                }
              },
              1,
              1);
      Bukkit.getScheduler()
          .runTaskLater(
              this,
              new Runnable() {
                public void run() {
                  restoreStatues();
                }
              },
              5);
      getLogger().info("DragonTravel Reforged 2.0.0 — hash014 — " + Bukkit.getBukkitVersion());
    } catch (Exception e) {
      getLogger().log(java.util.logging.Level.SEVERE, "Cannot enable DragonTravel Reforged", e);
      Bukkit.getPluginManager().disablePlugin(this);
    }
  }

  void readSettings() {
    speed = bounded(getConfig().getDouble("speed", 16), 1, 100, "speed");
    altitude = bounded(getConfig().getDouble("cruise-height", 100), 0, 1000, "cruise-height");
    maxDistance = bounded(getConfig().getDouble("max-distance", 30000), 1, 100000, "max-distance");
    maxDragons =
        (int) bounded(getConfig().getInt("max-active-dragons", 16), 1, 128, "max-active-dragons");
    maxPoints = (int) bounded(getConfig().getInt("max-waypoints", 64), 1, 64, "max-waypoints");
    maxSeconds =
        (int)
            bounded(
                getConfig().getInt("max-journey-seconds", 1800), 1, 7200, "max-journey-seconds");
    progressTicks =
        (int) bounded(getConfig().getInt("progress-seconds", 5), 1, 60, "progress-seconds") * 20;
    cooldownSeconds =
        (int) bounded(getConfig().getInt("cooldown-seconds", 10), 0, 3600, "cooldown-seconds");
    bounded(getConfig().getDouble("station-radius", 12), 1, 100, "station-radius");
    String fare = getConfig().getString("fare.mode", "FREE").toUpperCase(Locale.ROOT);
    if (!Arrays.asList("FREE", "ITEM", "VAULT").contains(fare))
      throw new IllegalArgumentException("fare.mode must be FREE, ITEM or VAULT");
    bounded(getConfig().getInt("fare.amount", 1), 0, 2304, "fare.amount");
    bounded(getConfig().getDouble("fare.vault-cost", 10), 0, 1000000, "fare.vault-cost");
    if (fare.equals("ITEM")
        && Material.matchMaterial(getConfig().getString("fare.material", "EMERALD")) == null)
      throw new IllegalArgumentException("Unknown fare material");
  }

  static double bounded(double n, double low, double high, String key) {
    if (!FlightPath.finite(n) || n < low || n > high)
      throw new IllegalArgumentException(key + " must be " + low + ".." + high);
    return n;
  }

  @Override
  public void onDisable() {
    shuttingDown = true;
    if (store != null)
      for (Journey j : new ArrayList<Journey>(journeys.values()))
        finish(j, false, "Server shutdown", true);
    for (Entity e : statues.values()) e.remove();
    statues.clear();
    if (adapter != null) adapter.close();
  }

  void tell(CommandSender sender, String text) {
    sender.sendMessage(ChatColor.DARK_PURPLE + "[DragonTravel] " + ChatColor.GRAY + text);
    if (Boolean.getBoolean("dragontravel.reforged.test") && sender instanceof Player)
      getLogger().info("TEST MESSAGE " + sender.getName() + ": " + text);
  }

  void require(CommandSender sender, String permission) {
    if (!sender.hasPermission(permission)
        && !(permission.equals("dragontravel.admin") && sender.hasPermission("dt.admin")))
      throw new IllegalArgumentException("Required permission: " + permission);
  }

  Player player(CommandSender sender) {
    if (!(sender instanceof Player))
      throw new IllegalArgumentException("This command is only available to players");
    return (Player) sender;
  }

  String arg(String[] args, int index) {
    if (index >= args.length) throw new IllegalArgumentException("Missing argument. /dt help");
    return args[index];
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    try {
      String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
      if (sub.equals("selftest")
          || sub.equals("playercheck")
          || sub.equals("protocolinfo")
          || sub.equals("persistcheck")
          || sub.equals("persistfixture")
          || sub.equals("crashfixture")
          || sub.equals("recoverycheck")) {
        if (!Boolean.getBoolean("dragontravel.reforged.test") || sender instanceof Player)
          throw new IllegalArgumentException("Tests require the console on a test server");
        tests.command(sub, args);
        return true;
      }
      require(sender, "dragontravel.use");
      if (sub.equals("help")) {
        tell(
            sender,
            "/dt menu | travel <station> | flight <route> | sethome | home | stop | status |"
                + " ptravel <player> | ptoggle");
        if (sender.hasPermission("dragontravel.admin"))
          tell(
              sender,
              "Admin: setstat/remstat <name>, createflight <name>, setwp, remlastwp, saveflight,"
                  + " remflight <name>, addstatdragon/remstatdragon <name>, reload, importlegacy,"
                  + " ctravel x y z [world]");
        return true;
      }
      if (sub.equals("travel")) {
        Player p = player(sender);
        String id = FlightPath.id(arg(args, 1));
        requireDestination(p, id, false);
        Location dest = TravelStore.get(store.data, "stations." + id);
        if (dest == null)
          throw new IllegalArgumentException("Station not found or its world is unloaded");
        start(p, Collections.singletonList(dest), id, false);
      } else if (sub.equals("flight")) {
        Player p = player(sender);
        if (args.length > 2) {
          require(sender, "dragontravel.admin");
          p = Bukkit.getPlayerExact(args[2]);
          if (p == null) throw new IllegalArgumentException("Player is offline");
        }
        String id = FlightPath.id(arg(args, 1));
        requireDestination(p, id, true);
        start(p, store.route(id), id, true);
      } else if (sub.equals("home")) {
        require(sender, "dragontravel.home");
        Player p = player(sender);
        Location loc = TravelStore.get(store.data, "homes." + p.getUniqueId());
        if (loc == null) throw new IllegalArgumentException("Set your home with /dt sethome");
        start(p, Collections.singletonList(loc), "home", false);
      } else if (sub.equals("sethome")) {
        require(sender, "dragontravel.home");
        Player p = player(sender);
        if (journeys.containsKey(p.getUniqueId()) || p.isInsideVehicle())
          throw new IllegalArgumentException("Dismount before setting your home");
        validateLanding(p.getLocation());
        TravelStore.put(store.data, "homes." + p.getUniqueId(), p.getLocation());
        store.save();
        tell(p, "Home saved");
      } else if (sub.equals("ptravel")) {
        require(sender, "dragontravel.player");
        Player p = player(sender), target = Bukkit.getPlayerExact(arg(args, 1));
        if (target == null
            || !p.canSee(target)
            || store.data.getBoolean("privacy." + target.getUniqueId(), false))
          throw new IllegalArgumentException(
              "Player is unavailable or has disabled travel to them");
        start(p, Collections.singletonList(target.getLocation()), target.getName(), false);
      } else if (sub.equals("ptoggle")) {
        Player p = player(sender);
        String key = "privacy." + p.getUniqueId();
        boolean deny = !store.data.getBoolean(key, false);
        store.data.set(key, deny);
        store.save();
        tell(
            p,
            deny
                ? "Other players can no longer travel to you"
                : "Other players can now travel to you");
      } else if (sub.equals("stop") || sub.equals("dismount")) {
        Journey j = journeys.get(player(sender).getUniqueId());
        if (j != null) finish(j, false, "Journey cancelled", false);
        else tell(sender, "No flight in progress");
      } else if (sub.equals("status")) {
        Journey j = journeys.get(player(sender).getUniqueId());
        tell(
            sender,
            j == null
                ? "No flight in progress"
                : j.label
                    + " : "
                    + percent(j)
                    + "%, remaining ~"
                    + (int) Math.ceil((j.path.length() - j.distance) / speed)
                    + " s");
      } else if (sub.equals("menu")) openMenu(player(sender), 0);
      else if (sub.equals("showstats") || sub.equals("stations"))
        tell(sender, "Stations: " + store.keys("stations"));
      else if (sub.equals("showflights") || sub.equals("routes"))
        tell(sender, "Routes: " + store.keys("routes"));
      else if (sub.equals("showflight"))
        tell(sender, "Waypoints: " + store.route(FlightPath.id(arg(args, 1))).size());
      else if (sub.equals("ctravel")) {
        require(sender, "dragontravel.coords");
        Player p = player(sender);
        World w = args.length > 4 ? Bukkit.getWorld(args[4]) : p.getWorld();
        if (w == null) throw new IllegalArgumentException("World not found");
        Location l =
            new Location(
                w,
                Double.parseDouble(arg(args, 1)),
                Double.parseDouble(arg(args, 2)),
                Double.parseDouble(arg(args, 3)));
        start(p, Collections.singletonList(l), "coordinates", false);
      } else {
        require(sender, "dragontravel.admin");
        if (sub.equals("setstat")) {
          String id = FlightPath.id(arg(args, 1));
          Location l = player(sender).getLocation();
          validateLanding(l);
          TravelStore.put(store.data, "stations." + id, l);
          store.save();
          tell(sender, "Station saved: " + id);
        } else if (sub.equals("remstat")) {
          store.data.set("stations." + FlightPath.id(arg(args, 1)), null);
          store.save();
          tell(sender, "Station removed");
        } else if (sub.equals("createflight")) {
          Player p = player(sender);
          editors.put(p.getUniqueId(), new Editor(FlightPath.id(arg(args, 1))));
          tell(p, "Editor opened. Use /dt setwp at each waypoint, then /dt saveflight");
        } else if (sub.equals("setwp") || sub.equals("remlastwp") || sub.equals("saveflight")) {
          Player p = player(sender);
          Editor e = editors.get(p.getUniqueId());
          if (e == null) throw new IllegalArgumentException("Use /dt createflight <name> first");
          if (sub.equals("setwp")) {
            if (e.points.size() >= maxPoints)
              throw new IllegalArgumentException("Waypoint limit reached");
            if (!e.points.isEmpty() && !e.points.get(0).getWorld().equals(p.getWorld()))
              throw new IllegalArgumentException("A route must remain in one world");
            validatePoint(p.getLocation());
            e.points.add(p.getLocation());
          } else if (sub.equals("remlastwp")) {
            if (!e.points.isEmpty()) e.points.remove(e.points.size() - 1);
          } else {
            if (e.points.isEmpty()) throw new IllegalArgumentException("No waypoints");
            validateLanding(e.points.get(e.points.size() - 1));
            store.route(e.id, e.points);
            store.save();
            editors.remove(p.getUniqueId());
          }
          tell(p, e.points.size() + " waypoints");
        } else if (sub.equals("remflight")) {
          store.data.set("routes." + FlightPath.id(arg(args, 1)), null);
          store.save();
          tell(sender, "Route removed");
        } else if (sub.equals("addstatdragon")) {
          String id = FlightPath.id(arg(args, 1));
          if (!store.data.isConfigurationSection("statues." + id)
              && store.keys("statues").size() >= maxDragons)
            throw new IllegalArgumentException("Decorative dragon limit reached");
          Location l = player(sender).getLocation();
          validatePoint(l);
          TravelStore.put(store.data, "statues." + id, l);
          store.save();
          Entity old = statues.remove(id);
          if (old != null) old.remove();
          statues.put(
              id,
              adapter.spawn(l, args.length > 2 ? args[2] : getConfig().getString("dragon-name")));
          tell(sender, "Decorative dragon saved");
        } else if (sub.equals("remstatdragon")) {
          String id = FlightPath.id(arg(args, 1));
          Entity e = statues.remove(id);
          if (e != null) e.remove();
          store.data.set("statues." + id, null);
          store.save();
          tell(sender, "Decorative dragon removed");
        } else if (sub.equals("reload")) {
          String previous = getConfig().saveToString();
          reloadConfig();
          try {
            readSettings();
          } catch (Exception ex) {
            getConfig().loadFromString(previous);
            readSettings();
            throw ex;
          }
          tell(sender, "Configuration reloaded");
        } else if (sub.equals("importlegacy")) importLegacy(sender);
        else if (sub.equals("remdragons")) {
          for (Journey j : new ArrayList<Journey>(journeys.values()))
            finish(j, false, "Cancelled by an administrator", true);
          tell(sender, "Flights stopped");
        } else throw new IllegalArgumentException("Unknown command. /dt help");
      }
    } catch (Exception ex) {
      tell(sender, ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
      if (!(ex instanceof IllegalArgumentException))
        getLogger().log(java.util.logging.Level.WARNING, "Command failed", ex);
    }
    return true;
  }

  void requireDestination(Player p, String id, boolean route) {
    String prefix = route ? "route" : "destination";
    if (!p.hasPermission("dragontravel." + prefix + ".*")
        && !p.hasPermission("dragontravel." + prefix + "." + id)
        && !p.hasPermission("dragontravel.admin"))
      throw new IllegalArgumentException("You do not have permission to use this destination");
  }

  void validatePoint(Location l) {
    if (l == null || l.getWorld() == null) throw new IllegalArgumentException("World unavailable");
    new FlightPath.Point(l.getX(), l.getY(), l.getZ());
    if (Math.abs(l.getX()) > 29999900
        || Math.abs(l.getZ()) > 29999900
        || l.getY() < minHeight(l.getWorld()) + 2
        || l.getY() > l.getWorld().getMaxHeight() - 12)
      throw new IllegalArgumentException("Coordinates outside the world limits");
    if (getConfig().getStringList("disabled-worlds").contains(l.getWorld().getName()))
      throw new IllegalArgumentException("Travel is disabled in this world");
    try {
      Object border = l.getWorld().getClass().getMethod("getWorldBorder").invoke(l.getWorld());
      if (!(Boolean) border.getClass().getMethod("isInside", Location.class).invoke(border, l))
        throw new IllegalArgumentException("Destination outside the world border");
    } catch (NoSuchMethodException ignored) {
    } catch (ReflectiveOperationException e) {
      throw new IllegalArgumentException("Cannot check the world border", e);
    }
  }

  static int minHeight(World w) {
    try {
      return (Integer) w.getClass().getMethod("getMinHeight").invoke(w);
    } catch (ReflectiveOperationException e) {
      return 0;
    }
  }

  void validateLanding(Location l) {
    validatePoint(l);
    l.getChunk().load();
    if (getConfig().getBoolean("require-safe-landing", true) && !safe(l))
      throw new IllegalArgumentException(
          "Unsafe landing: two clear blocks above solid ground are required");
  }

  static boolean safe(Location l) {
    Material floor = l.clone().subtract(0, 1, 0).getBlock().getType();
    String n = floor.name();
    return floor.isSolid()
        && !n.contains("MAGMA")
        && !n.contains("CACTUS")
        && !n.contains("CAMPFIRE")
        && l.getBlock().getType() == Material.AIR
        && l.clone().add(0, 1, 0).getBlock().getType() == Material.AIR;
  }

  static FlightPath.Point point(Location l) {
    return new FlightPath.Point(l.getX(), l.getY(), l.getZ());
  }

  Journey start(Player p, List<Location> stops, String label, boolean scenic) throws Exception {
    require(p, "dragontravel.use");
    if (shuttingDown
        || journeys.containsKey(p.getUniqueId())
        || p.isInsideVehicle()
        || p.isDead()
        || p.isSleeping()) throw new IllegalArgumentException("Cannot start a journey right now");
    if (journeys.size() >= maxDragons) throw new IllegalArgumentException("All dragons are busy");
    Long next = cooldowns.get(p.getUniqueId());
    if (next != null && System.currentTimeMillis() < next)
      throw new IllegalArgumentException("Wait before starting another journey");
    if (store.data.isConfigurationSection("recovery." + p.getUniqueId())) {
      recover(p);
      if (store.data.isConfigurationSection("recovery." + p.getUniqueId()))
        throw new IllegalArgumentException("Your previous flight must be recovered first");
    }
    if (stops.isEmpty() || stops.size() > maxPoints)
      throw new IllegalArgumentException("Invalid waypoint count");
    Location origin = p.getLocation().clone(), landing = stops.get(stops.size() - 1).clone();
    validatePoint(origin);
    validateLanding(landing);
    World world = stops.get(0).getWorld();
    for (Location l : stops) {
      validatePoint(l);
      if (!world.equals(l.getWorld()))
        throw new IllegalArgumentException("All waypoints must remain in the same world");
    }
    if (getConfig().getBoolean("station-only", false) && !p.hasPermission("dragontravel.admin")) {
      boolean nearby = false;
      double radius = getConfig().getDouble("station-radius", 12);
      for (String id : store.keys("stations")) {
        Location l = TravelStore.get(store.data, "stations." + id);
        if (l != null && l.getWorld().equals(origin.getWorld()) && l.distance(origin) <= radius)
          nearby = true;
      }
      if (!nearby) throw new IllegalArgumentException("Go to a station to take off");
    }
    boolean cross = !origin.getWorld().equals(world);
    if (cross && !getConfig().getBoolean("allow-cross-world", false))
      throw new IllegalArgumentException("Travel between worlds is disabled");
    Location departure = cross ? world.getSpawnLocation().clone() : origin.clone();
    validatePoint(departure);
    List<FlightPath.Point> points = new ArrayList<FlightPath.Point>();
    for (Location l : stops) points.add(point(l));
    FlightPath path =
        scenic
            ? new FlightPath(join(point(departure), points))
            : FlightPath.cruise(
                point(departure), points, Math.min(altitude, world.getMaxHeight() - 16));
    if (path.length() > maxDistance || path.length() / speed > maxSeconds)
      throw new IllegalArgumentException("Journey is too long");
    JourneyStartEvent event = new JourneyStartEvent(p, origin, landing, label);
    Bukkit.getPluginManager().callEvent(event);
    if (event.isCancelled())
      throw new IllegalArgumentException("Journey denied by server protection");
    String key = "recovery." + p.getUniqueId();
    TravelStore.put(store.data, key, origin);
    store.save();
    Entity dragon = null;
    Fare fare = null;
    try {
      if (cross && !teleport(p, departure))
        throw new IllegalArgumentException("World transition denied");
      dragon = adapter.spawn(departure, getConfig().getString("dragon-name", "Reforged Dragon"));
      fare = Fare.charge(this, p);
      if (!adapter.mount(dragon, p)
          || p.getVehicle() == null
          || !p.getVehicle().getUniqueId().equals(dragon.getUniqueId()))
        throw new IllegalArgumentException("Mounting the dragon was denied");
      Journey j = new Journey(p, origin, landing, path, dragon, label, fare);
      journeys.put(p.getUniqueId(), j);
      tell(
          p,
          "Taking off for "
              + label
              + " (~"
              + (int) Math.ceil(path.length() / speed)
              + " s). Use /dt stop to return to the departure point");
      return j;
    } catch (Exception e) {
      if (dragon != null) dragon.remove();
      if (fare != null) fare.refund(this, p);
      if (teleport(p, origin)) {
        store.data.set(key, null);
        store.save();
      }
      throw e;
    }
  }

  static List<FlightPath.Point> join(FlightPath.Point from, List<FlightPath.Point> stops) {
    List<FlightPath.Point> list = new ArrayList<FlightPath.Point>();
    list.add(from);
    list.addAll(stops);
    return list;
  }

  void tick() {
    adapter.prune();
    for (Journey j : new ArrayList<Journey>(journeys.values())) {
      try {
        j.ticks++;
        if (!j.player.isOnline()
            || j.dragon.isDead()
            || j.player.getVehicle() == null
            || !j.player.getVehicle().getUniqueId().equals(j.dragon.getUniqueId())
            || j.player.isSneaking()) {
          finish(j, false, "Flight interrupted", true);
          continue;
        }
        if (j.ticks > maxSeconds * 20) {
          finish(j, false, "Maximum journey duration reached", true);
          continue;
        }
        j.distance = Math.min(j.path.length(), j.distance + speed / 20);
        FlightPath.Point pos = j.path.at(j.distance),
            ahead = j.path.at(Math.min(j.path.length(), j.distance + 1));
        Location l = new Location(j.dragon.getWorld(), pos.x, pos.y, pos.z);
        l.setYaw((float) (Math.toDegrees(Math.atan2(-(ahead.x - pos.x), ahead.z - pos.z)) + 180));
        l.getChunk().load();
        validatePoint(l);
        Location rider = l.clone().add(0, 6, 0);
        rider.setYaw(j.player.getLocation().getYaw());
        rider.setPitch(j.player.getLocation().getPitch());
        if (rider.getBlock().getType().isSolid()
            || rider.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
          finish(j, false, "Obstacle on the route", true);
          continue;
        }
        adapter.move(j.dragon, l);
        adapter.move(j.player, rider);
        if (j.distance >= j.path.length()) finish(j, true, "Arrived at " + j.label, false);
        else if (j.ticks % progressTicks == 0)
          tell(
              j.player,
              j.label
                  + " : "
                  + percent(j)
                  + "% — ~"
                  + (int) Math.ceil((j.path.length() - j.distance) / speed)
                  + " s");
      } catch (Exception e) {
        getLogger().log(java.util.logging.Level.WARNING, "Journey failed", e);
        finish(j, false, "Flight stopped safely", true);
      }
    }
  }

  int percent(Journey j) {
    return j.path.length() == 0 ? 100 : (int) Math.min(100, j.distance * 100 / j.path.length());
  }

  void finish(Journey j, boolean arrived, String reason, boolean refund) {
    if (journeys.remove(j.player.getUniqueId()) == null) return;
    j.player.leaveVehicle();
    j.dragon.remove();
    Location dest = arrived ? j.landing : j.origin;
    try {
      if (arrived) validateLanding(dest);
      if (j.player.isOnline()) {
        if (!teleport(j.player, dest))
          throw new IllegalArgumentException("Dismount teleport denied");
        store.data.set("recovery." + j.player.getUniqueId(), null);
        store.save();
      }
    } catch (Exception ex) {
      arrived = false;
      refund = true;
      reason = "Landing cancelled; returning to the departure point";
      if (j.player.isOnline() && teleport(j.player, j.origin))
        try {
          store.data.set("recovery." + j.player.getUniqueId(), null);
          store.save();
        } catch (IOException e) {
          getLogger().warning(e.toString());
        }
    }
    if (refund)
      try {
        j.fare.refund(this, j.player);
      } catch (Exception e) {
        getLogger()
            .log(
                java.util.logging.Level.SEVERE,
                "Fare refund failed for " + j.player.getUniqueId(),
                e);
      }
    fallGrace.put(j.player.getUniqueId(), System.currentTimeMillis() + 5000);
    cooldowns.put(j.player.getUniqueId(), System.currentTimeMillis() + cooldownSeconds * 1000L);
    if (j.player.isOnline()) tell(j.player, reason);
    Bukkit.getPluginManager().callEvent(new JourneyEndEvent(j.player, j.label, arrived, reason));
  }

  /** Synchronous Bukkit API; call from the server thread. */
  public void travel(Player player, Location destination) throws Exception {
    if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Server thread required");
    start(player, Collections.singletonList(destination), "api", false);
  }

  public boolean isTravelling(Player player) {
    return journeys.containsKey(player.getUniqueId());
  }

  boolean teleport(Player p, Location location) {
    internalTeleports.add(p.getUniqueId());
    try {
      return p.teleport(location);
    } finally {
      internalTeleports.remove(p.getUniqueId());
    }
  }

  void recover(Player p) {
    String key = "recovery." + p.getUniqueId();
    if (!store.data.isConfigurationSection(key)) return;
    Location origin = TravelStore.get(store.data, key);
    if (origin == null) {
      tell(p, "Departure world unavailable: contact an administrator");
      return;
    }
    origin.getChunk().load();
    if (!safe(origin)) origin = origin.getWorld().getSpawnLocation();
    p.leaveVehicle();
    if (teleport(p, origin))
      try {
        store.data.set(key, null);
        store.save();
        tell(p, "Interrupted journey recovered: returned to the departure point");
      } catch (IOException e) {
        getLogger().warning(e.toString());
      }
  }

  void restoreStatues() {
    for (String id : store.keys("statues")) {
      try {
        Location l = TravelStore.get(store.data, "statues." + id);
        if (l != null && !statues.containsKey(id) && statues.size() < maxDragons) {
          validatePoint(l);
          statues.put(id, adapter.spawn(l, getConfig().getString("dragon-name")));
        }
      } catch (Exception e) {
        getLogger().warning("Decorative dragon " + id + ": " + e);
      }
    }
  }

  @EventHandler
  public void join(PlayerJoinEvent e) {
    final Player p = e.getPlayer();
    Bukkit.getScheduler()
        .runTaskLater(
            this,
            new Runnable() {
              public void run() {
                recover(p);
              }
            },
            10);
  }

  @EventHandler
  public void quit(PlayerQuitEvent e) {
    Journey j = journeys.get(e.getPlayer().getUniqueId());
    if (j != null) finish(j, false, "Player disconnected", true);
    UUID id = e.getPlayer().getUniqueId();
    editors.remove(id);
    cooldowns.remove(id);
    fallGrace.remove(id);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void teleported(PlayerTeleportEvent e) {
    if (!internalTeleports.contains(e.getPlayer().getUniqueId())
        && journeys.containsKey(e.getPlayer().getUniqueId())) {
      e.setCancelled(true);
      tell(e.getPlayer(), "Use /dt stop before teleporting");
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void damage(EntityDamageEvent e) {
    if (DragonAdapter.owned(e.getEntity())) e.setCancelled(true);
    if (e.getEntity() instanceof Player) {
      UUID id = e.getEntity().getUniqueId();
      Long grace = fallGrace.get(id);
      if (journeys.containsKey(id)
          || (e.getCause() == EntityDamageEvent.DamageCause.FALL
              && grace != null
              && grace > System.currentTimeMillis())) e.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void attack(EntityDamageByEntityEvent e) {
    if (DragonAdapter.owned(e.getDamager())) e.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void blocks(EntityChangeBlockEvent e) {
    if (DragonAdapter.owned(e.getEntity())) e.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void explode(EntityExplodeEvent e) {
    if (DragonAdapter.owned(e.getEntity())) {
      e.blockList().clear();
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void chunk(ChunkLoadEvent e) {
    for (Entity entity : e.getChunk().getEntities())
      if (DragonAdapter.owned(entity) && !active(entity)) entity.remove();
  }

  @EventHandler(ignoreCancelled = true)
  public void unload(ChunkUnloadEvent e) {
    if (adapter.held(e.getChunk())) e.setCancelled(true);
  }

  boolean active(Entity e) {
    for (org.bukkit.metadata.MetadataValue value : e.getMetadata(DragonAdapter.TAG))
      if (value.getOwningPlugin() == this) return true;
    return false;
  }

  @EventHandler(ignoreCancelled = true)
  public void signCreate(SignChangeEvent e) {
    if (e.getLine(0).equalsIgnoreCase("[DragonTravel]") || e.getLine(0).equalsIgnoreCase("[DTR]")) {
      if (!e.getPlayer().hasPermission("dragontravel.admin")) {
        e.setCancelled(true);
        return;
      }
      e.setLine(0, ChatColor.DARK_PURPLE + "[DTR]");
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void interact(PlayerInteractEvent e) {
    if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
        || e.getClickedBlock() == null
        || !(e.getClickedBlock().getState() instanceof Sign)) return;
    Sign s = (Sign) e.getClickedBlock().getState();
    String title = ChatColor.stripColor(s.getLine(0));
    if (!title.equalsIgnoreCase("[DTR]") && !title.equalsIgnoreCase("[DragonTravel]")) return;
    try {
      String action = s.getLine(1).toLowerCase(Locale.ROOT), id = FlightPath.id(s.getLine(2));
      boolean route = action.equals("flight");
      if (!route && !action.equals("travel")) throw new IllegalArgumentException("Invalid sign");
      requireDestination(e.getPlayer(), id, route);
      Location l = TravelStore.get(store.data, "stations." + id);
      start(e.getPlayer(), route ? store.route(id) : Collections.singletonList(l), id, route);
    } catch (Exception ex) {
      tell(e.getPlayer(), ex.getMessage());
    }
    e.setCancelled(true);
  }

  static final class Menu implements InventoryHolder {
    Inventory inventory;
    final List<String> entries;
    final int page;

    Menu(List<String> entries, int page) {
      this.entries = entries;
      this.page = page;
    }

    public Inventory getInventory() {
      return inventory;
    }
  }

  void openMenu(Player p, int page) {
    List<String> entries = new ArrayList<String>();
    for (String id : store.keys("stations")) if (allowed(p, id, false)) entries.add("travel " + id);
    for (String id : store.keys("routes")) if (allowed(p, id, true)) entries.add("flight " + id);
    page = Math.max(0, Math.min(page, Math.max(0, (entries.size() - 1) / 45)));
    Menu menu = new Menu(entries, page);
    menu.inventory = Bukkit.createInventory(menu, 54, "DragonTravel Reforged");
    for (int i = page * 45; i < Math.min(entries.size(), (page + 1) * 45); i++) {
      ItemStack item = new ItemStack(Material.ENDER_PEARL);
      org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
      meta.setDisplayName(ChatColor.LIGHT_PURPLE + entries.get(i));
      meta.setLore(Arrays.asList("Click to travel on a dragon"));
      item.setItemMeta(meta);
      menu.inventory.setItem(i - page * 45, item);
    }
    if (page > 0) menu.inventory.setItem(45, new ItemStack(Material.ARROW));
    if ((page + 1) * 45 < entries.size()) menu.inventory.setItem(53, new ItemStack(Material.ARROW));
    p.openInventory(menu.inventory);
  }

  boolean allowed(Player p, String id, boolean route) {
    try {
      requireDestination(p, id, route);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @EventHandler
  public void menuClick(InventoryClickEvent e) {
    if (!(e.getView().getTopInventory().getHolder() instanceof Menu)) return;
    e.setCancelled(true);
    if (!(e.getWhoClicked() instanceof Player)) return;
    Player p = (Player) e.getWhoClicked();
    Menu m = (Menu) e.getView().getTopInventory().getHolder();
    int slot = e.getRawSlot();
    if (slot == 45) {
      openMenu(p, m.page - 1);
      return;
    }
    if (slot == 53) {
      openMenu(p, m.page + 1);
      return;
    }
    int index = m.page * 45 + slot;
    if (slot < 0 || slot >= 45 || index >= m.entries.size()) return;
    p.closeInventory();
    onCommand(p, getCommand("dragontravel"), "dt", m.entries.get(index).split(" "));
  }

  @EventHandler
  public void menuDrag(InventoryDragEvent e) {
    if (e.getView().getTopInventory().getHolder() instanceof Menu) e.setCancelled(true);
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] args) {
    List<String> all = new ArrayList<String>();
    if (args.length == 1) {
      all.addAll(
          Arrays.asList(
              "help",
              "travel",
              "flight",
              "menu",
              "sethome",
              "home",
              "stop",
              "status",
              "stations",
              "routes",
              "ptravel",
              "ptoggle"));
      if (sender.hasPermission("dragontravel.admin"))
        all.addAll(
            Arrays.asList(
                "setstat",
                "remstat",
                "createflight",
                "setwp",
                "saveflight",
                "remlastwp",
                "remflight",
                "addstatdragon",
                "remstatdragon",
                "reload",
                "importlegacy",
                "ctravel"));
    } else if (args.length == 2) {
      if (args[0].equalsIgnoreCase("travel") || args[0].equalsIgnoreCase("remstat"))
        all.addAll(store.keys("stations"));
      if (args[0].equalsIgnoreCase("flight") || args[0].equalsIgnoreCase("remflight"))
        all.addAll(store.keys("routes"));
    }
    List<String> result = new ArrayList<String>();
    String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
    for (String s : all) if (s.startsWith(prefix)) result.add(s);
    return result;
  }

  void importLegacy(CommandSender sender) throws Exception {
    File old = new File(getDataFolder().getParentFile(), "DragonTravel/databases");
    if (!old.isDirectory())
      throw new IllegalArgumentException("Directory plugins/DragonTravel/databases not found");
    int stations = 0, routes = 0;
    YamlConfiguration s = new YamlConfiguration();
    File f = new File(old, "stations.yml");
    if (f.exists()) s.load(f);
    ConfigurationSection root = s.getConfigurationSection("Stations");
    if (root != null)
      for (String id : root.getKeys(false)) {
        String name = FlightPath.id(id);
        if (store.data.isConfigurationSection("stations." + name)) continue;
        Location l = TravelStore.get(root, id);
        if (l != null) {
          validateLanding(l);
          TravelStore.put(store.data, "stations." + name, l);
          stations++;
        }
      }
    YamlConfiguration r = new YamlConfiguration();
    f = new File(old, "flights.yml");
    if (f.exists()) r.load(f);
    root = r.getConfigurationSection("Flights");
    if (root != null)
      for (String id : root.getKeys(false)) {
        String name = FlightPath.id(id);
        if (store.data.isConfigurationSection("routes." + name)) continue;
        List<Location> points = new ArrayList<Location>();
        for (String text : root.getStringList(id + ".waypoints")) {
          String[] v = text.split("%");
          if (v.length != 4) throw new IllegalArgumentException("Invalid waypoint: " + id);
          World w = Bukkit.getWorld(v[3]);
          if (w == null) throw new IllegalArgumentException("Missing world: " + v[3]);
          Location l =
              new Location(
                  w, Double.parseDouble(v[0]), Double.parseDouble(v[1]), Double.parseDouble(v[2]));
          validatePoint(l);
          points.add(l);
        }
        if (!points.isEmpty() && points.size() <= maxPoints) {
          validateLanding(points.get(points.size() - 1));
          store.route(name, points);
          routes++;
        }
      }
    store.save();
    tell(
        sender,
        "Imported: " + stations + " stations, " + routes + " routes. Legacy files preserved.");
  }
}
