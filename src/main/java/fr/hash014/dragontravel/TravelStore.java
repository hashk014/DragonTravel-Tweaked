package fr.hash014.dragontravel;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/** Atomic data file; the journal is durable BEFORE mounting a player. */
public final class TravelStore {
  private final File file;
  public final YamlConfiguration data = new YamlConfiguration();

  public TravelStore(File folder) throws Exception {
    folder.mkdirs();
    file = new File(folder, "data.yml");
    if (file.exists()) data.load(file);
  }

  public void save() throws IOException {
    Path tmp = file.toPath().resolveSibling("data.yml.tmp");
    Files.write(tmp, data.saveToString().getBytes(StandardCharsets.UTF_8));
    for (int attempt = 0; attempt < 5; attempt++) {
      try {
        try {
          Files.move(
              tmp,
              file.toPath(),
              StandardCopyOption.ATOMIC_MOVE,
              StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
          Files.move(tmp, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return;
      } catch (AccessDeniedException ex) {
        if (attempt == 4) throw ex;
        // Windows antivirus/indexers can hold a freshly replaced file briefly.
        try {
          Thread.sleep(10L * (attempt + 1));
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          throw new IOException("Interrupted atomic save", interrupted);
        }
      }
    }
  }

  public static void put(ConfigurationSection section, String key, Location loc) {
    section.set(key, null);
    section.set(key + ".world", loc.getWorld().getName());
    section.set(key + ".x", loc.getX());
    section.set(key + ".y", loc.getY());
    section.set(key + ".z", loc.getZ());
    section.set(key + ".yaw", loc.getYaw());
    section.set(key + ".pitch", loc.getPitch());
  }

  public static Location get(ConfigurationSection section, String key) {
    if (section == null || !section.isConfigurationSection(key)) return null;
    World w = Bukkit.getWorld(section.getString(key + ".world", ""));
    if (w == null) return null;
    return new Location(
        w,
        section.getDouble(key + ".x"),
        section.getDouble(key + ".y"),
        section.getDouble(key + ".z"),
        (float) section.getDouble(key + ".yaw"),
        (float) section.getDouble(key + ".pitch"));
  }

  public List<String> keys(String path) {
    ConfigurationSection s = data.getConfigurationSection(path);
    List<String> list =
        s == null ? new ArrayList<String>() : new ArrayList<String>(s.getKeys(false));
    Collections.sort(list);
    return list;
  }

  public List<Location> route(String id) {
    List<Location> result = new ArrayList<Location>();
    for (String key : keys("routes." + id + ".points")) {
      Location p = get(data, "routes." + id + ".points." + key);
      if (p == null) throw new IllegalArgumentException("Monde de l'itinéraire indisponible");
      result.add(p);
    }
    return result;
  }

  public void route(String id, List<Location> points) {
    data.set("routes." + id, null);
    for (int i = 0; i < points.size(); i++)
      put(data, "routes." + id + ".points." + String.format(Locale.ROOT, "%03d", i), points.get(i));
  }
}
