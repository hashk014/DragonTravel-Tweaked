package fr.hash014.dragontravel;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.*;

/** Protection/economy addons can veto a journey before spawn, payment or world transition. */
public final class JourneyStartEvent extends Event implements Cancellable {
  private static final HandlerList HANDLERS = new HandlerList();
  private final Player player;
  private final Location origin, destination;
  private final String label;
  private boolean cancelled;

  public JourneyStartEvent(Player player, Location origin, Location destination, String label) {
    this.player = player;
    this.origin = origin.clone();
    this.destination = destination.clone();
    this.label = label;
  }

  public Player getPlayer() {
    return player;
  }

  public Location getOrigin() {
    return origin.clone();
  }

  public Location getDestination() {
    return destination.clone();
  }

  public String getJourneyName() {
    return label;
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}
