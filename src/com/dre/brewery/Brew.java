package com.dre.brewery;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.BrewerInventory;

import com.dre.brewery.BIngredients;

public class Brew {

	// represents the liquid in the brewed Potions

	public static Map<Integer, Brew> potions = new HashMap<Integer, Brew>();
	public static Boolean colorInBarrels; // color the Lore while in Barrels
	public static Boolean colorInBrewer; // color the Lore while in Brewer

	//public static Map<ItemStack, List<String>> oldLore = new HashMap<ItemStack, List<String>>();

	private BIngredients ingredients;
	private int quality;
	private int distillRuns;
	private float ageTime;
	private float wood;
	private BRecipe currentRecipe;
	private boolean unlabeled;

	public Brew(int uid, BIngredients ingredients) {
		this.ingredients = ingredients;
		potions.put(uid, this);
	}

	// quality already set
	public Brew(int uid, int quality, BRecipe recipe, BIngredients ingredients) {
		this.ingredients = ingredients;
		this.quality = quality;
		this.currentRecipe = recipe;
		potions.put(uid, this);
	}

	// loading from file
	public Brew(int uid, BIngredients ingredients, int quality, int distillRuns, float ageTime, float wood, String recipe, Boolean unlabeled) {
		potions.put(uid, this);
		this.ingredients = ingredients;
		this.quality = quality;
		this.distillRuns = distillRuns;
		this.ageTime = ageTime;
		this.wood = wood;
		this.unlabeled = unlabeled;
		if (recipe != null && !recipe.equals("")) {
			currentRecipe = BIngredients.getRecipeByName(recipe);
			if (currentRecipe == null) {
				if (quality > 0) {
					currentRecipe = ingredients.getBestRecipe(wood, ageTime, distillRuns > 0);
					if (currentRecipe != null) {
						this.quality = calcQuality();
						P.p.log("Brew was made from Recipe: '" + recipe + "' which could not be found. '" + currentRecipe.getName(5) + "' used instead!");
					} else {
						P.p.errorLog("Brew was made from Recipe: '" + recipe + "' which could not be found!");
					}
				}
			}
		}
	}

	// returns a Brew by its UID
	public static Brew get(int uid) {
		if (uid < -1) {
			if (!potions.containsKey(uid)) {
				P.p.errorLog("Database failure! unable to find UID " + uid + " of a custom Potion!");
				return null;// throw some exception?
			}
		} else {
			return null;
		}
		return potions.get(uid);
	}

	// returns a Brew by PotionMeta
	public static Brew get(PotionMeta meta) {
		return get(getUID(meta));
	}

	// returns a Brew by ItemStack
	public static Brew get(ItemStack item) {
		if (item.getTypeId() == 373) {
			if (item.hasItemMeta()) {
				PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
				return get(potionMeta);
			}
		}
		return null;
	}


	// returns UID of custom Potion item
	public static int getUID(ItemStack item) {
		return getUID((PotionMeta) item.getItemMeta());
	}

	// returns UID of custom Potion meta
	public static int getUID(PotionMeta potionMeta) {
		if (potionMeta.hasCustomEffect(PotionEffectType.REGENERATION)) {
			for (PotionEffect effect : potionMeta.getCustomEffects()) {
				if (effect.getType().equals(PotionEffectType.REGENERATION)) {
					if (effect.getDuration() < -1) {
						return effect.getDuration();
					}
				}
			}
		}
		return 0;
	}

	// remove potion from file (drinking, despawning, combusting, cmdDeleting, should be more!)
	public static void remove(ItemStack item) {
		potions.remove(getUID(item));
	}

	// generate an UID
	public static int generateUID() {
		int uid = -2;
		while (potions.containsKey(uid)) {
			uid -= 1;
		}
		return uid;
	}


	// Copy a Brew with a new unique ID and return its item
	public ItemStack copy(ItemStack item) {
		ItemStack copy = item.clone();
		int uid = generateUID();
		clone(uid);
		PotionMeta meta = (PotionMeta) copy.getItemMeta();
		meta.addCustomEffect((PotionEffectType.REGENERATION).createEffect((uid * 4), 0), true);
		copy.setItemMeta(meta);
		return copy;
	}

	// Clones this instance with a new unique ID
	public Brew clone(int uid) {
		Brew brew = new Brew(uid, quality, currentRecipe, ingredients);
		brew.distillRuns = distillRuns;
		brew.ageTime = ageTime;
		brew.unlabeled = unlabeled;
		return brew;
	}

