package com.dre.brewery.filedata;


import com.dre.brewery.*;
import com.dre.brewery.utility.BUtil;
import com.github.Anon8281.universalScheduler.UniversalRunnable;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DataSave extends UniversalRunnable {

	public static int lastBackup = 0;
	public static int lastSave = 1;
	public static int autosave = 3;
	final public static String dataVersion = "1.2";
	public static DataSave running;
	public static List<World> unloadingWorlds = new CopyOnWriteArrayList<>();

	public ReadOldData read;
	private final long time;
	private final List<World> loadedWorlds;
	public boolean collected = false;

	// Not Thread-Safe! Needs to be run in main thread but uses async Read/Write
	public DataSave(ReadOldData read) {
		this.read = read;
		time = System.currentTimeMillis();
		loadedWorlds = BreweryPlugin.getInstance().getServer().getWorlds();
	}


	// Running in Main Thread
	@Override
	public void run() {
		try {
			long saveTime = System.nanoTime();
			// Mutex has been acquired in ReadOldData
			FileConfiguration oldWorldData;
			if (read != null) {
				if (!read.done) {
					// Wait for async thread to load old data
					if (System.currentTimeMillis() - time > 50000) {
						BreweryPlugin.getInstance().errorLog("Old Data took too long to load! Mutex: " + BData.dataMutex.get());
						try {
							cancel();
							read.cancel();
						} catch (IllegalStateException ignored) {
						}
						running = null;
						BData.dataMutex.set(0);
					}
					return;
				}
				oldWorldData = read.getData();
			} else {
				oldWorldData = new YamlConfiguration();
			}
			try {
				cancel();
			} catch (IllegalStateException ignored) {
			}
			BData.worldData = null;

			FileConfiguration data = new YamlConfiguration();
			FileConfiguration worldData = new YamlConfiguration();

			data.set("installTime", Brew.installTime);
			data.set("MCBarrelTime", MCBarrel.mcBarrelTime);

			Brew.writePrevSeeds(data);

			List<Integer> brewsCreated = new ArrayList<>(7);
			brewsCreated.add(BreweryPlugin.getInstance().stats.brewsCreated);
			brewsCreated.add(BreweryPlugin.getInstance().stats.brewsCreatedCmd);
			brewsCreated.add(BreweryPlugin.getInstance().stats.exc);
			brewsCreated.add(BreweryPlugin.getInstance().stats.good);
			brewsCreated.add(BreweryPlugin.getInstance().stats.norm);
			brewsCreated.add(BreweryPlugin.getInstance().stats.bad);
			brewsCreated.add(BreweryPlugin.getInstance().stats.terr);
			data.set("brewsCreated", brewsCreated);
			data.set("brewsCreatedH", brewsCreated.hashCode());

			if (!Brew.legacyPotions.isEmpty()) {
				Brew.saveLegacy(data.createSection("Brew"));
			}

			if (!BPlayer.isEmpty()) {
				BPlayer.save(data.createSection("Player"));
			}

			if (!BCauldron.bcauldrons.isEmpty() || oldWorldData.contains("BCauldron")) {
				BCauldron.save(worldData.createSection("BCauldron"), oldWorldData.getConfigurationSection("BCauldron"));
			}

			if (!Barrel.barrels.isEmpty() || oldWorldData.contains("Barrel")) {
				Barrel.save(worldData.createSection("Barrel"), oldWorldData.getConfigurationSection("Barrel"));
			}

			if (!Wakeup.wakeups.isEmpty() || oldWorldData.contains("Wakeup")) {
				Wakeup.save(worldData.createSection("Wakeup"), oldWorldData.getConfigurationSection("Wakeup"));
			}

			saveWorldNames(worldData, oldWorldData.getConfigurationSection("Worlds"));

			data.set("Version", dataVersion);

			collected = true;

			if (!unloadingWorlds.isEmpty()) {
				try {
					for (World world : unloadingWorlds) {
						// In the very most cases, it is just one world, so just looping like this is fine
						Barrel.onUnload(world);
						BCauldron.onUnload(world);
						Wakeup.onUnload(world);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				unloadingWorlds.clear();
			}

			BreweryPlugin.getInstance().debugLog("saving: " + ((System.nanoTime() - saveTime) / 1000000.0) + "ms");

			if (BreweryPlugin.getInstance().isEnabled()) {
				BreweryPlugin.getScheduler().runTaskAsynchronously(new WriteData(data, worldData));
			} else {
				new WriteData(data, worldData).run();
			}
			// Mutex will be released in WriteData
		} catch (Exception e) {
			e.printStackTrace();
			BData.dataMutex.set(0);
		}
	}

	public void saveWorldNames(FileConfiguration root, ConfigurationSection old) {
		if (old != null) {
			root.set("Worlds", old);
		}
		for (World world : loadedWorlds) {
			String worldName = world.getName();
			if (worldName.startsWith("DXL_")) {
				worldName = BUtil.getDxlName(worldName);
				root.set("Worlds." + worldName, 0);
			} else {
				worldName = world.getUID().toString();
				root.set("Worlds." + worldName, world.getName());
			}
		}
	}

	// Finish the collection of data immediately
	public void now() {
		if (!read.done) {
			read.cancel();
			read.run();
		}
		if (!collected) {
			cancel();
			run();
		}
	}



	// Save all data. Takes a boolean whether all data should be collected in instantly
	public static void save(boolean collectInstant) {
		if (running != null) {
			BreweryPlugin.getInstance().log("Another Save was started while a Save was in Progress");
			if (collectInstant) {
				running.now();
			}
			return;
		}

		ReadOldData read = new ReadOldData();
		if (collectInstant) {
			read.run();
			running = new DataSave(read);
			running.run();
		} else {
			read.runTaskAsynchronously(BreweryPlugin.getInstance());
			running = new DataSave(read);
			running.runTaskTimer(BreweryPlugin.getInstance(), 1, 2);
		}
	}

	public static void autoSave() {
		if (lastSave >= autosave) {
			save(false);// save all data
		} else {
			lastSave++;
		}
	}
}
