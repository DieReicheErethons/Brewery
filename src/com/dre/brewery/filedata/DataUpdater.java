package com.dre.brewery.filedata;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import com.dre.brewery.P;

public class DataUpdater {

	private FileConfiguration data;
	private File file;

	public DataUpdater(FileConfiguration data, File file) {
		this.data = data;
		this.file = file;
	}



	public void update(String fromVersion) {
		if (fromVersion.equalsIgnoreCase("1.0")) {
			update10();
			//fromVersion = "1.1";
		}

		try {
			data.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}




	@SuppressWarnings("deprecation")
	public void update10() {

		data.set("Version", DataSave.dataVersion);

		ConfigurationSection section = data.getConfigurationSection("Ingredients");
		try {
			if (section != null) {
				for (String id : section.getKeys(false)) {
					ConfigurationSection matSection = section.getConfigurationSection(id + ".mats");
					if (matSection != null) {
						// matSection has all the materials + amount as Integers
						Map<String, Integer> ingredients = new HashMap<String, Integer>();
						for (String ingredient : matSection.getKeys(false)) {
							// convert to Material
							Material mat = Material.getMaterial(P.p.parseInt(ingredient));
							if (mat != null) {
								ingredients.put(mat.name(), matSection.getInt(ingredient));
							}
						}
						section.set(id + ".mats", ingredients);
					} else {
						P.p.errorLog("Ingredient id: '" + id + "' incomplete in data.yml");
					}
				}
			}
		} catch (Exception e) {
			// Getting Material by id may not work in the future
			P.p.errorLog("Error Converting Ingredient Section of the Data File, newer versions of Bukkit may not support the old Save File anymore:");
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
								Map<String, Integer> ingredients = new HashMap<String, Integer>();
								for (String ingredient : ingredientSection.getKeys(false)) {
									// convert to Material
									Material mat = Material.getMaterial(P.p.parseInt(ingredient));
									if (mat != null) {
										ingredients.put(mat.name(), ingredientSection.getInt(ingredient));
									}
								}
								cauldrons.set(id + ".ingredients", ingredients);
							} else {
								P.p.errorLog("BCauldron " + id + " is missing Ingredient Section");
							}
						}
					}
				}
			} catch (Exception e) {
				// Getting Material by id may not work in the future
				P.p.errorLog("Error Converting Ingredient Section of Cauldrons, newer versions of Bukkit may not support the old Save File anymore:");
				e.printStackTrace();
			}
		}
	}
}
