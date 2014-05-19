package com.dre.brewery.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import com.dre.brewery.Brew;
import com.dre.brewery.P;
import com.dre.brewery.integration.LogBlockBarrel;

public class InventoryListener implements Listener {

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
		} else if (event.getInventory().getType() == InventoryType.CHEST) {
			if (!event.getInventory().getTitle().equals(P.p.languageReader.get("Etc_Barrel"))) {
				return;
			}
		} else {
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
			if (event.getInventory().getType() == InventoryType.CHEST) {
				if (event.getInventory().getTitle().equals(P.p.languageReader.get("Etc_Barrel"))) {
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
}
