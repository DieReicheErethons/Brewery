package com.dre.brewery.api.addons;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.utility.BUtil;
import org.bukkit.Bukkit;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AddonLogger {
	private final static Logger logger = BreweryPlugin.getInstance().getLogger();
	private final String fullPrefix;
	private final String prefix;

	public AddonLogger(Class<? extends BreweryAddon> addonUninstantiated) {
		this.fullPrefix = "[Brewery] [" + addonUninstantiated.getSimpleName() + "] ";
		this.prefix = "[" + addonUninstantiated.getSimpleName() + "] ";
	}

	public void info(String message) {
		Bukkit.getConsoleSender().sendMessage(BUtil.color(fullPrefix + message));
	}

	public void warning(String message) {
		logger.log(Level.WARNING, prefix + message);
	}

	public void severe(String message) {
		logger.log(Level.SEVERE, prefix + message);
	}

	public void info(String message, Throwable throwable) {
		info(message);
		throwable.printStackTrace();
	}

	public void warning(String message, Throwable throwable) {
		logger.log(Level.WARNING, prefix + message, throwable);
	}

	public void severe(String message, Throwable throwable) {
		logger.log(Level.SEVERE, prefix + message, throwable);
	}
}
