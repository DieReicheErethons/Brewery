package com.dre.brewery.api.addons;

import com.dre.brewery.BreweryPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;

public class AddonManager {

	private final BreweryPlugin plugin;
	private final File addonsFolder;
	private final static List<BreweryAddon> addons = new ArrayList<>();
	private final static List<AddonCommand> addonCommands = new ArrayList<>();

	public AddonManager(BreweryPlugin plugin) {
		this.plugin = plugin;
		this.addonsFolder = new File(plugin.getDataFolder(), "addons");
		if (!addonsFolder.exists()) {
			addonsFolder.mkdirs();
		}
	}

	public void unloadAddons() {
		for (BreweryAddon addon : addons) {
			addon.onAddonDisable();
		}
	}

	public void reloadAddons() {
		for (BreweryAddon addon : addons) {
			addon.onBreweryReload();
		}
		plugin.getLogger().info("Reloaded " + addons.size() + " addons");
	}

	/**
	 * Get all classes that extend Addon and instantiates them
	 */
	public void loadAddons() {
		File[] files = addonsFolder.listFiles((dir, name) -> name.endsWith(".jar"));
		if (files == null) {
			return;
		}

		for (File file : files) {

			try {
				List<Class<?>> classes = loadAllClassesFromJar(file);

				for (Class<?> clazz : classes) {
					Class<? extends BreweryAddon> addonClass = clazz.asSubclass(BreweryAddon.class);
					try {
						BreweryAddon addon = addonClass.getConstructor(BreweryPlugin.class, AddonLogger.class).newInstance(plugin, new AddonLogger(addonClass));
						addon.onAddonEnable(new AddonFileManager(addon));
						addons.add(addon);
					} catch (Exception e) {
						plugin.getLogger().log(Level.SEVERE,"Failed to load addon class " + clazz.getSimpleName(), e);
					}
				}
			} catch (Exception ex) {
				plugin.getLogger().log(Level.SEVERE, "Failed to load addon classes from jar " + file.getName(), ex);
			}
		}
		plugin.getLogger().info("Loaded " + addons.size() + " addons");
	}


	public static <T> @NotNull List<Class<? extends T>> findClasses(@NotNull final File file, @NotNull final Class<T> clazz) throws CompletionException {
		if (!file.exists()) {
			return Collections.emptyList();
		}

		final List<Class<? extends T>> classes = new ArrayList<>();

		final List<String> matches = matchingNames(file);

		for (final String match : matches) {
			try {
				final URL jar = file.toURI().toURL();
				try (final URLClassLoader loader = new URLClassLoader(new URL[]{jar}, clazz.getClassLoader())) {
					Class<? extends T> addonClass = loadClass(loader, match, clazz);
					if (addonClass != null) {
						classes.add(addonClass);
					}
				}
			} catch (final VerifyError ignored) {
			} catch (IOException | ClassNotFoundException e) {
				throw new CompletionException(e.getCause());
			}
		}
		return classes;
	}

	public List<Class<?>> loadAllClassesFromJar(File jarFile) {

		List<Class<?>> classes = new ArrayList<>();
		try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, getClass().getClassLoader())) {
			try (JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile))) {
				JarEntry jarEntry;
				while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
					if (jarEntry.getName().endsWith(".class")) {
						String className = jarEntry.getName().replaceAll("/", ".").replace(".class", "");
						try {
							Class<?> clazz = Class.forName(className, true, classLoader);
							if (BreweryAddon.class.isAssignableFrom(clazz)) {
								classes.add(clazz);
							}
						} catch (ClassNotFoundException e) {
							plugin.getLogger().log(Level.SEVERE, "Failed to load class " + className, e);
						}
					}
				}
			}
		} catch (IOException e) {
			plugin.getLogger().log(Level.SEVERE, "Error loading classes from JAR", e);
		}
		return classes;
	}

	private static @NotNull List<String> matchingNames(final File file) {
		final List<String> matches = new ArrayList<>();
		try {
			final URL jar = file.toURI().toURL();
			try (final JarInputStream stream = new JarInputStream(jar.openStream())) {
				JarEntry entry;
				while ((entry = stream.getNextJarEntry()) != null) {
					final String name = entry.getName();
					if (!name.endsWith(".class")) {
						continue;
					}

					matches.add(name.substring(0, name.lastIndexOf('.')).replace('/', '.'));
				}
			}
		} catch (Exception e) {
			return Collections.emptyList();
		}
		return matches;
	}

	private static <T> @Nullable Class<? extends T> loadClass(final @NotNull URLClassLoader loader, final String match, @NotNull final Class<T> clazz) throws ClassNotFoundException {
		try {
			final Class<?> loaded = loader.loadClass(match);
			if (clazz.isAssignableFrom(loaded)) {
				return (loaded.asSubclass(clazz));
			}
		} catch (final NoClassDefFoundError ignored) {
		}
		return null;
	}


	public static void registerAddonCommand(AddonCommand addonCommand) {
		try {
			final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

			bukkitCommandMap.setAccessible(true);
			CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
			if (commandMap.getCommand(addonCommand.getName()) != null) {
				commandMap.getCommand(addonCommand.getName()).unregister(commandMap);
			}

			commandMap.register(addonCommand.getName(), "brewery", addonCommand);
			addonCommands.add(addonCommand);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public static void unRegisterAddonCommand(AddonCommand addonCommand) {
		try {
			final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

			bukkitCommandMap.setAccessible(true);
			CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
			if (commandMap == null) return;

			for (AddonCommand bukkitCommand : addonCommands) {
				for (String alias : bukkitCommand.getAliases()) {
					commandMap.getCommand(alias).unregister(commandMap);
				}
				commandMap.getCommand(bukkitCommand.getName()).unregister(commandMap);
			}
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

}
