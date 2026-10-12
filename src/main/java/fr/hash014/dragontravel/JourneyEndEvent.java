package fr.hash014.dragontravel;

import org.bukkit.entity.Player;
import org.bukkit.event.*;

public final class JourneyEndEvent extends Event {
  private static final HandlerList HANDLERS = new HandlerList();
  private final Player player;
  private final String label, reason;
  private final boolean arrived;

  public JourneyEndEvent(Player player, String label, boolean arrived, String reason) {
    this.player = player;
    this.label = label;
    this.arrived = arrived;
    this.reason = reason;
  }

  public Player getPlayer() {
    return player;
  }

  public String getJourneyName() {
    return label;
  }

  public boolean hasArrived() {
    return arrived;
  }

  public String getReason() {
    return reason;
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}
