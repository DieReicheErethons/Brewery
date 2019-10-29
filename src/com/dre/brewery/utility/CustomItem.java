package com.dre.brewery.utility;

import com.dre.brewery.P;
import com.dre.brewery.filedata.BConfig;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CustomItem {
	private String id;
	private boolean simple; // Simple Custom Item is just materials.get(0) and durability for old mc
	private boolean matchAny; // If only one of the values needs to match
	private short dur; // Old Mc
	private List<Material> materials;
	private List<String> names;
	private List<String> lore;

	public static CustomItem asSimpleItem(Material mat) {
		return asSimpleItem(mat, (short) 0);
	}

	public static CustomItem asSimpleItem(Material mat, short dur) {
		CustomItem it = new CustomItem();
		it.simple = true;
		it.dur = dur;
		it.materials = new ArrayList<>(1);
		it.materials.add(mat);
		return it;
	}

	@Nullable
	public static CustomItem fromConfig(ConfigurationSection cfg, String id) {
		CustomItem custom = new CustomItem();

		custom.id = id;
		custom.matchAny = cfg.getBoolean(id + ".matchAny", false);

		List<String> load = null;
		String path = id + ".material";
		if (cfg.isString(path)) {
			load = new ArrayList<>(1);
			load.add(cfg.getString(path));
		} else if (cfg.isList(path)) {
			load = cfg.getStringList(path);
		}
		if (load != null && !load.isEmpty()) {
			custom.materials = new ArrayList<>(load.size());
			if (!custom.loadMaterials(load)) {
				return null;
			}
		} else {
			custom.materials = new ArrayList<>(0);
		}

		load = null;
		path = id + ".name";
		if (cfg.isString(path)) {
			load = new ArrayList<>(1);
			load.add(cfg.getString(path));
		} else if (cfg.isList(path)) {
			load = cfg.getStringList(path);
		}
		if (load != null && !load.isEmpty()) {
			custom.names = load.stream().map(l -> P.p.color(l)).collect(Collectors.toList());
			if (P.use1_13) {
				// In 1.13 trailing Color white is removed from display names
				custom.names = custom.names.stream().map(l -> l.startsWith("§f") ? l.substring(2) : l).collect(Collectors.toList());
			}
		} else {
			custom.names = new ArrayList<>(0);
		}

		load = null;
		path = id + ".lore";
		if (cfg.isString(path)) {
			load = new ArrayList<>(1);
			load.add(cfg.getString(path));
		} else if (cfg.isList(path)) {
			load = cfg.getStringList(path);
		}
		if (load != null && !load.isEmpty()) {
			custom.lore = load.stream().map(l -> P.p.color(l)).collect(Collectors.toList());
		} else {
			custom.lore = new ArrayList<>(0);
		}

		if (custom.materials.isEmpty() && custom.names.isEmpty() && custom.lore.isEmpty()) {
			P.p.errorLog("No Config Entries found for Custom Item");
			return null;
		}

		return custom;
	}

	private boolean loadMaterials(List<String> ingredientsList) {
		for (String item : ingredientsList) {
			String[] ingredParts = item.split("/");
			if (ingredParts.length == 2) {
				P.p.errorLog("Item Amount can not be specified for Custom Items: " + item);
				return false;
			}
			Material mat = Material.matchMaterial(ingredParts[0]);

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
				return false;
			}
		}
		return true;
	}

	public String getId() {
		return id;
	}

	public boolean isSimple() {
		return simple;
	}

	public boolean isMatchAny() {
		return matchAny;
	}

	public List<Material> getMaterials() {
		return materials;
	}

	public Material getSimpleMaterial() {
		return materials.get(0);
	}

	public List<String> getNames() {
		return names;
	}

	public List<String> getLore() {
		return lore;
	}

	public boolean matches(ItemStack usedItem) {
		if (simple) {
			return matchSimple(usedItem);
		} else if (matchAny){
			return matchAny(usedItem);
		} else {
			return matchOne(usedItem);
		}
	}

	private boolean matchSimple(ItemStack usedItem) {
		if (!materials.get(0).equals(usedItem.getType())) {
			return false;
		}
		//noinspection deprecation
		return P.use1_13 || dur == usedItem.getDurability();
	}

	private boolean matchAny(ItemStack usedItem) {
		Material usedMat = usedItem.getType();
		for (Material mat : materials) {
			if (usedMat == mat) {
				return true;
			}
		}
		if (!usedItem.hasItemMeta()) {
			return false;
		}
		ItemMeta meta = usedItem.getItemMeta();
		assert meta != null;
		if (meta.hasDisplayName()) {
			String usedName = meta.getDisplayName();
			for (String name : names) {
				if (name.equalsIgnoreCase(usedName)) {
					return true;
				}
			}
		}

		if (meta.hasLore()) {
			List<String> usedLore = meta.getLore();
			assert usedLore != null;
			for (String line : this.lore) {
				for (String usedLine : usedLore) {
					if (line.equalsIgnoreCase(usedLine) || line.equalsIgnoreCase(ChatColor.stripColor(usedLine))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean matchOne(ItemStack item) {
		if (!materials.isEmpty()) {
			if (item.getType() != materials.get(0)) {
				return false;
			}
		}
		if (names.isEmpty() && lore.isEmpty()) {
			return true;
		}
		if (!item.hasItemMeta()) {
			return false;
		}
		ItemMeta meta = item.getItemMeta();
		assert meta != null;
		if (!names.isEmpty()) {
			if (!meta.hasDisplayName() || !names.get(0).equalsIgnoreCase(meta.getDisplayName())) {
				return false;
			}
		}


		if (!lore.isEmpty()) {
			if (!meta.hasLore()) {
				return false;
			}

			int lastIndex = 0;
			List<String> usedLore = meta.getLore();
			assert usedLore != null;
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
		}
		return true;
	}

	@NotNull
	public ItemStack createDummy(int amount) {
		if (simple) {
			if (P.use1_13) {
				return new ItemStack(getSimpleMaterial(), amount);
			} else {
				//noinspection deprecation
				return new ItemStack(getSimpleMaterial(), amount, dur);
			}
		} else if (matchAny) {
			if (!materials.isEmpty()) {
				return new ItemStack(materials.get(0), amount);
			} else if (!names.isEmpty()) {
				ItemStack item = new ItemStack(Material.DIAMOND_HOE, amount);
				ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : P.p.getServer().getItemFactory().getItemMeta(Material.DIAMOND_HOE);
				assert meta != null;
				meta.setDisplayName(names.get(0));
				item.setItemMeta(meta);
				return item;
			} else if (!lore.isEmpty()) {
				ItemStack item = new ItemStack(Material.DIAMOND_HOE, amount);
				ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : P.p.getServer().getItemFactory().getItemMeta(Material.DIAMOND_HOE);
				assert meta != null;
				List<String> l = new ArrayList<>();
				l.add(lore.get(0));
				meta.setLore(l);
				item.setItemMeta(meta);
				return item;
			}
			return new ItemStack(Material.DIAMOND_HOE, amount);
		} else {
			ItemStack item;
			ItemMeta meta;
			if (!materials.isEmpty()) {
				item = new ItemStack(materials.get(0), amount);
				meta = item.hasItemMeta() ? item.getItemMeta() : P.p.getServer().getItemFactory().getItemMeta(materials.get(0));
			} else {
				item = new ItemStack(Material.DIAMOND_HOE, amount);
				meta = item.hasItemMeta() ? item.getItemMeta() : P.p.getServer().getItemFactory().getItemMeta(Material.DIAMOND_HOE);
			}
			assert meta != null;
			if (!names.isEmpty()) {
				meta.setDisplayName(names.get(0));
				item.setItemMeta(meta);
			}
			if (!lore.isEmpty()) {
				meta.setLore(lore);
				item.setItemMeta(meta);
			}
			return item;
		}
	}

	@Override
	public String toString() {
		if (simple) {
			return "CustomItem{Simple: " + getSimpleMaterial().name().toLowerCase() + "}";
		}
		if (materials == null || names == null || lore == null) {
			return "CustomItem{" + id + "}";
		}
		return "CustomItem{" + id + ": " + (matchAny ? "MatchAny, " : "MatchOne, ") + materials.size() + " Materials, " + names.size() + " Names, " + lore.size() + " Lore}";
	}
}
