package com.dre.brewery.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.potion.PotionEffect;

import java.util.List;

/*
 * Called when the Alcohol in the Player demands its toll
 * These effects are applied regularly to the Player depending on his alcohol level
 * By default it is just one Confusion effect
 * Can be changed or cancelled
 */
public class PlayerAlcEffectEvent extends PlayerEvent implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private List<PotionEffect> effects;
	private boolean cancelled;

	public PlayerAlcEffectEvent(Player who, List<PotionEffect> effects) {
		super(who);
		this.effects = effects;
	}

	public List<PotionEffect> getEffects() {
		return effects;
	}

	public void setEffects(List<PotionEffect> effects) {
		this.effects = effects;
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
