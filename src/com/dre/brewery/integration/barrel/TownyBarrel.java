package com.dre.brewery.integration.barrel;

import com.dre.brewery.P;
import com.dre.brewery.api.events.barrel.BarrelAccessEvent;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import org.bukkit.Location;
import org.bukkit.Material;

public class TownyBarrel {
	public static boolean checkAccess(BarrelAccessEvent event) {
		Location barrelLoc = event.getSpigot().getLocation();
		Material mat = P.use1_14 ? Material.BARREL : Material.CHEST;

		try {
			if (!TownySettings.isSwitchMaterial(mat, barrelLoc)) {
				return true;
			}
		} catch (Exception e) {
			//noinspection deprecation
			if (!TownySettings.isSwitchMaterial("CHEST")) {
				return true;
			}
		}
		return PlayerCacheUtil.getCachePermission(event.getPlayer(), barrelLoc, mat, TownyPermission.ActionType.SWITCH);
	}
}
