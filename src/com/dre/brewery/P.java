package com.dre.brewery;

import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.filedata.BData;
import com.dre.brewery.filedata.DataSave;
import com.dre.brewery.filedata.LanguageReader;
import com.dre.brewery.filedata.UpdateChecker;
import com.dre.brewery.integration.IntegrationListener;
import com.dre.brewery.integration.barrel.LogBlockBarrel;
import com.dre.brewery.listeners.*;
import com.dre.brewery.recipe.BCauldronRecipe;
import com.dre.brewery.recipe.BRecipe;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.LegacyUtil;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

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

	// Language
	public String language;
	public LanguageReader languageReader;

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
		P.debug = true;
		if (LegacyUtil.initNbt()) {
			useNBT = true;
		}

		//P.p.log("§" + (use1_9 ? "a":"c") + "1.9 " + "§" + (use1_11 ? "a":"c") + "1.11 " + "§" + (use1_13 ? "a":"c") + "1.13 " + "§" + (use1_14 ? "a":"c") + "1.14");

		/*long master = new SecureRandom().nextLong();
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		XORScrambleStream scramble = new XORScrambleStream(new Base91EncoderStream(byteStream), master);
		DataOutputStream data = new DataOutputStream(scramble);
		DataInputStream dataIn = null;
		try {
			scramble.start();
			data.writeLong(12345L);
			scramble.stop();
			data.writeInt(1);
			data.writeInt(1);
			scramble.start();
			data.writeDouble(0.55555D);
			data.writeInt(234323);
			//data.writeUTF("Hallo Peter");
			data.writeLong(5419L); // Skip
			data.writeDouble(0.55555D);

			data.close();

			XORUnscrambleStream unscramble = new XORUnscrambleStream(new Base91DecoderStream(new ByteArrayInputStream(byteStream.toByteArray())), master);
			dataIn = new DataInputStream(unscramble);
			unscramble.start();
			P.p.log(dataIn.readLong() + "");
			unscramble.stop();
			P.p.log(dataIn.readInt() + "");
			P.p.log(dataIn.readInt() + "");
			unscramble.start();
			P.p.log(dataIn.readDouble() + "");
			dataIn.mark(1000);
			P.p.log(dataIn.readInt() + "");
			//P.p.log(dataIn.readUTF());
			dataIn.skip(8);
			P.p.log(dataIn.readDouble() + "");
			P.p.log("reset");
			dataIn.reset();
			P.p.log(dataIn.readInt() + "");
			//P.p.log(dataIn.readUTF());
			dataIn.skip(8);
			P.p.log(dataIn.readDouble() + "");

			dataIn.close();

			*//*for (int i = 0; i < 10; i++) {
				byteStream = new ByteArrayOutputStream();
				scramble = new XORScrambleStream(new Base91EncoderStream(byteStream));
				data = new DataOutputStream(scramble);
				data.writeInt(i);
				scramble.start();
				data.writeLong(12345L);
				data.writeLong(12345L);
				scramble.stop();
				data.writeInt(1);
				data.writeInt(1);
				scramble.start();
				data.writeInt(234323);
				data.writeDouble(0.55555D);

				P.p.log(byteStream.toString());
				data.close();
			}*//*


			long time = System.currentTimeMillis();
			for (int i = 0; i < 100000; i++) {
				unscramble = new XORUnscrambleStream(new Base91DecoderStream(new ByteArrayInputStream(byteStream.toByteArray())), master);
				dataIn = new DataInputStream(unscramble);
				unscramble.start();
				dataIn.readLong();
				unscramble.stop();
				dataIn.readInt();
				dataIn.readInt();
				unscramble.start();
				dataIn.readDouble();
				dataIn.mark(1000);
				dataIn.readInt();
				//dataIn.readUTF();
				dataIn.skip(8);
				dataIn.readDouble();
				dataIn.reset();
				dataIn.readInt();
				//dataIn.readUTF();
				dataIn.skip(8);
				dataIn.readDouble();

				dataIn.close();
			}
			long time2 = System.currentTimeMillis();

			for (int i = 0; i < 100000; i++) {
				unscramble = new XORUnscrambleStream(new ByteArrayInputStream(byteStream.toByteArray()), master);
				dataIn = new DataInputStream(unscramble);
				unscramble.start();
				dataIn.skip(2);
				dataIn.readLong();
				unscramble.stop();
				dataIn.readInt();
				dataIn.readInt();
				unscramble.start();
				dataIn.readDouble();
				dataIn.mark(1000);
				dataIn.readInt();
				//dataIn.readUTF();
				dataIn.skip(8);
				dataIn.readDouble();
				dataIn.reset();
				dataIn.readInt();
				//dataIn.readUTF();
				dataIn.skip(8);
				dataIn.readDouble();

				dataIn.close();
			}
			long time3 = System.currentTimeMillis();

			P.p.log("Time with base91: " + (time2 - time));
			P.p.log("Time without base91: " + (time3 - time2));

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} finally {
			try {
				data.close();
				if (dataIn != null) {
					dataIn.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}*/

		/*try {
			ItemMeta meta = new ItemStack(Material.POTION).getItemMeta();
			DataOutputStream data = new DataOutputStream(new Base91EncoderStream(new LoreSaveStream(meta, 3)));

			data.writeInt(2);
			data.writeLong(5);

			byte[] test = new byte[128];
			test[1] = 6;
			test[2] = 12;
			test[3] = 21;
			test[127] = 99;
			data.write(test);

			data.writeInt(123324);
			data.writeLong(12343843);

			data.close();
			meta.getLore();

			DataInputStream dataIn = new DataInputStream(new Base91DecoderStream(new LoreLoadStream(meta)));

			P.p.log(dataIn.readInt() + ", " + dataIn.readLong() + ", ");

			byte[] testIn = new byte[128];
			dataIn.read(testIn);
			P.p.log(testIn[1] + ", " + testIn[2] + ", " + testIn[3] + ", " + testIn[127]);

			P.p.log(dataIn.readInt() + ", " + dataIn.readLong() + ", ");

			dataIn.close();



			basE91 basE91 = new basE91();
			int[] input = new int[] {12, 65, 324, 5, 12, 129459, 1234567, Integer.MIN_VALUE, Integer.MAX_VALUE};
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			DataOutputStream data = new DataOutputStream(stream);
			for (int i = 0; i < input.length; i++) {
				data.writeInt(input[i]);
			}
			data.flush();
			data.close();
			byte[] in = stream.toByteArray();
			byte[] out = new byte[4096];
			int lenght = basE91.encode(in, in.length, out);
			basE91.encEnd(out);
			String done = new String(out, 0, lenght);

			byte[] tin = done.getBytes();

			byte[] tout = new byte[4096];
			lenght = basE91.decode(tin, tin.length, tout);
			basE91.decEnd(tout);


			ByteArrayInputStream tstream = new ByteArrayInputStream(tout, 0, lenght);
			DataInputStream tdata = new DataInputStream(tstream);
			int[] test = new int[4096];
			for (int j = 0; j < 6; j++) {
				if (tstream.available() <= 0) break;
				test[j] = tdata.readInt();

			}
			tdata.close();
			test = test;*/



			/*basE91 basE91 = new basE91();
			int[] input = new int[] {12, 65, 324, 5, 12, 129459, 1234567, Integer.MIN_VALUE, Integer.MAX_VALUE};
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			DataOutputStream data = new DataOutputStream(stream);
			for (int i = 0; i < input.length; i++) {
				data.writeInt(input[i]);
			}
			data.flush();
			data.close();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ByteArrayInputStream in = new ByteArrayInputStream(stream.toByteArray());

			encode(in, out, in.available());

			in.close();
			out.flush();
			out.close();

			String done = new String(out.toByteArray());

			ByteArrayInputStream tin = new ByteArrayInputStream(done.getBytes());
			ByteArrayOutputStream tout = new ByteArrayOutputStream();

			decode(tin, tout, tin.available());

			tin.close();
			tout.flush();
			tout.close();

			ByteArrayInputStream tstream = new ByteArrayInputStream(tout.toByteArray());
			DataInputStream tdata = new DataInputStream(tstream);
			int[] test = new int[4096];
			for (int j = 0; j < 9; j++) {
				if (tstream.available() <= 0) break;
				test[j] = tdata.readInt();

			}
			tdata.close();
			test = test;

		} catch (IOException e) {
			e.printStackTrace();
		}*/


		// load the Config
		try {
			FileConfiguration cfg = BConfig.loadConfigFile();
			if (cfg == null || !BConfig.readConfig(cfg)) {
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
		BData.readData();

		// Setup Metrics
		/*try {
			Metrics metrics = new Metrics(this);
			metrics.addCustomChart(new Metrics.SingleLineChart("drunk_players", BPlayer::numDrunkPlayers));
			metrics.addCustomChart(new Metrics.SingleLineChart("brews_in_existence", () -> Brew.potions.size()));
			metrics.addCustomChart(new Metrics.SingleLineChart("barrels_built", () -> Barrel.barrels.size()));
			metrics.addCustomChart(new Metrics.SingleLineChart("cauldrons_boiling", () -> BCauldron.bcauldrons.size()));
			metrics.addCustomChart(new Metrics.AdvancedPie("brew_quality", () -> {
				Map<String, Integer> map = new HashMap<>(5);
				int exc = 0;
				int good = 0;
				int norm = 0;
				int bad = 0;
				int terr = 0;
				for (Brew brew : Brew.potions.values()) {
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

				map.put("excellent", exc);
				map.put("good", good);
				map.put("normal", norm);
				map.put("bad", bad);
				map.put("terrible", terr);
				return map;
			}));
			metrics.addCustomChart(new Metrics.SimplePie("number_of_recipes", () -> {
				int recipes = BIngredients.recipes.size();
				if (recipes < 7) {
					return "Less than 7";
				} else if (recipes < 11) {
					return "7-10";
				} else if (recipes == 11) {
					// There are 11 default recipes, so show this as its own slice
					return "11";
				} else if (recipes <= 31) {
					if (recipes % 2 == 0) {
						return recipes + "-" + (recipes + 1);
					} else {
						return (recipes - 1) + "-" + recipes;
					}
				} else {
					return "More than 31";
				}

			}));
		} catch (Throwable e) {
			e.printStackTrace();
		}*/

		// Listeners
		blockListener = new BlockListener();
		playerListener = new PlayerListener();
		entityListener = new EntityListener();
		inventoryListener = new InventoryListener();
		worldListener = new WorldListener();
		integrationListener = new IntegrationListener();
		getCommand("Brewery").setExecutor(new CommandListener());
		getCommand("Brewery").setTabCompleter(new TabListener());

		p.getServer().getPluginManager().registerEvents(blockListener, p);
		p.getServer().getPluginManager().registerEvents(playerListener, p);
		p.getServer().getPluginManager().registerEvents(entityListener, p);
		p.getServer().getPluginManager().registerEvents(inventoryListener, p);
		p.getServer().getPluginManager().registerEvents(worldListener, p);
		p.getServer().getPluginManager().registerEvents(integrationListener, p);
		if (use1_9) {
			p.getServer().getPluginManager().registerEvents(new CauldronListener(), p);
		}

		// Heartbeat
		p.getServer().getScheduler().runTaskTimer(p, new BreweryRunnable(), 650, 1200);
		p.getServer().getScheduler().runTaskTimer(p, new DrunkRunnable(), 120, 120);

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

		// delete Data from Ram
		Barrel.barrels.clear();
		BCauldron.bcauldrons.clear();
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
		BPlayer.clear();
		Brew.legacyPotions.clear();
		Wakeup.wakeups.clear();
		DistortChat.words.clear();
		DistortChat.ignoreText.clear();
		DistortChat.commands = null;

		this.log(this.getDescription().getName() + " disabled!");
	}

	public void reload(CommandSender sender) {
		if (sender != null && !sender.equals(getServer().getConsoleSender())) {
			BConfig.reloader = sender;
		}
		FileConfiguration cfg = BConfig.loadConfigFile();
		if (cfg == null) {
			// Could not read yml file, do not proceed
			return;
		}

		// clear all existent config Data
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

		// load the Config
		try {
			if (!BConfig.readConfig(cfg)) {
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

		// load LanguageReader
		languageReader = new LanguageReader(new File(p.getDataFolder(), "languages/" + language + ".yml"));

		// Reload Recipes
		boolean successful = true;
		for (Brew brew : Brew.legacyPotions.values()) {
			if (!brew.reloadRecipe()) {
				successful = false;
			}
		}
		if (!successful && sender != null) {
			msg(sender, p.languageReader.get("Error_Recipeload"));
		} else {
			p.msg(sender, p.languageReader.get("CMD_Reload"));
		}
		BConfig.reloader = null;
	}

	public P getInstance() {
		return p;
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
			BConfig.reloader = null;
			for (BCauldron cauldron : BCauldron.bcauldrons.values()) {
				cauldron.onUpdate();// runs every min to update cooking time
			}
			Barrel.onUpdate();// runs every min to check and update ageing time
			if (use1_14) MCBarrel.onUpdate();
			BPlayer.onUpdate();// updates players drunkeness

			debugLog("Update");

			DataSave.autoSave();
		}

	}

}
