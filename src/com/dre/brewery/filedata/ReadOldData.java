package com.dre.brewery.filedata;


import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import com.dre.brewery.P;

public class ReadOldData extends BukkitRunnable {

	public FileConfiguration data;
	public boolean done = false;

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Override
	public void run() {
		int wait = 0;
		// Set the Data Mutex to -1 if it is 0=Free
		while (!BData.dataMutex.compareAndSet(0, -1)) {
			if (wait > 300) {
				P.p.errorLog("Loading Process active for too long while trying to save! Mutex: " + BData.dataMutex.get());
				return;
			}
			wait++;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return;
			}
		}


		File worldDataFile = new File(P.p.getDataFolder(), "worlddata.yml");
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
			worldDataFile.renameTo(new File(P.p.getDataFolder(), "worlddataBackup.yml"));
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
