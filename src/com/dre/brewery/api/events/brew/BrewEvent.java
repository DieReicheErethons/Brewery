package com.dre.brewery.api.events.brew;

import com.dre.brewery.Brew;
import org.bukkit.event.Event;
import org.bukkit.inventory.meta.ItemMeta;

public abstract class BrewEvent extends Event {
	protected final Brew brew;
	protected final ItemMeta meta;

	public BrewEvent(Brew brew, ItemMeta meta) {
		this.brew = brew;
		this.meta = meta;
	}

	public Brew getBrew() {
		return brew;
	}

	public ItemMeta getItemMeta() {
		return meta;
	}
}
