package com.dre.brewery.integration;


import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.dre.brewery.P;
import com.sk89q.worldguard.bukkit.RegionQuery;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.bukkit.permission.RegionPermissionModel;
import com.sk89q.worldguard.protection.flags.DefaultFlag;

public class WGBarrelNew implements WGBarrel {

	public boolean checkAccess(Player player, Block spigot, Plugin plugin) {
		WorldGuardPlugin wg = (WorldGuardPlugin) plugin;

		if (!wg.getGlobalStateManager().get(spigot.getWorld()).useRegions) return true; // Region support disabled
		if (new RegionPermissionModel(wg, player).mayIgnoreRegionProtection(spigot.getWorld())) return true; // Whitelisted cause

		RegionQuery query = wg.getRegionContainer().createQuery();

		if (!query.testBuild(spigot.getLocation(), player, DefaultFlag.USE, DefaultFlag.CHEST_ACCESS)) {
			P.p.msg(player, P.p.languageReader.get("Error_NoBarrelAccess"));
			return false;
		}

		return true;
	}

}
