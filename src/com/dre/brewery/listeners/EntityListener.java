package com.dre.brewery.listeners;

import com.dre.brewery.Barrel;
import com.dre.brewery.Brew;
import com.dre.brewery.P;
import com.dre.brewery.api.events.barrel.BarrelDestroyEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

public class EntityListener implements Listener {

	// Remove the Potion from Brew when it despawns
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onItemDespawn(ItemDespawnEvent event) {
		if (Brew.noLegacy()) return;
		ItemStack item = event.getEntity().getItemStack();
		if (item.getType() == Material.POTION) {
			Brew.removeLegacy(item);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityCombust(EntityCombustEvent event) {
		if (Brew.noLegacy()) return;
		Entity entity = event.getEntity();
		if (entity.getType() == EntityType.DROPPED_ITEM) {
			if (entity instanceof Item) {
				ItemStack item = ((Item) entity).getItemStack();
				if (item.getType() == Material.POTION) {
					Brew.removeLegacy(item);
				}
			}
		}
	}

	//  --- Barrel Breaking ---

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onExplode(EntityExplodeEvent event) {
		ListIterator<Block> iter = event.blockList().listIterator();
		if (!iter.hasNext()) return;
		Set<BarrelDestroyEvent> breakEvents = new HashSet<>(6);
		Block block;
		blocks: while (iter.hasNext()) {
			block = iter.next();
			if (!breakEvents.isEmpty()) {
				for (BarrelDestroyEvent breakEvent : breakEvents) {
					if (breakEvent.getBarrel().hasBlock(block)) {
						if (breakEvent.isCancelled()) {
							iter.remove();
						}
						continue blocks;
					}
				}
			}
			Barrel barrel = Barrel.get(block);
			if (barrel != null) {
				BarrelDestroyEvent breakEvent = new BarrelDestroyEvent(barrel, block, BarrelDestroyEvent.Reason.EXPLODED, null);
				// Listened to by LWCBarrel (IntegrationListener)
				P.p.getServer().getPluginManager().callEvent(breakEvent);
				breakEvents.add(breakEvent);
				if (breakEvent.isCancelled()) {
					iter.remove();
				} else {
					barrel.remove(block, null);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockChange(EntityChangeBlockEvent event) {
		if (Barrel.get(event.getBlock()) != null) {
			event.setCancelled(true);
		}
	}

}
