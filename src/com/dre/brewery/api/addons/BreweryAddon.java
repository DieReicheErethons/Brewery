package com.dre.brewery.api.addons;

import com.dre.brewery.BreweryPlugin;

public abstract class BreweryAddon {

	protected final BreweryPlugin plugin;
	protected final AddonLogger logger;

	public BreweryAddon(BreweryPlugin plugin, AddonLogger logger) {
        this.plugin = plugin;
		this.logger = logger;
	}

	public void onAddonEnable(AddonFileManager addonFileManager) {
	}

	public void onAddonDisable() {
	}

	public void onBreweryReload() {
	}

	public AddonLogger getLogger() {
		return logger;
	}
}
