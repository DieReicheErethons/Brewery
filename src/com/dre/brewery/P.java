package com.dre.brewery;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.HashMap;
import java.io.IOException;
import java.io.File;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import com.dre.brewery.integration.LogBlockBarrel;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.event.HandlerList;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.mcstats.Metrics;

import com.dre.brewery.listeners.*;

public class P extends JavaPlugin {
	public static P p;
	public static boolean debug;
	public static int lastBackup = 0;
	public static int lastSave = 1;
	public static int autosave = 3;

	// Third Party Enabled
	public boolean hasLWC;
	public boolean hasLB;

	// Listeners
	public BlockListener blockListener;
	public PlayerListener playerListener;
	public EntityListener entityListener;
	public InventoryListener inventoryListener;
	public WorldListener worldListener;

	// Language
	public String language;
	public LanguageReader languageReader;

	@Override
	public void onEnable() {
		p = this;

		readConfig();
		readData();
		
		// Setup Metrics
		setupMetrics();

		// Listeners
		blockListener = new BlockListener();
		playerListener = new PlayerListener();
		entityListener = new EntityListener();
		inventoryListener = new InventoryListener();
		worldListener = new WorldListener();
		getCommand("Brewery").setExecutor(new CommandListener());

		p.getServer().getPluginManager().registerEvents(blockListener, p);
		p.getServer().getPluginManager().registerEvents(playerListener, p);
		p.getServer().getPluginManager().registerEvents(entityListener, p);
		p.getServer().getPluginManager().registerEvents(inventoryListener, p);
		p.getServer().getPluginManager().registerEvents(worldListener, p);

		// Heartbeat
		p.getServer().getScheduler().runTaskTimer(p, new BreweryRunnable(), 650, 1200);
		p.getServer().getScheduler().runTaskTimer(p, new DrunkRunnable(), 120, 120);

		this.log(this.getDescription().getName() + " enabled!");
	}

	@Override
	public void onDisable() {

		// Disable listeners
		HandlerList.unregisterAll(p);

		// Stop shedulers
		p.getServer().getScheduler().cancelTasks(this);

		// save Data to Disk
		saveData();
		
		// save LanguageReader
		languageReader.save();

		// delete Data from Ram
		Barrel.barrels.clear();
		BCauldron.bcauldrons.clear();
		BIngredients.possibleIngredients.clear();
		BIngredients.recipes.clear();
		BIngredients.cookedNames.clear();
		BPlayer.players.clear();
		Brew.potions.clear();
		Wakeup.wakeups.clear();
		Words.words.clear();

		this.log(this.getDescription().getName() + " disabled!");
	}
	
	public void setupMetrics() {
		try {
		    Metrics metrics = new Metrics(this);
		    metrics.start();
		} catch (IOException e) {
		}
	}
	
	public void reload(CommandSender sender) {
		// clear all existent config Data
		BIngredients.possibleIngredients.clear();
		BIngredients.recipes.clear();
		BIngredients.cookedNames.clear();
		Words.words.clear();
		BPlayer.drainItems.clear();
		if (hasLB) {
			LogBlockBarrel.clear();
		}

		// load the Config
		readConfig();

		// save and load LanguageReader
		languageReader.save();
		languageReader = new LanguageReader(new File(p.getDataFolder(), "languages/" + language + ".yml"));

		// Reload Recipes
		Boolean successful = true;
		for (Brew brew : Brew.potions.values()) {
			if (!brew.reloadRecipe()) {
				successful = false;
			}
		}
		if (!successful) {
			msg(sender, p.languageReader.get("Error_Recipeload"));
		}
	}

	public void msg(CommandSender sender, String msg) {
		sender.sendMessage(color("&2[Brewery] &f" + msg));
	}

	public void log(String msg) {
		this.msg(Bukkit.getConsoleSender(), msg);
	}

	public void debugLog(String msg) {
		if (debug) {
			this.msg(Bukkit.getConsoleSender(), "&2[Debug] &f" + msg);
		}
	}