	// calculate alcohol from recipe
	public int calcAlcohol() {
		if (quality == 0) {
			// Give bad potions some alc
			int badAlc = 0;
			if (distillRuns > 1) {
				badAlc = distillRuns;
			}
			if (ageTime > 10) {
				badAlc += 5;
			} else if (ageTime > 2) {
				badAlc += 3;
			}
			if (currentRecipe != null) {
				return badAlc;
			} else {
				return badAlc / 2;
			}
		}

		if (currentRecipe != null) {
			int alc = currentRecipe.getAlcohol();
			if (currentRecipe.needsDistilling()) {
				if (distillRuns == 0) {
					return 0;
				}
				// bad quality can decrease alc by up to 40%
				alc *= 1 - ((float) (10 - quality) * 0.04);
				// distillable Potions should have half alc after one and full alc after all needed distills
				alc /= 2;
				alc *= 1.0F + ((float) distillRuns / currentRecipe.getDistillRuns()) ;
			} else {
				// quality decides 10% - 100%
				alc *= ((float) quality / 10.0);
			}
			if (alc > 0) {
				return alc;
			}
		}
		return 0;
	}

	// calculating quality
	public int calcQuality() {
		// calculate quality from all of the factors
		float quality = ingredients.getIngredientQuality(currentRecipe) + ingredients.getCookingQuality(currentRecipe, distillRuns > 0);
		if (currentRecipe.needsToAge() || ageTime > 0.5) {
			quality += ingredients.getWoodQuality(currentRecipe, wood) + ingredients.getAgeQuality(currentRecipe, ageTime);
			quality /= 4;
		} else {
			quality /= 2;
		}
		return (int) Math.round(quality);
	}

	public int getQuality() {
		return quality;
	}

	public boolean canDistill() {
		if (currentRecipe != null) {
			return currentRecipe.getDistillRuns() > distillRuns;
		} else if (distillRuns >= 6) {
			return false;
		}
		return true;
	}

	// return special effect
	public Map<String, Integer> getEffects() {
		if (currentRecipe != null && quality > 0) {
			return currentRecipe.getEffects();
		}
		return null;
	}

	// Set unlabeled to true to hide the numbers in Lore
	public void unLabel(ItemStack item) {
		PotionMeta meta = (PotionMeta) item.getItemMeta();
		if (meta.hasLore()) {
			if (distillRuns > 0) {
				addOrReplaceLore(meta, P.p.color("&7"), "Destilliert");
			}
			if (ageTime >= 1) {
				addOrReplaceLore(meta, P.p.color("&7"), "Fassgereift");
			}
			item.setItemMeta(meta);
		}
		unlabeled = true;
	}

	// Distilling section ---------------

	// distill all custom potions in the brewer
	public static void distillAll(BrewerInventory inv, Boolean[] contents) {
		int slot = 0;
		while (slot < 3) {
			if (contents[slot]) {
				ItemStack slotItem = inv.getItem(slot);
				PotionMeta potionMeta = (PotionMeta) slotItem.getItemMeta();
				Brew brew = get(potionMeta);
				brew.distillSlot(slotItem, potionMeta);
			}
			slot++;
		}
	}

	// distill custom potion in given slot
	public void distillSlot(ItemStack slotItem, PotionMeta potionMeta) {
		distillRuns += 1;
		BRecipe recipe = ingredients.getdistillRecipe(wood, ageTime);
		if (recipe != null) {
			// distillRuns will have an effect on the amount of alcohol, not the quality
			currentRecipe = recipe;
			quality = calcQuality();
			P.p.log("destilled " + recipe.getName(5) + " has Quality: " + quality + ", alc: " + calcAlcohol());

			addOrReplaceEffects(potionMeta, getEffects());
			potionMeta.setDisplayName(P.p.color("&f" + recipe.getName(quality)));
			slotItem.setDurability(PotionColor.valueOf(recipe.getColor()).getColorId(canDistill()));
		} else {
			quality = 0;
			potionMeta.setDisplayName(P.p.color("&f" + "Undefinierbares Destillat"));
			slotItem.setDurability(PotionColor.GREY.getColorId(canDistill()));
		}

		// Distill Lore
		if (currentRecipe != null) {
			if (colorInBrewer != hasColorLore(potionMeta)) {
				convertLore(potionMeta, colorInBrewer);
			}
		}
		String prefix = P.p.color("&7");
		if (colorInBrewer && currentRecipe != null) {
			prefix = getQualityColor(ingredients.getDistillQuality(recipe, distillRuns));
		}
		updateDistillLore(prefix, potionMeta);

		slotItem.setItemMeta(potionMeta);
	}

	// Ageing Section ------------------

