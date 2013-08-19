package com.dre.brewery;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.meta.PotionMeta;

import com.dre.brewery.BRecipe;
import com.dre.brewery.Brew;

public class BIngredients {
	public static ArrayList<Material> possibleIngredients = new ArrayList<Material>();
	public static ArrayList<BRecipe> recipes = new ArrayList<BRecipe>();
	public static Map<Material, String> cookedNames = new HashMap<Material, String>();

	private Map<Material, Integer> ingredients = new HashMap<Material, Integer>();
	private int cookedTime;

	// Represents ingredients in Cauldron, Brew
	// Init a new BIngredients
	public BIngredients() {
	}

	// Load from File
	public BIngredients(Map<Material, Integer> ingredients, int cookedTime) {
		this.ingredients = ingredients;
		this.cookedTime = cookedTime;
	}

	//returns the recipe with the given name
	public static BRecipe getRecipeByName(String name) {
		for (BRecipe recipe : recipes) {
			if (recipe.getName(5).equalsIgnoreCase(name)) {
				return recipe;
			}
		}
		return null;
	}

	// Add an ingredient to this
	public void add(Material ingredient) {
		if (ingredients.containsKey(ingredient)) {
			int newAmount = ingredients.get(ingredient) + 1;
			ingredients.put(ingredient, newAmount);
		} else {
			this.ingredients.put(ingredient, 1);
		}
	}

	// returns an Potion item with cooked ingredients
	public ItemStack cook(int state) {

		ItemStack potion = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		// cookedTime is always time in minutes, state may differ with number of ticks
		cookedTime = state;
		String cookedName = null;
		BRecipe cookRecipe = getCookRecipe();

		int uid = Brew.generateUID();

		if (cookRecipe != null) {
			// Potion is best with cooking only
			int quality = (int) Math.round((getIngredientQuality(cookRecipe) + getCookingQuality(cookRecipe, false)) / 2.0);
			P.p.log("cooked potion has Quality: " + quality);
			Brew brew = new Brew(uid, quality, cookRecipe, clone());
			Brew.addOrReplaceEffects(potionMeta, brew.getEffects());

			cookedName = cookRecipe.getName(quality);
			potion.setDurability(Brew.PotionColor.valueOf(cookRecipe.getColor()).getColorId(false));

		} else {
			// new base potion
			new Brew(uid, clone());

			if (state <= 1) {
				cookedName = "Schlammiger Sud";
				potion.setDurability(Brew.PotionColor.BLUE.getColorId(false));
			} else {
				for (Material ingredient : ingredients.keySet()) {
					if (cookedNames.containsKey(ingredient)) {
						// if more than half of the ingredients is of one kind
						if (ingredients.get(ingredient) > (getIngredientsCount() / 2)) {
							cookedName = cookedNames.get(ingredient);
							potion.setDurability(Brew.PotionColor.CYAN.getColorId(true));
						}
					}
				}
			}
		}
		if (cookedName == null) {
			// if no name could be found
			cookedName = "Undefinierbarer Sud";
			potion.setDurability(Brew.PotionColor.CYAN.getColorId(true));
		}

		potionMeta.setDisplayName(P.p.color("&f" + cookedName));
		// This effect stores the UID in its Duration
		potionMeta.addCustomEffect((PotionEffectType.REGENERATION).createEffect((uid * 4), 0), true);
		potion.setItemMeta(potionMeta);

		return potion;
	}

	// returns amount of ingredients
	private int getIngredientsCount() {
		int count = 0;
		for (int value : ingredients.values()) {
			count += value;
		}
		return count;
	}

	public int getCookedTime() {
		return cookedTime;
	}

	// best recipe for current state of potion, STILL not always returns the
	// correct one...
	public BRecipe getBestRecipe(byte wood, float time, boolean distilled) {
		float quality = 0;
		int ingredientQuality = 0;
		int cookingQuality = 0;
		int woodQuality = 0;
		int ageQuality = 0;
		BRecipe bestRecipe = null;
		for (BRecipe recipe : recipes) {
			ingredientQuality = getIngredientQuality(recipe);
			cookingQuality = getCookingQuality(recipe, distilled);

			if (ingredientQuality > -1) {
				if (recipe.needsToAge()) {
					// needs riping in barrel
					ageQuality = getAgeQuality(recipe, time);
					woodQuality = getWoodQuality(recipe, wood);
					P.p.log("Ingredient Quality: " + ingredientQuality + " Cooking Quality: " + cookingQuality + " Wood Quality: " + getWoodQuality(recipe, wood) + 
						" age Quality: " + getAgeQuality(recipe, time) + " for " + recipe.getName(5));

					// is this recipe better than the previous best?
					if ((((float) ingredientQuality + cookingQuality + woodQuality + ageQuality) / 4) > quality) {
						quality = ((float) ingredientQuality + cookingQuality + woodQuality + ageQuality) / 4;
						bestRecipe = recipe;
					}
				} else {
					P.p.log("Ingredient Quality: " + ingredientQuality + " Cooking Quality: " + cookingQuality + " for " + recipe.getName(5));
					// calculate quality without age and barrel
					if ((((float) ingredientQuality + cookingQuality) / 2) > quality) {
						quality = ((float) ingredientQuality + cookingQuality) / 2;
						bestRecipe = recipe;
					}
				}
			}
		}
		if (bestRecipe != null) {
			P.p.log("best recipe: " + bestRecipe.getName(5) + " has Quality= " + quality);
		}
		return bestRecipe;
	}

