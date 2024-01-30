package com.dre.brewery.api.addons;

import com.dre.brewery.BreweryPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class AddonFileManager {
	private final static BreweryPlugin plugin = BreweryPlugin.getInstance();

	private final BreweryAddon addon;
	private final String addonName;
	private final File addonFolder;
	private final AddonLogger logger;
	private final File configFile;
	private YamlConfiguration addonConfig;
	private final File jarFile;

	public AddonFileManager(BreweryAddon addon, File jarFile) {
		this.addon = addon;
		this.jarFile = jarFile;
		this.addonName = addon.getClass().getSimpleName();
		this.addonFolder = new File(plugin.getDataFolder().getAbsolutePath() + File.separator + "addons" + File.separator + addonName);
		this.logger = addon.getLogger();
		this.configFile = new File(addonFolder, addonName + ".yml");
		this.addonConfig = configFile.exists() ? YamlConfiguration.loadConfiguration(configFile) : null;
	}


	public void generateFile(String fileName) {
		generateFile(new File(addonFolder, fileName));
	}
	public void generateFileAbsPath(String absolutePath) {
		generateFile(new File(absolutePath));
	}
	public void generateFile(File parent, String fileName) {
		generateFile(new File(parent, fileName));
	}
	public void generateFile(File file) {
		createAddonFolder();
		try {
			if (!file.exists()) {
				file.createNewFile();
				try (JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile))) {
					JarEntry jarEntry;
					while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
						if (jarEntry.isDirectory() || !jarEntry.getName().equals(file.getName())) {
							continue;
						}
						OutputStream outputStream = Files.newOutputStream(file.toPath());
						byte[] buffer = new byte[1024];
						int bytesRead;
						while ((bytesRead = jarInputStream.read(buffer)) != -1) {
							outputStream.write(buffer, 0, bytesRead);
						}
						outputStream.flush();
						outputStream.close();
						break;
					}
				}
			}
		} catch (IOException ex) {
			logger.severe("Failed to generate file " + file.getName(), ex);
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
		generateAddonConfig();
		return addonConfig;
	}
	public void saveAddonConfig() {
		generateAddonConfig();
		try {
			addonConfig.save(configFile);
		} catch (IOException ex) {
			logger.severe("Failed to save addon config", ex);
		}
	}
	private void generateAddonConfig() {
		if (addonConfig == null) {
			generateFile(configFile);
			addonConfig = YamlConfiguration.loadConfiguration(configFile);
		}
	}

	private void createAddonFolder() {
		if (!addonFolder.exists()) {
			addonFolder.mkdirs();
		}
	}
}
