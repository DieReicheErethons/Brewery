package com.dre.brewery.listeners;

import com.dre.brewery.*;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.lore.BrewLore;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;

public class InventoryListener implements Listener {

	/* === Recreating manually the prior BrewEvent behavior. === */
	private HashSet<UUID> trackedBrewmen = new HashSet<>();

	/**
	 * Start tracking distillation for a person when they open the brewer window.
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBrewerOpen(InventoryOpenEvent event) {
		if (!P.use1_9) return;
		HumanEntity player = event.getPlayer();
		Inventory inv = event.getInventory();
		if (player == null || !(inv instanceof BrewerInventory)) return;

		P.p.debugLog("Starting brew inventory tracking");
		trackedBrewmen.add(player.getUniqueId());
	}

	/**
	 * Stop tracking distillation for a person when they close the brewer window.
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBrewerClose(InventoryCloseEvent event) {
		if (!P.use1_9) return;
		HumanEntity player = event.getPlayer();
		Inventory inv = event.getInventory();
		if (player == null || !(inv instanceof BrewerInventory)) return;

		P.p.debugLog("Stopping brew inventory tracking");
		trackedBrewmen.remove(player.getUniqueId());
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBrewerDrag(InventoryDragEvent event) {
		if (!P.use1_9) return;
		// Workaround the Drag event when only clicking a slot
		if (event.getInventory() instanceof BrewerInventory) {
			onBrewerClick(new InventoryClickEvent(event.getView(), InventoryType.SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PLACE_ALL));
		}
	}

	/**
	 * Clicking can either start or stop the new brew distillation tracking.
	 * Note that server restart will halt any ongoing brewing processes and
	 * they will _not_ restart until a new click event.
	 *
	 * @param event the Click event.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBrewerClick(InventoryClickEvent event) {
		if (!P.use1_9) return;

		HumanEntity player = event.getWhoClicked();
		Inventory inv = event.getInventory();
		if (player == null || !(inv instanceof BrewerInventory)) return;

		UUID puid = player.getUniqueId();
		if (!trackedBrewmen.contains(puid)) return;

		if (InventoryType.BREWING != inv.getType()) return;
		if (event.getAction() == InventoryAction.NOTHING) return; // Ignore clicks that do nothing

		BDistiller.distillerClick(event);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBrew(BrewEvent event) {
		if (P.use1_9) {
			if (BDistiller.hasBrew(event.getContents()) != 0) {
				event.setCancelled(true);
			}
			return;
		}
		if (BDistiller.runDistill(event.getContents())) {
			event.setCancelled(true);
		}
	}

	// Clicked a Brew somewhere, do some updating
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void onInventoryClickLow(InventoryClickEvent event) {
		if (event.getCurrentItem() != null && event.getCurrentItem().getType().equals(Material.POTION)) {
			ItemStack item = event.getCurrentItem();
			if (item.hasItemMeta()) {
				PotionMeta potion = ((PotionMeta) item.getItemMeta());
				// convert potions from 1.8 to 1.9 for color and to remove effect descriptions
				if (P.use1_9 && !potion.hasItemFlag(ItemFlag.HIDE_POTION_EFFECTS)) {
					Brew brew = Brew.get(potion);
					if (brew != null) {
						brew.convertPre19(item);
					}
				}
				Brew brew = Brew.get(item);
				if (brew != null) {
					P.p.log(brew.toString());
					//P.p.log(potion.getLore().get(0).replaceAll("§", ""));
					//P.p.log("similar to beispiel? " + BRecipe.get("Beispiel").createBrew(10).isSimilar(brew));

					brew.touch();

					/*try {
						DataInputStream in = new DataInputStream(new Base91DecoderStream(new LoreLoadStream(potion)));

						brew.testLoad(in);

						*//*if (in.readByte() == 27 && in.skip(48) > 0) {
							in.mark(100);
							if (in.readUTF().equals("TESTHalloª∆Ω") && in.readInt() == 34834 && in.skip(4) > 0 && in.readLong() == Long.MAX_VALUE) {
								in.reset();
								if (in.readUTF().equals("TESTHalloª∆Ω")) {
									P.p.log("true");
								} else {
									P.p.log("false3");
								}
							} else {
								P.p.log("false2");
							}
						} else {
							P.p.log("false1");
						}*//*

						in.close();
					} catch (IllegalArgumentException argExc) {
						P.p.log("No Data in Lore");

						try {

							DataOutputStream out = new DataOutputStream(new Base91EncoderStream(new LoreSaveStream(potion, 2)));

							brew.testStore(out);


							*//*out.writeByte(27);
							out.writeLong(1111); //skip
							out.writeLong(1111); //skip
							out.writeLong(1111); //skip
							out.writeLong(1111); //skip
							out.writeLong(1111); //skip
							out.writeLong(1111); //skip
							out.writeUTF("TESTHalloª∆Ω");
							out.writeInt(34834);
							out.writeInt(6436); //skip
							out.writeLong(Long.MAX_VALUE);*//*

							out.close();
							*//*StringBuilder b = new StringBuilder();
							for (char c : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!$%&()*+,-./:;<=>?@[]^_`{|}~\"".toCharArray()) {
								b.append('§').append(c);
							}
							List<String> lore = potion.getLore();
							lore.add(b.toString());
							potion.setLore(lore);*//*
							item.setItemMeta(potion);

						} catch (IOException h) {
							h.printStackTrace();
						}

					} catch (IOException e) {
						e.printStackTrace();
					}*/
				}
			}
		}
	}

	// convert to non colored Lore when taking out of Barrel/Brewer
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getInventory().getType() == InventoryType.BREWING) {
			if (event.getSlot() > 2) {
				return;
			}
		} else if (!(event.getInventory().getHolder() instanceof Barrel) && !(P.use1_14 && event.getInventory().getHolder() instanceof org.bukkit.block.Barrel)) {
			return;
		}

		ItemStack item = event.getCurrentItem();
		if (item != null && item.getType() == Material.POTION && item.hasItemMeta()) {
			PotionMeta meta = (PotionMeta) item.getItemMeta();
			Brew brew = Brew.get(meta);
			if (brew != null) {
				BrewLore lore = null;
				if (BrewLore.hasColorLore(meta)) {
					lore = new BrewLore(brew, meta);
					lore.convertLore(false);
				} else if (!BConfig.alwaysShowAlc && event.getInventory().getType() == InventoryType.BREWING) {
					lore = new BrewLore(brew, meta);
					lore.updateAlc(false);
				}
				if (lore != null) {
					lore.write();
					item.setItemMeta(meta);
					if (event.getWhoClicked() instanceof Player) {
						switch (event.getAction()) {
							case MOVE_TO_OTHER_INVENTORY:
							case HOTBAR_SWAP:
								// Fix a Graphical glitch of item still showing colors until clicking it
								P.p.getServer().getScheduler().runTask(P.p, () -> ((Player) event.getWhoClicked()).updateInventory());
						}
					}
				}
			}
		}
	}

	// Check if the player tries to add more than the allowed amount of brews into an mc-barrel
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onInventoryClickMCBarrel(InventoryClickEvent event) {
		if (!P.use1_14) return;
		if (event.getInventory().getType() != InventoryType.BARREL) return;

		Inventory inv = event.getInventory();
		for (MCBarrel barrel : MCBarrel.openBarrels) {
			if (barrel.getInventory().equals(inv)) {
				barrel.clickInv(event);
				return;
			}
		}
		MCBarrel barrel = new MCBarrel(inv);
		MCBarrel.openBarrels.add(barrel);
		barrel.clickInv(event);
	}

	//public static boolean opening = false;

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryOpen(InventoryOpenEvent event) {
		if (!P.use1_14) return;

		/*Barrel x = null;
		if (event.getInventory().getHolder() instanceof Barrel) {
			x = ((Barrel) event.getInventory().getHolder());
		}

		if (!opening) {
			opening = true;
			Barrel finalBarrel = x;
			P.p.getServer().getScheduler().scheduleSyncDelayedTask(P.p, () -> {finalBarrel.remove(null, null); opening = false;}, 100);
		}*/

		// Check for MC Barrel
		if (event.getInventory().getType() == InventoryType.BARREL) {
			Inventory inv = event.getInventory();
			for (MCBarrel barrel : MCBarrel.openBarrels) {
				if (barrel.getInventory().equals(inv)) {
					barrel.open();
					return;
				}
			}
			MCBarrel barrel = new MCBarrel(inv);
			MCBarrel.openBarrels.add(barrel);
			barrel.open();
		}
	}

	// block the pickup of items where getPickupDelay is > 1000 (puke)
	@EventHandler(ignoreCancelled = true)
	public void onInventoryPickupItem(InventoryPickupItemEvent event){
		if (event.getItem().getPickupDelay() > 1000 && event.getItem().getItemStack().getType() == BConfig.pukeItem) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!P.use1_14) return;

		// Barrel Closing Sound
		if (event.getInventory().getHolder() instanceof Barrel) {
			Barrel barrel = ((Barrel) event.getInventory().getHolder());
			barrel.playClosingSound();
		}

		// Check for MC Barrel
		if (event.getInventory().getType() == InventoryType.BARREL) {
			Inventory inv = event.getInventory();
			for (Iterator<MCBarrel> iter = MCBarrel.openBarrels.iterator(); iter.hasNext(); ) {
				MCBarrel barrel = iter.next();
				if (barrel.getInventory().equals(inv)) {
					barrel.close();
					if (inv.getViewers().size() == 1) {
						// Last viewer, remove Barrel from List of open Barrels
						iter.remove();
					}
					return;
				}
			}
			new MCBarrel(inv).close();
		}
	}
}
