package com.dre.brewery;

import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.filedata.BData;
import com.dre.brewery.filedata.DataSave;
import com.dre.brewery.filedata.LanguageReader;
import com.dre.brewery.filedata.UpdateChecker;
import com.dre.brewery.integration.ChestShopListener;
import com.dre.brewery.integration.IntegrationListener;
import com.dre.brewery.integration.ShopKeepersListener;
import com.dre.brewery.integration.barrel.LogBlockBarrel;
import com.dre.brewery.listeners.*;
import com.dre.brewery.recipe.*;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.LegacyUtil;
import org.apache.commons.lang.math.NumberUtils;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

public class P extends JavaPlugin {
	public static P p;
	public static boolean debug;
	public static boolean useUUID;
	public static boolean useNBT;
	public static boolean use1_9;
	public static boolean use1_11;
	public static boolean use1_13;
	public static boolean use1_14;

	// Listeners
	public BlockListener blockListener;
	public PlayerListener playerListener;
	public EntityListener entityListener;
	public InventoryListener inventoryListener;
	public WorldListener worldListener;
	public IntegrationListener integrationListener;

	// Registrations
	public Map<String, Function<ItemLoader, Ingredient>> ingredientLoaders = new HashMap<>();

	// Language
	public String language;
	public LanguageReader languageReader;

	// Metrics
	public int brewsCreated;
	public int brewsCreatedCmd; // Created by command
	public int exc, good, norm, bad, terr; // Brews drunken with quality

