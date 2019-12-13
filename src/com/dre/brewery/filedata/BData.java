package com.dre.brewery.filedata;

import com.dre.brewery.*;
import com.dre.brewery.lore.Base91DecoderStream;
import com.dre.brewery.recipe.CustomItem;
import com.dre.brewery.recipe.Ingredient;
import com.dre.brewery.recipe.PluginItem;
import com.dre.brewery.recipe.SimpleItem;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.BoundingBox;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class BData {


	// load all Data
	public static void readData() {
		File file = new File(P.p.getDataFolder(), "data.yml");
		if (file.exists()) {

			long t1 = System.currentTimeMillis();

			FileConfiguration data = YamlConfiguration.loadConfiguration(file);

			long t2 = System.currentTimeMillis();

			if (t2 - t1 > 8000) {
				// Spigot is very slow at loading inventories from yml. Notify Admin that loading Data took long
				P.p.log("Bukkit took " + (t2 - t1) / 1000.0 + "s to load the Data File,");
				P.p.log("consider switching to Paper, or have less items in Barrels");
			} else {
				P.p.debugLog("Loading data.yml: " + (t2 - t1) + "ms");
			}

			Brew.installTime = data.getLong("installTime", System.currentTimeMillis());
			MCBarrel.mcBarrelTime = data.getLong("MCBarrelTime", 0);

			Brew.loadPrevSeeds(data);

			List<Integer> brewsCreated = data.getIntegerList("brewsCreated");
			if (brewsCreated != null && brewsCreated.size() == 7) {
				int hash = data.getInt("brewsCreatedH");
				// Check the hash to prevent tampering with statistics
				if (brewsCreated.hashCode() == hash) {
					P.p.brewsCreated = brewsCreated.get(0);
					P.p.brewsCreatedCmd = brewsCreated.get(1);
					P.p.exc = brewsCreated.get(2);
					P.p.good = brewsCreated.get(3);
					P.p.norm = brewsCreated.get(4);
					P.p.bad = brewsCreated.get(5);
					P.p.terr = brewsCreated.get(6);
				}
			}

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

			// Register Item Loaders
			CustomItem.registerItemLoader();
			SimpleItem.registerItemLoader();
			PluginItem.registerItemLoader();

			// loading Ingredients into ingMap
			// Only for Legacy Brews
			Map<String, BIngredients> ingMap = new HashMap<>();
			ConfigurationSection section = data.getConfigurationSection("Ingredients");
			if (section != null) {
				for (String id : section.getKeys(false)) {
					if (section.isConfigurationSection(id + ".mats")) {
						// Old way of saving
						ConfigurationSection matSection = section.getConfigurationSection(id + ".mats");
						if (matSection != null) {
							// matSection has all the materials + amount as Integers
							List<Ingredient> ingredients = oldDeserializeIngredients(matSection);
							ingMap.put(id, new BIngredients(ingredients, section.getInt(id + ".cookedTime", 0), true));
						} else {
							P.p.errorLog("Ingredient id: '" + id + "' incomplete in data.yml");
						}
					} else {
						// New way of saving ingredients
						ingMap.put(id, deserializeIngredients(section.getString(id + ".mats")));
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
					int alc = section.getInt(uid + ".alc", 0);
					byte distillRuns = (byte) section.getInt(uid + ".distillRuns", 0);
					float ageTime = (float) section.getDouble(uid + ".ageTime", 0.0);
					float wood = (float) section.getDouble(uid + ".wood", -1.0);
					String recipe = section.getString(uid + ".recipe", null);
					boolean unlabeled = section.getBoolean(uid + ".unlabeled", false);
					boolean persistent = section.getBoolean(uid + ".persist", false);
					boolean stat = section.getBoolean(uid + ".stat", false);
					int lastUpdate = section.getInt(uid + ".lastUpdate", 0);

					Brew.loadLegacy(ingredients, P.p.parseInt(uid), quality, alc, distillRuns, ageTime, wood, recipe, unlabeled, persistent, stat, lastUpdate);
				}
			}

			// Store how many legacy brews were created
			if (P.p.brewsCreated <= 0) {
				P.p.brewsCreated = 0;
				P.p.brewsCreatedCmd = 0;
				P.p.exc = 0;
				P.p.good = 0;
				P.p.norm = 0;
				P.p.bad = 0;
				P.p.terr = 0;
				if (!Brew.noLegacy()) {
					for (Brew brew : Brew.legacyPotions.values()) {
						P.p.metricsForCreate(false);
					}
				}
			}

			// Remove Legacy Potions that haven't been touched in a long time, these may have been lost
			if (!Brew.noLegacy()) {
				int currentHoursAfterInstall = (int) ((double) (System.currentTimeMillis() - Brew.installTime) / 3600000D);
				int purgeTime = currentHoursAfterInstall - (24 * 30 * 4); // Purge Time is 4 Months ago
				if (purgeTime > 0) {
					int removed = 0;
					for (Iterator<Brew> iterator = Brew.legacyPotions.values().iterator(); iterator.hasNext(); ) {
						Brew brew = iterator.next();
						if (brew.getLastUpdate() < purgeTime) {
							iterator.remove();
							removed++;
						}
					}
					if (removed > 0) {
						P.p.log("Removed " + removed + " Legacy Brews older than 3 months");
					}
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
					loadWorldData(BUtil.getDxlName(world.getName()), world, data);
				} else {
					loadWorldData(world.getUID().toString(), world, data);
				}
			}

		} else {
			P.p.log("No data.yml found, will create new one!");
		}
	}

	public static BIngredients deserializeIngredients(String mat) {
		try (DataInputStream in = new DataInputStream(new Base91DecoderStream(new ByteArrayInputStream(mat.getBytes())))) {
			byte ver = in.readByte();
			return BIngredients.load(in, ver);
		} catch (IOException e) {
			e.printStackTrace();
			return new BIngredients();
		}
	}

	// Loading from the old way of saving ingredients
	public static List<Ingredient> oldDeserializeIngredients(ConfigurationSection matSection) {
		List<Ingredient> ingredients = new ArrayList<>();
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
			SimpleItem item;
			if (matSplit.length == 2) {
				item = new SimpleItem(m, (short) P.p.parseInt(matSplit[1]));
			} else {
				item = new SimpleItem(m);
			}
			item.setAmount(matSection.getInt(mat));
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
	public static BIngredients loadCauldronIng(ConfigurationSection section, String path) {
		if (section.isConfigurationSection(path)) {
			// Old way of saving
			ConfigurationSection matSection = section.getConfigurationSection(path);
			if (matSection != null) {
				// matSection has all the materials + amount as Integers
				return new BIngredients(oldDeserializeIngredients(section), 0);
			} else {
				P.p.errorLog("Cauldron is missing Ingredient Section");
				return new BIngredients();
			}
		} else {
			// New way of saving ingredients
			return deserializeIngredients(section.getString(path));
		}
	}

	// load Block locations of given world
	public static void loadWorldData(String uuid, World world, FileConfiguration data) {

		if (data == null) {
			File file = new File(P.p.getDataFolder(), "data.yml");
			if (file.exists()) {
				data = YamlConfiguration.loadConfiguration(file);
			} else {
				return;
			}
		}

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
						BIngredients ingredients = loadCauldronIng(section, cauldron + ".ingredients");
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

						BoundingBox box = null;
						if (section.contains(barrel + ".bounds")) {
							String[] bds = section.getString(barrel + ".bounds", "").split(",");
							if (bds.length == 6) {
								box = new BoundingBox(P.p.parseInt(bds[0]), P.p.parseInt(bds[1]), P.p.parseInt(bds[2]), P.p.parseInt(bds[3]), P.p.parseInt(bds[4]), P.p.parseInt(bds[5]));
							}
						} else if (section.contains(barrel + ".st")) {
							// Convert from Stair and Wood Locations to BoundingBox
							String[] st = section.getString(barrel + ".st", "").split(",");
							String[] wo = section.getString(barrel + ".wo", "").split(",");
							int woLength = wo.length;
							if (woLength <= 1) {
								woLength = 0;
							}
							String[] points = new String[st.length + woLength];
							System.arraycopy(st, 0, points, 0, st.length);
							if (woLength > 1) {
								System.arraycopy(wo, 0, points, st.length, woLength);
							}
							int[] locs = ArrayUtils.toPrimitive(Arrays.stream(points).map(s -> P.p.parseInt(s)).toArray(Integer[]::new));
							try {
								box = BoundingBox.fromPoints(locs);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

						Barrel b;
						if (invSection != null) {
							b = new Barrel(block, sign, box, invSection.getValues(true), time);
						} else {
							// Barrel has no inventory
							b = new Barrel(block, sign, box, null, time);
						}

						// In case Barrel Block locations were missing and could not be recreated: do not add the barrel

						if (b.getBody().getBounds() != null) {
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
