package com.dre.brewery.api.events.barrel;

import com.dre.brewery.Barrel;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * A Barrel is being removed. There may have been a BarrelDestroyEvent before this
 * If not, Worldedit, other Plugins etc may be the cause for unexpected removal
 */
public class BarrelRemoveEvent extends BarrelEvent {
	private static final HandlerList handlers = new HandlerList();
	private boolean dropItems;

	public BarrelRemoveEvent(Barrel barrel, boolean dropItems) {
		super(barrel);
		this.dropItems = dropItems;
	}

	public boolean willDropItems() {
		return dropItems;
	}

	/**
	 * @param dropItems Should the Items contained in this Barrel drop to the ground?
	 */
	public void setShouldDropItems(boolean dropItems) {
		this.dropItems = dropItems;
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
}