	public void errorLog(String msg) {
		Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.DARK_RED + "ERROR: " + ChatColor.RED + msg);
	}

	public void readConfig() {
		// Check Third Party
		hasLWC = getServer().getPluginManager().isPluginEnabled("LWC");
		hasLB = getServer().getPluginManager().isPluginEnabled("LogBlock");

		File file = new File(p.getDataFolder(), "config.yml");
		if (!file.exists()) {
			saveDefaultConfig();
		}
		FileConfiguration config = YamlConfiguration.loadConfiguration(file);

		// Set the Language
		language = config.getString("language", "en");

		// Load LanguageReader
		languageReader = new LanguageReader(new File(p.getDataFolder(), "languages/" + language + ".yml"));

		// Check if config is the newest version
		String version = config.getString("version", null);
		if (version != null) {
			String currentVersion = getDescription().getVersion();
			if (!version.equals(currentVersion)) {
				new ConfigUpdater(file).update(version, language);
				P.p.log("Config Updated to version: " + currentVersion);
				config = YamlConfiguration.loadConfiguration(file);
			}
		}

		// various Settings
		autosave = config.getInt("autosave", 3);
		debug = config.getBoolean("debug", false);
		BPlayer.pukeItemId = Material.matchMaterial(config.getString("pukeItem", "SOUL_SAND")).getId();
		BPlayer.hangoverTime = config.getInt("hangoverDays", 0) * 24 * 60;
		BPlayer.overdrinkKick = config.getBoolean("enableKickOnOverdrink", false);
		BPlayer.enableHome = config.getBoolean("enableHome", false);
		BPlayer.enableLoginDisallow = config.getBoolean("enableLoginDisallow", false);
		BPlayer.enablePuke = config.getBoolean("enablePuke", false);
		BPlayer.homeType = config.getString("homeType", null);
		Brew.colorInBarrels = config.getBoolean("colorInBarrels", false);
		Brew.colorInBrewer = config.getBoolean("colorInBrewer", false);
		PlayerListener.openEverywhere = config.getBoolean("openLargeBarrelEverywhere", false);
		Words.log = config.getBoolean("logRealChat", false);
		Words.commands = config.getStringList("distortCommands");
		Words.doSigns = config.getBoolean("distortSignText", false);
		for (String bypass : config.getStringList("distortBypass")) {
			Words.ignoreText.add(bypass.split(","));
		}

		// loading recipes
		ConfigurationSection configSection = config.getConfigurationSection("recipes");
		if (configSection != null) {
			for (String recipeId : configSection.getKeys(false)) {
				BRecipe recipe = new BRecipe(configSection, recipeId);
				if (recipe.isValid()) {
					BIngredients.recipes.add(recipe);
				} else {
					errorLog("Loading the Recipe with id: '" + recipeId + "' failed!");
				}
			}
		}

		// loading cooked names and possible ingredients
		configSection = config.getConfigurationSection("cooked");
		if (configSection != null) {
			for (String ingredient : configSection.getKeys(false)) {
				Material mat = Material.matchMaterial(ingredient);
				if (mat != null) {
					BIngredients.cookedNames.put(mat, (configSection.getString(ingredient, null)));
					BIngredients.possibleIngredients.add(mat);
				} else {
					errorLog("Unknown Material: " + ingredient);
				}
			}
		}

		// loading drainItems
		List<String> drainList = config.getStringList("drainItems");
		if (drainList != null) {
			for (String drainString : drainList) {
				String[] drainSplit = drainString.split("/");
				if (drainSplit.length > 1) {
					Material mat = Material.matchMaterial(drainSplit[0]);
					int strength = p.parseInt(drainSplit[1]);
					if (mat != null && strength > 0) {
						BPlayer.drainItems.put(mat, strength);
					}
				}
			}
		}

		// telling Words the path, it will load it when needed
		Words.config = config;
	}

	// load all Data
	public void readData() {
		File file = new File(p.getDataFolder(), "data.yml");
		if (file.exists()) {

			FileConfiguration data = YamlConfiguration.loadConfiguration(file);

			// loading Ingredients into ingMap
			Map<String, BIngredients> ingMap = new HashMap<String, BIngredients>();
			ConfigurationSection section = data.getConfigurationSection("Ingredients");
			if (section != null) {
				for (String id : section.getKeys(false)) {
					ConfigurationSection matSection = section.getConfigurationSection(id + ".mats");
					if (matSection != null) {
						// matSection has all the materials + amount as Integers
						Map<Material, Integer> ingredients = new HashMap<Material, Integer>();
						for (String ingredient : matSection.getKeys(false)) {
							// convert to Material
							ingredients.put(Material.getMaterial(parseInt(ingredient)), matSection.getInt(ingredient));
						}
						ingMap.put(id, new BIngredients(ingredients, section.getInt(id + ".cookedTime", 0)));
					} else {
						errorLog("Ingredient id: '" + id + "' incomplete in data.yml");
					}
				}
			}

			// loading Brew
			section = data.getConfigurationSection("Brew");
			if (section != null) {
				// All sections have the UID as name
				for (String uid : section.getKeys(false)) {
					BIngredients ingredients = getIngredients(ingMap, section.getString(uid + ".ingId"));
					int quality = section.getInt(uid + ".quality", 0);
					int distillRuns = section.getInt(uid + ".distillRuns", 0);
					float ageTime = (float) section.getDouble(uid + ".ageTime", 0.0);
					float wood = (float) section.getDouble(uid + ".wood", -1.0);
					String recipe = section.getString(uid + ".recipe", null);
					Boolean unlabeled = section.getBoolean(uid + ".unlabeled", false);

					new Brew(parseInt(uid), ingredients, quality, distillRuns, ageTime, wood, recipe, unlabeled);
				}
			}

			// loading BPlayer
			section = data.getConfigurationSection("Player");
			if (section != null) {
				// keys have players name
				for (String name : section.getKeys(false)) {
					int quality = section.getInt(name + ".quality");
					int drunk = section.getInt(name + ".drunk");
					int offDrunk = section.getInt(name + ".offDrunk", 0);
					boolean passedOut = section.getBoolean(name + ".passedOut", false);

					new BPlayer(name, quality, drunk, offDrunk, passedOut);
				}
			}

			for (World world : p.getServer().getWorlds()) {
				if (world.getName().startsWith("DXL_")) {
					loadWorldData(getDxlName(world.getName()), world);
				} else {
					loadWorldData(world.getUID().toString(), world);
				}
			}

		} else {
			errorLog("No data.yml found, will create new one!");
		}
	}

	// returns Ingredients by id from the specified ingMap
	public BIngredients getIngredients(Map<String, BIngredients> ingMap, String id) {
		if (!ingMap.isEmpty()) {
			if (ingMap.containsKey(id)) {
				return ingMap.get(id);
			}
		}
		errorLog("Ingredient id: '" + id + "' not found in data.yml");
		return new BIngredients();
	}

	// loads BIngredients from an ingredient section
	public BIngredients loadIngredients(ConfigurationSection section) {
		if (section != null) {
			// has all the materials + amount as Integers
			Map<Material, Integer> ingredients = new HashMap<Material, Integer>();
			for (String ingredient : section.getKeys(false)) {
				// convert to Material
				ingredients.put(Material.getMaterial(parseInt(ingredient)), section.getInt(ingredient));
			}
			return new BIngredients(ingredients, 0);
		} else {
			errorLog("Cauldron is missing Ingredient Section");
		}
		return new BIngredients();
	}

	// load Block locations of given world
	public void loadWorldData(String uuid, World world) {

		File file = new File(p.getDataFolder(), "data.yml");
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

							Block worldBlock = world.getBlockAt(parseInt(splitted[0]), parseInt(splitted[1]), parseInt(splitted[2]));
							BIngredients ingredients = loadIngredients(section.getConfigurationSection(cauldron + ".ingredients"));
							int state = section.getInt(cauldron + ".state", 1);

							new BCauldron(worldBlock, ingredients, state);
						} else {
							errorLog("Incomplete Block-Data in data.yml: " + section.getCurrentPath() + "." + cauldron);
						}
					} else {
						errorLog("Missing Block-Data in data.yml: " + section.getCurrentPath() + "." + cauldron);
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
							Block block = world.getBlockAt(parseInt(splitted[0]), parseInt(splitted[1]), parseInt(splitted[2]));
							float time = (float) section.getDouble(barrel + ".time", 0.0);
							byte sign = (byte) section.getInt(barrel + ".sign", 0);
							String[] st = section.getString(barrel + ".st", "").split(",");
							String[] wo = section.getString(barrel + ".wo", "").split(",");

							if (invSection != null) {
								new Barrel(block, sign, st, wo, invSection.getValues(true), time);
							} else {
								// Barrel has no inventory
								new Barrel(block, sign, st, wo, null, time);
							}

						} else {
							errorLog("Incomplete Block-Data in data.yml: " + section.getCurrentPath() + "." + barrel);
						}
					} else {
						errorLog("Missing Block-Data in data.yml: " + section.getCurrentPath() + "." + barrel);
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
							errorLog("Incomplete Location-Data in data.yml: " + section.getCurrentPath() + "." + wakeup);
						}
					}
				}
			}

		}
	}

	// save all Data
	public void saveData() {
		long time = System.nanoTime();
		File datafile = new File(p.getDataFolder(), "data.yml");

		FileConfiguration oldData = YamlConfiguration.loadConfiguration(datafile);

		if (datafile.exists()) {
			if (lastBackup > 10) {
				datafile.renameTo(new File(p.getDataFolder(), "dataBackup.yml"));
				lastBackup = 0;
			} else {
				lastBackup++;
			}
		}

		FileConfiguration configFile = new YamlConfiguration();

		if (!Brew.potions.isEmpty()) {
			Brew.save(configFile.createSection("Brew"));
		}

		if (!BCauldron.bcauldrons.isEmpty() || oldData.contains("BCauldron")) {
			BCauldron.save(configFile.createSection("BCauldron"), oldData.getConfigurationSection("BCauldron"));
		}

		if (!Barrel.barrels.isEmpty() || oldData.contains("Barrel")) {
			Barrel.save(configFile.createSection("Barrel"), oldData.getConfigurationSection("Barrel"));
		}

		if (!BPlayer.players.isEmpty()) {
			BPlayer.save(configFile.createSection("Player"));
		}

		if (!Wakeup.wakeups.isEmpty() || oldData.contains("Wakeup")) {
			Wakeup.save(configFile.createSection("Wakeup"), oldData.getConfigurationSection("Wakeup"));
		}

		saveWorldNames(configFile, oldData.getConfigurationSection("Worlds"));

		configFile.set("Version", "1.0");

		try {
			configFile.save(datafile);
		} catch (IOException e) {
			e.printStackTrace();
		}

		lastSave = 1;

		time = System.nanoTime() - time;
		float ftime = (float) (time / 1000000.0);
		p.debugLog("Writing Data to File (" + ftime + "ms)");
	}

	public void saveWorldNames(FileConfiguration root, ConfigurationSection old) {
		if (old != null) {
			root.set("Worlds", old);
		}
		for (World world : p.getServer().getWorlds()) {
			String worldName = world.getName();
			if (worldName.startsWith("DXL_")) {
				worldName = getDxlName(worldName);
				root.set("Worlds." + worldName, 0);
			} else {
				worldName = world.getUID().toString();
				root.set("Worlds." + worldName, world.getName());
			}
		}
	}

	// Utility

	public int parseInt(String string) {
		return NumberUtils.toInt(string, 0);
	}

	// gets the Name of a DXL World
	public String getDxlName(String worldName) {
		File dungeonFolder = new File(worldName);
		if (dungeonFolder.isDirectory()) {
			for (File file : dungeonFolder.listFiles()) {
				if (!file.isDirectory()) {
					if (file.getName().startsWith(".id_")) {
						return file.getName().substring(1).toLowerCase();
					}
				}
			}
		}
		return worldName;
	}

	// create empty World save Sections
	public void createWorldSections(ConfigurationSection section) {
		for (World world : p.getServer().getWorlds()) {
			String worldName = world.getName();
			if (worldName.startsWith("DXL_")) {
				worldName = getDxlName(worldName);
			} else {
				worldName = world.getUID().toString();
			}
			section.createSection(worldName);
		}
	}

	// prints a list of Strings at the specified page
	public void list(CommandSender sender, ArrayList<String> strings, int page) {
		int pages = (int) Math.ceil(strings.size() / 7F);
		if (page > pages || page < 1) {
			page = 1;
		}

		sender.sendMessage(color("&7-------------- &f" + languageReader.get("Etc_Page") + " &6" + page + "&f/&6" + pages + " &7--------------"));

		ListIterator<String> iter = strings.listIterator((page - 1) * 7);

		for (int i = 0; i < 7; i++) {
			if (iter.hasNext()) {
				sender.sendMessage(color(iter.next()));
			} else {
				break;
			}
		}
	}

	// Returns true if the Block can be destroyed by the Player or something else (null)
	public boolean blockDestroy(Block block, Player player) {
		switch (block.getType()) {
			case CAULDRON:
				// will only remove when existing
				BCauldron.remove(block);
				return true;
			case FENCE:
			case NETHER_FENCE:
				// remove barrel and throw potions on the ground
				Barrel barrel = Barrel.getBySpigot(block);
				if (barrel != null) {
					if (barrel.hasPermsDestroy(player)) {
						barrel.remove(null, player);
						return true;
					} else {
						return false;
					}
				}
				return true;
			case SIGN:
			case WALL_SIGN:
				// remove small Barrels
				Barrel barrel2 = Barrel.getBySpigot(block);
				if (barrel2 != null) {
					if (!barrel2.isLarge()) {
						if (barrel2.hasPermsDestroy(player)) {
							barrel2.remove(null, player);
							return true;
						} else {
							return false;
						}
					} else {
						barrel2.destroySign();
					}
				}
				return true;
			case WOOD:
			case WOOD_STAIRS:
			case ACACIA_STAIRS:
			case BIRCH_WOOD_STAIRS:
			case DARK_OAK_STAIRS:
			case JUNGLE_WOOD_STAIRS:
			case SPRUCE_WOOD_STAIRS:
				Barrel barrel3 = Barrel.getByWood(block);
				if (barrel3 != null) {
					if (barrel3.hasPermsDestroy(player)) {
						barrel3.remove(block, player);
					} else {
						return false;
					}
				}
		}
		return true;
	}

	public String color(String msg) {
		if (msg != null) {
			msg = ChatColor.translateAlternateColorCodes('&', msg);
		}
		return msg;
	}

	// Runnables

	public class DrunkRunnable implements Runnable {

		public DrunkRunnable() {
		}

		@Override
		public void run() {
			if (!BPlayer.players.isEmpty()) {
				BPlayer.drunkeness();
			}
		}
	}

	public class BreweryRunnable implements Runnable {

		public BreweryRunnable() {
		}

		@Override
		public void run() {
			for (BCauldron cauldron : BCauldron.bcauldrons) {
				cauldron.onUpdate();// runs every min to update cooking time
			}
			Barrel.onUpdate();// runs every min to check and update ageing time
			BPlayer.onUpdate();// updates players drunkeness

			debugLog("Update");

			if (lastSave >= autosave) {
				saveData();// save all data
			} else {
				lastSave++;
			}
		}

	}

}
