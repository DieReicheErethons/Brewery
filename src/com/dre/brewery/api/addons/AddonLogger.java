package com.dre.brewery.api.addons;

import com.dre.brewery.utility.BUtil;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Contract;

public class AddonLogger {

	private final String prefix;

	public AddonLogger(Class<? extends BreweryAddon> addonUninstantiated) {
		this.prefix = "[BreweryAddon: " + addonUninstantiated.getSimpleName() + "] ";
	}

	public void info(String message) {
		Bukkit.getConsoleSender().sendMessage(BUtil.color(prefix + message));
	}

	public void warning(String message) {
		Bukkit.getConsoleSender().sendMessage(BUtil.color("&e" + prefix + message));
	}

	public void severe(String message) {
		Bukkit.getConsoleSender().sendMessage(BUtil.color("&c" + prefix + message));
	}

	public void info(String message, Throwable throwable) {
		Bukkit.getConsoleSender().sendMessage(BUtil.color(prefix + message));
		throwable.printStackTrace();
	}

	public void warning(String message, Throwable throwable) {
		Bukkit.getConsoleSender().sendMessage(BUtil.color("&e" + prefix + message));
		throwable.printStackTrace();
	}

	public void severe(String message, Throwable throwable) {
		Bukkit.getConsoleSender().sendMessage(BUtil.color("&c" + prefix + message));
		throwable.printStackTrace();
	}
}
