package com.dre.brewery.filedata;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.utility.LegacyUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DataUpdater {

	private FileConfiguration data;
	private File file;
	private File worldFile;

	public DataUpdater(FileConfiguration data, File file, File worldFile) {
		this.data = data;
		this.file = file;
		this.worldFile = worldFile;
	}



	public void update(String fromVersion) {
		if (fromVersion.equalsIgnoreCase("1.0")) {
			update10();
			fromVersion = "1.1";
		}
		if (fromVersion.equalsIgnoreCase("1.1")) {
			update11();
			//fromVersion = "1.2";
		}

		try {
			data.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}




	public void update10() {

		data.set("Version", DataSave.dataVersion);

		ConfigurationSection section = data.getConfigurationSection("Ingredients");
		try {
			if (section != null) {
				for (String id : section.getKeys(false)) {
					ConfigurationSection matSection = section.getConfigurationSection(id + ".mats");
					if (matSection != null) {
						// matSection has all the materials + amount as Integers
						Map<String, Integer> ingredients = new HashMap<>();
						for (String ingredient : matSection.getKeys(false)) {
							// convert to Material
							Material mat = LegacyUtil.getMaterial(BreweryPlugin.breweryPlugin.parseInt(ingredient));
							if (mat != null) {
								ingredients.put(mat.name(), matSection.getInt(ingredient));
							}
						}
						section.set(id + ".mats", ingredients);
					} else {
						BreweryPlugin.breweryPlugin.errorLog("Ingredient id: '" + id + "' incomplete in data.yml");
					}
				}
			}
		} catch (Exception e) {
			// Getting Material by id may not work in the future
			BreweryPlugin.breweryPlugin.errorLog("Error Converting Ingredient Section of the Data File, newer versions of Bukkit may not support the old Save File anymore:");
			e.printStackTrace();
		}

		section = data.getConfigurationSection("BCauldron");
		if (section != null) {
			try {
				for (String uuid : section.getKeys(false)) {
					ConfigurationSection cauldrons = section.getConfigurationSection(uuid);
					if (cauldrons != null) {
						for (String id : cauldrons.getKeys(false)) {
							ConfigurationSection ingredientSection = cauldrons.getConfigurationSection(id + ".ingredients");
							if (ingredientSection != null) {
								// has all the materials + amount as Integers
								Map<String, Integer> ingredients = new HashMap<>();
								for (String ingredient : ingredientSection.getKeys(false)) {
									// convert to Material
									Material mat = LegacyUtil.getMaterial(BreweryPlugin.breweryPlugin.parseInt(ingredient));
									if (mat != null) {
										ingredients.put(mat.name(), ingredientSection.getInt(ingredient));
									}
								}
								cauldrons.set(id + ".ingredients", ingredients);
							} else {
								BreweryPlugin.breweryPlugin.errorLog("BCauldron " + id + " is missing Ingredient Section");
							}
						}
					}
				}
			} catch (Exception e) {
				// Getting Material by id may not work in the future
				BreweryPlugin.breweryPlugin.errorLog("Error Converting Ingredient Section of Cauldrons, newer versions of Bukkit may not support the old Save File anymore:");
				e.printStackTrace();
			}
		}
	}

	public void update11() {
		data.set("Version", DataSave.dataVersion);

		FileConfiguration worldData = new YamlConfiguration();
		if (data.contains("BCauldron")) {
			worldData.set("BCauldron", data.get("BCauldron"));
			data.set("BCauldron", null);
		}
		if (data.contains("Barrel")) {
			worldData.set("Barrel", data.get("Barrel"));
			data.set("Barrel", null);
		}
		if (data.contains("Wakeup")) {
			worldData.set("Wakeup", data.get("Wakeup"));
			data.set("Wakeup", null);
		}
		if (data.contains("Worlds")) {
			worldData.set("Worlds", data.get("Worlds"));
			data.set("Worlds", null);
		}

		try {
			worldData.save(worldFile);
			File bkup = new File(BreweryPlugin.breweryPlugin.getDataFolder(), "dataBackup.yml");
			if (bkup.exists()) {
				bkup.renameTo(new File(BreweryPlugin.breweryPlugin.getDataFolder(), "worlddataBackup.yml"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
