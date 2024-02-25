package com.dre.brewery.filedata;


import com.dre.brewery.BreweryPlugin;
import com.github.Anon8281.universalScheduler.UniversalRunnable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ReadOldData extends UniversalRunnable {

	public FileConfiguration data;
	public boolean done = false;

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Override
	public void run() {
		int wait = 0;
		// Set the Data Mutex to -1 if it is 0=Free
		while (!BData.dataMutex.compareAndSet(0, -1)) {
			if (wait > 300) {
				BreweryPlugin.getInstance().errorLog("Loading Process active for too long while trying to save! Mutex: " + BData.dataMutex.get());
				return;
			}
			wait++;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return;
			}
		}


		File worldDataFile = new File(BreweryPlugin.getInstance().getDataFolder(), "worlddata.yml");
		if (BData.worldData == null) {
			if (!worldDataFile.exists()) {
				data = new YamlConfiguration();
				done = true;
				return;
			}

			data = YamlConfiguration.loadConfiguration(worldDataFile);
		} else {
			data = BData.worldData;
		}

		if (DataSave.lastBackup > 10) {
			worldDataFile.renameTo(new File(BreweryPlugin.getInstance().getDataFolder(), "worlddataBackup.yml"));
			DataSave.lastBackup = 0;
		} else {
			DataSave.lastBackup++;
		}

		done = true;
	}

	public FileConfiguration getData() {
		return data;
	}

}
