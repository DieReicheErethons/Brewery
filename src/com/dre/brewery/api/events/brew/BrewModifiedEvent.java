package com.dre.brewery.api.events.brew;

import com.dre.brewery.Brew;
import com.dre.brewery.lore.BrewLore;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;

/*
 * A Brew has been created or modified
 * Usually happens on Filling from cauldron, distilling and aging.
 * Final Modifications to the Brew or the PotionMeta can be done now
 */
public class BrewModifiedEvent extends BrewEvent {
	private static final HandlerList handlers = new HandlerList();
	private final Type type;


	public BrewModifiedEvent(Brew brew, ItemMeta meta, Type type) {
		super(brew, meta);
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	public BrewLore getLore() {
		return new BrewLore(getBrew(), (PotionMeta) getItemMeta());
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
