package com.dre.brewery.api.events.barrel;

import com.dre.brewery.Barrel;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/*
 * A Barrel is being destroyed by something, may not be by a Player
 * A BarrelRemoveEvent will be called after this, if this is not cancelled
 * Use the BarrelRemoveEvent to monitor any and all barrels being removed in a non cancellable way
*/
public class BarrelDestroyEvent extends BarrelEvent implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final Block broken;
	private final Reason reason;
	private final Player player;
	private boolean cancelled;

	public BarrelDestroyEvent(Barrel barrel, Block broken, Reason reason, Player player) {
		super(barrel);
		this.broken = broken;
		this.player = player;
		this.reason = reason;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public Block getBroken() {
		return broken;
	}

	public Reason getReason() {
		return reason;
	}

	public boolean hasPlayer() {
		return player != null;
	}

	// MAY BE NULL if no Player is involved
	public Player getPlayerOptional() {
		return player;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public enum Reason {
		PLAYER, // A Player Broke the Barrel
		BROKEN, // A Block was broken by something
		BURNED, // A Block burned away
		EXPLODED, // The Barrel exploded somehow
		UNKNOWN // The Barrel was broken somehow else
	}
}
