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
		File datafile = new File(P.p.getDataFolder(), "data.yml");
		data = YamlConfiguration.loadConfiguration(datafile);

		if (DataSave.lastBackup > 10) {
			datafile.renameTo(new File(P.p.getDataFolder(), "dataBackup.yml"));
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
