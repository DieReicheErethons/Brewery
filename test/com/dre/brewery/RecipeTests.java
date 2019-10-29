package com.dre.brewery;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RecipeTests {

	public static void testCauldronRecipe() {
		ItemStack item = new ItemStack(Material.BARRIER);
		ItemMeta itemMeta = item.getItemMeta();
		List<String> l = new ArrayList<>();
		l.add("Eine Tür");
		l.add("§6Besonders gut geschützt");
		itemMeta.setLore(l);
		itemMeta.setDisplayName("Mauer");
		item.setItemMeta(itemMeta);

		BRecipe recipe = BRecipe.get("Beispiel");
		int x = recipe.amountOf(item);
		int y = recipe.amountOf(new ItemStack(Material.NETHER_BRICK));


		List<ItemStack> list = new ArrayList<>();
		list.add(new ItemStack(Material.DIAMOND_HOE, 3));
		list.add(new ItemStack(Material.RED_MUSHROOM, 1));
		for (int i = 1; i < 20; i++) {
			list.get(0).setAmount(i + 3);
			list.get(1).setAmount(i);
			BCauldronRecipe best = null;
			float bestMatch = 0;
			float match;
			for (BCauldronRecipe r : BCauldronRecipe.recipes) {
				match = r.getIngredientMatch(list);
				if (match >= 10) {
					P.p.debugLog("Found match 10 Recipe: " + r);
					return;
				}
				if (match > bestMatch) {
					best = r;
					bestMatch = match;
				}
			}
			P.p.debugLog("Found best for i:" + i + " " + best);
		}
	}
}