	public void age(ItemStack item, float time, byte woodType) {
		PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
		ageTime += time;
		
		// if younger than half a day, it shouldnt get aged form
		if (ageTime > 0.5) {
			if (wood == 0) {
				wood = woodType;
			} else {
				if (wood != woodType) {
					woodShift(time, woodType);
				}
			}
			BRecipe recipe = ingredients.getAgeRecipe(wood, ageTime, distillRuns > 0);
			if (recipe != null) {
				currentRecipe = recipe;
				quality = calcQuality();
				P.p.log("Final " + recipe.getName(5) + " has Quality: " + quality + ", alc: " + calcAlcohol());

				addOrReplaceEffects(potionMeta, getEffects());
				potionMeta.setDisplayName(P.p.color("&f" + recipe.getName(quality)));
				item.setDurability(PotionColor.valueOf(recipe.getColor()).getColorId(canDistill()));
			} else {
				quality = 0;
				potionMeta.setDisplayName(P.p.color("&f" + "Verdorbenes Getränk"));
				item.setDurability(PotionColor.GREY.getColorId(canDistill()));
			}
		}

		// Lore
		if (currentRecipe != null) {
			if (colorInBarrels != hasColorLore(potionMeta)) {
				convertLore(potionMeta, colorInBarrels);
			}
		}
		if (ageTime >= 1) {
			String prefix = P.p.color("&7");
			if (colorInBarrels && currentRecipe != null) {
				prefix = getQualityColor(ingredients.getAgeQuality(currentRecipe, ageTime));
			}
			updateAgeLore(prefix, potionMeta);
		}
		if (ageTime > 0.5) {
			if (colorInBarrels && !unlabeled && currentRecipe != null) {
				updateWoodLore(potionMeta);
			}
		}
		item.setItemMeta(potionMeta);
	}

	// Slowly shift the wood of the Brew to the new Type
	public void woodShift(float time, byte to) {
		byte factor = 1;
		if (ageTime > 5) {
			factor = 2;
		} else if (ageTime > 10) {
			factor = 2;
			factor += Math.round(ageTime / 10);
		}
		if (wood > to) {
			wood -= time / factor;
			if (wood < to) {
				wood = to;
			}
		} else {
			wood += time / factor;
			if (wood > to) {
				wood = to;
			}
		}
	}

	// Lore -----------

	// Converts to/from qualitycolored Lore
	public void convertLore(PotionMeta meta, Boolean toQuality) {
		if (currentRecipe == null) {
			return;
		}
		meta.setLore(null);
		int quality;
		String prefix = P.p.color("&7");
		String lore;

		// Ingredients
		if (toQuality && !unlabeled) {
			quality = ingredients.getIngredientQuality(currentRecipe);
			prefix = getQualityColor(quality);
			lore = "Zutaten";
			addOrReplaceLore(meta, prefix, lore);
		}

		// Cooking
		if (toQuality && !unlabeled) {
			if (distillRuns > 0 == currentRecipe.needsDistilling()) {
				quality = ingredients.getCookingQuality(currentRecipe, distillRuns > 0);
				prefix = getQualityColor(quality) + ingredients.getCookedTime() + " minute";
				if (ingredients.getCookedTime() > 1) {
					prefix = prefix + "n";
				}
				lore = " gegärt";
				addOrReplaceLore(meta, prefix, lore);
			}
		}

		// Distilling
		if (distillRuns > 0) {
			if (toQuality) {
				quality = ingredients.getDistillQuality(currentRecipe, distillRuns);
				prefix = getQualityColor(quality);
			}
			updateDistillLore(prefix, meta);
		}

		// Ageing
		if (ageTime >= 1) {
			if (toQuality) {
				quality = ingredients.getAgeQuality(currentRecipe, ageTime);
				prefix = getQualityColor(quality);
			}
			updateAgeLore(prefix, meta);
		}

		// WoodType
		if (toQuality && !unlabeled) {
			if (ageTime > 0.5) {
				updateWoodLore(meta);
			}
		}
	}

	// sets the DistillLore. Prefix is the color to be used
	public void updateDistillLore(String prefix, PotionMeta meta) {
		if (!unlabeled) {
			if (distillRuns > 1) {
				prefix = prefix + distillRuns + "-fach ";
			}
		}
		addOrReplaceLore(meta, prefix, "Destilliert");
	}

	// sets the AgeLore. Prefix is the color to be used
	public void updateAgeLore(String prefix, PotionMeta meta) {
		if (!unlabeled) {
			if (ageTime >= 1 && ageTime < 2) {
				prefix = prefix + "Ein Jahr ";
			} else if (ageTime < 201) {
				prefix = prefix + (int) Math.floor(ageTime) + " Jahre ";
			} else {
				prefix = prefix + "Hunderte Jahre ";
			}
		}
		addOrReplaceLore(meta, prefix, "Fassgereift");
	}

