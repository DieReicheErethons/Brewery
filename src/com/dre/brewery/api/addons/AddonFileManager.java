package com.dre.brewery.api.addons;

import com.dre.brewery.BreweryPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;

public class AddonFileManager {
	private final static BreweryPlugin plugin = BreweryPlugin.getInstance();

	private final BreweryAddon addon;
	private final String addonName;
	private final File addonFolder;
	private final AddonLogger logger;
	private final File configFile;
	private final YamlConfiguration addonConfig;

	public AddonFileManager(BreweryAddon addon) {
		this.addon = addon;
		this.addonName = addon.getClass().getSimpleName();
		this.addonFolder = new File(plugin.getDataFolder().getAbsolutePath() + File.separator + "addons" + File.separator + addonName);
		this.logger = addon.getLogger();
		this.configFile = new File(addonFolder, addonName + ".yml");
		this.addonConfig = YamlConfiguration.loadConfiguration(configFile);
	}


	public void generateFile(String fileName) {
		createAddonFolder();
		File file = new File(addonFolder, fileName);
		try {
			if (!file.exists()) {
				file.createNewFile();
				InputStream inputStream = getResource(fileName);
				if (inputStream != null) {
					OutputStream outputStream = Files.newOutputStream(file.toPath());
					byte[] buffer = new byte[1024];
					int bytesRead;
					while ((bytesRead = inputStream.read(buffer)) != -1) {
						outputStream.write(buffer, 0, bytesRead);
					}
					inputStream.close();
					outputStream.flush();
					outputStream.close();
				}
			}
		} catch (IOException ex) {
			logger.severe("Failed to generate file " + fileName, ex);
		}
	}

	public File getFile(String fileName) {
		createAddonFolder();
		return new File(addonFolder, fileName);
	}
	public YamlConfiguration getYamlConfiguration(String fileName) {
		createAddonFolder();
		return YamlConfiguration.loadConfiguration(new File(addonFolder, fileName));
	}
	public File getAddonFolder() {
		return addonFolder;
	}


	public YamlConfiguration getAddonConfig() {
		return addonConfig;
	}
	public void saveAddonConfig() {
		try {
			addonConfig.save(configFile);
		} catch (IOException ex) {
			logger.severe("Failed to save addon config", ex);
		}
	}

	@Nullable
	public InputStream getResource(@NotNull String filename) {
		try {
			URL url = addon.getClass().getClassLoader().getResource(filename);
			if (url == null) {
				return null;
			} else {
				URLConnection connection = url.openConnection();
				connection.setUseCaches(false);
				return connection.getInputStream();
			}
		} catch (IOException var4) {
			return null;
		}
	}

	private void createAddonFolder() {
		if (!addonFolder.exists()) {
			addonFolder.mkdirs();
		}
	}
}
