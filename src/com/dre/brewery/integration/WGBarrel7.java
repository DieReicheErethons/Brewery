package com.dre.brewery.integration;


import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.dre.brewery.P;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.internal.permission.RegionPermissionModel;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;

public class WGBarrel7 implements WGBarrel {

	public boolean checkAccess(Player player, Block spigot, Plugin plugin) {
		WorldGuardPlugin wg = (WorldGuardPlugin) plugin;
		WorldGuardPlatform platform = WorldGuard.getInstance().getPlatform();

                World world = platform.getWorldByName(spigot.getWorld().getName());
		if (!platform.getGlobalStateManager().get(world).useRegions) return true; // Region support disabled
		WorldEditPlugin we = JavaPlugin.getPlugin(WorldEditPlugin.class);
		if (new RegionPermissionModel((Actor) we.wrapPlayer(player)).mayIgnoreRegionProtection(world)) return true; // Whitelisted cause

		RegionQuery query = platform.getRegionContainer().createQuery();

		if (!query.testBuild(new Location(world, spigot.getX(), spigot.getY(), spigot.getZ()), wg.wrapPlayer(player), Flags.USE, Flags.CHEST_ACCESS)) {
			P.p.msg(player, P.p.languageReader.get("Error_NoBarrelAccess"));
			return false;
		}

		return true;
	}

}
