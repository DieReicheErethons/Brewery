package com.dre.brewery.api.events.brew;

import com.dre.brewery.Brew;
import com.dre.brewery.lore.BrewLore;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.jetbrains.annotations.NotNull;

/**
 * A Brew has been created or modified.
 * <p>Usually happens on filling from cauldron, distilling and aging.
 * <p>Modifications to the Brew or the PotionMeta can be done now
 * <p>Cancelling reverts the Brew to the state it was before the modification
 */
public class BrewModifyEvent extends BrewEvent implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final Type type;
	private boolean cancelled;


	public BrewModifyEvent(@NotNull Brew brew, @NotNull ItemMeta meta, @NotNull Type type) {
		super(brew, meta);
		this.type = type;
	}

	/**
	 * Get the Type of modification being applied to the Brew.
	 */
	@NotNull
	public Type getType() {
		return type;
	}

	/**
	 * Get the BrewLore to modify lore on the Brew
	 */
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
	 * <p>Modifications to the Brew or ItemMeta will not be applied
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

	/**
	 * The Type of Modification being applied to the Brew.
	 */
	public enum Type {
		/**
		 * A new Brew is created with arbitrary ways, like the create command.
		 * <p>Cancelling this will disallow the creation
		 */
		CREATE,

		/**
		 * Filled from a Cauldron into a new Brew.
		 */
		FILL,

		/**
		 * Distilled in the Brewing stand.
		 */
		DISTILL,

		/**
		 * Aged in a Barrel.
		 */
		AGE,

		/**
		 *  Unlabeling Brew with command.
		 */
		UNLABEL,

		/**
		 * Making Brew static with command.
		 */
		STATIC,

		/**
		 * Sealing the Brew (unlabel &amp; static &amp; stripped) With Command or Machine
		 */
		SEAL,

		/**
		 * Unknown modification, unused.
		 */
		UNKNOWN
	}
}
