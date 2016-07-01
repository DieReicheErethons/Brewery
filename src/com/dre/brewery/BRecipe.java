package com.dre.brewery;

import com.dre.brewery.lore.BrewLore;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BRecipe {

	private String[] name;
	private ArrayList<ItemStack> ingredients = new ArrayList<>(); // material and amount
	private int cookingTime; // time to cook in cauldron
	private byte distillruns; // runs through the brewer
	private int distillTime; // time for one distill run in seconds
	private byte wood; // type of wood the barrel has to consist of
	private int age; // time in minecraft days for the potions to age in barrels
	private String color; // color of the destilled/finished potion
	private int difficulty; // difficulty to brew the potion, how exact the instruction has to be followed
	private int alcohol; // Alcohol in perfect potion
	private ArrayList<BEffect> effects = new ArrayList<>(); // Special Effects when drinking

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
					String[] matParts;
					if (ingredParts[0].contains(",")) {
						matParts = ingredParts[0].split(",");
					} else if (ingredParts[0].contains(":")) {
						matParts = ingredParts[0].split(":");
					} else if (ingredParts[0].contains(";")) {
						matParts = ingredParts[0].split(";");
					} else {
						matParts = ingredParts[0].split("\\.");
					}
					Material mat = Material.matchMaterial(matParts[0]);
					short durability = -1;
					if (matParts.length == 2) {
						durability = (short) P.p.parseInt(matParts[1]);
					}
					if (mat == null && P.p.hasVault) {
						try {
							net.milkbowl.vault.item.ItemInfo vaultItem = net.milkbowl.vault.item.Items.itemByString(matParts[0]);
							if (vaultItem != null) {
								mat = vaultItem.getType();
								if (durability == -1 && vaultItem.getSubTypeId() != 0) {
									durability = vaultItem.getSubTypeId();
								}
								if (mat == Material.LEAVES) {
									if (durability > 3) {
										durability -= 4; // Vault has leaves with higher durability
									}
								}
							}
						} catch (Exception e) {
							P.p.errorLog("Could not check vault for Item Name");
							e.printStackTrace();
						}
					}
					if (mat != null) {
						ItemStack stack = new ItemStack(mat, P.p.parseInt(ingredParts[1]), durability);
						this.ingredients.add(stack);
						BIngredients.possibleIngredients.add(mat);
					} else {
						P.p.errorLog("Unknown Material: " + ingredParts[0]);
						this.ingredients = null;
						return;
					}
				} else {
					return;
				}
			}
		}
		this.cookingTime = configSectionRecipes.getInt(recipeId + ".cookingtime", 1);
		int dis = configSectionRecipes.getInt(recipeId + ".distillruns", 0);
		if (dis > Byte.MAX_VALUE) {
			this.distillruns = Byte.MAX_VALUE;
		} else {
			this.distillruns = (byte) dis;
		}
		this.distillTime = configSectionRecipes.getInt(recipeId + ".distilltime", 0) * 20;
		this.wood = (byte) configSectionRecipes.getInt(recipeId + ".wood", 0);
		this.age = configSectionRecipes.getInt(recipeId + ".age", 0);
		this.color = configSectionRecipes.getString(recipeId + ".color");
		this.difficulty = configSectionRecipes.getInt(recipeId + ".difficulty", 0);
		this.alcohol = configSectionRecipes.getInt(recipeId + ".alcohol", 0);

		List<String> effectStringList = configSectionRecipes.getStringList(recipeId + ".effects");
		if (effectStringList != null) {
			for (String effectString : effectStringList) {
				BEffect effect = new BEffect(effectString);
				if (effect.isValid()) {
					effects.add(effect);
				} else {
					P.p.errorLog("Error adding Effect to Recipe: " + getName(5));
				}
			}
		}
	}

	// check every part of the recipe for validity
	public boolean isValid() {
		if (name == null || name.length < 1) {
			P.p.errorLog("Recipe Name missing or invalid!");
			return false;
		}
		if (getName(5) == null || getName(5).length() < 1) {
			P.p.errorLog("Recipe Name invalid");
			return false;
		}
		if (ingredients == null || ingredients.isEmpty()) {
			P.p.errorLog("No ingredients could be loaded for Recipe: " + getName(5));
			return false;
		}
		if (cookingTime < 1) {
			P.p.errorLog("Invalid cooking time '" + cookingTime + "' in Recipe: " + getName(5));
			return false;
		}
		if (distillruns < 0) {
			P.p.errorLog("Invalid distillruns '" + distillruns + "' in Recipe: " + getName(5));
			return false;
		}
		if (distillTime < 0) {
			P.p.errorLog("Invalid distilltime '" + distillTime + "' in Recipe: " + getName(5));
			return false;
		}
		if (wood < 0 || wood > 6) {
			P.p.errorLog("Invalid wood type '" + wood + "' in Recipe: " + getName(5));
			return false;
		}
		if (age < 0) {
			P.p.errorLog("Invalid age time '" + age + "' in Recipe: " + getName(5));
			return false;
		}
		try {
			Brew.PotionColor.valueOf(getColor());
		} catch (IllegalArgumentException e) {
			P.p.errorLog("Invalid Color '" + color + "' in Recipe: " + getName(5));
			return false;
		}
		if (difficulty < 0 || difficulty > 10) {
			P.p.errorLog("Invalid difficulty '" + difficulty + "' in Recipe: " + getName(5));
			return false;
		}
		if (alcohol < 0) {
			P.p.errorLog("Invalid alcohol '" + alcohol + "' in Recipe: " + getName(5));
			return false;
		}
		return true;
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
		return age == 0 && distillruns == 0;
	}

	public boolean needsDistilling() {
		return distillruns != 0;
	}

	public boolean needsToAge() {
		return age != 0;
	}

	// true if given list misses an ingredient
	public boolean isMissingIngredients(List<ItemStack> list) {
		if (list.size() < ingredients.size()) {
			return true;
		}
		for (ItemStack ingredient : ingredients) {
			boolean matches = false;
			for (ItemStack used : list) {
				if (ingredientsMatch(used, ingredient)) {
					matches = true;
					break;
				}
			}
			if (!matches) {
				return true;
			}
		}
		return false;
	}

	// Returns true if this ingredient cares about durability
	public boolean hasExactData(ItemStack item) {
		for (ItemStack ingredient : ingredients) {
			if (ingredient.getType().equals(item.getType())) {
				return ingredient.getDurability() != -1;
			}
		}
		return true;
	}

	// Returns true if this item matches the item from a recipe
	public static boolean ingredientsMatch(ItemStack usedItem, ItemStack recipeItem) {
		if (!recipeItem.getType().equals(usedItem.getType())) {
			return false;
		}
		return recipeItem.getDurability() == -1 || recipeItem.getDurability() == usedItem.getDurability();
	}

	// Create a Potion from this Recipe with best values. Quality can be set, but will reset to 10 if put in a barrel
	public ItemStack create(int quality) {
		ItemStack potion = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		Brew brew = createBrew(quality);

		Brew.PotionColor.valueOf(getColor()).colorBrew(potionMeta, potion, false);
		potionMeta.setDisplayName(P.p.color("&f" + getName(quality)));
		// This effect stores the UID in its Duration
		//potionMeta.addCustomEffect((PotionEffectType.REGENERATION).createEffect((uid * 4), 0), true);

		BrewLore lore = new BrewLore(brew, potionMeta);
		lore.convertLore(false);
		lore.addOrReplaceEffects(effects, quality);
		lore.write();
		brew.touch();
		brew.save(potionMeta);

		potion.setItemMeta(potionMeta);
		return potion;
	}

	public Brew createBrew(int quality) {
		ArrayList<ItemStack> list = new ArrayList<>(ingredients.size());
		for (ItemStack item : ingredients) {
			if (item.getDurability() == -1) {
				list.add(new ItemStack(item.getType(), item.getAmount()));
			} else {
				list.add(item.clone());
			}
		}

		BIngredients bIngredients = new BIngredients(list, cookingTime);

		return new Brew(bIngredients, quality, distillruns, getAge(), wood, getName(5), false, true);
	}


	// Getter

	// how many of a specific ingredient in the recipe
	public int amountOf(ItemStack item) {
		for (ItemStack ingredient : ingredients) {
			if (ingredientsMatch(item, ingredient)) {
				return ingredient.getAmount();
			}
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

	// If one of the quality names equalIgnoreCase given name
	public boolean hasName(String name) {
		for (String test : this.name) {
			if (test.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	public int getCookingTime() {
		return cookingTime;
	}

	public byte getDistillRuns() {
		return distillruns;
	}

	public int getDistillTime() {
		return distillTime;
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

	public ArrayList<BEffect> getEffects() {
		return effects;
	}

	@Override
	public String toString() {
		return "BRecipe{" + getName(5) + '}';
	}

	public static BRecipe get(String name) {
		for (BRecipe recipe : BIngredients.recipes) {
			if (recipe.getName(5).equalsIgnoreCase(name)) {
				return recipe;
			}
		}
		return null;
	}
}
