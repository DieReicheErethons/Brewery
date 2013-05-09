package com.dre.brewery.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import com.dre.brewery.Brew;

public class InventoryListener implements Listener {

	@EventHandler(priority = EventPriority.HIGH)
	public void onBrew(BrewEvent event) {
		int slot = 0;
		BrewerInventory inv = event.getContents();
		ItemStack item;
		boolean custom = false;
		Integer[] contents = new Integer[3];
		while (slot < 3) {
			item = inv.getItem(slot);
			contents[slot] = 0;
			if (item != null) {
				if (item.getType() == Material.POTION) {
					if (item.hasItemMeta()) {
						if (Brew.potions.containsKey(Brew.getUID(item))) {
							// has custom potion in "slot"
							contents[slot] = 1;
							custom = true;
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

}
