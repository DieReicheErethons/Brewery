package com.dre.brewery.filedata;

import com.dre.brewery.*;
import com.dre.brewery.utility.BUtil;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BData {


	// load all Data
	public static void readData() {
		File file = new File(P.p.getDataFolder(), "data.yml");
		if (file.exists()) {

			FileConfiguration data = YamlConfiguration.loadConfiguration(file);

			Brew.installTime = data.getLong("installTime", System.currentTimeMillis());
			MCBarrel.mcBarrelTime = data.getLong("MCBarrelTime", 0);

			Brew.loadPrevSeeds(data);

			// Check if data is the newest version
			String version = data.getString("Version", null);
			if (version != null) {
				if (!version.equals(DataSave.dataVersion)) {
					P.p.log("Data File is being updated...");
					new DataUpdater(data, file).update(version);
					data = YamlConfiguration.loadConfiguration(file);
					P.p.log("Data Updated to version: " + DataSave.dataVersion);
				}
			}

			// loading Ingredients into ingMap
			Map<String, BIngredients> ingMap = new HashMap<>();
			ConfigurationSection section = data.getConfigurationSection("Ingredients");
			if (section != null) {
				for (String id : section.getKeys(false)) {
					ConfigurationSection matSection = section.getConfigurationSection(id + ".mats");
					if (matSection != null) {
						// matSection has all the materials + amount as Integers
						ArrayList<ItemStack> ingredients = deserializeIngredients(matSection);
						ingMap.put(id, new BIngredients(ingredients, section.getInt(id + ".cookedTime", 0), true));
					} else {
						P.p.errorLog("Ingredient id: '" + id + "' incomplete in data.yml");
					}
				}
			}

			// loading Brew legacy
			section = data.getConfigurationSection("Brew");
			if (section != null) {
				// All sections have the UID as name
				for (String uid : section.getKeys(false)) {
					BIngredients ingredients = getIngredients(ingMap, section.getString(uid + ".ingId"));
					int quality = section.getInt(uid + ".quality", 0);
					byte distillRuns = (byte) section.getInt(uid + ".distillRuns", 0);
					float ageTime = (float) section.getDouble(uid + ".ageTime", 0.0);
					float wood = (float) section.getDouble(uid + ".wood", -1.0);
					String recipe = section.getString(uid + ".recipe", null);
					boolean unlabeled = section.getBoolean(uid + ".unlabeled", false);
					boolean persistent = section.getBoolean(uid + ".persist", false);
					boolean stat = section.getBoolean(uid + ".stat", false);
					int lastUpdate = section.getInt("lastUpdate", 0);

					Brew.loadLegacy(ingredients, P.p.parseInt(uid), quality, distillRuns, ageTime, wood, recipe, unlabeled, persistent, stat, lastUpdate);
				}
			}

			// loading BPlayer
			section = data.getConfigurationSection("Player");
			if (section != null) {
				// keys have players name
				for (String name : section.getKeys(false)) {
					try {
						//noinspection ResultOfMethodCallIgnored
						UUID.fromString(name);
						if (!P.useUUID) {
							continue;
						}
					} catch (IllegalArgumentException e) {
						if (P.useUUID) {
							continue;
						}
					}

					int quality = section.getInt(name + ".quality");
					int drunk = section.getInt(name + ".drunk");
					int offDrunk = section.getInt(name + ".offDrunk", 0);

					new BPlayer(name, quality, drunk, offDrunk);
				}
			}

			for (World world : P.p.getServer().getWorlds()) {
				if (world.getName().startsWith("DXL_")) {
					loadWorldData(BUtil.getDxlName(world.getName()), world);
				} else {
					loadWorldData(world.getUID().toString(), world);
				}
			}

		} else {
			P.p.errorLog("No data.yml found, will create new one!");
		}
	}

	public static ArrayList<ItemStack> deserializeIngredients(ConfigurationSection matSection) {
		ArrayList<ItemStack> ingredients = new ArrayList<>();
		for (String mat : matSection.getKeys(false)) {
			String[] matSplit = mat.split(",");
			Material m = Material.getMaterial(matSplit[0]);
			if (m == null && P.use1_13) {
				if (matSplit[0].equals("LONG_GRASS")) {
					m = Material.GRASS;
				} else {
					m = Material.matchMaterial(matSplit[0], true);
				}
				P.p.debugLog("converting Data Material from " + matSplit[0] + " to " + m);
			}
			if (m == null) continue;
			ItemStack item = new ItemStack(m, matSection.getInt(mat));
			if (matSplit.length == 2) {
				item.setDurability((short) P.p.parseInt(matSplit[1]));
			}
			ingredients.add(item);
		}
		return ingredients;
	}

	// returns Ingredients by id from the specified ingMap
	public static BIngredients getIngredients(Map<String, BIngredients> ingMap, String id) {
		if (!ingMap.isEmpty()) {
			if (ingMap.containsKey(id)) {
				return ingMap.get(id);
			}
		}
		P.p.errorLog("Ingredient id: '" + id + "' not found in data.yml");
		return new BIngredients();
	}

	// loads BIngredients from an ingredient section
	public static BIngredients loadIngredients(ConfigurationSection section) {
		if (section != null) {
			return new BIngredients(deserializeIngredients(section), 0);
		} else {
			P.p.errorLog("Cauldron is missing Ingredient Section");
		}
		return new BIngredients();
	}

	// load Block locations of given world
	public static void loadWorldData(String uuid, World world) {

		File file = new File(P.p.getDataFolder(), "data.yml");
		if (file.exists()) {

			FileConfiguration data = YamlConfiguration.loadConfiguration(file);

			// loading BCauldron
			if (data.contains("BCauldron." + uuid)) {
				ConfigurationSection section = data.getConfigurationSection("BCauldron." + uuid);
				for (String cauldron : section.getKeys(false)) {
					// block is splitted into x/y/z
					String block = section.getString(cauldron + ".block");
					if (block != null) {
						String[] splitted = block.split("/");
						if (splitted.length == 3) {

							Block worldBlock = world.getBlockAt(P.p.parseInt(splitted[0]), P.p.parseInt(splitted[1]), P.p.parseInt(splitted[2]));
							BIngredients ingredients = loadIngredients(section.getConfigurationSection(cauldron + ".ingredients"));
							int state = section.getInt(cauldron + ".state", 1);

							new BCauldron(worldBlock, ingredients, state);
						} else {
							P.p.errorLog("Incomplete Block-Data in data.yml: " + section.getCurrentPath() + "." + cauldron);
						}
					} else {
						P.p.errorLog("Missing Block-Data in data.yml: " + section.getCurrentPath() + "." + cauldron);
					}
				}
			}

			// loading Barrel
			if (data.contains("Barrel." + uuid)) {
				ConfigurationSection section = data.getConfigurationSection("Barrel." + uuid);
				for (String barrel : section.getKeys(false)) {
					// block spigot is splitted into x/y/z
					String spigot = section.getString(barrel + ".spigot");
					if (spigot != null) {
						String[] splitted = spigot.split("/");
						if (splitted.length == 3) {

							// load itemStacks from invSection
							ConfigurationSection invSection = section.getConfigurationSection(barrel + ".inv");
							Block block = world.getBlockAt(P.p.parseInt(splitted[0]), P.p.parseInt(splitted[1]), P.p.parseInt(splitted[2]));
							float time = (float) section.getDouble(barrel + ".time", 0.0);
							byte sign = (byte) section.getInt(barrel + ".sign", 0);
							String[] st = section.getString(barrel + ".st", "").split(",");
							String[] wo = section.getString(barrel + ".wo", "").split(",");

							Barrel b;
							if (invSection != null) {
								b = new Barrel(block, sign, st, wo, invSection.getValues(true), time);
							} else {
								// Barrel has no inventory
								b = new Barrel(block, sign, st, wo, null, time);
							}

							// In case Barrel Block locations were missing and could not be recreated: do not add the barrel
							if (b.getBody().getStairsloc() != null || b.getBody().getWoodsloc() != null) {
								Barrel.barrels.add(b);
							}

						} else {
							P.p.errorLog("Incomplete Block-Data in data.yml: " + section.getCurrentPath() + "." + barrel);
						}
					} else {
						P.p.errorLog("Missing Block-Data in data.yml: " + section.getCurrentPath() + "." + barrel);
					}
				}
			}

			// loading Wakeup
			if (data.contains("Wakeup." + uuid)) {
				ConfigurationSection section = data.getConfigurationSection("Wakeup." + uuid);
				for (String wakeup : section.getKeys(false)) {
					// loc of wakeup is splitted into x/y/z/pitch/yaw
					String loc = section.getString(wakeup);
					if (loc != null) {
						String[] splitted = loc.split("/");
						if (splitted.length == 5) {

							double x = NumberUtils.toDouble(splitted[0]);
							double y = NumberUtils.toDouble(splitted[1]);
							double z = NumberUtils.toDouble(splitted[2]);
							float pitch = NumberUtils.toFloat(splitted[3]);
							float yaw = NumberUtils.toFloat(splitted[4]);
							Location location = new Location(world, x, y, z, yaw, pitch);

							Wakeup.wakeups.add(new Wakeup(location));

						} else {
							P.p.errorLog("Incomplete Location-Data in data.yml: " + section.getCurrentPath() + "." + wakeup);
						}
					}
				}
			}

		}
	}
}
