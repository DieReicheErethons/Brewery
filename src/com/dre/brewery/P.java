package com.dre.brewery;

import com.dre.brewery.filedata.*;
import com.dre.brewery.integration.LogBlockBarrel;
import com.dre.brewery.integration.WGBarrel;
import com.dre.brewery.integration.WGBarrel7;
import com.dre.brewery.integration.WGBarrelNew;
import com.dre.brewery.integration.WGBarrelOld;
import com.dre.brewery.listeners.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang.math.NumberUtils;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class P extends JavaPlugin {
	public static P p;
	public static final String configVersion = "1.6";
	public static boolean debug;
	public static boolean useUUID;
	public static boolean use1_9;
	public static boolean use1_12;
	public static boolean use1_13;
	public static boolean updateCheck;

	// Third Party Enabled
	public boolean useWG; //WorldGuard
	public WGBarrel wg;
	public boolean useLWC; //LWC
	public boolean useLB; //LogBlock
	public boolean useGP; //GriefPrevention
	public boolean hasVault; // Vault
	public boolean useCitadel; // CivCraft/DevotedMC Citadel

	// Listeners
	public BlockListener blockListener;
	public PlayerListener playerListener;
	public EntityListener entityListener;
	public InventoryListener inventoryListener;
	public WorldListener worldListener;

	// Language
	public String language;
	public LanguageReader languageReader;

	private CommandSender reloader;

	@Override
	public void onEnable() {
		p = this;

		// Version check
		String v = Bukkit.getBukkitVersion();
		useUUID = !v.matches("(^|.*[^\\.\\d])1\\.[0-6]([^\\d].*|$)") && !v.matches("(^|.*[^\\.\\d])1\\.7\\.[0-5]([^\\d].*|$)");
		use1_9 = !v.matches("(^|.*[^\\.\\d])1\\.[0-8]([^\\d].*|$)");
		use1_12 = !v.matches("(^|.*[^\\.\\d])1\\.1[0-1]([^\\d].*|$)") && !v.matches("(^|.*[^\\.\\d])1\\.[0-9]([^\\d].*|$)");
		use1_13 = !v.matches("(^|.*[^\\.\\d])1\\.1[0-2]([^\\d].*|$)") && !v.matches("(^|.*[^\\.\\d])1\\.[0-9]([^\\d].*|$)");

		// load the Config
		try {
			if (!readConfig()) {
				p = null;
				getServer().getPluginManager().disablePlugin(this);
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
			p = null;
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		readData();

		// Setup Metrics
		try {
			new Metrics(this);
		} catch (Exception e) {
			e.printStackTrace();
		}

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
		if (use1_9) {
			p.getServer().getPluginManager().registerEvents(new CauldronListener(), p);
		}

		// Heartbeat
		p.getServer().getScheduler().runTaskTimer(p, new BreweryRunnable(), 650, 1200);
		p.getServer().getScheduler().runTaskTimer(p, new DrunkRunnable(), 120, 120);

		if (updateCheck) {
			p.getServer().getScheduler().runTaskLaterAsynchronously(p, new UpdateChecker(), 135);
		}

		this.log(this.getDescription().getName() + " enabled!");
	}

	@Override
	public void onDisable() {

		// Disable listeners
		HandlerList.unregisterAll(this);

		// Stop shedulers
		getServer().getScheduler().cancelTasks(this);

		if (p == null) {
			return;
		}

		// save Data to Disk
		DataSave.save(true);

		// save LanguageReader
		languageReader.save();

		// delete Data from Ram
		Barrel.barrels.clear();
		BCauldron.bcauldrons.clear();
		BIngredients.possibleIngredients.clear();
		BIngredients.recipes.clear();
		BIngredients.cookedNames.clear();
		BPlayer.clear();
		Brew.potions.clear();
		Wakeup.wakeups.clear();
		Words.words.clear();
		Words.ignoreText.clear();
		Words.commands = null;

		this.log(this.getDescription().getName() + " disabled!");
	}

	public void reload(CommandSender sender) {
		if (sender != null && !sender.equals(getServer().getConsoleSender())) {
			reloader = sender;
		}
		// clear all existent config Data
		BIngredients.possibleIngredients.clear();
		BIngredients.recipes.clear();
		BIngredients.cookedNames.clear();
		Words.words.clear();
		Words.ignoreText.clear();
		Words.commands = null;
		BPlayer.drainItems.clear();
		if (useLB) {
			try {
				LogBlockBarrel.clear();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// load the Config
		try {
			if (!readConfig()) {
				p = null;
				getServer().getPluginManager().disablePlugin(this);
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
			p = null;
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		// save and load LanguageReader
		languageReader.save();
		languageReader = new LanguageReader(new File(p.getDataFolder(), "languages/" + language + ".yml"));

		// Reload Recipes
		boolean successful = true;
		for (Brew brew : Brew.potions.values()) {
			if (!brew.reloadRecipe()) {
				successful = false;
			}
		}
		if (!successful) {
			msg(sender, p.languageReader.get("Error_Recipeload"));
		}
		reloader = null;
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
		if (reloader != null) {
			reloader.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.DARK_RED + "ERROR: " + ChatColor.RED + msg);
		}
	}

	public boolean readConfig() {
		File file = new File(p.getDataFolder(), "config.yml");
		if (!checkConfigs()) {
			return false;
		}
		FileConfiguration config = YamlConfiguration.loadConfiguration(file);

		// Set the Language
		language = config.getString("language", "en");

		// Load LanguageReader
		languageReader = new LanguageReader(new File(p.getDataFolder(), "languages/" + language + ".yml"));

		// Check if config is the newest version
		String version = config.getString("version", null);
		if (version != null) {
			if (!version.equals(configVersion)) {
				copyDefaultConfigs(true);
				new ConfigUpdater(file).update(version, language);
				P.p.log("Config Updated to version: " + configVersion);
				config = YamlConfiguration.loadConfiguration(file);
			}
		}

		// If the Update Checker should be enabled
		updateCheck = config.getBoolean("updateCheck", false);

		// Third-Party
		useWG = config.getBoolean("useWorldGuard", true) && getServer().getPluginManager().isPluginEnabled("WorldGuard");
		if (useWG) {
			Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldEdit");
			if (plugin != null) {
				String wgv = plugin.getDescription().getVersion();
				if (wgv.startsWith("7.")) wg = new WGBarrel7();
				else if (wgv.startsWith("6.")) wg = new WGBarrelNew();
				else if (wgv.startsWith("5.")) wg = new WGBarrelOld();
			}
			if (wg == null) {
				P.p.errorLog("Failed loading WorldGuard Integration! Opening Barrels will NOT work!");
				P.p.errorLog("Brewery was tested with version 5.8, 6.1 and 7.0 of WorldGuard!");
				P.p.errorLog("Disable the WorldGuard support in the config and do /brew reload");
			}
		}
		useLWC = config.getBoolean("useLWC", true) && getServer().getPluginManager().isPluginEnabled("LWC");
		useGP = config.getBoolean("useGriefPrevention", true) && getServer().getPluginManager().isPluginEnabled("GriefPrevention");
		useLB = config.getBoolean("useLogBlock", false) && getServer().getPluginManager().isPluginEnabled("LogBlock");
		useCitadel = config.getBoolean("useCitadel", false) && getServer().getPluginManager().isPluginEnabled("Citadel");
		// The item util has been removed in Vault 1.7+
		hasVault = getServer().getPluginManager().isPluginEnabled("Vault")
			&& Integer.parseInt(getServer().getPluginManager().getPlugin("Vault").getDescription().getVersion().split("\\.")[1]) <= 6;

		// various Settings
		DataSave.autosave = config.getInt("autosave", 3);
		debug = config.getBoolean("debug", false);
		BPlayer.pukeItem = Material.matchMaterial(config.getString("pukeItem", "SOUL_SAND"));
		BPlayer.hangoverTime = config.getInt("hangoverDays", 0) * 24 * 60;
		BPlayer.overdrinkKick = config.getBoolean("enableKickOnOverdrink", false);
		BPlayer.enableHome = config.getBoolean("enableHome", false);
		BPlayer.enableLoginDisallow = config.getBoolean("enableLoginDisallow", false);
		BPlayer.enablePuke = config.getBoolean("enablePuke", false);
		BPlayer.pukeDespawntime = config.getInt("pukeDespawntime", 60) * 20;
		BPlayer.homeType = config.getString("homeType", null);
		Brew.colorInBarrels = config.getBoolean("colorInBarrels", false);
		Brew.colorInBrewer = config.getBoolean("colorInBrewer", false);
		PlayerListener.openEverywhere = config.getBoolean("openLargeBarrelEverywhere", false);

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
				if (mat == null && hasVault) {
					try {
						net.milkbowl.vault.item.ItemInfo vaultItem = net.milkbowl.vault.item.Items.itemByString(ingredient);
						if (vaultItem != null) {
							mat = vaultItem.getType();
						}
					} catch (Exception e) {
						P.p.errorLog("Could not check vault for Item Name");
						e.printStackTrace();
					}
				}
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
					if (mat == null && hasVault && strength > 0) {
						try {
							net.milkbowl.vault.item.ItemInfo vaultItem = net.milkbowl.vault.item.Items.itemByString(drainSplit[0]);
							if (vaultItem != null) {
								mat = vaultItem.getType();
							}
						} catch (Exception e) {
							P.p.errorLog("Could not check vault for Item Name");
							e.printStackTrace();
						}
					}
					if (mat != null && strength > 0) {
						BPlayer.drainItems.put(mat, strength);
					}
				}
			}
		}

		// Loading Words
		if (config.getBoolean("enableChatDistortion", false)) {
			for (Map<?, ?> map : config.getMapList("words")) {
				new Words(map);
			}
			for (String bypass : config.getStringList("distortBypass")) {
				Words.ignoreText.add(bypass.split(","));
			}
			Words.commands = config.getStringList("distortCommands");
		}
		Words.log = config.getBoolean("logRealChat", false);
		Words.doSigns = config.getBoolean("distortSignText", false);

		return true;
	}

	// load all Data
	public void readData() {
		File file = new File(p.getDataFolder(), "data.yml");
		if (file.exists()) {

			FileConfiguration data = YamlConfiguration.loadConfiguration(file);

			Brew.installTime = data.getLong("installTime", System.currentTimeMillis());

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
					boolean unlabeled = section.getBoolean(uid + ".unlabeled", false);
					boolean persistent = section.getBoolean(uid + ".persist", false);
					boolean stat = section.getBoolean(uid + ".stat", false);
					int lastUpdate = section.getInt("lastUpdate", 0);

					new Brew(parseInt(uid), ingredients, quality, distillRuns, ageTime, wood, recipe, unlabeled, persistent, stat, lastUpdate);
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
						if (!useUUID) {
							continue;
						}
					} catch (IllegalArgumentException e) {
						if (useUUID) {
							continue;
						}
					}

					int quality = section.getInt(name + ".quality");
					int drunk = section.getInt(name + ".drunk");
					int offDrunk = section.getInt(name + ".offDrunk", 0);

					new BPlayer(name, quality, drunk, offDrunk);
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

	public ArrayList<ItemStack> deserializeIngredients(ConfigurationSection matSection) {
		ArrayList<ItemStack> ingredients = new ArrayList<>();
		for (String mat : matSection.getKeys(false)) {
			String[] matSplit = mat.split(",");
			ItemStack item = new ItemStack(Material.getMaterial(matSplit[0]), matSection.getInt(mat));
			if (matSplit.length == 2) {
				item.setDurability((short) P.p.parseInt(matSplit[1]));
			}
			ingredients.add(item);
		}
		return ingredients;
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
			return new BIngredients(deserializeIngredients(section), 0);
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

	private boolean checkConfigs() {
		File cfg = new File(p.getDataFolder(), "config.yml");
		if (!cfg.exists()) {
			errorLog("No config.yml found, creating default file! You may want to choose a config according to your language!");
			errorLog("You can find them in plugins/Brewery/configs/");
			InputStream defconf = getResource("config/" + (use1_13 ? "v13/" : "v12/") + "en/config.yml");
			if (defconf == null) {
				errorLog("default config file not found, your jarfile may be corrupt. Disabling Brewery!");
				return false;
			}
			try {
				saveFile(defconf, getDataFolder(), "config.yml", false);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		if (!cfg.exists()) {
			errorLog("default config file could not be copied, your jarfile may be corrupt. Disabling Brewery!");
			return false;
		}

		copyDefaultConfigs(false);
		return true;
	}

	private void copyDefaultConfigs(boolean overwrite) {
		File configs = new File(getDataFolder(), "configs");
		File languages = new File(getDataFolder(), "languages");
		for (String l : new String[] {"de", "en", "fr", "it"}) {
			File lfold = new File(configs, l);
			try {
				saveFile(getResource("config/" + (use1_13 ? "v13/" : "v12/") + l + "/config.yml"), lfold, "config.yml", overwrite);
				saveFile(getResource("languages/" + l + ".yml"), languages, l + ".yml", false); // Never overwrite languages for now
			} catch (IOException e) {
				e.printStackTrace();
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
		Material type = block.getType();
		if (type == Material.CAULDRON) {
			// will only remove when existing
			BCauldron.remove(block);
			return true;

                } else if (LegacyUtil.isFence(type)) {
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

                } else if (LegacyUtil.isSign(type)) {
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

                } else if (LegacyUtil.isWoodPlanks(type) || LegacyUtil.isWoodStairs(type)){
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

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static void saveFile(InputStream in, File dest, String name, boolean overwrite) throws IOException {
		if (in == null) return;
		if (!dest.exists()) {
			dest.mkdirs();
		}
		File result = new File(dest, name);
		if (result.exists()) {
			if (overwrite) {
				result.delete();
			} else {
				return;
			}
		}

		OutputStream out = new FileOutputStream(result);
		byte[] buffer = new byte[1024];

		int length;
		//copy the file content in bytes
		while ((length = in.read(buffer)) > 0){
			out.write(buffer, 0, length);
		}

		in.close();
		out.close();
	}

	// Returns either uuid or Name of player, depending on bukkit version
	public static String playerString(Player player) {
		if (useUUID) {
			return player.getUniqueId().toString();
		} else {
			return player.getName();
		}
	}

	// returns the Player if online
	public static Player getPlayerfromString(String name) {
		if (useUUID) {
			try {
				return Bukkit.getPlayer(UUID.fromString(name));
			} catch (Exception e) {
				return Bukkit.getPlayerExact(name);
			}
		}
		return Bukkit.getPlayerExact(name);
	}

	// Runnables

	public class DrunkRunnable implements Runnable {
		@Override
		public void run() {
			if (!BPlayer.isEmpty()) {
				BPlayer.drunkeness();
			}
		}
	}

	public class BreweryRunnable implements Runnable {
		@Override
		public void run() {
			reloader = null;
			for (BCauldron cauldron : BCauldron.bcauldrons) {
				cauldron.onUpdate();// runs every min to update cooking time
			}
			Barrel.onUpdate();// runs every min to check and update ageing time
			BPlayer.onUpdate();// updates players drunkeness

			debugLog("Update");

			DataSave.autoSave();
		}

	}

}
