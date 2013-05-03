package com.dre.brewery.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
//import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.inventory.ItemStack;

import com.dre.brewery.Brew;

public class EntityListener implements Listener {

	// Remove the Potion from Brew when it despawns
	@EventHandler(priority = EventPriority.NORMAL)
	public void onItemDespawn(ItemDespawnEvent event) {
		ItemStack item = event.getEntity().getItemStack();
		if (item.getTypeId() == 373) {
			Brew.remove(item);
		}
	}

}
