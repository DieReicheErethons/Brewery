package com.dre.brewery.integration.barrel;

import com.dre.brewery.P;
import com.dre.brewery.api.events.barrel.BarrelAccessEvent;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import org.bukkit.Material;

public class TownyBarrel {
	public static boolean checkAccess(BarrelAccessEvent event) {
		if (!TownySettings.isSwitchMaterial("BREWERY") && !TownySettings.isSwitchMaterial("BARREL")) {
			if (P.use1_14) {
				return true;
			} else if (!TownySettings.isSwitchMaterial("CHEST")) {
				return true;
			}
		}
		Material mat = P.use1_14 ? Material.BARREL : Material.CHEST;
		return PlayerCacheUtil.getCachePermission(event.getPlayer(), event.getSpigot().getLocation(), mat, TownyPermission.ActionType.SWITCH);
	}
}
