package com.dre.brewery;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import java.io.File;
import org.apache.commons.lang.math.NumberUtils;

import com.dre.brewery.listeners.BlockListener;
import com.dre.brewery.listeners.PlayerListener;
import com.dre.brewery.listeners.EntityListener;
import com.dre.brewery.listeners.InventoryListener;
import com.dre.brewery.listeners.WorldListener;
import org.bukkit.event.HandlerList;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import java.io.IOException;

public class P extends JavaPlugin {
	public static P p;
	public static int lastBackup = 0;

	// Listeners
	public BlockListener blockListener;
	public PlayerListener playerListener;
	public EntityListener entityListener;
	public InventoryListener inventoryListener;
	public WorldListener worldListener;

	@Override
	public void onEnable() {
		p = this;

		readConfig();
		readData();

		// Listeners
		blockListener = new BlockListener();
		playerListener = new PlayerListener();
		entityListener = new EntityListener();
		inventoryListener = new InventoryListener();
		worldListener = new WorldListener();

		p.getServer().getPluginManager().registerEvents(blockListener, p);
		p.getServer().getPluginManager().registerEvents(playerListener, p);
		p.getServer().getPluginManager().registerEvents(entityListener, p);
		p.getServer().getPluginManager().registerEvents(inventoryListener, p);
		p.getServer().getPluginManager().registerEvents(worldListener, p);

		p.getServer().getScheduler().runTaskTimer(p, new BreweryRunnable(), 1200, 1200);

		this.log(this.getDescription().getName() + " enabled!");
	}

	@Override
	public void onDisable() {

		// Disable listeners
		HandlerList.unregisterAll(p);

		// Stop shedulers
		p.getServer().getScheduler().cancelTasks(this);

		saveData();

		this.log(this.getDescription().getName() + " disabled!");
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

	public void readConfig() {

		File file = new File(p.getDataFolder(), "config.yml");
		if (!file.exists()) {
			saveDefaultConfig();
		}
		FileConfiguration config = getConfig();

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
				BIngredients.cookedNames.put(Material.getMaterial(ingredient.toUpperCase()), (configSection.getString(ingredient)));
				BIngredients.possibleIngredients.add(Material.getMaterial(ingredient.toUpperCase()));
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
					new Brew(parseInt(uid), loadIngredients(section.getConfigurationSection(uid + ".ingredients")), section.getInt(uid + ".quality", 0), section.getInt(uid + ".distillRuns", 0),
							(float) section.getDouble(uid + ".ageTime", 0.0), section.getInt(uid + ".alcohol", 0));
				}
			}

			// loading BPlayer
			section = data.getConfigurationSection("Player");
			if (section != null) {
				// keys have players name
				for (String name : section.getKeys(false)) {
					new BPlayer(name, section.getInt(name + ".quality"), section.getInt(name + ".drunk"));
				}
			}

			for (org.bukkit.World world : p.getServer().getWorlds()) {
				loadWorldData(world.getUID().toString());
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
	public void loadWorldData(String uuid) {

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
							new BCauldron(getServer().getWorld(UUID.fromString(uuid)).getBlockAt(parseInt(splitted[0]), parseInt(splitted[1]), parseInt(splitted[2])),
									loadIngredients(section.getConfigurationSection(cauldron + ".ingredients")), section.getInt(cauldron + ".state", 1));
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
							if (invSection != null) {

								new Barrel(getServer().getWorld(UUID.fromString(uuid)).getBlockAt(parseInt(splitted[0]), parseInt(splitted[1]), parseInt(splitted[2])),
									invSection.getValues(true), (float) section.getDouble(barrel + ".time", 0.0));

							} else {
								// Barrel has no inventory
								new Barrel(getServer().getWorld(UUID.fromString(uuid)).getBlockAt(parseInt(splitted[0]), parseInt(splitted[1]), parseInt(splitted[2])),
									(float) section.getDouble(barrel + ".time", 0.0));
							}
						} else {
							errorLog("Incomplete Block-Data in data.yml: " + section.getCurrentPath() + "." + barrel);
						}
					} else {
						errorLog("Missing Block-Data in data.yml: " + section.getCurrentPath() + "." + barrel);
					}
				}
			}

		}
	}

	// save all Data
	public void saveData() {
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
		if (!BCauldron.bcauldrons.isEmpty()) {
			BCauldron.save(configFile.createSection("BCauldron"), oldData.getConfigurationSection("BCauldron"));
		}

		if (!Barrel.barrels.isEmpty()) {
			Barrel.save(configFile.createSection("Barrel"), oldData.getConfigurationSection("Barrel"));
		}

		if (!BPlayer.players.isEmpty()) {
			BPlayer.save(configFile.createSection("Player"));
		}

		try {
			configFile.save(datafile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int parseInt(String string) {
		return NumberUtils.toInt(string, 0);
	}

	public class BreweryRunnable implements Runnable {

		public BreweryRunnable() {
		}

		@Override
		public void run() {
			p.log("Update");
			for (BCauldron cauldron : BCauldron.bcauldrons) {
				cauldron.onUpdate();// runs every min to update cooking time
			}
			Barrel.onUpdate();// runs every min to check and update ageing time
			BPlayer.onUpdate();// updates players drunkeness

			saveData();// save all data
		}

	}

}
