package com.dre.brewery.api.events.barrel;

import com.dre.brewery.Barrel;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a Barrel is created by a Player by placing a Sign.
 * <p>Cancelling this will silently fail the Barrel creation
 */
public class BarrelCreateEvent extends BarrelEvent implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final Player player;
	private boolean cancelled;

	public BarrelCreateEvent(Barrel barrel, Player player) {
		super(barrel);
		this.player = player;
	}

	public Player getPlayer() {
		return player;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	// Required by Bukkit
	public static HandlerList getHandlerList() {
		return handlers;
	}
}
