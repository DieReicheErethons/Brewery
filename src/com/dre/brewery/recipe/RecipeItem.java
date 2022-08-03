package com.dre.brewery.recipe;

import com.dre.brewery.P;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.utility.BUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Item that can be used in a Recipe.
 * <p>They are not necessarily only loaded from config
 * <p>They are immutable if used in a recipe. If one implements Ingredient,
 * it can be used as mutable copy directly in a
 * BIngredients. Otherwise it needs to be converted to an Ingredient
 */
public abstract class RecipeItem implements Cloneable {

	private String cfgId;
	private int amount;
	private boolean immutable = false;


	/**
	 * Does this RecipeItem match the given ItemStack?
	 * <p>Used to determine if the given item corresponds to this recipeitem
	 *
	 * @param item The ItemStack for comparison
	 * @return True if the given item matches this recipeItem
	 */
	public abstract boolean matches(ItemStack item);

	/**
	 * Does this Item match the given Ingredient?
	 * <p>A RecipeItem matches an Ingredient if all required info of the RecipeItem are fulfilled on the Ingredient
	 * <br>This does not imply that the same holds the other way round, as the ingredient item might have more info than needed
	 *
	 *
	 * @param ingredient The ingredient that needs to fulfill the requirements
	 * @return True if the ingredient matches the required info of this
	 */
	public abstract boolean matches(Ingredient ingredient);

	/**
	 * Get the Corresponding Ingredient Item. For Items implementing Ingredient, just getMutableCopy()
	 * <p>This is called when this recipe item is added to a BIngredients
	 *
	 * @param forItem The ItemStack that has previously matched this RecipeItem. Used if the resulting Ingredient needs more info from the ItemStack
	 * @return The IngredientItem corresponding to this RecipeItem
	 */
	@NotNull
	public abstract Ingredient toIngredient(ItemStack forItem);

	/**
	 * Gets a Generic Ingredient for this recipe item
	 */
	@NotNull
	public abstract Ingredient toIngredientGeneric();

	/**
	 * @return True if this recipeItem has one or more materials that could classify an item. if true, getMaterials() is NotNull
	 */
	public abstract boolean hasMaterials();

	/**
	 * @return List of one or more Materials this recipeItem uses.
	 */
	@Nullable
	public abstract List<Material> getMaterials();

	/**
	 * @return A user-displayable name for this recipeItem
	 */
	public abstract String displayName();

	/**
	 * @return The Id this Item uses in the config in the custom-items section
	 */
	@Nullable
	public String getConfigId() {
		return cfgId;
	}

	/**
	 * @return The Amount of this Item in a Recipe
	 */
	public int getAmount() {
		return amount;
	}

	/**
	 * Set the Amount of this Item in a Recipe.
	 * <p>The amount can not be set on an existing item in a recipe or existing custom item.
	 * <br>To change amount you need to use getMutableCopy() and change the amount on the copy
	 *
	 * @param amount The new amount
	 */
	public void setAmount(int amount) {
		if (immutable) throw new IllegalStateException("Setting amount only possible on mutable copy");
		this.amount = amount;
	}

	/**
	 * Makes this Item immutable, for example when loaded from config. Used so if this is added to BIngredients,
	 * it needs to be cloned before changing anything like amount
	 */
	public void makeImmutable() {
		immutable = true;
	}

