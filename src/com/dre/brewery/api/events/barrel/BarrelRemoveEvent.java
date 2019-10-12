package com.dre.brewery.api.events.barrel;

import com.dre.brewery.Barrel;
import org.bukkit.event.HandlerList;

/*
 * A Barrel is being removed. There may have been a BarrelDestroyEvent before
 * If not, Worldedit, other Plugins etc may be the cause for unexpected removal
 */
public class BarrelRemoveEvent extends BarrelEvent {
	private static final HandlerList handlers = new HandlerList();
	private boolean itemsDrop = true;

	public BarrelRemoveEvent(Barrel barrel) {
		super(barrel);
	}

	public boolean willItemsDrop() {
		return itemsDrop;
	}

	public void setShouldItemsDrop(boolean itemsDrop) {
		this.itemsDrop = itemsDrop;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
