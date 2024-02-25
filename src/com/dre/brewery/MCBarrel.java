package com.dre.brewery;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Barrel;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class MCBarrel {

	public static final byte OAK = 2;
	public static final String TAG = "Btime";
	public static int maxBrews = 6;
	public static boolean enableAging = true;

	public static long mcBarrelTime; // Globally stored Barrel time. Difference between this and the time stored on each mc-barrel will give the barrel age time
	public static List<MCBarrel> openBarrels = new ArrayList<>();

	private byte brews = -1; // How many Brewery Brews are in this Barrel
	private final Inventory inv;
	private final int invSize;


	public MCBarrel(Inventory inv) {
		this.inv = inv;
		invSize = inv.getSize();
	}


	// Now Opening this Barrel for a player
	public void open() {
		// if nobody had the inventory opened
		if (inv.getViewers().size() == 1 && inv.getHolder() instanceof org.bukkit.block.Barrel) {
			Barrel barrel = (Barrel) inv.getHolder();
			PersistentDataContainer data = barrel.getPersistentDataContainer();
			NamespacedKey key = new NamespacedKey(BreweryPlugin.getInstance(), TAG);
			if (!data.has(key, PersistentDataType.LONG)) return;

			// Get the difference between the time that is stored on the Barrel and the current stored global mcBarrelTime
			long time = mcBarrelTime - data.getOrDefault(key, PersistentDataType.LONG, mcBarrelTime);
			data.remove(key);
			barrel.update();
			BreweryPlugin.getInstance().debugLog("Barrel Time since last open: " + time);

			if (time > 0) {
				brews = 0;
				// if inventory contains potions
				if (inv.contains(Material.POTION)) {
					long loadTime = System.nanoTime();
					for (ItemStack item : inv.getContents()) {
						if (item != null) {
							Brew brew = Brew.get(item);
							if (brew != null && !brew.isStatic()) {
								if (brews < maxBrews || maxBrews < 0) {
									// The time is in minutes, but brew.age() expects time in mc-days
									brew.age(item, ((float) time) / 20f, OAK);
								}
								brews++;
							}
						}
					}
					if (BreweryPlugin.debug) {
						loadTime = System.nanoTime() - loadTime;
						float ftime = (float) (loadTime / 1000000.0);
						BreweryPlugin.getInstance().debugLog("opening MC Barrel with potions (" + ftime + "ms)");
					}
				}
			}
		}
	}

	// Closing Inventory. Check if we need to set a time on the Barrel
	public void close() {
		if (inv.getViewers().size() == 1) {
			// This is the last viewer
			for (ItemStack item : inv.getContents()) {
				if (item != null) {
					if (Brew.isBrew(item)) {
						// We found a brew, so set time on this Barrel
						if (inv.getHolder() instanceof org.bukkit.block.Barrel) {
							Barrel barrel = (Barrel) inv.getHolder();
							PersistentDataContainer data = barrel.getPersistentDataContainer();
							data.set(new NamespacedKey(BreweryPlugin.getInstance(), TAG), PersistentDataType.LONG, mcBarrelTime);
							barrel.update();
						}
						return;
					}
				}
			}
			// No Brew found, ignore this Barrel
		}
	}

	public void countBrews() {
		brews = 0;
		for (ItemStack item : inv.getContents()) {
			if (item != null) {
				if (Brew.isBrew(item)) {
					brews++;
				}
			}
		}
	}

	public Inventory getInventory() {
		return inv;
	}


	public static void onUpdate() {
		if (enableAging) {
			mcBarrelTime++;
		}
	}

	// Used to visually stop Players from placing more than 6 (configurable) brews in the MC Barrels.
	// There are still methods to place more Brews in that would be too tedious to catch.
	// This is only for direct visual Notification, the age routine above will never age more than 6 brews in any case.
	public void clickInv(InventoryClickEvent event) {
		if (maxBrews >= invSize || maxBrews < 0) {
			// There are enough brews allowed to fill the inventory, we don't need to keep track
			return;
		}
		boolean adding = false;
		switch (event.getAction()) {
			case PLACE_ALL:
			case PLACE_ONE:
			case PLACE_SOME:
			case SWAP_WITH_CURSOR:
				// Placing Brew in MC Barrel
				if (event.getCursor() != null && event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.BARREL && event.getCursor().getType() == Material.POTION) {
					if (Brew.isBrew(event.getCursor())) {
						if (event.getAction() == InventoryAction.SWAP_WITH_CURSOR && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.POTION) {
							if (Brew.isBrew(event.getCurrentItem())) {
								// The item we are swapping with is also a brew, dont change the count and allow
								break;
							}
						}
						adding = true;
					}
				}
				break;
			case MOVE_TO_OTHER_INVENTORY:
				if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.POTION && event.getClickedInventory() != null) {
					if (event.getClickedInventory().getType() == InventoryType.BARREL) {
						// Moving Brew out of MC Barrel
						if (Brew.isBrew(event.getCurrentItem())) {
							if (brews == -1) {
								countBrews();
							}
							brews--;
						}
						break;
					} else if (event.getClickedInventory().getType() == InventoryType.PLAYER) {
						// Moving Brew into MC Barrel
						if (Brew.isBrew(event.getCurrentItem())) {
							adding = true;
						}
					}
				}
				break;

			case PICKUP_ALL:
			case PICKUP_ONE:
			case PICKUP_HALF:
			case PICKUP_SOME:
			case COLLECT_TO_CURSOR:
				// Pickup Brew from MC Barrel
				if (event.getCurrentItem() != null && event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.BARREL && event.getCurrentItem().getType() == Material.POTION) {
					if (Brew.isBrew(event.getCurrentItem())) {
						if (brews == -1) {
							countBrews();
						}
						brews--;
					}
				}
				break;
			case HOTBAR_MOVE_AND_READD:
			case HOTBAR_SWAP:
				brews = -1;
				break;
			default:
				return;
		}
		if (adding) {
			if (brews == -1) {
				countBrews();
			}
			if (brews >= maxBrews) {
				event.setCancelled(true);
				BreweryPlugin.getInstance().msg(event.getWhoClicked(), BreweryPlugin.getInstance().languageReader.get("Player_BarrelFull"));
			} else {
				brews++;
			}
		}
	}

}