	@Override
	public void onEnable() {
		p = this;

		// Version check
		String v = Bukkit.getBukkitVersion();
		useUUID = !v.matches("(^|.*[^.\\d])1\\.[0-6]([^\\d].*|$)") && !v.matches("(^|.*[^.\\d])1\\.7\\.[0-5]([^\\d].*|$)");
		use1_9 = !v.matches("(^|.*[^.\\d])1\\.[0-8]([^\\d].*|$)");
		use1_11 = !v.matches("(^|.*[^.\\d])1\\.10([^\\d].*|$)") && !v.matches("(^|.*[^.\\d])1\\.[0-9]([^\\d].*|$)");
		use1_13 = !v.matches("(^|.*[^.\\d])1\\.1[0-2]([^\\d].*|$)") && !v.matches("(^|.*[^.\\d])1\\.[0-9]([^\\d].*|$)");
		use1_14 = !v.matches("(^|.*[^.\\d])1\\.1[0-3]([^\\d].*|$)") && !v.matches("(^|.*[^.\\d])1\\.[0-9]([^\\d].*|$)");

		//MC 1.13 uses a different NBT API than the newer versions..
		// We decide here which to use, the new or the old or none at all
		if (LegacyUtil.initNbt()) {
			useNBT = true;
		}

		if (use1_14) {
			// Campfires are weird
			// Initialize once now so it doesn't lag later when we check for campfires under Cauldrons
			getServer().createBlockData(Material.CAMPFIRE);
		}

		// load the Config
		try {
			FileConfiguration cfg = BConfig.loadConfigFile();
			if (cfg == null) {
				p = null;
				getServer().getPluginManager().disablePlugin(this);
				return;
			}
			BConfig.readConfig(cfg);
		} catch (Exception e) {
			e.printStackTrace();
			p = null;
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		// Register Item Loaders
		CustomItem.registerItemLoader(this);
		SimpleItem.registerItemLoader(this);
		PluginItem.registerItemLoader(this);

		// Read data files
		BData.readData();

		// Setup Metrics
		setupMetrics();

		// Listeners
		blockListener = new BlockListener();
		playerListener = new PlayerListener();
		entityListener = new EntityListener();
		inventoryListener = new InventoryListener();
		worldListener = new WorldListener();
		integrationListener = new IntegrationListener();
		PluginCommand c = getCommand("Brewery");
		if (c != null) {
			c.setExecutor(new CommandListener());
			c.setTabCompleter(new TabListener());
		}

		p.getServer().getPluginManager().registerEvents(blockListener, p);
		p.getServer().getPluginManager().registerEvents(playerListener, p);
		p.getServer().getPluginManager().registerEvents(entityListener, p);
		p.getServer().getPluginManager().registerEvents(inventoryListener, p);
		p.getServer().getPluginManager().registerEvents(worldListener, p);
		p.getServer().getPluginManager().registerEvents(integrationListener, p);
		if (use1_9) {
			p.getServer().getPluginManager().registerEvents(new CauldronListener(), p);
		}
		if (BConfig.hasChestShop && use1_13) {
			p.getServer().getPluginManager().registerEvents(new ChestShopListener(), p);
		}
		if (BConfig.hasShopKeepers) {
			p.getServer().getPluginManager().registerEvents(new ShopKeepersListener(), p);
		}

		// Heartbeat
		p.getServer().getScheduler().runTaskTimer(p, new BreweryRunnable(), 650, 1200);
		p.getServer().getScheduler().runTaskTimer(p, new DrunkRunnable(), 120, 120);

		if (use1_9) {
			p.getServer().getScheduler().runTaskTimer(p, new CauldronParticles(), 1, 1);
		}

		if (BConfig.updateCheck) {
			try {
				p.getServer().getScheduler().runTaskLaterAsynchronously(p, new UpdateChecker(), 135);
			} catch (Exception e) {
				e.printStackTrace();
			}
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

		if (BConfig.sqlSync != null) {
			try {
				BConfig.sqlSync.closeConnection();
			} catch (SQLException ignored) {
			}
			BConfig.sqlSync = null;
		}

		// delete config data, in case this is a reload and to clear up some ram
		clearConfigData();

		this.log(this.getDescription().getName() + " disabled!");
	}

	public void reload(CommandSender sender) {
		if (sender != null && !sender.equals(getServer().getConsoleSender())) {
			BConfig.reloader = sender;
		}
		FileConfiguration cfg = BConfig.loadConfigFile();
		if (cfg == null) {
			// Could not read yml file, do not proceed, error was printed
			return;
		}

		// clear all existent config Data
		clearConfigData();

		// load the Config
		try {
			BConfig.readConfig(cfg);
		} catch (Exception e) {
			e.printStackTrace();
			p = null;
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		// Reload Cauldron Particle Recipes
		BCauldron.reload();

		// Reload Recipes
		boolean successful = true;
		for (Brew brew : Brew.legacyPotions.values()) {
			if (!brew.reloadRecipe()) {
				successful = false;
			}
		}
		if (sender != null) {
			if (!successful) {
				msg(sender, p.languageReader.get("Error_Recipeload"));
			} else {
				p.msg(sender, p.languageReader.get("CMD_Reload"));
			}
		}
		BConfig.reloader = null;
	}

	private void clearConfigData() {
		BRecipe.getConfigRecipes().clear();
		BRecipe.numConfigRecipes = 0;
		BCauldronRecipe.acceptedMaterials.clear();
		BCauldronRecipe.acceptedCustom.clear();
		BCauldronRecipe.acceptedSimple.clear();
		BCauldronRecipe.getConfigRecipes().clear();
		BCauldronRecipe.numConfigRecipes = 0;
		BConfig.customItems.clear();
		BConfig.hasSlimefun = null;
		BConfig.hasMMOItems = null;
		DistortChat.commands = null;
		BConfig.drainItems.clear();
		if (BConfig.useLB) {
			try {
				LogBlockBarrel.clear();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * For loading ingredients from ItemMeta.
	 * <p>Register a Static function that takes an ItemLoader, containing a DataInputStream.
	 * <p>Using the Stream it constructs a corresponding Ingredient for the chosen SaveID
	 *
	 * @param saveID The SaveID should be a small identifier like "AB"
	 * @param loadFct The Static Function that loads the Item, i.e.
	 *                public static AItem loadFrom(ItemLoader loader)
	 */
	public void registerForItemLoader(String saveID, Function<ItemLoader, Ingredient> loadFct) {
		ingredientLoaders.put(saveID, loadFct);
	}

	/**
	 * Unregister the ItemLoader
	 *
	 * @param saveID the chosen SaveID
	 */
	public void unRegisterItemLoader(String saveID) {
		ingredientLoaders.remove(saveID);
	}

	public static P getInstance() {
		return p;
	}

	private void setupMetrics() {
		try {
			Metrics metrics = new Metrics(this);
			metrics.addCustomChart(new Metrics.SingleLineChart("drunk_players", BPlayer::numDrunkPlayers));
			metrics.addCustomChart(new Metrics.SingleLineChart("brews_in_existence", () -> brewsCreated));
			metrics.addCustomChart(new Metrics.SingleLineChart("barrels_built", () -> Barrel.barrels.size()));
			metrics.addCustomChart(new Metrics.SingleLineChart("cauldrons_boiling", () -> BCauldron.bcauldrons.size()));
			metrics.addCustomChart(new Metrics.AdvancedPie("brew_quality", () -> {
				Map<String, Integer> map = new HashMap<>(8);
				map.put("excellent", exc);
				map.put("good", good);
				map.put("normal", norm);
				map.put("bad", bad);
				map.put("terrible", terr);
				return map;
			}));
			metrics.addCustomChart(new Metrics.AdvancedPie("brews_created", () -> {
				Map<String, Integer> map = new HashMap<>(4);
				map.put("by command", brewsCreatedCmd);
				map.put("brewing", brewsCreated - brewsCreatedCmd);
				return map;
			}));

			metrics.addCustomChart(new Metrics.SimplePie("number_of_recipes", () -> {
				int recipes = BRecipe.getAllRecipes().size();
				if (recipes < 7) {
					return "Less than 7";
				} else if (recipes < 11) {
					return "7-10";
				} else if (recipes == 11) {
					// There were 11 default recipes, so show this as its own slice
					return "11";
				} else if (recipes == 20) {
					// There are 20 default recipes, so show this as its own slice
					return "20";
				} else if (recipes <= 29) {
					if (recipes % 2 == 0) {
						return recipes + "-" + (recipes + 1);
					} else {
						return (recipes - 1) + "-" + recipes;
					}
				} else if (recipes < 35) {
					return "30-34";
				} else if (recipes < 40) {
					return "35-39";
				} else if (recipes < 45) {
					return "40-44";
				} else if (recipes <= 50) {
					return "45-50";
				} else {
					return "More than 50";
				}

			}));

			metrics.addCustomChart(new Metrics.SimplePie("wakeups", () -> {
				if (!BConfig.enableHome) {
					return "disabled";
				}
				int wakeups = Wakeup.wakeups.size();
				if (wakeups == 0) {
					return "0";
				} else if (wakeups <= 5) {
					return "1-5";
				} else if (wakeups <= 10) {
					return "6-10";
				} else if (wakeups <= 20) {
					return "11-20";
				} else {
					return "More than 20";
				}
			}));
			metrics.addCustomChart(new Metrics.SimplePie("v2_mc_version", () -> {
				String mcv = Bukkit.getBukkitVersion();
				mcv = mcv.substring(0, mcv.indexOf('.', 2));
				int index = mcv.indexOf('-');
				if (index > -1) {
					mcv = mcv.substring(0, index);
				}
				if (mcv.matches("^\\d\\.\\d{1,2}$")) {
					// Start, digit, dot, 1-2 digits, end
					return mcv;
				} else {
					return "undef";
				}
			}));
			metrics.addCustomChart(new Metrics.DrilldownPie("plugin_mc_version", () -> {
				Map<String, Map<String, Integer>> map = new HashMap<>(3);
				String mcv = Bukkit.getBukkitVersion();
				mcv = mcv.substring(0, mcv.indexOf('.', 2));
				int index = mcv.indexOf('-');
				if (index > -1) {
					mcv = mcv.substring(0, index);
				}
				if (mcv.matches("^\\d\\.\\d{1,2}$")) {
					// Start, digit, dot, 1-2 digits, end
					mcv = "MC " + mcv;
				} else {
					mcv = "undef";
				}
				Map<String, Integer> innerMap = new HashMap<>(3);
				innerMap.put(mcv, 1);
				map.put(getDescription().getVersion(), innerMap);
				return map;
			}));
			metrics.addCustomChart(new Metrics.SimplePie("language", () -> language));
			metrics.addCustomChart(new Metrics.SimplePie("config_scramble", () -> BConfig.enableEncode ? "enabled" : "disabled"));
			metrics.addCustomChart(new Metrics.SimplePie("config_lore_color", () -> {
				if (BConfig.colorInBarrels) {
					if (BConfig.colorInBrewer) {
						return "both";
					} else {
						return "in barrels";
					}
				} else {
					if (BConfig.colorInBrewer) {
						return "in distiller";
					} else {
						return "none";
					}
				}
			}));
			metrics.addCustomChart(new Metrics.SimplePie("config_always_show", () -> {
				if (BConfig.alwaysShowQuality) {
					if (BConfig.alwaysShowAlc) {
						return "both";
					} else {
						return "quality stars";
					}
				} else {
					if (BConfig.alwaysShowAlc) {
						return "alc content";
					} else {
						return "none";
					}
				}
			}));
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public void metricsForCreate(boolean byCmd) {
		if (brewsCreated == Integer.MAX_VALUE) return;
		brewsCreated++;
		if (byCmd) {
			if (brewsCreatedCmd == Integer.MAX_VALUE) return;
			brewsCreatedCmd++;
		}
	}

	public void metricsForDrink(Brew brew) {
		if (brew.getQuality() >= 9) {
			exc++;
		} else if (brew.getQuality() >= 7) {
			good++;
		} else if (brew.getQuality() >= 5) {
			norm++;
		} else if (brew.getQuality() >= 3) {
			bad++;
		} else {
			terr++;
		}
	}

	// Utility

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
		if (BConfig.reloader != null) {
			BConfig.reloader.sendMessage(ChatColor.DARK_GREEN + "[Brewery] " + ChatColor.DARK_RED + "ERROR: " + ChatColor.RED + msg);
		}
	}

	public int parseInt(String string) {
		return NumberUtils.toInt(string, 0);
	}

	public String color(String msg) {
		return BUtil.color(msg);
	}


	// Runnables

	public static class DrunkRunnable implements Runnable {
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
			long t1 = System.nanoTime();
			BConfig.reloader = null;
			Iterator<BCauldron> iter = BCauldron.bcauldrons.values().iterator();
			while (iter.hasNext()) {
				// runs every min to update cooking time
				if (!iter.next().onUpdate()) {
					iter.remove();
				}
			}
			long t2 = System.nanoTime();
			Barrel.onUpdate();// runs every min to check and update ageing time
			long t3 = System.nanoTime();
			if (use1_14) MCBarrel.onUpdate();
			long t4 = System.nanoTime();
			BPlayer.onUpdate();// updates players drunkeness

			long t5 = System.nanoTime();
			DataSave.autoSave();
			long t6 = System.nanoTime();

			debugLog("BreweryRunnable: " +
				"t1: " + (t2 - t1) / 1000000.0 + "ms" +
				" | t2: " + (t3 - t2) / 1000000.0 + "ms" +
				" | t3: " + (t4 - t3) / 1000000.0 + "ms" +
				" | t4: " + (t5 - t4) / 1000000.0 + "ms" +
				" | t5: " + (t6 - t5) / 1000000.0 + "ms" );
		}

	}

	public class CauldronParticles implements Runnable {
		@Override
		public void run() {
			if (!BConfig.enableCauldronParticles) return;
			if (BConfig.minimalParticles && BCauldron.particleRandom.nextFloat() > 0.5f) {
				return;
			}
			BCauldron.processCookEffects();
		}
	}

}
