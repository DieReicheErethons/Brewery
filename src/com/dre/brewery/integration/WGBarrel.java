package com.dre.brewery.integration;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.dre.brewery.P;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;

public class WGBarrel {

	public static boolean checkAccess(Player player, Block spigot, Plugin plugin) {
		WorldGuardPlugin wg = (WorldGuardPlugin) plugin;
		if (!wg.getGlobalRegionManager().hasBypass(player, player.getWorld())) {
			ApplicableRegionSet region = wg.getRegionManager(player.getWorld()).getApplicableRegions(spigot.getLocation());
			if (region != null) {
				LocalPlayer localPlayer = wg.wrapPlayer(player);
				if (!region.allows(DefaultFlag.CHEST_ACCESS, localPlayer)) {
					if (!region.canBuild(localPlayer)) {
						P.p.msg(player, P.p.languageReader.get("Error_NoBarrelAccess"));
						return false;
					}
				}
			}
		}
		return true;
	}
}
