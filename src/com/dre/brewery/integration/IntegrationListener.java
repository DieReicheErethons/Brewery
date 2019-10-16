package com.dre.brewery.integration;

import com.dre.brewery.Barrel;
import com.dre.brewery.LegacyUtil;
import com.dre.brewery.P;
import com.dre.brewery.api.events.barrel.BarrelAccessEvent;
import com.dre.brewery.api.events.barrel.BarrelDestroyEvent;
import com.dre.brewery.api.events.barrel.BarrelRemoveEvent;
import com.dre.brewery.filedata.BConfig;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.Plugin;

public class IntegrationListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBarrelAccessLowest(BarrelAccessEvent event) {
		if (BConfig.useWG) {
			Plugin plugin = P.p.getServer().getPluginManager().getPlugin("WorldGuard");
			if (plugin != null) {
				try {
					if (!BConfig.wg.checkAccess(event.getPlayer(), event.getSpigot(), plugin)) {
						event.setCancelled(true);
					}
				} catch (Throwable e) {
					event.setCancelled(true);
					P.p.errorLog("Failed to Check WorldGuard for Barrel Open Permissions!");
					P.p.errorLog("Brewery was tested with version 5.8, 6.1 to 7.0 of WorldGuard!");
					P.p.errorLog("Disable the WorldGuard support in the config and do /brew reload");
					e.printStackTrace();
					Player player = event.getPlayer();
					if (player.hasPermission("brewery.admin") || player.hasPermission("brewery.mod")) {
						P.p.msg(player, "&cWorldGuard check Error, Brewery was tested with up to v7.0 of Worldguard");
						P.p.msg(player, "&cSet &7useWorldGuard: false &cin the config and /brew reload");
					} else {
						P.p.msg(player, "&cError opening Barrel, please report to an Admin!");
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBarrelAccess(BarrelAccessEvent event) {
		if (BConfig.useGP) {
			if (P.p.getServer().getPluginManager().isPluginEnabled("GriefPrevention")) {
				try {
					if (!GriefPreventionBarrel.checkAccess(event)) {
						event.setCancelled(true);
						return;
					}
				} catch (Throwable e) {
					event.setCancelled(true);
					P.p.errorLog("Failed to Check GriefPrevention for Barrel Open Permissions!");
					P.p.errorLog("Brewery was tested with GriefPrevention v14.5 - v16.9");
					P.p.errorLog("Disable the GriefPrevention support in the config and do /brew reload");
					e.printStackTrace();
					Player player = event.getPlayer();
					if (player.hasPermission("brewery.admin") || player.hasPermission("brewery.mod")) {
						P.p.msg(player, "&cGriefPrevention check Error, Brewery was tested with up to v16.9 of GriefPrevention");
						P.p.msg(player, "&cSet &7useGriefPrevention: false &cin the config and /brew reload");
					} else {
						P.p.msg(player, "&cError opening Barrel, please report to an Admin!");
					}
				}
			}
		}

		if (BConfig.useLWC) {
			Plugin plugin = P.p.getServer().getPluginManager().getPlugin("LWC");
			if (plugin != null) {

				// If the Clicked Block was the Sign, LWC already knows and we dont need to do anything here
				if (!LegacyUtil.isSign(event.getClickedBlock().getType())) {
					Block sign = event.getBarrel().getSignOfSpigot();
					// If the Barrel does not have a Sign, it cannot be locked
					if (!sign.equals(event.getClickedBlock())) {
						Player player = event.getPlayer();
						try {
							if (!LWCBarrel.checkAccess(player, sign, plugin)) {
								event.setCancelled(true);
							}
						} catch (Throwable e) {
							P.p.errorLog("Failed to Check LWC for Barrel Open Permissions!");
							P.p.errorLog("Brewery was tested with version 4.5.0 of LWC!");
							P.p.errorLog("Disable the LWC support in the config and do /brew reload");
							e.printStackTrace();
							if (player.hasPermission("brewery.admin") || player.hasPermission("brewery.mod")) {
								P.p.msg(player, "&cLWC check Error, Brewery was tested with up to v4.5.0 of LWC");
								P.p.msg(player, "&cSet &7useLWC: false &cin the config and /brew reload");
							} else {
								P.p.msg(player, "&cError breaking Barrel, please report to an Admin!");
							}
						}
					}
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onBarrelDestroy(BarrelDestroyEvent event) {
		if (!BConfig.useLWC) return;

		if (event.hasPlayer()) {
			try {
				if (LWCBarrel.denyDestroy(event.getPlayerOptional(), event.getBarrel())) {
					event.setCancelled(true);
				}
			} catch (Throwable e) {
				event.setCancelled(true);
				P.p.errorLog("Failed to Check LWC for Barrel Break Permissions!");
				P.p.errorLog("Brewery was tested with version 4.5.0 of LWC!");
				P.p.errorLog("Disable the LWC support in the config and do /brew reload");
				e.printStackTrace();
				Player player = event.getPlayerOptional();
				if (player.hasPermission("brewery.admin") || player.hasPermission("brewery.mod")) {
					P.p.msg(player, "&cLWC check Error, Brewery was tested with up to v4.5.0 of LWC");
					P.p.msg(player, "&cSet &7useLWC: false &cin the config and /brew reload");
				} else {
					P.p.msg(player, "&cError opening Barrel, please report to an Admin!");
				}
			}
		} else {
			try {
				if (event.getReason() == BarrelDestroyEvent.Reason.EXPLODED) {
					if (LWCBarrel.denyExplosion(event.getBarrel())) {
						event.setCancelled(true);
					}
				} else {
					if (LWCBarrel.denyDestroyOther(event.getBarrel())) {
						event.setCancelled(true);
					}
				}
			} catch (Throwable e) {
				event.setCancelled(true);
				P.p.errorLog("Failed to Check LWC on Barrel Destruction!");
				P.p.errorLog("Brewery was tested with version 4.5.0 of LWC!");
				P.p.errorLog("Disable the LWC support in the config and do /brew reload");
				e.printStackTrace();
			}
		}
	}

	@EventHandler
	public void onBarrelRemove(BarrelRemoveEvent event) {
		if (!BConfig.useLWC) return;

		try {
			LWCBarrel.remove(event.getBarrel());
		} catch (Throwable e) {
			P.p.errorLog("Failed to Remove LWC Lock from Barrel!");
			P.p.errorLog("Brewery was tested with version 4.5.0 of LWC!");
			e.printStackTrace();
		}
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (BConfig.useLB) {
			if (event.getInventory().getHolder() instanceof Barrel) {
				try {
					LogBlockBarrel.closeBarrel(event.getPlayer(), event.getInventory());
				} catch (Exception e) {
					P.p.errorLog("Failed to Log Barrel to LogBlock!");
					P.p.errorLog("Brewery was tested with version 1.94 of LogBlock!");
					e.printStackTrace();
				}
			}
		}
	}
}
