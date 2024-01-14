package com.dre.brewery.filedata;


import java.io.File;

import com.dre.brewery.P;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Writes the collected Data to file in Async Thread
 */
public class WriteData implements Runnable {

	private FileConfiguration data;
	private FileConfiguration worldData;

	public WriteData(FileConfiguration data, FileConfiguration worldData) {
		this.data = data;
		this.worldData = worldData;
	}

	@Override
	public void run() {
		File datafile = new File(P.p.getDataFolder(), "data.yml");
		File worlddatafile = new File(P.p.getDataFolder(), "worlddata.yml");

		try {
			data.save(datafile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			worldData.save(worlddatafile);
		} catch (Exception e) {
			e.printStackTrace();
		}

		DataSave.lastSave = 1;
		DataSave.running = null;
		BData.dataMutex.set(0);
	}
}
