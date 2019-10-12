package com.dre.brewery.api.events;

import com.dre.brewery.BPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;

/*
 * The Players movement is hindered because of drunkeness
 * Called each time before pushing the Player with the Vector push 10 times
 * The Push Vector can be changed or multiplied
 */
public class PlayerPushEvent extends PlayerEvent implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final BPlayer bPlayer;
	private Vector push;
	private boolean cancelled;

	public PlayerPushEvent(Player who, Vector push, BPlayer bPlayer) {
		super(who);
		this.push = push;
		this.bPlayer = bPlayer;
	}

	public BPlayer getBPlayer() {
		return bPlayer;
	}

	// The Vector in which direction and magnitude the player is Pushed
	// Can be changed directly or through setPush
	public Vector getPush() {
		return push;
	}

	// Set the Push vector, can not be null
	public void setPush(Vector push) {
		if (push == null) {
			throw new NullPointerException("Push Vector is null");
		}
		this.push = push;
	}

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

}
