package com.dre.brewery.filedata;

import com.dre.brewery.Brew;
import com.dre.brewery.DistortChat;
import com.dre.brewery.MCBarrel;
import com.dre.brewery.P;
import com.dre.brewery.api.events.ConfigLoadEvent;
import com.dre.brewery.integration.barrel.WGBarrel;
import com.dre.brewery.integration.barrel.WGBarrel5;
import com.dre.brewery.integration.barrel.WGBarrel6;
import com.dre.brewery.integration.barrel.WGBarrel7;
import com.dre.brewery.integration.item.BreweryPluginItem;
import com.dre.brewery.integration.item.MMOItemsPluginItem;
import com.dre.brewery.integration.item.SlimefunPluginItem;
import com.dre.brewery.recipe.BCauldronRecipe;
import com.dre.brewery.recipe.BRecipe;
import com.dre.brewery.recipe.PluginItem;
import com.dre.brewery.recipe.RecipeItem;
import com.dre.brewery.utility.BUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BConfig {

	public static final String configVersion = "1.8";
	public static boolean updateCheck;
	public static CommandSender reloader;

	// Third Party Enabled
	public static boolean useWG; //WorldGuard
	public static WGBarrel wg;
	public static boolean useLWC; //LWC
	public static boolean useLB; //LogBlock
	public static boolean useGP; //GriefPrevention
	public static boolean hasVault; // Vault
	public static boolean useCitadel; // CivCraft/DevotedMC Citadel
	public static Boolean hasSlimefun = null; // Slimefun ; Null if not checked
	public static Boolean hasMMOItems = null; // MMOItems ; Null if not checked

	// Barrel
	public static boolean openEverywhere;

	//BPlayer
	public static Map<Material, Integer> drainItems = new HashMap<>();// DrainItem Material and Strength
	public static Material pukeItem;
	public static int pukeDespawntime;
	public static int hangoverTime;
	public static boolean overdrinkKick;
	public static boolean enableHome;
	public static boolean enableLoginDisallow;
	public static boolean enablePuke;
	public static String homeType;

	//Brew
	public static boolean colorInBarrels; // color the Lore while in Barrels
	public static boolean colorInBrewer; // color the Lore while in Brewer
	public static boolean enableEncode;
	public static boolean alwaysShowQuality; // Always show quality stars
	public static boolean alwaysShowAlc; // Always show alc%

	//Item
	public static List<RecipeItem> customItems = new ArrayList<>();

	public static P p = P.p;

	private static boolean checkConfigs() {
		File cfg = new File(p.getDataFolder(), "config.yml");
		if (!cfg.exists()) {
			p.errorLog("No config.yml found, creating default file! You may want to choose a config according to your language!");
			p.errorLog("You can find them in plugins/Brewery/configs/");
			InputStream defconf = p.getResource("config/" + (P.use1_13 ? "v13/" : "v12/") + "en/config.yml");
			if (defconf == null) {
				p.errorLog("default config file not found, your jarfile may be corrupt. Disabling Brewery!");
				return false;
			}
			try {
				BUtil.saveFile(defconf, p.getDataFolder(), "config.yml", false);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		if (!cfg.exists()) {
			p.errorLog("default config file could not be copied, your jarfile may be corrupt. Disabling Brewery!");
			return false;
		}

		copyDefaultConfigs(false);
		return true;
	}

	private static void copyDefaultConfigs(boolean overwrite) {
		File configs = new File(p.getDataFolder(), "configs");
		File languages = new File(p.getDataFolder(), "languages");
		for (String l : new String[] {"de", "en", "fr", "it", "zh", "tw"}) {
			File lfold = new File(configs, l);
			try {
				BUtil.saveFile(p.getResource("config/" + (P.use1_13 ? "v13/" : "v12/") + l + "/config.yml"), lfold, "config.yml", overwrite);
				BUtil.saveFile(p.getResource("languages/" + l + ".yml"), languages, l + ".yml", false); // Never overwrite languages for now
			} catch (IOException e) {
				if (!(l.equals("zh") || l.equals("tw"))) {
					e.printStackTrace();
				}
			}
		}
	}

	public static boolean readConfig() {
		File file = new File(P.p.getDataFolder(), "config.yml");
		if (!checkConfigs()) {
			return false;
		}
		FileConfiguration config = YamlConfiguration.loadConfiguration(file);

		// Set the Language
		p.language = config.getString("language", "en");

		// Load LanguageReader
		p.languageReader = new LanguageReader(new File(p.getDataFolder(), "languages/" + p.language + ".yml"));

		// Has to config still got old materials
		boolean oldMat = config.getBoolean("oldMat", false);

		// Check if config is the newest version
		String version = config.getString("version", null);
		if (version != null) {
			if (!version.equals(configVersion) || (oldMat && P.use1_13)) {
				copyDefaultConfigs(true);
				new ConfigUpdater(file).update(version, oldMat, p.language);
				P.p.log("Config Updated to version: " + configVersion);
				config = YamlConfiguration.loadConfiguration(file);
			}
		}

		// If the Update Checker should be enabled
		updateCheck = config.getBoolean("updateCheck", false);

		PluginManager plMan = p.getServer().getPluginManager();

		// Third-Party
		useWG = config.getBoolean("useWorldGuard", true) && plMan.isPluginEnabled("WorldGuard");

		if (useWG) {
			Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldEdit");
			if (plugin != null) {
				String wgv = plugin.getDescription().getVersion();
				if (wgv.startsWith("6.")) {
					wg = new WGBarrel6();
				} else if (wgv.startsWith("5.")) {
					wg = new WGBarrel5();
				} else {
					wg = new WGBarrel7();
				}
			}
			if (wg == null) {
				P.p.errorLog("Failed loading WorldGuard Integration! Opening Barrels will NOT work!");
				P.p.errorLog("Brewery was tested with version 5.8, 6.1 and 7.0 of WorldGuard!");
				P.p.errorLog("Disable the WorldGuard support in the config and do /brew reload");
			}
		}
		useLWC = config.getBoolean("useLWC", true) && plMan.isPluginEnabled("LWC");
		useGP = config.getBoolean("useGriefPrevention", true) && plMan.isPluginEnabled("GriefPrevention");
		useLB = config.getBoolean("useLogBlock", false) && plMan.isPluginEnabled("LogBlock");
		useCitadel = config.getBoolean("useCitadel", false) && plMan.isPluginEnabled("Citadel");
		// The item util has been removed in Vault 1.7+
		hasVault = plMan.isPluginEnabled("Vault")
			&& Integer.parseInt(plMan.getPlugin("Vault").getDescription().getVersion().split("\\.")[1]) <= 6;

		// various Settings
		DataSave.autosave = config.getInt("autosave", 3);
		P.debug = config.getBoolean("debug", false);
		pukeItem = Material.matchMaterial(config.getString("pukeItem", "SOUL_SAND"));
		hangoverTime = config.getInt("hangoverDays", 0) * 24 * 60;
		overdrinkKick = config.getBoolean("enableKickOnOverdrink", false);
		enableHome = config.getBoolean("enableHome", false);
		enableLoginDisallow = config.getBoolean("enableLoginDisallow", false);
		enablePuke = config.getBoolean("enablePuke", false);
		pukeDespawntime = config.getInt("pukeDespawntime", 60) * 20;
		homeType = config.getString("homeType", null);
		colorInBarrels = config.getBoolean("colorInBarrels", false);
		colorInBrewer = config.getBoolean("colorInBrewer", false);
		alwaysShowQuality = config.getBoolean("alwaysShowQuality", false);
		alwaysShowAlc = config.getBoolean("alwaysShowAlc", false);
		enableEncode = config.getBoolean("enableEncode", false);
		openEverywhere = config.getBoolean("openLargeBarrelEverywhere", false);
		MCBarrel.maxBrews = config.getInt("maxBrewsInMCBarrels", 6);

		Brew.loadSeed(config, file);

		PluginItem.registerForConfig("brewery", BreweryPluginItem::new);
		PluginItem.registerForConfig("mmoitems", MMOItemsPluginItem::new);
		PluginItem.registerForConfig("slimefun", SlimefunPluginItem::new);
		PluginItem.registerForConfig("exoticgarden", SlimefunPluginItem::new);

		// Loading custom items
		ConfigurationSection configSection = config.getConfigurationSection("customItems");
		if (configSection != null) {
			for (String custId : configSection.getKeys(false)) {
				RecipeItem custom = RecipeItem.fromConfigCustom(configSection, custId);
				if (custom != null) {
					custom.makeImmutable();
					customItems.add(custom);
				} else {
					p.errorLog("Loading the Custom Item with id: '" + custId + "' failed!");
				}
			}
		}

		// loading recipes
		configSection = config.getConfigurationSection("recipes");
		if (configSection != null) {
			List<BRecipe> configRecipes = BRecipe.getConfigRecipes();
			for (String recipeId : configSection.getKeys(false)) {
				BRecipe recipe = BRecipe.fromConfig(configSection, recipeId);
				if (recipe != null && recipe.isValid()) {
					configRecipes.add(recipe);
				} else {
					p.errorLog("Loading the Recipe with id: '" + recipeId + "' failed!");
				}
			}
			BRecipe.numConfigRecipes = configRecipes.size();
		}

		// Loading Cauldron Recipes
		configSection = config.getConfigurationSection("cauldron");
		if (configSection != null) {
			List<BCauldronRecipe> configRecipes = BCauldronRecipe.getConfigRecipes();
			for (String id : configSection.getKeys(false)) {
				BCauldronRecipe recipe = BCauldronRecipe.fromConfig(configSection, id);
				if (recipe != null) {
					configRecipes.add(recipe);
				} else {
					p.errorLog("Loading the Cauldron-Recipe with id: '" + id + "' failed!");
				}
			}
			BCauldronRecipe.numConfigRecipes = configRecipes.size();
		}

		// Recalculating Cauldron-Accepted Items for non-config recipes
		for (BRecipe recipe : BRecipe.getAddedRecipes()) {
			recipe.updateAcceptedLists();
		}
		for (BCauldronRecipe recipe : BCauldronRecipe.getAddedRecipes()) {
			recipe.updateAcceptedLists();
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
						drainItems.put(mat, strength);
					}
				}
			}
		}

		// Loading Words
		DistortChat.words = new ArrayList<>();
		DistortChat.ignoreText = new ArrayList<>();
		if (config.getBoolean("enableChatDistortion", false)) {
			for (Map<?, ?> map : config.getMapList("words")) {
				new DistortChat(map);
			}
			for (String bypass : config.getStringList("distortBypass")) {
				DistortChat.ignoreText.add(bypass.split(","));
			}
			DistortChat.commands = config.getStringList("distortCommands");
		}
		DistortChat.log = config.getBoolean("logRealChat", false);
		DistortChat.doSigns = config.getBoolean("distortSignText", false);

		// The Config was reloaded, call Event
		ConfigLoadEvent event = new ConfigLoadEvent();
		P.p.getServer().getPluginManager().callEvent(event);

		return true;
	}
}
