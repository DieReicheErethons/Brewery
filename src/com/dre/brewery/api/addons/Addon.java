package com.dre.brewery.api.addons;

import com.dre.brewery.BreweryPlugin;

public abstract class Addon {

	protected final BreweryPlugin plugin;
	protected final AddonLogger logger;

	public Addon(BreweryPlugin plugin, AddonLogger logger) {
		this.plugin = plugin;
		this.logger = logger;
	}

	public void onAddonEnable() {
	}

	public void onAddonDisable() {
	}
}
