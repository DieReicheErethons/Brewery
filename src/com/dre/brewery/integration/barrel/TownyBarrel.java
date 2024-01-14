package com.dre.brewery.integration.barrel;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.api.events.barrel.BarrelAccessEvent;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import org.bukkit.Location;
import org.bukkit.Material;

public class TownyBarrel {
	public static boolean checkAccess(BarrelAccessEvent event) {
		Location barrelLoc = event.getSpigot().getLocation();
		Material mat = BreweryPlugin.use1_14 ? Material.BARREL : Material.CHEST;

		if (!TownySettings.isSwitchMaterial(mat, barrelLoc)) {
			return true;
		}
		return PlayerCacheUtil.getCachePermission(event.getPlayer(), barrelLoc, mat, TownyPermission.ActionType.SWITCH);
	}
}