	// updates/sets the color on WoodLore
	public void updateWoodLore(PotionMeta meta) {
		if (currentRecipe.getWood() > 0) {
			int quality = ingredients.getWoodQuality(currentRecipe, wood);
			addOrReplaceLore(meta, getQualityColor(quality), "Holzart");
		} else {
			if (meta.hasLore()) {
				List<String> existingLore = meta.getLore();
				int index = indexOfSubstring(existingLore, "Holzart");
				if (index > -1) {
					existingLore.remove(index);
					meta.setLore(existingLore);
				}
			}
		}
	}

	// Adds or replaces a line of Lore. Searches for Substring lore and replaces it
	public static void addOrReplaceLore(PotionMeta meta, String prefix, String lore) {
		if (meta.hasLore()) {
			List<String> existingLore = meta.getLore();
			int index = indexOfSubstring(existingLore, lore);
			if (index > -1) {
				existingLore.set(index, prefix + lore);
			} else {
				existingLore.add(prefix + lore);
			}
			meta.setLore(existingLore);
			return;
		}
		List<String> newLore = new ArrayList<String>();
		newLore.add("");
		newLore.add(prefix + lore);
		meta.setLore(newLore);
	}

	// Adds the Effect names to the Items description
	public static void addOrReplaceEffects(PotionMeta meta, Map<String, Integer> effects) {
		if (effects != null) {
			for (Map.Entry<String, Integer> entry : effects.entrySet()) {
				if (!entry.getKey().endsWith("X")) {
					PotionEffectType type = PotionEffectType.getByName(entry.getKey());
					if (type != null) {
						meta.addCustomEffect(type.createEffect(0, 0), true);
					}
				}
			}
		}
	}

	// Returns the Index of a String from the list that contains this substring
	public static int indexOfSubstring(List<String> list, String substring) {
		for (int index = 0; index < list.size(); index++) {
			String string = list.get(index);
			if (string.contains(substring)) {
				return index;
			}
		}
		return -1;
	}

	// True if the PotionMeta has colored Lore
	public  static Boolean hasColorLore(PotionMeta meta) {
		if (meta.hasLore()) {
			return !meta.getLore().get(1).startsWith(P.p.color("&7"));
		}
		return false;
	}

	// gets the Color that represents a quality in Lore
	public static String getQualityColor(int quality) {
		String color;
		if (quality > 8) {
			color = "&a";
		} else if (quality > 6) {
			color = "&e";
		} else if (quality > 4) {
			color = "&6";
		} else if (quality > 2) {
			color = "&c";
		} else {
			color = "&4";
		}
		return P.p.color(color);
	}

	// Saves all data
	public static void save(ConfigurationSection config) {
		for (Map.Entry<Integer, Brew> entry : potions.entrySet()) {
			int uid = entry.getKey();
			Brew brew = entry.getValue();
			ConfigurationSection idConfig = config.createSection("" + uid);
			// not saving unneccessary data
			if (brew.quality != 0) {
				idConfig.set("quality", brew.quality);
			}
			if (brew.distillRuns != 0) {
				idConfig.set("distillRuns", brew.distillRuns);
			}
			if (brew.ageTime != 0) {
				idConfig.set("ageTime", brew.ageTime);
			}
			if (brew.wood != -1) {
				idConfig.set("wood", brew.wood);
			}
			if (brew.currentRecipe != null) {
				idConfig.set("recipe", brew.currentRecipe.getName(5));
			}
			if (brew.unlabeled) {
				idConfig.set("unlabeled", true);
			}
			// save the ingredients
			idConfig.set("ingId", brew.ingredients.save(config.getParent()));
		}
	}

	public static enum PotionColor {
		PINK(1),
		CYAN(2),
		ORANGE(3),
		GREEN(4),
		BRIGHT_RED(5),
		BLUE(6),
		BLACK(8),
		RED(9),
		GREY(10),
		WATER(11),
		DARK_RED(12),
		BRIGHT_GREY(14);

		private final int colorId;

		private PotionColor(int colorId) {
			this.colorId = colorId;
		}

		// gets the Damage Value, that sets a color on the potion
		// offset +32 is not accepted by brewer, so not further destillable
		public short getColorId(boolean destillable) {
			if (destillable) {
				return (short) (colorId + 64);
			}
			return (short) (colorId + 32);
		}
	}

}