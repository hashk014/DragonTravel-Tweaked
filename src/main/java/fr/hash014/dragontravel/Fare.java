package fr.hash014.dragontravel;

import java.lang.reflect.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Explicit charge/refund transaction; missing economy never turns a paid trip free. */
final class Fare {
  private final List<ItemStack> items = new ArrayList<ItemStack>();
  private Object economy;
  private Class<?> economyApi;
  private double money;
  private boolean refunded;

  static Fare charge(DragonTravelReforged plugin, Player player) throws Exception {
    Fare f = new Fare();
    if (player.hasPermission("dragontravel.bypasscost")) return f;
    String mode = plugin.getConfig().getString("fare.mode", "FREE").toUpperCase(Locale.ROOT);
    if (mode.equals("ITEM")) {
      Material material =
          Material.matchMaterial(plugin.getConfig().getString("fare.material", "EMERALD"));
      int count = plugin.getConfig().getInt("fare.amount", 1), available = 0;
      ItemStack[] contents = player.getInventory().getContents();
      // Only ordinary resource stacks qualify; named/enhanced items are never consumed.
      for (int i = 0; i < Math.min(36, contents.length); i++)
        if (contents[i] != null && contents[i].getType() == material && !contents[i].hasItemMeta())
          available += contents[i].getAmount();
      if (available < count) throw new IllegalArgumentException("Fare: " + count + " " + material);
      for (int i = 0; i < Math.min(36, contents.length) && count > 0; i++) {
        ItemStack s = contents[i];
        if (s == null || s.getType() != material || s.hasItemMeta()) continue;
        int n = Math.min(count, s.getAmount());
        ItemStack paid = s.clone();
        paid.setAmount(n);
        f.items.add(paid);
        count -= n;
        if (n == s.getAmount()) player.getInventory().setItem(i, null);
        else {
          s.setAmount(s.getAmount() - n);
          player.getInventory().setItem(i, s);
        }
      }
    } else if (mode.equals("VAULT")) {
      try {
        f.economyApi = Class.forName("net.milkbowl.vault.economy.Economy");
      } catch (ClassNotFoundException ex) {
        throw new IllegalArgumentException("Vault fares are enabled, but Vault is not installed");
      }
      org.bukkit.plugin.RegisteredServiceProvider<?> registration =
          plugin.getServer().getServicesManager().getRegistration(f.economyApi);
      if (registration == null)
        throw new IllegalArgumentException("No Vault economy provider available");
      f.economy = registration.getProvider();
      double amount = plugin.getConfig().getDouble("fare.vault-cost", 10);
      Object response = f.transaction("withdrawPlayer", player, amount);
      if (!(Boolean) response.getClass().getMethod("transactionSuccess").invoke(response))
        throw new IllegalArgumentException("Payment denied");
      f.money = amount;
    }
    return f;
  }

  private Object transaction(String method, Player player, double amount) throws Exception {
    try {
      return economyApi
          .getMethod(method, OfflinePlayer.class, double.class)
          .invoke(economy, player, amount);
    } catch (NoSuchMethodException ex) {
      return economyApi
          .getMethod(method, String.class, double.class)
          .invoke(economy, player.getName(), amount);
    }
  }

  void refund(DragonTravelReforged plugin, Player player) throws Exception {
    if (refunded) return;
    if (economy != null && money > 0) {
      Object response = transaction("depositPlayer", player, money);
      if (!(Boolean) response.getClass().getMethod("transactionSuccess").invoke(response))
        throw new IllegalStateException("Vault refund rejected");
    }
    for (ItemStack item : items)
      for (ItemStack leftover : player.getInventory().addItem(item).values())
        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
    refunded = true;
  }
}
