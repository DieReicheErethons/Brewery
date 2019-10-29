package com.dre.brewery;

import com.dre.brewery.utility.CustomItem;
import com.dre.brewery.utility.PotionColor;
import com.dre.brewery.utility.Tuple;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BCauldronRecipe {
	public static List<BCauldronRecipe> recipes = new ArrayList<>();
	public static Set<Material> acceptedMaterials = EnumSet.noneOf(Material.class);

	private String name;
	private List<Tuple<CustomItem, Integer>> ingredients; // Item and amount
	//private List<String> particles
	private PotionColor color;
	private List<String> lore;


	@Nullable
	public static BCauldronRecipe fromConfig(ConfigurationSection cfg, String id) {
		BCauldronRecipe recipe = new BCauldronRecipe();

		recipe.name = cfg.getString(id + ".name");
		if (recipe.name != null) {
			recipe.name = P.p.color(recipe.name);
		} else {
			P.p.errorLog("Missing name for Cauldron-Recipe: " + id);
			return null;
		}

		recipe.ingredients = BRecipe.loadIngredients(cfg, id);
		if (recipe.ingredients == null || recipe.ingredients.isEmpty()) {
			P.p.errorLog("No ingredients for Cauldron-Recipe: " + recipe.name);
			return null;
		}

		String col = cfg.getString(id + ".color");
		if (col != null) {
			recipe.color = PotionColor.fromString(col);
		} else {
			recipe.color = PotionColor.CYAN;
		}
		if (recipe.color == PotionColor.WATER && !col.equals("WATER")) {
			recipe.color = PotionColor.CYAN;
			// Don't throw error here as old mc versions will not know even the default colors
			//P.p.errorLog("Invalid Color '" + col + "' in Cauldron-Recipe: " + recipe.name);
			//return null;
		}


		List<Tuple<Integer,String>> lore = BRecipe.loadLore(cfg, id + ".lore");
		if (lore != null && !lore.isEmpty()) {
			recipe.lore = lore.stream().map(Tuple::second).collect(Collectors.toList());
		}

		return recipe;
	}

	@NotNull
	public String getName() {
		return name;
	}

	@NotNull
	public List<Tuple<CustomItem, Integer>> getIngredients() {
		return ingredients;
	}

	@NotNull
	public PotionColor getColor() {
		return color;
	}

	@Nullable
	public List<String> getLore() {
		return lore;
	}

	/**
	 * Find how much these ingredients match the given ones from 0-10.
	 * If any ingredient is missing, returns 0
	 * If all Ingredients and their amounts are equal, returns 10
	 * Returns something between 0 and 10 if all ingredients present, but differing amounts, depending on how much the amount differs.
	 */
	public float getIngredientMatch(List<ItemStack> items) {
		if (items.size() < ingredients.size()) {
			return 0;
		}
		float match = 10;
		search: for (Tuple<CustomItem, Integer> ing : ingredients) {
			for (ItemStack item : items) {
				if (ing.a().matches(item)) {
					double difference = Math.abs(ing.b() - item.getAmount());
					if (difference >= 1000) {
						return 0;
					}
					// The Item Amount is the determining part here, the higher the better.
					// But let the difference in amount to what the recipe expects have a tiny factor as well.
					// This way for the same amount, the recipe with the lower difference wins.
					double factor = item.getAmount() * (1.0 - (difference / 1000.0)) ;
					//double mod = 0.1 + (0.9 * Math.exp(-0.03 * difference)); // logarithmic curve from 1 to 0.1
					double mod = 1 + (0.9 * -Math.exp(-0.03 * factor)); // logarithmic curve from 0.1 to 1, small for a low factor

					P.p.debugLog("Mod for " + ing.a() + "/" + ing.b() + ": " + mod);
					assert mod >= 0.1;
					assert mod <= 1; // TODO Test




					match *= mod;
					continue search;
				}
			}
			return 0;
		}
		if (items.size() > ingredients.size()) {
			// If there are too many items in the List, multiply the match by 0.1 per Item thats too much
			float tooMuch = items.size() - ingredients.size();
			float mod = 0.1f / tooMuch;
			match *= mod;
		}
		P.p.debugLog("Match for Cauldron Recipe " + name + ": " + match);
		return match;
	}

	@Override
	public String toString() {
		return "BCauldronRecipe{" + name + '}';
	}
}
