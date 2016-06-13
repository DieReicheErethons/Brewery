package com.dre.brewery;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Brew {

	// represents the liquid in the brewed Potions

	public static Map<Integer, Brew> potions = new HashMap<>();
	public static long installTime = System.currentTimeMillis(); // plugin install time in millis after epoch
	public static Boolean colorInBarrels; // color the Lore while in Barrels
	public static Boolean colorInBrewer; // color the Lore while in Brewer

	private BIngredients ingredients;
	private int quality;
	private int distillRuns;
	private float ageTime;
	private float wood;
	private BRecipe currentRecipe;
	private boolean unlabeled;
	private boolean persistent;
	private boolean stat; // static potions should not be changed
	private int lastUpdate; // last update in hours after install time

	public Brew(int uid, BIngredients ingredients) {
		this.ingredients = ingredients;
		touch();
		potions.put(uid, this);
	}

	// quality already set
	public Brew(int uid, int quality, BRecipe recipe, BIngredients ingredients) {
		this.ingredients = ingredients;
		this.quality = quality;
		this.currentRecipe = recipe;
		touch();
		potions.put(uid, this);
	}

	// loading from file
	public Brew(int uid, BIngredients ingredients, int quality, int distillRuns, float ageTime, float wood, String recipe, boolean unlabeled, boolean persistent, boolean stat, int lastUpdate) {
		potions.put(uid, this);
		this.ingredients = ingredients;
		this.quality = quality;
		this.distillRuns = distillRuns;
		this.ageTime = ageTime;
		this.wood = wood;
		this.unlabeled = unlabeled;
		this.persistent = persistent;
		this.stat = stat;
		this.lastUpdate = lastUpdate;
		setRecipeFromString(recipe);
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
		if (item.getType() == Material.POTION) {
			if (item.hasItemMeta()) {
				return get((PotionMeta) item.getItemMeta());
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

	// generate an UID
	public static int generateUID() {
		int uid = -2;
		while (potions.containsKey(uid)) {
			uid -= 1;
		}
		return uid;
	}

	//returns the recipe with the given name, recalculates if not found
	public boolean setRecipeFromString(String name) {
		currentRecipe = null;
		if (name != null && !name.equals("")) {
			for (BRecipe recipe : BIngredients.recipes) {
				if (recipe.getName(5).equalsIgnoreCase(name)) {
					currentRecipe = recipe;
					return true;
				}
			}

			if (quality > 0) {
				currentRecipe = ingredients.getBestRecipe(wood, ageTime, distillRuns > 0);
				if (currentRecipe != null) {
					if (!stat) {
						this.quality = calcQuality();
					}
					P.p.log("Brew was made from Recipe: '" + name + "' which could not be found. '" + currentRecipe.getName(5) + "' used instead!");
					return true;
				} else {
					P.p.errorLog("Brew was made from Recipe: '" + name + "' which could not be found!");
				}
			}
		}
		return false;
	}

	public boolean reloadRecipe() {
		return currentRecipe == null || setRecipeFromString(currentRecipe.getName(5));
	}

	// Copy a Brew with a new unique ID and return its item
	public ItemStack copy(ItemStack item) {
		ItemStack copy = item.clone();
		int uid = generateUID();
		clone(uid);
		PotionMeta meta = (PotionMeta) copy.getItemMeta();
		if (!P.use1_14) {
			// This is due to the Duration Modifier, that is removed in 1.14
			uid *= 4;
		}
		meta.addCustomEffect((PotionEffectType.REGENERATION).createEffect(uid, 0), true);
		copy.setItemMeta(meta);
		return copy;
	}

	// Clones this instance with a new unique ID
	public Brew clone(int uid) {
		Brew brew = new Brew(uid, quality, currentRecipe, ingredients);
		brew.distillRuns = distillRuns;
		brew.ageTime = ageTime;
		brew.unlabeled = unlabeled;
		if (!brew.persistent) {
			brew.stat = stat;
		}
		return brew;
	}

	// remove potion from file (drinking, despawning, combusting, cmdDeleting, should be more!)
	public void remove(ItemStack item) {
		if (!persistent) {
			potions.remove(getUID(item));
		}
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
				alc *= 1.0F + ((float) distillRuns / currentRecipe.getDistillRuns());
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
		return Math.round(quality);
	}

	public int getQuality() {
		return quality;
	}

	public boolean canDistill() {
		if (stat) return false;
		if (currentRecipe != null) {
			return currentRecipe.getDistillRuns() > distillRuns;
		} else {
			return distillRuns < 6;
		}
	}

	// return special effect
	public ArrayList<BEffect> getEffects() {
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
				addOrReplaceLore(meta, P.p.color("&7"), P.p.languageReader.get("Brew_Distilled"));
			}
			if (ageTime >= 1) {
				addOrReplaceLore(meta, P.p.color("&7"), P.p.languageReader.get("Brew_BarrelRiped"));
			}
			item.setItemMeta(meta);
		}
		unlabeled = true;
	}

	// Do some regular updates
	public void touch() {
		lastUpdate = (int) ((double) (System.currentTimeMillis() - installTime) / 3600000D);
	}

	public int getDistillRuns() {
		return distillRuns;
	}

	public float getAgeTime() {
		return ageTime;
	}

	public BRecipe getCurrentRecipe() {
		return currentRecipe;
	}

	public boolean isPersistent() {
		return persistent;
	}

	// Make a potion persistent to not delete it when drinking it
	public void makePersistent() {
		persistent = true;
	}

	// Remove the Persistence Flag from a brew, so it will be normally deleted when drinking it
	public void removePersistence() {
		persistent = false;
	}

	public boolean isStatic() {
		return stat;
	}

	// Set the Static flag, so potion is unchangeable
	public void setStatic(boolean stat, ItemStack potion) {
		this.stat = stat;
		if (currentRecipe != null && canDistill()) {
			if (stat) {
				PotionColor.fromString(currentRecipe.getColor()).colorBrew(((PotionMeta) potion.getItemMeta()), potion, false);
			} else {
				PotionColor.fromString(currentRecipe.getColor()).colorBrew(((PotionMeta) potion.getItemMeta()), potion, true);
			}
		}
	}

	public int getLastUpdate() {
		return lastUpdate;
	}

	// Distilling section ---------------

	// distill all custom potions in the brewer
	public static void distillAll(BrewerInventory inv, Brew[] contents) {
		for (int slot = 0; slot < 3; slot++) {
			if (contents[slot] != null) {
				ItemStack slotItem = inv.getItem(slot);
				PotionMeta potionMeta = (PotionMeta) slotItem.getItemMeta();
				contents[slot].distillSlot(slotItem, potionMeta);
			}
		}
	}

	// distill custom potion in given slot
	public void distillSlot(ItemStack slotItem, PotionMeta potionMeta) {
		if (stat) {
			return;
		}

		distillRuns += 1;
		BRecipe recipe = ingredients.getdistillRecipe(wood, ageTime);
		if (recipe != null) {
			// distillRuns will have an effect on the amount of alcohol, not the quality
			currentRecipe = recipe;
			quality = calcQuality();

			addOrReplaceEffects(potionMeta, getEffects(), quality);
			potionMeta.setDisplayName(P.p.color("&f" + recipe.getName(quality)));
			PotionColor.fromString(recipe.getColor()).colorBrew(potionMeta, slotItem, canDistill());

		} else {
			quality = 0;
			removeEffects(potionMeta);
			potionMeta.setDisplayName(P.p.color("&f" + P.p.languageReader.get("Brew_DistillUndefined")));
			PotionColor.GREY.colorBrew(potionMeta, slotItem, canDistill());
		}

		// Distill Lore
		if (currentRecipe != null) {
			if (colorInBrewer != hasColorLore(potionMeta)) {
				convertLore(potionMeta, colorInBrewer);
			}
		}
		String prefix = P.p.color("&7");
		if (colorInBrewer && currentRecipe != null) {
			prefix = getQualityColor(ingredients.getDistillQuality(currentRecipe, distillRuns));
		}
		updateDistillLore(prefix, potionMeta);
		touch();

		slotItem.setItemMeta(potionMeta);
	}

	public int getDistillTimeNextRun() {
		if (!canDistill()) {
			return -1;
		}

		if (currentRecipe != null) {
			return currentRecipe.getDistillTime();
		}

		BRecipe recipe = ingredients.getdistillRecipe(wood, ageTime);
		if (recipe != null) {
			return recipe.getDistillTime();
		}
		return 0;
	}

	// Ageing Section ------------------

	public void age(ItemStack item, float time, byte woodType) {
		if (stat) {
			return;
		}

		PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
		ageTime += time;

		// if younger than half a day, it shouldnt get aged form
		if (ageTime > 0.5) {
			if (wood == 0) {
				wood = woodType;
			} else if (wood != woodType) {
				woodShift(time, woodType);
			}
			BRecipe recipe = ingredients.getAgeRecipe(wood, ageTime, distillRuns > 0);
			if (recipe != null) {
				currentRecipe = recipe;
				quality = calcQuality();

				addOrReplaceEffects(potionMeta, getEffects(), quality);
				potionMeta.setDisplayName(P.p.color("&f" + recipe.getName(quality)));
				PotionColor.fromString(recipe.getColor()).colorBrew(potionMeta, item, canDistill());
			} else {
				quality = 0;
				removeEffects(potionMeta);
				potionMeta.setDisplayName(P.p.color("&f" + P.p.languageReader.get("Brew_BadPotion")));
				PotionColor.GREY.colorBrew(potionMeta, item, canDistill());
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
		touch();
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
			lore = P.p.languageReader.get("Brew_Ingredients");
			addOrReplaceLore(meta, prefix, lore);
		}

		// Cooking
		if (toQuality && !unlabeled) {
			if (distillRuns > 0 == currentRecipe.needsDistilling()) {
				quality = ingredients.getCookingQuality(currentRecipe, distillRuns > 0);
				prefix = getQualityColor(quality) + ingredients.getCookedTime() + " " + P.p.languageReader.get("Brew_minute");
				if (ingredients.getCookedTime() > 1) {
					prefix = prefix + P.p.languageReader.get("Brew_MinutePluralPostfix");
				}
				lore = " " + P.p.languageReader.get("Brew_fermented");
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
				prefix = prefix + distillRuns + P.p.languageReader.get("Brew_-times") + " ";
			}
		}
		addOrReplaceLore(meta, prefix, P.p.languageReader.get("Brew_Distilled"));
	}

	// sets the AgeLore. Prefix is the color to be used
	public void updateAgeLore(String prefix, PotionMeta meta) {
		if (!unlabeled) {
			if (ageTime >= 1 && ageTime < 2) {
				prefix = prefix + P.p.languageReader.get("Brew_OneYear") + " ";
			} else if (ageTime < 201) {
				prefix = prefix + (int) Math.floor(ageTime) + " " + P.p.languageReader.get("Brew_Years") + " ";
			} else {
				prefix = prefix + P.p.languageReader.get("Brew_HundredsOfYears") + " ";
			}
		}
		addOrReplaceLore(meta, prefix, P.p.languageReader.get("Brew_BarrelRiped"));
	}

	// updates/sets the color on WoodLore
	public void updateWoodLore(PotionMeta meta) {
		if (currentRecipe.getWood() > 0) {
			int quality = ingredients.getWoodQuality(currentRecipe, wood);
			addOrReplaceLore(meta, getQualityColor(quality), P.p.languageReader.get("Brew_Woodtype"));
		} else if (meta.hasLore()) {
			List<String> existingLore = meta.getLore();
			int index = indexOfSubstring(existingLore, P.p.languageReader.get("Brew_Woodtype"));
			if (index > -1) {
				existingLore.remove(index);
				meta.setLore(existingLore);
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
		List<String> newLore = new ArrayList<>();
		newLore.add("");
		newLore.add(prefix + lore);
		meta.setLore(newLore);
	}

	// Adds the Effect names to the Items description
	public static void addOrReplaceEffects(PotionMeta meta, ArrayList<BEffect> effects, int quality) {
		if (!P.use1_9 && effects != null) {
			for (BEffect effect : effects) {
				if (!effect.isHidden()) {
					effect.writeInto(meta, quality);
				}
			}
		}
	}

	// Removes all effects except regeneration which stores data
	public static void removeEffects(PotionMeta meta) {
		if (meta.hasCustomEffects()) {
			for (PotionEffect effect : meta.getCustomEffects()) {
				PotionEffectType type = effect.getType();
				if (!type.equals(PotionEffectType.REGENERATION)) {
					meta.removeCustomEffect(type);
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
	public static Boolean hasColorLore(PotionMeta meta) {
		return meta.hasLore() && (meta.getLore().size() > 1 && !meta.getLore().get(1).startsWith(P.p.color("&7")));
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

	public void testStore(DataOutputStream out) throws IOException {
		out.writeByte(86); // Parity
		out.writeByte(1); // Version
		out.writeInt(quality);
		int bools = 0;
		bools += (distillRuns != 0 ? 1 : 0);
		bools += (ageTime > 0 ? 2 : 0);
		bools += (wood != -1 ? 4 : 0);
		bools += (currentRecipe != null ? 8 : 0);
		bools += (unlabeled ? 16 : 0);
		bools += (persistent ? 32 : 0);
		bools += (stat ? 64 : 0);
		out.writeByte(bools);
		if (distillRuns != 0) {
			out.writeByte(distillRuns);
		}
		if (ageTime > 0) {
			out.writeFloat(ageTime);
		}
		if (wood != -1) {
			out.writeFloat(wood);
		}
		if (currentRecipe != null) {
			out.writeUTF(currentRecipe.getName(5));
		}
		ingredients.testStore(out);
	}

	public void testLoad(DataInputStream in) throws IOException {
		if (in.readByte() != 86) {
			P.p.log("parity check failed");
			return;
		}
		if (in.readByte() != 1) {
			P.p.log("unknown version");
			return;
		}
		if (in.readInt() != quality) {
			P.p.log("quality wrong");
		}
		int bools = in.readUnsignedByte();
		if ((bools & 1) != 0) {
			if (in.readByte() != distillRuns) {
				P.p.log("distillruns wrong");
			}
		}
		if ((bools & 2) != 0) {
			if (in.readFloat() != ageTime) {
				P.p.log("agetime wrong");
			}
		}
		if ((bools & 4) != 0) {
			if (in.readFloat() != wood) {
				P.p.log("wood wrong");
			}
		}
		if ((bools & 8) != 0) {
			if (!in.readUTF().equals(currentRecipe.getName(5))) {
				P.p.log("currecipe wrong");
			}
		}
		if ((bools & 16) != 0 && !unlabeled) {
			P.p.log("unlabeled wrong");
		}
		if ((bools & 32) != 0 && !persistent) {
			P.p.log("persistent wrong");
		}
		if ((bools & 64) != 0 && !stat) {
			P.p.log("stat wrong");
		}
		ingredients.testLoad(in);
		P.p.log("load successful");
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
			if (brew.persistent) {
				idConfig.set("persist", true);
			}
			if (brew.stat) {
				idConfig.set("stat", true);
			}
			if (brew.lastUpdate > 0) {
				idConfig.set("lastUpdate", brew.lastUpdate);
			}
			// save the ingredients
			idConfig.set("ingId", brew.ingredients.save(config.getParent()));
		}
	}

	public static class PotionColor {
		public static final PotionColor PINK = new PotionColor(1, PotionType.REGEN, Color.FUCHSIA);
		public static final PotionColor CYAN = new PotionColor(2, PotionType.SPEED, Color.AQUA);
		public static final PotionColor ORANGE = new PotionColor(3, PotionType.FIRE_RESISTANCE, Color.ORANGE);
		public static final PotionColor GREEN = new PotionColor(4, PotionType.POISON, Color.GREEN);
		public static final PotionColor BRIGHT_RED = new PotionColor(5, PotionType.INSTANT_HEAL, Color.fromRGB(255,0,0));
		public static final PotionColor BLUE = new PotionColor(6, PotionType.NIGHT_VISION, Color.NAVY);
		public static final PotionColor BLACK = new PotionColor(8, PotionType.WEAKNESS, Color.BLACK);
		public static final PotionColor RED = new PotionColor(9, PotionType.STRENGTH, Color.fromRGB(196,0,0));
		public static final PotionColor GREY = new PotionColor(10, PotionType.SLOWNESS, Color.GRAY);
		public static final PotionColor WATER = new PotionColor(11, P.use1_9 ? PotionType.WATER_BREATHING : null, Color.BLUE);
		public static final PotionColor DARK_RED = new PotionColor(12, PotionType.INSTANT_DAMAGE, Color.fromRGB(128,0,0));
		public static final PotionColor BRIGHT_GREY = new PotionColor(14, PotionType.INVISIBILITY, Color.SILVER);

		private final int colorId;
		private final PotionType type;
		private final Color color;

		PotionColor(int colorId, PotionType type, Color color) {
			this.colorId = colorId;
			this.type = type;
			this.color = color;
		}

		public PotionColor(Color color) {
			colorId = -1;
			type = WATER.getType();
			this.color = color;
		}

		// gets the Damage Value, that sets a color on the potion
		// offset +32 is not accepted by brewer, so not further destillable
		public short getColorId(boolean destillable) {
			if (destillable) {
				return (short) (colorId + 64);
			}
			return (short) (colorId + 32);
		}

		public PotionType getType() {
			return type;
		}

		public Color getColor() {
			return color;
		}

		@SuppressWarnings("deprecation")
		public void colorBrew(PotionMeta meta, ItemStack potion, boolean destillable) {
			if (P.use1_9) {
				meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
				if (P.use1_11) {
					// BasePotionData was only used for the Color, so starting with 1.12 we can use setColor instead
					meta.setColor(getColor());
				} else {
					meta.setBasePotionData(new PotionData(getType()));
				}
			} else {
				potion.setDurability(getColorId(destillable));
			}
		}

		public static PotionColor fromString(String string) {
			switch (string) {
				case "PINK": return PINK;
				case "CYAN": return CYAN;
				case "ORANGE": return ORANGE;
				case "GREEN": return GREEN;
				case "BRIGHT_RED": return BRIGHT_RED;
				case "BLUE": return BLUE;
				case "BLACK": return BLACK;
				case "RED": return RED;
				case "GREY": return GREY;
				case "WATER": return WATER;
				case "DARK_RED": return DARK_RED;
				case "BRIGHT_GREY": return BRIGHT_GREY;
				default:
					try{
						if (string.length() >= 7) {
							string = string.substring(1);
						}
						return new PotionColor(Color.fromRGB(
							Integer.parseInt(string.substring( 0, 2 ), 16 ),
							Integer.parseInt(string.substring( 2, 4 ), 16 ),
							Integer.parseInt(string.substring( 4, 6 ), 16 )
						));
					} catch (Exception e) {
						return WATER;
					}
			}
		}

	}

}
