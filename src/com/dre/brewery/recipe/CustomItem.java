package com.dre.brewery.recipe;

import com.dre.brewery.P;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Minecraft Item with custon name and lore.
 * <p>Mostly used for Custom Items of the Config, but also for general custom items
 */
public class CustomItem extends RecipeItem implements Ingredient {

	private Material mat;
	private String name;
	private List<String> lore;

	public CustomItem() {
	}

	public CustomItem(Material mat) {
		this.mat = mat;
	}

	public CustomItem(Material mat, String name, List<String> lore) {
		this.mat = mat;
		this.name = name;
		this.lore = lore;
	}

	public CustomItem(ItemStack item) {
		mat = item.getType();
		if (!item.hasItemMeta()) {
			return;
		}
		ItemMeta itemMeta = item.getItemMeta();
		assert itemMeta != null;
		if (itemMeta.hasDisplayName()) {
			name = itemMeta.getDisplayName();
		}
		if (itemMeta.hasLore()) {
			lore = itemMeta.getLore();
		}
	}

	@Override
	public boolean hasMaterials() {
		return mat != null;
	}

	public boolean hasName() {
		return name != null;
	}

	public boolean hasLore() {
		return lore != null && !lore.isEmpty();
	}

	@Override
	public List<Material> getMaterials() {
		List<Material> l = new ArrayList<>(1);
		l.add(mat);
		return l;
	}

	@Nullable
	public Material getMaterial() {
		return mat;
	}

	protected void setMat(Material mat) {
		this.mat = mat;
	}

	@Nullable
	public String getName() {
		return name;
	}

	protected void setName(String name) {
		this.name = name;
	}

	@Nullable
	public List<String> getLore() {
		return lore;
	}

	protected void setLore(List<String> lore) {
		this.lore = lore;
	}

	@NotNull
	@Override
	public Ingredient toIngredient(ItemStack forItem) {
		return ((CustomItem) getMutableCopy());
	}

	@NotNull
	@Override
	public Ingredient toIngredientGeneric() {
		return ((CustomItem) getMutableCopy());
	}

	@Override
	public boolean matches(Ingredient ingredient) {
		if (isSimilar(ingredient)) {
			return true;
		}
		if (ingredient instanceof RecipeItem) {
			RecipeItem rItem = ((RecipeItem) ingredient);
			if (rItem instanceof SimpleItem) {
				// If the recipe item is just a simple item, only match if we also only define material
				// If this is a custom item with more info, we don't want to match a simple item
				return hasMaterials() && !hasLore() && !hasName() && getMaterial() == ((SimpleItem) rItem).getMaterial();
			} else if (rItem instanceof CustomItem) {
				// If the other is a CustomItem as well and not Similar to ours, it might have more data and we still match
				CustomItem other = ((CustomItem) rItem);
				if (mat == null || mat == other.mat) {
					if (!hasName() || (other.name != null && name.equalsIgnoreCase(other.name))) {
						return !hasLore() || lore == other.lore || (other.hasLore() && matchLore(other.lore));
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean matches(ItemStack item) {
		if (mat != null) {
			if (item.getType() != mat) {
				return false;
			}
		}
		if (name == null && !hasLore()) {
			return true;
		}
		if (!item.hasItemMeta()) {
			return false;
		}
		ItemMeta meta = item.getItemMeta();
		assert meta != null;
		if (name != null) {
			if (!meta.hasDisplayName() || !name.equalsIgnoreCase(meta.getDisplayName())) {
				return false;
			}
		}

		if (hasLore()) {
			if (!meta.hasLore()) {
				return false;
			}
			return matchLore(meta.getLore());
		}
		return true;
	}

	/**
	 * If this item has lore that matches the given lore.
	 * <p>It matches if our lore is contained in the given lore consecutively, ignoring color of the given lore.
	 *
	 * @param usedLore The given lore to match
	 * @return True if the given lore contains our lore consecutively
	 */
	public boolean matchLore(List<String> usedLore) {
		if (lore == null) return true;
		int lastIndex = 0;
		boolean foundFirst = false;
		for (String line : lore) {
			do {
				if (lastIndex == usedLore.size()) {
					// There is more in lore than in usedLore, bad
					return false;
				}
				String usedLine = usedLore.get(lastIndex);
				if (line.equalsIgnoreCase(usedLine) || line.equalsIgnoreCase(ChatColor.stripColor(usedLine))) {
					// If the line is correct, we have found our first and we want all consecutive lines to also equal
					foundFirst = true;
				} else if (foundFirst) {
					// If a consecutive line is not equal, thats bad
					return false;
				}
				lastIndex++;
				// If we once found one correct line, iterate over 'lore' consecutively
			} while (!foundFirst);
		}
		return true;
	}

	// We don't compare id here
	@Override
	public boolean isSimilar(Ingredient item) {
		if (this == item) {
			return true;
		}
		if (item instanceof CustomItem) {
			CustomItem ci = ((CustomItem) item);
			return mat == ci.mat && Objects.equals(name, ci.name) && Objects.equals(lore, ci.lore);
		}
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj)) return false;
		if (obj instanceof CustomItem) {
			return isSimilar(((CustomItem) obj));
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), mat, name, lore);
	}

	@Override
	public String toString() {
		return "CustomItem{" +
			"id=" + getConfigId() +
			", mat=" + (mat != null ? mat.name().toLowerCase() : "null") +
			", name='" + name + '\'' +
			", loresize: " + (lore != null ? lore.size() : 0) +
			'}';
	}

	@Override
	public void saveTo(DataOutputStream out) throws IOException {
		out.writeUTF("CI");
		if (mat != null) {
			out.writeBoolean(true);
			out.writeUTF(mat.name());
		} else {
			out.writeBoolean(false);
		}
		if (name != null) {
			out.writeBoolean(true);
			out.writeUTF(name);
		} else {
			out.writeBoolean(false);
		}
		if (lore != null) {
			short size = (short) Math.min(lore.size(), Short.MAX_VALUE);
			out.writeShort(size);
			for (int i = 0; i < size; i++) {
				out.writeUTF(lore.get(i));
			}
		} else {
			out.writeShort(0);
		}
	}

	public static CustomItem loadFrom(ItemLoader loader) {
		try {
			DataInputStream in = loader.getInputStream();
			CustomItem item = new CustomItem();
			if (in.readBoolean()) {
				item.mat = Material.getMaterial(in.readUTF());
			}
			if (in.readBoolean()) {
				item.name = in.readUTF();
			}
			short size = in.readShort();
			if (size > 0) {
				item.lore = new ArrayList<>(size);
				for (short i = 0; i < size; i++) {
					item.lore.add(in.readUTF());
				}
			}
			return item;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	// Needs to be called at Server start
	public static void registerItemLoader(P p) {
		p.registerForItemLoader("CI", CustomItem::loadFrom);
	}

	@Override
	public String displayName() {
		return this.name;
	}
}
