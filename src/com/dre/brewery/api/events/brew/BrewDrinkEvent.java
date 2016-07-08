package com.dre.brewery.api.events.brew;

import com.dre.brewery.BEffect;
import com.dre.brewery.BPlayer;
import com.dre.brewery.Brew;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.potion.PotionEffect;

import java.util.List;

/*
 * A Player Drinks a Brew
 * The amount of alcohol and quality that will be added to the player can be get/set here
 * If cancelled the drinking will fail silently
 */
public class BrewDrinkEvent extends BrewEvent implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final Player player;
	private final BPlayer bPlayer;
	private int alc;
	private int quality;
	private boolean cancelled;

	public BrewDrinkEvent(Brew brew, Player player, BPlayer bPlayer) {
		super(brew);
		this.player = player;
		this.bPlayer = bPlayer;
		alc = brew.calcAlcohol();
		quality = brew.getQuality();
	}

	public Player getPlayer() {
		return player;
	}

	public BPlayer getbPlayer() {
		return bPlayer;
	}

	public int getAddedAlcohol() {
		return alc;
	}

	public void setAddedAlcohol(int alc) {
		this.alc = alc;
	}

	public int getQuality() {
		return quality;
	}

	public void setQuality(int quality) {
		if (quality > 10 || quality < 0) {
			throw new IllegalArgumentException("Quality must be in range from 0 to 10");
		}
		this.quality = quality;
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
