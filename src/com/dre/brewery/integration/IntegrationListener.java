package com.dre.brewery.integration;

import com.dre.brewery.Barrel;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.api.events.barrel.BarrelAccessEvent;
import com.dre.brewery.api.events.barrel.BarrelDestroyEvent;
import com.dre.brewery.api.events.barrel.BarrelRemoveEvent;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.integration.barrel.BlocklockerBarrel;
import com.dre.brewery.integration.barrel.GriefPreventionBarrel;
import com.dre.brewery.integration.barrel.LWCBarrel;
import com.dre.brewery.integration.barrel.LogBlockBarrel;
import com.dre.brewery.integration.barrel.TownyBarrel;
import com.dre.brewery.integration.item.MMOItemsPluginItem;
import com.dre.brewery.recipe.BCauldronRecipe;
import com.dre.brewery.recipe.RecipeItem;
import com.dre.brewery.utility.LegacyUtil;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
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
			Plugin plugin = BreweryPlugin.breweryPlugin.getServer().getPluginManager().getPlugin("WorldGuard");
			if (plugin != null) {
				try {
					if (!BConfig.wg.checkAccess(event.getPlayer(), event.getSpigot(), plugin)) {
						event.setCancelled(true);
						BreweryPlugin.breweryPlugin.msg(event.getPlayer(), BreweryPlugin.breweryPlugin.languageReader.get("Error_NoBarrelAccess"));
					}
				} catch (Throwable e) {
					event.setCancelled(true);
					BreweryPlugin.breweryPlugin.errorLog("Failed to Check WorldGuard for Barrel Open Permissions!");
					BreweryPlugin.breweryPlugin.errorLog("Brewery was tested with version 5.8, 6.1 to 7.0 of WorldGuard!");
					BreweryPlugin.breweryPlugin.errorLog("Disable the WorldGuard support in the config and do /brew reload");
					e.printStackTrace();
					Player player = event.getPlayer();
					if (player.hasPermission("brewery.admin") || player.hasPermission("brewery.mod")) {
						BreweryPlugin.breweryPlugin.msg(player, "&cWorldGuard check Error, Brewery was tested with up to v7.0 of Worldguard");
						BreweryPlugin.breweryPlugin.msg(player, "&cSet &7useWorldGuard: false &cin the config and /brew reload");
					} else {
						BreweryPlugin.breweryPlugin.msg(player, "&cError opening Barrel, please report to an Admin!");
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBarrelAccess(BarrelAccessEvent event) {
		if (BConfig.useGMInventories) {
			Plugin pl = BreweryPlugin.breweryPlugin.getServer().getPluginManager().getPlugin("GameModeInventories");
			if (pl != null && pl.isEnabled()) {
				try {
					if (pl.getConfig().getBoolean("restrict_creative")) {
						Player player = event.getPlayer();
						if (player.getGameMode() == GameMode.CREATIVE) {
							if (!pl.getConfig().getBoolean("bypass.inventories") || (!player.hasPermission("gamemodeinventories.bypass") && !player.isOp())) {
								event.setCancelled(true);
								if (!pl.getConfig().getBoolean("dont_spam_chat")) {
									BreweryPlugin.breweryPlugin.msg(event.getPlayer(), BreweryPlugin.breweryPlugin.languageReader.get("Error_NoBarrelAccess"));
								}
								return;
							}
						}
					}
				} catch (Throwable e) {
					BreweryPlugin.breweryPlugin.errorLog("Failed to Check GameModeInventories for Barrel Open Permissions!");
					BreweryPlugin.breweryPlugin.errorLog("Players will be able to open Barrel with GameMode Creative");
					e.printStackTrace();
					BConfig.useGMInventories = false;
				}
			} else {
				BConfig.useGMInventories = false;
			}
		}
		if (BConfig.useGP) {
			if (BreweryPlugin.breweryPlugin.getServer().getPluginManager().isPluginEnabled("GriefPrevention")) {
				try {
					if (!GriefPreventionBarrel.checkAccess(event)) {
						BreweryPlugin.breweryPlugin.msg(event.getPlayer(), BreweryPlugin.breweryPlugin.languageReader.get("Error_NoBarrelAccess"));
						event.setCancelled(true);
						return;
					}
				} catch (Throwable e) {
					event.setCancelled(true);
					BreweryPlugin.breweryPlugin.errorLog("Failed to Check GriefPrevention for Barrel Open Permissions!");
					BreweryPlugin.breweryPlugin.errorLog("Brewery was tested with GriefPrevention v14.5 - v16.9");
					BreweryPlugin.breweryPlugin.errorLog("Disable the GriefPrevention support in the config and do /brew reload");
					e.printStackTrace();
					Player player = event.getPlayer();
					if (player.hasPermission("brewery.admin") || player.hasPermission("brewery.mod")) {
						BreweryPlugin.breweryPlugin.msg(player, "&cGriefPrevention check Error, Brewery was tested with up to v16.9 of GriefPrevention");
						BreweryPlugin.breweryPlugin.msg(player, "&cSet &7useGriefPrevention: false &cin the config and /brew reload");
					} else {
						BreweryPlugin.breweryPlugin.msg(player, "&cError opening Barrel, please report to an Admin!");
					}
					return;
				}
			}
		}

		if (BConfig.useLWC) {
			Plugin plugin = BreweryPlugin.breweryPlugin.getServer().getPluginManager().getPlugin("LWC");
			if (plugin != null) {

				// If the Clicked Block was the Sign, LWC already knows and we dont need to do anything here
				if (!LegacyUtil.isSign(event.getClickedBlock().getType())) {
					Block sign = event.getBarrel().getBody().getSignOfSpigot();
					// If the Barrel does not have a Sign, it cannot be locked
					if (!sign.equals(event.getClickedBlock())) {
						Player player = event.getPlayer();
						try {
							if (!LWCBarrel.checkAccess(player, sign, plugin)) {
								BreweryPlugin.breweryPlugin.msg(event.getPlayer(), BreweryPlugin.breweryPlugin.languageReader.get("Error_NoBarrelAccess"));
								event.setCancelled(true);
								return;
							}
						} catch (Throwable e) {
							event.setCancelled(true);
							BreweryPlugin.breweryPlugin.errorLog("Failed to Check LWC for Barrel Open Permissions!");
							BreweryPlugin.breweryPlugin.errorLog("Brewery was tested with version 4.5.0 of LWC!");
							BreweryPlugin.breweryPlugin.errorLog("Disable the LWC support in the config and do /brew reload");
							e.printStackTrace();
							if (player.hasPermission("brewery.admin") || player.hasPermission("brewery.mod")) {
								BreweryPlugin.breweryPlugin.msg(player, "&cLWC check Error, Brewery was tested with up to v4.5.0 of LWC");
								BreweryPlugin.breweryPlugin.msg(player, "&cSet &7useLWC: false &cin the config and /brew reload");
							} else {
								BreweryPlugin.breweryPlugin.msg(player, "&cError opening Barrel, please report to an Admin!");
							}
							return;
						}
					}
				}
			}
		}

		if (BConfig.useTowny) {
			if (BreweryPlugin.breweryPlugin.getServer().getPluginManager().isPluginEnabled("Towny")) {
				try {
					if (!TownyBarrel.checkAccess(event)) {
						BreweryPlugin.breweryPlugin.msg(event.getPlayer(), BreweryPlugin.breweryPlugin.languageReader.get("Error_NoBarrelAccess"));
						event.setCancelled(true);
						return;
					}
				} catch (Throwable e) {
					event.setCancelled(true);
					BreweryPlugin.breweryPlugin.errorLog("Failed to Check Towny for Barrel Open Permissions!");
					BreweryPlugin.breweryPlugin.errorLog("Brewery was tested with Towny v0.96.3.0");
					BreweryPlugin.breweryPlugin.errorLog("Disable the Towny support in the config and do /brew reload");
					e.printStackTrace();
					Player player = event.getPlayer();
					if (player.hasPermission("brewery.admin") || player.hasPermission("brewery.mod")) {
						BreweryPlugin.breweryPlugin.msg(player, "&cTowny check Error, Brewery was tested with up to v0.96.3.0 of Towny");
						BreweryPlugin.breweryPlugin.msg(player, "&cSet &7useTowny: false &cin the config and /brew reload");
					} else {
						BreweryPlugin.breweryPlugin.msg(player, "&cError opening Barrel, please report to an Admin!");
					}
					return;
				}
			}
		}

		if (BConfig.useBlocklocker) {
			if (BreweryPlugin.breweryPlugin.getServer().getPluginManager().isPluginEnabled("BlockLocker")) {
				try {
					if (!BlocklockerBarrel.checkAccess(event)) {
						BreweryPlugin.breweryPlugin.msg(event.getPlayer(), BreweryPlugin.breweryPlugin.languageReader.get("Error_NoBarrelAccess"));
						event.setCancelled(true);
						return;
					}
				} catch (Throwable e) {
					event.setCancelled(true);
					BreweryPlugin.breweryPlugin.errorLog("Failed to Check BlockLocker for Barrel Open Permissions!");
					BreweryPlugin.breweryPlugin.errorLog("Brewery was tested with BlockLocker v1.9");
					BreweryPlugin.breweryPlugin.errorLog("Disable the BlockLocker support in the config and do /brew reload");
					e.printStackTrace();
					Player player = event.getPlayer();
					if (player.hasPermission("brewery.admin") || player.hasPermission("brewery.mod")) {
						BreweryPlugin.breweryPlugin.msg(player, "&cBlockLocker check Error, Brewery was tested with v1.9 of BlockLocker");
						BreweryPlugin.breweryPlugin.msg(player, "&cSet &7useBlockLocker: false &cin the config and /brew reload");
					} else {
						BreweryPlugin.breweryPlugin.msg(player, "&cError opening Barrel, please report to an Admin!");
					}
					return;
				}
			}
		}

		if (BConfig.virtualChestPerms) {
			Player player = event.getPlayer();
			BlockState originalBlockState = event.getClickedBlock().getState();

			event.getClickedBlock().setType(Material.CHEST, false);
			PlayerInteractEvent simulatedEvent = new PlayerInteractEvent(
				player,
				Action.RIGHT_CLICK_BLOCK,
				player.getInventory().getItemInMainHand(),
				event.getClickedBlock(),
				event.getClickedBlockFace(),
				EquipmentSlot.HAND);

			try {
				BreweryPlugin.breweryPlugin.getServer().getPluginManager().callEvent(simulatedEvent);
			} catch (Throwable e) {
				BreweryPlugin.breweryPlugin.errorLog("Failed to simulate a Chest for Barrel Open Permissions!");
				BreweryPlugin.breweryPlugin.errorLog("Disable useVirtualChestPerms in the config and do /brew reload");
				e.printStackTrace();
				if (player.hasPermission("brewery.admin") || player.hasPermission("brewery.mod")) {
					BreweryPlugin.breweryPlugin.msg(player, "&cVirtual Chest Error");
					BreweryPlugin.breweryPlugin.msg(player, "&cSet &7useVirtualChestPerms: false &cin the config and /brew reload");
				} else {
					BreweryPlugin.breweryPlugin.msg(player, "&cError opening Barrel, please report to an Admin!");
				}
			} finally {
				event.getClickedBlock().setType(Material.AIR, false);
				originalBlockState.update(true);
			}

			if (simulatedEvent.useInteractedBlock() == Event.Result.DENY) {
				event.setCancelled(true);
				BreweryPlugin.breweryPlugin.msg(event.getPlayer(), BreweryPlugin.breweryPlugin.languageReader.get("Error_NoBarrelAccess"));
				//return;
			}
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onBarrelDestroy(BarrelDestroyEvent event) {
		if (!BConfig.useLWC) return;

		if (event.hasPlayer()) {
			Player player = event.getPlayerOptional();
			assert player != null;
			try {
				if (LWCBarrel.denyDestroy(player, event.getBarrel())) {
					event.setCancelled(true);
				}
			} catch (Throwable e) {
				event.setCancelled(true);
				BreweryPlugin.breweryPlugin.errorLog("Failed to Check LWC for Barrel Break Permissions!");
				BreweryPlugin.breweryPlugin.errorLog("Brewery was tested with version 4.5.0 of LWC!");
				BreweryPlugin.breweryPlugin.errorLog("Disable the LWC support in the config and do /brew reload");
				e.printStackTrace();
				if (player.hasPermission("brewery.admin") || player.hasPermission("brewery.mod")) {
					BreweryPlugin.breweryPlugin.msg(player, "&cLWC check Error, Brewery was tested with up to v4.5.0 of LWC");
					BreweryPlugin.breweryPlugin.msg(player, "&cSet &7useLWC: false &cin the config and /brew reload");
				} else {
					BreweryPlugin.breweryPlugin.msg(player, "&cError breaking Barrel, please report to an Admin!");
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
				BreweryPlugin.breweryPlugin.errorLog("Failed to Check LWC on Barrel Destruction!");
				BreweryPlugin.breweryPlugin.errorLog("Brewery was tested with version 4.5.0 of LWC!");
				BreweryPlugin.breweryPlugin.errorLog("Disable the LWC support in the config and do /brew reload");
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
			BreweryPlugin.breweryPlugin.errorLog("Failed to Remove LWC Lock from Barrel!");
			BreweryPlugin.breweryPlugin.errorLog("Brewery was tested with version 4.5.0 of LWC!");
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
					BreweryPlugin.breweryPlugin.errorLog("Failed to Log Barrel to LogBlock!");
					BreweryPlugin.breweryPlugin.errorLog("Brewery was tested with version 1.94 of LogBlock!");
					e.printStackTrace();
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent event) {
		// Catch the Interact Event early, so MMOItems does not act before us and cancel the event while we try to add it to the Cauldron
		if (!BreweryPlugin.use1_9) return;
		if (BConfig.hasMMOItems == null) {
			BConfig.hasMMOItems = BreweryPlugin.breweryPlugin.getServer().getPluginManager().isPluginEnabled("MMOItems")
				&& BreweryPlugin.breweryPlugin.getServer().getPluginManager().isPluginEnabled("MythicLib");
		}
		if (!BConfig.hasMMOItems) return;
		try {
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.hasItem() && event.getHand() == EquipmentSlot.HAND) {
				if (event.getClickedBlock() != null && LegacyUtil.isWaterCauldron(event.getClickedBlock().getType())) {
					NBTItem item = NBTItem.get(event.getItem());
					if (item.hasType()) {
						for (RecipeItem rItem : BCauldronRecipe.acceptedCustom) {
							if (rItem instanceof MMOItemsPluginItem) {
								MMOItemsPluginItem mmo = ((MMOItemsPluginItem) rItem);
								if (mmo.matches(event.getItem())) {
									event.setCancelled(true);
									BreweryPlugin.breweryPlugin.playerListener.onPlayerInteract(event);
									return;
								}
							}
						}
					}
				}
			}
		} catch (Throwable e) {
			BreweryPlugin.breweryPlugin.errorLog("Could not check MMOItems for Item");
			e.printStackTrace();
			BConfig.hasMMOItems = false;
		}
	}
}
