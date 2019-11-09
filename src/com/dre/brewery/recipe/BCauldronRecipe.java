package com.dre.brewery.recipe;

import com.dre.brewery.P;
import com.dre.brewery.utility.Tuple;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BCauldronRecipe {
	public static List<BCauldronRecipe> recipes = new ArrayList<>();
	public static int numConfigRecipes;
	public static List<RecipeItem> acceptedCustom = new ArrayList<>(); // All accepted custom and other items
	public static Set<Material> acceptedSimple = EnumSet.noneOf(Material.class); // All accepted simple items
	public static Set<Material> acceptedMaterials = EnumSet.noneOf(Material.class); // Fast cache for all accepted Materials

	private String name;
	private List<RecipeItem> ingredients;
	//private List<String> particles
	private PotionColor color;
	private List<String> lore;
	private boolean saveInData; // If this recipe should be saved in data and loaded again when the server restarts. Applicable to non-config recipes


	/**
	 * A New Cauldron Recipe with the given name
	 * Use new BCauldronRecipe.Builder() for easier Cauldron Recipe Creation
	 *
	 * @param name Name of the Cauldron Recipe
	 */
	public BCauldronRecipe(String name) {
		this.name = name;
		color = PotionColor.CYAN;
	}

	@Nullable
	public static BCauldronRecipe fromConfig(ConfigurationSection cfg, String id) {

		String name = cfg.getString(id + ".name");
		if (name != null) {
			name = P.p.color(name);
		} else {
			P.p.errorLog("Missing name for Cauldron-Recipe: " + id);
			return null;
		}

		BCauldronRecipe recipe = new BCauldronRecipe(name);

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


	// Getter

	@NotNull
	public String getName() {
		return name;
	}

	@NotNull
	public List<RecipeItem> getIngredients() {
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

	public boolean isSaveInData() {
		return saveInData;
	}


	// Setter

	/**
	 * When Changing ingredients, Accepted Lists have to be updated in BCauldronRecipe
	 */
	public void setIngredients(@NotNull List<RecipeItem> ingredients) {
		this.ingredients = ingredients;
	}

	public void setColor(@NotNull PotionColor color) {
		this.color = color;
	}

	public void setLore(List<String> lore) {
		this.lore = lore;
	}

	public void setSaveInData(boolean saveInData) {
		this.saveInData = saveInData;
	}

	/**
	 * Find how much these ingredients match the given ones from 0-10.
	 * If any ingredient is missing, returns 0
	 * If all Ingredients and their amounts are equal, returns 10
	 * Returns something between 0 and 10 if all ingredients present, but differing amounts, depending on how much the amount differs.
	 */
	public float getIngredientMatch(List<Ingredient> items) {
		if (items.size() < ingredients.size()) {
			return 0;
		}
		float match = 10;
		search: for (RecipeItem recipeIng : ingredients) {
			for (Ingredient ing : items) {
				if (recipeIng.matches(ing)) {
					double difference = Math.abs(recipeIng.getAmount() - ing.getAmount());
					if (difference >= 1000) {
						return 0;
					}
					// The Item Amount is the determining part here, the higher the better.
					// But let the difference in amount to what the recipe expects have a tiny factor as well.
					// This way for the same amount, the recipe with the lower difference wins.
					double factor = ing.getAmount() * (1.0 - (difference / 1000.0)) ;
					//double mod = 0.1 + (0.9 * Math.exp(-0.03 * difference)); // logarithmic curve from 1 to 0.1
					double mod = 1 + (0.9 * -Math.exp(-0.03 * factor)); // logarithmic curve from 0.1 to 1, small for a low factor

					P.p.debugLog("Mod for " + recipeIng + ": " + mod);




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

	public void updateAcceptedLists() {
		for (RecipeItem ingredient : getIngredients()) {
			if (ingredient.hasMaterials()) {
				BCauldronRecipe.acceptedMaterials.addAll(ingredient.getMaterials());
			}
			if (ingredient instanceof SimpleItem) {
				BCauldronRecipe.acceptedSimple.add(((SimpleItem) ingredient).getMaterial());
			} else {
				// Add it as acceptedCustom
				if (!BCauldronRecipe.acceptedCustom.contains(ingredient)) {
					BCauldronRecipe.acceptedCustom.add(ingredient);
				}
			}
		}
	}

	@Override
	public String toString() {
		return "BCauldronRecipe{" + name + '}';
	}

	@Nullable
	public static BCauldronRecipe get(String name) {
		for (BCauldronRecipe recipe : recipes) {
			if (recipe.name.equalsIgnoreCase(name)) {
				return recipe;
			}
		}
		return null;
	}

	/**
	 * Gets a Modifiable Sublist of the CauldronRecipes that are loaded by config
	 * Changes are directly reflected by the main list of all recipes
	 * Changes to the main List of all CauldronRecipes will make the reference to this sublist invalid
	 *
	 * After adding or removing elements, CauldronRecipes.numConfigRecipes MUST be updated!
	 */
	public static List<BCauldronRecipe> getConfigRecipes() {
		return recipes.subList(0, numConfigRecipes);
	}

	/**
	 * Gets a Modifiable Sublist of the CauldronRecipes that are added by plugins
	 * Changes are directly reflected by the main list of all recipes
	 * Changes to the main List of all CauldronRecipes will make the reference to this sublist invalid
	 */
	public static List<BCauldronRecipe> getAddedRecipes() {
		return recipes.subList(numConfigRecipes, recipes.size());
	}

	/**
	 * Gets the main List of all CauldronRecipes
	 */
	public static List<BCauldronRecipe> getAllRecipes() {
		return recipes;
	}

	/*public static boolean acceptItem(ItemStack item) {
		if (acceptedMaterials.contains(item.getType())) {
			// Extremely fast way to check for most items
			return true;
		}
		if (!item.hasItemMeta()) {
			return false;
		}
		// If the Item is not on the list, but customized, we have to do more checks
		ItemMeta meta = item.getItemMeta();
		assert meta != null;
		if (meta.hasDisplayName() || meta.hasLore()) {
			for (BItem bItem : acceptedCustom) {
				if (bItem.matches(item)) {
					return true;
				}
			}
		}
		return false;
	}

	@Nullable
	public static RecipeItem acceptItem(ItemStack item) {
		if (!acceptedMaterials.contains(item.getType()) && !item.hasItemMeta()) {
			// Extremely fast way to check for most items
			return null;
		}
		// If the Item is on the list, or customized, we have to do more checks
		for (RecipeItem rItem : acceptedItems) {
			if (rItem.matches(item)) {
				return rItem;
			}
		}
		return null;
	}*/

	public static class Builder {
		private BCauldronRecipe recipe;

		public Builder(String name) {
			recipe = new BCauldronRecipe(name);
		}


		public Builder addIngredient(RecipeItem... item) {
			if (recipe.ingredients == null) {
				recipe.ingredients = new ArrayList<>();
			}
			Collections.addAll(recipe.ingredients, item);
			return this;
		}

		public Builder addIngredient(ItemStack... item) {
			if (recipe.ingredients == null) {
				recipe.ingredients = new ArrayList<>();
			}
			for (ItemStack i : item) {
				recipe.ingredients.add(new CustomItem(i));
			}
			return this;
		}

		public Builder color(String colorString) {
			recipe.color = PotionColor.fromString(colorString);
			return this;
		}

		public Builder color(PotionColor color) {
			recipe.color = color;
			return this;
		}

		public Builder color(Color color) {
			recipe.color = PotionColor.fromColor(color);
			return this;
		}

		public Builder addLore(String line) {
			if (recipe.lore == null) {
				recipe.lore = new ArrayList<>();
			}
			recipe.lore.add(line);
			return this;
		}

		public BCauldronRecipe get() {
			if (recipe.name == null) {
				throw new IllegalArgumentException("CauldronRecipe name is null");
			}
			if (BCauldronRecipe.get(recipe.getName()) != null) {
				throw new IllegalArgumentException("CauldronRecipe with name " + recipe.getName() + " already exists");
			}
			if (recipe.color == null) {
				throw new IllegalArgumentException("CauldronRecipe has no color");
			}
			if (recipe.ingredients == null || recipe.ingredients.isEmpty()) {
				throw new IllegalArgumentException("CauldronRecipe has no ingredients");
			}
			return recipe;
		}
	}
}
