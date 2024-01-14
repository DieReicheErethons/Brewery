package com.dre.brewery.integration.barrel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.dre.brewery.P;
import org.bukkit.block.Block;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;


public class WGBarrel5 implements WGBarrel {

	private Method allows;
	private Method canBuild;
	private Method getApplicableRegions;

	public WGBarrel5() {
		try {
			allows = ApplicableRegionSet.class.getMethod("allows", StateFlag.class, LocalPlayer.class);
			canBuild = ApplicableRegionSet.class.getMethod("canBuild", LocalPlayer.class);
			getApplicableRegions = RegionManager.class.getMethod("getApplicableRegions", Location.class);
		} catch (NoSuchMethodException e) {
			P.p.errorLog("Failed to Hook WorldGuard for Barrel Open Permissions! Opening Barrels will NOT work!");
			P.p.errorLog("Brewery was tested with version 5.8, 6.1 to 7.0 of WorldGuard!");
			P.p.errorLog("Disable the WorldGuard support in the config and do /brew reload");
			e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	public boolean checkAccess(Player player, Block spigot, Plugin plugin) {
		WorldGuardPlugin wg = (WorldGuardPlugin) plugin;
		if (!wg.getGlobalRegionManager().hasBypass(player, player.getWorld())) {
			try {
				Object region = getApplicableRegions.invoke(wg.getRegionManager(player.getWorld()), spigot.getLocation());

				if (region != null) {
					LocalPlayer localPlayer = wg.wrapPlayer(player);

					if (!(Boolean) allows.invoke(region, DefaultFlag.CHEST_ACCESS, localPlayer)) {
						if (!(Boolean) canBuild.invoke(region, localPlayer)) {
							return false;
						}
					}
				}

			} catch (IllegalAccessException | InvocationTargetException e) {
					e.printStackTrace();
					return false;
			}
		}
		return true;
	}
}
