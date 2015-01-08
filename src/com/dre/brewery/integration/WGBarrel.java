package com.dre.brewery.integration;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public interface WGBarrel {
	public abstract boolean checkAccess(Player player, Block spigot, Plugin plugin);
}
