package com.dre.brewery.api.events.barrel;

import com.dre.brewery.Barrel;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;

public abstract class BarrelEvent extends Event {
	protected final Barrel barrel;

	public BarrelEvent(Barrel barrel) {
		this.barrel = barrel;
	}

	public Barrel getBarrel() {
		return barrel;
	}

	public Inventory getInventory() {
		return barrel.getInventory();
	}

	/**
	 * @return The Spigot Block of the Barrel, usually Sign or a Fence
	 */
	public Block getSpigot() {
		return barrel.getSpigot();
	}
}
