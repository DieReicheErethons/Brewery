package com.dre.brewery;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * The Sealing Inventory that is being checked for Brews and seals them after a second.
 * <p>Class doesn't load in mc <= 1.12 (Can't find RecipeChoice, BlockData and NamespacedKey)
 */
public class BSealer implements InventoryHolder {
	public static final NamespacedKey TAG_KEY = new NamespacedKey(P.p, "SealingTable");
	public static boolean recipeRegistered = false;
	public static boolean inventoryHolderWorking = true;

	private final Inventory inventory;
	private final Player player;
	private short[] slotTime = new short[9];
	private ItemStack[] contents = null;
	private BukkitTask task;

	public BSealer(Player player) {
		this.player = player;
		if (inventoryHolderWorking) {
			Inventory inv = P.p.getServer().createInventory(this, InventoryType.DISPENSER, P.p.languageReader.get("Etc_SealingTable"));
			// Inventory Holder (for DISPENSER, ...) is only passed in Paper, not in Spigot. Doing inventory.getHolder() will return null in spigot :/
			if (inv.getHolder() == this) {
				inventory = inv;
				return;
			} else {
				inventoryHolderWorking = false;
			}
		}
		inventory = P.p.getServer().createInventory(this, 9, P.p.languageReader.get("Etc_SealingTable"));
	}

	@Override
	public @NotNull Inventory getInventory() {
		return inventory;
	}


	public void clickInv() {
		contents = null;
		if (task == null) {
			task = P.p.getServer().getScheduler().runTaskTimer(P.p, this::itemChecking, 1, 1);
		}
	}

	public void closeInv() {
		if (task != null) {
			task.cancel();
			task = null;
		}
		contents = inventory.getContents();
		for (ItemStack item : contents) {
			if (item != null && item.getType() != Material.AIR) {
				player.getWorld().dropItemNaturally(player.getLocation(), item);
			}
		}
		contents = null;
		inventory.clear();
	}

	private void itemChecking() {
		if (contents == null) {
			contents = inventory.getContents();
			for (int i = 0; i < slotTime.length; i++) {
				if (contents[i] == null || contents[i].getType() != Material.POTION) {
					slotTime[i] = -1;
				} else if (slotTime[i] < 0) {
					slotTime[i] = 0;
				}
			}
		}
		boolean playerValid = player.isValid() && !player.isDead();
		for (int i = 0; i < slotTime.length; i++) {
			if (slotTime[i] > 20) {
				slotTime[i] = -1;
				Brew brew = Brew.get(contents[i]);
				if (brew != null && !brew.isStripped()) {
					brew.seal(contents[i]);
					if (playerValid && P.use1_9) {
						player.playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, 1, 1.5f + (float) (Math.random() * 0.2));
					}
				}
			} else if (slotTime[i] >= 0) {
				slotTime[i]++;
			}
		}
	}

	public static boolean isBSealer(Block block) {
		if (P.use1_14 && block.getType() == Material.SMOKER) {
			Container smoker = (Container) block.getState();
			if (smoker.getCustomName() != null) {
				if (smoker.getCustomName().equals("§e" + P.p.languageReader.get("Etc_SealingTable"))) {
					return true;
				} else
					return smoker.getPersistentDataContainer().has(TAG_KEY, PersistentDataType.BYTE);
			}
		}
		return false;
	}

	public static void blockPlace(ItemStack item, Block block) {
		if (item.getType() == Material.SMOKER && item.hasItemMeta()) {
			ItemMeta itemMeta = item.getItemMeta();
			assert itemMeta != null;
			if ((itemMeta.hasDisplayName() && itemMeta.getDisplayName().equals("§e" + P.p.languageReader.get("Etc_SealingTable"))) ||
				itemMeta.getPersistentDataContainer().has(BSealer.TAG_KEY, PersistentDataType.BYTE)) {
				Container smoker = (Container) block.getState();
				// Rotate the Block 180° so it doesn't look like a Smoker
				Directional dir = (Directional) smoker.getBlockData();
				dir.setFacing(dir.getFacing().getOppositeFace());
				smoker.setBlockData(dir);
				smoker.getPersistentDataContainer().set(BSealer.TAG_KEY, PersistentDataType.BYTE, (byte)1);
				smoker.update();
			}
		}
	}

	public static void registerRecipe() {
		recipeRegistered = true;
		ItemStack sealingTableItem = new ItemStack(Material.SMOKER);
		ItemMeta meta = P.p.getServer().getItemFactory().getItemMeta(Material.SMOKER);
		if (meta == null) return;
		meta.setDisplayName("§e" + P.p.languageReader.get("Etc_SealingTable"));
		meta.getPersistentDataContainer().set(TAG_KEY, PersistentDataType.BYTE, (byte)1);
		sealingTableItem.setItemMeta(meta);

		ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(P.p, "SealingTable"), sealingTableItem);
		recipe.shape("bb ",
					"ww ",
					"ww ");
		recipe.setIngredient('b', Material.GLASS_BOTTLE);
		recipe.setIngredient('w', new RecipeChoice.MaterialChoice(Tag.PLANKS));

		P.p.getServer().addRecipe(recipe);
	}

	public static void unregisterRecipe() {
		recipeRegistered = false;
		//P.p.getServer().removeRecipe(new NamespacedKey(P.p, "SealingTable"));    1.15 Method
		Iterator<Recipe> recipeIterator = P.p.getServer().recipeIterator();
		while (recipeIterator.hasNext()) {
			Recipe next = recipeIterator.next();
			if (next instanceof ShapedRecipe && ((ShapedRecipe) next).getKey().equals(TAG_KEY)) {
				recipeIterator.remove();
				return;
			}
		}
	}
}
