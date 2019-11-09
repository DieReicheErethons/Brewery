package com.dre.brewery.lore;

import com.dre.brewery.recipe.BEffect;
import com.dre.brewery.BIngredients;
import com.dre.brewery.recipe.BRecipe;
import com.dre.brewery.Brew;
import com.dre.brewery.P;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.utility.BUtil;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BrewLore {
	private Brew brew;
	private PotionMeta meta;
	private List<String> lore;
	private boolean lineAddedOrRem = false;

	public BrewLore(Brew brew, PotionMeta meta) {
		this.brew = brew;
		this.meta = meta;
		if (meta.hasLore()) {
			lore = meta.getLore();
		} else {
			lore = new ArrayList<>();
		}
	}

	/**
	 * Write the new lore into the Meta
	 * Should be called at the end of operation on this Brew Lore
 	 */
	public PotionMeta write() {
		if (lineAddedOrRem) {
			updateSpacer();
		}
		meta.setLore(lore);
		return meta;
	}

	/**
	 * adds or removes an empty line in lore to space out the text a bit
	 */
	public void updateSpacer() {
		boolean hasCustom = false;
		boolean hasSpace = false;
		for (int i = 0; i < lore.size(); i++) {
			Type t = Type.get(lore.get(i));
			if (t == Type.CUSTOM) {
				hasCustom = true;
			} else if (t == Type.SPACE) {
				hasSpace = true;
			} else if (t != null && t.isAfter(Type.SPACE)) {
				if (hasSpace) return;

				if (hasCustom || P.useNBT) {
					// We want to add the spacer if we have Custom Lore, to have a space between custom and brew lore.
					// Also add a space if there is no Custom Lore but we don't already have a invisible data line
					lore.add(i, Type.SPACE.id);
				}
				return;
			}
		}
		if (hasSpace) {
			// There was a space but nothing after the space
			removeLore(Type.SPACE);
		}
	}

	/*private void addSpacer() {
		if (!P.useNBT) return;

		for (int i = 0; i < lore.size(); i++) {
			if (Type.get(lore.get(i)) != null) {
				if (i == 0 || !lore.get(i - 1).equals("")) {
					lore.add(i, "");
				}
				break;
			}
		}
	}*/

	/**
	 * Add the list of strings as custom lore for the base potion coming out of the cauldron
	 */
	public void addCauldronLore(List<String> l) {
		int index = -1;
		for (String line : l) {
			if (index == -1) {
				index = addLore(Type.CUSTOM, "", line);
				index++;
			} else {
				lore.add(index, Type.CUSTOM.id + line);
				index++;
			}
		}
	}

	/**
	 * updates the IngredientLore
	 *
	 * @param qualityColor If the lore should have colors according to quality
	 */
	public void updateIngredientLore(boolean qualityColor) {
		if (qualityColor && brew.hasRecipe()) {
			String prefix = getQualityColor(brew.getIngredients().getIngredientQuality(brew.getCurrentRecipe()));
			addOrReplaceLore(Type.INGR, prefix, P.p.languageReader.get("Brew_Ingredients"));
		} else {
			removeLore(Type.INGR, P.p.languageReader.get("Brew_Ingredients"));
		}
	}

	/**
	 * updates the CookLore
	 *
	 * @param qualityColor If the lore should have colors according to quality
	 */
	public void updateCookLore(boolean qualityColor) {
		if (qualityColor && brew.hasRecipe() && brew.getDistillRuns() > 0 == brew.getCurrentRecipe().needsDistilling()) {
			BIngredients ingredients = brew.getIngredients();
			int quality = ingredients.getCookingQuality(brew.getCurrentRecipe(), brew.getDistillRuns() > 0);
			String prefix = getQualityColor(quality) + ingredients.getCookedTime() + " " + P.p.languageReader.get("Brew_minute");
			if (ingredients.getCookedTime() > 1) {
				prefix = prefix + P.p.languageReader.get("Brew_MinutePluralPostfix");
			}
			addOrReplaceLore(Type.COOK, prefix, " " + P.p.languageReader.get("Brew_fermented"));
		} else {
			removeLore(Type.COOK, P.p.languageReader.get("Brew_fermented"));
		}
	}

	/**
	 * updates the DistillLore
	 *
	 * @param qualityColor If the lore should have colors according to quality
	 */
	public void updateDistillLore(boolean qualityColor) {
		if (brew.getDistillRuns() <= 0) return;
		String prefix;
		byte distillRuns = brew.getDistillRuns();
		if (qualityColor && brew.hasRecipe()) {
			prefix = getQualityColor(brew.getIngredients().getDistillQuality(brew.getCurrentRecipe(), distillRuns));
		} else {
			prefix = "§7";
		}
		if (!brew.isUnlabeled()) {
			if (distillRuns > 1) {
				prefix = prefix + distillRuns + P.p.languageReader.get("Brew_-times") + " ";
			}
		}
		addOrReplaceLore(Type.DISTILL, prefix, P.p.languageReader.get("Brew_Distilled"));
	}

	/**
	 * updates the AgeLore
	 *
	 * @param qualityColor If the lore should have colors according to quality
	 */
	public void updateAgeLore(boolean qualityColor) {
		String prefix;
		float age = brew.getAgeTime();
		if (qualityColor && brew.hasRecipe()) {
			prefix = getQualityColor(brew.getIngredients().getAgeQuality(brew.getCurrentRecipe(), age));
		} else {
			prefix = "§7";
		}
		if (!brew.isUnlabeled()) {
			if (age >= 1 && age < 2) {
				prefix = prefix + P.p.languageReader.get("Brew_OneYear") + " ";
			} else if (age < 201) {
				prefix = prefix + (int) Math.floor(age) + " " + P.p.languageReader.get("Brew_Years") + " ";
			} else {
				prefix = prefix + P.p.languageReader.get("Brew_HundredsOfYears") + " ";
			}
		}
		addOrReplaceLore(Type.AGE, prefix, P.p.languageReader.get("Brew_BarrelRiped"));
	}

	/**
	 * updates the WoodLore
	 *
	 * @param qualityColor If the lore should have colors according to quality
	 */
	public void updateWoodLore(boolean qualityColor) {
		if (qualityColor && brew.hasRecipe()) {
			int quality = brew.getIngredients().getWoodQuality(brew.getCurrentRecipe(), brew.getWood());
			addOrReplaceLore(Type.WOOD, getQualityColor(quality), P.p.languageReader.get("Brew_Woodtype"));
		} else {
			removeLore(Type.WOOD, P.p.languageReader.get("Brew_Woodtype"));
		}
	}

	/**
	 * updates the Custom Lore
	 */
	public void updateCustomLore() {
		removeLore(Type.CUSTOM);

		BRecipe recipe = brew.getCurrentRecipe();
		if (recipe != null && recipe.hasLore()) {
			int index = -1;
			for (String line : recipe.getLoreForQuality(brew.getQuality())) {
				if (index == -1) {
					index = addLore(Type.CUSTOM, "", line);
					index++;
				} else {
					lore.add(index, Type.CUSTOM.id + line);
					index++;
				}
			}
		}
	}

	public void updateQualityStars(boolean qualityColor) {
		if (brew.hasRecipe() && brew.getCurrentRecipe().needsToAge() && brew.getAgeTime() < 0.5) {
			return;
		}
		if (!brew.isUnlabeled() && brew.getQuality() > 0 && (qualityColor || BConfig.alwaysShowQuality)) {
			int stars = (brew.getQuality() + 1) / 2;
			StringBuilder b = new StringBuilder(stars);
			for (; stars > 0; stars--) {
				b.append("⭑");
			}
			String color;
			if (qualityColor) {
				color = getQualityColor(brew.getQuality());
			} else {
				color = brew.getQuality() >= 10 ? "§6" : "§8";
			}
			addOrReplaceLore(Type.STARS, color, b.toString());
		} else {
			removeLore(Type.STARS);
		}
	}

	public void updateAlc(boolean inDistiller) {
		if (!brew.isUnlabeled() && (inDistiller || BConfig.alwaysShowAlc) && (!brew.hasRecipe() || brew.getCurrentRecipe().getAlcohol() > 0)) {
			int alc = brew.getOrCalcAlc();
			addOrReplaceLore(Type.ALC, "§8", P.p.languageReader.get("Brew_Alc", alc + ""));
		} else {
			removeLore(Type.ALC);
		}
	}

	/**
	 * Converts to/from qualitycolored Lore
 	 */
	public void convertLore(boolean toQuality) {
		if (!brew.hasRecipe()) {
			return;
		}

		updateCustomLore();
		updateQualityStars(toQuality);

		if (!brew.isUnlabeled()) {
			// Ingredients
			updateIngredientLore(toQuality);

			// Cooking
			updateCookLore(toQuality);
		}

		// Distilling
		updateDistillLore(toQuality);

		// Ageing
		if (brew.getAgeTime() >= 1) {
			updateAgeLore(toQuality);
		}

		// WoodType
		if (!brew.isUnlabeled()) {
			if (brew.getAgeTime() > 0.5) {
				updateWoodLore(toQuality);
			}
		}

		updateAlc(false);
	}

	/**
	 * Adds or replaces a line of Lore.
	 * Searches for type and if not found for Substring lore and replaces it
	 *
	 * @param type The Type of BrewLore to replace
	 * @param prefix The Prefix to add to the line of lore
	 * @param line The Line of Lore to add or replace
 	 */
	public int addOrReplaceLore(Type type, String prefix, String line) {
		int index = type.findInLore(lore);
		if (index == -1) {
			index = BUtil.indexOfSubstring(lore, line);
		}
		if (index > -1) {
			lore.set(index, type.id + prefix + line);
			return index;
		} else {
			return addLore(type, prefix, line);
		}
	}

	/**
	 * Adds a line of Lore in the correct ordering
	 *
	 * @param type The Type of BrewLore to add
	 * @param prefix The Prefix to add to the line of lore
	 * @param line The Line of Lore to add or add
 	 */
	public int addLore(Type type, String prefix, String line) {
		lineAddedOrRem = true;
		for (int i = 0; i < lore.size(); i++) {
			Type existing = Type.get(lore.get(i));
			if (existing != null && existing.isAfter(type)) {
				lore.add(i, type.id + prefix + line);
				return i;
			}
		}
		lore.add(type.id + prefix + line);
		return lore.size() - 1;
	}

	/**
	 * Searches for type and if not found for Substring lore and removes it
 	 */
	public void removeLore(Type type, String line) {
		int index = type.findInLore(lore);
		if (index == -1) {
			index = BUtil.indexOfSubstring(lore, line);
		}
		if (index > -1) {
			lineAddedOrRem = true;
			lore.remove(index);
		}
	}

	/**
	 * Searches for type and removes it
 	 */
	public void removeLore(Type type) {
		if (type != Type.CUSTOM) {
			int index = type.findInLore(lore);
			if (index > -1) {
				lineAddedOrRem = true;
				lore.remove(index);
			}
		} else {
			// Lore could have multiple lines of this type
			for (int i = lore.size() - 1; i >= 0; i--) {
				if (Type.get(lore.get(i)) == type) {
					lore.remove(i);
					lineAddedOrRem = true;
				}
			}
		}
	}

	/**
	 * Removes all Brew Lore lines
	 */
	public void removeAll() {
		for (int i = lore.size() - 1; i >= 0; i--) {
			if (Type.get(lore.get(i)) != null) {
				lore.remove(i);
				lineAddedOrRem = true;
			}
		}
	}

	/**
	 * Adds the Effect names to the Items description
 	 */
	public void addOrReplaceEffects(List<BEffect> effects, int quality) {
		if (!P.use1_9 && effects != null) {
			for (BEffect effect : effects) {
				if (!effect.isHidden()) {
					effect.writeInto(meta, quality);
				}
			}
		}
	}

	/**
	 * If the Lore Line at index is a Brew Lore line
	 *
	 * @param index the index in lore to check
	 * @return true if the line at index is of any Brew Lore type
	 */
	public boolean isBrewLore(int index) {
		return index < lore.size() && Type.get(lore.get(index)) != null;
	}

	/**
	 * Removes all effects
 	 */
	public void removeEffects() {
		if (meta.hasCustomEffects()) {
			for (PotionEffect effect : new ArrayList<>(meta.getCustomEffects())) {
				PotionEffectType type = effect.getType();
				//if (!type.equals(PotionEffectType.REGENERATION)) {
				meta.removeCustomEffect(type);
				//}
			}
		}
	}

	/**
	 * Remove the Old Spacer from the legacy potion data system
	 */
	public void removeLegacySpacing() {
		if (P.useNBT) {
			// Using NBT we don't get the invisible line, so we keep our spacing
			return;
		}
		if (lore.size() > 0 && lore.get(0).equals("")) {
			lore.remove(0);
			write();
		}
	}

	/**
	 * Remove any Brew Data from Lore
	 */
	public void removeLoreData() {
		int index = BUtil.indexOfStart(lore, LoreSaveStream.IDENTIFIER);
		if (index != -1) {
			lore.set(index, "");
			write();
		}
	}

	/**
	 * True if the PotionMeta has Lore in quality color
 	 */
	public static boolean hasColorLore(PotionMeta meta) {
		if (!meta.hasLore()) return false;
		List<String> lore = meta.getLore();
		if (lore.size() < 2) {
			return false;
		}
		if (Type.INGR.findInLore(lore) != -1) {
			// Ingredient lore present, must be quality colored
			return true;
		}
		return false;
		//!meta.getLore().get(1).startsWith("§7");
	}

	/**
	 * gets the Color that represents a quality in Lore
	 *
	 * @param quality The Quality for which to find the color code
	 * @return Color Code for given Quality
	 */
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

	public enum Type {
		STARS("§s"),
		CUSTOM("§t"),
		SPACE("§u"),

		INGR("§v"),
		COOK("§w"),
		DISTILL("§x"),
		AGE("§y"),
		WOOD("§z"),
		ALC("§k");

		public final String id;

		/**
		 * @param id Identifier as Prefix of the Loreline
		 */
		Type(String id) {
			this.id = id;
		}

		/**
		 * Find this type in the Lore
		 *
		 * @param lore The lore to search in
		 * @return index of this type in the lore, -1 if not found
		 */
		public int findInLore(List<String> lore) {
			return BUtil.indexOfStart(lore, id);
		}

		/**
		 * Is this type after the other in lore
		 *
		 * @param other the other type
		 * @return true if this type should be after the other type in lore
		 */
		public boolean isAfter(Type other) {
			return other.ordinal() <= ordinal();
		}

		/**
		 * Get the Type of the given line of Lore
		 */
		@Nullable
		public static Type get(String loreLine) {
			if (loreLine.length() >= 2) {
				return getById(loreLine.substring(0, 2));
			} else {
				return null;
			}
		}

		/**
		 * Get the Type of the given Identifier, prefix of a line of lore
		 */
		@Nullable
		public static Type getById(String id) {
			for (Type t : values()) {
				if (t.id.equals(id)) {
					return t;
				}
			}
			return null;
		}

	}
}
