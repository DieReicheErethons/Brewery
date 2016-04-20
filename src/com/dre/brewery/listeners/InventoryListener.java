package com.dre.brewery.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
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

public class InventoryListener implements Listener {
	
	/* === Recreating manually the prior BrewEvent behavior. === */
	private HashSet<UUID> trackedBrewmen = new HashSet<UUID>();
	private HashMap<Block, Integer> trackedBrewers = new HashMap<Block, Integer>();
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBrewerOpen(InventoryOpenEvent event) {
		HumanEntity player = event.getPlayer();
		Inventory inv = event.getInventory();
		if (player == null || inv == null || !(inv instanceof BrewerInventory)) return;
		
		P.p.log("Starting brew inventory tracking");
		trackedBrewmen.add(player.getUniqueId());
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBrewerClose(InventoryCloseEvent event) {
		HumanEntity player = event.getPlayer();
		Inventory inv = event.getInventory();
		if (player == null || inv == null || !(inv instanceof BrewerInventory)) return;
		
		P.p.log("Stopping brew inventory tracking");
		trackedBrewmen.remove(player.getUniqueId());
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBrewerClick(InventoryClickEvent event) {
		HumanEntity player = event.getWhoClicked();
		Inventory inv = event.getInventory();
		if (player == null || inv == null || !(inv instanceof BrewerInventory)) return;
		
		UUID puid = player.getUniqueId();
		if (!trackedBrewmen.contains(puid)) return;
		
		P.p.log("Tracking a new brew click event");
		
		BrewerInventory brewer = (BrewerInventory) inv;
		final Block brewery = brewer.getHolder().getBlock();
		
		if (isCustom(brewer)) {
			Integer curTask = trackedBrewers.get(brewery);
			if (curTask != null) {
				Bukkit.getScheduler().cancelTask(curTask); // cancel prior
				P.p.log("Cancelling prior brew countdown");
			}
			
			P.p.log("Starting a new brew countdown");
			trackedBrewers.put(brewery, new BukkitRunnable() {
				private int brewTime = 405;
				@Override
				public void run() {
					BlockState now = brewery.getState();
					if (now instanceof BrewingStand) {
						BrewingStand stand = (BrewingStand) now;
						// check if still custom
						BrewerInventory brewer = stand.getInventory();
						if (isCustom(brewer) ) {
							P.p.log("Still a valid brew distillation");
							brewTime = brewTime - 5; // count down.
							stand.setBrewingTime(brewTime); // arbitrary for now
							
							if (brewTime <= 5) { // trigger.
								P.p.log("Complete brew distillation!");
								BrewEvent doBrew = new BrewEvent(brewery, brewer);
								Bukkit.getServer().getPluginManager().callEvent(doBrew);
								if (doBrew.isCancelled()) {
									this.cancel();
									trackedBrewers.remove(brewery);
								}
								stand.setBrewingTime(0);
							}
						}
					}
				}
			}.runTaskTimer(P.p, 5l, 5l).getTaskId());
		}
	}
	
	private boolean isCustom(BrewerInventory brewer) {
		int slot = 0;
		ItemStack item;
		Boolean[] contents = new Boolean[3];
		while (slot < 3) {
			item = brewer.getItem(slot);
			contents[slot] = false;
			if (item != null) {
				if (item.getType() == Material.POTION) {
					if (item.hasItemMeta()) {
						int uid = Brew.getUID(item);
						if (Brew.potions.containsKey(uid)) {
							return true;
						}
					}
				}
			}
			slot++;
		}
		return false;
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
	public void onBrew(BrewEvent event) {
		if (event.isCancelled()) {
			P.p.log("Got Cancelled Brew Event");
			return;
		} else {
			P.p.log("Got Brew Event");
		}
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
