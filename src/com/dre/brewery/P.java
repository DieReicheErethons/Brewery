package com.dre.brewery;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.HashMap;
import java.io.IOException;
import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.apache.commons.lang.math.NumberUtils;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.event.HandlerList;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;

import com.dre.brewery.listeners.*;

public class P extends JavaPlugin {
	public static P p;
	public static int lastBackup = 0;
	public static int lastSave = 1;
	public static int autosave = 3;

	// Listeners
	public BlockListener blockListener;
	public PlayerListener playerListener;
	public EntityListener entityListener;
	public InventoryListener inventoryListener;
	public WorldListener worldListener;

	public Permission permission = null;

	@Override
	public void onEnable() {
		p = this;

		readConfig();
		readData();

		initPermissions();

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

	public void reload() {
		BIngredients.possibleIngredients.clear();
		BIngredients.recipes.clear();
		BIngredients.cookedNames.clear();
		Words.words.clear();
		BPlayer.drainItems.clear();

		readConfig();
	}

	public void msg(CommandSender sender, String msg) {
		sender.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.WHITE + msg);
	}

	public void log(String msg) {
		this.msg(Bukkit.getConsoleSender(), msg);
	}

	public void errorLog(String msg) {
		Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.DARK_RED + "ERROR: " + ChatColor.RED + msg);
	}

	public void initPermissions() {
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(Permission.class);
		if (permissionProvider != null) {
			permission = permissionProvider.getProvider();
		}
	}

	public void readConfig() {

		File file = new File(p.getDataFolder(), "config.yml");
		if (!file.exists()) {
			saveDefaultConfig();
		}
		FileConfiguration config = YamlConfiguration.loadConfiguration(file);

		// various Settings
		autosave = config.getInt("autosave", 3);
		BPlayer.pukeItemId = Material.matchMaterial(config.getString("pukeItem", "SOUL_SAND")).getId();

		// loading recipes
		ConfigurationSection configSection = config.getConfigurationSection("recipes");
		if (configSection != null) {
			for (String recipeId : configSection.getKeys(false)) {
				BIngredients.recipes.add(new BRecipe(configSection, recipeId));
			}
		}

		// loading cooked names and possible ingredients
		configSection = config.getConfigurationSection("cooked");
		if (configSection != null) {
			for (String ingredient : configSection.getKeys(false)) {
				Material mat = Material.matchMaterial(ingredient);
				BIngredients.cookedNames.put(mat, (configSection.getString(ingredient, null)));
				BIngredients.possibleIngredients.add(mat);
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

			// loading Brew
			ConfigurationSection section = data.getConfigurationSection("Brew");
			if (section != null) {
				// All sections have the UID as name
				for (String uid : section.getKeys(false)) {
					BIngredients ingredients = loadIngredients(section.getConfigurationSection(uid + ".ingredients"));
					int quality = section.getInt(uid + ".quality", 0);
					int distillRuns = section.getInt(uid + ".distillRuns", 0);
					float ageTime = (float) section.getDouble(uid + ".ageTime", 0.0);
					String recipe = section.getString(uid + ".recipe", null);

					new Brew(parseInt(uid), ingredients, quality, distillRuns, ageTime, recipe);
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

	// loads BIngredients from ingredient section
	public BIngredients loadIngredients(ConfigurationSection config) {
		if (config != null) {
			ConfigurationSection matSection = config.getConfigurationSection("mats");
			if (matSection != null) {
				// matSection has all the materials + amount in Integer form
				Map<Material, Integer> ingredients = new HashMap<Material, Integer>();
				for (String ingredient : matSection.getKeys(false)) {
					// convert to Material
					ingredients.put(Material.getMaterial(parseInt(ingredient)), matSection.getInt(ingredient));
				}
				return new BIngredients(ingredients, config.getInt("cookedTime", 0));
			}
		}
		errorLog("Ingredient section not found or incomplete in data.yml");
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

							if (invSection != null) {
								new Barrel(block, invSection.getValues(true), time);
							} else {
								// Barrel has no inventory
								new Barrel(block, time);
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

		time = System.nanoTime() - time;
		float ftime = (float) (time / 1000000.0);
		p.log("Creating a savefile (" + ftime + "ms)");
		time = System.nanoTime();

		if (!Brew.potions.isEmpty()) {
			Brew.save(configFile.createSection("Brew"));
		}

		time = System.nanoTime() - time;
		ftime = (float) (time / 1000000.0);
		p.log("Saving Brew (" + ftime + "ms)");
		time = System.nanoTime();

		if (!BCauldron.bcauldrons.isEmpty() || oldData.contains("BCauldron")) {
			BCauldron.save(configFile.createSection("BCauldron"), oldData.getConfigurationSection("BCauldron"));
		}

		time = System.nanoTime() - time;
		ftime = (float) (time / 1000000.0);
		p.log("Saving BCauldrons (" + ftime + "ms)");
		time = System.nanoTime();

		if (!Barrel.barrels.isEmpty() || oldData.contains("Barrel")) {
			Barrel.save(configFile.createSection("Barrel"), oldData.getConfigurationSection("Barrel"));
		}

		time = System.nanoTime() - time;
		ftime = (float) (time / 1000000.0);
		p.log("Saving Barrels (" + ftime + "ms)");
		time = System.nanoTime();

		if (!BPlayer.players.isEmpty()) {
			BPlayer.save(configFile.createSection("Player"));
		}

		time = System.nanoTime() - time;
		ftime = (float) (time / 1000000.0);
		p.log("Saving players (" + ftime + "ms)");
		time = System.nanoTime();

		if (!Wakeup.wakeups.isEmpty() || oldData.contains("Wakeup")) {
			Wakeup.save(configFile.createSection("Wakeup"), oldData.getConfigurationSection("Wakeup"));
		}

		time = System.nanoTime() - time;
		ftime = (float) (time / 1000000.0);
		p.log("Saving Wakeups (" + ftime + "ms)");
		time = System.nanoTime();

		try {
			configFile.save(datafile);
		} catch (IOException e) {
			e.printStackTrace();
		}

		lastSave = 1;

		time = System.nanoTime() - time;
		ftime = (float) (time / 1000000.0);
		p.log("Writing Data to File (" + ftime + "ms)");
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
						return file.getName().substring(1);
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
	public void list (CommandSender sender, ArrayList<String> strings, int page) {
		int pages = (int) Math.ceil(strings.size() / 7F);
		if (page > pages || page < 1) {
			page = 1;
		}

		sender.sendMessage(ChatColor.GRAY + "-------------- " + ChatColor.WHITE + "Seite " + ChatColor.GOLD + page + ChatColor.WHITE + "/" + ChatColor.GOLD + pages + ChatColor.GRAY + " --------------");

		ListIterator<String> iter = strings.listIterator((page - 1) * 7);

		for (int i = 0; i < 7; i++) {
			if (iter.hasNext()) {
				sender.sendMessage(iter.next());
			} else {
				break;
			}
		}
	}

	public String white() {
		return ChatColor.WHITE.toString();
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
			long time = System.nanoTime();

			for (BCauldron cauldron : BCauldron.bcauldrons) {
				cauldron.onUpdate();// runs every min to update cooking time
			}
			Barrel.onUpdate();// runs every min to check and update ageing time
			BPlayer.onUpdate();// updates players drunkeness

			if (lastSave >= autosave) {
				saveData();// save all data

				time = System.nanoTime() - time;
				float ftime = (float) (time / 1000000.0);
				p.log("Update and saving (" + ftime + "ms)");
			} else {
				lastSave++;

				time = System.nanoTime() - time;
				float ftime = (float) (time / 1000000.0);
				p.log("Update (" + ftime + "ms)");
			}
		}

	}

}
