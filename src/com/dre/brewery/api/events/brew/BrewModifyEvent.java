package com.dre.brewery.api.events.brew;

import com.dre.brewery.Brew;
import com.dre.brewery.lore.BrewLore;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.jetbrains.annotations.NotNull;

/**
 * A Brew has been created or modified
 * Usually happens on Filling from cauldron, distilling and aging.
 * Modifications to the Brew or the PotionMeta can be done now
 * Cancelling reverts the Brew to the state it was before the modification
 */
public class BrewModifyEvent extends BrewEvent implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final Type type;
	private boolean cancelled;


	public BrewModifyEvent(@NotNull Brew brew, @NotNull ItemMeta meta, @NotNull Type type) {
		super(brew, meta);
		this.type = type;
	}

	@NotNull
	public Type getType() {
		return type;
	}

	@NotNull
	public BrewLore getLore() {
		return new BrewLore(getBrew(), (PotionMeta) getItemMeta());
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * Setting the Event cancelled cancels all modificatons to the brew.
	 * Modifications to the Brew or ItemMeta will not be applied
	 */
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
