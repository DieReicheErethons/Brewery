package com.dre.brewery.filedata;

import com.dre.brewery.P;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class LanguageReader {
	private Map<String, String> entries = new HashMap<>(128);

	private File file;

	public LanguageReader(File file, String defaultPath) {
		/* Load */
		this.file = file;

		FileConfiguration configFile = YamlConfiguration.loadConfiguration(file);

		Set<String> keySet = configFile.getKeys(false);
		for (String key : keySet) {
			entries.put(key, configFile.getString(key));
		}

		/* Check */
		check(defaultPath);
	}

	private void check(String defaultPath) {
		FileConfiguration defaults = null;
		ConfigUpdater updater = null;
		String line;
		InputStream resource = P.p.getResource(defaultPath);
		if (resource == null) return;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource))) {
			while ((line = reader.readLine()) != null) {
				int index = line.indexOf(':');
				if (index != -1) {
					String key = line.substring(0, index);
					if (!entries.containsKey(key)) {
						if (defaults == null) {
							defaults = new YamlConfiguration();
							defaults.load(new BufferedReader(new InputStreamReader(Objects.requireNonNull(P.p.getResource(defaultPath)))));
							updater = new ConfigUpdater(file);
							updater.appendLines("", "# Updated");
						}
						entries.put(key, defaults.getString(key));
						updater.appendLines(line);
					}
				}
			}
			if (updater != null) {
				createBackup();
				updater.saveConfig();
				P.p.log("Language file updated");
			}
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
			P.p.errorLog("Language File could not be updated");
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private void createBackup() {
		/* Copy old File */
		File source = new File(file.getPath());
		String filePath = file.getPath();
		File backup = new File(filePath.substring(0, filePath.length() - 4) + "_old.yml");

		if (backup.exists()) {
			backup.delete();
		}

		source.renameTo(backup);
	}

	public String get(String key, String... args) {
		String entry = entries.get(key);

		if (entry != null) {
			int i = 0;
			for (String arg : args) {
				if (arg != null) {
					i++;
					entry = entry.replace("&v" + i, arg);
				}
			}
		} else {
			entry = "%placeholder%";
		}

		return entry;
	}
}
