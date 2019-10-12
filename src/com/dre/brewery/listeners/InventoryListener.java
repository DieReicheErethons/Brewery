package com.dre.brewery.listeners;

import com.dre.brewery.*;
import com.dre.brewery.lore.BrewLore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;

/**
 * Updated for 1.9 to replicate the "Brewing" process for distilling.
 * Because of how metadata has changed, the brewer no longer triggers as previously described.
 * So, I've added some event tracking and manual forcing of the brewing "animation" if the
 *  set of ingredients in the brewer can be distilled.
 * Nothing here should interfere with vanilla brewing.
 *
 * @author ProgrammerDan (1.9 distillation update only)
 */
public class InventoryListener implements Listener {

	/* === Recreating manually the prior BrewEvent behavior. === */
	private HashSet<UUID> trackedBrewmen = new HashSet<>();
	private HashMap<Block, Integer> trackedBrewers = new HashMap<>();
	private static final int DISTILLTIME = 400;

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

		BrewerInventory brewer = (BrewerInventory) inv;
		final Block brewery = brewer.getHolder().getBlock();

		// If we were already tracking the brewer, cancel any ongoing event due to the click.
		Integer curTask = trackedBrewers.get(brewery);
		if (curTask != null) {
			Bukkit.getScheduler().cancelTask(curTask); // cancel prior
			brewer.getHolder().setBrewingTime(0); // Fixes brewing continuing without fuel for normal potions
			brewer.getHolder().update();
		}
		final int fuel = brewer.getHolder().getFuelLevel();

