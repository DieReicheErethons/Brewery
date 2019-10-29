package com.dre.brewery;

import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.utility.CustomItem;
import com.dre.brewery.utility.PotionColor;
import com.dre.brewery.utility.Tuple;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BRecipe {

	public static List<BRecipe> recipes = new ArrayList<>();

	private String[] name;
	private List<Tuple<CustomItem, Integer>> ingredients = new ArrayList<>(); // Items and amounts
	private int cookingTime; // time to cook in cauldron
	private byte distillruns; // runs through the brewer
	private int distillTime; // time for one distill run in seconds
	private byte wood; // type of wood the barrel has to consist of
	private int age; // time in minecraft days for the potions to age in barrels
	private PotionColor color; // color of the destilled/finished potion
	private int difficulty; // difficulty to brew the potion, how exact the instruction has to be followed
	private int alcohol; // Alcohol in perfect potion
	private List<Tuple<Integer, String>> lore; // Custom Lore on the Potion. The int is for Quality Lore, 0 = any, 1,2,3 = Bad,Middle,Good
	private ArrayList<BEffect> effects = new ArrayList<>(); // Special Effects when drinking

	public BRecipe() {
	}

	@Nullable
	public static BRecipe fromConfig(ConfigurationSection configSectionRecipes, String recipeId) {
		BRecipe recipe = new BRecipe();
		String nameList = configSectionRecipes.getString(recipeId + ".name");
		if (nameList != null) {
			String[] name = nameList.split("/");
			if (name.length > 2) {
				recipe.name = name;
			} else {
				recipe.name = new String[1];
				recipe.name[0] = name[0];
			}
		} else {
			P.p.errorLog(recipeId + ": Recipe Name missing or invalid!");
			return null;
		}
		if (recipe.getRecipeName() == null || recipe.getRecipeName().length() < 1) {
			P.p.errorLog(recipeId + ": Recipe Name invalid");
			return null;
		}

		recipe.ingredients = loadIngredients(configSectionRecipes, recipeId);
		if (recipe.ingredients == null || recipe.ingredients.isEmpty()) {
			P.p.errorLog("No ingredients for: " + recipe.getRecipeName());
			return null;
		}
		recipe.cookingTime = configSectionRecipes.getInt(recipeId + ".cookingtime", 1);
		int dis = configSectionRecipes.getInt(recipeId + ".distillruns", 0);
		if (dis > Byte.MAX_VALUE) {
			recipe.distillruns = Byte.MAX_VALUE;
		} else {
			recipe.distillruns = (byte) dis;
		}
		recipe.distillTime = configSectionRecipes.getInt(recipeId + ".distilltime", 0) * 20;
		recipe.wood = (byte) configSectionRecipes.getInt(recipeId + ".wood", 0);
		recipe.age = configSectionRecipes.getInt(recipeId + ".age", 0);
		recipe.difficulty = configSectionRecipes.getInt(recipeId + ".difficulty", 0);
		recipe.alcohol = configSectionRecipes.getInt(recipeId + ".alcohol", 0);

		String col = configSectionRecipes.getString(recipeId + ".color", "BLUE");
		recipe.color = PotionColor.fromString(col);
		if (recipe.color == PotionColor.WATER && !col.equals("WATER")) {
			P.p.errorLog("Invalid Color '" + col + "' in Recipe: " + recipe.getRecipeName());
			return null;
		}

		recipe.lore = loadLore(configSectionRecipes, recipeId + ".lore");

		List<String> effectStringList = configSectionRecipes.getStringList(recipeId + ".effects");
		if (effectStringList != null) {
			for (String effectString : effectStringList) {
				BEffect effect = new BEffect(effectString);
				if (effect.isValid()) {
					recipe.effects.add(effect);
				} else {
					P.p.errorLog("Error adding Effect to Recipe: " + recipe.getRecipeName());
				}
			}
		}
		return recipe;
	}

	public static List<Tuple<CustomItem, Integer>> loadIngredients(ConfigurationSection cfg, String recipeId) {
		List<String> ingredientsList;
		if (cfg.isString(recipeId + ".ingredients")) {
			ingredientsList = new ArrayList<>(1);
			ingredientsList.add(cfg.getString(recipeId + ".ingredients", "x"));
		} else {
			ingredientsList = cfg.getStringList(recipeId + ".ingredients");
		}
		if (ingredientsList == null) {
			return null;
		}
		List<Tuple<CustomItem, Integer>> ingredients = new ArrayList<>(ingredientsList.size());
		listLoop: for (String item : ingredientsList) {
			String[] ingredParts = item.split("/");
			int amount = 1;
			if (ingredParts.length == 2) {
				amount = P.p.parseInt(ingredParts[1]);
				if (amount < 1) {
					P.p.errorLog(recipeId + ": Invalid Item Amount: " + ingredParts[1]);
					return null;
				}
			}
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

			// Try to find this Ingredient as Custom Item
			for (CustomItem custom : BConfig.customItems) {
				if (custom.getId().equalsIgnoreCase(matParts[0])) {
					ingredients.add(new Tuple<>(custom, amount));
					BCauldronRecipe.acceptedMaterials.addAll(custom.getMaterials());
					continue listLoop;
				}
			}

			Material mat = Material.matchMaterial(matParts[0]);
			short durability = -1;
			if (matParts.length == 2) {
				durability = (short) P.p.parseInt(matParts[1]);
			}
			if (mat == null && BConfig.hasVault) {
				try {
					net.milkbowl.vault.item.ItemInfo vaultItem = net.milkbowl.vault.item.Items.itemByString(matParts[0]);
					if (vaultItem != null) {
						mat = vaultItem.getType();
						if (durability == -1 && vaultItem.getSubTypeId() != 0) {
							durability = vaultItem.getSubTypeId();
						}
						if (mat.name().contains("LEAVES")) {
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
				CustomItem custom;
				if (durability > -1) {
					custom = CustomItem.asSimpleItem(mat, durability);
				} else {
					custom = CustomItem.asSimpleItem(mat);
				}
				ingredients.add(new Tuple<>(custom, amount));
				BCauldronRecipe.acceptedMaterials.add(mat);
			} else {
				P.p.errorLog(recipeId + ": Unknown Material: " + ingredParts[0]);
				return null;
			}
		}
		return ingredients;
	}

	@Nullable
	public static List<Tuple<Integer, String>> loadLore(ConfigurationSection cfg, String path) {
		List<String> load = null;
		if (cfg.isString(path)) {
			load = new ArrayList<>(1);
			load.add(cfg.getString(path));
		} else if (cfg.isList(path)) {
			load = cfg.getStringList(path);
		}
		if (load != null) {
			List<Tuple<Integer, String>> lore = new ArrayList<>(load.size());
			for (String line : load) {
				line = P.p.color(line);
				int plus = 0;
				if (line.startsWith("+++")) {
					plus = 3;
					line = line.substring(3);
				} else if (line.startsWith("++")) {
					plus = 2;
					line = line.substring(2);
				} else if (line.startsWith("+")) {
					plus = 1;
					line = line.substring(1);
				}
				if (line.startsWith(" ")) {
					line = line.substring(1);
				}
				if (!line.startsWith("§")) {
					line = "§9" + line;
				}
				lore.add(new Tuple<>(plus, line));
			}
			return lore;
		}
		return null;
	}

	// check every part of the recipe for validity
	public boolean isValid() {
		if (ingredients == null || ingredients.isEmpty()) {
			P.p.errorLog("No ingredients could be loaded for Recipe: " + getRecipeName());
			return false;
		}
		if (cookingTime < 1) {
			P.p.errorLog("Invalid cooking time '" + cookingTime + "' in Recipe: " + getRecipeName());
			return false;
		}
		if (distillruns < 0) {
			P.p.errorLog("Invalid distillruns '" + distillruns + "' in Recipe: " + getRecipeName());
			return false;
		}
		if (distillTime < 0) {
			P.p.errorLog("Invalid distilltime '" + distillTime + "' in Recipe: " + getRecipeName());
			return false;
		}
		if (wood < 0 || wood > 6) {
			P.p.errorLog("Invalid wood type '" + wood + "' in Recipe: " + getRecipeName());
			return false;
		}
		if (age < 0) {
			P.p.errorLog("Invalid age time '" + age + "' in Recipe: " + getRecipeName());
			return false;
		}
		if (difficulty < 0 || difficulty > 10) {
			P.p.errorLog("Invalid difficulty '" + difficulty + "' in Recipe: " + getRecipeName());
			return false;
		}
		if (alcohol < 0) {
			P.p.errorLog("Invalid alcohol '" + alcohol + "' in Recipe: " + getRecipeName());
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
		for (Tuple<CustomItem, Integer> ingredient : ingredients) {
			boolean matches = false;
			for (ItemStack used : list) {
				if (ingredient.a().matches(used)) {
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

	/**
	 * Create a Potion from this Recipe with best values. Quality can be set, but will reset to 10 if put in a barrel
	 *
	 * @param quality The Quality of the Brew
	 * @return The Created Item
	 */
	public ItemStack create(int quality) {
		return createBrew(quality).createItem(this);
	}

	/**
	 * Create a Brew from this Recipe with best values. Quality can be set, but will reset to 10 if put in a barrel
	 *
	 * @param quality The Quality of the Brew
	 * @return The created Brew
	 */
	public Brew createBrew(int quality) {
		List<ItemStack> list = ingredients.stream().map(ing -> ing.a().createDummy(ing.b())).collect(Collectors.toList());

		BIngredients bIngredients = new BIngredients(list, cookingTime);

		return new Brew(bIngredients, quality, distillruns, getAge(), wood, getRecipeName(), false, true, 0);
	}


	// Getter

	// how many of a specific ingredient in the recipe
	public int amountOf(ItemStack item) {
		for (Tuple<CustomItem, Integer> ingredient : ingredients) {
			if (ingredient.a().matches(item)) {
				return ingredient.b();
			}
		}
		return 0;
	}

	// Same as getName(5)
	public String getRecipeName() {
		return getName(5);
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

	@NotNull
	public PotionColor getColor() {
		return color;
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

	public boolean hasLore() {
		return lore != null && !lore.isEmpty();
	}

	@Nullable
	public List<Tuple<Integer, String>> getLore() {
		return lore;
	}

	@Nullable
	public List<String> getLoreForQuality(int quality) {
		if (lore == null) return null;
		int plus;
		if (quality <= 3) {
			plus = 1;
		} else if (quality <= 7) {
			plus = 2;
		} else {
			plus = 3;
		}
		List<String> list = new ArrayList<>(lore.size());
		for (Tuple<Integer, String> line : lore) {
			if (line.first() == 0 || line.first() == plus) {
				list.add(line.second());
			}
		}
		return list;
	}

	public ArrayList<BEffect> getEffects() {
		return effects;
	}

	@Override
	public String toString() {
		return "BRecipe{" + getRecipeName() + '}';
	}

	public static BRecipe get(String name) {
		for (BRecipe recipe : recipes) {
			if (recipe.getRecipeName().equalsIgnoreCase(name)) {
				return recipe;
			}
		}
		return null;
	}
}
