package com.dre.brewery.listeners;

import com.dre.brewery.BDistiller;
import com.dre.brewery.BSealer;
import com.dre.brewery.Barrel;
import com.dre.brewery.Brew;
import com.dre.brewery.MCBarrel;
import com.dre.brewery.P;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.lore.BrewLore;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Level;

public class InventoryListener implements Listener {

	/* === Recreating manually the prior BrewEvent behavior. === */
	private HashSet<UUID> trackedBrewmen = new HashSet<>();

	/**
	 * Start tracking distillation for a person when they open the brewer window.
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
	 * <p>Note that server restart will halt any ongoing brewing processes and
	 * they will _not_ restart until a new click event.
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

		BDistiller.distillerClick((BrewerInventory) event.getInventory());
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBrew(BrewEvent event) {
		if (P.use1_9) {
			if (BDistiller.hasBrew(event.getContents(), BDistiller.getDistillContents(event.getContents())) != 0) {
				event.setCancelled(true);
			}
			return;
		}
		if (BDistiller.runDistill(event.getContents(), BDistiller.getDistillContents(event.getContents()))) {
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
				assert potion != null;
				if (P.use1_11) {
					// Convert potions from 1.10 to 1.11 for new color
					if (potion.getColor() == null) {
						Brew brew = Brew.get(potion);
						if (brew != null) {
							brew.convertPre1_11(item);
						}
					}
				} else {
					// convert potions from 1.8 to 1.9 for color and to remove effect descriptions
					if (P.use1_9 && !potion.hasItemFlag(ItemFlag.HIDE_POTION_EFFECTS)) {
						Brew brew = Brew.get(potion);
						if (brew != null) {
							brew.convertPre1_9(item);
						}
					}
				}
				/*Brew brew = Brew.get(item);
				if (brew != null) {
					brew.touch();
				}*/
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
			assert meta != null;
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
		if (!MCBarrel.enableAging) return;

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

	// Handle the Brew Sealer Inventory
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onInventoryClickBSealer(InventoryClickEvent event) {
		if (!P.use1_13) return;
		InventoryHolder holder = event.getInventory().getHolder();
		if (!(holder instanceof BSealer)) {
			return;
		}
		((BSealer) holder).clickInv();
	}

	//public static boolean opening = false;

	@SuppressWarnings("deprecation")
	@EventHandler(ignoreCancelled = false)
	public void onInventoryOpenLegacyConvert(InventoryOpenEvent event) {
		if (Brew.noLegacy()) {
			return;
		}
		if (event.getInventory().getType() == InventoryType.PLAYER) {
			return;
		}
		for (ItemStack item : event.getInventory().getContents()) {
			if (item != null && item.getType() == Material.POTION) {
				int uid = Brew.getUID(item);
				// Check if the uid exists first, otherwise it will log that it can't find the id
				if (uid < 0 && Brew.legacyPotions.containsKey(uid)) {
					// This will convert the Brew
					Brew.get(item);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryOpen(InventoryOpenEvent event) {
		if (!P.use1_14) return;
		if (!MCBarrel.enableAging) return;

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
	public void onHopperPickupPuke(InventoryPickupItemEvent event) {
		if (event.getItem().getPickupDelay() > 1000 && event.getItem().getItemStack().getType() == BConfig.pukeItem) {
			event.setCancelled(true);
		}
	}

	// Block taking out items from running distillers,
	// Convert Color Lore from MC Barrels back into normal color on taking out
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	public void onHopperMove(InventoryMoveItemEvent event) {
		if (event.getDestination().getType() == InventoryType.BREWING) {
			dealWithBrewery(event.getItem(), (BrewerInventory) event.getDestination());
		}

		if (event.getSource().getType() == InventoryType.BREWING) {
			dealWithBrewery(event.getItem(), (BrewerInventory) event.getSource());
		}

		if (event.getSource().getType() == InventoryType.BARREL) {
			dealWithBarrel(event);
		}
	}

	private void dealWithBarrel(InventoryMoveItemEvent event){
		if (!P.use1_14) return;
		ItemStack item = event.getItem();
		if (item.getType() != Material.POTION || !Brew.isBrew(item)) {
			return;
		}
		PotionMeta meta = (PotionMeta) item.getItemMeta();
		assert meta != null;
		if (!BrewLore.hasColorLore(meta)) {
			return;
		}
		// has color lore, convert lore back to normal
		Brew brew = Brew.get(meta);
		if (brew != null) {
			BrewLore lore = new BrewLore(brew, meta);
			lore.convertLore(false);
			lore.write();
			item.setItemMeta(meta);
			event.setItem(item);
		}
	}

	private void dealWithBrewery(ItemStack item, BrewerInventory inventory){
		if (item.getType() != Material.POTION || !Brew.isBrew(item)) {
			return;
		}
		BDistiller.distillerClick(inventory);
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!P.use1_13) return;
		if (event.getInventory().getHolder() instanceof BSealer) {
			((BSealer) event.getInventory().getHolder()).closeInv();
		}

		if (!P.use1_14) return;

		// Barrel Closing Sound
		if (event.getInventory().getHolder() instanceof Barrel) {
			Barrel barrel = ((Barrel) event.getInventory().getHolder());
			barrel.playClosingSound();
		}

		// Check for MC Barrel
		if (MCBarrel.enableAging && event.getInventory().getType() == InventoryType.BARREL) {
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