		// Now check if we should bother to track it.
		trackedBrewers.put(brewery, new BukkitRunnable() {
			private int runTime = -1;
			private int brewTime = -1;
			@Override
			public void run() {
				BlockState now = brewery.getState();
				if (now instanceof BrewingStand) {
					BrewingStand stand = (BrewingStand) now;
					if (brewTime == -1) { // only check at the beginning (and end) for distillables
						switch (hasCustom(stand.getInventory())) {
							case 1:
								// Custom potion but not for distilling. Stop any brewing and cancel this task
								if (stand.getBrewingTime() > 0) {
									if (P.use1_11) {
										// The trick below doesnt work in 1.11, but we dont need it anymore
										// This should only happen with older Brews that have been made with the old Potion Color System
										stand.setBrewingTime(Short.MAX_VALUE);
									} else {
										// Brewing time is sent and stored as short
										// This sends a negative short value to the Client
										// In the client the Brewer will look like it is not doing anything
										stand.setBrewingTime(Short.MAX_VALUE << 1);
									}
									stand.setFuelLevel(fuel);
									stand.update();
								}
							case 0:
								// No custom potion, cancel and ignore
								this.cancel();
								trackedBrewers.remove(brewery);
								P.p.debugLog("nothing to distill");
								return;
							default:
								runTime = getLongestDistillTime(stand.getInventory());
								brewTime = runTime;
								P.p.debugLog("using brewtime: " + runTime);

						}
					}

					brewTime--; // count down.
					stand.setBrewingTime((int) ((float) brewTime / ((float) runTime / (float) DISTILLTIME)) + 1);

					if (brewTime <= 1) { // Done!
						stand.setBrewingTime(0);
						stand.update();
						BrewerInventory brewer = stand.getInventory();
						if (!runDistill(brewer)) {
							this.cancel();
							trackedBrewers.remove(brewery);
							P.p.debugLog("All done distilling");
						} else {
							brewTime = -1; // go again.
							P.p.debugLog("Can distill more! Continuing.");
						}
					} else {
						stand.update();
					}
				} else {
					this.cancel();
					trackedBrewers.remove(brewery);
					P.p.debugLog("The block was replaced; not a brewing stand.");
				}
			}
		}.runTaskTimer(P.p, 2L, 1L).getTaskId());
	}

	// Returns a Brew or null for every Slot in the BrewerInventory
	private Brew[] getDistillContents(BrewerInventory inv) {
		ItemStack item;
		Brew[] contents = new Brew[3];
		for (int slot = 0; slot < 3; slot++) {
			item = inv.getItem(slot);
			if (item != null) {
				contents[slot] = Brew.get(item);
			}
		}
		return contents;
	}

	private byte hasCustom(BrewerInventory brewer) {
		ItemStack item = brewer.getItem(3); // ingredient
		boolean glowstone = (item != null && Material.GLOWSTONE_DUST == item.getType()); // need dust in the top slot.
		byte customFound = 0;
		for (Brew brew : getDistillContents(brewer)) {
			if (brew != null) {
				if (!glowstone) {
					return 1;
				}
				if (brew.canDistill()) {
					return 2;
				} else {
					customFound = 1;
				}
			}
		}
		return customFound;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBrew(BrewEvent event) {
		if (P.use1_9) {
			if (hasCustom(event.getContents()) != 0) {
				event.setCancelled(true);
			}
			return;
		}
		if (runDistill(event.getContents())) {
			event.setCancelled(true);
		}
	}

	private boolean runDistill(BrewerInventory inv) {
		boolean custom = false;
		Brew[] contents = getDistillContents(inv);
		for (int slot = 0; slot < 3; slot++) {
			if (contents[slot] == null) continue;
			if (contents[slot].canDistill()) {
				// is further distillable
				custom = true;
			} else {
				contents[slot] = null;
			}
		}
		if (custom) {
			Brew.distillAll(inv, contents);
			return true;
		}
		return false;
	}

	private int getLongestDistillTime(BrewerInventory inv) {
		int bestTime = 0;
		int time;
		Brew[] contents = getDistillContents(inv);
		for (int slot = 0; slot < 3; slot++) {
			if (contents[slot] == null) continue;
			time = contents[slot].getDistillTimeNextRun();
			if (time == 0) {
				// Undefined Potion needs 40 seconds
				time = 800;
			}
			if (time > bestTime) {
				bestTime = time;
			}
		}
		if (bestTime > 0) {
			return bestTime;
		}
		return 800;
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
						brew.convertLegacy(item);
					}
				}
				Brew brew = Brew.get(item);
				if (brew != null) {
					P.p.log(brew.toString());
					P.p.log(potion.getLore().get(0).replaceAll("§", ""));
					P.p.log("similar to beispiel? " + BRecipe.get("Beispiel").createBrew(10).isSimilar(brew));

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
		if (item != null) {
			if (item.getType() == Material.POTION) {
				if (item.hasItemMeta()) {
					PotionMeta meta = (PotionMeta) item.getItemMeta();
					Brew brew = Brew.get(meta);
					if (brew != null) {
						if (BrewLore.hasColorLore(meta)) {
							BrewLore lore = new BrewLore(brew, meta);
							lore.convertLore(false);
							lore.write();
							item.setItemMeta(meta);
						}
					}
				}
			}
		}
	}

	// Check if the player tries to add more than the allowed amount of brews into an mc-barrel
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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
		if (event.getItem().getPickupDelay() > 1000 && event.getItem().getItemStack().getType() == BPlayer.pukeItem) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!P.use1_14) return;

		// Barrel Closing Sound
		if (event.getInventory().getHolder() instanceof Barrel) {
			Barrel barrel = ((Barrel) event.getInventory().getHolder());
			float randPitch = (float) (Math.random() * 0.1);
			if (barrel.isLarge()) {
				barrel.getSpigot().getWorld().playSound(barrel.getSpigot().getLocation(), Sound.BLOCK_BARREL_CLOSE, SoundCategory.BLOCKS, 0.5f, 0.5f + randPitch);
				barrel.getSpigot().getWorld().playSound(barrel.getSpigot().getLocation(), Sound.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 0.2f, 0.6f + randPitch);
			} else {
				barrel.getSpigot().getWorld().playSound(barrel.getSpigot().getLocation(), Sound.BLOCK_BARREL_CLOSE, SoundCategory.BLOCKS, 0.5f, 0.8f + randPitch);
			}
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
