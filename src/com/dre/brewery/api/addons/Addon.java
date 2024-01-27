package com.dre.brewery.api.addons;

import com.dre.brewery.BreweryPlugin;

public abstract class Addon {

	protected final BreweryPlugin plugin;

	public Addon(BreweryPlugin plugin) {
		this.plugin = plugin;
	}

	public void onAddonEnable() {
	}

	public void onAddonDisable() {
	}
}
