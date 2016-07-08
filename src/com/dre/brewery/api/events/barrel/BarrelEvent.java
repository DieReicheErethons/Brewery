package com.dre.brewery.api.events.barrel;

import com.dre.brewery.Barrel;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
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

	public Block getSpigot() {
		return barrel.getSpigot();
	}
}
