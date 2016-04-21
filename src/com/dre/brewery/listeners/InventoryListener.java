package com.dre.brewery.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import com.dre.brewery.Barrel;
import com.dre.brewery.Brew;
import com.dre.brewery.P;
import com.dre.brewery.integration.LogBlockBarrel;

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
	private HashSet<UUID> trackedBrewmen = new HashSet<UUID>();
	private HashMap<Block, Integer> trackedBrewers = new HashMap<Block, Integer>();
	
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
		
		BrewerInventory brewer = (BrewerInventory) inv;
		final Block brewery = brewer.getHolder().getBlock();
		
		// If we were already tracking the brewer, cancel any ongoing event due to the click.
		Integer curTask = trackedBrewers.get(brewery);
		if (curTask != null) {
			Bukkit.getScheduler().cancelTask(curTask); // cancel prior
		}
		
		// Now check if we should bother to track it.
		trackedBrewers.put(brewery, new BukkitRunnable() {
			private int brewTime = 401;
			@Override
			public void run() {
				BlockState now = brewery.getState();
				if (now instanceof BrewingStand) {
					BrewingStand stand = (BrewingStand) now;
					// check if still custom
					BrewerInventory brewer = stand.getInventory();
					if (isCustomAndDistill(brewer) ) {
						
						// Still a valid brew distillation
						brewTime = brewTime - 1; // count down.
						stand.setBrewingTime(brewTime); // arbitrary for now
						
						if (brewTime <= 1) { // Done!
							BrewEvent doBrew = new BrewEvent(brewery, brewer);
							Bukkit.getServer().getPluginManager().callEvent(doBrew);
							if (!doBrew.isCancelled()) { // BrewEvent _wasn't_ cancelled.
								this.cancel();
								trackedBrewers.remove(brewery);
								stand.setBrewingTime(0);
								P.p.debugLog("All done distilling");
							} else {
								brewTime = 401; // go again.
								P.p.debugLog("Can distill more! Continuing.");
							}
						}
					} else {
						this.cancel();
						trackedBrewers.remove(brewery);
					}
				} else {
					this.cancel();
					trackedBrewers.remove(brewery);
					P.p.debugLog("The block was replaced; not a brewing stand.");
				}
			}
		}.runTaskTimer(P.p, 2l, 1l).getTaskId());
	}
	
	private boolean isCustomAndDistill(BrewerInventory brewer) {
		ItemStack item = brewer.getItem(3); // ingredient
		if (item == null || Material.GLOWSTONE_DUST != item.getType()) return false; // need dust in the top slot.
		Boolean[] contents = new Boolean[3];
		for (int slot = 0; slot < 3; slot++) {
			item = brewer.getItem(slot);
			contents[slot] = false;
			if (item != null) {
				if (item.getType() == Material.POTION) {
					if (item.hasItemMeta()) {
						int uid = Brew.getUID(item);
						Brew pot = Brew.potions.get(uid);
						if (pot != null && pot.canDistill()) { // need at least one distillable potion.
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
		int slot = 0;
		BrewerInventory inv = event.getContents();
		ItemStack item;
		boolean custom = false;
		Boolean[] contents = new Boolean[3];
		while (slot < 3) {
			item = inv.getItem(slot);
			contents[slot] = false;
			if (item != null) {
				if (item.getType() == Material.POTION) {
					if (item.hasItemMeta()) {
						int uid = Brew.getUID(item);
						if (Brew.potions.containsKey(uid)) {
							// has custom potion in "slot"
							if (Brew.get(uid).canDistill()) {
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
			event.setCancelled(true);
			Brew.distillAll(inv, contents);
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
		if (event.getItem().getPickupDelay() > 1000) {
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
					P.p.errorLog("Brewery was tested with version 1.80 of LogBlock!");
					e.printStackTrace();
				}
			}
		}
	}
}
