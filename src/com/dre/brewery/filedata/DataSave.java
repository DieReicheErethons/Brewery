package com.dre.brewery.filedata;


import com.dre.brewery.*;
import com.dre.brewery.utility.BUtil;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class DataSave extends BukkitRunnable {

	public static int lastBackup = 0;
	public static int lastSave = 1;
	public static int autosave = 3;
	final public static String dataVersion = "1.1";
	public static DataSave running;

	public ReadOldData read;
	private long time;
	public boolean collected = false;

	// Not Thread-Safe! Needs to be run in main thread but uses async Read/Write
	public DataSave(ReadOldData read) {
		this.read = read;
		time = System.currentTimeMillis();
	}


	@Override
	public void run() {
		long saveTime = System.nanoTime();
		// Mutex has been acquired in ReadOldData
		try {
			FileConfiguration oldData;
			if (read != null) {
				if (!read.done) {
					// Wait for async thread to load old data
					if (System.currentTimeMillis() - time > 50000) {
						P.p.errorLog("Old Data took too long to load! Mutex: " + BData.dataMutex.get());
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
				oldData = read.getData();
			} else {
				oldData = new YamlConfiguration();
			}
			try {
				cancel();
			} catch (IllegalStateException ignored) {
			}

			FileConfiguration configFile = new YamlConfiguration();

			configFile.set("installTime", Brew.installTime);
			configFile.set("MCBarrelTime", MCBarrel.mcBarrelTime);

			Brew.writePrevSeeds(configFile);

			List<Integer> brewsCreated = new ArrayList<>(7);
			brewsCreated.add(P.p.brewsCreated);
			brewsCreated.add(P.p.brewsCreatedCmd);
			brewsCreated.add(P.p.exc);
			brewsCreated.add(P.p.good);
			brewsCreated.add(P.p.norm);
			brewsCreated.add(P.p.bad);
			brewsCreated.add(P.p.terr);
			configFile.set("brewsCreated", brewsCreated);
			configFile.set("brewsCreatedH", brewsCreated.hashCode());

			if (!Brew.legacyPotions.isEmpty()) {
				Brew.saveLegacy(configFile.createSection("Brew"));
			}

			if (!BCauldron.bcauldrons.isEmpty() || oldData.contains("BCauldron")) {
				BCauldron.save(configFile.createSection("BCauldron"), oldData.getConfigurationSection("BCauldron"));
			}

			if (!Barrel.barrels.isEmpty() || oldData.contains("Barrel")) {
				Barrel.save(configFile.createSection("Barrel"), oldData.getConfigurationSection("Barrel"));
			}

			if (!BPlayer.isEmpty()) {
				BPlayer.save(configFile.createSection("Player"));
			}

			if (!Wakeup.wakeups.isEmpty() || oldData.contains("Wakeup")) {
				Wakeup.save(configFile.createSection("Wakeup"), oldData.getConfigurationSection("Wakeup"));
			}

			saveWorldNames(configFile, oldData.getConfigurationSection("Worlds"));
			configFile.set("Version", dataVersion);

			collected = true;

			P.p.debugLog("saving: " + ((System.nanoTime() - saveTime) / 1000000.0) + "ms");

			if (P.p.isEnabled()) {
				P.p.getServer().getScheduler().runTaskAsynchronously(P.p, new WriteData(configFile));
			} else {
				new WriteData(configFile).run();
			}
			// Mutex will be released in WriteData
		} catch (Exception e) {
			e.printStackTrace();
			BData.dataMutex.set(0);
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
			P.p.log("Another Save was started while a Save was in Progress");
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
			read.runTaskAsynchronously(P.p);
			running = new DataSave(read);
			running.runTaskTimer(P.p, 1, 2);
		}
	}

	public static void autoSave() {
		if (lastSave >= autosave) {
			save(false);// save all data
		} else {
			lastSave++;
		}
	}

	public static void saveWorldNames(FileConfiguration root, ConfigurationSection old) {
		if (old != null) {
			root.set("Worlds", old);
		}
		for (World world : P.p.getServer().getWorlds()) {
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
}
