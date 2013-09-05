package com.dre.brewery;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Material;

public class BRecipe {

	private String[] name;
	private Map<Material, Integer> ingredients = new HashMap<Material, Integer>();// material and amount
	private int cookingTime;// time to cook in cauldron
	private int distillruns;// runs through the brewer
	private byte wood;// type of wood the barrel has to consist of
	private int age;// time in minecraft days for the potions to age in barrels
	private String color;// color of the destilled/finished potion
	private int difficulty;// difficulty to brew the potion, how exact the instruction has to be followed
	private int alcohol;// Alcohol in perfect potion
	private Map<String, Integer> effects = new HashMap<String, Integer>(); // Special Effect, Duration

	public BRecipe(ConfigurationSection configSectionRecipes, String recipeId) {
		String nameList = configSectionRecipes.getString(recipeId + ".name");
		if (nameList != null) {
			String[] name = nameList.split("/");
			if (name.length > 2) {
				this.name = name;
			} else {
				this.name = new String[1];
				this.name[0] = name[0];
			}
		} else {
			return;
		}
		List<String> ingredientsList = configSectionRecipes.getStringList(recipeId + ".ingredients");
		if (ingredientsList != null) {
			for (String item : ingredientsList) {
				String[] ingredParts = item.split("/");
				if (ingredParts.length == 2) {
					Material mat = Material.matchMaterial(ingredParts[0]);
					if (mat != null) {
						this.ingredients.put(Material.matchMaterial(ingredParts[0]), P.p.parseInt(ingredParts[1]));
					} else {
						P.p.errorLog("Unbekanntes Material: " + ingredParts[0]);
						this.ingredients = null;
						return;
					}
				} else {
					return;
				}
			}
		}
		this.cookingTime = configSectionRecipes.getInt(recipeId + ".cookingtime");
		this.distillruns = configSectionRecipes.getInt(recipeId + ".distillruns");
		this.wood = (byte) configSectionRecipes.getInt(recipeId + ".wood");
		this.age = configSectionRecipes.getInt(recipeId + ".age");
		this.color = configSectionRecipes.getString(recipeId + ".color");
		this.difficulty = configSectionRecipes.getInt(recipeId + ".difficulty");
		this.alcohol = configSectionRecipes.getInt(recipeId + ".alcohol");

		List<String> effectStringList = configSectionRecipes.getStringList(recipeId + ".effects");
		if (effectStringList != null) {
			for (String effectString : effectStringList) {
				String[] effectSplit = effectString.split("/");
				String effect = effectSplit[0];
				if (effect.equalsIgnoreCase("WEAKNESS") ||
					effect.equalsIgnoreCase("INCREASE_DAMAGE") ||
					effect.equalsIgnoreCase("SLOW") ||
					effect.equalsIgnoreCase("SPEED") ||
					effect.equalsIgnoreCase("REGENERATION")) {
					// hide these effects as they put crap into lore
					effect = effect + "X";
				}
				if (effectSplit.length > 1) {
					effects.put(effect, P.p.parseInt(effectSplit[1]));
				} else {
					effects.put(effect, 20);
				}
			}
		}
	}

	// allowed deviation to the recipes count of ingredients at the given difficulty
	public int allowedCountDiff(int count) {
		if (count < 8) {
			count = 8;
		}
		int allowedCountDiff = Math.round((float) ((11.0 - difficulty) * (count / 10.0)));

		if (allowedCountDiff == 0) {
			return 1;
		}
		return allowedCountDiff;
	}

	// allowed deviation to the recipes cooking-time at the given difficulty
	public int allowedTimeDiff(int time) {
		if (time < 8) {
			time = 8;
		}
		int allowedTimeDiff = Math.round((float) ((11.0 - difficulty) * (time / 10.0)));

		if (allowedTimeDiff == 0) {
			return 1;
		}
		return allowedTimeDiff;
	}

	// difference between given and recipe-wanted woodtype
	public float getWoodDiff(float wood) {
		return Math.abs(wood - this.wood);
	}

	public boolean isCookingOnly() {
		if (age == 0 && distillruns == 0) {
			return true;
		}
		return false;
	}

	public boolean needsDistilling() {
		if (distillruns == 0) {
			return false;
		}
		return true;
	}

	public boolean needsToAge() {
		if (age == 0) {
			return false;
		}
		return true;
	}

	// true if given map misses an ingredient
	public boolean isMissingIngredients(Map<Material, Integer> map) {
		if (map.size() < ingredients.size()) {
			return true;
		}
		for (Material ingredient : ingredients.keySet()) {
			if (!map.containsKey(ingredient)) {
				return true;
			}
		}
		return false;
	}

	// true if name and ingredients are correct
	public boolean isValid() {
		return (name != null && ingredients != null && !ingredients.isEmpty());
	}


	// Getter

	// how many of a specific ingredient in the recipe
	public int amountOf(Material material) {
		if (ingredients.containsKey(material)) {
			return ingredients.get(material);
		}
		return 0;
	}

	// name that fits the quality
	public String getName(int quality) {
		if (name.length > 2) {
			if (quality <= 3) {
				return name[0];
			} else if (quality <= 7) {
				return name[1];
			} else {
				return name[2];
			}
		} else {
			return name[0];
		}
	}

	public int getCookingTime() {
		return cookingTime;
	}

	public int getDistillRuns() {
		return distillruns;
	}

	public String getColor() {
		if (color != null) {
			return color.toUpperCase();
		}
		return "BLUE";
	}

	// get the woodtype
	public byte getWood() {
		return wood;
	}

	public float getAge() {
		return (float) age;
	}

	public int getDifficulty() {
		return difficulty;
	}

	public int getAlcohol() {
		return alcohol;
	}

	public Map<String, Integer> getEffects() {
		return effects;
	}

}