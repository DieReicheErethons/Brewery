package com.dre.brewery.api.events.brew;

import com.dre.brewery.Brew;
import org.bukkit.event.Event;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public abstract class BrewEvent extends Event {
	protected final Brew brew;
	protected final ItemMeta meta;

	public BrewEvent(@NotNull Brew brew, @NotNull ItemMeta meta) {
		this.brew = brew;
		this.meta = meta;
	}

	@NotNull
	public Brew getBrew() {
		return brew;
	}

	@NotNull
	public ItemMeta getItemMeta() {
		return meta;
	}
}
