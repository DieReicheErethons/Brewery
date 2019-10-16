package com.dre.brewery;

import com.dre.brewery.api.events.brew.BrewModifyEvent;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.lore.*;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Brew {

	// represents the liquid in the brewed Potions
	private static long saveSeed;
	public static Map<Integer, Brew> legacyPotions = new HashMap<>();
	public static long installTime = System.currentTimeMillis(); // plugin install time in millis after epoch

	private BIngredients ingredients;
	private int quality;
	private byte distillRuns;
	private float ageTime;
	private float wood;
	private BRecipe currentRecipe; // Recipe this Brew is currently Based off. May change between modifications and is often null when not modifying
	private boolean unlabeled;
	private boolean persistent; // Only for legacy
	private boolean immutable; // static/immutable potions should not be changed
	private int lastUpdate; // last update in hours after install time

	public Brew(BIngredients ingredients) {
		this.ingredients = ingredients;
		touch();
	}

	// quality already set
	public Brew(int quality, BRecipe recipe, BIngredients ingredients) {
		this.ingredients = ingredients;
		this.quality = quality;
		this.currentRecipe = recipe;
		touch();
	}

	// loading with all values set
	public Brew(BIngredients ingredients, int quality, byte distillRuns, float ageTime, float wood, String recipe, boolean unlabeled, boolean immutable, int lastUpdate) {
		this.ingredients = ingredients;
		this.quality = quality;
		this.distillRuns = distillRuns;
		this.ageTime = ageTime;
		this.wood = wood;
		this.unlabeled = unlabeled;
		this.immutable = immutable;
		this.lastUpdate = lastUpdate;
		setRecipeFromString(recipe);
	}

	// Loading from InputStream
	private Brew() {
	}

	// returns a Brew by ItemMeta
	public static Brew get(ItemMeta meta) {
		if (!meta.hasLore()) return null;

		if (meta instanceof PotionMeta && ((PotionMeta) meta).hasCustomEffect(PotionEffectType.REGENERATION)) {
			Brew brew = load(meta);
			if (brew != null) {
				// Load Legacy
				brew = getFromPotionEffect(((PotionMeta) meta), false);
			}
			return brew;
		} else {
			return load(meta);
		}
	}

	// returns a Brew by ItemStack
	public static Brew get(ItemStack item) {
		if (item.getType() != Material.POTION) return null;
		if (!item.hasItemMeta()) return null;

		ItemMeta meta = item.getItemMeta();
		if (!meta.hasLore()) return null;

		if (meta instanceof PotionMeta && ((PotionMeta) meta).hasCustomEffect(PotionEffectType.REGENERATION)) {
			Brew brew = load(meta);
			if (brew != null) {
				((PotionMeta) meta).removeCustomEffect(PotionEffectType.REGENERATION);
			} else {
				// Load Legacy and convert
				brew = getFromPotionEffect(((PotionMeta) meta), true);
				if (brew == null) return null;
				brew.save(meta);
			}
			item.setItemMeta(meta);
			return brew;
		} else {
			return load(meta);
		}
	}

	// Legacy Brew Loading
	private static Brew getFromPotionEffect(PotionMeta potionMeta, boolean remove) {
		for (PotionEffect effect : potionMeta.getCustomEffects()) {
			if (effect.getType().equals(PotionEffectType.REGENERATION)) {
				if (effect.getDuration() < -1) {
					if (remove) {
						return legacyPotions.remove(effect.getDuration());
					} else {
						return legacyPotions.get(effect.getDuration());
					}
				}
			}
		}
		return null;
	}

	// returns a Brew by its UID
	// Does not work anymore with new save system
	@Deprecated
	public static Brew get(int uid) {
		if (uid < -1) {
			if (!legacyPotions.containsKey(uid)) {
				P.p.errorLog("Database failure! unable to find UID " + uid + " of a custom Potion!");
				return null;// throw some exception?
			}
		} else {
			return null;
		}
		return legacyPotions.get(uid);
	}

	// returns UID of custom Potion item
	// Does not work anymore with new save system
	@Deprecated
	public static int getUID(ItemStack item) {
		return getUID((PotionMeta) item.getItemMeta());
	}

	// returns UID of custom Potion meta
	// Does not work anymore with new save system
	@Deprecated
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
	/*public static int generateUID() {
		int uid = -2;
		while (potions.containsKey(uid)) {
			uid -= 1;
		}
		return uid;
	}*/

	//returns the recipe with the given name, recalculates if not found
	public boolean setRecipeFromString(String name) {
		currentRecipe = null;
		if (name != null && !name.equals("")) {
			for (BRecipe recipe : BIngredients.recipes) {
				if (recipe.getRecipeName().equalsIgnoreCase(name)) {
					currentRecipe = recipe;
					return true;
				}
			}

			if (quality > 0) {
				currentRecipe = ingredients.getBestRecipe(wood, ageTime, distillRuns > 0);
				if (currentRecipe != null) {
					if (!immutable) {
						this.quality = calcQuality();
					}
					P.p.log("Brew was made from Recipe: '" + name + "' which could not be found. '" + currentRecipe.getRecipeName() + "' used instead!");
					return true;
				} else {
					P.p.errorLog("Brew was made from Recipe: '" + name + "' which could not be found!");
				}
			}
		}
		return false;
	}

	public boolean reloadRecipe() {
		return currentRecipe == null || setRecipeFromString(currentRecipe.getRecipeName());
	}

	// Copy a Brew with a new unique ID and return its item
	// Not needed anymore
	/*public ItemStack copy(ItemStack item) {
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
	}*/

	public boolean isSimilar(Brew brew) {
		if (brew == null) return false;
		if (equals(brew)) return true;
		return quality == brew.quality &&
				distillRuns == brew.distillRuns &&
				Float.compare(brew.ageTime, ageTime) == 0 &&
				Float.compare(brew.wood, wood) == 0 &&
				unlabeled == brew.unlabeled &&
				persistent == brew.persistent &&
				immutable == brew.immutable &&
				ingredients.equals(brew.ingredients) &&
				(currentRecipe != null ? currentRecipe.equals(brew.currentRecipe) : brew.currentRecipe == null);
	}

	// Clones this instance
	@Override
	public Brew clone() throws CloneNotSupportedException {
		super.clone();
		Brew brew = new Brew(quality, currentRecipe, ingredients);
		brew.distillRuns = distillRuns;
		brew.ageTime = ageTime;
		brew.unlabeled = unlabeled;
		brew.persistent = persistent;
		brew.immutable = immutable;
		return brew;
	}

	@Override
	public String toString() {
		return "Brew{" +
				ingredients + " ingredients" +
				", quality=" + quality +
				", distillRuns=" + distillRuns +
				", ageTime=" + ageTime +
				", wood=" + wood +
				", currentRecipe=" + currentRecipe +
				", unlabeled=" + unlabeled +
				", immutable=" + immutable +
				'}';
	}

	// remove potion from file (drinking, despawning, combusting, cmdDeleting, should be more!)
	// Not needed anymore
	/*public void remove(ItemStack item) {
		if (!persistent) {
			potions.remove(getUID(item));
		}
	}*/

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
		if (immutable) return false;
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
		unlabeled = true;
		ItemMeta meta = item.getItemMeta();
		if (meta instanceof PotionMeta && meta.hasLore()) {
			BrewLore lore = new BrewLore(this, ((PotionMeta) meta));
			if (distillRuns > 0) {
				lore.updateDistillLore(false);
			}
			if (ageTime >= 1) {
				lore.updateAgeLore(false);
			}
			lore.write();
			item.setItemMeta(meta);
		}
	}

	// Do some regular updates
	public void touch() {
		lastUpdate = (int) ((double) (System.currentTimeMillis() - installTime) / 3600000D);
	}

	public byte getDistillRuns() {
		return distillRuns;
	}

	public float getAgeTime() {
		return ageTime;
	}

	public float getWood() {
		return wood;
	}

	public BIngredients getIngredients() {
		return ingredients;
	}

	public boolean hasRecipe() {
		return currentRecipe != null;
	}

	public BRecipe getCurrentRecipe() {
		return currentRecipe;
	}

	// Not needed anymore
	// TODO remove
	@Deprecated
	public boolean isPersistent() {
		return persistent;
	}

	// Make a potion persistent to not delete it when drinking it
	// Not needed anymore
	@Deprecated
	public void makePersistent() {
		persistent = true;
	}

	// Remove the Persistence Flag from a brew, so it will be normally deleted when drinking it
	// Not needed anymore
	@Deprecated
	public void removePersistence() {
		persistent = false;
	}

	public boolean isStatic() {
		return immutable;
	}

	public boolean isImmutable() {
		return immutable;
	}

	public boolean isUnlabeled() {
		return unlabeled;
	}

	// Set the Static flag, so potion is unchangeable
	public void setStatic(boolean immutable, ItemStack potion) {
		this.immutable = immutable;
		if (currentRecipe != null && canDistill()) {
			if (immutable) {
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
		if (immutable) return;

		distillRuns += 1;
		BrewLore lore = new BrewLore(this, potionMeta);
		BRecipe recipe = ingredients.getdistillRecipe(wood, ageTime);
		if (recipe != null) {
			// distillRuns will have an effect on the amount of alcohol, not the quality
			currentRecipe = recipe;
			quality = calcQuality();

			lore.addOrReplaceEffects(getEffects(), quality);
			potionMeta.setDisplayName(P.p.color("&f" + recipe.getName(quality)));
			PotionColor.fromString(recipe.getColor()).colorBrew(potionMeta, slotItem, canDistill());

		} else {
			quality = 0;
			lore.removeEffects();
			potionMeta.setDisplayName(P.p.color("&f" + P.p.languageReader.get("Brew_DistillUndefined")));
			PotionColor.GREY.colorBrew(potionMeta, slotItem, canDistill());
		}

		// Distill Lore
		if (currentRecipe != null) {
			if (BConfig.colorInBrewer != BrewLore.hasColorLore(potionMeta)) {
				lore.convertLore(BConfig.colorInBrewer);
			}
		}
		lore.updateDistillLore(BConfig.colorInBrewer);
		lore.write();
		touch();
		BrewModifyEvent modifyEvent = new BrewModifyEvent(this, potionMeta, BrewModifyEvent.Type.DISTILL);
		P.p.getServer().getPluginManager().callEvent(modifyEvent);
		if (modifyEvent.isCancelled()) {
			// As the brew and everything connected to it is only saved on the meta from now on,
			// not saving the brew into potionMeta is enough to not change anything in case of cancel
			return;
		}
		save(potionMeta);

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
		if (immutable) return;
		PotionMeta potionMeta = (PotionMeta) item.getItemMeta();

		BrewLore lore = new BrewLore(this, potionMeta);
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

				lore.addOrReplaceEffects(getEffects(), quality);
				potionMeta.setDisplayName(P.p.color("&f" + recipe.getName(quality)));
				PotionColor.fromString(recipe.getColor()).colorBrew(potionMeta, item, canDistill());
			} else {
				quality = 0;
				lore.removeEffects();
				potionMeta.setDisplayName(P.p.color("&f" + P.p.languageReader.get("Brew_BadPotion")));
				PotionColor.GREY.colorBrew(potionMeta, item, canDistill());
			}
		}

		// Lore
		if (currentRecipe != null) {
			if (BConfig.colorInBarrels != BrewLore.hasColorLore(potionMeta)) {
				lore.convertLore(BConfig.colorInBarrels);
			}
		}
		if (ageTime >= 1) {
			lore.updateAgeLore(BConfig.colorInBarrels);
		}
		if (ageTime > 0.5) {
			if (BConfig.colorInBarrels && !unlabeled && currentRecipe != null) {
				lore.updateWoodLore(true);
			}
		}
		lore.write();
		touch();
		BrewModifyEvent modifyEvent = new BrewModifyEvent(this, potionMeta, BrewModifyEvent.Type.AGE);
		P.p.getServer().getPluginManager().callEvent(modifyEvent);
		if (modifyEvent.isCancelled()) {
			// As the brew and everything connected to it is only saved on the meta from now on,
			// not saving the brew into potionMeta is enough to not change anything in case of cancel
			return;
		}
		save(potionMeta);
		item.setItemMeta(potionMeta);
	}

	// Slowly shift the wood of the Brew to the new Type
	public void woodShift(float time, byte to) {
		float factor = 1;
		if (ageTime > 5) {
			factor = 2;
		} else if (ageTime > 10) {
			factor = 2;
			factor += ageTime / 10F;
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

	/**
	 * Create a new Item of this Brew. A BrewModifyEvent type CREATE will be called.
	 *
	 * @param recipe Recipe is required if the brew doesn't have a currentRecipe
	 * @return The created Item, null if the Event is cancelled
	 */
	public ItemStack createItem(BRecipe recipe) {
		return createItem(recipe, true);
	}

	/**
	 * Create a new Item of this Brew.
	 *
	 * @param recipe Recipe is required if the brew doesn't have a currentRecipe
	 * @param event Set event to true if a BrewModifyEvent type CREATE should be called and may be cancelled. Only then may this method return null
	 * @return The created Item, null if the Event is cancelled
	 */
	public ItemStack createItem(BRecipe recipe, boolean event) {
		if (recipe == null) {
			recipe = getCurrentRecipe();
		}
		if (recipe == null) {
			throw new IllegalArgumentException("Recipe can't be null if the brew doesn't have a currentRecipe");
		}
		ItemStack potion = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		PotionColor.fromString(recipe.getColor()).colorBrew(potionMeta, potion, false);
		potionMeta.setDisplayName(P.p.color("&f" + recipe.getName(quality)));
		//if (!P.use1_14) {
		// Before 1.14 the effects duration would strangely be only a quarter of what we tell it to be
		// This is due to the Duration Modifier, that is removed in 1.14
		//	uid *= 4;
		//}
		// This effect stores the UID in its Duration
		//potionMeta.addCustomEffect((PotionEffectType.REGENERATION).createEffect((uid * 4), 0), true);

		BrewLore lore = new BrewLore(this, potionMeta);
		lore.convertLore(false);
		lore.addOrReplaceEffects(recipe.getEffects(), quality);
		lore.write();
		touch();
		if (event) {
			BrewModifyEvent modifyEvent = new BrewModifyEvent(this, potionMeta, BrewModifyEvent.Type.CREATE);
			P.p.getServer().getPluginManager().callEvent(modifyEvent);
			if (modifyEvent.isCancelled()) {
				return null;
			}
		}
		save(potionMeta);
		potion.setItemMeta(potionMeta);
		return potion;
	}

	private static Brew load(ItemMeta meta) {
		LoreLoadStream loreStream;
		try {
			loreStream = new LoreLoadStream(meta, 0);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
		XORUnscrambleStream unscrambler = new XORUnscrambleStream(new Base91DecoderStream(loreStream), saveSeed);
		try (DataInputStream in = new DataInputStream(unscrambler)) {
			boolean parityFailed = false;
			if (in.readByte() != 86) {
				P.p.errorLog("Parity check failed on Brew while loading, trying to load anyways!");
				parityFailed = true;
			}
			Brew brew = new Brew();
			byte ver = in.readByte();
			switch (ver) {
				case 1:
					unscrambler.start();
					brew.loadFromStream(in);
					break;
				default:
					if (parityFailed) {
						P.p.errorLog("Failed to load Brew. Maybe something corrupted the Lore of the Item?");
					} else {
						P.p.errorLog("Brew has data stored in v" + ver + " this Plugin version supports up to v1");
					}
					return null;
			}
			return brew;
		} catch (IOException e) {
			P.p.errorLog("IO Error while loading Brew");
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			P.p.errorLog("Failed to load Brew, has the data key 'BrewDataSeed' in the data.yml been changed?");
			e.printStackTrace();
		}
		return null;
	}

	private void loadFromStream(DataInputStream in) throws IOException {
		quality = in.readByte();
		int bools = in.readUnsignedByte();
		if ((bools & 1) != 0) {
			distillRuns = in.readByte();
		}
		if ((bools & 2) != 0) {
			ageTime = in.readFloat();
		}
		if ((bools & 4) != 0) {
			wood = in.readFloat();
		}
		if ((bools & 8) != 0) {
			setRecipeFromString(in.readUTF());
		} else {
			setRecipeFromString(null);
		}
		unlabeled = (bools & 16) != 0;
		//persistent = (bools & 32) != 0;
		immutable = (bools & 32) != 0;
		ingredients = BIngredients.load(in);
	}

	// Save brew data into meta/lore
	public void save(ItemMeta meta) {
		XORScrambleStream scrambler = new XORScrambleStream(new Base91EncoderStream(new LoreSaveStream(meta, 0)), saveSeed);
		try (DataOutputStream out = new DataOutputStream(scrambler)) {
			out.writeByte(86); // Parity/sanity
			out.writeByte(1); // Version
			scrambler.start();
			saveToStream(out);
		} catch (IOException e) {
			P.p.errorLog("IO Error while saving Brew");
			e.printStackTrace();
		}
	}

	// Save brew data into the meta/lore of the specified item
	// The meta on the item changes, so to make further changes to the meta, item.getItemMeta() has to be called again after this
	public void save(ItemStack item) {
		ItemMeta meta;
		if (!item.hasItemMeta()) {
			meta = P.p.getServer().getItemFactory().getItemMeta(item.getType());
		} else {
			meta = item.getItemMeta();
		}
		save(meta);
		item.setItemMeta(meta);
	}

	public void saveToStream(DataOutputStream out) throws IOException {
		if (quality > 10) {
			quality = 10;
		}
		out.writeByte((byte) quality);
		int bools = 0;
		bools |= ((distillRuns != 0) ? 1 : 0);
		bools |= (ageTime > 0 ? 2 : 0);
		bools |= (wood != -1 ? 4 : 0);
		bools |= (currentRecipe != null ? 8 : 0);
		bools |= (unlabeled ? 16 : 0);
		//bools |= (persistent ? 32 : 0);
		bools |= (immutable ? 32 : 0);
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
			out.writeUTF(currentRecipe.getRecipeName());
		}
		ingredients.save(out);
	}

	public static void writeSeed(ConfigurationSection section) {
		section.set("BrewDataSeed", saveSeed);
	}

	public static void loadSeed(ConfigurationSection section) {
		if (section.contains("BrewDataSeed")) {
			saveSeed = section.getLong("BrewDataSeed");
		} else {
			while (saveSeed == 0) {
				saveSeed = new SecureRandom().nextLong();
			}
		}
	}

	public static boolean noLegacy() {
		return legacyPotions.isEmpty();
	}

	// Load potion data from data file for backwards compatibility
	public static void loadLegacy(BIngredients ingredients, int uid, int quality, byte distillRuns, float ageTime, float wood, String recipe, boolean unlabeled, boolean persistent, boolean stat, int lastUpdate) {
		Brew brew = new Brew(ingredients, quality, distillRuns, ageTime, wood, recipe, unlabeled, stat, lastUpdate);
		brew.persistent = persistent;
		legacyPotions.put(uid, brew);
	}

	// remove legacy potiondata from item
	public static void removeLegacy(ItemStack item) {
		if (legacyPotions.isEmpty()) return;
		if (!item.hasItemMeta()) return;
		ItemMeta meta = item.getItemMeta();
		if (!(meta instanceof PotionMeta)) return;
		for (PotionEffect effect : ((PotionMeta) meta).getCustomEffects()) {
			if (effect.getType().equals(PotionEffectType.REGENERATION)) {
				if (effect.getDuration() < -1) {
					legacyPotions.remove(effect.getDuration());
					return;
				}
			}
		}
	}

	public void convertLegacy(ItemStack item) {
		removeLegacy(item);
		PotionMeta potionMeta = ((PotionMeta) item.getItemMeta());
		if (hasRecipe()) {
			BrewLore lore = new BrewLore(this, potionMeta);
			lore.removeEffects();
			PotionColor.fromString(currentRecipe.getColor()).colorBrew(potionMeta, item, canDistill());
		} else {
			PotionColor.GREY.colorBrew(potionMeta, item, canDistill());
		}
		save(potionMeta);
		item.setItemMeta(potionMeta);
	}

	// Saves all data
	// Legacy method to save to data file
	@Deprecated
	public static void save(ConfigurationSection config) {
		for (Map.Entry<Integer, Brew> entry : legacyPotions.entrySet()) {
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
				idConfig.set("recipe", brew.currentRecipe.getRecipeName());
			}
			if (brew.unlabeled) {
				idConfig.set("unlabeled", true);
			}
			if (brew.persistent) {
				idConfig.set("persist", true);
			}
			if (brew.immutable) {
				idConfig.set("stat", true);
			}
			if (brew.lastUpdate > 0) {
				idConfig.set("lastUpdate", brew.lastUpdate);
			}
			// save the ingredients
			idConfig.set("ingId", brew.ingredients.save(config.getParent()));
		}
	}

}
