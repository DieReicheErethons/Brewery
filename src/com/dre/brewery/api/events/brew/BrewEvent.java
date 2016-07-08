package com.dre.brewery.api.events.brew;

import com.dre.brewery.Brew;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class BrewEvent extends Event {
	protected final Brew brew;

	public BrewEvent(Brew brew) {
		this.brew = brew;
	}

	public Brew getBrew() {
		return brew;
	}
}
