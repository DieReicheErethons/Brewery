package com.dre.brewery.api.events;

import com.dre.brewery.BPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/*
 * The player pukes (throws puke items to the ground)
 * Those items can never be picked up and despawn after the time set in the config
 * Number of items to drop can be changed with count
 */
public class PlayerPukeEvent extends PlayerEvent implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private int count;
	private boolean cancelled;
	private BPlayer bPlayer;


	public PlayerPukeEvent(Player who, int count) {
		super(who);
		this.count = count;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public BPlayer getBPlayer() {
		if (bPlayer == null) {
			bPlayer = BPlayer.get(player);
		}
		return bPlayer;
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
