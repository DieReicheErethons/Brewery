package com.dre.brewery.recipe;

import com.dre.brewery.P;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Simple Minecraft Item with just Material
 */
public class SimpleItem extends RecipeItem implements Ingredient {

	private Material mat;
	private short dur; // Old Mc


	public SimpleItem(Material mat) {
		this(mat, (short) 0);
	}

	public SimpleItem(Material mat, short dur) {
		this.mat = mat;
		this.dur = dur;
	}

	@Override
	public boolean hasMaterials() {
		return mat != null;
	}

	public Material getMaterial() {
		return mat;
	}

	@Override
	public List<Material> getMaterials() {
		List<Material> l = new ArrayList<>(1);
		l.add(mat);
		return l;
	}

	@NotNull
	@Override
	public Ingredient toIngredient(ItemStack forItem) {
		return ((SimpleItem) getMutableCopy());
	}

	@NotNull
	@Override
	public Ingredient toIngredientGeneric() {
		return ((SimpleItem) getMutableCopy());
	}

	@Override
	public boolean matches(ItemStack item) {
		if (!mat.equals(item.getType())) {
			return false;
		}
		//noinspection deprecation
		return P.use1_13 || dur == item.getDurability();
	}

	@Override
	public boolean matches(Ingredient ingredient) {
		if (isSimilar(ingredient)) {
			return true;
		}
		if (ingredient instanceof RecipeItem) {
			if (!((RecipeItem) ingredient).hasMaterials()) {
				return false;
			}
			if (ingredient instanceof CustomItem) {
				// Only match if the Custom Item also only defines material
				// If the custom item has more info like name and lore, it is not supposed to match a simple item
				CustomItem ci = (CustomItem) ingredient;
				return !ci.hasLore() && !ci.hasName() && mat == ci.getMaterial();
			}
		}
		return false;
	}

	@Override
	public boolean isSimilar(Ingredient item) {
		if (this == item) {
			return true;
		}
		if (item instanceof SimpleItem) {
			SimpleItem si = ((SimpleItem) item);
			return si.mat == mat && si.dur == dur;
		}
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		SimpleItem item = (SimpleItem) o;
		return dur == item.dur &&
			mat == item.mat;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), mat, dur);
	}

	@Override
	public String toString() {
		return "SimpleItem{" +
			"mat=" + mat.name().toLowerCase() +
			" amount=" + getAmount() +
			'}';
	}

	@Override
	public void saveTo(DataOutputStream out) throws IOException {
		out.writeUTF("SI");
		out.writeUTF(mat.name());
		out.writeShort(dur);
	}

	public static SimpleItem loadFrom(ItemLoader loader) {
		try {
			DataInputStream in = loader.getInputStream();
			Material mat = Material.getMaterial(in.readUTF());
			short dur = in.readShort();
			if (mat != null) {
				SimpleItem item = new SimpleItem(mat, dur);
				return item;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	// Needs to be called at Server start
	public static void registerItemLoader(P p) {
		p.registerForItemLoader("SI", SimpleItem::loadFrom);
	}

	@Override
	public String displayName() {
		// approximate a user-friendly display name since
		// server doesn't have access to translations :/
		return mat.toString().toLowerCase().replace("_", " ");
	}

}

