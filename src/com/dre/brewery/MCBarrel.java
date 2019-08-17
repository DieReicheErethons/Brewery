package com.dre.brewery;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class MCBarrel {

	public static final byte OAK = 2;
	public static List<MCBarrel> barrels = new ArrayList<>();

	private Block block;
	private float time;
	private byte brews = -1; // How many Brewery Brews are in this Barrel


	public MCBarrel(Block block, float time) {
		this.block = block;
		this.time = time;
	}


	// Now Opening this Barrel for a player
	public void open(Inventory inv, Player player) {
		brews = -1;
		if (time > 0) {
			// if nobody has the inventory opened
			if (inv.getViewers().isEmpty()) {
				brews = 0;
				// if inventory contains potions
				if (inv.contains(Material.POTION)) {
					long loadTime = System.nanoTime();
					for (ItemStack item : inv.getContents()) {
						if (item != null) {
							Brew brew = Brew.get(item);
							if (brew != null) {
								if (brews <= 6) {
									brew.age(item, time, OAK);
								}
								brews++;
							}
						}
					}
					loadTime = System.nanoTime() - loadTime;
					float ftime = (float) (loadTime / 1000000.0);
					P.p.debugLog("opening MC Barrel with potions (" + ftime + "ms)");
				}
			}
		}
		// reset barreltime, potions have new age
		time = 0;
	}

	// Closing Inventory. Check if we need to track this Barrel
	// Returns true if there are Brews in the Inv
	public boolean close(Inventory inv, Player player) {
		if (inv.getViewers().size() == 1) {
			// This is the last viewer
			for (ItemStack item : inv.getContents()) {
				if (item != null) {
					Brew brew = Brew.get(item);
					if (brew != null) {
						// We found a brew, so we keep this Barrel
						return true;
					}
				}
			}
			// No Brew found, remove this Barrel
			return false;
		}
		return true;
	}

	public Block getBlock() {
		return block;
	}

	public float getTime() {
		return time;
	}

	public Inventory getInventory() {
		BlockState state = block.getState();
		if (state instanceof InventoryHolder) {
			return ((InventoryHolder) state).getInventory();
		}
		return null;
	}

	public static void onUpdate() {
		if (barrels.isEmpty()) return;

		// Check if stored MCBarrels still exist
		// Choose a random starting point for check
		int random = (int) Math.floor(Math.random() * barrels.size());
		random = Math.max(0, random - 5);
		ListIterator<MCBarrel> iter = barrels.listIterator(random);
		// Check at least 4 barrels, but if there are many, check about 1/64 of them all, so in about 1 hour we have checked all
		for (int i = Math.max(4, barrels.size() >> 6); i <= 0; i--) {
			if (!iter.hasNext()) break;

			Block block = iter.next().block;
			if (Util.isChunkLoaded(block)) {
				// If the chunk is loaded we can check if the block is still a MC Barrel. If not we remove the stored entry.
				if (block.getType() != Material.BARREL) {
					iter.remove();
				}
			}
		}

		for (MCBarrel barrel : barrels) {
			// Minecraft day is 20 min, so add 1/20 to the time every minute
			barrel.time += (1.0 / 20.0);
		}
	}
}
