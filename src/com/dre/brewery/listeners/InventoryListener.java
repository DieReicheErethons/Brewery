package com.dre.brewery.listeners;

import com.dre.brewery.*;
import com.dre.brewery.integration.LogBlockBarrel;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import java.util.UUID;

/**
 * Updated for 1.9 to replicate the "Brewing" process for distilling.
 * Because of how metadata has changed, the brewer no longer triggers as previously described.
 * So, I've added some event tracking and manual forcing of the brewing "animation" if the
 *  set of ingredients in the brewer can be distilled.
 * Nothing here should interfere with vanilla brewing.
 *
 * Note in testing I did discover a few ways to "hack" brewing to distill your brews alongside
 * potions; put fuel and at least one "valid" water bottle w/ a brewing component. You can distill
 * two brews this way, just remove them before the "final" distillation or you will actually
 * brew the potion as well.
 *
 * @author ProgrammerDan (1.9 distillation update only)
 */
public class InventoryListener implements Listener {

	/* === Recreating manually the prior BrewEvent behavior. === */
	private HashSet<UUID> trackedBrewmen = new HashSet<>();
	private HashMap<Block, Integer> trackedBrewers = new HashMap<>();
	private static final int DISTILLTIME = 401;

	/**
	 * Start tracking distillation for a person when they open the brewer window.
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBrewerOpen(InventoryOpenEvent event) {
		if (!P.use1_9) return;
		HumanEntity player = event.getPlayer();
		Inventory inv = event.getInventory();
		if (player == null || inv == null || !(inv instanceof BrewerInventory)) return;

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
		if (player == null || inv == null || !(inv instanceof BrewerInventory)) return;

		P.p.debugLog("Stopping brew inventory tracking");
		trackedBrewmen.remove(player.getUniqueId());
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
		if (player == null || inv == null || !(inv instanceof BrewerInventory)) return;

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
		}

		// Now check if we should bother to track it.
		trackedBrewers.put(brewery, new BukkitRunnable() {
			private int brewTime = DISTILLTIME;
			@Override
			public void run() {
				BlockState now = brewery.getState();
				if (now instanceof BrewingStand) {
					BrewingStand stand = (BrewingStand) now;
					if (brewTime == DISTILLTIME) { // only check at the beginning (and end) for distillables
						if (!isCustom(stand.getInventory(), true)) {
							this.cancel();
							trackedBrewers.remove(brewery);
							P.p.debugLog("nothing to distill");
							return;
						}
					}

					brewTime--; // count down.
					stand.setBrewingTime(brewTime); // arbitrary for now

					if (brewTime <= 1) { // Done!
						//BrewEvent doBrew = new BrewEvent(brewery, brewer);
						//Bukkit.getServer().getPluginManager().callEvent(doBrew);

						BrewerInventory brewer = stand.getInventory();
						if (!runDistill(brewer)) {
							this.cancel();
							trackedBrewers.remove(brewery);
							stand.setBrewingTime(0);
							P.p.debugLog("All done distilling");
						} else {
							brewTime = DISTILLTIME; // go again.
							P.p.debugLog("Can distill more! Continuing.");
						}
					}
				} else {
					this.cancel();
					trackedBrewers.remove(brewery);
					P.p.debugLog("The block was replaced; not a brewing stand.");
				}
			}
		}.runTaskTimer(P.p, 2L, 1L).getTaskId());
	}

	private boolean isCustom(BrewerInventory brewer, boolean distill) {
		ItemStack item = brewer.getItem(3); // ingredient
		if (item == null || Material.GLOWSTONE_DUST != item.getType()) return false; // need dust in the top slot.
		for (int slot = 0; slot < 3; slot++) {
			item = brewer.getItem(slot);
			if (item != null) {
				if (item.getType() == Material.POTION) {
					if (item.hasItemMeta()) {
						Brew pot = Brew.get(item);
						if (pot != null && (!distill || pot.canDistill())) { // need at least one distillable potion.
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBrew(BrewEvent event) {
		if (P.use1_9) {
			if (isCustom(event.getContents(), false)) {
				event.setCancelled(true);
			}
			return;
		}
		if (runDistill(event.getContents())) {
			event.setCancelled(true);
		}
	}

	private boolean runDistill(BrewerInventory inv) {
		int slot = 0;
		ItemStack item;
		boolean custom = false;
		Boolean[] contents = new Boolean[3];
		while (slot < 3) {
			item = inv.getItem(slot);
			contents[slot] = false;
			if (item != null) {
				if (item.getType() == Material.POTION) {
					if (item.hasItemMeta()) {
						Brew brew = Brew.get(item);
						if (brew != null) {
							// has custom potion in "slot"
							if (brew.canDistill()) {
								// is further distillable
								contents[slot] = true;
								custom = true;
							}
						}
					}
				}
			}
			slot++;
		}
		if (custom) {
			Brew.distillAll(inv, contents);
			return true;
		}
		return false;
	}

	// Clicked a Brew somewhere, do some updating
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void onInventoryClickLow(InventoryClickEvent event) {
		if (event.getCurrentItem() != null && event.getCurrentItem().getType().equals(Material.POTION)) {
			ItemStack item = event.getCurrentItem();
			if (item.hasItemMeta()) {
				PotionMeta potion = ((PotionMeta) item.getItemMeta());
				Brew brew = Brew.get(potion);
				if (brew != null) {
					// convert potions from 1.8 to 1.9 for color and to remove effect descriptions
					if (P.use1_9 && !potion.hasItemFlag(ItemFlag.HIDE_POTION_EFFECTS)) {
						BRecipe recipe = brew.getCurrentRecipe();
						if (recipe != null) {
							Brew.PotionColor.valueOf(recipe.getColor()).colorBrew(potion, item, brew.canDistill());
							item.setItemMeta(potion);
						}
					}
					P.p.log(brew.toString());
					//P.p.log(potion.getLore().get(0));
					//brew.touch();

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
		} else if (!(event.getInventory().getHolder() instanceof Barrel)) {
			return;
		}

		ItemStack item = event.getCurrentItem();
		if (item != null) {
			if (item.getType() == Material.POTION) {
				if (item.hasItemMeta()) {
					PotionMeta meta = (PotionMeta) item.getItemMeta();
					Brew brew = Brew.get(meta);
					if (brew != null) {
						if (Brew.hasColorLore(meta)) {
							brew.convertLore(meta, false);
							item.setItemMeta(meta);
						}
					}
				}
			}
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
		if (P.p.useLB) {
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
