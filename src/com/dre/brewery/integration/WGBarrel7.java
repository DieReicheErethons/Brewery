package com.dre.brewery.integration;


import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.dre.brewery.P;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.bukkit.permission.RegionPermissionModel;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;

public class WGBarrel7 implements WGBarrel {

	public boolean checkAccess(Player player, Block spigot, Plugin plugin) {
		WorldGuardPlugin wg = (WorldGuardPlugin) plugin;
		WorldGuardPlatform platform = WorldGuard.getInstance().getPlatform();

                World world = platform.getWorldByName(spigot.getWorld().getName());
		if (!platform.getGlobalStateManager().get(world).useRegions) return true; // Region support disabled
		if (new RegionPermissionModel(wg, player).mayIgnoreRegionProtection(spigot.getWorld())) return true; // Whitelisted cause

		RegionQuery query = platform.getRegionContainer().createQuery();

		if (!query.testBuild(new Location(world, spigot.getX(), spigot.getY(), spigot.getZ()), wg.wrapPlayer(player), Flags.USE, Flags.CHEST_ACCESS)) {
			P.p.msg(player, P.p.languageReader.get("Error_NoBarrelAccess"));
			return false;
		}

		return true;
	}

}
