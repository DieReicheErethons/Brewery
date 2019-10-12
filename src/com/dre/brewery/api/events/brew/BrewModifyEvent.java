package com.dre.brewery.api.events.brew;

import com.dre.brewery.Brew;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/*
 * A Brew is created or modified
 * Usually happens on Filling from cauldron, distilling and aging.
 */
public class BrewModifyEvent extends BrewEvent implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final Type type;
	private boolean cancelled;
	//private List<Consumer<Brew>> fcts;

	public BrewModifyEvent(Brew brew, Type type) {
		super(brew);
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	/*public void addModification(Consumer<Brew> predicate) {
		fcts.add(predicate);
	}*/

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public enum Type {
		//CREATE, // A new Brew is created with arbitrary ways, like the create command
		FILL, // Filled from a Cauldron into a new Brew
		DISTILL, // Distilled in the Brewing stand
		AGE, // Aged in a Barrel
		UNLABEL, // Unlabeling Brew with command
		STATIC, // Making Brew static with command
		UNKNOWN // Unknown modification, unused
	}
}
