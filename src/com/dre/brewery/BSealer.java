package com.dre.brewery;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public class BSealer implements InventoryHolder {
	public static final String TABLE_NAME = "§eBrew Sealing Table";

	private final Inventory inventory;
	private final Player player;
	private short[] slotTime = new short[9];
	ItemStack[] contents = null;
	BukkitTask task;

	public BSealer(Player player) {
		this.player = player;
		inventory = P.p.getServer().createInventory(this, InventoryType.DISPENSER, "Brew Sealing Table");
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
					if (playerValid) {
						player.playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, 1, 1.5f + (float) (Math.random() * 0.2));
					}
				}
			} else if (slotTime[i] >= 0) {
				slotTime[i]++;
			}
		}
	}


	public static void registerRecipe() {
		ItemStack sealingTableItem = new ItemStack(Material.SMOKER);
		ItemMeta meta = P.p.getServer().getItemFactory().getItemMeta(Material.SMOKER);
		if (meta == null) return;
		meta.setDisplayName(TABLE_NAME);
		sealingTableItem.setItemMeta(meta);

		ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(P.p, "SealingTable"), sealingTableItem);
		recipe.shape("bb ",
					"ww ",
					"ww ");
		recipe.setIngredient('b', Material.GLASS_BOTTLE);
		recipe.setIngredient('w', new RecipeChoice.MaterialChoice(Tag.PLANKS));

		P.p.getServer().addRecipe(recipe);
	}
}
