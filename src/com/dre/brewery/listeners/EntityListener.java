package com.dre.brewery.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import com.dre.brewery.Brew;

public class EntityListener implements Listener {

	// Remove the Potion from Brew when it despawns
	@EventHandler(priority = EventPriority.MONITOR)
	public void onItemDespawn(ItemDespawnEvent event) {
		if (!event.isCancelled()) {
			ItemStack item = event.getEntity().getItemStack();
			if (item.getTypeId() == 373) {
				Brew.remove(item);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityCombust(EntityCombustEvent event) {
		if (!event.isCancelled()) {
			Entity entity = event.getEntity();
			if (entity.getType().getTypeId() == 1) {
				if (entity instanceof Item) {
					ItemStack item = ((Item) entity).getItemStack();
					if (item.getTypeId() == 373) {
						Brew.remove(item);
					}
				}
			}
		}
	}

}