	/**
	 * Gets a shallow clone of this RecipeItem whose fields like amount can be changed.
	 *
	 * @return A mutable copy of this
	 */
	public RecipeItem getMutableCopy() {
		try {
			RecipeItem i = (RecipeItem) super.clone();
			i.immutable = false;
			return i;
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e);
		}
	}

	/**
	 * Tries to find a matching RecipeItem for this item. It checks custom items and if it has found a unique custom item
	 * it will return that. If there are multiple matching custom items, a new CustomItem with all item info is returned.
	 * <br>If there is no matching CustomItem, it will return a SimpleItem with the items type
	 *
	 * @param item The Item for which to find a matching RecipeItem
	 * @param acceptAll If true it will accept any item and return a SimpleItem even if not on the accepted list
	 *                  <br>If false it will return null if the item is not acceptable by the Cauldron
	 * @return The Matched CustomItem, new CustomItem with all item info or SimpleItem
	 */
	@Nullable
	@Contract("_, true -> !null")
	public static RecipeItem getMatchingRecipeItem(ItemStack item, boolean acceptAll) {
		RecipeItem rItem = null;
		boolean multiMatch = false;
		for (RecipeItem ri : BCauldronRecipe.acceptedCustom) {
			// If we already have a multi match, only check if there is a PluginItem that matches more strictly
			if (!multiMatch || (ri instanceof PluginItem)) {
				if (ri.matches(item)) {
					// If we match a plugin item, thats a very strict match, so immediately return it
					if (ri instanceof PluginItem) {
						return ri;
					}
					if (rItem == null) {
						rItem = ri;
					} else {
						multiMatch = true;
					}
				}
			}
		}
		if (multiMatch) {
			// We have multiple Custom Items matching, so just store all item info
			return new CustomItem(item);
		}
		if (rItem == null && (acceptAll || BCauldronRecipe.acceptedSimple.contains(item.getType()))) {
			// No Custom item found
			if (P.use1_13) {
				return new SimpleItem(item.getType());
			} else {
				@SuppressWarnings("deprecation")
				short durability = item.getDurability();
				return new SimpleItem(item.getType(), durability);
			}
		}
		return rItem;
	}

	@Nullable
	public static RecipeItem fromConfigCustom(ConfigurationSection cfg, String id) {
		RecipeItem rItem;
		if (cfg.getBoolean(id + ".matchAny", false)) {
			rItem = new CustomMatchAnyItem();
		} else {
			rItem = new CustomItem();
		}

		rItem.cfgId = id;
		rItem.immutable = true;

		List<Material> materials;
		List<String> names;
		List<String> lore;

		List<String> load = BUtil.loadCfgStringList(cfg, id + ".material");
		if (load != null && !load.isEmpty()) {
			if ((materials = loadMaterials(load)) == null) {
				return null;
			}
		} else {
			materials = new ArrayList<>(0);
		}

		load = BUtil.loadCfgStringList(cfg, id + ".name");
		if (load != null && !load.isEmpty()) {
			names = load.stream().map(l -> P.p.color(l)).collect(Collectors.toList());
			if (P.use1_13) {
				// In 1.13 trailing Color white is removed from display names
				names = names.stream().map(l -> l.startsWith("§f") ? l.substring(2) : l).collect(Collectors.toList());
			}
		} else {
			names = new ArrayList<>(0);
		}

		load = BUtil.loadCfgStringList(cfg, id + ".lore");
		if (load != null && !load.isEmpty()) {
			lore = load.stream().map(l -> P.p.color(l)).collect(Collectors.toList());
		} else {
			lore = new ArrayList<>(0);
		}

		if (materials.isEmpty() && names.isEmpty() && lore.isEmpty()) {
			P.p.errorLog("No Config Entries found for Custom Item");
			return null;
		}

		if (rItem instanceof CustomItem) {
			CustomItem cItem = ((CustomItem) rItem);
			if (!materials.isEmpty()) {
				cItem.setMat(materials.get(0));
			}
			if (!names.isEmpty()) {
				cItem.setName(names.get(0));
			}
			cItem.setLore(lore);
		} else {
			CustomMatchAnyItem maItem = (CustomMatchAnyItem) rItem;
			maItem.setMaterials(materials);
			maItem.setNames(names);
			maItem.setLore(lore);
		}

		return rItem;
	}

	@Nullable
	protected static List<Material> loadMaterials(List<String> ingredientsList) {
		List<Material> materials = new ArrayList<>(ingredientsList.size());
		for (String item : ingredientsList) {
			String[] ingredParts = item.split("/");
			if (ingredParts.length == 2) {
				P.p.errorLog("Item Amount can not be specified for Custom Items: " + item);
				return null;
			}
			Material mat = Material.matchMaterial(ingredParts[0]);

			if (mat == null && !P.use1_14 && ingredParts[0].equalsIgnoreCase("cornflower")) {
				// Using this in default custom-items, but will error on < 1.14
				materials.add(Material.BEDROCK);
				continue;
			}

			if (mat == null && BConfig.hasVault) {
				try {
					net.milkbowl.vault.item.ItemInfo vaultItem = net.milkbowl.vault.item.Items.itemByString(ingredParts[0]);
					if (vaultItem != null) {
						mat = vaultItem.getType();
					}
				} catch (Exception e) {
					P.p.errorLog("Could not check vault for Item Name");
					e.printStackTrace();
				}
			}
			if (mat != null) {
				materials.add(mat);
			} else {
				P.p.errorLog("Unknown Material: " + ingredParts[0]);
				return null;
			}
		}
		return materials;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof RecipeItem)) return false;
		RecipeItem that = (RecipeItem) o;
		return amount == that.amount &&
			immutable == that.immutable &&
			Objects.equals(cfgId, that.cfgId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(cfgId, amount, immutable);
	}

	@Override
	public String toString() {
		return "RecipeItem{(" + getClass().getSimpleName() + ") ID: " + getConfigId() + " Materials: " + (hasMaterials() ? getMaterials().size() : 0) + " Amount: " + getAmount();
	}
}
