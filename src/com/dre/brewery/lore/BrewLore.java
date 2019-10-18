package com.dre.brewery.lore;

import com.dre.brewery.*;
import com.dre.brewery.utility.BUtil;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class BrewLore {

	private static final String INGR = "§v";
	private static final String COOK = "§w";
	private static final String DISTILL = "§x";
	private static final String AGE = "§y";
	private static final String WOOD = "§z";

	private Brew brew;
	private PotionMeta meta;
	private List<String> lore;

	public BrewLore(Brew brew, PotionMeta meta) {
		this.brew = brew;
		this.meta = meta;
		if (meta.hasLore()) {
			lore = meta.getLore();
		} else {
			lore = new ArrayList<>();
		}
	}

	// Write the new lore into the Meta
	public PotionMeta write() {
		meta.setLore(lore);
		return meta;
	}

	public void updateIngredientLore(boolean qualityColor) {
		if (qualityColor && brew.hasRecipe()) {
			String prefix = getQualityColor(brew.getIngredients().getIngredientQuality(brew.getCurrentRecipe()));
			addOrReplaceLore(INGR, prefix, P.p.languageReader.get("Brew_Ingredients"));
		} else {
			removeLore(INGR, P.p.languageReader.get("Brew_Ingredients"));
		}
	}

	public void updateCookLore(boolean qualityColor) {
		if (qualityColor && brew.hasRecipe() && brew.getDistillRuns() > 0 == brew.getCurrentRecipe().needsDistilling()) {
			BIngredients ingredients = brew.getIngredients();
			int quality = ingredients.getCookingQuality(brew.getCurrentRecipe(), brew.getDistillRuns() > 0);
			String prefix = getQualityColor(quality) + ingredients.getCookedTime() + " " + P.p.languageReader.get("Brew_minute");
			if (ingredients.getCookedTime() > 1) {
				prefix = prefix + P.p.languageReader.get("Brew_MinutePluralPostfix");
			}
			addOrReplaceLore(COOK, prefix, " " + P.p.languageReader.get("Brew_fermented"));
		} else {
			removeLore(COOK, P.p.languageReader.get("Brew_fermented"));
		}
	}

	// sets the DistillLore. Prefix is the color to be used
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
		addOrReplaceLore(DISTILL, prefix, P.p.languageReader.get("Brew_Distilled"));
	}

	// sets the AgeLore. Prefix is the color to be used
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
		addOrReplaceLore(AGE, prefix, P.p.languageReader.get("Brew_BarrelRiped"));
	}

	// updates/sets the color on WoodLore
	public void updateWoodLore(boolean qualityColor) {
		if (qualityColor && brew.hasRecipe()) {
			int quality = brew.getIngredients().getWoodQuality(brew.getCurrentRecipe(), brew.getWood());
			addOrReplaceLore(WOOD, getQualityColor(quality), P.p.languageReader.get("Brew_Woodtype"));
		} else {
			removeLore(WOOD, P.p.languageReader.get("Brew_Woodtype"));
		}
	}

	// Converts to/from qualitycolored Lore
	public void convertLore(boolean toQuality) {
		if (!brew.hasRecipe()) {
			return;
		}

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
	}

	// Adds or replaces a line of Lore.
	// Searches for type and if not found for Substring lore and replaces it
	public void addOrReplaceLore(String type, String prefix, String line) {
		int index = BUtil.indexOfStart(lore, type);
		if (index == -1) {
			index = BUtil.indexOfSubstring(lore, line);
		}
		if (index > -1) {
			lore.set(index, type + prefix + line);
		} else {
			lore.add(type + prefix + line);
		}
	}

	// Adds or replaces a line of Lore.
	// Searches for type and if not found for Substring lore and replaces it
	public void removeLore(String type, String line) {
		int index = BUtil.indexOfStart(lore, type);
		if (index == -1) {
			index = BUtil.indexOfSubstring(lore, line);
		}
		if (index > -1) {
			lore.remove(index);
		}
	}

	// Adds the Effect names to the Items description
	public void addOrReplaceEffects(ArrayList<BEffect> effects, int quality) {
		if (!P.use1_9 && effects != null) {
			for (BEffect effect : effects) {
				if (!effect.isHidden()) {
					effect.writeInto(meta, quality);
				}
			}
		}
	}

	// Removes all effects
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

	public void removeLegacySpacing() {
		if (lore.size() > 0 && lore.get(0).equals("")) {
			lore.remove(0);
			write();
		}
	}

	// True if the PotionMeta has Lore in quality color
	public static boolean hasColorLore(PotionMeta meta) {
		if (!meta.hasLore()) return false;
		List<String> lore = meta.getLore();
		if (lore.size() < 2) {
			return false;
		}
		if (BUtil.indexOfStart(lore, INGR) != -1) {
			// Ingredient lore present, must be quality colored
			return true;
		}
		return false;
		//!meta.getLore().get(1).startsWith("§7");
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
}
