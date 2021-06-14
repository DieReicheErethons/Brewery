package com.dre.brewery;

import com.dre.brewery.lore.BrewLore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Updated for 1.9 to replicate the "Brewing" process for distilling.
 * Because of how metadata has changed, the brewer no longer triggers as previously described.
 * So, I've added some event tracking and manual forcing of the brewing "animation" if the
 *  set of ingredients in the brewer can be distilled.
 * Nothing here should interfere with vanilla brewing.
 *
 * @author ProgrammerDan (1.9 distillation update only)
 */
public class BDistiller {

	private static final int DISTILLTIME = 400;
	private static Map<Block, BDistiller> trackedDistillers = new HashMap<>();

	private int taskId;
	private int runTime = -1;
	private int brewTime = -1;
	private Block standBlock;
	private int fuel;

	public BDistiller(Block standBlock, int fuel) {
		this.standBlock = standBlock;
		this.fuel = fuel;
	}

	public void cancelDistill() {
		Bukkit.getScheduler().cancelTask(taskId); // cancel prior
	}

	public void start() {
		taskId = new DistillRunnable().runTaskTimer(P.p, 2L, 1L).getTaskId();
	}

	public static void distillerClick(InventoryClickEvent event) {
		BrewerInventory standInv = (BrewerInventory) event.getInventory();
		final Block standBlock = standInv.getHolder().getBlock();

		// If we were already tracking the brewer, cancel any ongoing event due to the click.
		BDistiller distiller = trackedDistillers.get(standBlock);
		if (distiller != null) {
			distiller.cancelDistill();
			standInv.getHolder().setBrewingTime(0); // Fixes brewing continuing without fuel for normal potions
			standInv.getHolder().update();
		}
		final int fuel = standInv.getHolder().getFuelLevel();

		// Now check if we should bother to track it.
		distiller = new BDistiller(standBlock, fuel);
		trackedDistillers.put(standBlock, distiller);
		distiller.start();
	}

	public static boolean isTrackingDistiller(Block block) {
		return trackedDistillers.containsKey(block);
	}

	// Returns a Brew or null for every Slot in the BrewerInventory
	public static Brew[] getDistillContents(BrewerInventory inv) {
		ItemStack item;
		Brew[] contents = new Brew[3];
		for (int slot = 0; slot < 3; slot++) {
			item = inv.getItem(slot);
			if (item != null) {
				contents[slot] = Brew.get(item);
			}
		}
		return contents;
	}

	public static void checkContents(BrewerInventory inv, Brew[] contents) {
		ItemStack item;
		for (int slot = 0; slot < 3; slot++) {
			if (contents[slot] != null) {
				item = inv.getItem(slot);
				if (!Brew.isBrew(item)) {
					contents[slot] = null;
				}
			}
		}
	}

	public static byte hasBrew(BrewerInventory brewer, Brew[] contents) {
		ItemStack item = brewer.getItem(3); // ingredient
		boolean glowstone = (item != null && Material.GLOWSTONE_DUST == item.getType()); // need dust in the top slot.
		byte customFound = 0;
		for (Brew brew : contents) {
			if (brew != null) {
				if (!glowstone) {
					return 1;
				}
				if (brew.canDistill()) {
					return 2;
				} else {
					customFound = 1;
				}
			}
		}
		return customFound;
	}

	public static boolean runDistill(BrewerInventory inv, Brew[] contents) {
		boolean custom = false;
		for (int slot = 0; slot < 3; slot++) {
			if (contents[slot] == null) continue;
			if (contents[slot].canDistill()) {
				// is further distillable
				custom = true;
			} else {
				contents[slot] = null;
			}
		}
		if (custom) {
			Brew.distillAll(inv, contents);
			return true;
		}
		return false;
	}

	public static int getLongestDistillTime(Brew[] contents) {
		int bestTime = 0;
		int time;
		for (int slot = 0; slot < 3; slot++) {
			if (contents[slot] == null) continue;
			time = contents[slot].getDistillTimeNextRun();
			if (time == 0) {
				// Undefined Potion needs 40 seconds
				time = 800;
			}
			if (time > bestTime) {
				bestTime = time;
			}
		}
		if (bestTime > 0) {
			return bestTime;
		}
		return 800;
	}

	public static void showAlc(BrewerInventory inv, Brew[] contents) {
		for (int slot = 0; slot < 3; slot++) {
			if (contents[slot] != null) {
				// Show Alc in lore
				ItemStack item = inv.getItem(slot);
				PotionMeta meta = (PotionMeta) item.getItemMeta();
				BrewLore brewLore = new BrewLore(contents[slot], meta);
				brewLore.updateAlc(true);
				brewLore.write();
				item.setItemMeta(meta);
			}
		}
	}

	public class DistillRunnable extends BukkitRunnable {
		private Brew[] contents = null;

		@Override
		public void run() {
			BlockState now = standBlock.getState();
			if (now instanceof BrewingStand) {
				BrewingStand stand = (BrewingStand) now;
				if (brewTime == -1) { // check at the beginning for distillables
					if (!prepareForDistillables(stand)) {
						return;
					}
				}

				brewTime--; // count down.
				stand.setBrewingTime((int) ((float) brewTime / ((float) runTime / (float) DISTILLTIME)) + 1);

				if (brewTime <= 1) { // Done!
					contents = getDistillContents(stand.getInventory()); // Get the contents again at the end just in case
					stand.setBrewingTime(0);
					stand.update();
					if (!runDistill(stand.getInventory(), contents)) {
						this.cancel();
						trackedDistillers.remove(standBlock);
						P.p.debugLog("All done distilling");
					} else {
						brewTime = -1; // go again.
						P.p.debugLog("Can distill more! Continuing.");
					}
				} else {
					stand.update();
				}
			} else {
				this.cancel();
				trackedDistillers.remove(standBlock);
				P.p.debugLog("The block was replaced; not a brewing stand.");
			}
		}

		private boolean prepareForDistillables(BrewingStand stand) {
			BrewerInventory inventory = stand.getInventory();
			if (contents == null) {
				contents = getDistillContents(inventory);
			} else {
				checkContents(inventory, contents);
			}
			switch (hasBrew(inventory, contents)) {
				case 1:
					// Custom potion but not for distilling. Stop any brewing and cancel this task
					if (stand.getBrewingTime() > 0) {
						if (P.use1_11) {
							// The trick below doesnt work in 1.11, but we dont need it anymore
							// This should only happen with older Brews that have been made with the old Potion Color System
							// This causes standard potions to not brew in the brewing stand if put together with Brews, but the bubble animation will play
							stand.setBrewingTime(Short.MAX_VALUE);
						} else {
							// Brewing time is sent and stored as short
							// This sends a negative short value to the Client
							// In the client the Brewer will look like it is not doing anything
							stand.setBrewingTime(Short.MAX_VALUE << 1);
						}
						stand.setFuelLevel(fuel);
						stand.update();
					}
				case 0:
					// No custom potion, cancel and ignore
					this.cancel();
					trackedDistillers.remove(standBlock);
					showAlc(inventory, contents);
					P.p.debugLog("nothing to distill");
					return false;
				default:
					runTime = getLongestDistillTime(contents);
					brewTime = runTime;
					P.p.debugLog("using brewtime: " + runTime);

			}
			return true;
		}
	}
}
