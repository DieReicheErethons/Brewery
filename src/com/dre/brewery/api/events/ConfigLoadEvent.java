package com.dre.brewery.api.events;

import com.dre.brewery.api.BreweryApi;
import com.dre.brewery.recipe.BCauldronRecipe;
import com.dre.brewery.recipe.BRecipe;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The Brewery Config was reloaded
 */
public class ConfigLoadEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	/**
	 * One of the things one might need to do after reloading
	 * Removes a Recipe, can also remove config recipes
	 *
	 * @param name Name of the Recipe to remove
	 * @return The Recipe that was removed, null if none was removed
	 */
	public BRecipe removeRecipe(String name) {
		return BreweryApi.removeRecipe(name);
	}

	/**
	 * One of the things one might need to do after reloading
	 * Removes a Cauldron Recipe, can also remove config recipes
	 *
	 * @param name Name of the Cauldron Recipe to remove
	 * @return The Cauldron Recipe that was removed, null if none was removed
	 */
	public BCauldronRecipe removeCauldronRecipe(String name) {
		return BreweryApi.removeCauldronRecipe(name);
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
