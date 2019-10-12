package com.dre.brewery.api.events.brew;

import com.dre.brewery.Brew;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.meta.ItemMeta;

/*
 * A Brew is starting to be created or modified
 * Usually happens on Filling from cauldron, distilling and aging.
 */
public class BrewBeginModifyEvent extends BrewEvent implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final Type type;
	private boolean cancelled;

	public BrewBeginModifyEvent(Brew brew, ItemMeta meta, Type type) {
		super(brew, meta);
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public enum Type {
		CREATE, // A new Brew is created with arbitrary ways, like the create command
		FILL, // Filled from a Cauldron into a new Brew
		DISTILL, // Distilled in the Brewing stand
		AGE, // Aged in a Barrel
		UNLABEL, // Unlabeling Brew with command
		STATIC, // Making Brew static with command
		UNKNOWN // Unknown modification, unused
	}
}
