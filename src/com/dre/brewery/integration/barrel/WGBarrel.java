package com.dre.brewery.integration.barrel;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public interface WGBarrel {
	boolean checkAccess(Player player, Block spigot, Plugin plugin);
}