	// returns recipe that is cooking only and matches the ingredients and
	// cooking time
	public BRecipe getCookRecipe() {
		BRecipe bestRecipe = getBestRecipe((byte) 0, 0, false);

		// Check if best recipe is cooking only
		if (bestRecipe != null) {
			if (bestRecipe.isCookingOnly()) {
				return bestRecipe;
			}
		}
		return null;
	}

	// returns the currently best matching recipe for distilling for the
	// ingredients and cooking time
	public BRecipe getdistillRecipe() {
		BRecipe bestRecipe = getBestRecipe((byte) 0, 0, true);

		// Check if best recipe needs to be destilled
		if (bestRecipe != null) {
			if (bestRecipe.needsDistilling()) {
				return bestRecipe;
			}
		}
		return null;
	}

	// returns currently best matching recipe for ingredients, cooking- and
	// ageingtime
	public BRecipe getAgeRecipe(byte wood, float time, boolean distilled) {
		BRecipe bestRecipe = getBestRecipe(wood, time, distilled);

		if (bestRecipe != null) {
			if (bestRecipe.needsToAge()) {
				return bestRecipe;
			}
		}
		return null;
	}

	// returns the quality of the ingredients conditioning given recipe, -1 if
	// no recipe is near them
	public int getIngredientQuality(BRecipe recipe) {
		float quality = 10;
		int count = 0;
		int badStuff = 0;
		if (recipe.isMissingIngredients(ingredients)) {
			// when ingredients are not complete
			return -1;
		}
		for (Material ingredient : ingredients.keySet()) {
			count = ingredients.get(ingredient);
			if (recipe.amountOf(ingredient) == 0) {
				// this ingredient doesnt belong into the recipe
				if (count > (getIngredientsCount() / 2)) {
					// when more than half of the ingredients dont fit into the
					// recipe
					return -1;
				}
				badStuff++;
				if (badStuff < ingredients.size()) {
					// when there are other ingredients
					quality -= count * 2;
					continue;
				} else {
					// ingredients dont fit at all
					return -1;
				}
			}
			// calculate the quality
			quality -= (((float) Math.abs(count - recipe.amountOf(ingredient)) / recipe.allowedCountDiff(recipe.amountOf(ingredient))) * 10.0);
		}
		if (quality >= 0) {
			return Math.round(quality);
		}
		return -1;
	}

	// returns the quality regarding the cooking-time conditioning given Recipe
	public int getCookingQuality(BRecipe recipe, boolean distilled) {
		if (!recipe.needsDistilling() == distilled) {
			return 0;
		}
		int quality = 10 - (int) Math.round(((float) Math.abs(cookedTime - recipe.getCookingTime()) / recipe.allowedTimeDiff(recipe.getCookingTime())) * 10.0);

		if (quality > 0) {
			return quality;
		}
		return 0;
	}

	// returns pseudo quality of distilling. 0 if doesnt match the need of the recipes distilling
	public int getDistillQuality(BRecipe recipe, int distillRuns) {
		if (recipe.needsDistilling() != distillRuns > 0) {
			return 0;
		}
		return 10 - Math.abs(recipe.getDistillRuns() - distillRuns);
	}

	// returns the quality regarding the barrel wood conditioning given Recipe
	public int getWoodQuality(BRecipe recipe, byte wood) {
		if (recipe.getWood() == 0x8) {
			// type of wood doesnt matter
			return 10;
		}
		int quality = 10 - (int) Math.round(recipe.getWoodDiff(wood) * recipe.getDifficulty());

		if (quality > 0) {
			return quality;
		}
		return 0;
	}

	// returns the quality regarding the ageing time conditioning given Recipe
	public int getAgeQuality(BRecipe recipe, float time) {
		int quality = 10 - (int) Math.round(Math.abs(time - recipe.getAge()) * ((float) recipe.getDifficulty() / 2));

		if (quality > 0) {
			return quality;
		}
		return 0;
	}

	// Creates a copy ingredients
	public BIngredients clone() {
		BIngredients copy = new BIngredients();
		copy.ingredients.putAll(ingredients);
		copy.cookedTime = cookedTime;
		return copy;
	}

	// saves data into ingredient section of Brew/BCauldron
	public void save(ConfigurationSection config) {
		if (cookedTime != 0) {
			config.set("cookedTime", cookedTime);
		}

		// convert the ingredient Material to id
		Map<Integer, Integer> mats = new HashMap<Integer, Integer>();
		for (Material mat : ingredients.keySet()) {
			mats.put(mat.getId(), ingredients.get(mat));
		}
		// list all Material ids with their amount
		ConfigurationSection matSection = config.createSection("mats");
		for (int id : mats.keySet()) {
			matSection.set("" + id, mats.get(id));
		}
	}

}