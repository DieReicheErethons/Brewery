package com.dre.brewery.integration;

import com.dre.brewery.Barrel;
import com.dre.brewery.P;
import com.dre.brewery.api.events.barrel.BarrelAccessEvent;
import com.dre.brewery.api.events.barrel.BarrelDestroyEvent;
import com.dre.brewery.api.events.barrel.BarrelRemoveEvent;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.integration.barrel.GriefPreventionBarrel;
import com.dre.brewery.integration.barrel.LWCBarrel;
import com.dre.brewery.integration.barrel.LogBlockBarrel;
import com.dre.brewery.integration.item.MMOItemsPluginItem;
import com.dre.brewery.recipe.BCauldronRecipe;
import com.dre.brewery.recipe.RecipeItem;
import com.dre.brewery.utility.LegacyUtil;
import net.mmogroup.mmolib.MMOLib;
import net.mmogroup.mmolib.api.item.NBTItem;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
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
						P.p.msg(event.getPlayer(), P.p.languageReader.get("Error_NoBarrelAccess"));
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
		if (BConfig.useGMInventories) {
			Plugin pl = P.p.getServer().getPluginManager().getPlugin("GameModeInventories");
			if (pl != null && pl.isEnabled()) {
				try {
					if (pl.getConfig().getBoolean("restrict_creative")) {
						Player player = event.getPlayer();
						if (player.getGameMode() == GameMode.CREATIVE) {
							if (!pl.getConfig().getBoolean("bypass.inventories") || (!player.hasPermission("gamemodeinventories.bypass") && !player.isOp())) {
								event.setCancelled(true);
								if (!pl.getConfig().getBoolean("dont_spam_chat")) {
									P.p.msg(event.getPlayer(), P.p.languageReader.get("Error_NoBarrelAccess"));
								}
								return;
							}
						}
					}
				} catch (Throwable e) {
					P.p.errorLog("Failed to Check GameModeInventories for Barrel Open Permissions!");
					P.p.errorLog("Players will be able to open Barrel with GameMode Creative");
					e.printStackTrace();
					BConfig.useGMInventories = false;
				}
			} else {
				BConfig.useGMInventories = false;
			}
		}
		if (BConfig.useGP) {
			if (P.p.getServer().getPluginManager().isPluginEnabled("GriefPrevention")) {
				try {
					if (!GriefPreventionBarrel.checkAccess(event)) {
						P.p.msg(event.getPlayer(), P.p.languageReader.get("Error_NoBarrelAccess"));
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
					Block sign = event.getBarrel().getBody().getSignOfSpigot();
					// If the Barrel does not have a Sign, it cannot be locked
					if (!sign.equals(event.getClickedBlock())) {
						Player player = event.getPlayer();
						try {
							if (!LWCBarrel.checkAccess(player, sign, plugin)) {
								P.p.msg(event.getPlayer(), P.p.languageReader.get("Error_NoBarrelAccess"));
								event.setCancelled(true);
							}
						} catch (Throwable e) {
							event.setCancelled(true);
							P.p.errorLog("Failed to Check LWC for Barrel Open Permissions!");
							P.p.errorLog("Brewery was tested with version 4.5.0 of LWC!");
							P.p.errorLog("Disable the LWC support in the config and do /brew reload");
							e.printStackTrace();
							if (player.hasPermission("brewery.admin") || player.hasPermission("brewery.mod")) {
								P.p.msg(player, "&cLWC check Error, Brewery was tested with up to v4.5.0 of LWC");
								P.p.msg(player, "&cSet &7useLWC: false &cin the config and /brew reload");
							} else {
								P.p.msg(player, "&cError opening Barrel, please report to an Admin!");
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
					P.p.msg(player, "&cError breaking Barrel, please report to an Admin!");
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

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent event) {
		// Catch the Interact Event early, so MMOItems does not act before us and cancel the event while we try to add it to the Cauldron
		if (!P.use1_9) return;
		if (BConfig.hasMMOItems == null) {
			BConfig.hasMMOItems = P.p.getServer().getPluginManager().isPluginEnabled("MMOItems");
		}
		if (!BConfig.hasMMOItems) return;
		try {
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.hasItem() && event.getHand() == EquipmentSlot.HAND) {
				if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.CAULDRON) {
					NBTItem item = MMOLib.plugin.getNMS().getNBTItem(event.getItem());
					if (item.hasType()) {
						for (RecipeItem rItem : BCauldronRecipe.acceptedCustom) {
							if (rItem instanceof MMOItemsPluginItem) {
								MMOItemsPluginItem mmo = ((MMOItemsPluginItem) rItem);
								if (mmo.matches(event.getItem())) {
									event.setCancelled(true);
									P.p.playerListener.onPlayerInteract(event);
									return;
								}
							}
						}
					}
				}
			}
		} catch (Throwable e) {
			P.p.errorLog("Could not check MMOItems for Item");
			e.printStackTrace();
			BConfig.hasMMOItems = false;
		}
	}
}
